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

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextServicesManager;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Second screen to appear after installation, guides user to make the appropriate settings to enable MaxieKeyboard and its spellchecker
 */

public class GUI_activity extends Activity implements SpellCheckerSessionListener {

	private Button enableMaxieButton, enableSpellerButton, selectMaxieButton, next;
	private ImageView enableMaxieCheck, enableSpellerCheck, selectMaxieCheck;
	private InputMethodManager im;
	private PackageManager pm;
	private String selectedKeyboardName = "";
	private String spellerName="";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gui_activity);
		
		enableMaxieButton = (Button) findViewById(R.id.language);
		enableSpellerButton = (Button) findViewById(R.id.subtype);
		selectMaxieButton = (Button) findViewById(R.id.selectkb);
		next = (Button) findViewById(R.id.next);
		
		enableMaxieCheck = (ImageView) findViewById(R.id.imageView1);
		selectMaxieCheck = (ImageView) findViewById(R.id.imageView2);
		enableSpellerCheck = (ImageView) findViewById(R.id.imageView3);
		
		enableMaxieCheck.setVisibility(View.INVISIBLE);
		selectMaxieCheck.setVisibility(View.INVISIBLE);
		enableSpellerCheck.setVisibility(View.INVISIBLE);
		
		im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		pm = getPackageManager();
		
		next.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(getApplicationContext(), BioActivity.class);
				startActivity(intent);
				finish();
			}
		});
		enableMaxieButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent viewIntent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
	            startActivity(viewIntent);
			}
		});
		
		enableSpellerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				ComponentName componentToLaunch = new ComponentName("com.android.settings", 
				        "com.android.settings.Settings$SpellCheckersSettingsActivity");

				        Intent intent = new Intent();
				        intent.setComponent(componentToLaunch);
				        try {
				            startActivity(intent);
				        } catch (ActivityNotFoundException e) {
				            //TODO
				        }
			}
		});
		
		selectMaxieButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				im.showInputMethodPicker();				
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gui_activity, menu);
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
	
	@Override
	public void onResume()
	{
		
		super.onResume();
		
		enableMaxieCheck.setVisibility(View.INVISIBLE);
		selectMaxieCheck.setVisibility(View.INVISIBLE);
		enableSpellerCheck.setVisibility(View.INVISIBLE);
		
		String targetKeyboardName = getResources().getString(R.string.ime_name);
		
		boolean keyboardActive=false;
		
		
		
		try	{
			//get the name of the enabled spellchecker
			TextServicesManager tm = (TextServicesManager) this.getSystemService(TEXT_SERVICES_MANAGER_SERVICE);
			Locale greek = new Locale("el", "GR");    			
			SpellCheckerSession mScs = tm.newSpellCheckerSession(null, greek, this, false);
			spellerName=mScs.getSpellChecker().loadLabel(pm).toString();		
			//Log.i("Installer", "speller= "+spellerName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//Log.e("Installer", "speller= "+spellerName);	
		}
		
		try	{
			//get the list of keyboards to see if Maxie is enabled
			List<InputMethodInfo> imes = im.getEnabledInputMethodList();
			for(int x=0; x<imes.size(); x++)
			{
				if (targetKeyboardName.equals(imes.get(x).loadLabel(pm).toString()))
				{
					//Log.e("Installer", "Maxie is enabled");		
					keyboardActive=true;
					break;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
		}	
		
		
		if(keyboardActive==true) //our keyboard has been enabled
		{
			enableMaxieCheck.setVisibility(View.VISIBLE);
		}
		
		if (selectedKeyboardName.equals(getResources().getString(R.string.ime_name))) //the current selected kb is ours
		{
			selectMaxieCheck.setVisibility(View.VISIBLE);
		}
			
		if (spellerName.equals(getResources().getString(R.string.spellcheckername))) //the current speller is ours
		{
			enableSpellerCheck.setVisibility(View.VISIBLE);
		}
		
		
		
	}
	
	@Override
	public void onWindowFocusChanged (boolean hasFocus)
	{
		onResume();
		//Log.i("Focus", "it is now "+hasFocus);
		if (hasFocus)
		{
			try {
				//get the name of the currently selected keyboard
				selectedKeyboardName=Settings.Secure.getString(
						   getContentResolver(), 
						   Settings.Secure.DEFAULT_INPUT_METHOD
						);
				String pkgs[] = selectedKeyboardName.split("/");
				//Log.i("Installer", "currentkb package= "+pkgs[0]);	
				selectedKeyboardName = getResources().getString(pm.getApplicationInfo(pkgs[0], PackageManager.GET_META_DATA).labelRes);	
				
				if (selectedKeyboardName.equals(getResources().getString(R.string.ime_name))) //the current selected kb is not Maxie
				{
					selectMaxieCheck.setVisibility(View.VISIBLE);
				}
				else
					selectMaxieCheck.setVisibility(View.INVISIBLE);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//Log.e("Installer", "currentkb= "+selectedKeyboardName);	
				selectMaxieCheck.setVisibility(View.INVISIBLE);
			}
			
		}
		
	}

	@Override
	public void onGetSuggestions(SuggestionsInfo[] results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
		// TODO Auto-generated method stub
		
	}
}
