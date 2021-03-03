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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.text.edits.TextEdit;

import edu.brown.cs.ivy.file.IvyStringDiff;

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

protected RootLocation getLocation()            { return at_location; }




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public abstract void process();

protected abstract double getFinderPriority();



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


/**
 *      Add a potential repair.  Given the ASTRewrite based on the AST returned
 *      from getResolved... and a priority.  The priority is between 0 and 1 and
 *      is used to scale the priority of the repair finder.
 **/ 

protected void addRepair(ASTRewrite rw,String desc,double priority)
{
   if (rw == null) return;
   double pri = getFinderPriority();
   double p1 = getLocation().getPriority();
   if (p1 > 0) {
      pri = (1+priority+p1)/3.0 * getFinderPriority();
    }
   else {
      pri = (1+priority)/2.0 * getFinderPriority();
    }

   File f = getLocation().getFile();
   String p = getLocation().getProject();
   IDocument doc1 = getProcessor().getController().getSourceDocument(p,f);
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
   try {
      te = rw.rewriteAST(doc,null);
    }
   catch (Throwable e) {
      RoseLog.logE("ROOT","Problem creating text edit from rewrite",e);
    } 
   if (te == null) return;
   
   int delta = 0;
   if (pos != null) {
      try {
         te.apply(doc);
         int newline = doc.getLineOfOffset(pos.getOffset());
         int newline1 = doc.getLineOfOffset(pos1.getOffset());
         int newline2 = doc.getLineOfOffset(pos2.getOffset());
         if (newline != baseline || newline1 != baseline1 || newline2 != baseline2) {
            // newline :: baseline might be good enough
            int min = Math.min(newline,Math.min(newline1,newline2));
            int max = Math.max(newline,Math.max(newline1,newline2));
            delta = getLineDelta(doc1,baseline1,doc,min,max);
          }
         doc.removePosition(pos);
         doc.removePosition(pos1);
         doc.removePosition(pos2);
       }
      catch (BadLocationException e) { }
    }
   
   if (delta != 0) {
      System.err.println("LINE CHANGED BY " + delta);
    }
   
   RootRepair rr = new RootRepairDefault(this,desc,pri,loc,te);
   getProcessor().validateRepair(rr);
}




private int getLineDelta(IDocument d1,int oln,IDocument d2,int sln,int eln)
{
   int delta = 0;
   try {
      IRegion rgn = d1.getLineInformation(oln);
      String orig = d1.get(rgn.getOffset(),rgn.getLength()).trim();
      double best = 0;
      for (int i = sln-1; i <= eln+1; ++i) {
         IRegion rgn1 = d2.getLineInformation(i);
         String match = d2.get(rgn1.getOffset(),rgn1.getLength()).trim();
         double diff = IvyStringDiff.normalizedStringDiff(orig,match);
         if (diff > best) {
            best = diff;
            delta = i-oln;
          }
       }
    }
   catch (BadLocationException e) { }
   
   return delta;
}

}       // end of class RootRepairFinderDefault




/* end of RootRepairFinderDefault.java */

