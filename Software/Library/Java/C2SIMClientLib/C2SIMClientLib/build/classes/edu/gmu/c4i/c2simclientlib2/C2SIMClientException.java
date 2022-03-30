/*----------------------------------------------------------------*
|    Copyright 2001-2022 Networking and Simulation Laboratory     |
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
*----------------------------------------------------------------*/

package edu.gmu.c4i.c2simclientlib2;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * <h1>C2SIMClientException</h1>
 * Customized exception for BMLClient applications
 * Extends standard java Exception class
 * Adds an embedded root cause exception. The msg for this is accessible  
 * via getCauseMessage()
 * @author Douglas Corner - George Mason University C4I and Cyber Center
 */
public class C2SIMClientException extends Exception{

    private String msg;
    private String stackTrace;
    private Throwable cause;

    /**
    BMLCLientException Constructor
    @param msg -String - Description of exception
    */  
    public C2SIMClientException(String msg) {
        super(msg);
        this.msg = msg;
        this.cause = null;
        // saveStackTrace();
    }   //  constructor BMLException w/o Throwable

    /**
    BMLClientException Constructor
    @param msg - String Message added by routine catching the primary exceptoin
    @param cause - String Primary cause of exception
    */
    // BML Exception caused by another exception
    public C2SIMClientException(String msg, Throwable cause) {
        super(msg);
        this.msg = msg;
        this.cause = cause;
    }   // constructor BMLException with Throwable

    /**
    * Get message set in this exception when instantiated
    * @return String - Message included in constructor 
    */   
    @Override
    public String getMessage() {
        return msg;
    }   // getMessage
    
    /**
    * Get message from another exception thrown by underlying software and included in this exception
     * @return String - Underlying cause message 
    */
    public String getCauseMessage() {
        if (cause == null)
            return "";
        else
            return cause.getMessage();
    }   // getExceptionCause

    
}   // class C2SIMClientException
