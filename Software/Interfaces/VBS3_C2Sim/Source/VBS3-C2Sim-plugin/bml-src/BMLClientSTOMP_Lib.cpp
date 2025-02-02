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
#include "StompClient.h"

#pragma once

#include <iostream>
#include <boost/asio.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/write.hpp>
#include <boost/asio/buffer.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <array>
#include <iostream>

#include "boost/date_time/posix_time/posix_time.hpp"

using namespace boost;
using namespace boost::asio;
using namespace boost::asio::ip;

using std::vector;

// C++ semi=clone of Doug Corner's Java class of the same name
//
// We rely on Mark Eschbach's open source StompClient
// for most functionality; this class serves as an interface
//
// as a result we do not need a thread and queue as in the Java version;
// StompClient takes care of that

// stomp access derived from Mark Eschbach open source
mee::stomp::StompClient stompClient(1);

// constructor
BMLClientSTOMP_Lib::BMLClientSTOMP_Lib() {}

// destructor
BMLClientSTOMP_Lib::~BMLClientSTOMP_Lib() { 
	disconnect();
}

/***********************/
/*  formatGmtDateTime  */
/***********************/
/**
*  Internal function to capture current GMT date and time a strings
*/
std::string gmtDateTime() {
	// use Microsoft system function
	SYSTEMTIME st;
	GetSystemTime(&st);
	char systime[13], sysdate[11];
	
	// format to Java SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")
	sprintf_s(systime,13, "%02d:%02d:%02d,%03d", st.wHour, st.wMinute, st.wSecond, st.wMilliseconds);
	sprintf_s(sysdate,11, "%04d-%02d-%02d", st.wYear, st.wMonth, st.wDay);
	std::string formatDate = sysdate;
	std::string formatTime = systime;
	std::string datePlusTime = formatDate.append(" ").append(formatTime);
	return datePlusTime;
}

/****************/
/* connect      */
/****************/
/**
* Connect to Stomp server
* @return STOMPMessage - Response from server if connection 
* made otherwise throw an exception.  Response should be CONNECTED.
* @see edu.gmu.c4i.c2simclientlib2.BMLClientException
* @see edu.gmu.c4i.c2simclientlib2.BMLSTOMPMessage
* @throws BMLClientException - Includes various exceptions
*/
BMLSTOMPMessage* BMLClientSTOMP_Lib::connect() {

	// connect
	std::string connectOperation;
	try {
		connectOperation = stompClient.connect(host, port, false, "", "");
	}
	catch (mee::stomp::StompException &e) {
		throw BMLClientException(
			"StompException in BMLClientSTOMP_Lib:" +
			std::string(e.what()));
	}

	// Send subscription to the server
	stompClient.subscribe(
		destination,
		mee::stomp::Auto,
		adv_subscriptions,
		gmtDateTime());

	// Get the response to connection request
	BMLSTOMPMessage* resp = new BMLSTOMPMessage();
	resp->setMessageType(connectOperation);

	// Are we connected?  If so return the message.  If not throw an exception
	if (connectOperation == "CONNECTED")
		return resp;
	else
		throw new BMLClientException(
		"BMLSTOMP_Lib.connect() Expected 'CONNECTED' but received " +
		resp->getMessageType());

}// end connect()

/****************/
/* publish      */
/****************/
/**
* Send message to STOMP host on an already established connection 
* @param cmd - STOMP Command to be used - should normally be MESSAGE
* @param xml - The message to be sent
* @param headers - A Vector Strings containing STOMP headers in the form  
*                  headerName:headerValue
* @throws BMLClientException - Thrown by sendFrame()
*/
void BMLClientSTOMP_Lib::publish(
	std::string cmd,
	vector<std::string> headers,
	std::string xml)
{
	// make a StompFrame from it
	mee::stomp::StompFrame frame;
	frame.operation = "SEND";

	// move the headers from BMLSTOMPMessage to StompFrame properties
	std::map< std::string, std::string > properties;
	std::map<std::string, std::string>::iterator propertiesIterator;
	std::vector<std::string>::iterator headerIterator;
	for (headerIterator = headers.begin();
		headerIterator != headers.end();
		++headerIterator) {
		std::string header = (*headerIterator);

		// trim off endline if present
		int headerEnd = header.length()-1;
		if (headerEnd > 0) {
			if (header[headerEnd] == '\n')
				header = header.substr(0, headerEnd);
		}
		int colonPosition = header.find_first_of(':');
		if (colonPosition > 0)
			properties.insert(std::pair<std::string, std::string>(
				header.substr(0,colonPosition),
				header.substr(colonPosition+1)));
	}
	
	// Send the message
	try {
		stompClient.sendMessage("/topic/BML",xml,properties);
	}
	catch (const mee::stomp::StompException& e) {
		std::stringstream ss;
		ss << getPort();
		throw BMLClientException(
			"error in BLClientSTOMP_Lib sending STOMP " +
			getHost() + ":" + ss.str(), e);
	}
}// publish()

/*************************/
/* getNext_NoBlock()     */
/*************************/
/**
* Returns the next message received from the STOMP messaging server.
* The calling thread will NOT be blocked if a STOMPMessage is not available; .
* @return STOMPMessage - The next STOMP message or NULL if no message is available at this time.  Message should be MESSAGE.
* @see edu.gmu.c4i.c2simclientlib2.BMLSTOMPMessage
* @see edu.gmu.c4i.c2simclientlib2.BMLClientException
*  @throws BMLClientException - Encapsulates several specific exceptions
*/
BMLSTOMPMessage* BMLClientSTOMP_Lib::getNext_NoBlock() {

	// check whether there is something available to read
	if (stompClient.streamIsEmpty())
		return NULL;
	else
		// we know there is a message
		// use the getNextBlock code
		return getNext_Block();

}   // getNext_NoBlock()

/*************************/
/* getNext_Block()       */
/*************************/
/**
* Returns the message received from the STOMP messaging server.  The calling thread
* will be blocked until a message has been received.
* @return STOMPMessage - The next STOMP message.  Message should be MESSAGE.
* @see edu.gmu.c4i.c2simclientlib2.BMLSTOMPMessage
* @see edu.gmu.c4i.c2simclientlib2.BMLClientException
* @throws BMLClientException - Encapsulates various exceptions
*/
BMLSTOMPMessage* BMLClientSTOMP_Lib::getNext_Block() {

	// read a STOMP frame
	mee::stomp::StompFrame frame;
	try {
		stompClient.receiveFrame(frame);
	}
	catch (const mee::stomp::StompException& e) {
		std::stringstream portBuf;
		portBuf << port;
		throw BMLClientException(
			"error in BLClientSTOMP_Lib reading STOMP " +
			host + ":" + portBuf.str(), e);
	}
	catch (...) {
		std::stringstream portBuf;
		portBuf << port;
		throw BMLClientException(
			"error in BLClientSTOMP_Lib reading STOMP " +
			host + ":" + portBuf.str());
	}
	
	// make a BMLSTOMPMessage from the frame
	currentMsg = new BMLSTOMPMessage();
	currentMsg->setMessageType(frame.operation);
	std::string xml = frame.message.str();
	currentMsg->setMessageBody(xml);
	currentMsg->setMessageLength(xml.length());

	// capture the headers from the frame properties
	// content-length header may not have been used (e.g. CONNECTED message)  
	//  Provode a default value of 0 indicating no message body
	long contentLength = 0L;
	std::string cL = "0";
	currentMsg->setHeaderMap(frame.properties);

	// Use the value from content-length header (or default value if we didn't find one)
	cL = currentMsg->getHeader("content-length");
	if (cL != "") {
		contentLength = std::stol(cL);
	}
	else {
		contentLength = currentMsg->getMessageLength();
	}
	currentMsg->setContentLengthFromStompHeader(contentLength);
	currentMsg->setMessageLength(contentLength);// This may be modified later if this is a C2SIM message
	messageSelector = currentMsg->getHeader("message-selector");

	// If this is a C2SIM Message:
	//      Extract the original XML and return in messageBody
	//      Extract the C2SIM information and build a C2SIMHeader
	//      Add the C2SIMHeader to the currentMsg
	std::string xmlBody;
	if (currentMsg->getHeader("protocol") == "C2SIM") {

		// Fill out a new C2SIM Header with message
		C2SIMHeader* c2s = new C2SIMHeader;
		c2s->populateC2SIM(currentMsg->getMessageBody());
		currentMsg->setC2SIMHeader(c2s);

		// Remove C2SIM header and trailer
		xmlBody = c2s->removeC2SIM(currentMsg->getMessageBody());

	}// end build C2SIM Header

	else if (currentMsg->getHeader("protocol") == "CWIX") {

		// Fill out a new CWIX Header with message
		CWIXHeader* cwx = new CWIXHeader;
		cwx->populateCWIX(currentMsg->getMessageBody());
		currentMsg->setCWIXHeader(cwx);

		// Remove CWIX head and trailer
		xmlBody = cwx->removeCWIX(currentMsg->getMessageBody());

	}// end build CWIX Header
	
	// insert XML and return the BMLClientSTOMP_LibMessage object to the caller
	currentMsg->setMessageBody(xmlBody);
	currentMsg->setMessageLength(xmlBody.length());
	return currentMsg;

}// getNext_Block()

/************************/
/* sendC2SIM_Response   */
/************************/
/**
* sendC2SIM_Response - Send a C2SIM response to an incoming C2SIM request.  
* Response will be sent via STOMP.
* @param oldMsg - Message that is being responded to
* @param c2sResp - Response code to be sent*
* @param ackCode - Code describing the acknowledgement
* @throws edu.gmu.c4i.c2simclientlib2.BMLClientException 
*	- May throw a BMLClientException for several reasons
*      IOException during send or close
*      UnknownHost exception
*      Received something other that "CONNECTED" during connection process
*      InterruptedException while waiting for queue
*      Error caught in foreground thread
*/
void BMLClientSTOMP_Lib::sendC2SIM_Response(
	BMLSTOMPMessage* oldMsg, 
	std::string c2sResp, 
	std::string ackCode) {

	// Build Vector of headers for this message
	std::string msg;
	std::vector<std::string> headers;
	std::string conversationID;

	// if old message is C2SIM build a new C2SIM message
	if (oldMsg->getHeader("protocol") == "C2SIM"){
		C2SIMHeader* oldc2s = oldMsg->getC2SIMHeader();
		C2SIMHeader* c2s = new C2SIMHeader();

		// Use the conversationID from the incoming message
		conversationID = oldc2s->getConversationID();
		c2s->setConversationID(conversationID);

		// Set performative for this message
		c2s->setPerformative(c2sResp);

		// inReplyTo is the request message
		c2s->setInReplyTo(oldc2s->getMessageID());

		// Swap sender and receiver from incoming message
		c2s->setFromSendingSystem(oldc2s->getToReceivingSystem());
		c2s->setToReceivingSystem(oldc2s->getFromSendingSystem());

		// Convert new C2SIM header to xml
		std::string c2sxml = c2s->toXMLString();

		// Build the acknowledgement XML message content
		std::string xml = "<MessageBody><AcknowledgementBody><AcknowledgementTypeCode>" +
			ackCode + "</AcknowledgementTypeCode></AcknowledgementBody></MessageBody>";
		
		// add protocol to the headers vector
		headers.push_back(std::string("protocol:C2SIM").append("\n"));

		// Build xml message
		msg = XML_PREAMBLE +
			"<Message xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" +
			c2sxml + xml + "</Message>";

	}// C2SIM case

	else {// CWIX case - only other possibility beside C2SIM at this point

		CWIXHeader* oldcwx = oldMsg->getCWIXHeader();
		CWIXHeader* cwx = new CWIXHeader();

		// Use the conversationID from the incoming message
		conversationID = oldcwx->getConversationID();
		cwx->setConversationID(conversationID);

		// Set performative for this message
		cwx->setPerformative(c2sResp);

		// inReplyTo is the request message
		cwx->setInReplyTo(oldcwx->getMessageID());

		// Swap sender and receiver from incoming message
		cwx->setFromSendingSystem(oldcwx->getToReceivingSystem());
		cwx->setToReceivingSystem(oldcwx->getFromSendingSystem());

		// Convert new C2SIM header to xml
		std::string c2sxml = cwx->toXMLString();

		// Build the acknowledgement XML message content
		std::string xml = "<MessageBody><AcknowledgementBody><AcknowledgementTypeCode>" +
			ackCode + "</AcknowledgementTypeCode></AcknowledgementBody></MessageBody>";

		// add protocol to the headers vector
		headers.push_back(std::string("protocol:CWIX").append("\n"));

		// Build xml message
		msg = XML_PREAMBLE +
			"<Message xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" +
			c2sxml + xml + "</Message>";
	}

	// Use most headers from incoming message
	headers.push_back("destination:" + oldMsg->getHeader("destination") + "\n");
	headers.push_back("content-type:text/plain\n");
	headers.push_back("submitter:" + oldMsg->getHeader("submitterID") + "\n");
	headers.push_back("message-time:" + oldMsg->getHeader("msgTime") + "\n");
	headers.push_back("message-type:" + oldMsg->getHeader("msgType") + "\n");
	headers.push_back("message-number:" + oldMsg->getHeader("msgNumber") + "\n");
	headers.push_back("conversationid:" + conversationID + "\n");

	// Publish the message    
	publish("SEND", headers, msg);

} // end sendC2SIM_Response()

/*******************/
/* disconnect()    */
/*******************/
/**
* Disconnect from STOMP server and close socket.
* @return String - "OK" indicating successful completion of disconnect 
*	or else throws an exception
* @throws BMLClientException - Encapsulates various exceptions
*/
std::string BMLClientSTOMP_Lib::disconnect() {

	// disconnect from StomPClient
	try {
		stompClient.disconnect();
	}// try
	catch (mee::stomp::StompException& e) {
		throw BMLClientException(
			"StompException thrown in call to BMLClientSTOMP_Lib disconnect ", e);
	}// StompException
	catch (std::exception& e) {
		throw BMLClientException(
			"Exception thrown in call to BMLClientSTOMP_Lib disconnect ", e);
	}// Exception

	// Disconnect was successful return OK
	return "OK";

} // end disconnect()

/***********************/
/* getters and setters  */
/************************/
// Getters and setters

/**
* setPort (String) for STOMP connection
* @param port - String
*/
void BMLClientSTOMP_Lib::setPort(std::string p) {
	port = p;
}// end setPort()

/**
* setPort int for STOMP connection
* @param port - int
*/
void BMLClientSTOMP_Lib::setPort(int p) {
	std::ostringstream portstring;
	portstring << p;
	port = portstring.str();
}// end setPort()

/**
* @return int - Current port setting
*/
int BMLClientSTOMP_Lib::getPort() {
	return std::atoi(port.c_str());
}// getPort()

/**
* Set the host name or IP address
* @param host string name or IP address of STOMP server
*/
void BMLClientSTOMP_Lib::setHost(std::string h) {
	host = h;
}// end setHost()

/**
* Get the address of the STOMP Messaging server
* @return string - Host name/address
*/
std::string BMLClientSTOMP_Lib::getHost() {
	return host;
} // end getHost()

/**
* Set the destination queue or topic
* @param destination - string Name of topic.  The default is /topic/BML/
*/
void BMLClientSTOMP_Lib::setDestination(std::string dn) {
	destination = dn;
} // end setDestination()

/**
* Set the MessageSelector
* @param messageSelector - string MessageSelector used for subscribing to a particular message
*/
void BMLClientSTOMP_Lib::setMessageSelector(std::string m) {
	messageSelector = m;
} // end setMessageSelector()

/**
* Get the MessageSelector
* @return messageSelector - string MessageSelector used for subscribing to a particular message
*/
std::string BMLClientSTOMP_Lib::getMessageSelector() {
	return messageSelector;
} // end getMessageSelector()    

/**
* addSubscription - Add a Message Selector to list of selectors submitted with SUBSCRIBE
*   Host will only publish messages matching one of the selectors.
*   If no addSubscriptions are submitted then all messages will be received.
* @param msgSelector string - Name of a BML Message Type to be added to subscription list.  
*	If the list contains at least one Message Selector then the only messages
*   that will be received on the current connection will be those on the list.  
*	If no subscriptions are submitted then this system will receive all messages published to the topic
* @see setDestination
*/
void BMLClientSTOMP_Lib::addSubscription(std::string msgSelector) {
	throw BMLClientException(
		"this version of BMLClientSTOMP_Lib does not support addSubscription - it is deprecated");
} // end addSubscription()

/**
* addAdvSubscription - Add a general selector expression to be used with SUBSCRIBE
*   Host will only publish messages matching one of the selectors.
*   If no addSubscriptions are submitted then all messages will be received.
* @param msgSelector string - Name of a BML Message Type to be added to subscription list.  
*	If the list contains at least one Message Selector then the only messages
*   that will be received on the current connection will be those on the list.  
*	If no subscriptions are submitted then this system will receive all messages published to the topic
* @see setDestination
*/
void BMLClientSTOMP_Lib::addAdvSubscription(std::string s) {
	adv_subscriptions.push_back(s);

} // end addadvSubscription()  




