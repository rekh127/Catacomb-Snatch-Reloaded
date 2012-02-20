package com.mojang.mojam.gui;

import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Screen;

public class Font {
	public static String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ   "
			+ "0123456789-.!?/%$\\=*+,;:()&#\"'";

	public static int getStringWidth(String s) {
		return s.length() * 8;
	}

	private Font() {
	}

	public static void draw(Screen screen, String msg, int x, int y) {
		msg = msg.toUpperCase();
		int length = msg.length();
		for (int i = 0; i < length; i++) {
			int c = letters.indexOf(msg.charAt(i));
			if (c < 0)
				continue;
			screen.blit(Art.font[c % 29][c / 29], x, y);
			x += 8;
		}
	}
}
