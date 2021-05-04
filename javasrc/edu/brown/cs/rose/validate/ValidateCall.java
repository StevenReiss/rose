/********************************************************************************/
/*                                                                              */
/*              ValidateCall.java                                               */
/*                                                                              */
/*      Representation of a call in an execution trace                          */
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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class ValidateCall implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         context_element;
private ValidateTrace   for_trace;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/
ValidateCall(ValidateTrace vt,Element ctx)
{
   for_trace = vt;
   context_element = ctx;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getMethod()
{
   return IvyXml.getAttrString(context_element,"METHOD");
}


File getFile()
{
   String fnm = IvyXml.getAttrString(context_element,"FILE");
   if (fnm == null) return null;
   return new File(fnm);
}


long getStartTime()
{
   return IvyXml.getAttrLong(context_element,"START");
}


long getEndTime()
{
   return IvyXml.getAttrLong(context_element,"END");
}

int getContextId()
{
   return IvyXml.getAttrInt(context_element,"ID");
}

boolean sameAs(ValidateCall call) 
{
   if (call == null) return false;
   
   return getContextId() == call.getContextId();
}


ValidateTrace getTrace()
{
   return for_trace;
}

List<ValidateCall> getInnerCalls()
{
   List<ValidateCall> rslt = new ArrayList<>();
   for (Element c : IvyXml.children(context_element,"CONTEXT")) {
      rslt.add(for_trace.getCallForContext(c));
    }
   return rslt;
}


ValidateVariable getLineNumbers()
{
   for (Element e : IvyXml.children(context_element,"VARIABLE")) {
      String nm = IvyXml.getAttrString(e,"NAME");
      if (nm.equals("*LINE*")) return new ValidateVariable(e);
    }
   
   return null;
}


Map<String,ValidateVariable> getVariables()
{
   Map<String,ValidateVariable> rslt = new LinkedHashMap<>();
   for (Element e : IvyXml.children(context_element,"VARIABLE")) {
      String nm = IvyXml.getAttrString(e,"NAME");
      if (nm.equals("*LINE*")) continue;
      rslt.put(nm,new ValidateVariable(e));
    }
   return rslt;
}


}       // end of class ValidateCall




/* end of ValidateCall.java */

