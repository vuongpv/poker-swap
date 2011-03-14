package com.dawnsoft.games;

public enum Color {
	// ===========================================================
	// Elements
	// ===========================================================
	
	CLUB,  
	HEART,
	SPADE, // PIQUE
	DIAMOND;	
	

	// ===========================================================
	// Methods
	// ===========================================================

	@Override
	public String toString() {	
		return this.name().substring(0, 1);
	}
}
