/*
 *Copyright 2011 Norbert Pabian www.npsoftware.pl www.npsoft.clanteam.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
 package com.home.voicelab;

import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl.IOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.DigitalOnScreenControl;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.shape.IAreaShape;
import org.andengine.entity.shape.Shape;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.FixedStepPhysicsWorld;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.extension.tmx.TMXLayer;
import org.andengine.extension.tmx.TMXLoader;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.TMXObject;
import org.andengine.extension.tmx.TMXObjectGroup;
import org.andengine.extension.tmx.TMXProperties;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.extension.tmx.TMXTileProperty;
import org.andengine.extension.tmx.TMXTiledMap;
import org.andengine.extension.tmx.util.exception.TMXLoadException;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.Constants;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class VoiceLabitynth extends SimpleBaseGameActivity implements
		RecognitionListener {

	private final static int PIXEL_TO_METER_RATIO_DEFAULT = 32;
	private int CAMERA_WIDTH;
	private int CAMERA_HEIGHT;
	private static final String TAG = "SPEECH_RECOGNIZER";

	private Scene mScene;
	private BitmapTextureAtlas mBitmapTextureAtlas;
	private TiledTextureRegion mPlayerTextureRegion;
	private TMXTiledMap mTMXTiledMap;
	private TMXLayer tmxLayer;

	private AnimatedSprite player;
	private Body mPlayerBody;
	private BitmapTextureAtlas mOnScreenControlTexture;
	private ITextureRegion mOnScreenControlBaseTextureRegion;
	private ITextureRegion mOnScreenControlKnobTextureRegion;
	private DigitalOnScreenControl mDigitalOnScreenControl;
	private PhysicsWorld mPhysicsWorld;

	private static final long[] ANIMATE_DURATION = new long[] { 200, 200, 200 };
	private static final int PLAYER_VELOCITY = 50;

	private enum PlayerDirection {
		NONE, UP, DOWN, LEFT, RIGHT
	}

	private PlayerDirection playerDirection = PlayerDirection.NONE;

	private int startPointX;
	private int startPointY;
	private int lastCheckTileRow = 1;
	private int lastCheckTileColumn = 1;
	private SpeechRecognizer recognizer;
	private Rectangle alertRectangle;
	private Font font;
	private Text alertText;
	private float alertTextSize;
	private boolean voiceRecognitionBlocked = false;  // Change to true if you want use voice recognition
	private boolean gameEnd = false;
	protected Rectangle finishRectangle;
	protected Text finishText;
	private Rectangle helpRectangle;
	private Font fontH;
	private Text helpText;
	private boolean helpVisible = true;
	private BoundCamera mBoundChaseCamera;

	@Override
	public EngineOptions onCreateEngineOptions() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		CAMERA_HEIGHT = metrics.heightPixels;
		CAMERA_WIDTH = metrics.widthPixels;		
		this.mBoundChaseCamera = new BoundCamera(0, 0, CAMERA_WIDTH,CAMERA_HEIGHT);
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mBoundChaseCamera);			
	}

	@Override
	public void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		this.mOnScreenControlTexture = new BitmapTextureAtlas(
				this.getTextureManager(), 256, 128,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mOnScreenControlTexture, this,
						"onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mOnScreenControlTexture, this,
						"onscreen_control_knob.png", 128, 0);

		this.mBitmapTextureAtlas = new BitmapTextureAtlas(
				this.getTextureManager(), 128, 128, TextureOptions.DEFAULT);
		this.mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(this.mBitmapTextureAtlas, this,
						"hero.png", 0, 0, 3, 4);

		this.mOnScreenControlTexture.load();
		this.mBitmapTextureAtlas.load();
	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		// Create physics world
		this.mPhysicsWorld = new FixedStepPhysicsWorld(30, new Vector2(0, 0),true, 8, 1);

		mScene = new Scene();
		mScene.registerUpdateHandler(this.mPhysicsWorld);

		// Load map from TMX file.
		try {
			final TMXLoader tmxLoader = new TMXLoader(this.getAssets(), this.mEngine.getTextureManager(),
					TextureOptions.BILINEAR_PREMULTIPLYALPHA /* NEAREST */, this.getVertexBufferObjectManager(),
					new ITMXTilePropertiesListener() {
						@Override
						public void onTMXTileWithPropertiesCreated(final TMXTiledMap pTMXTiledMap, final TMXLayer pTMXLayer,
								final TMXTile pTMXTile, final TMXProperties<TMXTileProperty> pTMXTileProperties) {
							
							if (pTMXTileProperties.containsTMXProperty("startPoint", "true")) {
								startPointX = pTMXTile.getTileX();
								startPointY = pTMXTile.getTileY();
							}
						}
					});
			this.mTMXTiledMap = tmxLoader.loadFromAsset("tmx/labirynth.tmx");

		} catch (final TMXLoadException e) {
			Log.d("TMXLoader", "Problem with TMX loader");
		}

		// Add the non-object layers to the scene
		for (int i = 0; i < this.mTMXTiledMap.getTMXLayers().size(); i++) {
			tmxLayer = this.mTMXTiledMap.getTMXLayers().get(i);
			if (!tmxLayer.getTMXLayerProperties().containsTMXProperty("wall","true"))
				mScene.attachChild(tmxLayer);
		}

		// Make the camera not exceed the bounds of the TMXEntity.
		this.mBoundChaseCamera.setBounds(0, 0, tmxLayer.getWidth(),
				tmxLayer.getHeight());
		//this.mBoundChaseCamera.setBounds(0, 0, tmxLayer.getWidth(),
		//		tmxLayer.getHeight());
		this.mBoundChaseCamera.setBoundsEnabled(true);
		//this.mBoundChaseCamera.setBoundsEnabled(true);

		// Read in the unwalkable blocks from the object layer and create boxes for each
		this.createUnwalkableObjects(mTMXTiledMap);

		// Add outer walls
		this.addBounds(tmxLayer.getWidth(), tmxLayer.getHeight());

		// Create the player sprite and add it to the scene.
		player = new AnimatedSprite(startPointX, startPointY, this.mPlayerTextureRegion, this.getVertexBufferObjectManager());
		player.setScale(0.9f);

		//this.mBoundChaseCamera.setChaseEntity(player);
		final FixtureDef playerFixtureDef = PhysicsFactory.createFixtureDef(0, 0, 0);
		mPlayerBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, player, BodyType.DynamicBody, playerFixtureDef);

		mScene.registerUpdateHandler(mPhysicsWorld);

		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(player, mPlayerBody, true, false) {
			@Override
			public void onUpdate(float pSecondsElapsed) {
				super.onUpdate(pSecondsElapsed);
				mBoundChaseCamera.updateChaseEntity();
				//mBoundChaseCamera.updateChaseEntity();
			}
		});

		// Create a rectangle that will always highlight the tile below the feet of the pEntity.
		final Rectangle currentTileRectangle = new Rectangle(0, 0, this.mTMXTiledMap.getTileWidth(),
				this.mTMXTiledMap.getTileHeight(), this.getVertexBufferObjectManager());
		currentTileRectangle.setColor(1, 0, 0, 0.15f);
		mScene.attachChild(currentTileRectangle);		

		// Add the digital control
		initDigitalControl();

		mScene.registerUpdateHandler(new IUpdateHandler() {		

			@Override
			public void reset() {
			}

			@Override
			public void onUpdate(final float pSecondsElapsed) {

				/* Get the scene-coordinates of the players feet. */
//				final float[] playerFootCordinates = player
//						.convertLocalToSceneCoordinates(0,0);		
//				/* Get the tile the feet of the player are currently waking on. */
//				final TMXTile tmxTile = tmxLayer.getTMXTileAt(
//						playerFootCordinates[Constants.VERTEX_INDEX_X],
//						playerFootCordinates[Constants.VERTEX_INDEX_Y]);
				final TMXTile tmxTile = tmxLayer.getTMXTileAt(player.getX() + player.getWidth()/2,player.getY()+ player.getHeight()/2);

				if (tmxTile != null) {
					currentTileRectangle.setPosition(tmxTile.getTileX(),tmxTile.getTileY());
					if (tmxTile.getTileRow() != lastCheckTileRow || tmxTile.getTileColumn() != lastCheckTileColumn) {
							if (tmxTile.getTMXTileProperties(mTMXTiledMap) != null) {
								if (tmxTile.getTMXTileProperties(mTMXTiledMap).containsTMXProperty("checkPoint","true")) {									
									mPlayerBody.setLinearVelocity(0, 0);
									final float widthD2 = player.getWidth() / 2;
									final float heightD2 = player.getHeight() / 2;
									final float angle = mPlayerBody.getAngle(); // keeps the body angle
									final Vector2 v2 = Vector2Pool.obtain((tmxTile.getTileX() + widthD2) / PIXEL_TO_METER_RATIO_DEFAULT, (tmxTile.getTileY() + heightD2) / PIXEL_TO_METER_RATIO_DEFAULT);
									mPlayerBody.setTransform(v2, angle);
									Vector2Pool.recycle(v2);
									
									lastCheckTileRow = tmxTile.getTileRow();
									lastCheckTileColumn = tmxTile.getTileColumn();
									if (player.isAnimationRunning()) {
										player.stopAnimation();
										playerDirection = PlayerDirection.NONE;
									}
								}
								if (tmxTile.getTMXTileProperties(mTMXTiledMap).containsTMXProperty("endPoint", "true")) {
									mBoundChaseCamera.setChaseEntity(finishRectangle);
									finishRectangle.setSize(CAMERA_WIDTH, CAMERA_HEIGHT);
									finishRectangle.setVisible(true);
									finishText.setPosition(CAMERA_WIDTH/2 - alertTextSize/2, CAMERA_HEIGHT/4);
									finishText.setVisible(true);
									gameEnd = true;
								}
							
						}
					} else {
						lastCheckTileRow = tmxTile.getTileRow();
						lastCheckTileColumn = tmxTile.getTileColumn();
					}

				}
			}
		});
		
		player.registerUpdateHandler(new PhysicsHandler(player));
		mScene.attachChild(player);		
		
		// create alert rectangle that will show during recognizer work.
		alertRectangle = new Rectangle(0, 0, this.CAMERA_WIDTH, this.CAMERA_HEIGHT, this.getVertexBufferObjectManager());
		alertRectangle.setColor(1, 0, 0, 0.5f);
		alertRectangle.setVisible(false);
		mScene.attachChild(alertRectangle);	
		font = FontFactory.create(getFontManager(), getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 32, Color.argb(128, 255, 255, 255));
		font.load();	
		alertText = new Text(0, 0, font, "Recognition", getVertexBufferObjectManager());
		//alertText.setHorizontalAlign(HorizontalAlign.CENTER);
		alertText.setVisible(false);
		mScene.attachChild(alertText);
		
		helpRectangle = new Rectangle(0, 0, this.CAMERA_WIDTH, this.CAMERA_HEIGHT, this.getVertexBufferObjectManager());
		helpRectangle.setColor(0, 0, 0, 0.8f);
		helpRectangle.setVisible(true);
		mScene.attachChild(helpRectangle);	
		fontH = FontFactory.create(getFontManager(), getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), 13, Color.argb(255, 255, 255, 255));
		fontH.load();	
		helpText = new Text(0, 0, fontH, "Komendy" + '\n' +'\n' + "lewo, prao, góra, dó³ - Sterowanie postaci¹" + '\n' + 
				"pomoc - Wyœwietla tekst z pomoc¹" +'\n' + "mapa - Podgl¹d mapy" +'\n' + "cofnij - Zamuka pomoc lub podgl¹d mapy"  +'\n' + "zakoñcz - Wychodzi z gry" +'\n' +
				"rozpocznij - Rozpoczyna now¹ grê po przejœciu mapy"  +'\n'  +'\n'  +"System rozpoznaje równie¿ powy¿sze komendy" + '\n' + "w jêzyku angielskim.", getVertexBufferObjectManager());
		//alertText.setHorizontalAlign(HorizontalAlign.CENTER);
		helpText.setVisible(true);
		mScene.attachChild(helpText);		
		
		this.mBoundChaseCamera.setChaseEntity(helpRectangle);
		if(!helpVisible){
			helpRectangle.setVisible(false);
			helpText.setVisible(false);
			this.mBoundChaseCamera.setChaseEntity(player);
		}

		final float densityMultiplier = getBaseContext().getResources().getDisplayMetrics().density;
		final float scaledPx = 32 * densityMultiplier;
		Paint paint = new Paint();
		paint.setTextSize(scaledPx);
		alertTextSize = paint.measureText(" Recognition ");	
		
		
		finishRectangle = new Rectangle(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT, getVertexBufferObjectManager());
		finishRectangle.setColor(0, 0, 1, 0.5f);
		finishRectangle.setVisible(false);
		mScene.attachChild(finishRectangle);
		finishText = new Text(0, 0, font, "Success!", getVertexBufferObjectManager());
		finishText.setPosition(CAMERA_WIDTH/2 - alertTextSize/2, CAMERA_HEIGHT/4);
		finishText.setVisible(false);
		mScene.attachChild(finishText);	
		
		return mScene;
	}

	private void initDigitalControl() {

		this.mDigitalOnScreenControl = new DigitalOnScreenControl(CAMERA_WIDTH - this.mOnScreenControlBaseTextureRegion.getWidth(),
				CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight(), this.mBoundChaseCamera, 
				this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f,this.getVertexBufferObjectManager(),
				new IOnScreenControlListener() {

					@Override
					public void onControlChange(
							final BaseOnScreenControl pBaseOnScreenControl,
							final float pValueX, final float pValueY) {

						evaluateDirection(pValueX, pValueY);
					}
				});
		
		this.mDigitalOnScreenControl.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		this.mDigitalOnScreenControl.getControlBase().setAlpha(0.5f);
		this.mDigitalOnScreenControl.getControlBase().setScaleCenter(0, 0);
		this.mDigitalOnScreenControl.getControlBase().setScale(1.0f);
		this.mDigitalOnScreenControl.getControlKnob().setScale(1.0f);
		this.mDigitalOnScreenControl.getControlKnob().setAlpha(0.5f);
		this.mDigitalOnScreenControl.refreshControlKnobPosition();

		mScene.setChildScene(this.mDigitalOnScreenControl);
	}

	protected void evaluateDirection(float pValueX, float pValueY) {
		if (pValueY == 1) {
			// Up
			if (playerDirection != PlayerDirection.UP) {
				player.animate(ANIMATE_DURATION, 0, 2, true);
				playerDirection = PlayerDirection.UP;
				mPlayerBody.setLinearVelocity(0, 0);
				mPlayerBody.applyForce(new Vector2(pValueX * PLAYER_VELOCITY, pValueY * PLAYER_VELOCITY), mPlayerBody.getWorldCenter());
			}
		} else if (pValueY == -1) {
			// Down
			if (playerDirection != PlayerDirection.DOWN) {
				player.animate(ANIMATE_DURATION, 9, 11, true);
				playerDirection = PlayerDirection.DOWN;
				mPlayerBody.setLinearVelocity(0, 0);
				mPlayerBody.applyForce(new Vector2(pValueX * PLAYER_VELOCITY, pValueY * PLAYER_VELOCITY), mPlayerBody.getWorldCenter());
			}
		} else if (pValueX == -1) {
			// Left
			if (playerDirection != PlayerDirection.LEFT) {
				player.animate(ANIMATE_DURATION, 3, 5, true);
				playerDirection = PlayerDirection.LEFT;
				mPlayerBody.setLinearVelocity(0, 0);
				mPlayerBody.applyForce(new Vector2(pValueX * PLAYER_VELOCITY, pValueY * PLAYER_VELOCITY), mPlayerBody.getWorldCenter());
			}
		} else if (pValueX == 1) {
			// Right
			if (playerDirection != PlayerDirection.RIGHT) {
				player.animate(ANIMATE_DURATION, 6, 8, true);
				playerDirection = PlayerDirection.RIGHT;
				mPlayerBody.setLinearVelocity(0, 0);
				mPlayerBody.applyForce(new Vector2(pValueX * PLAYER_VELOCITY, pValueY * PLAYER_VELOCITY), mPlayerBody.getWorldCenter());

			}
		}

	}

	private void createUnwalkableObjects(TMXTiledMap map) {
		// Loop through the object groups
		for (final TMXObjectGroup group : this.mTMXTiledMap
				.getTMXObjectGroups()) {
			if (group.getTMXObjectGroupProperties().containsTMXProperty("wall",
					"true")) {
				// This is our "wall" layer. Create the boxes from it
				for (final TMXObject object : group.getTMXObjects()) {
					final Rectangle rect = new Rectangle(object.getX(),
							object.getY(), object.getWidth(),
							object.getHeight(),
							this.getVertexBufferObjectManager());
					final FixtureDef boxFixtureDef = PhysicsFactory
							.createFixtureDef(0, 0, 1f);
					PhysicsFactory.createBoxBody(this.mPhysicsWorld, rect,
							BodyType.StaticBody, boxFixtureDef);
					rect.setVisible(false);
					mScene.attachChild(rect);
				}
			}
		}
	}

	private void addBounds(float width, float height) {
		final Shape bottom = new Rectangle(0, height - 2, width, 2, this.getVertexBufferObjectManager());
		bottom.setVisible(false);
		final Shape top = new Rectangle(0, 0, width, 2, this.getVertexBufferObjectManager());
		top.setVisible(false);
		final Shape left = new Rectangle(0, 0, 2, height, this.getVertexBufferObjectManager());
		left.setVisible(false);
		final Shape right = new Rectangle(width - 2, 0, 2, height,this.getVertexBufferObjectManager());
		right.setVisible(false);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0,1f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, (IAreaShape) bottom, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, (IAreaShape) top, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, (IAreaShape) left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, (IAreaShape) right, BodyType.StaticBody, wallFixtureDef);

		this.mScene.attachChild(bottom);
		this.mScene.attachChild(top);
		this.mScene.attachChild(left);
		this.mScene.attachChild(right);
	}
	
	public void onCreate(Bundle pSavedInstanceState){
		super.onCreate(pSavedInstanceState);
		
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() == 0) {
			Toast.makeText(getApplicationContext(), "Recognizer not present",Toast.LENGTH_LONG);
		}
		Log.d(TAG, "Recognizer present");
	}

	
	public void onResume(){
		if (!voiceRecognitionBlocked) {
			startVoiceRecognitionActivity();
			Log.d(TAG, "Recognizer started");
		}
		super.onResume();
	}

	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		recognizeDirectly(intent);
	}

	public void recognizeDirectly(Intent recognizerIntent) {
		// SpeechRecognizer requires EXTRA_CALLING_PACKAGE, so add if it's not
		// here
		if (!recognizerIntent.hasExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE)) {
			recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"com.dummy");
		}
		SpeechRecognizer recognizer = getSpeechRecognizer();
		recognizer.startListening(recognizerIntent);
	}

	private SpeechRecognizer getSpeechRecognizer() {
		if (recognizer == null) {
			recognizer = SpeechRecognizer.createSpeechRecognizer(this);
			recognizer.setRecognitionListener(this);
		}
		return recognizer;
	}

	private void receiveResults(Bundle results) {
		if ((results != null) && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
			List<String> heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

			Log.d(TAG, heard.toString());

			if (receiveWhatWasHeard(heard)) {
				if (!voiceRecognitionBlocked) {
					startVoiceRecognitionActivity();
				}
			} else {
				destroyRecognizer();
				this.finish();
			}
		} else {
			Log.d(TAG, "Receive result null");
			if (!voiceRecognitionBlocked) {
				startVoiceRecognitionActivity();
			}
		}
	}

	private boolean receiveWhatWasHeard(List<String> heard) {

		if (heard.contains("zakoñcz") || heard.contains("wyjdŸ") || heard.contains("close") || heard.contains("exit")) {
			return false;
		}
		else if(heard.contains("prawo") || heard.contains("right")){
			evaluateDirection(1, 0);
			return true;
		}
		else if(heard.contains("lewo") || heard.contains("left")){
			evaluateDirection(-1, 0);
			return true;
		}
		else if(heard.contains("góra") || heard.contains("up")){
			evaluateDirection(0, -1);
			return true;
		}
		else if(heard.contains("dó³") || heard.contains("down")){
			evaluateDirection(0, 1);
			return true;
		}
		else if(heard.contains("mapa") ||  heard.contains("map")){
			mDigitalOnScreenControl.setVisible(false);
			mBoundChaseCamera.set(0, 0,  tmxLayer.getWidth(),  tmxLayer.getHeight());
			return true;
		}		
		else if(heard.contains("cofnij") || heard.contains("back")){
			if(helpVisible){
				this.mBoundChaseCamera.setChaseEntity(player);
				this.helpRectangle.setVisible(false);
				this.helpText.setVisible(false);
				this.helpVisible = false;
				return true;
			}
			mBoundChaseCamera.set(0, 0,  CAMERA_WIDTH,  CAMERA_HEIGHT);
			mDigitalOnScreenControl.setVisible(true);
			return true;
		}
		else if(((heard.contains("rozpocznij") || heard.contains("begin")) )&& this.gameEnd){
			this.finishRectangle.setVisible(false);
			this.finishText.setVisible(false);
			final float widthD2 = player.getWidth() / 2;
			final float heightD2 = player.getHeight() / 2;
			final float angle = mPlayerBody.getAngle(); // keeps the body angle
			final Vector2 v2 = Vector2Pool.obtain((startPointX + widthD2) / PIXEL_TO_METER_RATIO_DEFAULT, (startPointY + heightD2) / PIXEL_TO_METER_RATIO_DEFAULT);
			mPlayerBody.setTransform(v2, angle);
			Vector2Pool.recycle(v2);
			this.mBoundChaseCamera.setChaseEntity(player);
			return true;
		}
		else if(heard.contains("pomoc") || heard.contains("help")){
			this.mBoundChaseCamera.setChaseEntity(helpRectangle);
			this.helpRectangle.setSize(CAMERA_WIDTH, CAMERA_HEIGHT);
			this.helpRectangle.setVisible(true);
			this.helpText.setPosition(1,1);
			this.helpText.setVisible(true);
			helpVisible = true;
			return true;
		}
		else if(heard.contains("rozpocznij") && helpVisible == true){
			if(helpVisible){
				this.mBoundChaseCamera.setChaseEntity(player);
				this.helpRectangle.setVisible(false);
				this.helpText.setVisible(false);
				this.helpVisible = false;
			}
			return true;
		}
		return true;
	}

	@Override
	public void onBeginningOfSpeech() {
		Log.d(TAG, "Begin of speech");
	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEndOfSpeech() {
		Log.d(TAG, "End speech");
		if (alertRectangle != null) {
			this.mBoundChaseCamera.setChaseEntity(alertRectangle);
			alertRectangle.setSize(CAMERA_WIDTH, CAMERA_HEIGHT);
			alertRectangle.setVisible(true);
			alertText.setPosition(CAMERA_WIDTH/2 - alertTextSize/2, CAMERA_HEIGHT/4);
			alertText.setVisible(true);
		}
	}

	@Override
	public void onError(int error) {
		Log.d(TAG, "speech recognition error: " + error);
		recognitionFailure(error);
	}

	private void recognitionFailure(int error) {
		switch (error) {

		case 1:
			Log.d(TAG, "ERROR_NETWORK_TIMEOUT");
			break;
		case 2:
			Log.d(TAG, "ERROR_NETWORK");
			break;
		case 3:
			Log.d(TAG, "ERROR_AUDIO");
			break;
		case 4:
			Log.d(TAG, "ERROR_SERVER");
			break;
		case 5:
			Log.d(TAG, "ERROR_CLIENT");
			break;
		case 6:
			Log.d(TAG, "ERROR_SPEECH_TIMEOUT");
			if (!voiceRecognitionBlocked) {
				startVoiceRecognitionActivity();
			}
			break;
		case 7:
			Log.d(TAG, "ERROR_NO_MATCH");
			if (!voiceRecognitionBlocked) {
				startVoiceRecognitionActivity();
			}
			break;
		case 8:
			Log.d(TAG, "ERROR_RECOGNIZER_BUSY");
			break;
		case 9:
			Log.d(TAG, "ERROR_INSUFFICIENT_PERMISSIONS");
			break;
		}
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		Log.d(TAG, "Partial results");
		receiveResults(partialResults);
	}

	@Override
	public void onReadyForSpeech(Bundle params) {
		Log.d(TAG, "Ready for speech " + params);
		if(alertRectangle != null){
			alertRectangle.setVisible(false);
			alertText.setVisible(false);
			if(!helpVisible)
				mBoundChaseCamera.setChaseEntity(player);
		}
	}

	@Override
	public void onResults(Bundle results) {
		Log.d(TAG, "Full results");
		receiveResults(results);
	}

	@Override
	public void onRmsChanged(float rmsdB) {
		// Log.d(TAG, "RMS Changed " + rmsdB);
	}

	public void onPause() {
		destroyRecognizer();
		super.onPause();
	}
	
	public void destroyRecognizer(){
		if (getSpeechRecognizer() != null) {
			getSpeechRecognizer().stopListening();
			getSpeechRecognizer().cancel();
			getSpeechRecognizer().destroy();
		}
	}

}
