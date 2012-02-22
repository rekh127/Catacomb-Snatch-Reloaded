package com.mojang.mojam.entity.mob;

import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.network.TurnSynchronizer;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;

public class Snake extends Enemy {
	public int facing;
	public int walkTime;
	public int stepTime;

	public Snake(double x, double y) {
		super(x, y);
		setPos(x, y);
		setStartHealth(3);
		dir = TurnSynchronizer.synchedRandom.nextDouble() * Math.PI * 2;
		minimapColor = 0xffff0000;
		yOffs = 10;
		facing = TurnSynchronizer.synchedRandom.nextInt(4);

		deathPoints = 2;
	}

	@Override
	public void tick() {
		super.tick();
		if (freezeTime > 0)
			return;

		double speed = 1.5;
		if (facing == 0)
			yd += speed;
		if (facing == 1)
			xd -= speed;
		if (facing == 2)
			yd -= speed;
		if (facing == 3)
			xd += speed;
		walkTime++;

		if (walkTime / 12 % 4 != 0) {
			stepTime++;
			if (!move(xd, yd)
					|| (walkTime > 10 && TurnSynchronizer.synchedRandom
							.nextInt(200) == 0)) {
				facing = TurnSynchronizer.synchedRandom.nextInt(4);
				walkTime = 0;
			}
		}
		xd *= 0.2;
		yd *= 0.2;
	}

	@Override
	public void die() {
		super.die();
	}

	@Override
	public Bitmap getSprite() {
		return Art.snake[((stepTime / 6) & 3)][(facing + 3) & 3];
	}

	@Override
	public void collide(Entity entity, double xa, double ya) {
		super.collide(entity, xa, ya, 2);
	}

	@Override
	public String getDeatchSound() {
		return "/res/sound/Enemy Death 2.wav";
	}
}
