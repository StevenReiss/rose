/********************************************************************************/
/*                                                                              */
/*              PicotEffectItem.java                                            */
/*                                                                              */
/*      Descriptionn of source/target of an effect                              */
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

import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.TypeLiteral;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;

abstract class PicotEffectItem implements PicotConstants
{



/********************************************************************************/
/*                                                                              */
/*      Factory Methods                                                         */
/*                                                                              */
/********************************************************************************/

static PicotEffectItem createEffectItem(ASTNode n,Map<JcompSymbol,PicotEffectItem> locals)
{
   if (n == null) return null;
   
   PicotEffectItem rslt = null;
   ASTNode lhs = null;
   ASTNode rhs = null;
   PicotEffectItem refitm = null;
   
   switch (n.getNodeType()) {
      case ASTNode.STRING_LITERAL :
         rslt = new ConstantItem((StringLiteral) n);
         break;
      case ASTNode.NULL_LITERAL :
         rslt = new ConstantItem((NullLiteral) n);
         break;
      case ASTNode.NUMBER_LITERAL :
         rslt = new ConstantItem((NumberLiteral) n);
         break;
      case ASTNode.BOOLEAN_LITERAL :
         rslt = new ConstantItem((BooleanLiteral) n);
         break;
      case ASTNode.TYPE_LITERAL :
         rslt = new ConstantItem((TypeLiteral) n);
         break;
      case ASTNode.SIMPLE_NAME :
         rhs = n;
         break;
      case ASTNode.QUALIFIED_NAME :
         QualifiedName qn = (QualifiedName) n;
         rhs = qn.getName();
         lhs = qn.getQualifier();
         break;
      case ASTNode.FIELD_ACCESS :
         FieldAccess fa = (FieldAccess) n;
         rhs = fa.getName();
         lhs = fa.getExpression();
         break;
      case ASTNode.SUPER_FIELD_ACCESS :
         SuperFieldAccess sfa = (SuperFieldAccess) n;
         rhs = sfa.getName();
         break;
      case ASTNode.CLASS_INSTANCE_CREATION :
         JcompSymbol cnst = JcompAst.getReference(n);
         if (cnst != null) rslt = new NewObjectItem(cnst);
         break;
      case ASTNode.THIS_EXPRESSION :
         rslt = new ThisItem();
         break;
      default :
         if (n instanceof Expression) {
            rslt = new ExpressionItem();
          }
         else break;
    }
   
   if (rhs != null && rslt == null) {
      JcompSymbol jsym = JcompAst.getReference(rhs);
      if (jsym == null) jsym = JcompAst.getDefinition(rhs);
      if (jsym == null) return null;
      ASTNode ndef = jsym.getDefinitionNode();
      if (lhs != null) {
         refitm = createEffectItem(lhs,locals);
       }
      if (ndef.getLocationInParent() == MethodDeclaration.PARAMETERS_PROPERTY) {
         int idx = ((MethodDeclaration) ndef.getParent()).parameters().indexOf(ndef);
         rslt = new ParameterItem(jsym,idx);
       }
      else if (jsym.isFieldSymbol()) {
         Expression init = jsym.getInitializer();
         if (jsym.isFinal() && init != null) {
            PicotEffectItem inititm = createEffectItem(init,null);
            if (inititm instanceof ConstantItem) rslt = inititm;
          }
         if (rslt == null) {
            if (refitm == null) {
               if (!jsym.isStatic()) refitm = new ThisItem();
             }
            rslt = new FieldItem(jsym,refitm);
          }
       }
      else if (locals != null) {
         rslt = locals.get(jsym);
       }
    }
   
   return rslt;
}


static PicotEffectItem createBinaryParameter(int pno,JcompType jt)
{
   return new ParameterItem(jt,pno);
}

static PicotEffectItem createThisItem()
{
   return new ThisItem();
}


static PicotEffectItem createConstantItem(Object o,JcompTyper typer)
{
   return new ConstantItem(o,typer);
}

static PicotEffectItem createNewItem(JcompType jt)
{
   return new NewObjectItem(jt);
}


static PicotEffectItem createExpressionItem()
{
   return new ExpressionItem();
}

static PicotEffectItem createExpressionItem(JcompType jt)
{
   return new ExpressionItem(jt);
}


static PicotEffectItem createFieldItem(PicotEffectItem lhs,JcompSymbol js)
{
   return new FieldItem(js,lhs);
}


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcompType       data_type;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected PicotEffectItem()
{
   data_type = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access Methods                                                          */
/*                                                                              */
/********************************************************************************/

JcompType getDataType()                         { return data_type; }

abstract PicotItemType getItemType();

PicotEffectItem getItemSource()                 { return null; }

PicotEffectItem getItemParameter()              { return null; }

Object getConstantValue()                       { return null; }

JcompSymbol getSymbolValue()                    { return null; }

int getParameterNumber()                        { return -1; }

protected void setDataType(JcompType jt)        { data_type = jt; }



/********************************************************************************/
/*                                                                              */
/*      Constant Items                                                          */
/*                                                                              */
/********************************************************************************/

private static class ConstantItem extends PicotEffectItem {
   
   private Object constant_value;
   
   ConstantItem(StringLiteral n) {
      initialize(n);
      constant_value = n.getEscapedValue();
    }
   
   ConstantItem(NullLiteral n) {
      initialize(n);
      constant_value = null;
    }
   
   ConstantItem(NumberLiteral n) {
      initialize(n);
      constant_value = JcompAst.getNumberValue(n);
    }
   
   ConstantItem(BooleanLiteral n) {
      initialize(n);
      constant_value = n.booleanValue();
    }
   
   ConstantItem(TypeLiteral n) {
      initialize(n);
      constant_value = JcompAst.getJavaType(n);
    }
   
   ConstantItem(Object o,JcompTyper typer) {
      constant_value = o;
      String s = "int";
      if (o == null) {
         setDataType(typer.ANY_TYPE);
       }
      else {
         if (o instanceof String) s = "java.lang.String";
         else if (o instanceof Float) s = "float";
         else if (o instanceof Double) s = "double";
         else if (o instanceof Long) s = "long";
         else if (o instanceof Integer) s = "int";
         else s = "java.lang.Class";
         JcompType jt = typer.findSystemType(s);
         setDataType(jt);
       }
    }
   
   
   @Override PicotItemType getItemType()        { return PicotItemType.CONSTANT; }
   
   @Override Object getConstantValue()          { return constant_value; }
   
   private void initialize(ASTNode n) {
      setDataType(JcompAst.getExprType(n));
    }
   
}       // end of inner class ConstantItem



/********************************************************************************/
/*                                                                              */
/*      This item                                                               */
/*                                                                              */
/********************************************************************************/

private static class ThisItem extends PicotEffectItem {


   ThisItem() { }
   
   @Override PicotItemType getItemType()        { return PicotItemType.THIS; }
   

}       // end of inner class ThisItem



/********************************************************************************/
/*                                                                              */
/*      Parameter item                                                          */
/*                                                                              */
/********************************************************************************/

private static class ParameterItem extends PicotEffectItem {
   
   private JcompSymbol parameter_symbol;
   private int parameter_number;
   
   ParameterItem(JcompSymbol p,int pno) {
      parameter_symbol = p;
      parameter_number = pno;
      setDataType(p.getType());
    }
   
   ParameterItem(JcompType jt,int pno) {
      parameter_symbol = null;
      parameter_number = pno;
      setDataType(jt);
      
      
    }
   
   @Override PicotItemType getItemType()        { return PicotItemType.PARAMETER; }
   
   @Override JcompSymbol getSymbolValue()       { return parameter_symbol; }
   
   @Override int getParameterNumber()           { return parameter_number; }
   
}       // end of inner class ParameterItem



/********************************************************************************/
/*                                                                              */
/*      Parameter item                                                          */
/*                                                                              */
/********************************************************************************/

private static class FieldItem extends PicotEffectItem {

   private JcompSymbol field_symbol;
   private PicotEffectItem field_object;
   
   FieldItem(JcompSymbol s,PicotEffectItem lhs) {
      field_object = lhs;
      field_symbol = s;
      setDataType(s.getType());
    }
   
   @Override PicotItemType getItemType()        { return PicotItemType.FIELD; }
   
   @Override JcompSymbol getSymbolValue()       { return field_symbol; }
   
   @Override PicotEffectItem getItemSource()    { return field_object; }
   
}       // end of inner class FieldItem



/********************************************************************************/
/*                                                                              */
/*      Expression item                                                         */
/*                                                                              */
/********************************************************************************/

private static class ExpressionItem extends PicotEffectItem {
   
   ExpressionItem() { }
   
   ExpressionItem(JcompType jt) {
      setDataType(jt);
    }
   
   @Override PicotItemType getItemType()        { return PicotItemType.EXPRESSION; }
   
}       // end of inner class ParameterItem



/********************************************************************************/
/*                                                                              */
/*      New Object Item                                                         */
/*                                                                              */
/********************************************************************************/

private static class NewObjectItem extends PicotEffectItem {
   
   private JcompSymbol constructor_symbol;
   
   NewObjectItem(JcompSymbol cnst) {
      constructor_symbol = cnst;
      setDataType(cnst.getClassType());
    }
   
   NewObjectItem(JcompType typ) {
      constructor_symbol = null;
      setDataType(typ);
    }
   
   @Override PicotItemType getItemType()        { return PicotItemType.NEW_OBJECT; }
   
   @Override JcompSymbol getSymbolValue()       { return constructor_symbol; }
   
}       // end of inner class NewObjectItem



}       // end of class PicotEffectItem




/* end of PicotEffectItem.java */

