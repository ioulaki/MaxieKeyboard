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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

/**
 * Captures information about a Typing Session (i.e. use of the keyboard from the time it was show, until it was hidden)
 * @author ako2
 *
 */
public class TypingSession {
	
	public long start_time; //time when the keyboard was shown
	public long end_time; //time when the keyboard was hidden
	public int sess_height; //keyboard view height
	public int sess_width; //keyboard view width
	public ArrayList<TypingEvent> events; //list of all typing events in this session
	public ArrayList<Character> suspects; //list of all suspect characters in this session
	public String app; //application in which input was performed
	public String firstWord; //first word typed in this session
	public int nLowErrors; //number of minor mistakes
	public int nHighErrors; //number of serious mistakes
	public int nSuggestionsPicked; //number of times the suggestion bar was used
	public int nInjections; //number of artificially injected errors in this session
	public String user; //user device id
	public int autocorrect; //was autocorrect enabled?
	public int sound; //was sound enabled?
	public int haptic; //was vibration enabled? 
	public int visual; //was visual feedback enabled?
	public int sugg_highlight; //was the highlighting of suggestion bar words enabled?
	public int dots; //was the touch input history enabled?

	/**
	 * create a new typing session object
	 * @param up
	 */
	public TypingSession(UserPreferences up)
	{
		start_time=System.currentTimeMillis();
		events=new ArrayList<TypingEvent>();
		suspects=new ArrayList<Character>();
		Log.i("Session Start", ""+start_time/1000);
		sess_height=0;
		sess_width=0;
		
		autocorrect=up.autocorrect;
		sound=up.sound;
		haptic=up.haptic;
		visual=up.visual;
		sugg_highlight=up.sugg_highlight;
		dots=up.dots;
	}
	
	/**
	 * figure out what was the first word typed in this session
	 * @param text the text, as input by the user at the end of the session
	 * @param composing the current composing text (in case the session didn't end with a word terminator character)
	 */
	public void getFirstWord(String text, String composing)
	{
		//Log.i("get First Word", "Extr: "+text+"\nComposing"+composing);
		
		if (!composing.equals(""))
			firstWord=composing;
		else
		if(!text.equals(""))
		{
			int pos;
			Pattern p = Pattern.compile("[\\W ]"); //regex to find the first word
			Matcher m = p.matcher(text);
			if (m.find()) {
			   pos = m.start();
			   firstWord = text.substring(0, pos);
			}
			else
				firstWord=text;
		}
			
	}
	
	/**
	 * output all session details to console for debugging
	 */
	public void printall()
	{
		Log.i("Typing Session Printall", "-------------");
		Log.i("Typing Session Printall", "Session start "+start_time);
		for (TypingEvent i : events)
		{
			Log.i("Typing Session Printall", "Event type: "+i.type+" keyChar: " +i.keyChar);
		}
		Log.i("Typing Session Printall", "Session end "+end_time);
		
		for (Character c : suspects)
		{
			Log.i("Typing Session Printall", "Suspect: "+ c);
		}
		Log.i("Typing Session Printall", "First word:" +firstWord);
		Log.i("Typing Session Printall", "Serious errors:" +nHighErrors+ " Small: "+nLowErrors + " Sugg:"+ nSuggestionsPicked +" Inj:"+nInjections);
		
		Log.i("Typing Session Printall", "-------------");
	}
}
