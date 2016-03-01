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
package com.strathclyde.oats;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * First screen to appear after installation of the keyboard, used to set the automatic data log upload
 * 
 */
public class IntroActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro);
		
		final CheckBox cb = (CheckBox) findViewById(R.id.consentCheck);
		Button next = (Button) findViewById(R.id.intronext);
		
		next.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				Editor e = sp.edit();
				e.putBoolean("automode", cb.isChecked());
				e.commit();
				Intent intent = new Intent(getApplicationContext(), GUI_activity.class);
				startActivity(intent);
				finish();
			}
		});
		
	}
}
