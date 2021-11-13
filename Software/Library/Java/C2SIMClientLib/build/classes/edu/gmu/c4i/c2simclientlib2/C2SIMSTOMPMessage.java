/*----------------------------------------------------------------*
|    Copyright 2001-2020 Networking and Simulation Laboratory     |
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

import java.util.HashMap;
import java.util.Vector;


/**
 *  <h1>C2SIMSTOMPMessage</h1>
 *  Encapsulates a STOMP Message along with other data created during the processing of the message
 * @author Douglas Corner - George Mason University C4I and Cyber Center
 */
public class C2SIMSTOMPMessage {
    /**
     * messageType  STRING - STOMP Message COMMAND received
    */
    protected String         messageType;                   // CONNECTED, MESSAGE, etc
    
    /**
     * messageSelector  STRING - Type of BML message as determined by XML matching
    */
    protected String         messageSelector;               // IBMLReport, ..
    
    /**
     * headers  Vector of STRINGs - Raw unparsed STOMP message headers
    */
    protected Vector<String> headers; 
    
    /**
     *  headerMap - HashMap of String,String = Maps header to header value
    */
    protected java.util.HashMap<String, String> headerMap;  // Header     
    
    /**
     * messageBody String = The body of the message not including terminating null
    */
    protected String         messageBody;
    
    /**
     * contentLength Long - Length of message body as received not including 
     * terminating null.  Obtained from content-length header    
    */
    protected Long           contentLength;
    
    /**
    * messageLength - Length of the message after removing the C2SIM header.  
    * For non C2SIM messages this is the same as the contentLength
    */
    protected Long           messageLength;
       
    /**
    * C2SIMHeader stripped from incoming Message
    */
    protected C2SIMHeader   c2sim;
    
    /**
     * C2SIM Header stripped form incoming message
     */
    /**
     * error Throwable - Exception caught in foreground thread.  Used to 
     * communicate exception to background.
     * May be an otherwise empty message
     */
    protected Throwable      error;

    // Constructor - Initialize properties
    /**
     * BMLSTOMPMessage Constructor
     */
    public C2SIMSTOMPMessage() {
        messageSelector = "";               
        messageType = "";
        headers = new Vector<>();           // Empty vector
        headerMap = new HashMap<>();        // Initialize empty header map
        messageBody = "";                   // Empty message body
        messageLength = 0L;                 // There may not be a message body
        error = null;
    }

    // The actual instance variables are all protected and will only be 
    // accesses by other members of the BMLClientLib
    // Add a line to the list of headers
    protected void addHeader(String s) {
        headers.add(s);
    }

    // Add line to message body
    protected void addToBody(String s) {
        messageBody += s;
    }   

    // Getters for class properties
    /**
    * getMessageType - Returns the STOMP command for this message.  
    * Normally CONNECTED or MESSAGE
    * @return String - The STOMP COMMAND for this message
    */
    public String getMessageType() {
        return messageType;
    }   // getMessageType()
    
    /**
     * Return the BML message type determined when the server receives 
     * the message from its creator 
     * @return String - BML Message Selector e.g. IBML09GSR
    */
    public String getMessageSelector() {
        return messageSelector;
    }   // getMessageSelector
    
    /**
     * Return the body of the message, i.e. the part of the message 
     * following the headers.  Does not include the terminating NULL
     * @return String - The message body from the STOMP Message 
    */
    public String getMessageBody() {
        return messageBody;
    }   // getMessageBody()
    
    /**
     * getMessageLength - Get the length of the message without the C2SIM Header <BR>
     *  For non C2SIM messages this is the same as the contentLength.
     * @return Long - The message length
    */
    public Long getMessageLength() {
        return messageLength;
    }   // getMessageLength()
    
    /**
     * Get the length of the message as determined by the content-length header
     * @return Long - The content length
    */
    public Long getContentLength() {
        return contentLength;
    }   // getMessageLength()
    
    /**
    * getC2SIMHeader
    * @return C2SIHeader the c2sim header from this message 
    */
    public C2SIMHeader getC2SIMHeader() {
            return c2sim;
    }    
    
    /**
    *   createHeaderMap <BR>
    *   Move the values from headers Vector creating a HashMap of header 
    *   names and header values
    *   @return String - messageSelector if one was found 
    */
    public String createHeaderMap() {
        int i;
        String header = "";
        int headerValStart;
        String headerVal;
        String messasgeSelector = "";
        
         for (i = 0; i < headers.size(); ++i) {
            String s = headers.elementAt(i);
            headerValStart = s.indexOf(":") + 1;
            header = s.substring(0, headerValStart - 1);
            headerVal = s.substring(headerValStart, s.length());
            headerMap.put(header, headerVal);

            // A value of true is used with the particular message Selector 
            // for this message
            if (headerVal.toLowerCase().equals("true"))
                messageSelector = header;
        }   // header loop  
         return messageSelector;
    }   // setHeaderMap()
    
    /**
     *  getHeader <BR>
     *      Get the contents of a specific STOMP header
     * @param header - Specific header e.g. "content-length"
     * @return - String - Value of header or "" if header not set in incoming message
     */
    public String getHeader(String header) {
       
        if (headerMap.containsKey(header)) {
            return headerMap.get(header);
        }
        else {
            return "";
        }
        
    }   // getHeader()    
    
}   // class STOMPMessge
