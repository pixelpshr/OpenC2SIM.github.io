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
//#include "stdafx.h"
#include <vector>



class GeoPoint
{
public:

	GeoPoint();
	~GeoPoint();
	double latitude;
	double longitude;
	double altitude;

	bool isOK();
};


class UnitOrderTask
{
public:
	UnitOrderTask();
	~UnitOrderTask();

	std::string performingEntityUUID;
	std::string toRcvrUUID;
	std::string fromSendUUID;
	std::string orderUUID;

	std::string mapGraphicID;

	std::string issueTime;

	std::vector<GeoPoint> waypointList;
	long long timeToEx;
	std::string taskNameCode;
	std::string desiredEffectCode;
	std::string nameOfTask;
	std::string vbsWpType;
	std::string vbsWpCombat;
	std::string vbsWpBehav;
	void printAll();
	bool okUUIDs();



};

class UnitBehaviors {
private:
	std::map<std::string, std::string> tnc2vbsWpType;
	std::map<std::string, std::string> tnc2vbsWpBehavr;
	std::map<std::string, std::string> dec2vbsWpns;	
	void loadValues();
public:
	UnitBehaviors();
	~UnitBehaviors();
	std::string taskNameCodeToVbsWpType(std::string tnc);	
	std::string taskNameCodeToVbsWpBehaviourType(std::string tnc);
	std::string desiredEffectCOdeToVbsWpType(std::string tnc);


};

