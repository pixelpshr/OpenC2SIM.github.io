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

import edu.gmu.c4i.c2simclientlib2.*;

import edu.gmu.c4i.c2simserver4.schema.C2SIMDB;
import edu.gmu.c4i.c2simserver4.schema.*;

import java.io.StringReader;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
//import edu.gmu.c4i.bmlserver4.bmlserver.C2SIM_Server.SessionState_Enum;
//import edu.gmu.c4i.bmlserver4.bmlschema.C2SIMSchemaProcess;
//import edu.gmu.c4i.bmlserver4.bmlschema.C2SIMMessageDefinition;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import org.jdom2.output.XMLOutputter;
import org.apache.logging.log4j.*;
import org.jdom2.JDOMException;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.Format;


/**
 * <h1>C2SIM_ServerProcess</h1> <p>
 * Performs processing of BML messages
 *  Functions
 *      If document mode, just publish the message as received and return 
 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_ServerProcess {
    public static String SISOSTD = "SISO-STD-C2SIM";
    
    final static String schemaFolder = System.getenv("BML_HOME") + "/schema/flattenedSchemas/";
    final static String dbFolder = System.getenv("BML_HOME");

    static C2SIMDB db;
    static C2SIMSchemaProcess proc;       // Used to read BMLDB

    static C2SIMSchemaProcess bmlSchema;

    // Processing modes from properties files
    static Boolean justDocumentMode = false;
    static Boolean justParseDocument = false;
    static Boolean justIdentifyMessage = true;

    static Boolean translateOrders = false;
    static Boolean translateReports = false;
    static Boolean captureUnitPosition = false;

    /********************/
    /*  initialize      */
    /********************/
    
    static {

        // Determine processing modes

        justDocumentMode = C2SIM_Util.toBoolean(C2SIM_Server.props.getProperty("server.justDocumentMode"));
        justParseDocument = C2SIM_Util.toBoolean(C2SIM_Server.props.getProperty("server.justParseDocument"));
        justIdentifyMessage = C2SIM_Util.toBoolean(C2SIM_Server.props.getProperty("server.justIdentifyMessage"));
        captureUnitPosition = C2SIM_Util.toBoolean(C2SIM_Server.props.getProperty("server.CaptureUnitPosition"));

        // Log processing modes
        C2SIM_Server.debugLogger.info("justDocument Mode = " + justDocumentMode.toString());
        C2SIM_Server.debugLogger.info("justParseDocument = " + justParseDocument.toString());
        C2SIM_Server.debugLogger.info("justIdentifyMessage " + justIdentifyMessage.toString());

        C2SIM_Server.debugLogger.info("Class Initialization complete");
    }   // static block


    /************************/
    /*  processMessage      */
    /************************/
    /**
    * processMessage - Main processing method for XML message
    @param trans - C2SIM_Transaction contain message and other information
    @throws C2SIMException 
    */
    public static void processMessage(C2SIM_Transaction trans) throws C2SIMException {

        C2SIMMessageDefinition thisMessage = null;
        List<C2SIMMessageDefinition> otherMessages = new ArrayList<>();
        XMLOutputter xmlOut;
        String newXML;
        SAXBuilder sb;
        Document bmlDoc = null;
        String msgSelector;

        String rootElement;


        if ((trans.getProtocol().equalsIgnoreCase(SISOSTD)) && !(trans.getXmlText().contains("<Message"))) {
            C2SIM_Server.debugLogger.error("Protocol = C2SIM but XML root element is not <Message>");
            throw new C2SIMException("Protocol is C2SIM but XML root element is not <Message>");
        }


        if (justDocumentMode) {
            doDocumentMode(trans);
            return;
        }

        if (justParseDocument) {
            doParseMessage(trans);
            return;
        }

        if (justIdentifyMessage) {
            doIdentifyMessage(trans);
            return;
        }

        processManually(trans);
        return;

    }   // processMessage()


    /********************/
    /* processManually  */
    /********************/
    /**
    * processManually - Message is to be fully processed rather than document mode.
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    static void processManually(C2SIM_Transaction trans) throws C2SIMException {

        // Parse and Identify the core message without the headers
        if (trans.protocol.equalsIgnoreCase(SISOSTD)) {

            // Get the C2SIM Header 
            C2SIMHeader ch = null;
            String x = trans.getXmlText();
            
            try {
                ch = C2SIMHeader.populateC2SIM(x);
            }
            catch (C2SIMClientException e) {
                throw new C2SIMException("Exception while parsing C2SIM Header", e);
            }
            
            // Check protocol version in message - Version 1.1 was the default through CWIX19 and through the version of C2SIM that went to ballot
            String c2simV = ch.getProtocolVersion();
            trans.setc2SIM_Version(c2simV);
            
            // Save xml without C2SIM Header
            trans.setXmlMsg (C2SIMHeader.removeC2SIM(trans.getXmlText()));
            
            // Parse message
            trans.doc = C2SIM_Mapping.parseMessage(trans.getXmlMsg());
            
        }   // if C2SIM

        else if (trans.protocol.equalsIgnoreCase("BML"))
            trans.doc = C2SIM_Mapping.parseMessage(trans.getXmlText());

        else
            throw new C2SIMException("Unknown protocol " + trans.getProtocol());

        // To identify C2SIM Messages we must go into Tasks or individual Reports
        if (trans.protocol.equalsIgnoreCase(SISOSTD))
            trans.messageDef = C2SIM_C2SIM.identify(trans.doc);
        else
            trans.messageDef = C2SIM_Mapping.identifyMessage(trans.doc);

        // If we can't identify the message we don't know how to process it.
        if (trans.messageDef == null) {
            C2SIM_Server.debugLogger.info("Message number; " + trans.getMsgnumber() + " from submitter: " + trans.getSubmitterID() + " Can't identify message type");
            throw new C2SIMException("Unable to identify message");
        }   // can't identify message

        // We have identified the message.  Log to debug log
        C2SIM_Server.debugLogger.info("Message Number; " + trans.getMsgnumber()
                + " from submitter: " + trans.getSubmitterID()
                + " Identified as: " + trans.messageDef.messageDescriptor);

        // Is this message type inactive?
        if (trans.messageDef.inactive)
            return;

        // Are we doing simlated cyber attack?
        if (C2SIM_Server.cyberAttack) {

            String result;
            result = C2SIM_Cyber.cyberProcessMessage(trans);
            if (result.equalsIgnoreCase("DROP"))
                return;
        }   // cyberAttack true

        // Check if this type of message is permitted with server in this state        
        checkMessageState(trans);

        /*
        // Examine the dialect or messageDescriptor field and pass to the proper class.process() 
         */
        // MSDL
        if (trans.messageDef.dialect.equalsIgnoreCase("MSDL")) {
            C2SIM_MSDL.process(trans);
            return;
        }   // MSDL

        // IBML009
        if (trans.messageDef.dialect.equalsIgnoreCase("IBML09")) {
            C2SIM_IBML09.process(trans);
            return;
        }   // IBML09

        // CBML
        if (trans.messageDef.dialect.equalsIgnoreCase("CBML_Light")) {
            C2SIM_CBML.process(trans);
            return;
        }   // IBML09     

        // C2SIM
        if (trans.messageDef.dialect.equalsIgnoreCase("C2SIM")) {
            C2SIM_C2SIM.process(trans);
            return;
        }   // C2SIM

        // Log information about received message in debug Log
        C2SIM_Server.debugLogger.info("Message number; " + trans.getMsgnumber() + " from submitter: "
                + trans.getSubmitterID() + "  Identified As = " + trans.getMessageDef().messageDescriptor + " Translation");


    }   // processManually()


    /********************/
    /* doDocumentMode   */
    /********************/
    /**
    * doDocumentMode - Simply publish all messages without processing.  
    @param trans - C2SIM_Transaction containing message
    @throws C2SIMException 
    */
    static void doDocumentMode(C2SIM_Transaction trans) throws C2SIMException {
        // If justDocumentMode is true then log and publish the document as received with no further processing

        C2SIM_Server.debugLogger.info("Message number; " + trans.getMsgnumber() + " from submitter: " + trans.getSubmitterID() + " Document Mode");
        C2SIM_Server_STOMP.publishMessage(trans);
        return;
        // DoDocument Mode    
    }


    /************************/
    /* doParseMessage   */
    /************************/
    /**
    * doParseMessage - Parse message to insure that it is well formed XML and publish
    *   No further processing
    @param trans - C2SIM_Transaction containing message
    @throws C2SIMException 
    */
    static void doParseMessage(C2SIM_Transaction trans) throws C2SIMException {
        // If justParseDocument is true then parse the xml with JDOM, to be sure it is well formed, log, and publish it  

        // parseMessage() will throw an exception if there is anything wrong.
        Document bmlDoc = C2SIM_Mapping.parseMessage(trans.getXmlText());

        C2SIM_Server.debugLogger.info("Message number; " + trans.getMsgnumber() + " from submitter: " + trans.getSubmitterID() + " Document Mode");

        // Publish message as it came in
        C2SIM_Server_STOMP.publishMessage(trans);

        return;
    }   // doParseMessage())


    /***************************/
    /* doIdentifyMessage   */
    /***************************/
    /**
    * doIdentifyMessage - Only parse, identify and publish message.  No further processing
    @param trans
    @return
    @throws C2SIMException 
    */
    static C2SIMMessageDefinition doIdentifyMessage(C2SIM_Transaction trans) throws C2SIMException {

        // If justIdentifyMessage is true then parse it with JDOM, determine the message type, log, and publish 

        Document bmlDoc = null;
        C2SIMMessageDefinition thisMessage = null;
        String msgSelector = "";

        // If this is a C2SIM message then remove the C2SIM envelope
        if (trans.getProtocol().equalsIgnoreCase(SISOSTD))
            bmlDoc = C2SIM_Mapping.parseMessage(C2SIMHeader.removeC2SIM(trans.getXmlText()));
        else
            bmlDoc = C2SIM_Mapping.parseMessage(trans.getXmlText());

        // Add parsed document to transaction
        trans.setDocument(bmlDoc);

        // Identify the message with the schema tables
        thisMessage = C2SIM_Mapping.identifyMessage(bmlDoc);

        // Set the message definition in the current transaction
        trans.setMessageDef(thisMessage);

        // If we can't identify write a log message
        if (thisMessage == null) {
            C2SIM_Server.debugLogger.info("Message number; " + trans.getMsgnumber() + " from submitter: " + trans.getSubmitterID() + " Can't identify message type");
            return null;
        }

        C2SIM_Server.debugLogger.info("Message number; " + trans.getMsgnumber() + " from submitter: " + trans.getSubmitterID() + " MessageSelector: " + trans.getMessageDef().messageDescriptor);

        // Should this message be published?;
        if (thisMessage.inactive || thisMessage.processManually)
            return thisMessage;

        // Publish the message as received;
        C2SIM_Server_STOMP.publishMessage(trans);
        return thisMessage;

    }   // doIdentifyMessage()


    /************************/
    /* checkMessageState    */
    /************************/
    /**
    * checkMessageState - Check that this type of message is permitted in current server state
    *   Throw exception if server state and message type don't agree
    @param t - C2SIM_Transaction
    @throws C2SIMException 
    */
    /*
        Check that this type of message is permitted in current server state
     */
    static void checkMessageState(C2SIM_Transaction t) throws C2SIMException {

        C2SIM_Server.SessionState_Enum state = C2SIM_Server.sessionState;
        String msgType = t.getMessageDef().messageType;

        if ((state == C2SIM_Server.SessionState_Enum.UNINITIALIZED)
                || (state == C2SIM_Server.SessionState_Enum.INITIALIZED)
                || (state == C2SIM_Server.SessionState_Enum.PAUSED)) {
            C2SIM_Server.debugLogger.error("Messages not permitted in server state: " + state.toString());
            throw new C2SIMException("Messages not permitted in server state: " + state.toString());
        }

        if ((state == C2SIM_Server.SessionState_Enum.INITIALIZING) && (!msgType.equalsIgnoreCase("INITIALIZATION"))) {
            C2SIM_Server.debugLogger.error("Only INITIALIZATION messages are permitted in server state " + state.toString());
            throw new C2SIMException("Only INITIALIZATION messages are preermitted in server state " + state.toString());
        }
        if ((state == C2SIM_Server.SessionState_Enum.RUNNING) && (msgType.equalsIgnoreCase("INITIALIZATION"))) {
            C2SIM_Server.debugLogger.error("INITIALIZATION messages are not permitted in server state " + state.toString());
            throw new C2SIMException("INITIALIZATION messages are not permitted in server state " + state.toString());
        }
        // Everything seems OK, Return
    }   // checkMessageState()


}   // class C2SIM_ServerProcess
