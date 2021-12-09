/********************************************************************************/
/*                                                                              */
/*              PicotMethodDataAst.java                                         */
/*                                                                              */
/*      MNethod data where we have the abstract syntax tree                     */
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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;

class PicotMethodDataAst extends PicotMethodData
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private MethodDeclaration       method_declaration;
private PicotLocalMap           local_variables;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotMethodDataAst(MethodDeclaration md)
{
   super(JcompAst.getDefinition(md),JcompAst.getTyper(md));
   
   method_declaration = md;
   local_variables = null;
}



/********************************************************************************/
/*                                                                              */
/*      Compute method effects                                                  */
/*                                                                              */
/********************************************************************************/

protected void processLocal()
{
   local_variables = new PicotLocalMap();
   
   for (Object o : method_declaration.parameters()) {
      SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
      JcompSymbol psym = JcompAst.getDefinition(svd);
      method_parameters.add(psym);
    }
   
   if (method_declaration.isConstructor()) {
      ASTNode par = method_declaration.getParent();
      if (par instanceof AbstractTypeDeclaration) {
         FieldScanner fs = new FieldScanner();
         par.accept(fs);
       }
    }
   Block body = method_declaration.getBody();
   CodeScanner cs = new CodeScanner();
   body.accept(cs);
   
   local_variables = null;
}



/********************************************************************************/
/*                                                                              */
/*      Setup effect methods                                                    */
/*                                                                              */
/********************************************************************************/

private void handleAssignment(ASTNode lhs,ASTNode rhs) 
{
   if (rhs == null) return;
   
   PicotMethodEffect efct = PicotMethodEffect.createAssignment(lhs,rhs,local_variables);
   
   if (efct != null) {
      method_effects.add(efct);
    }
}


private void handleReturn(ReturnStatement s) 
{
   PicotMethodEffect efct = PicotMethodEffect.createReturn(s,local_variables);
   if (efct != null) {
      method_effects.add(efct);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Handle initialized fields for a constructor                             */
/*                                                                              */
/********************************************************************************/

private class FieldScanner extends ASTVisitor {
   
   @Override public boolean visit(MethodDeclaration md) {
      return false;
    }
   
   @Override public boolean visit(TypeDeclaration td) {
      return false;
    }
   
   @Override public boolean visit(EnumDeclaration ed) {
      return false;
    }
   
   @Override public boolean visit(VariableDeclarationFragment n) {
      handleAssignment(n.getName(),n.getInitializer());
      return false;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Determine effects for a method                                          */
/*                                                                              */
/********************************************************************************/

private class CodeScanner extends ASTVisitor {
   
   
   CodeScanner() {
    }
   
   @Override public boolean visit(Assignment a) {
      if (a.getOperator() == Assignment.Operator.ASSIGN) {
         handleAssignment(a.getLeftHandSide(),a.getRightHandSide());
       }
      return false;
    }
   
   @Override public boolean visit(MethodInvocation mi) {
      JcompSymbol js = JcompAst.getReference(mi);
      if (js != null && (js.getName().equals("add") || js.getName().equals("put"))) {
         JcompType jt = js.getClassType();
         JcompType mp = jcomp_typer.findSystemType("java.util.Collection");
         JcompType mp1 = jcomp_typer.findSystemType("java.util.Map");
         if (jt.isDerivedFrom(mp) || jt.isDerivedFrom(mp1)) {
            handleCollectionAdd(mi);
          }
       }
      else if (js != null && js.getName().equals("remove")) {
         JcompType jt = js.getClassType();
         JcompType mp = jcomp_typer.findSystemType("java.util.Collection");
         JcompType mp1 = jcomp_typer.findSystemType("java.util.Map");
         if (jt.isDerivedFrom(mp) || jt.isDerivedFrom(mp1)) {
            handleCollectionRemove(mi);
          }
       }
      return false;
    }
   
   @Override public boolean visit(ReturnStatement s) {
      handleReturn(s);
      return false;
    }
   
   @Override public boolean visit(VariableDeclarationStatement s) {
      accept(s.fragments());
      return false;
    }
   
   @Override public boolean visit(VariableDeclarationFragment n) {
      handleAssignment(n.getName(),n.getInitializer());
      return false;
    }
   
   @Override public boolean visit(WhileStatement s) {
      accept(s.getExpression());
      acceptInner(s.getBody());
      return false;
    }
   
   @Override public boolean visit(IfStatement s) {
      accept(s.getExpression());
      acceptInner(s.getThenStatement());
      acceptInner(s.getElseStatement());
      return false;
    }
   
   @Override public boolean visit(DoStatement s) {
      accept(s.getExpression());
      acceptInner(s.getBody());
      return false;
    }
   
   @Override public boolean visit(ForStatement s) {
      acceptInner(s.getBody());
      return false;
    }
   
   @Override public boolean visit(EnhancedForStatement s) {
      acceptInner(s.getBody());
      return false;
    }
   
   @Override public boolean visit(SynchronizedStatement s) {
      acceptInner(s.getBody());
      return false;
    }
   
   @Override public boolean visit(SwitchStatement s) {
      accept(s.getExpression());
      acceptInner(s.statements());
      return false;
    }
   
   @Override public boolean visit(ConstructorInvocation s) {
      JcompSymbol js = JcompAst.getReference(s);
      if (js != null) {
         ASTNode def = js.getDefinitionNode();
         def.accept(this);
       }
      return false;
    }
   
   @Override public boolean visit(SuperConstructorInvocation s) {
      // need to add in the other constructor -- do after
      return false;
    }
   
   @Override public boolean visit(AssertStatement s) {
      return false;
    }
   
   private void acceptInner(ASTNode b) {
      if (b != null) {
         b.accept(this);
       }
    }
   
   private void acceptInner(List<?> ls) {
      accept(ls);
    }
   
   private void accept(ASTNode n) {
      if (n != null) {
         n.accept(this);
       }
    }
   
   private void accept(List<?> ls) {
      if (ls != null) {
         for (Object o : ls) {
            ASTNode n = (ASTNode) o;
            n.accept(this);
          }
       }
    }
   
   private void handleCollectionAdd(MethodInvocation mi) {
    }
   
   private void handleCollectionRemove(MethodInvocation mi) {
    }
   
}



}       // end of class PicotMethodDataAst




/* end of PicotMethodDataAst.java */

