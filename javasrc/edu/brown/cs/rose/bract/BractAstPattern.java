/********************************************************************************/
/*                                                                              */
/*              BractAstPattern.java                                            */
/*                                                                              */
/*      AST patterns for matching and replacing                                 */
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



package edu.brown.cs.rose.bract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.AST;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.rose.root.RoseLog;

public class BractAstPattern implements BractConstants
{



/********************************************************************************/
/*                                                                              */
/*      Static creation methods                                                 */
/*                                                                              */
/********************************************************************************/

public static BractAstPattern statement(String... pat)
{
   return new BractAstPattern(PatternType.STATEMENT,null,pat);
}


public static BractAstPattern statement(PatternMap defaults,String ... pat)
{
   return new BractAstPattern(PatternType.STATEMENT,defaults,pat);
}



public static BractAstPattern expression(String... pat)
{ 
   return new BractAstPattern(PatternType.EXPRESSION,null,pat);
}


public static BractAstPattern expression(PatternMap defaults,String... pat)
{
   return new BractAstPattern(PatternType.EXPRESSION,defaults,pat);
}




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum PatternType { STATEMENT, EXPRESSION, DECLARATION }

private List<ASTNode>   base_nodes;
private PatternMap      default_values;
private String          pat_summary;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BractAstPattern(PatternType type,PatternMap defaults,String [] pats)
{
   base_nodes = new ArrayList<>();
   default_values = defaults;
   pat_summary = null;
   
   for (String pat : pats) {
      if (pat_summary == null) pat_summary = pat;
      ASTNode node = null;
      try {
         switch (type) {
            case STATEMENT :
               node = JcompAst.parseStatement(pat);
               break;
            case EXPRESSION :
               node = JcompAst.parseExpression(pat);
               break;
            case DECLARATION :
               node = JcompAst.parseDeclarations(pat);
               break;
          }
       }
      catch (Throwable t) {
         RoseLog.logE("Problem parsing pattern " + pat,t);
       }
      if (node != null) base_nodes.add(node);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Top-level match and replace methods                                     */
/*                                                                              */
/********************************************************************************/

public boolean match(ASTNode n,PatternMap values) 
{
   for (ASTNode base : base_nodes) {
      PatternMap nmap = new PatternMap(values);
      if (matchPattern(base,n,nmap)) {
         values.putAll(nmap);
         addDefaults(values);
         return true;
       }
    }
   
   return false;
}



public Map<ASTNode,PatternMap> matchAll(ASTNode n,PatternMap values)
{
   SubtreeMatcher sm = new SubtreeMatcher(values);
   n.accept(sm);
   
   return sm.getResults();
}


public ASTRewrite replace(ASTNode orig,PatternMap values)
{
   AST ast = orig.getAST();
   
   ASTNode n = getResult(orig,values);
   if (n != null) {
      ASTRewrite rw = ASTRewrite.create(ast);
      rw.replace(orig,n,null);
      return rw;
    }
   
   return null;
}



public ASTNode getResult(ASTNode orig,PatternMap values)
{
   AST ast = orig.getAST();
   
   for (ASTNode base : base_nodes) {
      ASTNode n = ASTNode.copySubtree(ast,base);
      SubtreeSubst ss = new SubtreeSubst(n,values);
      n.accept(ss);
      if (ss.isValid()) return ss.getResult();
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Subtree matching                                                        */
/*                                                                              */
/********************************************************************************/

private class SubtreeMatcher extends ASTVisitor {
   
   private Map<ASTNode,PatternMap> match_results;
   private PatternMap orig_values;
   
   SubtreeMatcher(PatternMap values) {
      match_results = new HashMap<>();
      orig_values = values;
    }
   
   Map<ASTNode,PatternMap> getResults() {
      if (match_results.isEmpty()) return null;
      return match_results;
    }
   
   @Override public boolean preVisit2(ASTNode n) {
      for (ASTNode base : base_nodes) {
         if (n.getNodeType() == base.getNodeType()) {
            PatternMap nmap = new PatternMap(orig_values);
            if (matchPattern(base,n,nmap)) {
               addDefaults(nmap);
               match_results.put(n,nmap);
//             return false;
             }
          }
       }
      return true;
    }
   
}       // end of inner class SubtreeMatcher



/********************************************************************************/
/*                                                                              */
/*      Basic matching                                                          */
/*                                                                              */
/********************************************************************************/

private boolean matchPattern(ASTNode pat,ASTNode orig,PatternMap values)
{
   if (pat == null){
      if (orig == null) return true;
      return false;
    }  
   else if (values != null && isVariableNode(pat)) {
      return matchVariable(pat,orig,values);
    }
   else if (values != null && isSubPattern(pat)) {
      return matchSubpattern(pat,orig,values);
    }
   else if (orig == null) return false;
   else if (pat.getNodeType() != orig.getNodeType()) return false;
   
   for (Object o1 : pat.structuralPropertiesForType()) {
      StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) o1;
      if (spd.isChildProperty()) {
        ASTNode pc = (ASTNode) pat.getStructuralProperty(spd); 
        ASTNode oc = (ASTNode) orig.getStructuralProperty(spd);
        if (!matchPattern(pc,oc,values)) return false;
       }
      else if (spd.isChildListProperty()) {
         List<?> pl = (List<?>) pat.getStructuralProperty(spd);
         List<?> ol = (List<?>) orig.getStructuralProperty(spd);   
         if (pl.size() != ol.size()) return false;
         for (int i = 0; i < pl.size(); ++i) {
            ASTNode pc = (ASTNode) pl.get(i);
            ASTNode oc = (ASTNode) ol.get(i);
            if (!matchPattern(pc,oc,values)) return false;
          }
       }
      else {
         Object pv = pat.getStructuralProperty(spd);
         Object ov = orig.getStructuralProperty(spd);
         if (pv == null) {
            if (ov != null) return false;
          }
         else if (!pv.equals(ov)) return false;
       }
    }
   
   return true;
}




private boolean isSubPattern(ASTNode pat)
{
   String id = getPatternNode(pat);
   if (id != null && id.charAt(0) == ANY_PATTERN) return true;
   
   return false;
}


private String getPatternNode(ASTNode n)
{
   if (n instanceof SimpleName) {
      String pat = ((SimpleName) n).getIdentifier();
      if (Character.isUpperCase(pat.charAt(0))) return pat;
    }
   else if (n instanceof ExpressionStatement) {
      ExpressionStatement es = (ExpressionStatement) n;
      return getPatternNode(es.getExpression());
    }
   else if (n instanceof MethodInvocation) {
      MethodInvocation mi = (MethodInvocation) n;
      if (mi.arguments().size() == 0 && mi.getExpression() == null) {
         return getPatternNode(mi.getName());
       }
    }
   return null;
}



private String getPatternName(ASTNode n)
{
   String s = getPatternNode(n);
   if (s == null) return null;
   if (s.length() == 1) return null;
   if (!Character.isUpperCase(s.charAt(0))) return null;
   return s.substring(1);
}



private boolean matchSubpattern(ASTNode pat,ASTNode orig,PatternMap values)
{
   String s = getPatternName(pat);
   if (s == null) return false;
   BractAstPattern p0 = (BractAstPattern) values.get(s);
   if (p0 == null) return false;
   for (String key : p0.default_values.keySet()) {
      if (values.get(key) == null) values.put(key,p0.default_values.get(key));
    }
   
   for (ASTNode base : p0.base_nodes) {
      if (matchPattern(base,orig,values)) {
         Integer ctr = (Integer) values.get(s + "_COUNT");
         if (ctr == null) ctr = 0;
         ++ctr;
         values.put(s + "_" + ctr,orig);
         values.put(s + "_COUNT",ctr);
         return true;

       }    
    }
   
   return false;
}



private void addDefaults(PatternMap map) 
{
   if (default_values == null) return;
   
   for (Map.Entry<String,Object> ent : default_values.entrySet()) {
      String nam = ent.getKey();
      if (map.get(nam) != null) continue;
      map.put(nam,ent.getValue());
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle variables                                                        */
/*                                                                              */
/********************************************************************************/

private boolean isVariableNode(ASTNode pat)
{
   String id = getPatternNode(pat);
   if (id != null) {
      switch (id.charAt(0)) {
         case ANY_AST :
         case ANY_EXPR :
         case ANY_STMT :
         case ANY_VAR :
         case ANY_INT :
         case ANY_STRING :
         case ESCAPE_PATTERN :
            return true;
         case ANY_PATTERN :
            return false;
       }
    }
   
   return false;
}



private boolean matchVariable(ASTNode pat,ASTNode orig,PatternMap values)
{
   char typ = getPatternNode(pat).charAt(0);
   
   String nam = getPatternName(pat);
   
   switch (typ) {
      case ANY_AST :
         return matchSubtree(nam,values,orig);
      case ANY_EXPR :
         if (orig instanceof Expression) {
            return matchSubtree(nam,values,orig);
          }
         break;
      case ANY_STMT :
         if (orig instanceof Statement) {
            return matchSubtree(nam,values,orig);
          }
         break;
      case ANY_VAR :
         if (orig instanceof SimpleName) {
            return matchSubtree(nam,values,orig);
          }
         break;
      case ANY_INT :
         if (orig instanceof NumberLiteral) {
            if (nam == null) return true;
            String nv = ((NumberLiteral) orig).getToken();
            try {
               int i = Integer.parseInt(nv);
               Object val = values.get(nam);
               if (val == null) {
                  values.put(nam,i);
                  return true;
                }
               else if (val instanceof Integer) {
                  return i == ((Integer) val).intValue();
                }
             }
            catch (NumberFormatException e) { }
          }
         break;
      case ANY_STRING :
         if (orig instanceof StringLiteral) {
            if (nam == null) return true;
            String nv = ((StringLiteral) orig).getLiteralValue();
            Object val = values.get(nam);
            if (val == null) {
               values.put(nam,nv);
               return true;
             }
            else if (val instanceof String) {
               return val.equals(nv);
             }
          }
         else if (orig instanceof TextBlock) {
            if (nam == null) return true;
            String nv = ((TextBlock) orig).getLiteralValue();
            Object val = values.get(nam);
            if (val == null) {
               values.put(nam,nv);
               return true;
             }
            else if (val instanceof String) {
               return val.equals(nv);
             }
          }
         break;
      case ESCAPE_PATTERN :
         if (orig instanceof SimpleName) {
            String id = ((SimpleName) orig).getIdentifier();
            if (nam == null) return id.equals("X");
            else return id.equals(nam);
          }
         break;
    }
   
   return false;
}



private boolean matchSubtree(String nam,PatternMap values,ASTNode orig)
{
   if (nam == null) return true;
   Object v = values.get(nam);
   if (v == null) {
      values.put(nam,orig);
      return true;
    }
   if (v instanceof ASTNode) {
     return  matchPattern((ASTNode) v,orig,null);
    }
   
   return false;
}


public String getSummary()
{
   return pat_summary;
}



/********************************************************************************/
/*                                                                              */
/*      Handle substitutions                                                    */
/*                                                                              */
/********************************************************************************/

private class SubtreeSubst extends ASTVisitor {
   
   private PatternMap pattern_values;
   private AST base_ast;
   private ASTNode result_node;
   private boolean is_valid;
   
   SubtreeSubst(ASTNode base,PatternMap map) {
      base_ast = base.getAST();
      result_node = base;
      pattern_values = map;
      is_valid = true;
    }
   
   boolean isValid()                                    { return is_valid; }
   ASTNode getResult()                                  { return result_node; }
  
   @Override public boolean visit(SimpleName sn) {
      ASTNode rep = getSubstitution(sn);
      if (rep == sn) return false;
      
      replaceNode(sn,rep);
      return false;
    }
   
   @Override public boolean visit(ExpressionStatement es) {
      ASTNode rep = getSubstitution(es);
      if (rep == es) return true;
      
      if (rep instanceof Statement) {
         replaceNode(es,rep);
       }
      else if (rep == null) {
         replaceNode(es,null);
       }
      else {
         es.setExpression((Expression) rep);
       }
      return false;
    }
   
   
   private ASTNode getSubstitution(ASTNode sn) {
      String s = getPatternNode(sn);
      if (s == null) return sn;
      char typ = s.charAt(0);
      String s1 = getPatternName(sn);
      if (s1 == null) return sn;
      if (typ == ESCAPE_PATTERN) {
         return base_ast.newSimpleName(s1);
       }
      Object val = pattern_values.get(s1);
      if (val == null) {
         if (!pattern_values.containsKey(s1)) is_valid = false;
         return sn;
       }
      
      ASTNode rep = null;
      if (val instanceof ASTNode) { 
         rep = ASTNode.copySubtree(base_ast,(ASTNode) val);
       }
      else if (val instanceof Number) {
         rep = base_ast.newNumberLiteral(val.toString());
       }
      else if (val instanceof String) {
         if (typ == ANY_VAR) {
            rep = base_ast.newSimpleName((String) val);
          }
         else if (typ == ANY_STRING) {
            StringLiteral slit = base_ast.newStringLiteral();
            slit.setLiteralValue((String) val);
            rep = slit;
          }
       }
      
      return rep;
    }
   
   @SuppressWarnings({ "rawtypes", "unchecked" })
   private void replaceNode(ASTNode node,ASTNode rep) {
      if (node == result_node) {
         result_node = rep;
         return;
       }
      
      StructuralPropertyDescriptor spd = node.getLocationInParent();
      if (spd != null && spd.isChildProperty()) {
         node.getParent().setStructuralProperty(spd,rep);
       }
      else if (spd != null && spd.isChildListProperty()) {
         List vals = (List) node.getParent().getStructuralProperty(spd);
         int idx = vals.indexOf(node);
         if (rep != null) vals.set(idx,rep);
         else vals.remove(idx);
       }
    }
   
}       // end of inner class SubtreeSubst









}       // end of class BractAstPattern




/* end of BractAstPattern.java */

