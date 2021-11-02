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
#include "PosReportXml.h"
#include "Util.h"
#include "tinyxml2.h"
#include "XmlTags.h"

using namespace tinyxml2;




XmlTags xt_default;
XmlTags xt_c2sNs;



PosReportXml::PosReportXml()
{
	xt_default = XmlTags();
	xt_c2sNs = XmlTags("c2s");
}


PosReportXml::~PosReportXml()
{
}

void PosReportXml::setSimEntity(SimEntity& simEntity) {

	this->se = SimEntity(simEntity);

}

std::string PosReportXml::toXML(bool useC2SNameSpace) {

	XmlTags* xt = (useC2SNameSpace) ? &xt_c2sNs : &xt_default;

	XMLDocument xdoc;
	xdoc.NewDeclaration(); // automatic "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	XMLElement* eMsgBody = xdoc.NewElement(xt->MessageBody.c_str());
	xdoc.InsertFirstChild(eMsgBody);
	//eMsgBody->SetAttribute("xmlns:xsi", _xmlns_xsi.c_str());
	//eMsgBody->SetAttribute("xsi:schemaLocation", _xsi_schemaLocation.c_str());
	eMsgBody->SetAttribute("xmlns", xt->attr_xmlns.c_str());
	if (useC2SNameSpace) {
		eMsgBody->SetAttribute("xmlns:c2s", xt->attr_xmlns_c2s.c_str());
	}
	
	XMLElement* eDomMsgBdy = xdoc.NewElement(xt->DomainMessageBody.c_str());
	eMsgBody->InsertEndChild(eDomMsgBdy);

	XMLElement* eRptBdy = xdoc.NewElement(xt->ReportBody.c_str());
	eDomMsgBdy->InsertEndChild(eRptBdy);
	
	XMLElement* eFromS = xdoc.NewElement(xt->FromSender.c_str());
	eRptBdy->InsertEndChild(eFromS);
	eFromS->SetText(se.entityUUID.c_str());

	XMLElement* eToRcvr = xdoc.NewElement(xt->ToReceiver.c_str());
	eRptBdy->InsertEndChild(eToRcvr);
	eToRcvr->SetText(se.superiorUUID.c_str());

	XMLElement* eRptContent = xdoc.NewElement(xt->ReportContent.c_str());
	eRptBdy->InsertEndChild(eRptContent);

	XMLElement* ePosRptCnt = xdoc.NewElement(xt->PositionReportContent.c_str());
	eRptContent->InsertEndChild(ePosRptCnt);

	// next few are all in ePosRptCnt
	// time
	XMLElement* eTimeObs = xdoc.NewElement(xt->TimeOfObservation.c_str());
	ePosRptCnt->InsertEndChild(eTimeObs);

	XMLElement* eIsoDT = xdoc.NewElement(xt->IsoDateTime.c_str());
	eTimeObs->InsertEndChild(eIsoDT);	
	eIsoDT->SetText(Util::isoTime(std::chrono::system_clock::now()).c_str());
	
	// location
	XMLElement* eLocn = xdoc.NewElement(xt->Location.c_str());
	ePosRptCnt->InsertEndChild(eLocn);

	XMLElement* eCoord = xdoc.NewElement(xt->Coordinate.c_str());
	eLocn->InsertEndChild(eCoord);

	XMLElement* eGeoCord = xdoc.NewElement(xt->GeodeticCoordinate.c_str());
	eCoord->InsertEndChild(eGeoCord);
	
	XMLElement* eLat = xdoc.NewElement(xt->Latitude.c_str());
	XMLElement* eLon = xdoc.NewElement(xt->Longitude.c_str());
	eGeoCord->InsertEndChild(eLat);
	eGeoCord->InsertEndChild(eLon);
	double lat, lon, alt;
	se.getPosition(lat, lon, alt);
	eLat->SetText(lat);
	eLon->SetText(lon);
	
	// Operational Status
	XMLElement* eOpStat = xdoc.NewElement(xt->OperationalStatus.c_str());
	ePosRptCnt->InsertEndChild(eOpStat);

	XMLElement* eOpStCd = xdoc.NewElement(xt->OperationalStatusCode.c_str());
	eOpStat->InsertEndChild(eOpStCd);
	eOpStCd->SetText(se.getOprStatus().c_str());

	// strength
	XMLElement* eStr = xdoc.NewElement(xt->Strength.c_str());
	ePosRptCnt->InsertEndChild(eStr);

	XMLElement* eStPerc = xdoc.NewElement(xt->StrengthPercentage.c_str());
	eStr->InsertEndChild(eStPerc);
	eStPerc->SetText(se.getStrength());

	// subject entity
	XMLElement* eSbjEnt = xdoc.NewElement(xt->SubjectEntity.c_str());
	ePosRptCnt->InsertEndChild(eSbjEnt);
	eSbjEnt->SetText(se.entityUUID.c_str());

	// back to ReportBody
	XMLElement* eRptingEtn = xdoc.NewElement(xt->ReportingEntity.c_str());
	eRptBdy->InsertEndChild(eRptingEtn);
	eRptingEtn->SetText(se.entityUUID.c_str());

	// should be the end!

	XMLPrinter xprinter;
	xdoc.Print(&xprinter);
	
	std::string xmlFullString = xprinter.CStr();

	return xmlFullString;
	
}
