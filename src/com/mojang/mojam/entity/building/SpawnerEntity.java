package com.mojang.mojam.entity.building;

import com.mojang.mojam.entity.mob.Bat;
import com.mojang.mojam.entity.mob.Mob;
import com.mojang.mojam.entity.mob.Mummy;
import com.mojang.mojam.entity.mob.Snake;
import com.mojang.mojam.entity.mob.Team;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.network.TurnSynchronizer;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;

public class SpawnerEntity extends Building {
	public static final int SPAWN_INTERVAL = 60 * 4;

	public int spawnTime = 0;

	public int type;

	public SpawnerEntity(double x, double y, int type) {
		super(x, y, Team.Enemy);

		this.type = type;
		setStartHealth(20);
		freezeTime = 10;
		spawnTime = TurnSynchronizer.synchedRandom.nextInt(SPAWN_INTERVAL);
		minimapIcon = 4;

		deathPoints = type * 5 + 5;
	}

	@Override
	public void tick() {
		super.tick();
		if (freezeTime > 0)
			return;

		if (--spawnTime <= 0) {
			spawn();
			spawnTime = SPAWN_INTERVAL;
		}
	}

	private void spawn() {
		double x = pos.x + (TurnSynchronizer.synchedRandom.nextFloat() - 0.5)
				* 5;
		double y = pos.y + (TurnSynchronizer.synchedRandom.nextFloat() - 0.5)
				* 5;
		x = Math.max(Math.min(x, ((level.width-10) * Tile.WIDTH)), 10 * Tile.WIDTH);// spawn only
																// inside the
																// level!
		y = Math.max(Math.min(y, ((level.height-10) * Tile.HEIGHT)), 10 * Tile.HEIGHT);
		Mob te = null;
		if (type == 0)
			te = new Bat(x, y);
		if (type == 1)
			te = new Snake(x, y);
		if (type == 2)
			te = new Mummy(x, y);

		if (level.getEntities(te.getBB().grow(8), te.getClass()).size() == 0) {
			level.addEntity(te);
		}
	}

	@Override
	public void die() {
		super.die();
	}

	private int lastIndex = 0;

	@Override
	public Bitmap getSprite() {
		int newIndex = 3 - (3 * health) / maxHealth;
		if (newIndex != lastIndex) {
			// if (newIndex > lastIndex) // means more hurt
			// level.addEntity(new SmokeAnimation(pos.x - 12, pos.y - 20,
			// Art.fxSteam24, 40));
			lastIndex = newIndex;
		}
		return Art.mobSpawner[newIndex][0];
	}
}
