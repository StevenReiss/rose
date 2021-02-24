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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bract.BractFactory;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
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
// RoseLog.logD("STEM","HISTORY RESULT: " + IvyXml.convertXmlToString(hrslt));
   
   if (hrslt == null) throw new RoseException("Can't find history");
   hrslt = IvyXml.getChild(hrslt,"QUERY");
   xw.begin("RESULT");
   Element grslt = IvyXml.getChild(hrslt,"GRAPH");
   processGraph(grslt,xw);
   if (for_problem != null) for_problem.outputXml(xw);
   xw.end("RESULT");
}



private void processGraph(Element gelt,IvyXmlWriter xw)
{
   Map<String,GraphNode> locs = new HashMap<>();
   
   for (Element nelt : IvyXml.children(gelt,"NODE")) {
      GraphNode gn = new GraphNode(stem_control,nelt);
      if (!gn.isValid()) continue;
      String id = gn.getLocationString();
      GraphNode ogn = locs.get(id);
      if (ogn != null) {
         if (ogn.getPriority() >= gn.getPriority()) continue;
       }
      locs.put(id,gn);
    }
   xw.begin("NODES");
   for (GraphNode gn : locs.values()) {
      gn.outputXml(xw);
    }
   xw.end("NODES");
   
   RootMetrics.noteCommand("STEM","HISTORYRESULT",
         IvyXml.getAttrInt(gelt,"SIZE"),locs.size(),
         IvyXml.getAttrInt(gelt,"TIME"));
}




private static class GraphNode {
    
   private RootLocation node_location;
   private double node_priority;
   private String node_reason;
   private String node_type;
   
   GraphNode(RootControl ctrl,Element nelt) {
      Element locelt = IvyXml.getChild(nelt,"LOCATION");
      node_location = BractFactory.getFactory().createLocation(ctrl,locelt);
      node_reason = IvyXml.getAttrString(nelt,"REASON");
      node_priority = IvyXml.getAttrDouble(nelt,"PRIORITY",0.5);
      Element point = IvyXml.getChild(nelt,"POINT");
      node_type = IvyXml.getAttrString(point,"NODETYPE");
    }
   
   boolean isValid() {
      if (node_location == null || node_reason == null) return false;
      if (node_location.getFile() == null) return false;
      if (!node_location.getFile().exists()) return false;
      if (node_location.getLineNumber() <= 0) return false;
      if (node_type == null) return false;
      switch (node_type) {
         case "MethodDeclaration" :
            return false;
         default :
            
       }
      
      return true;
    }
   
   double getPriority()                    { return node_priority; }
   
   String getLocationString() {
      String s = node_location.getFile().getPath();
      s += "@" + node_location.getLineNumber();
      s += ":" + node_location.getStartOffset();
      s += "-" + node_location.getEndOffset();
      return s;
    }
   
   void outputXml(IvyXmlWriter xw) {
      xw.begin("NODE");
      xw.field("PRIORITY",node_priority);
      xw.field("REASON",node_reason);
      node_location.outputXml(xw);
      xw.end("NODE");
    }
   
}       // end of inner class GraphNode





}	// end of class StemQueryHistory




/* end of StemQueryHistory.java */

