package com.dawnsoft.games;

import android.util.Log;

public interface IAnimFinishedListener {
	
	public enum AnimType {
		FALL,
		COMBO,
		SLIDE,
		BACK
	}	
	
	// TODO : peut être renvoyer directement la CardSprite
	public void AnimIsFinished(int iIndexCard, AnimType animType);

}


/*
 	private boolean CheckVerticalClones (int iCardIndex, int iMinCardForCombo)
	{
		boolean bCombo = false;
		int[] tabfixIndexClone = null;		
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
					if (sprite.getCard().name().equals(mGrid[iIndexMax].getCard().name()))
						{
							tabIndexClone[itabIndexClone] = iIndexMax;
							itabIndexClone++;
						}
						else
							bContinue = false;
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
					if (sprite.getCard().name().equals(mGrid[iIndexMin].getCard().name()))
						{
							tabIndexClone[itabIndexClone] = iIndexMin;
							itabIndexClone++;
						}
						else
						bContinue = false;
				}
				else
					bContinue = false;
			}				
		} catch (Exception e) {			
			e.printStackTrace();
		}
//		if ( itabIndexClone>0)
//		{
//			tabfixIndexClone = new int[itabIndexClone];
//			for(int i=0;i<itabIndexClone;i++)
//			{
//				Log.i("=-=-=-=-=-PS","Clones V" + tabIndexClone[i]);
//				if (itabIndexClone>iMinCardForCombo-2)
//				{
//					tabfixIndexClone[i] = tabIndexClone[i];
//					mCardToRemoveStack.push(tabIndexClone[i]);
//				}
//			}			
//		}
//		}
//		return tabfixIndexClone;
		if ( itabIndexClone>iMinCardForCombo-2)
		{
			bCombo = true;
			for(int i=0;i<itabIndexClone;i++)	
			{
				mCardToRemoveStack.push(tabIndexClone[i]);
				Log.i("=-=-=-=-=-PS","Clones V" + tabIndexClone[i]);
			}
		}			
		}
		return bCombo;
	}
	
		private boolean CheckHorizontalClones (int iCardIndex, int iMinCardForCombo)
	{
		boolean bCombo = false;
		int[] tabfixIndexClone = null;		
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
					if (sprite.getCard().name().equals(mGrid[c+1].getCard().name()))
						{
							tabIndexClone[itabIndexClone] = c+1;
							itabIndexClone++;
						}
						else
							break;
				}
				else
					break;
			}
		
			for (int c=iCardIndex;c>iIndexMin;c--)
			{
				if (mGrid[c-1] != null)
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
					break;
			}						
		} catch (Exception e) {			
			e.printStackTrace();
		}
//		if ( itabIndexClone>0)
//		{
//			tabfixIndexClone = new int[itabIndexClone];
//			for(int i=0;i<itabIndexClone;i++)
//			{
//				Log.i("=-=-=-=-=-PS","Clones H" + tabIndexClone[i]);
//				if (itabIndexClone>iMinCardForCombo-2) // pour une paire (2) il faut au moins une autre carte donc itabIndexClone>0
//				{
//					tabfixIndexClone[i] = tabIndexClone[i];
//					mCardToRemoveStack.push(tabIndexClone[i]);
//				}
//			}			
//		}
//		}
//		return tabfixIndexClone;
		if ( itabIndexClone>iMinCardForCombo-2)
		{
			bCombo = true;
			for(int i=0;i<itabIndexClone;i++)		
			{
				mCardToRemoveStack.push(tabIndexClone[i]);
				Log.i("=-=-=-=-=-PS","Clones H" + tabIndexClone[i]);
			}
		}			
		}
		return bCombo;
	}
	*/
