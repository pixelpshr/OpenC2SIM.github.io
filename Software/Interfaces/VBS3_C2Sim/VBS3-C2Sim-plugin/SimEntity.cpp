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
#include "SimEntity.h"
#include "Util.h"


const double oneMetreThres = 1e-5; 
const double tenMetreThres = 1e-4;
//const long thirySecThres = 30000;



SimEntity::SimEntity() : pMap()
{
	lat = 0;
	lon = 0;
	alt = 0;


	tRept = 55;
	tPos = 66;
	strength = 100;
}


SimEntity::~SimEntity()
{
}

//void SimEntity::setTime(std::chrono::system_clock::time_point t) {
//	this->tPos = t;
//}
void SimEntity::setTime(long long tms) {
	this->tPos = tms;
}


//void SimEntity::setRptTime(std::chrono::system_clock::time_point rt) {
//	this->tRept = rt;
//}
void SimEntity::setRptTime(long long rtms) {
	this->tRept = rtms;
}


//std::chrono::system_clock::time_point SimEntity::getTime() {
//	return this->tPos;
//}
long long SimEntity::getTime() {
	return this->tPos;
}

//std::chrono::seconds SimEntity::getRptAge() {
//	auto sec = std::chrono::duration_cast<std::chrono::seconds>(tPos - tRept);
//	return sec;
//}
unsigned long long SimEntity::getRptAge() {
	unsigned long long deltams = tPos - tRept;
	return deltams;
}


void SimEntity::setSide(std::string uuidSide) {
	this->sideUUID = uuidSide;
}
// probably east, west, civ...
void SimEntity::setSideVBS(std::string vbsSide) {
	this->sideVBS = vbsSide;
}

bool SimEntity::setPosition(double latitude, double longitude, double altitude){
	double dLat = std::abs(latitude - lat);
	double dLon = std::abs(longitude - lon);

	if (dLat > oneMetreThres || dLon > oneMetreThres) {
		lat = latitude;
		lon = longitude;
		alt = altitude;
		return true;
	}
	return false;

}

void SimEntity::getPosition(double& latitude, double& longitude, double& altitude) {
	latitude = lat;
	longitude = lon;
	altitude = alt;

}

double SimEntity::getLatitude() {
	return lat;
}

double SimEntity::getLongitude() {
	return lon;
}

double SimEntity::getAltitude() {
	return alt;
}





void SimEntity::setStrength(int s) {
	this->strength = s;
}
int SimEntity::getStrength() {
	return this->strength;
}

std::string SimEntity::getOprStatus() {
	if (strength > 75) {
		return "FullyOperational";
	}
	else if (strength > 50) {
		return "MostlyOperational";
	}
	else if (strength > 25) {
		return "PartlyOperational";
	}
	else {
		return "NotOperational";
	}
}

void SimEntity::setOprStatus(std::string opStat) {
	if (opStat.compare("FullyOperational") == 0) {
		strength = 100;
	}
	else if (opStat.compare("MostlyOperational") == 0) {
		strength = 75;
	} 
	else 	if (opStat.compare("PartlyOperational") == 0) {
		strength = 50;
	}
	else 	if (opStat.compare("NotOperational") == 0) {
		strength = 20;
	}
	else 	{
		// nothing
	}


}

void SimEntity::positionInitialized() {
	this->initPosSet = true;
}
bool SimEntity::isPositionInitialized() {
	return this->initPosSet;
}

bool SimEntity::isAircraft() {
	bool dis = false;
	bool app6 = false;
	if (!disType.empty()) {
		dis = disType.find("1.2") == 0;
	}
	if (app6Code.length() > 2) {
		app6 = app6Code.at(2) == 'A';
	}
	return dis || app6;;
}

bool SimEntity::isWatercraft() {
	bool dis = false;
	bool app6 = false;
	if (!disType.empty()) {
		dis = disType.find("1.3") == 0;
	}
	if (app6Code.length() > 2) {
		app6 = app6Code.at(2) == 'S';
	}
	return dis || app6;;
}


void SimEntity::addPerceivedEntity(SimEntity& prcvEnt, float knowledge, float visibility, long long obsTime) {
	/*

	auto pItr = pMap.find(prcvEnt.name);
	if (pItr == pMap.end()) {
		Perception p = Perception();
		p.prcvEntity = &prcvEnt;
		p.knowledge = knowledge;
		p.visibility = visibility;
		p.tObs = obsTime;
		p.tRept = 0;

		pMap[prcvEnt.name] = p;
	}
	else {
		Perception& p = pMap[prcvEnt.name];		
		p.knowledge = knowledge;
		p.visibility = visibility;
		p.tObs = obsTime;
	}
	*/
}

Perception* SimEntity::getPerceivedEntity() {
	Perception* prcvEntToReport = NULL;

	auto pItr = pMap.begin();
	while (pItr != pMap.end()) {
		Perception& p = pItr->second;
		if (p.tObs > (tRept + 10000)) {
			p.tRept = Util::sysTimeMillis();
			prcvEntToReport = &p;
			return prcvEntToReport;
		}
	}

}