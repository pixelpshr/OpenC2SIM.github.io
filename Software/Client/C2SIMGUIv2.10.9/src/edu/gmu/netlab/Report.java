/*----------------------------------------------------------------*
|   Copyright 2009-2021 Networking and Simulation Laboratory      |
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
import java.util.Scanner;
import javax.swing.JFileChooser;
import com.jaxfront.core.util.URLHelper;

/**
 * Report Support Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 *              amd mpullen 28Sep2021
 * @since	10/28/2010
 */ 
public class Report {
	
    String root="";
    C2SIMGUI bml = C2SIMGUI.bml;
      
    // constructor
    public Report() 
    {
        bml.orderDomainName = "CBML";
        bml.generalBMLFunction = "CBML";
    }
	
    /**
     * Create a new C2SIM Report (XML Document)
     * 
     * @since	10/17/2009
     */
    void newReport(String reportType) {	
        bml.releaseXUICache();
        bml.reportBMLType = "CBML";
		
        //set xsdUrl and xuiUrl
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.cbmlReportSchemaLocation);
        root = bml.setUrls(reportType);	
        bml.bmlDocumentType = "CBML Light Report";
        bml.documentTypeLabel.setText(bml.bmlDocumentType);
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
                "New CBML Light Report",
                bml.cbmlReportSchemaLocation,
                "Report");
        } catch(Exception e) {
            bml.printError("Exception loading to JaxFront:"+e);
            e.printStackTrace();
            return;
        }
    }
	
    /**
     * Open an existing CBML Light Report (XML Document) from The File System
     * 
     * @deprecated	Uses the old schema testing. To be removed when done
     * @since	10/15/2009
     */
    void openReport() {	
        bml.releaseXUICache();
        bml.reportBMLType = "CBML";
        bml.bmlDocumentType = "CBML Light Report";
        bml.documentTypeLabel.setText(bml.bmlDocumentType);

        //XML file
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.cbmlReportSchemaLocation);
        bml.xmlUrl = 
            URLHelper.getUserURL(bml.guiFolderLocation + "Reports/CBML_Reports.xml");
        bml.xuiUrl = 
            URLHelper.getUserURL(bml.xuiFolderLocation + "TaskStatusReportView09.xui");
        bml.bmlDocumentType = "CBML Light Report";
        root = "MultipleReportPull";
		
        // Generate the swing GUI
        bml.drawFromXML(
            "default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            root, 
            bml.bmlDocumentType,
            "Report",
            (new String[]{"GeneralStatusReport","UnitID","Hostility"}),
            (new String[]{"Latitude","Longitude"}),
            bml.cbmlns,
            null,
            bml.c2simProtocolVersion,
            true
        );
    }
	
    /**
     * Open an existing CBML Light Report (XML Document) from The File System
     * 
     * @since	02/03/2009
     */
    boolean openReportFS(String subFolder) throws IOException {		
        bml.releaseXUICache();
        String reportString ="";
        String reportType ="";
        bml.reportBMLType = "CBML";
        bml.bmlDocumentType = "CBML Light Report";
        bml.documentTypeLabel.setText(bml.bmlDocumentType);
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.cbmlReportSchemaLocation);

        // XML file
        JFileChooser xmlFc = 
            new JFileChooser(bml.guiFolderLocation + "/" + subFolder + "//");
        xmlFc.setDialogTitle("Open CBML Light Report XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;            
        bml.xmlUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl = // Jaxfront XUI file
        URLHelper.getUserURL(
            bml.xuiFolderLocation + 
                "/GeneralStatusReportView09.xui");// Jaxfront XUI file
        bml.root = "CBMLReport";
        bml.currentXmlString = 
            bml.readAnXmlFile(xmlFc.getSelectedFile().getPath());

        // determine report type by scanning XML file
        File reportFile = new File(bml.xmlUrl.getFile());
        Scanner reportpullScanner = new Scanner(reportFile);
        while (reportpullScanner.hasNext()){
          reportString = reportpullScanner.next();
          reportType = bml.getReportDocumentType(reportString);
          if(reportType != "UNKNOWN")break;
        }
        bml.bmlDocumentType = "CBML Light Report";

        //Step 6 the report type now is known
        if(bml.debugMode)bml.printDebug("======== Report Type is  : " + reportType);
        bml.documentTypeLabel.setText(reportType);
        
        // Step 7 : Display the Report Document
 
        // load the report into JAXFront panel
        try{
            File loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "CBML Light Report:" + loadFile.getName(),
                bml.cbmlReportSchemaLocation,
                "CBMLReport");
        } catch(Exception e) {
            bml.printError("Exception in loading report XML file:"+e);
            e.printStackTrace();
            return false;
        }

        // report will be posted to map when transmitted by server
        // Generate the swing GUI
        return bml.drawFromXML(
            "default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            bml.root, 
            bml.bmlDocumentType,
            "GeneralStatusReport",
            (new String[]{"GeneralStatusReport","OID","UnitID","Hostility"}),
            (new String[]{"Latitude","Longitude"}),
            bml.cbmlns,
            null,
            bml.c2simProtocolVersion,
            true
        );
    }// end openReportFS()

    /**
     * Send an Edited (optionally validated) C2SIM Report (XML Document) to the Web Services through the SBML Client
     * 
     * @since	1/30/2010
     */
    void pushReport() {
        
        // check wehther we have something to push
        if(bml.checkReportNotPushable())return;
        
        // input and output of SBML XML query
        String pushResultString ="";	
        String pushReportInputString ="";
        bml.reportBMLType = "CBML";

        pushReportInputString = bml.currentXmlString;

        // Running the SBML Query through the SBMLClient
        pushResultString = 
            bml.ws.processBML(
                pushReportInputString, 
                bml.orderDomainName, 
                "BML", 
                "IBML Report Push");

        if(bml.debugMode)bml.printDebug("The query result is : " + pushResultString);  
        bml.showInfoPopup( 
            pushResultString.substring(38), 
            "Report Push Server Message");
        
        // delete the pushed object
        bml.currentXmlString = null;
    
    } // end pushReport()

} // end of Report class
