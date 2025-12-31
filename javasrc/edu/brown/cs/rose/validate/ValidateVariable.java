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
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RootValidate.RootTrace;
import edu.brown.cs.rose.root.RootValidate.RootTraceValue;

class ValidateVariable implements ValidateConstants, RootValidate.RootTraceVariable
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

@Override public String getName()                       
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


@Override public List<RootTraceValue> getTraceValues(RootTrace rtrace)
{
   ValidateTrace trace = (ValidateTrace) rtrace;
   List<RootTraceValue> rslt = new ArrayList<>();
   for (Element e : IvyXml.children(variable_element,"VALUE")) {
      Element v1 = e;
      if (trace != null) v1 = trace.dereference(e);
      rslt.add(new ValidateValue(v1));
    }
   
   return rslt;
}




@Override public ValidateValue getValueAtTime(RootTrace rtrace,long time)
{
   ValidateTrace trace = (ValidateTrace) rtrace;
   Element prior = null;
   for (Element e : IvyXml.children(variable_element,"VALUE")) {
      long t0 = IvyXml.getAttrLong(e,"TIME");
      if (t0 > 0 && t0 > time) break;
      if (trace == null) prior = e;
      else prior = trace.dereference(e);
    }
   
   return new ValidateValue(prior);
}


@Override public int getLineAtTime(long time) 
{
   ValidateValue vv = getValueAtTime(null,time);
   if (vv == null) return 0;
   Long lv = vv.getNumericValue();
   if (lv == null) return 0;
   return lv.intValue();
}





/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object o) 
{
   if (o instanceof ValidateVariable) {
      ValidateVariable vv = (ValidateVariable) o;
      return variable_element.equals(vv.variable_element);
    }
   return false;
}


@Override public int hashCode()
{
   return variable_element.hashCode();
}



}       // end of class ValidateVariable




/* end of ValidateVariable.java */

