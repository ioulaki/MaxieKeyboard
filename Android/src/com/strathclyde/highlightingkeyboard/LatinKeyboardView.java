/******************************************************************************
 * Based on code provided as a Copyright (C) 2008-2009 Google Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Contributors: 
 * Andreas Komninos, University of Strathclyde - Additional code implementation
 * http://www.komninos.info
 * http://mobiquitous.cis.strath.ac.uk
 ******************************************************************************/

package com.strathclyde.highlightingkeyboard;

import java.util.ArrayList;
import java.util.List;

import com.strathclyde.oats.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Extends the KeyboardView so we can draw on the keyboard
 * @author ako2
 *
 */
public class LatinKeyboardView extends KeyboardView {

	private int width=0;
	private int height=0;
	public List<Float> xcoords = new ArrayList<Float>();
	public List<Float> ycoords = new ArrayList<Float>();
	public int topOpacity = 200;
	public int wordcompletedtype = -1;
	public boolean dots;
	public String colorbar;
	private int big_err, small_err, no_err, autocorrect, kb_background;
	private LatinKeyboard mQwertyKeyboard;
	private LatinKeyboard mGreekKeyboard;
	private LatinKeyboard mSymbolsKeyboard;
	private LatinKeyboard mSymbolsShiftedKeyboard;
	protected int keysAfterColorBar = 0;
	private int colourbarLastColour;
	
	protected int currentKeyboard = KeyboardViews.QWERTY_EN; //1=English, 2=Greek 3=symbols 4=shiftedSymbols;
	protected int currentLang = KeyboardViews.QWERTY_EN;
	
	protected int imeOptions=-1;
	//public Context cx;
	
    static final int KEYCODE_OPTIONS = -100;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        assignColours();
        inflateKeyboards(context);
   
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        assignColours();
        inflateKeyboards(context);
      
    }
    
    private void inflateKeyboards(Context context)
    {
    	mQwertyKeyboard = new LatinKeyboard(context, R.xml.qwerty);
        mGreekKeyboard = new LatinKeyboard(context, R.xml.greekqwerty);
        mSymbolsKeyboard = new LatinKeyboard(context, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(context, R.xml.symbols_shift);
        //setKeyboard(mQwertyKeyboard);
    }
    
    /**
     * retrieves the colours for the error support mechanisms, as defined in XML
     */
    private void assignColours()
    {
    	big_err = getResources().getColor(R.color.big_error_solid);
        small_err = getResources().getColor(R.color.slight_error_solid);
        no_err = getResources().getColor(R.color.no_error_solid);
        autocorrect = getResources().getColor(R.color.autocorrect_solid);
        kb_background = getResources().getColor(R.color.kb_background);
    }
    
    /**
     * instructs the keyboard view to switch to a different key layout
     */
    protected void switchKeyboard()
    {
    	switch(currentKeyboard)
    	{
    	case KeyboardViews.QWERTY_EN:
    			setKeyboard(mQwertyKeyboard);
    			break;
    	case KeyboardViews.QWERTY_EL:
			setKeyboard(mGreekKeyboard);
			break;
    	case KeyboardViews.SYMBOLS:
			setKeyboard(mSymbolsKeyboard);
			break;
    	case KeyboardViews.SYMBOLS_SHIFTED:
			setKeyboard(mSymbolsShiftedKeyboard);
			break;
    	}
    	
    	if(imeOptions!=-1)
    	{
    		((LatinKeyboard)getKeyboard()).setImeOptions(getContext().getResources(), imeOptions);
    		invalidateAllKeys();
    	}
    }
    
    /**
     * used to switch between English and Greek by long-pressing the '123' button
     */
    @Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
        	if(key.codes[0]==-2)
        	{
        		Log.i("KeyboardView Long Press", "Key "+key.codes[0]);
    			//Keyboard current = getKeyboard();	           	            
    			if(currentKeyboard==KeyboardViews.QWERTY_EN)
    			{
    				Log.i("KeyboardView Long Press", "Switching to Greek");
    				setKeyboard(mGreekKeyboard);
    				currentKeyboard=KeyboardViews.QWERTY_EL;
    				currentLang=KeyboardViews.QWERTY_EL;
    				
    				return true;
    			}
    			else
    				if (currentKeyboard==KeyboardViews.QWERTY_EL)
    				{
    					Log.i("KeyboardView Long Press", "Switching to English");
    					setKeyboard(mQwertyKeyboard);
    					currentKeyboard=KeyboardViews.QWERTY_EN;
    					currentLang=KeyboardViews.QWERTY_EN;
    					return true;
    				}
        	}
        }              	
    		return super.onLongPress(key);
    }
    
    /**
     * Records data during keyboard touch down and up events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent me)
    {
    	   	
    	//Log.i("Keyboard View","Key ACTION = " +me.getAction());
    	
    	if (me.getAction()==MotionEvent.ACTION_DOWN) //a touch down has just been detected
    	{
    		//Log.i("Keyboard View","Touch down");
    		//reset the current event state
        	
        	SoftKeyboardService.currentEvent=null;
        	
        	//create a new event and record the parameters
        	SoftKeyboardService.currentEvent=new TypingEvent(3, null);
        	SoftKeyboardService.currentEvent.timeDown=System.currentTimeMillis();
        	
    		SoftKeyboardService.currentEvent.rawxDown=me.getRawX();
    		SoftKeyboardService.currentEvent.rawyDown=me.getRawY();
    		xcoords.add(me.getX());
    		ycoords.add(me.getY());
    		SoftKeyboardService.currentEvent.xDown=me.getX();
    		SoftKeyboardService.currentEvent.yDown=me.getY();
    		SoftKeyboardService.currentEvent.minorAxisDown=me.getTouchMinor();
    		SoftKeyboardService.currentEvent.majorAxisDown=me.getTouchMajor();
    		
    	}
    	
    	else if (me.getAction()==MotionEvent.ACTION_UP)
    	{
    		
    		//SoftKeyboard.currentEvent.timeUp=System.currentTimeMillis();
    		//Log.i("Keyboard View","Touch up");
    		SoftKeyboardService.currentEvent.rawxUp=me.getRawX();
    		SoftKeyboardService.currentEvent.rawyUp=me.getRawY();
    		SoftKeyboardService.currentEvent.xUp=me.getX();
    		SoftKeyboardService.currentEvent.yUp=me.getY();
    		SoftKeyboardService.currentEvent.minorAxisUp=me.getTouchMinor();
    		SoftKeyboardService.currentEvent.majorAxisUp=me.getTouchMajor();
    		keysAfterColorBar++;
    	}
    	else
    	{
    		//Log.i("Keyboard View","Key ACTION = " +me.getAction());
    	}
    	
    	invalidateAllKeys();
    	
    	return super.onTouchEvent(me);
    	//return false;
    }
    
    public int keyboardHeight()
    {
    	return height;
    }
    public int keyboardWidth()
    {
    	return width;
    }
    
    /**
     * captures the keyboard's screen dimensions
     */
    @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh)
    {
    	width=w;
    	height=h;
    	//Log.i("Keyboard size", "Old W-H = "+oldw+", "+oldh+"\tNew W-H = "+w+", "+h);	
    }
    
    /**
     * used to draw the error support mechanisms (colour bar and dots) on the keyboard
     */
    @Override
    public void onDraw(Canvas canvas)
    {
    	super.onDraw(canvas);
    	Paint p = new Paint();
    	
		if (dots) //if the touch history dots preference is set to true
		{	    	
	    	p.setARGB(topOpacity, 51, 181, 229);
	    	//Log.i("Draw", "Event size "+xcoords.size());
	    	int lim=0;
	    	if (xcoords.size()>10)
	    	{
	    		lim=xcoords.size()-10;
	    	}
	    	/*
	    	 * draw the most recent touch event with full opacity, then reduce the opacity of previous events
	    	 * this produces a "fading" effect
	    	 */
	    	for (int z = xcoords.size()-1; z>=lim; z--) 
	    	{
	    		
	    		int iteration = xcoords.size()-z-1;
	    		//Log.i("Draw", "Circle at "+xcoords.get(z)+","+ycoords.get(z)+" alpha: "+(topOpacity-iteration*topOpacity/10));
	    		p.setARGB(topOpacity-iteration*topOpacity/10, 51, 181, 229);
	    		canvas.drawCircle(xcoords.get(z), ycoords.get(z), 20, p);
	    	}
		}
    	
    	
    	/*
    	 * if the word is not under composition, ie. just completed
    	 * find the appropriate colour for the colour bar based on whether the last word had a spelling mistake
    	 */
    	if (wordcompletedtype!=-1 && colorbar!="off")
    	{
    		p.setStyle(Paint.Style.STROKE);
    		p.setStrokeWidth(20);
    		switch (wordcompletedtype)
        	{
        	case 1:
        		p.setColor(small_err);
        		break;
        	case 2:
        		p.setColor(big_err);
        		break;
        	case 3:
        		p.setColor(no_err);
        		break;
        	case 4:
        		p.setColor(autocorrect);
        		break;
        	default:
        		break;
        	}
    		//Log.i("Keyboard View", colorbar);
    		
    		drawColourbar(colorbar, canvas, p);
    		
    		keysAfterColorBar=0; //set the keys pressed after the colour bar had been drawn as a result to a word having finished being typed, to 0
    		colourbarLastColour = p.getColor();
    	}

    	wordcompletedtype=-1;
    	
    	/*
    	 * a word is being typed, create a fading effect for the colour bar, based on the result of the last word that had been typed.
    	 * this way, the user can be reminded that something went on with the previous word, in case they missed the colour bar changing colours 
    	 */
    	if (keysAfterColorBar>0 && keysAfterColorBar<=8)
    	{
    		int transparent = Color.argb((256-(32*keysAfterColorBar)), Color.red(colourbarLastColour), Color.green(colourbarLastColour), Color.blue(colourbarLastColour));
    		p.setColor(transparent);
    		p.setStyle(Paint.Style.STROKE);
    		p.setStrokeWidth(20);
    		drawColourbar(colorbar, canvas, p);
    		return;
    	}
    	
    }
    
    /**
     * Draws the colour bar based on the user's preferences
     */
    private void drawColourbar(String colorbarStyle, Canvas c, Paint p)
    {
    	if(colorbarStyle.equals("top"))
		{
			c.drawLine(0, 0, c.getWidth()-1, 0, p);
		}
		else
			if (colorbarStyle.equals("border"))
			{
				c.drawRect(0, 0, c.getWidth()-1, c.getHeight()-1, p);
			}
			else if (colorbarStyle.equals("background"))
			{
				int resred, resgreen, resblue;
				int newColour;
				int pcolor = p.getColor();
				float opacity = p.getAlpha()/255.0f;

				resred = (int) (Color.red(pcolor) * opacity + ((1-opacity)*Color.red(kb_background)));
				resblue = (int) (Color.blue(pcolor) * opacity + ((1-opacity)*Color.blue(kb_background)));
				resgreen = (int) (Color.green(pcolor) * opacity + ((1-opacity)*Color.green(kb_background)));
				newColour= Color.rgb(resred, resgreen, resblue);
				//Log.i("Draw Colourbar", "New Colour "+opacity+", "+Color.red(newColour)+", "+Color.green(newColour)+", "+Color.blue(newColour));
				this.setBackgroundColor(newColour);
			}
    }
    
    public void resetBackground()
    {
    	this.setBackgroundColor(kb_background);
    }
      
}
