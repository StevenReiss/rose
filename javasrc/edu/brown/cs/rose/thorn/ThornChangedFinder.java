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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompScope;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootTestCase;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootControl.AssertionData;

class ThornChangedFinder implements ThornConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl     for_control;
private Map<ASTNode,ThornChangeMap>  known_methods;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ThornChangedFinder(RootControl ctrl)
{
   for_control = ctrl;
   known_methods = null;
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

ThornChangeData process(BudLaunch bl,RootProblem rp,String topframe)
{
   ThornChangeData rslt = new ThornChangeData();
   
   known_methods = new HashMap<>();
   // need to find correct starting frame
   BudStackFrame bf0 = bl.getStack().getTopFrame();
   ASTNode stmt0 = getNodeForFrame(bf0);
   
   ThornChangeMap initvars = findProblemVariables(stmt0,rp);
   RootTestCase rtc = rp.getCurrentTest();
   if (rtc != null && rtc.getFrameId() != null) {
      topframe = rtc.getFrameId();
    }
   
   ThornChangeMap chngs = initvars;
   BudStackFrame prevframe = null;
   ASTNode prevnode = null;
   BudStack bs = bl.getStack();
   boolean havefirst = false;
   for (BudStackFrame bf : bs.getFrames()) {
      if (!havefirst && bf.isUserFrame()) havefirst = true;
      ASTNode n = getNodeForFrame(bf);
      if (n != null) {
         initvars = convertChangesToParent(chngs,bf,prevframe,n,prevnode);
	 chngs = processMethod(n,initvars);
         rslt.setChanges(bf,chngs);
         if (topframe.equals(bf.getFrameId())) {
            rslt.setTopFrame(bf);
            break;
          }
         prevframe = bf;
         prevnode = n;
       }
      else if (havefirst) return null;
    }
   
   known_methods = null;
   
   return rslt;
}



private ASTNode getNodeForFrame(BudStackFrame bf) 
{
   File f = bf.getSourceFile();
   if (f == null || !f.exists() || !f.canRead()) return null;
   String proj = for_control.getProjectForFile(f);
   ASTNode n = for_control.getSourceNode(proj,f,-1,bf.getLineNumber(),true,true);
   
   return n;
}


private ThornChangeMap processMethod(ASTNode stmt0,ThornChangeMap initmap)
{
   ThornChangeMap rslt = null;
   if (initmap == null && known_methods != null && known_methods.containsKey(stmt0)) {
      rslt = known_methods.get(stmt0);
      return rslt;
    }
   
   known_methods.put(stmt0,new ThornChangeMap());
   
   ChangedVisitor visitor = new ChangedVisitor(initmap);
   
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
   
   rslt = visitor.getChanges();
   if (known_methods != null && initmap == null) {
      known_methods.put(stmt0,rslt);
    }
   
   return rslt;
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
/*      Find initial relevant variables                                         */
/*                                                                              */
/********************************************************************************/

ThornChangeMap findProblemVariables(ASTNode base,RootProblem rp)
{
   ASTNode n = null;
   switch (rp.getProblemType()) {
      case ASSERTION :
         AssertionData ad = for_control.getAssertionData(rp);
         if (ad != null) n = ad.getExpression();
         break;
      case EXCEPTION :
         n = for_control.getExceptionNode(rp);
         break;
      case VARIABLE :
         JcompSymbol js = findVariableSymbol(base,rp.getProblemDetail());
         if (js != null) {
            ThornChangeMap cm = new ThornChangeMap();
            ThornChangedItem cd = new ThornChangedItem(js);
            cd = cd.setRelevant();
            cm.put(js,cd);
            return cm;
          }
         break;
      case EXPRESSION :
         if (rp.getNodeContext() != null) {
            n = rp.getNodeContext().findAstNode(base);
          }
         break;
      case LOCATION :
      case OTHER :
         return null;
      case NONE :
         if (rp.getProblemDetail() == null) return null;
         ThornChangeMap cm = new ThornChangeMap();
         StringTokenizer tok = new StringTokenizer(rp.getProblemDetail()," \t,;-&\n");
         while (tok.hasMoreTokens()) {
            String var = tok.nextToken();
            JcompSymbol js1 = findVariableSymbol(base,var);
            if (js1 != null) {
               ThornChangedItem cd = new ThornChangedItem(js1);
               cd = cd.setRelevant();
               cm.put(js1,cd);
             }
          }
         return cm;
    }
   
   if (n == null) return null;
   
   NodeVarFinder vf = new NodeVarFinder();
   n.accept(vf);
   return vf.getChanges();
}



private static class NodeVarFinder extends ASTVisitor {
   
   private ThornChangeMap change_map;
   
   NodeVarFinder() {
      change_map = new ThornChangeMap();
    }
   
   NodeVarFinder(ThornChangeMap tcm) {
      change_map = tcm;
    }
   
   ThornChangeMap getChanges()                  { return change_map; }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null) {
         ThornChangedItem cd = change_map.get(js);
         if (cd == null) {
            cd = new ThornChangedItem(js);
          }
         cd = cd.setRelevant();
         change_map.put(js,cd);
       }
    }
   
}       // end of inner class VarFinder



private JcompSymbol findVariableSymbol(ASTNode n,String name)
{
   JcompScope curscp = null;
   for (ASTNode p = n; p != null; p = p.getParent()) {
      if (curscp == null) {
         curscp = JcompAst.getJavaScope(p);
       }
      if (curscp != null) break;
    }
   if (curscp == null) {
      RoseLog.logE("THORN","Can't find scope for " + n);
      return null;
    } 
   
   if (name.startsWith("this.")) {
      String s = name.substring(5);
      return curscp.lookupVariable(s);
    }
   else {
      return curscp.lookupVariable(name);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Convert change map to parent                                            */
/*                                                                              */
/********************************************************************************/

private ThornChangeMap convertChangesToParent(ThornChangeMap orig,BudStackFrame cur,
        BudStackFrame prev,ASTNode call,ASTNode prevcall)
{ 
   if (prev == null) return orig;
   if (orig == null) return null;
   
   JcompSymbol mthdcur = getMethodOfNode(call);
   JcompSymbol mthdprev = getMethodOfNode(prevcall);
   if (mthdcur == null || mthdprev == null) return null;
   
   CallFinder cf = new CallFinder(mthdprev);
   call.accept(cf);
   
   ThornChangeMap newmap = new ThornChangeMap();
   for (ThornChangedItem tcd : orig.values()) {
      JcompSymbol js = tcd.getReference();
      if (js.isFieldSymbol()) {
         newmap.put(js,tcd);
       }
      else if (js.isMethodSymbol() || js.isConstructorSymbol()) ;
      else {
         int pno = getParameter(js);
         if (pno >= 0) {
            NodeVarFinder finder = new NodeVarFinder(newmap);
            List<?> args = cf.getArgumentList();
            if (args != null) {
               ASTNode argn = (ASTNode) args.get(pno);
               argn.accept(finder);
             }
          }
       }
    }
   
   return newmap;
}


private JcompSymbol getMethodOfNode(ASTNode base) 
{
   JcompSymbol mthd = null;
   for (ASTNode n = base; n != null; n = n.getParent()) {
      if (n instanceof MethodDeclaration) {
         mthd = JcompAst.getDefinition(n);
         break;
       }
    }
   return mthd;
}


private int getParameter(JcompSymbol js)
{
   ASTNode n = js.getDefinitionNode();
   if (n instanceof SingleVariableDeclaration && 
         n.getParent() instanceof MethodDeclaration) {
      MethodDeclaration md = (MethodDeclaration) n.getParent();
      int i = 0;
      for (Object o : md.parameters()) {
         if (o == n) return i;
       }
    }
   return -1;
}




private static class CallFinder extends ASTVisitor {

   private JcompSymbol call_symbol;
   private ASTNode found_node;
   private List<?> arg_nodes;
   
   CallFinder(JcompSymbol js) {
      call_symbol = js;
      found_node = null;
      arg_nodes = null;
    }
   
   List<?> getArgumentList()            { return arg_nodes; }
   
   @Override public void endVisit(MethodInvocation mi) {
      checkResult(mi,mi.getName().getIdentifier(),mi.arguments());
    }
   
   @Override public void endVisit(ClassInstanceCreation ci) {
      checkResult(ci,"<init>",ci.arguments()); 
    }
   
   @Override public void endVisit(SuperMethodInvocation mi) {
      checkResult(mi,mi.getName().getIdentifier(),mi.arguments());
    }
   
   @Override public void endVisit(SuperConstructorInvocation ci) {
      checkResult(ci,"<init>",ci.arguments());
    }
   
   private void checkResult(ASTNode n,String name,List<?> args) {
      JcompSymbol ref = JcompAst.getReference(n);
      if (ref == null) {
         if (name.equals(call_symbol.getName())) {
            found_node = n;
            arg_nodes = args;
          }
       }
      else if (ref == call_symbol && found_node == null) {
         found_node = n;
         arg_nodes = args;
       }
    }
   
}       // end of inner class CallFinder




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
/*      Visitor to handle statement processing                                  */
/*                                                                              */
/********************************************************************************/

private class ChangedVisitor extends ASTVisitor {
   
   private boolean note_relevant;
   private Stack<Boolean> relevant_stack;
   private boolean doing_loop;
   private Stack<Boolean> loop_stack;
   private ThornChangeMap change_data;
   
   ChangedVisitor(Map<JcompSymbol,ThornChangedItem> ch) {
      note_relevant = false;
      relevant_stack = new Stack<>();
      doing_loop = false;
      loop_stack = new Stack<>();
      change_data = new ThornChangeMap();
      if (ch != null) change_data.putAll(ch);
   }
   
   ThornChangeMap getChanges() {
      return change_data;
    }
   
   @Override public boolean visit(Assignment a) {
      JcompSymbol js = getAssignSymbol(a.getLeftHandSide());
      if (js == null) return true;
      ThornChangedItem cd = change_data.get(js);
      if (cd == null) {
         cd = new ThornChangedItem(js);
       }
      cd = cd.setChanged();
      change_data.put(js,cd);
      accept(a.getLeftHandSide());
      if (cd.isRelevant()) acceptRelevant(a.getRightHandSide());
      else accept(a.getRightHandSide());
      return false;
    }
   
   @Override public boolean visit(VariableDeclarationFragment vdf) {
      JcompSymbol js = JcompAst.getDefinition(vdf);
      if (js == null) return true;
      if (vdf.getInitializer() == null) return true;
      ThornChangedItem cd = change_data.get(js);
      if (cd == null) {
         cd = new ThornChangedItem(js);
       }
      cd = cd.setChanged();
      change_data.put(js,cd);
      if (cd.isRelevant()) acceptRelevant(vdf.getInitializer());
      else accept(vdf.getInitializer());
      return false; 
    }
   
   @Override public void postVisit(ASTNode n) {
      if (note_relevant) {
         JcompSymbol js = JcompAst.getReference(n);
         if (js != null) {
            ThornChangedItem cd = change_data.get(js);
            if (cd == null) {
               cd = new ThornChangedItem(js);
             }
            cd = cd.setRelevant();
            change_data.put(js,cd);
          }
       }
    }
   
   @Override public boolean visit(IfStatement s) {
      acceptRelevant(s.getExpression());
      if (doing_loop) {
         accept(s.getThenStatement());
         accept(s.getElseStatement());
       }
      return false;
    }
   
   @Override public boolean visit(WhileStatement s) {
      acceptRelevant(s.getExpression());
      acceptLoop(s.getBody());
      return false;
    }
   
   @Override public boolean visit(DoStatement s) {
      acceptRelevant(s.getExpression());
      acceptLoop(s.getBody());
      return false;
    }
   
   @Override public boolean visit(SwitchStatement s) {
      acceptRelevant(s.getExpression());
      if (doing_loop) {
         for (Object o : s.statements()) {
            ASTNode n = (ASTNode) o;
            accept(n);
          }
       }
      return false;
    }
   
   @Override public boolean visit(ForStatement s) {
      acceptRelevant(s.getExpression());
      acceptLoop(s.getBody());
      accept(s.initializers());
      accept(s.updaters());
      return false;
    }
   
   @Override public boolean visit(EnhancedForStatement s) {
      acceptLoop(s.getBody());
      accept(s.getExpression());
      return false;
    }
   
   @Override public void endVisit(MethodInvocation mi) {
      JcompSymbol js = JcompAst.getReference(mi.getName());
      if (js == null) return;
      ASTNode mthd = js.getDefinitionNode();
      if (mthd == null) return;
      List<ASTNode> rets = findReturns(mthd);
      for (ASTNode base : rets) {
         ThornChangeMap vm = processMethod(base,null);
         for (ThornChangedItem tcd : vm.values()) {
            JcompSymbol rjs = tcd.getReference();
            if (rjs.isFieldSymbol() && (tcd.isChanged() || tcd.isRelevant())) {
               ThornChangedItem ocd = change_data.get(rjs);
               if (ocd == null) {
                  ocd = new ThornChangedItem(rjs);
                }
               if (tcd.isRelevant()) ocd = ocd.setRelevant();
               if (tcd.isChanged()) ocd = ocd.setChanged();
               change_data.put(rjs,ocd);
             }
          }
       }
    }
   
   private void acceptRelevant(ASTNode n) {
      if (n == null) return;
      relevant_stack.push(note_relevant);
      note_relevant = true;
      n.accept(this);
      note_relevant = relevant_stack.pop();
    }
   
   private void acceptLoop(ASTNode n) {
      if (n == null) return;
      loop_stack.push(doing_loop);
      doing_loop = true;
      n.accept(this);
      doing_loop = loop_stack.pop();
    }
   
   private void accept(List<?> nlist) {
      for (Object o : nlist) {
         ASTNode n = (ASTNode) o;
         accept(n);
       }
    }
   
   private void accept(ASTNode n) {
      if (n == null) return;
      n.accept(this);
    }
   
   private JcompSymbol getAssignSymbol(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null) return js;
      
      AssignFinder af = new AssignFinder();
      n.accept(af);
      return af.getFoundName();
    }
   
}       // end of inner class ChangedVisitor





private static final class AssignFinder extends ASTVisitor {
   
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
/*      Visitor to find returns in a method                                     */
/*                                                                              */
/********************************************************************************/

private List<ASTNode> findReturns(ASTNode mthd)
{
   ReturnFinder rf = new ReturnFinder();
   mthd.accept(rf);
   
   return rf.getReturns();
}


private static class ReturnFinder extends ASTVisitor {
   
   private List<ASTNode> return_statements;
   private ASTNode last_statement;
   
   ReturnFinder() {
      return_statements = new ArrayList<>();
      last_statement = null;
    }
   
   List<ASTNode> getReturns() { 
      if (return_statements.isEmpty() && last_statement != null) {
         return_statements.add(last_statement);
       }
      return return_statements; 
    }
   
   @Override public void endVisit(ReturnStatement rs) {
      return_statements.add(rs);
    }
   
   @Override public void postVisit(ASTNode n) {
      if (n instanceof Statement) {
         while (n.getParent() instanceof Statement) n = n.getParent();
         last_statement = n;
       }
    }
   
}       // end of inner class ReturnFinder





}       // end of class ThornChangedFinder




/* end of ThornChangedFinder.java */

