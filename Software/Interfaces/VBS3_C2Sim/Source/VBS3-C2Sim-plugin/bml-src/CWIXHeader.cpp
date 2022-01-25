/*----------------------------------------------------------------*
|    Copyright 2001-2019 Networking and Simulation Laboratory     |
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
#pragma once
#include "stdafx.h"
#include "C2SIMClientLib.h"
#include <boost/uuid/uuid.hpp>            // uuid class
#include <boost/uuid/uuid_generators.hpp> // generators
#include <boost/uuid/uuid_io.hpp>         // streaming operators etc.
#include <boost/lexical_cast.hpp>

/**
* CWIXHeader - Object containing CWIX header information. <BR>
*  This data may have been extracted from the CWIX header in an incoming XML document <BR>
*      or may be used to build the CWIX header before creating a CWIX message to be sent
* @author Douglas Corner - George Mason University C4I and Cyber Center
*/

CWIXHeader::CWIXHeader()
{
}

	/********************/
	/*  insertCWIX     */
	/********************/
	/**
	* Given an xml document with the XML header and content create and insert a CWIX header into it
	@param xml - The xml string that header is to be inserted into
	@param sender - The CWIX Sender
	@param receiver - The CWIX Receiver
	@param performative - One of the CWIX performatives <BR>
	Possible values for performative are: Inform, Confirm, Refuse, Accept, Agree, Request
	@return - The updated XML string with the header inserted
	*/
	std::string CWIXHeader::insertCWIX(std::string xml, std::string sender, std::string receiver, std::string performative) {

		// Create a new CWIXHeader and add the parameters passed to this routine
		CWIXHeader* cwx = new CWIXHeader();
		cwx->setFromSendingSystem(sender);
		cwx->setToReceivingSystem(receiver);
		if (performative != "")
			cwx->setPerformative(performative);
		else
			cwx->setPerformative("Inform");

		// Add Message ID and Conversation ID to the header
		cwx->generateConversationID();
		cwx->generateMessageID();

		// We need to construct a CWIX message, XMLPreamble + CWIXHeader + XML
		// Get the CWIX Header in xml text
		std::string temp = "";
		std::string cwxHeader = "";
		std::string newXML;

		cwxHeader = cwx->toXMLString();

		// Is the XML preamble present?
		if (xml.compare("<?xml version") == 0) {
			// Locate the actual beginning of the xml (Find the second "<")
			size_t start = xml.find("<", 1);

			// Get the xml after the XML preamble
			temp = xml.substr(start);
		}   // xml preamble was present
		else
			temp = xml;

		// Now build the message and frame with with C2SIM_Message
		newXML = 
			XML_PREAMBLE + "<Message xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" + 
			cwxHeader + temp + "</Message>";

		return newXML;

	}   // insertCWIX

	/*******************************/
	/* findStartTag and findEndTag */
	/*******************************/

	// next two functions only work when findEndTag is called
	// with same tag and immediately after findStartTag
	// also they ignore any namespace prefix that is present

	// look for start of opening tag, which might have a namespace prefix
	size_t CWIXHeader::findStartTag(std::string xml, std::string tag, size_t offset){
		startPosition = xml.find("<" + tag, offset);
		if (startPosition != std::string::npos)return startPosition;
		startPosition = xml.find(":" + tag, offset);
		return startPosition;
	}

	// look for end of closing tag, which might have a namespace prefix
	// and must come after startPosition
	size_t CWIXHeader::findEndTag(std::string xml, std::string tag, size_t offset){
		size_t endPosition = xml.find("</" + tag + ">", offset);
		if (endPosition != std::string::npos)return endPosition + tag.length() + 3;
		endPosition = xml.find(":" + tag + ">", startPosition + 1);
		if (endPosition == std::string::npos)return endPosition;
		return endPosition + tag.length() + 2;
	}

	/********************/
	/* removeCWIX      */
	/********************/

	/**
	* Remove CWIX message envelope and return core xml message with header
	@param xml - String - Input xml message with C2SIM envelop
	@return String - Reconstructed message without C23SIM components
	*/
	std::string CWIXHeader::removeCWIX(std::string xml) {
		size_t headerStart = 0, bodyStart = 0;
		size_t headerEnd = 0, bodyEnd = 0;
		std::string msg;

		// verify we are scanning XML
		if (xml.compare(0, 5, "<?xml") == 0){

			// find start and end of C2SIM Header
			headerStart = findStartTag(xml, "C2SIMHeader", 0);
			if (headerStart == std::string::npos)return "";
			headerEnd = findEndTag(xml, "C2SIMHeader", headerStart);
			if (headerEnd == std::string::npos)return "";

			// find start and end of MessageBody
			bodyStart = findStartTag(xml, "MessageBody", headerEnd);
			if (bodyStart == std::string::npos)return "";
			bodyEnd = findEndTag(xml, "MessageBody", bodyStart);
			if (bodyEnd == std::string::npos)return "";
		}
		msg = XML_PREAMBLE + xml.substr(headerEnd, bodyEnd - headerEnd);
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
	std::string toDoc() {
		return NULL;
	}

	/***********************/
	/* extractDataForTag   */
	/***********************/
	/**
	* Utility routine to pull data from XML string, given a tag
	* This does not properly parse the XML; it just assumes
	* the XMLwill be well structured accorind to C2SIM schema
	* and that tag requested is for a leaf node which occurs
	* only once in the XML;
	* ignores namespace prefix in xmlString but not in tagSought
	*
	* @param xmlString - the string to be examined
	* @param tagSought - the tag for which data is sought
	* @return the string of data from the tag or "" if not found
	*/
	std::string CWIXHeader::extractDataForTag(std::string xmlString, std::string tagSought){

		// build XML format for tag
		std::string startTag = "<" + tagSought + ">";
		std::string startNsTag = ":" + tagSought + ">";
		std::string endTag = "</" + tagSought + ">";
		std::string endNsTag = ":" + tagSought + ">";

		// locate end of startTag if present
		size_t startIndex = xmlString.find(startTag);
		if (startIndex == std::string::npos)
			startIndex = xmlString.find(startNsTag);
		if (startIndex == std::string::npos)return "";
		startIndex = xmlString.find(">", startIndex) + 1;

		// locate endTag if present
		size_t endIndex = xmlString.find(endTag, startIndex);
		if (endIndex == std::string::npos)
			endIndex = xmlString.find(endNsTag);
		if (endIndex == std::string::npos)return "";
		endIndex = xmlString.find("<", startIndex);
		if (endIndex == std::string::npos)return "";

		// return substring with data
		return xmlString.substr(startIndex, endIndex - startIndex);

	}//end extractDataByTag()

	/**
	* Populate a CWIXHeader object from an XML string.
	* @param xmlString -
	* @return The CWIXHeader object that was passed as a parameter
	* @throws BMLClientException - Thrown for one of the following reasons <BR>
	*  IOException in SAXBuilder
	*  JDOMException
	*/
	CWIXHeader* CWIXHeader::populateCWIX(std::string xmlString) throw (BMLClientException) {

		CWIXHeader* c2s = new CWIXHeader();
		std::string temp;

		temp = extractDataForTag(xmlString, "conversationID");
		if (temp != "")
			c2s->setConversationID(temp);

		temp = extractDataForTag(xmlString, "inReplyTo");
		if (temp != "")
			c2s->setInReplyTo(temp);

		temp = extractDataForTag(xmlString, "messageID");
		if (temp != "")
			c2s->setMessageID(temp);

		temp = extractDataForTag(xmlString, "performative");
		if(temp != "")
			c2s->setPerformative(temp);

		temp = extractDataForTag(xmlString, "receiver");
		if (temp != "")
			c2s->setToReceivingSystem(temp);

		temp = extractDataForTag(xmlString, "replyBy");
		if (temp != "")
			c2s->setReplyBy(temp);

		temp = extractDataForTag(xmlString, "replyTo");
		if (temp != "")
			c2s->setReplyTo(temp);

		temp = extractDataForTag(xmlString, "sender");
		if (temp != "")
			c2s->setFromSendingSystem(temp);

		return c2s;

	}// end populateCWIX()


	/********************/
	/*  toXMLString     /*
	/********************/
	/**
	* toXMLString
	* @return - XML String containing the contents of this C2SIM header object
	*/
	std::string CWIXHeader::toXMLString() {

		std::string xmlString = "<Header>";

		// conversationID
		if (conversationID != "")
			xmlString += "<conversationID>" + conversationID + "</conversationID>";

		// inReplyTo
		if (inReplyTo != "")
			xmlString += "<inReplyTo>" + inReplyTo + "</inReplyTo>";

		// messageID
		if (messageID != "")
			xmlString += "<messageID>" + messageID + "</messageID>";

		// performative
		if (performative != "")
			xmlString += "<performative>" + performative + "</performative>";

		// protocolAndVersion
		if (protocolAndVersion != "")
			xmlString += "<protocolAndVersion>" + protocolAndVersion + "</protocolAndVersion>";

		// receiver
		if (toReceivingSystem != "")
			xmlString += "<receiver>" + toReceivingSystem + "</receiver>";

		// replyBy
		if (replyBy != "")
			xmlString += "<replyBy>" + replyBy + "</replyBy>";

		// sender
		if (fromSendingSystem != "")
			xmlString += "<sender>" + fromSendingSystem + "</sender>";

		xmlString += "</Header>";

		return xmlString;

	}   // toXMLString

	/**
	*     Getters and Setters are implemented for all instance variables.
	*/
	/**
	* setConversationID - Set field used to connect a series of messages into one conversation
	@param conversationID - String UUID  C2SIM conversationID .
	*/
	void CWIXHeader::setConversationID(std::string conversationID) {
		conversationID = conversationID;
	}

	/**
	* getConversationID - Get field used to connect a series of messages into one conversation
	@return - conversationID - String UUID
	*/
	std::string CWIXHeader::getConversationID() {
		return conversationID;
	}

	/**
	* generateConversationID - Generate a new Conversation ID (UIID format) for this C2SIM Header
	*/
	void CWIXHeader::generateConversationID(){
		boost::uuids::uuid uuid = boost::uuids::random_generator()();
		conversationID = boost::lexical_cast<std::string>(uuid);
	}

	/**
	* getInReplyTo property indicating the system to reply to
	* @return InReplyTo - String - System identifier
	*/
	std::string CWIXHeader::getInReplyTo() {
		return inReplyTo;
	}

	/**
	* setInReplyTo  Set inReplyTo property indicating identification of message being replied to
	@param inReplyTo - String messageID - UUID
	*/
	void CWIXHeader::setInReplyTo(std::string inReplyTo) {
		inReplyTo = inReplyTo;
	}

	/**
	* getMessageID - Get ID of message using current C2SIM header
	@return messageID = String - UUID
	*/
	std::string CWIXHeader::getMessageID() {
		return messageID;
	}

	/**
	( setMessageID - Set the message ID of the message using this C2SIM Header
	@param messageID String - UUID
	*/
	void CWIXHeader::setMessageID(std::string messageIDParm) {
		messageID = messageIDParm;
	}

	/**
	* generateMessageID - Generate ID for this C2SIM Message Header
	*/
	void CWIXHeader::generateMessageID() {
		boost::uuids::uuid uuid = boost::uuids::random_generator()();
		messageID = boost::lexical_cast<std::string>(uuid);
	}

	/**
	* getPerformative - Get the value of the performative property - Indicates processing requested for this message
	@return - String - Possible values: Inform, Confirm, Refuse, Accept, Agree, Request
	*/
	std::string CWIXHeader::getPerformative() {
		return performative;
	}

	/**
	* setPerformative - Set value of performative property in current CWIXHeader
	@param performative - String - Possible values see getPerformative()
	@see getPerformative
	*/
	void CWIXHeader::setPerformative(std::string performativeParm) {
		performative = performativeParm;
	}

	/**
	* getProtocolAndVersion - Get the protocol and current version
	*/
	std::string CWIXHeader::getProtocolAndVersion() {
		return protocolAndVersion;
	}

	/**
	* There is no setter for protocol and version
	*/

	/**
	* getToReceivingSystem - Get value of intended received for this message
	@return toReceivingSystem - String
	*/
	std::string CWIXHeader::getToReceivingSystem() {
		return toReceivingSystem;
	}

	/**
	* setReceiver - Set the receiver property indicating the indented recipient for this C2SIM message
	@param receiver - String
	*/
	void CWIXHeader::setToReceivingSystem(std::string receiverParm) {
		toReceivingSystem = receiverParm;
	}

	/**
	* getReplyBy - Specifies date/time by which reply is needed
	@return DateTime as - yyyy-mm-ddThh:mm:ssZ
	*/
	std::string CWIXHeader::getReplyBy() {
		return replyBy;
	}

	/**
	* setReplyBy - Set date/time by which reply is requested
	@param replyBy - Date/Time as yyyy-mm-ddThh:mm:ssZ
	*/
	void CWIXHeader::setReplyBy(std::string replyByParm) {
		replyBy = replyByParm;
	}

	/**
	* getReplyTo = Get replyTo property indicating what system is to be replied to
	@return - System Identification - String
	*/
	std::string CWIXHeader::getReplyTo() {
		return replyTo;
	}

	/**
	* setReplyTo - Set replyTo property indicating what system is to be replied to
	@param replyTo
	*/
	void CWIXHeader::setReplyTo(std::string replyToParm) {
		replyTo = replyToParm;
	}

	/**
	* getReplyWith - Get replyWith property indicating response required from receiver
	@return - Performative - String
	*/
	std::string CWIXHeader::getReplyWith() {
		return replyWith;
	}

	/**
	* setReplyWith - Set replyWith property indicating response required from receiver
	@param replyWith - Performative - String
	*/
	void CWIXHeader::setReplyWith(std::string replyWithParm) {
		replyWith = replyWithParm;
	}

	/**
	* getFromSendingSystem - Get the identification of the sender of thie message
	* @return System ID - String
	*/
	std::string CWIXHeader::getFromSendingSystem(){
		return fromSendingSystem;
	}

	/**
	* setFromSendingSystem - Set identification of the sender of this message
	* @param sender - System identifier - String
	*/
	void CWIXHeader::setFromSendingSystem(std::string s){
		fromSendingSystem = s;
	}


