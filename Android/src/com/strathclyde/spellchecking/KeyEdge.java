/******************************************************************************
 * 
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

/**
 * An edge of the qwerty keyboard graph model (vertices are the keys)
 */
package com.strathclyde.spellchecking;

public class KeyEdge {
	private final int id; 
	  private final KeyVertex source;
	  private final KeyVertex destination;
	  private final int weight; 
	  
	  public KeyEdge(int id, KeyVertex source, KeyVertex destination, int weight) {
	    this.id = id;
	    this.source = source;
	    this.destination = destination;
	    this.weight = weight;
	  }
	  
	  public int getId() {
	    return id;
	  }
	  public KeyVertex getDestination() {
	    return destination;
	  }

	  public KeyVertex getSource() {
	    return source;
	  }
	  public int getWeight() {
	    return weight;
	  }
	  
	  @Override
	  public String toString() {
	    return source + " " + destination;
	  }
}
