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

import edu.gmu.c4i.c2simclientlib2.C2SIMClientException;
import edu.gmu.c4i.c2simclientlib2.*;
import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Server.debugLogger;
//import edu.gmu.c4i.c2sim_wsclient2.*;

import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;
import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Server.msgNumber;
import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Server.replayLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import org.apache.logging.log4j.Logger;


/**
 * <h1>C2SIM_Server_STOMP</h1>
   Contains code for interface between C2SIM_Server and STOMP Server (Apache Apollo)
 * @author Douglas Corner - George Mason University C4I and Cyber Center
 */
public class C2SIM_Server_STOMP {

    private static Socket socket = null;
    private static final String END_OF_FRAME = "\u0000";
    static Logger debugLogger;
    static String stompHost;
    static String stompPort;
    static String topicName;
    static String topicName2;
    static Boolean publishToBoth;
    static String publishToBothS;
    private static C2SIMClientSTOMP_Lib c = null;
    BufferedReader input;
    public static String SISOSTD = "SISO-STD-C2SIM";

    /****************************/
    /*  C2SIM_Server_STOMP      */
    /****************************/
    /**
    * initialize - Perform initialization of STOMP interface
    @param props - Properties object containing switches controlling processing
    @param debugLoggerP - Logger object used to log debug information
    @throws C2SIMException 
    */
    public static void initialize(Properties props, Logger debugLoggerP) throws C2SIMException {

        /*
            The topic is expected to be /topic/BML.
        
            Filtering by STOMP server within thsi topic is accomplised by clietn-side
            e.g.  addAdvSubscription("message-selector = 'C2SIM_Report') or
            any other MessageDescriptor from C2SIM_C2SIM.identoty()
         */
        String response;
        debugLogger = debugLoggerP;
        stompHost = props.getProperty("stomp.serverHost");
        stompPort = props.getProperty("stomp.port");
        topicName = props.getProperty("stomp.topicName");
        topicName2 = props.getProperty("stomp.topicName2", "");
        publishToBothS = props.getProperty("stomp.publishToBoth", "F");

        publishToBoth = C2SIM_Util.toBoolean(publishToBothS);

        // Create the Client Object
        c = new C2SIMClientSTOMP_Lib();

        // Set the host
        c.setHost(stompHost);                

        // Set the STOMP port port
        c.setPort(stompPort);

        // Set the topic
        c.setDestination(topicName);

        // Connect to the host
        C2SIMSTOMPMessage resp;
        debugLogger.info("Connecting to STOMP Server: " + stompHost + " port: " + stompPort + " topic: " + topicName);
        try {
            resp = c.connect();
        }
        catch (C2SIMClientException e) {
            java.util.logging.Logger.getLogger(C2SIM_Server_STOMP.class.getName()).log(Level.SEVERE, null, e);

            // Error during connect print message and return
            debugLogger.error("Unable to connect to STOMP server " + e.getMessage());
            throw new C2SIMException("Unable to connect to STOMP server " + e.getMessage());

        }   // BMLCLientException   // BMLCLientException

        debugLogger.info("Response from STOMP Server: " + resp.getMessageType());

        //Are we publishing to two topics?

        // OK, return
        return;

    }   // Constructor   


    /****************************/
    /*  publishMessage          */
    /****************************/
    /**
    * publishMessage - Format and publish message to STOMP server
    @param t - C2SIM_Transaction
    @throws C2SIMException 
    */
    public static void publishMessage(C2SIM_Transaction t) throws C2SIMException {
        
        C2SIM_Server.debugLogger.info("START PUBLISH MsgNumber:" + t.msgnumber +
            " MD:" + t.messageDef.messageDescriptor);

        String msgType = "";
        Vector<String> headers = new Vector<>();
        if (t.messageDef == null) {
            msgType = "Miscellaneous";
        }
        else msgType = t.getMessageDef().messageDescriptor;

        // Build vector of headers
        headers.add("destination:" + topicName + "\n");
        headers.add("protocol:" + t.getProtocol() + "\n");
        headers.add("content-type:text/plain\n");
        headers.add("submitter:" + t.getSubmitterID() + "\n");
        headers.add("source:" + t.getSource() + "\n");
        headers.add("message-time:" + t.getMsgTime() + "\n");
        headers.add("forwarders:" + t.getForwarders() + "\n");
        if (t.getMsTemp() == null)
            headers.add("message-selector:" + t.messageDef.messageDescriptor + "\n");
        else {
            if (t.getMsTemp().equals(""))
                headers.add("message-selector:" + t.messageDef.messageDescriptor + "\n");
            else
                headers.add("message-selector:" + t.getMsTemp() + "\n");
        }
        headers.add("message-type:" + msgType + "\n");
        if (t.getProtocol().equalsIgnoreCase(SISOSTD)) {
            headers.add("message-dialect:" + SISOSTD + "\n");
            headers.add("c2sim-version:" + t.getc2SIM_Version() + "\n");
        }
        else
            headers.add("message-dialect:" + "IBML09" + "\n");
        headers.add("message-number:" + t.getMsgnumber() + "\n");

        // If this is a C2SIM Message add headers with information from C2SIM Header
        if (t.getProtocol().equals(SISOSTD)) {
            headers.add("sender:" + t.getSender() + "\n");
            headers.add("receiver:" + t.getReceiver() + "\n");
            headers.add("communicativeActTypeCode:" + t.communicativeActTypeCode + "\n");
        }   // if C2SIM
     
        // Log message being published to debug log and if not in playback
        // Log to replay log file, removing all NL's
        if(C2SIM_Command.getRecordingStatus().equals("RECORDING_IN_PROGRESS")) {
            // do not publish server replay to replayLogger
            if(!C2SIM_Server.playbackStatus.equals("PLAYBACK_RUNNING"))
                C2SIM_Server.replayLogger.info(t.getSource() + "  " + t.getMsgnumber() + "  " + 
                    t.getSubmitterID() + "  " + 
                    t.getXmlText().replaceAll("\n", "").replaceAll(">\\s*<", "><"));
        }
        // if not replayLogging sends, log to debugLog instead
        // but do not log playback to debugLogger
        else if(!C2SIM_Server.playbackStatus.equals("PLAYBACK_RUNNING")){ 
            C2SIM_Server.debugLogger.info("STOMP:" + topicName + " "+
            t.getSource() + "  " + t.getMsgnumber() + "  " + 
            t.getSubmitterID() + "  " + t.getXmlText().replaceAll("\n", "").replaceAll(">\\s*<", "><"));
        }
        try {
            // Publish the message 
            c.setDestination(topicName);
            c.publish("SEND", headers, t.getXmlText());
            
            // for debug:
            int i,s=headers.size();
            String hdr="";
            for(i=0;i<s;++i)hdr+=(headers.get(i)+"|");
            C2SIM_Server.debugLogger.info("SENDHEADER " + hdr);
            
        }   // try

        catch (C2SIMClientException e) {
            C2SIM_Server.debugLogger.error("Error while publishing: " + e.getMessage());
            throw new C2SIMException("Error while publishing: " + e.getMessage());
        }

    }   // publishMessage()


    /********************************************/
    /*  publishMessage - Document Mode          */
    /********************************************/
    /**
    * publishMessageDoc - Publish message with server in document mode.  Message has not been identified
    @param t - C2SIM_Transaction
    @throws C2SIMException 
    */
    public static void publishMessageDoc(C2SIM_Transaction t) throws C2SIMException {

        Vector<String> headers = new Vector<>();

        // Build vector of headers
        headers.add("destination:" + topicName + "\n");
        headers.add("protocol:" + t.getProtocol());
        headers.add("content-type:text/plain\n");
        headers.add("submitter:" + t.getSubmitterID() + "\n");
        headers.add("first-forwarder:" + t.getForwarders() + "\n");
        headers.add("content-length:" + new Integer(t.getXmlText().length()).toString() + "\n");
        headers.add("message-number:" + t.getMsgnumber() + "\n");

        // Log to replay log file, removing all NL's
        if(C2SIM_Command.getRecordingStatus().equals("RECORDING_IN_PROGRESS"))
        C2SIM_Server.replayLogger.info(
            "Document  " + t.getMsgnumber() + "  " + t.getSubmitterID() + "  " + 
            t.getXmlText().replaceAll("\n", "").replaceAll(">\\s*<", "><"));

        try {
            // Publish the message    
            c.publish("SEND", headers, t.getXmlText());
        }

        catch (C2SIMClientException e) {
            debugLogger.error("Error while publishing " + e.getMessage());
            throw new C2SIMException("Error while publishing " + e.getMessage());
        }

    }   // publishMessageDoc


}   // class C2SIM_Server_STOMP
