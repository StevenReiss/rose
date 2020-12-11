/********************************************************************************/
/*                                                                              */
/*              StemQueryExpressions.java                                       */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.rose.stem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RoseException;

class StemQueryExpressions extends StemQueryBase implements StemConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemQueryExpressions(StemMain ctrl,Element xml)
{
   super(ctrl,xml);
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process(StemMain sm,IvyXmlWriter xw) throws RoseException
{
   ASTNode node = getSourceStatement();
   if (node == null) return;
   
   Map<String,ASTNode> exprs = new LinkedHashMap<>();
   ExprFinder exprfinder = new ExprFinder(exprs);
   
   while (node != null) {
      node.accept(exprfinder); 
      StructuralPropertyDescriptor spd = node.getLocationInParent();
      if (spd.isChildListProperty()) {
         ASTNode par = node.getParent();
         List<?> chlds = (List<?>) par.getStructuralProperty(spd);
         int idx = chlds.indexOf(node);
         if (idx == 0) node = par.getParent();
         else node = (ASTNode) chlds.get(idx-1);
         node = getStatementOf(node);
       }
      else if (spd.isChildProperty()) {
         node = getStatementOf(node.getParent());
       }
    }
   
   xw.begin("RESULT");
   for (ASTNode n : exprs.values()) {
      addXmlForLocation("EXPR",n,true,xw);
    }
   xw.end("RESULT");
}







private static class ExprFinder extends ASTVisitor {
   
   private Map<String,ASTNode> expr_set;
   
   ExprFinder(Map<String,ASTNode> exprs) {
      expr_set = exprs;
    }
   
   @Override public void postVisit(ASTNode n) {
      if (n instanceof Expression) {
         switch (n.getNodeType()) {
            case ASTNode.NORMAL_ANNOTATION :
            case ASTNode.MARKER_ANNOTATION :
            case ASTNode.SINGLE_MEMBER_ANNOTATION :
            case ASTNode.ARRAY_INITIALIZER :
            case ASTNode.ASSIGNMENT :
            case ASTNode.BOOLEAN_LITERAL :
            case ASTNode.CAST_EXPRESSION :
            case ASTNode.CHARACTER_LITERAL :
            case ASTNode.CLASS_INSTANCE_CREATION :
            case ASTNode.CONDITIONAL_EXPRESSION :
            case ASTNode.INSTANCEOF_EXPRESSION :
            case ASTNode.LAMBDA_EXPRESSION :
            case ASTNode.SIMPLE_NAME :
            case ASTNode.NULL_LITERAL :
            case ASTNode.PARENTHESIZED_EXPRESSION :
            case ASTNode.POSTFIX_EXPRESSION :
            case ASTNode.STRING_LITERAL :
            case ASTNode.SWITCH_EXPRESSION :
            case ASTNode.THIS_EXPRESSION :
//          case ASTNode.TEXT_BLOCK :
            case ASTNode.TYPE_LITERAL :
            case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
               return;
            case ASTNode.PREFIX_EXPRESSION :
               PrefixExpression pfx = (PrefixExpression) n;
               PrefixExpression.Operator op = pfx.getOperator();
               if (op == PrefixExpression.Operator.DECREMENT  ||
                     op == PrefixExpression.Operator.INCREMENT)
                     return;
               break;
            case ASTNode.INFIX_EXPRESSION :
               InfixExpression ifx = (InfixExpression) n;
               InfixExpression.Operator iop = ifx.getOperator();
               if (iop == InfixExpression.Operator.CONDITIONAL_AND ||
                     iop == InfixExpression.Operator.CONDITIONAL_OR)
                  return;
               break;
            default :
               break;
          }
         String exp = n.toString();
         if (expr_set.get(exp) == null) {
            expr_set.put(exp,n);
          }
       }
    }
   
}       // end of inner class ExprFinder



}       // end of class StemQueryExpressions




/* end of StemQueryExpressions.java */

