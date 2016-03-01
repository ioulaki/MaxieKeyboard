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

import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A helper object for database operations 
 * @author ako2
 *
 */
public class DBmanager {
	
	private DBHelper dbhelper;
	private SQLiteDatabase database;
	Context c;

	public DBmanager(Context c)
	{	
		dbhelper = new DBHelper(c);
		this.c=c;
	}
	
	  public void open() throws SQLException {
	    database = dbhelper.getWritableDatabase();
	  }

	  public void close() {
	    dbhelper.close();
	  }
	  
	  /**
	   * inserts data from a typing session into the database
	   * @param ts the typing session object
	   */
	  public void insert(TypingSession ts)
	  {
		  ContentValues cv = new ContentValues();
		  cv.put("start_time", ts.start_time);
		  cv.put("end_time", ts.end_time);
		  cv.put("first_word", ts.firstWord);
		  cv.put("session_height", ts.sess_height);
		  cv.put("session_width", ts.sess_width);
		  cv.put("low_Errors", ts.nLowErrors);
		  cv.put("high_errors", ts.nHighErrors);
		  cv.put("suggestions_picked", ts.nSuggestionsPicked);
		  cv.put("injections", ts.nInjections);
		  cv.put("app", ts.app);
		  cv.put("user", ts.user);
		  cv.put("autocorrect", ts.autocorrect);
		  cv.put("sound", ts.sound);
		  cv.put("haptic", ts.haptic);
		  cv.put("visual", ts.visual);
		  cv.put("sugg_highlight", ts.sugg_highlight);
		  cv.put("dots", ts.dots);
		  long insertId = database.insert("sessions", null, cv);
		  
		  int counter=0;
		  	for (TypingEvent i : ts.events)
			{
		  		cv = new ContentValues();
		  		cv.put("sessionid", insertId);
		  		cv.put("timesincelast", i.timeSinceLast);
		  		cv.put("duration", i.duration);
				cv.put("rawxdiff", (i.rawxDown-i.rawxUp));
				cv.put("rawydiff", (i.rawyDown-i.rawyUp));
				cv.put("xdiff", (i.xDown-i.xUp));
				cv.put("ydiff", (i.yDown-i.yUp));				
				cv.put("keycode", i.keyCode);
				cv.put("keychar", ""+i.keyChar);
				cv.put("followspace", i.followsSpace);
				cv.put("precedespace", i.precedesSpace);
				cv.put("followshift", i.followsShift);
				cv.put("user", i.user);
				database.insert("typingevents", null, cv);
				counter++;
			}
		  	//Log.i("Database", "Inserted "+counter+" typing events");
			
			for (Character c : ts.suspects)
			{
				cv = new ContentValues();
		  		cv.put("sessionid", insertId);
		  		cv.put("suspect", ""+c);
		  		cv.put("user", ts.user);
		  		database.insert("suspects", null, cv);
			}

	  }
	  
	  /**
	   * empties the database tables
	   * @return
	   */
	  public boolean truncate()
	  {
		  try
		  {
			  database.execSQL("DELETE FROM sessions"); 
			  database.execSQL("DELETE FROM typingevents");
			  database.execSQL("DELETE FROM suspects");
			  database.execSQL("DELETE FROM user");
			  database.execSQL("VACUUM");
			  return true;
		  }
		  catch(Exception e)
		  {
			  return false;
		  }
		 
	  }
	  
	  /**
	   * checks if the database has recorded any typing sessions
	   * @return true if there are sessions recorded in the database, otherwise false
	   */
	  public boolean hasSessions()
	  {
		  //open database
		  open();
		  Cursor c = database.query(false, "sessions", null, null, null, null, null, null, null, null);
		  if (c.getCount()==0)
		  {
			  close();
			  return false;
		  }
		  else
		  {
			  close();
			  return true;
		  }
	  }
	  
	  /**
	   * saves the user's basic details in the database
	   */
	  public void saveUserDetails()
	  {
		  	open();
		  	database.execSQL("DELETE FROM user");		  	
		  	
		  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);	  	
		  	/*
		  	Log.i("DBManager","About to set user to "+sp.getString("prefUsername", "-1")+
		  			","+sp.getInt("age", -1)+
		  			","+sp.getInt("sex", -1)+
		  			","+sp.getInt("country", -1)+
		  			","+sp.getString("code", "-1"));
		  	*/
		  	ContentValues cv = new ContentValues();
	  		cv.put("user", sp.getString("prefUsername", "-1"));
	  		cv.put("age", sp.getInt("age", -1));
	  		cv.put("sex", sp.getInt("sex", -1));
			cv.put("country", sp.getInt("country", -1));
			cv.put("code", sp.getString("code", "-1"));			
			database.insert("user", null, cv);
			close();
	  }

}
