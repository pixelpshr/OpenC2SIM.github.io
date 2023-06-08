/*----------------------------------------------------------------*
|   Copyright 2009-2022 Networking and Simulation Laboratory      |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for all purposes is hereby       |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
*----------------------------------------------------------------*/
package edu.gmu.netlab;

import java.io.*;
import javax.swing.JFileChooser;
import com.jaxfront.core.util.URLHelper;
import edu.gmu.c4i.c2simclientlib2.*;
import static edu.gmu.netlab.C2SIMGUI.debugMode;
import static edu.gmu.netlab.C2SIMGUI.printDebug;
import static edu.gmu.netlab.C2SIMGUI.printError;

/**
 * Report Support Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 *              mpullen 09/28/2021
 * @since	12/1/2011
 */ 
public class ReportC2SIM
{ 	
    String root="";
    C2SIMGUI bml = C2SIMGUI.bml;
    String documentType = "C2SIM Report";
  
    // constructor
    public ReportC2SIM() 
    {
        bml.orderDomainName = "C2SIM";
        bml.generalBMLFunction = "C2SIM";
    }
	
    /**
     * Create a new C2SIM Report (XML Document)
     * text to be filled in by JAXFront panel
     * 
     * @mpullen
     * @since 5Apr18
     */
    void newReportC2SIM() {
        bml.releaseXUICache();
        bml.root = "MessageBody";
        bml.reportBMLType = "C2SIM";
        bml.documentTypeLabel.setText(documentType);

        //set xsdUrl and xuiUrl
        bml.xsdUrl = 
            URLHelper.getUserURL(
                bml.guiFolderLocation + bml.c2simReportSchemaLocation);
        bml.initDom(
            "default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            root);
                     
        // load the report into JAXFront panel
        try{
            bml.loadJaxFront(
                null,
                "New C2SIM Report",
                bml.c2simReportSchemaLocation,
                "MessageBody");
        } catch(Exception e) {
            bml.printError("Exception loading JaxFront:" + e);
            e.printStackTrace();
            return;
        }

    }//end newReport()
	
	
    /**
     * Open an existing C2SIM Report (XML Document) from The File System
     * 
     * @since 5Apr18
     */
    boolean openReportFSC2SIM(String subFolder) throws IOException {		
        bml.releaseXUICache();
        bml.reportBMLType = "C2SIM";	
        bml.documentTypeLabel.setText(documentType);
        JFileChooser xmlFc = 
            new JFileChooser(bml.guiFolderLocation + "/" + subFolder + "//");//XML file
        xmlFc.setDialogTitle("Enter the C2SIM Report XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;

        bml.xmlUrl = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl = URLHelper.getUserURL(
            bml.xuiFolderLocation + "/C2SIMReportView.xui");// Jaxfront XUI file
        bml.root = "MessageBody";
        bml.currentXmlString = bml.readAnXmlFile(xmlFc.getSelectedFile().getPath());
        
        // adjust for ASX report
        String schemaFileLocation = bml.c2simReportSchemaLocation;
        if(bml.currentXmlString.contains("Autonomous"))
            schemaFileLocation = bml.asxReportSchemaLocation;// ASX report schema XSD
        bml.xsdUrl = URLHelper.getUserURL(schemaFileLocation);
                    
        // load the report into JAXFront panel
        File loadFile = null;
        try{
            loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "C2SIM Report:" + loadFile.getName(),
                schemaFileLocation,
                "MessageBody");
        } catch(Exception e) {
            bml.printError("Exception in JaxFront loading report XML file:"+e);
            e.printStackTrace();
            return false;
        }
        // the C2SIM standard report can consist of multiple
        // <ReportContent> segments. Here we convert that format 
        // to single-segment so we can use the existing drawFomXML
        // to place its icon on the map

        // extract the parts that apply to all pieces
        String messageBody = bml.currentXmlString;
        String firstPart = bml.extractReportFirstPart(messageBody);
        if(firstPart == null)return false;
        int endingIndex = bml.findEndingIndex(messageBody);
        if(endingIndex < 0)return false;
        String endingPart = messageBody.substring(endingIndex);

        // extract the ReportContent parts in order
        // parse them and draw to map
        int startReportContent = firstPart.length();
        String reportContent = 
            bml.extractReportContent(messageBody,startReportContent); 
        while(reportContent != null){

            // make a new single-ReportContent report with uniqe ReportID
            String newReport = firstPart + reportContent +
                bml.uniqueEndingPart(messageBody,endingPart);

            // draw the map icon, using a generated reportId
            // Generate the swing GUI 
            if(!bml.drawFromXML(
                "default-context", 
                bml.xsdUrl, 
                null, 
                bml.xuiUrl, 
                bml.root, 
                "C2SIM Report",
                "MessageBody",
                (new String[]{"SubjectEntity","ReportingEntity","UUID",
                    "SideHostilityCode","PositionReportContent",
                    "ObservationReportContent","ReportID","Name",
                    "Latitude","Longitude","OperationalStatusCode"}),
                (new String[]{}),
                bml.c2simns,
                newReport,
                bml.c2simProtocolVersion,
                false
            ))return false;// stop here if error or duplicate ID


            // go on to the next ReportContent part
            startReportContent += reportContent.length();
            reportContent = 
                bml.extractReportContent(messageBody,startReportContent);
        }
        return true;
    }// end openReportFSC2SIM()

    /**
     * Send an Edited (optionally validated) C2SIM Report 
     * (XML Document) to the Web Services through the SBML Client
     * 
     * @since	1/30/2010
     */
    void pushReportC2SIM() {
    
        // check whether we have something to push
        if(bml.checkReportNotPushable())return;
        
        // input and output of XML push
        String pushResultString ="";
        String pushReportInputString ="";
        bml.reportBMLType = "C2SIM";

        // Pushing the C2SIM Report Query into the C2SIMClient
        pushResultString = 
            bml.ws.sendC2simREST(
                bml.currentXmlString,
                "REPORT",
                bml.c2simProtocolVersion);
        if(bml.debugMode)
            bml.printDebug("The C2SIM report push result is : " + pushResultString);
        String popupString = pushResultString;
        if(popupString.length() > 38)popupString = popupString.substring(30);
        bml.showInfoPopup( 
          pushResultString,
          "Report Push Server Message");
        
        // delete the pushed object
        bml.currentXmlString = null;

    } // end pushReportC2SIM()
    
    /**
     * saves C2SIM order to a file location chosen by user
     */
    void saveJaxFrontReportC2SIM() {
        JFileChooser xmlFc = new JFileChooser(bml.guiFolderLocation + "//");
        try {
            bml.saveJaxFront("Reports");
        } catch(Exception e) {
            bml.printError("Exception in saving XML file:"+e);
            bml.printError("Cause:" + e.getCause());
            e.printStackTrace();
            return;
        }
    }// end saveReportC2SIM()

} // end ReportC2SIM Class
