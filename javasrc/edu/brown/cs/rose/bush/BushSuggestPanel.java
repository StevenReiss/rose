/********************************************************************************/
/*										*/
/*		BushSuggestPanel.java						*/
/*										*/
/*     Panel and bubble to show repair suggestions				*/
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.board.BoardAttributes;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingLineScrollPane;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.bush.BushConstants.BushRepairAdder;
import edu.brown.cs.rose.root.RootEdit;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootRepair;

class BushSuggestPanel implements BushConstants, BushRepairAdder
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BushProblem	for_problem;
private BushLocation	for_location;
private Component	from_panel;
private BudaBubbleArea  bubble_area;
private JPanel		content_pane;
private SuggestList	suggestion_list;
private SuggestListModel list_model;
private JLabel		suggestions_pending;
private String		metric_id;
private JLabel		done_label;

private static boolean do_preview = true;

private static final double THRESHOLD = 0.1;
private final short MARGIN_WIDTH_PX = 35;

private static final String PENDING = "Finding suggestions ...";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BushSuggestPanel(Component src,BushProblem prob,BushLocation loc,String mid)
{
   from_panel = src;
   for_problem = prob;
   for_location = loc;
   content_pane = null;
   metric_id = mid;
   bubble_area = BudaRoot.findBudaBubbleArea(from_panel);
}



/********************************************************************************/
/*										*/
/*	Create the actual bubble						*/
/*										*/
/********************************************************************************/

BudaBubble createBubble()
{
   content_pane = createDisplay();

   BudaBubble bbl = new PanelBubble(content_pane);
   bubble_area.addBubble(bbl,from_panel,null,
	 BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_BELOW|BudaConstants.PLACEMENT_GROUPED);
   bbl.setVisible(true);

   return bbl;
}


private JPanel createDisplay()
{
   SwingGridPanel pnl = new SuggestPanel();
   pnl.beginLayout();
   done_label = pnl.addBannerLabel("Finding Repairs for " + for_problem.getDescription());
   done_label.setFont(SwingText.deriveLarger(done_label.getFont()));

   RootLocation loc = for_problem.getBugLocation();
   if (loc != null) {
      pnl.addBannerLabel("   At " + loc.getLineNumber() + " in " +
	    loc.getMethod());
    }
   if (for_location != null) {
      pnl.addBannerLabel("Fix " + for_location.getLineNumber() + " in " +
	    for_location.getMethod());
    }
   pnl.addSeparator();

   suggestions_pending = new JLabel(PENDING);
   pnl.addLabellessRawComponent("PENDING",suggestions_pending);
   list_model = new SuggestListModel();
   suggestion_list = new SuggestList(list_model);
   pnl.addLabellessRawComponent("REPAIRS",new JScrollPane(suggestion_list));
   suggestion_list.setVisible(false);

   return pnl;
}



/********************************************************************************/
/*										*/
/*	Handle repairs								*/
/*										*/
/********************************************************************************/

@Override public synchronized void addRepair(BushRepair repair)
{
   RepairAdder ra = new RepairAdder(repair);
   try {
      SwingUtilities.invokeLater(ra);
    }
   catch (Throwable e) {
      BoardLog.logE("BUSH","Problem adding repair",e);
    }
}



private class RepairAdder implements Runnable {

   private BushRepair for_repair;

   RepairAdder(BushRepair br) {
      for_repair = br;
    }

   @Override public void run() {
      BoardLog.logD("BUSH","Add repair " + for_repair);
      if (for_repair == null) return;
   
      int sz = list_model.getSize();
      if (sz == 0) {
         suggestions_pending.setVisible(false);
         suggestion_list.setVisible(true);
       }
   
      list_model.addElement(for_repair);
      BudaBubble bbl = BudaRoot.findBudaBubble(suggestion_list);
      if (bbl != null) {
         Dimension d1 = bbl.getPreferredSize();
         bbl.setSize(d1);
         BoardLog.logD("BUSH","Suggest size " + list_model.getSize() + " " +
               suggestion_list.getVisibleRowCount() + " " + d1);
       }
    }

}	// end of inner class RepairAdder


@Override public synchronized void doneRepairs()
{
   RepairFinisher ra = new RepairFinisher();
   try {
      SwingUtilities.invokeLater(ra);
    }
   catch (Throwable e) {
      BoardLog.logE("BUSH","Problem finishing repairs",e);
    }
}


private class RepairFinisher implements Runnable {

   RepairFinisher() { }

   @Override public void run() {
      int sz = list_model.getSize();
      if (sz == 0) {
	 suggestions_pending.setText("No suggestions found");
       }
      if (done_label != null) {
	 String txt = done_label.getText();
	 if (txt.startsWith("Finding")) {
	    txt = "Showing" + txt.substring(7);
	    done_label.setText(txt);
	  }
       }
    }

}	// end of inner class RepairFinisher


/********************************************************************************/
/*										*/
/*	Create a preview bubble 						*/
/*										*/
/********************************************************************************/

private void showPreviewBubble(RootRepair repair)
{
   RootLocation loc = repair.getLocation();
   int m0 = loc.getMethodOffset();
   int m1 = loc.getMethodEndOffset();
   if (m0 == 0 || m1 == 0) return;
   RootEdit edit = repair.getEdit();
   int e0 = edit.getEditStartOffset();
   int e1 = edit.getEditEndOffset();
   if (e0 == 0 || e1 == 0) return;
   BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(loc.getProject(),loc.getFile());
   if (bfo == null) return;

   BoardAttributes atts = new BoardAttributes("Rose");
   AttributeSet oldedit = atts.getAttributes("Original");
   AttributeSet newedit = atts.getAttributes("Edited");

   int e2 = e0;
   int e3 = e1;

   DefaultStyledDocument d1,d2;
   try {
      String text = bfo.getText(m0,m1-m0);
      d1 = new DefaultStyledDocument();
      d1.insertString(0,text,null);
      d2 = new DefaultStyledDocument();
      d2.insertString(0,text,null);
      Position p0 = (e0 <= m0 ? null : d2.createPosition(e0-m0-1));
      Position p1 = (e1-m0 >= d2.getLength() ? null : d2.createPosition(e1-m0+1));
      int delta = applyEdit(d2,m0,repair.getEdit());
      int e2a = (e0-m0);
      int e3a = (e1-m0) - (delta-m0);
      e2 = (p0 == null ? 0 : p0.getOffset()+1);
      e3 = (p1 == null ? d2.getLength() : p1.getOffset()-1);
      if (e2 != e2a || e3 != e3a)
	 BoardLog.logE("BUSH","Edit change " + e2a + " " + e2 + " " + e3a + " " + e3);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BUSH","Problem getting text for preview",e);
      return;
    }

   int lno = bfo.findLineNumber(m0);
   PreviewPanel pnl = new PreviewPanel(repair,d1,d2,lno);

   JTextPane tp = pnl.getOriginalEditor();
   tp.select(e0-m0,e1-m0);
   tp.setCharacterAttributes(oldedit,true);

   JTextPane etp = pnl.getEditedEditor();
   etp.select(e2,e3);
   etp.setCharacterAttributes(newedit,true);

   PreviewBubble bbl = new PreviewBubble(pnl);
   bubble_area.addBubble(bbl,content_pane,null,
	 BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_RIGHT);
   bbl.setVisible(true);
}



private int applyEdit(DefaultStyledDocument doc,int delta,RootEdit edit) throws BadLocationException
{
   Element editxml = IvyXml.getChild(edit.getTextEditXml(),"EDIT");
   delta = applyEdit(doc,delta,editxml);

   return delta;
}


private int applyEdit(DefaultStyledDocument doc,int delta,Element edit) throws BadLocationException
{
   int off = IvyXml.getAttrInt(edit,"OFFSET");
   int len = IvyXml.getAttrInt(edit,"LENGTH");
   String text = IvyXml.getTextElement(edit,"TEXT");

   switch (IvyXml.getAttrString(edit,"TYPE")) {
      case "MULTI" :
	 for (Element cedit : IvyXml.children(edit,"EDIT")) {
	    delta = applyEdit(doc,delta,cedit);
	  }
	 break;
      case "INSERT" :
	 doc.insertString(off-delta,text,null);
	 delta -= text.length();
	 break;
      case "DELETE" :
	 doc.remove(off-delta,len);
	 delta += len;
	 break;
      case "REPLACE" :
	 doc.replace(off-delta,len,text,null);
	 delta -= text.length()-len;
	 break;
      default :
	 BoardLog.logE("BUSH","Unknown edit type: " + IvyXml.convertXmlToString(edit));
	 break;
    }

   return delta;
}



/********************************************************************************/
/*										*/
/*	Main panel								*/
/*										*/
/********************************************************************************/

private class SuggestPanel extends SwingGridPanel {

   private static final long serialVersionUID = 1;

   SuggestPanel() {
      setBackground(BoardColors.getColor("Rose.background.color"));
      setOpaque(true);
    }

}	// end of inner class SimplePanel


/********************************************************************************/
/*										*/
/*	Bubble for the panel							*/
/*										*/
/********************************************************************************/

private class PanelBubble extends BudaBubble {

   private static final long serialVersionUID = 1;

   PanelBubble(Component cnts) {
      setContentPane(cnts);
    }

   @Override public void handlePopupMenu(MouseEvent e) {
      JPopupMenu menu = new JPopupMenu();

      Point p0 = SwingUtilities.convertPoint(this,e.getPoint(),suggestion_list);
      int row = suggestion_list.locationToIndex(p0);
      Rectangle r0 = suggestion_list.getCellBounds(row,row);

      if (r0.contains(p0)) {
	 BushRepair br = list_model.getElementAt(row);
	 if (br != null) {
	    if (do_preview) menu.add(new PreviewAction(br));
	    menu.add(new RepairAction(br));
	    menu.add(new SourceAction(br));
	  }
       }

      menu.add(getFloatBubbleAction());

      menu.show(this,p0.x,p0.y-5);
    }

}	// end of inner class PanelBubble



/********************************************************************************/
/*										*/
/*	Our list widget 							*/
/*										*/
/********************************************************************************/

private class SuggestList extends JList<BushRepair> {

   private transient SuggestRenderer suggest_renderer;

   private static final long serialVersionUID = 1;

   SuggestList(ListModel<BushRepair> mdl) {
      super(mdl);
      suggest_renderer = null;
      setVisibleRowCount(1);
      addMouseListener(new SuggestMouser());
    }

   @Override public SuggestRenderer getCellRenderer() {
      if (suggest_renderer == null) {
	 suggest_renderer = new SuggestRenderer();
       }
      return suggest_renderer;
    }

}	// end of inner class SuggestList


private class SuggestRenderer implements ListCellRenderer<BushRepair> {

   private DefaultListCellRenderer base_renderer;

   SuggestRenderer() {
      base_renderer = new DefaultListCellRenderer();
    }

   @Override public Component getListCellRendererComponent(JList<? extends BushRepair> l,
	 BushRepair r,int idx,boolean sel,boolean foc) {
      RootLocation loc = r.getLocation();
      String desc = r.getDescription();

      String mthd = loc.getMethod();
      int midx = mthd.lastIndexOf(".");
      if (midx > 0) idx = mthd.lastIndexOf(".",midx-1);
      if (midx > 0) mthd = mthd.substring(midx+1);

      String cnts = "<html>" + IvyXml.htmlSanitize(desc) + "<p>";
      cnts += "&nbsp;&nbsp;&nbsp;At " + loc.getLineNumber() + " of " + mthd;
      cnts += " (" + IvyFormat.formatNumber(r.getValidatedPriority()) + ")";

      BoardLog.logD("BUSH","Display repair: " + cnts);
      Component c = base_renderer.getListCellRendererComponent(l,cnts,idx,sel,foc);

      return c;
    }

}	// end of inner class SuggestRenderer




private class SuggestMouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
	 int index = suggestion_list.locationToIndex(e.getPoint());
	 BushRepair br = list_model.getElementAt(index);
	 if (br != null) {
	    SourceAction act = new SourceAction(br);
	    act.actionPerformed(null);
	  }
       }
    }

}	// end of inner class SuggestMouser



/********************************************************************************/
/*										*/
/*	Sorted list model							*/
/*										*/
/********************************************************************************/

private class SuggestListModel extends DefaultListModel<BushRepair> {

   private static final long serialVersionUID = 1;

   @Override public synchronized void addElement(BushRepair r) {
      double th = 0;
      if (getSize() > 0) th = elementAt(0).getValidatedPriority() * THRESHOLD;
      if (r.getValidatedPriority() < th) return;

      int min = 0;
      int max = getSize()-1;
      while (min <= max) {
	 int mid = (min+max)/2;
	 BushRepair r1 = elementAt(mid);
	 if (r1.getValidatedPriority() >= r.getValidatedPriority()) {
	    min = mid+1;
	  }
	 else {
	    max = mid-1;
	  }
       }

      BoardLog.logD("BUSH","Add repair " + min + " " + getSize() + " " + r.getDescription());

      add(min,r);
      if (min == 0) {
	 th = r.getValidatedPriority() * THRESHOLD;
	 int cut = -1;
	 for (int i = 1; i < getSize(); ++i) {
	    BushRepair r1 = elementAt(i);
	    if (r1.getValidatedPriority() < th) {
	       cut = i;
	       break;
	     }
	  }
	 if (cut > 0) {
	    removeRange(cut,getSize()-1);
	  }
       }

      int sz = Math.min(getSize(),10);

      BoardLog.logD("BUSH","Set row count to " + sz);

      suggestion_list.setVisibleRowCount(sz+1);
    }

}	// end of inner class SuggestListModel



/********************************************************************************/
/*										*/
/*	Suggestion actions							*/
/*										*/
/********************************************************************************/

private class PreviewAction extends AbstractAction {

   private transient BushRepair for_repair;

   private static final long serialVersionUID = 1;

   PreviewAction(BushRepair r) {
      super("Preview repair " + r.getDescription());
      for_repair = r;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BushFactory.metrics("SUGGEST_PREVIEW",metric_id,for_repair.getId(),
	    for_repair.getLogData(),for_repair.getDescription(),
	    for_repair.getPriority(),for_repair.getValidatedPriority());
      BoardLog.logD("BUSH","PREVIEW REPAIR " + for_repair.getDescription());
      showPreviewBubble(for_repair);
    }

}	// end of inner class PreviewAction


private class RepairAction extends AbstractAction {

   private transient RootRepair for_repair;

   private static final long serialVersionUID = 1;

   RepairAction(RootRepair r) {
      super("Make repair " + r.getDescription());
      for_repair = r;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BushFactory.metrics("MAKE_REPAIR",metric_id,for_repair.getId(),
	    for_repair.getLogData(),for_repair.getDescription(),
	    for_repair.getPriority(),for_repair.getValidatedPriority());
      RootEdit redit = for_repair.getEdit();
      Element tedit = redit.getTextEditXml();

      BoardLog.logD("BUSH","MAKE REPAIR " + for_repair.getDescription() + " " + redit.getFile() + " " + IvyXml.convertXmlToString(redit.getTextEditXml()));

      if (IvyXml.isElement(tedit,"REPAIREDIT")) tedit = IvyXml.getChild(tedit,"EDIT");
      BaleFactory.getFactory().applyEdits(redit.getFile(),tedit);
   }

}	// end of inner class RepairAction



private class SourceAction extends AbstractAction implements Runnable {

   private transient RootRepair for_repair;
   private BudaBubble source_bubble;
   private static final long serialVersionUID = 1;

   SourceAction(RootRepair r) {
      super ("Show source for " + r.getDescription());
      for_repair = r;
      source_bubble = null;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (for_repair == null) return;
      BoardMetrics.noteCommand("BUSH","GotoSuggestSource");
      BushFactory.metrics("SUGGEST_SOURCE",metric_id,for_repair.getId(),
	    for_repair.getLogData(),for_repair.getDescription(),
	    for_repair.getPriority(),for_repair.getValidatedPriority());
      BushFactory.metrics("GOTO_SOURCE",for_repair.getDescription());
      BushLocation loc = (BushLocation) for_repair.getLocation();
      if (loc == null) loc = for_location;
      if (loc == null) return;
      BoardLog.logD("BUSH","Go to source for " + loc + " " + loc.getBumpLocation());
      String proj = loc.getProject();
      BumpLocation bloc = loc.getBumpLocation();
      String fct = loc.getMethod();
      if (bloc != null) fct = bloc.getKey();
      int idx = fct.indexOf("(");
      if (idx > 0) {
	 String f1 = fct.substring(0,idx);
	 String args = fct.substring(idx+1);
	 int idx1 = args.lastIndexOf(")");
	 if (idx1 > 0) args = args.substring(0,idx1);
	 String a1 = IvyFormat.formatTypeNames(args,",");
	 fct = f1 + "(" + a1 + ")";
       }
      BoardLog.logD("BUSH","Source request " + fct);
      source_bubble = BaleFactory.getFactory().createMethodBubble(proj,fct);
      if (source_bubble != null) {
	 SwingUtilities.invokeLater(this);
       }
    }

   @Override public void run() {
      BudaBubble bbl = BudaRoot.findBudaBubble(content_pane);
      bubble_area.addBubble(source_bubble,bbl,null,
            BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_GROUPED|BudaConstants.PLACEMENT_MOVETO);
   }

}	// end of inner class SourceAction



/********************************************************************************/
/*										*/
/*	Preview Panel and Bubble						*/
/*										*/
/********************************************************************************/

private class PreviewPanel extends SwingGridPanel {

   private transient RootRepair for_repair;
   private JTextPane before_editor;
   private JTextPane after_editor;

   private static final long serialVersionUID = 1;

   PreviewPanel(RootRepair rep,StyledDocument d1,StyledDocument d2,int start) {
      for_repair = rep;
      String txt = "Preview of " + rep.getDescription();
      beginLayout();
      addBannerLabel(txt);
      addSeparator();
      before_editor = new PreviewEditor(d1);
      after_editor = new PreviewEditor(d2);
      JScrollPane bsp = new SwingLineScrollPane(before_editor,start);
      JScrollPane asp = new SwingLineScrollPane(after_editor,start);
      
      SwingGridPanel codepanel = new SwingGridPanel();
      codepanel.addGBComponent(bsp,0,0,1,1,10,10);
      codepanel.addGBComponent(new JSeparator(JSeparator.VERTICAL),1,0,1,1,0,10);
      codepanel.addGBComponent(asp,2,0,1,1,10,10);
      addLabellessRawComponent("EDITS",codepanel);
      setBackground(BoardColors.getColor("Rose.background.color"));
      setOpaque(true);
      if (start > 0) {
         Dimension d = codepanel.getPreferredSize();
         d.width += 2*MARGIN_WIDTH_PX + 6;
         codepanel.setPreferredSize(d);
       }
      
    }

   RootRepair getRepair()				{ return for_repair; }

   JTextPane getOriginalEditor()			{ return before_editor; }
   JTextPane getEditedEditor()				{ return after_editor; }

}	// end of inner class PreviewPanel


private class PreviewEditor extends JTextPane {

   private static final long serialVersionUID = 1;

   PreviewEditor(StyledDocument d) {
      super(d);
      setEditable(false);
    }

}	// end of inner class PreviewEditor







private class PreviewBubble extends BudaBubble {

   private transient RootRepair for_repair;

   private static final long serialVersionUID = 1;

   PreviewBubble(PreviewPanel pnl) {
      for_repair = pnl.getRepair();
      setContentPane(pnl);
    }

   @Override public void handlePopupMenu(MouseEvent e) {
      JPopupMenu menu = new JPopupMenu();
      menu.add(new RepairAction(for_repair));
      menu.add(new SourceAction(for_repair));
      menu.add(getFloatBubbleAction());
    }

}	// end of inner class PreviewBubble


}	// end of class BushSuggestPanel




/* end of BushSuggestPanel.java */

