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
#pragma once
#include <string>
#include <vector>
#include <map>
#include <exception>
#include <stdio.h>

/**
*  version 4.6.3 converted from Doug Corner's Java
*/

class BMLClientException : public std::exception {

private:

	std::string msg;
	exception cause;

public:

	/// BML Exception caused by another exception
	BMLClientException(std::string m);

	/// BML Exception caused by another exception
	BMLClientException(std::string m, exception e);
	~BMLClientException();

	/**
	* Get message set in this exception when instantiated
	* @return String - Message included in constructor
	*/
	std::string getMessage();

	/**
	* Get message from another exception thrown by underlying software and included in this exception
	* @return String - Underlying cause message
	*/
	std::string getCauseMessage() const throw();

};

/**
*
* \brief BML Server Web Services REST Client
* This client does the following:
*       Open a connection with the server on specified port (Default is 8080)
*       Build an HTTP POST transaction from parameters and BML XML document
*       Submit the transaction
*       Read the result
*       Disconnect from the server
*       Return the result received from the server to the 
* @see BMLSTOMPMessage
*
* \author Douglas - George Mason University C4I and Cyber Center
* translated frpm Java by Aakarshika Priydarshi 
*
*/

class C2SIMHeader
{

private:

	// Instance variables


	/**
	* startPosition - variable shared by findStartTag and findEndTag
	*/
	size_t startPosition;

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

	std::string schemaProtocol = "C2SIM";

	/**
	* protocolVersion - The version of the protocol of the this message
	*/
	std::string protocolVersion = "1.1";

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

public:

	C2SIMHeader();
	~C2SIMHeader();

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
	std::string insertC2SIM(
		std::string xml, 
		std::string sender, 
		std::string receiver, 
		std::string performative,
		std::string isoDateTime);

	/********************/
	/* removeC2SIM      */
	/********************/
	/**
	* Remove C2SIM message envelope and return core xml message with header
	@param xml - String - Input xml message with C2SIM envelop
	@return String - Reconstructed message without C23SIM components
	*/
	std::string removeC2SIM(std::string xml);

	/*************/
	/*  xmlDoc   */
	/*************/
	/**
	*   xmlDoc - return a DOM Document representing this message header <BR>
	*   NOT IMPLEMENTED AT THIS TIME
	* @return null
	*/
	std::string toDoc();

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
	std::string extractDataForTag(std::string xmlString, std::string tagSought);

	/********************/
	/*  populateC2SIM   */
	/********************/
	/**
	* Populate a new C2SIMHeader object from an XML string.
	* @param xmlString - source of data to popuate the header
	* @throws BMLClientException - Thrown for one of the following reasons <BR>
	*  IOException in SAXBuilder
	*  JDOMException
	*/
	C2SIMHeader* populateC2SIM(std::string xmlString);

	/********************/
	/*  toXMLString     */
	/********************/
	/**
	* toXMLString
	* @return - XML String containing the contents of this C2SIM header object
	*/
	std::string toXMLString();

	/***********************************/
	/*  findStartTag and findEndTag     /
	/***********************************/
	/**
	* toXMLString
	* these two functions only work when findEndTag is called
	* with same tag and immediately after findStartTag
	* also they ignore any namespace prefix that is present
	*
	* findStartTag
	* @param std:string XML string
	* @param std:string tag to find
	* @param size_t offset in XML to start search
	* @return - size_T position where opening tag starts in XML
	*
	* findEndTag
	* @param std:string XML string
	* @param std:string tag to find
	* @param size_t offset in XML to start search
	* @return - size_t position where closing tag ends in XML
	*/
	size_t findStartTag(std::string xml, std::string tag, size_t offset);
	size_t findEndTag(std::string xml, std::string tag, size_t offset);


	/****************************/
	/*  getters and setters     */
	/****************************/

	/**
	* getCommunicativeActTypeCode - Get the value of the CommunicativeActTypeCode property - Indicates processing requested for this message
	@return - String - Possible values: Inform, Confirm, Refuse, Accept, Agree, Request
	*/
	std::string getCommunicativeActTypeCode();

	/**
	* setCommunicativeActTypeCode - Set value of CommunicativeActTypeCode property in current C2SIMHeader
	@param CommunicativeActTypeCode - String - Possible values see getPerformative()
	@see getCommunicativeActTypeCode#getCommunicativeActTypeCode
	*/
	void setCommunicativeActTypeCode(std::string communicativeActTypeCode);

	/**
	* setSecurityClassificationCode - Set field used to connect a series of messages into one conversation
	@param securityClassificationCode - String UUID  C2SIM securityClassificationCode .
	*/
	void setSecurityClassificationCode(std::string securityClassificationCode);

	/**
	* getSecurityClassificationCode - Get field used to connect a series of messages into one conversation
	@return - securityClassificationCode - String UUID
	*/
	std::string getSecurityClassificationCode();

	/**
	* setConversationID - Set field used to connect a series of messages into one conversation
	* @param conversationID - String UUID  C2SIM conversationID .
	*/
	void setConversationID(std::string cid);

	/**
	* getConversationID - Get field used to connect a series of messages into one conversation
	* @return - conversationID - String UUID
	*/
	std::string getConversationID();

	/**
	* generateConversationID - Generate a new Conversation ID (UIID format) for this C2SIM Header
	*/
	void generateConversationID();

	/**
	* getInReplyTo property indicating the system to reply to
	* @return InReplyTo - String - System identifier
	*/
	std::string getInReplyTo();

	/**
	* setInReplyTo  Set inReplyTo property indicating identification of message being replied to
	* @param inReplyTo - String messageID - UUID
	*/
	void setInReplyTo(std::string irt);

	/**
	* getMessageID - Get ID of message using current C2SIM header
	* @return messageID = String - UUID
	*/
	std::string getMessageID();

	/**
	* setMessageID - Set the message ID of the message using this C2SIM Header
	* @param messageID String - UUID
	*/
	void setMessageID(std::string mid);

	/**
	* generateMessageID - Generate ID for this C2SIM Message Header
	*/
	void generateMessageID();

	/**
	* setProtocol - Set field used to indicate the protocol of the message
	@param protocol - String UUID  C2SIM protocol .
	*/
	void  setProtocol(std::string protocolParm);

	/**
	* getProtocol - Get field used to indicate the protocol of the message
	@return - protocol - String UUID
	*/
	std::string getProtocol();

	/**
	* getProtocolVersion - Get field used to indicate the version of the protocol of the message
	@return - protocolVersion - String
	* NOTE: we don't support setProtocolVersion because it is intrinsic to ClientLib code.
	*/
	std::string getProtocolVersion();

	/**
	* getSendingTime - ISO Format time when C2SIMHeader object was instantiated
	*/
	std::string getSendingTime();

	/**
	* setSendingTime - Set sendingTime = ISO Format
	@param sendingTime
	*/
	void setSendingTime(std::string sendingTime);

	/**
	* getReplyToSystem = Get replyTo property indicating what system is to be replied to
	@return - System Identification - String
	*/
	std::string getReplyToSystem();

	/**
	* setReplyToSystem - Set replyTo property indicating what system is to be replied to
	@param replyTo
	*/
	void setReplyToSystem(std::string replyTo);

	/**
	* getPerformative - Get the value of the performative property - Indicates processing requested for this message
	* @return - String - Possible values: Inform, Confirm, Refuse, Accept, Agree, Request
	*/
	std::string getPerformative();

	/**
	* setPerformative - Set value of performative property in current C2SIMHeader
	* @param performative - String - Possible values see getPerformative()
	* @see getPerformative
	*/
	void setPerformative(std::string p);

	/**
	* getToReceivingSystem - Get value of intended receiver for this message
	* @return receiver - String
	*/
	std::string getToReceivingSystem();

	/**
	* setToReceivingSystem - Set the receiver property indicating the indented recipient for this C2SIM message
	* @param receiver - String
	*/
	void setToReceivingSystem(std::string r);

	/**
	* getReplyBy - Specifies date/time by which reply is needed
	@return DateTime as - yyyy-mm-ddThh:mm:ssZ
	*/
	std::string getReplyBy();

	/**
	* setReplyBy - Set date/time by which reply is requested
	* @param replyBy - Date/Time as yyyy-mm-ddThh:mm:ssZ
	*/
	void setReplyBy(std::string rb);

	/**
	* getReplyTo = Get replyTo property indicating what system is to be replied to
	* @return - System Identification - String
	*/
	std::string getReplyTo();

	/**
	* setReplyTo - Set replyTo property indicating what system is to be replied to
	* @param replyTo
	*/
	void setReplyTo(std::string rt);

	/**
	* getReplyWith - Get replyWith property indicating response required from receiver
	* @return - Performative - String
	*/
	std::string getReplyWith();

	/**
	* setReplyWith - Set replyWith property indicating response required from receiver
	* @param replyWith - Performative - String
	*/
	void setReplyWith(std::string rw);

	/**
	* ggetFromSendingSystem - Get the identification of the sender of thie message
	* @return System ID - String
	*/
	std::string getFromSendingSystem();

	/**
	* setFromSendingSystem - Set identification of the sender of this message
	* @param sender - System identifier - String
	*/
	void setFromSendingSystem(std::string s);

	/**
	* getInReplyToMessageID - Get ID message being replied to
	@return - String System ID UUID format
	*/
	std::string getInReplyToMessageID();

	/**
	* setInReplyToMessageID - Set ID message being replied to
	@param inReplyToMessageID- ID in UUID format
	*/
	void setInReplyToMessageID(std::string inReplyToMessageID);

};// end Class C2SIMHeader

class CWIXHeader
{
private:

	std::string XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	// Instance variables

	/**
	* startPosition - variable shared by findStartTag and findEndTag
	*/
	size_t startPosition;

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
	* messageID - Unique identifier for the message. 
	* Other messages refer to the message using this ID.
	*/
	std::string messageID = "";

	/**
	* performative - Communicative act; one of the values from 
	* enumCommunicativeActCategoryCode indicating type of message
	*/
	std::string performative = "";

	/**
	* Protocol and version
	*/
	std::string protocolAndVersion = "CWIX 1.0";

	/**
	* toReceivingSystem - Non empty list of unique identifiers of the receivers of the message
	*/
	std::string toReceivingSystem = "";

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
	* fromSendingSystem - Unique identifier of the unit or the system that sent the message.
	*/
	std::string fromSendingSystem = "";

public:
	CWIXHeader();
	~CWIXHeader();

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
	std::string insertCWIX(std::string xml, std::string sender, std::string receiver, std::string performative);

	/********************/
	/* removeC2SIM      */
	/********************/
	/**
	* Remove CWIX message envelope and return core xml message with header
	@param xml - String - Input xml message with C2SIM envelop
	@return String - Reconstructed message without C23SIM components
	*/
	std::string removeCWIX(std::string xml);

	/*************/
	/*  xmlDoc   */
	/*************/
	/**
	*   xmlDoc - return a DOM Document representing this message header <BR>
	*   NOT IMPLEMENTED AT THIS TIME
	* @return null
	*/
	std::string toDoc();

	std::string extractDataForTag(std::string xmlString, std::string tagSought);

	/**
	* Populate a CWIXHeader object from an XML string.
	* @param xmlString -
	* @return The CWIXHeader object that was passed as a parameter
	* @throws BMLClientException - Thrown for one of the following reasons <BR>
	*  IOException in SAXBuilder
	*  JDOMException
	*/
	CWIXHeader* populateCWIX(std::string xmlString);

	/********************/
	/*  toXMLString     /*
	/********************/
	/**
	* toXMLString
	* @return - XML String containing the contents of this C2SIM header object
	*/
	std::string toXMLString();

	/***********************************/
	/*  findStartTag and findEndTag     /
	/***********************************/
	/**
	* toXMLString
	* these two functions only work when findEndTag is called
	* with same tag and immediately after findStartTag
	* also they ignore any namespace prefix that is present
	*
	* findStartTag
	* @param std:string XML string
	* @param std:string tag to find
	* @param size_t offset in XML to start search
	* @return - size_T position where opening tag starts in XML
	*
	* findEndTag
	* @param std:string XML string
	* @param std:string tag to find
	* @param size_t offset in XML to start search
	* @return - size_t position where closing tag ends in XML
	*/
	size_t findStartTag(std::string xml, std::string tag, size_t offset);
	size_t findEndTag(std::string xml, std::string tag, size_t offset);

	/**
	*     Getters and Setters are implemented for all instance variables.
	*/
	/**
	* setConversationID - Set field used to connect a series of messages into one conversation
	@param conversationID - String UUID  C2SIM conversationID .
	*/
	void setConversationID(std::string conversationID);

	/**
	* getConversationID - Get field used to connect a series of messages into one conversation
	@return - conversationID - String UUID
	*/
	std::string getConversationID();

	/**
	* generateConversationID - Generate a new Conversation ID (UIID format) for this C2SIM Header
	*/
	void generateConversationID();

	/**
	* getInReplyTo property indicating the system to reply to
	* @return InReplyTo - String - System identifier
	*/
	std::string getInReplyTo();

	/**
	* setInReplyTo  Set inReplyTo property indicating identification of message being replied to
	@param inReplyTo - String messageID - UUID
	*/
	void setInReplyTo(std::string inReplyTo);

	/**
	* getMessageID - Get ID of message using current C2SIM header
	@return messageID = String - UUID
	*/
	std::string getMessageID();

	/**
	( setMessageID - Set the message ID of the message using this C2SIM Header
	@param messageID String - UUID
	*/
	void setMessageID(std::string messageID);

	/**
	* generateMessageID - Generate ID for this C2SIM Message Header
	*/
	void generateMessageID();

	/**
	* getPerformative - Get the value of the performative property - Indicates processing requested for this message
	@return - String - Possible values: Inform, Confirm, Refuse, Accept, Agree, Request
	*/
	std::string getPerformative();

	/**
	* setPerformative - Set value of performative property in current CWIXHeader
	@param performative - String - Possible values see getPerformative()
	@see getPerformative
	*/
	void setPerformative(std::string performative);

	/**
	* getToReceivingSystem - Get value of intended receiver for this message
	* @return receiver - String
	*/
	std::string getToReceivingSystem();

	/**
	* setToReceivingSystem - Set the receiver property indicating the indented recipient for this C2SIM message
	* @param receiver - String
	*/
	void setToReceivingSystem(std::string r);

	/**
	* getProtocolAndVersion - Get the protocol and current version
	*/
	std::string getProtocolAndVersion();

	/**
	* There is no setter for protocol and version
	*/

	/**
	* getReplyBy - Specifies date/time by which reply is needed
	@return DateTime as - yyyy-mm-ddThh:mm:ssZ
	*/
	std::string getReplyBy();

	/**
	* setReplyBy - Set date/time by which reply is requested
	@param replyBy - Date/Time as yyyy-mm-ddThh:mm:ssZ
	*/
	void setReplyBy(std::string replyBy);

	/**
	* getReplyTo = Get replyTo property indicating what system is to be replied to
	@return - System Identification - String
	*/
	std::string getReplyTo();

	/**
	* setReplyTo - Set replyTo property indicating what system is to be replied to
	@param replyTo
	*/
	void setReplyTo(std::string replyTo);

	/**
	* getReplyWith - Get replyWith property indicating response required from receiver
	@return - Performative - String
	*/
	std::string getReplyWith();

	/**
	* setReplyWith - Set replyWith property indicating response required from receiver
	@param replyWith - Performative - String
	*/
	void setReplyWith(std::string replyWith);

	/**
	* getFromSendingSystem - Get the identification of the sender of thie message
	* @return System ID - String
	*/
	std::string getFromSendingSystem();

	/**
	* setFromSendingSystem - Set identification of the sender of this message
	* @param sender - System identifier - String
	*/
	void setFromSendingSystem(std::string s);

};// end class CWIXHeader

class BMLClientREST_Lib
{
public:

	/****************************************/
	/* BMLClientREST_Lib() - Constructors   */
	/****************************************/
	/**
	* BMLClientREST_Lib Constructor w/o parameters
	*/
	BMLClientREST_Lib();
	~BMLClientREST_Lib();

	/**
	* getVersion
	* @return current version of C2SIMClientLib
	*/
	std::string getVersion();

	/**
	* Constructor used when the intention is to send a C2SIM document. <BR>
	* The supplied parameters will be saved and used to create the C2SIM message header 
	*	when bmlRequest is called to make the submission
	* @param sender   -   C2SIMHeader field - Sender of document
	* @param receiver -   C2SIMHeader field - Receiver of document
	* @param performative - C2SIMHeader field - Action that receiver is to  
	*	perform as specified by the C2SIM specification
	*/
	BMLClientREST_Lib(std::string sender, std::string receiver, std::string performative);

	/********************/
	/* serverStatus     */
	/********************/
	/**
	* Get status of C2SIM Server. - Confirm that server is running and return initialization status<BR>
	* setHost()  and setSubmitter() must have must have been executed before calling this method.<BR>
	@return - XML Document indicating current status of the server.<BR>
	@throws BMLClientException - Primary and secondary causes will be available in
	Sample output:
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
	*/
	std::string serverStatus();

	/************************/
	/* c2simCommand         */
	/************************/
	/**
	* c2simCommand pass a command to the C2SIM Server <BR>
	* Current commands are NEW, LOAD, SAVE, SAVEAS, DELETE, SHARE, QUERYUNIT, QUERYINIT<BR>
	* See the latest version of <i>C2SIM Server Reference Implementation</i> for details.<BR>
	* Result is an XML document which may contain {@code<status>OK</status>} or may be actual 
	*	data depending on the command submitted.<BR>
	*
	@param cmd      Command to be processed.
	@param parm1    Optional first parameter
	@param parm2    Optional second parameter
	@return String result - XML Document giving results of command and server status similar to serverStatus method.
	@throws BMLClientException = Primary and secondary causes are transmitted within the BMLClientException object.
	*/
	std::string c2simCommand(
		std::string cmd,
		std::string parm1,
		std::string parm2);

	/********************/
	/* c2simRequest     */
	/********************/
	/**
	* Submit a request to a BML/C2SIM Server <BR>
	* This method performs the same function as the bmlRequest method and is 
	*	included as part of the migration from BML to C2SIM
	@param xml - The xml document being submitted
	@return - Indication of success of operation along with server status.  See serverStatus method.
	@throws BMLClientException - Primary and secondary causes will be included in BMLClientException object
	*/
	std::string c2simRequest(std::string xml);

	/********************/
	/*  bmlRequest()    */
	/********************/
	/**
	* Submit a BML transaction to the host specified
	* Request string using default is:
	* {@code "http://10.2.10.30::8080/BMLServer/bml?submitterID=jmp&forwarder=" }
	* @param xml - An XML string containing the bml
	* @return  XML - The response returned by the host BML server
	* @see edu.gmu.c4i.bmlclientlib2.BMLClientException
	* @throws edu.gmu.c4i.bmlclientlib2.BMLClientException - Various causes
	*/
	std::string bmlRequest(std::string xmlTransaction);

	/****************************/
	/*  getters and setters     */
	/****************************/

	/**
	* Set buffer length (default is 50000)
	*/
	void setBufferLength(size_t size);

	/**
	* Set the port number (as a string)
	* @param port
	*/
	void setPort(std::string port);

	/**
	* Set the host name or address
	* @param host
	*/
	void setHost(std::string host);

	/**
	* Set the port number (as a string)
	* @param path
	*/
	void setPath(std::string path);

	/**
	* Set the domain property.  Used to discriminate between BML dialects
	* @param domain
	*/
	void setDomain(std::string domain);

	/**
	* Set the Requestor property indicating the identity of the client
	* @param requestor
	*/
	void setSubmitter(std::string submitter);

	/**
	* Set the protocol property
	* @param protocol
	*/
	void setProtocol(std::string protocolParm);

	/**
	* Set the FirstForwarder property indicating first server to
	* forward the XML
	* @param firstForwarder
	*/
	void setFirstForwarder(std::string firstForwarder);

	/**
	* Set the value of C2SIMHeader to be used with submission of C2SIM transaction
	@param c C2SIMHeader
	*/
	void setC2SIMHeader(C2SIMHeader* ch);

	/**
	* Set the stompServer property indicating STOMP Server host name (or IP)

	* @param stompServer    String - Host name (or IP) of server providing stomp pub/sub services
	*/
	void  setStompServer(std::string ss);

	/**
	* Get the current setting of the port property
	* @return port
	*/
	std::string getPort();
	/**
	* Get the current setting of the path property
	* @return
	*/
	std::string getPath();

	/**
	* Get current setting of host property
	* @return
	*/
	std::string getHost();

	/**
	*
	* Returns the current setting of the domain property
	* @return  the current setting of the domain property
	*/
	std::string getDomain();

	/**
	* Get current setting of Requestor property
	* @return
	*/
	std::string getSubmitter();

	/**
	* Get the protocol property
	*/
	std::string getProtocol();

	/**
	* Get the FirstForwarder property indicating first server to
	* forward the XML
	*/
	std::string getFirstForwarder();

	/**
	* Return value of C2SIM Header
	@return C2SIMHeader the current C2SIM header.
	*/
	C2SIMHeader* getC2SIMHeader();

	/**
	* Get the stompServer property indicating name of host providing STOMP pub/sub services
	* @return  String - Name (or IP)  identifying STOMP Server
	*/
	std::string  getStompServer();
};

class BMLSTOMPMessage
{
public:

	BMLSTOMPMessage();
	~BMLSTOMPMessage();

	/**
	 * Add header to message
	 */
	void addHeader(std::string s);

	/**
	 * import reference to the StompClient frame.properties
	 * for use as headerMap
	 */
	void setHeaderMap(std::map<std::string, std::string> frameProperties);

	/* 
	 * Add line to message body
	 */
	void addToBody(std::string s);

	/**
	* error Description of Exception caught in foreground thread.  Used to communicate exception to background.
	*      May be an otherwise empty message
	*/
	std::string error;

	/**
	* getMessageType - Returns the STOMP command for this message.  Normally CONNECTED or MESSAGE
	* @return String - The STOMP COMMAND for this message
	*/
	std::string getMessageType();

	/**
	* setMessageType - Saves the STOMP command for this message.  Normally CONNECTED or MESSAGE
	* @param type e.g. - The STOMP COMMAND for this message
	*/
	void setMessageType(std::string type);

	/**
	* Return the BML message type determined when the server receives the message from its creator
	* @return String - BML Message Selector e.g. IBML09GSR
	*/
	std::string getMessageSelector();

	/**
	* Return the body of the message, i.e. the part of the message following the headers.  
	* Does not include the terminating NULL
	* @return String - The message body from the STOMP Message
	*/
	std::string getMessageBody();

	/**
	* Sets value of body of the message, i.e. the part of the message following the headers.  
	* Does not include the terminating NULL
	* @param body - message content
	*/
	void setMessageBody(std::string body);

	/**
	* Get the length of the message determined internally
	* @return Long - The message length
	*/
	long getMessageLength();

	/**
	* set messageLength for this message
	*/
	void setMessageLength(long messageMengthParm);

	/**
	* set contentLength for this message
	*/
	void setContentLengthFromStompHeader(long contentLengthParm);

	/**
	* Get the length of the message as determined by the content-length header
	* @return Long - The content length
	*/
	long getContentLength();

	/**
	* getC2SIMHeader
	* @return C2SIHeader the c2sim header from this essage
	*/
	C2SIMHeader* getC2SIMHeader();

	/**
	* setC2SIMHeader
	* @paramn C2SIHeader the c2sim header from this essage
	*/
	void setC2SIMHeader(C2SIMHeader* c2simHeaderParm);

	/**
	* Return value of CWIX Header
	* @return CWIXHeader the c2sim header from this message
	*/
	CWIXHeader* getCWIXHeader();

	/**
	* setCWIXHeader
	* @param CWIXHeader the cwix header from this essage
	*/
	void setCWIXHeader(CWIXHeader* cwixHeaderParm);

	/**
	* Get the contents of a specific STOMP header
	* @param header e.g. "content-length"
	* @return - String - Value of header or "" if header not set
	*/
	std::string getHeader(std::string header);

};

class BMLClientSTOMP_Lib
{
private:
	std::string XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	std::string END_OF_FRAME = "\x0000";// was "\u0000"
	std::string host = "10.2.10.30";			// STOMP server IP address
	std::string port = "61613";					// STOMP port on server
	std::string destination = "/topic/BML";     // queue or topic filtered
	std::vector<std::string> adv_subscriptions;	// STOMP subscription expressions 
	BMLSTOMPMessage* currentMsg;				// message now in play
	std::string messageSelector = "";			// picks message subscribed to

public:
	/**************************************/
	/* BMLClientSTOMP_Lib() Constructor   */
	/**************************************/
	/**
	* Constructor - No parameters
	*/
	BMLClientSTOMP_Lib();

	/**************************************/
	/* BMLClientSTOMP_Lib() Destructor   */
	/**************************************/
	/*
	* Destructor - Default - No parameters
	*/
	~BMLClientSTOMP_Lib();

	/****************/
	/* connect      */
	/****************/
	// Connect to STOMP Host
	//  Wait for CONNECTED Message
	/**
	* Connect to Stomp host
	* @return STOMPMessage - Response from host if connection made otherwise throw an exception.  Response should be CONNECTED.
	* @see edu.gmu.c4i.bmlclientlib2.BMLClientException
	* @see edu.gmu.c4i.bmlclientlib2.BMLSTOMPMessage
	* @throws BMLClientException - Includes various exceptions
	*/
	BMLSTOMPMessage* connect();

	/****************/
	/* publish      */
	/****************/
	/**
	* Send message to STOMP host on connection already established
	* @param xml - The message to be sent
	* @param headers - A Vector Strings containing STOMP headers in the form  headerName:headerValue
	*@throws BMLClientException
	*/
	void publish(std::string cmd, std::vector<std::string> headers, std::string xml);

	/*************************/
	/* getNext_NoBlock()     */
	/*************************/
	/**
	* Returns the next message received from the STOMP messaging server.
	* The calling thread will NOT be blocked if a STOMPMessage is not available; .
	* @return STOMPMessage - The next STOMP message or NULL if no message is available at this time.  Message should be MESSAGE.
	* @see edu.gmu.c4i.bmlclientlib2.BMLSTOMPMessage
	* @see edu.gmu.c4i.bmlclientlib2.BMLClientException
	* @throws BMLClientException - Encapsulates several different exceptions.
	*/
	BMLSTOMPMessage* getNext_NoBlock();

	/*************************/
	/* getNext_Block()       */
	/*************************/
	/**
	* Returns the message received from the STOMP messaging server.  The calling thread
	* will be blocked until a message has been received.
	* @return STOMPMessage - The next STOMP message.  Message should be MESSAGE.
	* @see edu.gmu.c4i.bmlclientlib2.BMLSTOMPMessage
	* @see edu.gmu.c4i.bmlclientlib2.BMLClientException
	* @throws BMLClientException - Encapsulates various exceptions
	*/
	BMLSTOMPMessage* getNext_Block();

	/************************/
	/* sendC2SIM_Response   */
	/************************/
	/**
	* sendC2SIM_Response - Send a C2SIM response to an incoming C2SIM request.  
	*	Response will be sent via STOMP
	* @param oldMsg - Message that is being responsed to
	* @param c2sResp - Response code to be sent*
	* @param ackCode - Code describing the acknowledgement
	* @throws edu.gmu.c4i.c2simclientlib2.BMLClientException 
	*	May throw a BMLClientException for several reasons <BR>
	*      IOException during send or close
	*      UnknownHost exception
	*      Received something other that "CONNECTED" during connection process
	*      InterruptedException while waiting for queue
	*      Error caught in foreground thread
	*/
	void sendC2SIM_Response(
		BMLSTOMPMessage* oldMsg,
		std::string c2sResp,
		std::string ackCode);

	/*******************/
	/* disconnect()    */
	/*******************/
	/**
	* Disconnect from STOMP server and close socket.
	* @return std::string - "OK" indicating successful completion of disconnect or else throws an exception
	* @throws BMLClientException - Encapsulates various exceptions
	*/
	std::string disconnect();

	/***********************/
	/* getters and setters  */
	/************************/
	// Getters and setters

	/**
	* setPort (std::string) for STOMP connection
	* @param port - std::string
	*/
	void setPort(std::string port);

	/**
	* setPort int for STOMP connection
	* @param port - int
	*/
	void setPort(int port);

	/**
	* @return int - Current port setting
	*/
	int getPort();

	/**
	* Set the host name or IP address
	* @param host std::string name or IP address of STOMP server
	*/
	void setHost(std::string host);

	/**
	* Get the address of the STOMP Messaging server
	* @return std::string - Host name/address
	*/
	std::string getHost();

	/**
	* Set the destination queue or topic
	* @param destination - std::string Name of topic.  The default is /topic/BML/
	*/
	void setDestination(std::string destination);

	/**
	* Set the MessageSelector
	* @param messageSelector - std::string MessageSelector used for subscribing to a particular message
	*/
	void setMessageSelector(std::string messageSelector);

	/**
	* Get the MessageSelector
	* @return messageSelector - std::string MessageSelector used for subscribing to a particular message
	*/
	std::string getMessageSelector();

	/**
	* addSubscription - Add a Message Selector to list of selectors submitted with SUBSCRIBE
	*   This is deprecated.
	*   Host will only publish messages matching one of the selectors.
	*   If no addSubscriptions are submitted then all messages will be received.
	* @param msgSelector std::string - Name of a BML Message Type to be added to subscription list.  
	*	If the list contains at least one Message Selector then the only messages
	*   that will be received on the current connection will be those on the list.  
	*	If no subscriptions are submitted then this system will receive all messages published to the topic
	* @see setDestination
	*/
	void addSubscription(std::string msgSelector);

	/**
	* addAdvSubscription - Add a general selector expression to be used with SUBSCRIBE
	*	This must be called before connect().
	*   Host will only publish messages matching one of the selectors.
	*   If no addSubscriptions are submitted then all messages will be received.
	* @param msgSelector std::string - Name of a BML Message Type to be added to subscription list.
	*	If the list contains at least one Message Selector then the only messages
	*   that will be received on the current connection will be those on the list.
	*	If no subscriptions are submitted then this system will receive all messages published to the topic
	* @see setDestination
	*/
	void addAdvSubscription(std::string substring);
	// End of Getters/Setters

	/********************************************/
	/*  Foreground message receive thread run() */
	/********************************************/
	/**
	* run method is used internally to create a foreground thread for receiving and queuing messages
	* from STOMP server <BR>
	* This is required to be public to make a thread and should not be called except by internal code.
	*/
	void run();

	/*****************/
	/* sendFrame()    */
	/******************/
	// Send frame on current socket
	static void sendFrame(std::ostream& outputSink, std::string data);

	void receiveProperties(std::map<std::string, std::string> &props);

	std::pair<std::string, std::string> receiveProperty();
};