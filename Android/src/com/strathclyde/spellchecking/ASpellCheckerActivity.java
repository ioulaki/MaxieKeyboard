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


import com.strathclyde.oats.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * 
 * Builds the spellchecker settings activity
 *
 */

public class ASpellCheckerActivity extends PreferenceActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(	R.xml.prefs);
    }
}