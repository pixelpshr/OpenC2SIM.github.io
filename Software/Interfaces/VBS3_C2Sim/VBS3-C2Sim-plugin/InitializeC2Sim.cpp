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
#include "InitializeC2Sim.h"

//std::string noValue = "<null>";

// this really works!! weird syntax
InitializeC2Sim::InitializeC2Sim() : myEntityMap(), entityUuidList(), entityNameToUuidMap(), sideMap(), theMutex()
{
	

}


InitializeC2Sim::~InitializeC2Sim()
{
}

bool InitializeC2Sim::isC2SimInitFilled() {
	return isFilled;

}
bool InitializeC2Sim::isC2SimInitComplete() {
	return isInitComplete;
}

void InitializeC2Sim::setInitFileFilled() {
	this->isFilled = true;
}
void InitializeC2Sim::setInitComplete() {
	this->isInitComplete = true;
}

bool InitializeC2Sim::contains(std::string& uuid) {
	if (myEntityMap.count(uuid) > 0) {
		return true;
	}
	else return false;

}

 SimEntity& InitializeC2Sim::getEnitiy(std::string& uuid) {
	 
	 return myEntityMap[uuid];

/*
	auto itr = myEntityMap.find(uuid);
	if (itr != myEntityMap.end()) {
		return &(itr->second);
	}
	else {
		return NULL;
	}
*/
}
 void InitializeC2Sim::putEntity(std::string& uuid, SimEntity& se) {
	 myEntityMap[uuid] = se;
	 entityNameToUuidMap[se.name] = uuid;
}

 
 std::string InitializeC2Sim::getEntityName(std::string& uuid) {
	
	 auto itr = myEntityMap.find(uuid);
	 if (itr != myEntityMap.end()) {
		 return itr->second.name;
	 }
	 else
		 return noValue;

 }

 std::string InitializeC2Sim::getEntityUUID(std::string& name) {
	 std::lock_guard<std::mutex> theLockGuard(theMutex);
	 auto itr = entityNameToUuidMap.find(name);
	 if (itr != entityNameToUuidMap.end()) {
		 return itr->second;
	 }
	 else return noValue;

 }


 void InitializeC2Sim::addEntityUUID(std::string uuid) {
	 std::lock_guard<std::mutex> theLockGuard(theMutex);
	 this->entityUuidList.emplace_back(uuid);
 }

 bool InitializeC2Sim::listContains(std::string uuid) {
	 std::lock_guard<std::mutex> theLockGuard(theMutex);

	 for (int i = 0; i < entityUuidList.size(); i++) {
		 std::string& eu = entityUuidList[i];
		 if (eu.compare(uuid) == 0) {
			 return true;
		 }
	 }

	 return false;
 }

 std::string InitializeC2Sim::getSideName(std::string& uuid) {

	 return sideMap[uuid];
}
 void InitializeC2Sim::putSideName(std::string& uuid, std::string& sidename) {
	 std::lock_guard<std::mutex> theLockGuard(theMutex);
	 sideMap[uuid] = sidename;
}

 std::vector<std::string>* InitializeC2Sim::getAllEntityUUIDs() {
	 return &entityUuidList;
 }

 std::mutex* InitializeC2Sim::getTheMutex() {
	 return &theMutex;
 }

 //void InitializeC2Sim::lockMutex() {
	// theMutex.lock();
 //}
 //void InitializeC2Sim::unlockMutex() {
	// theMutex.unlock();
 //}
