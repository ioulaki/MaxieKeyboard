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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * sets up a process for automatically uploading the data to a web server. It's called by an alarm that is scheduled in the SoftKeyboardService class
 * @author ako2
 *
 */
public class UploadDataReceiver extends BroadcastReceiver {
	Context c;
	
	/**
	 * Checks if the user has enabled the automatic upload mode and we are connected, if so on both accounts, upload the data
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		boolean insert = intent.getBooleanExtra("insert", false);
		String origin = intent.getStringExtra("origin");
		c=context;
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		/*
		 * if the received intent is from the alarm and manual mode is on, do nothing
		 */
		
		if (!sharedPrefs.getBoolean("automode", false) && origin.equals("alarm"))
		{
			//Log.i("Alarm fired", "origin from alarm, but we are in manual mode");
		}
		else
		{
			
			//Log.i("Alarm fired", "time: "+System.currentTimeMillis());
			if(isNetworkAvailable(context))
			{
				//Log.i("Alarm event", "Network Available");
				DBmanager dbm = new DBmanager(context);
				if (dbm.hasSessions())
				{
					dbm.saveUserDetails();
					//save the file
					String fileToUpload=exportDB(context);
					//send the file to the server - depending on the results, the asynctask will truncate db on success.
					HttpPostAsyncTask task = new HttpPostAsyncTask(dbm, insert, c);
					task.execute(fileToUpload);
				}
				else
				{
					//Toast.makeText(c, "Nothing to upload: No sessions in DB", Toast.LENGTH_SHORT).show();
					//Log.i("Alarm event", "No sessions in DB");
				}
			}
			else
			{
				//Log.i("Alarm event", "Network Not Available");
				//Toast.makeText(c, "Can't upload: No Network. Try saving locally!", Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Checks if the device is connected to the Internet via Wi-Fi
	 * @param c the application context
	 * @return true if connected via Wi-Fi, else returns false
	 */
	private boolean isNetworkAvailable(Context c) {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();

	}
	
	/**
	 * Save database to a file
	 * @param c the application context
	 * @return the path to which the database file was saved
	 */
	private String exportDB(Context c)
	{
	   SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(c);
	   try
	   {
	       File sd = Environment.getExternalStorageDirectory();
	       if (sd.canWrite())
	       {
	    	   SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
	    	   Date now = new Date();
	    	   String fileName = formatter.format(now);
	    	   String savelocation = sharedPrefs.getString("prefUsername", "none")+"_"+fileName+".db";
	           String currentDBPath = c.getDatabasePath("keyboard.db").toString();
	           File currentDB = new File(currentDBPath);
	           File backupDB = new File(sd.getAbsolutePath()+"/LoggingIME/"+savelocation);
	           File backupPath = new File(sd.getAbsolutePath()+"/LoggingIME");
	           /*
	           if(backupPath.mkdirs())
	        	   Log.i("Export", "Dirs created");
	           else
	        	   Log.i("Export", "Dirs NOT created");
	           */
	           
	           @SuppressWarnings("resource")
	           FileChannel src = new FileInputStream(currentDB).getChannel();
	           @SuppressWarnings("resource")
	           FileChannel dst = new FileOutputStream(backupDB).getChannel();
	           dst.transferFrom(src, 0, src.size());
	           src.close();
	           dst.close();
	       
	           //finally notify the media scanner of the new file
	           Intent intent = 
	        		      new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	        		intent.setData(Uri.fromFile(backupDB));
	        		c.sendBroadcast(intent);
	           
	           //Log.i("Alarm event", "Exported to "+backupDB.toString());
	           return backupDB.toString();
	    
	       }
	   } catch (Exception e)
	   {
		   //Log.i("Alarm event", "Failed to export db");
		   e.printStackTrace();
	   }
	return null;
	 }
	
	
}
