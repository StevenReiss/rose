/********************************************************************************/
/*										*/
/*		BushUsageMonitor.java						*/
/*										*/
/*	Monitor programmer usage of Bush facilities				*/
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextListener;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.BussBubble;
import edu.brown.cs.bubbles.buss.BussConstants.BussEntry;
import edu.brown.cs.bubbles.buss.BussConstants.BussListener;

class BushUsageMonitor implements BushConstants, BussListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BumpLocation,String> location_data;
private String		metric_id;

private static List<BushUsageMonitor> all_monitors = new ArrayList<>();

static {
   BaleFactory.getFactory().addContextListener(new EditorListener());
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BushUsageMonitor(BussBubble buss,String id,Map<BumpLocation,String> locs)
{
   location_data = locs;
   metric_id = id;
   all_monitors.add(this);
   if (buss != null) buss.addBussListener(this);
}



static void remove(BushUsageMonitor um)
{
   all_monitors.remove(um);
}



/********************************************************************************/
/*										*/
/*	Handle bubble stack events						*/
/*										*/
/********************************************************************************/

@Override public void entrySelected(BussEntry ent)
{
   handleEvent(ent,"SELECTED");
}


@Override public void entryExpanded(BussEntry ent)
{
   handleEvent(ent,"EXPANDED");
}


@Override public void entryHovered(BussEntry ent)
{
   handleEvent(ent,"HOVERED");
}


private void handleEvent(BussEntry ent,String what)
{
   Collection<BumpLocation> locs = ent.getLocations();
   for (BumpLocation loc : locs) {
      String reason = location_data.get(loc);
      if (reason != null) {
	 BushFactory.metrics(what,metric_id,reason);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Track editors								*/
/*										*/
/********************************************************************************/

private static class EditorListener implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) { }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void noteEditorAdded(BaleWindow win) {
      // check window versus locations
    }

   @Override public void noteEditorRemoved(BaleWindow win) {
      // check window versus locations
    }

}	// end of inner class EditorListener



}	// end of class BushUsageMonitor




/* end of BushUsageMonitor.java */

