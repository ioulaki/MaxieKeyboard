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

import com.strathclyde.oats.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Creates the preferences screen for the keyboard
 * @author ako2
 *
 */
public class LoggingIMESettings extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
        ListView v = getListView();
        
        Button export = new Button(this);
        export.setText("Export Data");
        export.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				if(sharedPrefs.getBoolean("prefLocal", false))
				{
					//push db
					Intent i = new Intent(getApplicationContext(), UploadDataReceiver.class);
			        i.putExtra("insert", false);
			        i.putExtra("origin", "manual");
					sendBroadcast(i);
				}
				else
				{
					exportDB();
				}
				
			}
		});
        
        v.addFooterView(export);
		
	}
	
	/**
	 * Exports the database to a local file on the device's external storage
	 */
	private void exportDB()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	       try
	       {
	           File sd = Environment.getExternalStorageDirectory();

	           if (sd.canWrite())
	           {
	        	   SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
	        	   Date now = new Date();
	        	   String fileName = formatter.format(now);
	        	   String savelocation = sharedPrefs.getString("prefUsername", "none")+"_"+fileName+".db";
	        	   boolean truncated=false;
	               String currentDBPath = getDatabasePath("keyboard.db").toString();
	               File currentDB = new File(currentDBPath);
	               File backupDB = new File(sd.getAbsolutePath()+"/LoggingIME/"+savelocation);
	               File backupPath = new File(sd.getAbsolutePath()+"/LoggingIME");
	               
	               /*
	               if(backupPath.mkdirs())
	            	   Log.i("Export", "Dirs created");
	               else
	            	   Log.i("Export", "Dirs NOT created");
	               */
	               
	               //After eclipse Juno update
	               @SuppressWarnings("resource")
	               FileChannel src = new FileInputStream(currentDB).getChannel();
	               @SuppressWarnings("resource")
	               FileChannel dst = new FileOutputStream(backupDB).getChannel();
	               dst.transferFrom(src, 0, src.size());
	               src.close();
	               dst.close();
	               
	               if(sharedPrefs.getBoolean("prefTruncate", true))
	               {
	            	   DBmanager dbm=new DBmanager(this);
	                   dbm.open();
	                   if(dbm.truncate())
	                	   truncated=true;
	                   dbm.close();
	               }
	               
	               //finally notify the media scanner of the new file
	               Intent intent = 
	            		      new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	            		intent.setData(Uri.fromFile(backupDB));
	            		sendBroadcast(intent);
	               
	               Toast.makeText(getBaseContext(), "Exported to "+backupDB.toString()+", truncation: "+truncated, Toast.LENGTH_SHORT).show();
	        
	           }
	       } catch (Exception e)
	       {
	    	   Toast.makeText(getBaseContext(), "Failed to Export Database", Toast.LENGTH_SHORT).show();
	    	   e.printStackTrace();
	       }
	   }
	
}
