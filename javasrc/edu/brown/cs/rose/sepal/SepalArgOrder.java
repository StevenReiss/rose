/********************************************************************************/
/*                                                                              */
/*              SepalArgOrder.java                                              */
/*                                                                              */
/*      Hnadle changing argument order on vulnerable calls                      */
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

import edu.brown.cs.rose.root.RootRepairFinderDefault;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.bract.BractAstPattern;
import edu.brown.cs.rose.bract.BractConstants;

public class SepalArgOrder extends RootRepairFinderDefault implements BractConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final BractAstPattern call_pattern;

static {
   call_pattern = BractAstPattern.expression("Vrtn(Ea,Eb)");
}

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalArgOrder()
{ }


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public double getFinderPriority()
{
   return 0.33;
}




@Override public void process()
{
   ASTNode stmt = getResolvedStatementForLocation(null);
   if (stmt == null) return;
   
   Map<ASTNode,PatternMap> matches = call_pattern.matchAll(stmt,null);
   if (matches == null || matches.isEmpty()) return;
   
   for (ASTNode n : matches.keySet()) {
      MethodInvocation mi = (MethodInvocation) n;
      JcompSymbol callee = JcompAst.getReference(mi);
      if (!isCalleeRelevant(callee)) continue;
      for (Point p : getSwaps(mi)) {
         flipArgs(mi,p.x,p.y);
       }
    }
}



private boolean isCalleeRelevant(JcompSymbol callee)
{
   if (callee == null) return false;
   if (callee.isBinarySymbol()) return false;
   JcompType jt = callee.getType();
   while (jt.isParameterizedType()) jt = jt.getBaseType();
   JcompType t0 = null;
   for (JcompType jt1 : jt.getComponents()) {
      if (t0 == null) t0 = jt1;
      else if (t0 != jt1) return false;
    }
   
   return true;
}


private List<Point> getSwaps(MethodInvocation mi)
{
   Point pt = new Point(0,1);
   return List.of(pt);
}



@SuppressWarnings("unchecked")
private void flipArgs(MethodInvocation mi,int arg0,int arg1)
{
   AST ast = mi.getAST();
   MethodInvocation rslt = ast.newMethodInvocation();
   rslt.setName(ast.newSimpleName(mi.getName().getIdentifier()));
   List<?> args = mi.arguments();
   for (int i = 0; i < args.size(); ++i) {
      Expression a = null;
      if (i == arg0) {
         a = (Expression) args.get(arg1);
       }
      else if (i == arg1) {
         a = (Expression) args.get(arg0);
       }
      else a = (Expression) args.get(i);
      rslt.arguments().set(i,ASTNode.copySubtree(ast,a));
    }
   ASTRewrite rw = ASTRewrite.create(ast);
   rw.replace(mi,rslt,null);
   addRepair(rw,"Change argument order in call to " + mi.getName(),0.5);
}







}       // end of class SepalArgOrder




/* end of SepalArgOrder.java */

