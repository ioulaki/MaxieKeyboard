/******************************************************************************
 * Based on code provided as a Copyright (C) 2008-2009 Google Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Based on code provided as a Copyright 2011 KeyPoint Technologies (UK) Ltd.   
 * All rights reserved. This program and the accompanying materials   
 * are made available under the terms of the Eclipse Public License v1.0  
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Andreas Komninos, University of Strathclyde - Additional code implementation
 * http://www.komninos.info
 * http://mobiquitous.cis.strath.ac.uk
 ******************************************************************************/

package com.strathclyde.highlightingkeyboard;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Looper;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.MetaKeyKeyListener;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamKeymapId;
import com.strathclyde.corehandler.CoreEngine;
import com.strathclyde.corehandler.CoreEngineInitialize;
import com.strathclyde.corehandler.CoreEngine.KPTSuggestion;
import com.strathclyde.corehandler.CoreEngineInitialize.KPT_SUGG_STATES;
import com.strathclyde.oats.R;
import com.strathclyde.spellchecking.KeyGraph;
import com.strathclyde.spellchecking.SpellForSamsung;

/**
 * Extends the InputMethodService class to provide the keyboard functionality
 * Handles the keyboard view creation and destruction
 * Manages user input and spell-checking
 * Stores user input data
 * Manages the highlighting of text in the editor views
 * Manages audio and haptic feedback
 * @author ako2
 *
 */

public class SoftKeyboardService extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener, SpellCheckerSessionListener {
    static final boolean DEBUG = false;
    
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;
    
    //keyboard service components
    private LatinKeyboardView mInputView; //whatever keyboard view is currently active
    private CandidateView mCandidateView; //the candidates view
    private CompletionInfo[] mCompletions;
    private LatinKeyboard mCurKeyboard; //the current keyboard
    private InputConnection ic;
	private ExtractedText extr;
	private DBmanager dbm;
	private SpellCheckerSession mScs;
	private TextServicesManager tsm;
    private StringBuilder mComposing = new StringBuilder(); //the current word under composition
    private JSONObject suspectReplacementDistribution; //loaded from JSON file in assets
	private KeyGraph keyModel;
	protected CoreEngine coreEngine;
	int lastKeyboardView;
	
    //keyboard service parameters
    private boolean mPredictionOn; 
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;
    private String mWordSeparators;
    private String mSpecialSeparators;
    //keyboard service operational parameters & flags
    private List<String> suggestions= new ArrayList<String>();
    private boolean firstWordSet=false;
    private boolean captureData = true;
    private String composition;
    private String mComposingTemp="";
	private int wordSeparatorKeyCode;
	private ExtractedText extractedText;
    private HashMap<String, String> autocorrected_words;
	private boolean replacemode = false;
	private boolean updateSuggestionList=false;
	private String origWord;
	private boolean shouldInsertSpace = false;
	private boolean errorInjection=false;
	private int errorInjectionThreshold = 10;
	private boolean errorInjectionSound=true;
	private int startingKeyboard;
	private int big_err, small_err, autocorrect, suggestion;
    
    //data to be logged
    protected static TypingSession currentSession; //there is one session object - every time a close event happens, the object gets flushed in the DB
    protected static TypingEvent currentEvent;
    protected char lastDeleted;
    protected int lastDeletedPos;
    private int nInjections;
	private String userid;
	private HashMap<Integer, Character> errorMap;
    
    /**
     * Main initialization of the input method component.  
     * Set up the word separators list
     * Initialize the core service
     * Initialize the colours to be used in highlighting
     * Initialize the list of autocorrected words
     * Load the suspect-replacement probability distribution map
     */
    @Override public void onCreate() {
        super.onCreate();

        //get User ID 
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            userid = (String) get.invoke(c, "ro.serialno");
            Log.i("OnCreate", "User id= "+userid);
        } catch (Exception ignored) {
        	Log.i("OnCreate", "Could not obtain userid");
        	userid="xxx";
        }
        Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
        e.putString("prefUsername", userid);
        e.commit();
        
        //used for managing injected errors
        errorMap = new HashMap<Integer, Character>();
        
        mWordSeparators = getResources().getString(R.string.word_separators);
        mSpecialSeparators = getResources().getString(R.string.special_separators);
        CoreEngineInitialize.initializeCoreService(getApplicationContext());
        initializeCore();
        assignColours();
        autocorrected_words = new HashMap<String, String>();
        try {
			suspectReplacementDistribution = new JSONObject(loadJSONFromAsset());
		} catch (JSONException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
        
        //setup the upload task alarm manager
        /*
         * Twice daily, broadcast an event
         * This will be trapped by our receiver 
         */          
        Intent alarmIntent = new Intent(this, UploadDataReceiver.class);
        alarmIntent.putExtra("origin", "alarm");
        alarmIntent.putExtra("insert", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC, Calendar.getInstance().getTimeInMillis(), AlarmManager.INTERVAL_HALF_DAY, pendingIntent);
        //Log.i("OnCreate", "Alarm set ");
    }
    
    /**
     * Prepare the colours to be used in highlighting by loading them from XML
     */
    private void assignColours()
    {
    	big_err = getResources().getColor(R.color.big_error_trans);
        small_err = getResources().getColor(R.color.slight_error_trans);
        //no_err = getResources().getColor(R.color.no_error_trans);
        autocorrect = getResources().getColor(R.color.autocorrect_trans);
        suggestion = getResources().getColor(R.color.suggestion_highlight);
        
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
    	if (mCurKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     * 
     * Inflate the input view
     * Set the listener of the view to this service
     * Initialize OpenAdaptxt core
     */
    @Override public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        initializeCore();     
        return mInputView;
       
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        //mCandidateView.setLayoutParams(params);
        return mCandidateView;
    }
    
    /**
     * Check to see what's going on with the services, useful to check if the spell-checker service is active.
     * Probably should remove this, no longer necessary.
     * @return true if the Spellchecker service is running
     */
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        	//Log.i("isService", service.service.getClassName()+",\n"+service.service.getPackageName());
        	if (service.service.getClassName().contains("pell"))
        	{
        		//Log.i("isService", service.service.getClassName()+",\n"+service.service.getPackageName());
	            if (service.service.getClassName().contains("ASpellChecker")) {
	                return true;
	            }
        	}
        }
        return false;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     * 
     * Set up edit field behaviour and disable logging if pass/email/url
     * Bind to spell-checking service
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
               
      //get connection to editor field
        ic = getCurrentInputConnection();
        
        //create a temporary keyboard to obtain the necessary options
        mCurKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        keyModel = new KeyGraph(mCurKeyboard);     
        /*display the coordinates of all keys
        
        List<Key> keys = mCurKeyboard.getKeys();
        for (int x=0; x<keys.size(); x++)
        {
        	System.out.println(keys.get(x).label+"*"
        			+keys.get(x).x+"*"
        			+keys.get(x).y+"*"
        			+keys.get(x).width+"*"
        			+keys.get(x).height);
        }
        */
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        captureData=true;

        //updateCandidates();
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                //mCurKeyboard = mSymbolsKeyboard;
            	startingKeyboard=KeyboardViews.SYMBOLS;
                break;
                
            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                //mCurKeyboard = mSymbolsKeyboard;
            	startingKeyboard=KeyboardViews.SYMBOLS;
            	break;
                
            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                //mCurKeyboard = mQwertyKeyboard;
            	startingKeyboard=KeyboardViews.QWERTY_EN;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                
                //do not log data from passwords, email addresses, URL fields
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || 
                        variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                    // Do not log data
                    captureData = false;
                    Log.i("OnStartInput", "DANGER FIELD - DO NOT CAPTURE");
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS 
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }
                
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                //mCurKeyboard = mQwertyKeyboard;
            	startingKeyboard=KeyboardViews.QWERTY_EN;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        if(mInputView!=null)
        {
        	//Log.i("onStartInput","Setting existing keyboard");
        	((LatinKeyboard) mInputView.getKeyboard()).setImeOptions(getResources(), attribute.imeOptions);
        }
        else
        {
        	//Log.i("onStartInput","Setting temp keyboard");
        	mCurKeyboard.imeOptions=attribute.imeOptions;
        	//mCurKeyboard.shifted=attribute.initialCapsMode;
        }
        
        //bind to the spell checking service
        tsm = (TextServicesManager) getSystemService(
        	      Context.TEXT_SERVICES_MANAGER_SERVICE);
        
        //Log.i("OnStartInput", "ID:"+attribute.fieldId+" "+attribute.fieldName);
       
      
    }
    
    //this is to ensure that the candidate view does not eat into the application space!
    @Override public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }
    
    /**
     * Finish the current session
     * Set session end time
     * Record high, low errors, suggestions picked
     * Dump session data to db
     */
    public void endSession()
    {	
    	
    	long endtime=System.currentTimeMillis();
    	//Log.i("Session End", ""+endtime/1000);
    	
    	if(currentSession!=null)
        {    		
    		currentSession.end_time=endtime;
    		currentSession.user=userid;
    		
    		if(currentSession.events.size()<=0)
    		{
    			//Log.i("Session End","No Events\n Event Dump follows");
    			//currentSession.printall();
    			currentSession=null;
    			return;
    		}
        	//if the last event was a backspace add the deleted char to the suspects
            if (currentSession.events.size()>0 && currentSession.events.get(currentSession.events.size()-1).keyCode==Keyboard.KEYCODE_DELETE)
            {
            	currentSession.suspects.add(lastDeleted);
            	//System.out.println("Suspect = "+lastDeleted);
            }
        	
        	try
        	{
        		
	        	if(!firstWordSet)
	        		currentSession.getFirstWord((String) extr.text, composition);
        	}
        	catch (Exception e)
        	{
        		System.out.println("Error getting current text");
        	}
        	
        	//clear out all the characters
        	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        	if (sp.getBoolean("fullLogging", false)==false)
        	{
	        	for (int x=0; x<currentSession.events.size();x++)
	        	{
		        	//don't record anything but backspaces
			    	if(currentSession.events.get(x).keyCode!=-5)	    	
			    	{
			    		currentSession.events.get(x).keyCode = -400;
			    		currentSession.events.get(x).keyChar='$';
			    	}
	        	}
        	}
        	
        	//Log.i("Session End","Event Dump follows");
        	currentSession.end_time=endtime;
        	//currentSession.nHighErrors=nHighErrors;
        	//currentSession.nLowErrors=nLowErrors;
        	//currentSession.nSuggestionsPicked=nSuggestionsPicked;
        	//currentSession.nInjections=nInjections;
        	currentSession.printall();        	
        	dbm.insert(currentSession);
        	//Log.i("Session End",""+currentSession.end_time);
        	currentSession=null;
        	extr=null;
        	firstWordSet=false;
        	composition=null;
        	if (coreEngine!=null)
        		coreEngine.resetCoreString();
            dbm.close();
            if (ic!=null)
            	ic.finishComposingText();   
        }
    }
    

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
    	Log.i("Finish Input", "INPUT FINISHED");
    	
        super.onFinishInput();        
        clearDots();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();
                
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        
        mCurKeyboard = null;
        if (mInputView != null) {
            mInputView.closing();
        }
        
        
    }
    /**
     * This method is called when the keyboard is shown
     * 
     * reset errors & suggestion picked counters
     * get dots and colourbar prefs
     * get injection prefs
     * update engine core prefs
     * prepare database for writing
     * create a new typing session
     * apply selected keyboard to input view
     * switch core engine dictionaries according to keyboard
     */
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
       
    	//Log.i("OnStartInputView","Keyboard about to be shown");
    	nInjections=0;
    	
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mInputView.dots = sharedPrefs.getBoolean("dots", false);
		mInputView.colorbar = sharedPrefs.getString("colorbar", "top");
    	mInputView.ycoords.clear();
    	mInputView.xcoords.clear();
    	mInputView.resetBackground();
    	
    	errorInjection=sharedPrefs.getBoolean("errorinjection", false);
    	errorInjectionThreshold = Integer.parseInt(sharedPrefs.getString("injectionThreshold", "20"));
    	errorInjectionSound = sharedPrefs.getBoolean("errorinjectionsound", true);
    	
    	
    	if (coreEngine!=null)
        	updateCorePrefs();
        
    	if(currentSession!=null)
    	{
    		endSession();
    	}
    	
    	//open the database for writing
    	if(dbm==null)
        	dbm=new DBmanager(getApplicationContext());
        dbm.open();
        
        //get connection to editor field
        //ic = getCurrentInputConnection();
		
        if(extractedText!=null)
        {
        	if(ic.deleteSurroundingText(9999, 0))
        	{
        		ic.commitText(extractedText.text, 1);
        		//extractedText=null;
        		//Log.i("onStartInputView", "Text Replaced");	
        	}
        	else
        	{
        		//Log.i("onStartInputView", "IC not valid");
        	}
        }
        
        //create a new typing session
        UserPreferences up = new UserPreferences();
        up.autocorrect = (sharedPrefs.getBoolean("autocorrect", true)) ? 1 : 0;
        up.sound = (sharedPrefs.getBoolean("audio", false)) ? 1 : 0;
        up.haptic = (sharedPrefs.getBoolean("vibrator", false)) ? 1 : 0;
        up.visual = (sharedPrefs.getBoolean("highlightwords", true)) ? 1 : 0;
        up.sugg_highlight = (sharedPrefs.getBoolean("suggestion_highlight", false)) ? 1 : 0;
        up.dots = (sharedPrefs.getBoolean("dots", false)) ? 1 : 0;
        
        currentSession=new TypingSession(up);
        currentSession.sess_height=mInputView.getHeight();
        currentSession.sess_width=mInputView.getWidth();
        currentSession.events.add(new TypingEvent(1, "Keyboard shown"));
        currentSession.user=userid;
        
        //find out what application has invoked the keyboard
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        if(android.os.Build.VERSION.SDK_INT<21) //works only on Android <5, retrieve the app name
        {
	        RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
	        
	        String foregroundTaskPackageName = foregroundTaskInfo .topActivity.getPackageName();
	        PackageManager pm = this.getPackageManager();
	        PackageInfo foregroundAppPackageInfo;
			try {
				foregroundAppPackageInfo = pm.getPackageInfo(foregroundTaskPackageName, 0);
				currentSession.app=foregroundAppPackageInfo.applicationInfo.loadLabel(pm).toString();
				//Log.i("OnStartInputView", "ForeGround app is "+foregroundAppPackageInfo.applicationInfo.loadLabel(pm).toString());
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
        }
        else //retrieve the package name
        {
        	List<RunningAppProcessInfo> ps = am.getRunningAppProcesses();
        	//Log.i("OnStartInput", "Running apps "+ps.size());
        	for (int x=0; x<ps.size(); x++)
        	{
        		RunningAppProcessInfo p=ps.get(x);
        		//Log.i("OnStartInput", "App is "+p.processName+p.importance);
        		
        		if(p.importance==RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
        		{
        			//Log.i("OnStartInput", "ForeGround app is "+p.processName);
        			currentSession.app=p.processName;
        			break; //the first one is the foreground app
        		}
        	}
        }
           	
        // Apply the selected keyboard to the input view.            
        //set the new keyboard options based on the temporary one created during OnStartInput
        if(mCurKeyboard!=null)
        {
        	//Log.i("onStartInputView","setting new keyboard to temp settings");
        	mInputView.setShifted(mCurKeyboard.isShifted());
        	mInputView.imeOptions=mCurKeyboard.imeOptions;
        	
        	
        	
        }
        else
        {
        	//Log.i("onStartInputView","mCurKeyboard is null");
        }
        
        mInputView.currentKeyboard=startingKeyboard;
        mInputView.switchKeyboard();
        
        
        if (coreEngine!=null)
        {
        	switch (mInputView.currentKeyboard)
        	{
            	case KeyboardViews.QWERTY_EL:
            		coreEngine.activateLanguageKeymap(131092, null);
            		coreEngine.setDictionaryPriority(131092, 0);
            		break;
            	case KeyboardViews.QWERTY_EN:
            		coreEngine.activateLanguageKeymap(131081, null);
            		coreEngine.setDictionaryPriority(131092, 1);
            		break;
        	} 	
        	
        	coreEngine.resetCoreString();
        	updateCandidates();
        	//KPTkeymapInfo();
        	     	
        	
        }
        
        super.onStartInputView(attribute, restarting);
    }
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        
    	// Log.i("onUpdateSelection", "Cursor Moved");
        // If the current selection in the text view changes, we should clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
        
        ExtractedText alltext=ic.getExtractedText(new ExtractedTextRequest(), 0);
        
        //special case, there is nothing in the editor (been deleted)
        if (alltext==null)
        	return;
        //special case - the text in the editor has all been deleted but the ic is still active
        if (newSelStart==0 && alltext.text.length()==0)
        {
        	coreEngine.resetCoreString();
        	updateCandidates();
        	return;
        }
        try{
	        if(newSelEnd-newSelStart==0 && newSelEnd<alltext.text.length()-1) //only if cursor movement, not actual selection, and we are not at the end of the text
	        {
	        	//Log.i("Selection Update", "Old: "+oldSelStart+","+oldSelEnd+"...New: "+newSelStart+","+newSelEnd);
	        	   	
	        	WordDetails w = findWord(newSelStart, alltext.text); //find the current word
	        	
	        	//Log.i("Selection Update", "WordStart, End = "+w.wordStart+","+w.wordEnd);
	        	if(w.wordStart>=0 && w.wordEnd>=0 && w.wordStart<w.wordEnd)
	        	{
	        		
		        	w.word=alltext.text.toString().substring(w.wordStart, w.wordEnd);	        	
		        	//Log.i("Selection Update","\nCurrent Word: ["+w.word+"]");
		        	        	       	
		        	//check if current word has been autocorrected
		        	//Log.i("Selection Update","Original word was "+autocorrected_words.get(w.word));
		            replacemode = true;
		        	//ic.setComposingRegion(w.wordStart, w.wordEnd); //mark this as composing - any key input will erase the word
	        		
		        	if (autocorrected_words.containsKey(w.word)) //a word that was autocorrected or highlighted as a mistake
		        	{
		        		//Log.i("Selection Update","Original word was "+autocorrected_words.get(w.word));
		        		updateCandidatesWithSpellChecker(autocorrected_words.get(w.word));        		
		        		//show candidates
		        	}
		        	else //not a mistake word, so just use adaptxt for suggestions
		        	{
		        		//Log.i("Selection Update","Not a mistake Word");
		        		coreEngine.resetCoreString();
		        		coreEngine.insertText(w.word);
		        		updateCandidates();
		        	}
		        	
		        	
	        	}
	        	
	        }
	        else
	    	{
	        	//Log.i("Selection Update", "Old: "+oldSelStart+","+oldSelEnd+"...New: "+newSelStart+","+newSelEnd);
	        	replacemode=false;
	        	if(newSelEnd==alltext.text.length() && newSelEnd-oldSelEnd>2)
	        	{
	        		//Log.i("Selection Update","At sentence end");
	        		ic.finishComposingText();
	        		coreEngine.resetCoreString();
	        		updateCandidates();
	        		
	        	}
	    	}
        }
        catch(Exception e)
        {
        	//Log.i("onUpdateSelection", "Failed to get extracted text");
        }
        
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
    }

    /**
     * Find out the position of a word in the text being input
     * @param cursor starting position in the text from which to begin the search (backwards)
     * @param text the text having been input so far
     * @return a WordDetails object with the start and end position of a given word
     */
    public WordDetails findWord(int cursor, CharSequence text)
    {
    	WordDetails word = new WordDetails();
    	
    	//loop backwards to find start of current word
    	for (int x=cursor-1; x>=0; x--)
    	{
    		if(x==0)
    		{
    			word.wordStart=x;
    			break;
    		}
    		else
        		if(isWordSeparator(text.charAt(x)))
        		{	
        			word.wordStart = x+1;
        			break;
        		}
    	}
    	//loop forwards to find end of current word
    	for (int x=cursor-1; x<text.length(); x++)
    	{
    		if(x<0)
    			break;
    		if(isWordSeparator(text.charAt(x)) || x==text.length()-1)
    		{	
    			word.wordEnd = x;
    			break;
    		}
    	}
    	
    	return word;
    }
    
    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        //InputConnection ic = ic;
        if (c == 0 || ic == null) {
            return false;
        }
        
        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        
        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }
        
        onKey(c, null);
        
        return true;
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        
    	//event.
    	//Log.i("OnKeyDown", "Keycode: "+keyCode);
    	switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
            
            case -2: //123 button
            	//Log.i("KeyDown", "Keycode: "+keyCode);
            	event.startTracking();
            	return true;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
                
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        //InputConnection ic = ic;
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }
    
    /**
     * Helper function to commit any text being composed in the editor
     * @param inputConnection our current connection with the editor
     * @param isWordSeparator 
     */
	private void commitTyped(InputConnection inputConnection, boolean isWordSeparator) 
	{       
		if (mComposing.length() > 0 ) 
        {
        	
        	//do some spell-checking
        	mComposingTemp = mComposing.toString();
        	extr=ic.getExtractedText(new ExtractedTextRequest(), 0);
	    	
        	WordDetails w = findWord(extr.selectionStart, extr.text);
        	if(w.wordStart>=0 && w.wordEnd>=0 && w.wordStart<w.wordEnd)
        	{
	        	w.word=extr.text.toString().substring(w.wordStart, extr.selectionStart);
	        	System.out.println("Cursor Position = "+extr.selectionStart+" Word="+w.word);
	        	if(w!=null && mComposingTemp!=null)
	        	{
		        	if(w.word.length()!=mComposingTemp.length())
		        	{
		        		mComposingTemp=w.word;
		        		if(captureData)
		        			ic.setComposingRegion(w.wordStart, extr.selectionStart);
		        	}
	        	}
	        	
        	}
        	
        	if (captureData) // don't want to be spell-checking on urls, emails, passwords
        	{
	        	if (mScs!=null) //get some suggestions
	        	{
	        		//Log.i("CommitTyped", "About to spellcheck "+mComposingTemp);	        		
	        		mScs.getSuggestions(new TextInfo(mComposingTemp), 5);
	        		
	        	}
	        	else //handle spelling for Samsung devices
	        	{
	        		//Log.e("CommitTyped", "mScs NULL");
	        		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    			String lang = prefs.getString("available_dicts", "el");
	    			XmlResourceParser p;
	    			if (lang.equals("en"))
	    				p=getResources().getXml(R.xml.qwerty);
	    			else
	    				p=getResources().getXml(R.xml.greekqwerty);

	        		SpellForSamsung sp = new SpellForSamsung(getAssets(),p,getFilesDir()+File.separator+"data",lang,5);
	        		SuggestionsInfo si[] = new SuggestionsInfo[1];
	        		si[0]=sp.spell(mComposingTemp);
	        		onGetSuggestions(si);
	        	}
        	}
        	else
        	{
    			//Log.i("CommitTyped", "Will not spellcheck in an inappropriate field, mComposing = "+mComposingTemp);
        		inputConnection.commitText(mComposingTemp, mComposingTemp.length());
        	}
        		
    	}        	    	  		
        mComposing.setLength(0);
        updateCandidates();
   
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
    	if (mInputView != null)
    	{
	    	if (attr != null 
	                //&& mQwertyKeyboard == mInputView.getKeyboard()) {
	                && (mInputView.currentKeyboard==KeyboardViews.QWERTY_EL || mInputView.currentKeyboard==KeyboardViews.QWERTY_EN)) {
	            int caps = 0;
	            EditorInfo ei = getCurrentInputEditorInfo();
	            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
	                caps = ic.getCursorCapsMode(attr.inputType);
	            }
	            mInputView.setShifted(mCapsLock || caps != 0);
	        }
    	}
    	else
    	{
    		int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = ic.getCursorCapsMode(attr.inputType);
            }
            mCurKeyboard.setShifted(mCapsLock || caps != 0);
    	}
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        ic.sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        ic.sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    ic.commitText(String.valueOf((char) keyCode), 1);
                    if (shouldInsertSpace)
            		{
            			ic.commitText(" ", 1);
            			shouldInsertSpace=false;
            		}
            		
                }
                break;
        }
    }

    /**
     * Remove all touch history events
     */
    public void clearDots()
    {
    	if(mInputView!=null)
    	{
	    	if (!mInputView.xcoords.isEmpty())
	    		mInputView.xcoords.clear();
	    	if (!mInputView.ycoords.isEmpty())
	    		mInputView.ycoords.clear();
    	}
    }
    
    /**
     * Helper function to read the suspect character and replacement probability distributions from a JSON object
     * @return
     */
    public String loadJSONFromAsset() {
        String json = null;
        try {

            InputStream is = getAssets().open("keyJSON.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
    
    // Implementation of KeyboardViewListener

    /**
     * Manages actual input into the editor. Here we:
     * implement our injection algorithm as required
     * store data relating to the key press
     * initiate any spell checking as required
     */
    public void onKey(int primaryCode, int[] keyCodes) 
    {
        
    	// touches all done, add the chars to the event and then the event to the session    	
    	currentEvent.keyCode=primaryCode;
    	
    	if(errorInjection && primaryCode>=32)
    	{
	    	//give a n% chance of the key being modified
	    	Random r = new Random();
	    	int res = r.nextInt(100);
	    	if (res<=errorInjectionThreshold) //%n chance of key being modified
	    	{
	    		//Log.i("OnKey", "Will modify");
	    		try {
					//for each combination in the model, find the eucleidian distance and the replacement freq
					JSONObject targetObj = suspectReplacementDistribution.getJSONObject(Integer.toString(primaryCode));
					Iterator<?> keys = targetObj.keys();
					ArrayList<Character> list = new ArrayList();
					while (keys.hasNext()) {
					    String key = (String)keys.next();
					    int freq = targetObj.getInt(key);
					    //if the frequency is 0, add the suspect as a replacement candidate
					    double dist = keyModel.distance2(primaryCode, Integer.parseInt(key));				    
			    		
					    if(dist>0) 
					    {
					    	if (dist>2.0) //fix it so that only nearby keys have a chance of being elected
					    		dist=100;
					    	//add to the list of candidates as many times as required if specific freq>0;
						    int sfreq = (int) Math.round(freq/dist);
					    	//Log.i("Test", "Freq/Dist to "+key+": "+freq+"/"+dist+" final prob: "+sfreq);
					    	
					    	if(sfreq==0) //add the suspect as a replacement candidate
					    	{
					    		list.add(Character.toChars(primaryCode)[0]);
					    	}
					    	else //add the other replacement candidates as required
					    	{
						    	for (int x=0; x<targetObj.getInt(key); x++)
						    	{
						    		list.add(Character.toChars(Integer.parseInt(key))[0]);
						    		
						    	}
					    	}
					    }
					}
					//Log.i("OnKey", "Replace list size: "+list.size());
					
					Random x = new Random();
					int sel = x.nextInt(list.size());					
					
					//if the replacement eventually happens
					if((int)list.get(sel)!=primaryCode)
					{
						
						if (errorInjectionSound)
						{
							final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
							tg.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE);
						}
						//primaryCode = (int)list.get(sel);
						
						//Log.w("OnKey", "Replace "+Character.toChars(primaryCode)[0]+" with "+list.get(sel));
						errorMap.put(mComposing.length(), (char)(int)list.get(sel)); //put in our current position and the replacement
						//nInjections++;		
					}
					else
						Log.i("OnKey", "Replacement will not happen, same key selected");
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	else
	    	{
	    		//Log.i("OnKey", "Will not modify, r="+res);
	    	}
    	}
    	//switch adaptxt language if necessary
    	if(coreEngine!=null)
    	{
	    	switch (mInputView.currentKeyboard)
	    	{
	        	case KeyboardViews.QWERTY_EL:
	        		coreEngine.activateLanguageKeymap(131092, null);
	        		coreEngine.setDictionaryPriority(131092, 0);
	        		break;
	        	case KeyboardViews.QWERTY_EN:
	        		coreEngine.activateLanguageKeymap(131081, null);
	        		coreEngine.setDictionaryPriority(131092, 1);
	        		break;
	    	}
    	}
    	
    	//get the full inputted text
    	extr=ic.getExtractedText(new ExtractedTextRequest(), 0);	
    	
    	if(currentEvent!=null && captureData==true)
    	{
	    	
    		//Log.i("OnKey", "OK to capture data!");
    		currentEvent.user=userid;

	    	if(primaryCode>0)
	    		currentEvent.keyChar=(char) primaryCode;

	    	//handle the booleans
	    	if(currentSession.events.get(currentSession.events.size()-1).keyChar==' ') //space
	    	{
	    		currentEvent.followsSpace=true;
	    	}
	    	
	    	if(currentEvent.keyCode==Keyboard.KEYCODE_DELETE )
	    	{
	    		System.out.println("Backspace Pressed!");
	    		   		
	    		//if a delete is pressed after another delete
	    		//and its cursor position is not -1 from the previous delete
	    		//we must commit the previous deletion as a suspect character.
	    		if(currentSession.events.get(currentSession.events.size()-1).keyCode==Keyboard.KEYCODE_DELETE && extr.selectionStart!=lastDeletedPos-1)
	    		{
	    			currentSession.suspects.add(lastDeleted);
	    			//System.out.println("Suspect = "+lastDeleted);
	    		}
	    		
	    		//get all the text before the backspace press and the current cursor position
	    		if (extr.selectionStart>0)
	    		{
		    		lastDeleted=extr.text.charAt(extr.selectionStart-1);
		    		lastDeletedPos=extr.selectionStart;
		    		//System.out.println("Deleted = "+lastDeleted+"\nCursor Position = "+extr.selectionStart);
	    		}	    		
	    	}
	    	
	    	//if the current key is NOT a backspace but the previous one was
	    	if(currentEvent.keyCode!=Keyboard.KEYCODE_DELETE && currentSession.events.get(currentSession.events.size()-1).keyCode==Keyboard.KEYCODE_DELETE)
	    	{
	    		currentSession.suspects.add(lastDeleted);    		
	    		//System.out.println("Suspect = "+lastDeleted);
	    			
	    	}
	    	
    	}
    	
    	//do the handling  	
    	if (isWordSeparator(primaryCode)) {
            // Handle separator
    		//System.out.println("Detected a word separator \""+primaryCode+"\"");
    		if(primaryCode!=32)
    		{
    			shouldInsertSpace=false;
    			if (extr.text.length()>0){
	    			//Log.i("On Key ", "last letter after separator was ["+extr.text.charAt(extr.selectionStart-1)+"]");
	    			//check if the previous char was a space, if so delete it.
	    			if (extr.text.charAt(extr.selectionStart-1)==' ' && !isSpecialSeparator(primaryCode)) //detecting if the current char is not part of a smiley face
	    			{
	    				onKey(-5, null);
	    			}
    			}
    		}
    		
    		//clear the touch history
    		clearDots();
    		
    		//ensure spell checker is using correct language    		
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
    		Editor ed = prefs.edit();
    		if (captureData)
    		{
	    		if(mInputView.currentLang==1) //english
	    		{
	    			ed.putString("available_dicts", "en");
	    			ed.commit();
	    			if(mScs!=null)
	    				mScs.close();
	    			String lang = prefs.getString("available_dicts", "jam");
	        		Log.i("OnKey", "Spellcheck lang set to "+lang);
	    			Locale english = new Locale("en", "GB");
	    			mScs = tsm.newSpellCheckerSession(null, english, this, false);
	    			if(mScs==null)
	    				Log.e("OnKey","Failed to obtain spell-checker session");
	    		}
	    		else
	    		{
	    			ed.putString("available_dicts", "el");
	    			ed.commit();
	    			if(mScs!=null)
	    				mScs.close();
	    			String lang = prefs.getString("available_dicts", "jam");
	        		Log.i("OnKey", "Spellcheck lang set to "+lang);
	    			Locale greek = new Locale("el", "GR");    			
	    			mScs = tsm.newSpellCheckerSession(null, greek, this, false);
	    			if(mScs==null)
	    				Log.e("OnKey","Failed to obtain spell-checker session");
	    		}
    		}
    		
    		//handle space for Adaptxt
    		if (Character.isSpaceChar(primaryCode))
            {	
    			if(coreEngine!=null)
    			{
	            	//Log.i("Handle Character", "Space pressed");
	        		coreEngine.resetCoreString();
	        		//Log.i("Space Pressed", "Word is "+mComposing+" ");
	                coreEngine.insertText(mComposing.toString()+" ");
	        		updateCandidates();
    			}
            }
    		
    		
    		if(!firstWordSet && mComposing.length() > 1)
			{
    			if(captureData)
    				currentSession.firstWord=mComposing.toString();
    			else
    				currentSession.firstWord="$$$$";
    			
    			firstWordSet=true;
    			//System.out.println("First Word\""+mComposing.toString()+"\"");
    		}
    		
    		//effect any injections as required
            if (mComposing.length() > 0) {
                //commitTyped(getCurrentInputConnection());
            	//check the errormap for any replacements 
            	if (errorMap.size()>0)
            	{
            		
            		//restrict the errormap to the 25% of word length cap
      
            		int replacementstodelete = errorMap.size()-(int) Math.round(mComposing.length()*0.25); //total replacements - those to keep
            		if (replacementstodelete<0)
            			replacementstodelete=0;
            		//allow at least one
            		if (errorMap.size()==replacementstodelete)
            			replacementstodelete=errorMap.size()-1;
            		
            		if(replacementstodelete>0)
            		{
	            		List<Integer> keys= new ArrayList<Integer>(errorMap.keySet());
	            		
	            		for (int z=0;z<replacementstodelete;z++)
	            		{
		            		Random random= new Random();	            		
		            		int listposition = random.nextInt(keys.size());
		            		int randomKey = keys.get(listposition);
		            		//remove this from the error map and the list
		            		errorMap.remove(randomKey);
		            		keys.remove(listposition);
		            		
	            		}
            		}
            		
            		//effect the injections
            		String oldmComposing=mComposing.toString();
            		Iterator it = errorMap.entrySet().iterator();
            	    while (it.hasNext()) 
            	    {
            	        Map.Entry pair = (Map.Entry)it.next();            	        
            	        mComposing.replace((Integer) pair.getKey(), (Integer)pair.getKey()+1, ""+(Character)pair.getValue());
            	        //it.remove(); // avoids a ConcurrentModificationException
            	    }            	    
            	    nInjections+=errorMap.size();
            	    currentSession.nInjections=nInjections;
            	    //Log.i("Injections", "Will replace "+oldmComposing+" with "+mComposing+", nInjections="+nInjections);
            	    errorMap.clear();
            	}
            	           	
            	wordSeparatorKeyCode=primaryCode;
            	if (captureData)
            		commitTyped(ic, isWordSeparator(primaryCode));
            	else
            	{
            		if(primaryCode!=Keyboard.KEYCODE_DONE && primaryCode!=10) //done and go/enter
            			handleCharacter(primaryCode, keyCodes);
            		else
            			sendKey(primaryCode);
            		commitTyped(ic);
            	}
            }
            else
            {
            	sendKey(primaryCode);
            }
            
            updateShiftKeyState(getCurrentInputEditorInfo());
            
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
        	if(errorMap.get(mComposing.length()-1)!=null)
        	{
        		//Log.i("Injection", "Delete from map pos="+(mComposing.length()-1)+", char="+errorMap.get(mComposing.length()-1));
        		errorMap.remove(mComposing.length()-1);
        	}
        	
        	handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) { //keyboard hiding button
        	//override this for settings activity
            //handleClose();
        	Intent intent = new Intent(this, LoggingIMESettings.class);
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE && mInputView != null) {
            //Keyboard current = mInputView.getKeyboard();
        	
            if (mInputView.currentKeyboard == KeyboardViews.SYMBOLS || mInputView.currentKeyboard == KeyboardViews.SYMBOLS_SHIFTED) {
                //mInputView.currentKeyboard = KeyboardViews.QWERTY_EN;
            	mInputView.currentKeyboard = lastKeyboardView;
            } else { //about to change to symbols
            	lastKeyboardView = mInputView.currentKeyboard; //keep track of where we came from
                mInputView.currentKeyboard = KeyboardViews.SYMBOLS;
            }
            mInputView.switchKeyboard();
            if (mInputView.currentKeyboard == KeyboardViews.SYMBOLS) {
                mInputView.getKeyboard().setShifted(false);
            }
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    /**
     * Retrieves key map information from AdapTxt core
     */
    public void KPTkeymapInfo()
    {
    	KPTParamKeymapId[] keymapParamIds =coreEngine.getAvailableKeymaps();
    	for (int x=0;x<keymapParamIds.length; x++)
    	{
    		Log.i("KPTKeymapInfo", "Available "+keymapParamIds[x].getLanguage().getLanguage()+
    				", keyMapLangid="+keymapParamIds[x].getLanguageId()+
    				", langId"+keymapParamIds[x].getLanguage().getId());
    	}
    	
    	KPTParamComponentInfo[] components = coreEngine.getAvailableComponents();
    	for (int x=0;x<components.length; x++)
    	{
    		if(components[x].getComponentType()==KPTParamComponentInfo.KPT_COMPONENT_TYPE_DICTIONARY)
    		Log.i("KPTComponentInfo", "Available id="+components[x].getComponentId()+
    				", type="+components[x].getComponentType()+
    				", name="+components[x].getExtraDetails().getDictDisplayName()+
    				", priority="+components[x].getExtraDetails().getDictPriority());
    	}
    	
    }
    

    public void onText(CharSequence text) {
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text, using the AdapTxt core
     */
    private void updateCandidates() {
        
    	if(coreEngine!=null)
    	{
	    	 //get the KPT suggestions
	    	List <KPTSuggestion> suglist = coreEngine.getSuggestions();
	        suggestions.clear();
	    	for (int i=0; i<suglist.size(); i++)
	        {
	        	String sug = suglist.get(i).getsuggestionString();
	        	if(sug!=null)
	        	{
	        		suggestions.add(sug);
	        		//Log.i("Suggestion "+i, sug+"-"+suglist.get(i).getsuggestionType());
	        	}
	        	
	        }
	    	
	    	if (!mCompletionOn) {
	            if (mComposing.length() >= 0) {
	                ArrayList<String> list = new ArrayList<String>();
	                list.add(mComposing.toString());
	                setSuggestions(list, true, true);
	            	setSuggestions(suggestions, true, true);
	            } else {
	                setSuggestions(null, false, false);
	            }
	        }
    	}
    }
    
    /**
     * Update the list of available candidates using the spell-checker (for when a user moves the cursor within a word)
     * @param word the word to pass into the spell checker
     */
   	public void updateCandidatesWithSpellChecker(String word)
    {
    	if (mScs!=null)
    	{
    		origWord=word;
    		mScs.getSuggestions(new TextInfo(word), 5);
    		updateSuggestionList=true;
    	}
    	else //handle this for Samsung devices
    	{
    		origWord=word;
    		//Log.e("UpdateCandidatesWithSpell", "mScs NULL");
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String lang = prefs.getString("available_dicts", "el");
			XmlResourceParser p;
			if (lang.equals("en"))
				p=getResources().getXml(R.xml.qwerty);
			else
				p=getResources().getXml(R.xml.greekqwerty);
			
			
    		SpellForSamsung sp = new SpellForSamsung(getAssets(),p,getFilesDir()+File.separator+"data",lang,5);
    		SuggestionsInfo si[] = new SuggestionsInfo[1];
    		si[0]=sp.spell(word);
    		updateSuggestionList=true;
    		onGetSuggestions(si);    		
    		//Log.e("updateCandsWithSpell", "mScs NULL");
    	}
    }
   	
   	/**
   	 * Pass the suggestions in to the suggestion bar view
   	 * @param corrections
   	 */
   	public void updateSuggestionListWithSpellChecker(List<String> corrections)
   	{
   		corrections.add(0,origWord);
   		suggestions.clear();
   		suggestions=corrections;
   		setSuggestions(corrections, true, true);
   	}
    
   	/**
   	 * draw the suggestions into the suggestion bar
   	 * @param suggestions
   	 * @param completions
   	 * @param typedWordValid
   	 */
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }
    
    /**
     * Handle a press of the backspace key
     */
    private void handleBackspace() {
        
    	coreEngine.removeString(true, 1);
    	
    	final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            ic.setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            ic.commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Handle a press of the shift key
     */
    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        //Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mInputView.currentKeyboard == KeyboardViews.QWERTY_EL || mInputView.currentKeyboard == KeyboardViews.QWERTY_EN) {
            // Alphabet keyboard
        	//Log.i("Handle Shift", "it is pressed");
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (mInputView.currentKeyboard == KeyboardViews.SYMBOLS) {
            mInputView.getKeyboard().setShifted(true);
            mInputView.currentKeyboard=KeyboardViews.SYMBOLS_SHIFTED;
            mInputView.switchKeyboard();
            mInputView.getKeyboard().setShifted(true);
        } else if (mInputView.currentKeyboard == KeyboardViews.SYMBOLS_SHIFTED) {
        	mInputView.getKeyboard().setShifted(false);
        	mInputView.currentKeyboard=KeyboardViews.SYMBOLS;
            mInputView.switchKeyboard();
            mInputView.getKeyboard().setShifted(false);
        }
    }
    
    /**
     * Handle the input of any normal character
     * @param primaryCode the button key code
     * @param keyCodes the characters associated with the button key code
     */
    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
                //Log.i("Handle Character", "it is "+(char)primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            
        	if(coreEngine!=null)
        	coreEngine.addChar((char)primaryCode , false , false, false);
        	
        	mComposing.append((char) primaryCode);
            ic.setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {
        	//Log.i("handleCharacter", "adding "+(char) primaryCode+" to mComposing");
        	mComposing.append((char) primaryCode);
            ic.setComposingText(mComposing, 1);      	
        }
    }

    /**
     * Handle shutting down of the keyboard
     */
    private void handleClose() {
    	//Log.i("Keyboard hiding", ""+System.currentTimeMillis()/1000);
        composition=mComposing.toString();
    	commitTyped(ic);
        requestHideSelf(0);
        mInputView.closing();
    }

    /**
     * Helper to manage the toggling of the caps lock function of the shift key
     */
    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    /**
     * get the word separators
     * @param special
     * @return
     */
    private String getWordSeparators(boolean special) {
        if (special)
        	return mSpecialSeparators;
        else
        	return mWordSeparators;
    }
    
    /**
     * check if a character input is a word separator
     * @param code the input character
     * @return true if it is a word separator, else return false
     */
    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators(false);
        return separators.contains(String.valueOf((char)code));
    }
    
    /**
     * check if a character is a special separator
     * @param code the input character
     * @return true if it is a special word separator, else return false
     */
    public boolean isSpecialSeparator(int code) {
        String separators = getWordSeparators(true);
        return separators.contains(String.valueOf((char)code));
    }

    /**
     * helper to pick the best available suggestion
     */
    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    
    /**
     * Handles the manual selection of suggestions from the suggestion bar
     * @param index the position of the suggestion in the suggestion list
     */
    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            ic.commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() >= 0) {
            
        	String picked;
        	if(!replacemode)
        		picked = suggestions.get(index)+" ";
        	else
        		picked = suggestions.get(index);
        	
        	//Log.i("Suggestion picked 2", picked);
            
        	//ic.commitText(picked, picked.length());
            
            getCurrentInputEditorInfo();
            
            //colour the text according to user preferences
            SpannableString text = new SpannableString(picked);
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if(sharedPrefs.getBoolean("suggestion_highlight", false))
            	text.setSpan(new BackgroundColorSpan(suggestion), 0, picked.length()-1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            currentSession.nSuggestionsPicked++;
            
            if(replacemode)
            {
            	replacemode=false;
            	//remove from list of autocorrected words.
            	//Log.i("PickSuggestion", "Removed "+autocorrected_words.remove(text.toString()));
            	
            	//find the current word
            	extr=ic.getExtractedText(new ExtractedTextRequest(), 0);
            	WordDetails w = findWord(extr.selectionStart, extr.text);
            	//Log.i("FindWord", w.word+", "+w.wordStart+" - "+w.wordEnd);
            	ic.setComposingRegion(w.wordStart, w.wordEnd);
            	text.setSpan(null, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            	/*
            	//clear any spans    	
            	BackgroundColorSpan[] spans=(new SpannableString(extr.text.toString())).getSpans(w.wordStart, w.wordEnd, BackgroundColorSpan.class);
            	for(int i=0; i<spans.length; i++){
            	  text.removeSpan(spans[i]);
            	}*/

            	
            	//commit the update
            	ic.commitText(text, 1);
            	  	
            }
            else
            	ic.commitText(text, picked.length());
            
            coreEngine.resetCoreString();
            coreEngine.insertText(picked);
            mComposing.setLength(0);
            updateCandidates();
        }
    }
    
    /**
	 * initialize the OpenAdaptxt core
	 */
	public void initializeCore() {
		
		if(coreEngine==null)
		{
			//Log.w("Initialize Core","Starting...");
			new Thread(new Runnable() {
				public void run() {
					Looper.prepare();
					
					if(CoreEngineInitialize.initializeCore(getApplicationContext())) 
					{
						coreEngine = CoreEngineInitialize.getCoreInstance();						
						updateCorePrefs();
						//Log.i("KPT Engine core","core initialized");
					}
					else
					{
						//Log.e("KPT Engine core","core NOT initialized");
					}
				}
			}).start();
		}
	}
	
	/**
	 * Set the adaptxt core preferences according to the users' preferences
	 */
	public void updateCorePrefs()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		coreEngine.setErrorCorrection(sharedPrefs.getBoolean("corrections", true));
		coreEngine.setCompletions(sharedPrefs.getBoolean("completions", true));
		coreEngine.setProximitySuggestion(sharedPrefs.getBoolean("proximity", true));	
		coreEngine.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_FORCE_LOWER);
		coreEngine.setMaxSuggestions(Integer.valueOf(sharedPrefs.getString("maxsuggs", "10")));		
	}
	
	/**
	 * Stop the adaptxt core
	 */
	public void destroyCore()
	{
		if(coreEngine != null) {
			coreEngine.destroyCore();
		}
		CoreEngineInitialize.clearCore();
		coreEngine = null;
	}
    
	/**
	 * called when the keyboard view is being hidden
	 */
    @Override
    public void onFinishInputView(boolean finishingInput)
    {
    	if(composition==null)
    		composition=mComposing.toString();
    	currentSession.events.add(new TypingEvent(2, "Keyboard hidden"));
    	
    	//Log.i("onFinishInputView","KEYBOARD DOWN");
        endSession();
        
    	super.onFinishInputView(finishingInput);
    }
    
    
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }
    
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    	//Log.i("OnPress","Pressed "+primaryCode);
    		
    }
    
    /**
     * handle key release
     */
    public void onRelease(int primaryCode) {
    	//Log.i("OnRelease","Released "+primaryCode);
    	if(currentEvent!=null)
    	{
    		currentEvent.timeUp=System.currentTimeMillis();
    		//calculate the time since last event and also the duration of the keypress
	    	if(currentSession.events.size()>1) //at least two events so we can do the calculation
	    	{
	    		currentEvent.calcTimeSinceLast(currentSession.events.get(currentSession.events.size()-1).timeUp);
	    	}	    	
	    	currentEvent.calcDuration();
	    	//add to the session
	    	currentSession.events.add(currentEvent);  	
    	}
    }
    /**
     * handle the stop of the input method service
     */
    @Override public void onDestroy()
    {
    	if (coreEngine!=null)
    	{
	    	coreEngine.resetCoreString();
	    	destroyCore();
    	}
    	super.onDestroy();
    }

    /**
     * handle the receipt of suggestions from the spell checker
     * colour the text in the editor as required
     * pass information to the keyboard view so it can draw the colour bar
     * initiate audio and haptic feedback as required
     */
	@Override
	public void onGetSuggestions(SuggestionsInfo[] results) {
		// TODO Auto-generated method stub
		int colortype=-1;
		final StringBuilder sb = new StringBuilder();
		
		if(updateSuggestionList)
   	 	{
   		 	updateSuggestionList=false;
   		 	ArrayList<String> s = new ArrayList<String>();
   		 	for (int i = 0; i < results.length; ++i)
   		 	{
   		 		final int length = results[i].getSuggestionsCount();
   		 		for (int j = 0; j < length; ++j) {	 
   		 			s.add(results[i].getSuggestionAt(j));
   		 		} 		 		
   		 	}
   		 	updateSuggestionListWithSpellChecker(s);
   	 	}
		else{
		  
	      for (int i = 0; i < results.length; ++i) {
	         // Returned suggestions are contained in SuggestionsInfo
	    	  
	    	 
	         final int len = results[i].getSuggestionsCount();
	         sb.append("Suggestion Attribs: "+results[i].getSuggestionsAttributes());
	         if ((results[i].getSuggestionsAttributes() & SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY)==SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY)
	        	 {
	        	 	sb.append("The word was found in the dictionary\n");
	        	 	mInputView.wordcompletedtype=3;
	        	 }
	         else
	         {
	        	 
	        	 
	        	 if ((results[i].getSuggestionsAttributes() & SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO)==SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO)
	        	 {	
	        		 if ((results[i].getSuggestionsAttributes() & SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS)==SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS)
		        	 {
		        		 colortype=1; //yellow
		        		 mInputView.wordcompletedtype=1;
		        		 sb.append("There are strong candidates for this word\n");
		        		 currentSession.nLowErrors++;
		        	 }
	        		 else
	        		 {
		        		 colortype=2; //red
		        		 mInputView.wordcompletedtype=2;
		        		 sb.append("The word looks like a typo\n");
		        		 currentSession.nHighErrors++;
		        		
	        		 }
	        	 }
	        	 
	         }
        	 
        	 sb.append("\n--These are the suggestions--\n");
	         for (int j = 0; j < len; ++j) {	 
	            sb.append("," + results[i].getSuggestionAt(j));
	         }
	         sb.append(" (" + len + ")");
	      	}
	      	//Log.i("Spelling suggestions", sb.toString());
	   
	  //this comes after a word separator, hence just add 1 to the cursor
	   SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	   
	   SpannableString text = new SpannableString(mComposingTemp);  
	   
	   if(sharedPrefs.getBoolean("highlightwords", true))
	   {
		   switch (colortype)
		   {
			   case 1:
				   text.setSpan(new BackgroundColorSpan(small_err), 0, mComposingTemp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);  
				   break;
			   case 2:
				   text.setSpan(new BackgroundColorSpan(big_err), 0, mComposingTemp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				   break;
			   default:
				   break;
		   }
	   }
	   	
		if(sharedPrefs.getBoolean("autocorrect", true) && mInputView.wordcompletedtype==1) //handle autocorrection
		{
			SpannableString autoc = autocorrect(results);
			autocorrected_words.put(autoc.toString(), text.toString()); //autocorrected word, original input
			//Log.i("Autocorrecting","Key= "+autoc.toString()+", Value= "+text.toString());
			text=autoc;
			if(sharedPrefs.getBoolean("highlightwords", true))
				text.setSpan(new BackgroundColorSpan(autocorrect), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			mInputView.wordcompletedtype=4;
		}
		else //autocorrection is turned off
		{ 
			if(!sharedPrefs.getBoolean("autocorrect", true) && colortype>=1) //a mistake word
			{
				//Log.i("OnGetSentenceSuggestions","Key= "+text.toString()+", Value= "+text.toString());
				//no autocorrects, just put the word in and itself as the replacement
				autocorrected_words.put(text.toString(), text.toString());
			}
		}
		
		if(sharedPrefs.getBoolean("vibrator", false))
		{
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			final int on_time = Integer.parseInt(sharedPrefs.getString("shortvibe", "35"));
			
			switch(mInputView.wordcompletedtype)
			{
			case 1: //small err
				// Vibrate for 300 milliseconds
				v.vibrate(on_time);
				break;
			case 2: //big err
				//v.vibrate(Integer.parseInt(sharedPrefs.getString("longvibe", "300")));
				v.vibrate(new long[]{0, on_time, 200, on_time}, -1);
				break;
			case 4: //autocorr
				v.vibrate(on_time);
				break;
			default:
				break;
				
			}
		}
		
		if(sharedPrefs.getBoolean("audio", false))
		{
			final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
			switch(mInputView.wordcompletedtype)
			{
			case 1: //small err
				tg.startTone(ToneGenerator.TONE_PROP_BEEP);
				break;
			case 2: //big err
				tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
				break;
			case 4: //autocorr
				tg.startTone(ToneGenerator.TONE_PROP_BEEP);
				break;
			default:
				break;
				
			}
		}

		mInputView.invalidateAllKeys();
		ic.commitText(text, 1);
       	sendKey(wordSeparatorKeyCode);
       	coreEngine.resetCoreString();
   		updateCandidates();
		}
		
	}
	
	/**
	 * create a spannable string object that can be coloured from a spell checker suggestion
	 * @param results
	 * @return
	 */
	public SpannableString autocorrect(SuggestionsInfo[] results)
	{
		SpannableString text = new SpannableString(results[0].getSuggestionAt(0));  
		return text;
	}
	
	/**
	 * for passing entire sentences to the spell checker, not used
	 */
	
	@Override
	public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
		// TODO Auto-generated method stub
		Log.i("OnGetSentenceSugs", "Sentence Sugs = "+results.length);
		for (int x=0; x<results.length; x++)
		{
			int sugCount = results[x].getSuggestionsCount();
			Log.i("OnGetSentenceSugs", "Sentence Sugs "+x+ ", SugInfos = "+sugCount);
			for (int z=0; z<sugCount; z++)
			{
				int sugCount2 = results[x].getSuggestionsInfoAt(z).getSuggestionsCount();
				Log.i("OnGetSentenceSugs", "Sentence Sugs "+x+ ", SugInfos = "+sugCount+", Suggestions = "+sugCount2);
				for (int y=0; y<sugCount2; y++)
				{
					Log.i("OnGetSentenceSugs", results[x].getSuggestionsInfoAt(z).getSuggestionAt(y));
				}
			}
		}
		
	}
	
	/**
	 * handle keyboard orientation being changed
	 */
	@Override
	public void onConfigurationChanged (Configuration newConfig)
	{
		/*
		Log.i("Configuration Changed", "Now in mode: "+newConfig.orientation);
		Log.i("Configuration Changed", "FullScreen mode: "+isFullscreenMode());
		Log.i("Configuration Changed", "ExtractView shown: "+isExtractViewShown());
		*/
		//get what's input so far
		try
		{
		   	ExtractedTextRequest req = new ExtractedTextRequest();
		    req.token = 0;
		    req.flags = InputConnection.GET_TEXT_WITH_STYLES;
		    extractedText = ic.getExtractedText(req, 0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		super.onConfigurationChanged(newConfig);
		
	}
	
	
	
}
