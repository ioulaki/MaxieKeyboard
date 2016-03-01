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

import com.kpt.adaptxt.core.coreapi.KPTCommands.KPTCmd;
import com.kpt.adaptxt.core.coreapi.KPTCore;
import com.kpt.adaptxt.core.coreapi.KPTLanguage;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamDictOperations;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;
import com.kpt.adaptxt.core.coreapi.KPTParamPersonalDict;
import com.kpt.adaptxt.core.coreapi.KPTTypes.KPTStatusCode;
import com.strathclyde.corehandler.KPTLog;

/**
 * class holds all the information regarding the user dictionary.
 * @author 
 *
 */
public class CoreDictionary {

	private static final String KPTDebugString ="CoreDictionary" ;
	CoreEngine cEngine;
	private static int MAX_NO_WORDS_MYDICTIONARY = 2500;
	static KPTCore mAdaptxtCore;
	//boolean mIsCoreMaintanenceMode;

	public CoreDictionary() {
		cEngine = new CoreEngine();
		mAdaptxtCore = cEngine.getKPTCore();
		//mIsCoreMaintanenceMode = cEngine.isCoreMaintainanceMode();
	}

	public String[] getUserDictionary() {
		/*if(mIsCoreMaintanenceMode){
			return null;
		}*/

		String[] words = null;
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_GETENTRYCOUNT, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_GETENTRYCOUNT " + statuscode + " count = " + pDict.getNumWordsFound());
		if(pDict.getNumWordsFound() <= 0)
			return words;

		pDict.setMaxEntries(15);        
		pDict.setOffsetFromStart(0);
		if(pDict.getNumWordsFound()<=2500) {
			pDict.setNumWordsToView(pDict.getNumWordsFound());
		} else {
			pDict.setNumWordsToView(MAX_NO_WORDS_MYDICTIONARY);
		}

		int filterState = KPTParamPersonalDict.KPT_PD_VIEWFILTER_NONE;
		int viewOption = KPTParamPersonalDict.KPT_PD_VIEW_ALPHA_ASCENDING;
		pDict.setOpenViewRequest(viewOption, filterState, null);

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_OPENVIEW, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_OPENVIEW " + statuscode);

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_VIEWPAGE, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_VIEWPAGE " + statuscode);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) { 
			words = pDict.getWordsFromPage();
			mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_CLOSEVIEW, pDict);
		}
		return words;        
	}

	public boolean addUserDictionaryWords(String[] words) {
		/*if(mIsCoreMaintanenceMode){
			return false;
		} */
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);        
		pDict.setWordsTemp(words, words.length);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_ADDWORDS, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_ADDWORDS " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) 
			return true;
		return false;                
	}

	public boolean removeUserDictionaryWords(String[] words) {
		/*if(mIsCoreMaintanenceMode){
			return false;
		}*/
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);
		pDict.setRemoveRequest(words, null, 0, KPTParamPersonalDict.KPT_PD_REMOVE_BY_WORD, words.length);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_REMOVEWORDS, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_REMOVEWORDS " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)        
			return true;
		return false;
	}    

	public boolean removeAllUserDictionaryWords() {
		/*if(mIsCoreMaintanenceMode){
			return false;
		}*/
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_REMOVEALLWORDS, null);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_REMOVEALLWORDS " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)        
			return true;
		return false;
	}

	/**
	 * Loads and enables a Dictionary.
	 * @param componentId Component Id of the dictionary to be loaded.
	 * @return Success or Failure.
	 */

	public boolean loadDictionary(int componentId) {
		/*if(mIsCoreMaintanenceMode){
			return false;
		}*/

		KPTParamComponentInfo componentInfo = new KPTParamComponentInfo(KPTCmd.KPTCMD_COMPONENTS.getBitNumber());
		componentInfo.setComponentId(componentId);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_COMPONENTS_LOAD, componentInfo);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			//KPTLog.e(KPTDebugString, "KPTCMD_COMPONENTS_LOAD failed status code: " + statuscode);
			return false;
		}
		else {
			// Enabling/ Loading a componnet is success.
			return true;
		}
	}

	/**
	 * Gets the list of installed dicionaties in the core
	 * 
	 * @return Dictionary List
	 */

	public static KPTParamDictionary[] getInstalledDictionaries() {
		if(mAdaptxtCore == null) { 
			return null;
		}

		KPTParamDictOperations dictOps = new KPTParamDictOperations(KPTCmd.KPTCMD_DICT.getBitNumber());
		//Use null language filter, so we get all language dictionaries
		KPTLanguage langMatch = null;

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_DICT_GETLIST, langMatch, dictOps);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS){
			//KPTLog.e(KPTDebugString, "KPTCMD_DICT_GETLIST Failed " + statuscode);
			return null;
		}
		else {
			// Get installed Dictionaries success
			return dictOps != null ? dictOps.getDictList(): null; 
		}
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
		}*/

		int feildMaskState = KPTParamDictionary.KPT_DICT_STATE_PRIORITY;
		KPTParamDictionary dictionary = new KPTParamDictionary(KPTCmd.KPTCMD_DICT.getBitNumber());
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
	 * Gets the loaded top priority component id from installed dictionary list.
	 * @param installedPkgs Installed packages array
	 * @return top priority dictionary
	 */
	public static KPTParamDictionary getTopPriorityDictionary() {
		KPTParamDictionary  topPriorityDict = null;
		int priorityRank = Integer.MAX_VALUE; 
		KPTParamDictionary[] dictList = CoreDictionary.getInstalledDictionaries();
		if(dictList != null && dictList.length > 0) {
			for(KPTParamDictionary installedDict : dictList) {
				if(installedDict.isDictLoaded() &&
						installedDict.getDictPriority() < priorityRank) {
					priorityRank = installedDict.getDictPriority();
					topPriorityDict = installedDict;
				}
			}
		}
		return topPriorityDict;
	}
}
