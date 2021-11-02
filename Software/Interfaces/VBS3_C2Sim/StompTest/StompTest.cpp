// StompTest.cpp : This file contains the 'main' function. Program execution begins and ends there.
//

#include "pch.h"
#include <iostream>
#include <thread>
#include <atomic>

#include <queue>
#include <Windows.h>

#include "C2SIMClientLib.h"

std::string myaddr = "192.168.24.202";
std::string myport = "61613";

std::atomic<bool> stayAlive = false;

std::queue<std::string>* myq;
BMLClientSTOMP_Lib * stompLib;



//std::thread t1(&C2SIMinterface::readStomp, textIf, c2simInterface,
//	skipInitialize == "1", clientId);


void runStompListener(std::queue<std::string>* q) {
	

	stompLib = new  BMLClientSTOMP_Lib();
	
	try {
		std::cout << "\n********************************\n\n";

		stompLib->setHost(myaddr);
		stompLib->setPort(myport);
		stompLib->setDestination("/topic/BML");
		stompLib->addAdvSubscription("protocol = 'C2SIM'");
		std::cout << "created StompLib\n";
	
		BMLSTOMPMessage* cntMsg = stompLib->connect();
		std::string isConnected = cntMsg->getMessageType();
		std::cout << "tried to connect to Stomp: response = " << isConnected << "\n";
		if (isConnected.compare("CONNECTED") == 0) {
			stayAlive = true;
		}
		else {
			stayAlive = false;
		}
		delete cntMsg;
	}

	catch (BMLClientException bce) {
		std::cout << "STOMP connection ex. " << bce.getMessage() << "\n";
		stayAlive = false;
	}

	BMLSTOMPMessage * stompMsg = NULL;
	while (stayAlive) {

		if (stompMsg != NULL) {
			delete stompMsg;
		} 
		try {
			stompMsg = stompLib->getNext_Block();
			std::string xml = stompMsg->getMessageBody();

			int len = stompMsg->getMessageLength();

			std::cout << "Recv Stomp= " << len << "\n";

			size_t i = xml.find("SessionStateCode");
			if (i != std::string::npos) {
				std::string ssc = xml.substr(i, 40);
				q->push(ssc);
			}
		}
		catch (BMLClientException ex) {
			std::cout << "disconnect\n";
		}

	}

	try {
		delete stompLib;
	}
	catch (...) {
		std::cout << " ex on stompLib Destructor!\n";
	}
	stompLib = NULL;
}




int main()
{
	myq = new std::queue<std::string>();

	char again = 'y';

	while (! (again == 'n')) {



		stayAlive = true;
		std::thread thrdStomp(&runStompListener, myq);

		while (stayAlive) {
			if (myq->empty()) {
				Sleep(100);
			}
			else {

				std::string ssc = myq->front();
				myq->pop();

				if (ssc.find("UNINITIALIZED") != std::string::npos) {
					stayAlive = false;
					if (stompLib != NULL) {
						//stompLib->disconnect();

					}
				}
			}
		}


		thrdStomp.join();
		std::cout << "Try stomp connection again? (y | n)  ";

		again = getchar();
	}

	delete myq;

}

// Run program: Ctrl + F5 or Debug > Start Without Debugging menu
// Debug program: F5 or Debug > Start Debugging menu

// Tips for Getting Started: 
//   1. Use the Solution Explorer window to add/manage files
//   2. Use the Team Explorer window to connect to source control
//   3. Use the Output window to see build output and other messages
//   4. Use the Error List window to view errors
//   5. Go to Project > Add New Item to create new code files, or Project > Add Existing Item to add existing code files to the project
//   6. In the future, to open this project again, go to File > Open > Project and select the .sln file
