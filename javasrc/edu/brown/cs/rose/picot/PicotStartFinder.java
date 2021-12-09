/********************************************************************************/
/*                                                                              */
/*              PicotStartFinder.java                                           */
/*                                                                              */
/*      Find appropriate starting method for a test case                        */
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



package edu.brown.cs.rose.picot;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.validate.ValidateFactory;

class PicotStartFinder implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PicotTestCreator        test_creator;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotStartFinder(PicotTestCreator ptc)
{
   test_creator = ptc;
}



/********************************************************************************/
/*                                                                              */
/*      Main entry to return starting points                                    */
/*                                                                              */
/********************************************************************************/

BudStackFrame findStartingPoint()
{
   RootProblem problem = test_creator.getProblem();
   RootControl ctrl = test_creator.getRootControl();
   
   // first find the frame containing potential errors
   // the actual starting point should be this or above this
   ValidateFactory validate = ValidateFactory.getFactory(test_creator.getRootControl());
   BudStackFrame frame0 = validate.getStartingFrame(problem,null,false);
   
   if (problem.getMaxUp() >= 0) return frame0;
   
   BudLaunch bl = new BudLaunch(test_creator.getRootControl(),problem);
   boolean fnd = false;
   Map<BudStackFrame,Double> scores = new HashMap<>();
   double score4 = 1.0;
   double depth = 1.0/bl.getStack().getFrames().size();
   for (BudStackFrame bsf : bl.getStack().getFrames()) {
      if (bsf.getFrameId().equals(frame0.getFrameId())) fnd = true;
      if (fnd) {
         if (!bsf.isUserFrame()) continue;
         File f = bsf.getSourceFile();
         String proj = ctrl.getProjectForFile(f);
         ASTNode stmt = ctrl.getSourceNode(proj,f,-1,bsf.getLineNumber(),true,true);
         ASTNode mthd = null;
         for (ASTNode p = stmt; p != null; p = p.getParent()) {
            if (p instanceof MethodDeclaration) {
               mthd = p;
               break;
             }
          }
         if (mthd != null) {
            JcompSymbol js = JcompAst.getDefinition(mthd);
            JcompTyper typer = JcompAst.getTyper(mthd);
            double score = 0;
            double score1 = 0;
            score = getProtection(js);
            if (js.isStatic()) score1 = 1.0;
            else score1 = getConstructorProtection(typer,js);
            int narg = js.getType().getComponents().size();
            double score2 = 1.0 - narg/10;
            if (score2 < 0) score2 = 0;
            double score3 = 1.0;
            for (JcompType ajt : js.getType().getComponents()) {
               score3 = Math.min(score3,getConstructorProtection(typer,ajt));
             }
            double sco = score * score1 * score2 * score3 * score4;
            if (sco > 0) scores.put(bsf,sco);
          }
       }
      score4 -= depth;
    }
   
   BudStackFrame best = null;
   double bscore = 0;
   for (Map.Entry<BudStackFrame,Double> ent : scores.entrySet()) {
      if (ent.getValue() > bscore) {
         best = ent.getKey();
         bscore = ent.getValue();
       }
    }
  
   return best;
}



private double getProtection(JcompSymbol js)
{
   if (js == null) return 0;
   
   double score = 0;
   if (js != null) {
      if (js.isPublic()) score = 1.0;
      else if (js.isPrivate()) score = 0.1;
      else if (js.isProtected()) score = 0.3;
      else score = 0.5;
    } 
   
   return score;
}



private double getConstructorProtection(JcompTyper typer,JcompSymbol js)
{
   if (js.isConstructorSymbol()) return getProtection(js);
   JcompType jt = js.getClassType();
   double s = getConstructorProtection(typer,jt);
   
   return s;
}


private double getConstructorProtection(JcompTyper typer,JcompType jt)
{
   double score = 0;
   JcompSymbol cjs = jt.getDefinition();
   if (cjs == null) return 1.0;
   
   double score1 = getProtection(cjs);
   int ct = 0;
   for (JcompSymbol js1 : jt.getDefinedMethods(typer)) {
      if (js1.isConstructorSymbol()) {
         ++ct;
         double s = getProtection(js1);
         if (s > score) score = s;
       }
    }
   if (ct == 0) score = 1.0;
   score = Math.min(score,score1);
   
   if (jt.getOuterType() != null && !cjs.isStatic()) {
      double s1 = getConstructorProtection(typer,jt.getOuterType());
      score = Math.min(score,s1);
    }
   
   return score;
}


}       // end of class PicotStartFinder




/* end of PicotStartFinder.java */

