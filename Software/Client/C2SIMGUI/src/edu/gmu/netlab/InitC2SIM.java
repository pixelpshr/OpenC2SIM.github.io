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
 * @author mpullen
 */
import javax.swing.JFileChooser;
import com.jaxfront.core.util.URLHelper;
import java.io.*;
import edu.gmu.c4i.c2simclientlib2.*;
import static edu.gmu.netlab.MilOrg.bml;

/**
 * build or reads a new C2SIM Initialize document
 */

public class InitC2SIM {
    
    C2SIMGUI bml = C2SIMGUI.bml;
    private String documentType = "InitC2SIM";
    private String serverStatus = "UNKNOWN";
    C2SIMHeader c2simHeader = new C2SIMHeader();
    
    // constructor
    public InitC2SIM() 
    {
        bml.orderDomainName = "C2SIM";
        bml.generalBMLFunction = "C2SIM";
    }
    
    /**
     * make a new C2SIM Initialize document
     */
    void newInitC2SIM()
    {
        bml.releaseXUICache();
        bml.root = "MessageBody";
        bml.documentTypeLabel.setText("New InitC2SIM");
        bml.xsdUrl = 
            URLHelper.getUserURL(bml.c2simInitSchemaLocation);
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
                        
        // load the initialize document into JAXFront panel
        try{
            bml.loadJaxFront(
                null,
                "C2SIM Initialize",
                bml.c2simInitSchemaLocation,
                "MessageBody");
        } catch(Exception e) {return;}
               
    }// end newInitC2SIM()
    
    /**
     * load a new C2SIM Initialize document from file system
     * returns true if successful
     */
    boolean openInitFSC2SIM(String subFolder)
    {
        bml.releaseXUICache();	
        bml.documentTypeLabel.setText(documentType);
        bml.xsdUrl = //Schema File XSD
            URLHelper.getUserURL(bml.c2simInitSchemaLocation);	
        JFileChooser xmlFc = //XML file
            new JFileChooser(bml.guiFolderLocation + "/" + subFolder + "//");	
        xmlFc.setDialogTitle("Enter the Initialization XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null)return false;

        // load the initialize document into JAXFront panel
        try{
            File loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "C2SIM Object Initialization:" + loadFile.getName(),
                bml.c2simInitSchemaLocation,
                "MessageBody");
        } catch(Exception e) {
            bml.printError("Exception loading JaxFront:"+e);
            e.printStackTrace();
            return false;
        }
        return true;
        
    }// end openInitFSC2SIM()
    
    /**
     * push loaded C2SIM Initialize document to REST WS
     */
    String pushInitC2SIM()
    {
        // check whether init was loaded
        if(bml.xmlUrl == null) {
            bml.showInfoPopup(
                "cannot push - no C2SIMInitialization has been loaded",
                "Init Push Message");
            return "cannot push - no document has been loaded";
        }
    
        // open connection to REST server
        if(bml.submitterID.length() == 0) {
            bml.showInfoPopup( 
                "cannot push C2SIM Init - submitterID required", 
                "C2SIM Init Push Message");
            return "cannot push C2SIM Init - submitterID required";
        }
	
        // should push C2SIM from memory but for now use a file
        FileReader xmlFile;
        String pushInitInputString = "";
        try{
          xmlFile=new FileReader(new File(bml.xmlUrl.getFile()));
          int charBuf; 
          while((charBuf = xmlFile.read())>0) {
            pushInitInputString += (char)charBuf;
          }
        }
        catch(Exception e) {
          bml.printError("Exception in reading XML file:"+e);
          e.printStackTrace();
          return "";
        }
        
        // display and send the input
        if(bml.debugMode)bml.printDebug(
            "PUSH C2SIM INITIALIZE XML:"+pushInitInputString);
        String pushInitResponseString = "";
        bml.pushingInitialize = true;
        pushInitResponseString = 
            bml.ws.sendC2simREST(
                pushInitInputString,
                "INFORM",
                bml.c2simProtocolVersion);
        
        // display result
        bml.showInfoPopup( 
            pushInitResponseString, 
            "C2SIM Initialize Push Message");
        
        // delay clearing popup
        try{Thread.sleep(1000);}catch(InterruptedException ie){}
        
        // clear the data and return
        bml.pushingInitialize = false;
        bml.xmlUrl = null;
        return pushInitResponseString;
        
    }//end pushInitC2SIM()
        
    /**
     * send C2SIM command e.g. SHARE (do at end of Initialize)
     */
    String pushC2simServerControl(String command)
    {
        // open connection to REST server
        if(bml.debugMode)bml.printDebug("server control command:");
        if(bml.submitterID.length() == 0) {
            bml.showInfoPopup( 
                "cannot push C2SIM server control - submitter ID required", 
                "C2SIM Server Control Push Message");
            return "cannot push C2SIM server control - submitter ID required";
        }
        
        // start REST connection using performative for Initialize
        C2SIMClientREST_Lib c2simClient = bml.ws.newRESTLib("INFORM");
        c2simClient.setHost(bml.serverName);
        if(bml.debugMode)bml.printDebug("C2SIM Order/Init Host:"+bml.serverName);
        c2simClient.setSubmitter(bml.submitterID);
        if(bml.debugMode)bml.printDebug("C2SIM Order/Init Submitter:"+bml.submitterID);
        c2simClient.setPath("C2SIMServer/c2sim");
      
        // get the password and check that it has content
        if(bml.serverPassword.length() == 0 &&
            !command.equals("STATUS") &&
            !command.equals("QUERYINIT")) {
            bml.showInfoPopup( 
                "cannot push C2SIM Init - password required", 
                "C2SIM Server Control Push Message");
            return "cannot push C2SIM server control - password required";
        }

        // send the command
        bml.pushingInitialize = true;
        String pushShareResponseString = "";
        try{
            pushShareResponseString = 
                c2simClient.c2simCommand(command,bml.serverPassword,"","");
        } catch (C2SIMClientException bce) {
            bml.showErrorPopup(
                "exception pushing C2SIMserver control:" +
                    bce.getMessage()+" cause:" + bce.getCauseMessage(), 
                "C2SIM Server Control Push Message");
            bml.printError("RESPONSE:" + pushShareResponseString);
            bce.printStackTrace();
            bml.pushingInitialize = false;
            return pushShareResponseString;
        }
         
        // display and return result
        if(bml.debugMode)bml.printDebug("The C2SIM server control push result length : " +
                pushShareResponseString.length());
                //+ "XML:"+  pushShareResponseString + "|");
        if(!command.equals("STATUS") && !command.equals("QUERYINIT")) {
            if(!bml.runningServerTest)
                bml.showInfoPopup( 
                    pushShareResponseString, 
                    "C2SIM Init Push Message");
            int startStatus = pushShareResponseString.indexOf("State set to ") + 13;
            serverStatus = pushShareResponseString.substring(startStatus);
            
            // special case for SHARE: extract unit count
            if(command.equals("SHARE")) {
                int unitsPosition = pushShareResponseString.indexOf("Units")-1;
                int publishedEnd = pushShareResponseString.indexOf("published")+10;
                String unitsCount = "0";
                if(unitsPosition > 0 && publishedEnd > 10)
                    unitsCount = pushShareResponseString.substring(publishedEnd,unitsPosition);
                bml.initStatusLabel.setText(unitsCount);
            }
        }
        // initial startup (status showing UNKNOWN) - extract and post status
        else if(serverStatus.equals("UNKNOWN")){
            int startOfState, endOfState;
            int sessionIndex = pushShareResponseString.indexOf("<sessionState>");
            if(sessionIndex > 0){
                startOfState = sessionIndex + 14;
                endOfState = pushShareResponseString.indexOf("</sessionState>");
            }
            else
            {
                startOfState = pushShareResponseString.indexOf("<SessionState>") + 14;
                endOfState = pushShareResponseString.indexOf("</SessionState>");
            }
            // pull the state out of reply
            if (startOfState > 14) {
                serverStatus = pushShareResponseString.substring(startOfState, endOfState);
            }
            bml.setServerStateLabel(serverStatus);
        
            // pull number of units out of status
            int startOfTag = pushShareResponseString.indexOf(bml.c2simUnitCountTag);
            if(startOfTag < 0) {
                if(bml.debugMode)bml.printDebug(
                    "ERROR: "+ bml.c2simUnitCountTag + " tag not found in " + 
                    " server status message");
                bml.pushingInitialize = false;
                return pushShareResponseString;
            }
            int endOfTag = startOfTag + (bml.c2simUnitCountTag).length();
            int endOfValue = pushShareResponseString.
                substring(endOfTag).indexOf(bml.c2simUnitCountTagEnd);
            String numberValue = 
                pushShareResponseString.substring(endOfTag,endOfTag+endOfValue);
            if(bml.debugMode)bml.printDebug("STATUS REPLY INIT UNITS:"+numberValue);
            bml.initStatusLabel.setText(numberValue);
        }
        
        bml.pushingInitialize = false;
        return pushShareResponseString;
        
    }// end pushC2simServerControl()
    
    /**
     * send C2SIM command e.g. SHARE (do at end of Initialize)
     */
    String pushC2simServerInput(String command) {
        return pushC2simServerInput(command,"","",""); 
    }
    String pushC2simServerInput(String command,String parm) {
        return pushC2simServerInput(command,parm,"","");
    }
    String pushC2simServerInput(
        String command, String parm1, String parm2, String parm3)
    {
        // open connection to REST server
        if(bml.debugMode)bml.printDebug("server input:" +
            command + " " + parm1 + " " + parm2 + " " + parm3);
        if(bml.submitterID.length() == 0) {
            bml.showInfoPopup( 
                "cannot push C2SIM server input - submitter ID required", 
                "C2SIM Server Control Push Message");
            return "cannot push C2SIM server control - submitter ID required";
        }
        
        // start REST connection using performative for Initialize
        C2SIMClientREST_Lib c2simClient = bml.ws.newRESTLib("INFORM");
        c2simClient.setHost(bml.serverName);
        if(bml.debugMode)bml.printDebug("C2SIM Order/Init Host:"+bml.serverName);
        c2simClient.setSubmitter(bml.submitterID);
        if(bml.debugMode)bml.printDebug("C2SIM Order/Init Submitter:"+bml.submitterID);
        c2simClient.setPath("C2SIMServer/c2sim");

        // send the command
        bml.pushingInitialize = true;
        String pushShareResponseString = "";
        try{
            pushShareResponseString = 
                c2simClient.c2simCommand(command,parm1,parm2,parm3);
        } catch (C2SIMClientException bce) {
            bml.showErrorPopup(
                "exception pushing C2SIMserver input:" +
                    bce.getMessage()+" cause:" + bce.getCauseMessage(), 
                "C2SIM Server Input Push Message");
            bml.printError("RESPONSE:" + pushShareResponseString);
            bce.printStackTrace();
            bml.pushingInitialize = false;
            return pushShareResponseString;
        }
         
        // display and return result
        if(bml.debugMode)bml.printDebug("The C2SIM server input push result length : " +
                pushShareResponseString.length() + " XML:" +
                pushShareResponseString + "|");

        if(!bml.runningServerTest)
            bml.showInfoPopup( 
                pushShareResponseString, 
                "C2SIM Input Push Message");
        
        bml.pushingInitialize = false;
        return pushShareResponseString;
        
    }// end pushC2simServerInput()
    
    
    /**
     * server control functions
     */
    String pushShareC2SIM(){return pushC2simServerControl("SHARE");}
    String pushInitializeC2SIM(){return pushC2simServerControl("INITIALIZE");}
    String pushResetC2SIM(){return pushC2simServerControl("RESET");}
    String pushStartC2SIM(){return pushC2simServerControl("START");}
    String pushPauseC2SIM(){return pushC2simServerControl("PAUSE");}
    String pushStopC2SIM(){return pushC2simServerControl("STOP");}
    String pushEditC2SIM(){return pushC2simServerControl("EDIT");}
    
    /**
     * server record and playback controls
     * @return 
     */
    String pushServerRecStart(){return pushC2simServerInput("STARTREC");}
    String pushServerRecPause(){return pushC2simServerInput("PAUSEREC");}
    String pushServerRecRestart(){return pushC2simServerInput("RESTARTREC");}
    String pushServerRecStop(){return pushC2simServerInput("STOPREC");}
    String pushServerRecGetStatus() {return pushC2simServerInput("GETRECSTAT");}
    String pushServerPlayStart(){return pushC2simServerInput("STARTPLAY");}
    String pushServerPlayPause(){return pushC2simServerInput("PAUSEPLAY");}
    String pushServerPlayRestart(){return pushC2simServerInput("RESTARTPLAY");}
    String pushServerPlayStop(){return pushC2simServerInput("STOPPLAY");}
    String pushServerPlayGetStatus() {return pushC2simServerInput("GETPLAYSTAT");}
    String getSimTimeMult() {return pushC2simServerInput("GETSIMMULT");}
    String getPlayTimeMult() {return pushC2simServerInput("GETPLAYMULT");}
    
    // status check; can initiate a late joiner initialization
    String pushStatusC2SIM(){
                
        String response = pushC2simServerControl("STATUS");
        
        // if in late joiner mode and server is running, 
        // invoke server command to fetch C2SIMInitialization
        if(bml.lateJoinerMode.equals("1")){
            String systemStatus = bml.serverStatusLabel.getText();
            if(systemStatus.equals("RUNNING") ||
               systemStatus.equals("PAUSED") ||
               systemStatus.equals("UNKNOWN") )    
            pushLateJoinerC2SIM(); 
        }
        
        return response;
        
    }// end pushStatusC2SIM()
    
    // command returns C2SIMInitialize structure
    String pushLateJoinerC2SIM(){
        
        String messageBody = pushC2simServerControl("QUERYINIT");
                     
        // capture a copy of the input
        /*
        try{
            (new File("latejoincopy.xml")).delete();
            BufferedWriter out = new BufferedWriter(
                new FileWriter("latejoincopy.xml"));
            out.write(messageBody);
            out.close();
        }
        catch(IOException ioe){
            bml.printError("IOException capturing initialization XML:" +
                ioe.getMessage());
        }
        */
        // process late join
        bml.lateJoinerInitialization = messageBody;
        bml.showButtons(false);
        MilOrg milOrg = new MilOrg();
        if(milOrg.parseC2SIMInit(messageBody) > 0)
            bml.showButtons(true);
        if(bml.orderIconsOnScreen)
            bml.orderRemoveButton.setVisible(true);
        if(bml.reportIconsOnScreen)
            bml.reportRemoveButton.setVisible(true);
        return "";
        
    }// end pushLateJoinerC2SIM()
    
    /**
     * saves C2SIM initialization to a file location chosen by user
     */
    void saveJaxFrontInitC2SIM() {
        try {
            bml.saveJaxFront("Initialize");
        } catch(Exception e) {
            bml.printError("Exception in saving XML file:"+e);
            e.printStackTrace();
            return;
        }
    }// end saveJaxFrontInitC2SIM()
    
}// end class InitC2SIM
