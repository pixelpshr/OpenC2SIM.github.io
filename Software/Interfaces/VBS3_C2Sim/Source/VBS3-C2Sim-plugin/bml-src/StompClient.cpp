/*
 * StompClient.cpp
 *
 * this version modified to use Boost for C2SIMinterface by JMP
 *
 * Copyright (c) 2010, Mark Eschbach 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mark Eschbach nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Mark Eschbach BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  Created on: Aug 22, 2010
 *      Author: "Mark Eschbach" &lt;meschbach@gmail.c&gt;
 * $HeadURL: file:///home/gremlin/backups/rcs/svn/software/msg-util/trunk/stomp-util/StompClient.cpp $
 * $Id: StompClient.cpp 2566 2010-12-12 09:39:15Z meschbach $
 */
#include "stdafx.h"
#include "StompClient.h"
#include <iomanip>

// Boost
#include <boost/asio.hpp>

//#define DEBUG // to see incoming messages
#ifdef DEBUG
#define DEBUG_PARSER
#endif

using namespace mee::stomp;

/******************************************************************************
 * StompClient
 *****************************************************************************/
/**
* Constructs a new StompClient with the given socket for communication
* @param socket is the I/O stream for communicating with the STOMP broker
* @since 0.0.1
*
StompClient::StompClient(std::iostream &networkSocket)
	: inputSource(networkSocket),
	  outputSink(networkSocket)
{}
*/
boost::asio::ip::tcp::iostream stompStream;
StompClient::StompClient(int dummy)
{
}


/**
 * Standard destructor, overridable
 * @since 0.0.1
 */
StompClient::~StompClient() { stompStream.close(); }


// lets BMLClientSTOMP_Lib peek JMP 20May18
bool StompClient::streamIsEmpty() {
	try {
		return stompStream.peek() == EOF;
	}
	catch(std::exception &e) {
		throw StompException(
			"Exception in StompClient::streamIsEmpty() " + std::string(e.what()));
	}
}

void StompClient::sendFrame(StompFrame& frame) throw (StompException){
	/*
	 * Write our operation
	 */
	//outputSink << frame.operation << "\n";
	stompStream << frame.operation << "\n";
	/*
	 * Write our properties
	 */
	for (std::map<std::string, std::string>::iterator it = frame.properties.begin();
		it != frame.properties.end(); 
		it++) {

		//outputSink << it->first << ":" << it->second << "\n";
		stompStream << it->first << ":" << it->second << "\n";
	}
	/*
	 * Write our message
	 */
	//outputSink << "\n" << frame.message.str();
	stompStream << "\n" << frame.message.str();
	//outputSink.put(0);
	stompStream.put(0);
	//outputSink.flush();
	stompStream.flush();
}

void StompClient::receiveFrame(StompFrame& frame) {
	/*
	 * Ensure our underlying stream is in a good state
	 */
	//if (inputSource.bad()){
	if (stompStream.bad()){
		throw StompException("STOMP socket is not in a good state (stompStream.bad)");
	}

	/*
	 *  clear out the frame provided
	 */
	frame.message.str(std::string());
	
	/*
	 * Read in the operation
	 */
	//std::getline(inputSource, frame.operation);
	std::getline(stompStream, frame.operation);
#ifdef DEBUG_PARSER
	std::cout << "Received message operation '" << frame.operation 
		<< "' (" << (int)frame.operation.c_str()[0] << ")" << std::endl;
#endif
	/*
	 * Ensure the frame hasn't prematurely ended
	 */
	//if (inputSource.peek() == 0) {
	if (stompStream.peek() == 0) {
		throw StompException("Unexpected termination of STOMP frame");
	}
	/*
	 * Receive our properties
	 */
	receiveProperties(frame.properties);
	/*
	 * Read in the message body
	 */
#ifdef DEBUG_PARSER
	std::cout << "Reading message body: '";
#endif
	//while ( inputSource.peek() != 0 && inputSource.good()) {
	while (stompStream.peek() != 0 && stompStream.good()) {
		char c;
		//inputSource.get(c);
		stompStream.get(c);
#ifdef DEBUG_PARSER
		std::cout << c;
#endif
		frame.message.put(c);
	}
#ifdef DEBUG_PARSER
	std::cout << "'"<<std::endl;
#endif
	/*
	 * Notify the user of failure
	 */
	//if (!inputSource.good()) {
	if (!stompStream.good()) {
		throw StompException("STOMP socket is not in a good state (!stompStream.good)");
	}
	/*
	 * Read the null character
	 */
	char frameTerminator;
	//inputSource.get(frameTerminator);
	stompStream.get(frameTerminator);
	if (frameTerminator != 0) {
#ifdef DEBUG_PARSER
		std::cout << "Received frame terminator '" << frameTerminator << "'" <<std::endl;
#endif
		throw StompException("Invalid frame terminator");
	}
	/*
	 * Ensure sure a new line doesn't fallow.  This fixes AMQ, which I don't
	 * believe follows the specification.
	 */
	//if ( inputSource.peek() == '\n' ) {
	if (stompStream.peek() == '\n') {
		//inputSource.get();
		stompStream.get();
	}
	/*
	 * Debugging information
	 */
#ifdef DEBUG_PARSER
	std::cout << "Done reading frame" << std::endl;
#endif
}// end StompClient::receiveFrame()

void StompClient::receiveProperties(std::map<std::string, std::string> &props) {
	/*
	 * Setup our property reader
	 */
	bool hasMoreProperties = true;
	/*
	 * While the stream is good, and we haven't encountered any empty lines
	 */
	//while ( inputSource.good() && hasMoreProperties) {
	while (stompStream.good() && hasMoreProperties) {
		/*
		 * have we encountered our new line yet?
		 */
		//char nextChar = inputSource.peek();
		char nextChar = stompStream.peek();
		if (nextChar == '\n') {
			/*
			 * Yes, we are done
			 */
			hasMoreProperties = false;
		} else if (nextChar == '\0') {
			throw StompException(
			        "Null character encountered while reading properties");
		} else {
			props.insert(receiveProperty());
		}
	}
#ifdef DEBUG_PARSER
	std::cout << "Completed property reading" << std::endl;
#endif
	/*
	 * Eat the new line character
	 */
	//inputSource.get();
	stompStream.get();
}// end StompClient::receiveProperties()

std::pair<std::string, std::string> StompClient::receiveProperty() {
	/*
	 * Read a line from the stream
	 */
	std::string line;
	//std::getline( inputSource, line
	std::getline(stompStream, line);
#ifdef DEBUG_PARSER
	std::cout << "Received the following property line '" << line << "'" << std::endl;
#endif
	//if (!inputSource.good()) 
	if (!stompStream.good()) {
		throw StompException("Socket is no longer good");
	}
	/*
	 * Separate our property key from value
	 */
	size_t index = line.find(':');
	if (index == std::string::npos) {
#ifdef DEBUG_PARSER
		std::cout << "Invalid property (unable to find separator): '" << line<< "'" << std::endl;
#endif
		throw StompException("No property separator found, invalid frame");
	} else {
		std::string key = line.substr(0, index);
		size_t valueStart = line[index + 1] == ' ' ? index + 2 : index + 1;
		std::string value = line.substr(valueStart);
#ifdef DEBUG_PARSER
		std::cout << "Read property '" << key << "': '"<< value << "'" << std::endl;
#endif
		return std::pair<std::string, std::string>(key, value);
	}
}

// make STOMP connection over TCP connection
//void StompClient::connect(
std::string StompClient::connect(
		const std::string host,
		const std::string port,
        const bool sendLogin,
        const std::string& user,
        const std::string& pass) {

	// make TCP connection
	try {
		stompStream.connect(host.c_str(), port.c_str());
		if (!stompStream)
			throw StompException(
			"TCP connection failed in BMLClientSTOMP_Lib:");
	}
	catch (std::exception &e) {
		throw StompException(
			"BMLClientException in BMLClientSTOMP_Lib:" +
			std::string(e.what()));
	}
#ifdef DEBUG
	std::cout << "Sending connect frame" << std::endl;
#endif
	sendConnect(sendLogin,user,pass);
	StompFrame frame;
#ifdef DEBUG
	std::cout << "Reading server response" << std::endl;
#endif
	receiveFrame(frame);
	std::string operation = frame.operation;
#ifdef DEBUG
	std::cout << "Processing server response" << std::endl;
#endif
	processConnect(frame);
	return operation;// JMP added
}// end StompClient::connect()

void StompClient::sendConnect(
        const bool sendLogin,
        const std::string& user,
        const std::string& pass) {
	StompFrame frame;
	frame.operation = "CONNECT";
	if (sendLogin) {
		frame.properties.insert(std::pair<std::string, std::string>(
		        "login",
		        user));
		frame.properties.insert(std::pair<std::string, std::string>(
		        "passcode",
		        pass));
	}
	sendFrame(frame);

}// end StompClient::sendConnect()

void StompClient::processConnect(StompFrame& frame) {
	if (frame.operation.compare("CONNECTED") != 0) {
		std::stringstream buf;
		buf << "Expected CONNECTED response, got '" << frame.operation << "'";
		throw StompException(buf.str());
	}
	sessionID = frame.properties["session"];

}// end StompClient::processConnect()

void StompClient::disconnect() {
	StompFrame frame;
	frame.operation = "DISCONNECT";
	sendFrame(frame);
	stompStream.close();

}// end StompClient::disconnect()

const std::string& StompClient::getSessionID() const {
	return sessionID;
}

void StompClient::sendMessage(const std::string& dest, const std::string& msg) throw (StompException) {
	std::map< std::string, std::string > props;
	sendMessage( dest, msg, props );
}

void StompClient::sendMessage(const std::string& dest, 
		const std::string& msg,
		const std::map< std::string, std::string >& properties )
	throw (StompException)
{
	StompFrame frame;
	frame.operation = "SEND";
	frame.properties = properties;
	frame.properties["destination"] = dest;
	frame.message << msg;
	sendFrame( frame );
}

void StompClient::sendMessage( const std::string& dest, const StompFrame& frame ) throw ( StompException ) {
	sendMessage( dest, frame.message.str(), frame.properties );
}

void StompClient::subscribe(
	const std::string& dest, 
	const AcknowledgeMode mode, 
	//const std::string& selector,
	const std::vector<std::string>& selectors,// JMP change to vector
	const std::string& messageID) { // messageID added by JMP
	StompFrame frame;
	frame.operation = "SUBSCRIBE";
	frame.properties.insert(std::pair<std::string, std::string>(
	        "destination",
	        dest));
	/*
	 * Figure out our acknowledgment mode
	 */
	std::string ack;
	if (mode == Auto) {
		ack = "auto";
	} else if (mode == Client) {
		ack = "client";
	} else {
		throw StompException("Currently unsupported ack mode");
	}
	frame.properties.insert(std::pair<std::string, std::string>("ack", ack));

	// insert zero or more selectors and messageID JMP mod
	//if(std::strcmp(selector.c_str(), "") != 0)
	//	frame.properties.insert(std::pair<std::string, std::string>("selector", selector));
	if (!selectors.empty())
		for (std::string adv : selectors)
			frame.properties.insert(std::pair<std::string, std::string>("selector", adv));
	if (messageID != "")
		frame.properties.insert(std::pair<std::string, std::string>("id", messageID));// JMP
	/*
	 *
	 */
	sendFrame(frame);
}

void StompClient::unsubscribe(const std::string& dest) {
	StompFrame frame;
	frame.operation = "UNSUBSCRIBE";
	frame.properties.insert(std::pair<std::string, std::string>(
	        "destination",
	        dest));
	sendFrame(frame);
}

/**
 * @since 0.0.1
 * @deprecated Use the getInput() implementation instead, will be removed in 2.0 release
 * @return the input stream used by the STOMP client
 */
	std::istream& StompClient::input() { return getInput(); }
/**
 * @since 1.1.0
 * @return the input stream used to receive data from the STOMP client
 */
	//std::istream& StompClient::getInput() { return inputSource; }
	std::istream& StompClient::getInput() { return stompStream; }
/**
 * @since 1.1.0
 * @return the output stream used to send data to the remote STOMP client
 */
	//std::ostream& StompClient::getOutput() { return outputSink; }
	std::ostream& StompClient::getOutput() { return stompStream; }

void StompClient::acknowledge(const std::string &msgid) {
	StompFrame frame;
	frame.operation = "ACK";
	frame.properties.insert(std::pair<std::string, std::string>(
	        "message-id",
	        msgid));
	sendFrame(frame);
}

/******************************************************************************
 * StompException
 *****************************************************************************/
StompException::StompException(const std::string& msg) :
	message(msg) {

}
StompException::~StompException() throw () {

}
const char* StompException::what() const throw () {
	return message.c_str();
}

