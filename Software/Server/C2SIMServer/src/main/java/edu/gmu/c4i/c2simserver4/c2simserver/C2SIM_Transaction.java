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
 *-----------------------------------------------------------------*/
package edu.gmu.c4i.c2simserver4.c2simserver;

import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;
import org.jdom2.Document;
import org.jdom2.Element;

 
/**
 * <h1>C2SIM_Transaction</h1>
 * Class to hold WS parameters and other variables that are passed between modules
 *  Avoids the use of long parameter lists
 * @author Douglas Corner - George Mason University C4I and Cyber Center
 */
public class C2SIM_Transaction implements Cloneable {

    // Instance Variables
    String submitterID;
    String source;
    String protocol;
    String sender;
    String receiver;
    String communicativeActTypeCode;
    String conversationid;
    String forwarders;
    Integer msgnumber;
    String msgTime;
    String xmlText;
    String xmlMsg;      // C2SIM Message text without header
    C2SIMMessageDefinition messageDef;
    Document doc;
    Document coreDoc;
    Element cyberReport;    // Hold single report for cyber processing
    String msTemp;      // Temporary MessageSelector for publishing
    String c2SIM_Version;

    /**************************************/
    /* C2SIM_Transaction - Constructor    */
    /**************************************/
    /**
    * C2SIM_Transaction - Data only class contains single C2SIM Message and associated data
    @param bml  - XML Message
    @param submitterID - Identification of submitter
    @param protocol - Protocol submitted by client
    @param sender - C2SIM ID of sender
    @param receiver - C2IM ID of receiver
    @param communicativeActTypeCode - C2SIM CommunicativeActCode
    @param forwarders - String containing list of IP addresses of servers that have handled this message
    */
    C2SIM_Transaction(String bml, String submitterID, String protocol, String sender, 
        String receiver, String communicativeActTypeCode, String forwarders) {

        // Add parameters to object
        this.submitterID = submitterID;
        this.protocol = protocol;
        this.sender = sender;
        this.receiver = receiver;
        this.communicativeActTypeCode = communicativeActTypeCode;
        if (forwarders == null)
            this.forwarders = "";
        else
            this.forwarders = forwarders;
        this.xmlText = bml;
        msTemp = "";


    }   // constructor    

    
    /****************************************************/
    /* C2SIM_Transaction Constructor - No parameters    */
    /****************************************************/
    /**
        C2SIM_Transaction - Constructor with no parameters.
    */
    
    C2SIM_Transaction() {

    }


    /****************************************************/
    /* clone() for making a copy of a C2SIM_Transaction    */
    /****************************************************/
    /**
    * clone - Create clone of C2SIM_Transaction object
    @return - Complete copy of current object
    */
    public C2SIM_Transaction clone() {
        C2SIM_Transaction t = null;
        try {
            t = (C2SIM_Transaction) super.clone();
        }
        catch (CloneNotSupportedException c) {
            System.out.println("Clone not implemented in super.clone()");
        }
        return t;
    }


    /************************/
    /* Getters and Setters  */
    /************************/

    /**
    * getSubmitterID 
    @return submitterID
    */
    public String getSubmitterID() {
        return submitterID;
    }

    /**
    * setSubmitterID
    @param submitterID 
    */
    public void setSubmitterID(String submitterID) {
        this.submitterID = submitterID;
    }

    /**
    * get c2SIM_Version
    @return c2SIM_Version
    */
    public String getc2SIM_Version() {
        return c2SIM_Version;
    }

    /**
    * setC2SIM_Version
    @param c2SIM_Version 
    */
    public void setc2SIM_Version(String c2SIM_Version) {
        this.c2SIM_Version = c2SIM_Version;
    }    

    public String getProtocol() {
        return protocol;
    }

    /**
    * setProtocol
    @param protocol 
    */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
    * getSender
    @return sender
    */
    public String getSender() {
        return sender;
    }

    /**
    * setSender
    @param sender 
    */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
    * getReceiver
    @return receiver
    */
    public String getReceiver() {
        return receiver;
    }

    /**
    * setReceiver
    @param receiver 
    */
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    /**
    * getCommunicativeActTypeCode
    @return communicativeActTypeCode
    */
    public String getCommunicativeActTypeCode() {
        return communicativeActTypeCode;
    }

    /**
    * setCommunicativeActTypeCode
    @param communicativeActTypeCode 
    */
    public void setCommunicativeActTypeCode(String communicativeActTypeCode) {
        this.communicativeActTypeCode = this.communicativeActTypeCode;
    }

    /**
    * getConversationid
    @return conversationid
    */
    public String getConversationid() {
        return conversationid;
    }

    /**
    * setConversationid
    @param conversationid 
    */
    public void setConversationid(String conversationid) {
        this.conversationid = conversationid;
    }

    /**
    * getForwarders
    @return forwarders
    */
    public String getForwarders() {
        return forwarders;
    }

    /**
    * setForwarders
    @param forwarders 
    */
    public void setForwarders(String forwarders) {
        this.forwarders = forwarders;
    }

    /**
    * getXmlText
    @return xmlText
    */
    public String getXmlText() {
        return xmlText;
    }

    /**
    * setXmlText
    @param xmlText 
    */
    public void setXmlText(String xmlText) {
        this.xmlText = xmlText;
    }

    /**
    * getXmlMsg
    @return xmlMsg
    */
    public String getXmlMsg() {
        return xmlMsg;
    }

    /**
    * setXmlMsg
    @param xmlMsg 
    */
    public void setXmlMsg(String xmlMsg) {
        this.xmlMsg = xmlMsg;
    }    

    /**
    * getMsgnumber
    @return msgnumber
    */
    public Integer getMsgnumber() {
        return msgnumber;
    }

    /**
    * setMsgnumber
    @param msgnumber 
    */
    public void setMsgnumber(Integer msgnumber) {
        this.msgnumber = msgnumber;
    }

    /**
    * getMsgTime
    @return msgTime
    */
    public String getMsgTime() {
        return msgTime;
    }

    /**
    * setMstTime
    @param msgTime 
    */
    public void setMsgTime(String msgTime) {
        this.msgTime = msgTime;
    }

    /**
    getMessageDef
    @return messageDef
    */
    public C2SIMMessageDefinition getMessageDef() {
        return messageDef;
    }

    /**
    * setMessageDef
    @param messageDef 
    */
    public void setMessageDef(C2SIMMessageDefinition messageDef) {
        this.messageDef = messageDef;
    }

    /**
    * getDocument
    @return doc
    */
    public Document getDocument() {
        return doc;
    }

    /**
    * setDocument
    @param doc 
    */
    public void setDocument(Document doc) {
        this.doc = doc;
    }

    /**
    * getCoreDocument
    @return coreDoc
    */
    public Document getCoreDocument() {
        return coreDoc;
    }

    /**
    * setCoreDocument
    @param coreDoc 
    */
    public void setCoreDocument(Document coreDoc) {
        this.coreDoc = coreDoc;
    }

    /**
    * getMsTemp
    @return msTemp
    */
    public String getMsTemp() {
        return msTemp;
    }

    /**
    * setMsTemp
    @param msTemp 
    */
    public void setMsTemp(String msTemp) {
        this.msTemp = msTemp;
    }

    /**
    * getSource
    @return source
    */
    public String getSource() {
        return source;
    }

    /**
    * setSource
    @param source 
    */
    public void setSource(String source) {
        this.source = source;
    }


}   // class C2SIM_Transaction

