/********************************************************************************/
/*                                                                              */
/*              ValidateSetup.java                                              */
/*                                                                              */
/*      Find potential reset expressions for a run                              */
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



package edu.brown.cs.rose.validate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.thorn.ThornChangeData;

class ValidateSetup implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ValidateContext         base_context;
private ThornChangeData         changed_items;
private BudStackFrame           active_frame;
private ValidateCall            active_call;
private List<ValidateAction>    action_set;
private ValidateCall            last_call;
private Map<String,String>      variable_map;
private long                    first_time;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateSetup(ValidateContext ctx,ThornChangeData cd,BudStackFrame frm,ValidateCall vc)
{
   base_context = ctx;
   changed_items = cd;
   active_frame = frm;
   active_call = vc;
   action_set = new ArrayList<>();
   last_call = null;
   variable_map = new HashMap<>();
   for (String blv : frm.getLocals()) {
      variable_map.put(blv,blv);
    }
}


ValidateSetup(ValidateSetup par,ValidateCall vc,Map<String,String> varmap)
{
   base_context = par.base_context;
   changed_items = par.changed_items;
   active_frame = par.active_frame;
   active_call = vc;
   action_set = par.action_set;
   last_call = null;
   variable_map = varmap;
   first_time = 0;
}




/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

List<ValidateAction> findResets()
{
   ASTNode mdcl = getAstNodeForCall(active_call);
   
   ValidateTrace vt = active_call.getTrace();
   ValidateVariable vline = active_call.getLineNumbers();
   List<ValidateValue> lines = vline.getValues(vt);
   for (int i = 0; i < lines.size(); ++i) {
      ValidateValue vv = lines.get(i);
      long time = vv.getStartTime();
      if (time != 0 && first_time == 0) first_time = time;
      long etime;
      if (i+1 < lines.size()) etime = lines.get(i+1).getStartTime();
      else etime = active_call.getEndTime();
      if (time != 0) {
         updateVariableMap(time);
         int lno =  vv.getLineValue();
         Statement stmt = getStatementAtLine(mdcl,lno);
         if (stmt == null) continue;
         processStatement(lno,stmt,time,etime);
       }
    }
   
   if (action_set.isEmpty()) return null;
   
   return action_set;
}



private void processStatement(int lno,Statement n,long start,long end)
{
   SetupProcessor sp = new SetupProcessor(lno,start,end);
   n.accept(sp);
}


private void processCall(JcompSymbol js,Expression thisexpr,List<?> args,
      long start,long end)
{
   if (js == null) return;
   
   List<String> argvals = getArguments(js,thisexpr,args);
   
   processSpecialCall(js,thisexpr,args,argvals);
   
   JcompType jt = js.getType();
   String s0 = jt.getName();
   int idx = s0.lastIndexOf(")");
   if (idx > 0) s0 = s0.substring(0,idx+1);
   
   ValidateCall usecall = null;
   
   for (ValidateCall vc : active_call.getInnerCalls()) {
      if (last_call != null) {
         if (vc == last_call) last_call = null;
         else continue;
       }
      String s1 = js.getFullName() + s0; 
      String s2 = vc.getMethod();
      if (!s1.equals(s2)) continue;
      last_call = vc;
      usecall = vc;
      break;
    }
   
   if (usecall == null) return;
   
   ASTNode mthdast = getAstNodeForCall(usecall);
   if (mthdast == null || !(mthdast instanceof MethodDeclaration)) return;
   MethodDeclaration mthd = (MethodDeclaration) mthdast;
   
   Map<String,String> varmap = new HashMap<>();
   int argidx = 0;
   if (!js.isStatic()) {
      String thisv = argvals.get(argidx++);
      if (thisv != null) varmap.put("this",thisv);
    }
      
   // need to handle varargs
   for (Object o : mthd.parameters()) {
      SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
      String nexpr = argvals.get(argidx++);
      String name = svd.getName().getIdentifier();
      if (nexpr != null) varmap.put(name,nexpr);
    }
   
   ValidateSetup nsetup = new ValidateSetup(this,usecall,varmap);
   nsetup.findResets();
}



private void processSpecialCall(JcompSymbol mthd,Expression thisexpr,
      List<?> args,List<String> argvals)
{
   if (thisexpr == null) return;
   JcompSymbol js = JcompAst.getReference(thisexpr);
   if (js == null) return;
   
   System.err.println("CHECK " + mthd.getFullName());
   if (mthd.getFullName().contains("get"))
      System.err.println("CHECK HERE");
   
   switch (mthd.getFullName()) {
      default :
         break;
    }
}



private void processAssignment(Assignment a,long start,long end)
{
   JcompSymbol js = JcompAst.getReference(a.getLeftHandSide());
   if (js == null) return;
}



private List<String> getArguments(JcompSymbol js,Expression thisexpr,List<?> args)
{
   List<String> rslt = new ArrayList<>();
   
   if (!js.isStatic()) {
      if (thisexpr != null) {
         String nthis = getRelativeExpression(thisexpr);
         if (nthis != null) rslt.add(nthis);
       }
      else {
         String curthis = variable_map.get("this");
         rslt.add(curthis);
       }
    }
   
   for (Object o : args) {
      Expression e = (Expression) o;
      String nexpr = getRelativeExpression(e); 
      rslt.add(nexpr);
    }
   
   return rslt;
}



private void updateVariableMap(long when)
{
   for (Iterator<String> it = variable_map.keySet().iterator(); it.hasNext(); ) {
      String var = it.next();
      ValidateVariable vv = active_call.getVariables().get(var);
      if (vv == null) continue;
      for (ValidateValue vval : vv.getValues(active_call.getTrace())) {
         long start = vval.getStartTime();
         if (start < first_time) continue;
         if (start < when) it.remove();
         break;
       }
    }
}
/********************************************************************************/
/*                                                                              */
/*      Processing visitor                                                      */
/*                                                                              */
/********************************************************************************/

private class SetupProcessor extends ASTVisitor {
   
   private int start_line;
   private long start_time;
   private long end_time;
   
   SetupProcessor(int lno,long start,long end) {
      start_line = lno;
      start_time = start;
      end_time = end;
    }
   
   @Override public boolean visit(ConstructorInvocation n) {
      process(n.arguments());
      JcompSymbol mthd = JcompAst.getReference(n);
      processCall(mthd,null,n.arguments(),start_time,end_time);
      return false;
    }
   
   @Override public boolean visit(SuperConstructorInvocation n) {
      process(n.arguments());
      JcompSymbol mthd = JcompAst.getReference(n);
      processCall(mthd,null,n.arguments(),start_time,end_time);
      return false;
    }
   
   @Override public boolean visit(MethodInvocation n) {
      process(n.getExpression());
      process(n.arguments());
      JcompSymbol mthd = JcompAst.getReference(n.getName());
      processCall(mthd,n.getExpression(),n.arguments(),start_time,end_time);
      return false;
    }
   
   @Override public boolean visit(ClassInstanceCreation n) {
      process(n.getExpression());
      process(n.arguments());
      JcompSymbol mthd = JcompAst.getReference(n);
      if (mthd == null) return false;
      // processCall(mthd,null,n.arguments(),start_time,end_time);
      // need to get a value for "this" inside the constructor
      return false;
    }
   
   @Override public boolean visit(Assignment n) {
      process(n.getRightHandSide());
      process(n.getLeftHandSide());
      processAssignment(n,start_time,end_time);
      return false;
    }
   
   @Override public boolean visit(Block b) {
      processSame(b.statements());
      return false;
    }
   
   @Override public boolean visit(DoStatement s) {
      process(s.getExpression());
      processSame(s.getBody());
      return false;
    }
   
   @Override public boolean visit(EnhancedForStatement s) {
      process(s.getExpression());
      processSame(s.getBody());
      return false;
    }

   @Override public boolean visit(ForStatement s) {
      process(s.initializers());
      process(s.getExpression());
      process(s.updaters());
      processSame(s.getBody());
      return false;
    }
  
   @Override public boolean visit(IfStatement s) {
      process(s.getExpression());
      processSame(s.getThenStatement());
      processSame(s.getElseStatement());
      return false;
    }
   
   @Override public boolean visit(SwitchStatement s) {
      process(s.getExpression());
      processSame(s.statements());
      return false;
    }
   
   @Override public boolean visit(SynchronizedStatement s) {
      process(s.getExpression());
      processSame(s.getBody());
      return false;
    }
   
   @Override public boolean visit(TryStatement s) {
      process(s.resources());
      processSame(s.getBody());
      processSame(s.catchClauses());
      processSame(s.getFinally());
      return false;
    }
   
   @Override public boolean visit(WhileStatement s) {
      process(s.getExpression());
      processSame(s.getBody());
      return false;
    }
   
   private void process(ASTNode n) {
      if (n != null) n.accept(this);
    }
   
   private void process(List<?> ns) {
      for (Object o : ns) {
         ASTNode n = (ASTNode) o;
         n.accept(this);
       }
    }
   
   private void processSame(ASTNode s) {
      if (s == null) return;
      CompilationUnit cu = (CompilationUnit) s.getRoot();
      int lno = cu.getLineNumber(s.getStartPosition());
      if (lno != start_line) return;
      process(s);
    }
   
   private void processSame(List<?> ns) {
      CompilationUnit cu = null;
      for (Object o : ns) {
         ASTNode n = (ASTNode) o;
         if (cu == null) cu = (CompilationUnit) n.getRoot();
         int lno = cu.getLineNumber(n.getStartPosition());
         if (lno != start_line) break;
         process(n);
       }
    }
   
}       // end of inner class SetupProcessor




/********************************************************************************/
/*                                                                              */
/*      Helper methodsm                                                         */
/*                                                                              */
/********************************************************************************/

private ASTNode getAstNodeForCall(ValidateCall vc)
{
   ValidateVariable vv = vc.getLineNumbers();
   if (vv == null || vc.getFile() == null) return null;
   int lno = vv.getLineAtTime(vc.getStartTime()+1);
   ASTNode n = base_context.getControl().getSourceNode(null,
         vc.getFile(),-1,lno,true,false);
   while (n != null) {
      switch (n.getNodeType()) {
         case ASTNode.METHOD_DECLARATION :
         case ASTNode.INITIALIZER :
            return n;
       }
      n = n.getParent();
    }
   return null; 
}



private Statement getStatementAtLine(ASTNode base,int line)
{
   ASTNode n = JcompAst.findNodeAtLine(base,line);
   while (n != null) {
      if (n instanceof Statement) return ((Statement) n);
      switch (n.getNodeType()) {
         case ASTNode.METHOD_DECLARATION :
         case ASTNode.INITIALIZER :
         case ASTNode.TYPE_DECLARATION :
         case ASTNode.ENUM_DECLARATION :
         case ASTNode.ANNOTATION_TYPE_DECLARATION :
         case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
            return null;
       }
      n = n.getParent();
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Get expression relative to initial frame                                */
/*                                                                              */
/********************************************************************************/

private String getRelativeExpression(ASTNode n) 
{
   ExprRewriter er = new ExprRewriter();
   n.accept(er);
   return er.getRewrite();
}


private class ExprRewriter extends ASTVisitor {
   
   private StringBuffer output_buffer;
   private boolean      is_valid;
   
   ExprRewriter() {
      output_buffer = new StringBuffer();
      is_valid = true;
    }
   
   String getRewrite()  {
      if (is_valid && output_buffer != null) return output_buffer.toString();
      return null;
    }
   
   @Override public boolean visit(ArrayAccess n) {
      gen(n.getArray());
      output("[");
      gen(n.getIndex());
      output("]");
      return false;
    }
   
   @Override public boolean visit(ArrayCreation n) {
      output("new ");
      output(n.getType());
      gen(n.dimensions(),null,"[","]");
      gen(n.getInitializer());
      return false;
    }
   
   @Override public boolean visit(ArrayInitializer n) {
      output("{");
      gen(n.expressions(),",",null,null);
      output("}");
      return false;
    }
   
   @Override public boolean visit(Assignment n) {
      gen(n.getLeftHandSide());
      output(n.getOperator().toString());
      gen(n.getRightHandSide());
      return false;
    }
   
   @Override public boolean visit(CastExpression n) {
      output("(");
      output(n.getType());
      output(")");
      gen(n.getExpression());
      return false;
    }

   @Override public boolean visit(ClassInstanceCreation n) {
      gen(n.getExpression(),null,".");
      output(n.getType());
      output("(");
      gen(n.arguments(),",",null,null);
      output(")");
      return false;
    }
   
   @Override public boolean visit(ConditionalExpression n) {
      gen(n.getExpression());
      output("?");
      gen(n.getThenExpression());
      output(":");
      gen(n.getElseExpression());
      return false;
    }
   
   @Override public boolean visit(FieldAccess n) {
      gen(n.getExpression());
      output(".");
      gen(n.getName());
      return false;
    }
   
   @Override public boolean visit(InfixExpression n) {
      gen(n.getLeftOperand());
      output(n.getOperator().toString());
      gen(n.getRightOperand());
      gen(n.extendedOperands(),null,n.getOperator().toString(),null);
      return false;
    }
   
   @Override public boolean visit(InstanceofExpression n) {
      gen(n.getLeftOperand());
      output(" instanceof ");
      output(n.getRightOperand());
      return false;
    }
   
   @Override public boolean visit(MethodInvocation n) {
      gen(n.getExpression());
      gen(n.getName());
      output("(");
      gen(n.arguments(),",",null,null);
      output(")");
      return false;
    }
   
   @Override public boolean visit(ParenthesizedExpression n) {
      output("(");
      gen(n.getExpression());
      output(")");
      return false;
    }
   
   @Override public boolean visit(PostfixExpression n) {
      gen(n.getOperand());
      output(n.getOperator().toString());
      return false;
    }
   
   @Override public boolean visit(PrefixExpression n) {
      output(n.getOperator().toString());
      gen(n.getOperand());
      return false;
    }
   
   
   @Override public boolean visit(QualifiedName n) {
      gen(n.getQualifier());
      output(".");
      gen(n.getName());
      return false;
    }
   
   @Override public boolean visit(SimpleName n) {
      String s = n.getIdentifier();
      JcompSymbol js = JcompAst.getReference(n);
      // should use JcompSymbol here
      String r = variable_map.get(s);
      if (r != null && !r.equals(s) && js != null && !js.isFieldSymbol() &&
            !js.isMethodSymbol() && !js.isTypeSymbol()) {
         output("(");
         output(r);
         output(")");
       }
      else if (js.isFieldSymbol()) {
         ASTNode par = n.getParent();
         if (par instanceof FieldAccess || par instanceof QualifiedName) {
            output(s);
          }
         else {
             String rthis = variable_map.get("this");
             if (rthis != null && !rthis.equals("this")) {
                output("(");
                output(rthis);
                output(".");
                output(s);
                output(")");
              }
             else output(s);
          }
       }
      else output(s);
      return false;
    }
   
   @Override public boolean visit(SuperFieldAccess n) {
      if (n.getQualifier() != null) {
         output(n.getQualifier());
         output(".");
       }
      output("super.");
      gen(n.getName());
      return false;
    }
   
   @Override public boolean visit(SuperMethodInvocation n) {
      if (n.getQualifier() != null) {
         output(n.getQualifier());
         output(".");
       }
      output("super.");
      gen(n.getName());
      output("(");
      gen(n.arguments(),",",null,null);
      output(")");
      return false;
    }
   
   @Override public boolean visit(ThisExpression n) {
      if (n.getQualifier() != null) {
         output(n.getQualifier());
         output(".");
       }
      output("this");
      return false;
    }
   
   @Override public boolean preVisit2(ASTNode n) {
      if (n instanceof Expression) {
         switch (n.getNodeType()) {
            case ASTNode.BOOLEAN_LITERAL :
            case ASTNode.CHARACTER_LITERAL :
            case ASTNode.NULL_LITERAL :
            case ASTNode.NUMBER_LITERAL :
            case ASTNode.STRING_LITERAL :
            case ASTNode.TYPE_LITERAL :
               return output(n);
            case ASTNode.NORMAL_ANNOTATION :
            case ASTNode.MARKER_ANNOTATION :
            case ASTNode.SINGLE_MEMBER_ANNOTATION :
            case ASTNode.CREATION_REFERENCE :
            case ASTNode.EXPRESSION_METHOD_REFERENCE :
            case ASTNode.SUPER_METHOD_REFERENCE :
            case ASTNode.TYPE_METHOD_REFERENCE :
            case ASTNode.LAMBDA_EXPRESSION :
            case ASTNode.SWITCH_EXPRESSION :
            case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
               is_valid = false;
               return false;
            default :
               return true;
          }
       }
      else {
         output(n);
         return false;
       }
    }
   
   
   private void gen(ASTNode n) {
      if (n == null || !is_valid || output_buffer == null) return;
      n.accept(this);
    }
   
   private void gen(ASTNode n,String pfx,String sfx) {
      if (n == null || !is_valid || output_buffer == null) return;
      output(pfx);
      n.accept(this);
      output(sfx);
    }
   
   private void gen(List<?> ns,String sep,String pfx,String sfx) {
      int ct = 0;
      for (Object o : ns) {
         ASTNode n = (ASTNode) o;
         if (ct++ > 0) output(sep);
         output(pfx);
         gen(n);
         output(sfx);
       }
    }
   
   private void output(String s) {
      if (s != null && output_buffer != null) output_buffer.append(s);
    }
   
   private boolean output(ASTNode n) {
      output(n.toString());
      return false;
    }
   
}




}       // end of class ValidateSetup




/* end of ValidateSetup.java */

