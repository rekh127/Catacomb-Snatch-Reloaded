package com.mojang.mojam.entity;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.mob.Mob;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Screen;

public class Bullet extends Entity {
	public double xa, ya;
	public Mob owner;
	boolean hit = false;
	public int life;
	private int facing;

	public Bullet(Mob e, double xa, double ya) {
		this.owner = e;
		pos.set(e.pos.x + xa * 4, e.pos.y + ya * 4 - 3);
		this.xa = xa * 6;
		this.ya = ya * 6;
		this.setSize(4, 4);
		physicsSlide = false;
		life = 40;
		double angle = (Math.atan2(ya, xa) + Math.PI * 1.625);
		facing = (8 + (int) (angle / Math.PI * 4)) & 7;
	}

	@Override
	public void tick() {
		if (--life <= 0) {
			remove();
			return;
		}
		if (!move(xa, ya)) {
			hit = true;
		}
		if (hit && !removed) {
			remove();
		}
	}

	@Override
	protected boolean shouldBlock(Entity e) {
		if (e instanceof Bullet) {
			return false;
		}
		if (e instanceof Mob) {
			Mob m = (Mob) e;
			return !m.isFriendOf(owner);
		}
		return true;
	}

	@Override
	public void render(Screen screen) {
		screen.blit(Art.bullet[facing][0], pos.x - 8, pos.y - 10);
	}

	@Override
	public void collide(Entity entity, double xa, double ya) {
		if (entity instanceof Mob) {
			Mob mob = (Mob) entity;
			if (!mob.isFriendOf(owner)) {
				entity.hurt(this);
				hit = true;
			}
		} else {
			entity.hurt(this);
			hit = true;
		}
		if (hit) {
			MojamComponent.soundPlayer.playSound("/res/sound/Shot 2.wav",
					(float) pos.x, (float) pos.y);
		}
	}
}
