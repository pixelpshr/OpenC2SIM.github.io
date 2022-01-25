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

#pragma once
#include <thread>
#include "SimEntity.h"
#include "C2SIMClientLib.h"
#include "XmlParser.h"
#include <atomic>
#include "InitializeC2Sim.h"
#include "UnitOrderTask.h"

class IOThread
{
private:
	
	std::thread restSendThread;
	std::thread stompRecvThread;
	std::atomic<bool> stayAliveRest;
	std::atomic<bool> stayAliveStomp;
	std::atomic<bool> restThreadOK;
	std::atomic<bool> stompThreadOK;
	bool useC2SNameSpace;


	BMLClientSTOMP_Lib* bmlStompLib;
	BMLClientREST_Lib* bmlRestLib;
	//XmlParser* xmlparser;


	void runRestSender(std::string name ,IOThread* myself, XmlParser* xmlp);
	//void runStompReceiver(std::deque<std::string>* queue, IOThread* myself);
	void runStompReceiver(std::string name, IOThread* myself, XmlParser* xmlp);

	std::string submitterName;

	std::string exceptionMessage = "";
	bool errorStomp;
	bool errorRest;
	

public:
	IOThread();
	~IOThread();

	bool isRestThreadRunning();
	bool isStompThreadRunning();
	void setStayAliveRest();
	void setStayAliveStomp();

	//int getNewOrderCount();
	//InitializeC2Sim* getInitC2SimMessage();
	//UnitOrderTask* getNewestUnitOrderTask();
	//void consumeUnitOrderTask();	

	void setBmlServer(std::string bmlServerAddr);
	void setBmlSubmitter(std::string submitter);
	void setBmlProtocol(std::string protocol);
	void setUseC2SNameSpace(bool useC2Sns);

	void stayAliveEnabled();
	void startRestClientThread(XmlParser* xmlp);
	void startStompClientThread(XmlParser* xmlp);
	void stopRest();
	void stopStomp();

	void sendPosReport(SimEntity& simEntity);

	bool isError();
	std::string& getErrorMessage();

};

