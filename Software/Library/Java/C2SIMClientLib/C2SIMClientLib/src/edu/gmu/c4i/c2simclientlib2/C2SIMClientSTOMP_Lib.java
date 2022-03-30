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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Date;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

/**
 * 
 * <h1>BMLClientSTOMP</h1><p>
 *  Lib - STOMP Client library<p>
 *  Contains collection of routines to access and receive messages from a STOMP server<p>
  Creates and returns C2SIMSTOMPMessage objects <p>
 * @see edu.gmu.c4i.c2simclientlib2.C2SIMSTOMPMessage
 * @author Douglas Corner - George Mason University C4I and Cyber Center
 *  code version 4.8.0.4
 */
public class C2SIMClientSTOMP_Lib extends Thread {
    
    static String SISOSTD = "SISO-STD-C2SIM";

    private static final String XML_PREAMBLE = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String END_OF_FRAME = "\u0000";

    // Instance Variables
    private Socket socket = null;

    /** 
     * host - Name of STOMP host
     */
    private String host;

    /**
     * port - The TCP port used to communicate with the 
     * STOMP host, normal default is 61613
     */
    private Integer port;

    /**
     * destination - The topic being used for this 
     * STOMP session - The only one being used currently 
     * is "/topic/C2SIM"
     */
    private String destination;

    /**
     * subscriptions - Vector of message types (e.g. BML_Report).<BR> 
     *   These will be submitted at connection time so that only 
     *   messages with message matching one of the 
     *   @deprecated - adv subsciptions and addAdvSubscription 
     *   should be used instead
     */
    private Vector<String> subscriptions;

    /**
     * adv_subscriptions - These are subscriptions that can address any 
     * header in the message using SQL-like statements.
     */
    private Vector<String> adv_subscriptions;

    /**
     * currentMsg - Reference to the XML message with the C2SIM Header removed.   
     */
    private C2SIMSTOMPMessage currentMsg;

    /**
     * messageSelector - String - Indicates type of message, e.g. IBML09_Report
     */
    private String messageSelector = "";

    private InputStreamReader inputStream;
    private BufferedReader in;
    private byte b[] = new byte[1000];
    private static java.util.concurrent.LinkedBlockingQueue<C2SIMSTOMPMessage> queue;

    // There is only one queue (It is a static variable).  Initialize it in a static block
    static {
        queue = new java.util.concurrent.LinkedBlockingQueue<>();
    }   // static


    /**************************************/
    /* C2SIMClientSTOMP_Lib() Constructor   */
    /**************************************/
    /**
     * Constructor - No parameters
     */
    public C2SIMClientSTOMP_Lib() {
        // Constructor

        subscriptions = new Vector<>();
        adv_subscriptions = new Vector<>();


        // Default value for port
        this.port = 61613;

        // Default value for destination
        this.destination = "/topic/C2SIM";


    }   // BMLClientSTOMP()


    /****************/
    /* connect      */
    /****************/
    // Connect to STOMP Host
    //  Wait for CONNECTED Message
    /**
     * Connect to Stomp host
     * @return STOMPMessage - Response from host if connection 
     * made otherwise throw an exception.  Response should be CONNECTED.
     * @see edu.gmu.c4i.c2simclientlib2.C2SIMClientException
     * @see edu.gmu.c4i.c2simclientlib2.C2SIMSTOMPMessage    
     * @throws C2SIMClientException - Includes various exceptions
     */
    public C2SIMSTOMPMessage connect() throws C2SIMClientException {

        try {
            // Create socket and make initial connection to host
            socket = new Socket(host, port);

            // Create CONNECT message
            String connectFrame = "CONNECT\n"
                    + "login:\n"
                    + "passcode:\n"
                    + "\n"
                    + END_OF_FRAME;
            // Send the CONNECT
            sendFrame(socket, connectFrame);

            // Send subscription
            Date date = new Date();
            String subscribeFrame;

            // Create SUBSCRIBE message
            subscribeFrame = "SUBSCRIBE\n";

            // Add any message selector

            if (subscriptions.size() != 0) {
                subscribeFrame += "selector: message-selector = '" + 
                    subscriptions.elementAt(0) + "'";
                for (int i = 1; i < subscriptions.size(); ++i) {
                    subscribeFrame += " OR message-selector = '" + 
                        subscriptions.elementAt(i) + "'";
                }   // for subscriptions 1 through n

                subscribeFrame += "\n";
            }   // if there are subscriptions

            if (adv_subscriptions.size() != 0) {
                subscribeFrame += "selector: ";
                for (String adv : adv_subscriptions) {
                    subscribeFrame += adv + "\n";
                }

            }   // if there are adv subscriptions

            subscribeFrame += "destination: " + destination + "\n";

            // Add message ID, blank line and null
            subscribeFrame += "id: " + date.toString() + "\n"
                    + "\n" + END_OF_FRAME;

            // Send the SUBSCRIBE frame
            sendFrame(socket, subscribeFrame);

            // Start forground thread so messages from host can be received.
            this.start();

            // Get the response to connection request
            C2SIMSTOMPMessage resp = getNext_Block();

            // Are we connected?  If so return the message.  If not throw an exception
            if (resp.getMessageType().equals("CONNECTED"))
                return resp;

            else
                throw new C2SIMClientException("Expected 'CONNECTED' but received " + resp.getMessageType());

        } //try //try //try //try //try //try //try //try
        catch (java.net.UnknownHostException e) {
            throw new C2SIMClientException("Unknown Host", e);
        } // Unknown host exception        
        catch (IOException i) {
            throw new C2SIMClientException("IOException", i);
        } // IO Exceptoin // IO Exceptoin

    }   // connect()


    /****************/
    /* publish      */
    /****************/
    /**
     * Send message to STOMP host on an already established connection 
     * @param cmd - STOMP Command to be used - should normally be MESSAGE
     * @param xml - The message to be sent
     * @param headers - A Vector Strings containing STOMP headers in the form  headerName:headerValue
     * @throws C2SIMClientException - Thrown by sendFrame()
     */
    public void publish(String cmd, Vector<String> headers, String xml) throws C2SIMClientException {

        // Compute the content-length including the terminating NL and the END_OF_FRAME and add a header
        headers.add("content-length:" + new Integer(xml.length() + 1).toString() + "\n");

        String msg = cmd + "\n";
        for (String h : headers) {
            msg += h;
        }
        // Add blank line to mark end of headers
        msg += "\n";

        // Add message to be published  Make sure there is a terminating NL.
        msg += xml + "\n";

        // Add null to mark end of message
        msg += END_OF_FRAME;

        // Send the message
        sendFrame(socket, msg);

    }   // publish


    /*************************/
    /* getNext_NoBlock()     */
    /*************************/
    /**
     * Returns the next message received from the STOMP messaging server.  
     * The calling thread will NOT be blocked if a STOMPMessage is not available; .
     * @return STOMPMessage - The next STOMP message or NULL if no message is available at this time.  Message should be MESSAGE.
     * @see edu.gmu.c4i.c2simclientlib2.C2SIMSTOMPMessage
     * @see edu.gmu.c4i.c2simclientlib2.C2SIMClientException
     *  @throws C2SIMClientException - Encapsulates several specific exceptions
     */
    public C2SIMSTOMPMessage getNext_NoBlock() throws C2SIMClientException {

        if (queue.isEmpty()) {
            return null;
        }
        else
            // We know there is a message, Go use the getNextBlock code
            return getNext_Block();
    }   // getNext_NoBlock()


    /*************************/
    /* getNext_Block()       */
    /*************************/
    /**
     * Returns the message received from the STOMP messaging server.  The calling thread
     * will be blocked until a message has been received.
     * @return STOMPMessage - The next STOMP message.  Message should be MESSAGE.
     * @see edu.gmu.c4i.c2simclientlib2.C2SIMSTOMPMessage
     * @see edu.gmu.c4i.c2simclientlib2.C2SIMClientException    
     * @throws C2SIMClientException - Encapsulates various exceptions
     */
    public C2SIMSTOMPMessage getNext_Block() throws C2SIMClientException {

        Vector<String> lines = new Vector<>();
        String header;
        String headerVal;
        String stompMessage = "";

        int i, j;
        int headerValStart;

        // Share processor
        Thread.yield();

        try {
            // Wait for next STOMP Message
            currentMsg = null;
            currentMsg = queue.take();
        }   // try
        catch (InterruptedException ie) {
            throw new C2SIMClientException("Interrupted exception in queue.take", ie);
        }   // InterruptedException   // InterruptedException

        // Foreground thread can't throw an exception as there is no caller.
        //   Check for a STOMPMsg object in the queue with something in error
        if (currentMsg.error != null) {
            throw new C2SIMClientException("Error caught in foreground thread", 
                currentMsg.error);
        }   // if

        // The headers have been extracted from the incoming message
        //    Move them into a HashMap so lookups can be done
        //    This code also detects the presence of a Message Selector
        messageSelector = currentMsg.createHeaderMap();

        // If this is a C2SIM Message:
        //      Extract the original XML and return in messageBody
        //      Extract the C2SIM information and build a CWIXHeader
        //      Add kthe CWIXHeader to the currentMsg
        if ((currentMsg.headerMap.containsKey("protocol")) && 
            (currentMsg.headerMap.get("protocol").equalsIgnoreCase(SISOSTD))) {

            // Fill out a new C2SIM Header with message
            C2SIMHeader c2s =C2SIMHeader.populateC2SIM(currentMsg.getMessageBody());
            currentMsg.c2sim = c2s;

            // Remove C2SIM header and trailer
            String xml = C2SIMHeader.removeC2SIM(currentMsg.getMessageBody());

            currentMsg.messageBody = xml;
            currentMsg.messageLength = new Long(xml.length());
        }   // Build C2SIM Header

        // Return the STOMPMessage object to the caller
        return currentMsg;

    }   // getNext()


    /************************/
    /* sendC2SIM_Response   */
    /************************/
    /**
     * sendC2SIM_Response - Send a C2SIM response to an incoming C2SIM request.  
     * Response will be sent via STOMP
     * @param oldMsg - Message that is being responsed to
     * @param c2sResp - Response code to be sent*
     * @param ackCode - Code describing the acknowledgement
     * @throws edu.gmu.c4i.c2simclientlib2.C2SIMClientException - May 
     * throw a C2SIMClientException for several reasons <BR>
     *      IOException during send or close
     *      UnknownHost exception
     *      Received something other that "CONNECTED" during connection process
     *      InterruptedException while waiting for queue
     *      Error caught in foreground thread
     */
    public void sendC2SIM_Response(
        C2SIMSTOMPMessage oldMsg, 
        String c2sResp, 
        String ackCode) 
        throws C2SIMClientException {

        C2SIMHeader c2s;
        C2SIMHeader oldc2s;
        String xml = "";
        String header = "";
        Vector<String> headers = new Vector<>();
        String msg = "";

        if (oldMsg.headerMap.get("protocol").equalsIgnoreCase(SISOSTD)) {
            oldc2s = oldMsg.c2sim;
            c2s = new C2SIMHeader();
            // Use the conversationID from the incoming message
            c2s.conversationID = oldc2s.conversationID;

            // Set performative for this message
            c2s.communicativeActTypeCode = oldc2s.communicativeActTypeCode;

            // inReplyTo is the request message;
            c2s.inReplyToMessageID = oldc2s.messageID;

            // Swap sender and receiver from inoming message
            c2s.fromSendingSystem = oldc2s.toReceivingSystem;
            c2s.toReceivingSystem = oldc2s.fromSendingSystem;
            
            // Convert header to xml
            header = c2s.toXMLString();
            
            // Build the acknowledement
            xml = "<MessageBody><AcknowledgementBody><AcknowledgementTypeCode>" + 
                ackCode +"</AcknowledgementTypeCode></AcknowledgementBody></MessageBody>"; 
            
            headers.add("protocol:C2SIM");
            
            // Build the full message
            msg = XML_PREAMBLE + 
                "<Message xmlns=\"http://www.sisostds.org/schemas/c2sim/1.0\">" + 
                header + xml + "</Message>"; 

        }   // C2SIM

        // Build Vector of headers for this message
        // Use most headers from incoming message
        headers.add("destination:" + oldMsg.getHeader("destination") + "\n");
        headers.add("content-type:text/plain\n");
        headers.add("submitter:" + oldMsg.getHeader("submitterID") + "\n");
        headers.add("message-time:" + oldMsg.getHeader("msgTime") + "\n");
        headers.add("message-type:" + oldMsg.getHeader("msgType") + "\n");
        headers.add("message-number:" + oldMsg.getHeader("msgNumber") + "\n");
        headers.add("conversationid:" + oldMsg.getHeader("conversationID") + "\n");
        headers.add("protocol" + SISOSTD + "\n");

        // Publish the message    
        publish("SEND", headers, msg);

    }   // sendCWSIM_Response


    /*******************/
    /* disconnect()    */
    /*******************/
    /**
     * Disconnect from STOMP server and close socket.
     * @return String - "OK" indicating successful completion of disconnect or else throws an exception
     * @throws C2SIMClientException - Encapsulates various exceptions
     */
    public String disconnect() throws C2SIMClientException {
        String disconnectFrame = "DISCONNECT\n"
                + "\n"
                + END_OF_FRAME;
        try {
            sendFrame(socket, disconnectFrame);
            socket.close();
            //this.interrupt();
        }   // try
        catch (IOException e) {
            throw new C2SIMClientException("IOException thrown in call to disconnect ", e);
        }   // Exception   // Exception

        // Disconnect was successful return OK
        return "OK";

    }   // disconnect()


    /*****************/
    /* sendFrame()    */
    /******************/
    // Send frame on current socket
    private static void sendFrame(Socket socket, String data) 
        throws C2SIMClientException {

        try {
            byte[] bytes = data.getBytes("UTF-8");
            BufferedOutputStream bos = 
                new BufferedOutputStream(socket.getOutputStream());
            bos.write(bytes, 0, bytes.length);
            bos.flush();
        }   // try
        catch (IOException i) {
            throw new C2SIMClientException("IO Exception while sending frame", i);
        }   // IOException   // IOException
    }   // sendFream


    /***********************/
    /* getters and setters  */
    /************************/
    // Getters and setters
    /**
     * setPort (String) for STOMP connection
     * @param port - String
     */
    public void setPort(String port) {
        this.port = Integer.decode(port);
    }   // setPort()


    /**
     * setPort int for STOMP connection
     * @param port - int
     */
    public void setPort(int port) {
        this.port = port;
    }


    /**
     * @return int - Current port setting
     */
    int getPort() {
        return port;
    }   // getPort()


    /**
     * Set the host name or IP address
     * @param host String name or IP address of STOMP server
     */
    public void setHost(String host) {
        this.host = host;
    }   // setHost()


    /**
     * Get the address of the STOMP Messaging server
     * @return String - Host name/address
     */
    public String getHost() {
        return this.host;
    }   // getHost()


    /**
     * Set the destination queue or topic
     * @param dest - String Name of topic.  The default is /topic/BML <BR>  
     *  If there is a trailing slash it will be removed.
     */
    public void setDestination(String dest) {

        destination = dest;

        // Make sure there isn't a trailing slash
        if (destination.endsWith("/"))
            destination = destination.substring(0, destination.length() - 1);

    }   // setDestination()


    /**
     * addSubscription - Add a Message Selector to list of selectors submitted with SUBSCRIBE
     *   Host will only publish messages matching one of the selectors.
     *   If no addSubscriptions are submitted then all messages will be received.
     * @param msgSelector String - Name of a BML Message Type to be added to subscription list.  If the list contains at least one Message Selector then the only messages 
     *   that will be received on the current connection will be those on the list.  If no subscriptions are submitted then this system will receive all messages published to the topicn
     */
    public void addSubscription(String msgSelector) {
        subscriptions.add(msgSelector);
    }   // addSubscription()


    /**
     * addAdvSubscription - Add a general selector expression to be used with SUBSCRIBE
     *   Host will only publish messages matching one of the selectors.
     *   If no addSubscriptions are submitted then all messages will be received.
     * @param subString String - Expression to be added to subscription list.  Expression will provide a header value to be used as a filter.  If specified  the only messages <BR>
     *   that will be received on the current connection will be those Satisfying the expression or those msgSelectors specified in addSubscription. <BR>
     *   If no subscriptions are submitted then this system will receive all messages published to the topic
     */
    public void addAdvSubscription(String subString) {
        adv_subscriptions.add(subString);
    }   // addadvSubscription()  


    // End of Getters/Setters
    /*******************************************/
    /*  Foreground message receive thread      */
    /*******************************************/
    /*
        Read command
            Only commands we should get are CONNECTED and MESSAGE
        Read headers, one per line
            Blank line makes end of headers
        Get content-length header and extract the lengch
            Use content-length to read message content
            Read NULL NL marking end of message
    
        Build STOMPMessage object and add
            Command
            Message headers (Vector)
            Message content as single string
        
        Add STOMPMessage to quque for background processing
     */
    /*******************/
    /* run()           */
    /*******************/
    /**
     * run method used internally to create a foreground thread for receiving 
     * and queuing messages form STOMP server <BR>
     * This is required to be public by JAVA and should not be called except 
     * by internal code.
     */
    @Override
    public void run() {

        String line;
        String body;

        // Initialize BufferedReader for reading incoming messages 
        try {
            //  BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            inputStream = new InputStreamReader(socket.getInputStream());
            in = new BufferedReader(inputStream);
        }   // try
        catch (IOException e) {
            // Exception thrown during initialization.  
            // Can't just throw or pass back as we are in a thread and there is no caller.
            // Add the exception to an otherwise blank STOMPMesaege and add it to the queue
            // The caller will find this when attempting to read the first message

            C2SIMSTOMPMessage sm = new C2SIMSTOMPMessage();
            sm.error = e;
            queue.add(sm);
        }   // catch   // catch   // catch   // catch


        String cmd = "";
        String msgBody;

        /* 
            Main Foreground Loop 
                Read messages from the STOMP server
                Process the message building a STOMPMessage object
                Add each message to a thread safe queue
            Message structure
                Command nl
                Multiple headers as header_name:header_value nl
                nl  (Blank line)
                Multiple lines of text
                Null (0x0)
                If content-length header is present if will provided the length of the text
                This code was tested with STOMP 1.2 on Apache-Apollo server and assumes that
                    the content length header will be present in all MESSAGE mesages
         */

        // Loop forever
        while (true) {

            C2SIMSTOMPMessage msg = new C2SIMSTOMPMessage();
            try {
                // Read command
                cmd = "";
                while (cmd.length() == 0)
                    cmd = in.readLine();

                // Set message type
                if (cmd.equals("MESSAGE")) {
                    msg.messageType = "MESSAGE";
                }
                else if (cmd.equals("CONNECTED")) {
                    msg.messageType = "CONNECTED";
                }
                else
                    msg.messageType = "INVALID " + cmd;

                // content-length header may not have been used (e.g. CONNECTED message)  
                //  Provode a default value of 0 indicating no message body
                Long contentLength = 0L;
                String cL = "0";

                // Read the STOMP message headers into a Vector and look for a content-length header
                // Headers are terminated by a blank line
                while (!(line = in.readLine()).equals("")) {
                    msg.addHeader(line);
                    // Is this a content-length header?
                    if (line.startsWith("content-length"))
                        cL = line.substring(line.indexOf(":") + 1, line.length());
                }   // while

                // Use the value from content-length header (or default value if we didn't find one)
                contentLength = Long.parseLong(cL);
                msg.contentLength = contentLength;
                msg.messageLength = contentLength;          // This may be modified later if this is a C2SIM message

                // End of headers - accumulate the message body into a single string
                //  Read until contentLength is exhausted
                String inbuf = "";

//                int linelen = 0;
//                while (contentLength > 0) {
//                    inbuf = in.readLine();                 
//                    contentLength -= (inbuf.length() + 1);      // Add one for NL
//                    linelen += inbuf.length() + 1;
//                            
//                    msg.messageBody += inbuf;
//                }   // while
//                System.out.println(linelen);
//                // Get the null at the end of the STOMP message
//                line = in.readLine();
//                

                char inchar = 10;
                StringBuffer inmsg = new StringBuffer(1000);
                int length = 0;

                while ((inchar = (char) in.read()) != 0) {
                    inmsg.append(inchar);
                }
                msg.messageBody = new String(inmsg);


                // Add the message to the queue
                queue.add(msg);
            }   // try

            catch (IOException e) {
                // Exception thrown.  Can't just throw or pass back as we are in a thread and there is no caller.
                // Add the exception to an otherwise blank STOMPMesaege and add it to the queue
                String cause = e.getMessage();
                if ((cause.equals("Socket closed")) || (cause.equals("Connection reset")))
                    return;
                C2SIMSTOMPMessage sm = new C2SIMSTOMPMessage();

                sm.error = e;
                queue.add(sm);
            }   // catch   // catch   // catch   // catch
        }   // Main Loop
    }   // run()


}   // Class C2SIMClientSTOMP_Lib

