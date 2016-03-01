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

/**
 * Represents a key as a vertex of the keyboard graph model
 * @author ako2
 *
 */
public class KeyVertex {

	private int id = -200;
	final private char name;
	private int xcoord;
	private int ycoord;
	  
	  
	  public KeyVertex(int id, char name) {
	    this.id = id;
	    this.name = name;
	  }
	  
	  public KeyVertex(int id, char name, int x, int y) {
		    this.id = id;
		    this.name = name;
		    xcoord=x;
		    ycoord=y;
		  }
	  
	  public int getId() {
	    return id;
	  }

	  public char getName() {
	    return name;
	  }
	  
	  public int getX()
	  {
		  return xcoord;
	  }
	  public int getY()
	  {
		  return ycoord;
	  }
	  public void setCoords(int x, int y)
	  {
		  xcoord=x;
		  ycoord=y;
	  }
	  
	  @Override
	  public boolean equals(Object obj) {
	    if (this == obj)
	      return true;
	    if (obj == null)
	      return false;
	    if (getClass() != obj.getClass())
	      return false;
	    KeyVertex other = (KeyVertex) obj;
	    if (id == -200) {
	      if (other.id != -200)
	        return false;
	    } else if (id!=other.id)
	      return false;
	    return true;
	  }

	  @Override
	  public String toString() {
	    return ""+name;
	  }
	
}
