/*******************************************************************************
** Copyright (c) 2002 MAK Technologies, Inc.
** All rights reserved.
*******************************************************************************/
/*******************************************************************************
** $RCSfile: main.cxx,v $ $Revision: 1.13 $ $State: Exp $
*******************************************************************************/
//** MODIFIED 1Mar2019 by GMU C4I & Cyber Lab for c2simVRF

// c2simVRFv2.16 updated to use C++ClientLibv4.8.0.3 1May2021

#include <iostream>
#include <cstring>
#include <string>
#include "textIf.h"
#include "remoteControlInit.h"

// VR-Forces headers
#include <vrfMsgTransport/networkEventManager.h>
#include <vrfMsgTransport/vrfMessageInterface.h>
#include <vrfcontrol/vrfRemoteController.h>
#include <vrfMsgTransport/networkEventManager.h>
#include <vrfMsgTransport/vrfTDLMessageInterface.h>
#include <vl/exerciseConn.h>
#include <vlutil/vlProcessControl.h>
#include <vrfmsgs/ifStatus.h>

// path to the VRForces executable directory
std::string vrfBin64 = "C:\\MAK\\vrforces4.9\\bin64\\";

// current C2SIM version
std::string c2simVersion = "1.0.0";

// IP address and port of C2SIM (BML) server
std::string serverAddress = "10.2.10.30";
std::string stompPort = "61613"; // standard server STOMP port
std::string restPort = "8080";   // normal C2SIM REST port

// local configuration defaults
int reportInterval = 0; // in seconds 
std::string clientId = "VRFORCES";
std::string skipInitialize = "0";
std::string useIbmlFlag = "0";
int sendTrackingCode = 0;
int sendObservationsCode = 0;
std::string vrfLocalAddress = "127.0.0.1";
std::string debugFlag = "0";
char* vrfAddr = new char[15];
std::string blueForceName = "";
int sessionId = 1;
std::string applicationNumber = "3201";
char* appNumber = new char[5];
std::string site = "1";
char* siteId = new char[2];

// provides STOMP interface and build VRForces commands
#include "C2SIMinterface.h"

// Thread functionality
#include <thread>

static int timesCalled = 0;

int main(int argc, char** argv)
{
	// extract up to 12 optional command line arguments;
	// can be omitted unless providing one of the later ones:
	// server IP address
	// REST port number
	// STOMP port number
	// clientID name 
	// 1 to skip initialize, 0 otherwise (default 0)
	// 1 to use IBML, 0 otherwise (default 0)
	// 0 to send blue tracking, 1 to send red and blue tracking, 2 to send only red tracking, 3 to send none (default 0)
	// VRForces Local IP Address (defaults to loopback)
	// report generation interval in seconds
	// blue force name for initialization
	// 1 to print debug data, 0 otherwise (default 0)
	// VRForces Session ID (default 1)
	// remote control interface application number (default 3201)
	// VRForces Site ID (default 1)
	// 0 to send blue observations, 1 to send red and blue observations, 2 to send only red observations, 3 to send none (default 0)

	std::cout << 
		"Starting VR-Forces C2SIM Interface v2.16 compatible VR-Forcesv4.9 using C2SIMv" << 
		c2simVersion << "\n"; 

	// server address parameter
	if (argc < 2)
		std::cout << "using default server IP address: " << serverAddress << "\n";
	else {
		serverAddress = argv[1];
		std::cout << "using server address: " << serverAddress << "\n";
	}

	// REST port parameter
	if (argc < 3)
		std::cout << "using default REST port: " << restPort << "\n";
	else {
		restPort = argv[2];
		std::cout << "using REST port: " << restPort << "\n";
	}

	// STOMP port parameter
	if (argc < 4)
		std::cout << "using default STOMP port: " << stompPort << "\n";
	else {
		stompPort = argv[3];
		std::cout << "using STOMP port: " << stompPort << "\n";
	}

	// client ID parameter
	if (argc < 5)
		std::cout << "using default client ID: " << clientId << "\n";
	else {
		clientId = argv[4];
		std::cout << "using client ID: " << clientId << "\n";
	}

	// initialization parameter (skip or not)
	if (argc < 6)
		std::cout << "defaulting to require initialization sequence\n";
	else {
		skipInitialize = argv[5];
		std::cout << "skipping initialization sequence\n";
	}

	// use IBML09 in place of C2SIM parameter 
	if (argc < 7)
		std::cout << "defaulting to C2SIM_SMX_LOXv" << c2simVersion << " schema\n";
	else {
		useIbmlFlag = argv[6];
		if (useIbmlFlag == "1")std::cout << "expecting IBML09 input\n";
		else std::cout << "expecting C2SIMv" << c2simVersion << " input\n";
	}

	// tracking reports parameter
	if (argc < 8)
		std::cout << "defaulting to sending blue and not red tracking reports\n";
	else {
		sendTrackingCode = std::stoi(argv[7]);
		if (sendTrackingCode == 0 || sendTrackingCode == 1)std::cout << "sending blue tracking reports\n";
		else std::cout << "not sending blue tracking reports\n";
		if(sendTrackingCode == 1 || sendTrackingCode == 2)std::cout << "sending red tracking reports\n";
		else std::cout << "not sending red tracking reports\n";
	}

	// address for VRForces parameter
	if (argc < 9)
		std::cout << "VRForces address defaulting to " << vrfLocalAddress << "\n";
	else {
		vrfLocalAddress = argv[8];
		std::cout << "VRForces address set to:" << vrfLocalAddress << "\n";
	}
	std::strcpy(vrfAddr, vrfLocalAddress.c_str());

	// report interval (if reports generated in c2simVRF rather than lua scripts
	if (argc < 10)
		std::cout << "C2SIM report interval defaulting to " << reportInterval << "\n";
	else {
		reportInterval = std::stoi(argv[9]);
		std::cout << "internal position report send time interval:" << reportInterval << " seconds\n";
	}
	if (reportInterval == 0)
		std::cout << "generating VRF-based position and reports\n";

	// blue force name parameter
	if (argc >= 11){
		// zero means command line is not provide blue force name
		// (this is needed if not providing and debugFlag is required)
	    blueForceName = argv[10];
		if (blueForceName != "0") {
			std::cout << "blue force name: " << blueForceName << "\n";
		}
		else blueForceName = "";
		if(blueForceName == "")
			std::cout << "blue force name defaulting to first XML ForceSide entry\n";
	} 

	// print debug data parameter
	if (argc >= 12) { // 1 to dispay debug info
		debugFlag = argv[11];
		if(debugFlag == "1")std::cout << "displaying debug output\n";
	}

	// VRForces Session ID parameter
	if (argc >= 13) { // integer sessionId}
		sessionId = atoi(argv[12]);
		std::cout << "setting Session ID to: " << sessionId << "\n";
	}
	else std::cout << "Session ID defaulting to: " << sessionId;

	// VRForces remote interface application number parameter
	if (argc < 14) // integer in char[2] Application number}
		std::cout << "remote interface application number defaulting to " << applicationNumber << "\n";
	else {
		applicationNumber = argv[13];
		std::cout << "setting remote interface application number to:" << applicationNumber << "\n";
	}
	std::strcpy(appNumber, applicationNumber.c_str());

	// VRForces Site ID parameter
	if (argc < 15) // integer in char[2] Site ID
		std::cout << "VRForces Site ID defaulting to " << site << "\n";
	else {
		site = argv[14];
		std::cout << "setting remote interface site number to:" << site << "\n";
	}
	std::strcpy(siteId, site.c_str());

	// observation reports parameter
	if (argc < 16)
		std::cout << "defaulting to sending blue and not red tracking reports\n";
	else {
		sendObservationsCode = std::stoi(argv[15]);
		if (sendObservationsCode == 0 || sendObservationsCode == 1)std::cout << "sending blue observation reports\n";
		else std::cout << "not sending blue obervation reports\n";
		if (sendObservationsCode == 1 || sendObservationsCode == 2)std::cout << "sending red observation reports\n";
		else std::cout << "not sending red observation reports\n";
	}
	
	// arguments for VRForces command line
#if DtHLA 
	char* fomFile = new char[50];
	std::strcpy(fomFile, (vrfBin64 + "MAK-VRFExt-4_evolved.xml").c_str());
	char* rprFomFile = new char[50];
	std::strcpy(rprFomFile, (vrfBin64 + "RPR_FOM_v2.0_1516-2010.xml").c_str());
	char* vrfArgv[] =
	{ "bin64\\c2simVRFHLA1516e", "--rprFomVersion", "2.0", "--fomModules", fomFile,
		"-a", appNumber, "-s", siteId, "--execName", "MAK-RPR-2.0",
		"--fedFileName", rprFomFile, "-n", "1" };
#else
	char* vrfArgv[] = { "bin64\\c2simVRF", "--disVersion", "7", "--deviceAddress", vrfAddr,
		"--disPort", "3000", "-a", appNumber, "-s", siteId, "-x", "1", "-n", "1" };
#endif
	int argCount = sizeof(vrfArgv)/sizeof(vrfArgv[0]);
	std::cout << "VR-Forces arguments:";
	for (int i = 1; i < argCount; i+=2)std::cout << vrfArgv[i] << " " << vrfArgv[i+1] << "|";
	std::cout << "\n";
	
	// Create initializer object used to provide initialization data to the
	// exercise connection, configured through command line arguments.
	DtRemoteControlInitializer appInitializer(argCount, vrfArgv);
	appInitializer.parseCmdLine();
	
	// Create the controller
	DtVrfRemoteController* controller = new DtVrfRemoteController();
	
	// Create an exercise connection
	DtExerciseConn* exConn;
	try {
		exConn = new DtExerciseConn(appInitializer);
	}
	catch (...) {
		std::cout << "exception starting VRForces Exercise Connection\n";
		DtSleep(10);
		return 1;
	}
	
	// set the session ID
    DtNetworkEventManager* dtEm = new DtNetworkEventManager();
    dtEm->setExerciseConnection(exConn);
    dtEm->setProcessEventsImmediately(true);
    DtVrfMessageInterface* msgIf = new DtVrfMessageInterface(dtEm);
	msgIf->init();
	msgIf->setSessionId(sessionId);
	
	// initialize the controller
	DtVrfTDLMessageInterface* tdl = new DtVrfTDLMessageInterface(dtEm);
	tdl->init();
	controller->init(msgIf, 0, 0, 0, "entity-identifier",
#if DtHLA
		0,
#endif		
		tdl);
	
	// Create our text interface so we can enter remote commands
	DtTextInterface* textIf =
		new DtTextInterface(controller, serverAddress, restPort, clientId,
			useIbmlFlag == "1", sendTrackingCode, reportInterval == 0,
			debugFlag == "1", c2simVersion, sendObservationsCode);
	
	// Create a C2SIM controller to read orders and translate them for VRF
	C2SIMinterface* c2simInterface =
		new C2SIMinterface(textIf, serverAddress, stompPort, restPort, clientId,
			useIbmlFlag == "1", sendTrackingCode, debugFlag == "1", 
			reportInterval, c2simVersion);
	
	// Start a thread to read from STOMP and generate VRForces commands
	std::thread t1(&C2SIMinterface::readStomp, c2simInterface,
		skipInitialize == "1", clientId, reportInterval);
	
	// start a thread to generate reports (must start after objects are created)
	std::thread t3 (&C2SIMinterface::reportGenerator);
	
	/*Processing: read stdin and call drainInput.
		Calling drainInput() ensures that the controller receives
		necessary feedback from VR-Forces backend applications.
	*/
	while (!textIf->timeToQuit())
	{ 
		textIf->readCommand();
		exConn->clock()->setSimTime(exConn->clock()->elapsedRealTime());
		exConn->drainInput();
		DtSleep(0.1);
	}
	
	// shutdown
	t3.join();
	t1.join();
	delete textIf;
	delete controller;
	delete exConn;
   
	return 0;
}

