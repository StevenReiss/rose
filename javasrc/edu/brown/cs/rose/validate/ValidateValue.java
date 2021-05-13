/********************************************************************************/
/*                                                                              */
/*              ValidateValue.java                                              */
/*                                                                              */
/*      Representation of a value (that can change over time)                   */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.rose.validate;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class ValidateValue implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         value_element;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateValue(Element v)
{
   value_element = v;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

long getStartTime()                     
{
   return IvyXml.getAttrLong(value_element,"TIME");
}

boolean isNull()
{
   return IvyXml.getAttrBool(value_element,"NULL");
}


String getDataType()
{
   return IvyXml.getAttrString(value_element,"TYPE");
}


Long getNumericValue()
{
   try {
      return Long.parseLong(IvyXml.getText(value_element));
    }
   catch (NumberFormatException e) { }
    
   return null;
}


int getLineValue()
{
   try {
      return Integer.parseInt(IvyXml.getText(value_element));
    }
   catch (NumberFormatException e) { }
   
   return 0;
}



String getValue()
{
   return IvyXml.getText(value_element);
}



}       // end of class ValidateValue




/* end of ValidateValue.java */

