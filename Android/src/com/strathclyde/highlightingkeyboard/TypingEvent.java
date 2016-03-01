/******************************************************************************
 * Copyright 2016 University of Strathclyde   
 * All rights reserved. This program and the accompanying materials   
 * are made available under the terms of the Eclipse Public License v1.0  
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html

 *           
 * Contributors: 
 * Andreas Komninos, University of Strathclyde - code implementation
 * http://www.komninos.info
 * http://mobiquitous.cis.strath.ac.uk
 *****************************************************************************/
package com.strathclyde.highlightingkeyboard;

import android.util.Log;

/**
 * Captures details of a typing event, i.e. a key being pressed
 * @author ako2
 *
 */
public class TypingEvent {

	public int type;
	public long timeSinceLast; //time elapsed since last keypress
	public long timeDown; //time of touch down action
	public long duration; //input event duration (key down to key up)
	public long timeUp; //time of touch up action
	public float rawxDown; //raw x touch down coords
	public float rawyDown; //raw y touch down coords
	public float rawxdiff; //input event x difference in raw coordinates
	public float rawydiff; //input event y difference in raw coordinates
	public float xdiff; //input event x difference in relative coordinates to keyboard view
	public float ydiff; //input event y difference in relative coordinates to keyboard view
	public float xDown; //x touch down coords
	public float yDown; //y touch down coords
	public float majorAxisDown; //ellipse major axis down
	public float minorAxisDown; //ellipse minor axis down
	public float rawxUp; //raw x touch up coords
	public float rawyUp; //raw y touch up coords
	public float xUp; //x touch up coords
	public float yUp; //y touch up coords
	public float majorAxisUp; // ellipse major axis up
 	public float minorAxisUp; //ellipse minor axis up
	public int keyCode; //keycode that was finally input
	public char keyChar; //keychar that was finally input - if applicable
	public boolean followsSpace; //did this character come after a press of the space bar?
	public boolean precedesSpace; //did this character precede a press of the space bar?
	public boolean followsShift; //did this character follow a press of the shift key?
	public boolean isSuspect; //is this a suspect character?
	public String user; //ID of the user's device
	
	public String details;
	
	
	public TypingEvent(int t, String de)
	{
		type=t;
		details=de;
		timeUp=0;
		timeDown=0;
		timeSinceLast=0;
	}
	
	public void calcTimeSinceLast(long lastTime)
	{
		timeSinceLast=timeDown-lastTime;
		//Log.i("TIME","this Down: "+timeDown+"\nprevious Up: "+lastTime+"\nelapsed: "+timeSinceLast);
	}
	
	public void calcDuration()
	{
		duration=timeUp-timeDown;
		//timeUp=0;
		//timeDown=0;
	}
	
}
