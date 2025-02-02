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
#include "C2SIMClientLib.h"
#include <iostream>
#include <sstream>
#include <time.h>

// Boost
#include <boost/asio.hpp>

//  version for all of C2SIMClientLib
const std::string version = "4.6.3.11";

using namespace std;

boost::asio::ip::tcp::iostream restStream;

std::string sendDateTime();

std::string host = "10.2.10.30";	// REST server default IP address
std::string port = "8080";			// REST server default port
std::string path = "";				// path for HTTP header
std::string submitter = "VRFORCES";	// used to identifier source of XML
std::string firstForwarder = "";	// used with chained servers to block loops
std::string stompServer = "";		// not used- provided for backward compatibility
std::string protocol = "";			// BML, CWIX or C2SIM
std::string domain = "ibml09";		// no longer used but older code might set it
std::size_t bufferLength = 10000;

C2SIMHeader* c2s;

std::string XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

/**
*  BMLClientREST_Lib
* BML Server Web Services REST Client<p>
* This client does the following:<p>
*      Open a connection with the server on specified port (Default is 8080)<p>
*      Build an HTTP POST transaction from parameters and BML XML document<p>
*      Submit the transaction<p>
*      Read the result<p>
*      Disconnect from the server<p>
*      Return the result received from the server to the caller<p>
*/
/****************************************/
/* BMLClientREST_Lib() - Constructor    */
/****************************************/
// constructor used with BML: not C2SIM header
BMLClientREST_Lib::BMLClientREST_Lib(){
	// create a C2SIM header and populate its random IDs
	try {
		c2s = new C2SIMHeader();
		c2s->generateConversationID();
		c2s->generateMessageID();
		c2s->setFromSendingSystem("");
		c2s->setToReceivingSystem("");
		c2s->setPerformative("");
	}
	catch (const std::exception& e) {
		throw BMLClientException(
			"can't make C2SIM Header: " + std::string(e.what()));
	}
}

/**
* Constructor used when the intention is to send a C2SIM document. <BR>
* The supplied parameters will be saved and used to create the C2SIM message header
*	when bmlRequest is called to make the submission
* @param sender   -   C2SIMHeader - Sender of document
* @param receiver -   C2SIMHeader - Receiver of document
* @param performative - C2SIMHeader - Action that receiver is to  perform
*/
BMLClientREST_Lib::BMLClientREST_Lib(
	std::string sender, 
	std::string receiver, 
	std::string performative) {

	// create a C2SIM header and populate its random IDs
	try {
		c2s = new C2SIMHeader();
		c2s->generateConversationID();
		c2s->generateMessageID();
		c2s->setFromSendingSystem(sender);
		c2s->setToReceivingSystem(receiver);
		c2s->setCommunicativeActTypeCode(performative);
		c2s->setSendingTime(sendDateTime()); 
	}
	catch (const std::exception& e) {
		throw BMLClientException(
			"can't make C2SIM Header: " + std::string(e.what()));
	}

}// end constuctor

// destructor
BMLClientREST_Lib::~BMLClientREST_Lib() {}

/***********************/
/*  formatGmtDateTime  */
/***********************/
/**
*  Internal function to capture current GMT date and time a strings
*/
std::string sendDateTime() {
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

/**************/
/* getVersion */
/**************/
/**
* getVersion
* @return current version of C2SIMClientLib
*/
std::string BMLClientREST_Lib::getVersion() {
	return version;
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
std::string parseXML(std::string xmlString, std::string tagName) {

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
	return xmlString.substr(startData, endData - startData);

}// end parseXML()

/***************************/
/* configureSocketTimeouts */
/***************************/
/**
*	internal function to adjust socket timeout to 5 seconds
* adapted from Carlo Medas 31Jan2012 posting on StackOverflow
*/
void configureSocketTimeouts(boost::asio::ip::tcp::socket& socket)
{
	int32_t timeout = 5000;
	setsockopt(socket.native_handle(), SOL_SOCKET, SO_RCVTIMEO, (const char*)&timeout, sizeof(timeout));
	setsockopt(socket.native_handle(), SOL_SOCKET, SO_SNDTIMEO, (const char*)&timeout, sizeof(timeout));

	/* Linux/MacOSX option (not implemented)
	struct timeval tv;
	tv.tv_sec = 5;
	tv.tv_usec = 0;
	setsockopt(socket.native(), SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
	setsockopt(socket.native(), SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
    */
}

/*****************************/
/*  httpRestTransaction()    */
/*****************************/
/**
* Internal function to send http string
* and return its reponse
* build of http without library by JMP 16May18
*
* @param xml - xml to post, if none ""
* @param transactionType - http option "GET" or "POST"
* @param parameters - C2SIM http parameters or "" 
* @param contentType - the http Content-Type if posting, else "" (no \r\n)
* @param acceptType - the http Accept type (no \r\n)
* @param isQueryInit - the tranction is a QUERYINIT
* @return the response string from the server
*/
std::string httpRestTransaction(
	std::string xml,
	std::string transactionType,
	std::string parameters,
	std::string contentType,
	std::string acceptType)
	throw (BMLClientException)
{
	// exchange C2SIM HTTP transaction with server
	// open a boost connection
	try {
		// Establish the connection and associate it with RestClient
		restStream.connect(host.c_str(), port.c_str());
		if (!restStream){
			throw BMLClientException(
				"error in BMLClientREST_Lib opening REST connection to " +
				host + ":" + port);
		}
	}
	catch (const std::exception& e) {
		throw BMLClientException(
			"error in BLClientREST_Lib opening REST connection to " +
			host + ":" + port + " " + e.what());
	}
	
	// assemble http request header
	std::string httpRequest;
	httpRequest += transactionType + " /" + path;
	if (parameters != "")
		httpRequest += parameters;
	httpRequest += " HTTP/1.1\r\n";
	if (contentType != "")
		httpRequest += "Content-Type:" + contentType + "\r\n";
	httpRequest += "Accept:" + acceptType + "\r\n";
	httpRequest += "Host:" + host + ":8080\r\n";
	httpRequest += "Connection:.keep-alive\r\n";
	if (xml == "") httpRequest += "\r\n";
	else {
		httpRequest += "Content-Length:";
		std::stringstream transBuf;
		transBuf << xml;
		std::stringstream lengthBuf;
		lengthBuf << transBuf.str().length();
		httpRequest += lengthBuf.str() + "\r\n";
		httpRequest += "\r\n";
		httpRequest += xml + "\r\n";
	}
	try {
		// send the header and XML document
		restStream << httpRequest;
		restStream.flush();
		
		// receive http response and check that response is OK.
		std::string httpVersion;
		restStream >> httpVersion;
		unsigned int statusCode;
		restStream >> statusCode;
		if (statusCode != 200)
		{
			throw BMLClientException(
				"REST response returned with status code " + statusCode);
		}
		std::string statusMessage;
		std::getline(restStream, statusMessage);
		if (!restStream || httpVersion.substr(0, 5) != "HTTP/") {
			throw BMLClientException(
				"invalid REST response in BMLClientREST" + httpVersion);
		}

		// read past the response headers, which are terminated by a blank line
		// in the process capture Content-Length
		std::string responseLine;
		int contentLength = bufferLength;
		while (!restStream.eof() && !restStream.bad()) {
			std::getline(restStream, responseLine);
			if (responseLine.substr(0, 15) == "Content-Length:") {
				contentLength = std::stoi(responseLine.substr(15));
			}
			if (responseLine.length() <= 1) break;
		}
		if (restStream.eof() || restStream.bad()) {
			throw BMLClientException(
				"error reading HTTP response header in REST\n");
		} 

		// read the response
		char* httpResponse = new char[bufferLength + 1];
		httpResponse[0] = '\0';
		std::string responseString = "";
		if (contentLength < bufferLength){

			// found contentLength in the header; just read that amount
			// NOTE: this depends on correct value of 
			// Content-Length in server output
			restStream.read(httpResponse, contentLength);
			if (restStream.bad()) {
				throw BMLClientException(
					"error reading HTTP response in REST\n");
			}
			// null-terminate response
			// and make it into a std::string
			httpResponse[contentLength] = '\0';
			responseString = std::string(httpResponse);
		}
		else {
			// did not find contentLength so
			// pull out the segments of content 
			// assemble them into response
			// each segment starts with count in hex
			while (true) {
				std::string hexLength, segment;

				// get length of next segment from http stream
				std::getline(restStream, hexLength);
				int segmentLength = strtol(hexLength.c_str(), NULL, 16);
				if (segmentLength == 0)break;

				// read the segment
				httpResponse[0] = '\0';
				restStream.read(httpResponse, segmentLength+2);
				if (restStream.bad()) {
					throw BMLClientException(
						"error reading HTTP response in REST\n");
				}

				// null-terminate response
				// and make it into a std::string
				httpResponse[segmentLength] = '\0';
				segment = std::string(httpResponse);
				responseString += segment;
			}
		}

		// return the result
		delete httpResponse;
		restStream.clear();
		restStream.close();
		return responseString;
	}
	catch (const std::exception& e) {
		restStream.clear();
		restStream.close();
		throw BMLClientException(
			"exception in BMLClientREST_Lib: " + std::string(e.what()));
	}

}// end httpRestTransaction()

/***********************/
/*  formatGmtDateTime  */
/***********************/
/**
 *  Internal function to capture current GMT date and time a strings
 */
void formatGmtDateTime(std::string* date, std::string* time, long* lStartTime){
	// use Microsoft system function
	SYSTEMTIME st;
	GetSystemTime(&st);
	char systime[13], sysdate[11];

	// format to Java SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")
	sprintf_s(systime,13, "%02d:%02d:%02d,%03d", st.wHour, st.wMinute, st.wSecond, st.wMilliseconds);
	sprintf_s(sysdate,11, "%04d-%02d-%02d", st.wYear, st.wMonth, st.wDay);
	std::string formatDate = sysdate;
	std::string formatTime = systime;
	*date = formatDate;
	*time = formatTime;
	*lStartTime = 3600000 * st.wHour + 60000 * st.wMinute + 1000 * st.wSecond + st.wMilliseconds;
}

/********************/
/* serverStatus     */
/********************/
/**
* Get status of C2SIM Server. - Confirm that server is running and return initialization status<BR>
* setHost()  and setSubmitter() must have must have been executed before calling this method.<BR>

@return - XML Document indicating current status of the server.<BR>
Sample output:<BR>
<pre>{@code
<?xml version="1.0" encoding="UTF-8"?>
<result>
<status>OK</status>
<message>Server is operating</message>
<serverInitialized>false</serverInitialized>
<sessionInitialized>false</sessionInitialized>
<unitDatabaseName>defaultDB</unitDatabaseName>
<unitDatabaseSize>0</unitDatabaseSize>
<msgNumber>0</msgNumber>
<time> 0.000</time>
</result>
}</pre>
@throws BMLClientException
*/
std::string BMLClientREST_Lib::serverStatus()
throw (BMLClientException) {

	std::string output;
	std::string result = "";
	setPath("BMLServer/status");

	result = httpRestTransaction("", "GET", "", "", "text/plain");

	// Did we get an error from the server? (The server returns XML)
	if (!result.find("<status>Error</status>"))
		throw BMLClientException("Error received in serverStatus\n" + result.substr(100));
	return result;

}// end serverStatus()

/************************/
/* c2simCommand         */
/************************/
/**
* c2simCommand pass a command to the C2SIM Server <BR>
* Current commands are NEW, LOAD, SAVE, SAVEAS, DELETE, SHARE, QUERYUNIT, QUERYINIT<BR>
* See the latest version of <i>C2SIM Server Reference Implementation</i> for details.<BR>
* Result is an XML document which may contain {@code<status>OK</status>} or may be actual data depending on the command submitted.<BR>
*
@param cmd      Command to be processed.
@param parm1    Optional first parameter
@param parm2    Optional second parameter
@return String result - XML Document giving results of command and server status similar to serverStatus method.
@throws BMLClientException = Primary and secondary causes are transmitted within the BMLClientException object.
*/
std::string BMLClientREST_Lib::c2simCommand(
	std::string cmd,
	std::string parm1,
	std::string parm2)
	throw (BMLClientException)
{
	std::string output;
	std::string result = "";
	setPath("BMLServer/command");
	std::string xml = "<C2SIM_Statistics xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\"/>";

	// Make sure the required parameters have been provided
	if (submitter == "")
		throw new BMLClientException("Error - Submitter not specified");
	if (cmd.empty())
		throw BMLClientException("Error in c2simCommand- No command specified");
	if (parm1.empty())
		parm1 = "";
	if (parm2.empty())
		parm2 = "";

	// Build the parameter string to include
	try {
		std::string parameters
			= "?submitter=" + submitter
			+ "&command=" + cmd
			+ "&parm1=" + parm1
			+ "&parm2=" + parm2;

		// do a POST with only XML header, no body; only parameters
		result = httpRestTransaction(
			xml, "POST", parameters, "application/xml", "application/xml");
	} // try
	catch (const std::exception& e) {
		throw BMLClientException(
			"BMLClientException in c2simCommand: " + std::string(e.what()));
	}
	return result;

} // end c2simCommand

/********************/
/* c2simRequest     */
/********************/
/**
* Submit a request to a BML/C2SIM Server <BR>
* This method performs the same function as the bmlRequest method 
* and is included as part of the migration from BML to C2SIM
@param xml - The xml document being submitted
@return - Indication of success of operation along with server status.
	See serverStatus method.
@throws BMLClientException - Primary and secondary causes will 
	be included in BMLClientException object
*/
std::string BMLClientREST_Lib::c2simRequest(std::string xml) {
	try {
		return BMLClientREST_Lib::bmlRequest(xml);
	}
	catch (const std::exception& e) {
		throw BMLClientException(
			"exception in BMLClientREST_Lib: " + std::string(e.what()));
	}
} // end c2simRequest()

/********************/
/*  bmlRequest()    */
/********************/
/**
* Submit a bml transaction to the host specified
* @param xmlTransaction - An XML string containing the bml
* @return - The response returned by the host BML server
* build of http without library by JMP 16May18
*/
std::string BMLClientREST_Lib::bmlRequest(std::string xml)
{
	std::string header, body,  msg = "";

	// build parameter string for http header
	setPath("BMLServer/bml");

	// Make sure the required parameters have been provided
	if (submitter == "") {
		throw BMLClientException(
			"error in BMLClientREST_Lib::bmlRequest - Submitter not specified");
	}

	// check for missing 'protocol'
	if (protocol == "") {
		throw BMLClientException(
			"error in BMLClientREST_Lib::bmlRequest - Protocol not specified");
	}

	if (protocol == "C2SIM"){
		// check for missing 'FromSendingSystem'
		if (c2s->getFromSendingSystem() == "") {
			throw BMLClientException(
				"error in BMLClientREST_Lib::bmlRequest - FromSendingSystem not specified");
		}

		// check for missing 'ToReceivingSystem'
		if (c2s->getToReceivingSystem() == "") {
			throw BMLClientException(
				"error in BMLClientREST_Lib::bmlRequest - ToReceivingSystem not specified");
		}
		// check for missing 'SendingTime'
		if (c2s->getSendingTime() == "") {
			throw BMLClientException(
				"error in BMLClientREST_Lib::bmlRequest - SendingTime not specified");
		}
	}

	// if message is BML we have nothing more to do
	if (protocol == "BML")msg = xml;


	//   A preliminary version of C2SIM was used for the CWIX18 exercise.  That version is now termed CWIX
	//   and C2SIM is used for the "Official" version.  If the client is still using C2SIM with a CWIX payload
	//   we detect it here, generate the proper header and use the proper value for the protocol parameter

	// We need to construct a  message, XMLPreamble + Header + XML.
	// Header may be C2SIM, CWIX, or BML (Blank header)

	// If protocol is CWIX generate a CWIX header and insert it into the message
	if (protocol =="CWIX") {

		// The client should have implicitly created a C2SIM heaader by calling 
		// the BMLClientREST_Lib three parameter constructor.

		// Create a CWIX header using the parameters used in the BMLClientREST_Lib constructor
		CWIXHeader* cwx = new CWIXHeader();
		msg = cwx->insertCWIX(xml, 
			c2s->getFromSendingSystem(), 
			c2s->getToReceivingSystem(), 
			c2s->getCommunicativeActTypeCode());

	}   // CWIX

	else if (protocol == "C2SIM") {

		msg = c2s->insertC2SIM(xml,
			c2s->getFromSendingSystem(),
			c2s->getToReceivingSystem(),
			c2s->getCommunicativeActTypeCode(),
			c2s->getSendingTime());

	}   // C2SIM
	
	// build the HTTP parameter string
	std::string parameters;
	parameters += "?submitterID=" + submitter;
	parameters += "&protocol=" + protocol;
	parameters += "&version=" + this->getVersion();
	if (protocol.compare("C2SIM") == 0)
		parameters += "&sender=" + c2s->getFromSendingSystem()
		+ "&receiver=" + c2s->getToReceivingSystem()
		+ "&conversationid=" + c2s->getConversationID();

	// If first forwarder is set add it
	if (firstForwarder.compare("") != 0)
		parameters += "&forwarders=" + firstForwarder;
	
	// Record the start time
	std::string startDate, startTime;
	long lStartTime;
	formatGmtDateTime(&startDate, &startTime, &lStartTime);
	
	// post the XML transaction
	std::string result;
	try {
		result =
			httpRestTransaction(
			msg,
			"POST",
			parameters,
			"application/xml",
			"application/xml");
	}
	catch (const BMLClientException& e) {
		throw BMLClientException(
			"BMLClientException in bmlRequest XML post: " + std::string(e.what()));
	}
	catch (const std::exception& e) {
		throw BMLClientException(
			"exception in bmlRequest XML post: " + std::string(e.what()));
	}

	// Record the end time for the transaction
	std::string endDate, endTime;
	long lEndTime;
	formatGmtDateTime(&endDate, &endTime, &lEndTime);
	double dElapsedTime = ((double)(lEndTime - lStartTime))*.001f;
	
	// If the server indicates that response time statistics should be collected, send them.
	std::string elementValue = parseXML(result, "collectResponseTime");
	if (elementValue == "t" || elementValue == "T"){

		// Set up the xml with the response time of the first transaction
		setPath("BMLServer/stats");
		parameters = "?submitterID=" + submitter;
		char elapsedTime[15];
		sprintf_s(elapsedTime,15, "%.3f", dElapsedTime);
		std::string responseTimeResult = XML_PREAMBLE
			+ "<C2SIM_Statistics xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">"
			+ "<REST_ResponseTime>"
			+ "<submitterID>" + submitter + "</submitterID>"
			+ "<msgNumber>" + parseXML(result, "msgNumber") + "</msgNumber>"
			+ "<startTime>" + startDate + " " + startTime + "</startTime>"
			+ "<endTime>" + endDate + " " + endTime + "</endTime>"
			+ "<elapsedTime>" + elapsedTime + "</elapsedTime>"
			+ "<serverTime>" + parseXML(result, "time") + "</serverTime>"
			+ "</REST_ResponseTime></C2SIM_Statistics>";

		// send the responseTime message to server
		try {
			result =
				httpRestTransaction(
				responseTimeResult,
				"POST",
				parameters,
				"application/xml",
				"application/xml");
		}
		catch (const std::exception& e) {
			throw BMLClientException(
				"exception in bmlRequest responseTime post: " + std::string(e.what()));
		}
	}
	return result;

}// end BMLClientREST_Lib::bmlRequest	

/****************************/
/*  getters and setters     */	
/****************************/

/**
* Set buffer length (default is 10000)
*/
void BMLClientREST_Lib::setBufferLength(size_t size){
	bufferLength = size;
}

/**
* Set the port number (as a string)
* @param port
*/
void BMLClientREST_Lib::setPort(std::string p)
{
	port = p;
}

/**
* Set the host name or address
* @param host
*/
void BMLClientREST_Lib::setHost(std::string h)
{
	host = h;
}

/**
* Set the port number (as a string)
* @param path
*/
void BMLClientREST_Lib::setPath(std::string pt)
{
	path = pt;
}

/**
* Set the domain property.  Used to discriminate between BML dialects
* @param domain
*/
void BMLClientREST_Lib::setDomain(std::string d)
{
	domain = d;
}

/**
* Set the Requestor property indicating the identity of the client
* @param requestor
*/
void BMLClientREST_Lib::setSubmitter(std::string s)
{
	submitter = s;
}

/**
* Set the FirstForwarder property indicating first server to
* forward the XML
* @param firstForwarder
*/
void BMLClientREST_Lib::setFirstForwarder(std::string f)
{
	firstForwarder = f;
}

/**
* Set the protocol property
* @param protocol
*/
void BMLClientREST_Lib::setProtocol(std::string pr)
{
	protocol = pr;

}

/**
* Set the value of C2SIMHeader to be used with submission of C2SIM transaction
* @param c C2SIMHeader
*/
void  BMLClientREST_Lib::setC2SIMHeader(C2SIMHeader* c) {
	c2s = c;
} 

/**
* Set the stompServer property indicating STOMP Server host name (or IP)

* @param stompServer    String - Host name (or IP) of server providing stomp pub/sub services
*/
void  BMLClientREST_Lib::setStompServer(std::string ss) {
	stompServer = ss;
} 

/**
* Get the current setting of the port property
* @return port
*/
std::string BMLClientREST_Lib::getPort()
{
	return port;
}

/**
* Get the current setting of the path property
* @return
*/
std::string BMLClientREST_Lib::getPath()
{
	return path;
}

/**
*
* @return the current setting of the domain property
*/
std::string BMLClientREST_Lib::getDomain()
{
	return domain;
}

/**
* Get current setting of host property
* @return
*/
std::string BMLClientREST_Lib::getHost()
{
	return host;
}

/**
* Get current setting of Requestor property
* @return
*/
std::string BMLClientREST_Lib::getSubmitter()
{
	return submitter;
}

/**
* Get the FirstForwarder property indicating first server to
* forward the XML
*/
std::string BMLClientREST_Lib::getFirstForwarder()
{
	return firstForwarder;
}

/**
* Get the protocol property
*/
std::string BMLClientREST_Lib::getProtocol()
{
	return protocol;
}

/**
* Return value of C2SIM Header
@return C2SIMHeader the current C2SIM header.
*/
C2SIMHeader*  BMLClientREST_Lib::getC2SIMHeader() {
	return c2s;
} 

/**
* Get the stompServer property indicating name of host providing STOMP pub/sub services
* @return  String - Name (or IP)  identifying STOMP Server
*/
std::string  BMLClientREST_Lib::getStompServer() {
	return stompServer;
} 

							
