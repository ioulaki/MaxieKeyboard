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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import com.strathclyde.oats.R;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.service.textservice.SpellCheckerService;
import android.util.Log;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import uk.ac.shef.wit.simmetrics.*;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

/**
 * Creates the Android SpellChecker service based on the ASpell library
 *
 */
public class ASpellCheckerService extends SpellCheckerService
{

	private static final String DATA = "data";
	private static final String TAG = ASpellCheckerService.class.getSimpleName();
	private static final boolean DBG = true;
	private final static float threshold = 0.75f;
	public static KeyGraph keymodel;
	
	/**
	 * Creates a new spellchecker session object
	 */
	@Override
	public Session createSession()
	{
		//Log.i(TAG, "creating Session");
		// check if the data files are correctly copied from the assets.
		try{
			String dataDir = checkAndUpdateDataFiles();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String lang = prefs.getString("available_dicts", "el");
			Log.i(TAG, "service will use "+lang);
			
			if (lang.equals("en"))
				keymodel = new KeyGraph(getResources().getXml(R.xml.qwerty));
			else
				keymodel = new KeyGraph(getResources().getXml(R.xml.greekqwerty));
		
			return new ASpellCheckerSession(dataDir, lang);
			
			
			
		}catch(IOException e)
		{
			//Log.e(TAG,"Failed to initialize ASpellCheckerService", e);
		}
		
		//Log.i(TAG, "service will use default ");
		return new ASpellCheckerSession(getFilesDir()+File.separator+DATA,"en"); // TODO Find a good way to gracefully fail. This will fail on ASpell initialization.
	}
	
	/**
	 * Checks if the data files for the spell checker are present, if not, installs them
	 * @return path to the spell checker data files
	 * @throws IOException if unable to copy data files to the spellchecker directory
	 */
	private String checkAndUpdateDataFiles() throws IOException
	{
		File dataDir = new File(getFilesDir()+File.separator+DATA);
		
		String []files = dataDir.list();
		if(files==null)
		{
			dataDir.mkdir();
			files = dataDir.list();
		}
		
		if(files!=null && files.length<=1)
		{
			//Log.d(TAG, "Data dir is not available. Creating.");
			
			dataDir.mkdir();
			AssetManager assets = getAssets();
			files = assets.list(DATA);
			for(String file:files)
			{
				String dst = dataDir+File.separator+file;
				try{
					//Log.d(TAG, "Copying "+file+" from assets to "+dst);
					
					FileOutputStream fout = new FileOutputStream(dst);
					InputStream in = assets.open(DATA+File.separator+file);
					
					byte buf[] = new byte[1024];
					int count=0;
					while((count=in.read(buf))!=-1)
					{
						fout.write(buf,0,count);
					}
					in.close();
					fout.close();
				}catch(IOException e){
					throw new IOException("Failed to copy "+file+" to "+dst, e);
				}
			}
		}
		
		String res = getFilesDir()+File.separator+DATA;
		return res;
	}
	
	/**
	 * Class declaration for the Spell Checker session object, with the appropriate locale. 
	 * The session object allows us to retrieve suggestions for a given word.
	 * @author ako2
	 *
	 */
	private static class ASpellCheckerSession extends Session
	{
		private String mLocale;
		private ASpell bridge;
		private String dataDir;
		private String lang;
		
		public ASpellCheckerSession(String dataDir,String lang)
		{
			this.lang = lang;
			this.dataDir = dataDir;
			//Log.i(TAG, "Data: "+dataDir+", lan: "+lang);
		}

		@Override
		public void onCreate()
		{
			
			mLocale = getLocale();
			//Log.d(TAG, "Creating ASpell Speller. DataDir: "+dataDir+" Lang: "+lang);
			bridge = new ASpell(dataDir,lang);
			
		}
		
		@Override
		/**
		 * Callback method to handle retrieved suggestions from the spellchecking library.
		 * Sets the appropriate SuggestionsInfo flag that describes the results
		 * Returns a list of suggestions based on appropriate weighting of the suggestions plus the appropriate flag.
		 */
		public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit)
		{
			mLocale = getLocale();
			String text = textInfo.getText();
			long start = System.currentTimeMillis();
			String []suggestions = bridge.check(text);
			long end = System.currentTimeMillis();
			String code = suggestions[0];
			//Log.d(TAG, "===Suggestion code ==> "+code);

			if(suggestions.length>1) // we have some suggestions
			{
				if(suggestions.length>suggestionsLimit+1)
				{
					String []tmp = new String[suggestionsLimit];
					System.arraycopy(suggestions, 1, tmp, 0, suggestionsLimit);
					suggestions = tmp;
				}
				else  // just keep all suggestions
				{
					String []tmp = new String[suggestions.length-1];
					System.arraycopy(suggestions, 1, tmp, 0, tmp.length);
					suggestions = tmp;
				}
				
			}
			else{
				suggestions = new String[]{};
			}
			
			if (DBG)
			{
				Log.d(TAG, "["+mLocale+"].onGetSuggestions ("+textInfo.getText()+","+suggestionsLimit+"): " + " Code : "+code+". Time to ASPELL: "+(end-start)+" ms.");
			}
			int flags;
			if("1".equals(code))// correct.
			{
				flags = SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY;
			}
			else
			{
				flags = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;
			}
			if(suggestions.length>0) 
			{
				/**
				 * for each suggestion, calculate some similarity metric
				 * order the suggestion list by the metric (best first)
				 * if the metric is over some threshold value then set flags to recommended suggestions
				 * else just set the flag to looks like typo and return any suggestions
				 */
				
				ArrayList<WeightedSuggestion> orderedSuggestions = new ArrayList<WeightedSuggestion>();
				
				for (int i=0;i<suggestions.length; i++)
				{ 
					orderedSuggestions.add(new WeightedSuggestion(text, suggestions[i]));
							
				}
				
				Collections.sort(orderedSuggestions, new Comparator<WeightedSuggestion>() {
				    public int compare(WeightedSuggestion wa, WeightedSuggestion wb) {
				        if (wa.weight>wb.weight)
				        	return -1;
				        else if(wa.weight<wb.weight)
				        	return 1;
				        return 0;
				    	}
				    });
				
				for (int i=0;i<suggestions.length;i++)
				{
					suggestions[i]=orderedSuggestions.get(i).text;
					//Log.i("SpellChecker", "Suggestion "+suggestions[i]+" ("+orderedSuggestions.get(i).weight+")");
				}
				
				if (orderedSuggestions.get(0).weight>=threshold)
					flags |= SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS;
			}
			
			return new SuggestionsInfo(flags, suggestions);
		}
	}
}