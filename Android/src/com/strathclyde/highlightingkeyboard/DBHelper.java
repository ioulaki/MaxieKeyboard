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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates the necessary database structures for data logging
 * @author ako2
 *
 */
public class DBHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 6;
    
	
    DBHelper(Context context) {
        super(context, "keyboard.db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    	
        db.execSQL(
                "CREATE TABLE sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "user TEXT, "+
                "session_width INTEGER, "+
                "session_height INTEGER, "+
                "start_time TEXT, " +
                "end_time TEXT, " +
                "app TEXT, " +
                "low_errors INTEGER, " +
                "high_errors INTEGER, " +
                "suggestions_picked INTEGER, " +
                "injections INTEGER, " +
                "first_word TEXT, " +
                "autocorrect INTEGER, " +
                "sound INTEGER, " +
                "haptic INTEGER, " +
                "visual INTEGER, " +
                "sugg_highlight INTEGER, " +
                "dots INTEGER);");
       
        db.execSQL(
                "CREATE TABLE typingevents (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "sessionid INTEGER, "+
                "user TEXT, "+
                "timesincelast INTEGER, " +
                "duration INTEGER, " +
                "rawxdiff REAL, " +
                "rawydiff REAL, " +             
                "xdiff REAL, " +
                "ydiff REAL, " +
                "keycode INTEGER, " +
                "keychar TEXT, " +
                "followspace NUMERIC, " +
                "precedespace NUMERIC, " +
                "followshift NUMERIC);");
        
        db.execSQL(
                "CREATE TABLE suspects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "user TEXT, "+
                "sessionid INTEGER, " +
                "suspect TEXT);");
        
        db.execSQL(
                "CREATE TABLE user (" +
                "user TEXT PRIMARY KEY, "+
                "age INTEGER, "+
                "sex INTEGER, " +
                "country INTEGER, " +
                "code TEXT);");
        
        
        //System.out.println("Database created successfully");
        
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
}