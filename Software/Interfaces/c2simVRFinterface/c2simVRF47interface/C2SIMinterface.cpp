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
*----------------------------------------------------------------*/

/*
VR-Forces C2SIM interface v2.17
consistent with C2SIM_SMX_LOX_v1.0.1.xsd
and C2SIM standard schema

To run demo on Sandbox: 
1. start VR Forces; at Config panel clock Launch;
on Startup panel run Bogaland9; wait until icon shows on map
(this can be slow)
2. run c2simVRF; wait for READY FOR C2SIM ORDERS
3. send an order by C2SIMGUI.bat
4. requires C2SIM initialization for unit names & coordinates
5. icon should move on route from order and position reports should 
   be emitted; if an armed friendly senses red unit, OBSERVATION
   report will be emitted
6. to see reports run C2SIMGUI and click "listen to XML"
7. accepts IBML09 or C2SIMv1.0.0 schema and produces report 
   from same schema, selected by parameter 6 of main
   to use IBML09 see command-line parameters under main.cxx
8. can be run without initialization phase, in which case
   initial position of object is first (or only) geocoordinate
   in the task (this has not been tested recently)
*/

// updated to VRForces4.5 by JMP 12May18 by adding DtUUID()
// updated to VRForces4.6.1 by JMP May2019
// updated to VRForces4.7 by JMP 5Mar20

#include "C2SIMinterface.h"
#include <queue>
#include <fstream>
#include <sstream>
#include <stdio.h>
#include <vector>
#include <cmath>
#include <stdexcept>
#include <tchar.h>
#include <windows.h>
#include <math.h>
#include <thread>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "xerces_utils.h"
#include "vrftasks/scriptedTaskTask.h"
#include "vrftasks/scriptedTaskSet.h"
#include "vrfobjcore/vrfObject.h"
#include "vrfcontrol/UUIDNetworkManager.h"
#include "vl/reflectedEntity.h"

using namespace xercesc;

static int timesCalled = 0;
DtVrfRemoteController* cs2sim_controller;
SAXParser* parser;
C2SIMxmlHandler* c2simXmlHandler;
ErrorHandler* errHandler;
std::string serverRestStompAddress;
std::string stompServerPort;
std::string restServerPort;
std::string c2simClientIDCode;
bool useIbml;
bool sendRedTracking, sendBlueTracking;
static string vrfTerrain;
const static double degreesToRadians = 57.2957795131L;
int numberOfObjects = 0;
int orderCount = 0;
#define MAX_OBJECTS 100
std::string objectNames[MAX_OBJECTS];
C2SIMClientSTOMP_Lib* stompLib = nullptr;
std::string localHostAddress;
bool initializationDone = false;
DtTextInterface* textIf = nullptr;
std::string headerSender = "";
std::string headerReceiver = "";
int reportTimeInterval = 0;
DtUUIDNetworkManager* uuidMgr;
long runStartTime = 0;
bool debug = false;
int reportIntervalParm;
std::string c2simVersionCif;
bool usingHLA = false;

// constructor
C2SIMinterface::C2SIMinterface(
	DtTextInterface* textIfRef,
	std::string serverAddressRef,
	std::string stompPortRef,
	std::string restPortRef,
	std::string clientIDRef,
	bool useIbmlRef,
	int sendTrackingRef,
	bool debugMode,
	int reportInterval,
	std::string c2simVersionRef,
	bool usingHLARef)
{
	// pick up parameters from main()
	textIf = textIfRef;
	serverRestStompAddress = serverAddressRef;
	stompServerPort = stompPortRef;
	restServerPort = restPortRef;
	c2simClientIDCode = clientIDRef;
	useIbml = useIbmlRef;
	sendRedTracking = sendTrackingRef == 1 || sendTrackingRef == 2;
	sendBlueTracking = sendTrackingRef == 0 || sendTrackingRef == 1;
	uuidMgr = textIf->controller()->uuidNetworkManager();
	debug = debugMode;
	reportIntervalParm = reportInterval;
	c2simVersionCif = c2simVersionRef;
	usingHLA = usingHLARef;

	// initialize Xerces
	try {
		XMLPlatformUtils::Initialize();
	}
	catch (const XMLException& toCatch) {
		char* message = XMLString::transcode(toCatch.getMessage());
		std::cout << "error during xercesc initialization:\n" << message << "\n";
		XMLString::release(&message);
		return;
	}

	// setup parser and handler
	parser = new SAXParser();
	parser->setDoNamespaces(false);
	parser->setDoSchema(false);
	c2simXmlHandler = new C2SIMxmlHandler(useIbml, debugMode);
	errHandler = (ErrorHandler*)c2simXmlHandler;
	parser->setDocumentHandler(c2simXmlHandler);
	parser->setErrorHandler(errHandler);

	// get our local host IP address
	boost::asio::io_service ipService;
	boost::asio::ip::tcp::resolver resolver(ipService);
	localHostAddress = boost::asio::ip::host_name();

}// end constructor

// destructor
C2SIMinterface::~C2SIMinterface() {
	delete parser;
	delete c2simXmlHandler;
	stompLib->disconnect();
	XMLPlatformUtils::Terminate();
	stopReports();
}

// text for position reports
namespace {
	// pieces of ibml general status report
	std::string ibml09GSRpart1(
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		"<bml:BMLReport xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
		"xmlns:jc3iedm=\"urn:int:nato:standard:mip:jc3iedm:3.1a:oo:2.0\" "
		"xmlns:bml=\"http://netlab.gmu.edu/IBML\" "
		"xmlns:msdl=\"http://netlab.gmu.edu/JBML/MSDL\">"
		"<bml:Report>"
		"<bml:CategoryOfReport>StatusReport</bml:CategoryOfReport>"
		"<bml:TypeOfReport>GeneralStatusReport</bml:TypeOfReport>"
		"<bml:StatusReport><bml:GeneralStatusReport>"
		"<bml:ReporterWho><bml:UnitID>");
	std::string ibml09GSRpart2(
		"</bml:UnitID></bml:ReporterWho> "
		"<bml:Hostility>");
	std::string ibml09GSRpart3(
		"</bml:Hostility> "
		"<bml:Executer>"
		"<bml:UnitID>");
	std::string ibml09GSRpart4(
		"</bml:UnitID></bml:Executer>"
		"<bml:OpStatus>OPR</bml:OpStatus>"
		"<bml:WhereLocation><bml:GDC>"
		"<bml:Latitude>");
	std::string ibml09GSRpart5(
		"</bml:Latitude>"
		"<bml:Longitude>");
	std::string ibml09GSRpart6(
		"</bml:Longitude>"
		"<bml:ElevationAGL>0</bml:ElevationAGL>"
		"</bml:GDC></bml:WhereLocation>\n"
		"<bml:When>20070101000000.000</bml:When>"
		"<bml:ReportID>");
	std::string ibml09GSRpart7(
		"</bml:ReportID>"
		"<bml:Credibility>"
		"<bml:Source>HUMINT</bml:Source>"
		"<bml:Reliability>A</bml:Reliability>"
		"<bml:Certainty>RPTFCT</bml:Certainty>"
		"</bml:Credibility>"
		"</bml:GeneralStatusReport></bml:StatusReport></bml:Report></bml:BMLReport>");

	// pieces of C2SIM position report
	std::string c2simPositionPart1(
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		"<MessageBody xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\" "
		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
		"xsi:schemaLocation=\"http://www.sisostds.org/schemas/C2SIM/1.1/C2SIM"
		" http://www.sisostds.org/schemas/C2SIM/1.1/C2SIM_Experimental.xsd\">"
		"<DomainMessageBody>"
		"<ReportBody>"
		"<FromSender>");
	std::string c2simPositionPart2(
		"</FromSender>"
		"<ToReceiver>00000000-0000-0000-0000-000000000000</ToReceiver>"
		"<ReportContent>"
		"<PositionReportContent>"
		"<TimeOfObservation>"
		"<IsoDateTime>");
	std::string c2simPositionPart3(
		"</IsoDateTime>"
		"</TimeOfObservation>"
		"<EntityHealthStatus>"
		"<OperationalStatus>"
		"<OperationalStatusCode>");
	std::string c2simPositionPart3a(
		"</OperationalStatusCode>"
		"</OperationalStatus>"
		"</EntityHealthStatus>"
		"<EntityHealthStatus>"
		"<Strength>"
		"<StrengthPercentage>");
	std::string c2simPositionPart3b(
		"</StrengthPercentage>"
		"</Strength>"
		"</EntityHealthStatus>"
		"<Location>"
		"<GeodeticCoordinate>"
		"<Latitude>");
	std::string c2simPositionPart4(
		"</Latitude>"
		"<Longitude>");
	std::string c2simPositionPart5(
		"</Longitude>"
		"</GeodeticCoordinate>"
		"</Location>"
		"<SubjectEntity>");
	std::string c2simPositionPart8(
		"</SubjectEntity>"
		"</PositionReportContent>"
		"</ReportContent><ReportID>");
	std::string c2simPositionPart9(
		"</ReportID><ReportingEntity>");
	std::string c2simPositionPart10(
		"</ReportingEntity>"
		"</ReportBody>"
		"</DomainMessageBody>"
		"</MessageBody>");
}

/**
 *  function to find lat, lon and alt of a UUID as strins
 *  returns false if values not found; otherwise true
 */
bool C2SIMinterface::getUnitGeodeticFromSim(
	Unit* unit,
	std::string &latString, 
	std::string &lonString, 
	std::string &elAglString ) {

	// set return values to zero
	latString = "0";
	lonString = "0";
	elAglString = "0";
	
	// make a DtUUID from uuid
	std::string uuid = unit->uuid;
	DtString vrfUuid = unit->vrfUuid;

	// get the geolocation of entity associated with uuid
	DtReflectedObject* obj = uuidMgr->reflectedObjectFor(DtUUID(vrfUuid));
	if (obj == nullptr) {
		std::cout << "UNABLE TO GET REFLECTED OBJECT FOR UNIT:" << unit->name <<
			"|" << unit->uuid << "| CAN'T DETERMINE GEOCOORDS\n";
		return false;
	}
	DtReflectedEntity* ent = static_cast<DtReflectedEntity*>(obj);
	DtEntityStateRepository* esr = ent->entityStateRep();
	if (esr == nullptr){
		std::cout << "UNABLE TO GET ENTITY STATE REPOSITORY FOR UNIT:" << unit->name <<
			"|" << unit->uuid << "| CAN'T DETERMINE GEOCOORDS\n";
		return false;
	}
	DtVector geoLocation = esr->location();
	DtGeodeticCoord geod;
	geod.setGeocentric(geoLocation);

	// make a char* for lat and lon and return
	latString = doubleToString(geod.lat()*degreesToRadians);
	lonString = doubleToString(geod.lon()*degreesToRadians);
	elAglString = doubleToString(geod.alt());
	return true;
}

/**
*   function to capture current GMT date and time a strings
*/
std::string c2simReportDateTime() {
	// use Microsoft system function
	SYSTEMTIME st;
	GetSystemTime(&st);
	char systime[13], sysdate[11];

	// format to Java SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")
	// example 2019-01-01T00:00:01Z
	std::sprintf(systime, "%02d:%02d:%02d,%03d", st.wHour, st.wMinute, st.wSecond, st.wMilliseconds);
	std::sprintf(sysdate, "%04d-%02d-%02d", st.wYear, st.wMonth, st.wDay);
	std::string formatDate = sysdate;
	std::string formatTime = systime;
	std::string datePlusTime = formatDate.append("T").append(formatTime).append("Z");
	return datePlusTime;
}

// thread function to generate C2SIMv11 reports
void C2SIMinterface::stopReports() {
	reportTimeInterval = 0;
}
void C2SIMinterface::reportGenerator() {

	// this runs only if reportInterval > 0 on command line
	if (reportIntervalParm== 0)return;

	// done proceed until all objects have bene created
	while (!initializationDone)DtSleep(.1);
	while (!textIf->timeToQuit()) {
		if (reportTimeInterval == 0)return;
		DtSleep(reportTimeInterval);
		std::cout << "**********REPORT CYCLE\n";
		// cycle through the UnitMap generating one report for each
		std::map<std::string, Unit*>::iterator unitIter;
		for (unitIter = c2simXmlHandler->getUnitMapBegin();
			unitIter != c2simXmlHandler->getUnitMapEnd();
			++unitIter) {

			// get next real unit uuid that we are simulating, to report on
			Unit* reportingUnit = unitIter->second;
			if (reportingUnit == nullptr)continue;
			if (reportingUnit->uuid == "")continue;
			if (reportingUnit->systemName != c2simClientIDCode)continue;

			// get the geolocation of entity associated with reportingUnit
			std::string latString, lonString, altString;
			if (!getUnitGeodeticFromSim(reportingUnit, latString, lonString, altString)) {
				std::cout << "CAN'T MAKE POSITION REPORT FOR " << reportingUnit->name << "/n";
				continue;
			}

			// make char* for geodetic coords
			int latSize = latString.size() + 1;
			int lonSize = lonString.size() + 1;
			int altSize = altString.size() + 1;
			char* latChars = new char[latSize];
			char* lonChars = new char[lonSize];
			char* altChars = new char[altSize];
			strcpy(latChars,latString.c_str());
			strcpy(lonChars, lonString.c_str());
			strcpy(altChars, altString.c_str());

			// create a position report
			// get hostility and OpStat from the unitMap
			std::string objectName = reportingUnit->name;
			std::string hostilityCode = c2simXmlHandler->getHostilityCode(objectName);
			std::string opStatusCode = c2simXmlHandler->getOpStatusCode(objectName);
			std::string strength = c2simXmlHandler->getStrength(objectName);

			// get UUID from the unitNameMap
			std::string uuid = reportingUnit->uuid;

			// choose report format matching order received
			if ((hostilityCode == "HO" && sendRedTracking) ||
				(hostilityCode != "HO" && sendBlueTracking)){

				std::string reportMessage =
					c2simPositionPart1 + uuid + //objectName +
					c2simPositionPart2 + c2simReportDateTime() + // reporting time
					c2simPositionPart3 + opStatusCode + // operational status
					c2simPositionPart3a + strength + // strength percentage
					c2simPositionPart3b + latChars + // latitude
					c2simPositionPart4 + lonChars + // longitude
					c2simPositionPart5 + uuid + // subjsctEntity
					c2simPositionPart8 + textIf->makeReportID() + // unique ID
					c2simPositionPart9 + uuid + // reportingEntity
					c2simPositionPart10;

				DtTextInterface::sendRest(true,
					serverRestStompAddress,
					restServerPort,
					c2simClientIDCode,
					uuid,
					reportMessage);
				
			}// end if(hostility...
		}// end for (unitIter
	}// end while(!textIf->timeToQuit())
}// end reportGenerator()

// instantiate a REST client
C2SIMClientREST_Lib* getRestLib()
{
	C2SIMClientREST_Lib*restLib =
	new C2SIMClientREST_Lib(
		"VRFORCES@" + localHostAddress,
		serverRestStompAddress,
		"INFORM",
		c2simVersionCif);
	restLib->setHost(serverRestStompAddress);
	restLib->setSubmitter("VRFORCES");
	restLib->setPath("C2SIMServer/c2sim");
	restLib->setProtocol(SISOSTD);
	return restLib;
}


// returns FromSender from order
std::string C2SIMinterface::getOrderSender() {
	return headerSender;
}

// returns ToReceiver from order
std::string C2SIMinterface::getOrderReceiver() {
	return headerReceiver;
}

// updates system state from parser if it changed
// otherwise uses initial system state from connection
// returns the updated value
std::string systemState;
std::string updateSystemState(){
	std::string checkState = c2simXmlHandler->getSystemState();
	if (checkState != "")systemState = checkState;
	return systemState;
}

// produce next unique name as string
unsigned int nameIndex = 0;
std::string nextName(){
	char buffer[5];
	sprintf(buffer, "%04u", ++nameIndex);
	return "C2S" + string(buffer);
}

// coordinate conversions
// GDC: lat/lon in degrees; alt in meters
// geocentric: x/y/z in meters from center of earth
// NOTE: VRForces uses radian angles; we convert here
std::string doubleToString(double d) {
	std::ostringstream oss;
	oss << d;
	return oss.str();
}
double stringToDouble(std::string s) {
	double d = atof(s.c_str());
	return d;
}
void geodeticToGeocentric(
	std::string lat, std::string lon, std::string alt,
	std::string &x, std::string &y, std::string &z) {

	// convert to geocentric
	DtGeodeticCoord geod(
		stod(lat) / degreesToRadians,
		stod(lon) / degreesToRadians,
		stod(alt));
	DtVector geoc = geod.geocentric();
	
	// move into output strings
	x = doubleToString(geoc.x());
	y = doubleToString(geoc.y());
	z = doubleToString(geoc.z());
}
void geocentricToGeodetic(std::string x, std::string y, std::string z,
	std::string &lat, std::string &lon, std::string &alt) {

	// convert to geodetic
	DtVector geoc(stod(x), stod(y), stod(z));
	DtGeodeticCoord geod;
	geod.setGeocentric(geoc);

	// move into output strings
	lat = doubleToString(geod.lat()*degreesToRadians);
	lon = doubleToString(geod.lon()*degreesToRadians);
	alt = doubleToString(geod.alt());
}

// convert string coordinates to double
void convertCoordinates(
	string lat, string lon, string elev,
	double &x, double &y, double &z) 
{
	// convert input geodetic coordinates to geocentric to return
	if (elev == "")elev = "0.0e0";
	DtGeodeticCoord geod(
		std::stod(lat) / degreesToRadians,
		std::stod(lon) / degreesToRadians,
		std::stod(elev));
	DtVector geoc = geod.geocentric();

	// check for converted elevation below MSL
	DtGeodeticCoord checkGeod;
	checkGeod.setGeocentric(geoc);
	if (checkGeod.alt() < 0.0e0) {
		std::cerr << "negative elevation set to 5.0m when recoding coordinate (" <<
			lat << "," << lon << "," << elev << ")\n";
		DtGeodeticCoord hackGeod(
			std::stod(lat) / degreesToRadians,
			std::stod(lon) / degreesToRadians,
			5.0e0);
		geoc = geod.geocentric();
	}

	// return geocentric 
	x = geoc.x();
	y = geoc.y();
	z = geoc.z();
}
// end convertCoordinates()

 // read an XML file and return it as wstring
string C2SIMinterface::readAnXmlFile(string filename) {

	// open file, get count of lines, and start reader
	ifstream xmlFile(filename);
	if (!xmlFile.is_open()) {
		std::cout << 
			"readAnXmlFile can't open input file " << filename << "\n";
		return "";
	}

	// read the file into string
	string xmlBuffer, xmlLine;
	while (getline(xmlFile, xmlLine)) {
		xmlBuffer.append(xmlLine);
	}
	xmlFile.close();
	return xmlBuffer;

}// end readAnXmlFile()

 // write an XML file given name and wstring
void C2SIMinterface::writeAnXmlFile(std::string filename, string content) {

	// open file for output
	std::remove(filename.c_str());
	std::ofstream xmlFile(filename, ios::out);
	if (!xmlFile.is_open()) {
		std::cout << 
			"writeAnXmlFile can't open output file " << filename << "\n";
		return;
	}

	// write the file from outputFrame message content
	xmlFile << content;
	xmlFile.close();

}// end writeAnXmlFile()

// holds up execution of its thread until Unit variable we use
// as semaphore is set to something other than empty string
void waitForData(Unit* unitData){

	while (!unitData->createdObject) DtSleep(.1);
}

// create rotary wing AH-64 Apache and CXH-47 Chinook helicopters  TODO: greater variety of platforms
void createRW(
	DtVector vec,
	std::string objectName,
	std::string hostilityCode,
	std::string disEntityType) {

	DtReal heading = 90.0f;
	Unit* unitData = c2simXmlHandler->getUnitByName(objectName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);
	DtForceType forceType = DtForceFriendly;
	if (hostilityCode == "HO")forceType = DtForceOpposing;

	// create the aircraft
	if (disEntityType == "1.2.225.23.1.1.0")// CH-47 Chinook
		textIf->controller()->createEntity(
			DtTextInterface::vrfObjectCreatedCb, textIf,
			DtEntityType(1, 2, 225, 23, 1, 1, 0), vec,
			forceType, heading, objectName);
	else // everything else is an AH-64 for now
		textIf->controller()->createEntity(
			DtTextInterface::vrfObjectCreatedCb, textIf,
			DtEntityType(1, 2, 225, 20, 1, 1, 0), vec,
			forceType, heading, objectName);

	// wait for new entity to be available
	waitForData(unitData);

	// give it an elevation above ground
	textIf->controller()->
		setAltitude(
		DtUUID(unitData->vrfUuid),
		stod(unitData->elevationAgl) + 1.,
		TRUE);

	std::cout << "created rotary wing aircraft name:" << objectName 
		<< " hostility:" << hostilityCode << " DIS Entity type:" 
		<< disEntityType << " UUID:" << unitData->uuid << "\n";
	
}// end createRW()

// create a boat object in VR-Forces
void createBoat(
	DtVector vec,
	std::string objectName,
	std::string hostilityCode) {

	DtReal heading = 90.0f;
	Unit* unitData = c2simXmlHandler->getUnitByName(objectName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);
	DtForceType forceType = DtForceFriendly;
	if (hostilityCode == "HO")forceType = DtForceOpposing;

	// create the boat
	if (hostilityCode == "HO")
		textIf->controller()->createEntity(
			DtTextInterface::vrfObjectCreatedCb, textIf,
			DtEntityType(1, 3, 0, 84, 1, 0, 0), vec,
			forceType, heading, objectName);
	else
		textIf->controller()->createEntity(
			DtTextInterface::vrfObjectCreatedCb, textIf,
			DtEntityType(1, 3, 0, 61, 11, 0, 1), vec,
			forceType, heading, objectName);
	
	// wait for new entity to be available
	waitForData(unitData);

	// give it an elevation above water
	textIf->controller()->
		setAltitude(
		DtUUID(unitData->vrfUuid),
		stod(unitData->elevationAgl) + 1.,
		TRUE);

	std::cout << "created BOAT name:" << objectName
		<< " hostility:" << hostilityCode << " UUID:" << unitData->uuid << "\n";

}// end createBoat()

// create a tank object in VR-Forces
void createTank( 
	DtVector vec, 
	std::string objectName,
	std::string hostilityCode) {

	DtReal heading = 90.0f;
	Unit* unitData = c2simXmlHandler->getUnitByName(objectName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);
	DtForceType forceType = DtForceFriendly;
	if (hostilityCode == "HO")forceType = DtForceOpposing;

	// create the tank
	textIf->controller()->createEntity(
		DtTextInterface::vrfObjectCreatedCb, textIf,
		DtEntityType(1, 1, 225, 1, 1, 3, 0), vec,
		forceType, heading, objectName);

	// wait for new entity to be available
	waitForData(unitData);

	// give it an elevation above water
	textIf->controller()->
		setAltitude(
		DtUUID(unitData->vrfUuid),
		stod(unitData->elevationAgl) + 1.,
		TRUE);

	std::cout << "created TANK name:" << objectName 
		<< " hostility:" << hostilityCode << " UUID:" << unitData->uuid << "\n";

}// end createTank()

 // creates an aggregated object in VR-Forces
 // adding the individual platforms to UnitMap
static int nextAggregateVehicleId = 0;
void createAggregate(
	DtVector vec,
	std::string unitName,
	std::string hostilityCode,
	std::string vehicleNameBase,
	DtObjectType oType,
	std::string vehicleDISType) {

	// if a heading was initialized, apply it
	DtReal heading = 0.0f;// default
	Unit* unitData = c2simXmlHandler->getUnitByName(unitName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);

	// apply friendly/hostile status
	DtForceType forceType = DtForceFriendly;
	if (hostilityCode == "HO")forceType = DtForceOpposing;

	// create the aggregated unit 
	//DtList vehicleNames;
	unsigned long requestId = textIf->controller()->generateRequestId();
	DtList vertices;
	vertices.add(&vec);
	textIf->controller()->sendVrfObjectCreateMsg(
		DtSimSendToAll,
		requestId,
		unitName,
		oType,
		vertices,
		DtString::nullString(),
		DtAppearance::nullAppearance(),
		forceType,
		false,
		false,
		heading,
		true);
	unitData->vrfUuid = DtUUID(unitName).uuidString();

	std::cout << "created aggregated " << vehicleNameBase << " unit named:" <<
		unitName << " vrfUuid:" << unitData->vrfUuid <<
		" C2SIM UUID:" << unitData->uuid <<
		" hostility:" << hostilityCode << "\n";

}// end createAggregate()

// create an aggregated Scout Unit object in VR-Forces
static int nextScoutVehicleId = 0;
void createScoutUnit( 
	DtVector vec, 
	std::string unitName,
	std::string hostilityCode) {

	// create aggregated unit 
	DtObjectType oType(DtObjectTypePseudoAggregate,
		DtEntityType(11, 1, 225, 14, 30, 0, 0));

	textIf->controller()->createAggregate(
		DtTextInterface::vrfObjectCreatedCb,
		textIf->controller(),
		DtEntityType(11, 1, 225, 2, 1, 1, 0), vec,
		DtForceFriendly, 0, unitName,
		DtString::nullString(), DtSimSendToAll,
		DtDisaggregated, DtUUID::nullUUID(), true);

}// end createScoutUnit()

// create an aggregatedArmor Platoon object in VR-Forces
void createArmorPlatoon(
	DtVector vec,
	std::string unitName,
	std::string hostilityCode) {

	DtReal heading = 90.0f;// default
	Unit* unitData = c2simXmlHandler->getUnitByName(unitName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);
	DtForceType forceType = DtForceFriendly;
	if (hostilityCode == "HO")forceType = DtForceOpposing;

	// create aggregated unit 
	unsigned long requestId = textIf->controller()->generateRequestId();
	DtObjectType oType(DtObjectTypePseudoAggregate,
		DtEntityType(11, 1, 225, 3, 2, 0, 0));
	textIf->controller()->createAggregate(
		DtTextInterface::vrfObjectCreatedCb,
		textIf->controller(),
		DtEntityType(11, 1, 225, 1, 1, 3, 0), vec,
		forceType, 0, unitName,
		DtString::nullString(), DtSimSendToAll,
		DtDisaggregated, DtUUID::nullUUID(), true);
	/*
	//createAggregate(vec, unitName, hostilityCode, "AFV", oType, "1.1.225.1.1.3.0");
	DtList vertices;
	vertices.add(&vec);
	textIf->controller()->sendVrfObjectCreateMsg(
		DtSimSendToAll,
		requestId,
		unitName,
		oType,
		vertices,
		DtString::nullString(),
		DtAppearance::nullAppearance(),
		forceType,
		false,
		false,
		heading,
		true
	);
	unitData->vrfUuid = DtUUID(unitName).uuidString();
	*/
	std::cout << "created aggregated ArmorPlatoon unit named:" << unitName <<
		" vrfUuid:" << unitData->vrfUuid <<
		" C2SIM UUID:" << unitData->uuid <<
		" hostility:" << hostilityCode << "\n";

	// wait for new entity to be available
	waitForData(unitData);

}// end createArmorPlatoon()

// create an aggregatedArmor Company object in VR-Forces
void createArmorCompany(
	DtVector vec,
	std::string unitName,
	std::string hostilityCode) {
	
	// set parameters from initialization
	DtReal heading = 90.0f;// default
	Unit* unitData = c2simXmlHandler->getUnitByName(unitName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);
	DtForceType forceType = DtForceFriendly;
	if (hostilityCode == "HO")forceType = DtForceOpposing;
	
	// create aggregated unit 
	unsigned long requestId = textIf->controller()->generateRequestId();
	DtObjectType oType(DtObjectTypePseudoAggregate,
		DtEntityType(11, 1, 225, 5, 2, 0, 0));
	textIf->controller()->createAggregate(
		DtTextInterface::vrfObjectCreatedCb,
		textIf->controller(),
		DtEntityType(11, 1, 225, 5, 2, 0, 0), vec,
		forceType, 0, unitName,
		DtString::nullString(), DtSimSendToAll, 
		DtDisaggregated, DtUUID::nullUUID(), true);
	std::cout << "created aggregated ArmorCompany unit named:" << unitName <<
		" vrfUuid:" << unitData->vrfUuid <<
		" C2SIM UUID:" << unitData->uuid <<
		" hostility:" << hostilityCode << "\n";

	// wait for new entity to be available
	waitForData(unitData);

}// end createArmorCompany()

// create an aggregated Armor Company HQ object in VR-Forces
void createArmorCoHQ(
	DtVector vec,
	std::string unitName,
	std::string hostilityCode) {

	DtReal heading = 90.0f;// default
	Unit* unitData = c2simXmlHandler->getUnitByName(unitName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);
	DtForceType forceType = DtForceFriendly;
	if (hostilityCode == "HO")forceType = DtForceOpposing;
	DtList vehicleNames;

	// create aggregated unit 
	unsigned long requestId = textIf->controller()->generateRequestId();
	DtObjectType oType(DtObjectTypePseudoAggregate,
		DtEntityType(11, 1, 225, 5, 20, 0, 0));
	textIf->controller()->createAggregate(
		DtTextInterface::vrfObjectCreatedCb,
		textIf->controller(),
		DtEntityType(11, 1, 225, 5, 20, 0, 0), vec,
		forceType, 0, unitName,
		DtString::nullString(), DtSimSendToAll,
		DtDisaggregated, DtUUID::nullUUID(), true);
	std::cout << "created aggregated ArmorCoHQ unit named:" << unitName <<
		" vrfUuid:" << unitData->vrfUuid <<
		" C2SIM UUID:" << unitData->uuid <<
		" hostility:" << hostilityCode << "\n";

	// wait for new entity to be available
	waitForData(unitData);

}// end createArmorCoHQ()

// creates a civilian male neutral
void createCivilian(DtVector newUnitVec, std::string objectName){
	DtReal heading = 90.0f;
	Unit* unitData = c2simXmlHandler->getUnitByName(objectName);
	const char* phi = (unitData->directionPhi).c_str();
	if (unitData->directionPhi != "")heading = atof(phi);
	DtForceType forceType = DtForceNeutral;

	// create the civilian
	textIf->controller()->createEntity(
		DtTextInterface::vrfObjectCreatedCb, textIf,
		DtEntityType(3, 1, 225, 3, 0, 1, 0), newUnitVec,
		forceType, heading, objectName);

	// wait for new entity to be available
	waitForData(unitData);

}// end createCivilian()

// sets a control point altitude
// parameters: name of point, new altitude
void setPointAltitudeAgl(DtString name, double altitudeAgl)
{
	DtScriptedTaskSet sTSet;
	sTSet.init();
	sTSet.setScriptId("set_point_agl");

	DtRwReal* newAlt = new DtRwReal("altitudeAgl");
	newAlt->setValue(altitudeAgl);
	sTSet.variables().addVariable(newAlt);

	textIf->controller()->sendSetDataMsg(DtUUID(name), &sTSet, DtSimSendToAll);
}

// builds a Waypoint given string version of coordinates
void makeWaypoint(
	std::string lat, 
	std::string lon,
	std::string elev,
	std::string waypointName,
	Task* waypointTask)
{
	double x, y, z;
	convertCoordinates(lat, lon, elev, x, y, z);
	DtVector vec(x, y, z);
	waypointTask->taskRouteDtString = "";
	textIf->controller()->createWaypoint(
		DtTextInterface::vrfObjectCreatedCb,
		(void*)"waypoint",
		vec,
		waypointName
		);
	
	// wait for the new route to be available
	while (waypointTask->taskRouteDtString == "")Sleep(.1);

	// set waypoint elevation to ground altitude
	setPointAltitudeAgl(waypointName, 0.f);
	std::cout << "CREATED WAYPOINT NAMED |" << waypointName <<
		"| coordinates [" << lat << "," << lon << "," << "0.(AGL)]\n";
}

// implements Evacuate_civilians lua script 
void runEvacuate(Task* evacTask){

	// Fill in unit name from C2SIM tasking msg.
	Unit* rwaUnit = c2simXmlHandler->getUnit(evacTask->taskeeUuid);

	// make waypoints for three coordinate points in Task
	// pickupPoint is the Task's Location
	// dropoffPoint is the last point in the Route
	// returnPoint is the tasked unit's original position

	// pickupPoint 
	std::string pickupPointName = evacTask->taskName + " EVAC PICKUP";
	makeWaypoint(
		evacTask->latitudes[evacTask->locationPointCount-1],
		evacTask->longitudes[evacTask->locationPointCount - 1],
		evacTask->elevations[evacTask->locationPointCount - 1],
		pickupPointName,
		evacTask);

	// dropoffPoint
	std::string dropoffPointName = evacTask->taskName + " EVAC DROPOFF";
	makeWaypoint(
		evacTask->latitudes[0],
		evacTask->longitudes[0],
		evacTask->elevations[0],
		dropoffPointName,
		evacTask);

	// returnPoint
	std::string returnPointName = evacTask->taskName + " EVAC RETURN";
	makeWaypoint(
		rwaUnit->latitude,
		rwaUnit->longitude,
		rwaUnit->elevationAgl,
		returnPointName,
		evacTask);

	// if the Unit is not a Chinook don't run this
	if (rwaUnit->disEntityType != "1.2.225.23.1.1.0"){
		c2simXmlHandler->displayError(
			"ERROR _ TASK " + evacTask->taskName + 
			" calls for evacuate-civilians with inapproprite aircraft");
		return;
	}

	// make the task to execute the luascript
	DtScriptedTaskTask task;
	task.init();
	task.setScriptId("evacuate_civilians");

	// insert pickup point in task
	DtUUID puPt(pickupPointName);
	DtRwObjectName* puPointVar = new DtRwObjectName("pickupPoint");
	puPointVar->setUUID(puPt);
	task.variables().addVariable(puPointVar);

	// insert dropoff point in task
	DtUUID doPt(dropoffPointName);
	DtRwObjectName* doPointVar = new DtRwObjectName("dropoffPoint");
	doPointVar->setUUID(doPt);
	task.variables().addVariable(doPointVar);

	// insert return point in task
	DtUUID rtPt(returnPointName);
	DtRwObjectName* retPointVar = new DtRwObjectName("returnPoint");
	retPointVar->setUUID(rtPt);
	task.variables().addVariable(retPointVar);

	// make UUID and invoke the task
	evacTask->scriptedTaskUuid = DtUUID(rwaUnit->vrfUuid);
	textIf->controller()->sendTaskMsg(DtUUID(rwaUnit->vrfUuid), &task);

}// end runEvacuate()
/*
// runs an Embark task
void runEmbark(Task* task){
	Unit* taskeeUnit = c2simXmlHandler->getUnit(task->taskeeUuid);
	Unit* embarkToUnit = c2simXmlHandler->getUnit(task->affectedEntity);
	textIf->controller()->embarkObject(
		textIf->controller()->simulationAddress(),
		DtUUID(taskeeUnit->name),
		DtUUID(embarkToUnit->name));
}

// runs a Debark task
void runDebark(Task* task){
	Unit* taskeeUnit = c2simXmlHandler->getUnit(task->taskeeUuid);
	textIf->controller()->disembarkObject(
		textIf->controller()->simulationAddress(),
		DtUUID(taskeeUnit->name));
}
*/

// create a MobileIrregular aggregate
void createMobileIrregular(
		DtVector vec,
		std::string unitName,
		std::string hostilityCode) {

	textIf->controller()->createAggregate(
		DtTextInterface::vrfObjectCreatedCb,
		textIf->controller(),
		DtEntityType(11, 1, 0, 13, 34, 0, 1), vec,
		//DtEntityType(11, 1, 225, 5, 2, 0, 0), vec,
		DtForceOpposing, 0, unitName,
		DtString::nullString(), DtSimSendToAll,
		DtDisaggregated, DtUUID::nullUUID(), true);

	// wait for new entity to be available
	waitForData(c2simXmlHandler->getUnitByName(unitName));

}// end createMobileIrregular()

// extract data associated with first instance of a tag in an XML string
std::string extractFromXml(std::string xmlToSearch, std::string tagToFind) {
	std::string openTag = "<" + tagToFind;
	std::string closeTag = "</" + tagToFind + ">";
	size_t startOfOpenTag = xmlToSearch.find(openTag);
	if (startOfOpenTag == std::string::npos) return "";
	size_t startOfData = xmlToSearch.find(">", startOfOpenTag) + 1;
	size_t endOfData = xmlToSearch.find(closeTag, startOfData);
	return xmlToSearch.substr(startOfData, endOfData - startOfData);

}// end extractFromXml()

// extract values from parsed C2SIMInitialization message 
int extractC2simInit(std::string xmlString, std::string clientId) {
	
	// get count of units to be initialized
	int numberOfUnits = 
		c2simXmlHandler->C2SIMxmlHandler::getNumberOfUnits();
	if (c2simXmlHandler->unitMapIsEmpty()) {
		std::cout << "no units found - can't run simulation\n";
		Sleep(10000);
		return 0;
	}
	std::cout << "initialization message contains " << numberOfUnits << " units\n";
	
	// initialize in VR-Forces units that match this SystemName
	int numberInitialized = 0;
	std::map<std::string, Unit*>::iterator unitIter;
	for (unitIter = c2simXmlHandler->getUnitMapBegin();
		unitIter != c2simXmlHandler->getUnitMapEnd();
		++unitIter) {

		// confirm it is a real unit not a reference
		Unit* newUnit = unitIter->second;
		if (newUnit == nullptr)continue;
		if (newUnit->uuid == "")continue;
		
		// determine what name we'll use for it
		std::string unitName = newUnit->name;

		// if unit name is not provided, create one
		if (unitName.empty()){
			newUnit->name = nextName();
			
			// insert this Unit in unitNameMap indexed by unit name
			if (!c2simXmlHandler->addUnitByName(newUnit))continue;
			newUnit->systemName = clientId;

			// wait for new entity to be available
			waitForData(newUnit);
		}

		// confirm our Unit has a SystemName
		if (newUnit->systemName == "") {
			c2simXmlHandler->displayError(
				"ERROR: unit " + unitName + " with UUID " + newUnit->uuid +
				" missing SystemName - cannot create VRF object");
			continue;// to while(true)
		}
		std::string systemName = std::string(newUnit->systemName);
		if (systemName != clientId){
			std::cout << "unit " << unitName
				<< " SystemName " << systemName
				<< " does not match our clientId "
				<< clientId << " - will not create VRF object or tasks\n";
			continue;// to end for(unitIter...
		}
		if (newUnit->hostilityCode == "") {
			std::cout << "unit " << unitName
				<< " missing Hostility - cannot create VRF object\n";
			continue;// to while(true)
		}
		
		// instantiate the object in VR-Forces
		if (!newUnit->vrfObjectHasBeenCreated &&
			systemName == clientId) {

			// check that coordinates were provided
			if ((newUnit->latitude == "") || (newUnit->longitude == "")) {
				std::cout << "missing initial latitude/longitude for unit " << unitName
					<< " - omitting it\n";
				continue;
			}
			
			// convert coordinate to geocentric
			double x, y, z;
			if (newUnit->elevationAgl == "")
				newUnit->elevationAgl = "1000.0";// triggers VRForces Gound Clamping
			convertCoordinates(
				newUnit->latitude, newUnit->longitude, newUnit->elevationAgl, x, y, z);
			DtVector newUnitVec(x, y, z);
			
			// create unit objects TODO: more types
			std::string symbolIdString = std::string(newUnit->symbolId);
			if (symbolIdString != "") {
				std::string echelonCode = symbolIdString.substr(11, 1);
				
				// aircraft: for now, use AH64 for both fixed and rotary wing TODO: more aircraft
				if (symbolIdString.substr(2, 1) == "A") {
					createRW(newUnitVec, unitName, newUnit->hostilityCode, newUnit->disEntityType);
				}
				else if (symbolIdString.substr(2, 1) == "S")// Sea Surface
					createBoat(newUnitVec, unitName, newUnit->hostilityCode);
				else if (symbolIdString.substr(1, 1) == "N")// Neutral
					createCivilian(newUnitVec, unitName);
				else if (echelonCode == "B") {//newUnit->echelon == "SQUAD")
					if (newUnit->hostilityCode == "HO")
						createMobileIrregular(newUnitVec, unitName, newUnit->hostilityCode);
					else
						createScoutUnit(newUnitVec, unitName, newUnit->hostilityCode);
				}
				else if (echelonCode == "D")
					createArmorPlatoon(newUnitVec, unitName, newUnit->hostilityCode);
				else if (echelonCode == "E")
					createArmorCompany(newUnitVec, unitName, newUnit->hostilityCode);
				else if (echelonCode == "F")// battalion code - substitute Company HQ as HHC
					createArmorCoHQ(newUnitVec, unitName, newUnit->hostilityCode);
				else // default object
					createTank(newUnitVec, unitName, newUnit->hostilityCode);
				newUnit->vrfObjectHasBeenCreated = true;
				numberInitialized++;
			}
			else { // default object
				if(newUnit->disEntityType != "1.1.225.1.1.3.0")
					std::cout << "missing SIDC String for Unit:" << unitName << 
						" creating Tank object\n";
				createTank(newUnitVec, unitName, newUnit->hostilityCode);
			}// end if (symbolIdString != "")
		}// end if (newUnit.submitter == clientId)
	}// end for(unitIter...

	std::cout << "initialized " << numberInitialized << " units in VRForces\n";
	// next line probably this will beat the server
	// message that causes the same effect
	initializationDone = true;
	return numberOfUnits;

}// end extractC2simInit()

 // thread function to listen for incoming STOMP message; parse the message,
 // and use the result to send a command to VRForces
void C2SIMinterface::readStomp(
	C2SIMinterface* c2simInterface,
	bool skipInitialize,
	std::string clientId,
	int reportIntervalRef) {

	// establish pieces we need
	reportTimeInterval = reportIntervalRef;
	textIf->setC2SIMxmlHandler(c2simXmlHandler);
	HRESULT hr = CoInitialize(NULL);
	stompLib = new C2SIMClientSTOMP_Lib();
	C2SIMSTOMPMessage* stompMessage = nullptr;
	initializationDone = false;
	int numberOfUnits = 0;
	int taskNumber = 0;
	std::string xmlString = "";
	int xmlSize;
	std::string lastOrderId = "";

	// connect to STOMP server
	std::cout << "connecting STOMP stream to "
		<< serverRestStompAddress << ":" << stompServerPort << "\n";
	try {
		// set filter to receive only the chosen format
		if (useIbml && skipInitialize)stompLib->addAdvSubscription("protocol = 'BML'");
		if (!useIbml && skipInitialize)stompLib->addAdvSubscription("protocol = 'SISO-STD-C2SIM'");

		// set connection parameters and connect
		stompLib->setHost(serverRestStompAddress);
		stompLib->setPort(stompServerPort);
		std::cout << "CONNECTION RESPONSE:" << 
			stompLib->connect()->getMessageType() << "\n";

		// get system status
		C2SIMClientREST_Lib* restLib = getRestLib();
		std::string statusResponse = restLib->c2simCommand("STATUS", "", "");
		restLib->~C2SIMClientREST_Lib();
		systemState = extractFromXml(statusResponse, "sessionState");
		std::cout << "SYSTEM STATE AT CONNECTION:" << systemState << "|\n";

		// if we are a late joiner (scenario already running) ask for initialzation
		if(!skipInitialize)
		if (systemState == "RUNNING" || systemState == "INITIALIZED" || systemState == "PAUSED") {
			std::cout << "SERVER ALREADY RUNNING - REQUESTING LATE JOIN INITIALIZATION\n";
			restLib = getRestLib(); 
			xmlString = restLib->c2simCommand("QUERYINIT", "", "");
			std::cout << "received INIT XML file, length:" << xmlString.size() << "\n";
			if (debug) 
				std::cout << "INIT XML:" << xmlString << "\n";
			xmlString = 
				"<MessageBody>" + extractFromXml(xmlString, "MessageBody") + "</MessageBody>";
			restLib->~C2SIMClientREST_Lib();
			textIf->setStarted(updateSystemState());
			
			// make sure VRForces is ready for object creation
		    if(usingHLA)while (!textIf->controller()->allBackendsReady())DtSleep(.1);
			
			// run the parser to digest the initialization
			c2simXmlHandler->startC2SIMParse(xmlString);
			try {
				// run the parser, which will callback
				// our XML handler, input from XML in memory
				xmlSize = xmlString.size();			
				char* xmlCstr = new char[xmlSize + 1];
				strncpy(xmlCstr, xmlString.c_str(), xmlSize);
				MemBufInputSource xmlInMemory(
					(const XMLByte*)xmlCstr,
					xmlSize,
					"xmlInMemory",
					false);
				parser->parse(xmlInMemory);
			}
			catch (const XMLException& toCatch) {
				char* message = XMLString::transcode(toCatch.getMessage());
				std::cerr << "Exception message is: \n" << message << "\n";
				XMLString::release(&message);
			}
			catch (const SAXParseException& toCatch) {
				char* message = XMLString::transcode(toCatch.getMessage());
				std::cerr << "Exception message is: \n" << message << "\n";
				XMLString::release(&message);
			}
			catch (...) {
				std::cerr << "unexpected Exception in c2simVRF SAX parsing\n";
			}

			// at this point valid input can only be INIT
			if (!initializationDone && !skipInitialize) {
				if (c2simXmlHandler->getMessageType() != INIT) {
					c2simXmlHandler->displayError(
						"ERROR - DID NOT RECEIVE EXPECTED LATE JOINER C2SIM INITIALIZATION");
					DtSleep(10.);
					return;
				}
				numberOfUnits = extractC2simInit(xmlString, clientId);
			}
		}// end if (systemState...
	}
	catch (C2SIMClientException &bce) {
		std::cout << "C2SIM ClientLib connection exception:" << bce.getMessage() << "\n";
		DtSleep(10);
		return;
	}
	catch (...) {
		std::cout << "\nException in C2SIM ClientLib connection code\n\n";
		DtSleep(10);
		return;
	}
	if (!SUCCEEDED(hr)) {
		std::cout << "VRForces interconnect setup failed\n";
		return;
	} else {
		try {
			// wait a second for VR-Forces to start
			// TODO: figure out how to synchronize this
			DtSleep(1.0);
			if (!skipInitialize) {
				if(numberOfUnits == 0)
					std::cout << "READY FOR C2SIM INITIALIZATION\n";
			}
			else {
				std::cout << "CONFIGURED TO SKIP INITIALIZATION\n";
				textIf->setStarted("RUNNING");
			}
			
			// loop reading XML documents, 
			// parsing them into VR-Forces controls
			// could have loaded C2SIMInitialization in xmlString;
			// clear the xmlString in all but first pass		
			while (true) {
				
				// read a STOMP message 
				xmlString = "";
				if (stompMessage != nullptr)delete stompMessage;
				stompMessage = stompLib->getNext_Block();

				// with server translation we get formats other than
				// C2SIM standard and IBML09
				std::string protocol = stompMessage->getC2SIMHeader()->getProtocol();
				std::string protocolVersion = stompMessage->getHeader("c2sim-version");
				if (useIbml)if (protocol != "BML")continue;
				if (!useIbml) {
					if (protocol != "SISO-STD-C2SIM")continue;
					if (protocolVersion != c2simVersionCif)continue;
				}
				xmlString = stompMessage->getMessageBody();
			    
				//std::string xmlString = // used for testing
				//	readAnXmlFile("C:\\Users\\c2sim\\Documents\\C2SIMtestBlueOrderH1.00_DR.xml").c_str();
				xmlSize = xmlString.size();
				if (xmlSize == 0)continue;
				if (debug)
					std::cout << "received STOMP message, protocol:" << protocol << " protocolVersion:" <<
					protocolVersion << " length:" << xmlSize << "\n" << xmlString << "\n";
				
				// drop reports immediately
				if (xmlString.find("PositionReportContent") != std::string::npos)continue;
				if (xmlString.find("ObservationReportContent") != std::string::npos)continue;
				if (xmlString.find("TaskStatus") != std::string::npos)continue;
				if (xmlString.find("BMLReport") != std::string::npos)continue;

				// use SAX to parse XML from a STOMP frame
				c2simXmlHandler->startC2SIMParse(xmlString);
				try {
					// run the parser, which will callback
					// our XML handler, input from XML in memory
					char* xmlCstr = new char[xmlSize + 1];
					strncpy(xmlCstr, xmlString.c_str(), xmlSize);
					MemBufInputSource xmlInMemory(
						(const XMLByte*)xmlCstr,
						xmlSize,
						"xmlInMemory",
						false);
					parser->parse(xmlInMemory);
				}
				catch (const XMLException& toCatch) {
					char* message = XMLString::transcode(toCatch.getMessage());
					std::cerr << "Exception message is: \n" << message << "\n";
					XMLString::release(&message);
				}
				catch (const SAXParseException& toCatch) {
					char* message = XMLString::transcode(toCatch.getMessage());
					std::cerr << "Exception message is: \n" << message << "\n";
					XMLString::release(&message);
				}
				catch (...) {
					std::cerr << "unexpected Exception in c2simVRF SAX parsing\n";
				}
				
				// check that we parsed something usable; if not drop this XML
				string parsedRootTag = c2simXmlHandler->getRootTag();
				if (parsedRootTag.length() == 0)continue;
				if (useIbml)
					if (parsedRootTag != "OrderPushIBML")continue;// to while(true)
				else
					if(parsedRootTag != "MessageBody")continue;// to while(true)

				// check for simulation control message
				std::string checkState = systemState;
				updateSystemState();
				if (systemState != checkState) {
					std::cout << "received new system state:" << systemState << "\n";

					// pass the control on to VR-Forces
					if (systemState == "RUNNING") {
						textIf->controller()->run();
						textIf->setStarted("RUNNING");
						continue;
					}
					else if (systemState == "PAUSED") {
						textIf->controller()->pause();
						textIf->setStarted("PAUSED");
						continue;// to while(true)
					}
					else if (systemState == "UNINITIALIZED") {
						textIf->setTimeToQuit(true);
						textIf->setStarted("UNINITIALIZED");
						Sleep(10000);
						break;// past while(true)
					}
				}
				std::cout << "completed parse of document with root tag " << parsedRootTag << "\n";

				// ignore SystemCommandBody messages
				if (c2simXmlHandler->getMessageType() == SYSTEM)continue;
				
				// if still in the initialize phase wait for message type INIT
				if (!initializationDone && !skipInitialize) {
					if (c2simXmlHandler->getMessageType() != INIT) {
						continue;
					}
					numberOfUnits = extractC2simInit(xmlString, clientId);
					continue;
				}
				
				// not report, control or initialization; must be an order

				// confirm it is an order we can use
				if ((useIbml && !c2simXmlHandler->isIbmlOrder()) ||
					(!useIbml && !c2simXmlHandler->isC2simOrder())) {
					c2simXmlHandler->displayError(
						"ERROR - XML ORDER NOT RECEIVED AS EXPECTED");
					continue;
				}
				int taskCount = c2simXmlHandler->getNumberOfTasksThisOrder();
				if (taskCount == 0) {
					std::cout << "ORDER IGNORED\n";
					continue;
				}
				++orderCount;
				std::cout << "order contains " << 
					c2simXmlHandler->getNumberOfTasksThisOrder() << " task(s)\n";

				// extract sender & receiver
				headerSender = stompMessage->getC2SIMHeader()->getFromSendingSystem();
				headerReceiver = stompMessage->getC2SIMHeader()->getToReceivingSystem();

				// wait for direction to run to start
				while (true) {
					std::string state = updateSystemState(); 
					DtSleep(1.); 
					if (state == "RUNNING")break;
				}
				textIf->controller()->run(); // server run command comes too late - do it here
				runStartTime = GetTickCount();// in ms
				
				// loop through all tasks from this order
				for (std::map<std::string, Task*>::iterator taskIter = c2simXmlHandler->getTaskMapBegin(); 
					taskIter != c2simXmlHandler->getTaskMapEnd(); 
					++taskIter) {
					
					Task* execTask = c2simXmlHandler->getTask(taskIter);
					if(execTask == nullptr)continue;
					if (execTask->orderUuid != c2simXmlHandler->getOrderUuid())continue;
					Unit* taskUnit = c2simXmlHandler->getUnit(execTask->taskeeUuid);
					
					// Task taskeeUuid must be findable in the unitMap
					if (taskUnit == nullptr) {
						if (skipInitialize) {
							if (execTask->taskeeUuid == "") {
								std::string missingTaskId = "MISSING";
								if (execTask->taskUuid != "")missingTaskId = execTask->taskUuid;
								std::cout << "TASK WITH ID " << missingTaskId <<
									" MISSING TASKEEUUID -- CANNOT PROCESS\n";
								continue;
							}
							// make a new Unit for this taskeeUuid and add it to unitMap
							taskUnit = c2simXmlHandler->makeEmptyUnit(execTask->taskeeUuid);
							if (!c2simXmlHandler->addUnit(taskUnit))continue;

							// generate a name for it and add to name map
							taskUnit->name = nextName();
							if(!c2simXmlHandler->addUnitByName(taskUnit))continue;
							taskUnit->systemName = clientId;

							// initialize unit position from data in Task
							taskUnit->hostilityCode = "AFR";
							taskUnit->latitude = execTask->latitudes[0];
							taskUnit->longitude = execTask->longitudes[0];
							taskUnit->elevationAgl = execTask->elevations[0];
							taskUnit->superiorUnit = "UNKNOWN";
							if (taskUnit->name.substr(taskUnit->name.length()-3) == "AV")
								taskUnit->symbolId = "SFAPMHAA--*****";
							else
								taskUnit->symbolId = "SFGPUCAA--*****";
						}
						else {
							// the unit name was not in the unitMap so did not get iniitialized
							string printTaskId = "MISSING";
							if (execTask->taskUuid != "")
								printTaskId = execTask->taskUuid;
							c2simXmlHandler->displayError(
								"ERROR - TASKEEUUID:" + execTask->taskeeUuid +
								" NOT FOUND IN C2SIMINITIALIZATION - CANNOT EXECUTE TASK");
							continue;
						}
					}
					
					// Start a thread to execute the task
					std::string taskId;
					if (execTask->taskUuid != "")
						taskId = execTask->taskUuid;
					else {
						if (orderCount > 1)
							taskId << "Order" << orderCount << "_Task"<< (taskNumber + 1);
						else
							taskId += "Task" + (taskNumber + 1);
						execTask->taskUuid = taskId;
					}
					std::thread t2(&C2SIMinterface::executeTask, taskId, textIf,
						skipInitialize, c2simInterface, execTask);
					t2.detach();

					// wait a bit so multiple tasks don't overlap in output
					DtSleep(.1);// 100 ms
					
				}//end for (taskNumber...
			
			}// end while(true) loop to receive XML
			
			// tell VRForces to shutdown
			textIf->setTimeToQuit(true);
			std::cout << "C2SIM VRF interface quitting\n";   
		}
		catch (C2SIMClientException& e) {
			std::cout << "can't read STOMP; C2SIMClientException:" << e.getMessage() << "\n";
			std::cout << "case.wqhat():" << e.getCauseMessage() << "\n";
		    std::cout << "C2SIM VRF interface quitting\n";
			DtSleep(10);
			return;
		}
		catch (...) {
			std::cerr << "unanticipated Exception in c2simVRFinterface readStomp\n";
		}
	}// end if (SUCCEEDED)

}// end readStomp()


// thread function to execute a C2SIM Task in VR-Forces
void C2SIMinterface::executeTask(
	std::string taskId,
	DtTextInterface* textIf,
	bool skipInitialize,
	C2SIMinterface* c2simInterface,
	Task* thisTask)
{
	// extract parameters from this task
	string dateTime = thisTask->dateTime;
	string taskeeUuid = thisTask->taskeeUuid;
	std::string taskUuid = thisTask->taskUuid;
	std::string taskName = thisTask->taskName;
	std::string waitOnTaskUuid = thisTask->startAfterTaskUuid;
	std::string waitOnTaskName = c2simXmlHandler->getTaskNameByUuid(waitOnTaskUuid);
	int numberOfPoints = 0;

	// confirm Unit was initialized 
	Unit* taskUnit = c2simXmlHandler->getUnit(taskeeUuid);
	if (!skipInitialize)
		if (!taskUnit->vrfObjectHasBeenCreated)return;

	// output Task parameters to console
	std::cout << "C2SIMinterface queued taskUuid:" << taskId
		<< " for taskeeUuid:" << taskeeUuid << " name:" << taskUnit->name
		<< " dateTime:" << dateTime << " vrfUuid:" << taskUnit->vrfUuid
		<< " ROE:" << thisTask->ruleOfEngagementCode
		<< "\n";
	if (thisTask->startAfterTaskUuid.length() > 0)
		std::cout << "    starts after end of Task: " << waitOnTaskName << " with UUID:" <<
		thisTask->startAfterTaskUuid << "\n    and delay of:" <<
		thisTask->relativeDelayMs << " ms \n";
	if (thisTask->simulationStartMs > 0)
		std::cout << "    starts after delay of:" << thisTask->simulationStartMs << " ms \n";

	// wait loop until trigerring Task completion 
	long startTicks = thisTask->simulationStartMs + GetTickCount();
	if (thisTask->startAfterTaskUuid.length() > 0) {
		while (!c2simXmlHandler->getTaskIsComplete(taskeeUuid) ||
			c2simXmlHandler->getCurrentTaskUuid(taskeeUuid) != waitOnTaskUuid)
			DtSleep(.1);
	}

	// then wait for simulationStartTime (absolute) or relativeDelayTime
	while (startTicks > (signed)GetTickCount())DtSleep(.1);
	startTicks = thisTask->relativeDelayMs + GetTickCount();
	while (startTicks > (signed)GetTickCount())DtSleep(.1);

	// output route parameters to console
	std::cout << "C2SIMinterface starting taskUuid:" << taskId
		<< " for taskeeUuid:" << taskeeUuid << " name:" << taskUnit->name
		<< " dateTime:" << dateTime << " vrfUuid:" << taskUnit->vrfUuid
		<< " ROE:" << thisTask->ruleOfEngagementCode
		<< "\n";

	// set this Task as current in the Unit
	c2simXmlHandler->setCurrentTaskUuid(taskeeUuid, taskUuid);

	// if there is a Route pick up those points; 
	// otherwise use route points from Task
	PhysicalRoute* physicalRoute = nullptr;
	std::string routeUUID = thisTask->mapGraphicUuid;
	if (thisTask->mapGraphicUuid != "") {
		physicalRoute = c2simXmlHandler->retrieveRoute(routeUUID);
		if (physicalRoute == nullptr) {
			std::cout << "CANNOT EXECUTE TASK DUE TO INVALID MapGraphicID:" << thisTask->mapGraphicUuid;
			return;
		}
		numberOfPoints = physicalRoute->locations.size();
	} else {

		// find smallest count of lat/lon/elev points
		numberOfPoints = thisTask->locationPointCount;
		if (numberOfPoints == 0) {
			c2simXmlHandler->displayError(
				"ERROR - NO LOCATION GIVEN - CAN'T EXECUTE TASK");
			return;
		}
	}

	//  moveAlongRoute from Location
	std::cout << "route point count:" << numberOfPoints
		<< " actionTask:" << thisTask->actionTaskActivityCode << "\n";

	// create list for route points and a name for route
	// move first the route (starts with point 1) then the destination
	DtList routePointList;

	// get the geolocation of executing Unit from the simulation
	boolean success = false;
	double initX, initY, initZ, nextX, nextY, nextZ;
	DtVector64* listData;
	std::string latString, lonString, elAglString;
	success = getUnitGeodeticFromSim(taskUnit, latString, lonString, elAglString);
	if (!success) {
		std::cout << "ABANDONING TASK\n";
		return;
	}

	// start route at present object location
	std::cout << "POINT 0 coordinates:" << latString << "/" << lonString << 
		"/" << elAglString << "\n";
	convertCoordinates(
		latString,//taskUnit->latitude,
		lonString,//taskUnit->longitude,
		elAglString,//taskUnit->elevationAgl,
		initX,
		initY,
		initZ);
	listData = new DtVector(initX, initY, initZ);
	routePointList.add(listData);

	// add Locations from order Task or Route
	for (int pointNumber = 0; pointNumber < numberOfPoints; ++pointNumber) {
		std::cout << "Point " << pointNumber + 1 << " coordinates:";
		if (routeUUID != "") {
			PhysicalLocation* loc = physicalRoute->locations[pointNumber];
			std::cout << loc->latitude << "/" << loc->longitude << "/" << loc->elevation << "\n";
			convertCoordinates(
				loc->latitude,
				loc->longitude,
				loc->elevation,
				nextX,
				nextY,
				nextZ);
		} else {
			std::cout << thisTask->latitudes[pointNumber] << "/" <<
				thisTask->longitudes[pointNumber] << "/" <<
				thisTask->elevations[pointNumber] << "\n";
			convertCoordinates(
				thisTask->latitudes[pointNumber],
				thisTask->longitudes[pointNumber],
				thisTask->elevations[pointNumber],
				nextX,
				nextY,
				nextZ);
		}

		// combine coordinates to a DtVector
		listData = new DtVector(nextX, nextY, nextZ);
		routePointList.add(listData);

	}// end for (int pointNumber

	// if running without initialize phase and this is the first time we
	// have seen this object name, create an object to execute the task
	if (skipInitialize)
		if (!taskUnit->vrfObjectHasBeenCreated) {
			DtVector vec(initX, initY, initZ);
			std::string hostility = "AFR";

			// TODO: units other than ScoutUnit
			// based on DIS Entity type and/or APP-6/SIDC
			//if (taskeeUuid.rfind("INF") == taskeeUuid.length() - 3)
			createScoutUnit(vec, taskeeUuid, hostility);
			//else if (taskeeUuid.rfind("AV") == taskeeUuid.length() - 2)
			//	c2simInterface->createAH64(vec, taskeeUuid, hostility);
			//else
			//	c2simInterface->createTank(vec, taskeeUuid, hostility);
			taskUnit->vrfObjectHasBeenCreated = true;
		}

	// special case for evacuation script
	if (thisTask->actionTaskActivityCode == "EVACTN") {
		runEvacuate(thisTask);
		return;
	}
	/*
		// and EMBARK/DEBARK
		if (thisTask->actionTaskActivityCode == "EMBARK"){
			runEmbark(thisTask);
			return;
		}
		if (thisTask->actionTaskActivityCode == "DEBARK")
		{
			runDebark(thisTask);
			return;
		}
	*/
	// send rules of engagement to the Unit
	DtUUID objectDtuuid(taskUnit->vrfUuid);
	if (thisTask->ruleOfEngagementCode == "ROEFree")
		textIf->controller()->
		setRulesOfEngagement(objectDtuuid, "fire-at-will", DtSimSendToAll);
	else if (thisTask->ruleOfEngagementCode == "ROEHold")
		textIf->controller()->
		setRulesOfEngagement(objectDtuuid, "hold-fire", DtSimSendToAll);
	else
		textIf->controller()->
		setRulesOfEngagement(objectDtuuid, "fire-when-fired-upon", DtSimSendToAll);

	// identify the target to Unit
	textIf->controller()->setTarget(
		DtUUID(taskeeUuid),
		DtUUID(c2simXmlHandler->getAffectedEntity(thisTask))
	);

	// if one point, just move unit there
	if (routePointList.count() == 1)
		textIf->controller()->moveToLocation(
			DtUUID(taskUnit->vrfUuid),
			DtVector64(nextX, nextY, nextZ),
			DtSimSendToAll
		);

	// follow the route if there is one
	else {
		// create the route
		thisTask->taskRouteDtString = "";
		std::string routeName = thisTask->taskName + " ROUTE";
		textIf->controller()->createRoute(
			DtTextInterface::vrfObjectCreatedCb, textIf,
			routePointList, routeName);// taskRouteDtString??
		routePointList.removeAll();

		// wait for the new route to be available
		while (thisTask->taskRouteDtString == "")DtSleep(.1);
		std::cout << "Task:" << thisTask->taskName << " made Route:" << routeName <<
			" for Unit:" << taskUnit->name << " UUID:" << taskUnit->uuid <<
			"| start at:" << latString << "/" << lonString << "/" << elAglString << "\n";
		while (DtUUID(routeName).uuidString() == routeName)DtSleep(.1);

		// run the object along route
		textIf->controller()->moveAlongRoute(objectDtuuid, DtUUID(routeName), DtSimSendToAll);
		std::cout << "Tasked moveAlongRoute for obj " << objectDtuuid.uuidString() << "|" << routeName <<
			" (vrfUuid " << DtUUID(routeName).uuid() << ")\n";
	}

	// wait for route to be completed
	while(!c2simXmlHandler->getTaskRouteIsComplete(taskeeUuid))DtSleep(.1);

	// wait for task completion
	while (!c2simXmlHandler->getTaskIsComplete(taskeeUuid))DtSleep(.1);
	
	// send TaskStatus message
	textIf->sendStatusReport(taskeeUuid, "TASKCMPLT", taskUuid);

}// end executeTask()
