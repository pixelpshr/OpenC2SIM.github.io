/*******************************************************************************
** Copyright (c) 2002 MAK Technologies, Inc.
** All rights reserved.
*******************************************************************************/
/*******************************************************************************
** $RCSfile: main.cxx,v $ $Revision: 1.13 $ $State: Exp $
*******************************************************************************/

#include "textIf.h"
#include "remoteControlInit.h"
#include "C2SIMinterface.h"

//VR-Forces headers
#include <vrfMsgTransport/networkEventManager.h>
#include <vrfMsgTransport/vrfMessageInterface.h>
#include <vrfcontrol/vrfRemoteController.h>

// TMP new
#include <vrfMsgTransport/networkEventManager.h>
#include <vrfMsgTransport/vrfTDLMessageInterface.h>

//VR-Link headers
#include <vl/exerciseConn.h>
#include <vlutil/vlProcessControl.h>

std::string blueForceName = "";

///////////////// Debug VrfMessageInterface Code ////////////////////
class testVrfMessageInterface : public DtVrfMessageInterface
{
public:
    testVrfMessageInterface(DtEventManager* eventManager,
        bool useDefaultInterfaceFactory = true);
    void initializeConnections();
    void receiveMessage(DtDataInteraction* dataInter);
    bool receiveMessage(const DtSimInterfaceMessage& msg);
    void simInterfaceContentMessageEvent(const DtEvent& c);
    void netMessageCallback(const DtEvent& e);
};
testVrfMessageInterface::testVrfMessageInterface(DtEventManager* eventManager,
    bool useDefaultInterfaceFactory)
    : DtVrfMessageInterface(eventManager, useDefaultInterfaceFactory)
{}
void testVrfMessageInterface::initializeConnections()
{
    if (myEventManager)
    {
        printf("Msg IF: connecting signals\n");
    }
    DtVrfMessageInterface::initializeConnections();
}

void testVrfMessageInterface::receiveMessage(DtDataInteraction* dataInter)
{
    {
        DtSimulationAddress recipient;
        DtEntityIdentifier addrBuf;
        addrBuf = dataInter->receiverId();

        recipient.setSiteId(addrBuf.site());
        recipient.setApplicationId(addrBuf.host());

        if (matchPattern(messageAddress(), recipient))
        {
            DtSimInterfaceContent* content = NULL;
            if (getContentFromNet(dataInter, content) &&
                dataInter->datumId(DtFixed, 1) == DtDatumSessionId)
            {
                unsigned int msgSessionId = dataInter->datumValUnsigned32(DtFixed, 1);
                printf("Msg IF: Receiving data interaction. Msg session ID %d (mine %d)\n",
                    msgSessionId, mySessionId);
            }
        }
    }

    DtVrfMessageInterface::receiveMessage(dataInter);
}

bool testVrfMessageInterface::receiveMessage(const DtSimInterfaceMessage& msg)
{
    DtString content;
    const_cast<DtSimInterfaceMessage&>(msg).printDataToString(content);
    printf("Msg IF: processing sim IF msg\n");
    if (matchPattern(messageAddress(), msg.recipient()))
    {
        printf("...Msg for me:\n");
        printf("%s\n--------End of msg -------------\n",
            content.c_str());
    }
    return DtVrfMessageInterface::receiveMessage(msg);
}

void testVrfMessageInterface::simInterfaceContentMessageEvent(const DtEvent& c)
{
    printf("\nMsg IF: handling sim IF Content msg\n");
    DtVrfMessageInterface::simInterfaceContentMessageEvent(c);
}

void testVrfMessageInterface::netMessageCallback(const DtEvent& e)
{
    printf("\nMsg IF: Handling net message (data interaction event)\n");

    DtVrfMessageInterface::netMessageCallback(e);
}

//--------------
#include <vrfmsgs/ifStatus.h>
class testBackendListener : public DtVrfBackendListener
{
public:
    testBackendListener(DtVrfMessageInterface* mIf);
    void processStatusMessage(DtSimMessage* msg);
};

testBackendListener::testBackendListener(DtVrfMessageInterface* mIf)
    : DtVrfBackendListener(mIf)
{}
void testBackendListener::processStatusMessage(DtSimMessage* msg)
{
    printf("Listener processing BE status message.\n");
    DtSimInterfaceMessage* ifMsg = (DtSimInterfaceMessage*)msg;

    // ignore messages from local address   
    if (ifMsg->sender() == myMessageIf->messageAddress())
    {
        printf("...From me; ignoring.\n");
    }
    else
    {
        DtIfStatus* ifStatusObject = (DtIfStatus*)ifMsg->interfaceContent();

        // ignore messages from from old front ends
        if (ifStatusObject->port() == 0)
        {
            printf("...Port 0; ignoring\n");
        }
        else
        {
            DtStringHashKey key(ifMsg->sender().string());
            DtBackend* status = (DtBackend*)myBackends.lookup(key);
            if (!status)
            {
                printf("...New backend; adding\n");
            }
        }
    }

    DtVrfBackendListener::processStatusMessage(msg);
}

class testRC : public DtVrfRemoteController
{
public:
    testRC::testRC();
    void setListener(DtVrfBackendListener* l);
};

testRC::testRC() : DtVrfRemoteController()
{}

void testRC::setListener(DtVrfBackendListener* l)
{
    if (myListener)
    {
        delete myListener;
    }
    myListener = l;
}
//////////////// End Debug VrfMessageInterface Code /////////////////////

int main(int argc, char** argv)
{
   //Create initializer object used to provide initialization data to the
   //exercise connection, configured through command line arguments.
   DtRemoteControlInitializer appInitializer(argc, argv);
   appInitializer.parseCmdLine();
   
   //Create the controller
   //DtVrfRemoteController* controller = new DtVrfRemoteController();
   testRC* controller = new testRC();  // NEW


   //Create an exercise connection
   DtExerciseConn* exConn = new DtExerciseConn(appInitializer);

   // TMP
#if 1
   // To set session ID
   DtNetworkEventManager* dtEm = new DtNetworkEventManager();
   dtEm->setExerciseConnection(exConn);
   dtEm->setProcessEventsImmediately(true); // !!!!!!!!!!!!!!
   testVrfMessageInterface* msgIf = new testVrfMessageInterface(dtEm);
   msgIf->init();
   msgIf->setSessionId(2);

   DtVrfTDLMessageInterface* tdl = new DtVrfTDLMessageInterface(dtEm);
   tdl->init();

   controller->init(msgIf,
      0,0,0, "entity-identifier",
#if DtHLA
     makVrfVrlinkExtToolkit::DtReflectedExtendedAttributesObjectList* eaol = 0);
#else
      tdl);
#endif

   // NEW
   testBackendListener* listener = new testBackendListener(msgIf);
   controller->setListener(listener);
#else

   // OLD
   controller->init(exConn);

   DtVrfMessageInterface* mIf = controller->vrfMessageInterface();
//   mIf->setSessionId(2);
//   mIf->eventManager()->setProcessEventsImmediately(true);
#endif

   //Create our text interface so we can enter text commands.
   DtTextInterface* textIf = new DtTextInterface(controller,
       "10.2.10.70", "8080", "61613", 0, 0, 0, 0);

   // Create a C2SIM controller to read orders and translate them for VRF
   C2SIMinterface* c2simInterface =
       new C2SIMinterface(textIf, "10.2.10.70", "61613", "8080", "USA-SIM2",
           false,false,false);

   // Start a thread to read from STOMP and generate VRForces commands
   std::thread t1(&C2SIMinterface::readStomp, c2simInterface,
       false, "USA-SIM2", 0);
 
   //Processing: read stdin and call drainInput.
   //Calling drainInput() ensures that the controller receives
   //necessary feedback from VR-Forces backend applications.
   while (!textIf->timeToQuit())
   { 
      textIf->readStdin();
      exConn->clock()->setSimTime(exConn->clock()->elapsedRealTime());
      exConn->drainInput();
      DtSleep(0.1);
   }

   delete textIf;
   delete controller;
   delete exConn;

   return 0;
}
