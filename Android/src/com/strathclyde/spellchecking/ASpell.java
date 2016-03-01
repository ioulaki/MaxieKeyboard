/******************************************************************************
 * Based on code provided by Paschalis Padeleris https://github.com/padeler/aspellchecker
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

package com.strathclyde.spellchecking;

import android.util.Log;

/**
 * Initialises the ASpell spell checking library
 *
 */
public class ASpell
{
	public static final String TAG = ASpell.class.getSimpleName(); 
	
	static 
	{  
		try {
	        System.loadLibrary("aspell");
	    } catch (UnsatisfiedLinkError e) {
	    	e.printStackTrace();
	    	System.loadLibrary("<aspell>");
	    }
	   
	    
	}
	
	public ASpell(String dataDir, String locale)
	{
		//Log.d(TAG, "ASpell Speller Initializing....");
		
		boolean res = initialize(dataDir, locale);
		//Log.d(TAG, "ASpell Speller Initialized ("+res+")");
	}
	
	public native String[] check(String str);
	public native boolean initialize(String dataDir, String locale);
}
