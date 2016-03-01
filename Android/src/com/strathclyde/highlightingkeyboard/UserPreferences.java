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

/**
 * retrieves and saves in memory the user's preferences for the keyboard
 * @author ako2
 *
 */
public class UserPreferences {

	public int autocorrect;
	public int sound;
	public int haptic;
	public int visual;
	public int sugg_highlight;
	public int dots;
	
	public UserPreferences()
	{
		
	}
}
