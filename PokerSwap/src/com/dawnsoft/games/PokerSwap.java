package com.dawnsoft.games;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.FontFactory;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.HorizontalAlign;

import android.graphics.Color;
import android.util.Log;

import com.dawnsoft.games.Card;
import com.dawnsoft.games.IAnimFinishedListener.AnimType;

// Poker Swing
// Idee : En réseau sur le meme tapis !

/*
 * 
 * La Quinte Royale : suite de même couleur commençant obligatoirement par un as (Ex : As, R,D,V,10, de même couleur)

La Quinte flush : Suite quelconque de cinq cartes de même couleur. (Ex : 8, 9, 10, V, D, de même couleur).

Le Carré : Quatre cartes identiques. (Ex : 4 Dames)

Le Full : Combinaison du brelan et d'une paire (Ex : 3 As et une paire de 9).

La Couleur : Cinq cartes quelconques de même couleur.(Ex : 2, 6, 9, V et roi de coeur)

La Suite : Cinq cartes consécutives, mais pas de même couleur.(Ex : 3, 4,5, 6, 7, de couleur différentes)

Le Brelan : Trois cartes identiques.(Ex : 6 de trèfle, 6 de pique et 6 de coeur)

La Double paire : Deux paires distinctes (Ex : deux 10 et deux dames).

Paire : Deux cartes de identiques (valeur).


Des clones ne sont pas des paires ( même valeur + même couleur)

On peut faire un jeux en interdisant deux fois la même carte sur le tapis (voir si c'est jouable), dans ce cas pas de clone

 */

public class PokerSwap extends BaseGameActivity implements IOnSceneTouchListener, IAnimFinishedListener {
	
	public enum SlideDirection {
		UP,
		RIGHT,
		DOWN,
		LEFT,
		TOO_SMALL_X,
		TOO_SMALL_Y,
		UNDEFINED
	}
	
	public enum CardProperty {
		CLONE,
		VALUE,
		COLOR
	}

	public static final int CAMERA_WIDTH = 320;// ; //720
	public static final int CAMERA_HEIGHT = 480;// ; //480
	
	public static final int GRID_ROWS = 8;
	public static final int GRID_COLS = 8;
	public static final int GRID_SIZE = GRID_ROWS*GRID_COLS;
	
	private static final int LAYER_BACKGROUND = 0; // must be first
	private static final int LAYER_CARD = LAYER_BACKGROUND + 1;	
	private static final int LAYER_SCORE = LAYER_CARD + 1;
	private static final int LAYER_COUNT = LAYER_SCORE +1; // must be last
	
	public static CardSprite[] mGrid = new CardSprite[GRID_SIZE];
	public static Stack<Integer> UpdCardStack = new Stack<Integer>();
	
	public Stack<Integer> mCardToRemoveStack = new Stack<Integer>();
	//public static Stack<Integer> mCardToFallStack = new Stack<Integer>();
	public static Stack<CardSprite> mCardToDestroyStack = new Stack<CardSprite>();
	public static Stack<CardSprite> mCardMovingOutStack = new Stack<CardSprite>();
	public static Stack<Integer> mSwappedCardStack = new Stack<Integer>();
	
	public static boolean mInTimerCallBack = false;
	
	private Camera mCamera;	
	private Texture mFontTexture;
	private Font mFont;
	private ChangeableText mDebugText[] = new ChangeableText[4];
	private Texture mCardDeckTexture;
	private HashMap<Card, TextureRegion> mCardTotextureRegionMap;
	private List<Card> deck = null;
	private CardSprite mSelectedCard = null;
	private float mSelectedCardOriginalPosition[] = new float[2];
	private float mTouchDownPosition[] = new float[2];	

	private Texture mBackgroundTexture;
	private TextureRegion mBackgroundTextureRegion;
	
	private Boolean mDrawDebugCardValue = true;

	@Override
	public void onLoadComplete() {
	}

	@Override
	public Engine onLoadEngine() {
		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new Engine(new EngineOptions(true, ScreenOrientation.PORTRAIT,
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT),
				this.mCamera));
	}
	

	@Override
	public void onLoadResources() {
		FontFactory.setAssetBasePath("font/");
		this.mFontTexture = new Texture(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.mFont = FontFactory.createFromAsset(this.mFontTexture, this, "Droid.ttf", 20, true, Color.BLACK);
		this.mEngine.getTextureManager().loadTexture(this.mFontTexture);
		this.mEngine.getFontManager().loadFont(this.mFont);
		
		TextureRegionFactory.setAssetBasePath("gfx/");
		// Multiple de 2 !!!
		this.mBackgroundTexture = new Texture(512, 512, TextureOptions.DEFAULT);
		this.mBackgroundTextureRegion = TextureRegionFactory.createFromAsset(
				this.mBackgroundTexture, this, "bigtapis.png", 0, 0);

		this.mCardDeckTexture = new Texture(512,256,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		TextureRegionFactory.createFromAsset(this.mCardDeckTexture, this,
				"fulldeck.png", 0, 0);

		this.mCardTotextureRegionMap = new HashMap<Card, TextureRegion>();

		for (final Card card : Card.values()) {
			final TextureRegion cardTextureRegion = TextureRegionFactory
					.extractFromTexture(this.mCardDeckTexture, card
							.getTexturePositionX(), card.getTexturePositionY(),
							Card.CARD_WIDTH, Card.CARD_HEIGHT);
			this.mCardTotextureRegionMap.put(card, cardTextureRegion);
		}
		this.mEngine.getTextureManager().loadTextures(this.mCardDeckTexture, mBackgroundTexture);
	}

	public Scene onLoadScene() {
		final Scene scene = new Scene(LAYER_COUNT);
		
		for (int t=0;t<mDebugText.length;t++)
		{			
			this.mDebugText[t] = new ChangeableText(5, 350+(t*20), this.mFont, t+":", 20);
			this.mDebugText[t].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			this.mDebugText[t].setAlpha(0.5f);
			scene.getLayer(LAYER_SCORE).addEntity(this.mDebugText[t]);
		}

		scene.setBackgroundEnabled(false);
		scene.getLayer(LAYER_BACKGROUND).addEntity(new Sprite(0, 0, this.mBackgroundTextureRegion));	
		
		deck  = Arrays.asList(Card.values());
		Collections.shuffle(deck);
				
		Random rndCard = new Random();
		Card oCard = null;
		for (int l = 0; l < GRID_ROWS; l++) {
			for (int c = 0; c < GRID_COLS; c++) {
				oCard = deck.get(rndCard.nextInt(deck.size())); 										
				mGrid[(PokerSwap.GRID_COLS*l)+c] = this.addCard(scene, oCard, c, l);
			}
		}	
				
		scene.registerUpdateHandler(new TimerHandler(5.0f, true, new ITimerCallback() {
			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				if (!mCardToDestroyStack.isEmpty())
				{
					PokerSwap.this.runOnUpdateThread(new Runnable() {
						@Override
						public void run() {	
							while(!mCardToDestroyStack.isEmpty())
								PokerSwap.this.mEngine.getScene().getLayer(LAYER_CARD).removeEntity(mCardToDestroyStack.pop());
						}
					});
				}					
			}
		}));
		
		scene.registerUpdateHandler(new TimerHandler(1.0f, true, new ITimerCallback() {
			//Stack<Integer> mTempCardStack = new Stack<Integer>();
			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				if (mInTimerCallBack == false)
				{
					mInTimerCallBack = true;					
					boolean bAtLeastOneCombo = false;
					int iCardIndex1 = -1, iCardIndex2 =-1;
					if (mSwappedCardStack.size() == 2)
					{
						iCardIndex1 = mSwappedCardStack.get(0);
						if ( DoCheckCombos(iCardIndex1) )
							bAtLeastOneCombo = true;
						iCardIndex2 = mSwappedCardStack.get(1); // on doit les garder dans la pile au cas ou DoMoveBackSwappedCards
						if ( DoCheckCombos(iCardIndex2) )
							bAtLeastOneCombo = true;
						if (bAtLeastOneCombo)
						{													
							mSwappedCardStack.clear();
							mGrid[iCardIndex1].resetLastDir();
							mGrid[iCardIndex2].resetLastDir();
							DoRemoveCombosCards();						
						}
						else
							DoMoveBackSwappedCards();
					}
					else
						//if (!UpdCardStack.isEmpty())
						// TODO : Le pb c'est que l'on cree un thread pour animer une carte à la fois.
						while (!UpdCardStack.isEmpty())
						{
							iCardIndex1 = UpdCardStack.pop();
							// Si il y a un trou sous cette carte, elle tombe.
							if (( iCardIndex1+GRID_COLS< GRID_SIZE) && (mGrid[iCardIndex1+GRID_COLS] == null))
								DoFallCard(iCardIndex1);
							else // sinon on test si combos
								if ( DoCheckCombos(iCardIndex1) )
									DoRemoveCombosCards();
						}
					mInTimerCallBack = false;
				}
				else
					Log.w("=-=-=-=-=-PS","Already searching for combos ...");
			}
		}));
		scene.setOnSceneTouchListener(this);	
		CardSprite.setAnimFinishedListener(this);
		return scene;
	}
		
	private void DoSwapCards(SlideDirection peDirection)
	{		
		final SlideDirection eSlideDirection = peDirection; // thread parameters must be final
		this.runOnUpdateThread(new Runnable() {
			@Override
			public void run() {			
				try {
					mDebugText[2].setText("Slide: " + eSlideDirection);
					int iIndexCardToMove = mSelectedCard.Move(eSlideDirection);	
					if (iIndexCardToMove > -1)
						mGrid[iIndexCardToMove].MoveExchange(eSlideDirection);
				} catch (Exception e) {					
					e.printStackTrace();
				}
			}
		});
	}
	
		
	private void DoFallCard(int piIndexCardToFall)
	{		
		final int iIndexCardToFall = piIndexCardToFall;
		this.runOnUpdateThread(new Runnable() {
			@Override
			public void run() {			
				try {
					if (mGrid[iIndexCardToFall] != null)
					{
						mGrid[iIndexCardToFall].FallMove();
					}
				} catch (Exception e) {					
					e.printStackTrace();
				}
			}
		});
	}
	
	private void DoMoveBackSwappedCards()
	{		
		this.runOnUpdateThread(new Runnable() {
			@Override
			public void run() {			
				try {
					mGrid[mSwappedCardStack.pop()].MoveBack();
					mGrid[mSwappedCardStack.pop()].MoveBack();
				} catch (Exception e) {
					Log.i("=-=-=-=-=-PS","MovingBack...");	
					e.printStackTrace();
				}
			}
		});
	}
	
	private void DoRemoveCombosCards()
	{
		this.runOnUpdateThread(new Runnable() {
			@Override
			public void run() {
				int iCurrentCardIndex = -1;
				Stack<Integer> mFallingCardCheckStack = new Stack<Integer>();
				try {
					while (!PokerSwap.this.mCardToRemoveStack.isEmpty())
					{					
						iCurrentCardIndex = PokerSwap.this.mCardToRemoveStack.pop();
						mFallingCardCheckStack.push(iCurrentCardIndex); // On garde l'index pour tester après si la carte du dessus va tomber.
						Log.i("=-=-=-=-=-PS","Processing..." + iCurrentCardIndex);		
						if (PokerSwap.mGrid[iCurrentCardIndex] != null)
						{
							// On doit avant tout retirer toutes les cartes issues du combo
							PokerSwap.mGrid[iCurrentCardIndex].ComboMove(PokerSwap.mCardToDestroyStack.size()%10);	
							mCardMovingOutStack.push(PokerSwap.mGrid[iCurrentCardIndex]);
							PokerSwap.mGrid[iCurrentCardIndex] = null; // on libère la place														
						}									
					}
					while (!mFallingCardCheckStack.isEmpty())
					{
						iCurrentCardIndex = mFallingCardCheckStack.pop();
						while (iCurrentCardIndex>GRID_COLS-1) // Il n'y a rien au dessus de la premiere ligne
						{
							iCurrentCardIndex -= GRID_COLS;
							if ( iCurrentCardIndex >-1)
							{
								if (mGrid[iCurrentCardIndex] != null)
									mGrid[iCurrentCardIndex].FallMove();
							}
						}
					}
				} catch (Exception e) {
					Log.e("=-=-=-=-=-PS","Processing..." + iCurrentCardIndex);	
					e.printStackTrace();
				}
			}
			});
	}
	
	private boolean DoCheckCombos(int iCardIndex)
	{
		boolean bOneOrMoreCombos = false;
		Log.i("=-=-=-=-=-PS","Check combo around " + iCardIndex);
		// Toujours tester de la combo la plus compliquée (qui rapporte le plus ) vers la plus simple
//		int[] tabComboH = CheckHorizontalClones(iCardIndex,2);
//		int[] tabComboV = CheckVerticalClones(iCardIndex,2);
//		if (((tabComboH != null ) && (tabComboH.length>1 )) ||
//				((tabComboV != null ) && (tabComboV.length>1 )))
//		{
//			mCardToRemoveStack.push(iCardIndex);
//			bOneOrMoreCombos = true;
//		}	
		
		boolean bHClone = false; //CheckHorizontalSameCardProperty(iCardIndex,2,CardProperty.CLONE);	
		boolean bVClone = false; //CheckVerticalSameCardProperty(iCardIndex, 2, CardProperty.CLONE);
		
		boolean bHCarre =  CheckHorizontalSameCardProperty(iCardIndex,4,CardProperty.VALUE);	
		boolean bHColor =  CheckHorizontalSameCardProperty(iCardIndex,5,CardProperty.COLOR);
		boolean bHBrelan =  CheckHorizontalSameCardProperty(iCardIndex,3,CardProperty.VALUE);				
		boolean bHPair =  CheckHorizontalSameCardProperty(iCardIndex,2,CardProperty.VALUE);
		
		boolean bVCarre =  CheckVerticalSameCardProperty(iCardIndex,4,CardProperty.VALUE);	
		boolean bVColor =  CheckVerticalSameCardProperty(iCardIndex,5,CardProperty.COLOR);
		boolean bVBrelan =  CheckVerticalSameCardProperty(iCardIndex,3,CardProperty.VALUE);				
		boolean bVPair =  CheckVerticalSameCardProperty(iCardIndex,2,CardProperty.VALUE);
//		if (bHClone && bVClone)
//			mDebugText[3].setText("Double Clone !!!");
//		else
//			if (bHClone || bVClone)
//				mDebugText[3].setText("One Clone");
		if (bHPair || bVPair)
			mDebugText[3].setText("One Pair");
		if (bHColor || bVColor)
			mDebugText[3].setText("Color");
		if(bHBrelan || bVBrelan)
			mDebugText[3].setText("Brelan");
		if (bHCarre || bVCarre)
			mDebugText[3].setText("Carre");
		//if (bHBrelan && bHPair ) ==> bFull  mais pas possible pour l'instant
		bOneOrMoreCombos = bHClone || bVClone || bHPair || bHColor || bHBrelan || bHCarre|| bVPair || bVColor || bVBrelan || bVCarre;
		if (bOneOrMoreCombos)
			mCardToRemoveStack.push(iCardIndex);
		return bOneOrMoreCombos;			
	}

	private CardSprite addCard(final Scene pScene, final Card pCard, final int iC,
			final int iL) {
		final CardSprite sprite = new CardSprite(pCard, iC,iL, this.mCardTotextureRegionMap.get(pCard));
		pScene.getLayer(LAYER_CARD).addEntity(sprite);
		return sprite;
	}
	
	private SlideDirection CalculateSlideDirection(float DestX, float DestY)
	{
		SlideDirection eDir = SlideDirection.UNDEFINED;
		int iThreshold = 10;		
		if (mSelectedCard!=null)
		{
			float iX = Math.abs(DestX-mTouchDownPosition[0]);
			float iY = Math.abs(DestY-mTouchDownPosition[1]);
			//mDebugText[3].setText("DX:" + iX + " DY:" + iY); 
			// determine si le plus grand slide est vertical ou horizontal
			if (iX>iY)
			{
				if (iX>iThreshold)
				{
					if ( DestX>mTouchDownPosition[0])
						eDir=SlideDirection.RIGHT;
					else
						eDir=SlideDirection.LEFT;
				}
				else
					eDir=SlideDirection.TOO_SMALL_X;
			}
			else
			{
				if (iY>iThreshold)
				{
					if ( DestY>mTouchDownPosition[1])
						eDir=SlideDirection.DOWN;
					else
						eDir=SlideDirection.UP;
				}
				else
					eDir=SlideDirection.TOO_SMALL_Y;
			}						
		}
		return eDir;
	}
	
	
	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		switch (pSceneTouchEvent.getAction())
		{
		case TouchEvent.ACTION_DOWN:
			if (pSceneTouchEvent.getY()<Card.CARD_HEIGHT*8)
			{				
				mDebugText[0].setText("X:" + pSceneTouchEvent.getX() + " Y:" + pSceneTouchEvent.getY());
				mTouchDownPosition[0] = pSceneTouchEvent.getX();
				mTouchDownPosition[1] = pSceneTouchEvent.getY();
				int c = (int)pSceneTouchEvent.getX() / Card.CARD_WIDTH;
				int l = (int)pSceneTouchEvent.getY() / Card.CARD_HEIGHT;
				mDebugText[1].setText("C:" + c + " L:" + l);
				if (((l*PokerSwap.GRID_COLS)+c>=0 ) && ((l*PokerSwap.GRID_COLS)+c)<mGrid.length)
				{
					mSelectedCard = mGrid[(l*PokerSwap.GRID_COLS)+c];
					if (mSelectedCard != null)
					{
						mDebugText[3].setText(mSelectedCard.getCard().mValue + mSelectedCard.getCard().mColor.toString());		
						mSelectedCardOriginalPosition[0] = mSelectedCard.getX();
						mSelectedCardOriginalPosition[1] = mSelectedCard.getY();
					}
					else
					{
						mSelectedCardOriginalPosition[0] = -1;
						mSelectedCardOriginalPosition[1] = -1;
					}
				}
				else
				{
					mSelectedCard = null;
					mSelectedCardOriginalPosition[0] = -1;
					mSelectedCardOriginalPosition[1] = -1;
					mDebugText[2].setText("OutOfBound");
				}
			}
			else
			{
				pScene.getLayer(LAYER_SCORE).clear();
				if (mDrawDebugCardValue)
				{
					mDrawDebugCardValue = false;
					String strGridCard = "";
					for (int r = 0; r < GRID_ROWS; r++) {
						for (int c = 0; c < GRID_COLS; c++) {				
							if (mGrid[(PokerSwap.GRID_COLS*r)+c] != null)
								strGridCard = mGrid[(GRID_COLS*r)+c].toString();
							else
								strGridCard = "N";					
							pScene.getLayer(LAYER_SCORE).addEntity(new Text((c*Card.CARD_WIDTH)+10, (r*Card.CARD_HEIGHT)+10, this.mFont,strGridCard, HorizontalAlign.LEFT));					
						}
					}				
				}
				else
					mDrawDebugCardValue = true;
				mSelectedCard = null;
			}
			break;
		case TouchEvent.ACTION_MOVE:			
			break;
		case TouchEvent.ACTION_UP:
			if (mSelectedCard != null && mSwappedCardStack.size()==0)
			{				
				SlideDirection eDirection = CalculateSlideDirection( pSceneTouchEvent.getX(), pSceneTouchEvent.getY()); 
				PokerSwap.this.DoSwapCards(eDirection);
			}
			break;
		}		
		return true;
	}	
	
	private boolean CheckHorizontalSameCardProperty (int iCardIndex, int iMinCardForCombo, CardProperty cardProperty)
	{
		boolean bCombo = false;		
		int[] tabIndexClone = new int[GRID_COLS];
		int itabIndexClone = 0;
		final CardSprite sprite = mGrid[iCardIndex];
		int iIndexMin = (iCardIndex / GRID_COLS)*GRID_COLS;
		int iIndexMax = (((iCardIndex / GRID_COLS)+1)*GRID_COLS)-1;
		if (sprite != null)
		{
		try {
			for (int c=iCardIndex;c<iIndexMax;c++)
			{
				if (mGrid[c+1] != null)
				{
					if (cardProperty.equals(CardProperty.CLONE))
					{					
						if (sprite.getCard().name().equals(mGrid[c+1].getCard().name()))
						{
							tabIndexClone[itabIndexClone] = c+1;
							itabIndexClone++;
						}
						else
							break;
					}
					else
						if (cardProperty.equals(CardProperty.COLOR))
						{					
							if (sprite.getCard().mColor.equals(mGrid[c+1].getCard().mColor))
							{
								tabIndexClone[itabIndexClone] = c+1;
								itabIndexClone++;
							}
							else
								break;
						}
						else
							if (cardProperty.equals(CardProperty.VALUE))
							{					
								if (sprite.getCard().mValue.equals(mGrid[c+1].getCard().mValue))
								{
									tabIndexClone[itabIndexClone] = c+1;
									itabIndexClone++;
								}
								else
									break;
							}					
				}
				else
					break;
			}								
			for (int c=iCardIndex;c>iIndexMin;c--)
			{
				if (mGrid[c-1] != null)
				{
					if (cardProperty.equals(CardProperty.CLONE))
					{	
						if (sprite.getCard().name().equals(mGrid[c-1].getCard().name()))
							{
								tabIndexClone[itabIndexClone] = c-1;
								itabIndexClone++;
							}
							else
								break;
					}
					else
						if (cardProperty.equals(CardProperty.COLOR))
						{	
							if (sprite.getCard().mColor.equals(mGrid[c-1].getCard().mColor))
							{
								tabIndexClone[itabIndexClone] = c-1;
								itabIndexClone++;
							}
							else
								break;						
						}
						else
							if (cardProperty.equals(CardProperty.VALUE))
							{					
								if (sprite.getCard().mValue.equals(mGrid[c-1].getCard().mValue))
								{
									tabIndexClone[itabIndexClone] = c-1;
									itabIndexClone++;
								}
								else
									break;	
							}
				}
				else
					break;
			}						
		} catch (Exception e) {			
			e.printStackTrace();
		}
		if ( itabIndexClone>iMinCardForCombo-2)
		{
			bCombo = true;
			for(int i=0;i<itabIndexClone;i++)		
			{
				mCardToRemoveStack.push(tabIndexClone[i]);
				Log.i("=-=-=-=-=-PS","H "+ cardProperty.toString()+ " " + tabIndexClone[i]);
			}
		}			
		}
		return bCombo;
	}		
	
	private boolean CheckVerticalSameCardProperty (int iCardIndex, int iMinCardForCombo, CardProperty cardProperty)
	{
		boolean bCombo = false;			
		int[] tabIndexClone = new int[GRID_ROWS];
		int itabIndexClone = 0;
		final CardSprite sprite = mGrid[iCardIndex];
		int iIndexMin = iCardIndex;
		int iIndexMax = iCardIndex;
		boolean bContinue = true;
		if (sprite != null)
		{
		try {
			while (bContinue)
			{
				iIndexMax += GRID_COLS;
				if ((iIndexMax<GRID_SIZE) && (mGrid[iIndexMax] != null))
				{
					if (cardProperty.equals(CardProperty.CLONE))
					{
						if (sprite.getCard().name().equals(mGrid[iIndexMax].getCard().name()))
							{
								tabIndexClone[itabIndexClone] = iIndexMax;
								itabIndexClone++;
							}
							else
								bContinue = false;
					}
					else
						if (cardProperty.equals(CardProperty.COLOR))
						{
							if (sprite.getCard().mColor.equals(mGrid[iIndexMax].getCard().mColor))
							{
								tabIndexClone[itabIndexClone] = iIndexMax;
								itabIndexClone++;
							}
							else
								bContinue = false;
						}
						else
							if (cardProperty.equals(CardProperty.VALUE))
							{
								if (sprite.getCard().mValue.equals(mGrid[iIndexMax].getCard().mValue))
								{
									tabIndexClone[itabIndexClone] = iIndexMax;
									itabIndexClone++;
								}
								else
									bContinue = false;
							}
				}
				else
					bContinue = false;
			}
			bContinue = true;
			while (bContinue)
			{
				iIndexMin -= GRID_COLS;
				if ((iIndexMin>=0) && (mGrid[iIndexMin] != null))
				{
					if (cardProperty.equals(CardProperty.CLONE))
					{
						if (sprite.getCard().name().equals(mGrid[iIndexMin].getCard().name()))
							{
								tabIndexClone[itabIndexClone] = iIndexMin;
								itabIndexClone++;
							}
							else
							bContinue = false;
					}else
						if (cardProperty.equals(CardProperty.COLOR))
						{
							if (sprite.getCard().mColor.equals(mGrid[iIndexMin].getCard().mColor))
							{
								tabIndexClone[itabIndexClone] = iIndexMin;
								itabIndexClone++;
							}
							else
							bContinue = false;
						}
						else
							if (cardProperty.equals(CardProperty.VALUE))
							{
								if (sprite.getCard().mValue.equals(mGrid[iIndexMin].getCard().mValue))
								{
									tabIndexClone[itabIndexClone] = iIndexMin;
									itabIndexClone++;
								}
								else
								bContinue = false;
							}
				}
				else
					bContinue = false;
			}				
		} catch (Exception e) {			
			e.printStackTrace();
		}
		if ( itabIndexClone>iMinCardForCombo-2)
		{
			bCombo = true;
			for(int i=0;i<itabIndexClone;i++)	
			{
				mCardToRemoveStack.push(tabIndexClone[i]);				
				Log.i("=-=-=-=-=-PS","V "+ cardProperty.toString()+ " " + tabIndexClone[i]);
			}
		}			
		}
		return bCombo;
	}

	@Override
	public void AnimIsFinished(int piIndexCard, AnimType animType) {	
		final int iIndexCard = piIndexCard;
		Log.i("=-=-=-=-=-PS","End of " + animType.toString() + " for " + iIndexCard);
		switch (animType)
		{
		case SLIDE:
			break;
		case COMBO:
			break;
		case FALL:
			if ((iIndexCard>=GRID_COLS) && (iIndexCard<2*GRID_COLS))
			{				
				Random rndCard = new Random();	
				int l=(iIndexCard/GRID_COLS); 
				int c=iIndexCard-(l*GRID_COLS);
				l--; // La nouvelle carte va sur la ligne du dessus
				Card card = deck.get(rndCard.nextInt(deck.size()));				
				final CardSprite sprite = new CardSprite(card, c, l, this.mCardTotextureRegionMap.get(card));
				final int newSpriteIndex = sprite.getGridIndex();
				mGrid[newSpriteIndex] = sprite;
				this.runOnUpdateThread(new Runnable() {
					@Override
					public void run() {							
						try {
							Log.i("=-=-=-=-=-PS","New card" + mGrid[newSpriteIndex].toString() + " at " + newSpriteIndex);
							PokerSwap.this.getEngine().getScene().getLayer(LAYER_CARD).addEntity(mGrid[newSpriteIndex]);							
						} catch (Exception e) {					
							e.printStackTrace();
						}
					}
				});
			}
			break;
		case BACK:
			break;
		}
	}
}