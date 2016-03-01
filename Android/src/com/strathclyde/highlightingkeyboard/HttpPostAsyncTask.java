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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * An asynchronous task for posting logged data to a web server
 * @author ako2
 *
 */
public class HttpPostAsyncTask extends AsyncTask<String, Void, Boolean>{

	Context c;
	String uploadFilePath;
	DBmanager dbm;
	File file;
	boolean insert;
	
	/**
	 * Creates the asynchronous task for uploading data
	 * @param dbm the database from which to upload
	 * @param insert set to true for posting to a service that inserts the data to a remote MySQL database, or false for simply sending the database as a .db file
	 * @param con the application context
	 */
	public HttpPostAsyncTask(DBmanager dbm, boolean insert, Context con)
	{
		this.dbm=dbm;
		this.insert=insert;
		this.c=con;
	}
	
	/**
	 * perform the data upload via HTTP POST
	 */
	@Override
	protected Boolean doInBackground(String... params) {
		String filePath = params[0];
		
		//Log.i("AsyncTask", "received file to upload "+filePath);

		String url;
		if(insert)
			url = "http://150.140.15.50/oats/receivedb.php";
		else
			url = "http://150.140.15.50/oats/receivedb_no_insert.php";
		file = new File(filePath);
		try {
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);
			HttpEntity httpEntity = MultipartEntityBuilder.create()
				    .addBinaryBody("file", file, ContentType.create("application/octet-stream"), file.getName())
				    .build();

			post.setEntity(httpEntity);
			HttpResponse response = client.execute(post);
		    
		    if (response.getStatusLine().getStatusCode()==HttpStatus.SC_OK)
		    {
		    	//Log.i("AsyncTask", "Upload OK"+filePath);
		    	String responseString = EntityUtils.toString(response.getEntity());
		    	//Log.i("AsyncTask", responseString);
		    	if (responseString.contains("Done"))
		    	{
		    		return true;
		    	}
		    }
		    else
		    {
		    	//Log.i("AsyncTask", "Upload failed: "+response.getStatusLine().getStatusCode());
		    	//Log.i("AsyncTask", "Upload failed: "+EntityUtils.toString(response.getEntity()));
		    	//Toast.makeText(c, "Upload failed: "+response.getStatusLine().getStatusCode()+ " db saved locally to "+filePath, Toast.LENGTH_SHORT).show();
		    	return false;
		    }

		} catch (Exception e) {
		    // show error
			e.printStackTrace();
			return false;
		}
		
		return false;
      
	}
	
	@Override
	protected void onPostExecute(Boolean inserted) {
		try{
			if (inserted)
			{
				Log.i("AsyncTask", "Upload complete, truncating DB");
				dbm.open();
				dbm.truncate();
				dbm.close();
				//Toast.makeText(c, "Upload OK", Toast.LENGTH_SHORT).show();
				file.delete();
			}
			else
			{
				//Log.i("AsyncTask", "Upload not complete, truncating DB");
				dbm.open();
				dbm.truncate();
				dbm.close();
				//Toast.makeText(c, "Upload failed, db saved locally to "+file.getPath(), Toast.LENGTH_SHORT).show();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
    }

}
