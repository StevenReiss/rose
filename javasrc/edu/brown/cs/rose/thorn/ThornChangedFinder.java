/********************************************************************************/
/*                                                                              */
/*              ThornChangedFinder.java                                         */
/*                                                                              */
/*      Find variable/fields changed in a method up to given statement          */
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



package edu.brown.cs.rose.thorn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.rose.root.RootControl;

class ThornChangedFinder implements ThornConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<JcompSymbol,ThornChangedData>   change_data;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ThornChangedFinder(RootControl ctrl)
{
   change_data = new HashMap<>();
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

List<ThornVariable> process(ASTNode stmt0)
{
   if (stmt0 == null) return null;
   
   change_data.clear();                 // remove after done debugging
   
   ChangedVisitor visitor = new ChangedVisitor();
   
   for (ASTNode stmt = stmt0; stmt != null; ) {   
      processStatement(stmt,visitor);
      StructuralPropertyDescriptor spd = stmt.getLocationInParent();
      if (spd.isChildListProperty()) {
         ASTNode par = stmt.getParent();
         List<?> chlds = (List<?>) par.getStructuralProperty(spd);
         int idx = chlds.indexOf(stmt);
         if (idx == 0) stmt = par.getParent();
         else stmt = (ASTNode) chlds.get(idx-1);
         stmt = getStatementOf(stmt);
       }
      else if (spd.isChildProperty()) {
         stmt = getStatementOf(stmt.getParent());
       }
    }
   
   return processResult(stmt0);
}


private ASTNode getStatementOf(ASTNode n)
{
   while (n != null) {
      if (n instanceof Statement) break;
      if (n instanceof MethodDeclaration) return null;
      n = n.getParent();
    }
   
   return n;
}




/********************************************************************************/
/*                                                                              */
/*      Process a single statement                                              */
/*                                                                              */
/********************************************************************************/

private void processStatement(ASTNode stmt,ChangedVisitor v)
{
   stmt.accept(v);
}



/********************************************************************************/
/*                                                                              */
/*      Get results based on process scan                                       */
/*                                                                              */
/********************************************************************************/

private List<ThornVariable> processResult(ASTNode base)
{
   List<ThornVariable> rslt = new ArrayList<>();
   
   JcompSymbol mthd = null;
   for (ASTNode n = base; n != null; n = n.getParent()) {
      if (n instanceof MethodDeclaration) {
         mthd = JcompAst.getDefinition(n);
         break;
       }
    }
   
   boolean usethis = false;
   
   for (ThornChangedData tcd : change_data.values()) {
      JcompSymbol js = tcd.getReference();
      VarData vd = null;
      if (js.isMethodSymbol()) {
         if (js.getClassType() == mthd.getClassType()) {
            if (!mthd.isStatic()) usethis = true;
          }
       }
      else if (js.isFieldSymbol()) {
         if (tcd.isChanged() && tcd.isRelevant()) {
            if (js.getClassType() == mthd.getClassType()) {
               vd = new VarData(js.getName(),ThornVariableType.THIS_FIELD,tcd);
             }
            else {
               vd = new VarData(js.getFullName(),ThornVariableType.FIELD,tcd);
             }
          }
       }
      else {
         ASTNode n = js.getDefinitionNode();
         if (n instanceof SingleVariableDeclaration && 
               n.getParent() instanceof MethodDeclaration) {
            // handle parameters
            if (tcd.isChanged()) {
               vd = new VarData(js.getName(),ThornVariableType.PARAMETER,tcd);
             }
          }
         else {
            // handle locals
          }
       }
      if (vd != null) rslt.add(vd);
    }
   
   if (usethis) {
      // might want to check for non-const methods and note that
    }
   
   return rslt;
}





/********************************************************************************/
/*                                                                              */
/*      Visitor to handle statement processing                                  */
/*                                                                              */
/********************************************************************************/

private class ChangedVisitor extends ASTVisitor {
   
   private boolean note_relevant;
   private Stack<Boolean> relevant_stack;
   
   ChangedVisitor() {
      note_relevant = false;
      relevant_stack = new Stack<>();
    }
   
   @Override public boolean visit(Assignment a) {
      JcompSymbol js = getAssignSymbol(a.getLeftHandSide());
      if (js == null) return true;
      ThornChangedData cd = change_data.get(js);
      if (cd == null) {
         cd = new ThornChangedData(js);
       }
      cd = cd.setChanged();
      change_data.put(js,cd);
      accept(a.getLeftHandSide());
      if (cd.isRelevant()) {
         acceptRelevant(a.getRightHandSide());
       }
      else accept(a.getRightHandSide());
      return false;
    }
   
   
   
   @Override public void postVisit(ASTNode n) {
      if (note_relevant) {
         JcompSymbol js = JcompAst.getReference(n);
         if (js != null) {
            ThornChangedData cd = change_data.get(js);
            if (cd == null) {
               cd = new ThornChangedData(js);
             }
            cd = cd.setRelevant();
            change_data.put(js,cd);
          }
       }
    }
   
   @Override public boolean visit(IfStatement s) {
      acceptRelevant(s.getExpression());
      accept(s.getThenStatement());
      accept(s.getElseStatement());
      return false;
    }
   
   @Override public boolean visit(WhileStatement s) {
      acceptRelevant(s.getExpression());
      accept(s.getBody());
      return false;
    }
   
   @Override public boolean visit(DoStatement s) {
      acceptRelevant(s.getExpression());
      accept(s.getBody());
      return false;
    }
   
   @Override public boolean visit(SwitchStatement s) {
      acceptRelevant(s.getExpression());
      for (Object o : s.statements()) {
         ASTNode n = (ASTNode) o;
         accept(n);
       }
      return false;
    }
   
   private void acceptRelevant(ASTNode n) {
      if (n == null) return;
      relevant_stack.push(note_relevant);
      note_relevant = true;
      n.accept(this);
      note_relevant = relevant_stack.pop();
    }
   
   private void accept(ASTNode n) {
      if (n == null) return;
      n.accept(this);
    }
   
}       // end of inner class ChangedVisitor



private JcompSymbol getAssignSymbol(ASTNode n)
{
   JcompSymbol js = JcompAst.getReference(n);
   if (js != null) return js;
   
   AssignFinder af = new AssignFinder();
   n.accept(af);
   return af.getFoundName();
}


private class AssignFinder extends ASTVisitor {
   
   private JcompSymbol found_name;
   
   JcompSymbol getFoundName()                   { return found_name; }
   
   @Override public boolean visit(ArrayAccess n) {
      if (found_name == null) n.getArray().accept(this);
      return false;
    }
   
   @Override public boolean visit(FieldAccess n) {
      if (found_name == null) n.getName().accept(this);
      return false;
    }
   
   @Override public boolean visit(QualifiedName n) {
      if (found_name == null) found_name = JcompAst.getReference(n);
      if (found_name == null) n.getName().accept(this);
      return false;
    }
   
   @Override public boolean visit(SimpleName n) {
      if (found_name == null) found_name = JcompAst.getReference(n);
      return false;
    }
   
}       // end of inner class AssignFinder



/********************************************************************************/
/*                                                                              */
/*      Result structure                                                        */
/*                                                                              */
/********************************************************************************/

private static class VarData implements ThornVariable {
   
   private String var_name; 
   private ThornVariableType var_type;
   
   VarData(String nm,ThornVariableType typ,ThornChangedData tcd) {
      var_name = nm;
      var_type = typ;
    }
   
   @Override public String getName()                    { return var_name; }
   @Override public ThornVariableType getVariableType() { return var_type; }
   
   @Override public String toString() {
      return var_type.toString() + ":" + var_name;
    }
   
}       // end of inner class VarData





}       // end of class ThornChangedFinder




/* end of ThornChangedFinder.java */

