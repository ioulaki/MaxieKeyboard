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

import android.content.Context;
import android.util.Log;

import com.strathclyde.corehandler.CoreEngine;
import com.strathclyde.corehandler.CoreServiceHandler;
/**
 * class which connects and interacts with the core
 * @author 
 *
 */
public class CoreEngineInitialize {

	static CoreEngine coreEngine;
	static CoreServiceHandler mCoreServiceHandler;

	/**
	 * initialize the core if it is not yet loaded
	 * @param context
	 * @return true if successfully initialized
	 */
	public static boolean initializeCore(Context context) {
		
		
		mCoreServiceHandler = CoreServiceHandler.
				getCoreServiceInstance(context);
		/*
		if (mCoreServiceHandler== null)
			Log.e("KPT Engine core","service handler NOT OK");
		else
			Log.i("KPT Engine core","service handler OK");
		*/
		coreEngine = mCoreServiceHandler.getCoreInterface();
		
		if (coreEngine != null) {
			//Log.i("KPT Engine core","core OK");
			coreEngine.initializeCore(context);
			return true;
		} 
		//Log.e("KPT Engine core","core NOT OK");
		return false;
	}
	
	public static void initializeCoreService(Context context) {
		mCoreServiceHandler = CoreServiceHandler.
		getCoreServiceInstance(context);
	}

	/**
	 * return the core instance
	 * @return
	 */
	public static CoreEngine getCoreInstance() {
		return coreEngine;
	}

	/**
	 * clear the core while exiting the application
	 */
	public static void clearCore() {
		coreEngine = null;
		mCoreServiceHandler = null;
	}

	public enum KPT_SUGG_STATES {
		KPT_SUGG_SENTENCE_CASE,
		KPT_SUGG_FORCE_UPPER,
		KPT_SUGG_FORCE_LOWER,
		KPT_SUGG_HONOUR_USER_CAPS
	}
}