/********************************************************************************/
/*                                                                              */
/*              BudType.java                                                    */
/*                                                                              */
/*      Description of a data type                                              */
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



package edu.brown.cs.rose.bud;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RootControl;

abstract public class BudType
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          type_name;



/********************************************************************************/
/*                                                                              */
/*      Static creation methods                                                 */
/*                                                                              */
/********************************************************************************/

static BudType createNewType(BudLaunch lnch,String name)
{
   if (name.endsWith("[]")) return new ArrayType(lnch,name);
   if (name.contains("<")) return new ParameterizedType(lnch,name);
   
   switch (name) {
      case "boolean" :
         return new BooleanType();
      case "int" :
      case "short" :
      case "byte" :
      case "char" :
      case "long" :
         return new IntegerType(name);
      case "float" :
      case "double" :
         return new FloatingType(name);
      case "void" :
         return new VoidType();
      case "java.lang.String" :
         return new StringType();
    }
   
   return new ObjectType(lnch,name);
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BudType(String name)
{
   type_name = name; 
   String vtype = type_name;
   if (vtype != null) {
      int idx = vtype.indexOf("<");
      int idx1 = vtype.lastIndexOf(">");
      if (idx >= 0) {
	 vtype = type_name.substring(0,idx);
	 if (idx1 > 0) vtype += type_name.substring(idx1+1);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getName()
{
   return type_name;
}



boolean isBooleanType()
{
   return false;
}


boolean isNumericType()
{
   return false;
}

boolean isStringType()
{
   return false; 
}


boolean isArrayType()
{
   return false; 
}


boolean isParameterizedType()
{
   return false;
}



BudType getBaseType()           
{
   return null;
}


Map<String,BudType> getFields()
{
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Primitive types                                                         */
/*                                                                              */
/********************************************************************************/

private static class VoidType extends BudType {
   
   VoidType() {
      super("void");
    }
   
}       // end of inner class VoidType



private static class BooleanType extends BudType {
   
   BooleanType() {
      super("boolean");
    }
   
   @Override boolean isBooleanType()                    { return true; }
   
}       // end of innerclass BooleanType



private static class IntegerType extends BudType {
   
   IntegerType(String name) {
      super(name);
    }
   
   @Override boolean isNumericType()                    { return true; }

}       // end of inner class IntegerType



private static class FloatingType extends BudType {

   FloatingType(String name) {
      super(name);
    }
   
   @Override boolean isNumericType()                    { return true; }
   
}       // end of inner class IntegerType



private static class StringType extends BudType {
   
   StringType() {
      super("java.lang.String");
    }
   
   @Override boolean isStringType()                     { return true; }
   
}       // end of inner class StringType



private static class ArrayType extends BudType {
   
   private BudType base_type;
   
   ArrayType(BudLaunch lnch,String name) {
      super(name);
      int idx = name.lastIndexOf("[]");
      if (idx < 0) base_type = null;
      else {
         String basename = name.substring(0,idx).trim();
         base_type = lnch.findType(basename);
       }
    }
   
   @Override boolean isArrayType()                      { return true; }
   @Override BudType getBaseType()                      { return base_type; }
   
}       // end of inner class ArrayType



private static class ParameterizedType extends BudType {
   
   private BudType base_type;
   
   ParameterizedType(BudLaunch lnch,String name) {
      super(name);
      int idx = name.indexOf("<");
      if (idx < 0) base_type = null;
      else {
         String basename = name.substring(0,idx).trim();
         base_type = lnch.findType(basename);
       }
    }
   
   @Override boolean isParameterizedType()              { return true; }
   @Override BudType getBaseType()                      { return base_type; }
   
}       // end of inner class




private static class ObjectType extends BudType {
   
   private Map<String,BudType>  field_map;
   private BudLaunch            for_launch;
   
   ObjectType(BudLaunch lnch,String name) {
      super(name);
      for_launch = lnch;
    }
   
   Map<String,BudType> getFields() {
      if (field_map != null) return field_map;
      field_map = new HashMap<>();
      RootControl ctrl = for_launch.getControl();
      String pat = getName() + ".*";
      CommandArgs args = new CommandArgs("PATTERN",pat,"FOR","FIELD",
            "FIELDS",true,"DEFS",true,"REFS",false);
      Element xml = ctrl.sendBubblesMessage("PATTERNSEARCH",args,null);
      for (Element mat : IvyXml.children(xml,"MATCH")) {
         Element itm = IvyXml.getChild(mat,"ITEM");
         if (itm == null) continue;
         String typ = IvyXml.getAttrString(itm,"TYPE");
         if (typ == null || !typ.equals("Field")) continue;
         String key = IvyXml.getAttrString(itm,"KEY");
         int idx = key.indexOf(")");
         if (idx < 0) continue;
         String typ0 = key.substring(idx+1);
         String typ1 = IvyFormat.formatTypeName(typ0,true);
         String fnm = IvyXml.getAttrString(itm,"NAME");
         field_map.put(fnm,for_launch.findType(typ1));
       }
   
      return field_map;
    }

}       // end of inner class ObjectType




}       // end of class BudType




/* end of BudType.java */

