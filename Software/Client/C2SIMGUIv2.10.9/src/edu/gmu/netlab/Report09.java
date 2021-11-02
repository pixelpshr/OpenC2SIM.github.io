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
import javax.swing.JFileChooser;
import com.jaxfront.core.util.URLHelper;

/**
 * Report Support Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 *              amd JMP 28Sep2021
 * @since	12/1/2011
 */ 
public class Report09 {
	
    String root="";
    C2SIMGUI bml = C2SIMGUI.bml;
        
    // constructor
    public Report09() 
    {
        bml.orderDomainName = "IBML";
        bml.generalBMLFunction = "IBML";
    }
	
    /**
     * Create a new IBML09 Report (XML Document)
     * 
     * @mababneh
     */
    void newReport(String reportType) {	
        bml.releaseXUICache();
        bml.reportBMLType = "IBML";
		
        //set xsdUrl and xuiUrl
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.ibml09ReportSchemaLocation);
        root = bml.setUrls(reportType);	
        bml.bmlDocumentType = "IBML09 Report";
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
                "New IBML09 Report",
                bml.ibml09OrderSchemaLocation,
                "BMLReport");
        } catch(Exception e) {
            bml.printError("Exception loading to JaxFront:"+e);
            e.printStackTrace();
            return;
        }
        
    }// end newReport()
	
	
    /**
     * Open an existing IBML09 Report (XML Document) from The File System
     * 
     * @since	
     */
    boolean openReportFS_General09(String subFolder) throws IOException {		
        bml.releaseXUICache();
        bml.reportBMLType = "IBML";
        bml.bmlDocumentType = "IBML09 Report";	
        bml.documentTypeLabel.setText(bml.bmlDocumentType);
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.ibml09ReportSchemaLocation);//Schema File XSD

        JFileChooser xmlFc = 
            new JFileChooser(bml.guiFolderLocation + "/" + subFolder + "//");//XML file
        xmlFc.setDialogTitle("Enter the IBML09 Report XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;

        bml.xmlUrl = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl = URLHelper.getUserURL(
            bml.xuiFolderLocation + "/GeneralStatusReportView09.xui");// Jaxfront XUI file
        bml.root = "BMLReport";
        bml.currentXmlString = bml.readAnXmlFile(xmlFc.getSelectedFile().getPath());
        
        // load the report into JAXFront panel
        try{
            File loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "IBML09 Report:" + loadFile.getName(),
                bml.ibml09ReportSchemaLocation,
                "BMLReport");
        } catch(Exception e) {
            bml.printError("Exception in loading report XML file:"+e);
            e.printStackTrace();
            return false;
        }
		
        //Generate the swing GUI
        return bml.drawFromXML(
            "default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            bml.root, 
            bml.bmlDocumentType,
            "Report",
            (new String[]{"GeneralStatusReport","ReportID","UnitID","Hostility"}),
            (new String[]{"Latitude","Longitude"}),
            bml.ibmlns,
            null,
            bml.c2simProtocolVersion,
            true
        );
    }// end openReportFS_General09()
	
    /**
     * Open an existing IBML09 Report (XML Document) from The File System
     * 
     * @since	
     */
    boolean openReportFS_Task09() throws IOException {		
        bml.releaseXUICache();
        bml.reportBMLType = "IBML";
        bml.bmlDocumentType = "IBML09 Report";	
        bml.documentTypeLabel.setText(bml.bmlDocumentType);
        bml.xsdUrl = 
        URLHelper.getUserURL(bml.ibml09ReportSchemaLocation);//Schema File XSD
        JFileChooser xmlFc = new JFileChooser(bml.guiFolderLocation + "//");//XML file
        xmlFc.setDialogTitle("Enter the IBML09 Report XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;
        bml.xmlUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = 
        URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl = // Jaxfront XUI file
        URLHelper.getUserURL(bml.xuiFolderLocation + "/TaskStatusReportView09.xui");
        bml.root = "BMLReport";	

        // Generate the swing GUI
        return bml.drawFromXML(
            "default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            bml.root, 
            bml.bmlDocumentType,
            "GeneralStatusReport",
            (new String[]{"UnitID","Hostility"}),
            (new String[]{"Latitude","Longitude"}),
            bml.ibmlns,
            null,
            bml.c2simProtocolVersion,
            true
        );
    }

    /**
     * Send an Edited (optionally validated) IBML09 Report 
     * (XML Document) to the Web Services through the SBML Client
     * 
     * @since	1/30/2010
     */
    void pushReport09() {
        
        // check wehther we have something to push
        if(bml.checkReportNotPushable())return;
        
        // input and output of SBML XML query
        String pushResultString ="";
        String pushReportInputString ="";
        bml.reportBMLType = "IBML";
        pushReportInputString = bml.currentXmlString;

        // Running the SBML Query through the SBMLClient
        pushResultString = 
            bml.ws.processBML(
                pushReportInputString, 
                bml.orderDomainName, 
                "BML", 
                "IBML Report Push");
        if(bml.debugMode)bml.printDebug("The query result is : " + pushResultString);
        String popupString = pushResultString;
        if(popupString.length() > 38)popupString = popupString.substring(30);
        bml.showInfoPopup( 
            popupString,
            "Report Push Server Message");
        
        // delete the pushed object
        bml.currentXmlString = null;

    }    // end pushReport()
    
} // End of Report Class
