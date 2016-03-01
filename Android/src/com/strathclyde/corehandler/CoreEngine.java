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

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.kpt.adaptxt.core.coreapi.KPTCommands.KPTCmd;
import com.kpt.adaptxt.core.coreapi.KPTCore;
import com.kpt.adaptxt.core.coreapi.KPTFrameWork;
import com.kpt.adaptxt.core.coreapi.KPTLanguage;
import com.kpt.adaptxt.core.coreapi.KPTPackage;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentsOperations;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;
import com.kpt.adaptxt.core.coreapi.KPTParamInputCursor;
import com.kpt.adaptxt.core.coreapi.KPTParamInputInsertion;
import com.kpt.adaptxt.core.coreapi.KPTParamInputResetRemoveReplace;
import com.kpt.adaptxt.core.coreapi.KPTParamKeyMapOperations;
import com.kpt.adaptxt.core.coreapi.KPTParamKeymapId;
import com.kpt.adaptxt.core.coreapi.KPTParamPackageInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamSuggestion;
import com.kpt.adaptxt.core.coreapi.KPTParamSuggestionConfig;
import com.kpt.adaptxt.core.coreapi.KPTSuggEntry;
import com.kpt.adaptxt.core.coreapi.KPTTypes.KPTStatusCode;
import com.strathclyde.corehandler.CopyAssets;
import com.strathclyde.corehandler.KPTLog;
import com.strathclyde.corehandler.CoreEngineInitialize.KPT_SUGG_STATES;

/**
 * CoreEngine handles the Core engine object from JNI layer. Also provides
 * singleton reference to core engine handle.
 * 
 * @author 
 *
 */
public class CoreEngine {

	public static final int PACKAGE_ALREADY_EXISTS = -1;

	/**
	 * The JNI core engine handle using which all Core engine function calls are made
	 */
	private KPTCore  mAdaptxtCore;

	/**
	 * Static reference to this class to provide single instance across binders.
	 */
	private static CoreEngine sCoreEngine = null;

	/**
	 * Static counter to keep track of instances and releasing core engine framework on
	 * destruction of last instance.
	 */
	private static int mReferenceCount = 0;

	private String apkDirPath = null;

	/**
	 * Blacklisting flag is set by IME based on editor
	 */
	//private volatile boolean mblisted = true;

	/**
	 * Set by package installer to put core in maintenance mode
	 */
	//private volatile boolean mIsCoreMaintanenceMode = true;

	/**
	 * This flag is set to true only after a user is properly loaded.
	 */
	private volatile boolean mIsCoreUserLoaded = false;

	public int mPrefixLength =0 ;
	public int mSuffixLength = 0;


	/* Reverted String*/
	@SuppressWarnings("unused")
	private String mRevertedWord = "";

	/* Flag for maintaining auto Correction is on/off */
	//private boolean mAutoCorrection = false;

	/* Maintained globally as it is used for inserting suggestions.*/
	private KPTParamSuggestion mGetSuggs = null;

	/**
	 * Global suggestion status code value
	 */
	private KPTStatusCode mSuggStatusCode;

	private static String KPTDebugString = "CoreEngine";

	//private Context mApplicationContext;
	
	/**
	 * Default constructor
	 */
	CoreEngine() {
	}

	/**
	 * Static getter function to provide single instance to all binders.
	 */
	public static CoreEngine getCoreEngineImpl() {
		if (sCoreEngine == null) {
			sCoreEngine = new CoreEngine();
		}
		return sCoreEngine;
	}

	/**
	 * Copies required core files from assets to the profile folder.
	 * @param filePath
	 * @param assetMgr
	 */
	public void prepareCoreFiles(String filePath, AssetManager assetMgr) {
		apkDirPath = filePath +"/Profile" ;
		CopyAssets.atxAssestCopy(filePath, assetMgr);
		apkDirPath = apkDirPath + "/Profile";
	}

	/**
	 * Initializes handle to the core.
	 * 
	 */
	public boolean initializeCore(Context clientContext) {
		KPTLog.e(KPTDebugString, "initializeCore mReferenceCount--before " + mReferenceCount);
		/* We are synchronizing the current instance in this function to make that the Core initialization
		 * processing of one thread doesn't interfere with a possible Core destruction processing of
		 * another thread. Currently this issue was identified during system locale change which initiates
		 * the above conflict between KPTLocaleHandlerService and KPTPackageInstallerService.
		 */
		synchronized (this) {
			if (mReferenceCount == 0) {
				//mApplicationContext = clientContext;
				mAdaptxtCore = new KPTCore();
				// Creating framework
				Log.e("CoreEngine", apkDirPath+"****");
				KPTStatusCode statuscode = mAdaptxtCore.KPTFwkCreate(0, 1,
						apkDirPath, true);
				if (statuscode == KPTStatusCode.KPT_SC_SUCCESS
						|| statuscode == KPTStatusCode.KPT_SC_ALREADYEXISTS) {
					KPTFrameWork fwk = new KPTFrameWork();
					mAdaptxtCore.KPTFwkGetVersion(fwk);
					KPTLog.e(KPTDebugString, "===>> Framework create success "
							+ fwk.getVersionMajor() + fwk.getVersionMinor());
				} else {
					KPTLog.e(KPTDebugString, "===>> core create failed------>** "+ statuscode);
				}
			}
			mReferenceCount++;
			//KPTLog.e(KPTDebugString, "mReferenceCount--after "+ mReferenceCount);
		}
		return true;
	}

	public KPTCore getKPTCore() {
		if(mAdaptxtCore == null) {
			mAdaptxtCore = new KPTCore();
		}
		return mAdaptxtCore;
	}

	public boolean resetCoreString() {
		KPTParamInputResetRemoveReplace resetString = new KPTParamInputResetRemoveReplace(1);
		resetString.setResetInfo(0, "");
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_RESET, resetString);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_RESET status: " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;
		return false;
	}

	
	public boolean replaceString(int startIndex, int endIndex, String replacedString) {
		
		KPTStatusCode statuscode;
		KPTParamInputResetRemoveReplace replaceString = new KPTParamInputResetRemoveReplace(1);

		replaceString.setReplaceInfo(startIndex, endIndex, replacedString, replacedString.length());

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_REPLACECONTENTS, replaceString);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;	        
		return false;
	}
	public boolean setAbsoluteCaretPosition(int cursorPos) {
		KPTParamInputCursor cursorPosition = new KPTParamInputCursor(1);
		cursorPosition.setOffset(cursorPos);
		cursorPosition.setMovementType(KPTParamInputCursor.KPT_SEEK_START);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_MOVECURSOR, cursorPosition);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_MOVECURSOR cursor pos " + cursorPos + " status: " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;
		return false;
	}
	
	public int getAbsoluteCaretPosition() {
		KPTParamInputCursor cursorPosition = new KPTParamInputCursor(1);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETCURSOR, cursorPosition);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_GETCURSOR " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return cursorPosition.getCursorPos();
		return -1;
	}

	
	/**
	 * Activates current top priority dictionary keymap
	 * @return Setting keymap is successful.
	 */

	public boolean activateTopPriorityDictionaryKeymap() {
		KPTParamDictionary activeDictionary = CoreDictionary.getTopPriorityDictionary();
		if(activeDictionary != null) {
			return activateLanguageKeymap(activeDictionary.getComponentId(), activeDictionary);
		}
		return false;
	}

	public boolean removeString(boolean beforeCursor, int positions) {
		KPTStatusCode statuscode;
		KPTParamInputResetRemoveReplace removeString = new KPTParamInputResetRemoveReplace(1);
		if(beforeCursor) {
			removeString.setRemoveInfo(positions, 0);
		} else {
			removeString.setRemoveInfo(0, positions);
		}
		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_REMOVE, removeString);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_REMOVE before-cursor: " + beforeCursor + " count: " + positions + "status: " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;	        
		return false;
	}

	public void addChar(char c, boolean isPrevSpace, boolean justAddedAutoSpace, boolean isAccVisible)
	{
			KPTParamInputInsertion insertChar = new KPTParamInputInsertion(1);
			//insertChar.setInsertChar(c, KPTParamInputInsertion.KPT_INSERT_AMBIGUOUS, 0);
			insertChar.setInsertChar(c, 0, 0);
			KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTCHAR, insertChar);
			//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_INSERTCHAR " + statuscode);

	}



	public boolean insertText(String text) {
		/*if(mblisted || mIsCoreMaintanenceMode){
			return false;
		}*/
		KPTParamInputInsertion insertString = new KPTParamInputInsertion(1);
		insertString.setInsertString(text, text.length(), 0, 0, 0, null);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTSTRING, insertString);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_INSERTSTRING text: " + text+ "status: " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		return false;
	}

	public void setErrorCorrection(boolean errorCorrect) {
		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_ERROR_CORRECTION;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setErrorCorrectionOn(errorCorrect);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config);
		//KPTLog.e(KPTDebugString, "------------>KPTCMD_SUGGS_SETCONFIG " + statuscode);
		
	}
	
	public void setCompletions(boolean completions) {
		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_COMPLETION;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setCompletionOn(completions);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config);
		//KPTLog.e(KPTDebugString, "------------>KPTCMD_SUGGS_SETCONFIG " + statuscode);
		
	}
	public void setProximitySuggestion(boolean proximity) {
		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_PROXIMITY_SUGGESTION;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setproximitySuggestionOn(proximity);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config);
		//KPTLog.e(KPTDebugString, "------------>KPTCMD_SUGGS_SETCONFIG " + statuscode);
		
	}


	public void setMaxSuggestions(int maxSugg) {

		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_MAX_SUGGESTIONS;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(KPTCmd.KPTCMD_SUGGS.getBitNumber());
		config.setFieldMasktemp(maskAll);
		config.setMaxNumSuggestions(maxSugg);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config);
		//KPTLog.e(KPTDebugString, "KPTCMD_SUGGS_SETCONFIG max sugg cnt " + maxSugg + " status: " + statuscode);
	}
	
	/**
	 * Setting Caps States to get Suggestions according to mode.
	 */

	public void setCapsStates(KPT_SUGG_STATES state) {
		KPTLog.e(KPTDebugString,"Caps states: " + state);
		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_FORCE_LOWER |
		KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_FORCE_LOWER |
		KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_USER_CAPS |
		KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_SENTENCE_CASE |
		KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_CAP_NEXT;

		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setForceUpper(false);
		config.setForceLower(false);
		config.setHonourUserCaps(false);
		config.setUseSentenceCase(false);
		switch(state) {
		case KPT_SUGG_SENTENCE_CASE:
			config.setUseSentenceCase(true);
			break;
		case KPT_SUGG_FORCE_UPPER:
			config.setCapNext(false);
			config.setForceUpper(true);
			break;
		case KPT_SUGG_FORCE_LOWER:
			config.setCapNext(false);
			config.setHonourUserCaps(true);
			break;
		case KPT_SUGG_HONOUR_USER_CAPS:
			config.setHonourUserCaps(true);
			config.setCapNext(true);
			break;
		}
		
		KPTParamSuggestionConfig retConfig = new KPTParamSuggestionConfig(1);

		mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config, retConfig);
	}

	public List<KPTSuggestion> getSuggestions() {
		setRevertedWord("");
		mGetSuggs  = new KPTParamSuggestion(1, 0);
		mSuggStatusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_GETSUGGESTIONS, mGetSuggs);
		//KPTLog.e(KPTDebugString, "KPTCMD_SUGGS_GETSUGGESTIONS Cnt: " + mGetSuggs.getCount() + "status: " + mSuggStatusCode);
		List<KPTSuggestion> sugg = null;

		if(mSuggStatusCode == KPTStatusCode.KPT_SC_SUCCESS)
		{
			sugg = new ArrayList<KPTSuggestion>();
			int count = mGetSuggs.getSuggestionEntries().length;
			KPTSuggEntry[] entry = mGetSuggs.getSuggestionEntries();
			KPTSuggestion suggestion;
			for(int i=0; i< count ; i++)
			{
				suggestion = new KPTSuggestion();
				suggestion.setsuggestionString(entry[i].getSuggestionString());
				suggestion.setsuggestionType(entry[i].getSuggestionType());
					sugg.add(suggestion);
			}
		}
		return sugg;		
	}	

	/**
	 * Gets available addon atp packages in default package folder. "/profile/package".
	 * @return packages list
	 */
	public KPTPackage[] getAvailablePackages() {
		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_GETAVAILABLE, pkgInfo);
		//KPTLog.e(KPTDebugString, "KPTCMD_PACKAGE_GETAVAILABLE " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return pkgInfo.getAvailPackages();
		}
		else {
			return null;
		}
	}

	/**
	 * Gets installed addon atp packages in core.
	 * @return packages list
	 */
	public KPTPackage[] getInstalledPackages() {
		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_GETINSTALLED, pkgInfo);
		//KPTLog.e(KPTDebugString, "getInstalledPackages KPTCMD_PACKAGE_GETINSTALLED " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return pkgInfo.getInstalledPackages();
		}
		else {
			return null;
		}
	}

	/**
	 * Installs supplied package from default package folder.
	 * @param packageName Package name to be installed.
	 * @return Installed packages's id
	 */

	public int installAddOnPackage(String packageName) {

		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());
		pkgInfo.setPkgNameToInstall(packageName);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_INSTALL, pkgInfo);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			//KPTLog.e(KPTDebugString, "KPTCMD_PACKAGE_INSTALL success statuscode = " + statuscode);
			int installedPkgId = pkgInfo.getPkgIdInstalled();
			return installedPkgId;
		}
		else if(statuscode == KPTStatusCode.KPT_SC_ALREADYEXISTS) {
			//KPTLog.e(KPTDebugString, "KPTCMD_PACKAGE_INSTALL already exists-success statuscode= " + statuscode);
			return PACKAGE_ALREADY_EXISTS;
		}
		else {
			KPTLog.e(KPTDebugString, "KPTCMD_PACKAGE_INSTALL failed statuscode = " + statuscode);
			return -2;
		}
	}

	/**
	 * Uninstalls a package from core
	 * @param packageId package Id to uninstall
	 * @return Success or Failure
	 */

	public boolean uninstallAddonPackage(int packageId) {

		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());
		pkgInfo.setPkgIdToUninstall(packageId);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_UNINSTALL, pkgInfo);
		//KPTLog.e(KPTDebugString, "KPTCMD_PACKAGE_UNINSTALL status: " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		else {
			KPTLog.e(KPTDebugString, "UninstallAddonPackage failed statuscode = " + statuscode);
			return false;
		}
	}


	/**
	 * Get available keymaps in core
	 * @return Available Keymap id list
	 */

	public KPTParamKeymapId[] getAvailableKeymaps() {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETAVAILABLE, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETAVAILABLE status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success, return available keymap id list
			return keymapOps.getAvailKeymapIds();
		} else {
			// Error, return empty keymap id list
			return new KPTParamKeymapId[0];
		}
	}

	/**
	 * Get opened keymaps list in core.
	 * @return Opened keymap list
	 */

	public KPTParamKeymapId[] getOpenKeymaps() {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETOPEN, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETOPEN status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success, return open keymap
			return keymapOps.getKeymapIdsOpen();
		} else {
			// error, return empty list
			return new KPTParamKeymapId[0];
		}
	}

	/**
	 * Get current active keymap in core.
	 * @return Active keymap
	 */

	public KPTParamKeymapId[] getActiveKeymap() {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETACTIVE, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETACTIVE status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success, return active keymap
			return keymapOps.getIdsActive();
		} else {
			// error, return empty list
			return new KPTParamKeymapId[0];
		}
	}

	/**
	 * Opens and loads a keymap layout in core.
	 * @param keymapLayout Keymap layout to be loaded in core.
	 * @return True if success
	 */

	public boolean openKeymap(KPTParamKeymapId keymapId) {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());

		// First check if any keymaps are open.
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETOPEN, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETOPEN status code: " + statusCode);
		if(statusCode != KPTStatusCode.KPT_SC_SUCCESS) {
			return false;
		}

		KPTParamKeymapId[] openKeymapIds = keymapOps.getKeymapIdsOpen();

		if(openKeymapIds != null && openKeymapIds.length > 0) {
			// Close all the open keymaps.
			for(KPTParamKeymapId openKeymapId : openKeymapIds) {
				statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_CLOSE, openKeymapId);
				//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_CLOSE status code: " + statusCode);
				if(statusCode != KPTStatusCode.KPT_SC_SUCCESS) {
					return false;
				}
			}
		}

		// Now open the new keymap
		keymapOps.setCreateIfRequiredOpen(true);
		statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_OPEN, keymapId, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_OPEN status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			//  success
			return true;
		} else {
			//error
			return false;
		}
	}

	/**
	 * Sets active keymap in core.
	 * @param keymapId Keymap id to be set active.
	 * @return True if success
	 */

	public boolean setActiveKeymap(KPTParamKeymapId keymapId) {
		KPTParamKeymapId[] keymapIdList = {keymapId};
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_SETACTIVE, keymapIdList, null);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_SETACTIVE status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success
			


			return true;
		}
		else {
			// error
			return false;
		}
	}

	/**
	 * Activates the keymap corresponding to the particular language dictionary component.
	 * @param componentId Component Id of the dictionary whose language keymap has to be acticvated.
	 * @param currentLanguageDict [Optional param else supply null]
	 * 		Language Dictionary whose corresponding keymap has to be activated.
	 * @return If activation is success.
	 */

	public boolean activateLanguageKeymap(int componentId, KPTParamDictionary currentLanguageDict) {

		boolean result = false;

		if(currentLanguageDict == null) {
			KPTParamDictionary[] installedDictList = CoreDictionary.getInstalledDictionaries();

			if(installedDictList != null) {
				for(KPTParamDictionary installedDictionary : installedDictList) {
					if(installedDictionary.getComponentId() == componentId) {
						currentLanguageDict = installedDictionary;
						break;
					}
				}
			}
		}

		if(currentLanguageDict != null) {
			KPTLanguage currentLanguage = currentLanguageDict.getDictLanguage();
			//KPTLog.i(KPTDebugString, "current language keymap " + currentLanguage.getLanguage().substring(0, 5));
			KPTParamKeymapId[] keymapList = getAvailableKeymaps();

			for(KPTParamKeymapId availableKeymap : keymapList) {
				//KPTLog.i(KPTDebugString, "Available language keymap " + availableKeymap.getLanguage().getLanguage().substring(0, 5));
				if(availableKeymap.getLanguage().getLanguage().substring(0, 5).equals
						(currentLanguage.getLanguage().substring(0, 5))) {

					KPTParamKeymapId[] activeKeymapArray = getActiveKeymap();

					boolean isKeymapAlreadyActive = false;
					for(KPTParamKeymapId activeKeymap : activeKeymapArray) {
						if(activeKeymap.getLanguage().getLanguage().substring(0,5).equals
								(availableKeymap.getLanguage().getLanguage().substring(0, 5))) {
							// Already the keymap is set as active
							// Ignore and break.
							//KPTLog.i(KPTDebugString, "Already keymap is set as active keymap.");
							result = true;
							isKeymapAlreadyActive = true;
							break;
						}
					}

					if(isKeymapAlreadyActive) {
						// Break from the outer loop
						break;
					}

					// Set this keymap id as the active keymap id
					result = openKeymap(availableKeymap);

					// If keymap is successfully opened, then activate that keymap.
					if(result) {

						result = setActiveKeymap(availableKeymap);
						if(result) {
							KPTLog.e(KPTDebugString, "Set Active keymap success");
						}else {
							KPTLog.e(KPTDebugString, "Set Active keymap failed");
						}
					} else {
						KPTLog.e(KPTDebugString, "Open keymap failed");
					}

					break;
				}
			}
		}
		return result;
	}
	
	
	/**
	 * Changes priority of installed dictionaries
	 * @param componentId component id of dictionary to change priority.
	 * @param priority priority to be set. (Priority starts from zero index)
	 * @return If changing priority is success or not.
	 */
	
	public boolean setDictionaryPriority(int componentId, int priority) {
		/*if(mIsCoreMaintanenceMode){
			return false;
		}
		
	*/
		int feildMaskState = KPTParamDictionary.KPT_DICT_STATE_PRIORITY;
		KPTParamDictionary dictionary = new KPTParamDictionary(KPTCmd.KPTCMD_DICT.getBitNumber());
		
		// componentId can be obtained from getAvailableComponents() function in this class.
		
		dictionary.setDictState(feildMaskState, componentId, false, priority, false);
		
		KPTParamDictionary[] dictionaryList = new KPTParamDictionary[1];
		dictionaryList[0] = dictionary;

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_DICT_SETSTATES, dictionaryList, null);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS){
			// Changing priority failed
			//KPTLog.e(KPTDebugString, "**ERROR**" + "KPTCMD_DICT_SETSTATES status: " + statuscode);
			return false;
		}
		else {
			// Changing priority is successful
			return true;
		}
	}
	
	/**
	 * Gets list of available components and its details.
	 * @return List of components and their info
	 */
	public KPTParamComponentInfo[] getAvailableComponents() {
		/*if(mIsCoreMaintanenceMode){
			return null;
		}
*/
		KPTParamComponentsOperations componentOps = new KPTParamComponentsOperations(KPTCmd.KPTCMD_COMPONENTS.getBitNumber());
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_COMPONENTS_GETAVAILABLE, componentOps);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS){
			//KPTLog.e(KPTDebugString, "KPTCMD_COMPONENTS_GETAVAILABLE Failed " + statuscode);
			return null;
		}
		else {
			// Get component details is success
			KPTParamComponentInfo[] compInfo =  componentOps.getAvailComponents();
			return compInfo;
		}

	}


	/**
	 * Saves User Context in Core(Saves user added words to core User Dictionary.)
	 */
/*
	public void saveUserContext(){
		if(!isCoreUserLoaded() || mIsCoreMaintanenceMode){
			return;
		}
		if(mAdaptxtCore != null) {
			KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_SAVE, user);
			KPTLog.i(KPTDebugString, "KPTCMD_USER_SAVE- " + statuscode);
		}
	}*/


	public void destroyCore() {
		KPTLog.e(KPTDebugString, "CoreEngine::destroyCore() mReferenceCount " + mReferenceCount);
		/* We are synchronizing the current instance in this function to make that the Core destruction
		 * processing of one thread doesn't interfere with a possible Core initialization processing of
		 * another thread. Currently this issue was identified during system locale change which initiates
		 * the above conflict between KPTLocaleHandlerService and KPTPackageInstallerService.
		 */
		synchronized (this) {
			if (mReferenceCount > 0) {
				mReferenceCount--;
				if (mReferenceCount == 0) {
					//logout();
					KPTStatusCode statuscode = mAdaptxtCore.KPTFwkDestroy();
					if (statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
						// Error in core destruction
					}
					sCoreEngine = null;
				}
			}
		}
	}

	/**
	 * Forces core destruction in case service fails abruptly
	 */
	public void forceDestroyCore() {
		KPTLog.e(KPTDebugString, "CoreEngine::forceDestroyCore() mReferenceCount " + mReferenceCount);
		if(mReferenceCount > 0) {
			mReferenceCount = 1;
			destroyCore();
		}
	}

	private void setRevertedWord(String mRevertedWord) {
		this.mRevertedWord = mRevertedWord;
	}

	public boolean isCoreUserLoaded() {
		return mIsCoreUserLoaded;
	}	

	public class KPTSuggestion {

		/**
		 * Empty Suggestion.
		 * This value is used for initialization only.
		 * @see KPT_SUGGS_TYPE_EMPTY::suggestionType
		 */
		public static final int KPT_SUGGS_TYPE_EMPTY = 0;

		/**
		 * Error Correction.
		 * The suggestion is generated by the error correction algorithms.
		 */
		public static final int KPT_SUGGS_TYPE_ERROR_CORRECTION = 10;



		private int mSuggestionId;
		private int mSsuggestionType;
		private String mSuggestionString;


		public void setsuggestionId(int suggestionId){
			mSuggestionId = suggestionId;
		}

		public void setsuggestionType(int suggestionType){
			mSsuggestionType = suggestionType;
		}

		public void setsuggestionString(String suggestionString){
			mSuggestionString = suggestionString;
		}

		public int getsuggestionId(){
			return mSuggestionId; 
		}

		public int getsuggestionType(){
			return mSsuggestionType;
		}

		public String getsuggestionString(){
			return mSuggestionString;
		}



	}
}
