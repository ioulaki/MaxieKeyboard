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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * 
 * Third screen to appear after installation, saves basic user information
 *
 */
public class BioActivity extends Activity {

	Spinner age, sex, country;
	EditText code;
	Button done;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bio);
		
		age = (Spinner) findViewById(R.id.spinnerAge);
		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
		        R.array.ages_array, R.layout.spinner_item);
		age.setAdapter(adapter1);
		
		sex = (Spinner) findViewById(R.id.spinnerSex);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
		        R.array.sex_array, R.layout.spinner_item);
		sex.setAdapter(adapter2);
		
		country = (Spinner) findViewById(R.id.spinnerCountry);
		ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this,
		        R.array.countries_array, R.layout.spinner_item);
		country.setAdapter(adapter3);
		
		done = (Button)findViewById(R.id.buttonDone);
		done.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				//save user data
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				Editor e = sp.edit();
				e.putInt("age", age.getSelectedItemPosition());
				e.putInt("sex", sex.getSelectedItemPosition());
				e.putInt("country", country.getSelectedItemPosition());
				e.putString("code", code.getText().toString());
				e.commit();
				
				Log.i("BioActivity","About to set user to "+sp.getString("prefUsername", "-1")+
			  			","+sp.getInt("age", -1)+
			  			","+sp.getInt("sex", -1)+
			  			","+sp.getInt("country", -1)+
			  			","+sp.getString("code", "-1"));
			  	
				
				finish();				
			}
		});
		
		code = (EditText)findViewById(R.id.code);		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bio, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
