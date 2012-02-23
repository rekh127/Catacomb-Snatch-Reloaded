package com.mojang.mojam;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.Stack;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.mojang.mojam.entity.Player;
import com.mojang.mojam.entity.building.Base;
import com.mojang.mojam.entity.mob.Team;
import com.mojang.mojam.gui.Button;
import com.mojang.mojam.gui.ButtonListener;
import com.mojang.mojam.gui.ChatStack;
import com.mojang.mojam.gui.CheckBox;
import com.mojang.mojam.gui.Font;
import com.mojang.mojam.gui.menu.ChatOverlay;
import com.mojang.mojam.gui.menu.GuiMenu;
import com.mojang.mojam.gui.menu.HostingWaitMenu;
import com.mojang.mojam.gui.menu.InvalidHostMenu;
import com.mojang.mojam.gui.menu.JoinGameMenu;
import com.mojang.mojam.gui.menu.Overlay;
import com.mojang.mojam.gui.menu.PauseMenu;
import com.mojang.mojam.gui.menu.TitleMenu;
import com.mojang.mojam.gui.menu.WinMenu;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.network.ClientSidePacketLink;
import com.mojang.mojam.network.CommandListener;
import com.mojang.mojam.network.EndGameCommand;
import com.mojang.mojam.network.NetworkCommand;
import com.mojang.mojam.network.NetworkPacketLink;
import com.mojang.mojam.network.Packet;
import com.mojang.mojam.network.PacketLink;
import com.mojang.mojam.network.PacketListener;
import com.mojang.mojam.network.TurnSynchronizer;
import com.mojang.mojam.network.packet.ChangeKeyCommand;
import com.mojang.mojam.network.packet.ChatCommand;
import com.mojang.mojam.network.packet.PauseCommand;
import com.mojang.mojam.network.packet.StartGamePacket;
import com.mojang.mojam.network.packet.TurnPacket;
import com.mojang.mojam.screen.Screen;
import com.mojang.mojam.sound.SoundPlayer;

public class MojamComponent extends Canvas implements Runnable,
		MouseMotionListener, CommandListener, PacketListener, MouseListener,
		ButtonListener, KeyListener {

	private static final long serialVersionUID = 1L;
	public static final int GAME_WIDTH = 512;
	public static final int GAME_HEIGHT = GAME_WIDTH * 3 / 4;
	public static final int SCALE = 2;
	private boolean running = true;
	private Cursor emptyCursor;
	private double framerate = 60;
	private int fps;
	private Screen screen = new Screen(GAME_WIDTH, GAME_HEIGHT);
	private Level level;

	private Stack<GuiMenu> menuStack = new Stack<GuiMenu>();

	private boolean mouseMoved = false;
	private boolean mouseHidden = false;
	private int mouseHideTime = 0;
	public MouseButtons mouseButtons = new MouseButtons();
	public Keys keys = new Keys();
	public Keys[] synchedKeys = { new Keys(), new Keys() };
	public Player player;
	public TurnSynchronizer synchronizer;
	private PacketLink packetLink;
	private ServerSocket serverSocket;
	private boolean isMultiplayer;
	private boolean isServer;
	private int localId;
	private Thread hostThread;
	public static SoundPlayer soundPlayer;

	private int createServerState = 0;
	private boolean showFPS;
	private boolean paused;
	private boolean chatting;

	private ChatStack chats = new ChatStack();
	private Thread parentThread;

	public MojamComponent() {
		this.setPreferredSize(new Dimension(GAME_WIDTH * SCALE, GAME_HEIGHT
				* SCALE));
		this.setMinimumSize(new Dimension(GAME_WIDTH * SCALE, GAME_HEIGHT
				* SCALE));
		this.setMaximumSize(new Dimension(GAME_WIDTH * SCALE, GAME_HEIGHT
				* SCALE));
		this.addKeyListener(new InputHandler(keys));
		this.addMouseMotionListener(this);
		this.addMouseListener(this);

		TitleMenu menu = new TitleMenu(GAME_WIDTH, GAME_HEIGHT);
		addMenu(menu);
		addKeyListener(this);
		showFPS = false;
		chatting = false;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		mouseMoved = true;
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		mouseMoved = true;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		mouseButtons.releaseAll();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mouseButtons.setNextState(e.getButton(), true);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouseButtons.setNextState(e.getButton(), false);
	}

	@Override
	public void paint(Graphics g) {
	}

	@Override
	public void update(Graphics g) {
	}

	public void start() {
		parentThread = Thread.currentThread();
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}

	public void stop() {
		running = false;
		soundPlayer.shutdown();
	}

	private void init() {
		soundPlayer = new SoundPlayer();
		soundPlayer.playMusic(SoundPlayer.TITLE_ID);

		try {
			emptyCursor = Toolkit.getDefaultToolkit().createCustomCursor(
					new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
					new Point(0, 0), "empty");
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		setFocusTraversalKeysEnabled(false);
		requestFocus();

	}

	private synchronized void createLevel() {
		soundPlayer.playMusic(SoundPlayer.BACKGROUND_ID);
		Player[] players = new Player[2];
		try {
			level = Level.fromFile("/res/levels/level1.bmp");
		} catch (Exception ex) {
			throw new RuntimeException("Unable to load level", ex);
		}

		level.init();

		players[0] = new Player(synchedKeys[0], level.width * Tile.WIDTH / 2
				- 16, (level.height - 5 - 1) * Tile.HEIGHT - 16, Team.Team1);
		players[0].setFacing(4);
		level.addEntity(new Base(34 * Tile.WIDTH, 7 * Tile.WIDTH, Team.Team1));
		level.addPlayer(players[0]);
		if (isMultiplayer) {
			players[1] = new Player(synchedKeys[1], level.width * Tile.WIDTH
					/ 2 - 16, 7 * Tile.HEIGHT - 16, Team.Team2);
			// players[1] = new Player(synchedKeys[1], 10, 10);
			level.addEntity(new Base(32 * Tile.WIDTH - 20,
					32 * Tile.WIDTH - 20, Team.Team2));

		} else {
			players[1] = new Player(new Keys(), level.width * Tile.WIDTH / 2
					- 16, 7 * Tile.HEIGHT - 16, Team.Team2);
		}
		level.addPlayer(players[1]);
		player = players[localId];
		player.setCanSee(true);
	}

	@Override
	public void run() {
		while (parentThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		running = true;
		long lastTime = System.nanoTime();
		double unprocessed = 0;
		int frames = 0;
		long lastTimer1 = System.currentTimeMillis();

		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		int toTick = 0;

		long lastRenderTime = System.nanoTime();
		int min = 999999999;
		int max = 0;

		while (running) {
			if (!this.hasFocus()) {
				keys.release();
			}

			double nsPerTick = 1000000000.0 / framerate;
			boolean shouldRender = false;
			while (unprocessed >= 1) {
				toTick++;
				unprocessed -= 1;
			}

			int tickCount = toTick;
			if (toTick > 0 && toTick < 3) {
				tickCount = 1;
			}
			if (toTick > 20) {
				toTick = 20;
			}

			for (int i = 0; i < tickCount; i++) {
				toTick--;
				// long before = System.nanoTime();
				tick();
				// long after = System.nanoTime();
				// System.out.println("Tick time took " + (after - before) *
				// 100.0 / nsPerTick + "% of the max time");
				shouldRender = true;
			}
			// shouldRender = true;

			BufferStrategy bs = getBufferStrategy();
			if (bs == null) {
				createBufferStrategy(3);
				continue;
			}
			if (shouldRender) {
				frames++;
				Graphics g = bs.getDrawGraphics();

				Random lastRandom = TurnSynchronizer.synchedRandom;
				TurnSynchronizer.synchedRandom = null;

				render(g);

				TurnSynchronizer.synchedRandom = lastRandom;

				long renderTime = System.nanoTime();
				int timePassed = (int) (renderTime - lastRenderTime);
				if (timePassed < min) {
					min = timePassed;
				}
				if (timePassed > max) {
					max = timePassed;
				}
				lastRenderTime = renderTime;
			}

			long now = System.nanoTime();
			unprocessed += (now - lastTime) / nsPerTick;
			lastTime = now;

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (shouldRender) {
				if (bs != null) {
					bs.show();
				}
			}

			if (System.currentTimeMillis() - lastTimer1 > 1000) {
				lastTimer1 += 1000;
				fps = frames;
				frames = 0;
				chats.trim();
			}
		}
		JPanel panel = (JPanel) getParent();
		panel.remove(this);
		panel.validate();
	}

	private synchronized void render(Graphics g) {
		if (level != null) {
			int xScroll = (int) (player.pos.x - screen.w / 2);
			int yScroll = (int) (player.pos.y - (screen.h - 24) / 2);
			soundPlayer.setListenerPosition((float) player.pos.x,
					(float) player.pos.y);
			level.render(screen, xScroll, yScroll);
		}
		GuiMenu topMenu;
		if (!menuStack.isEmpty()) {
			topMenu = menuStack.peek();
			topMenu.render(screen);
		} else {
			topMenu = null;
		}
		if (showFPS) {
			Font.draw(screen, "FPS: " + fps, 10, 10);
		}
		// for (int p = 0; p < players.length; p++) {
		// if (players[p] != null) {
		// String msg = "P" + (p + 1) + ": " + players[p].getScore();
		// Font.draw(screen, msg, 320, screen.h - 24 + p * 8);
		// }
		// }
		if (player != null && (topMenu == null || (topMenu instanceof Overlay))) {
			Font.draw(screen, player.health + " / 10", 340, screen.h - 19);
			Font.draw(screen, "" + player.money, 340, screen.h - 33);
			if (chats.size() > 0) {
				ChatStack temp = (ChatStack) chats.clone();
				int ypos = 300;
				while (!temp.isEmpty()) {
					Font.draw(screen, temp.pop(), 0, ypos);
					ypos -= 10;
				}

			}
		}

		g.setColor(Color.BLACK);

		g.fillRect(0, 0, getWidth(), getHeight());
		g.translate((getWidth() - GAME_WIDTH * SCALE) / 2,
				(getHeight() - GAME_HEIGHT * SCALE) / 2);
		g.clipRect(0, 0, GAME_WIDTH * SCALE, GAME_HEIGHT * SCALE);

		if (!menuStack.isEmpty() || level != null) {
			g.drawImage(screen.image, 0, 0, GAME_WIDTH * SCALE, GAME_HEIGHT
					* SCALE, null);
		}

		// String msg = "FPS: " + fps;
		// g.setColor(Color.LIGHT_GRAY);
		// g.drawString(msg, 11, 11);
		// g.setColor(Color.WHITE);
		// g.drawString(msg, 10, 10);

	}

	private void tick() {

		if (packetLink != null) {
			packetLink.tick();
		}

		if (level != null) {
			if (synchronizer.preTurn()) {
				synchronizer.postTurn();
				if (!paused) {
					for (int index = 0; index < keys.getAll().size(); index++) {
						Keys.Key key = keys.getAll().get(index);
						boolean nextState = key.nextState;
						if (key.isDown != nextState) {
							if (!chatting) {
								synchronizer.addCommand(new ChangeKeyCommand(
										index, nextState));
							}
						}
					}

					keys.tick();
					for (Keys skeys : synchedKeys) {
						skeys.tick();
					}

					level.tick();
					if (keys.pause.wasPressed()) {
						keys.tick();
						synchronizer.addCommand(new PauseCommand(true));
					}
					if (!chatting && keys.chat.wasPressed()) {
						addMenu(new ChatOverlay(localId));
						chatting = true;
						keys.release();
						keys.tick();
					}
					if (player.getScore() >= Level.TARGET_SCORE) {
						synchronizer.addCommand(new EndGameCommand(localId));
					}
				}
			}
		}
		mouseButtons.setPosition(getMousePosition());
		if (!menuStack.isEmpty()) {
			menuStack.peek().tick(mouseButtons);
		}
		if (mouseMoved) {
			mouseMoved = false;
			mouseHideTime = 0;
			if (mouseHidden) {
				mouseHidden = false;
				setCursor(null);
			}
		}
		if (mouseHideTime < 60) {
			mouseHideTime++;
			if (mouseHideTime == 60) {
				setCursor(emptyCursor);
				mouseHidden = true;
			}
		}
		mouseButtons.tick();

		if (createServerState == 1) {
			createServerState = 2;

			synchronizer = new TurnSynchronizer(MojamComponent.this,
					packetLink, localId, 2);

			clearMenus();
			createLevel();

			synchronizer.setStarted(true);
			packetLink.sendPacket(new StartGamePacket(
					TurnSynchronizer.synchedSeed));
			packetLink.setPacketListener(MojamComponent.this);

		}
	}

	public static void main(String[] args) {
		MojamComponent mc = new MojamComponent();
		JFrame frame = new JFrame();
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(mc);
		frame.setContentPane(panel);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		mc.start();
	}

	@Override
	public void handle(int playerId, NetworkCommand packet) {

		if (packet instanceof ChangeKeyCommand) {
			ChangeKeyCommand ckc = (ChangeKeyCommand) packet;
			synchedKeys[playerId].getAll().get(ckc.getKey()).nextState = ckc
					.getNextState();
		}
		if (packet instanceof PauseCommand) {
			PauseCommand pc = (PauseCommand) packet;
			paused = pc.isPause();
			if (paused) {
				addMenu(new PauseMenu(GAME_WIDTH, GAME_HEIGHT, showFPS,
						playerId));
			} else {
				popMenu();
			}
		}
		if (packet instanceof EndGameCommand) {
			EndGameCommand egc = (EndGameCommand) packet;
			addMenu(new WinMenu(GAME_WIDTH, GAME_HEIGHT, egc.getWinner()));
			soundPlayer.playMusic(SoundPlayer.ENDING_ID);
		}
		if (packet instanceof ChatCommand) {
			ChatCommand cc = (ChatCommand) packet;
			chats.push(cc.getMessage());
		}

	}

	@Override
	public void handle(Packet packet) {
		if (packet instanceof StartGamePacket) {
			if (!isServer) {
				synchronizer.onStartGamePacket((StartGamePacket) packet);
				createLevel();
			}
		} else if (packet instanceof TurnPacket) {
			synchronizer.onTurnPacket((TurnPacket) packet);
		}
	}

	@Override
	public void buttonPressed(Button button) {
		if (button.getId() == GuiMenu.RESTART_GAME_ID) {
			cleanUp();
			stop();
			MojamComponent mc = new MojamComponent();
			getParent().add(mc);
			mc.start();

		} else if (button.getId() == GuiMenu.START_GAME_ID) {
			clearMenus();
			isMultiplayer = false;

			localId = 0;
			synchronizer = new TurnSynchronizer(this, null, 0, 1);
			synchronizer.setStarted(true);

			createLevel();
		} else if (button.getId() == GuiMenu.HOST_GAME_ID) {
			addMenu(new HostingWaitMenu());
			isMultiplayer = true;
			isServer = true;
			try {
				if (isServer) {
					localId = 0;
					serverSocket = new ServerSocket(3000);
					serverSocket.setSoTimeout(1000);

					hostThread = new Thread() {

						@Override
						public void run() {
							boolean fail = true;
							try {
								while (!isInterrupted()) {
									Socket socket = null;
									try {
										socket = serverSocket.accept();
									} catch (SocketTimeoutException e) {

									}
									if (socket == null) {
										System.out.println("asdf");
										continue;
									}
									fail = false;

									packetLink = new NetworkPacketLink(socket);

									createServerState = 1;
									break;
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							if (fail) {
								try {
									serverSocket.close();
								} catch (IOException e) {
								}
							}
						};
					};
					hostThread.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (button.getId() == GuiMenu.JOIN_GAME_ID) {
			addMenu(new JoinGameMenu());
		} else if (button.getId() == GuiMenu.CANCEL_JOIN_ID) {
			popMenu();
			if (hostThread != null) {
				hostThread.interrupt();
				hostThread = null;
			}
		} else if (button.getId() == GuiMenu.PERFORM_JOIN_ID) {
			try {
				localId = 1;
				packetLink = new ClientSidePacketLink(TitleMenu.ip, 3000);
				synchronizer = new TurnSynchronizer(this, packetLink, localId,
						2);
				packetLink.setPacketListener(this);
				isMultiplayer = true;
				isServer = false;
				menuStack.clear();
			} catch (Exception e) {
				addMenu(new InvalidHostMenu());
			}
		} else if (button.getId() == GuiMenu.EXIT_GAME_ID) {
			System.exit(0);
		} else if (button.getId() == GuiMenu.RETURN_ID) {
			synchronizer.addCommand(new PauseCommand(false));
			keys.tick();
		} else if (button.getId() == GuiMenu.END_GAME_ID) {
			popMenu();
			synchronizer.addCommand(new EndGameCommand(1 - localId));

		} else if (button.getId() == GuiMenu.FPS_ID) {
			CheckBox box = (CheckBox) button;
			showFPS = box.isChecked();

		} else if (button.getId() == GuiMenu.BACK_ID) {
			popMenu();
		} else if (button.getId() == GuiMenu.SEND_ID) {
			ChatOverlay chat = (ChatOverlay) menuStack.peek();
			chatting = false;
			popMenu();
			String message = chat.getMessage();
			if (message != null) {
				synchronizer.addCommand(new ChatCommand(message));
			}
			keys.tick();

		}

	}

	private void cleanUp() {
		soundPlayer.stopMusic();
		level = null;
		player = null;
		synchronizer = null;
		emptyCursor = null;
		if (isMultiplayer) {
			packetLink = null;
			if (isServer) {
				serverSocket = null;
			}
		}
	}

	private void clearMenus() {
		while (!menuStack.isEmpty()) {
			menuStack.pop();
		}
	}

	private void addMenu(GuiMenu menu) {
		menuStack.add(menu);
		menu.addButtonListener(this);
	}

	private void popMenu() {
		if (!menuStack.isEmpty()) {
			menuStack.pop();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (!menuStack.isEmpty()) {
			menuStack.peek().keyPressed(e);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!menuStack.isEmpty()) {
			menuStack.peek().keyReleased(e);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (!menuStack.isEmpty()) {
			menuStack.peek().keyTyped(e);
		}
	}

}