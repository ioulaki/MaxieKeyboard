/******************************************************************************
 * Based on code provided as a Copyright 2011 KeyPoint Technologies (UK) Ltd.   
 * All rights reserved. This program and the accompanying materials   
 * are made available under the terms of the Eclipse Public License v1.0  
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html

 *           
 * Contributors: 
 * KeyPoint Technologies (UK) Ltd - Initial API and implementation
 * Andreas Komninos, University of Strathclyde - Additional code implementation
 * http://www.komninos.info
 * http://mobiquitous.cis.strath.ac.uk
 *****************************************************************************/

package com.strathclyde.corehandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 * This class provides controlled logging functionality.
 * This a wrapper on top of android's system log class.
 * In the release build all the following logs must be disabled.
 * Should be enabled only for development purposes.
 * @author 
 *
 */
public class KPTLog {
	public static ArrayList<String> logArrrayList;

	/**
	 * Sends a error log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void e(String tag, String msg) {
		if(logArrrayList == null)
		{
			logArrrayList = new ArrayList<String>();
		}
		addToLog(tag,msg);
		Log.e(tag, msg);
	}

	private static void addToLog(String tag, String msg) {
		Message message = new Message();
		Bundle bundle = new Bundle();
		bundle.putString("tag", tag);
		bundle.putString("msg", msg);
		message.setData(bundle);
		mHandler.sendMessage(message);
	}

	/**
	 * Sends a information log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void i(String tag, String msg) {
		Log.i(tag, msg);
	}

	public static ArrayList<String> displayLogData(){
		return logArrrayList;
	}

	static Handler mHandler = new Handler()
	{
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String date = convertMilliSecondsToDate(System.currentTimeMillis(), "MM/dd/yyyy hh:mm:ss a");
			Bundle bundle = msg.getData();
			logArrrayList.add(date +" : "+bundle.getString("tag") +" : "+bundle.getString("msg") +"\n\n");
		}

		/**
		 * Return date in specified format.
		 * @param milliSeconds Date in milliseconds
		 * @param dateFormat Date format 
		 * @return String representing date in specified format
		 */
		public String convertMilliSecondsToDate(long milliSeconds, String dateFormat)
		{
			DateFormat formatter = new SimpleDateFormat(dateFormat);
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(milliSeconds);
			return "[ " + formatter.format(calendar.getTime()) +" ]";
		}
	};
}