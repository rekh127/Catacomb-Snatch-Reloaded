package com.mojang.mojam.network;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.gui.menu.PauseMenu;
import com.mojang.mojam.gui.menu.WinMenu;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.network.packet.*;
import com.mojang.mojam.sound.SoundPlayer;

public class PacketHandler implements CommandListener, PacketListener {
	
	public static TurnSynchronizer synchronizer;
	private MojamComponent component;
	private PacketLink packetLink;
	private int GAME_WIDTH=MojamComponent.GAME_WIDTH;
	private int GAME_HEIGHT=MojamComponent.GAME_HEIGHT;
	private int localId;
	private Level level;
	
	public PacketHandler(PacketLink pl, int lId, int numPlayers, MojamComponent comp){
		packetLink=pl;
		
		synchronizer = new TurnSynchronizer(this, packetLink, lId, numPlayers);
		synchronizer.setStarted(true);
		
		component = comp;
		
		if(packetLink != null){
			packetLink.sendPacket(new StartGamePacket(TurnSynchronizer.synchedSeed));
			packetLink.setPacketListener(this);
		}
		localId=lId;
		
	}
	
	public void setLevel(Level lvl){
		level=lvl;
	}
	
	public void changeKey(int index, boolean nextState) {
		ChangeKeyCommand command = new ChangeKeyCommand(index, nextState);
		synchronizer.addCommand(command);
		handle(localId, command);
	}
	
	public void chat(String message) {
		ChatCommand command = new ChatCommand(message, ChatCommand.CHAT_TYPE);
		synchronizer.addCommand(command);
		handle(localId, command);
		
	}
	
	public void endGame(int i) {
		EndGameCommand command = new EndGameCommand(1 - i);
		synchronizer.addCommand(command);
		handle(localId, command);
		
	}
	
	public void pause(boolean b){
		PauseCommand command = new PauseCommand(b);
		synchronizer.addCommand(command);
		handle(localId, command);
	}
	
	public void notify(String message) {
		ChatCommand command = new ChatCommand(message, ChatCommand.NOTE_TYPE);
		synchronizer.addCommand(command);
		handle(localId, command);
	}

	
	public void cleanUp() {
		synchronizer = null;
		packetLink = null;
		component = null;
		
	}
	
	
	public boolean preTurn() {
		return synchronizer.preTurn();
	}
	public void postTurn() {
		synchronizer.postTurn();
	}
	
	public void tick() {
		if(packetLink != null){
			packetLink.tick();
		}
		
	}

	@Override
	public void handle(int playerId, ChangeKeyCommand command) {
		component.synchedKeys[playerId].getAll().get(command.getKey()).nextState = command.getNextState();
		
	}
	public void handle(int playerId, ChatCommand packet) {
		switch(packet.getType()) {
		case ChatCommand.CHAT_TYPE:	component.addChat(packet.getMessage());
									break;
		case ChatCommand.NOTE_TYPE:	component.addNote(packet.getMessage());
									break;
		}
	}
	public void handle(int playerId, EndGameCommand packet) {
		component.addMenu(new WinMenu(GAME_WIDTH, GAME_HEIGHT, packet.getWinner()));
		MojamComponent.soundPlayer.playMusic(SoundPlayer.ENDING_ID);
	}
	public void handle(int playerId, PauseCommand packet) {
		component.paused = packet.isPause();
		if (component.paused) {
			component.addMenu(new PauseMenu(GAME_WIDTH, GAME_HEIGHT, component.showFPS,
					playerId));
		} else {
			component.popMenu();
		}
	}

	@Override
	public void handle(Packet packet) {
		if (packet instanceof StartGamePacket) {
			if (!component.isServer) {
				synchronizer.onStartGamePacket((StartGamePacket) packet);
				component.createLevel();
			}
		} else if (packet instanceof TurnPacket) {
			synchronizer.onTurnPacket((TurnPacket) packet);
		}
	}

}
