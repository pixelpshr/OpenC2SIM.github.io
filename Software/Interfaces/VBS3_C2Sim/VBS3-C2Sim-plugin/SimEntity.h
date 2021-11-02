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
#include "stdafx.h"
#include "Perception.h"
#include <map>

class SimEntity
{
private:	

	std::string posStr;
	double lat, lon, alt;
	bool initPosSet = false;
	//std::chrono::system_clock::time_point tPos;
	//std::chrono::system_clock::time_point tRept;
	long long tPos;
	long long tRept;
	int strength;

	std::map<std::string, Perception> pMap;
	


public:
	SimEntity();
	~SimEntity();

	std::string name;
	std::string entityUUID;
	//std::string toRcvrUUID;
	//std::string fromSendUUID;  // from is same as EntityUUID
	std::string superiorUUID; // use this as ToReceiver
	std::string sideUUID;
	std::string app6Code;
	std::string disType;
	std::string sideVBS;

	//void setTime(std::chrono::system_clock::time_point t);
	//void setRptTime(std::chrono::system_clock::time_point rt);
	//std::chrono::system_clock::time_point getTime();
	//std::chrono::seconds getRptAge();
	void setTime(long long sTimeMillis);
	void setRptTime(long long sTimeMillis);
	long long getTime();
	unsigned long long getRptAge();


	void setSide(std::string side);
	void setSideVBS(std::string);
	
	bool setPosition(double lat, double lon, double alt);
	void getPosition(double& lat, double& lon, double& alt);
	double getLatitude();
	double getLongitude();
	double getAltitude();


	void setStrength(int h);
	int getStrength();

	std::string getOprStatus();
	void setOprStatus(std::string opStat);

	void positionInitialized();
	bool isPositionInitialized();
	bool isAircraft();
	bool isWatercraft();

	void addPerceivedEntity(SimEntity& prcvEnt, float knowledge, float visibility, long long obsTime);
	Perception* getPerceivedEntity();

};

