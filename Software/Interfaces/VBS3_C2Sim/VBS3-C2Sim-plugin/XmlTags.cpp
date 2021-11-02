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
#include "XmlTags.h"


XmlTags::XmlTags()
{
	setXMLNameSpacePrefix("");
}

XmlTags::XmlTags(std::string ns) {
	setXMLNameSpacePrefix(ns);
}


XmlTags::~XmlTags()
{
}

void XmlTags::setXMLNameSpacePrefix(std::string xns) {
	if (xns.length() > 0) {
		xns.append(":");
	}
	this->ns = xns;

	MessageBody = ns + "MessageBody";
	DomainMessageBody = ns + "DomainMessageBody";
	OrderBody = ns + "OrderBody";
	FromSender = ns + "FromSender";
	ToReceiver = ns + "ToReceiver";
	PerformingEntity = ns + "PerformingEntity";
	Task = ns + "Task";
	ManeuverWarfareTask = ns + "ManeuverWarfareTask";
	DesiredEffectCode = ns + "DesiredEffectCode";
	TaskNameCode = ns + "TaskNameCode";
	Name = ns + "Name";
	MapGraphicID = ns + "MapGraphicID";
	Location = ns + "Location";
	Coordinate = ns + "Coordinate";
	GeodeticCoordinate = ns + "GeodeticCoordinate";
	Latitude = ns + "Latitude";
	Longitude = ns + "Longitude";
	Route = ns + "Route";
	RouteLocation = ns + "RouteLocation";
	IssuedTime = ns + "IssuedTime";
	StartTime = ns + "StartTime";
	IsoDateTime = ns + "IsoDateTime";
	DateTime = ns + "DateTime";
	OrderID = ns + "OrderID";
	UUID = ns + "UUID";

	ObjectInitialization = ns + "ObjectInitialization";
	C2SIMInitializationBody = ns + "C2SIMInitializationBody";
	InitializationDataFile = ns + "InitializationDataFile";
	SystemName = ns + "SystemName";
	ObjectDefinitions = ns + "ObjectDefinitions";
	AbstractObject = ns + "AbstractObject";
	CommunicationNetwork = ns + "CommunicationNetwork";
	ForceSide = ns + "ForceSide";
	SideHostilityCode = ns + "SideHostilityCode";
	Action = ns + "Action";
	Event = ns + "Event";
	EntityDefinedLocation = ns + "EntityDefinedLocation";
	LocationReferenceEntity = ns + "LocationReferenceEntity";
	EventCode = ns + "EventCode";
	Entity = ns + "Entity";
	ActorEntity = ns + "ActorEntity";
	PhysicalEntity = ns + "PhysicalEntity";
	CollectiveEntity = ns + "CollectiveEntity";
	MilitaryOrganization = ns + "MilitaryOrganization";
	Platform = ns + "Platform";
	CurrentState = ns + "CurrentState";
	Resource = ns + "Resource";

	PhysicalState = ns + "PhysicalState";
	DirectionOfMovement = ns + "DirectionOfMovement";
	EulerAngles = ns + "EulerAngles";
	Phi = ns + "Phi";
	Psi = ns + "Psi";
	Theta = ns + "Theta";
	EntityHealthStatus = ns + "EntityHealthStatus";
	OperationalStatus = ns + "OperationalStatus";
	OperationalStatusCode = ns + "OperationalStatusCode";
	EntityDescriptor = ns + "EntityDescriptor";
	Superior = ns + "Superior";
	Side = ns + "Side";
	EntityType = ns + "EntityType";
	CurrentTask = ns + "sCurrentTask";
	Marking = ns + "sMarking";

	APP6_SIDC = ns + "APP6-SIDC";
	EntityTypeString = ns + "EntityTypeString";
	SIDCString = ns + "SIDCString";
	PlanPhaseReference = ns + "PlanPhaseReference";
	SystemEntityList = ns + "SystemEntityList";
	ActorReference = ns + "ActorReference";

	ReportBody = ns + "ReportBody";
	ReportContent = ns + "ReportContent";
	PositionReportContent = ns+"PositionReportContent";
	TimeOfObservation = ns + "TimeOfObservation";
	ReportingEntity = ns + "ReportingEntity";
	Strength = ns + "Strength";
	StrengthPercentage = ns + "StrengthPercentage";
	SubjectEntity = ns + "SubjectEntity";

	ObservationReportContent = ns + "ObservationReportContent";
	Observation = ns + "Observation";
	LocationObservation = ns + "LocationObservation";
	ConfidenceLevel = ns + "ConfidenceLevel";
	UncertaintyInterval = ns + "UncertaintyInterval";

	SystemCommandBody = ns + "SystemCommandBody";
	SystemCommandTypeCode = ns + "SystemCommandTypeCode";
	SessionStateCode = ns + "SessionStateCode";



	// TaskNameCode
	tncACQUIR = "ACQUIR";
	tncADVANC = "ADVANC";
	tncAIRDEF = "AIRDEF";
	tncAMBUSH = "AMBUSH";
	tncARASLT = "ARASLT";
	tncASSMBL = "ASSMBL";
	tncATTACK = "ATTACK";
	tncATTRIT = "ATTRIT";
	tncATTSPT = "ATTSPT";
	tncAVOID = "AVOID";
	tncAssistOtherUnit = "AssistOtherUnit";
	tncBLOCK = "BLOCK";
	tncBREACH = "BREACH";
	tncBYPASS = "BYPASS";
	tncCAPTUR = "CAPTUR";
	tncCLOSE = "CLOSE";
	tncCLRLND = "CLRLND";
	tncCLROBS = "CLROBS";
	tncCNFPSL = "CNFPSL";
	tncCNRPSL = "CNRPSL";
	tncCOVER = "COVER";
	tncCRESRV = "CRESRV";
	tncCTRATK = "CTRATK";
	tncCTRBYF = "CTRBYF";
	tncCTRFIR = "CTRFIR";
	tncDEBARK = "DEBARK";
	tncDECEIV = "DECEIV";
	tncDEFEAT = "DEFEAT";
	tncDEFEND = "DEFEND";
	tncDELAY = "DELAY";
	tncDENY = "DENY";
	tncDESTRY = "DESTRY";
	tncDISENG = "DISENG";
	tncDISRPT = "DISRPT";
	tncDLBATK = "DLBATK";
	tncDRONL = "DRONL";
	tncEMBARK = "EMBARK";
	tncENGAGE = "ENGAGE";
	tncENVLP = "ENVLP";
	tncEXPLT = "EXPLT";
	tncFIX = "FIX";
	tncGUARD = "GUARD";
	tncHARASS = "HARASS";
	tncHASTY = "HASTY";
	tncHONASP = "HONASP";
	tncHoldInPlace = "HoldInPlace";
	tncINFILT = "INFILT";
	tncINTDCT = "INTDCT";
	tncISOLAT = "ISOLAT";
	tncLOCATE = "LOCATE";
	tncMEDEVC = "MEDEVC";
	tncMOPUP = "MOPUP";
	tncMOVE = "MOVE";
	tncMoveToLocation = "MoveToLocation";
	tncOBSCUR = "OBSCUR";
	tncOBSRV = "OBSRV";
	tncOCCUPY = "OCCUPY";
	tncObserve = "Observe";
	tncOrientToLocation = "OrientToLocation";
	tncPATROL = "PATROL";
	tncPENTRT = "PENTRT";
	tncPLAN = "PLAN";
	tncPREFIR = "PREFIR";
	tncPURSUE = "PURSUE";
	tncRECCE = "RECCE";
	tncRECONS = "RECONS";
	tncRECOVR = "RECOVR";
	tncREFUEL = "REFUEL";
	tncREINF = "REINF";
	tncRESCUE = "RESCUE";
	tncRESUPL = "RESUPL";
	tncRETAIN = "RETAIN";
	tncRLFPLC = "RLFPLC";
	tncReportPosition = "ReportPosition";
	tncSCREEN = "SCREEN";
	tncSECURE = "SECURE";
	tncSEIZE = "SEIZE";
	tncSUPPRS = "SUPPRS";
	tncSUPPRT = "SUPPRT";
	tncTHREAT = "THREAT";
	tncTURN = "TURN";
	tncUseCapability = "UseCapability";

	//DesiredEffectCode
	decBURN = "BURN";
	decCAPTRD = "CAPTRD";
	decCONS = "CONS";
	decDSTRYK = "DSTRYK";
	decFKIL = "FKIL";
	decFLIG = "FLIG";
	decGenericEffect = "GenericEffect";
	decIDNT = "IDNT";
	decILLUMN = "ILLUMN";
	decINTREC = "INTREC";
	decKILL = "KILL";
	decLDAM = "LDAM";
	decLGTRST = "LGTRST";
	decLOST = "LOST";
	decMKIL = "MKIL";
	decMODDAM = "MODDAM";
	decNBCAS = "NBCAS";
	decNKN = "NKN";
	decNORSTN = "NORSTN";
	decNOS = "NOS";
	decNUTRLD = "NUTRLD";
	decSDAM = "SDAM";
	decSUPRSD = "SUPRSD";
	decWNDD = "WNDD";

	// systemCommand
	scInitializationComplete = ns + "InitializationComplete";
	scShareScenario = ns + "ShareScenario";
	scStartScenario = ns + "StartScenario";
	scSubmitInitialization = ns + "SubmitInitialization";
	scUNINITIALIZED = ns + "UNINITIALIZED";
	scINITIALIZING = ns + "INITIALIZING";
	scINITIALIZED = ns + "INITIALIZED";
	scRUNNING = ns + "RUNNING";
	scPAUSED = ns + "PAUSED";


}
