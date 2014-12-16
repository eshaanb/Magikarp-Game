package com.eshaan.magikarpsplash.Widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.eshaan.magikarpsplash.MagikarpGame;
import com.eshaan.magikarpsplash.model.ClientServerObject;

import java.io.IOException;
import java.io.OutputStream;

import static com.badlogic.gdx.Application.ApplicationType.Android;
import static com.eshaan.magikarpsplash.MagikarpGame.BIT_COUNTER;
import static com.eshaan.magikarpsplash.MagikarpGame.BIT_COUNTER_BAR;
import static com.eshaan.magikarpsplash.MagikarpGame.BIT_COUNTER_UPPER_BAR;
import static com.eshaan.magikarpsplash.MagikarpGame.BIT_GROUND;
import static com.eshaan.magikarpsplash.MagikarpGame.BIT_MAGIKARP;
import static com.eshaan.magikarpsplash.MagikarpGame.FIXTURES.MECOUNTER;
import static com.eshaan.magikarpsplash.MagikarpGame.FIXTURES.OPPCOUNTER;
import static com.eshaan.magikarpsplash.MagikarpGame.PX_PER_METER;

/**
 * Created by ebhalla on 10/9/14.
 */
public class MagikarpPlayer extends Actor {

	private Socket myClient;
	private BitmapFont myFont;
	private Body magikarpBody;
	private Sprite meSprite;
	private float elapsedTime = 0;
	private Animation splashAnim;

	private int myCount = 0;
	//160x40 is player 1 xy

	public MagikarpPlayer(float unscaledX, float unscaledY, World world, TextureAtlas myAtlas, Color fontColor, String name, Socket client) {

		setName(name);

		myClient = client;

		this.meSprite = new Sprite(myAtlas.findRegion("0001"));
		meSprite.setSize(100, 120);

		myFont = new BitmapFont();
		myFont.setColor(fontColor);

		splashAnim = new Animation(1/15f, myAtlas.getRegions());


		BodyDef magikarpBodyDef = new BodyDef();
		magikarpBodyDef.type = BodyDef.BodyType.DynamicBody;
		magikarpBodyDef.position.set(unscaledX/ PX_PER_METER, (meSprite.getHeight()/2f+unscaledY)/PX_PER_METER);
		magikarpBody = world.createBody(magikarpBodyDef);
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(meSprite.getWidth()/2f/PX_PER_METER, meSprite.getHeight()/2f/PX_PER_METER);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.density = .3f;
		fixtureDef.filter.categoryBits = BIT_MAGIKARP;
		fixtureDef.filter.maskBits = BIT_COUNTER | BIT_GROUND;
		magikarpBody.createFixture(fixtureDef);
		shape.dispose();

		BodyDef counterDef = new BodyDef();
		counterDef.position.set(magikarpBodyDef.position.x, (Gdx.graphics.getHeight()-39.9f)/PX_PER_METER);
		counterDef.type = BodyDef.BodyType.DynamicBody;
		Body counterBody = world.createBody(counterDef);
		PolygonShape counterShape = new PolygonShape();
		counterShape.setAsBox(10f/PX_PER_METER, 10f/PX_PER_METER);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = counterShape;
		fixtureDef.filter.categoryBits = BIT_COUNTER;
		fixtureDef.filter.maskBits = BIT_MAGIKARP | BIT_COUNTER_BAR | BIT_COUNTER_UPPER_BAR;
		counterBody.createFixture(fixtureDef).setUserData(myClient != null ? MECOUNTER : OPPCOUNTER);
		counterShape.dispose();

		PrismaticJointDef magikarpJoint = new PrismaticJointDef();
		magikarpJoint.initialize(magikarpBody, counterBody, new Vector2(magikarpBody.getPosition().x, magikarpBody.getPosition().y), Vector2.Y);
		magikarpJoint.collideConnected = true;
		world.createJoint(magikarpJoint);


		setBounds(magikarpBody.getPosition().x, magikarpBody.getPosition().y, meSprite.getWidth(), meSprite.getHeight());
	}

	public boolean keyDown(InputEvent event, int keycode) {
		if (keycode == Input.Keys.SPACE) {
			doUpMove();
		}
		return true;
	}

	public boolean keyUp(InputEvent event, int keycode) {
		if (keycode == Input.Keys.SPACE && magikarpBody.getLinearVelocity().y > 0) {
			fallDown();
		}
		return true;
	}

	public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
		doUpMove();
		return true;
	}

	public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
		fallDown();
	}

	private void doUpMove() {
		Gdx.app.log(MagikarpGame.class.getSimpleName(), "position: " + magikarpBody.getPosition().y);
		if (magikarpBody.getPosition().y < 135f/PX_PER_METER && magikarpBody.getPosition().y > 101.5f/PX_PER_METER) {
			fastUp();
		} else if (magikarpBody.getPosition().y < 165f/PX_PER_METER) {
			slowUp();
		}
	}

	public void slowUp() {
		resetLinearVel();
		magikarpBody.applyForceToCenter(0, Gdx.app.getType() == Android ? 350f : 350f, true);
		if (myClient != null) {
			OutputStream out = myClient.getOutputStream();
			ClientServerObject sendToServer = new ClientServerObject();
			sendToServer.action = MagikarpGame.CLIENTSERVER_ACTION.JUMP_SLOW.getValue();
			sendToServer.x = magikarpBody.getPosition().x;
			sendToServer.y = magikarpBody.getPosition().y;
			sendToServer.yvel = magikarpBody.getLinearVelocity().y;
			try {
				out.write(sendToServer.toJson());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void fastUp() {
		resetLinearVel();
		magikarpBody.applyForceToCenter(0, Gdx.app.getType() == Android ? 800f : 500f, true);
		if (myClient != null) {
			OutputStream out = myClient.getOutputStream();
			ClientServerObject sendToServer = new ClientServerObject();
			sendToServer.action = MagikarpGame.CLIENTSERVER_ACTION.JUMP_FAST.getValue();
			sendToServer.x = magikarpBody.getPosition().x;
			sendToServer.y = magikarpBody.getPosition().y;
			sendToServer.yvel = magikarpBody.getLinearVelocity().y;
			try {
				out.write(sendToServer.toJson());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void fallDown() {
		if (magikarpBody.getLinearVelocity().y > 0) {
			magikarpBody.applyLinearImpulse(new Vector2(0, -magikarpBody.getLinearVelocity().y*.3f), new Vector2(magikarpBody.getPosition().x, magikarpBody.getPosition().y), true);
		}
		if (myClient != null) {
			OutputStream out = myClient.getOutputStream();
			ClientServerObject sendToServer = new ClientServerObject();
			sendToServer.action = MagikarpGame.CLIENTSERVER_ACTION.FALL_DOWN.getValue();
			sendToServer.x = magikarpBody.getPosition().x;
			sendToServer.y = magikarpBody.getPosition().y;
			sendToServer.yvel = magikarpBody.getLinearVelocity().y;
			try {
				out.write(sendToServer.toJson());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public float getHeight() {
		return meSprite.getHeight();
	}

	public float getWidth() {
		return meSprite.getWidth();
	}

	public float getX() {
		return magikarpBody.getPosition().x;
	}

	public float getY() {
		return magikarpBody.getPosition().y;
	}

	private void resetLinearVel() {
		magikarpBody.setLinearVelocity(new Vector2(0, 0));
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		elapsedTime += Gdx.graphics.getDeltaTime();
		meSprite.setRegion(splashAnim.getKeyFrame(elapsedTime, true));
		meSprite.setPosition(magikarpBody.getPosition().x * PX_PER_METER - 50, magikarpBody.getPosition().y * PX_PER_METER - 66);
		meSprite.draw(batch);
		myFont.draw(batch, Integer.toString(myCount), (magikarpBody.getPosition().x*PX_PER_METER)-40, Gdx.graphics.getHeight() - 40f + myFont.getLineHeight());
	}

	public void incrementCount() {
		myCount++;
	}

	public void sync() {
	}
}
