/********************************************************************************/
/*                                                                              */
/*              BudLocalVariable.java                                           */
/*                                                                              */
/*      Representation of a stack variable                                      */
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



package edu.brown.cs.rose.bud;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

public class BudLocalVariable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String var_kind;
private String var_type;
private String var_value;
private String var_name;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BudLocalVariable(Element xml)
{
   var_name = IvyXml.getAttrString(xml,"NAME");
   var_kind = IvyXml.getAttrString(xml,"KIND");
   var_type = IvyXml.getAttrString(xml,"TYPE");
   var_value = IvyXml.getTextElement(xml,"DESCRIPTION");
   var_value = IvyXml.decodeXmlString(var_value);
   if (var_type != null && var_type.equals("java.lang.Class")) {
      var_kind = "CLASS";
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getName()                         { return var_name; }

public String getType()                         { return var_type; }

public String getKind()                         { return var_kind; }

public String getValue()                        { return var_value; }




}       // end of class BudLocalVariable




/* end of BudLocalVariable.java */

