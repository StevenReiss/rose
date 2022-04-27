/********************************************************************************/
/*                                                                              */
/*              RootValidate.java                                               */
/*                                                                              */
/*      Information for doing a validation                                      */
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



package edu.brown.cs.rose.root;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface RootValidate extends RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

RootProblem getProblem();


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void validateAndSend(RootProcessor rp,RootRepair rr);

boolean checkTestResult(RootTrace rt);


boolean canCheckResult();

boolean haveGoodResult();

RootTrace getExecutionTrace();

boolean addLocalFile(File f,String src);
boolean editLocalFile(File f,int start,int len,String cnts);
void setOutputOptions(boolean showall,boolean tostring,boolean toarray);
void createTestSession(File f,RootLocation loc);
RootTrace getTestTrace(File f);
void finishTestSession(File f);



interface RootTrace {
   long getProblemTime();
   RootTraceCall getProblemContext();
   RootTraceCall getRootContext();
   RootTraceValue getException();
   RootTraceValue getReturnValue();
   Map<String,RootTraceVariable> getGlobalVariables();
}

interface RootTraceCall {
   File getFile();
   String getMethod();
   long getStartTime();
   long getEndTime();
   List<RootTraceCall> getInnerTraceCalls();
   RootTraceVariable getLineNumbers();
   Map<String,RootTraceVariable> getTraceVariables();
}


interface RootTraceVariable {
   String getName();
   List<RootTraceValue> getTraceValues(RootTrace rt);
   RootTraceValue getValueAtTime(RootTrace tr,long time);
   int getLineAtTime(long time);
}


interface RootTraceValue {
   long getStartTime();
   boolean isNull();
   String getDataType();
   Long getNumericValue();
   int getLineValue();
   String getValue();
   RootTraceValue getFieldValue(RootTrace rt,String fld,long when);
   RootTraceValue getIndexValue(RootTrace rt,int idx,long when);
   String getId();
   int getArrayLength();
   String getEnum();
   
}

}       // end of class RootValidate




/* end of RootValidate.java */

