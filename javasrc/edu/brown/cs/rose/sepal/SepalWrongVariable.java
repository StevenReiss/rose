/********************************************************************************/
/*                                                                              */
/*              SepalWrongVariable.java                                         */
/*                                                                              */
/*      Suggest repairs where the user might have used the wrong variable       */
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.file.IvyStringDiff;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompScope;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompSymbolKind;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.root.RootRepairFinderDefault;
import edu.brown.cs.rose.root.RoseLog;

public class SepalWrongVariable extends RootRepairFinderDefault
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

public SepalWrongVariable()
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
   if (stmt instanceof AssertStatement) return;
   
   Collection<UserVariable> vars = findVariables(stmt);
   if (vars == null || vars.isEmpty()) return;
   
   Map<JcompType,List<UserVariable>> types = findTypes(vars);
   
   int ct = findReplacements(stmt,types);
   ct += findMethodReplacements(vars);
   if (ct == 0) return;
   
   RoseLog.logD("SEPAL","Check variables on " +  getLocation().getLineNumber() + " " + stmt);
   
   for (UserVariable uv : vars) {
      int rct = uv.getReplacements().size();
      int lct = uv.getLocations().size();
      for (JcompSymbol js : uv.getReplacements()) {
         // Might want to take location priority into account as well
         if (rct > 4 || lct > 6 || lct*rct > 10) {
            double pri = IvyStringDiff.normalizedStringDiff(uv.getSymbol().getName(),js.getName());
            if (pri < 0.4) continue;
          }
         for (ASTNode n : uv.getLocations()) {
            if (isReplacementRelevant(uv.getSymbol(),js,n)) {
               createRepair(uv,js,n);
             }
          }
       }
    }
   
   return;
}



private boolean isReplacementRelevant(JcompSymbol orig,JcompSymbol rep,ASTNode n)
{
   if (rep.getDefinitionNode() != null && 
         rep.getDefinitionNode().getStartPosition() > n.getStartPosition())
      if (!orig.isMethodSymbol() && !orig.isFieldSymbol())
         return false;
   
   if (n.getParent() instanceof InfixExpression) {
      InfixExpression exp = (InfixExpression) n.getParent();
      JcompSymbol js1 = JcompAst.getReference(exp.getLeftOperand());
      JcompSymbol js2 = JcompAst.getReference(exp.getRightOperand());
      if (rep == js1 || rep == js2) 
         return false;
    }
   
   return true;
}



@Override public double getFinderPriority()
{
   return 0.5;
}




/********************************************************************************/
/*                                                                              */
/*      Find variables in statement                                             */
/*                                                                              */
/********************************************************************************/

private Collection<UserVariable> findVariables(ASTNode n)
{
   VariableFinder vf = new VariableFinder(n);
   n.accept(vf);
   return vf.getUserVariables();
}



private class VariableFinder extends ASTVisitor {

   private Map<JcompSymbol,UserVariable> symbol_map;
   private CompilationUnit base_unit;
   private int base_line;
   
   VariableFinder(ASTNode n) {
      symbol_map = new HashMap<>(); 
      base_unit = (CompilationUnit) n.getRoot();
      base_line = base_unit.getLineNumber(n.getStartPosition());
    }
   
   Collection<UserVariable> getUserVariables()          { return symbol_map.values(); }
   
   @Override public boolean preVisit2(ASTNode n) {
      if (n instanceof Statement) {
         int ln = base_unit.getLineNumber(n.getStartPosition());
         if (ln > base_line) return false;
       }
   
      return true;
    }
   
   
   @Override public void endVisit(SimpleName n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null) {
         UserVariable uv = symbol_map.get(js);
         if (uv == null) {
            uv = new UserVariable(js);
            symbol_map.put(js,uv);
          }
         uv.addLocation(n);
       }
    }
  
}       // end of inner class VariableFinder



/********************************************************************************/
/*                                                                              */
/*      Find the set of types that are relevant to user variables               */
/*                                                                              */
/********************************************************************************/

private Map<JcompType,List<UserVariable>> findTypes(Collection<UserVariable> vars)
{
   Map<JcompType,List<UserVariable>> rslt = new HashMap<>();
   
   for (UserVariable uv : vars) {
      JcompSymbol js = uv.getSymbol();
      if (js.isTypeSymbol()) continue;
      JcompType jt = js.getType();
      List<UserVariable> luv = rslt.get(jt);
      if (luv == null) {
         luv = new ArrayList<>();
         rslt.put(jt,luv);
       }
      luv.add(uv);
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Find replacements for each user variable                                */
/*                                                                              */
/********************************************************************************/

private int findReplacements(ASTNode base,Map<JcompType,List<UserVariable>> typemap)
{
   int ct = 0;
   
   JcompScope curscp = null;
   JcompSymbol curmthd = null;
   for (ASTNode n = base; n != null; n = n.getParent()) {
      if (curscp == null) curscp = JcompAst.getJavaScope(n);
      if (n instanceof MethodDeclaration && curmthd == null) {
         MethodDeclaration md = (MethodDeclaration) n;
         curmthd = JcompAst.getDefinition(md);
       }
      if (curscp != null && curmthd != null) break;
    }
   if (curscp == null) return 0;
   
   JcompTyper typer = JcompAst.getTyper(base);
   
   Collection<JcompSymbol> allsyms = curscp.getAllSymbols();
   for (JcompSymbol cand : allsyms) {
      if (!isRelevant(cand)) continue;
      if (cand == curmthd) continue;
      JcompType jt = cand.getDeclaredType(typer);
      List<UserVariable> vars = typemap.get(jt);
      if (vars == null) continue;
      for (UserVariable uv : vars) {
         if (isRelevant(cand,uv.getSymbol(),base)) {
            uv.addReplacement(cand);
            ++ct;
          }
       }
    }
  
  return ct;
}


private int findMethodReplacements(Collection<UserVariable> vars)
{
   int ct = 0;
   
   for (UserVariable uv : vars) {
      JcompSymbol js = uv.getSymbol();
      if (!js.isMethodSymbol()) continue;
      if (js.isConstructorSymbol()) continue;
      if (js.isBinarySymbol()) continue;
      JcompType jt = js.getClassType();
      JcompScope jscp = jt.getScope();
      for (JcompSymbol js1 : jscp.getDefinedMethods()) {
         if (js1.getName().equals(js.getName())) continue;
         if (js1.getType().getName().equals(js.getType().getName())) {
            uv.addReplacement(js1);
            ++ct;
          }
         
       }
    }
   
   return ct;
}



private boolean isRelevant(JcompSymbol js)
{
   return true;
}


private boolean isRelevant(JcompSymbol rep,JcompSymbol orig,ASTNode base)
{
   if (rep == orig) return false;
   
   if (orig.isBinarySymbol() != rep.isBinarySymbol()) return false;
   if (rep.isFinal() != rep.isFinal()) return false;
   if (rep.getName().equals(orig.getName())) return false;
   if (rep.getSymbolKind() != orig.getSymbolKind()) return false;
   if (rep.isConstructorSymbol()) return false;
   if (rep.getSymbolKind() == JcompSymbolKind.LOCAL) {
      if (rep.getDefinitionNode().getStartPosition() >= base.getStartPosition()) return false;
    }
   if (rep.getName().equals("serialVersionUID")) return false;
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Create a repair based on substitution                                   */
/*                                                                              */
/********************************************************************************/

private void createRepair(UserVariable uv,JcompSymbol js,ASTNode where)
{
   AST ast = where.getAST();
   ASTRewrite rw = null;
   
   synchronized (ast) {
      if (where instanceof SimpleName) {
         rw = ASTRewrite.create(ast);
         String nnm = js.getName();
         if (js.isConstructorSymbol()) return;
         if (nnm.startsWith("<")) return;
         RoseLog.logD("SEPAL","Use replacement name " + nnm + " for " + where);
         ASTNode rep = ast.newSimpleName(nnm);
         rw.replace(where,rep,null);
       }
      else if (where instanceof QualifiedName) {
         // handle qualified name changes
       }
    }
   
   // this should be skipped if the variable being replaced is the only use of that variable
   if (rw != null) {
      double pri = IvyStringDiff.normalizedStringDiff(uv.getSymbol().getName(),js.getName());
      String desc = "Replace `" + uv.getSymbol().getName() + "' with `" + js.getName() + "'";

      ASTNode par = where.getParent();
      String pdesc = par.toString();
      int idx = pdesc.indexOf("\n");
      if (idx > 0) pdesc = pdesc.substring(0,idx) + "...";
      desc += " in " + pdesc;
      String logdata = getClass().getName() + "#" + pri;
      double priority = 0.25 + (pri * 0.75);
      addRepair(rw,desc,logdata,priority);
    }
   
}

/********************************************************************************/
/*                                                                              */
/*      Reopresentation of a user variable that might be replaced               */
/*                                                                              */
/********************************************************************************/

private class UserVariable {
    
   private JcompSymbol  variable_symbol;
   private List<ASTNode> variable_locations;
   private Set<JcompSymbol> replace_with;
   
   UserVariable(JcompSymbol js) {
      variable_symbol = js;
      variable_locations = new ArrayList<>();
      replace_with = new HashSet<>();
    }
   
   JcompSymbol getSymbol()                      { return variable_symbol; }
   Collection<JcompSymbol> getReplacements()    { return replace_with; }
   Collection<ASTNode> getLocations()           { return variable_locations; }
   
   void addReplacement(JcompSymbol js) {
      if (js == variable_symbol) return;
      replace_with.add(js);
    }
   
   void addLocation(ASTNode n) {
      variable_locations.add(n);
    }
   
}       // end of inner class UserVariable

}       // end of class SepalWrongVariable




/* end of SepalWrongVariable.java */

