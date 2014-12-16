package com.eshaan.magikarpsplash;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Timer;
import com.eshaan.magikarpsplash.Widget.MagikarpPlayer;
import com.eshaan.magikarpsplash.model.ClientServerObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.eshaan.magikarpsplash.MagikarpGame.FIXTURES.MECOUNTER;
import static com.eshaan.magikarpsplash.MagikarpGame.FIXTURES.OPPCOUNTER;
import static com.eshaan.magikarpsplash.MagikarpGame.FIXTURES.UPPER_BAR;


public class MagikarpGame extends ApplicationAdapter implements ContactListener {

	private static final float TIME_STEP = 1/60f;
	public static final String HOST = "10.33.111.189";
	public static final int PORT = 7011;
	private float accumulator = 0;
	private List<TextButton> buttons;

	public enum FIXTURES {MECOUNTER, UPPER_BAR, OPPCOUNTER};

	private enum GAME_STATE {IP_LIST, COUNTDOWN_START, PLAYING, ENDED};

	public enum CLIENTSERVER_ACTION {
		SYNC("sync_opp"),
		FALL_DOWN("fall"),
		JUMP_FAST("jump_fast"),
		JUMP_SLOW("jump_slow"),
		GAME_ENDED("end_game"),
		CONNECT("connect"),
		OPP_SCORE("opp_score"),
		CHALLENGE("challenge"),
		IP_LIST("ipList");

		private final String value;

		private CLIENTSERVER_ACTION(String value) {
			this.value = value;
		}

		public boolean isAction(String action) {
			return value.equalsIgnoreCase(action);
		}

		public String getValue() {
			return value;
		}

		public static CLIENTSERVER_ACTION realValueOf(String input) {
			if (CONNECT.isAction(input)) {
				return CONNECT;
			} else if (OPP_SCORE.isAction(input)) {
				return OPP_SCORE;
			} else if (IP_LIST.isAction(input)) {
				return IP_LIST;
			} else if (CHALLENGE.isAction(input)) {
				return CHALLENGE;
			} else if (GAME_ENDED.isAction(input)) {
				return GAME_ENDED;
			} else if (SYNC.isAction(input)) {
				return SYNC;
			} else if (FALL_DOWN.isAction(input)) {
				return FALL_DOWN;
			} else if (JUMP_FAST.isAction(input)) {
				return JUMP_FAST;
			} else if (JUMP_SLOW.isAction(input)) {
				return JUMP_SLOW;
			}
			throw new IllegalArgumentException("couldnt find action: "+input);
		}

	}

	private List<Stage> stages;

	private Table table;

	private Socket myClient;
	private GAME_STATE myState;
	private SpriteBatch batch;
	private Sprite backGroundSprite;
	private TextureAtlas textureAtlas;
	private BitmapFont gameCountdownFont;
	private List<Integer> scores;
	private World world;
	private MagikarpPlayer me;
	private MagikarpPlayer opponent;
	private int playTime = 30;
	private int time = 3;
	private Box2DDebugRenderer debugRenderer;
	private OrthographicCamera camera;

	public static final short BIT_MAGIKARP = 2;
	public static final short BIT_COUNTER = 4;
	public static final short BIT_COUNTER_BAR = 8;
	public static final short BIT_GROUND = 16;
	public static final short BIT_COUNTER_UPPER_BAR = 32;


	public static final float PX_PER_METER = 100;

	private Timer.Task startCountdownTask = new Timer.Task() {
		@Override
		public void run() {
			if (time == 0) {
				if (myState == GAME_STATE.COUNTDOWN_START) {
					setState(GAME_STATE.PLAYING);
//  				Timer.schedule(playCountdownTask, 1, 1, playTime+1);
					Gdx.input.setInputProcessor(stages.get(1));
				}
				return;
			}
			time--;
		}
	};

	private Timer.Task playCountdownTask = new Timer.Task() {
		@Override
		public void run() {

		}
	};

	@Override
	public void create() {
//		final Skin mySkin ;
//		switch (Gdx.app.getType()) {
//			case Android:
//				mySkin = new Skin(Gdx.files.internal("androidskin.json"));
//				break;
//			case Desktop:
//				mySkin = new Skin(Gdx.files.internal("uiskin.json"));
//				break;
//			default:
//				mySkin = new Skin(Gdx.files.internal("uiskin.json"));
//		}
		final Skin mySkin = new Skin(Gdx.files.internal("uiskin.json"));
		TextureAtlas buttonAtlas = new TextureAtlas(Gdx.files.internal("uiskin.atlas"));
		mySkin.addRegions(buttonAtlas);
		stages = new ArrayList<Stage>();
		final Stage ipList = new Stage();
		ipList.setDebugAll(true);
		table = new Table(mySkin);
		final ScrollPane pane = new ScrollPane(table, mySkin);
		pane.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		ipList.addActor(pane);
		table.padTop(20);
		Stage playing = new Stage();
		ipList.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if (keycode == Input.Keys.SPACE) {
					for (TextButton butt : buttons) {
						if (!butt.getText().toString().contains("ME")) {
							butt.getClickListener().clicked(null,0,0);
						}
					}
				}
				return super.keyDown(event, keycode);
			}
		});
		playing.addListener(new InputListener() {

			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				return me.keyDown(event, keycode);
			}

			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				return me.keyUp(event, keycode);
			}

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				return me.touchDown(event, x, y, pointer, button);
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				me.touchUp(event, x, y, pointer, button);
			}
		});
		stages.add(ipList);
		stages.add(playing);
		setState(GAME_STATE.IP_LIST);

		scores = new ArrayList<Integer>();
		scores.add(0);
		gameCountdownFont = new BitmapFont();
		gameCountdownFont.setScale(10);
		gameCountdownFont.setColor(Color.NAVY);
		world = new World(new Vector2(0, -9.8f), true);
		world.setContactListener(this);
		debugRenderer = new Box2DDebugRenderer();
		batch = new SpriteBatch();
		textureAtlas = new TextureAtlas(Gdx.files.internal("anims/packed.atlas"));
		Texture backgroundImage = new Texture(Gdx.files.internal("grassbcg.png"));
		backGroundSprite = new Sprite(backgroundImage);
		backGroundSprite.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		try {
			myClient = Gdx.net.newClientSocket(Net.Protocol.TCP, HOST, PORT, null);
		} catch (GdxRuntimeException e) {
			Gdx.app.log("error", e.getMessage());
			Gdx.app.exit();
			return;
		}

		me = new MagikarpPlayer(160, 40, world, textureAtlas, Color.RED, "me", myClient);
		opponent = new MagikarpPlayer(450, 40, world, textureAtlas, Color.BLUE, null, null);

		playing.addActor(me);
		playing.addActor(opponent);


		BodyDef groundBodyDef = new BodyDef();
		groundBodyDef.position.set(new Vector2(0, me.getY()-me.getHeight()/2/PX_PER_METER));
		groundBodyDef.type = BodyDef.BodyType.StaticBody;
		Body groundBody = world.createBody(groundBodyDef);
		EdgeShape groundBox = new EdgeShape();
		groundBox.set(0, 0, Gdx.graphics.getWidth() / PX_PER_METER, 0);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = groundBox;
		fixtureDef.filter.categoryBits = BIT_GROUND;
		fixtureDef.filter.maskBits = BIT_MAGIKARP;
		groundBody.createFixture(fixtureDef);
		groundBox.dispose();

		BodyDef counterBarDef = new BodyDef();
		counterBarDef.position.set(0, (Gdx.graphics.getHeight()-40f)/PX_PER_METER);
		counterBarDef.type = BodyDef.BodyType.StaticBody;
		Body counterBarBody = world.createBody(counterBarDef);
		EdgeShape line = new EdgeShape();
		line.set(0, 0, Gdx.graphics.getWidth() / PX_PER_METER, 0);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = line;
		fixtureDef.filter.categoryBits = BIT_COUNTER_BAR;
		fixtureDef.filter.maskBits = BIT_COUNTER;
		counterBarBody.createFixture(fixtureDef);
		line.dispose();

		BodyDef upperBarDef = new BodyDef();
		upperBarDef.position.set(0, (Gdx.graphics.getHeight())/PX_PER_METER);
		upperBarDef.type = BodyDef.BodyType.StaticBody;
		Body upperBarBody = world.createBody(upperBarDef);
		EdgeShape upperBar = new EdgeShape();
		upperBar.set(0, 0, Gdx.graphics.getWidth() / PX_PER_METER, 0);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = upperBar;
		fixtureDef.filter.categoryBits = BIT_COUNTER_UPPER_BAR;
		fixtureDef.filter.maskBits = BIT_COUNTER;
		upperBarBody.createFixture(fixtureDef).setUserData(UPPER_BAR);
		upperBar.dispose();

		Gdx.input.setInputProcessor(stages.get(0));

		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth() / PX_PER_METER, Gdx.graphics.getHeight() / PX_PER_METER);


		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					OutputStream stream = myClient.getOutputStream();
					stream.write(("{ \"action\" : \"" + CLIENTSERVER_ACTION.IP_LIST.getValue() + "\"}").getBytes());
					stream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				InputStream myStream = myClient.getInputStream();
				while (myClient.isConnected()) {
					try {
						byte[] messageBytes = new byte[1024];

						int bytes = myStream.read(messageBytes);

						String jsonFromServer = new String(messageBytes, 0, bytes, "UTF-8");
						Gdx.app.log("Got json from server: ", jsonFromServer);
						JsonValue root = new JsonReader().parse(jsonFromServer);
						final ClientServerObject resp = new ClientServerObject();
						if (root.has("action")) {
							resp.action = root.getString("action");
						}
						if (root.has("from")) {
							resp.from = root.getString("from");
						}
						if (root.has("x")) {
							resp.x = root.getFloat("x");
						}
						if (root.has("y")) {
							resp.y = root.getFloat("y");
						}
						if (root.has("yvel")) {
							resp.yvel = root.getFloat("yvel");
						}
						if (root.has(CLIENTSERVER_ACTION.IP_LIST.getValue())) {
							List<String> ips = new ArrayList<String>();
							JsonValue listOfIps = root.get(CLIENTSERVER_ACTION.IP_LIST.getValue());
							for (int i = 0; i < listOfIps.size; i++) {
								ips.add(listOfIps.getString(i));
							}
							resp.ipList = ips;
						}

						switch (CLIENTSERVER_ACTION.realValueOf(resp.action)) {
							case CHALLENGE:
								setState(GAME_STATE.COUNTDOWN_START);
								opponent.setName(resp.from);
								Timer.schedule(startCountdownTask, 1, 1, time);
								break;
							case IP_LIST:
								Gdx.app.postRunnable(new Runnable() {
									@Override
									public void run() {
										buttons = new ArrayList<TextButton>();
										pane.removeActor(table);
										table = new Table(mySkin);
										table.padTop(20);
										pane.setWidget(table);
										if (myState != GAME_STATE.IP_LIST) {
											if (!resp.ipList.contains(opponent.getName())) {
												FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("arial.ttf"));
												TextButton.TextButtonStyle myStyle = new TextButton.TextButtonStyle(mySkin.getDrawable("default-round"),
														mySkin.getDrawable("default-round-down"), null, generator.generateFont(100));
												generator.dispose();
												table.add(new TextButton(opponent.getName()+" has forfeitted. YOU WON!", myStyle)).padBottom(20).row();
												setState(GAME_STATE.IP_LIST);
											}
										}
										for (String s : resp.ipList) {
											Gdx.app.log("ip - ", s);
											FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("arial.ttf"));
											TextButton.TextButtonStyle myStyle = new TextButton.TextButtonStyle(mySkin.getDrawable("default-round"),
													mySkin.getDrawable("default-round-down"), null, generator.generateFont(50));
											generator.dispose();
											final TextButton tempButton = new TextButton(s, myStyle);
											tempButton.pad(10);
											tempButton.addListener(new ClickListener() {
												@Override
												public void clicked(InputEvent event, float x, float y) {
													String oppip = tempButton.getText().toString();
													if (!oppip.endsWith("ME")) {
														OutputStream out = myClient.getOutputStream();
														try {
															ClientServerObject response = new ClientServerObject();
															response.action = CLIENTSERVER_ACTION.CONNECT.getValue();
															response.iptochallenge = oppip;
															out.write(response.toJson());
															out.flush();
															setState(GAME_STATE.COUNTDOWN_START);
															opponent.setName(oppip);
															Timer.schedule(startCountdownTask, 1, 1, time + 1);
														} catch (IOException e) {
															e.printStackTrace();
														}
													} else {

													}
													super.clicked(event, x, y);
												}
											});
											table.add(tempButton).center().pad(10);
											buttons.add(tempButton);
											table.row();
										}
										table.align(Align.top | Align.center);
									}
								});
								break;
							case FALL_DOWN:
								opponent.fallDown();
								break;
							case JUMP_FAST:
								opponent.fastUp();
								break;
							case JUMP_SLOW:
								opponent.slowUp();
								break;
							case SYNC:
								opponent.sync();
							case OPP_SCORE:
								break;
						}
					} catch (Exception e) {
						Gdx.app.log("got error:", e.getMessage().toString());
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	public void dispose() {
		batch.dispose();
		textureAtlas.dispose();
		world.dispose();
		if (myClient != null) {
			myClient.dispose();
		}
	}

	private void setState(GAME_STATE state) {
		if (myState != state) {
			myState = state;
		}
		switch (myState) {
			case IP_LIST:
				time = 3;
				break;
		}
	}

	@Override
	public void render() {
		if (Gdx.input.isKeyPressed(Input.Keys.DPAD_CENTER)) {
			Gdx.app.log("PRESSED!!!!", "");
		}
		switch (myState) {
			case IP_LIST:
				Gdx.gl.glClearColor(0, 0, 0, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				stages.get(0).draw();
				break;
			case COUNTDOWN_START:
				Gdx.gl.glClearColor(1, 1, 1, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				batch.begin();
				String textToDraw;
				if (time == 0) {
					textToDraw = "GO!";
				}
				else {
					textToDraw = time+"...";
				}
				gameCountdownFont.draw(batch, textToDraw, Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f);
				batch.end();
				break;
			case PLAYING:
				Gdx.gl.glClearColor(1, 1, 1, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				doPhysicsStep(Gdx.graphics.getDeltaTime());
				stages.get(1).draw();
				debugRenderer.render(world, camera.combined);
				break;
		}
	}

	@Override
	public void beginContact(Contact contact) {
		Fixture one = contact.getFixtureA();
		Fixture two = contact.getFixtureB();
		if (one.getUserData() == UPPER_BAR || two.getUserData() == UPPER_BAR) {
			if (one.getUserData() == MECOUNTER || two.getUserData() == MECOUNTER) {
				me.incrementCount();
			}
			else if (one.getUserData() == OPPCOUNTER || two.getUserData() == OPPCOUNTER) {
				opponent.incrementCount();
			}
		}
	}

	@Override
	public void endContact(Contact contact) {

	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {

	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {

	}

	private void doPhysicsStep(float deltaTime) {
		// fixed time step
		// max frame time to avoid spiral of death (on slow devices)
		float frameTime = Math.min(deltaTime, 0.25f);
		accumulator += frameTime;
		while (accumulator >= TIME_STEP) {
			world.step(TIME_STEP, 6, 2);
			accumulator -= TIME_STEP;
		}
	}
}
