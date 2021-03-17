/********************************************************************************/
/*                                                                              */
/*              SepalCommonProblems.java                                        */
/*                                                                              */
/*      Suggest repairs based on common Java problems                           */
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



package edu.brown.cs.rose.sepal;

import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.bract.BractAstPattern;
import edu.brown.cs.rose.bract.BractConstants;
import edu.brown.cs.rose.root.RootRepairFinderDefault;

public class SepalCommonProblems extends RootRepairFinderDefault implements BractConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final BractAstPattern cond_pattern;
private static final BractAstPattern cond_result;

private static final BractAstPattern equal_pattern;
private static final BractAstPattern neq_pattern;
private static final BractAstPattern equal_result;
private static final BractAstPattern neq_result;

private static final BractAstPattern string_call;
private static final BractAstPattern string_result;

private static final BractAstPattern loop_pattern;
private static final BractAstPattern loop_result;

private static final BractAstPattern assign_pattern;
private static final BractAstPattern assign_result;

private static final BractAstPattern mult_pattern;
private static final BractAstPattern mult_result;

static {
   cond_pattern = BractAstPattern.expression("Ex = Ey");
   cond_result = BractAstPattern.expression("Ex == Ey");
   
   equal_pattern = BractAstPattern.expression("Ex == Ey");
// equal_result = BractAstPattern.expression("((Vx != null && Vx.equals(Vy)) || (Vx == null && Vy == null))");
   equal_result = BractAstPattern.expression("Ex.equals(Ey)");
   
   neq_pattern = BractAstPattern.expression("Ex != Ey");
// equal_result = BractAstPattern.expression("((Vx != null && !Vx.equals(Vy)) || (Vx == null && Vy != null))");
   neq_result = BractAstPattern.expression("!(Ex.equals(Ey))");
   
   string_call = BractAstPattern.expression("Vx.Vm()","Vx.Vm(Ea)","Vx.Vm(Ea,Eb)","Vx.Vm(Ea,Eb,Ec)");
   string_result = BractAstPattern.expression("Vx = Vx.Vm(Ea,Eb,Ec)","Vx = Vx.Vm(Ea,Eb)","Vx = Vx.Vm(Ea)",
         "Vx = Vx.Vm()");
   
   loop_pattern = BractAstPattern.statement("for (int Vi = 1; Vi <= Emax; ++Vi) Sbody;");
   loop_result = BractAstPattern.statement("for (int Vi = 0; Vi < Emax; ++Vi) Sbody;");
   
   assign_pattern = BractAstPattern.expression("Ex == Ey");
   assign_result = BractAstPattern.expression("Ex = Ey");
   
   mult_pattern = BractAstPattern.expression("Ex*Ey == 0","(Ex*Ey) == 0");
   mult_result = BractAstPattern.expression("(Ex == 0 || Ey == 0)");
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalCommonProblems()
{ }


/********************************************************************************/
/*                                                                              */
/*      <comment here>                                                          */
/*                                                                              */
/********************************************************************************/


@Override public void process()
{
   ASTNode stmt = getResolvedStatementForLocation(null);
   if (stmt == null) return;
   
   checkAssignInConditional(stmt);
   checkStringEquality(stmt);
   checkStringOperations(stmt);
   checkLoopIndex(stmt);
   checkNonAssignment(stmt);
   checkMultiplyForZero(stmt);
}


@Override protected double getFinderPriority()
{
   return 0.75;
}


/********************************************************************************/
/*                                                                              */
/*      Check for (x = y) as a conditional                                      */
/*                                                                              */
/********************************************************************************/

private void checkAssignInConditional(ASTNode stmt)
{
   Map<ASTNode,PatternMap> rslt = cond_pattern.matchAll(stmt,null);
   if (rslt == null) return;
   for (ASTNode n : rslt.keySet()) {
      ASTNode p = n.getParent();
      PatternMap pmap = rslt.get(n);
      switch (p.getNodeType()) {
         case ASTNode.IF_STATEMENT :
         case ASTNode.WHILE_STATEMENT :
         case ASTNode.DO_STATEMENT :
            ASTRewrite rw = cond_result.replace(n,pmap);
            if (rw != null) {
               String desc = "Use " + pmap.get("x") + "==" + pmap.get("y") + " instead of =";
               addRepair(rw,desc,1.0);
             }
            break;
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check for x == y where x and y are strings                              */
/*                                                                              */
/********************************************************************************/

private void checkStringEquality(ASTNode stmt)
{
   stringCompare(stmt,equal_pattern,equal_result);
   stringCompare(stmt,neq_pattern,neq_result);
}



private void stringCompare(ASTNode stmt,BractAstPattern pat,BractAstPattern repl)
{
   Map<ASTNode,PatternMap> rslt = pat.matchAll(stmt,null);
   if (rslt == null) return;
   for (ASTNode n : rslt.keySet()) {
      PatternMap vals = rslt.get(n);
      ASTNode lhs = (ASTNode) vals.get("x");
      ASTNode rhs = (ASTNode) vals.get("y");
      JcompType lht = JcompAst.getExprType(lhs);
      JcompType rht = JcompAst.getExprType(rhs);
      if (lht.isStringType() || rht.isStringType()) {
         if (lht.isAnyType() || rht.isAnyType()) return;
         ASTRewrite rw = repl.replace(n,vals);
         if (rw != null) {
            String desc = null;
            if (pat == equal_pattern) {
               desc = "Use " + vals.get("x") + ".equals(" + vals.get("y") + ")";
               desc += " instead of ==";
             }
            else {
               desc = "Use !" + vals.get("x") + ".equals(" + vals.get("y") + ")";
               desc += " instead of !=";
             }
            addRepair(rw,desc,0.75);
          }
       }
    } 
}




/********************************************************************************/
/*                                                                              */
/*      Check s.toLowerCase() without assignment                                */
/*                                                                              */
/********************************************************************************/

private void checkStringOperations(ASTNode stmt)
{ 
   if (!(stmt instanceof ExpressionStatement)) return;
   
   ExpressionStatement estmt = (ExpressionStatement) stmt;
   Expression expr = estmt.getExpression();
   PatternMap vals = new PatternMap();
   if (!string_call.match(expr,vals)) return;
   Map<ASTNode,PatternMap> rslt = string_call.matchAll(stmt,null);
   if (rslt == null) return;
   ASTNode lhs = (ASTNode) vals.get("x");
   JcompType lht = JcompAst.getExprType(lhs);
   if (!lht.isStringType()) return;
   SimpleName mthd = (SimpleName) vals.get("m");
   switch (mthd.getIdentifier()) {
      case "concat" :
      case "intern" :
      case "replace" :
      case "replaceAll" :
      case "replaceFirst" :
      case "substring" :
      case "toLowerCase" :
      case "toString" :
      case "toUpperCase" :
      case "trim" :
         ASTRewrite rw = string_result.replace(expr,vals);
         if (rw != null) {
            String desc = "Assign result of " + mthd + " to " + lhs;
            addRepair(rw,desc,0.9);
          }
         break;
      default :
         break;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check for with starting index 1 rather than 0                           */
/*                                                                              */
/********************************************************************************/
 
private void checkLoopIndex(ASTNode stmt)
{ 
   PatternMap rslt = new PatternMap();
   if (loop_pattern.match(stmt,rslt)) {
      ASTRewrite rw = loop_result.replace(stmt,rslt);
      if (rw != null) {
         String desc = "Change for to loop from 0 to " + rslt.get("max") + "-1";
         addRepair(rw,desc,0.5);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check for (x = y) as a conditional                                      */
/*                                                                              */
/********************************************************************************/

private void checkNonAssignment(ASTNode stmt)
{
   Map<ASTNode,PatternMap> rslt = assign_pattern.matchAll(stmt,null);
   if (rslt == null) return;
   for (ASTNode n : rslt.keySet()) {
      ASTNode p = n.getParent();
      switch (p.getNodeType()) {
         case ASTNode.EXPRESSION_STATEMENT :
            PatternMap pmap = rslt.get(n);
            ASTRewrite rw = assign_result.replace(n,pmap);
            if (rw != null) {
               String desc = "Use " + pmap.get("x") + " = ... rather than ==";
               addRepair(rw,desc,0.9);
             }
            break;
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check for x*y == 0                                                      */
/*                                                                              */
/********************************************************************************/

private void checkMultiplyForZero(ASTNode stmt)
{
   Map<ASTNode,PatternMap> rslt = mult_pattern.matchAll(stmt,null);
   if (rslt == null) return;
   for (ASTNode n : rslt.keySet()) {
      PatternMap pmap = rslt.get(n);
      ASTRewrite rw = mult_result.replace(n,pmap);
      if (rw != null) {
         String desc = "Use " + pmap.get("x") + " == 0 || " + pmap.get("y") + " == 0";
         desc += " instead of multiplication";   
         addRepair(rw,desc,0.9);
       }
    }
}





}       // end of class SepalCommonProblems




/* end of SepalCommonProblems.java */

