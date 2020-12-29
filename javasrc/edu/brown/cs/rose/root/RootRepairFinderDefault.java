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
import org.eclipse.text.edits.TextEdit;

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
}



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
      pri = (1+priority+(p1/MAX_NODE_PRIORITY))/3.0 * getFinderPriority();
    }
   else {
      pri = (1+priority)/2.0 * getFinderPriority();
    }

   File f = getLocation().getFile();
   String p = getLocation().getProject();
   
   TextEdit te = null;
   try {
      te = rw.rewriteAST(getProcessor().getController().getSourceDocument(p,f),null);
    }
   catch (Throwable e) {
      RoseLog.logE("ROOT","Problem creating text edit from rewrite",e);
    } 
   if (te == null) return;
   
   RootRepair rr = new RootRepairDefault(this,desc,pri,getLocation(),te);
   getProcessor().sendRepair(rr);
}

}       // end of class RootRepairFinderDefault




/* end of RootRepairFinderDefault.java */

