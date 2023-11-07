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

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.bract.BractAstPattern;
import edu.brown.cs.rose.bract.BractConstants;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
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

private static final BractAstPattern and_pattern;
private static final BractAstPattern and_result;

private static final BractAstPattern overflow_pattern;
private static final BractAstPattern overflow_result;

private static final BractAstPattern int_double_pattern;
private static final BractAstPattern int_double_result;

private static final BractAstPattern to_string_pattern;
private static final BractAstPattern to_string_result;

private static final BractAstPattern substring_pattern;
private static final BractAstPattern substring_result;

private static final BractAstPattern zero_pattern;
private static final BractAstPattern zero_result;


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
   
   and_pattern = BractAstPattern.expression("Ex^(Ex-1)");
   and_result = BractAstPattern.expression("Ex&(Ex-1)");
   
   overflow_pattern = BractAstPattern.expression("(double) (Ex*Ey)/Ez","((double) (Ex*Ey))/Ez");
   overflow_result = BractAstPattern.expression("Ex * (((double) Ey)/Ez)");
   
   int_double_pattern = BractAstPattern.expression("Ex / Ic");
   int_double_result = BractAstPattern.expression("Ex / Ecf");
   
   to_string_pattern = BractAstPattern.expression("Ex.toString()");
   to_string_result = BractAstPattern.expression("XString.valueOf(Ex)");
   
   substring_pattern = BractAstPattern.expression("Ex.substring(Ey)");
   substring_result = BractAstPattern.expression("Ex.substring(Ey+1)");
   
   zero_pattern = BractAstPattern.expression("\"\\\\0\"");
   zero_result = BractAstPattern.expression("\"\\\\000\"");
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
/*      Handle common problem based suggestions                                 */
/*                                                                              */
/********************************************************************************/

@Override public void process()
{
   ASTNode stmt = getResolvedStatementForLocation(null);
   if (stmt == null) return;
   if (stmt instanceof AssertStatement) return;
   
   checkAssignInConditional(stmt);
   checkStringEquality(stmt);
   checkStringOperations(stmt);
   checkLoopIndex(stmt);
   checkNonAssignment(stmt);
   checkSimplePattern(stmt,mult_pattern,mult_result,0.9);
   checkSimplePattern(stmt,and_pattern,and_result,0.75);
   checkSimplePattern(stmt,overflow_pattern,overflow_result,0.75);
   checkSimplePattern(stmt,substring_pattern,substring_result,0.60);
   checkSimplePattern(stmt,zero_pattern,zero_result,0.60);
   checkConversion(stmt);
   checkToString(stmt);
}


@Override public double getFinderPriority()
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
   
   String logdata = getClass().getName();
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
               addRepair(rw,desc,logdata + "@ASSIGNCOND",1.0);
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
   
   String logdata = getClass().getName();
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
            addRepair(rw,desc,logdata + "STRINGCOMP",0.75);

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
   
   String logdata = getClass().getName();
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
            addRepair(rw,desc,logdata + "@STRINGOP",0.9);
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
   String logdata = getClass().getName();
   if (loop_pattern.match(stmt,rslt)) {
      ASTRewrite rw = loop_result.replace(stmt,rslt);
      if (rw != null) {
         String desc = "Change for to loop from 0 to " + rslt.get("max") + "-1";
         addRepair(rw,desc,logdata + "@LOOPINDEX",0.5);
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
   
   String logdata = getClass().getName();
   for (ASTNode n : rslt.keySet()) {
      ASTNode p = n.getParent();
      switch (p.getNodeType()) {
         case ASTNode.EXPRESSION_STATEMENT :
            PatternMap pmap = rslt.get(n);
            ASTRewrite rw = assign_result.replace(n,pmap);
            if (rw != null) {
               String desc = "Use " + pmap.get("x") + " = ... rather than ==";
               addRepair(rw,desc,logdata + "@NONASSIGN",0.9);
             }
            break;
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check for simple expression patterns                                    */
/*                                                                              */
/********************************************************************************/

private void checkSimplePattern(ASTNode stmt,BractAstPattern p1,BractAstPattern p2,double v)
{
   Map<ASTNode,PatternMap> rslt = p1.matchAll(stmt,null);
   if (rslt == null || rslt.isEmpty()) return;
   for (ASTNode n : rslt.keySet()) {
      if (!isSameLine(stmt,n)) continue;
      PatternMap pmap = rslt.get(n);
      ASTRewrite rw = p2.replace(n,pmap);
      if (rw != null) {
         ASTNode tgt = p2.getResult(n,pmap);
         String desc = "Use " + tgt + " instead of " + n;
         String logdata = getClass().getName() + "@" + p1.getSummary();
         addRepair(rw,desc,logdata,v);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle conversion errors                                                */
/*                                                                              */
/********************************************************************************/

private void checkConversion(ASTNode stmt)
{
   String logdata = getClass().getName();
   Map<ASTNode,PatternMap> rslt = int_double_pattern.matchAll(stmt,null);
   if (rslt != null) {
      for (ASTNode n : rslt.keySet()) {
         if (!isDivideConversionRelevant(n)) continue;
         PatternMap omap = rslt.get(n);
         int iv = ((Integer) omap.get("c"));
         String newcon = null;
         JcompType typ = getExpectedType(n);
         if (typ != null && typ.isFloatType()) newcon = String.valueOf(iv) + "f";
         else newcon = String.valueOf(iv) + ".0";
         AST ast = n.getAST();
         NumberLiteral nc = ast.newNumberLiteral(newcon);
         omap.put("cf",nc);
         ASTRewrite rw = int_double_result.replace(n,omap);
         String desc = "Replace " + iv + " with " + newcon;
         if (rw != null) addRepair(rw,desc,logdata + "@INT_DOUBLE",0.75);
       }
    }
}




private boolean isDivideConversionRelevant(ASTNode n)
{
   // this is made complicated since compilation is local, not global
   if (!(n instanceof InfixExpression)) return false;
   InfixExpression infix = (InfixExpression) n;
   JcompType typl = JcompAst.getExprType(infix.getLeftOperand());
   if (!typl.isIntType()) return false;
   
   JcompType typrslt = getExpectedType(n);
   if (typrslt != null && typrslt.isFloatingType()) return true; 
 
   typrslt = JcompAst.getExprType(infix);
   if (typrslt != null && typrslt.isFloatingType()) return true;
   ASTNode par = infix.getParent();
   if (par instanceof MethodInvocation) {
      MethodInvocation pmi = (MethodInvocation) par;
      for (Object o : pmi.arguments()) {
         ASTNode argv = (ASTNode) o;
         JcompType atyp = JcompAst.getExprType(argv);
         if (atyp.isFloatingType()) return true;
         if (atyp.isErrorType()) return true;
       }
      if (pmi.arguments().size() > 1) return true;
    }
   if (typrslt != null && typrslt.isNumericType()) return false;
   
   return false;
}



private JcompType getExpectedType(ASTNode n)
{
   ASTNode par = n.getParent();
   if (par == null) return null;
   JcompSymbol js = null;
   
   switch (par.getNodeType()) {
      case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
        js = JcompAst.getDefinition(par);
        if (js != null) return js.getType();
        break;
      case ASTNode.PARENTHESIZED_EXPRESSION :
         return getExpectedType(par);
      case ASTNode.METHOD_INVOCATION :
         MethodInvocation mi = (MethodInvocation) par;
         if (n.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
            js = JcompAst.getReference(par);
            if (js != null) {
               int idx = mi.arguments().indexOf(n);
               if (idx >= 0) {
                  List<JcompType> argtypes = js.getType().getComponents();
                  if (idx < argtypes.size()) {
                     return argtypes.get(idx);
                   }
                }
             }
            return JcompAst.getJavaType(mi);            // ERROR or null
          }
         break;
      case ASTNode.INFIX_EXPRESSION :
         InfixExpression ie = (InfixExpression) par;
         Expression alt = null;
         if (n.getLocationInParent() == InfixExpression.LEFT_OPERAND_PROPERTY) 
            alt = ie.getRightOperand();
         else if (n.getLocationInParent() == InfixExpression.RIGHT_OPERAND_PROPERTY)
            alt = ie.getLeftOperand();
         if (alt != null) {
            JcompType t1 = JcompAst.getExprType(alt);
            if (t1!= null && t1.isFloatingType()) return t1;
          }
         break;
    }
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Convert toString to String.valueOf                                      */
/*                                                                              */
/********************************************************************************/

private void checkToString(ASTNode stmt)
{
   RootProblem rp = getProblem();
   if (rp.getProblemType() != RoseProblemType.EXCEPTION) return;
   if (!rp.getProblemDetail().equals("java.lang.NullPointerException")) return;
   
   RootLocation ploc = rp.getBugLocation();
   ASTNode bstmt = getResolvedStatementForLocation(ploc);
   if (bstmt != stmt) return;
   ASTNode exc = getProcessor().getController().getExceptionNode(rp);
   if (exc == null) return;
   
   Map<ASTNode,PatternMap> rslt = to_string_pattern.matchAll(stmt,null);
   if (rslt != null) {
      for (ASTNode n : rslt.keySet()) {
         ASTNode prior = null;
         for (ASTNode n1 = exc; n1 instanceof Expression; n1 = n1.getParent()) {
            if (n1 == n) break;
            prior = n1;
          }
         if (prior == null || prior.getParent() != n) continue;
         if (prior.getLocationInParent() != MethodInvocation.EXPRESSION_PROPERTY) continue;
         
         PatternMap omap = rslt.get(n);
         ASTRewrite rw = to_string_result.replace(n,omap);
         String logdata = getClass().getName();
         String desc = "Replace toString with String.valueOf in " + n;
         if (rw != null) addRepair(rw,desc,logdata + "@TOSTRING",0.75);
       }
    }
}




}       // end of class SepalCommonProblems




/* end of SepalCommonProblems.java */

