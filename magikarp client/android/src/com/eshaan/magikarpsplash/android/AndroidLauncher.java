package com.eshaan.magikarpsplash.android;

import android.os.Bundle;
import android.view.MotionEvent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.eshaan.magikarpsplash.MagikarpGame;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new MagikarpGame(), config);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Gdx.input.getInputProcessor().keyDown(Input.Keys.SPACE);
				break;
			case MotionEvent.ACTION_UP:
				Gdx.input.getInputProcessor().keyUp(Input.Keys.SPACE);
				break;
		}
		return true;
	}


}
