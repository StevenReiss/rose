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
import edu.brown.cs.rose.root.RootValue;

public class BudValue implements RootValue, BudConstants, BudConstants.BudGenericValue
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
   boolean v = Boolean.getBoolean(val);
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
   return null;
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

BudValue getFieldValue(String name) throws RoseException
{
   throw new RoseException("Value is not an object");
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
   
   @Override BudValue getFieldValue(String nm) {
      BudGenericValue gv = field_values.get(nm);
      if (gv == null) return null;
      if (gv instanceof BudDeferredValue) {
         BudDeferredValue dv = (BudDeferredValue) gv;
         gv = dv.getValue();
         field_values.put(nm,gv);
       }
      return (BudValue) gv;
    }
   
}       // end of inner class ObjectValue



}       // end of class BudValue




/* end of BudValue.java */

