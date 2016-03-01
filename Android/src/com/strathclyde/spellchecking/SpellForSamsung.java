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

package com.strathclyde.spellchecking;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.xmlpull.v1.XmlPullParser;

import com.strathclyde.oats.R;

import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import android.util.Log;
import android.view.textservice.SuggestionsInfo;
import android.content.Context;
import android.content.ContextWrapper;

/**
 * A version of the spellchecking code for Samsung phones, which do not support the installation and management of 3rd party 
 * spell checkers.
 * @author ako2
 *
 */
public class SpellForSamsung {

	private ASpell bridge;
	private int suggestionsLimit;
	private String locale;
	private final static float threshold = 0.75f;
	private String datadir;
	private AssetManager am;
	protected static KeyGraph keymodel;

	
	private static final String TAG = "SamsungSpell";
	
	public SpellForSamsung(AssetManager c, XmlResourceParser parser, String datadir, String locale, int suggestionsLimit)
	{
		this.suggestionsLimit=suggestionsLimit;
		 this.locale=locale;
		 this.datadir=datadir;
		 this.am=c;
		try {
			datadir=checkAndUpdateDataFiles();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		bridge = new ASpell(datadir, locale);
		keymodel = new KeyGraph(parser);
			
		 
	}
	
	public SuggestionsInfo spell(String word)
	{
		String[] suggestions = bridge.check(word);
		String code = suggestions[0];
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
		
		//Log.d(TAG, "["+locale+"] ("+word+","+suggestionsLimit+"): " + " Code : "+code);
		
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
				orderedSuggestions.add(new WeightedSuggestion(word, suggestions[i]));
						
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
	
	private String checkAndUpdateDataFiles() throws IOException
	{
		
		File dataDir = new File(datadir);
		
		String []files = dataDir.list();

		if(files==null)
		{
			//Log.d(TAG, "Data dir is not available. Creating.");
			
			dataDir.mkdir();
			files = am.list("data");
			for(String file:files)
			{
				String dst = dataDir+File.separator+file;
				try{
					//Log.d(TAG, "Copying "+file+" from assets to "+dst);
					
					FileOutputStream fout = new FileOutputStream(dst);
					InputStream in = am.open("data"+File.separator+file);
					
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
		
		String res = datadir;
		/*for(String file:files)
		{
			Log.i("AspellChecker", file+" ["+res+"]");
		}
		*/
		return res;
	}
}
	
	
	
