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
//#include "pch.h"

#include "XmlParser.h"
#include "SimEntity.h"
#include "tinyxml2.h"
#include "XmlTags.h"
#include <iostream>
#include <string>
#include <map>

using namespace tinyxml2;


	XmlParser::XmlParser() 
	{
		//setXMLNameSpacePrefix("");
		xtagDefaultNS = XmlTags();
		xtagC2sNS = XmlTags("c2s");
		uBehv = UnitBehaviors();
		this->systemCommandCodeType = NO_SYS_CMD;
		this->sessionStateCodeType = UNINITIALIZED;
		
	}



	XmlParser::~XmlParser()
	{
		logStream = NULL;
		unitOrderTaskQ = NULL;
		initC2SimMsg = NULL;
	}
	
	void XmlParser::setVBSSystemName(std::string vbsSysName) {
		this->vbsSystemName = vbsSysName;
	}


	MessageBodyType XmlParser::getMessageType() {
		return latestMsg;
	}

	void XmlParser::setC2SimInitMsg(InitializeC2Sim* initC2SimMessage) {
		this->initC2SimMsg = initC2SimMessage;
		this->systemCommandCodeType = NO_SYS_CMD;
		this->sessionStateCodeType = UNINITIALIZED;


	}
	void XmlParser::setUnitOrderQueue(std::queue<UnitOrderTask>* unitOrderTaskQueue) {
		this->unitOrderTaskQ = unitOrderTaskQueue;
	}

	SystemCommandCodeType XmlParser::getSystemCommandCodeType(){
		return this->systemCommandCodeType;

	}
	SessionStateCodeType XmlParser::getSessionStateCodeType(){
		return this->sessionStateCodeType;
	}






	void XmlParser::setOutStream(std::ostream* os) {
		this->logStream = os;
	}





	/**
		this is where we start to parse some xml
		DomainMessageBody is probably an order
		ObjectInitialization is a initialize
	*/
	bool XmlParser::parseXmlMessage(std::string& xmlMessage) {
		

		if (xmlMessage.find("c2s:") != std::string::npos) {
			xt = &xtagC2sNS;
		}
		else {
			xt = &xtagDefaultNS;
		}


		XMLDocument* xmldoc = new XMLDocument();
		xmldoc->Parse(xmlMessage.c_str(), xmlMessage.length());

		bool okXmlMsg = false;

		//XMLElement* eRoot = xmldoc.FirstChildElement();
		
		// figure out if this has a namespace "c2s:"		


		// Need this only for Late Join when the xml includes the full <Message> and <C2SIMHeader>
		XMLElement* eMsg = xmldoc->FirstChildElement("Message");
		if (eMsg == NULL) {
			eMsg = xmldoc->FirstChildElement("c2s:Message");
		}

		XMLElement* eMsgBody = NULL;
		
		if (eMsg == NULL) {
			// this is if svr is not initialised and this comes through STOMP and should not have the <C2SIMHeader>
			// <MessageBody> is the root element
			eMsgBody = xmldoc->FirstChildElement(xt->MessageBody.c_str());
		}
		else {
			// this is if svr is running and this comes through late join
			// <MessageBody> is incapsulated in the <Message> element
			eMsgBody =   eMsg->FirstChildElement(xt->MessageBody.c_str());
		}

		
		if (eMsgBody == NULL) {

			latestMsg = NO_MESSEAGE;
			return okXmlMsg;
		}
		
		XMLElement* eDomainMsgBody = eMsgBody->FirstChildElement(xt->DomainMessageBody.c_str());
		XMLElement* eC2SIMInitBody = eMsgBody->FirstChildElement(xt->C2SIMInitializationBody.c_str());
		XMLElement* eObjectInit = eMsgBody->FirstChildElement(xt->ObjectInitialization.c_str());
		XMLElement* eSysCmdBody = eMsgBody->FirstChildElement(xt->SystemCommandBody.c_str());
		
		if (eDomainMsgBody != NULL) {
			XMLElement* eOrderBody = eDomainMsgBody->FirstChildElement(xt->OrderBody.c_str());


			if (eOrderBody != NULL) {
				(*logStream) << "parseOrder\n";
				okXmlMsg = parseOrder(eOrderBody);
				if (!okXmlMsg) {
					(*logStream) << "parseOrder() Failed\n";
				}
				(*logStream).flush();
			}
			latestMsg = DomainMessageBody;
		}
		else if (eObjectInit != NULL) {
			(*logStream) << "parseObjInit\n";
			(*logStream).flush();
			okXmlMsg = parseObjInit(eObjectInit);
			latestMsg = ObjectInitialization;
		}
		else if (eC2SIMInitBody != NULL) {
			(*logStream) << "parseC2SImInit\n";
			(*logStream).flush();
			okXmlMsg = parseObjInit(eC2SIMInitBody);
			latestMsg = C2SIMInitializationBody;
		}

		else if (eSysCmdBody != NULL) {
			(*logStream) << "parseSysCommandBody\n";
			(*logStream).flush();
			okXmlMsg = parseSysCommandBody(eSysCmdBody);
			latestMsg = SystemCommandBody;
		}
		else 
		{ // not order or report
			XMLElement* el = eMsgBody->FirstChildElement();
			(*logStream) << "Not Init or Order, MessageType= "<< el->Name()<< "\n";
			(*logStream).flush();
			latestMsg = NO_MESSEAGE;
		}

		delete xmldoc;

		return okXmlMsg;
	}

/**
parseOrder
*/
	bool XmlParser::parseOrder(XMLElement * xmlOrder) {
		bool okUUIDs = false;
		bool okTask = false;
		// this is to hold the sender/receiver UUIDs
		UnitOrderTask defaultUOT = UnitOrderTask();

		XMLElement* eFromSender = xmlOrder->FirstChildElement(xt->FromSender.c_str());
		XMLElement* eToReceiver = xmlOrder->FirstChildElement(xt->ToReceiver.c_str());
		XMLElement* eOrderID = xmlOrder->FirstChildElement(xt->OrderID.c_str());

		defaultUOT.fromSendUUID = eFromSender->GetText();
		defaultUOT.toRcvrUUID = eToReceiver->GetText();
		defaultUOT.orderUUID = eOrderID->GetText();

		okUUIDs = defaultUOT.okUUIDs();
		if (!okUUIDs) {
			(*logStream) << "DefaultUOT.okUUIDs() failed\n";
		}


		XMLElement* eIssueT = xmlOrder->FirstChildElement(xt->IssuedTime.c_str());
		XMLElement* eDT = eIssueT->FirstChildElement(xt->DateTime.c_str());
		XMLElement* eIsoT = eDT->FirstChildElement(xt->IsoDateTime.c_str());
		defaultUOT.issueTime = eIsoT->GetText();

		XMLElement* eTask1 = xmlOrder->FirstChildElement(xt->Task.c_str());

		XMLElement* eTaskn = eTask1;

		while (eTaskn != NULL) {
			UnitOrderTask uot = UnitOrderTask(defaultUOT);
			uot.waypointList.clear();

			(*logStream) << "****************\n";
			bool ok =parseTask(eTaskn, uot);			
			okTask = ok || okTask;

			// the stomp server sends all orders to all clients.  
			// check to see if this order if for one of my entities
			if( this->initC2SimMsg->contains(uot.performingEntityUUID) ) {
				this->unitOrderTaskQ->push(uot);
			}

			eTaskn = eTaskn->NextSiblingElement();
			if (xt->Task.compare(eTaskn->Name()) != 0) {
				break;
			}

		}
		if (!okTask) {
			(*logStream) << "parseTask() Failed\n";
		}

		return okUUIDs && okTask;

	}

/**
	parseObjInit
*/
	bool XmlParser::parseObjInit(XMLElement* eObjInit) {
		bool okInitFile = false;  
		bool okSysEntityList = false;
		bool okEntityObjDefs = false;


		// first is InitializationDataFile
		XMLElement* eInitDataFIle = eObjInit->FirstChildElement(xt->InitializationDataFile.c_str());
		if (eInitDataFIle != NULL) {
			XMLElement* eName = eInitDataFIle->FirstChildElement(xt->Name.c_str());
			XMLElement* eSysName = eInitDataFIle->FirstChildElement(xt->SystemName.c_str());

			std::string filename = "";
			std::string sysname = "";

			if (eName != NULL) {
				filename = eName->GetText();
			}
			if (eSysName != NULL) {
				sysname = eSysName->GetText();
			}
			(*logStream) << "InitDataFile = " << filename << ", SystemName = " << sysname << "\n";
		
			okInitFile = true;
		} 

		// do the system entity list first
		XMLElement* eSystemEntityList = eObjInit->FirstChildElement(xt->SystemEntityList.c_str());
		while (eSystemEntityList != NULL) {
			bool ok = parseSystemEntityList(eSystemEntityList);
			okSysEntityList = okSysEntityList || ok; // or so returns true if one is good

			eSystemEntityList = eSystemEntityList->NextSiblingElement(xt->SystemEntityList.c_str());

		}

		// now parse the ObjectDefinition
		XMLElement* eObjDefs = eObjInit->FirstChildElement(xt->ObjectDefinitions.c_str());
		if (eObjDefs != NULL) {
			(*logStream) << "Reading init data for " << this->vbsSystemName << "\n";
			XMLElement* elem = eObjDefs->FirstChildElement();
			while (elem != NULL) {
				if (xt->AbstractObject.compare(elem->Name()) == 0) {
					XMLElement* eAbChild = elem->FirstChildElement();
					if (eAbChild != NULL) {
						std::string nm = eAbChild->Name();
						(*logStream) << xt->AbstractObject << " is " << nm << "... Not Implemented\n";
					}
				}
				else if (xt->Action.compare(elem->Name()) == 0) {
					XMLElement* eAbChild = elem->FirstChildElement();
					if (eAbChild != NULL) {
						std::string nm = eAbChild->Name();
						(*logStream) << xt->AbstractObject << " is " << nm << "... Not Implemented\n";
					}
				}
				else if (xt->Entity.compare(elem->Name()) == 0) {
					bool ok = parseEntity(elem);
					okEntityObjDefs = okEntityObjDefs || ok; // or so returns true if one is good
				}
				else if (xt->PlanPhaseReference.compare(elem->Name()) == 0) {

				}


				elem = elem->NextSiblingElement();
			}

		}

		initC2SimMsg->setInitFileFilled();

		// i used to use the InitializeComplete SystemCommand message but that does not work with late join
		initC2SimMsg->setInitComplete(); 

		return /*okInitFile && */ okSysEntityList && okEntityObjDefs;
	}

	bool XmlParser::parseSystemEntityList(tinyxml2::XMLElement* eSysEntityList) {
		bool okSysName = false;
		bool okHasAnActor = false;

		XMLElement* eSystemName = eSysEntityList->FirstChildElement(xt->SystemName.c_str());
		if (eSystemName != NULL) {
			// have to check that this is the name of my system e.g. VBS3
			if (this->vbsSystemName.compare(eSystemName->GetText()) == 0) {
				// good this is my list
				okSysName = true;
				(*logStream) << "Found SystemEntityList for " << eSystemName->GetText() << "\n";
			}
			else {
				(*logStream) << "Ignoring entities for SystemName = " << eSystemName->GetText() << "\n";
				return false;
			}
		}
	
		XMLElement * eActorRef = eSysEntityList->FirstChildElement(xt->ActorReference.c_str());
		int i = 1;
		while (eActorRef!=NULL) {
			if (xt->ActorReference.compare(eActorRef->Name()) == 0) {
				std::string uuid = eActorRef->GetText();
				this->initC2SimMsg->addEntityUUID(uuid);				
				okHasAnActor = true;
				(*logStream) << "Entity(" << i++ << ") = "  << uuid << "\n";
			}
			eActorRef = eActorRef->NextSiblingElement();
		}

		return okSysName && okHasAnActor;

	}



	bool XmlParser::parseEntity(tinyxml2::XMLElement* eEntity) {
		bool okEntity = false;
		XMLElement* eActorEntity = eEntity->FirstChildElement(xt->ActorEntity.c_str());
		XMLElement* ePhysEntity = eEntity->FirstChildElement(xt->PhysicalEntity.c_str());
		if (eActorEntity != NULL) {
			XMLElement* e1Entity = eActorEntity->FirstChildElement();
			// this should be either Collective Entity, Platform or Person
			if (e1Entity != NULL) {
				XMLElement* e2Entity = e1Entity->FirstChildElement();
				//collective { milOrg, nonMilOrg}
				// platform { aircraft, vehicle, surface vessel, subsurfacevessel}
				if (e2Entity != NULL) {
					okEntity = parseFinalEntity(e2Entity);
				} 

			}
			


		}
		else if (ePhysEntity != NULL) {
			//ignore 
			// cultural, enviro, geographic etc
		}

		return okEntity;
	}


	
	bool XmlParser::parseFinalEntity(tinyxml2::XMLElement* eEntity) {
		bool okState = false;
		bool okUUID = false;
		bool okName = false;
		
		XMLElement* e = eEntity->FirstChildElement();
		SimEntity siment =  SimEntity();

		while (e != NULL) {
			std::string eTag = e->Name();

			if (eTag.compare(xt->UUID) == 0) {
				siment.entityUUID = e->GetText();
				okUUID = true;
			}
			else if (eTag.compare(xt->Name) == 0) {
				siment.name = e->GetText();
				okName = true;
			}
			else if (eTag.compare(xt->EntityType) == 0) {
				XMLElement* eAPP6 = e->FirstChildElement(xt->APP6_SIDC.c_str());
				if (eAPP6 != NULL) {
					XMLElement* eSIDC = eAPP6->FirstChildElement(xt->SIDCString.c_str());
					if (eSIDC != NULL) {
						siment.app6Code = eSIDC->GetText();						
					}				
					XMLElement* eDisType = e->FirstChildElement(xt->EntityTypeString.c_str());
					if (eDisType != NULL) {
						siment.disType = eDisType->GetText();
					}
				}
			}
			else if (eTag.compare(xt->EntityDescriptor) == 0) {
				XMLElement* eSup = e->FirstChildElement(xt->Superior.c_str());
				if (eSup != NULL) {
					siment.superiorUUID = eSup->GetText();					
				}
				XMLElement* eSide = e->FirstChildElement(xt->Side.c_str());
				if (eSide != NULL) {
					siment.sideUUID = eSide->GetText();
				}
			}
			else if (eTag.compare(xt->CurrentTask) == 0) {

			}
			else if (eTag.compare(xt->Resource) == 0) {

			}
			else if (eTag.compare(xt->Marking) == 0) {

			}
			else if (eTag.compare(xt->CurrentState) == 0) {
				XMLElement* ePhys = e->FirstChildElement(xt->PhysicalState.c_str());
				if (ePhys != NULL) {
					XMLElement* eLoc = ePhys->FirstChildElement(xt->Location.c_str());
					if (eLoc != NULL) {
						XMLElement* eCoord = eLoc->FirstChildElement(xt->Coordinate.c_str());
						if (eCoord != NULL) {
							XMLElement* eGeoC = eCoord->FirstChildElement(xt->GeodeticCoordinate.c_str());
							if (eGeoC != NULL) {
								GeoPoint geoPt = GeoPoint();
								okState = parseGeoCoord(eGeoC, geoPt);
								siment.setPosition(geoPt.latitude, geoPt.longitude, 0);
							}
							// ignore geocentric x y z coords
						}
					}

					XMLElement* eEHeath = ePhys->FirstChildElement(xt->EntityHealthStatus.c_str());
					if (eEHeath!= NULL) {
						XMLElement* eOpstat = eEHeath->FirstChildElement(xt->OperationalStatus.c_str());
						if (eOpstat != NULL) {
							XMLElement* eOSCode = eOpstat->FirstChildElement(xt->OperationalStatusCode.c_str());
							std::string sc = eOSCode->GetText();
							siment.setOprStatus(sc);
						}
					}
				}




			}
		
			else if (eTag.find("CategoryCode") != std::string::npos) {

			}				
			e = e->NextSiblingElement();
		} // end while
		
		
		// check to see if this entity is the the SystemEntityList
		if (this->initC2SimMsg->listContains(siment.entityUUID)) {
			this->initC2SimMsg->putEntity(siment.entityUUID, siment);
			

			(*logStream) << "Added Entity()\n UUID= " << siment.entityUUID << "\n Name= " << siment.name
				<< "\n side= " << siment.sideUUID << "\n APP6= " << siment.app6Code << "\n DIS = "
				<< siment.disType << "\n Lat = " << siment.getLatitude() << "\n Lon = " << siment.getLongitude()
				<< "\n";
		}
		else {
			(*logStream) << "Ignore Entity UUID= " << siment.entityUUID << "\n";				
		}
		logStream->flush();

		return okName && okUUID && okState;
	}

	bool XmlParser::parseTask(XMLElement* eTask, UnitOrderTask &uot) {
		bool okNameofTask = false;
		bool okUUID = false;
		bool okLocn = false;
		bool okTNC = false;
		bool okPerfEnt = false;

		GeoPoint locationGP = GeoPoint();
		XMLElement* eMWT = eTask->FirstChildElement();

		XMLElement* elm = eMWT->FirstChildElement();
		while (elm != NULL) {

			std::string name = elm->Name();

			if (name.compare(xt->DesiredEffectCode) == 0) {
				uot.desiredEffectCode = elm->GetText();
				uot.vbsWpCombat = uBehv.desiredEffectCOdeToVbsWpType(uot.desiredEffectCode);
			}
			else if (name.compare(xt->TaskNameCode) == 0) {
				uot.taskNameCode = elm->GetText();
				uot.vbsWpType = uBehv.taskNameCodeToVbsWpType(uot.taskNameCode);
				uot.vbsWpBehav = uBehv.taskNameCodeToVbsWpBehaviourType(uot.taskNameCode);
				okTNC = true;
			}
			else if (name.compare(xt->StartTime) == 0) {
				XMLElement* eName = elm->FirstChildElement(xt->Name.c_str());
				std::string timepoint = eName->GetText();
				
				XMLElement* eIsoT = elm->FirstChildElement(xt->IsoDateTime.c_str());
				std::string isoTime = eIsoT->GetText();
			}
			else if (name.compare(xt->PerformingEntity) == 0) {
				uot.performingEntityUUID = elm->GetText();
				okPerfEnt = true;
			}
			else if (name.compare(xt->Location) == 0) {

				XMLElement* eCoord = elm->FirstChildElement(xt->Coordinate.c_str());
				XMLElement* eGeoCoord = eCoord->FirstChildElement(xt->GeodeticCoordinate.c_str());
				okLocn = parseGeoCoord(eGeoCoord, locationGP);
			}
			else if (name.compare(xt->Route) == 0) {
				XMLElement* eRouteLoc1 = elm->FirstChildElement(xt->RouteLocation.c_str());

				okLocn = parseRoute(eRouteLoc1, uot);

			}
			else if (name.compare(xt->UUID) == 0) {
				uot.orderUUID = elm->GetText();
				okUUID = true;
			}
			else if (name.compare(xt->Name) == 0) {
				uot.nameOfTask = elm->GetText();
				okNameofTask = true;
			}
			else if (name.compare(xt->MapGraphicID) == 0) {
				uot.mapGraphicID = elm->GetText();
			}

			

			elm = elm->NextSiblingElement();
		}	
        
		uot.waypointList.push_back(locationGP);
		
		return okLocn && okNameofTask && okPerfEnt && okTNC && okUUID;

	}

	bool XmlParser::parseGeoCoord(XMLElement* eGeo, GeoPoint&  latlon) {
		bool ok = false;

		XMLElement* eLat =  eGeo->FirstChildElement(xt->Latitude.c_str());
		XMLElement* eLon = eGeo->FirstChildElement(xt->Longitude.c_str());
		
		latlon.latitude = std::stod(eLat->GetText());
		latlon.longitude = std::stod(eLon->GetText());

		return latlon.isOK();

	}

	bool XmlParser::parseRoute(XMLElement* eRouteLocn1, UnitOrderTask& uot) {
		bool ok = false;
		XMLElement* eRouteLocn = eRouteLocn1;

		while (eRouteLocn != NULL) {
			XMLElement* eGeoCoord = eRouteLocn->FirstChildElement(xt->GeodeticCoordinate.c_str());
			GeoPoint gpt = GeoPoint();
			ok = parseGeoCoord(eGeoCoord, gpt);

			uot.waypointList.push_back(gpt);
			eRouteLocn =  eRouteLocn->NextSiblingElement();

		}
		return ok;
	}

	bool XmlParser::parseSysCommandBody(XMLElement* eSysCmdBdy) {

		bool okCmd = false;
		bool okState = false;
		XMLElement* eSysCmdType = eSysCmdBdy->FirstChildElement(xt->SystemCommandTypeCode.c_str());
		if (eSysCmdType != NULL) {
			okCmd = true;
			std::string cmdType = eSysCmdType->GetText();
			if (cmdType.compare(xt->scInitializationComplete) == 0) {
				this->systemCommandCodeType = InitializationComplete;
				// this won't happen with late join
				//initC2SimMsg->setInitComplete();
			}
			else if (cmdType.compare(xt->scShareScenario) == 0) {
				this->systemCommandCodeType = ShareScenario;
			}
			else if (cmdType.compare(xt->scStartScenario) == 0) {
				this->systemCommandCodeType = StartScenario;
			}
			else if (cmdType.compare(xt->scSubmitInitialization) == 0) {
				this->systemCommandCodeType = SubmitInitialization;
			}
			else {
				this->systemCommandCodeType = NO_SYS_CMD;
				okCmd = false;
			}
		}

		XMLElement* eSesStateCode = eSysCmdBdy->FirstChildElement(xt->SessionStateCode.c_str());
		if (eSesStateCode != NULL) {
			okState = true;
			std::string sesState = eSesStateCode->GetText();
			if (sesState.compare(xt->scUNINITIALIZED) == 0) {
				this->sessionStateCodeType = UNINITIALIZED;
			}
			else if (sesState.compare(xt->scINITIALIZING) == 0) {
				this->sessionStateCodeType = INITIALIZING;
			}
			else if (sesState.compare(xt->scINITIALIZED) == 0) {
				this->sessionStateCodeType = INITIALIZED;
			}
			else if (sesState.compare(xt->scRUNNING) == 0) {
				this->sessionStateCodeType = RUNNING;
			}
			else if (sesState.compare(xt->scPAUSED) == 0) {
				this->sessionStateCodeType = PAUSED;
			}
			else {
				this->sessionStateCodeType = UNINITIALIZED;
				okState = false;
			}
		}

		return okCmd && okState;

	}


	std::string XmlParser::parseStatusResponse(std::string xmlResp) {

		this->xt = &xtagDefaultNS;

		XMLDocument* xmldoc = new XMLDocument();

		xmldoc->Parse(xmlResp.c_str(), xmlResp.length());

		XMLElement* eRes = xmldoc->FirstChildElement("result");
		XMLElement* eSvrInit = eRes->FirstChildElement("serverInitialized");
		XMLElement* eSesState = eRes->FirstChildElement("sessionState");

		std::string sesState = "ERROR";

		if (eSesState != NULL) {

			sesState = eSesState->GetText();
			if (sesState.compare(xt->scUNINITIALIZED) == 0) {
				this->sessionStateCodeType = UNINITIALIZED;
			}
			else if (sesState.compare(xt->scINITIALIZING) == 0) {
				this->sessionStateCodeType = INITIALIZING;
			}
			else if (sesState.compare(xt->scINITIALIZED) == 0) {
				this->sessionStateCodeType = INITIALIZED;
			}
			else if (sesState.compare(xt->scRUNNING) == 0) {
				this->sessionStateCodeType = RUNNING;
			}
			else if (sesState.compare(xt->scPAUSED) == 0) {
				this->sessionStateCodeType = PAUSED;
			}
			else {
				this->sessionStateCodeType = UNINITIALIZED;
			}
			return sesState;
		}

		delete xmldoc;

		return sesState;
	}

