/********************************************************************************/
/*                                                                              */
/*              StemQueryExpressionHistory.java                                 */
/*                                                                              */
/*      Handle queries for expressions                                          */
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



package edu.brown.cs.rose.stem;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bract.BractProblem;
import edu.brown.cs.rose.root.RoseException;

class StemQueryExpressionHistory extends StemQueryHistory
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String expression_name;
private String current_value;
private Element where_data;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemQueryExpressionHistory(StemMain ctrl,BractProblem prob,Element xml)
{ 
   super(ctrl,prob,xml);
   expression_name = prob.getProblemDetail();
   current_value = prob.getOriginalValue();
//    shouldbe_value = prob.getTargetValue();
   where_data = xml;
}
   

/********************************************************************************/
/*                                                                              */
/*      Process expression query                                                */
/*                                                                              */
/********************************************************************************/

@Override void process(StemMain stem,IvyXmlWriter xw) throws RoseException 
{
   Element hrslt = getHistoryData(stem);
   outputGraph(hrslt,xw);
}



/********************************************************************************/
/*                                                                              */
/*      Set up appropriate query                                                */
/*                                                                              */
/********************************************************************************/

private Element getHistoryData(StemMain stem)
{
   stem.waitForAnalysis();
   
   CommandArgs args = new CommandArgs("FILE",for_file.getAbsolutePath(),
         "QTYPE","EXPRESSION",
         "CURRENT",current_value,
         "LINE",line_number,
         "TOKEN",expression_name,
         "METHOD",method_name);
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("EXPRESSION");
   String [] flds = new String [] { "START", "END", "NODETYPE", "NODETYPEID",
         "AFTER", "AFTERSTART", "AFTEREND", "AFTERTYPE", "AFTERTYPEID" };
   for (String fld : flds) {
      String fval = IvyXml.getAttrString(where_data,fld);
      if (fval != null) args.put(fld,fval);
    }
   xw.end("EXPRESSION");
   String qxml = xw.toString();
   String sxml = getXmlForStack();
   if (sxml != null) qxml += sxml; 
   Element rslt = stem.sendFaitMessage("FLOWQUERY",args,qxml);
   
   xw.close();
   
   return rslt;
}








}       // end of class StemQueryExpressionHistory




/* end of StemQueryExpressionHistory.java */

