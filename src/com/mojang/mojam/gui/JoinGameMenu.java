package com.mojang.mojam.gui;

import java.awt.event.KeyEvent;

import com.mojang.mojam.screen.Screen;

public class JoinGameMenu extends GuiMenu {

	private Button joinButton;
	int textHeight;

	public JoinGameMenu() {
		super(100);
		textHeight=getNextHeight();
		joinButton = addButton(new Button(TitleMenu.PERFORM_JOIN_ID, 3, 100,
				getNextHeight()));
		addButton(new Button(TitleMenu.CANCEL_JOIN_ID, 4, 250, getSameHeight()));
	}

	@Override
	public void render(Screen screen) {

		screen.clear(0);
		Font.draw(screen, "Enter IP of Host:", 100, textHeight);
		Font.draw(screen, TitleMenu.ip + "-", 100, textHeight+20);

		super.render(screen);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_ENTER && TitleMenu.ip.length() > 0) {
			joinButton.postClick();
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent e) {

		if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE
				&& TitleMenu.ip.length() > 0) {
			TitleMenu.ip = TitleMenu.ip.substring(0, TitleMenu.ip.length() - 1);
		} else if (Font.letters.indexOf(Character.toUpperCase(e.getKeyChar())) >= 0) {
			TitleMenu.ip += e.getKeyChar();
		}
	}

}
