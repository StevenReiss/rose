/********************************************************************************/
/*                                                                              */
/*              BudLaunch.java                                                  */
/*                                                                              */
/*      Handle for a particular launch/thread/frame                             */
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
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootProblem;

public class BudLaunch
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl     rose_control;
private String          launch_id;
private String          thread_id;
private String          frame_id;
private String          project_name;
private Map<String,BudValueData> unique_values;
private Map<String,BudType> type_map;
private BudStack        call_stack;

private static AtomicInteger eval_counter = new AtomicInteger();


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BudLaunch(RootControl ctrl,String lid,String tid,String fid,String proj)
{
   rose_control = ctrl;
   launch_id = lid;
   thread_id = tid;
   frame_id = fid;
   project_name = proj;
   unique_values = new HashMap<>();
   type_map = new HashMap<>();
   call_stack = null;
}


public BudLaunch(RootControl ctrl,RootProblem p)
{
   this(ctrl,p.getLaunchId(),p.getThreadId(),p.getFrameId(),
         p.getBugLocation().getProject());
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getLaunch()               { return launch_id; }

public String getThread()               { return thread_id; }

public String getFrame()                { return frame_id; }

public RootControl getControl()         { return rose_control; }



/********************************************************************************/
/*                                                                              */
/*      Local type access methods                                               */
/*                                                                              */
/********************************************************************************/

BudType findType(String typ)
{
   synchronized (type_map) {
      BudType bt = type_map.get(typ);
      if (bt != null) return bt;
    }
   
   BudType nbt = BudType.createNewType(this,typ);
   
   synchronized (type_map) {
      BudType bt = type_map.putIfAbsent(typ,nbt);
      if (bt != null) return bt;
    }
   
   return nbt;
}


BudType intType()                { return findType("int"); }
BudType stringType()             { return findType("java.lang.String"); }



/********************************************************************************/
/*                                                                              */
/*      Stack methods                                                           */
/*                                                                              */
/********************************************************************************/

public BudStack getStack()
{
   if (call_stack == null) {
      CommandArgs args = new CommandArgs("THREAD",thread_id);
      Element rply = sendBubblesMessage("GETSTACKFRAMES",args,null);
      Element stack = IvyXml.getChild(rply,"STACKFRAMES");
      for (Element telt : IvyXml.children(stack,"THREAD")) {
         String teid = IvyXml.getAttrString(telt,"ID");
         if (teid.equals(thread_id)) {
            call_stack = new BudStack(telt);
            break;
          }
       }
    }
   
   return call_stack;
}



/********************************************************************************/
/*                                                                              */
/*      Parameter computation methods                                           */
/*                                                                              */
/********************************************************************************/

public Map<String,BudValue> getParameterValues()
{
   BudParameterValues pvs = new BudParameterValues(this);
   return pvs.getValues();
}




/********************************************************************************/
/*                                                                              */
/*      Evaluation methods                                                      */
/*                                                                              */
/********************************************************************************/

Element sendBubblesMessage(String cmd,CommandArgs args,String xml)
{
   return rose_control.sendBubblesMessage(cmd,args,xml);
}


BudValueData evaluateExpr(String expr)
{
   String eid = "ROSE_E_" + eval_counter.incrementAndGet();
   // expr = "edu.brown.cs.seede.poppy.PoppyValue.register(" + expr + ")";
   
   CommandArgs args = new CommandArgs("THREAD",thread_id,
	 "FRAME",frame_id,"BREAK",false,"EXPR",expr,"IMPLICIT",true,
         "PROJECT",project_name,
	 "LEVEL",3,"ARRAY",-1,"REPLYID",eid);
   args.put("SAVEID",eid);
   Element xml = rose_control.sendBubblesMessage("EVALUATE",args,null);
   if (IvyXml.isElement(xml,"RESULT")) {
      Element root = rose_control.waitForEvaluation(eid);
      Element v = IvyXml.getChild(root,"EVAL");
      Element v1 = IvyXml.getChild(v,"VALUE");
      String assoc = expr;
      if (args.get("SAVEID") != null) {
	 assoc = "*" + args.get("SAVEID").toString();
       }
      BudValueData svd = new BudValueData(this,v1,assoc);
      svd = getUniqueValue(svd);
      return svd;
    }
   return null;
}


public BudValue evaluate(String expr)
{
   BudValueData bvd = evaluateExpr(expr);
   if (bvd == null) return null;
   
   return bvd.getBudValue();
}


Element evaluateFields(String expr)
{
   CommandArgs args = new CommandArgs("FRAME",getFrame(),"THREAD",getThread(),
         "PROJECT",project_name,
         "DEPTH",1,"ARRAY",-1);
   String var = "<VAR>" + IvyXml.xmlSanitize(expr) + "</VAR>";
   Element xml = rose_control.sendBubblesMessage("VARVAL",args,var);
   if (IvyXml.isElement(xml,"RESULT")) {
      return IvyXml.getChild(xml,"VALUE");
    }
   
   return null;
}


BudValueData evaluateHashCode(String expr)
{
   CommandArgs args = new CommandArgs("FRAME",getFrame(),"THREAD",getThread(),
         "PROJECT",project_name,
         "DEPTH",1,"ARRAY",-1);
   String var = "<VAR>" + IvyXml.xmlSanitize(expr) + "</VAR>";
   Element xml = rose_control.sendBubblesMessage("VARVAL",args,var);
   if (IvyXml.isElement(xml,"RESULT")) {
      return new BudValueData(this,IvyXml.getChild(xml,"VALUE"),null);
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Manage unique values                                                    */
/*                                                                              */
/********************************************************************************/

BudValueData getUniqueValue(BudValueData bvd)
{
   if (bvd == null) return null;
   switch (bvd.getKind()) {
      case OBJECT :
      case ARRAY :
         String dnm = bvd.getValue();
	 if (dnm != null && dnm.length() > 0) {
            synchronized (unique_values) {
               BudValueData nsvd = unique_values.get(dnm);
               if (nsvd != null) bvd = nsvd;
               else unique_values.put(dnm,bvd);
             }
	  }
	 break; 
      default :
         break;
    }
   return bvd;
}



}       // end of class BudLaunch




/* end of BudLaunch.java */

