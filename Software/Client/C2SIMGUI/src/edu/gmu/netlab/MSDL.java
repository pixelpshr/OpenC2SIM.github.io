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

import com.jaxfront.core.schema.ValidationException;
import com.jaxfront.core.util.URLHelper;
import javax.swing.JFileChooser;
import edu.gmu.c4i.c2simclientlib2.*;

/**
 * MSDL Support Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 * @since	5/15/2011
 */ 
public class MSDL {
    
    C2SIMGUI bml = C2SIMGUI.bml;
    String documentType = "MSDL";	
	
    /**
     * Creates and displays a new Opord DOM from an XML file
     */
    void openMSDL_FS(String subFolder) { 
        bml.releaseXUICache();		
        bml.documentTypeLabel.setText(documentType);	
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.guiFolderLocation +
                "\\C2SIMGUI\\Schema\\MSDL\\MilitaryScenario_1.0.0.xsd"
        );
		
        // XML file
        JFileChooser xmlFc = 
            new JFileChooser(bml.guiFolderLocation + "/" + subFolder + "//");
        xmlFc.setDialogTitle("Enter the MSDL file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return;
        bml.xmlUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl = 
            URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.xuiUrl = 
            URLHelper.getUserURL(bml.guiFolderLocation +
                "\\C2SIMGUI\\XUIView\\MSDLView.xui");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        if(bml.debugMode)bml.printDebug(bml.xuiUrl.toString());
        bml.root = "MilitaryScenario";
        //C2SIMGUI.bml.opord_C2CoreRoot; //"C-BML OrderPush"; // NATO OPORD"OperationsOrder";
        bml.drawMSDL(bml.xsdUrl, bml.xmlUrl, bml.xuiUrl, bml.root);
    }

    /**
     * Create a new MSDL file
     * 
     * 11/28/2011
     * mababneh
     */
    void newMSDL() {
        bml.root = null;
        bml.documentTypeLabel.setText(documentType);
        bml.releaseXUICache();

        // Order Schema Schema/IBML-MSG048-09/IBMLOrderPushPulls.xsd
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.guiFolderLocation +
                "Schema\\MSDL\\MilitaryScenario_1.0.0.xsd"
        );
        bml.xmlUrl = null;		//Empty XML
        bml.xuiUrl = 
            URLHelper.getUserURL(bml.guiFolderLocation +
                    "\\Schema\\XUIView\\MSDLView.xui"
        );
 
        if(bml.debugMode)bml.printDebug(bml.xuiUrl.toString());
        bml.root = "MilitaryScenario";
        bml.initDom("default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            bml.root);
    }// end newMSDL()
    
    /**
     * Push MSDL file to web service
     * 
     * 11/28/2011
     * mababneh
     */
    void pushMSDL() {		
        String pushResultString ="";	 // String to hold the result of the execution of the SBML XML query
        String pushOrderInputString = "";// String to hold the input to the SBML XML query
        try {
            // assign the text of the XML document to pushOrderInputString
            pushOrderInputString = bml.currentDom.serialize().toString();
        } catch (ValidationException e) {
            e.printStackTrace();
        }		

        // Running the SBML Query through the SBMLClient
        // ClientLib - library implementation
        C2SIMClientREST_Lib c2simClient = new C2SIMClientREST_Lib();
        c2simClient.setHost(bml.serverName);	
        if(bml.debugMode)bml.printDebug("Starting the Web Service query ");

        // call the bmlRequest method to execute the query
        // TODO: unify this with WebServices
        try{
            c2simClient.setDomain(bml.orderDomainName);
            c2simClient.setProtocol(bml.orderDomainName);
            c2simClient.setSubmitter(bml.submitterID);      
            pushResultString = c2simClient.bmlRequest(pushOrderInputString);
        }
        catch(C2SIMClientException bce){
            bml.printError("C2SIMClientException in pushMSDL:" + 
                bce.getMessage() + " cause:" + bce.getCauseMessage());
        }
        if(bml.debugMode)bml.printDebug("The query result is : " + pushResultString);  
        bml.showInfoPopup( 
            pushResultString.substring(38) , 
            "Order Push Message");	       
           
    }// end pushMSDL()
        
} // end of class MSDL
