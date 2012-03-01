package com.mojang.mojam.network.packet;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.mojang.mojam.gui.menu.LevelSelectMenu;
import com.mojang.mojam.network.Packet;
import com.mojang.mojam.network.PacketListener;

public class VerifyLevelPacket extends Packet {

	private byte[] md5;
	private String name;
	
	public VerifyLevelPacket() {
	}

	public VerifyLevelPacket(String lvl) {
		File level = new File(lvl);
		name=level.getName();
		byte[] bytes = null;
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(level));
			in.read(bytes);
			in.close();
			MessageDigest digester = MessageDigest.getInstance("MD5");
			md5=digester.digest(bytes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void read(DataInputStream dis) throws IOException {
		byte[] name = new byte[dis.readInt()];
		dis.readFully(name);
		this.name = new String(name);
		md5 = new byte[dis.readInt()];
		dis.readFully(md5);

	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		byte[] name = this.name.getBytes();
		dos.writeInt(name.length);
		dos.write(name);
		dos.writeInt(md5.length);
		dos.write(md5);

	}
	
	public String getLevel(){
		String path=LevelSelectMenu.levelDirectory.getPath();
		File level = new File(path+name);
		return level.getPath();
	}
	
	public boolean verify(String lvl) {
		File level = new File(lvl);
		name=level.getName();
		byte[] bytes = null;
		byte[] localMd5 = null;
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(level));
			in.read(bytes);
			in.close();
			MessageDigest digester = MessageDigest.getInstance("MD5");
			localMd5 =digester.digest(bytes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return MessageDigest.isEqual(localMd5, md5);
	}
	
	@Override
	public void handle(PacketListener packetListener) {
		packetListener.handle(this);
		
	}

}
