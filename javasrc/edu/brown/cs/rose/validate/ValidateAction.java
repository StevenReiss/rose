/********************************************************************************/
/*                                                                              */
/*              ValidateAction.java                                             */
/*                                                                              */
/*      Action for setting up proper execution context                          */
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


import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootControl;

abstract class ValidateAction implements ValidateConstants
{



/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

static ValidateAction createSetAction(String nm,BudValue bv)
{
   return new SetAction(nm,bv);
}



static ValidateAction createInitAction(String expr)
{
   return new InitAction(expr);
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected ValidateAction()
{ }




/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

abstract void perform(RootControl ctrl,String session,String tid,boolean first);


private static void sendInitialization(String init,RootControl rc,String session,String tid,boolean first) 
{
   CommandArgs args = new CommandArgs("THREAD",tid,"REMOVE",first);
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.cdataElement("EXPRESSION",init);
   String cnts = xw.toString();
   xw.close();
   rc.sendSeedeMessage(session,"INITIALIZATION",args,cnts);
}



/********************************************************************************/
/*                                                                              */
/*      Set a variable to a value                                               */
/*                                                                              */
/********************************************************************************/

private static class SetAction extends ValidateAction {
  
   private String var_name;
   private BudValue set_value;
   
   SetAction(String nm,BudValue v) {
      var_name = nm;
      set_value = v;
    }
   
   @Override public String toString() {
      return var_name + "=" + set_value;
    }
   
   @Override void perform(RootControl rc,String session,String tid,boolean first) {
      String expr = var_name + " = " + set_value.getJavaValue();
      sendInitialization(expr,rc,session,tid,first);
//    CommandArgs args = new CommandArgs("VAR",var_name);
//    IvyXmlWriter xw = new IvyXmlWriter();
//    set_value.outputXml(xw);
//    String cnts = xw.toString();
//    xw.close();
//    rc.sendSeedeMessage(session,"SETVALUE",args,cnts);
    }
   
}       // end of inner class SetAction



/********************************************************************************/
/*                                                                              */
/*      Initialization action                                                   */
/*                                                                              */
/********************************************************************************/

private static class InitAction extends ValidateAction {
   
   private String init_expression;
   
   InitAction(String expr) {
      init_expression = expr;
    }
   
   @Override public String toString() {
      return init_expression;
    }
   
   @Override void perform(RootControl rc,String session,String tid,boolean first) {
      sendInitialization(init_expression,rc,session,tid,first);
    }
   
}       // end of inner class InitAction




}       // end of class ValidateAction




/* end of ValidateAction.java */

