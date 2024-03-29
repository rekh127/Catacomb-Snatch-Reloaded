package com.mojang.mojam.gui;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.screen.Screen;

public class Notifications {

	public class Note {
		public String message;
		public int life;

		public Note(String message, int life) {
			this.message = message;
			this.life = life;
		}

		public void tick() {
			if (life-- <= 0) {
				Notifications.getInstance().notes.remove(this);
			}
		}
	}

	private static Notifications instance = null;

	private List<Note> notes = new CopyOnWriteArrayList<Note>();

	public void add(String message) {
		add(message, 150);
	}

	public void add(String message, int life) {
		notes.add(new Note(message, life));
		System.out.println(notes.size());
	}

	public void render(Screen screen) {
		Iterator<Note> it = notes.iterator();
		int i = 0;
		while (it.hasNext()) {
			i += 1;
			Note note = it.next();
			Font.draw(screen, note.message, (MojamComponent.GAME_WIDTH / 2)
					- (Font.getStringWidth(note.message) / 2),
					MojamComponent.GAME_HEIGHT / 5
							+ (i * 8 * MojamComponent.SCALE));
		}
	}

	public void tick() {
		for (int i = 0; i < notes.size(); i++) {

		}
		for (Note n : notes) {
			n.tick();
		}
	}

	private Notifications() {
	}

	public static synchronized Notifications getInstance() {
		if (instance == null) {
			instance = new Notifications();
		}

		return instance;
	}

}
