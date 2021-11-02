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
#include "UnitOrderTask.h"
#include <iostream>
#include <string>


UnitOrderTask::UnitOrderTask()
{
	//waypointList = std::vector<GeoPoint>(1);
	//std::cout <<  "vector size = " << waypointList.size() << "\n";
}


UnitOrderTask::~UnitOrderTask()
{
}

bool UnitOrderTask::okUUIDs() {
	return (this->orderUUID.length() == 36 &&
		this->fromSendUUID.length() == 36 &&
		this->toRcvrUUID.length() == 36 );
}

void UnitOrderTask::printAll() {
	std::cout << "performingEntityUUID = " << performingEntityUUID << "\n";
	std::cout << "toRcvrUUID = " << toRcvrUUID << "\n";
	std::cout << "fromSendUUID = " << fromSendUUID << "\n";
	std::cout << "orderUUID = " << orderUUID << "\n";

	std::cout << "issueTime = "<< issueTime << "\n";

	
	std::cout << "timeToEx = " << timeToEx << "\n";
	std::cout << "taskNameCode = " << taskNameCode << "\n";
	std::cout << "desiredEffectCode = " << desiredEffectCode << "\n";


	std::cout << "waypointList\n    ";
	size_t n = waypointList.size();
	for (int i = 0; i < n; i++) {
		std::cout << waypointList[i].latitude << " " << waypointList[i].longitude << "\n ";
	}
		
		


}



GeoPoint::GeoPoint() {
	latitude = 500;
	longitude = 500;
	altitude = 0;
}

GeoPoint::~GeoPoint() {}

bool GeoPoint::isOK() {
	bool ok = -90 <= latitude &&
		latitude <= 90 &&
		-180 <= longitude &&
		longitude <= 180;
	return ok;

}

UnitBehaviors::UnitBehaviors() : tnc2vbsWpType(), tnc2vbsWpBehavr(), dec2vbsWpns() {

	loadValues();
}

UnitBehaviors::~UnitBehaviors() {}

void UnitBehaviors::loadValues() {
// first is the TaskNameCode
	// vbs MOVE
	tnc2vbsWpType["MoveToLocation"] = "MOVE";
	tnc2vbsWpType["ADVANC"] = "MOVE";
	tnc2vbsWpType["AMBUSH"] = "MOVE";
	tnc2vbsWpType["ASSMBL"] = "MOVE";
	tnc2vbsWpType["ATTSPT"] = "MOVE";
	tnc2vbsWpType["BYPASS"] = "MOVE";
	tnc2vbsWpType["CLOSE"] = "MOVE";
	tnc2vbsWpType["CNFPSL"] = "MOVE";
	tnc2vbsWpType["CNRPSL"] = "MOVE";
	tnc2vbsWpType["DISRPT"] = "MOVE";
	tnc2vbsWpType["ENGAGE"] = "MOVE";
	tnc2vbsWpType["ENVLP"] = "MOVE";
	tnc2vbsWpType["HARASS"] = "MOVE";
	tnc2vbsWpType["HASTY"] = "MOVE";
	tnc2vbsWpType["MOVE"] = "MOVE";
	tnc2vbsWpType["PATROL"] = "MOVE";
	tnc2vbsWpType["REFUEL"] = "MOVE";
	tnc2vbsWpType["RESCUE"] = "MOVE";
	tnc2vbsWpType["RESUPL"] = "MOVE";
	tnc2vbsWpType["RLFPLC"] = "MOVE";
	tnc2vbsWpType["THREAT"] = "SUPPORT";


	// vbs HOLD
	tnc2vbsWpType["HoldInPlace"] = "HOLD";
	tnc2vbsWpType["AIRDEF"] = "HOLD";
	tnc2vbsWpType["BLOCK"] = "HOLD";
	tnc2vbsWpType["CAPTUR"] = "HOLD";
	tnc2vbsWpType["CTRBYF"] = "HOLD";
	tnc2vbsWpType["DEFEND"] = "HOLD";
	tnc2vbsWpType["DENY"] = "HOLD";
	tnc2vbsWpType["FIX"] = "HOLD";
	tnc2vbsWpType["HONASP"] = "HOLD"; 
	tnc2vbsWpType["ISOLAT"] = "HOLD";
	tnc2vbsWpType["OCCUPY"] = "HOLD";
	tnc2vbsWpType["RECOVR"] = "HOLD";
	tnc2vbsWpType["RETAIN"] = "HOLD";
	tnc2vbsWpType["SECURE"] = "HOLD";
	tnc2vbsWpType["TURN"] = "HOLD";

	//vbs destroy
	tnc2vbsWpType["ARASLT"] = "DESTROY";
	tnc2vbsWpType["ATTACK"] = "DESTROY";
	tnc2vbsWpType["ATTRIT"] = "DESTROY";
	tnc2vbsWpType["BREACH"] = "DESTROY";
	tnc2vbsWpType["BURN"] = "DESTROY";
	tnc2vbsWpType["CLROBS"] = "DESTROY";
	tnc2vbsWpType["CTRATK"] = "DESRTOY";
	tnc2vbsWpType["CTRFIR"] = "DESTROY";
	tnc2vbsWpType["DESTROY"] = "DESTROY";
	tnc2vbsWpType["DLBATK"] = "DESTROY";
	tnc2vbsWpType["EXPLT"] = "DESTROY";
	tnc2vbsWpType["INTDCT"] = "DESTROY";
	tnc2vbsWpType["PENTRT"] = "DESTROY";
	tnc2vbsWpType["SDAM"] = "DESTROY";

	// vbs retreat
	tnc2vbsWpType["AVOID"] = "RETREAT";
	tnc2vbsWpType["DISENG"] = "RETREAT";
	tnc2vbsWpType["RECONS"] = "RETREAT";

	// vbs SeekAndDestroy
	tnc2vbsWpType["CLRLND"] = "SAD";
	tnc2vbsWpType["DEFEAT"] = "SAD";
	tnc2vbsWpType["MOPUP"] = "SAD";
	tnc2vbsWpType["PURSUE"] = "SAD";
	tnc2vbsWpType["SEIZE"] = "SAD";

	//vbs Support
	tnc2vbsWpType["COVER"] = "SUPPORT";
	tnc2vbsWpType["PREFIR"] = "SUPPORT";
	tnc2vbsWpType["REINF"] = "SUPPORT";
	tnc2vbsWpType["SUPPRS"] = "SUPPORT";
	tnc2vbsWpType["SUPPRT"] = "SUPPORT";

	//vbs sentry
	tnc2vbsWpType["Observe"] = "SENTRY";
	tnc2vbsWpType["DELAY"] = "SENTRY";
	tnc2vbsWpType["DRONL"] = "SENTRY"; // ?
	tnc2vbsWpType["GUARD"] = "SENTRY";
	tnc2vbsWpType["INFLT"] = "SENTRY";
	tnc2vbsWpType["LOCATE"] = "SENTRY";
	tnc2vbsWpType["OBSRV"] = "SENTRY";
	tnc2vbsWpType["PLAN"] = "SENTRY";
	tnc2vbsWpType["RECEE"] = "SENTRY";
	tnc2vbsWpType["SCREEN"] = "SENTRY";

	// vbs load (vehicle)
	tnc2vbsWpType["MEDEVC"] = "LOAD";

	// NOT SURE - SET TO MOVE
	tnc2vbsWpType["CONS"] = "MOVE";
	tnc2vbsWpType["CRESRV"] = "MOVE";
	tnc2vbsWpType["DECEIV"] = "MOVE";
	tnc2vbsWpType["OBSCUR"] = "MOVE";


// now TNC to combat Behaviour
	// vbs MOVE
	tnc2vbsWpBehavr["MoveToLocation"] = "CARELESS";
	tnc2vbsWpBehavr["ADVANC"] = "SAFE";
	tnc2vbsWpBehavr["AMBUSH"] = "SAFE";
	tnc2vbsWpBehavr["ASSMBL"] = "SAFE";
	tnc2vbsWpBehavr["ATTSPT"] = "SAFE";
	tnc2vbsWpBehavr["BYPASS"] = "SAFE";
	tnc2vbsWpBehavr["CLOSE"] = "SAFE";
	tnc2vbsWpBehavr["CNFPSL"] = "SAFE";
	tnc2vbsWpBehavr["CNRPSL"] = "SAFE";
	tnc2vbsWpBehavr["DISRPT"] = "SAFE";
	tnc2vbsWpBehavr["ENGAGE"] = "SAFE";
	tnc2vbsWpBehavr["ENVLP"] = "SAFE";
	tnc2vbsWpBehavr["HARASS"] = "COMBAT";
	tnc2vbsWpBehavr["HASTY"] = "AWARE";
	tnc2vbsWpBehavr["MOVE"] = "CARELESS";
	tnc2vbsWpBehavr["PATROL"] = "AWARE";
	tnc2vbsWpBehavr["REFUEL"] = "SAFE";
	tnc2vbsWpBehavr["RESCUE"] = "SAFE";
	tnc2vbsWpBehavr["RESUPL"] = "SAFE";
	tnc2vbsWpBehavr["RLFPLC"] = "SAFE";
	tnc2vbsWpBehavr["THREAT"] = "SUPPORT";


	// vbs AWARE
	tnc2vbsWpBehavr["HoldInPlace"] = "AWARE";
	tnc2vbsWpBehavr["AIRDEF"] = "AWARE";
	tnc2vbsWpBehavr["BLOCK"] = "AWARE";
	tnc2vbsWpBehavr["CAPTUR"] = "AWARE";
	tnc2vbsWpBehavr["CTRBYF"] = "AWARE";
	tnc2vbsWpBehavr["DEFEND"] = "AWARE";
	tnc2vbsWpBehavr["DENY"] = "AWARE";
	tnc2vbsWpBehavr["FIX"] = "AWARE";
	tnc2vbsWpBehavr["HONASP"] = "AWARE";
	tnc2vbsWpBehavr["ISOLAT"] = "AWARE";
	tnc2vbsWpBehavr["OCCUPY"] = "AWARE";
	tnc2vbsWpBehavr["RECOVR"] = "AWARE";
	tnc2vbsWpBehavr["RETAIN"] = "AWARE";
	tnc2vbsWpBehavr["SECURE"] = "AWARE";
	tnc2vbsWpBehavr["TURN"] = "AWARE";

	//vbs destroy
	tnc2vbsWpBehavr["ARASLT"] = "COMBAT";
	tnc2vbsWpBehavr["ATTACK"] = "COMBAT";
	tnc2vbsWpBehavr["ATTRIT"] = "COMBAT";
	tnc2vbsWpBehavr["BREACH"] = "COMBAT";
	tnc2vbsWpBehavr["BURN"] = "COMBAT";
	tnc2vbsWpBehavr["CLROBS"] = "COMBAT";
	tnc2vbsWpBehavr["CTRATK"] = "DESRTOY";
	tnc2vbsWpBehavr["CTRFIR"] = "COMBAT";
	tnc2vbsWpBehavr["DESTROY"] = "COMBAT";
	tnc2vbsWpBehavr["DLBATK"] = "COMBAT";
	tnc2vbsWpBehavr["EXPLT"] = "COMBAT";
	tnc2vbsWpBehavr["INTDCT"] = "COMBAT";
	tnc2vbsWpBehavr["PENTRT"] = "COMBAT";
	tnc2vbsWpBehavr["SDAM"] = "COMBAT";

	// vbs retreat
	tnc2vbsWpBehavr["AVOID"] = "CARELESS";
	tnc2vbsWpBehavr["DISENG"] = "CARELESS";
	tnc2vbsWpBehavr["RECONS"] = "CARELESS";

	// vbs SeekAndDestroy
	tnc2vbsWpBehavr["CLRLND"] = "COMBAT";
	tnc2vbsWpBehavr["DEFEAT"] = "COMBAT";
	tnc2vbsWpBehavr["MOPUP"] = "COMBAT";
	tnc2vbsWpBehavr["PURSUE"] = "COMBAT";
	tnc2vbsWpBehavr["SEIZE"] = "COMBAT";

	//vbs Support
	tnc2vbsWpBehavr["COVER"] = "COMBAT";
	tnc2vbsWpBehavr["PREFIR"] = "COMBAT";
	tnc2vbsWpBehavr["REINF"] = "COMBAT";
	tnc2vbsWpBehavr["SUPPRS"] = "COMBAT";
	tnc2vbsWpBehavr["SUPPRT"] = "COMBAT";

	//vbs sentry
	tnc2vbsWpBehavr["Observe"] = "AWARE";
	tnc2vbsWpBehavr["DELAY"] = "AWARE";
	tnc2vbsWpBehavr["DRONL"] = "AWARE"; // ?
	tnc2vbsWpBehavr["GUARD"] = "AWARE";
	tnc2vbsWpBehavr["INFLT"] = "STEALTH";
	tnc2vbsWpBehavr["LOCATE"] = "AWARE";
	tnc2vbsWpBehavr["OBSRV"] = "AWARE";
	tnc2vbsWpBehavr["PLAN"] = "AWARE";
	tnc2vbsWpBehavr["RECEE"] = "AWARE";
	tnc2vbsWpBehavr["SCREEN"] = "AWARE";

	// vbs load (vehicle)
	tnc2vbsWpBehavr["MEDEVC"] = "LOAD";

	// NOT SURE - SET TO MOVE
	tnc2vbsWpBehavr["CONS"] = "AWARE";
	tnc2vbsWpBehavr["CRESRV"] = "AWARE";
	tnc2vbsWpBehavr["DECEIV"] = "AWARE";
	tnc2vbsWpBehavr["OBSCUR"] = "AWARE";


// now its is the DesiredEffectCode
	// try to get the weapon hold/free fire from DEC
	//"NO CHANGE" (No change)
	//"BLUE" (Never fire)
	//"GREEN" (Hold fire - defend only)
	//"WHITE" (Hold fire, engage at will)
	//"YELLOW" (Fire at will)
	//"RED" (Fire at will, engage at will)

	dec2vbsWpns["DSTRYK"] = "RED";
	dec2vbsWpns["FKILL"] = "RED";
	dec2vbsWpns["KILL"] = "RED";
	dec2vbsWpns["LOST"] = "RED";
	dec2vbsWpns["MKILL"] = "RED";
	dec2vbsWpns["SDAM"] = "RED";

	dec2vbsWpns["MODDAM"] = "YELLOW";
	dec2vbsWpns["NORSTN"] = "YELLOW";
	dec2vbsWpns["NUTRLD"] = "YELLOW";
	dec2vbsWpns["SUPRSD"] = "YELLOW";

	dec2vbsWpns["INTREC"] = "WHITE";
	dec2vbsWpns["LDAM"] = "WHITE";
	dec2vbsWpns["LGTRST"] = "WHITE";
	dec2vbsWpns["WNDD"] = "WHITE";
	
	dec2vbsWpns["FLIG"] = "GREEN";
	dec2vbsWpns["IDNT"] = "GREEN";
	dec2vbsWpns["ILLUM"] = "GREEN";
	dec2vbsWpns["NBCAS"] = "GREEN";
	dec2vbsWpns["NKN"] = "GREEN";
	dec2vbsWpns["NOS"] = "GREEN";

}

std::string UnitBehaviors::taskNameCodeToVbsWpType(std::string tnc) {
	std::string tp = tnc2vbsWpType[tnc];
	if (tp.empty()) {
		return "MOVE";
	}
	else {
		return tp;
	}

}

std::string UnitBehaviors::taskNameCodeToVbsWpBehaviourType(std::string tnc) {
	std::string beh = tnc2vbsWpBehavr[tnc];
	if (beh.empty()) {
		return "SAFE";
	}
	else {
		return beh;
	}

}
std::string UnitBehaviors::desiredEffectCOdeToVbsWpType(std::string dec) {
	std::string wp = dec2vbsWpns[dec];
	if (dec.empty()) {
		return "GREEN";
	}
	else {
		return wp;
	}
}