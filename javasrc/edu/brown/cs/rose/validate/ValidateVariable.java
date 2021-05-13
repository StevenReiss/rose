/********************************************************************************/
/*                                                                              */
/*              ValidateVariable.java                                           */
/*                                                                              */
/*      Representation of an execution variable                                 */
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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class ValidateVariable implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         variable_element;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateVariable(Element v)
{
   variable_element = v;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getName()                       
{
   return IvyXml.getAttrString(variable_element,"NAME");
}

List<ValidateValue> getValues(ValidateTrace trace)
{
   List<ValidateValue> rslt = new ArrayList<>();
   for (Element e : IvyXml.children(variable_element,"VALUE")) {
      Element v1 = e;
      if (trace != null) v1 = trace.dereference(e);
      rslt.add(new ValidateValue(v1));
    }
   
   return rslt;
}


ValidateValue getValueAtTime(ValidateTrace trace,long time)
{
   Element prior = null;
   for (Element e : IvyXml.children(variable_element,"VALUE")) {
      long t0 = IvyXml.getAttrLong(e,"TIME");
      if (t0 > 0 && t0 > time) break;
      if (trace == null) prior = e;
      else prior = trace.dereference(e);
    }
   
   return new ValidateValue(prior);
}


int getLineAtTime(long time) 
{
   ValidateValue vv = getValueAtTime(null,time);
   if (vv == null) return 0;
   Long lv = vv.getNumericValue();
   if (lv == null) return 0;
   return lv.intValue();
}



}       // end of class ValidateVariable




/* end of ValidateVariable.java */

