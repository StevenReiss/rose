/********************************************************************************/
/*                                                                              */
/*              BudValueData.java                                               */
/*                                                                              */
/*      Representation of a partial value from Bubbles                          */
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

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RoseException;
import edu.brown.cs.rose.root.RoseLog;

class BudValueData implements BudConstants, BudConstants.BudGenericValue  
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BudLaunch       bud_launch;
private RoseValueKind   value_kind;
private String val_name;
private String val_expr;
private String val_type;
private String val_value;
private boolean has_values;
private boolean is_local;
private boolean is_static;
private int array_length;
private Map<String,BudValueData> sub_values;
private BudValue result_value;
private int hash_code;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BudValueData(BudLaunch sm,Element xml,String name)
{
   bud_launch = sm;
   if (name == null) val_name = IvyXml.getAttrString(xml,"NAME");
   else val_name = name;
   val_expr = null;
   initialize(xml,null);
}

BudValueData(BudValueData par,Element xml)
{
   bud_launch = par.bud_launch;
   String vnm = IvyXml.getAttrString(xml,"NAME");
   if (par.val_expr != null) {
      val_expr = par.val_expr + "." + vnm;
    }
   String cnm = IvyXml.getAttrString(xml,"DECLTYPE");
   if (cnm != null) {
      vnm = getFieldKey(vnm,cnm);
    }
   val_name = par.val_name + "?" + vnm;
   
   initialize(xml,val_expr);
}


BudValueData(BudValue cv)
{
   bud_launch = null;
   val_name = null;
   val_expr = null;
   initialize(null,null);
   result_value = cv;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

RoseValueKind getKind()	{ return value_kind; }

String getType()		{ return val_type; }
String getValue()		{ return val_value; }

String getActualType()		{ return null; }
boolean hasContents()		{ return has_values; }
boolean isLocal()		{ return is_local; }
boolean isStatic()		{ return is_static; }
String getFrame()		{ return bud_launch.getFrame(); }
String getThread()		{ return bud_launch.getThread(); }
int getLength() 		{ return array_length; }



   
BudValue getBudValue()
{
   if (result_value != null) return result_value;
   
   if (value_kind == RoseValueKind.UNKNOWN && val_type == null) {
      return null;
    }
   
   if (val_type != null && val_type.equals("null")) {
      return BudValue.nullValue(bud_launch.findType("java.lang.Object"));
    }
   if (val_type != null && val_type.equals("void")) return null;
   
   BudType typ = bud_launch.findType(val_type);
   
   switch (value_kind) {
      case PRIMITIVE :
	 if (typ.isBooleanType()) {
	    result_value = BudValue.booleanValue(bud_launch.findType("boolean"),val_value);
	  }
	 else if (typ.isNumericType()) {
	    result_value = BudValue.numericValue(typ,val_value);
	  }
	 break;
      case STRING :
	 result_value = BudValue.stringValue(bud_launch.findType("java.lang.String"),val_value);
	 break;
      case OBJECT :
	 Map<String,BudGenericValue> inits = new HashMap<>();
	 Map<String,BudValueData> sets = new HashMap<>();
         if (typ.getFields() != null) {
            for (Map.Entry<String,BudType> ent : typ.getFields().entrySet()) {
               String fnm = ent.getKey();
               String cnm = null;
               String key = fnm;
               int idx1 = fnm.lastIndexOf(".");
               if (idx1 >= 0) {
                  cnm = fnm.substring(0,idx1);
                  key = fnm.substring(idx1+1);
                }
               if (cnm == null) cnm = typ.getName();
               key = getKey(key,cnm);
               if (sub_values != null && sub_values.get(key) != null) {
                  BudValueData fsvd = sub_values.get(key);
                  fsvd = bud_launch.getUniqueValue(fsvd);
                  sets.put(fnm,fsvd);
                }
               else {
                  DeferredLookup def = new DeferredLookup(fnm);
                  inits.put(fnm,def);
                }
             }
          }
	 if (hash_code == 0) {
	    inits.put(HASH_CODE_FIELD,new DeferredLookup(HASH_CODE_FIELD));
	  }
	 else {
	    BudValue hvl = BudValue.numericValue(bud_launch.intType(),hash_code);
	    inits.put(HASH_CODE_FIELD,hvl);
	  }
	 result_value = BudValue.objectValue(typ,inits);
         
	 for (Map.Entry<String,BudValueData> ent : sets.entrySet()) {
	    BudValue cv = ent.getValue().getBudValue();
	    try {
	       result_value.setFieldValue(ent.getKey(),cv);
	     }
	    catch (RoseException e) {
	       RoseLog.logE("BUD","Unexpected error setting field value",e);
	     }
	  }
	 break;
      case ARRAY :
	 if (array_length <= 1024) computeValues();
	 Map<Integer,BudGenericValue> ainits = new HashMap<>();
	 for (int i = 0; i < array_length; ++i) {
	    String key = "[" + i + "]";
	    String fullkey = getKey(key,null);
	    if (sub_values != null && sub_values.get(fullkey) != null) {
	       BudValueData fsvd = sub_values.get(fullkey);
	       fsvd = bud_launch.getUniqueValue(fsvd);
	       ainits.put(i,fsvd.getBudValue());
	     }
	    else {
	       DeferredLookup def = new DeferredLookup(key);
	       ainits.put(i,def);
	     }
	  }
	 result_value = BudValue.arrayValue(typ,array_length,ainits);
	 break;
      case CLASS :
	 int idx2 = val_value.lastIndexOf("(");
	 String tnm = val_value.substring(0,idx2).trim();
	 if (tnm.startsWith("(")) {
	    idx2 = tnm.lastIndexOf(")");
	    tnm = tnm.substring(1,idx2).trim();
	  }
         BudType ctyp = bud_launch.findType(tnm);
	 result_value = BudValue.classValue(bud_launch.findType("java.lang.Class"),ctyp);
	 break;
      case UNKNOWN :
	 break;
    }
   
   if (result_value == null) {
      RoseLog.logE("BUD","Unknown conversion to cashew value from bubbles");
    }
   
   return result_value;
}


private String getKey(String fnm,String cnm)
{
   if (fnm.equals(HASH_CODE_FIELD)) return fnm;
   
   String knm = getFieldKey(fnm,cnm);
   
   return val_name + "?" + knm;
}



private String getFieldKey(String fnm,String cnm)
{
   if (fnm.equals(HASH_CODE_FIELD)) return fnm;
   
   if (fnm.startsWith("[")) return fnm;
   
   if (cnm == null) {
      System.err.println("CHECK NULL HERE");
    }
   
   if (cnm != null) return cnm.replace("$",".") + "." + fnm;
   
   return fnm;
}



String findValue(BudValue cv,int lvl)
{
   if (result_value == null) return null;
   if (result_value == cv) return "";
   if (lvl == 0 || sub_values == null) return null;
   
   for (Map.Entry<String,BudValueData> ent : sub_values.entrySet()) {
      String r = ent.getValue().findValue(cv,lvl-1);
      if (r != null) {
	 if (array_length > 0) {
	    return "[" + ent.getKey() + "]";
	  }
	 else return "." + ent.getKey();
       }
    }
   
   return null;
}





/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void initialize(Element xml,String expr)
{
   val_type = IvyXml.getAttrString(xml,"TYPE");
   value_kind = IvyXml.getAttrEnum(xml,"KIND",RoseValueKind.UNKNOWN);
   val_value = IvyXml.getTextElement(xml,"DESCRIPTION");
   if (val_value == null) val_value = "";
   has_values = IvyXml.getAttrBool(xml,"HASVARS");
   is_local = IvyXml.getAttrBool(xml,"LOCAL");
   is_static = IvyXml.getAttrBool(xml,"STATIC");
   array_length = IvyXml.getAttrInt(xml,"LENGTH",0);
   sub_values = null;
   hash_code = 0;
   val_expr = expr;
   addValues(xml);
}



private void addValues(Element xml)
{
   if (xml == null) return;
   for (Element e : IvyXml.children(xml,"VALUE")) {
      if (sub_values == null) sub_values = new HashMap<String,BudValueData>();
      BudValueData vd = new BudValueData(this,e);
      String nm = vd.val_name;
      vd = bud_launch.getUniqueValue(vd);
      sub_values.put(nm,vd);
      // AcornLog.logD("ADD VALUE " + nm + " = " + vd);
    }
}



private synchronized void computeValues()
{
   if (!has_values || sub_values != null) return;
   if (val_expr == null) {
      Element root = bud_launch.evaluateFields(val_name);
      if (root != null) addValues(root);
    }
   else {
      BudValueData svd = bud_launch.evaluateExpr(val_expr);
      sub_values = svd.sub_values;
    }
}



void merge(BudValueData bvd)
{
   if (!has_values && bvd.has_values) {
      sub_values = bvd.sub_values;
      has_values = true;
      result_value = null;
    }
}

/********************************************************************************/
/*										*/
/*	Deferred value lookup							*/
/*										*/
/********************************************************************************/

private class DeferredLookup implements BudDeferredValue {
   
   private String field_name;
   
   DeferredLookup(String name) {
      field_name = name;
    }
   
   @Override public BudValue getValue() {
      computeValues();
      if (field_name.equals(HASH_CODE_FIELD)) {
         if (sub_values == null) sub_values = new HashMap<String,BudValueData>();
         if (sub_values.get(field_name) == null) {
            BudValueData svd = null;
            if (val_expr != null) {
               svd = bud_launch.evaluateExpr("System.identityHashCode(" + val_expr + ")");
             }
            else {
               svd = bud_launch.evaluateHashCode(val_name);
             }
            if (svd != null) sub_values.put(field_name,svd);
          }
       }
      
      if (sub_values == null) return null;
      String fnm = field_name;
      String cnm = null;
      int idx = fnm.lastIndexOf(".");
      if (idx >= 0) {
         cnm = fnm.substring(0,idx);
         fnm = fnm.substring(idx+1);
       }
      if (cnm == null) {
         cnm = getType();
       }
      String lookup = getKey(fnm,cnm);
      BudValueData svd = sub_values.get(lookup);
      svd = bud_launch.getUniqueValue(svd);
      if (svd == null) {
         RoseLog.logE("BUD","Deferred Lookup of " + lookup + " not found");
         return null;
       }
      return svd.getBudValue();
    }
   
}	// end of inner class DeferredLookup



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("<<");
   buf.append(value_kind);
   buf.append(":");
   buf.append(val_type);
   buf.append("@");
   buf.append(val_value);
   if (array_length > 0) buf.append("#" + array_length);
   buf.append(" ");
   buf.append(val_name);
   buf.append(">>");
   return buf.toString();
}







}       // end of class BudValueData




/* end of BudValueData.java */

