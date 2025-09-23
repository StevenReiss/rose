/********************************************************************************/
/*                                                                              */
/*              BractFactory.java                                               */
/*                                                                              */
/*      Factory for Basic Resources for Accessing Common Teachnolgy             */
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



package edu.brown.cs.rose.bract;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootRepairFinder;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseLog;

public final class BractFactory implements BractConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<Class<?>> processor_classes;
private List<Class<?>> location_classes;

private static BractFactory the_factory;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public static synchronized BractFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BractFactory();
    }
   return the_factory;
}

private BractFactory() 
{ 
   processor_classes = new ArrayList<>();
   location_classes = new ArrayList<>();
   BoardProperties props = BoardProperties.getProperties("Rose");
   RoseLog.logD("BRACT","Found properties " + props.size());
   for (String s : props.stringPropertyNames()) {
      if (s.startsWith("Rose.processor")) {
         String v = props.getString(s);
         RoseLog.logD("BRACT","REGISTER " + s + " " +  v);
         registerProcessor(v);
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Create a problem description                                            */
/*                                                                              */
/********************************************************************************/

public RootProblem createProblemDescription(RootControl ctrl,Element xml)
{
   return new BractProblem(ctrl,xml);
}




/********************************************************************************/
/*                                                                              */
/*      Create a location                                                       */
/*                                                                              */
/********************************************************************************/

public RootLocation createLocation(RootControl ctrl,Element xml)
{
   if (xml == null) return null;
   
   return new BractLocation(ctrl,xml);
}



public RootLocation createLocation(File f,int loc,int eloc,int lin,String proj,
      String mthd)
{
   return new BractLocation(f,loc,eloc,lin,proj,mthd,0);
}


public RootLocation createLocation(File f,int loc,int eloc,int lin,String proj,
      String mthd,double pri)
{
   return new BractLocation(f,loc,eloc,lin,proj,mthd,pri);
}



/********************************************************************************/
/*                                                                              */
/*      Register a suggestion processor                                         */
/*                                                                              */
/********************************************************************************/

public boolean registerProcessor(String clsnm)
{
   try {
      Class<?> cls = (Class<?>) Class.forName(clsnm);
      if (!RootRepairFinder.class.isAssignableFrom(cls)) {
         return false;
       }
      Constructor<?> cnst = cls.getConstructor();
      RootRepairFinder rrf = (RootRepairFinder) cnst.newInstance();
      boolean loc = rrf.requiresLocation();
      if (loc) location_classes.add(cls);
      else processor_classes.add(cls);
      RoseLog.logD("BRACT","Successful load of " + clsnm);
      return true;
    }
   catch (ClassNotFoundException e) { }
   catch (NoSuchMethodException e) { }
   catch (InvocationTargetException e) { }
   catch (IllegalAccessException e) { }
   catch (InstantiationException e) { }
   catch (Throwable e) {
      RoseLog.logE("BRACT","Problem loading class " + clsnm,e);
    }
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Start generating suggestions                                            */
/*                                                                              */
/********************************************************************************/

public void startSuggestions(RootControl ctrl,String rid,RootProblem prob,RootLocation at,
      RootValidate validate)
{
   BractControl proc = new BractControl(ctrl,rid,prob,at,processor_classes,
         location_classes,validate);
   proc.start();
}

}       // end of class BractFactory




/* end of BractFactory.java */

