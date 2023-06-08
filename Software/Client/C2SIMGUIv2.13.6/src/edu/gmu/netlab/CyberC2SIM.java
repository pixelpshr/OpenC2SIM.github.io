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
import javax.swing.JFileChooser;
import com.jaxfront.core.util.URLHelper;
import java.io.*;
import edu.gmu.c4i.c2simclientlib2.*;

public class CyberC2SIM {

    C2SIMGUI bml = C2SIMGUI.bml;
    String conversationID = "";
    private String bmlDocumentType = "";
    private String serverStatus = "UNKNOWN";
    private String documentType = "C2SIM Cyber";

    // constructor
    public CyberC2SIM() {
        bml.orderDomainName = "C2SIM";
        bml.generalBMLFunction = "C2SIM";
    }

    /**
     * make a new C2SIM Cyber document
     */
    void newCyberC2SIM() {
        bml.root = "Cyber_Event";
        bmlDocumentType = "New C2SIM Cyber";
        bml.documentTypeLabel.setText(bmlDocumentType);
        bml.releaseXUICache();

        bml.xsdUrl
                = URLHelper.getUserURL(bml.c2simCyberSchemaLocation);
        bml.xmlUrl = null;		//Empty XML
        bml.xuiUrl
                = URLHelper.getUserURL(
                        bml.xuiFolderLocation + "/TabStyleOrderC2SIM.xui");// XUI Style
        bml.initDom(
                "default-context",
                bml.xsdUrl,
                bml.xmlUrl,
                bml.xuiUrl,
                bml.root);

        // load the Cyber document into JAXFront panel
        try {
            bml.loadJaxFront(
                null,
                "C2SIM Cyber",
                bml.c2simCyberSchemaLocation,
                "Cyber_Event");
        } catch (Exception e) {
            return;
        }

    }// end newCyberC2SIM()

    /**
     * returns conversationID
     */
    String getConversationID() {
        return conversationID;
    }

    /**
     * load a new C2SIM Cyber document from file system
     */
    void openCyberFSC2SIM(String subFolder) {
        bml.releaseXUICache();
        bml.documentTypeLabel.setText(documentType);
        bml.xsdUrl
            = //Schema File XSD
            URLHelper.getUserURL(bml.c2simOrderSchemaLocation);
        JFileChooser xmlFc
            = //XML file
            new JFileChooser(bml.guiFolderLocation + "/" + subFolder + "//");
        xmlFc.setDialogTitle("Enter the Cyber XML file name");
        xmlFc.showOpenDialog(bml);
        if (xmlFc.getSelectedFile() == null) {
            return;
        }
        bml.xmlUrl
            = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());
        bml.tmpUrl
            = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        bml.tmpFileString = xmlFc.getSelectedFile().toString() + "(tmp)";
        bml.xuiUrl
            = // Jaxfront XUI file
            URLHelper.getUserURL(bml.xuiFolderLocation + "/TabStyleOrderC2SIM.xui");
        bml.root = "Cyber_Event";

        // load the Cyber document into JAXFront panel
        try {
            bml.loadJaxFront(
                xmlFc.getSelectedFile(),
                "C2SIM Cyber",
                bml.c2simCyberSchemaLocation,
                "Cyber_Event");
        } catch (Exception e) {
            return;
        }
    }// end openCyberFSC2SIM()

    /**
     * push loaded C2SIM Cyber document to REST WS
     */
    String pushCyberC2SIM() {
        // check whether Cyber was loaded
        if (bml.xmlUrl == null) {
            bml.showInfoPopup(
                "cannot push - no CyberAttack has been loaded",
                "Cyber Push Message");
            return "cannot push - no document has been loaded";
        }

        // open connection to REST server
        if (bml.submitterID.length() == 0) {
            bml.showInfoPopup(
                "cannot push C2SIM Cyber - submitterID required",
                "C2SIM Cyber Push Message");
            return "cannot push C2SIM Cyber - submitterID required";
        }
        C2SIMClientREST_Lib c2simClient;
        try {
            // start REST connection using performative for Cyber
            c2simClient = bml.ws.newRESTLib("INFORM");
            c2simClient.setProtocol("cyber");
            c2simClient.setHost(bml.serverName);
            if(bml.debugMode)bml.printDebug("C2SIM Order/Cyber Host:" + bml.serverName);
            c2simClient.setSubmitter(bml.submitterID);
            if(bml.debugMode)bml.printDebug("C2SIM Order/Cyber Submitter:" + bml.submitterID);
            c2simClient.setPath("C2SIMServer/cyber");
        }
        catch(C2SIMClientException cce){
            bml.printError("C2SIMClientException in newRESTLib:" +
                cce.getMessage() + " cause:" + cce.getCauseMessage());
            return null;
        }

        // XML is not yet in JaxFront - read it from file
        String pushCyberInputString = bml.readAnXmlFile(bml.xmlUrl);
        
        // display and send the input
        if(bml.debugMode)
            bml.printDebug("PUSH C2SIM CYBER XML:" + pushCyberInputString);
        conversationID = c2simClient.getC2SIMHeader().getConversationID();
        String pushCyberResponseString = "";
        try {
            pushCyberResponseString
                = c2simClient.bmlRequest(pushCyberInputString);
        } catch (C2SIMClientException bce) {
            bml.showErrorPopup(
                "exception pushing C2SIM Cyber:"
                + bce.getMessage() + " cause:" + bce.getCauseMessage(),
                "C2SIM Cyber Push Message");
            return pushCyberResponseString;
        }

        // display result
        bml.showInfoPopup(
            pushCyberResponseString,
            "C2SIM Cyber Push Message");

        // clear the data and return
        bml.xmlUrl = null;
        return pushCyberResponseString;

    }//end pushCyberC2SIM()

    /**
     * saves C2SIM iCyber to a file location chosen by user
     */
    void saveJaxFrontCyberC2SIM() {
        try {
            bml.saveJaxFront("Cyber");
        } catch (Exception e) {
            bml.printError("Exception in saving XML file:" + e);
            e.printStackTrace();
            return;
        }
    }// end saveJaxFrontCyberC2SIM()

}// end class CyberC2SIM
