// Copyright 2019 Defence Technology Agency,
// New Zealand Defence Force
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// and associated documentation files(the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions :
// 1.	The above copyright notice and this permission notice shall be included in all copies or substantial
//      portions of the Software;
// 2.	the Name of Defence Technology Agency and New Zealand Defence Force not be used in advertising or 
//      publicity  pertaining to distribution of the software without written prior permission.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
// LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
// OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#include "stdafx.h"
#include "IOThread.h"
#include "PosReportXml.h"
#include "Util.h"
#include <deque>


const auto oneHundredMS = std::chrono::milliseconds(100);

std::deque<SimEntity> sendQueue;

//BMLClientREST_Lib bmlRestClient;
//BMLClientSTOMP_Lib bmlStompClient;

std::ofstream restLog;
std::ofstream stompLog;

//Util utils;

IOThread::IOThread()
{
	this->bmlRestLib = new BMLClientREST_Lib();		//i need to use the one with params); 
	this->bmlStompLib = new BMLClientSTOMP_Lib();
	
	useC2SNameSpace = false;
}


IOThread::~IOThread()
{
	delete bmlStompLib;
	delete bmlRestLib;
}

void IOThread::sendPosReport(SimEntity& simEntity) {
	sendQueue.push_back(simEntity);
}

void IOThread::setStayAliveRest() {
	stayAliveRest = true;
}
void IOThread::setStayAliveStomp() {
	stayAliveStomp = true;
}

void IOThread::runRestSender(std::string name, IOThread* myself, XmlParser* xmlp) {
	myself->restThreadOK = true;
	myself->errorRest = false;

	restLog.open("dta-c2sim-rest.log", std::ofstream::trunc);
	restLog << "Thread '" << name << "' started\n";

	// Adding test for status and query init
	try {
		std::string resp = bmlRestLib->c2simCommand("STATUS", "", "");

		restLog << "STATUS command = \n" << resp << "\n";
		std::string svrState = xmlp->parseStatusResponse(resp);

		if ((svrState.compare("RUNNING") == 0) || (svrState.compare("PAUSED") == 0)) {
			resp = bmlRestLib->c2simCommand("QUERYINIT", "", "");
			restLog << "QUERYINIT command = \n" << resp << "\n";
			xmlp->parseXmlMessage(resp);

		}
	}
	catch (BMLClientException bex) {
		restLog << "Exception while trying to get Status\n" << bex.getMessage() << "\n";
	}

	while (myself->stayAliveRest) {
		myself->restThreadOK = true;
		if (sendQueue.size() > 0) {
			SimEntity se = SimEntity(sendQueue.front());
			

			PosReportXml posXml;
			posXml.setSimEntity(se);			
			std::string xml = posXml.toXML(this->useC2SNameSpace);

			restLog << "SENDING REST XML for\n " << se.entityUUID << " " << se.name << "\n";
			restLog << xml << "\n";
			restLog.flush();
			std::string response = "<empty>";
			try {
				//response = bmlRestLib->bmlRequest(xml);
				response = bmlRestLib->c2simRequest(xml);
				errorRest = false;
			}
			catch (BMLClientException ex) {
				outFile << "error sending to " << se.name << "\n" << ex.getMessage() << "\n";
				myself->errorRest = true;
				myself->exceptionMessage = ex.getMessage();
				exceptionMessage.append("\\n").append(ex.getCauseMessage());

			}
			restLog << "Response= \n" << response << "\n";

			//Perception* prcvEnt = se.getPerceivedEntity();
			//while (prcvEnt != NULL) {

			//}


			restLog.flush();

			sendQueue.pop_front();
		}
		else {
			std::this_thread::sleep_for(oneHundredMS);
		}

		
	}
	myself->restThreadOK = false;

	restLog << "Thread '" << name << "' ending\n";
	restLog.close();
	errorRest = true;
}

void IOThread::runStompReceiver(std::string name, IOThread* myself, XmlParser* xmlp) {
	myself->stompThreadOK = true;
	errorStomp = false;

	stompLog.open("dta-c2sim-stomp.log", std::ofstream::trunc);
	stompLog << "Thread '" << name << "' started\n";
	stompLog.flush();
	bool connectedStomp = false;

	xmlp->setOutStream(&stompLog);

	try {
		stompLog << "connecting to " << bmlStompLib->getHost() << ":" << bmlStompLib->getPort() << "\n";
		BMLSTOMPMessage* connectMsg = bmlStompLib->connect();

		if (connectMsg != NULL) {

			if (connectMsg->getMessageType() == "CONNECTED") {
				connectedStomp = true;
				stompLog << "Connected\n";
			}
			else {
				stompLog << connectMsg->getMessageType() << "\n";
			}
			delete connectMsg;
		}
		else {
			stompLog << "connect returned NULL\n";
		}
		stompLog.flush();

	}
	catch (BMLClientException e) {
		stompLog << "BMLClientException connect() " << e.getMessage() << "\n" << e.getCauseMessage() << "\n";
		stompLog.flush();
		connectedStomp = false;
		errorStomp = true;
		exceptionMessage = e.getMessage();
		exceptionMessage.append("\\n").append(e.getCauseMessage());
		
	}

	BMLSTOMPMessage* message = NULL;

	while (connectedStomp && myself->stayAliveStomp) {
		myself->stompThreadOK = true;
		
		try {
			// note StompLib allocates memory with new, but does not delete!
			if (message != NULL) {
				delete message;
				message = NULL;
			}

			// new is used inside here!
			message = bmlStompLib->getNext_NoBlock();


			if (message == NULL) {
				std::this_thread::sleep_for(oneHundredMS);
			}
			else {

				bool isReport = false;
				if (message->getMessageLength() > 0) {
					//C2SIMHeader* c2sHdr = message->getC2SIMHeader(); {
					//	if (c2sHdr != NULL) {
					//		if (this->submitterName.compare(c2sHdr->getFromSendingSystem()) == 0) {
					//			stompLog << "Received my own report, MsgID=" << c2sHdr->getMessageID();
					//			stompLog.flush();
					//			//return;
					//		}
					//	}
					//}
					std::string xml = message->getMessageBody();		
					if (xml.find("ReportBody") != std::string::npos) {
						isReport = true;
						//return;
					} 

					stompLog << "RECEIVED STOMP XML\n" << xml << "\n";
					std::string xType = "";
					if (!isReport) {
						bool ok = xmlp->parseXmlMessage(xml);
						if (!ok) {
							stompLog << "parseMessage() Failed\n";
						}
					}
					MessageBodyType mbt = xmlp->getMessageType();
					switch (mbt)
					{
					case NO_MESSEAGE:
						break;
					case AcknowledgementBody:
						break;
					case C2SIMInitializationBody: 
						xType = "C2SIMInitializationBody\n";
						break;
					case DomainMessageBody:
						xType = "DomainMessageBody\n";
						break;
					case ObjectInitialization:
						break;
					case SystemCommandBody:
						xType = "SystemCommandBody\n";
						break;
					default:
						break;
					}

					if (xType.length() > 0) {
						//stompLog << xType;
					}
					stompLog.flush();


				}
				
			}
		}
		catch (BMLClientException ex) {
			stompLog << "BMLClientException getNext_NoBlock() " << ex.getMessage() << "\n" << ex.getCauseMessage() << "\n";
			stompLog.flush();
			connectedStomp = true;
			errorStomp = true;
			exceptionMessage = ex.getMessage();
			exceptionMessage.append("\\n").append(ex.getCauseMessage());
		}
	}

	myself->stompThreadOK = false;


	// check this again
	if (message != NULL) {
		delete message;
		message = NULL;
	} 

	stompLog << "Thread '" << name << "' ending\n";
	stompLog.close();
	errorStomp = true;

}




void IOThread::startRestClientThread(XmlParser* xmlp) {
	outFile << "startRestClientThread()... starting\n";
	std::string name = "BMLRestClientTHread";
	restSendThread = std::thread(&IOThread::runRestSender, this, name, this, xmlp);

}


void IOThread::startStompClientThread(XmlParser* xmlp) {
	outFile << "startStompClientThread()... starting\n";
	std::string name = "BMLStompClientTHread";
	stompRecvThread = std::thread(&IOThread::runStompReceiver, this, name, this, xmlp);
}

bool IOThread::isRestThreadRunning() {
	return restThreadOK;
}

bool IOThread::isStompThreadRunning() {
	return stompThreadOK;
}

void IOThread::stopRest() {
	this->stayAliveRest = false;
	try {
		this->restSendThread.join();
	}
	catch (...) {}

	outFile << "BML Rest Thread Stopped\n";

}

void IOThread::stopStomp() {
	try {
		outFile << "bmlStompClient.disconnect().....";
		bmlStompLib->disconnect();
		outFile << "Disconnected\n";
	}
	catch (...) {
		outFile << "Exception, but it might close anyway \n";
	}

	try {
		this->stompRecvThread.join();
	}
	catch (... ) 
	{ }

	outFile << "BML Stomp Threads stopped\n";

}

void IOThread::setBmlServer(std::string bmlServerAddr) {

	if (!isRestThreadRunning()) {
		bmlRestLib->setHost(bmlServerAddr);
	}
	if(!isStompThreadRunning()) {
		bmlStompLib->setHost(bmlServerAddr);
		bmlStompLib->setPort("61613");
		bmlStompLib->setDestination("/topic/BML");
		//bmlStompLib->addAdvSubscription("protocol = 'C2SIM'");
	}
}
void IOThread::setBmlSubmitter(std::string submitter) {
	this->submitterName = submitter;
	bmlRestLib->setSubmitter(submitter);
	C2SIMHeader* c2sHdr = bmlRestLib->getC2SIMHeader();
	c2sHdr->setFromSendingSystem(submitter);
	c2sHdr->setToReceivingSystem("server");
	c2sHdr->setPerformative("Inform");
	c2sHdr->setSendingTime(Util::isoTimeNow());
	//c2sHdr->set ?? anything else
	
}
void IOThread::setBmlProtocol(std::string protocol) {
	bmlRestLib->setProtocol(protocol);
}

void IOThread::setUseC2SNameSpace(bool useC2Sns) {
	this->useC2SNameSpace = useC2Sns;

}

bool IOThread::isError() {
	return errorRest || errorStomp;
}
std::string& IOThread::getErrorMessage() {
	return exceptionMessage;
}