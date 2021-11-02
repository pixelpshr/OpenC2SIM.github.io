/*----------------------------------------------------------------*
|   Copyright 2009-2019 Networking and Simulation Laboratory      |
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
#include "stdafx.h"
#include "C2SIMClientLib.h"

#pragma once

#include <iostream>
#include <boost/algorithm/string.hpp>

std::string messageType;            // CONNECTED, MESSAGE, etc
std::string messageSelector;        // Type of BML message as determined by XML matching: IBMLReport, ..
std::vector<std::string> headers;	// Raw unparsed STOMP message headers
std::map<std::string, std::string> headerMap;  // Maps header to header value 
std::string messageBody;			// body of the message not including terminating null
long messageLength;					// Length of message body not including terminating null
long contentLength;					// Length received in http header
C2SIMHeader* c2simHeader;			// Header stripped from incoming C2SIM Message
CWIXHeader* cwixHeader;             // Header stipped from incoming CWIX message

// Constructor - Initialize properties
/**
* BMLSTOMPMessage Constructor
*/
BMLSTOMPMessage::BMLSTOMPMessage() {
	messageSelector = "";
	messageType = "";
	headers;           // Empty vector
	headerMap;        //  intialize empty map
	messageBody = "";  // Empty message body
	messageLength = 0L;// There may not be a message body
	contentLength = 0l;// therefore no content
}


// import reference to the StompClient frame.properties
// for use as headerMap
void BMLSTOMPMessage::setHeaderMap(std::map<std::string, std::string> frameProperties) {
	headerMap = frameProperties;
}


// Add line to message body
void BMLSTOMPMessage::addToBody(std::string s) {
	messageBody += s;
}

// Getters for class properties
/**
* getMessageType - Returns the STOMP command for this message.  Normally CONNECTED or MESSAGE
* @return String - The STOMP COMMAND for this message
*/
std::string BMLSTOMPMessage::getMessageType() {
	return messageType;
}   // end getMessageType()

/**
* setMessageType - Saves the STOMP command for this message.  Normally CONNECTED or MESSAGE
* @param type e.g. - The STOMP COMMAND for this message
*/
void BMLSTOMPMessage::setMessageType(std::string type){
	messageType = type;
}// end setMessageType()

/**
* Return the BML message type determined when the server receives the message from its creator
* @return String - BML Message Selector e.g. IBML09GSR
*/
std::string BMLSTOMPMessage::getMessageSelector() {
	return messageSelector;
}   // end getMessageSelector

/**
* Return the body of the message, i.e. the part of the message 
* following the headers.  Does not include the terminating NULL
* @return String - The message body from the STOMP Message
*/
std::string BMLSTOMPMessage::getMessageBody() {
	return messageBody;
}   // end getMessageBody()

/**
* Sets value of body of the message, i.e. the part of the message following the headers.
* Does not include the terminating NULL
*/
void BMLSTOMPMessage::setMessageBody(std::string body){
	messageBody = body;
}// end setMessageBody()

/**
* Get the length of the message detrmined internally
* @return Long - The message length
*/
long BMLSTOMPMessage::getMessageLength() {
	return messageLength;
} // end getMessageLength()

/**
* set messageLength for this message
*/
void BMLSTOMPMessage::setMessageLength(long messageLengthParm) {
	messageLength = messageLengthParm;
}// end setMessageLength()

/**
* set contentLength for this message
*/
void BMLSTOMPMessage::setContentLengthFromStompHeader(long contentLengthParm) {
	contentLength = contentLengthParm;
}// end setContentLengthFromHeader()

/**
* Get the length of the message as determined by the content-length header
* @return Long - The content length
*/
 long BMLSTOMPMessage::getContentLength() {
	return contentLength;
}// end getContentLength()

/**
* getC2SIMHeader
* @return C2SIMHeader the c2sim header from this message
*/
 C2SIMHeader* BMLSTOMPMessage::getC2SIMHeader() {
	return c2simHeader;
}// end getC2SIMHeader()


 /**
 * setC2SIMHeader
 * @param C2SIMHeader the c2sim header from this essage
 */
 void BMLSTOMPMessage::setC2SIMHeader(C2SIMHeader* c2simHeaderParm) {
	 c2simHeader = c2simHeaderParm;
 }// end setC2SI<Header()

 /**
 * getCWIXHeader
 * @return CWIXHeader the c2sim header from this message
 */
 CWIXHeader* BMLSTOMPMessage::getCWIXHeader() {
	 return cwixHeader;
 }// end getC2SIMHeader()

 /**
 * setCWIXHeader
 * @param CWIXHeader the cwix header from this essage
 */
 void BMLSTOMPMessage::setCWIXHeader(CWIXHeader* cwixHeaderParm){
	 cwixHeader = cwixHeaderParm;
 }// end setCWIXHeader()

/**
* Get the contents of a specific STOMP header
* @param header e.g. "content-length"
* @return - String - Value of header or "" if header not set
*/
std::string BMLSTOMPMessage::getHeader(std::string header) {
	if (headerMap.count(header)) {
		return headerMap[header];
	}
	else {
		return "";
	}
} // end getHeader() 

// destructor
BMLSTOMPMessage::~BMLSTOMPMessage()
{
}
