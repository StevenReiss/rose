/********************************************************************************/
/*                                                                              */
/*              ValidateTrace.java                                              */
/*                                                                              */
/*      Representation of an execution trace                                    */
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



package edu.brown.cs.rose.validate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudLocalVariable;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;

public class ValidateTrace
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         seede_result;
private long            problem_time;
private Element         problem_context;
private Map<Integer,Element> id_map;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateTrace(Element rslt)
{
   seede_result = IvyXml.getChild(rslt,"CONTENTS");
   problem_time = -1;
   problem_context = null;
   setupIdMap();
}



/********************************************************************************/
/*                                                                              */
/*      Find the point in the execution corresponding to the launch             */
/*                                                                              */
/********************************************************************************/

void setupForLaunch(BudLaunch launch)
{
   if (problem_time >= 0 || seede_result == null) return;
   
   Stack<String> stack = new Stack<>();
   for (Element runner : IvyXml.children(seede_result,"RUNNER")) {
      String tid = IvyXml.getAttrString(runner,"THREAD");
      if (!tid.equals(launch.getThread())) continue;
      findProblemTime(IvyXml.getChild(runner,"CONTEXT"),launch,stack);
    }
}



private void findProblemTime(Element ctx,BudLaunch launch,Stack<String> stack)
{
   String mthd = IvyXml.getAttrString(ctx,"METHOD");
   stack.push(mthd);
   
   if (checkStack(launch,stack)) {
      findContextTime(ctx,launch);
    }
   else {
      for (Element subctx : IvyXml.children(ctx,"CONTEXT")) {
         findProblemTime(subctx,launch,stack);
       }
    }
   
   stack.pop();
}



private boolean checkStack(BudLaunch launch,Stack<String> stack)
{
   BudStack stk = launch.getStack();
   String base = stack.get(0);
   List<BudStackFrame> frms = stk.getFrames();
   for (int i = frms.size()-1; i >= 0; --i) {
      BudStackFrame frame = frms.get(i);
      String sgn = frame.getFormatSignature();
      String id = frame.getClassName() + "." + frame.getMethodName() + sgn;
      if (id.equals(base)) {
         return checkStack(launch,stack,i);
       }
    }
   return false;
}


private boolean checkStack(BudLaunch launch,Stack<String> stack,int start)
{
   BudStack stk = launch.getStack();
   List<BudStackFrame> frms = stk.getFrames();
   if (start != stack.size() - 1) return false;
   for (int i = start; i >= 0; --i) {
      BudStackFrame frm = frms.get(i);
      String id = frm.getClassName() + "." + frm.getMethodName() + 
            frm.getFormatSignature();
      if (!id.equals(stack.get(start-i))) return false;
    }
   return true;
}



private void findContextTime(Element ctx,BudLaunch launch)
{
   Element linevar = null;
   for (Element var : IvyXml.children(ctx,"VARIABLE")) {
      String varname = IvyXml.getAttrString(var,"NAME");
      if (varname.equals("*LINE*")) {
         linevar = var;
         break;
       }
    }
   if (linevar == null) return;
   
   BudStackFrame frame = launch.getStack().getFrames().get(0);
   String lno = Integer.toString(frame.getLineNumber());
   long atline = -1;
   for (Element val : IvyXml.children(linevar,"VALUE")) {
      long time = IvyXml.getAttrLong(val,"TIME");
      if (time == 0) time = IvyXml.getAttrLong(ctx,"START"); 
      if (atline > 0) {
         findContextTime(ctx,launch,atline,time-1);
         atline = -1;
       }
      if (lno.equals(IvyXml.getText(val))) {
         atline = time;
       }
    }
   if (atline > 0) {
      findContextTime(ctx,launch,atline,IvyXml.getAttrLong(ctx,"END"));
    }
}



private void findContextTime(Element ctx,BudLaunch launch,long from,long to)
{
   // check local variables in the context vs those of the launch
   BudStackFrame frame = launch.getStack().getFrames().get(0);
   for (String var : frame.getLocals()) {
      BudLocalVariable local = frame.getLocal(var);
      for (Element varelt : IvyXml.children(ctx,"VARIABLE")) {
         String varnam = IvyXml.getAttrString(varelt,"NAME");
         if (varnam.equals(var)) {
            long prev = -1;
            Element prevval = null;
            boolean found = false;
            int foundct = 0;
            for (Element valelt : IvyXml.children(varelt,"VALUE")) {
               long time = IvyXml.getAttrLong(valelt,"TIME");
               if (prev > 0) {
                  if (time >= from && prev <= to) {
                     Boolean fg = compareVariable(local,prevval,launch);
                     if (fg != null) {
                        ++foundct;
                        found |= fg;
                      }
                   }
                }
               prev = time;
               prevval = valelt;
             }
            if (prev > 0) {
               long time = IvyXml.getAttrLong(ctx,"END");
               if (time >= from && prev <= to) {
                  Boolean fg = compareVariable(local,prevval,launch);
                  if (fg != null) {
                     ++foundct;
                     found |= fg;
                   }
                }
             }
            else if (prev == -1) {
               Boolean fg = compareVariable(local,prevval,launch);
               if (fg != null) {
                  ++foundct;
                  found |= fg;
                }
             }
            if (foundct > 0 && !found) return;
          }
       }
    }
   
   
   if (problem_time > 0 && problem_context !=  null) {
      // see if this context is better than saved context
    }
   
   problem_time = from;
   problem_context = ctx;
}



/********************************************************************************/
/*                                                                              */
/*      Compare variables in execution with those in launch                     */
/*                                                                              */
/********************************************************************************/

private Boolean compareVariable(BudLocalVariable local,Element valelt,BudLaunch launch)
{
   switch (local.getKind()) {
      case "PRIMITIVE" :
      case "STRING" :
         String valtxt = IvyXml.getText(valelt);
         return local.getValue().equals(valtxt);
      case "ARRAY" :
         valelt = dereference(valelt);
         // use launch to get value of variable -> BudValue
         // then compare that with the local indexed values and length
         break;
      case "OBJECT" :
         if (local.getType().equals("null")) {
            if (IvyXml.getAttrBool(valelt,"NULL")) return true;
            return false;
          }
         valelt = dereference(valelt);
         // use launch to get value of variable -> BudValue
         // then compare that with the local fields
         break;
      default :
         break;
    }
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Setup mapping for ID matching                                           */
/*                                                                              */
/********************************************************************************/

private void setupIdMap()
{
   id_map = new HashMap<>();
   for (Element valelt : IvyXml.elementsByTag(seede_result,"VALUE")) {
      int id = IvyXml.getAttrInt(valelt,"ID");
      if (id < 0) continue;
      if (IvyXml.getAttrBool(valelt,"REF")) continue;
      Element use = id_map.get(id);
      if (use != null) continue;
      id_map.put(id,valelt);
    }
}


private Element dereference(Element val)
{
   if (IvyXml.getAttrBool(val,"REF")) {
      int id = IvyXml.getAttrInt(val,"ID");
      return id_map.get(id);
    }
   
   return val;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return IvyXml.convertXmlToString(seede_result);
}


}       // end of class ValidateTrace




/* end of ValidateTrace.java */

