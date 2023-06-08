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

/**
 *
 * @author mpullen
 */
import java.io.*;
import javax.swing.JFileChooser;
import edu.gmu.c4i.c2simclientlib2.*;
import com.jaxfront.core.util.URLHelper;

/**
 * OrderC2SIM  Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 * @since	11/28/2011
 */ 
public class OrderC2SIM {
    
    C2SIMGUI bml = C2SIMGUI.bml;
    
    String documentType = "C2SIM Order";
        
    // constructor
    public OrderC2SIM() 
    {
        bml.orderDomainName = "C2SIM";
        bml.generalBMLFunction = "C2SIM";
    }
	
    /**
     * Create a new C2SIM Order (XML Document)
     * 
     * mababneh 11/28/2011 + mpullen 06/24/2019
     */
    void newOrderC2SIM() {
        bml.releaseXUICache();
        bml.root = "MessageBody";
        bml.documentTypeLabel.setText(documentType);
        bml.releaseXUICache();
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.c2simOrderSchemaLocation);
        bml.xmlUrl = null;		//Empty XML
        bml.xuiUrl = 
            URLHelper.getUserURL(
                bml.xuiFolderLocation + "/TabStyleOrderC2SIM.xui");// XUI Style
                bml.initDom(
                    "default-context", 
                    bml.xsdUrl, 
                    bml.xmlUrl, 
                    bml.xuiUrl, 
                    bml.root);           
               
        // load the order into JaxFront panel
        try{
            bml.loadJaxFront(
                null,
                "New C2SIM Order",
                bml.c2simOrderSchemaLocation,
                "MessageBody");
        } catch(Exception e) {
            bml.printError("Exception in loadJaxFront for C2SIM Order file:"+e);
            e.printStackTrace();
            return;
        }
                    
    }// end newOrderC2SIM()

    /**
     * Open an existing C2SIM Order (XML Document) from file system
     * 
     * mababneh 11/28/2011 mpullen 03/27/18 and 09/27/21
     * 
     * returns false if order opened has duplicate ID; true otherwise
     */
    boolean openOrderFSC2SIM(String subFolder, boolean showOnMap) {
        bml.releaseXUICache();	
        bml.documentTypeLabel.setText(documentType);
        bml.xsdUrl = //Schema File XSD
        URLHelper.getUserURL(bml.c2simOrderSchemaLocation);	
        JFileChooser xmlFc = //XML file
            new JFileChooser(
                bml.guiFolderLocation + bml.delimiter + subFolder + bml.delimiter);	
        xmlFc.setDialogTitle("Enter the C2SIM Order XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;
        bml.xmlUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl = // Jaxfront XUI file
            URLHelper.getUserURL(bml.xuiFolderLocation + "/TabStyleOrderC2SIM.xui");
        bml.root = "MessageBody";
        
        // adjust for ASX order 
        bml.currentXmlString = bml.readAnXmlFile(xmlFc.getSelectedFile().getAbsolutePath());
        String schemaFileLocation = bml.c2simOrderSchemaLocation;
        String taskTag = "ManeuverWarfareTask";
        if(bml.currentXmlString.contains("Autonomous")){
            schemaFileLocation = bml.asxOrderSchemaLocation;// ASX order schema XSD
            taskTag = "AutonomousSystemManeuverWarfareTask";
        }
        bml.xsdUrl = URLHelper.getUserURL(schemaFileLocation);
        
        // load the order into JAXFront panel
        try{
            File loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "C2SIM Order:" + loadFile.getName(),
                schemaFileLocation,
                bml.root);
        } catch(Exception e) {
            bml.printError("Exception in C2SIM Order file:"+e);
            e.printStackTrace();
            return false;
        }

        // Generate the swing GUI only if not
        // autodisplaying C2SIM orders
        if(showOnMap)
            return bml.drawFromXML(
              "default-context", 
              bml.xsdUrl, 
              bml.xmlUrl, 
              bml.xuiUrl, 
              bml.root, 
              documentType,
              "ManeuverWarfareTask",
              (new String[]{
                "PerformingEntity",
                "StartTime",
                "TemporalAssociationWithAction",
                "UUID",
                "MapGraphicID",
                "Name"}),
              (new String[]{
                "Latitude",
                "Longitude"}),
              bml.c2simns,
              null,
              bml.c2simProtocolVersion,
              true
            );
        return true;
    }// end openOrderFSC2SIM()
	
    /**
     * Push an C2SIM Order (XML Document)
     * 
     * mpullen 4/1//2018
     */
    void pushOrderC2SIM() {
        // open connection to REST server
        if(bml.checkOrderNotPushable())return;
        if(bml.submitterID.length() == 0) {
            bml.showInfoPopup( 
                "cannot push C2SIM Order - submitterID required", 
                "C2SIM Order Push Message");
            return;
        }

        // set parameters and send C2SIM message
        String pushResultString =
            bml.ws.sendC2simREST(
                bml.currentXmlString,
                "ORDER",
                bml.c2simProtocolVersion);
        bml.showInfoPopup( 
            pushResultString , 
            "C2SIM Order Push Message");
        
        // clear the data
        bml.xmlUrl = null;
    
    }// end pushOrderC2SIM()
    
    /**
     * saves C2SIM order to a file location chosen by user
     */
    void saveJaxFrontOrderC2SIM() {
        try {
            bml.saveJaxFront("Orders");
        } catch(Exception e) {
            bml.printError("Exception in saving XML file:"+e);
            e.printStackTrace();
            return;
        }
    }// end saveOrderC2SIM()
    
}// end class OrderC2SIM
