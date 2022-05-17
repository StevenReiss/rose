/********************************************************************************/
/*                                                                              */
/*              PicotValueAccessor.java                                         */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.root.RootValidate.RootTraceValue;

abstract class PicotValueAccessor implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Static constructors                                                     */
/*                                                                              */
/********************************************************************************/

static PicotValueAccessor createVariableAccessor(String var,String typename)
{
   return new VariableAccessor(var,typename);
}



static PicotValueAccessor createFieldAccessor(PicotValueAccessor base,String field,JcompType fldtyp)
{
   return new FieldAccessor(base,field,fldtyp);
}


static PicotValueAccessor createFieldAccessor(PicotValueAccessor base,String field,String fldtyp)
{
   return new FieldAccessor(base,field,fldtyp);
}


static PicotValueAccessor createArrayAccessor(PicotValueAccessor array,JcompType elttyp,int idx)
{
   return new ArrayAccessor(array,elttyp,idx);
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcompType           data_type;
protected String            type_name;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected PicotValueAccessor(JcompType typ)
{
   data_type = typ;
   type_name = typ.getName();
}


protected PicotValueAccessor(String typnam)
{
   data_type = null;
   type_name = typnam;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

abstract PicotAccessorType getAccessorType();

abstract PicotCodeFragment getGetterCode(PicotValueBuilder bldr,JcompType targettype);

List<PicotCodeFragment> getSetterCodes(PicotValueBuilder bldr,
      PicotCodeFragment val,RootTraceValue base)
{
   List<PicotCodeFragment> rslt = new ArrayList<>();
   PicotCodeFragment pcf1 = getSetterCode(bldr,val);
   if (pcf1 != null) rslt.add(pcf1);
   
   return rslt;
}


protected PicotCodeFragment getSetterCode(PicotValueBuilder bldr,PicotCodeFragment val)
{
   return null;
}


String getTypeName()
{
   return type_name;
}


JcompType getDataType(PicotValueBuilder bldr)                        
{ 
   if (data_type == null && bldr != null) {
      JcompTyper typer = bldr.getJcompTyper();
      data_type = typer.findType(type_name);
    }
   
   return data_type;
}




/********************************************************************************/
/*                                                                              */
/*      Basic accessor -- simple variable                                       */
/*                                                                              */
/********************************************************************************/

private static class VariableAccessor extends PicotValueAccessor
{
   
   private String variable_name;
   
   VariableAccessor(String var,String typ) {
      super(typ);
      variable_name = var;
    }
   
   @Override PicotAccessorType getAccessorType()          { return PicotAccessorType.VARIABLE; }
   
   @Override PicotCodeFragment getGetterCode(PicotValueBuilder bldr,JcompType targettype) {
      return new PicotCodeFragment(variable_name);
    }
   
   @Override protected PicotCodeFragment getSetterCode(PicotValueBuilder bldr,PicotCodeFragment val) {
      if (variable_name.equals(val.getCode())) return null;
      return new PicotCodeFragment(variable_name + " = " + val.getCode() + ";\n");
    }
   
   @Override public String toString() {
      return "@" + variable_name;
    }
   
}       // end of inner class VariableAccessor




/********************************************************************************/
/*                                                                              */
/*      Field accessor                                                          */
/*                                                                              */
/********************************************************************************/

private static class FieldAccessor extends PicotValueAccessor {
   
   private PicotValueAccessor base_value;
   private String field_name;
   
   FieldAccessor(PicotValueAccessor base,String fldnam,JcompType fldtyp) {
      super(fldtyp);
      base_value = base;
      field_name = fldnam;
    }
   
   FieldAccessor(PicotValueAccessor base,String fldname,String fldtyp) {
      super(fldtyp);
      base_value = base;
      field_name = fldname;
   }
   
   @Override PicotAccessorType getAccessorType()        { return PicotAccessorType.FIELD; } 
   
   @Override PicotCodeFragment getGetterCode(PicotValueBuilder bldr,JcompType targettype) {
      PicotCodeFragment lhs = base_value.getGetterCode(bldr,null);
      if (lhs == null) return null;
      JcompType btyp = base_value.getDataType(bldr);
      JcompSymbol fldsym = btyp.lookupField(bldr.getJcompTyper(),field_name);
      PicotCodeFragment pcf = bldr.buildFieldGetter(lhs,fldsym,base_value.getDataType(bldr));
      if (targettype != null && fldsym.getType() != targettype && pcf != null) {
         pcf = new PicotCodeFragment("((" + targettype.getName() + ") " + pcf.getCode() + ")");
       }
      return pcf;
    }
   
   List<PicotCodeFragment> getSetterCodes(PicotValueBuilder bldr,PicotCodeFragment val,RootTraceValue base) {
      JcompType btyp = base_value.getDataType(bldr);
      JcompTyper typer = bldr.getJcompTyper();
      JcompSymbol fldsym = btyp.lookupField(typer,field_name);
      JcompType tgttyp = typer.findSystemType(base.getDataType());
      PicotCodeFragment lhs = base_value.getGetterCode(bldr,tgttyp);
      
      List<PicotCodeFragment> rlst = bldr.buildFieldSetter(lhs,fldsym,base_value.getDataType(bldr),val,base);
      
      return rlst;
    }
   
   @Override public String toString() {
      return base_value.toString() + "." + field_name;
    }
   
}       // end of inner class FieldAccessor




/********************************************************************************/
/*                                                                              */
/*      Array element accessor                                                  */
/*                                                                              */
/********************************************************************************/

private static class ArrayAccessor extends PicotValueAccessor {

   private PicotValueAccessor base_value;
   private int array_index;
   
   ArrayAccessor(PicotValueAccessor base,JcompType basetype,int idx) {
      super(basetype);
      base_value = base;
      array_index = idx;
    }
   
   @Override PicotAccessorType getAccessorType()        { return PicotAccessorType.ARRAY; } 
   
   @Override PicotCodeFragment getGetterCode(PicotValueBuilder bldr,JcompType targettype) {
      PicotCodeFragment pcf = base_value.getGetterCode(bldr,null);
      if (pcf == null) return null;
      return pcf.append("[",Integer.toString(array_index),"]");
    }
   
   @Override protected PicotCodeFragment getSetterCode(PicotValueBuilder bldr,
         PicotCodeFragment val) {
      JcompType arrtyp = base_value.getDataType(bldr);
      PicotCodeFragment pcf = base_value.getGetterCode(bldr,arrtyp);
      if (pcf == null) return null;
      return pcf.append("[",Integer.toString(array_index),"] = ",val.getCode(),";\n");
    }
   
   @Override public String toString() {
      return base_value.toString() + "[" + array_index + "]";
    }
}       // end of inner class ArrayAccessor




}       // end of class PicotValueAccessor




/* end of PicotValueAccessor.java */

