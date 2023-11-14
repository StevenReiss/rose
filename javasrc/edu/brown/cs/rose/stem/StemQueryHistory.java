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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
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
      case ASSERTION :
         return new StemQueryAssertionHistory(ctrl,prob);
      case OTHER :
      case NONE :
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


protected CommandArgs addCommandArgs(CommandArgs args) 
{
   if (args == null) args = new CommandArgs();
   args.put("FILE",for_file.getAbsolutePath());
   args.put("LINE",line_number);
   args.put("METHOD",method_name);
   if (args.get("CONDDEPTH") == null) args.put("CONDDEPTH",cond_depth);
   if (args.get("DEPTH") == null) args.put("DEPTH",query_depth);
   
   return args;
}


/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected void outputGraph(Element hrslt,IvyXmlWriter xw) throws RoseException
{
// RoseLog.logD("STEM","HISTORY RESULT: " + IvyXml.convertXmlToString(hrslt));
   
   if (hrslt == null) throw new RoseException("Can't find history");
   xw.begin("RESULT");
   if (for_problem != null) for_problem.outputXml(xw);
   xw.begin("NODES");
   int lsz = 0;
   int tsz = 0;
   long ttim = 0;
   for (Element qrslt : IvyXml.children(hrslt,"QUERY")) {
      Element grslt = IvyXml.getChild(qrslt,"GRAPH");
      int sz = IvyXml.getAttrInt(grslt,"SIZE");
      tsz += sz;
      ttim += IvyXml.getAttrLong(grslt,"TIME");
      if (sz > 0) lsz += processGraphNodes(grslt,xw);
    }
   xw.end("NODES");
   xw.end("RESULT");
   
   RootMetrics.noteCommand("STEM","HISTORYRESULT",tsz,lsz,ttim);
}



private int processGraphNodes(Element gelt,IvyXmlWriter xw)
{
   Map<String,GraphNode> locs = new HashMap<>();
   
   List<GraphNode> allnodes = new ArrayList<>();
   for (Element nelt : IvyXml.children(gelt,"NODE")) {
      GraphNode gn = new GraphNode(stem_control,nelt);
      if (gn.shouldCheck()) allnodes.add(gn);
    }
   
   Set<File> done = new HashSet<>();
   for ( ; ; ) {
      File workon = null;
      for (GraphNode gn : allnodes) {
         File gfile = gn.getFile();
         if (done.contains(gfile)) continue;
         if (workon == null) {
            workon = gfile;
            gn.getLineNumber();
          }
         else if (gfile.equals(workon)) gn.getLineNumber();
       }
      if (workon == null) break;
      done.add(workon); 
    }
   
   for (GraphNode gn : allnodes) {
      if (!gn.isValid()) continue;
      String id = gn.getLocationString();
      GraphNode ogn = locs.get(id);
      if (ogn != null) {
         if (ogn.getPriority() >= gn.getPriority()) continue;
       }
      locs.put(id,gn);
    }
   for (GraphNode gn : locs.values()) {
      gn.outputXml(xw);
    }
   
   return locs.size();
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
   
   boolean shouldCheck() {
      if (node_location == null || node_reason == null) return false;
      if (node_location.getFile() == null) return false;
      if (!node_location.getFile().exists()) return false;
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
   
   File getFile() {
      return  node_location.getFile();
    }
   
   int getLineNumber() {
      return node_location.getLineNumber();
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

