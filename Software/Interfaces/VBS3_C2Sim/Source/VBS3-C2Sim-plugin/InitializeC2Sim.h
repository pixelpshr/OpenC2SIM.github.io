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

#include <string>
#include <map>
#include <vector>
#include "SimEntity.h"
#include <mutex>
#include <atomic>

class InitializeC2Sim
{

public:
	InitializeC2Sim();
	~InitializeC2Sim();

	const std::string noValue = "<null>";
	std::string systemName;

	bool contains(std::string& uuid);
	SimEntity& getEnitiy(std::string& uuid);
	void putEntity(std::string& uuid, SimEntity& se);

	std::string getEntityName(std::string& uuid);
	std::string getEntityUUID(std::string&name);
	void putEntityName(std::string& uuid, std::string& name);
	void addEntityUUID(std::string uuid);

	std::vector<std::string>* getAllEntityUUIDs();

	std::string getSideName(std::string& uuid);
	void putSideName(std::string& uuid, std::string& sidename);
	bool listContains(std::string uuid);

	bool isC2SimInitFilled();
	bool isC2SimInitComplete();
	void setInitFileFilled();
	void setInitComplete();


	//void lockMutex();
	//void unlockMutex();

	std::mutex* getTheMutex();

private: 
	std::mutex theMutex;
	std::vector<std::string> entityUuidList;
	std::map<std::string, std::string> entityNameToUuidMap;
	std::map<std::string, SimEntity> myEntityMap;
	std::map<std::string, std::string> sideMap;

	std::atomic<bool>  isFilled = false;
	std::atomic<bool> isInitComplete = false;


};

