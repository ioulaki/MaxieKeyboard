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
 * 
 * Uses the String Similarity Metrics Library from Marco Aurelio Graciotto Silva
 * https://github.com/magsilva/SimMetrics
 *****************************************************************************/

package com.strathclyde.spellchecking;

import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;;

/**
 * Creates a weighted spellchecking suggestion. Weights are calculated by taking into account
 * - the levenshtein distance of the suggestion and the original input if the length of the two words is not the same
 * - the levenshtein distance and physical distance between keys at each position, if the words have the same length
 * The latter method is used to give a better metric for detecting finger slippage (i.e. proximity errors)
 * @author ako2
 *
 */
public class WeightedSuggestion {

	public double weight;
	public String text;
	
	public WeightedSuggestion(String orig, String sug)
	{
		double modifier=0.0;
		
		if(orig.length()==sug.length())
		{
			if(ASpellCheckerService.keymodel!=null)
			{
				for (int x=0; x<orig.length(); x++)
				{
					modifier+=ASpellCheckerService.keymodel.distance(orig.charAt(x), sug.charAt(x));
				}
			}
			else
			{
				for (int x=0; x<orig.length(); x++)
				{
					modifier+=SpellForSamsung.keymodel.distance(orig.charAt(x), sug.charAt(x));
				}
			}
			modifier=modifier/orig.length();
		}
		else
			modifier = 1.0;
		
		//modifier=1.0;
		Levenshtein ls = new Levenshtein();
		weight = (1/modifier)*ls.getSimilarity(orig, sug);
		text=sug;
		
		
		
	}
}
