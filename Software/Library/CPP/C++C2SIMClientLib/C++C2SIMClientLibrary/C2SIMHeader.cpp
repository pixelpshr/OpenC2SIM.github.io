/*----------------------------------------------------------------*
|   Copyright 2009-2022 Networking and Simulation Laboratory      |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for all purposes is hereby       |
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
#pragma once
#include "C2SIMClientLib2.h"
#include <boost/uuid/uuid.hpp>            // uuid class
#include <boost/uuid/uuid_generators.hpp> // generators
#include <boost/uuid/uuid_io.hpp>         // streaming operators etc.
#include <boost/lexical_cast.hpp>

// Instance variables

namespace {

	/**
	* startPosition - variable shared by findStartTag and findEndTag
	*/
	namespace {
		size_t startPosition;
	}

	/**
	* CommunicativeActTypeCode - Communicative act; one of the values from
	* enumCommunicativeActCategoryCode indicating type of message
	*/
	std::string communicativeActTypeCode = "";

	/**
	* securityClassificationCode -  Indicates the security classification of this message
	*/
	std::string securityClassificationCode = "";

	/**
	* conversationID - Unique identifier for the conversation. Should be kept identical for all replies.<BR>
	*   The conversationID may be used to associate a number of messages into a logical grouping.
	*/
	std::string conversationID = "";

	/**
	* - inReplyTo - References the message id that this message is a reply to.<BR>
	*       Element is missing if no message is referenced.
	*/
	std::string inReplyTo = "";

	/**
	* messageID - Unique identifier for the message. Other messages refer to the message using this ID.
	*/
	std::string messageID = "";

	std::string schemaProtocol = SISOSTD;

	/**
	* protocolVersion - The version of the protocol of default message
	*/
	std::string protocolVersion = "1.0.1";

	/**
	* - replyToSystem - Specifies what system to reply to.<BR>
	*/
	std::string replyToSystem = "";

	/**
	* Sending Time - ISO Date format yyyy-MM-ddTHH:mm:ssZ
	*/
	std::string sendingTime;

	/**
	* performative - Communicative act; one of the values from
	* enumCommunicativeActCategoryCode indicating type of message
	*/
	std::string performative = "";

	/**
	* receiver - Non empty list of unique identifiers of the receivers of the message
	*/
	std::string toReceivingSystem = "";

	/**
	* inReplyToMessageID - ID of message being replied to (UUID)
	*/
	std::string inReplyToMessageID = "";

	/**
	* replyBy - Defines the timestamp by which the sender expects the receiver to have responded to the message.
	*/
	std::string replyBy = "";

	/**
	* replyTo - Unique identifier of the unit or the system that should be contacted for anything concerning this message.<BR>
	*   This field is used only if the receiver is to respond to a station other than the sender.
	*/
	std::string replyTo = "";

	/**
	* replyWith - Defines the expected response communicative act, or empty if no response is intended
	*/
	std::string replyWith = "";

	/**
	* sender - Unique identifier of the unit or the system that sent the message.
	*/
	std::string fromSendingSystem = "";

	/**
	* XML_PREABMLE - used to build XML strings
	*/
	std::string XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
}

C2SIMHeader::C2SIMHeader()
{
}

/********************/
/*  insertC2SIM     */
/********************/
/**
* Given an xml document with the XML header and content create and insert a C2SIM header into it
@param xml - The xml string that header is to be inserted into
@param sender - The C2SIM Sender
@param receiver - The C2SIM Receiver
@param performative - One of the C2SIM performatives <BR>
Possible values for performative are: Inform, Confirm, Refuse, Accept, Agree, Request
@return - The updated XML string with the header inserted
*/
std::string C2SIMHeader::insertC2SIM(
	std::string xml, 
	std::string sender, 
	std::string receiver, 
	std::string performative,
	std::string isoDateTime){
	
	// Create a new C2SIMHeader and add the parameters passed to this routine
	C2SIMHeader* c2s = new C2SIMHeader();
	c2s->setFromSendingSystem(sender);
	c2s->setToReceivingSystem(receiver);
	if (performative != "")
		c2s->setPerformative(performative);
	else
		c2s->setPerformative("Inform");
	c2s->setCommunicativeActTypeCode(communicativeActTypeCode);
	c2s->setSendingTime(isoDateTime);

	// Add Message ID and Conversation ID to the header
	c2s->generateConversationID();
	c2s->generateMessageID();

	// We need to construct a C2SIM message, XMLPreamble + C2SIMHeader + XML
	// Get the C2SIM Header in xml text
	std::string temp = "";
	std::string c2sHeader = "";
	std::string newXML;
	c2sHeader = c2s->toXMLString();

	// Is the XML preamble present?
	if (xml.compare(0, 13, "<?xml version") == 0){
		// Locate the actual beginning of the xml (Find the second "<")
		std::size_t start = xml.find("<", 1);

		// Get the xml after the XML preamble
		temp = xml.substr(start);
	}   // xml preamble was present
	else
		temp = xml;

	// Now build the message and frame with with C2SIM_Message
	newXML = XML_PREAMBLE + 
		"<Message xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" + 
		c2sHeader + temp + "</Message>";
	
	return newXML;

}   // end C2SIMHeader::insertC2SIM()

/*******************************/
/* findStartTag and findEndTag */
/*******************************/

// next two functions only work when findEndTag is called
// with same tag and immediately after findStartTag
// also they ignore any namespace prefix that is present

// look for start of opening tag, which might have a namespace prefix
size_t C2SIMHeader::findStartTag(std::string xml, std::string tag, size_t offset){
	startPosition = xml.find("<" + tag, offset);
	if (startPosition != std::string::npos)return startPosition;
	startPosition = xml.find(":" + tag, offset);
	return startPosition;
}

// look for end of closing tag, which might have a namespace prefix
// and must come after startPosition
size_t C2SIMHeader::findEndTag(std::string xml, std::string tag, size_t offset){
	size_t endPosition = xml.find("</" + tag + ">", offset);
	if (endPosition != std::string::npos)return endPosition + tag.length() + 3;
	endPosition = xml.find(":" + tag + ">", startPosition+1);
	if (endPosition == std::string::npos)return endPosition;
	return endPosition + tag.length() + 2;
}

/********************/
/* removeC2SIM      */
/********************/
/**
* Remove C2SIM message envelope and return core xml message with header
@param xml - String - Input xml message with C2SIM envelop
@return String - Reconstructed message without C23SIM components
*/
std::string C2SIMHeader::removeC2SIM(std::string xml){
	size_t headerStart = 0, bodyStart = 0;
	size_t headerEnd = 0, bodyEnd = 0;
	std::string msg;

	// verify we are scanning XML
	if (xml.compare(0, 5, "<?xml") == 0){

		// find start and end of C2SIM Header
		headerStart = findStartTag(xml,"C2SIMHeader", 0);
		if (headerStart == std::string::npos)return "";
		headerEnd = findEndTag(xml, "C2SIMHeader", headerStart);
		if (headerEnd == std::string::npos)return "";

		// find start and end of MessageBody
		bodyStart = findStartTag(xml, "MessageBody", headerEnd);
		if (bodyStart == std::string::npos)return "";
		bodyEnd = findEndTag(xml, "MessageBody", bodyStart);
		if (bodyEnd == std::string::npos)return "";
	}
	msg = XML_PREAMBLE + xml.substr(headerEnd, bodyEnd-headerEnd);
	return msg;

}   // end removeC2SIM()

/*************/
/*  xmlDoc   */
/*************/
/**
*   xmlDoc - return a DOM Document representing this message header <BR>
*   NOT IMPLEMENTED AT THIS TIME
* @return null
*/
std::string C2SIMHeader::toDoc(){
	return "";
}

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
std::string C2SIMHeader::extractDataForTag(std::string xmlString, std::string tagSought){

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

/********************/
/*  populateC2SIM   */
/********************/
/**
* Populate a C2SIMHeader object from an XML string.
* @param xmlString -
* @return The C2SIMHeader object that was passed as a parameter
* @throws BMLClientException - Thrown for one of the following reasons <BR>
*  IOException in SAXBuilder
*  JDOMException
*/
C2SIMHeader* C2SIMHeader::populateC2SIM(std::string xmlString){// throw (C2SIMClientException)

	C2SIMHeader* c2s = new C2SIMHeader();
	std::string temp;

	// Work through the parsed document and build a C2SIMHeader object

	temp = extractDataForTag(xmlString, "CommunicativeActTypeCode");
	if (temp != "")
		c2s->setCommunicativeActTypeCode(temp);

	temp = extractDataForTag(xmlString, "ConversationID");
	if (temp != "")
		c2s->setConversationID(temp);

	temp = extractDataForTag(xmlString, "MessageID");
	if (temp != "")
		c2s->setMessageID(temp);

	temp = extractDataForTag(xmlString, "Protocol");
	if (temp != "")
		c2s->setProtocol(temp);

	temp = extractDataForTag(xmlString, "ProtocolVersion");
	if (temp != "")
		c2s->setProtocolVersion(temp);

	temp = extractDataForTag(xmlString, "ReplyToSystem");
	if (temp != "")
		c2s->setReplyToSystem(temp);

	temp = extractDataForTag(xmlString, "SecurityClassificationCode ");
	if (temp != "")
		c2s->setSecurityClassificationCode(temp);

	temp = extractDataForTag(xmlString, "SendingTime");
	if (temp != "")
		c2s->setSendingTime(temp);

	temp = extractDataForTag(xmlString, "FromSendingSystem");
	if (temp != "")
		c2s->setFromSendingSystem(temp);

	temp = extractDataForTag(xmlString, "ToReceivingSystem");
	if (temp != "")
		c2s->setToReceivingSystem(temp);

	temp = extractDataForTag(xmlString, "InReplyToMessageID");
	if (temp != "")
		c2s->setInReplyToMessageID(temp);

	return c2s;
}

/*******************/
/* getElementValue */
/*******************/
/**
*	internal function to extract value of an XML element from a string
*	given the tagName associated with the element
*  limitations:
*  1. designated element cannot have nested subelements or quoted data
*  2. only the first element of that tagname is found
*  3. any namespace markup in tags is ignored
* @param xmlString - the XML to be parsed
* @param tagName - the tagName with no < > markup
* @return elementValue if present, otherwise ""
*/
std::string getElementValue(std::string xmlString, std::string tagName) {
	
	// find index of starting and ending tag
	std::string openTag = tagName.append(">");
	std::size_t beginOpenTagName = xmlString.find(openTag);
	if (beginOpenTagName == std::string::npos)return "";
	std::size_t startData = beginOpenTagName + openTag.length();
	if (beginOpenTagName-- < 1)return "";// not good XML 
	std::size_t endData = xmlString.find("</", beginOpenTagName);
	if (endData == std::string::npos)return "";

	// verify the two tags match
	std::size_t beginCloseTagName = endData;
	if (xmlString[beginOpenTagName] == ':'){// has namespace
		beginCloseTagName = xmlString.find(":", endData);
		if (beginCloseTagName == std::string::npos)return "";
		beginCloseTagName++;
	}
	else beginCloseTagName += 2;
	if (xmlString.substr(beginCloseTagName, openTag.length()) != openTag)
		return "";// tags don't match
	
	// extract the data and return it
	return xmlString.substr(startData, endData-startData);

}// end getElementValue()

/********************/
/*  toXMLString     /*
/********************/
/**
* toXMLString
* @return - XML String containing the contents of this C2SIM header object
*/
std::string C2SIMHeader::toXMLString(){

	std::string xmlString = "<C2SIMHeader>";

	// CommunicativeActTypeCode
	if (communicativeActTypeCode != "")
		xmlString += 
			"<CommunicativeActTypeCode>" + communicativeActTypeCode + "</CommunicativeActTypeCode>";

	// ConversationID
	if (conversationID != "")
		xmlString += "<ConversationID>" + conversationID + "</ConversationID>";

	// FromSendingSystem
	if (fromSendingSystem != "")
		xmlString += "<FromSendingSystem>" + fromSendingSystem + "</FromSendingSystem>";

	// InReplyToMessageID
	if (inReplyTo != "")
		xmlString += "<InReplyToMessageID>" + inReplyTo + "</InReplyToMessageID>";

	// MessageID
	if (messageID != "")
		xmlString += "<MessageID>" + messageID + "</MessageID>";

	// Protocol
	if (schemaProtocol != "")
		xmlString += "<Protocol>" + schemaProtocol + "</Protocol>";

	// ProtocolVersion
	if (protocolVersion != "")
		xmlString += "<ProtocolVersion>" + protocolVersion + "</ProtocolVersion>";

	// ReplyToSystem
	if (replyToSystem != "")
		xmlString += "<ReplyToSystem>" + replyToSystem + "</ReplyToSystem>";

	// SecurityClassificationCode
	if (securityClassificationCode != "")
		xmlString += "<SecurityClassificationCode>" + securityClassificationCode + "</SecurityClassificationCode>";

	// SendingTime
	if (sendingTime != "")
		xmlString += "<SendingTime>" + sendingTime + "</SendingTime>";

	// ToReceivingSystem
	if (toReceivingSystem != "")
		xmlString += "<ToReceivingSystem>" + toReceivingSystem + "</ToReceivingSystem>";

	xmlString += "</C2SIMHeader>";

	return xmlString;

} // end toXMLString()

/**
*     Getters and Setters are implemented for all instance variables.
*/

/**
* getCommunicativeActTypeCode - Get the value of the CommunicativeActTypeCode property - Indicates processing requested for this message
@return - String - Possible values: Inform, Confirm, Refuse, Accept, Agree, Request
*/
std::string C2SIMHeader::getCommunicativeActTypeCode() {
	return communicativeActTypeCode;
}


/**
* setCommunicativeActTypeCode - Set value of CommunicativeActTypeCode property in current C2SIMHeader
@param CommunicativeActTypeCode - String - Possible values see getPerformative()
@see getCommunicativeActTypeCode#getCommunicativeActTypeCode
*/
void C2SIMHeader::setCommunicativeActTypeCode(std::string catc) {
	communicativeActTypeCode = catc;
}


/**
* setSecurityClassificationCode - Set field used to connect a series of messages into one conversation
@param securityClassificationCode - String UUID  C2SIM securityClassificationCode .
*/
void C2SIMHeader::setSecurityClassificationCode(std::string securityClassificationCode) {
	securityClassificationCode = securityClassificationCode;
}


/**
* getSecurityClassificationCode - Get field used to connect a series of messages into one conversation
@return - securityClassificationCode - String UUID
*/
std::string C2SIMHeader::getSecurityClassificationCode() {
	return securityClassificationCode;
}

/**
* setConversationID - Set field used to connect a series of messages into one conversation
* @param conversationID - String UUID  C2SIM conversationID .
*/
void C2SIMHeader::setConversationID(std::string cid){
	conversationID = cid;
}

/**
* getConversationID - Get field used to connect a series of messages into one conversation
@return - conversationID - String UUID
*/
std::string C2SIMHeader::getConversationID(){
	return conversationID;
}

/**
* generateConversationID - Generate a new Conversation ID (UIID format) for this C2SIM Header
*/
void C2SIMHeader::generateConversationID(){
	boost::uuids::uuid uuid = boost::uuids::random_generator()();
	conversationID = boost::lexical_cast<std::string>(uuid);
}

/**
* getInReplyTo property indicating the system to reply to
* @return InReplyTo - String - System identifier
*/
std::string C2SIMHeader::getInReplyTo() {
	return inReplyTo;
}

/**
* setInReplyTo  Set inReplyTo property indicating identification of message being replied to
* @param inReplyTo - String messageID - UUID
*/
void C2SIMHeader::setInReplyTo(std::string irt){
	inReplyTo = irt;
}

/**
* getMessageID - Get ID of message using current C2SIM header
* @return messageID = String - UUID
*/
std::string C2SIMHeader::getMessageID(){
	return messageID;
}

/**
* setMessageID - Set the message ID of the message using this C2SIM Header
* @param messageID String - UUID
*/
void C2SIMHeader::setMessageID(std::string mid){
	messageID = mid;
}

/**
* generateMessageID - Generate ID for this C2SIM Message Header
*/
void C2SIMHeader::generateMessageID() {
	boost::uuids::uuid uuid = boost::uuids::random_generator()();
	messageID = boost::lexical_cast<std::string>(uuid);
}

/**
* setProtocol - Set field used to indicate the protocol of the message
@param protocol - String UUID  C2SIM protocol .
*/
void C2SIMHeader::setProtocol(std::string protocolParm) {

	if (protocolParm != SISOSTD &&  protocolParm != "BML" &&  protocolParm != "")
		throw C2SIMClientException(
			"unsupported protocol:" + protocolParm);
	schemaProtocol = protocolParm;
}

/**
* getProtocol - Get field used to indicate the protocol of the message
@return - protocol - String UUID
*/
std::string C2SIMHeader::getProtocol() {
	return schemaProtocol;
}

/**
* setProtocolVersion - Set field used to indicate the protocol version of the message
@param protocol - String UUID  C2SIM protocol .
*/
void C2SIMHeader::setProtocolVersion(std::string protocolVersionParm) {
	protocolVersion = protocolVersionParm;
}

/**
* getProtocolVersion - Get field used to indicate the version of the 
* protocol for standard C2SIM message: 
* NOTE: this is not always the same as received c2sim-version
* for that use getHeader("c2sim-version")
@return - protocolVersion - String
*/
std::string C2SIMHeader::getProtocolVersion() {
	return protocolVersion;
}

/**
* getReplyToSystem = Get replyTo property indicating what system is to be replied to
@return - System Identification - String
*/
std::string C2SIMHeader::getReplyToSystem() {
	return replyToSystem;
}

/**
* setReplyToSystem - Set replyTo property indicating what system is to be replied to
@param replyTo
*/
void C2SIMHeader::setReplyToSystem(std::string replyTo) {
	replyToSystem = replyTo;
}

/**
* getSendingTime - ISO Format time when C2SIMHeader object was instantiated
*/
std::string C2SIMHeader::getSendingTime() {
	return sendingTime;
}

/**
* setSendingTime - Set sendingTime = ISO Format
@param sendingTime
*/
void C2SIMHeader::setSendingTime(std::string sendingTimeParm) {
	sendingTime = sendingTimeParm;
}

/**
* getPerformative - Get the value of the performative property - 
* Indicates processing requested for this message
* @return - String - Possible values: Inform, Confirm, Refuse, Accept, Agree, Request
*/
std::string C2SIMHeader::getPerformative(){
	return performative;
}

/**
* setPerformative - Set value of performative property in current C2SIMHeader
* @param performative - String - Possible values see getPerformative()
* @see getPerformative
*/
void C2SIMHeader::setPerformative(std::string p){
	performative = p;
}

/**
* getReceived - Get value of intended received for this message
* @return receiver - String
*/
std::string C2SIMHeader::getToReceivingSystem(){
	return toReceivingSystem;
}

/**
* setReceiver - Set the receiver property indicating the indented recipient for this C2SIM message
* @param receiver - String
*/
void C2SIMHeader::setToReceivingSystem(std::string r){
	toReceivingSystem = r;
}

/**
* getReplyBy - Specifies date/time by which reply is needed
@return DateTime as - yyyy-mm-ddThh:mm:ssZ
*/
std::string C2SIMHeader::getReplyBy(){
	return inReplyTo;
}

/**
* setReplyBy - Set date/time by which reply is requested
* @param replyBy - Date/Time as yyyy-mm-ddThh:mm:ssZ
*/
void C2SIMHeader::setReplyBy(std::string rb){
	replyBy = rb;
}

/**
* getReplyTo = Get replyTo property indicating what system is to be replied to
* @return - System Identification - String
*/
std::string C2SIMHeader::getReplyTo(){
	return replyBy;
}

/**
* setReplyTo - Set replyTo property indicating what system is to be replied to
* @param replyTo
*/
void C2SIMHeader::setReplyTo(std::string rt){
	replyTo = rt;
}

/**
* getReplyWith - Get replyWith property indicating response required from receiver
* @return - Performative - String
*/
std::string C2SIMHeader::getReplyWith(){
	return replyWith;
}

/**
* setReplyWith - Set replyWith property indicating response required from receiver
* @param replyWith - Performative - String
*/
void C2SIMHeader::setReplyWith(std::string rw){
	replyWith = rw;
}

/**
* getFromSendingSystem - Get the identification of the sender of thie message
* @return System ID - String
*/
std::string C2SIMHeader::getFromSendingSystem(){
	return fromSendingSystem;
}

/**
* setFromSendingSystem - Set identification of the sender of this message
* @param sender - System identifier - String
*/
void C2SIMHeader::setFromSendingSystem(std::string s){
	fromSendingSystem = s;
}

/**
* getInReplyToMessageID - Get ID message being replied to
@return - String System ID UUID format
*/
std::string C2SIMHeader::getInReplyToMessageID() {
	return inReplyToMessageID;
}

/**
* setInReplyToMessageID - Set ID message being replied to
@param inReplyToMessageID- ID in UUID format
*/
void C2SIMHeader::setInReplyToMessageID(std::string inReplyToMessageID) {
inReplyToMessageID = inReplyToMessageID;
}

