/********************************************************************************/
/*										*/
/*		BushPanelSimple.java						*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.rose.bush;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaLinkStyle;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunValue;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpStackFrame;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThread;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.BussBubble;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpEvaluationHandler;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RootNodeContext;

class BushPanelSimple implements BushConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpThread	for_thread;
private BumpStackFrame	for_frame;
private Component	base_editor;
private BaleFileOverview bale_file;
private VariablePanel	variable_panel;
private ExpressionPanel  expression_panel;
private DataPanel       exception_panel;
private DataPanel       location_panel;
private DataPanel       other_panel;
private JPanel		content_panel;
private JButton         show_button;
private JButton         suggest_button;
private DataPanel       active_panel;    
private BushUsageMonitor usage_monitor;
private Map<String,Element> expression_data;
private boolean         rose_ready;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BushPanelSimple(BumpThread th,BumpStackFrame frm,Component base,BaleFileOverview doc)
{
   for_thread = th;
   for_frame = frm;
   base_editor = base;
   bale_file = doc;
   content_panel = null;
   active_panel = null;
   usage_monitor = null;
   expression_data = null;
   rose_ready = false;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getMetricId()
{
   return String.valueOf(hashCode());
}



/********************************************************************************/
/*										*/
/*	Activation methods							*/
/*										*/
/********************************************************************************/

BudaBubble createBubble(Component src)
{
   content_panel = createDisplay();

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(base_editor);
   BudaBubble bbl = new PanelBubble(content_panel);
   bba.addBubble(bbl,base_editor,null,BudaConstants.PLACEMENT_LOGICAL);
   bbl.setVisible(true);
   
   return bbl;
}



private JPanel createDisplay()
{
   variable_panel = new VariablePanel();
   variable_panel.setVisible(false);
   expression_panel = new ExpressionPanel();
   expression_panel.setVisible(false);
   exception_panel = new ExceptionPanel();
   exception_panel.setVisible(false);
   location_panel = new LocationPanel();
   location_panel.setVisible(false);
   other_panel = new OtherPanel();
   other_panel.setVisible(false);
   active_panel = location_panel;
   
   SwingGridPanel pnl = new SimplePanel();
   pnl.beginLayout();
   JLabel lbl = pnl.addBannerLabel("ROSE debugging assistant");
   lbl.setIcon(BoardImage.getIcon("rose",getClass()));
   pnl.addSeparator();
   pnl.addDescription("Thread",for_thread.getName());
   pnl.addDescription("Location",for_frame.getLineNumber() + " @ " +
	 for_frame.getMethod());
   pnl.addSeparator();
   List<String> choices = new ArrayList<>();
   BoardLog.logD("BUSH","Choices " + for_thread.getExceptionType() + " " +
         for_frame.getLevel());
   if (for_thread.getExceptionType() != null &&
	 for_thread.getStack().getFrame(0) == for_frame) {
      choices.add("Exception should not be thrown");
      active_panel = exception_panel;
    }
   choices.add("Should not be here");
   choices.add("Variable value incorrect");
   choices.add("Expression value incorrect");
   choices.add("Other ...");
   pnl.addChoice("Problem",choices,0,false,new PanelSelector());
   
   show_button = pnl.addBottomButton("Show History","HISTORY",new ShowHandler());
   show_button.setEnabled(false);
   suggest_button = pnl.addBottomButton("Suggest Repairs","SUGGEST",new SuggestHandler());
   EnableWhenReady ewr = new EnableWhenReady();
   ewr.start();

   pnl.addLabellessRawComponent("VARIABLES",variable_panel);
   pnl.addLabellessRawComponent("EXPRSSIONS",expression_panel);
   pnl.addLabellessRawComponent("OTHER",other_panel);
   
   pnl.addSeparator();
   
   pnl.addBottomButtons();
   
   updateShow();
   
   return pnl;
}


private void updateSize()
{
   if (content_panel == null) return;
   BudaBubble bbl = BudaRoot.findBudaBubble(content_panel);
   Dimension d = bbl.getPreferredSize();
   Dimension d1 = bbl.getSize();
   BoardLog.logD("BUSH","Check size " + d + " " + d1);
   bbl.setSize(d);
}


private void updateShow()
{
   BoardLog.logD("BUSH","Update show " + rose_ready + " " + active_panel);
   if (active_panel != null && rose_ready) {
      active_panel.setVisible(true);
      show_button.setEnabled(active_panel.isReady());
      suggest_button.setEnabled(active_panel.isReady());
    }
   else {
      show_button.setEnabled(false);
      suggest_button.setEnabled(false);
    }
}


private class PanelSelector implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
      String v = (String) cbx.getSelectedItem();
      BoardLog.logD("BUSH","Handle panel choice " + v);
   
      if (active_panel != null) active_panel.setVisible(false);
      active_panel = null;
      if (v == null) v = "";
      
      switch (v) {
         case "Exception should not be thrown" :
            active_panel = exception_panel;
            break;
         case "Should not be here" :
            active_panel = location_panel;
            break;
         case "Variable value incorrect" :
            active_panel = variable_panel;
            break;
         case "Expression value incorrect" :
            active_panel = expression_panel;
            break;
         case "Other ..." :
            active_panel = other_panel;
            break;
         default :
            BoardLog.logE("BUSH","Unknown panel action " + v);
            break;
       }
   
      updateShow();
      updateSize();
    }

}	// end of inner class PanelSelector



/********************************************************************************/
/*                                                                              */
/*      Handle Show action                                                      */
/*                                                                              */
/********************************************************************************/

private class ShowHandler implements ActionListener, Runnable {

   private Element show_result;
   
   ShowHandler() {
      show_result = null;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      BoardLog.logD("BUSH","Handle show");
      BoardThreadPool.start(this);
    }
   
   @Override public void run() {
      if (show_result == null) {
         BoardLog.logD("BUSH","Start processing request");
         int off = bale_file.findLineOffset(for_frame.getLineNumber());
         String mthd = for_frame.getMethod() + for_frame.getSignature();
         CommandArgs args = new CommandArgs("THREAD",for_thread.getId(),
               "FRAME",for_frame.getId(),
               "FILE",for_frame.getFile(),
               "CLASS",for_frame.getFrameClass(),
               "METHOD",mthd,
               "OFFSET",off,
               "PROJECT",for_thread.getLaunch().getConfiguration().getProject(),
               "LINE",for_frame.getLineNumber());
         String body = active_panel.addShowData(args);
         BushFactory bush = BushFactory.getFactory();
         Element rslt = bush.sendRoseMessage("HISTORY",args,body);
         if (IvyXml.isElement(rslt,"RESULT")) {
            BoardLog.logD("BUSH","Handling ROSE result: " + IvyXml.convertXmlToString(rslt));
            show_result = rslt;
            SwingUtilities.invokeLater(this);
          }
         else {
            BoardLog.logD("ROSE","Bad ROSE result: " + rslt);
          }
       }
      else { 
         setupBubbleStack();
       }
    }
   
   private void setupBubbleStack() {
      Map<BumpLocation,String> locs = new HashMap<>();
      List<BumpLocation> loclist = new ArrayList<>();
      List<BushLocation> bloclist = new ArrayList<>();
      Element gelt = IvyXml.getChild(show_result,"GRAPH");
      for (Element nelt : IvyXml.children(gelt,"NODE")) {
         Element locelt = IvyXml.getChild(nelt,"LOCATION");
         if (locelt == null) continue;
         BumpLocation loc = BumpLocation.getLocationFromXml(locelt);
         String reason = IvyXml.getAttrString(nelt,"REASON");
         BoardLog.logD("BUSH","LOC " + IvyXml.convertXmlToString(nelt));
         if (loc != null) {
            if (reason != null) locs.put(loc,reason);
            loclist.add(loc);
            int pri = IvyXml.getAttrInt(nelt,"PRIORITY");
            BushLocation bloc = new BushLocation(loc,pri);
            bloclist.add(bloc);
            BoardLog.logD("BUSH","Line " + bloc.getLineNumber());
          }       
         if (locs.size() > 256) break;
       }
      BoardLog.logD("BUSH","Bubble stack with " + locs.size() + " for " + active_panel.getProblem());
      if (locs.size() == 0) return;
      // if all the locs are in the same method, then create a different display
      
      BushProblem bp = active_panel.getProblem();
      if (bp != null) {
         BushFactory.getFactory().addFixAnnotations(bp,bloclist);
       }
      
      Rectangle ploc = content_panel.getBounds();
      Point pt = new Point(ploc.x + ploc.width/2,ploc.y + ploc.height/2);
      BussBubble buss = BaleFactory.getFactory().createBubbleStackForced(content_panel,null,pt,
            true,loclist,BudaLinkStyle.NONE);
      BoardLog.logD("BUSH","Created buss bubble " + buss);
      usage_monitor = new BushUsageMonitor(buss,getMetricId(),locs);
    }
   
}



private class SuggestHandler implements ActionListener {
   
   SuggestHandler() { }
   
   @Override public void actionPerformed(ActionEvent evt) {
      BushProblem problem = active_panel.getProblem();
      if (problem != null) {
         BushFactory bf = BushFactory.getFactory();
         AbstractAction rsa =  bf.getSuggestAction(problem,null,content_panel);
         rsa.actionPerformed(evt);
       }
    }
   
}       // end of inner class SuggestHandler



/********************************************************************************/
/*										*/
/*	Main panel								*/
/*										*/
/********************************************************************************/

private class SimplePanel extends SwingGridPanel {

   private static final long serialVersionUID = 1;

   SimplePanel() {
      setBackground(BoardColors.getColor("Rose.background.color"));
      setOpaque(true);
    }

}	// end of inner class SimplePanel




/********************************************************************************/
/*                                                                              */
/*      Data panel extension                                                    */
/*                                                                              */
/********************************************************************************/


private abstract static class DataPanel extends SwingGridPanel {

   private static final long serialVersionUID = 1;
   
   abstract boolean isReady();
   
   String addShowData(CommandArgs args) {
      BushProblem bp = getProblem();
      IvyXmlWriter xw = new IvyXmlWriter();
      bp.outputXml(xw);
      String body = xw.toString();
      xw.close();
      
      BushFactory.metrics("SHOW",bp.getProblemType(),bp.getProblemDetail(),
            bp.getOriginalValue(),bp.getTargetValue());
      localMetrics();
    
      return body;
   }
   
   protected void localMetrics()                                   { }
   
   abstract BushProblem getProblem();
   
}


/********************************************************************************/
/*										*/
/*	ValuePanel -- any panel with a value					*/
/*										*/
/********************************************************************************/

private interface ValuePanel {

   void setValue(String base,BumpRunValue value,String error);

}	// end of inner interface ValuePanel



/********************************************************************************/
/*                                                                              */
/*      Dummy panel for choices with no data                                    */
/*                                                                              */
/********************************************************************************/

private class LocationPanel extends DataPanel {

   private static final long serialVersionUID = 1;
   
   @Override public boolean isReady()                           { return true; }
   
   
   @Override public BushProblem getProblem() {
      return new BushProblem(for_frame,RoseProblemType.LOCATION,null,null,null,null);
    }
   
}       // end of inner class LocationPanel



private class ExceptionPanel extends DataPanel {

   private static final long serialVersionUID = 1;
   
   @Override public boolean isReady()                           { return true; }
   
   @Override public BushProblem getProblem() {
      return new BushProblem(for_frame,RoseProblemType.EXCEPTION,for_thread.getExceptionType(),null,null,null);
    }

}       // end of inner class ExceptionPanel



/********************************************************************************/
/*										*/
/*	Variable panel								*/
/*										*/
/********************************************************************************/

private abstract class VarExprPanel extends DataPanel implements ActionListener, ValuePanel {
   
   private SwingComboBox<String> variable_selector;
   private JLabel current_value;
   private SwingComboBox<String> should_be;
   private JTextField other_value;
   private SwingGridPanel other_panel;
   private boolean is_ready;
   
   private static final long serialVersionUID = 1;
   
   VarExprPanel() {
      is_ready = false;
      setBackground(BoardColors.getColor("Rose.background.color"));
      setOpaque(false);
      beginLayout();
      List<String> vars = new ArrayList<>();
      String what = getHeading();
      vars.add(0,"Select a " + what + " ...");
      variable_selector = addChoice(what,vars,0,false,this);
      ElementsFinder finder = new ElementsFinder(this);
      BoardThreadPool.start(finder);
      current_value = addDescription("Current Value",null);
      List<String> shoulds = new ArrayList<>();
      should_be = addChoice("Should Be",shoulds,0,false,this);
      should_be.setVisible(false);
      other_panel = new SwingGridPanel();
      other_panel.setBackground(BoardColors.getColor("Rose.background.color"));
      other_panel.setOpaque(false);
      other_panel.beginLayout();
      other_value = other_panel.addTextField("Other Value","",32,this,null);
      addLabellessRawComponent("OTHER",other_panel);
      other_panel.setVisible(false);
    }
   
   protected abstract List<String> getElements();
   protected abstract String getHeading();
   protected BumpRunValue getValue(String what)                 { return null; }
   
   protected void addElements(List<String> elts) {
      if (elts == null) return;
      for (String elt : elts) variable_selector.addItem(elt);
    }
   
   
   @Override public boolean isReady()                           { return is_ready; }
   
   @Override public void actionPerformed(ActionEvent evt) {
      String what = getHeading();
      BoardLog.logD("BUSH",what + " action " + evt.getActionCommand() + " " + evt);
      
      switch (evt.getActionCommand()) {
         case "Expression" :
         case "Variable" :
            setReady(false);
            String var = (String) variable_selector.getSelectedItem();
            BoardLog.logD("BUSH","Check variable " + var + " @ " + variable_selector.getSelectedIndex());
            BoardLog.logD("BUSH","Selections: " + variable_selector.getModel().getSize());
            BoardLog.logD("BUSH",what + " " + var + " selected");
            current_value.setText("");
            if (var != null && !var.startsWith("Select ")) {
               BumpRunValue rval = getValue(var);
               if (rval == null) {
                  var = var.replace("?",".");
                  for_frame.evaluate(var,new EvalHandler(this));
                }
               else setValue(var,rval,null);
             }
            break;
         case "Should Be" :
            String s = (String) should_be.getSelectedItem();
            BoardLog.logD("BUSH","Should be " + s + " " + should_be.getSelectedIndex());
            if (s != null && s.startsWith("Other")) {
               other_panel.setVisible(true);
               BoardLog.logD("BUSH","Other panel should be visible");
               invalidate();
             }
            else {
               BoardLog.logD("BUSH","Set other invisible");
               other_panel.setVisible(false);
             }
            updateSize();
            break;
         case "Other Value" :
            other_value.getText();
            break;
         case "comboBoxEdited" :
            break;
         default :
            BoardLog.logE("BUSH","Unknown " + what + " action " + evt.getActionCommand());
            break;
       }
    }
   
   @Override public void setValue(String expr,BumpRunValue value,String err) {
      BoardLog.logD("BUSH","Set value " + expr + " " + value + " " + err);
      if (err != null) {
         current_value.setForeground(BoardColors.getColor("Rose.value.error.color"));
         current_value.setText("???");
         should_be.setVisible(false);
         other_panel.setVisible(false);
       }
      else {
         current_value.setForeground(BoardColors.getColor("Rose.value.color"));
         String val = value.getType() + " " + value.getValue();
         if (value.getType().equals("null")) val = "null";
         current_value.setText(val);
         setupShouldBe(value);
       }
    }
   
   
   
   
   protected String getCurrentItem() {
      return (String) variable_selector.getSelectedItem();
    }
   
   protected String getCurrentValue() {
      return current_value.getText();
    }
   
   protected String getShouldBeValue() {
      String shd = (String) should_be.getSelectedItem();
      if (shd.startsWith("Other")) {
         shd = other_value.getText();
       }
      return shd;
    }
   
   private void setReady(boolean fg) {
      is_ready = fg;
      updateShow();
    }
   
   private void setupShouldBe(BumpRunValue value) {
      List<String> alternatives = findAlternatives(value);
      other_panel.setVisible(false);
      if (alternatives == null || alternatives.isEmpty()) {
         BoardLog.logD("BUSH","Should be contents: NONE");
         should_be.setVisible(false);
       }
      else {
         BoardLog.logD("BUSH","Should be contents: " + alternatives.size());
         should_be.setContents(alternatives);
         should_be.setSelectedIndex(0);
         should_be.setVisible(true);
       }
      setReady(true);
    }
   
   @Override protected void localMetrics() {
      String shd = (String) should_be.getSelectedItem();
      if (shd.startsWith("Other")) {
         shd = other_value.getText();
         BushFactory.metrics("OTHERVALUE",getCurrentItem(),getCurrentValue(),shd);
       }
    }
}       // end of inner class VarExprPanel




private class VariablePanel extends VarExprPanel {

   private static final long serialVersionUID = 1;

   VariablePanel() { }

   protected String getHeading()                        { return "Variable"; }
   
   protected List<String> getElements() {
      List<String> vars = findVariables();
      Collections.sort(vars);
      return vars;
    }
   
   protected BumpRunValue getValue(String elt) {
      return getVariableValue(elt,null,null);
    }
   
   
   
   @Override public BushProblem getProblem() {
      return new BushProblem(for_frame,RoseProblemType.VARIABLE,
            getCurrentItem(),getCurrentValue(),getShouldBeValue(),null);
    }
   
   private BumpRunValue getVariableValue(String s,BumpRunValue brv,String pfx) {
      String var = s;
      String sfx = null;
      int idx = s.indexOf("?");
      if (idx > 0) {
         var = s.substring(0,idx);
         sfx = s.substring(idx+1);
       }
   
      if (brv != null) {
         BoardLog.logD("BUSH","Inner variables");
         for (String s1 : brv.getVariables()) {
            BoardLog.logD("BUSH","\t VAR: " + s1);
          }
       }
      BumpRunValue base = null;
      if (pfx != null) var = pfx + "?" +var;
      if (brv == null) base = for_frame.getValue(var);
      else base = brv.getValue(var);
   
      BoardLog.logD("BUSH","GET VALUE " + var + " " + pfx + " = " + base + " " + sfx);
   
      if (base == null) return null;
      if (sfx == null) return base;
      return getVariableValue(sfx,base,var);
    }

}	// end of inner class VariablePanel



private class ExpressionPanel extends VarExprPanel {
   
   private static final long serialVersionUID = 1;
   
   ExpressionPanel() { }
   
   protected String getHeading()                        { return "Expression"; }
   
   protected List<String> getElements() {
      return findExpressions();
    }
   
   
   
   @Override public BushProblem getProblem() {
      Element expelt = expression_data.get(getCurrentItem());
      RootNodeContext ctx = null;
      if (expelt != null) ctx = new RootNodeContext(expelt);
      return new BushProblem(for_frame,RoseProblemType.EXPRESSION,
            getCurrentItem(),getCurrentValue(),getShouldBeValue(),ctx);
    }
   
}       // end of inner class ExpressionPanel


private class ElementsFinder implements Runnable {

   VarExprPanel for_panel;
   
   ElementsFinder(VarExprPanel pnl) {
      for_panel = pnl;
    }
   
   @Override public void run() {
      List<String> exps = for_panel.getElements();
      for_panel.addElements(exps);
    }
   
}       // end of inner class ElementsFinder




/********************************************************************************/
/*										*/
/*	Find alternative values for a value					*/
/*										*/
/********************************************************************************/

private List<String> findAlternatives(BumpRunValue value)
{
   List<String> rslt = null;
   switch (value.getKind()) {
      case PRIMITIVE :
	 String typ = value.getType();
	 switch (typ) {
	    case "int" :
	    case "short" :
	    case "byte" :
	    case "long" :
	    case "char" :
	       rslt = findIntegerAlternatives(value);
	       break;
	    case "float" :
	    case "double" :
	       rslt = findFloatAlternatives(value);
	       break;
	    case "boolean" :
	       rslt = findBooleanAlternatives(value);
	       break;
	    case "void" :
	       break;
	  }
	 break;
      case STRING :
	 rslt = findStringAlterantives(value);
	 break;
      case CLASS :
      case OBJECT :
	 rslt = findObjectAlternatives(value);
	 break;
      case ARRAY :
	 break;
      case UNKNOWN :
	 break;
    }

   return rslt;
}



private List<String> findIntegerAlternatives(BumpRunValue rv)
{
   long ival = 0;
   try {
      ival = Long.parseLong(rv.getValue());
    }
   catch (NumberFormatException e) {
      return null;
    }

   List<String> rslt = new ArrayList<>();
   rslt.add(Long.toString(ival+1));
   rslt.add(Long.toString(ival-1));
   rslt.add("> " + ival);
   rslt.add("< " + ival);
   if (ival != 0 && Math.abs(ival) != 1) rslt.add("0");
   rslt.add("Other ...");

   return rslt;
}


private List<String> findFloatAlternatives(BumpRunValue rv)
{
   double ival = 0;
   try {
      ival = Double.parseDouble(rv.getValue());
    }
   catch (NumberFormatException e) {
      return null;
    }

   List<String> rslt = new ArrayList<>();
   rslt.add("> " + ival);
   rslt.add("< " + ival);
   if (ival != 0) rslt.add("0");
   rslt.add("Other ...");

   return rslt;
}


private List<String> findBooleanAlternatives(BumpRunValue rv)
{
   boolean bval = Boolean.parseBoolean(rv.getValue());

   List<String> rslt = new ArrayList<>();
   rslt.add(Boolean.toString(!bval));
   return rslt;
}


private List<String> findStringAlterantives(BumpRunValue rv)
{
   List<String> rslt = new ArrayList<>();
   rslt.add("null");
   rslt.add("Other ...");
   return rslt;
}


private List<String> findObjectAlternatives(BumpRunValue rv)
{
   List<String> rslt = new ArrayList<>();
   if (rv.getValue().equals("null")) {
      rslt.add("Non-null");
    }
   else {
      rslt.add("null");
    }
   rslt.add("Other ...");
   return rslt;
}



private List<String> findVariables()
{
   List<String> rslt = new ArrayList<>();
   for (String s : for_frame.getVariables()) {
      rslt.add(s);
    }
   for (String s : for_frame.getVariables()) {
      BumpRunValue rv = for_frame.getValue(s);
      switch (rv.getKind()) {
	 case CLASS :
	 case PRIMITIVE :
	 case STRING :
	 case UNKNOWN :
	    break;
	 case OBJECT :
	    if (s.equals("this")) {
	       for (String fld : rv.getVariables()) {
                  String disp = fld.replace("?",".");
		  rslt.add(disp);
		}
	     }
	    break;
	 case ARRAY :
	    break;
       }
    }

   BoardLog.logD("BUSH","VARIABLE LIST " + rslt.size());

   return rslt;
}


private List<String> findExpressions()
{
   int off = bale_file.findLineOffset(for_frame.getLineNumber());
   CommandArgs args = new CommandArgs("THREAD",for_thread.getId(),
         "FRAME",for_frame.getId(),
         "FILE",for_frame.getFile(),
         "CLASS",for_frame.getFrameClass(),
         "METHOD",for_frame.getMethod(),
         "PROJECT",for_thread.getLaunch().getConfiguration().getProject(),
         "OFFSET",off,
         "LINE",for_frame.getLineNumber());
   BushFactory bush = BushFactory.getFactory();
   Element rslt = bush.sendRoseMessage("EXPRESSIONS",args,null); 
   
   List<String> exps = new ArrayList<>();
   expression_data = new HashMap<>();
   for (Element e : IvyXml.children(rslt,"EXPR")) {
      String exp = IvyXml.getTextElement(e,"TEXT");
      if (exp == null) continue;
      exp = exp.trim();
      if (exp.length() == 0) continue;
      exps.add(exp);
      expression_data.put(exp,e);
    }
   
   return exps;
   
}




/********************************************************************************/
/*										*/
/*	Handle evaluation results asynchronously				*/
/*										*/
/********************************************************************************/

private class EvalHandler implements BumpEvaluationHandler {

   private ValuePanel	value_panel;

   EvalHandler(ValuePanel pnl) {
      value_panel = pnl;
    }

   @Override public void evaluationResult(String eid,String expr,BumpRunValue val) {
      value_panel.setValue(expr,val,null);
    }

   @Override public void evaluationError(String eid,String expr,String err) {
      value_panel.setValue(expr,null,err);
    }

}	// end of inner class EvalHandler




/********************************************************************************/
/*                                                                              */
/*      Other panel                                                             */
/*                                                                              */
/********************************************************************************/

private class OtherPanel extends DataPanel {
   
   private JTextArea    other_description;
   
   private static final long serialVersionUID = 1;
   
   OtherPanel() {
      setBackground(BoardColors.getColor("Rose.background.color"));
      setOpaque(false);
      beginLayout();
      other_description = addTextArea("Describe Problem",null,32,4,null);
    }
   
   @Override public boolean isReady() {
      String cnts = other_description.getText();
      if (cnts == null || cnts.length() == 0) return false;
      return true;
    }
   
   
   
   @Override public BushProblem getProblem() {
      return new BushProblem(for_frame,RoseProblemType.OTHER,other_description.getText(),
            null,null,null);
    }
}



/********************************************************************************/
/*										*/
/*	PanelBubble -- bubble for this panel					*/
/*										*/
/********************************************************************************/

private class PanelBubble extends BudaBubble {

   private static final long serialVersionUID = 1;

   PanelBubble(Component cnts) {
      setContentPane(cnts);
    }
   
   @Override protected void localDispose() {
      BushFactory.metrics("END",getMetricId());
      BushUsageMonitor.remove(usage_monitor);
      usage_monitor = null;
    }

}	// end of inner class PanelBubble



private class EnableWhenReady extends Thread {
   
   private boolean done_setup;
   
   EnableWhenReady() {
      super("EnableRoseWhenReady");
      done_setup = false;
    }
   
   @Override public void run() {
      if (!done_setup) {
         BushFactory bf = BushFactory.getFactory();
         boolean fg = bf.waitForRoseReady();
         if (fg) {
            rose_ready = true;
            done_setup = true;
            SwingUtilities.invokeLater(this);
          }
       }
      else {
         updateShow();
       }
    }
   
}       // end of inner class EnableWhenReady



}	// end of class BushPanelSimple




/* end of BushPanelSimple.java */

