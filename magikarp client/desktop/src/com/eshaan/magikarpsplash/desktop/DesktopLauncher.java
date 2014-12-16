package com.eshaan.magikarpsplash.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.eshaan.magikarpsplash.MagikarpGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
//		TexturePacker.process("/Users/ebhalla/Downloads/e912f39440-gif" , "/Users/ebhalla/Downloads/e912f39440-gif", "packed");
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1920;
		config.height = 1080;
		new LwjglApplication(new MagikarpGame(), config);
	}
}
