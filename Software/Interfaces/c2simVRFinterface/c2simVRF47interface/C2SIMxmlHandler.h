/*----------------------------------------------------------------*
|     Copyright 2021 Networking and Simulation Laboratory         |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for academic purposes is hereby  |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
*-----------------------------------------------------------------*/

// c2simVRF 2.8 consistent with C2SIM_SMX_LOX_v11.xsd

#define _WINSOCK_DEPRECATED_NO_WARNINGS

#define byte uint8_t

#pragma once
#include <iostream>
#include <fstream>
#include <string>
#include <stdio.h>
#include <iostream>
#include <map>

// xercesc
#include "xercesc/sax/HandlerBase.hpp"
#include "xercesc/sax/AttributeList.hpp"
#include "xercesc/sax/AttributeList.hpp"

// SaxParser
#include "xercesc/parsers/SAXParser.hpp"

// VRLink
#include <matrix/geodeticCoord.h>
#include <vrfcontrol/vrfRemoteController.h>

using namespace xercesc;

// size of strings and arrays
#define MAXPOINTS 100   // points in route

// initial tags found in parse

// data from an ObjectInitialization Unit
struct Unit {
	std::string uuid;
	std::string name;
	std::string vrfUuid;
	std::string opStatusCode;
	std::string strengthPercent;
	std::string hostilityCode;
	std::string echelon;
	std::string superiorUnit;
	std::string latitude;
	std::string longitude;
	std::string elevationAgl;
	std::string directionPhi;
	std::string directionPsi;
	std::string directionTheta;
	std::string symbolId;
	std::string disEntityType;
	std::string forceSideUuid;
	std::string systemName;
	std::string aggregateName;
	std::string reportFromSender;
	std::string reportToReceiver;
	std::string currentTaskUuid;
	bool createdObject;
	bool vrfObjectHasBeenCreated;
	bool routeIsComplete;
	bool taskIsComplete;
	int addInitCount;
	int nameInitCount;
	int vrfUuidInitCount;
	int numberOfVehicles;
	int reportCount;
	byte DISKind;
	byte DISDomain;
	int DISCountry;
	byte DISCategory;
	byte DISSubcategory;
	byte DISSpecific;
	byte DISExtra;
};

// data from an order (OrderBody or OrderPushIBML)
struct Task {
	std::string taskUuid;
	std::string taskName;
	std::string orderUuid;
	DtString taskVrfDtString;
	DtString taskRouteDtString;
	std::string performingEntity;
	std::string affectedEntity;
	std::string taskeeUuid;
	std::string actionTaskActivityCode;
	std::string ruleOfEngagementCode;
	std::string dateTime;
	int simulationStartMs;
	std::string startAfterTaskUuid;
	int relativeDelayMs;
	int locationPointCount;
	std::string pickupPointName;
	std::string dropoffPointName;
	std::string returnPointName;
	DtUUID scriptedTaskUuid;
	std::string mapGraphicUuid;
	std::string latitudes[MAXPOINTS];
	std::string longitudes[MAXPOINTS];
	std::string elevations[MAXPOINTS];
};

struct PhysicalLocation {
	std::string latitude;
	std::string longitude;
	std::string elevation;
};

struct PhysicalRoute {
	std::string routeUUID;
	std::vector<PhysicalLocation*> locations;
};

// data from ForceSideRelations
struct BlueForceSide {
	std::string uuid;
	std::string otherSide1HostilityCode;
	std::string otherSide1Uuid;
	std::string otherSide1Name;
	std::string otherSide2HostilityCode;
	std::string otherSide2Uuid;
	std::string otherSide2Name;
};

class C2SIMxmlHandler : public HandlerBase
{
	// globals

	// description of XML order
	std::string ibmlTaskersIntentTag = "TaskersIntent";
	std::string c2simDesiredEffectTag = "DesiredEffectCode";

public:
	C2SIMxmlHandler(bool useIbmlRef, bool displayDebugRef);
	~C2SIMxmlHandler();
	void C2SIMxmlHandler::displayError(std::string errorText);

	// parts of the parser
	int C2SIMxmlHandler::getMessageType();
	void C2SIMxmlHandler::parseC2SIMOrder();
	void C2SIMxmlHandler::parseC2SIMReport();
	void C2SIMxmlHandler::parseIBMLOrder();
	void C2SIMxmlHandler::parseC2simInitialize();
	void C2SIMxmlHandler::parseSystemCommand();

	bool C2SIMxmlHandler::isC2simOrder();
	bool C2SIMxmlHandler::isIbmlOrder();
	bool C2SIMxmlHandler::isC2simReport();
	bool C2SIMxmlHandler::isC2simTaskStatusReport();
	bool C2SIMxmlHandler::isIbmlReport();
	std::string C2SIMxmlHandler::getOrderUuid();
	std::string C2SIMxmlHandler::getOrderSender();
	std::string C2SIMxmlHandler::getOrderReceiver();
	std::string C2SIMxmlHandler::getSystemState();
	std::string C2SIMxmlHandler::getTaskersIntent();
	std::string C2SIMxmlHandler::getDateTime();
	std::string C2SIMxmlHandler::getTaskeeUuid(Task* task);
	std::string C2SIMxmlHandler::getAffectedEntity(Task* task);
	std::string C2SIMxmlHandler::getActionTaskActivityCode(Task* task);
	std::string C2SIMxmlHandler::getMapGraphicID(Task* task);
	std::string C2SIMxmlHandler::getRuleOfEngagementCode(Task* task);
	std::string C2SIMxmlHandler::getRootTag();
	int C2SIMxmlHandler::getNumberOfUnits();
	int C2SIMxmlHandler::getNumberOfTasksThisOrder();
	std::string C2SIMxmlHandler::getUuidByName(std::string unitName);
	std::string C2SIMxmlHandler::getHostilityCode(std::string unitName);
	std::string C2SIMxmlHandler::getOpStatusCode(std::string unitName);
	std::string C2SIMxmlHandler::getStrength(std::string unitName);
	std::string C2SIMxmlHandler::getAggregateUnitName(std::string unitName);
	Unit* C2SIMxmlHandler::getUnit(std::string uuid);
	Unit* C2SIMxmlHandler::getUnitByName(std::string name);
	Unit* C2SIMxmlHandler::getUnitByVrfUuid(std::string vrfUuid);
	bool C2SIMxmlHandler::unitMapIsEmpty();
	bool C2SIMxmlHandler::addUnit(Unit* newUnit);
	bool C2SIMxmlHandler::addUnitByName(Unit* newUnit);
	bool C2SIMxmlHandler::addUnitByVrfUuid(Unit* newUnit);
	std::string C2SIMxmlHandler::getSuperiorUnit(std::string uuid);
	std::string C2SIMxmlHandler::getReportFromSender(std::string uuid);
	std::string C2SIMxmlHandler::getReportToReceiver(std::string uuid);
	std::string C2SIMxmlHandler::getTaskStatusCode();
	std::map<std::string, Unit*>::iterator C2SIMxmlHandler::getUnitMapBegin();
	std::map<std::string, Unit*>::iterator C2SIMxmlHandler::getUnitMapEnd();
	Task* C2SIMxmlHandler::makeNewTask();
	bool C2SIMxmlHandler::addTask(Task*);
	bool C2SIMxmlHandler::addOrderId(std::string newOrderId);
	Task* C2SIMxmlHandler::getTask(std::string taskUuid);
	Task* C2SIMxmlHandler::getTask(std::map<std::string, Task*>::iterator);
	Task* getTaskByName(std::string name);
	std::string getTaskNameByUuid(std::string taskUuid);
	std::map<std::string, Task*>::iterator C2SIMxmlHandler::getTaskMapBegin();
	std::map<std::string, Task*>::iterator C2SIMxmlHandler::getTaskMapEnd();
	std::string C2SIMxmlHandler::getStartAfterTaskUuid(std::string taskUuid);
	bool C2SIMxmlHandler::getTaskRouteIsComplete(std::string taskUuid);
	bool C2SIMxmlHandler::getTaskIsComplete(std::string taskUuid);
	void C2SIMxmlHandler::setTaskRouteIsComplete(std::string taskUuid);
	void C2SIMxmlHandler::setTaskIsComplete(std::string taskUuid);
	void C2SIMxmlHandler::setCurrentTaskUuid(std::string unitUuid, std::string newCurrentTask);
	std::string getCurrentTaskUuid(std::string unitUuid);
	void startElement(const XMLCh* const, AttributeList&);
	void fatalError(const SAXParseException&);
	void startDocument();
	void endDocument();
	void characters(const XMLCh* const, const XMLSize_t);
	void endElement(const XMLCh* const name);
	void startC2SIMParse(std::string xmlString);
	Unit* makeEmptyUnit(std::string UUID);
	int findTotalIsoMs(std::string isoTimeDuration);
	BlueForceSide* makeEmptyForceSide();
	bool  C2SIMxmlHandler::insertRoute(std::string routeUUID, PhysicalRoute* route);
	PhysicalRoute* C2SIMxmlHandler::retrieveRoute(std::string routeUUID);

private:

	// collection of all units received in C2SIMInitialization by UUID
	std::map<std::string, Unit*> unitMap;

	// version of unitMap that is mapped by name not UUID
	std::map<std::string, Unit*> unitNameMap;

	// map for VRForces UUIDs mapped to units
	std::map<std::string, Unit*> unitVrfUuidMap;

	// collection of all tasks received
	// (also those in Order.Task if skipping Initialization)
	std::map<std::string, Task*>taskMap;

	// version of taskMap that is mapped by name not UUID
	std::map<std::string, Task*> taskNameMap;

	// collection of all orderID received
	std::map<std::string, std::string>orderIdMap;

	// copies an XML char* to std::string after stripping off namespace:
	void C2SIMxmlHandler::copyXMLChLessNs(std::string &output, const XMLCh* const input);

	// repository of all the routes for lookup by Route UUID
	std::map<std::string, PhysicalRoute*> allPhysicalRoutes;
};