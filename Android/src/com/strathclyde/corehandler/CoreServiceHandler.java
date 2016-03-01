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

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.strathclyde.corehandler.KPTLog;
import com.strathclyde.corehandler.CoreService.KPTLocalBinder;

/**
 * CoreServiceHandler provides the functionality to create and handle core engine service 
 * 
 *
 */
public class CoreServiceHandler {

	/**
	 * Service Connection Object
	 */
	private final KPTServiceConnection mServiceConnection;

	/**
	 * Handle to core engine interface
	 */
	private CoreEngine mCoreEngine = null;

	/**
	 * Context for which service connection is made
	 */
	private final Context mComponentContext;

	/**
	 * Singleton core service instance
	 */
	private static CoreServiceHandler mCoreServiceHandler;

	/**
	 * To maintain list of listeners.
	 */
	private final List<KPTCoreServiceListener> mCoreServiceListeners;

	/**
	 * Maintain reference count for singleton instance to release the core service properly
	 */
	private int mReferenceCount;

	/**
	 * Default Constructor
	 */
	private CoreServiceHandler(Context componentContext) {
		mServiceConnection = new KPTServiceConnection();
		mComponentContext = componentContext;
		mCoreServiceListeners = new ArrayList<KPTCoreServiceListener>();
	}

	/**
	 * Gets the singleton instance of core service handler.
	 * @param componentContext Context of the calling component
	 * @return Singleton instance of this class.
	 */
	public static CoreServiceHandler getCoreServiceInstance(Context componentContext) {
		if(mCoreServiceHandler == null) {
			mCoreServiceHandler = new CoreServiceHandler(componentContext);
			if (mCoreServiceHandler.initializeCoreService())
				Log.i("CoreServiceHandler","Service Started");
			else
				Log.e("CoreServiceHandler","Service NOT Started");
		}
		KPTLog.i("CoreServiceHandler ", "CoreServiceHandler.getCoreServiceInstance() mReferenceCount=" + mCoreServiceHandler.mReferenceCount);
		mCoreServiceHandler.mReferenceCount++;
		return mCoreServiceHandler;
	}

	/**
	 * Connects to the local core service
	 * @return If core service is successfully bound
	 */
	private boolean initializeCoreService() {
		KPTLog.e("CoreServiceHandler ", "CoreServiceHandler.initializeCoreService() mReferenceCount=" + mReferenceCount);
		boolean result = mComponentContext.bindService(new Intent(mComponentContext, 
				CoreService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
		return result;
	}

	/**
	 * Registers an observer for receiving notification once
	 * connection with core service is established.
	 */
	public void registerCallback(KPTCoreServiceListener coreServiceListener) {
		if(!mCoreServiceListeners.isEmpty()) {

			// Don't add duplicate listeners if it already exists
			boolean listenerExists = false;
			for(KPTCoreServiceListener coreServiceObserver : mCoreServiceListeners) {
				if(coreServiceObserver == coreServiceListener) {
					listenerExists = true;
					break;
				}
			}
			if(!listenerExists) {
				mCoreServiceListeners.add(coreServiceListener);
			}
		}
		else {
			mCoreServiceListeners.add(coreServiceListener);
		}
	}

	/**
	 * Removes the registered observer from the observer's list. 
	 */
	public boolean unregisterCallback(KPTCoreServiceListener coreServiceListener) {
		if(!mCoreServiceListeners.isEmpty()) {
			boolean result = mCoreServiceListeners.remove(coreServiceListener);
			return result;
		}
		else {
			return false;
		}
	}

	/**
	 * Connects to the local core service
	 * 
	 */
	public CoreEngine getCoreInterface() {
		//Assert.assertNotNull(mCoreEngine);
		return mCoreEngine;
	}

	/**
	 * Connects to the local core service
	 * 
	 */
	public void destroyCoreService() {
		KPTLog.e("KPT Debug", "CoreServiceHandler.destroyCoreService() mReferenceCount=" + mReferenceCount);
		mReferenceCount--;
		if(mComponentContext != null && mReferenceCount == 0) {
			mComponentContext.unbindService(mServiceConnection);
			mCoreServiceHandler = null;
		}
	}

	private class KPTServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName serviceClass, IBinder binder) {
			//Log.i("Core Service Handler", "Service attempt!");
			KPTLocalBinder localBinder = (KPTLocalBinder)binder;
			mCoreEngine = localBinder.getCoreEngineInterface();
			/*if (mCoreEngine==null)
				Log.e("Core Service Handler", "Core is NULL!");*/
			
			// Inform the observers if any observer is registered
			if(!mCoreServiceListeners.isEmpty()) {
				for(KPTCoreServiceListener coreServiceListener : mCoreServiceListeners) {
					coreServiceListener.serviceConnected(mCoreEngine);
					//Log.i("Core Service Handler", "Service connected!");
				}
			}
		}

		public void onServiceDisconnected(ComponentName serviceClass) {
			KPTLog.e("KPT Debug", "CoreServiceHandler.onServiceDisconnected() mReferenceCount=" + mReferenceCount);
			mCoreEngine = null;
		}

	}

	public interface KPTCoreServiceListener {
		/**
		 * Callback to inform observers that connection with service is established.
		 * 
		 * @param coreEngine The fully constructed core engine instance.
		 */
		public void serviceConnected(CoreEngine coreEngine);
	}


}
