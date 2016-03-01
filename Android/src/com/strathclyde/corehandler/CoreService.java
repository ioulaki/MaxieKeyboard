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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * CoreService The android service class through which adaptxt
 * core engine functionalities re provided. 
 * 
 *
 */

public class CoreService extends Service {

	/**
	 * Local binder used by IMS and Home Screen
	 */
	private KPTLocalBinder mLocalBinder;

	/**
	 * Core Engine handle
	 */
	private CoreEngine mCoreEngineImpl;

	/**
	 * Creates and initializes binders
	 * 
	 */
	@Override
	public void onCreate() {
		mLocalBinder = new KPTLocalBinder();
		mCoreEngineImpl = CoreEngine.getCoreEngineImpl();
		//Log.i("CoreService","On Create done");
	} 

	/**
	 * returns binder objects based on the intents received
	 */
	@Override
	public IBinder onBind(Intent clientIntent) {
		mCoreEngineImpl.prepareCoreFiles(getFilesDir().getAbsolutePath(), getAssets());
		return mLocalBinder;
	}

	/**
	 * Destroys the binder resources
	 */
	@Override
	public void onDestroy() {
		//KPTLog.e("KPT Debug", "CoreService onDestroy() calling forceDestroyCore");
		mCoreEngineImpl.forceDestroyCore();
	}

	public class KPTLocalBinder extends Binder {

		/**
		 * Core Engine reference
		 */
		private CoreEngine coreEngine = null;

		/**
		 * Initializes and gets core engine object.
		 * 
		 */
		public CoreEngine getCoreEngineInterface() {
			coreEngine = CoreEngine.getCoreEngineImpl();
			/*
			if(coreEngine!=null)
				Log.i("CoreService","CoreEngine created");
			else
				Log.e("CoreService","CoreEngine Not created");
				*/
			return coreEngine;
		}
	}
}


