/*----------------------------------------------------------------*
|   Copyright 2009-2020 Networking and Simulation Laboratory      |
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
#include "C2SIMClientLib2.h"

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

// Constructor - Initialize properties
/**
* C2SIMSTOMPMessage Constructor
*/
C2SIMSTOMPMessage::C2SIMSTOMPMessage() {
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
void C2SIMSTOMPMessage::setHeaderMap(std::map<std::string, std::string> frameProperties) {
	headerMap = frameProperties;
}


// Add line to message body
void C2SIMSTOMPMessage::addToBody(std::string s) {
	messageBody += s;
}

// Getters for class properties
/**
* getMessageType - Returns the STOMP command for this message.  Normally CONNECTED or MESSAGE
* @return String - The STOMP COMMAND for this message
*/
std::string C2SIMSTOMPMessage::getMessageType() {
	return messageType;
}   // end getMessageType()

/**
* setMessageType - Saves the STOMP command for this message.  Normally CONNECTED or MESSAGE
* @param type e.g. - The STOMP COMMAND for this message
*/
void C2SIMSTOMPMessage::setMessageType(std::string type){
	messageType = type;
}// end setMessageType()

/**
* Return the BML message type determined when the server receives the message from its creator
* @return String - BML Message Selector e.g. IBML09GSR
*/
std::string C2SIMSTOMPMessage::getMessageSelector() {
	return messageSelector;
}   // end getMessageSelector

/**
* Return the body of the message, i.e. the part of the message 
* following the headers.  Does not include the terminating NULL
* @return String - The message body from the STOMP Message
*/
std::string C2SIMSTOMPMessage::getMessageBody() {
	return messageBody;
}   // end getMessageBody()

/**
* Sets value of body of the message, i.e. the part of the message following the headers.
* Does not include the terminating NULL
*/
void C2SIMSTOMPMessage::setMessageBody(std::string body){
	messageBody = body;
}// end setMessageBody()

/**
* Get the length of the message detrmined internally
* @return Long - The message length
*/
long C2SIMSTOMPMessage::getMessageLength() {
	return messageLength;
} // end getMessageLength()

/**
* set messageLength for this message
*/
void C2SIMSTOMPMessage::setMessageLength(long messageLengthParm) {
	messageLength = messageLengthParm;
}// end setMessageLength()

/**
* set contentLength for this message
*/
void C2SIMSTOMPMessage::setContentLengthFromStompHeader(long contentLengthParm) {
	contentLength = contentLengthParm;
}// end setContentLengthFromHeader()

/**
* Get the length of the message as determined by the content-length header
* @return Long - The content length
*/
 long C2SIMSTOMPMessage::getContentLength() {
	return contentLength;
}// end getContentLength()

/**
* getC2SIMHeader
* @return C2SIMHeader the c2sim header from this message
*/
 C2SIMHeader* C2SIMSTOMPMessage::getC2SIMHeader() {
	return c2simHeader;
}// end getC2SIMHeader()


 /**
 * setC2SIMHeader
 * @param C2SIMHeader the c2sim header from this essage
 */
 void C2SIMSTOMPMessage::setC2SIMHeader(C2SIMHeader* c2simHeaderParm) {
	 c2simHeader = c2simHeaderParm;
 }// end setC2SI<Header()


/**
* Get the contents of a specific STOMP header
* @param header e.g. "content-length"
* @return - String - Value of header or "" if header not set
*/
std::string C2SIMSTOMPMessage::getHeader(std::string header) {
	if (headerMap.count(header)) {
		return headerMap[header];
	}
	else {
		return "";
	}
} // end getHeader() 

// destructor
C2SIMSTOMPMessage::~C2SIMSTOMPMessage()
{
}
