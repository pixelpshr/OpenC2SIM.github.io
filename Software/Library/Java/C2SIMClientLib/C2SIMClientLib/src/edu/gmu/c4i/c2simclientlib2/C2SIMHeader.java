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

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;


/**
 *
 * @author Douglas Corner - George Mason University C4I Center
 */
public class C2SIMHeader {
    
    static String SISOSTD = "SISO-STD-C2SIM";

    String c2simFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    SimpleDateFormat sdf = new SimpleDateFormat(c2simFormat);

    protected static final String XML_PREAMBLE = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    // Instance variables
    /**
     * CommunicativeActTypeCode - Communicative act; one of the values 
     * from enumCommunicativeActCategoryCode indicating type of message
     */
    String communicativeActTypeCode = "";

    /**
     * securityClassificationCode -  Indicates the security 
     * classification of this message
     */
    String securityClassificationCode = "";

    /**
     * conversationID - Unique identifier for the conversation. 
     * Should be kept identical for all replies.<BR> 
     *   The conversationID may be used to associate a number of 
     * messages into a logical grouping.
     */
    String conversationID = "";

    /**
     * messageID - Unique identifier for the message. Other messages refer to the message using this ID. 
     */
    String messageID = "";

    /**
     * protocol - The protocol of the this message
     */
    String protocol = SISOSTD;

    /**
     * protocolVersion - The version of the protocol of the this message
     */
    String protocolVersion = "1.0.1";

    /**
     * - replyToSystem - Specifies what system to reply to.<BR>
     */
    String replyToSystem = "";

    /**
     * Sending Time - ISO Date format yyyy-MM-ddTHH:mm:ssZ
     */
    String sendingTime = sdf.format(new Date());

    /**
     * fromSendingSystem - ID of sending system (UUID)
     */
    String fromSendingSystem = "";

    /**
     * toReceivingSystem -  ID of destination system (UUID))
     */
    String toReceivingSystem = "";

    /**
     * inReplyToMessageID - ID of message being replied to (UUID)
     */
    String inReplyToMessageID = "";

    /********************/
    /*  insertC2SIM     */
    /********************/
    /**
     * Given an xml document with the XML header and content create and insert a C2SIM header into it
    @param xml - The xml string that header is to be inserted into
    @param sender - The C2SIM Sender
    @param receiver - The C2SIM Receiver
    @param performative - One of the C2SIM performatives <BR>
        Possible values for CommunicativeActTypeCode are: Inform, Confirm, Refuse, Accept, Agree, Request
    @return - The updated XML string with the header inserted
     */
    public static String insertC2SIM(String xml, String sender, String receiver, String performative, String version) {

        // Create a new C2SIMHeader and add the parameters passed to this routine
        C2SIMHeader c2s = new C2SIMHeader();
        c2s.setFromSendingSystem(sender);
        c2s.setToReceivingSystem(receiver);
        if (!performative.equals(""))
            c2s.setCommunicativeActTypeCode(performative);
        else
            c2s.setCommunicativeActTypeCode("Inform");
        
        // Add C2SIM Version
        c2s.setProtocolVersion(version);

        // Add Messge ID and Conversation ID to the header
        c2s.generateConversationID();
        c2s.generateMessageID();

        // We need to construct a C2SIM message, XMLPreamble + C2SIMHeader + XML
        // Get the C2SIM Header in xml text
        String temp = "";
        String c2sHeader = "";
        String newXML;

        c2sHeader = c2s.toXMLString();

        // Is the XML preamble present?
        if (xml.startsWith("<?xml version")) {
            // Locate the actual beginning of the xml (Find the second "<")
            int start = xml.indexOf("?>") + "?>".length();

            // Get the xml after the XML preamble
            temp = xml.substring(start);
        }   // xml preamble was present
        else
            temp = xml;

        // Now build the message and frame with with C2SIM_Message
        newXML = XML_PREAMBLE + 
            "<Message xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" + 
            c2sHeader + temp + "</Message>";

        return newXML;

    }   // insertC2SIM


    /********************/
    /* removeC2SIM      */
    /********************/
    /**
     * Remove C2SIM message envelope and return core xml message with xml header
    @param xml - String - Input xml message with C2SIM envelop
    @return String - Reconstructed message without C23SIM components
     */
    public static String removeC2SIM(String xml) {
        String msg = "";
        
        // Get rid of NL
        String temp = xml.replaceAll("\n", "");
        
        int start = xml.indexOf("</C2SIMHeader>") + "</C2SIMHeader>".length();
        int end = xml.indexOf("</Message>");
        msg = XML_PREAMBLE + xml.substring(start, end);
        return msg;

    }   // removeC2SIM()


    /*************/
    /*  xmlDoc   */
    /*************/
    /**
     *   xmlDoc - return a DOM Document representing this message header <BR>
     *   NOT IMPLEMENTED AT THIS TIME
     * @return null
     */
    public Document toDoc() {
        return null;
    }


    /**
     * Populate a C2SIMHeader object from an XML string.
     * @param xmlString - 
     * @return The C2SIMHeader object that was passed as a parameter
     * @throws C2SIMClientException - Thrown for one of the following reasons <BR>
     *  IOException in SAXBuilder
     *  JDOMException
     */
    public static C2SIMHeader populateC2SIM(String xmlString) 
        throws C2SIMClientException {

        SAXBuilder sb;
        Document c2sDoc;
        C2SIMHeader c2s = new C2SIMHeader();

        // Parse the message
        try {
            sb = new SAXBuilder();
            c2sDoc = sb.build(new StringReader(xmlString));
        }
        catch (IOException i) {
            throw new C2SIMClientException(
                "IO Exception creating new StringReader for SAXBuilder", i);
        }
        catch (JDOMException j) {
            throw new C2SIMClientException(
                "JDOM Exception while parsing input BML", j);
        }

        Namespace c2simNS = Namespace.getNamespace(
            "http://www.sisostds.org/schemas/C2SIM/1.1");

        /*
            Work through the parsed document and build a C2SIMHeader object
         */
        // Get the root element
        Element root = c2sDoc.getRootElement();
        Element header = root.getChild("C2SIMHeader", c2simNS);
        if(header == null)
            throw new C2SIMClientException("error in C2SIM header parameter");

        // get incoming header values and set corresponding in new header
        String temp;
        temp = header.getChildText("CommunicativeActTypeCode", c2simNS);
        if (!(temp == null))
            c2s.setCommunicativeActTypeCode(temp);

        temp = header.getChildText("ConversationID", c2simNS);
        if (!(temp == null))
            c2s.setConversationID(temp);
        
        temp = header.getChildText("MessageID", c2simNS);
        if (!(temp == null))
            c2s.setMessageID(temp);        
        
        temp = header.getChildText("Protocol", c2simNS);
        if (!(temp == null))
            c2s.setProtocol(temp);
        
        temp = header.getChildText("ProtocolVersion", c2simNS);
        if (!(temp == null))
            c2s.setProtocolVersion(temp);
        
        temp = header.getChildText("ReplyToSystem", c2simNS);
        if (!(temp == null))
            c2s.setReplyToSystem(temp);

        
        temp = header.getChildText("SendingTime", c2simNS);
        if (!(temp == null))
            c2s.setSendingTime(temp);
        
        temp = header.getChildText("FromSendingSystem", c2simNS);
        if (!(temp == null))
            c2s.setFromSendingSystem(temp);

        temp = header.getChildText("ToReceivingSystem", c2simNS);
        if (!(temp == null))
            c2s.setToReceivingSystem(temp);
        
        temp = header.getChildText("InReplyToMessageID", c2simNS);
        if (!(temp == null))
            c2s.setInReplyToMessageID(temp);
        
        return c2s;
    }


    /********************/
    /*  toXMLString     /*     
    /********************/
    /**
     * toXMLString
     * @return - XML String containing the contents of this C2SIM header object
     */
    public String toXMLString() {


        String xmlString = "<C2SIMHeader>";

        // CommunicativeActTypeCode
        if (!communicativeActTypeCode.equals(""))
            xmlString += "<CommunicativeActTypeCode>" + 
                communicativeActTypeCode + "</CommunicativeActTypeCode>";


        // SecurityClassificationCode
        if (!securityClassificationCode.equals(""))
            xmlString += "<SecurityClassificationCode>" + 
                securityClassificationCode + "</securityClassificationCode>";

        // ConversationID
        if (!conversationID.equals(""))
            xmlString += "<ConversationID>" + conversationID + "</ConversationID>";

        // messageID
        if (!messageID.equals(""))
            xmlString += "<MessageID>" + messageID + "</MessageID>";

        // protocol
        if (!protocol.equals(""))
            xmlString += "<Protocol>" + protocol + "</Protocol>";

        // ProtocolVerion
        if (!protocolVersion.equals(""))
            xmlString += "<ProtocolVersion>" + protocolVersion + "</ProtocolVersion>";

        // ReplyToSystem
        if (!replyToSystem.equals(""))
            xmlString += "<ReplyToSystem>" + replyToSystem + "</ReplyToSystem>";

        // SendingTime
        if (!sendingTime.equals(""))
            xmlString += "<SendingTime>" + sendingTime + "</SendingTime>";
        
        // FromSendingSystem
        if (!fromSendingSystem.equals(""))
            xmlString += "<FromSendingSystem>" + 
                fromSendingSystem + "</FromSendingSystem>";

        // ToReceivingSystem
        if (!toReceivingSystem.equals(""))
            xmlString += "<ToReceivingSystem>" + 
                toReceivingSystem + "</ToReceivingSystem>";
        
        // InReplyTo - Message ID
        if (!inReplyToMessageID.equals(""))
            xmlString += "<InReplyToMessageID>" + 
                inReplyToMessageID + "</InReplyToMessageID>";

        xmlString += "</C2SIMHeader>";

        return xmlString;

    }   // toXMLString


    /*
     *     Getters and Setters are implemented for all instance variables.
     */
    /**
     * getCommunicativeActTypeCode - Get the value of the CommunicativeActTypeCode property - Indicates processing requested for this message
    @return - String - Possible values: Inform, Confirm, Refuse, Accept, Agree, Request
     */
    public String getCommunicativeActTypeCode() {
        return communicativeActTypeCode;
    }


    /**
     * setCommunicativeActTypeCode - Set value of CommunicativeActTypeCode 
     * property in current C2SIMHeader
    @param communicativeActTypeCode - String - Possible values see getPerformative()
     */
    public void setCommunicativeActTypeCode(String communicativeActTypeCode) {
        this.communicativeActTypeCode = communicativeActTypeCode;
    }


    /**
     * setSecurityClassificationCode - Set field used to connect a series 
     * of messages into one conversation
    @param securityClassificationCode - String UUID  C2SIM securityClassificationCode . 
     */
    public void setSecurityClassificationCode(String securityClassificationCode) {
        this.securityClassificationCode = securityClassificationCode;
    }


    /**
     * getSecurityClassificationCode - Get field used to connect a series of messages into one conversation
    @return - securityClassificationCode - String UUID 
     */
    public String getSecurityClassificationCode() {
        return securityClassificationCode;
    }


    /**
     * setConversationID - Set field used to connect a series of messages into 
     * one conversation
    @param conversationID - String UUID  C2SIM conversationID . 
     */
    public void setConversationID(String conversationID) {
        this.conversationID = conversationID;
    }


    /**
     * getConversationID - Get field used to connect a series of messages into 
     * one conversation
    @return - conversationID - String UUID 
     */
    public String getConversationID() {
        return conversationID;
    }


    /**
     * generateConversationID - Generate a new Conversation ID (UIID format) 
     * for this C2SIM Header
     */
    public void generateConversationID() {
        conversationID = UUID.randomUUID().toString();
    }


    /**
     * getMessageID - Get ID of message using current C2SIM header
    @return messageID = String - UUID
     */
    public String getMessageID() {
        return messageID;
    }


    /**
    ( setMessageID - Set the message ID of the message using this C2SIM Header
    @param messageID String - UUID 
     */
    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }


    /**
     * generateMessageID - Generate ID for this C2SIM Message Header
     */
    public void generateMessageID() {
        messageID = UUID.randomUUID().toString();
    }


    /**
     * setProtocol - Set field used to indicate the protocol of the message
    @param protocol - String UUID  C2SIM protocol . 
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    /**
     * getProtocol - Get field used to indicate the protocol of the message
    @return - protocol - String UUID 
     */
    public String getProtocol() {
        return protocol;
    }


    /**
     * setProtocolVersio - Set field used to indicate the version of the 
     * protocol of the message
    @param protocolVersion - String  C2SIM protocol version . 
     */
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }


    /**
     * getProtocolVersion - Get field used to indicate the version of the 
     * protocol of the message
    @return - protocolVersion - String 
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }


    /**
     * getReplyTo = Get replyTo property indicating what system is to be replied to
    @return - System Identification - String
     */
    public String getReplyToSystem() {
        return replyToSystem;
    }


    /**
     * setReplyToSystem - Set replyTo property indicating what system is to be replied to
    @param replyTo - System to be replied to
     */
    public void setReplyToSystem(String replyTo) {
        this.replyToSystem = replyTo;
    }


    /**
     * getSendingTime - ISO Format time when C2SIMHeader object was instantiated
    @return String (ISO Time Format) = Sending time
     */
    public String getSendingTime() {
        return sendingTime;
    }

    /**
     * setSendingTime - Set sendingTime = ISO Format
    @param sendingTime  - Time message header was created 
     */
    public void setSendingTime(String sendingTime) {
        this.sendingTime = sendingTime;
    }
    
    /**
     * getFromSendingSystem - Get ID of system originating this message
    @return - String System ID UUID format
     */
    public String getFromSendingSystem() {
        return fromSendingSystem;
    }


    /**
     * setFromSendingSystemo - Set ID of system originating this message
    @param fromSendingSystem- ID in UUID format
     */
    public void setFromSendingSystem(String fromSendingSystem) {
        this.fromSendingSystem = fromSendingSystem;
    }


    /**
     * getToReceivingSystem - Get ID of system this is message is directed to
    @return - String System ID UUID format
     */
    public String getToReceivingSystem() {
        return toReceivingSystem;
    }


    /**
     * setToReceivingSystem - Set ID of system to receive this message
    @param toReceivingSystem- ID in UUID format
     */
    public void setToReceivingSystem(String toReceivingSystem) {
        this.toReceivingSystem = toReceivingSystem;
    }


    /**
     * getInReplyToMessageID - Get ID message being replied to
    @return - String System ID UUID format
     */
    public String getInReplyToMessageID() {
        return inReplyToMessageID;
    }


    /**
     * setInReplyToMessageID - Set ID messge being replied to
    @param inReplyToMessageID- ID in UUID format
     */
    public void setInReplyToMessageID(String inReplyToMessageID) {
        this.inReplyToMessageID = inReplyToMessageID;
    }


}   // C2SIMHeader Class
