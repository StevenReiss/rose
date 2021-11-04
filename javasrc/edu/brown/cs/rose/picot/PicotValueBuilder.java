/********************************************************************************/
/*                                                                              */
/*              PicotValueBuilder.java                                          */
/*                                                                              */
/*      Create a value based on SEEDE value                                     */
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootValidate.RootTrace;
import edu.brown.cs.rose.root.RootValidate.RootTraceValue;
import edu.brown.cs.rose.root.RootValidate.RootTraceVariable;

class PicotValueBuilder implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootTrace               for_trace;
private long                    start_time;
private JcompTyper              jcomp_typer;
private Map<JcompType,PicotClassData> class_data;
private PicotValueChecker       value_checker;
private PicotValueContents      cur_contents;

private static final AtomicInteger variable_counter = new AtomicInteger(0);



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotValueBuilder(RootControl ctrl,RootValidate rv,long start,JcompTyper typer)
{
   for_trace = rv.getExecutionTrace();
   start_time = start;
   jcomp_typer = typer;
   class_data = new HashMap<>();
   value_checker = new PicotValueChecker(ctrl,rv);
   cur_contents = new PicotValueContents(value_checker,jcomp_typer);
}   



void finished()
{ 
   value_checker.finished();
}



/********************************************************************************/
/*                                                                              */
/*      Build code to create value                                              */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment buildValue(RootTraceVariable rtv)
{
   RootTraceValue rval = rtv.getValueAtTime(for_trace,start_time);
   return buildValue(rval);
}



PicotCodeFragment buildValue(RootTraceValue rtv)
{
   if (rtv.isNull()) return new PicotCodeFragment("null");
   
   PicotCodeFragment rslt = null;
   
   String id = rtv.getId();
   rslt = cur_contents.getPreviousValue(id);
   if (rslt != null) return rslt;
   
   String typ = rtv.getDataType();
   JcompType jtyp = jcomp_typer.findType(typ);
   if (jtyp == null) return null;
   if (jtyp.isPrimitiveType()) {
      rslt = buildPrimitiveValue(jtyp,rtv.getValue());
    }
   else if (jtyp.isStringType()) {
      rslt = buildStringValue(rtv.getValue());
    }
   else if (jtyp.getName().equals("java.lang.Class")) {
      rslt = buildClassValue(rtv.getValue());
    }
   else if (jtyp.getName().equals("java.io.File")) {
      rslt = buildFileValue(rtv.getValue());
    }
   else if (jtyp.isArrayType()) {
       rslt = buildArrayValue(rtv,jtyp);
    }
   else if (jtyp.isFunctionRef()) {
      
    }
   else if (jtyp.isCompiledType()) {
      rslt = buildUserObjectValue(rtv,jtyp);
    }
   else {
      rslt = buildSystemObjectValue(rtv,jtyp);
    }
   
   cur_contents.setPreviousValue(id,rslt);
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Primitive types                                                         */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment buildPrimitiveValue(JcompType typ,String val)
{
   String rslt = null;
   
   switch (typ.getName()) {
      case "int" :
         rslt = val;
         break;
      case "short" :
         rslt = "((short) " + val + ")";
         break;
      case "byte" :
         rslt = "((byte) " + val + ")";
         break;
      case "long" :
         if (val.endsWith("L") || val.endsWith("l")) rslt = val;
         else rslt = val + "L";
         break;
      case "char" :
         char cv = (char) Integer.parseInt(val);
         String sv = String.valueOf(cv);
         rslt = "'" + IvyFormat.formatChar(sv) + "'";
         break;
      case "float" :
         if (val.endsWith("F") || val.endsWith("F")) rslt = val;
         else rslt = val + "F";
         break;
      case "double" :
         if (val.contains(".") || val.contains("E") || val.contains("e")) 
            rslt = val;
         else rslt = val + ".0";
         break;
      case "boolean" :
         if (val.equals("0") || val.equalsIgnoreCase("false")) rslt = "false";
         else rslt = "true";
         break;
    }
   
   if (rslt == null) return null;
   
   return new PicotCodeFragment(rslt);
}


private PicotCodeFragment buildStringValue(String val)
{
   String rslt = "\"" + IvyFormat.formatString(val) + "\"";
   
   return new PicotCodeFragment(rslt);
}


private PicotCodeFragment buildClassValue(String val)
{
   String rslt = val + ".class";
   
   return new PicotCodeFragment(rslt);
}


private PicotCodeFragment buildFileValue(String val)
{
   String rslt = "new java.io.File(" + val + ")";
   
   return new PicotCodeFragment(rslt);
}



/********************************************************************************/
/*                                                                              */
/*      Handle arrays                                                           */
/*                                                                              */
/********************************************************************************/

private PicotCodeFragment buildArrayValue(RootTraceValue rtv,JcompType typ)
{
   int sz = rtv.getArrayLength();
   StringBuffer buf = new StringBuffer();
   buf.append("new " + typ.getBaseType() + "[" + sz + "]");
   if (sz > 0) {
      buf.append(" { ");
      for (int i = 0; i < sz; ++i) {
         RootTraceValue etv = rtv.getIndexValue(for_trace,i,start_time);
         PicotCodeFragment efg = buildValue(etv);
         if (efg == null) return null;
         if (i > 0) buf.append(" , ");
         buf.append(efg.getCode());
       }
      buf.append(" } ");
    }
   
   return new PicotCodeFragment(buf.toString());
}



/********************************************************************************/
/*                                                                              */
/*      Handle objects                                                          */
/*                                                                              */
/********************************************************************************/

private PicotCodeFragment buildUserObjectValue(RootTraceValue rtv,JcompType typ)
{
   Map<String,JcompType> flds = typ.getFields(jcomp_typer);
   PicotClassData pcd = getClassData(typ);
   Map<JcompSymbol,PicotCodeFragment> fldval = new HashMap<>();
   Collection<JcompSymbol> mthds = pcd.getMethods(); 
   Set<JcompType> paramtypes = new HashSet<>();
   for (JcompSymbol js : mthds) {
      if (js.isConstructorSymbol()) {
         paramtypes.addAll(js.getType().getComponents());
       }
      else if (js.isStatic() && js.getType().getBaseType() == typ) {
         paramtypes.addAll(js.getType().getComponents());
       }
    }
      
   for (String fld : flds.keySet()) {
      String fnm = fld;
      int idx = fnm.lastIndexOf(".");
      if (idx > 0) fnm = fld.substring(idx+1);
      JcompSymbol fldsym = typ.lookupField(jcomp_typer,fnm);
      RootTraceValue ftv = rtv.getFieldValue(for_trace,fld,start_time);
      if (ftv != null && fldsym != null && paramtypes.contains(fldsym.getType())) {
         PicotCodeFragment rslt = buildValue(ftv);
         if (rslt != null) {
            fldval.put(fldsym,rslt);
          }
       }
      else if (ftv != null && fldsym != null) {
         PicotCodeFragment rslt = buildValue(ftv);
         if (rslt != null) {
            fldval.put(fldsym,rslt);
          }
       }
      else if (fldsym == null) {
         System.err.println("Can't find field " + fld);
       }
    }
   
   List<PicotCodeFragment> rslts = new ArrayList<>();
   for (JcompSymbol js : mthds) {
      PicotMethodData pmd = null;
      if (js.isConstructorSymbol()) {
         pmd = pcd.getDataForMethod(js);
       }
      else if (js.isStatic() && js.getType().getBaseType() == typ) {
         pmd = pcd.getDataForMethod(js);
       }
      if (pmd != null) {
         buildCodeForMethod(js,pmd,null,fldval,rslts);
       }
    }

   RoseLog.logD("PICOT","FOR " + typ.getName());

   String var = "v" + variable_counter.incrementAndGet();
   for (PicotCodeFragment pcf : rslts) {
      String decl = typ.getName() + " " + var + " = " + pcf.getCode() + ";\n";
      PicotCodeFragment npcf = new PicotCodeFragment(decl);
      RoseLog.logD("PICOT","TRY: " + npcf.getCode());
      PicotValueContents npvc = new PicotValueContents(cur_contents,npcf,var,rtv);
      PicotValueContents nnpvc = npvc.fixupValues();
      if (nnpvc == null) continue;
      cur_contents = nnpvc;
      return new PicotCodeFragment(var);
    }
   
   return null;
}








/********************************************************************************/
/*                                                                              */
/*      Handle system objects                                                   */
/*                                                                              */
/********************************************************************************/

private PicotCodeFragment buildSystemObjectValue(RootTraceValue rtv,JcompType typ)
{
   // handle Collections by creating empty collection and doing ADD multiple times
   // handle Map by creating empty map and doing PUT multiple times
   // special case Point, etc. when rtv has actual values
   
   switch (typ.getName()) {
      default :
         break;
    }
   
   Collection<JcompSymbol> mthds = typ.getDefinedMethods(jcomp_typer);
   for (JcompSymbol js : mthds) {
      if (js.isPublic() && js.isConstructorSymbol()) {
         if (js.getType().getComponents().size() == 0) {
            String call = "new " + typ.getName() + "()";
            RoseLog.logD("PICOT","SYS TRY : " + call);
            return new PicotCodeFragment(call);
          }
       }
    }
   
   return null;
}



private void buildCodeForMethod(JcompSymbol js,PicotMethodData pmd,
      PicotCodeFragment thiscode,Map<JcompSymbol,PicotCodeFragment> fldvals,
      List<PicotCodeFragment> rslts)
{
   String call = null;
   
   if (js.isConstructorSymbol()) {
      call = "new " + js.getClassType().getName();
    }
   else if (js.isStatic()) {
      call = js.getClassType().getName() + "." + js.getName();
    }
   else if (thiscode == null) return;
   else {
      call = thiscode + "." + js.getName();
    }
   call += "(";
   
   int ct = 0;
   addParameter(call,pmd,ct,fldvals,rslts);
}


private void addParameter(String pfx,PicotMethodData pmd,int pno,
      Map<JcompSymbol,PicotCodeFragment> fldvals,List<PicotCodeFragment> rslts)
{
   List<JcompSymbol> params = pmd.getParameters();
   if (pno >= params.size()) {
      rslts.add(new PicotCodeFragment(pfx + ")"));
      return;
    }
   if (pno > 0) pfx += ",";
   JcompSymbol param = params.get(pno);
   List<String> pvals = getParameterValues(param,pmd,fldvals);
   for (String s : pvals) {
      String call = pfx + s;
      addParameter(call,pmd,pno+1,fldvals,rslts);
    }
}



private List<String> getParameterValues(JcompSymbol param,PicotMethodData pmd,
      Map<JcompSymbol,PicotCodeFragment> fldvals)
{
   List<String> rslt = new ArrayList<>();
   Set<PicotCodeFragment> used = new HashSet<>();
   JcompType ptyp = param.getType();
   
   // first see if there is a relevant field assignment
   for (PicotMethodEffect meff : pmd.getEffects()) {
      switch (meff.getEffectType()) {
         case SET_FIELD :
            PicotEffectItem srcitm = meff.getEffectSource();
            if (srcitm.getSymbolValue() == param) {
               PicotEffectItem flditm = meff.getEffectTarget();
               JcompSymbol fldsym = flditm.getSymbolValue();
               PicotCodeFragment fldpcf = fldvals.get(fldsym);
               if (fldpcf != null) {
                  rslt.add(fldpcf.getCode());
                  used.add(fldpcf);
                }
             }
            break;
         default :
            break;
       }
    }
   
   // next use any values of the same type from fields
   for (Map.Entry<JcompSymbol,PicotCodeFragment> ent : fldvals.entrySet()) {
      JcompSymbol fldsym = ent.getKey();
      JcompType fldtyp = fldsym.getType();
      if (fldtyp == ptyp) {
         PicotCodeFragment fldpcf = fldvals.get(fldsym);
         if (fldpcf != null && ! used.contains(fldpcf)) {
            rslt.add(fldpcf.getCode());
            used.add(fldpcf);
          }
       }
    }
   
   // nest use default value based on parameter type
   if (ptyp.isNumericType()) {
      rslt.add("0");
    }
   else if (ptyp.isBooleanType()) {
      rslt.add("false");
      rslt.add("true");
    }
   else if (ptyp.isStringType()) {
      rslt.add("null");
    }
   else {
      rslt.add("null");
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/
   
private PicotClassData getClassData(JcompType typ)
{
   PicotClassData pcd = class_data.get(typ);
   if (pcd == null) {
      pcd = new PicotClassData(typ,jcomp_typer);
      PicotClassData opcd = class_data.putIfAbsent(typ,pcd);
      if (opcd != null) pcd = opcd;
    }
   
   return pcd;
}
   
   
}       // end of class PicotValueBuilder




/* end of PicotValueBuilder.java */

