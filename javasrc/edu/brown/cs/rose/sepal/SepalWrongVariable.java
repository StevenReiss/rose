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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompScope;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.root.RootRepairFinderDefault;

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
   
   Collection<UserVariable> vars = findVariables(stmt);
   if (vars == null || vars.isEmpty()) return;
   
   Map<JcompType,List<UserVariable>> types = findTypes(vars);
   
   int ct = findReplacements(stmt,types);
   if (ct == 0) return;
}



/********************************************************************************/
/*                                                                              */
/*      Find variables in statement                                             */
/*                                                                              */
/********************************************************************************/

private Collection<UserVariable> findVariables(ASTNode n)
{
   VariableFinder vf = new VariableFinder();
   n.accept(vf);
   return vf.getUserVariables();
}



private class VariableFinder extends ASTVisitor {

   private Map<JcompSymbol,UserVariable> symbol_map;
   
   VariableFinder() {
      symbol_map = new HashMap<>(); 
    }
   
   Collection<UserVariable> getUserVariables()          { return symbol_map.values(); }
   
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
   for (ASTNode n = base; n != null; n = n.getParent()) {
      curscp = JcompAst.getJavaScope(n);
      if (curscp != null) break;
    }
   if (curscp == null) return 0;
   
   Collection<JcompSymbol> allsyms = curscp.getAllSymbols();
   for (JcompSymbol cand : allsyms) {
      if (!isRelevant(cand)) continue;
      JcompType jt = cand.getDeclaredType();
      List<UserVariable> vars = typemap.get(jt);
      if (vars == null) continue;
      for (UserVariable uv : vars) {
         if (isRelevant(cand,uv.getSymbol())) {
            uv.addReplacement(cand);
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


private boolean isRelevant(JcompSymbol rep,JcompSymbol orig)
{
   if (rep == orig) return false;
   
   if (orig.isBinarySymbol() != rep.isBinarySymbol()) return false;
   if (rep.isFinal() != rep.isFinal()) return false;
   if (rep.getName().equals(orig.getName())) return false;
   if (rep.getSymbolKind() != orig.getSymbolKind()) return false;
   
   return true;
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

