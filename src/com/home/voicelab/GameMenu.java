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

import java.util.ArrayList;
import java.util.List;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.ClickDetector;
import org.andengine.input.touch.detector.ClickDetector.IClickDetectorListener;
import org.andengine.input.touch.detector.ScrollDetector;
import org.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener;
import org.andengine.input.touch.detector.SurfaceScrollDetector;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.ui.activity.BaseGameActivity;

import android.graphics.Color;
import android.util.DisplayMetrics;

public class GameMenu extends BaseGameActivity implements IScrollDetectorListener, IOnSceneTouchListener, IClickDetectorListener {

    protected static int FONT_SIZE = 24;
    protected static int PADDING = 50; 
    protected static int MENUITEMS = 3;

	protected int CAMERA_HEIGHT;
	protected int CAMERA_WIDTH;
	protected Camera mCamera;
	private BitmapTextureAtlas mFontTexture;
	private Font mFont;
    private List<ITextureRegion> columns = new ArrayList<ITextureRegion>();
	private BitmapTextureAtlas mMenuTextureAtlas;
	private ITextureRegion mMenuLeftTextureRegion;
	private ITextureRegion mMenuRightTextureRegion;
	private Scene mScene;
	private SurfaceScrollDetector mScrollDetector;
	private ClickDetector mClickDetector;
    private float mMinX = 0;
    private float mMaxX = 0;
    private float mCurrentX = 0;
    private int iItemClicked = -1;
	private Rectangle scrollBar;
	private Sprite menuleft;
	private Sprite menuright;

	@Override
	public EngineOptions onCreateEngineOptions() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		CAMERA_HEIGHT = metrics.heightPixels;
		CAMERA_WIDTH = metrics.widthPixels;
		
        this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);      
        final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new FillResolutionPolicy(), this.mCamera);
        return engineOptions;
	}

	@Override
	public void onCreateResources(
			OnCreateResourcesCallback pOnCreateResourcesCallback)
			throws Exception {
		// Paths
        FontFactory.setAssetBasePath("font/");
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        // Font
        this.mFontTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 256);
        this.mFont = FontFactory.createFromAsset(this.getFontManager(), mFontTexture, getAssets(), "Plok.TTF", FONT_SIZE, true, Color.BLACK);
        this.mEngine.getTextureManager().loadTexture(this.mFontTexture);
        this.mEngine.getFontManager().loadFont(this.mFont);
        
        //Images for the menu
        for (int i = 0; i < MENUITEMS; i++) {				
        	BitmapTextureAtlas mMenuBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 256,256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
    		ITextureRegion mMenuTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mMenuBitmapTextureAtlas, this, "menu"+i+".png", 0, 0);
        	
        	this.mEngine.getTextureManager().loadTexture(mMenuBitmapTextureAtlas);
        	columns.add(mMenuTextureRegion);
        	
        	
        }
        //Textures for menu arrows
        this.mMenuTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 128,128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        this.mMenuLeftTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mMenuTextureAtlas, this, "menu_left.png", 0, 0);
        this.mMenuRightTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mMenuTextureAtlas, this, "menu_right.png",64, 0);
        this.mEngine.getTextureManager().loadTexture(mMenuTextureAtlas);
		
	}

	@Override
	public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback)
			throws Exception {
        this.mEngine.registerUpdateHandler(new FPSLogger());
        
        this.mScene = new Scene();
        this.mScene.setBackground(new Background(0, 0, 0));
       
        this.mScrollDetector = new SurfaceScrollDetector(this);
        this.mClickDetector = new ClickDetector(this);

        this.mScene.setOnSceneTouchListener(this);
        //this.mScene.setTouchAreaBindingEnabled(true);
        //this.mScene.setOnSceneTouchListenerBindingEnabled(true);

        CreateMenuBoxes();

        //return this.mScene;
		
	}

	private void CreateMenuBoxes() {
		int spriteX = PADDING;
		int spriteY = PADDING;

		// current item counter
		int iItem = 1;

		for (int x = 0; x < columns.size(); x++) {

			// On Touch, save the clicked item in case it's a click and not a
			// scroll.
			final int itemToLoad = iItem;

			Sprite sprite = new Sprite(spriteX, spriteY, columns.get(x), this.getVertexBufferObjectManager()) {

				public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
						final float pTouchAreaLocalX,
						final float pTouchAreaLocalY) {
					iItemClicked = itemToLoad;
					return false;
				}
			};
			iItem++;

			this.mScene.attachChild(sprite);
			this.mScene.registerTouchArea(sprite);

			spriteX += 20 + PADDING + sprite.getWidth();
		}

		mMaxX = spriteX - CAMERA_WIDTH;

		// set the size of the scrollbar
		float scrollbarsize = CAMERA_WIDTH / ((mMaxX + CAMERA_WIDTH) / CAMERA_WIDTH);
		scrollBar = new Rectangle(0, CAMERA_HEIGHT - 20, scrollbarsize, 20, this.getVertexBufferObjectManager());
		scrollBar.setColor(1, 0, 0);
		this.mScene.attachChild(scrollBar);

		menuleft = new Sprite(0, CAMERA_HEIGHT / 2 - mMenuLeftTextureRegion.getHeight() / 2, mMenuLeftTextureRegion, this.getVertexBufferObjectManager());
		menuright = new Sprite(CAMERA_WIDTH - mMenuRightTextureRegion.getWidth(), CAMERA_HEIGHT / 2 - mMenuRightTextureRegion.getHeight() / 2, mMenuRightTextureRegion, this.getVertexBufferObjectManager());
		this.mScene.attachChild(menuright);
		menuleft.setVisible(false);
		this.mScene.attachChild(menuleft);

	}

	@Override
	public void onPopulateScene(Scene pScene,
			OnPopulateSceneCallback pOnPopulateSceneCallback) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(ClickDetector pClickDetector, int pPointerID,
			float pSceneX, float pSceneY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
        this.mClickDetector.onTouchEvent(pSceneTouchEvent);
        this.mScrollDetector.onTouchEvent(pSceneTouchEvent);
        return true;
	}

	@Override
	public void onScrollStarted(ScrollDetector pScollDetector, int pPointerID,
			float pDistanceX, float pDistanceY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onScroll(ScrollDetector pScollDetector, int pPointerID,float pDistanceX, float pDistanceY) {
		//Disable the menu arrows left and right (15px padding)
    	if(mCamera.getXMin()<=15)
         	menuleft.setVisible(false);
         else
         	menuleft.setVisible(true);
    	 
    	 if(mCamera.getXMin()>mMaxX-15)
             menuright.setVisible(false);
         else
        	 menuright.setVisible(true);
         	
        //Return if ends are reached
        if ( ((mCurrentX - pDistanceX) < mMinX)  ){                	
            return;
        }else if((mCurrentX - pDistanceX) > mMaxX){
        	
        	return;
        }
        
        //Center camera to the current point
        this.mCamera.offsetCenter(-pDistanceX,0 );
        mCurrentX -= pDistanceX;
        	
       
        //Set the scrollbar with the camera
        float tempX =mCamera.getCenterX()-CAMERA_WIDTH/2;
        // add the % part to the position
        tempX+= (tempX/(mMaxX+CAMERA_WIDTH))*CAMERA_WIDTH;      
        //set the position
        scrollBar.setPosition(tempX, scrollBar.getY());
        
        //set the arrows for left and right
        menuright.setPosition(mCamera.getCenterX()+CAMERA_WIDTH/2-menuright.getWidth(),menuright.getY());
        menuleft.setPosition(mCamera.getCenterX()-CAMERA_WIDTH/2,menuleft.getY());
        
      
        
        //Because Camera can have negative X values, so set to 0
    	if(this.mCamera.getXMin()<0){
    		this.mCamera.offsetCenter(0,0 );
    		mCurrentX=0;
    	}
		
	}

	@Override
	public void onScrollFinished(ScrollDetector pScollDetector, int pPointerID,
			float pDistanceX, float pDistanceY) {
		// TODO Auto-generated method stub
		
	}

}
