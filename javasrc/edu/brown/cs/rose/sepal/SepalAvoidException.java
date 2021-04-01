/********************************************************************************/
/*                                                                              */
/*              SepalAvoidException.java                                        */
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



package edu.brown.cs.rose.sepal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootRepairFinderDefault;

public class SepalAvoidException extends RootRepairFinderDefault
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

public SepalAvoidException()
{ }



/********************************************************************************/
/*                                                                              */
/*      Basic Processing methods                                                */
/*                                                                              */
/********************************************************************************/

@Override public double getFinderPriority()
{
   return 0.5;
}



@Override public void process()
{
   ConditionMaker cm = null;
   RootProblem rp = getProblem();
   Statement stmt = (Statement) getResolvedStatementForLocation(null);
   RootLocation ploc = rp.getBugLocation();
   ASTNode bstmt = getResolvedStatementForLocation(ploc);
   if (stmt != bstmt) {
      // if the erroreous value is a variable computed here, then can add check afterwards
      // If this is a call passing in the erroreous value, skip the call
      return;
    }  
   
   ASTNode n = getProcessor().getController().getExceptionNode(rp);
   if (n == null) return;
   
   if (stmt != bstmt) {
      // if the erroreous value is a variable computed here, then can add check afterwards
      // If this is a call passing in the erroreous value, skip the call
      return;
    }  
   Object o1 = null;
   switch (rp.getProblemDetail()) {
      case "java.lang.NullPointerException" :
         o1 = new NullCondition();
         break;
      case "java.lang.ArrayIndexOutOfBoundsException" :
         o1 = new BoundsCondition();
         break;
      case "java.lang.IndexOutOfBoundsException" :
         o1 = new ListBounds();
         break;
      default :
         return;
    }
   cm = (ConditionMaker) o1;
   
   Expression base = (Expression) n;
   ASTNode blk = bstmt.getParent();
   CondScan cond = new CondScan(stmt,base);
   blk.accept(cond);
   Statement endstmt = cond.getEndStatement();
   
   String stmtdesc = stmt.toString();
   if (stmtdesc.length() > 23) {
      stmtdesc = stmtdesc.substring(0,20) + "...";
    }
   
   ASTRewrite rw1 = skipStatements(cm,base,stmt,endstmt);
   if (rw1 != null) {
      String desc = "Add 'if (" + cm.getNeqCondition(base).toString() + ") { ' ";
      if (stmt == endstmt) desc += "around " + stmtdesc;
      else desc += "around " + stmtdesc + " and subsequent statements";
      addRepair(rw1,desc,0.75);
    }
   ASTRewrite rw2 = loopContine(cm,base,stmt);
   if (rw2 != null) {
      String desc = "Add 'if (" + cm.getEqlCondition(base).toString() + ") continue;' before " + stmtdesc;
      addRepair(rw2,desc,0.5);
    }
   ASTRewrite rw3 = condReturn(cm,base,stmt);
   if (rw3 != null) {
      String desc = "Add 'if (" + cm.getEqlCondition(base).toString() + ") return;' before " + stmtdesc;
      addRepair(rw3,desc,0.5);
    }
   ASTRewrite rw4 = ifcondRepair(cm,base,stmt);
   if (rw4 != null) {
      String desc = "Add '(" + cm.getNeqCondition(base).toString() + ") &&' to " + stmtdesc;
      addRepair(rw4,desc,0.7);
    }
   ASTRewrite rw5 = nullReturn(cm,base,stmt);
   if (rw5 != null) {
      String desc = "Add '(" + cm.getEqlCondition(base).toString() + ") ||' to " + stmtdesc;
      addRepair(rw5,desc,0.5);
    } 
}




/********************************************************************************/
/*                                                                              */
/*      Generate a rewrite for start->end                                       */
/*                                                                              */
/********************************************************************************/

private ASTRewrite skipStatements(ConditionMaker cm,Expression base,
      Statement start,Statement end)
{
   ASTNode par = start.getParent();
   if (par instanceof Block) {
      return skipBlock(cm,base,start,end);
    }
   else if (par instanceof IfStatement) {
      IfStatement s = (IfStatement) par;
      if (start.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) return null;
      return addToConditionalAnd(cm,base,s.getExpression(),null);
    }
   else if (par instanceof WhileStatement) {
      WhileStatement s = (WhileStatement) par;
      return addToConditionalAnd(cm,base,s.getExpression(),null);
    }
   else if (par instanceof ForStatement) {
      ForStatement f = (ForStatement) par;
      if (start.getLocationInParent() == ForStatement.BODY_PROPERTY) {
         if (f.getExpression() != null) {
            return addToConditionalAnd(cm,base,f.getExpression(),null);
          }
         else {
            return createForCondition(cm,base,f);
          }
       }
    }
   
   return null;
}



@SuppressWarnings("unchecked")
private ASTRewrite skipBlock(ConditionMaker cm,Expression base,Statement start,Statement end)
{
   AST ast = start.getAST();
   
   IfStatement ifs = ast.newIfStatement();
   ifs.setExpression(cm.getCheckExpression(ast,base,true));
   Block nblk = ast.newBlock();
   ifs.setThenStatement(nblk);
   
   Block oblk = (Block) start.getParent();
   ASTRewrite rw = ASTRewrite.create(ast);
   ListRewrite lrw = rw.getListRewrite(oblk,Block.STATEMENTS_PROPERTY);
   
   boolean inblk = false;
   for (Object o1 : oblk.statements()) {
      Statement s1 = (Statement) o1;
      if (s1 == start) {
         inblk = true;
         lrw.insertBefore(ifs,s1,null);
       }
      if (inblk) {
         lrw.remove(s1,null);
         nblk.statements().add(ASTNode.copySubtree(ast,s1));
       }
      if (s1 == end) inblk = false;
    }
   
   return rw;
}



/********************************************************************************/
/*                                                                              */
/*      Try continue if inside loop                                             */
/*                                                                              */
/********************************************************************************/

private ASTRewrite loopContine(ConditionMaker cm,Expression base,Statement start)
{
   Boolean inloop = null;
   Block blk = null;
   ASTNode prev = start;
   for (ASTNode p = start.getParent(); p != null; p = p.getParent()) {
      switch (p.getNodeType()) {
         case ASTNode.METHOD_DECLARATION :
         case ASTNode.FIELD_DECLARATION :
         case ASTNode.INITIALIZER :
         case ASTNode.TYPE_DECLARATION :
            inloop = false;
            break;
         case ASTNode.FOR_STATEMENT :
         case ASTNode.WHILE_STATEMENT :
         case ASTNode.DO_STATEMENT :
         case ASTNode.ENHANCED_FOR_STATEMENT :
            inloop = true;
            break;
         case ASTNode.BLOCK :
            if (blk == null) blk = (Block) p;
            break;
       }
      if (inloop != null) break;
      if (blk == null) prev = p;
    }
   if (inloop == null || inloop == Boolean.FALSE || blk == null) return null;
   
   AST ast = start.getAST();
   ASTRewrite rw = ASTRewrite.create(ast);
   IfStatement s = ast.newIfStatement();
   s.setExpression(cm.getCheckExpression(ast,base,false));
   s.setThenStatement(ast.newContinueStatement());
   ListRewrite lrw = rw.getListRewrite(blk,Block.STATEMENTS_PROPERTY);
   lrw.insertBefore(s,prev,null);
   
   return rw;
}




/********************************************************************************/
/*                                                                              */
/*      Generate a return for bad value                                         */
/*                                                                              */
/********************************************************************************/

private ASTRewrite condReturn(ConditionMaker cm,Expression base,Statement start)
{
   JcompSymbol mthd = null;
   Block blk = null;
   ASTNode prev = start;
   for (ASTNode p = start.getParent(); p != null; p = p.getParent()) {
      boolean done = false;
      switch (p.getNodeType()) {
         case ASTNode.METHOD_DECLARATION :
            mthd = JcompAst.getDefinition(p);
            done = true;
            break;
         case ASTNode.FIELD_DECLARATION :
         case ASTNode.INITIALIZER :
         case ASTNode.TYPE_DECLARATION :
            done = true;
            break;
         case ASTNode.BLOCK :
            if (blk == null) blk = (Block) p;
            break;
       }
      if (done) break;
      if (blk == null) prev = p;
    }
   if  (mthd == null || blk == null) return null;
   
   AST ast = start.getAST();
   
   JcompType typ = mthd.getType().getBaseType();
   Expression rslt = null;
   if (typ.isBooleanType()) rslt = ast.newBooleanLiteral(false);
   else if (typ.isNumericType()) rslt = ast.newNumberLiteral("0");
   else if (typ.isVoidType()) rslt = null;
   else rslt = ast.newNullLiteral();
   ReturnStatement ret = ast.newReturnStatement();
   if (rslt != null) ret.setExpression(rslt);
   
   ASTRewrite rw = ASTRewrite.create(ast);
   IfStatement s = ast.newIfStatement();
   s.setExpression(cm.getCheckExpression(ast,base,false));
   s.setThenStatement(ret);
   ListRewrite lrw = rw.getListRewrite(blk,Block.STATEMENTS_PROPERTY);
   lrw.insertBefore(s,prev,null);
   
   return rw;
}



/********************************************************************************/
/*                                                                              */
/*      Augment existing if with condition                                      */
/*                                                                              */
/********************************************************************************/

private ASTRewrite ifcondRepair(ConditionMaker cm,Expression base,Statement s)
{
   if (s.getNodeType() != ASTNode.IF_STATEMENT) return null;
   
   ASTNode cond = null;
   ASTNode elt = null;
   for (ASTNode n = base; n != null; n = n.getParent()) {
      if (n == s) break;
      elt = cond;
      cond = n;
    }
   if (cond.getLocationInParent() != IfStatement.EXPRESSION_PROPERTY) return null;
   
   return addToConditionalAnd(cm,base,(Expression) cond,(Expression) elt);
}



/********************************************************************************/
/*                                                                              */
/*      Generate return if bad condition                                        */
/*                                                                              */
/********************************************************************************/

private ASTRewrite nullReturn(ConditionMaker cm,Expression base,Statement stmt)
{
   if (!(stmt instanceof IfStatement)) return null;
   IfStatement ifstmt = (IfStatement) stmt;
   if (!(ifstmt.getThenStatement() instanceof ReturnStatement)) return null;
   
   return addToConditionalOr(cm,base,ifstmt.getExpression(),base);
}  




/********************************************************************************/
/*                                                                              */
/*      Handle condition manipulation                                           */
/*                                                                              */
/********************************************************************************/

private ASTRewrite addToConditionalAnd(ConditionMaker cm,Expression base,
      Expression cond,Expression before)
{
   AST ast = cond.getAST();
   ASTRewrite rw = ASTRewrite.create(ast);
   Expression nbase = cm.getCheckExpression(ast,base,true);
   
   if (cond instanceof InfixExpression) {
      InfixExpression inf = (InfixExpression) cond;
      if (inf.getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
         ListRewrite lrw = rw.getListRewrite(cond,InfixExpression.EXTENDED_OPERANDS_PROPERTY);
         if (before == null) lrw.insertLast(nbase,null);
         else lrw.insertBefore(nbase,before,null);
         return rw;
       }
    }
   
   InfixExpression inf = ast.newInfixExpression();
   inf.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
   inf.setLeftOperand(nbase);
   inf.setRightOperand((Expression) ASTNode.copySubtree(ast,cond));
   
   rw.replace(cond,inf,null);
   
   return rw;
}



private ASTRewrite addToConditionalOr(ConditionMaker cm,Expression base,Expression cond,Expression before)
{
   AST ast = cond.getAST();
   ASTRewrite rw = ASTRewrite.create(ast);
   Expression nbase = cm.getCheckExpression(ast,base,false);
   
   if (cond instanceof InfixExpression) {
      InfixExpression inf = (InfixExpression) cond;
      if (inf.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
         ListRewrite lrw = rw.getListRewrite(cond,InfixExpression.EXTENDED_OPERANDS_PROPERTY);
         if (before == null) lrw.insertLast(nbase,null);
         else lrw.insertBefore(nbase,before,null);
         return rw;
       }
    }
   
   InfixExpression inf = ast.newInfixExpression();
   inf.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
   inf.setLeftOperand(nbase);
   inf.setRightOperand((Expression) ASTNode.copySubtree(ast,cond));
   
   rw.replace(cond,inf,null);
   
   return rw;
}



private ASTRewrite createForCondition(ConditionMaker cm,Expression base,Statement f)
{
   AST ast = f.getAST();
   ASTRewrite rw = ASTRewrite.create(ast);
   Expression nbase = cm.getCheckExpression(ast,base,true);
   
   rw.set(f,ForStatement.EXPRESSION_PROPERTY,nbase,null);
   
   return rw;
}



/********************************************************************************/
/*                                                                              */
/*      Handle condition generation                                             */
/*                                                                              */
/********************************************************************************/

private abstract static class ConditionMaker {

   protected ConditionMaker() { }
   
   ASTNode getNeqCondition(ASTNode base) {
      return getCheckExpression(base.getAST(),(Expression) base,true);
    }
   
   ASTNode getEqlCondition(ASTNode base) {
      return getCheckExpression(base.getAST(),(Expression) base,false);
    }
   
   Expression getCheckExpression(AST ast,Expression base,boolean neq) {
      return null;
    }
   
}       // end of inner abstract class ConditionMaker




private static class NullCondition extends ConditionMaker {
   
   NullCondition() { }
   
   Expression getCheckExpression(AST ast,Expression base,boolean neq) {
      Expression e1 = (Expression) ASTNode.copySubtree(ast,base);
      
      InfixExpression inf = ast.newInfixExpression();
      inf.setLeftOperand(e1);
      inf.setRightOperand(ast.newNullLiteral());
      if (neq) inf.setOperator(InfixExpression.Operator.NOT_EQUALS);
      else inf.setOperator(InfixExpression.Operator.EQUALS);
      
      return inf; 
    }
   
}       // end of inner class NullCondition



private static class BoundsCondition extends ConditionMaker {
   
   BoundsCondition() { }
 
   Expression getCheckExpression(AST ast,Expression base,boolean neq) {
      if (base.getNodeType() != ASTNode.ARRAY_ACCESS) return null;
      ArrayAccess aa = (ArrayAccess) base;
      InfixExpression e1 = ast.newInfixExpression();
      e1.setLeftOperand((Expression) ASTNode.copySubtree(ast,aa.getIndex()));
      if (neq) e1.setOperator(InfixExpression.Operator.LESS_EQUALS);
      else e1.setOperator(InfixExpression.Operator.GREATER);
      FieldAccess fa = ast.newFieldAccess();
      fa.setExpression((Expression) ASTNode.copySubtree(ast,aa.getArray()));
      fa.setName(ast.newSimpleName("length"));
      e1.setRightOperand(fa);
      InfixExpression e2 = ast.newInfixExpression();
      e2.setLeftOperand((Expression) ASTNode.copySubtree(ast,aa.getIndex()));
      if (neq) e2.setOperator(InfixExpression.Operator.GREATER_EQUALS);
      else e2.setOperator(InfixExpression.Operator.LESS);
      e2.setRightOperand(ast.newNumberLiteral("0"));
      InfixExpression e3 = ast.newInfixExpression();
      e3.setLeftOperand(e1);
      if (neq) e3.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
      else e3.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
      e3.setRightOperand(e2);
      ParenthesizedExpression pe = ast.newParenthesizedExpression();
      pe.setExpression(e3);
      return pe;
    }
   
}       // end of inner class BoundsCondition


private static class ListBounds extends ConditionMaker {
   
   ListBounds() { }
   
   Expression getCheckExpression(AST ast,Expression base,boolean neq) {
      if (base.getNodeType() != ASTNode.METHOD_INVOCATION) return null;
      MethodInvocation mi = (MethodInvocation) base;
      Expression idx = null;
      List<?> args = mi.arguments();
      if (args.size() > 0) idx = (Expression) args.get(0);
      
      MethodInvocation e4 = ast.newMethodInvocation();
      e4.setExpression((Expression) ASTNode.copySubtree(ast,mi.getExpression()));
      e4.setName(ast.newSimpleName("size"));
     
      if (idx == null) {
         InfixExpression e5 = ast.newInfixExpression();
         e5.setLeftOperand(e4);
         if (neq) e5.setOperator(InfixExpression.Operator.NOT_EQUALS);
         else e5.setOperator(InfixExpression.Operator.EQUALS);
         e5.setRightOperand(ast.newNumberLiteral("0"));
         return e5;
       }
      
      InfixExpression e1 = ast.newInfixExpression();
      e1.setLeftOperand((Expression) ASTNode.copySubtree(ast,idx));
      if (neq) e1.setOperator(InfixExpression.Operator.LESS_EQUALS);
      else e1.setOperator(InfixExpression.Operator.GREATER);
      e1.setRightOperand(e4);
      InfixExpression e2 = ast.newInfixExpression();
      e2.setLeftOperand((Expression) ASTNode.copySubtree(ast,idx));
      if (neq) e2.setOperator(InfixExpression.Operator.GREATER_EQUALS);
      else e2.setOperator(InfixExpression.Operator.LESS);
      e2.setRightOperand(ast.newNumberLiteral("0"));
      InfixExpression e3 = ast.newInfixExpression();
      e3.setLeftOperand(e1);
      if (neq) e3.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
      else e3.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
      e3.setRightOperand(e2);
      ParenthesizedExpression pe = ast.newParenthesizedExpression();
      pe.setExpression(e3);
      return pe;
    }
   
}       // end of inner class ListBounds


/********************************************************************************/
/*                                                                              */
/*      Find span of statement for skipping                                     */
/*                                                                              */
/********************************************************************************/

private static class CondScan extends ASTVisitor {

   private Set<JcompSymbol> defined_symbols;
   private String bad_expr;
   private Statement end_statement;
   private int nest_level;
   private Statement start_statement;
   private Set<JcompSymbol> add_symbols;
   private boolean is_required;
   
   CondScan(Statement start,Expression exp) {
      defined_symbols = new HashSet<>();
      add_symbols = new HashSet<>();
      if (start instanceof VariableDeclarationStatement) {
         VariableDeclarationStatement vds = (VariableDeclarationStatement) start;
         for (Object o1 : vds.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o1;
            JcompSymbol js = JcompAst.getDefinition(vdf);
            defined_symbols.add(js);
          }
       }
      bad_expr = exp.toString();
      nest_level = 0;
      start_statement = start;
      end_statement = null;
      is_required = false;
    }
   
   Statement getEndStatement()                  { return end_statement; }
   
   @Override public void preVisit(ASTNode n) {
      if (n instanceof Block) {
         ++nest_level;
       }
      if (n == start_statement) {
         end_statement = (Statement) n;
       }
    }
   
   @Override public void postVisit(ASTNode n) {
      if (n instanceof Block) {
         --nest_level;
       }
      else if (n instanceof Statement && nest_level <= 1 && end_statement != null) {
         if (is_required) {
            end_statement = (Statement) n;
            defined_symbols.addAll(add_symbols);
            add_symbols.clear();
            is_required = false;
          }
       }
      else if (n instanceof Expression && end_statement != null) {
         JcompSymbol js = JcompAst.getReference(n);
         if (js != null && defined_symbols.contains(js)) is_required = true;
         checkExpression((Expression) n);
       }
    }
   
   @Override public boolean visit(VariableDeclarationFragment vdf) {
      if (end_statement != null && nest_level <= 1) {
         JcompSymbol js = JcompAst.getDefinition(vdf);
         add_symbols.add(js);
       }
      return true;
    }
   
   private void checkExpression(Expression n) {
      String etxt = n.toString();
      if (etxt.equals(bad_expr)) {
         StructuralPropertyDescriptor spd = n.getLocationInParent();
         if (spd == FieldAccess.EXPRESSION_PROPERTY ||
               spd == QualifiedName.QUALIFIER_PROPERTY ||
               spd == MethodInvocation.EXPRESSION_PROPERTY ||
               spd == ArrayAccess.ARRAY_PROPERTY ||
               spd == ClassInstanceCreation.EXPRESSION_PROPERTY)
            is_required = true;
       }
    }

}       // end of inner class CondScan




}       // end of class SepalAvoidException




/* end of SepalAvoidException.java */

