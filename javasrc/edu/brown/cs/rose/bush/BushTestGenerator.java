/********************************************************************************/
/*                                                                              */
/*              BushTestGenerator.java                                          */
/*                                                                              */
/*      Handle generating test cases                                            */
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



package edu.brown.cs.rose.bush;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpSymbolType;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BushTestGenerator implements BushConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BushProblem     for_problem;
private String          test_id;
private String          metric_id;
private TestOutputPanel output_panel;

private static final String NEW_FILE_LABEL = "New Test Class ...";
private static AtomicInteger id_counter = new AtomicInteger(1);
private static Map<String,BushTestGenerator> action_map = new ConcurrentHashMap<>();


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BushTestGenerator(BushProblem bp,Component c,String mid)
{
   for_problem = bp;
   metric_id = mid;
   test_id = "ROSETEST_" + IvyExecQuery.getProcessId() + "_" + id_counter.incrementAndGet();
   
   output_panel = new TestOutputPanel();
   TestOutputBubble bbl = new TestOutputBubble(output_panel);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
   bba.addBubble(bbl,c,null,
         BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_RIGHT);
   bbl.setVisible(true);
}


/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

void generateTestCase()
{
   CommandArgs args = new CommandArgs("REPLYID",test_id);
   action_map.put(test_id,this);
   
   IvyXmlWriter xw = new IvyXmlWriter();
   for_problem.outputXml(xw);
   String body = xw.toString();
   xw.close();
   
   BushFactory bf = BushFactory.getFactory();
   Element rply = bf.sendRoseMessage("CREATETEST",args,body);
   if (IvyXml.isElement(rply,"RESULT")) {
      String name = IvyXml.getAttrString(rply,"NAME");
      if (!name.equals(test_id)) {
         action_map.remove(test_id);
         test_id = name;
         action_map.put(name,this);
       }
    }
   else {
      finished();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle feedback                                                         */
/*                                                                              */
/********************************************************************************/

static void handleTestGenerated(Element testdata)
{ 
   String rid = IvyXml.getAttrString(testdata,"NAME");
   BushTestGenerator gen = action_map.remove(rid);
   if (gen != null) gen.handleTest(testdata);
}



private void handleTest(Element testdata)
{ 
   output_panel.setTestResult(testdata);
}



private void finished()
{ 
   action_map.remove(test_id);
}



/********************************************************************************/
/*                                                                              */
/*      Insert test into test class                                             */
/*                                                                              */
/********************************************************************************/

private void insertTest(String testclass,Element testcase)
{ 
   BushFactory.metrics("GENERATE_TEST",metric_id,for_problem.getDescription());
   CommandArgs args = new CommandArgs("CLASS",testclass,"NAME",test_id);
   String cnts = IvyXml.convertXmlToString(testcase);
   BushFactory bf = BushFactory.getFactory();
   bf.sendRoseMessage("INSERTTEST",args,cnts);
}



/********************************************************************************/
/*                                                                              */
/*      Test Output Panel                                                       */
/*                                                                              */
/********************************************************************************/

private class TestOutputPanel extends SwingGridPanel implements ActionListener {
    
   private transient Element test_data;
   private TestResultEditor test_result;
   private Component result_area;
   private JLabel    working_label;
   private SwingComboBox<String> file_field;
   private JTextField new_file_field;
   private JButton accept_button;
   
   private static final long serialVersionUID = 1;
   
   TestOutputPanel() {
      test_data = null;
      layoutPanel();
    }
   
   void setTestResult(Element testdata) {
      test_data = testdata;
      updatePanel();
    }
   
   private void layoutPanel() {
      setBackground(BoardColors.getColor("Rose.background.color"));
      beginLayout();
      addBannerLabel("Generate Test for " + for_problem.getDescription());
      addSeparator();
      working_label = new JLabel();
      addLabellessRawComponent("WORKING",working_label);
      test_result = new TestResultEditor();
      result_area = addLabellessRawComponent("RESULT",new JScrollPane(test_result));
      List<String> files = findTestFiles();
      file_field = addChoice("Insert into",files,files.get(0),this);
      new_file_field = addTextField("New Test Class",null,32,this,null);
      accept_button = addBottomButton("ACCEPT","ACCEPT",this);
      addBottomButtons();
      updatePanel();
    }
   
   private List<String> findTestFiles() {
      List<String> clss = getTestClasses();
      List<String> rslt = new ArrayList<>();
      String pkg = getPackage();
      if (pkg != null) {
         for (String s : clss) {
            if (s.startsWith(pkg + ".")) {
               s = s.substring(pkg.length()+1);
             }
            rslt.add(s);
          }
       }
      rslt.add(NEW_FILE_LABEL);
      return rslt;
    }
   
   
   private String getDefaultTestFile() {
      Set<String> clsset = new HashSet<>(getTestClasses());
      String pkg = getPackage();
      Element testcase = IvyXml.getChild(test_data,"TESTCASE");
      String cls = IvyXml.getAttrString(testcase,"CLASS");
      String cnm = null;
      if (pkg != null) {
         cnm = pkg;
         int idx = cnm.lastIndexOf(".");
         if (idx > 0) cnm = cnm.substring(idx+1);
         cnm = cnm.substring(0,1).toUpperCase() + cnm.substring(1);
         cnm += "Test";
         if (clsset.contains(cnm)) cnm = null;
       } 
      if (cls != null && cnm == null) {
         cnm = cls;
         int idx = cnm.lastIndexOf(".");
         if (idx > 0) cnm = cnm.substring(idx+1);
         cnm += "Test";
         if (clsset.contains(cnm)) cnm = null;
       }
      return cnm;
    }
 
   private List<String> getTestClasses() {
      List<String> rslt = new ArrayList<>();
      String pkg = getPackage();
      if (pkg != null) {
         List<BumpLocation> clocs = BumpClient.getBump().findAllClasses(pkg + ".*");
         for (BumpLocation clsloc : clocs) {
            if (clsloc.getSymbolType() != BumpSymbolType.CLASS) continue;
            String nm = clsloc.getSymbolName();
            int idx = nm.lastIndexOf(".");
            if (idx >= 0 && nm.substring(0,idx).equals(pkg)) {
               boolean use = false;
               try {
                  String cnts = IvyFile.loadFile(clsloc.getFile());
                  if (cnts.contains("org.junit.Test")) use = true;
                  if (cnts.contains("org.junit.") && cnts.contains("@Test")) use = true;
                }
               catch (IOException e) { }
               if (use) rslt.add(nm);
             }
          }
       }
      return rslt;
    }
   
   private void updatePanel() {
      if (test_data == null) {
         hideAll("Working on Test Case Generation","working");
       }
      else switch (IvyXml.getAttrString(test_data,"STATUS","FAIL")) {
         case "NOTEST" :
            hideAll("Nothing to Generate","fail");
            break;
         case "FAIL" :
         case "NO_DUP" :
            hideAll("Test Case Generation Failed","fail");
            break;
         case "SUCCESS" :
            if (!result_area.isVisible()) setupTest("done");
            else updateTest();
            break;
       }
    }
   
   private void hideAll(String lbl,String c) {
      working_label.setText(lbl);
      setLabelColor(c);
      hideField(result_area);
      hideField(file_field);
      hideField(new_file_field);
      accept_button.setVisible(false);
    }
   
   private void hideField(Component c) {
      JLabel lbl = getJLabelForComponent(c);
      if (lbl != null) lbl.setVisible(false);
      c.setVisible(false);
    }
   
   private void showField(Component c) {
      JLabel lbl = getJLabelForComponent(c);
      if (lbl != null) lbl.setVisible(true);
      c.setVisible(true);
    }
   
   void setupTest(String c) {
      setLabelColor(c);
      working_label.setText("Generated Test Case");
      Element testcase = IvyXml.getChild(test_data,"TESTCASE");
      String testcode = IvyXml.getTextElement(testcase,"TESTCODE");
      test_result.setText(testcode);
      showField(result_area);
      List<String> files = findTestFiles();
      file_field.setContents(files);
      showField(file_field);
      new_file_field.setText(getDefaultTestFile());
      hideField(new_file_field);
      accept_button.setVisible(true);
      accept_button.setEnabled(false);
    }
   
   void setLabelColor(String c) {
      if (c == null) return;
      String cnm = "Rose.test." + c + ".color";
      working_label.setForeground(BoardColors.getColor(cnm));
    }
   
   void updateTest() {
      String fil = (String) file_field.getSelectedItem();
      boolean enable = false;
      if (fil.equals(NEW_FILE_LABEL)) {
         showField(new_file_field);
         if (new_file_field.getText().trim().length() == 0) enable = true;
       }
      else enable = true;
      accept_button.setEnabled(enable);
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      if (evt.getSource() == accept_button) {
         String cls = (String) file_field.getSelectedItem();
         if (cls.equals(NEW_FILE_LABEL)) cls = new_file_field.getText().trim();
         if (cls == null || cls.length() == 0) return;
         if (!cls.contains(".")) {
            String pkg = getPackage();
            if (pkg != null) cls = pkg + "." + cls;
          }
         Element testcase = IvyXml.getChild(test_data,"TESTCASE");
         insertTest(cls,testcase);
       }
      else updatePanel();
    }
   
   private String getPackage() {
      Element testcase = IvyXml.getChild(test_data,"TESTCASE");
      String pkg = IvyXml.getAttrString(testcase,"PACKAGE");
      return pkg;
    }

}       // end of inner class TestOutputPanel



private class TestResultEditor extends JTextPane {
   
   private static final long serialVersionUID = 1;
   
   TestResultEditor() {
      super();
      setEditable(false);
    }
   
   @Override public boolean getScrollableTracksViewportWidth()          { return true; }
   @Override public Dimension getPreferredScrollableViewportSize() {
      return new Dimension(60,10);
    }
   
}       // end of inner class TestResultEditor



private class TestOutputBubble extends BudaBubble {
   
   private static final long serialVersionUID = 1;
   
   TestOutputBubble(TestOutputPanel pnl) {
      setContentPane(pnl);
    }
   
   @Override public void handlePopupMenu(MouseEvent e) {
      JPopupMenu menu = new JPopupMenu();
      menu.add(getFloatBubbleAction());
    }
   
}       // end of inner class TestOutputBubble

}       // end of class BushTestGenerator




/* end of BushTestGenerator.java */

