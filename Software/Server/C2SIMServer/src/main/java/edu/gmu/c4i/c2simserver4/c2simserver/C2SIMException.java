/*----------------------------------------------------------------*
|    Copyright 2001-2018 Networking and Simulation Laboratory     |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for academic purposes is hereby  |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
 *-----------------------------------------------------------------*/
package edu.gmu.c4i.c2simserver4.c2simserver;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * <h1>C2SIMException</h1>
  Customized exception class for C2SIM_Server <P>
 *    Provides for embedded root cause exception as well as a message indicating the application activity where the exception was caught
 * @author Douglas Corner - George Mason University C4I and Cyber Center
 */
public class C2SIMException extends Exception {

    private String msg;
    private StackTraceElement[] stackTrace;
    private String stackTraceString;
    private Throwable cause;
    private final int NUMBER_STACK_TRACE_ELEMENTS = 8;

    // BML Exception with a message
    /**
     * <h1>BMLException</h1>
     * Constructor no root cause
     * @param msg String - Descriptive message 
     */
    public C2SIMException(String msg) {
        super(msg);
        this.msg = msg;
        this.cause = null;
        stackTrace = getStackTrace();
        stackTraceString = "";
        for (int i = 0; i < Math.min(NUMBER_STACK_TRACE_ELEMENTS,stackTrace.length); ++i) {
            stackTraceString += "\n\tat " + stackTrace[i].toString();
        }
        C2SIM_Server.debugLogger.error(msg + stackTraceString);
        // saveStackTrace();
    }   //  constructor C2SIMException w/o Throwable


    // BML Exception caused by another exception
    /**
     * <h1>BMLException</h1>
     *   Constructor with message and root cause
     * @param msg String - Descriptive message
     * @param cause String - 
     */
    public C2SIMException(String msg, Throwable cause) {
        super(msg);
        this.msg = msg;
        this.cause = cause;
        stackTraceString = "";
        
        stackTrace = getStackTrace();       
        for (int i = 0; i < Math.min(NUMBER_STACK_TRACE_ELEMENTS, stackTrace.length); ++i) {
            stackTraceString += "\n\tat " + stackTrace[i].toString();
        C2SIM_Server.debugLogger.error(msg + stackTraceString);    
        }
    }   // constructor C2SIMException with Throwable

//    private void saveStackTrace() {
//        StringWriter errors = new StringWriter();
//        super.printStackTrace(new PrintWriter(errors));
//        msg = errors.toString();        
//    }

    /**
     * Get the primary exception message
     * @return String - Exception message as set within the application
     */
    public String getMessage() {
        return msg;
    }   // getMessage


    /**
     * Return a root cause message set by an exception caught within the application
     * @return String - root cause message 
     */
    public String getCauseMessage() {
        if (cause == null)
            return "";
        else
            return cause.getMessage();
    }   // getExceptionCause

     /**
     * Return limited stack trace
     * @return String - root cause message 
     */
    public String getST() {
        return stackTraceString;
    }   // getST
}   // class C2SIMException
