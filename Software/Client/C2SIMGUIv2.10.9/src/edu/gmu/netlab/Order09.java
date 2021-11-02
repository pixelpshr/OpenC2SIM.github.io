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
 * Order09  Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 *              and JMP 9/28/2021
 * @since	11/28/2011
 */ 
public class Order09 {
    
    private String bmlDocumentType = "";
    C2SIMGUI bml = C2SIMGUI.bml;
    
    // constructor
    public Order09() 
    {
        bml.orderDomainName = "IBML";
        bml.generalBMLFunction = "IBML";
    }
	
    /**
     * Create a new IBML09 Order (XML Document)
     * 
     * mababneh 11/28/2011
     */
    void newOrder09() {
        bml.root = "OrderPushIBML";
        bmlDocumentType = "IBML09 Order";
        bml.documentTypeLabel.setText(bmlDocumentType);
        bml.releaseXUICache();

        // Order Schema IBML-MSG048-09/IBMLOrderPushPulls.xsd
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.ibml09OrderSchemaLocation);
        bml.xmlUrl = null;		//Empty XML
        bml.xuiUrl = URLHelper.getUserURL(
            bml.xuiFolderLocation + "/TabStyleOrder09.xui");// XUI Style
        bml.initDom(
            "default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            bml.root);
        
        // load the order into JAXFront panel
        try{
            bml.loadJaxFront(
                null,
                "New IBML Order",
                bml.ibml09OrderSchemaLocation,
                "OrderPushIBML");
        } catch(Exception e) {
            bml.printError("Exception in loadJaxFront for IBML Order file:"+e);
            e.printStackTrace();
            return;
        }

    }// end newOrder09()
	
    /**
     * Open an existing IBML09 Order (XML Document)
     * 
     * mababneh 11/28/2011
     */
    boolean openOrderFS09(String subFolder, boolean showOnMap) {
        bml.releaseXUICache();
        bml.bmlDocumentType = "IBML09 Order";	
        bml.documentTypeLabel.setText(bml.bmlDocumentType);
        bml.xsdUrl = //Schema File XSD
            URLHelper.getUserURL(bml.ibml09OrderSchemaLocation);	
        
        // chhose a file to open
        JFileChooser xmlFc = //XML file
            new JFileChooser(
                bml.guiFolderLocation + bml.delimiter + subFolder + bml.delimiter);	
        xmlFc.setDialogTitle("Enter the IBML09 Order XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;
        
        bml.xmlUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl = // Jaxfront XUI file
            URLHelper.getUserURL(bml.xuiFolderLocation + "/TabStyleOrder09.xui");
        bml.root = "OrderPushIBML";
                
        // load the order into JAXFront panel
        try{
            File loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "IBML09 Order:" + loadFile.getName(),
                bml.ibml09OrderSchemaLocation,
                bml.root);
        } catch(Exception e) {
            bml.printError("Exception loading JaxFront:"+e);
            e.printStackTrace();
            return false;
        }
	
        // Generate the swing GUI only if not
        // autodisplaying IBML09 orders
        if(showOnMap)
            return bml.drawFromXML(
                "default-context", 
                bml.xsdUrl, 
                bml.xmlUrl, 
                bml.xuiUrl, 
                bml.root, 
                bml.bmlDocumentType,
                "GroundTask",
                (new String[]{
                    "UnitID",
                    "DateTime",
                    "WhereID",
                    "WhereClass",
                    "WhereCategory",
                    "WhereLabel",
                    "WhereQualifier"}),
                (new String[]{
                  "Latitude",
                  "Longitude"}),
                bml.ibmlns,
                null,
                null,
                true
            );
        return true;
    }// end openOrderFS09()
	
    /**
     * Push an IBML09 Order (XML Document)
     * 
     * mababneh 11/28/2011
     */
    void pushOrder09() {
        
        // read XML from filename in xmlURL
        if(bml.checkOrderNotPushable())return;
        if(bml.debugMode)bml.printDebug("In IBML09 Order XML file:"+bml.xmlUrl);
        String pushOrderInputString = bml.readAnXmlFile(bml.xmlUrl);
        if(bml.debugMode)bml.printDebug("PUSH IBBML09 XML:" + pushOrderInputString);
        if(pushOrderInputString.equals(""))return;

        // Running the SBML Query through the SBMLClient
        String pushResultString = 
            bml.ws.processBML(
                pushOrderInputString, 
                bml.orderDomainName, 
                "BML", 
                "IBML Order Push");
        bml.showInfoPopup( 
            pushResultString , 
            "IBML09 Order Push Message");
        
        // clear the data
        bml.xmlUrl = null;
    
    }// end pushOrder09()
    
}//end class Order09
