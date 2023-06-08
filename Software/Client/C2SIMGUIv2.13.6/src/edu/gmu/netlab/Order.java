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
import com.jaxfront.core.util.URLHelper;
import javax.swing.JFileChooser;

/**
 * Order Support Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 *              repurposed to CBML by JMP 2Aug2013 and 28Sep2021
 * @since	02/03/2010
 */ 
public class Order {
    private String root = null;
    private String documentType = "CBML Light Order";
    C2SIMGUI bml = C2SIMGUI.bml;
    
    // constructor
    public Order() 
    {
        bml.orderDomainName = "CBML";
        bml.generalBMLFunction = "CBML";
    }
	
    /**
     * Create a new BML Order (XML Document)
     * 
     * @since	4/17/2009
     */
    void newOrder() {
        
        bml.root = null;
        bml.documentTypeLabel.setText(documentType);
        bml.releaseXUICache();

        bml.xsdUrl = 
            URLHelper.getUserURL(bml.cbmlOrderSchemaLocation);
        bml.xmlUrl = null; //Empty XML
        bml.xuiUrl = 
            URLHelper.getUserURL(bml.xuiFolderLocation + "TabStyleOrder.xui");// XUI Style
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
                "New CBML Light Order",
                bml.cbmlOrderSchemaLocation,
                "CBMLOrder");
        } catch(Exception e) {
            bml.printError("Exception in loadJaxFront for CBML Light Order file:"+e);
            e.printStackTrace();
            return;
        }
                
    }// end newOrder() 
	
    /**
     * Open an existing BML Order (XML Document) From the file System
     *
     * @deprecated	Only for testing old schema.  To be removed when everything is OK.
     * @since	02/03/2010
     */
    void openOrder(String subFolder) {
        bml.releaseXUICache();
        bml.documentTypeLabel.setText(documentType);
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.cbmlOrderSchemaLocation);//Schema File XSD
            bml.xmlUrl = 
                URLHelper.getUserURL(//XML file
                    bml.guiFolderLocation + bml.delimiter + subFolder + bml.delimiter);
            bml.xuiUrl = 
                URLHelper.getUserURL(
                bml.xuiFolderLocation + "TabStyleOrder.xui");		// Jaxfront XUI file
		root = "CBMLOrder";
		
            // Generate the swing GUI
            bml.drawFromXML(
                "default-context", 
                bml.xsdUrl, 
                bml.xmlUrl, 
                bml.xuiUrl, 
                root, 
                documentType,
                "Task",
                (new String[]{
                  "AtWhere",
                  "RouteWhere",
                  "OID",
                  "PointLight",
                  "Line",
                  "Surface",
                  "CorridorArea",
                  "TaskeeWhoRef",
                  "SpecificRoute",
                  "DateTime"}),
                (new String[]{
                  "Latitude",
                  "Longitude"}),
                bml.cbmlns,
                null,
                bml.c2simProtocolVersion,
                true
              );
    }

    /**
     * Open an existing C2SIM Order (XML Document) from the file System
     * 
     * @since	02/03/2010
     */
    boolean openOrderFS(String subFolder, boolean showOnMap) {
        bml.releaseXUICache();
        bml.documentTypeLabel.setText(documentType);
        String xsdFileLocation = bml.cbmlOrderSchemaLocation;
        if(bml.debugMode)bml.printDebug("OPEN CBML FILE XSD:" + xsdFileLocation);
        bml.xsdUrl = URLHelper.getUserURL(xsdFileLocation);
        JFileChooser xmlFc = 
            new JFileChooser(bml.guiFolderLocation + "/" + subFolder + "//");//XML file
        xmlFc.setDialogTitle("Enter the CBML Order XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;
        bml.xmlUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = 
            xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl = 
            URLHelper.getUserURL(
        bml.xuiFolderLocation + "TabStyleOrder.xui");// Jaxfront XUI file
            bml.root = "CBMLOrder"; 
                
        // load the order into JAXFront panel
        try{
            File loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "CBML Order:" + loadFile.getName(),
                bml.cbmlOrderSchemaLocation,
                bml.root);
        } catch(Exception e) {
            bml.printError("Exception loading JaxFront:"+e);
            e.printStackTrace();
            return false;
        }
	
        // Generate the swing GUI only if not
        // autodisplaying CBML orders
        if(showOnMap)
            return bml.drawFromXML(
                "default-context", 
                bml.xsdUrl, 
                bml.xmlUrl, 
                bml.xuiUrl, 
                bml.root, 
                documentType,
                "Task",
                (new String[]{
                  "AtWhere",
                  "RouteWhere",
                  "OID",
                  "TaskID",
                  "PointLight",
                  "Line",
                  "Surface",
                  "CorridorArea",
                  "TaskeeWhoRef",
                  "SpecificRoute",
                  "DateTime"}),
                (new String[]{
                  "Latitude",
                  "Longitude"}),
                bml.cbmlns,
                null,
                bml.c2simProtocolVersion,
                true
              );
            return true;
	}
	
    /**
     * Send an Edited (optionally validated) BML Order (XML Document) to the Web Services through the SBML Client
     * 
     * @since	10/25/2009
     */
    void pushOrder() {

        // read XML from filename in xmlURL
        if(bml.checkOrderNotPushable())return;
        if(bml.debugMode)bml.printDebug("In CBML Order XML file:"+bml.xmlUrl);
        String pushOrderInputString = bml.readAnXmlFile(bml.xmlUrl);
        if(bml.debugMode)bml.printDebug("PUSH CBML XML:" + pushOrderInputString);
        if(pushOrderInputString.equals(""))return;

        // push Order through ClientLIb
        String pushResultString = 
            bml.ws.processBML(
                pushOrderInputString, 
                bml.orderDomainName, 
                "BML", 
                "CBML Order Push");
        bml.showInfoPopup( 
            pushResultString , 
            "CBML Order Push Message");         
        
        // clear the data
        bml.xmlUrl = null;
    
    }// end pushOrder()

}// end  class Order
