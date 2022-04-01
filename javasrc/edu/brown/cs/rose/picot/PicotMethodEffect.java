/********************************************************************************/
/*                                                                              */
/*              PicotMethodEffect.java                                          */
/*                                                                              */
/*      Describe an effect of invoking a methods                                */
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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThisExpression;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;

abstract class PicotMethodEffect implements PicotConstants
{



/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

static PicotMethodEffect createAssignment(ASTNode lhs,ASTNode rhs,PicotLocalMap lcls)
{
   PicotEffectItem rhsitm = PicotEffectItem.createEffectItem(rhs,lcls);
   if (rhsitm == null) rhsitm = PicotEffectItem.createExpressionItem();
   
   JcompSymbol lhssym = JcompAst.getReference(lhs);
   if (lhssym == null && lhs != null && lhs.getNodeType() == ASTNode.FIELD_ACCESS) {
      FieldAccess facc = (FieldAccess) lhs;
      if (facc.getExpression() instanceof ThisExpression) {
         lhssym = JcompAst.getReference(facc.getName());
       }
    }
    
   if (lhssym == null) lhssym = JcompAst.getDefinition(lhs);
   if (lhssym == null) return null;
   
   if (lhssym.isTypeSymbol() || lhssym.isEnumSymbol() ||
         lhssym.isMethodSymbol())
      return null;
   
   if (lhssym.isFieldSymbol()) {
      PicotEffectItem lhsitm = PicotEffectItem.createEffectItem(lhs,null);
      if (lhsitm != null) {
         return new FieldEffect(lhsitm,rhsitm);
       }
    }
   else if (lhs instanceof SimpleName) {
       lcls.put(lhssym,rhsitm);
    }
   
   return null;
}


static PicotMethodEffect createReturn(ReturnStatement s,PicotLocalMap lcls)
{
   PicotEffectItem retitm = PicotEffectItem.createEffectItem(s.getExpression(),lcls);
   
   if (retitm != null) {
      return new ReturnEffect(retitm);
    }
   
   return null;
}

static PicotMethodEffect createReturn(PicotEffectItem pei) 
{
   return new ReturnEffect(pei);
}

static PicotMethodEffect createField(PicotEffectItem lhs,PicotEffectItem rhs)
{
   return new FieldEffect(lhs,rhs);
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected PicotMethodEffect()
{ }



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

abstract PicotEffectType getEffectType();

PicotEffectItem getEffectTarget()                       { return null; }

PicotEffectItem getEffectSource()                       { return null; }

PicotEffectItem getEffectArgument()                     { return null; }



/********************************************************************************/
/*                                                                              */
/*      Assignment effect                                                       */
/*                                                                              */
/********************************************************************************/

private static class FieldEffect extends PicotMethodEffect {
   
   private PicotEffectItem target_item;
   private PicotEffectItem source_item;
   
   FieldEffect(PicotEffectItem lhs,PicotEffectItem rhs) {
      target_item = lhs;
      source_item = rhs;
    }
   
   @Override PicotEffectType getEffectType()            { return PicotEffectType.SET_FIELD; }
   
   @Override PicotEffectItem getEffectSource()          { return source_item; }
   
   @Override PicotEffectItem getEffectTarget()          { return target_item; }   
   
}       // end of inner class FieldEffect



/********************************************************************************/
/*                                                                              */
/*      Return effect                                                           */
/*                                                                              */
/********************************************************************************/

private static class ReturnEffect extends PicotMethodEffect {

   private PicotEffectItem target_item;
   
   ReturnEffect(PicotEffectItem exp) {
      target_item = exp;
    }
   
   @Override PicotEffectType getEffectType()            { return PicotEffectType.RETURN; }
   
   @Override PicotEffectItem getEffectTarget()          { return target_item; }   
   
}       // end of inner class FieldEffect




}       // end of class PicotMethodEffect




/* end of PicotMethodEffect.java */

