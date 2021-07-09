/********************************************************************************/
/*                                                                              */
/*              BudValue.java                                                   */
/*                                                                              */
/*      Representation of a run-time value                                      */
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

import edu.brown.cs.rose.root.RoseException;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RootValue;

public abstract class BudValue implements RootValue, BudConstants, BudConstants.BudGenericValue
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BudType         value_type;



/********************************************************************************/
/*                                                                              */
/*      Static factory methods                                                  */
/*                                                                              */
/********************************************************************************/

static BudValue nullValue(BudType typ)
{
   return new NullValue(typ);
}


static BudValue booleanValue(BudType typ,String val)
{
   boolean v = Boolean.valueOf(val);
   return new BooleanValue(typ,v);
}

static BudValue numericValue(BudType typ,String val)
{
   try {
      Long lval = Long.parseLong(val);
      return new NumericValue(typ,lval);
    }
   catch (NumberFormatException e) { }
   try {
      Double dval = Double.parseDouble(val);
      return new NumericValue(typ,dval);
    }
   catch (NumberFormatException e) { }
   
   return new NumericValue(typ,0);
}



static BudValue numericValue(BudType typ,long val)
{
   return new NumericValue(typ,val);
}


static BudValue stringValue(BudType typ,String val)
{
   return new StringValue(typ,val);
}


static BudValue objectValue(BudType typ,Map<String,BudGenericValue> inits)
{
   return new ObjectValue(typ,inits);
}


static BudValue arrayValue(BudType typ,int len,Map<Integer,BudGenericValue> inits)
{
   return new ArrayValue(typ,len,inits);
}


static BudValue classValue(BudType typ)
{
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BudValue(BudType typ) 
{
   value_type = typ;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void setFieldValue(String name,BudValue value) throws RoseException
{
   throw new RoseException("Value is not an object");
}

public BudValue getFieldValue(String name) throws RoseException
{
   throw new RoseException("Value is not an object");
}

void setArrayElement(int idx,BudValue value) throws RoseException
{
   throw new RoseException("Value is not an array");
}


public BudValue getArrayElement(int idx) throws RoseException
{
   throw new RoseException("Value is not an array");
}


public boolean isNull()                        { return false; }


public boolean getBoolean()   
{
   throw new IllegalArgumentException("Non-boolean value");
}


public long getInt()
{
   throw new IllegalArgumentException("Non-numeric value");
}

public String getString()
{
   throw new IllegalArgumentException("Non-string value");
}



public BudType getDataType()                   { return value_type; }


public String getJavaValue()                    { return toString(); }



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   xw.begin("VALUE");
   xw.field("TYPE",value_type.getName());
   localOutputXml(xw);
   xw.end("VALUE");
}



abstract protected void localOutputXml(IvyXmlWriter xw);




/********************************************************************************/
/*                                                                              */
/*      Null value                                                              */
/*                                                                              */
/********************************************************************************/

private static class NullValue extends BudValue {

   NullValue(BudType typ) {
      super(typ);
    }
   
   @Override public boolean isNull()           { return true; }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("NULL",true);
      xw.field("VALUE","null");
    }
   
   @Override public String toString()          { return "null"; }
   
}       // end of inner class NullValue



/********************************************************************************/
/*                                                                              */
/*      Boolean values                                                          */
/*                                                                              */
/********************************************************************************/

private static class BooleanValue extends BudValue {
   
   private boolean cur_value;
   
   BooleanValue(BudType typ,boolean v) {
      super(typ);
      cur_value = v;
    }
   
   @Override public boolean getBoolean()                 { return cur_value; }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("BOOLEAN",cur_value);
      xw.field("VALUE",cur_value);
    }
   
   @Override public String toString() {
      return Boolean.toString(cur_value);
    }
   
}       // end of inner class BooleanValue



/********************************************************************************/
/*                                                                              */
/*      Numeric values                                                          */
/*                                                                              */
/********************************************************************************/

private static class NumericValue extends BudValue {
   
   private Number cur_value;
   
   NumericValue(BudType typ,Number v) {
      super(typ);
      cur_value = v;
    }
   
   @Override public long getInt() {
      return cur_value.longValue();
    }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("NUMBER",cur_value);
      xw.field("VALUE",cur_value);
    }
   
   @Override public String getJavaValue() {
      switch (getDataType().getName()) {
         case "int" :
            return Integer.toString(cur_value.intValue());
         case "short" :
            return "((short) " + Short.toString(cur_value.shortValue()) + ")";
         case "byte" :
            return "((byte) " + Byte.toString(cur_value.byteValue()) + ")";
         case "long" :
            return Long.toString(cur_value.longValue()) + "L";
         case "float" :
            return Float.toString(cur_value.floatValue()) + "F";
         case "double" :
            return Double.toString(cur_value.doubleValue());
         case "char" :
            return "((char) " +  Short.toString(cur_value.shortValue()) + ")";
         default :
            break;
       }
      return toString();
    }
   @Override public String toString()           { return cur_value.toString(); }
   
}       // end of inner class IntegerValue



/********************************************************************************/
/*                                                                              */
/*      String values                                                           */
/*                                                                              */
/********************************************************************************/

private static class StringValue extends BudValue {
   
   private String cur_value;
   
   StringValue(BudType typ,String s) {
      super(typ);
      cur_value = s;
    }
   
   @Override public String getString() {
      return cur_value;
    }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("STRING",true);
      xw.cdataElement("CONTENTS",cur_value);
      xw.cdataElement("VALUE",cur_value);
    }
   
   @Override public String getJavaValue() {
      return "\"" + IvyFormat.formatString(cur_value) + "\"";
    }
   
   @Override public String toString()           { return "\"" + cur_value + "\""; }
   
}       // end of inner class StringValue



/********************************************************************************/
/*                                                                              */
/*      Object values                                                           */
/*                                                                              */
/********************************************************************************/

private static class ObjectValue extends BudValue {

   private Map<String,BudGenericValue> field_values;
   
   ObjectValue(BudType typ,Map<String,BudGenericValue> flds) {
      super(typ);
      if (flds == null) field_values = new HashMap<>();
      else field_values = new HashMap<>(flds);
    }
   
   @Override void setFieldValue(String nm,BudValue val) {
      field_values.put(nm,val);
    }
   
   @Override public BudValue getFieldValue(String nm) {
      BudGenericValue gv = field_values.get(nm);
      if (gv == null) {
         int idx = nm.lastIndexOf(".");
         if (idx < 0) return null;
         String nm1 = nm.substring(idx+1);
         gv = field_values.get(nm1);
       }
      if (gv == null) return null;
      if (gv instanceof BudDeferredValue) {
         BudDeferredValue dv = (BudDeferredValue) gv;
         gv = dv.getValue();
         field_values.put(nm,gv);
       }
      return (BudValue) gv;
    }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("OBJECT",true);
      for (Map.Entry<String,BudGenericValue> ent : field_values.entrySet()) {
         xw.begin("FIELD");
         xw.field("NAME",ent.getKey());
         BudGenericValue gv = ent.getValue();
         if (gv instanceof BudDeferredValue) {
            xw.field("DEFERRED",true);
          }
         else if (gv instanceof BudValue) {
            BudValue fvl = (BudValue) gv;
            fvl.outputXml(xw);
          }
         xw.end("FIELD");
       }
    }
   
   @Override public String getJavaValue() {
      // this doesn't work -- is it needed?
      return toString();
    }
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("{ ");
      int ct = 0;
      for (Map.Entry<String,BudGenericValue> ent : field_values.entrySet()) {
         BudGenericValue gv = ent.getValue();
         if (gv instanceof BudValue) {
            BudValue bv = (BudValue) gv;
            if (bv.getDataType().isArrayType() || bv.getDataType().isObjectType()) continue;
            if (ct++ > 0) buf.append(",");
            buf.append(ent.getKey());
            buf.append(":");
            buf.append(bv.toString());
          }
       }
      buf.append("}");
      return buf.toString();
    }
   
}       // end of inner class ObjectValue


/********************************************************************************/
/*                                                                              */
/*      Array values                                                            */
/*                                                                              */
/********************************************************************************/

private static class ArrayValue extends BudValue {
   
   private Map<Integer,BudGenericValue> array_values;
   private int dim_size;
   
   ArrayValue(BudType typ,int dim,Map<Integer,BudGenericValue> elts) {
      super(typ);
      if (elts == null) array_values = new HashMap<>();
      else array_values = new HashMap<>(elts);
    }
   
   @Override void setArrayElement(int idx,BudValue val) throws RoseException {
      if (idx < 0 || idx >= dim_size) throw new RoseException("Index out of bounds");
      array_values.put(idx,val);
    }
   
   @Override public BudValue getArrayElement(int idx) throws RoseException {
      if (idx < 0 || idx >= dim_size) throw new RoseException("Index out of bounds");
      BudGenericValue gv = array_values.get(idx);
      if (gv == null) return null;
      if (gv instanceof BudDeferredValue) {
         BudDeferredValue dv = (BudDeferredValue) gv;
         gv = dv.getValue();
         array_values.put(idx,gv);
       }
      return (BudValue) gv;
    }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("ARRAY",true);
      for (Map.Entry<Integer,BudGenericValue> ent : array_values.entrySet()) {
         xw.begin("ELEMENT");
         xw.field("INDEX",ent.getKey());
         BudGenericValue gv = ent.getValue();
         if (gv instanceof BudDeferredValue) {
            xw.field("DEFERRED",true);
          }
         else if (gv instanceof BudValue) {
            BudValue fvl = (BudValue) gv;
            fvl.outputXml(xw);
          }
         xw.end("ELEMENT");
       }
    }
   
   @Override public String getJavaValue() {
      // this doesn't work -- is it needed?
      return toString();
    } 
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("[ ");
      int ct = 0;
      for (Map.Entry<Integer,BudGenericValue> ent : array_values.entrySet()) {
         BudGenericValue gv = ent.getValue();
         if (gv instanceof BudValue) {
            BudValue bv = (BudValue) gv;
            if (bv.getDataType().isArrayType() || bv.getDataType().isObjectType()) continue;
            if (ct++ > 0) buf.append(",");
            buf.append(ent.getKey());
            buf.append(":");
            buf.append(bv.toString());
          }
       }
      buf.append("]");
      return buf.toString();
    }
   
}       // end of iinner class ArrayValue


}       // end of class BudValue




/* end of BudValue.java */

