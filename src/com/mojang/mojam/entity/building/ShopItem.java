package com.mojang.mojam.entity.building;

import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.player.LocalPlayer;
import com.mojang.mojam.entity.mob.Team;
import com.mojang.mojam.gui.Font;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;
import com.mojang.mojam.screen.Screen;

public class ShopItem extends Building {

	private int facing = 0;

	public static final int SHOP_TURRET = 0;
	public static final int SHOP_HARVESTER = 1;
	public static final int SHOP_BOMB = 2;

	public static final int[] COST = { 150, 300, 500 };

	private final int type;

	public ShopItem(double x, double y, int type, int team) {
		super(x, y, team);
		this.type = type;
		isImmortal = true;
		if (team == Team.Team1) {
			facing = 4;
		}
	}

	@Override
	public void render(Screen screen) {
		super.render(screen);
		Bitmap image = getSprite();
		Font.draw(screen, "" + COST[type], (int) (pos.x - image.w / 2) + 3,
				(int) (pos.y + 7));
	}

	@Override
	public void init() {
	}

	@Override
	public void tick() {
		super.tick();
	}

	@Override
	public Bitmap getSprite() {
		switch (type) {
		case SHOP_TURRET:
			return Art.turret[facing][0];
		case SHOP_HARVESTER:
			return Art.harvester[facing][0];
		case SHOP_BOMB:
			return Art.bomb;
		}
		return Art.turret[facing][0];
	}

	@Override
	public void use(Entity user) {
		if (user instanceof LocalPlayer && ((LocalPlayer) user).getTeam() == team) {
			LocalPlayer player = (LocalPlayer) user;
			if (player.carrying == null && player.getMoney() >= COST[type]) {
				player.payCost(COST[type]);
				Building item = null;
				switch (type) {
				case SHOP_TURRET:
					item = new Turret(pos.x, pos.y, team);
					break;
				case SHOP_HARVESTER:
					item = new Harvester(pos.x, pos.y, team);
					break;
				case SHOP_BOMB:
					item = new Bomb(pos.x, pos.y);
					break;
				}
				level.addEntity(item);
				player.pickup(item);
			}
		}
	}

}
