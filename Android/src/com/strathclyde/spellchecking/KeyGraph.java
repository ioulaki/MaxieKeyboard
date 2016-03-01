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

package com.strathclyde.spellchecking;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.strathclyde.highlightingkeyboard.LatinKeyboard;

import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard.Key;
import android.util.Log;
import android.util.Xml;

/**
 * 
 * Creates a graph model of the qwerty keyboard layout
 *
 */
public class KeyGraph {
	
		
	private List<KeyVertex> keys;
	private HashMap<Character, String> keymap; //map of keys and their row, column position in the keyboard
	private HashMap<Integer, KeyVertex> keymap2; //map of keys and their keyvertex (screen x, y coords) in the keyboard
	private LatinKeyboard kb;
	private int keyDist; //the "average key distance" based on the keyboard layout actual screen size
	
	  private List<KeyEdge> edges;
	  private static final String ns = "http://schemas.android.com/apk/res/android";

	  /**
	   * Loads the current keyboard layout from a layout XML and parses it, creating the keyboard graph model
	   */
	  public KeyGraph(XmlResourceParser xmlResourceParser) {
		
		  if (xmlResourceParser==null)
			  Log.i("KeyGraph", "InputStream is null");
		  /*
		  else
			  Log.i("KeyGraph", "Resource dump \n"+ new java.util.Scanner(in).useDelimiter("\\A").next());
		  */
		  keymap = new HashMap<Character, String>();
			try {
				parse(xmlResourceParser);
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   
	  }
	  
	  /**
	   * Loads the current keyboard layout from a LatinKeyboard object and creates a hashmap of the keys as graph vertices
	   * @param kb the keyboard object
	   */
	  public KeyGraph(LatinKeyboard kb) {
		  this.kb=kb;
		  keymap2 = new HashMap<Integer, KeyVertex>();
		  populateMap();
	  }
	  
	  /**
	   * populates a hashmap of the keyboard key codes and their corresponding KeyVertex representations
	   */
	  public void populateMap()
	  {
		  
		  List<Key> klist = kb.getKeys();
		  if(klist!=null)
		  {
			  keyDist = (klist.get(0).width+klist.get(0).height)/2; //average key dist
			  Log.i("Populate map", "W="+klist.get(0).width+", H="+klist.get(0).height+", keyDist="+keyDist);
			  for (int x=0; x<klist.size(); x++)
		      {			 
				  keymap2.put(klist.get(x).codes[0], new KeyVertex(klist.get(x).codes[0], '*', 
						  (klist.get(x).x+(klist.get(x).width/2)), (klist.get(x).y+(klist.get(x).height/2))));
				 /* Log.i("KeyModel", klist.get(x).codes[0]+" centre XY "+
						  (klist.get(x).x+(klist.get(x).width/2))+","+(klist.get(x).y+(klist.get(x).height/2)));*/
		      }
		  }
	  }
	  
	  /**
	   * 
	   * @return the key vertexes of the keyboard graph
	   */
	  public List<KeyVertex> getVertexes() {
	    return keys;
	  }
	  
	  /**
	   * 
	   * @return the edges of the graph
	   */
	  public List<KeyEdge> getEdges() {
	    return edges;
	  }
	  
	  /**
	   * Parses a keyboard layout XML file and creates a graph model from it
	   * @param parser
	   * @throws XmlPullParserException
	   * @throws IOException
	   */
	  public void parse(XmlResourceParser parser) throws XmlPullParserException, IOException {
	        try {
	        	
	          //an arraylist of all the keyboard rows, as retrieved from the xml file
				ArrayList<ArrayList<KeyVertex>> keyRows = new ArrayList<ArrayList<KeyVertex>>();
	            
	            int eventType = parser.getEventType();
	            //parse the xml and read the keys into their respective rows
	            while (eventType != XmlPullParser.END_DOCUMENT) {
	            	String tagname = parser.getName();                
	            	switch (eventType) {
		                case XmlPullParser.START_TAG:
		                	//Log.i("KeyGraph","tag found:"+tagname);
		                	if(tagname.equalsIgnoreCase("Row"))
		                	{
		                		keyRows.add(new ArrayList<KeyVertex>());
		                		//Log.i("KeyGraph","added new row");
		                		break;
		                	}
		                	if(tagname.equalsIgnoreCase("Key"))
		                	{
		                		
		                		if(parser.getAttributeValue(ns,"keyLabel")!=null && parser.getAttributeValue(ns, "codes")!=null)
		                		{
		                			//a key can represent multiple codes
		                			String[] codes = parser.getAttributeValue(ns, "codes").split(",");
		                			KeyVertex k = new KeyVertex(Integer.parseInt(codes[0]), 
			        		        		parser.getAttributeValue(ns, "keyLabel").charAt(0));
		                			keyRows.get(keyRows.size()-1).add(k);
		                			//Log.i("KeyGraph", (parser.getAttributeValue(ns, "codes"))+","+
		    		        		//        		parser.getAttributeValue(ns, "keyLabel").charAt(0));
		                		}
		                		//Log.i("KeyGraph","added new key nvalues="+parser.getAttributeValue(ns,"keyLabel"));
		                		//Log.i("KeyGraph", (parser.getAttributeValue(null, "android:codes"))+","+
		        		        //		parser.getAttributeValue(null, "android:keyLabel").charAt(0));
		                		break;
		                	}
	                	default:
		                		break;
		                }
	            	
	                eventType = parser.next();
	            }
	            
	            
	            //dump all keys into the hashmap
	            for (int z=0;z<keyRows.size();z++)
			    {
			    	for (int x=0;x<keyRows.get(z).size();x++)
			    	{
			    		keymap.put(keyRows.get(z).get(x).getName(), z+","+x);
			    		//Log.i("KeyGraph","added "+ keyRows.get(z).get(x).getName()+" at "+ z+","+x);
			    	}
			    }
			    
	        } finally {
	            parser.close();
	        }
	    }
	  
	 
	 /**
	  * Returns the distance between keys when their position is modelled as a (row, column) coordinate 
	  * @param fromChar character from which to begin measuring distance
	  * @param targetChar character to which we want the distance
	  * @return the calculated distance
	  */
	 public double distance (char fromChar, char targetChar)
	 {
		 try{
		 fromChar=Character.toLowerCase(fromChar);
		 fromChar=Normalizer.normalize(""+fromChar, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").charAt(0);
		 targetChar=Character.toLowerCase(targetChar);
		 targetChar=Normalizer.normalize(""+targetChar, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").charAt(0);
		 //Log.i("KeyGraph","Distance of "+ fromChar+" to "+targetChar);
		 String fromCoords = keymap.get(fromChar);
		 String toCoords = keymap.get(targetChar);
		 int startrow = Integer.parseInt(fromCoords.substring(0,1));
		 int startcol = Integer.parseInt(fromCoords.substring(2));
		 int targetrow = Integer.parseInt(toCoords.substring(0,1));
		 int targetcol = Integer.parseInt(toCoords.substring(2));
		 
		 //euclidian distance between keys
		 return Math.sqrt(Math.pow((startrow-targetrow), 2)+Math.pow((startcol-targetcol), 2));}
		 catch(Exception e)
		 {
			 return 1;
		 }
	 }
	 
	 /**
	  * Returns the distance between keys when their position is modelled as screen x,y coordinates
	  * @param fromCharCode character from which to begin measuring distance
	  * @param targetCharCode character to which we want the distance
	  * @return the calculated distance in terms of "average key size"
	  */
	 public double distance2 (int fromCharCode, int targetCharCode)
	 {
		 try{
		 
		 KeyVertex fromCoords = keymap2.get(fromCharCode);
		 KeyVertex toCoords = keymap2.get(targetCharCode);
		 int startx = fromCoords.getX();
		 int starty = fromCoords.getY();
		 int targetx = toCoords.getX();
		 int targety = toCoords.getY();
		 
		 //euclidian distance between keys
		 return (Math.sqrt(Math.pow((startx-targetx), 2)+Math.pow((starty-targety), 2)))/keyDist; //normalise distance to average key sizes
		 }
		 catch(Exception e)
		 {
			 return 1;
		 }
	 }
}
