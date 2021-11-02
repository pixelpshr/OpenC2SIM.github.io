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
#include "UnitOrderTask.h"
#include "InitializeC2Sim.h"
#include "tinyxml2.h"
#include "XmlTags.h"
#include <queue>
#include <fstream>
#include <ostream>

enum MessageBodyType { NO_MESSEAGE, AcknowledgementBody, C2SIMInitializationBody, DomainMessageBody, ObjectInitialization, SystemCommandBody };
enum SystemCommandCodeType { InitializationComplete, ShareScenario, StartScenario, SubmitInitialization, NO_SYS_CMD};
enum SessionStateCodeType { UNINITIALIZED, INITIALIZING, INITIALIZED, RUNNING, PAUSED, STOMP_ERROR };

class XmlParser
{
public:
	XmlParser();
	~XmlParser();

	//void setXMLNameSpacePrefix(std::string ns);
	void setVBSSystemName(std::string vbsSysName);

	bool parseXmlMessage(std::string& xmlMessage);
	MessageBodyType getMessageType();
		
	void setOutStream(std::ostream* os);
	void setC2SimInitMsg(InitializeC2Sim* initC2SimMessage);
	void setUnitOrderQueue(std::queue<UnitOrderTask>* unitOrderTaskQueue);

	SystemCommandCodeType getSystemCommandCodeType();
	SessionStateCodeType getSessionStateCodeType();
	
	std::string parseStatusResponse(std::string xmlResp);
	
private:
	std::ostream* logStream;
	std::string vbsSystemName;
	std::string xNameSpace;
	std::queue<UnitOrderTask>* unitOrderTaskQ;
	
	MessageBodyType latestMsg;
	InitializeC2Sim* initC2SimMsg;

	XmlTags xtagDefaultNS;
	XmlTags xtagC2sNS;
	XmlTags* xt;
	UnitBehaviors uBehv;


	SystemCommandCodeType systemCommandCodeType;
	SessionStateCodeType sessionStateCodeType;
	
	bool parseOrder(tinyxml2::XMLElement * xmlOrder);

	bool parseTask(tinyxml2::XMLElement* eTask, UnitOrderTask &uot);
	bool parseGeoCoord(tinyxml2::XMLElement* geo, GeoPoint&  latlon);
	bool parseRoute(tinyxml2::XMLElement* eRouteLocn1, UnitOrderTask& uot);

	bool parseObjInit(tinyxml2::XMLElement* eObjInit);
	bool parseSystemEntityList(tinyxml2::XMLElement* eSysEntityList);
	bool parseEntity(tinyxml2::XMLElement* eEntity);
	bool parseFinalEntity(tinyxml2::XMLElement* eEntity);
	
	bool parseSysCommandBody(tinyxml2::XMLElement* eSysCmdBdy);
	
};

