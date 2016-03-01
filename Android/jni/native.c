#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "aspell.h"

#include <stdio.h>

#define DEBUG_TAG "NDK_ASpell"

void log_word_list(AspellSpeller * speller,
			    const AspellWordList *wl)
{
  if (wl == 0) {
	  __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: Failed to get suggested world list [%s]",aspell_speller_error_message(speller));
  } else {
    AspellStringEnumeration * els = aspell_word_list_elements(wl);
    const char * word;
    while ( (word = aspell_string_enumeration_next(els)) != 0) {
    	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: Suggested: [%s]",word);
    }
    delete_aspell_string_enumeration(els);
  }
}

AspellSpeller * globalSpeller = NULL;

AspellSpeller *getAspeller()
{
	return globalSpeller;
}

jboolean Java_com_strathclyde_spellchecking_ASpell_initialize(JNIEnv * env, jobject this, jstring dataDirStr, jstring localeStr)
{
	if(globalSpeller!=NULL)
	{ // release an already created speller
		delete_aspell_speller(globalSpeller);
		globalSpeller=NULL;
	}

	AspellSpeller *speller=NULL;
	AspellConfig * config = NULL;
	AspellCanHaveError * ret;


	config = new_aspell_config();

	jboolean isCopy;
	const char * locale= (*env)->GetStringUTFChars(env, localeStr, &isCopy);
	const char * dataDir= (*env)->GetStringUTFChars(env, dataDirStr, &isCopy);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:ASpell Creating Speller [%s] [%s]",locale,dataDir);

	aspell_config_replace(config, "lang", locale);
	aspell_config_replace(config, "data-dir", dataDir);
	aspell_config_replace(config, "dict-dir", dataDir);
	aspell_config_replace(config, "rem-filter", "url");
	aspell_config_replace(config, "encoding", "UTF-8");

	ret = new_aspell_speller(config);
	delete_aspell_config(config);

	(*env)->ReleaseStringUTFChars(env, localeStr, locale);
	(*env)->ReleaseStringUTFChars(env, dataDirStr, dataDir);

	if (aspell_error(ret) != 0)
	{
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:ASpell Failed to create speller [%s]",aspell_error_message(ret));
		delete_aspell_can_have_error(ret);
		return JNI_FALSE;
	}

	speller = to_aspell_speller(ret);
	config = aspell_speller_config(speller);

	globalSpeller = speller;

	return JNI_TRUE;
}

jobjectArray Java_com_strathclyde_spellchecking_ASpell_check(JNIEnv * env, jobject this, jstring wordStr)
{
	jobjectArray res;

	AspellSpeller *speller = getAspeller();
	if(speller==NULL)
	{
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:ASpell Speller Not initialized.");
		res = (jobjectArray)(*env)->NewObjectArray(env,1,
													(*env)->FindClass(env,"java/lang/String"),
													(*env)->NewStringUTF(env,""));

		(*env)->SetObjectArrayElement(env,res,0,(*env)->NewStringUTF(env, "-100"));
		return res;
	}

	jboolean isCopy;
	const char * word= (*env)->GetStringUTFChars(env, wordStr, &isCopy);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:ASpell Checking word [%s]",word);

	int have = aspell_speller_check(speller, word, -1);
	char code[5];
	sprintf(code,"%d",have);

	if(have!=0 && have!=1)
	{ // error. Keep the code in the result and log the event.
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:ASpell Error %s\n", aspell_speller_error_message(speller));
		res = (jobjectArray)(*env)->NewObjectArray(env,1,
													(*env)->FindClass(env,"java/lang/String"),
													(*env)->NewStringUTF(env,""));
		(*env)->SetObjectArrayElement(env,res,0,(*env)->NewStringUTF(env, code));
	}
	else
	{ // i have results, store them to the result array.
		const AspellWordList *wl = aspell_speller_suggest(speller, word, -1);
		int len = aspell_word_list_size(wl)+1;
		res = (jobjectArray)(*env)->NewObjectArray(env,len,
													(*env)->FindClass(env,"java/lang/String"),
													(*env)->NewStringUTF(env,""));

		AspellStringEnumeration * els = aspell_word_list_elements(wl);

		// store res code to first element of the list.
		(*env)->SetObjectArrayElement(env,res,0,(*env)->NewStringUTF(env, code));

		const char * suggestion;
		int i;
		for(i=1;i<len;++i)
		{
			suggestion = aspell_string_enumeration_next(els);
			(*env)->SetObjectArrayElement(env,res,i,(*env)->NewStringUTF(env, suggestion));
		}
		delete_aspell_string_enumeration(els);
	}
	(*env)->ReleaseStringUTFChars(env, wordStr, word);
	return res;
}
