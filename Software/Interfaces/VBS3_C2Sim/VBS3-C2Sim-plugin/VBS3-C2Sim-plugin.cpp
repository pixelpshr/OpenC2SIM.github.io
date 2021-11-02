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

// VBS3-C2Sim-plugin.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"
#include <sstream>
#include <fstream>
#include <chrono>
#include <vector>
#include "Util.h"
#include "SimEntity.h"
#include "PosReportXml.h"
#include "IOThread.h"

// Command function declaration
typedef int (WINAPI * ExecuteCommandType)(const char *command, char *result, int resultLength);

// Command function definition
ExecuteCommandType vbsExecuteCommand = NULL;


const size_t BS = 512;
char vbsCmd[BS];
char vbsResp[BS];

bool pluginStartCallBack = false;
bool simRunning = false;
bool isVBSPluginInitialised = false;
bool recvC2SimInit = false;
bool completeInitPositions = false;
//std::set<std::string> entitySet;
std::map<std::string, SimEntity> entityMap = std::map<std::string, SimEntity>();
std::queue<UnitOrderTask> unitOrderQueue = std::queue<UnitOrderTask>();
std::vector<std::string> blueList = std::vector<std::string>();
std::vector<std::string> redList = std::vector<std::string>();
std::vector<std::string> greenList = std::vector<std::string>();
std::vector<std::string> resistanceList = std::vector<std::string>();

std::string currentVeh = "<BLANK>";

std::ofstream outFile;
IOThread* ioThread = NULL;
XmlParser* xmlparser = NULL;
InitializeC2Sim* c2sInit = NULL;

// properties that can be set in the init.sqf
std::string c2simSvrAddr = "NOT-SET";
std::string c2simSysName = "VBS3";
bool useC2SNamespace = false;
int minSendTime = 5000;

//std::vector<std::string> hintLines;
std::string hintLines = "";
int hintLength = 0;

enum ReportingSide { REPORT_BLUE, REPORT_RED, REPORT_GREEN, REPORT_RESISTANCE, REPORT_ALL};
ReportingSide rptSide = REPORT_ALL;


void initializeVBSPlugin() {
	
	if (c2simSvrAddr.compare("NOT-SET") == 0) {
		return;
	}


	hintLines.append("Parameters set in init.sqf \\n");
	hintLines.append("C2SIM-SERVER = ").append(c2simSvrAddr).append(" \\n");
	hintLines.append("C2SIM-SYSNAME = ").append(c2simSysName).append(" \\n");
	hintLines.append("C2SIM-MinT = ").append(std::to_string(minSendTime)).append(" \\n");
	hintLines.append("C2SIM-UseNameSpace = ").append((useC2SNamespace ? "true" : "false")).append(" \\n");


	// only create these if this is the first time runnung because 
	// want to keep stomp connection open all the time

	if (ioThread == NULL) {
		ioThread = new IOThread();
		xmlparser = new  XmlParser();
		c2sInit = new InitializeC2Sim();

		outFile.open("dta-c2sim.log", std::ofstream::trunc);
		outFile << "Opened log file\n";
		outFile << "C2Sim-Server = " << c2simSvrAddr << "\n";
	}

	xmlparser->setUnitOrderQueue(&unitOrderQueue);
	xmlparser->setC2SimInitMsg(c2sInit);
	xmlparser->setVBSSystemName(c2simSysName);

	outFile << "Set BML Server params:\n C2Sim-server= " << c2simSvrAddr
		<< "\n C2SIM-SYSNAME= " << c2simSysName
		<< "\n C2SIM-UseNameSpace= " << useC2SNamespace
		<< "\n C2SIM-MinT= " << minSendTime
		<< "\n INITIALISING... \n";

	ioThread->setBmlProtocol("C2SIM");
	ioThread->setBmlServer(c2simSvrAddr);
	ioThread->setBmlSubmitter(c2simSysName);  //"DTA-VBS3-C2Sim-Plugin");
	ioThread->setUseC2SNameSpace(useC2SNamespace);

	if (ioThread->isRestThreadRunning() == false) {
		ioThread->setStayAliveRest();
		ioThread->startRestClientThread(xmlparser);
	}
	if(ioThread->isStompThreadRunning() == false) {
		ioThread->setStayAliveStomp();
		ioThread->startStompClientThread(xmlparser);

	}
	
	isVBSPluginInitialised = true;

	
	outFile.flush();
}

void resetBooleans() {
	simRunning = false;
	pluginStartCallBack = false;
	isVBSPluginInitialised = false;
	recvC2SimInit = false;
	completeInitPositions = false;
}


void resetForAnotherMission() {
	ioThread->stopRest();

	delete c2sInit;
	c2sInit = new InitializeC2Sim();
	xmlparser->setC2SimInitMsg(c2sInit);
	entityMap.clear();
	
	currentVeh = "<BLANK>";

	outFile << "Resetting VBS c2sim plugin but kep stomp open\n";

}

void shutdown() {
	
	
	entityMap.clear();

	if (ioThread != NULL) {
		ioThread->stopStomp();
		delete ioThread;
	}
	if (xmlparser != NULL) {
		delete xmlparser;
	}
	if (c2sInit != NULL) {
		delete c2sInit;
	}

	ioThread = NULL;
	xmlparser = NULL;
	c2sInit = NULL;

	c2simSvrAddr = "NOT-SET";

	if (outFile.is_open()) {

		outFile << "Stopping C2-Sim plugin\n";
		outFile.close();
	}
}



// check if the sim is running
bool isSimRunning() {
	

	vbsExecuteCommand("isSimulationEnabled", vbsResp, BS);

	if (std::strcmp(vbsResp, "false") == 0) {
		if (simRunning ) {
			resetBooleans();

			//shutdown();
			// not a full shutdown because i want to keep stomp connection open
			resetForAnotherMission();
		}
		return false;
	}
	else {

		simRunning = true;
		return true;
	}
}

void addToSideList(std::string side, std::string vehName) {
	if (side.compare("west") == 0) {
		blueList.emplace_back(vehName);
	}
	else 	if (side.compare("east") == 0) {
		redList.emplace_back(vehName);
	}
	else if (side.compare("civilian") == 0) {
		greenList.emplace_back(vehName);
	} 
	else 	if (side.compare("resistance") == 0) {
		resistanceList.emplace_back(vehName);
	}
}


// this is called by getC2SInitVehicles
void getSimulatedVehicles() {
	
	// first count vehicles
	vbsExecuteCommand("count (allVehicles)", vbsResp, BS);
	
	int nVeh = std::stoi(vbsResp);

	// at this point entityMap is populated with all entities from the initialize
	if (entityMap.size() < nVeh) {
		vbsExecuteCommand("allVehicles", vbsResp, BS);

		outFile << "cmd= count (allVehicles)\nresp= " << vbsResp << "\n";
		
		std::string ans(vbsResp);
		ans.erase(ans.find(']'), 1);
		ans.erase(ans.find('['), 1);
		std::istringstream ss(ans);
		std::string vName;

		int numGood = 0;

		while (std::getline(ss, vName, ',')) {

			auto seItr = entityMap.find(vName);			
			if(seItr == entityMap.end()) { // map.end() means it is not found

				outFile << "WARN: VBS Scenario has '" << vName << "' but C2SimInitfile does not\n";
				hintLines.append(vName).append(" in VBS mission, but not in C2Sim Initialize\\n");
			}
			else {
				
				SimEntity& se = seItr->second;
				se.setRptTime(Util::sysTimeMillis());
				se.setTime(Util::sysTimeMillis());
				outFile << "VBS is simulating '" << vName << "' C2SimInit UUID = " << se.entityUUID << "\n";
				hintLines.append(vName).append(" in VBS mission, C2Sim UUID = ").append(se.entityUUID).append("\\n");
				numGood++;
				sprintf_s(vbsCmd, BS, "side %s;", vName.c_str());
				vbsExecuteCommand(vbsCmd, vbsResp, BS);
				se.setSideVBS(vbsResp);
				addToSideList(se.sideVBS, vName);


			}
		}
		
		if (numGood != c2sInit->getAllEntityUUIDs()->size()) {
			hintLines.append("numGood != AllEntities.size(\\n)");
		}


		outFile.flush();
	}
	
}


void getC2sInitVehicles() {

	

	// looks for if the StompLib has received the C2Sim Initialise with the list of entities
	if (c2sInit->isC2SimInitFilled() && c2sInit->isC2SimInitComplete()) {
		std::mutex* theMutex = c2sInit->getTheMutex();
		std::lock_guard<std::mutex> theLockGuard(*theMutex);

		std::vector<std::string>* eUUIDs = c2sInit->getAllEntityUUIDs();
		outFile << "C2SimInit contained " + eUUIDs->size() << "Entities\n";

		for (int i = 0; i < eUUIDs->size(); i++) {
			std::string uuid = (*eUUIDs)[i];
			if (c2sInit->contains(uuid)) {
				SimEntity& c2sVeh = c2sInit->getEnitiy(uuid);
				SimEntity vbsVeh = SimEntity(c2sVeh);
				outFile << i << ") " << uuid << " " << c2sVeh.name << " " << c2sVeh.app6Code << "\n";
				vbsVeh.setTime(0);
				entityMap[vbsVeh.name] = vbsVeh;
			}
			else {
				outFile << i << ") " << uuid << " not found!\n";
			}
		}

		recvC2SimInit = true;

	}

	if (recvC2SimInit) {
		// now check VBS to see what is actually in the mission
		getSimulatedVehicles();
	}
	
}

void setInitialVehiclePosition() {
	bool anyChanged = false;

	if (recvC2SimInit) {		

		// move the entities to the right place in the scenario
		auto itr = entityMap.begin();
		while (itr != entityMap.end()) {

			SimEntity& se = itr->second;
			if (!se.isPositionInitialized()) {
				double lat, lon, alt;
				se.getPosition(lat, lon, alt);
				std::string vName = itr->second.name;
				std::string lls = Util::latLonString(lat, lon, true);


				sprintf_s(vbsCmd, BS, "%s setPos (coordToPos [ %s , \"LL\"] )", vName.c_str(), lls.c_str());
				outFile << "Setting inital position: " << vbsCmd << "\n";
				vbsExecuteCommand(vbsCmd, vbsResp, BS);
				outFile << "Response: " << vbsResp << "\n";

				hintLines.append("Setting initial position of ").append(vName).append("\\n");
				//sprintf_s(vbsCmd, BS, "hint format [\" Setting initial position of %s \"]; ", vName.c_str());
				//vbsExecuteCommand(vbsCmd, vbsResp, BS);
				//outFile << "HINT: " << vbsCmd << "Resp:" << vbsResp << "\n";

				se.positionInitialized();
				anyChanged = true;
				// only setPos one each VBS frame
				break;
			}
			else { 
				itr++;
			}
		}
	}

	if (anyChanged == false) {

		//then i have to flag that it is done;
		completeInitPositions = true;
	}

}

void checkOneVehPosition() {
	if (currentVeh.compare("<BLANK>") == 0 || currentVeh.compare("<null>")==0) {
		return;
	}

	size_t ems = entityMap.size();
	//outFile << "map size=" << ems << "\n";

	SimEntity& veh = entityMap[currentVeh];
	
	sprintf_s(vbsCmd, BS, "posToCoord [(getPos %s), 'LL']", veh.name.c_str());
	vbsExecuteCommand(vbsCmd, vbsResp, BS);
	double lat, lon;
	if (std::strstr(vbsResp, "error") != NULL) {
		return;
	}
	Util::vbsParseLatLonArray(vbsResp, &lat, &lon);

	double posASL[3];
	sprintf_s(vbsCmd, BS, "getPosASL2 %s", veh.name.c_str());
	vbsExecuteCommand(vbsCmd, vbsResp, BS);
	if (std::strstr(vbsResp, "error") != NULL) {
		return;
	}

	Util::vbsArrayToDoubles(vbsResp, posASL, 3);


	bool posChange = veh.setPosition(lat, lon, posASL[2]);

		
	

	sprintf_s(vbsCmd, BS, "getDammage %s", veh.name.c_str());
	vbsExecuteCommand(vbsCmd, vbsResp, BS);
	if (std::strstr(vbsResp, "error") != NULL) {
		return;
	}
	std::string dammage = vbsResp;
	float d = std::stof(dammage);
	int h = (int)(100 * (1.0f - d));
	
	bool healthChange = (veh.getStrength() == h);
	veh.setStrength(h);

	if (posChange || healthChange) {

		veh.setTime(Util::sysTimeMillis());
	}
}

void sendXmlIfNeeded() {
	
	SimEntity& se = entityMap[currentVeh];
	long long c = se.getRptAge();
	
	if (c >= minSendTime) {
		double lat, lon, alt;
		se.getPosition(lat, lon, alt);
	
		
		//outFile << "sending xml pos report for" << se.name << "\n";
		se.setRptTime(Util::sysTimeMillis());
		ioThread->sendPosReport(se);
		
	}
	else {
		//outFile << "not written xml: " << se.getName() << " t=" << c << "\n";
	}

}




void checkKnowledge() {
	if (rptSide == REPORT_ALL) {
		return; // no need because everyone will self-report
	}

	float knowledge;
	float visibility;
	SimEntity& se = entityMap[currentVeh];
	
	for (int i = 0; i < redList.size(); i++) {
		
		// don't observe myself
		if (currentVeh.compare(redList[i]) == 0) {
			continue;
		}

		knowledge = 0;
		visibility = 0;

		sprintf_s(vbsCmd, BS, "(commander %s) knowsAbout %s", currentVeh.c_str(), redList[i].c_str());
		vbsExecuteCommand(vbsCmd, vbsResp, BS);
		// should be number between 0 and 4
		knowledge = std::stof(vbsResp);

		// over 2.5 vbs reckons you can classifiy enemy 4.0 is fully identified
		if (knowledge > 2.5f) {
			sprintf_s(vbsCmd, BS, "%s getVisibility %s", currentVeh.c_str(), redList[i].c_str());
			vbsExecuteCommand(vbsCmd, vbsResp, BS);
			// should be number between 0 and 1
			visibility = std::stof(vbsResp);
			if (visibility > 0.4) {
				se.addPerceivedEntity(entityMap[redList[i]], knowledge, visibility, Util::sysTimeMillis());
			}
		}

	}


}

void nextVehicle()
{
	// now set CurrentVeh to the next one
	auto itr = entityMap.find(currentVeh);
	itr++;
	if (itr == entityMap.end()) {
		itr = entityMap.begin();
	}
	currentVeh = itr->first;


}



void checkForOrders() {
	size_t numOrders = unitOrderQueue.size();
	bool orderConsumed = false;

	if (numOrders > 0) {
		UnitOrderTask& uot = unitOrderQueue.front();
		
		std::string entName = c2sInit->getEntityName(uot.performingEntityUUID);

		if(entName.compare(c2sInit->noValue) != 0 ) { // this checks that it is not <null>
			SimEntity& se = entityMap[entName];

			if (entName.compare(se.name) == 0) {  // make sure indeed get the right entity
				
				outFile << "Order for Entity " << se.name << " " << se.entityUUID << "\n";

				// check if there were waypoints already there;
				sprintf_s(vbsCmd, BS, "nWaypoints (group %s);", se.name.c_str());
				vbsExecuteCommand(vbsCmd, vbsResp, BS);
				int nwp = std::stoi(vbsResp);
				if (nwp > 1) {
					// send a deleteWaypoint in a while loop (works better than foreach
					sprintf_s(vbsCmd, BS, "_i = %d; while {_i >= 0} do {deleteWaypoint [(group %s), _i]; _i = _i - 1; };", 
						(nwp-1), se.name.c_str());
					vbsExecuteCommand(vbsCmd, vbsResp, BS);
					sprintf_s(vbsCmd, BS, "[(group %s), 0] setWPPos (getPos %s);", se.name.c_str(), se.name.c_str());
					vbsExecuteCommand(vbsCmd, vbsResp, BS);
//					vbsExecuteCommand("hint \" deleted old waypoints\"; ", vbsResp, BS);
					hintLines.append("deleted old waypoints for ").append(se.name).append("\\n");
					orderConsumed = false;
					outFile << " First delete old Waypoints\n";
				} 
				else { // only one WayPoint so add new ones from the order
					outFile << " Convert Waypoints to VBS coords\n";

					// convert LatLon WPs to vbs coords
					size_t nwp = uot.waypointList.size();
					std::vector<std::string> wpStrs(nwp);

					for (int i = 0; i < nwp; i++) {
						GeoPoint& gp = uot.waypointList[i];


						std::string lls = Util::latLonString(gp.latitude, gp.longitude, true);

						sprintf_s(vbsCmd, BS, "coordToPos [ %s , \"LL\"]", lls.c_str());
						vbsExecuteCommand(vbsCmd, vbsResp, BS);
						std::string poss = vbsResp;
						wpStrs[i] = poss;

						outFile << "  WP(" << i << ") = " << lls << " -> " << poss << "\n";

					}


					// get group for unit
					//std::string vbsGroup;
					//sprintf_s(vbsCmd, BS, "_wp = (group %s) ", se.name.c_str());
					//vbsExecuteCommand(vbsCmd, vbsResp, BS);
					//vbsGroup = std::string(vbsResp);

					// loop through waypoints and add to group

					if (se.isWatercraft()) {
						sprintf_s(vbsCmd, BS, "(driver %s) disableAI \"PATHPLAN\";", entName.c_str());
						vbsExecuteCommand(vbsCmd, vbsResp, BS);
						outFile << vbsCmd << " WaterCraft = true\n";
					}

					//std::string wptp = uot.vbsWpType;

					for (int i = 0; i < nwp; i++) {
						//if (se.isAircraft() && (i == (nwp - 1))) {
						//	wptp = "LOITER";
						//}

						sprintf_s(vbsCmd, BS,   // name, LL str, uot.Wptp,uot.WpBeh, uot.WpCbt
							"_wp = (group %s) addWaypoint [ %s , 0]; "
							"_wp setWaypointType \"%s\"; "		
							"_wp setWaypointSpeed \"NORMAL\"; "	
							"_wp setWaypointBehaviour \"%s\"; "
							"_wp setWaypointCombatMode \"%s\"; ",		
							
							se.name.c_str(), wpStrs[i].c_str(),
							uot.vbsWpType.c_str(), uot.vbsWpBehav.c_str(), uot.vbsWpCombat.c_str() );


						outFile << "cmd = " << vbsCmd << "\n";
						vbsExecuteCommand(vbsCmd, vbsResp, BS);
						outFile << "resp= " << vbsResp << "\n";

						if (se.isAircraft()) {
							sprintf_s(vbsCmd, BS, "_wp setWaypointStatements [\"true\", \" %s flyInHeight %d \" ];", entName.c_str(), (int) uot.waypointList[i].altitude);
							vbsExecuteCommand(vbsCmd, vbsResp, BS);
						}

					}
					orderConsumed = true;


					hintLines.append("Order issued to ").append(se.name).append("\\n");
					hintLines.append(" nWPs = ").append(std::to_string(nwp)).append("\\n");
					hintLines.append(" WP Type = ").append(uot.vbsWpType).append("\\n");
					//sprintf_s(vbsCmd, BS, "hint format [\"Order issued to %s \\n nWPs = %zu \\n WP Type = %s \"]; ",
					//	se.name.c_str(), nwp, uot.vbsWpType.c_str());
					//vbsExecuteCommand(vbsCmd, vbsResp, BS);
					//outFile << "HINT: " << vbsCmd << "Resp:" << vbsResp << "\n";
				}

			}
			else {
				outFile << "PerfEntity UUID = " << uot.performingEntityUUID << ", but name = " + entName << "\n";
			}
			outFile.flush();
		}
		else { //
			// On MSG-145 miniEx I was processing orders for other sims.  but not pop()ing bad orders off the queue
			unitOrderQueue.pop();

		}
	
		if (orderConsumed) { // only pop if it has been consumed
			unitOrderQueue.pop();
		}

	} // end if (no > 0) 
}




// Function that will register the ExecuteCommand function of the engine
VBSPLUGIN_EXPORT void WINAPI RegisterCommandFnc(void *executeCommandFnc)
{
	vbsExecuteCommand = (ExecuteCommandType)executeCommandFnc;
}

SessionStateCodeType ssct = UNINITIALIZED;
// This function will be executed every simulation step (every frame) and took a part in the simulation procedure.
// We can be sure in this function the ExecuteCommand registering was already done.
// deltaT is time in seconds since the last simulation step
VBSPLUGIN_EXPORT void WINAPI OnSimulationStep(float deltaT)
{
	// first check to see that plugin has been activated by script call
	if (!pluginStartCallBack) {
		return;
	}

	if (!isSimRunning()) {
		return;
	}
	
	if (hintLines.length() > 0) {
		if (hintLines.length() != hintLength) {
			sprintf_s(vbsCmd, BS, "hint \"%s\"", hintLines.c_str());
			vbsExecuteCommand(vbsCmd, vbsResp, BS);
			hintLength = hintLines.length();
		}
		else if (hintLines.length() == hintLength) {
			hintLines = "";
			hintLength = 0;
		}
	}

	if (!isVBSPluginInitialised) {
		initializeVBSPlugin();
	}

	if (ioThread->isError()) {
		hintLines = ioThread->getErrorMessage();
		return;
	}

	SessionStateCodeType ssct2 = xmlparser->getSessionStateCodeType();
	if (ssct2 != ssct) {
		outFile << "SessionStateCodeType changed: " << ssct << " -> " << ssct2 << "\n";
		ssct = ssct2;
	}

	
	//if (xmlparser->getSessionStateCodeType() == UNINITIALIZED ||
	//	xmlparser->getSessionStateCodeType() == INITIALIZING) {
	//	return;
	//}

	//if (xmlparser->getSessionStateCodeType() == INITIALIZED) {
	//	if (!recvC2SimInit) {
	//		getC2sInitVehicles();
	//	} 
	//	else if(!completeInitPositions) {
	//		setInitialVehiclePosition();
	//	}
	//}

	if (!recvC2SimInit) {
		getC2sInitVehicles();
	}
	else if (!completeInitPositions) {
		setInitialVehiclePosition();
	}
	else {
	//if (xmlparser->getSessionStateCodeType() == RUNNING) {
	
		checkOneVehPosition();

		sendXmlIfNeeded();

		checkKnowledge();

		nextVehicle();

		checkForOrders();
	}
	
}

// This function will be executed every time the script in the engine calls the script function "pluginFunction"
// We can be sure in this function the ExecuteCommand registering was already done.
// Note that the plugin takes responsibility for allocating and deleting the returned string
char retbuf[64];
VBSPLUGIN_EXPORT const char* WINAPI PluginFunction(const char *input)
{	
	std::string in(input);
	
	if (in.compare("C2SIM-INIT") == 0) {
		pluginStartCallBack = true;
		strcpy_s(retbuf, 64, "C2SIM-Plugin Initialised");
	}
	else if(in.compare(0, 12, "C2SIM-SERVER") ==0) {
		size_t ieq = in.find_first_of('=', 0);
		if (ieq != std::string::npos) {			
			c2simSvrAddr = in.substr(ieq + 1);
			
			std::string rt = "Address " + c2simSvrAddr;
			strcpy_s(retbuf, 64, rt.c_str());

		}
	}

	else if (in.compare(0,18, "C2SIM-UseNameSpace") == 0) {
		size_t ieq = in.find_first_of('=', 0);
		if (ieq != std::string::npos) {
			std::string enabled = in.substr(ieq + 1);
			if (enabled.find("true") != std::string::npos) {
				useC2SNamespace = true;
				strcpy_s(retbuf, 64, "nsTRUE");
				
			}
			else {
				useC2SNamespace = false;
				strcpy_s(retbuf, 64, "nsFALSE");
			}
			
		}
	}
	else if (in.compare(0, 10, "C2SIM-MinT") == 0) {
		size_t ieq = in.find_first_of('=', 0);
		if (ieq != std::string::npos) {
			std::string sMinTime = in.substr(ieq + 1);
			minSendTime = std::stoi(sMinTime);
		}
	}
	else if(in.compare(0, 13, "C2SIM-SYSNAME") == 0) {
		size_t ieq = in.find_first_of('=', 0);
		if (ieq != std::string::npos) {
			c2simSysName = in.substr(ieq + 1);			
		}
	}

	else if (in.compare(0, 16, "C2SIM-REPORTSIDE") == 0) {
		size_t ieq = in.find_first_of('=', 0);
		if (ieq != std::string::npos) {
			std::string side= in.substr(ieq + 1);
			if (side.compare("ALL") == 0) {
				rptSide = REPORT_ALL;
			}
			else if (side.compare("BLUE") == 0) {
				rptSide = REPORT_BLUE;
			}
			else if (side.compare("RED") == 0) {
				rptSide = REPORT_RED;
			}
			else if (side.compare("GREEN") == 0) {
				rptSide = REPORT_RED;
			}
			else if (side.compare("RESISTANCE") == 0) {
				rptSide = REPORT_RESISTANCE;
			}
			else {
				rptSide = REPORT_ALL;
			}
		}
	}
	return retbuf;

}


BOOL APIENTRY DllMain(HMODULE hModule,
	DWORD  ul_reason_for_call,
	LPVOID lpReserved
)
{
	switch (ul_reason_for_call)
	{
	case DLL_PROCESS_ATTACH:
		break;
	case DLL_THREAD_ATTACH:
		break;
	case DLL_THREAD_DETACH:
		break;
	case DLL_PROCESS_DETACH:
		shutdown();
		break;
	}
	return TRUE;
}

