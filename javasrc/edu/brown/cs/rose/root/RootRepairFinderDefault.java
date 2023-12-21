/********************************************************************************/
/*                                                                              */
/*              RootRepairFinderDefault.java                                    */
/*                                                                              */
/*      Default (generic) impolementation of a repiar finder                    */
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



package edu.brown.cs.rose.root;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.text.edits.TextEdit;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyFormat;

public abstract class RootRepairFinderDefault implements RootRepairFinder, RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootProcessor  bract_control;
private RootProblem   for_problem;
private RootLocation  at_location;
private Set<String>     done_repairs;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected RootRepairFinderDefault()
{
   bract_control = null;
   for_problem = null;
   at_location = null;
   done_repairs = new HashSet<>();
}


/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void setup(RootProcessor ctrl,RootProblem prob,RootLocation at)
{
   bract_control = ctrl;
   for_problem = prob;
   at_location = at;
   localSetup();
}


protected void localSetup()                     { }



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public boolean requiresLocation()     { return true; }

protected RootProcessor getProcessor()          { return bract_control; }

protected RootProblem getProblem()              { return for_problem; }

@Override public RootLocation getLocation()     { return at_location; }

protected void setLocation(RootLocation loc)
{
   at_location = loc;
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public abstract void process();

public abstract double getFinderPriority();



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected ASTNode getResolvedAstNodeForLocation(RootLocation loc)
{
   if (loc == null) loc = at_location;
   if (loc == null) loc = for_problem.getBugLocation();
   if (loc == null) return null;
   
   return bract_control.getController().getSourceNode(loc,true,false);
}



protected ASTNode getResolvedStatementForLocation(RootLocation loc)
{
   if (loc == null) loc = at_location;
   if (loc == null) loc = for_problem.getBugLocation();
   if (loc == null) return null;
   
   return bract_control.getController().getSourceNode(loc,true,true);
}

protected Statement getResolvedStatementOnly(RootLocation loc) 
{
   ASTNode n = getResolvedStatementForLocation(loc);
   if (n == null) return null;
   if (n instanceof Statement) return (Statement) n;
   return null;
}


/**
 *      Add a potential repair.  Given the ASTRewrite based on the AST returned
 *      from getResolved... and a priority.  The priority is between 0 and 1 and
 *      is used to scale the priority of the repair finder.
 **/ 

protected void addRepair(ASTRewrite rw,String desc,String logdata,double priority)
{
   addRepair(rw,null,desc,logdata,priority);
}


protected void addRepair(Element editxml,String desc,String logdata,double priority)
{
   addRepair(null,editxml,desc,logdata,priority);
}



private void addRepair(ASTRewrite rw,Element editxml,String desc,String logdata,double priority)
{
   if (rw == null && editxml == null) return;
   
   double pri = getFinderPriority();
   double p1 = getLocation().getPriority();
   if (p1 > 0) {
      p1 = 0.75 + p1 / 4.0;
      pri = (1+priority+p1)/3.0 * getFinderPriority();
    }
   else {
      pri = (1+priority)/2.0 * getFinderPriority();
    }

   File f = getLocation().getFile();
   IDocument doc1 = getProcessor().getController().getSourceDocument(f);
   Document doc = new Document(doc1.get());
   ASTNode stmt = getResolvedStatementForLocation(null);
   RootLocation loc = getLocation();
   Position pos = new Position(loc.getStartOffset());
   Position pos1 = new Position(stmt.getStartPosition());
   Position pos2 = new Position(stmt.getStartPosition() + stmt.getLength());
   int baseline = 0;
   int baseline1 = 0;
   int baseline2 = 0;
   try {
      doc.addPosition(pos);
      doc.addPosition(pos1);
      doc.addPosition(pos2);
      baseline = doc.getLineOfOffset(pos.getOffset());
      baseline1 = doc.getLineOfOffset(pos1.getOffset());
      baseline2 = doc.getLineOfOffset(pos2.getOffset());
    }
   catch (BadLocationException e) {
      pos = null;
    }
   
   TextEdit te = null;
   if (rw != null) {
      try {
         te = rw.rewriteAST(doc,null);
       }
      catch (Throwable e) {
         RoseLog.logE("ROOT","Problem creating text edit from rewrite",e);
       } 
    }
   else if (editxml != null) {
      RootEdit ed = new RootEdit(editxml);
      RoseLog.logD("ROOT","FOUND EDIT " + ed);
      te = ed.getTextEdit();
    }
   if (te == null) return;
   RoseLog.logD("ROOT","Edit to apply: " + te);
   
   RootLineMap vlm = null;
   if (pos != null) {
      try {
         TextEdit te1 = te.copy();
         te1.apply(doc);
         if (isRepairDone(doc.get())) return;
         int newline = doc.getLineOfOffset(pos.getOffset());
         int newline1 = doc.getLineOfOffset(pos1.getOffset());
         int newline2 = doc.getLineOfOffset(pos2.getOffset());
         if (baseline != newline || baseline1 != newline1 || baseline2 != newline2) {
            vlm = new RootLineMap(f,baseline1,newline1,
                  baseline,newline,baseline2,newline2);
          }
         doc.removePosition(pos);
         doc.removePosition(pos1);
         doc.removePosition(pos2);
       }
      catch (BadLocationException e) { }
    }
   
   RootRepair rr = new RootRepairDefault(this,desc,pri,loc,te,vlm,logdata);
   getProcessor().validateRepair(rr);
}



private boolean isRepairDone(String text)
{
   String rslt = IvyFile.digestString(text);
   if (done_repairs.add(rslt)) return false;
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected boolean isSameLine(ASTNode stmt,ASTNode node)
{
   ASTNode nodestmt = node;
   while (nodestmt != null) {
      if (nodestmt instanceof Statement) break;
      nodestmt = nodestmt.getParent();
    }
   if (nodestmt == null) return true;
   if (nodestmt == stmt) return true;
   
   CompilationUnit cu = (CompilationUnit) stmt.getRoot();
   int lno = cu.getLineNumber(stmt.getStartPosition());
   int lno1 = cu.getLineNumber(nodestmt.getStartPosition());
   
   return lno == lno1;
}



/********************************************************************************/
/*                                                                              */
/*      Compute a default description                                           */
/*                                                                              */
/********************************************************************************/

protected String computeDescription(ASTNode patchnode,int sourceposition,int sourcelength,
      String patchcontents,String filecontents)
{
   // get relevant node and node type
   String what = null;
   ASTNode usenode = patchnode;
   ASTNode prevnode = usenode;
   
   if (sourceposition < 0) {
      sourceposition = patchnode.getStartPosition();
      sourcelength = patchnode.getLength();
    }
   
   while (usenode != null && (usenode instanceof Expression || usenode instanceof Type)) {
      prevnode = usenode;
      switch (usenode.getNodeType()) {
         case ASTNode.METHOD_INVOCATION :
         case ASTNode.CONSTRUCTOR_INVOCATION :
         case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
            what = "Call";
            break;
         case ASTNode.CLASS_INSTANCE_CREATION :
            what = "New";
            break;
         case ASTNode.LAMBDA_EXPRESSION :
            what = "Lambda";
            break;
         case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
            what = "Declaration";
            break;
         default :
            break;
       }
      if (what != null) break;
      usenode = usenode.getParent();
    }
   
   // now get original text and patched text
   int pstart1 = prevnode.getStartPosition();
   int plen1 = prevnode.getLength();
   String origtxt = filecontents.substring(pstart1,pstart1+plen1);
   String txt0 = origtxt.substring(0,sourceposition-pstart1);
   String txt1 = patchcontents;
   String txt2 = origtxt.substring(sourceposition-pstart1+sourcelength);
   String newtxt = txt0 + txt1 + txt2;
   
   origtxt = IvyFormat.formatString(compress(origtxt));
   newtxt = IvyFormat.formatString(compress(newtxt));
   
   return "Use " + newtxt + " instead of " + trunc(origtxt,24);
}



protected String trunc(String txt,int len)
{
   if (txt.length() < len) return txt;
   return txt.substring(0,len-2) + "..";
}


protected String compress(String text)
{
   StringBuffer buf = new StringBuffer();
   char havespace = 0;
   for (int i = 0; i < text.length(); ++i) {
      char ch = text.charAt(i);
      if (Character.isWhitespace(ch)) {
         if (havespace == 0) {
            havespace = ch;
          }
       }
      else {
         if (havespace != 0) buf.append(havespace);
         havespace = 0;
         buf.append(ch);
       }
    }
   
   return buf.toString();
}





}       // end of class RootRepairFinderDefault




/* end of RootRepairFinderDefault.java */

