/********************************************************************************/
/*                                                                              */
/*              SepalForLoopIndex.java                                          */
/*                                                                              */
/*      Handle issues with a for loop index                                     */
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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.rose.bract.BractAstPattern;
import edu.brown.cs.rose.bract.BractConstants;
import edu.brown.cs.rose.root.RootRepairFinderDefault;

public class SepalForLoopIndex extends RootRepairFinderDefault
        implements BractConstants 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static BractAstPattern match_pattern;
private static BractAstPattern incr_pattern;
private static PatternMap match_values;
private static BractAstPattern result_pattern;
private static BractAstPattern delta_result;

static {
   incr_pattern = BractAstPattern.expression(
         new PatternMap("delta",1),
         "++Vi", "Vi++", "Vi = Vi+Idelta","Vi = Idelta + Vi",
         "Vi+=Idelta");
   match_pattern = BractAstPattern.statement( 
         "for (int Vi = Iinit; Vi <= Varray.length; Pincr()) Sbody;",
         "for (int Vi = Iinit; Vi <= Vcoll.size(); Pincr()) Sbody;",
         "for (int Vi = Iinit; Vi < Varray.length; Pincr()) Sbody;",
         "for (int Vi = Iinit; Vi < Vcoll.size(); Pincr()) Sbody;");
   
   match_values = new PatternMap("incr",incr_pattern);
   
   result_pattern = BractAstPattern.expression(
         "Vi < Varray.length", "Vi < Vcoll.size()");
   delta_result = BractAstPattern.expression(
         "Vi < Varray.length - Id1", "Vi < Vcoll.size() - Id1");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalForLoopIndex()
{ }



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void process()
{
   ASTNode stmt = getResolvedStatementForLocation(null);
   if (stmt == null) return;
   if (stmt.getNodeType() != ASTNode.FOR_STATEMENT) return;
   
   PatternMap nmap = new PatternMap(match_values);
   if (match_pattern.match(stmt,nmap)) {
      ForStatement fs = (ForStatement) stmt;
      InfixExpression ex = (InfixExpression) fs.getExpression();
      ASTRewrite rw = null;
      int iv = ((Integer) nmap.get("delta"));
      if (iv > 1) {
         nmap.put("d1",iv-1);
         rw = delta_result.replace(ex,nmap);
       }
      
      else if (ex.getOperator() == InfixExpression.Operator.LESS_EQUALS) {
         rw = result_pattern.replace(ex,nmap);
       }
      if (rw != null) {
         addRepair(rw,"Replace <= with < in FOR expression",0.75);
       }
    }
}


@Override protected double getFinderPriority()
{
   return 0.5;
}




}       // end of class SepalForLoopIndex




/* end of SepalForLoopIndex.java */

