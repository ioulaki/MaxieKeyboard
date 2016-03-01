# MaxieKeyboard
An input method for Android with advanced error support features, including visual, audio and haptic feedback when users make spelling mistakes

MaxieKeyboard is a research tool for studies in mobile text entry. 

It offers improved support for users during text entry, by providing visual, audio and haptic feedback whenever a spelling mistake is made by the user. Visual feedback includes the highlighting of text in the editor according to the seriousness of the spelling mistake, as well as a colour bar just above the top row of letters, providing immediate feedback with regard to the spelling mistakes made. Mistakes are highlighted red (major mistakes for which suggestions can be recommended with low confidence), yellow (minor spelling mistakes for which suggestions can be recommended with good confidence) and orange (minor spelling mistakes which have been autocorrected, if autocorrect is enabled). The colourbar additionally lights up as green, when no mistake was made by the user while entering a word. 

Audio and haptic feedback consist of one or two short vibrations, or tones, depending on the gravity of the spelling mistake.

The keyboard also offers logging infrastructure allowing researchers to capture detailed information about usage during lab trials, or obfuscated information which is better suited to longitudinal trials (avoids logging of actual input and touch coordinates).

Finally, for lab testing, the keyboard includes an algorithm for artificially injecting errors into the user's input stream, in order to provide more opportunities to help researchers observe user behaviour during erroneous input.

MaxieKeyboard was developed at the University of Strathclyde, as part of the EPSRC grant "Empirical investigation & user-centred development of touch-screen text entry methods older adults" http://gow.epsrc.ac.uk/NGBOViewGrant.aspx?GrantRef=EP/K024647/1. The development of MaxieKeyboard was made by Dr. Andreas Komninos with the supervision of Dr. Mark Dunlop

For academic publications related to the use of MaxieKeyboard please visit our website http://mobiquitous.strath.ac.uk and http://www.komninos.info

This software is provided under the Apache License 2.0 as is and without any warranty of any kind. Please note that it is intended for research purposes only. If you use our software in your project, please cite our work as follows:

Komninos, A., Nicol E., & Dunlop M. D. (2015).  Designed with Older Adults to Support Better Error Correction in SmartPhone Text Entry: The MaxieKeyboard. Adjunct proc. of the 17th International Conference on Human-Computer Interaction with Mobile Devices and Services. Copenhagen, Denmark, ACM. DOI:10.1145/2786567.2793703

Credits and Thanks:

MaxieKeyboard was developed with the use of the OpenAdapTxt engine http://openadaptxt.sourceforge.net/

It uses the ASpell library as ported to Android by Paschalis Panteleris https://github.com/padeler/aspellchecker

It uses the String Similarity Metrics library by Marco Aurélio Graciotto Silva https://github.com/magsilva/SimMetrics

Development was based on Google's Input Method Service sample code https://android.googlesource.com/platform/development/+/master/samples/SoftKeyboard/
