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
class XmlTags
{
public:
	XmlTags();
	XmlTags(std::string);
	~XmlTags();

	std::string preamble = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	std::string attr_xmlns_xsi = "http://www.w3.org/2001/XMLSchema-instance";
	std::string attr_xsi_schemaLocation = "http://www.sisostds.org/schemas/C2SIM/1.1 ../Schema/C2SIM/C2SIMv9_SMXv9_LOXplusv5.xsd";
	std::string attr_xmlns = "http://www.sisostds.org/schemas/C2SIM/1.1";
	std::string attr_xmlns_c2s = "http://www.sisostds.org/schemas/C2SIM/1.1";


	// these are in the order schema
	std::string MessageBody;
	std::string DomainMessageBody;
	std::string OrderBody;
	std::string FromSender;
	std::string ToReceiver;
	std::string PerformingEntity;
	std::string Task;
	std::string ManeuverWarfareTask;
	std::string DesiredEffectCode;
	std::string TaskNameCode;
	std::string Name;
	std::string MapGraphicID;
	std::string Location;
	std::string Coordinate;
	std::string GeodeticCoordinate;
	std::string Latitude;
	std::string Longitude;
	std::string Route;
	std::string RouteLocation;
	std::string IssuedTime;
	std::string StartTime;
	std::string DateTime;
	std::string IsoDateTime;
	std::string OrderID;
	std::string UUID;
	// these are in initialization
	std::string ObjectInitialization;
	std::string C2SIMInitializationBody;
	std::string InitializationDataFile;
	std::string SystemName;
	std::string ObjectDefinitions;
	std::string AbstractObject;
	std::string CommunicationNetwork;
	std::string ForceSide;
	std::string SideHostilityCode;
	std::string Action;
	std::string Event;
	std::string EntityDefinedLocation;
	std::string LocationReferenceEntity;
	std::string EventCode;
	std::string Entity;
	std::string ActorEntity;
	std::string PhysicalEntity;
	std::string CollectiveEntity;
	std::string MilitaryOrganization;
	std::string Platform;
	std::string CurrentState;
	std::string Resource;

	std::string PhysicalState;
	std::string DirectionOfMovement;
	std::string EulerAngles;
	std::string Phi;
	std::string Psi;
	std::string Theta;
	std::string EntityHealthStatus;
	std::string OperationalStatus;
	std::string OperationalStatusCode;
	std::string EntityDescriptor;
	std::string Superior;
	std::string Side;
	std::string EntityType;
	std::string CurrentTask;
	std::string Marking;
	std::string APP6_SIDC;
	std::string EntityTypeString;
	std::string SIDCString;
	std::string PlanPhaseReference;
	std::string SystemEntityList;
	std::string ActorReference;

	// these are in position report
	std::string ReportBody;
	std::string ReportContent;
	std::string PositionReportContent;
	std::string TimeOfObservation;
	std::string ReportingEntity;
	std::string Strength;
	std::string StrengthPercentage;
	std::string SubjectEntity;
	// more for observation report
	std::string ObservationReportContent;
	std::string Observation;
	std::string LocationObservation;
	std::string ConfidenceLevel;
	std::string UncertaintyInterval;


	// these are in SystemCommand
	std::string SystemCommandBody;
	std::string SystemCommandTypeCode;
	std::string SessionStateCode;

	//----------------------------------------------
	// TaskNameCodeType

	std::string tncACQUIR;
	std::string tncADVANC;
	std::string tncAIRDEF;
	std::string tncAMBUSH;
	std::string tncARASLT;
	std::string tncASSMBL;
	std::string tncATTACK;
	std::string tncATTRIT;
	std::string tncATTSPT;
	std::string tncAVOID;
	std::string tncAssistOtherUnit;
	std::string tncBLOCK;
	std::string tncBREACH;
	std::string tncBYPASS;
	std::string tncCAPTUR;
	std::string tncCLOSE;
	std::string tncCLRLND;
	std::string tncCLROBS;
	std::string tncCNFPSL;
	std::string tncCNRPSL;
	std::string tncCOVER;
	std::string tncCRESRV;
	std::string tncCTRATK;
	std::string tncCTRBYF;
	std::string tncCTRFIR;
	std::string tncDEBARK;
	std::string tncDECEIV;
	std::string tncDEFEAT;
	std::string tncDEFEND;
	std::string tncDELAY;
	std::string tncDENY;
	std::string tncDESTRY;
	std::string tncDISENG;
	std::string tncDISRPT;
	std::string tncDLBATK;
	std::string tncDRONL;
	std::string tncEMBARK;
	std::string tncENGAGE;
	std::string tncENVLP;
	std::string tncEXPLT;
	std::string tncFIX;
	std::string tncGUARD;
	std::string tncHARASS;
	std::string tncHASTY;
	std::string tncHONASP;
	std::string tncHoldInPlace;
	std::string tncINFILT;
	std::string tncINTDCT;
	std::string tncISOLAT;
	std::string tncLOCATE;
	std::string tncMEDEVC;
	std::string tncMOPUP;
	std::string tncMOVE;
	std::string tncMoveToLocation;
	std::string tncOBSCUR;
	std::string tncOBSRV;
	std::string tncOCCUPY;
	std::string tncObserve;
	std::string tncOrientToLocation;
	std::string tncPATROL;
	std::string tncPENTRT;
	std::string tncPLAN;
	std::string tncPREFIR;
	std::string tncPURSUE;
	std::string tncRECCE;
	std::string tncRECONS;
	std::string tncRECOVR;
	std::string tncREFUEL;
	std::string tncREINF;
	std::string tncRESCUE;
	std::string tncRESUPL;
	std::string tncRETAIN;
	std::string tncRLFPLC;
	std::string tncReportPosition;
	std::string tncSCREEN;
	std::string tncSECURE;
	std::string tncSEIZE;
	std::string tncSUPPRS;
	std::string tncSUPPRT;
	std::string tncTHREAT;
	std::string tncTURN;
	std::string tncUseCapability;


	//----------------------------------------------
	// DesiredEffectCodeType
	std::string decBURN;
	std::string decCAPTRD;
	std::string decCONS;
	std::string decDSTRYK;
	std::string decFKIL;
	std::string decFLIG;
	std::string decGenericEffect;
	std::string decIDNT;
	std::string decILLUMN;
	std::string decINTREC;
	std::string decKILL;
	std::string decLDAM;
	std::string decLGTRST;
	std::string decLOST;
	std::string decMKIL;
	std::string decMODDAM;
	std::string decNBCAS;
	std::string decNKN;
	std::string decNORSTN;
	std::string decNOS;
	std::string decNUTRLD;
	std::string decSDAM;
	std::string decSUPRSD;
	std::string decWNDD;

	//------------------------------------------
	// syscmd enums
	std::string scInitializationComplete;
	std::string scShareScenario;
	std::string scStartScenario;
	std::string scSubmitInitialization;
	std::string scUNINITIALIZED;
	std::string scINITIALIZING;
	std::string scINITIALIZED;
	std::string scRUNNING;
	std::string scPAUSED;

private:
	std::string ns;
	void setXMLNameSpacePrefix(std::string xns);

};

