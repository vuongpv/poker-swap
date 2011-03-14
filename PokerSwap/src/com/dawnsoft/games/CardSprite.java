package com.dawnsoft.games;

import org.anddev.andengine.entity.shape.IShape;
import org.anddev.andengine.entity.shape.modifier.MoveXModifier;
import org.anddev.andengine.entity.shape.modifier.MoveYModifier;
import org.anddev.andengine.entity.shape.modifier.ParallelShapeModifier;
import org.anddev.andengine.entity.shape.modifier.RotationModifier;
import org.anddev.andengine.entity.shape.modifier.ScaleModifier;
import org.anddev.andengine.entity.shape.modifier.IShapeModifier.IShapeModifierListener;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.IModifier;

import android.util.Log;

import com.dawnsoft.games.IAnimFinishedListener.AnimType;
import com.dawnsoft.games.PokerSwap.SlideDirection;


public class CardSprite extends Sprite {
	
	private static IAnimFinishedListener AnimFinishedListener = null;
	
	private Card card = null;
	
	private final float fSlideDuration = 0.5f; 
	private final float fComboDuration = 2.0f; 
	private SlideDirection eLastDir = SlideDirection.UNDEFINED;
	private boolean bMoveBack = false;
	
	private int miC = -1;
	private int miR = -1;
		
	public static void setAnimFinishedListener(IAnimFinishedListener listener) {
		AnimFinishedListener = listener;
	}
	
	public void resetLastDir() {
		eLastDir = SlideDirection.UNDEFINED;
	}	

	public Card getCard() {
		return card;
	}	
	
	public int getColumn()
	{ 
		return miC;
	}
	
	public int getRow()
	{
		return miR;
	}
	
	public int getGridIndex()
	{
		return (PokerSwap.GRID_COLS*miR)+miC;
	}
	
	private IModifier<IShape> mMoveModifier = null; 

	public CardSprite(Card pCard,int iC, int iL, TextureRegion pTextureRegion) {		
		super(iC*Card.CARD_WIDTH, iL*Card.CARD_HEIGHT, pTextureRegion);
		card = pCard;
		miC = iC;
		miR = iL;
		
	}			

	public void FallMove ()
	{
		IModifier<IShape> mFallModifier = new MoveYModifier(fSlideDuration, this.getY(), this.getY()+Card.CARD_HEIGHT);
		mFallModifier.setRemoveWhenFinished(true);
		mFallModifier.setModifierListener(new IShapeModifierListener() {
			@Override
			public void onModifierFinished(final IModifier<IShape> pShapeModifier, final IShape pShape) {													
				miC = (int)(CardSprite.this.getX()+10) / Card.CARD_WIDTH; // +10 not be on card edge, may be have to compute center 
				miR = (int)(CardSprite.this.getY()+10) / Card.CARD_HEIGHT;// +10 not be on card edge, may be have to compute center
				if (getGridIndex()<PokerSwap.GRID_SIZE)
				{										
					PokerSwap.UpdCardStack.push(getGridIndex());
				}
				else
					Log.i("=-=-=-=-=-PS"," No place below this point onModifierFinished" + getGridIndex());
				AnimFinishedListener.AnimIsFinished(getGridIndex(), AnimType.FALL);
			}
		});		
		this.addShapeModifier(mFallModifier);	
		if (getGridIndex()+PokerSwap.GRID_COLS<PokerSwap.GRID_SIZE)
		{
			PokerSwap.mGrid[getGridIndex()+PokerSwap.GRID_COLS] = CardSprite.this;	
			if (PokerSwap.mGrid[getGridIndex()] == CardSprite.this)
				PokerSwap.mGrid[getGridIndex()] = null; //On libère la place
		}
		else
			Log.i("=-=-=-=-=-PS"," No place below this point FallMove" + getGridIndex());
	}
	
	public void ComboMove (int iComboPos)
	{
		IModifier<IShape> ComboModifier = new ParallelShapeModifier(
									new ScaleModifier(fComboDuration, 1.2f, 0.8f),
									new RotationModifier(fComboDuration, 0, 360),
									new MoveXModifier(fComboDuration, getX(), 5+(iComboPos*(Card.CARD_WIDTH>>1))),
									new MoveYModifier(fComboDuration, getY(), PokerSwap.CAMERA_HEIGHT-Card.CARD_HEIGHT)
							);
		ComboModifier.setRemoveWhenFinished(true);
		ComboModifier.setModifierListener(new IShapeModifierListener() {
			@Override
			public void onModifierFinished(final IModifier<IShape> pShapeModifier, final IShape pShape) {	
				PokerSwap.mCardToDestroyStack.push(CardSprite.this);	
				PokerSwap.mCardMovingOutStack.remove(CardSprite.this);
				AnimFinishedListener.AnimIsFinished(getGridIndex(), AnimType.COMBO);
			}
		});		
		this.addShapeModifier(ComboModifier);
	}	
	
	public void MoveExchange(SlideDirection Dir)
	{
		if ( Dir != SlideDirection.UNDEFINED)				
			Move(getOppositeDirection(Dir));		
	}
	
	public void MoveBack()
	{		
		bMoveBack = true;
		MoveExchange(eLastDir);
	}
	
	public int Move (SlideDirection Dir)
	{		
		eLastDir = Dir;
		int iIndexCardToMoveBack = -1;
		if ((mMoveModifier == null ) || (mMoveModifier.isFinished()))
		{
			switch (Dir)
			{
			case RIGHT:
				if ( miC<PokerSwap.GRID_COLS-1)
				{
					mMoveModifier = new MoveXModifier(fSlideDuration, this.getX(), this.getX()+Card.CARD_WIDTH);
					iIndexCardToMoveBack = getGridIndex()+1;
				}
				break;
			case LEFT:
				if ( miC>0)
				{
					mMoveModifier = new MoveXModifier(fSlideDuration, this.getX(), this.getX()-Card.CARD_WIDTH);
					iIndexCardToMoveBack = getGridIndex()-1;
				}
				break;
			case UP:
				if ( miR>0)
				{
					mMoveModifier = new MoveYModifier(fSlideDuration, this.getY(), this.getY()-Card.CARD_HEIGHT);
					iIndexCardToMoveBack = getGridIndex()-PokerSwap.GRID_COLS;
				}
				break;
			case DOWN:
				if ( miR<PokerSwap.GRID_ROWS-1)
				{
					mMoveModifier = new MoveYModifier(fSlideDuration, this.getY(), this.getY()+Card.CARD_HEIGHT);
					iIndexCardToMoveBack = getGridIndex()+PokerSwap.GRID_COLS;
				}
				break;
			}
			if ( mMoveModifier != null)
			{
				// Il faut deux cartes pour faire un swap
				if ((iIndexCardToMoveBack >=0) && (iIndexCardToMoveBack<PokerSwap.GRID_SIZE) && (PokerSwap.mGrid[iIndexCardToMoveBack] != null))
				{
					mMoveModifier.setRemoveWhenFinished(true); 
					mMoveModifier.setModifierListener(new IShapeModifierListener() {
						@Override
						public void onModifierFinished(final IModifier<IShape> pShapeModifier, final IShape pShape) {
							// Ici CardSprite.this == pShape										
							miC = (int)(CardSprite.this.getX()+10) / Card.CARD_WIDTH; // +10 not be on card edge, may be have to compute center 
							miR = (int)(CardSprite.this.getY()+10) / Card.CARD_HEIGHT;// +10 not be on card edge, may be have to compute center 
							PokerSwap.mGrid[getGridIndex()] = CardSprite.this;
							if (!bMoveBack)		
							{
								PokerSwap.mSwappedCardStack.push(getGridIndex());
								AnimFinishedListener.AnimIsFinished(getGridIndex(), AnimType.SLIDE);
							}
							else
							{
								bMoveBack = false;
								AnimFinishedListener.AnimIsFinished(getGridIndex(), AnimType.BACK);
							}
							//Log.i("=-=-=-=-=-PS",card.name() +  " GridIndex: " + getGridIndex());
						}
					});		
					this.addShapeModifier(mMoveModifier);		
				}
				else // on annule le swap
				{
					iIndexCardToMoveBack = -1;
					mMoveModifier = null;
				}
			}
		}
		else
			Log.i("=-=-=-=-=-PS","Animation in progress");
		return iIndexCardToMoveBack;
	}	
	
	@Override
	public String toString() {	
		return card.toString();
	}
	
	public SlideDirection getOppositeDirection(SlideDirection Dir)
	{
		SlideDirection eOppositeDir = SlideDirection.UNDEFINED;
		switch (Dir)
		{
		case RIGHT:
			eOppositeDir = SlideDirection.LEFT;
			break;
		case LEFT:
			eOppositeDir = SlideDirection.RIGHT;
			break;
		case UP:
			eOppositeDir = SlideDirection.DOWN;
			break;
		case DOWN:
			eOppositeDir = SlideDirection.UP;
			break;
		}
		return eOppositeDir;
	}
	
	


}
