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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import edu.gmu.c4i.c2simclientlib2.*;


/**
 * Web Services Support Methods

 These methods support the C2SIMGUI object
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 * @since	5/11/2011
 * 
 * @author	Eric Popelka, C4I Center, George Mason University
 * @since	6/29/2011
 */ 
public class Webservices {
    C2SIMGUI bml = C2SIMGUI.bml;
    String root;
    private static final String xmlPreamble = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
  
    /**
     *  constructor
     */
    public Webservices(C2SIMGUI bmlRef){
        bml = bmlRef;
    }
    
    /**
     * instantiate old or new C2SIMClientREST_Lib
     * @param performative is the FIPA performative
     */   
    C2SIMClientREST_Lib newRESTLib(String performative){
        
        try {
            C2SIMClientREST_Lib c2simClient = 
                new C2SIMClientREST_Lib(
                    "C2SImGUI@"+bml.localAddress,
                    bml.serverName,
                    performative,
                    bml.c2simProtocolVersion);
            c2simClient.setProtocol(bml.c2simProtocol);
            c2simClient.setPath(bml.c2simPath);
            c2simClient.setHost(bml.serverName);
            c2simClient.setPort(bml.restPort);
            return c2simClient;
        }
        catch(C2SIMClientException cce){
            bml.printError("C2SIMClientException in newRESTLib:"  +
                cce.getMessage() + " cause:" + cce.getCauseMessage());
            return null;
        }

    }// end newRESTLib()
    
    /**
     * send a C2SIM REST transaction and return the response
     */
    String sendC2simREST(
        String xmlString, 
        String performative, 
        String protocolVersion){
  
        // start REST connection using performative for Report
        if(bml.debugMode)bml.printDebug(
            "Starting the sendC2simREST Web Service push to:" +
            bml.serverName +
            " protocolVersion = " + protocolVersion);
        if(bml.submitterID.length() == 0) {
            bml.showInfoPopup( 
                "cannot push C2SIM Report - submitterID required", 
                "C2SIM Report Push Message");
            return "";
        }
        
        // open connection to REST server
        C2SIMClientREST_Lib restClient = newRESTLib(performative);
        try {          
            restClient.setSubmitter(bml.submitterID);
            restClient.setDomain(bml.orderDomainName);
            restClient.getC2SIMHeader().setProtocolVersion(protocolVersion);
            restClient.getC2SIMHeader().setFromSendingSystem(bml.submitterID);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        bml.conversationID = restClient.getC2SIMHeader().getConversationID();

        // execute the HTTP transaction
        String response = "";
        try {
            // call the clientLib to execute the query
            response = restClient.bmlRequest(xmlString);
        } catch (Exception e2) {
            if(!bml.runningServerTest)
                bml.showErrorPopup( 
                    "Exception in sendC2simREST:" + e2.getMessage() +
                        " cause:" + e2.getCause(), 
                    "sendC2simREST error");
            bml.printError("Exception in bmlRequest:" + e2.getMessage() +
                " cause:" + e2.getCause());
            e2.printStackTrace();
            response = "";
        }
        return response;
        
    }// end sendC2simREST()
	
    /**
     *  push an XML document string into a C2SIM server
     *  with no C2SIM header (for legacy CBML and IBML)
     */
    public String processBML(
        String xml, 
        String domain,
        String bmlType, 
        String bmlDescription) {
        
        if(bml.debugMode)bml.printDebug(
            "Starting bmlRequest Web Service query to:" +
            bml.serverName);

        // Sanitize the input
        Pattern p = Pattern.compile("<\\?\\s*jaxfront\\s*.*?>\r?\n?");
        Matcher m = p.matcher(xml);
        String result = "Error";
        if(m == null) {
            if(bml.debugMode)bml.printDebug(
                "Cannot make Matcher in Webservices.processBML");
            return result;
        }
        xml = m.replaceFirst("");
        C2SIMClientREST_Lib restClient = null;
        if(bml.debugMode)bml.printDebug("bmlType:" + bmlType);
        
        // instantiate and configure BML REST client
        restClient = new C2SIMClientREST_Lib();
        restClient.setHost(bml.serverName);
        restClient.setPort(bml.restPort);
        restClient.setSubmitter(bml.submitterID);
        restClient.setDomain(domain);

        try {
            // execute the push
            result = restClient.bmlRequest(xml);
        } catch (C2SIMClientException e2) {
            if(!bml.runningServerTest)
                bml.showErrorPopup( 
                    "C2SIMClientException in bmlRequest:" + e2.getMessage() +
                        " cause:" + e2.getCause(), 
                    bmlDescription);
            bml.printError("C2SIMClientException in bmlRequest:" + e2.getMessage() +
                " cause:" + e2.getCause());
            e2.printStackTrace();
            result = "";
        }
        if(bml.debugMode)bml.printDebug("The bmlRequest query result is : " + result); 
        return result;
        
    }// end processBML()

} // End of Webservices class
