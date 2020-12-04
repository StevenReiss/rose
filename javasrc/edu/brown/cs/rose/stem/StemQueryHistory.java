/********************************************************************************/
/*										*/
/*		StemQueryHistory.java						*/
/*										*/
/*	Handle history request to get potential location information		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.rose.stem;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RootMetrics;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RoseException;

abstract class StemQueryHistory extends StemQueryBase implements StemConstants
{


/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

static StemQueryHistory createHistoryQuery(StemMain ctrl,RootProblem prob)
{
   // Rather than passing xml, the RootProblem should include an optional expession
   //   context (START/END/NODETYPE/NODETYPEID/AFTER/AFTERSTART/AFTEREND/AFTERTYPE/
   //   AFTERTYPEID).
   
   switch (prob.getProblemType()) {
      case VARIABLE :
	 return new StemQueryVariableHistory(ctrl,prob);
      case EXPRESSION :
         return new StemQueryExpressionHistory(ctrl,prob);
      case EXCEPTION :
         return new StemQueryExceptionHistory(ctrl,prob);
      case OTHER :
      case LOCATION :
         return new StemQueryLocationHistory(ctrl,prob);
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected StemQueryHistory(StemMain ctrl,RootProblem prob)
{
   super(ctrl,prob);
}



/********************************************************************************/
/*										*/
/*	Process methods 							*/
/*										*/
/********************************************************************************/

abstract void process(StemMain stem,IvyXmlWriter xw) throws RoseException;




/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected void outputGraph(Element hrslt,IvyXmlWriter xw) throws RoseException
{
//    RoseLog.logD("STEM","HISTORY RESULT: " + IvyXml.convertXmlToString(hrslt));
   
   if (hrslt == null) throw new RoseException("Can't find history");
   hrslt = IvyXml.getChild(hrslt,"QUERY");
   xw.begin("RESULT");
   Element grslt = IvyXml.getChild(hrslt,"GRAPH");
   xw.writeXml(grslt);
   xw.end("RESULT");
   
   int ct = 0;
   for (Element e : IvyXml.elementsByTag(grslt,"LOCATION")) {
      if (e != null) ++ct;
    }
   RootMetrics.noteCommand("STEM","HISTORYRESULT",
         IvyXml.getAttrInt(grslt,"SIZE"),ct,
         IvyXml.getAttrInt(grslt,"TIME"));
}






}	// end of class StemQueryHistory




/* end of StemQueryHistory.java */

