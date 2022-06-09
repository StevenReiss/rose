/********************************************************************************/
/*                                                                              */
/*              StemQueryLocationHistory.java                                   */
/*                                                                              */
/*      Handle location history queries                                         */
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



package edu.brown.cs.rose.stem;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bract.BractAstPattern;
import edu.brown.cs.rose.bract.BractConstants;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RoseException;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootControl.AssertionData;

class StemQueryLocationHistory extends StemQueryHistory implements BractConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final BractAstPattern expr_pattern;

static {
   expr_pattern = BractAstPattern.expression("Ex == Ey","Ex != Ey",
         "Ex.equals(Ey)","!Ex.equals(Ey)");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemQueryLocationHistory(StemMain ctrl,RootProblem prob)
{
   super(ctrl,prob);
}

/********************************************************************************/
/*                                                                              */
/*      Process location query                                                  */
/*                                                                              */
/********************************************************************************/

@Override void process(StemMain stem,IvyXmlWriter xw) throws RoseException
{
   String locxml = getExecLocation();
   if (locxml == null) {
      RoseLog.logE("STEM","No location for location query");
      throw new RoseException("Location undefined");
    }
   
   Element hrslt = getLocationData(stem,locxml);
   outputGraph(hrslt,xw); 
}



/********************************************************************************/
/*                                                                              */
/*      Set up and execute query                                                */
/*                                                                              */
/********************************************************************************/

private Element getLocationData(StemMain stem,String locxml)
{
   stem.waitForAnalysis();
   
   CommandArgs args = new CommandArgs("FILE",for_file.getAbsolutePath(),
         "QTYPE","LOCATION",
         "LINE",line_number,
         "METHOD",method_name);
   
   String qxml = locxml;
   String sxml = getXmlForStack();
   if (sxml != null) {
      if (qxml == null) qxml = sxml;
      else qxml += sxml;
    }
   Element rslt = stem.sendFaitMessage("FLOWQUERY",args,qxml);
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Handle special cases where additional information is available          */
/*                                                                              */
/********************************************************************************/

AssertionData getAssertionData()
{
   stem_control.waitForAnalysis();
   
   try {
      ASTNode stmt = getSourceStatement();
      ASTNode from = stmt;
      ASTNode par = stmt.getParent();
      while (par.getNodeType() == ASTNode.BLOCK) {
         from = par;
         par = par.getParent();
       }
      if (par.getNodeType() == ASTNode.IF_STATEMENT) {
         IfStatement ifstmt = (IfStatement) par;
         Expression cond = ifstmt.getExpression();
         PatternMap pmap = new PatternMap();
         if (expr_pattern.match(cond,pmap)) return null;
         Expression ex1 = (Expression) pmap.get("x");
         Expression ex2 = (Expression) pmap.get("y");
         boolean comp = false;
         if (cond.getNodeType() == ASTNode.INFIX_EXPRESSION) {
            InfixExpression ifx = (InfixExpression) cond;
            if (ifx.getOperator() == InfixExpression.Operator.NOT_EQUALS) comp = true;
          }
         else if (cond.getNodeType() == ASTNode.PREFIX_EXPRESSION) comp = true;
         if (ifstmt.getElseStatement() == from) comp = !comp;
         if (comp) return null;
         switch (ex1.getNodeType()) {
            case ASTNode.NUMBER_LITERAL :
            case ASTNode.STRING_LITERAL :
            case ASTNode.NULL_LITERAL :
            case ASTNode.TEXT_BLOCK :
               Expression exx = ex1;
               ex1 = ex2;
               ex2 = exx;
               break;
          }
         BudValue v1 = bud_launch.evaluate(ex1.toString());
         BudValue v2 = bud_launch.evaluate(ex2.toString());
         return new LocationData(ex1,v1.toString(),v2.toString());
       }
    }
   catch (RoseException e) { }
   
   return null;
}




private static class LocationData implements AssertionData {
   
   private Expression use_node;
   private String orig_value;
   private String target_value;
   
   LocationData(Expression nd,String ov,String nv) {
      use_node = nd;
      orig_value = ov;
      target_value = nv;
    }
   
   @Override public ASTNode getExpression()             { return use_node; }
   @Override public String getOriginalValue()           { return orig_value; }
   @Override public String getTargetValue()             { return target_value; }
   @Override public boolean isLocation()                { return false; }
   
}

}       // end of class StemQueryLocationHistory




/* end of StemQueryLocationHistory.java */

