/*----------------------------------------------------------------*
|    Copyright 2001-2023 Networking and Simulation Laboratory     |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for academic purposes is hereby  |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
 *-----------------------------------------------------------------*/

// C2SIMServerv4.8.3.2
// implements schema C2SIM_SMX_LOX_CWIX2023v2.zsd

package edu.gmu.c4i.c2simserver4.c2simserver;

import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Server.SessionState_Enum;
import java.util.HashMap;
import org.jdom2.Document;
import java.io.File;

import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Cyber.c2sim_NS;
//import edu.gmu.c4i.c2simclientlib2.*;
//import edu.gmu.c4i.c2simclientlib2.CWIXHeader;
//import edu.gmu.c4i.c2simclientlib2.C2SIMHeader;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Vector;
import org.jdom2.*;
import org.jdom2.Namespace;
import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;


/**
 * <h1>C2SIM_Command</h1> <p>
 * Performs server command of IBML09 & C2SIM messages

 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_Command {
    public static String SISOSTD = "SISO-STD-C2SIM";
    
    /*******************/
    /* commandProcess  */
    /*******************/
    
    // SystemMessage getters
    static String getRecordingStatus(){return C2SIM_Server.recordingStatus;}
    static String getPlaybackStatus(){return C2SIM_Server.playbackStatus;}
    static String getPlaybackStartTime(){return C2SIM_Server.playbackStartTime;}
    static String getP(){return C2SIM_Server.playbackMultipleString;}
    static String getPlaybackFilePathName(){
        return C2SIM_Server.playbackPath+C2SIM_Server.playbackFileName;}

    /**
    * commandProcess - Process commands sent from client 
    @param cmd - String the command to be processed
    @param parm1 - First parameter Depends on specific command
    @param parm2 - Second parameter Depends on specific command
    @param parm3 - Third parameter Depends on specific command
    @param t - C2SIM_Transaction
    @return - String - Message to submitter on success or failure
    @throws C2SIMException 
    */
    static String commandProcess(
        String cmd, String parm1, String parm2, String parm3, 
        C2SIM_Transaction t) 
        throws C2SIMException {
        Namespace n;
        Collection<Document> vals = new Vector<>();
        C2SIMMessageDefinition md = C2SIM_Util.mdIndex.get("C2SIM_Command");
        Document doc = new Document();
        int systemsInitialized = 0;
        String multiple;     
        switch (cmd.toUpperCase()) {

            /*
                Commands that change stare of the server and the exercise 
             */
            case "START":
                
                systemsInitialized = 0;
                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                C2SIM_Server.sessionState = SessionState_Enum.RUNNING;
                C2SIM_Server.setRunningState(true);
                C2SIM_Server.setScenarioPausedState(false);
                publishSystemMessage("StartScenario","","","","","","","","",t);
                return "START Command processed.  State set to " + C2SIM_Server.sessionState;

            case "INITIALIZE":             
                // TODO: connfirm all required clients respond
                verifyPassword(parm1);        
                checkCommandState(cmd, parm1, parm2, t);
                C2SIM_Server.sessionState = SessionState_Enum.INITIALIZING;
                publishSystemMessage("SubmitInitialization","","","","","","","","",t);
                return "INITIALIZE Command processed.  State set to " + C2SIM_Server.sessionState;

            case "STOP":  
                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2,t); 
                C2SIM_Server.sessionState = SessionState_Enum.UNINITIALIZED;
                return "STOP command processed.  State set to " + C2SIM_Server.sessionState;
                
            case "RESET":
                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2,t); 
                C2SIM_Server.sessionState = SessionState_Enum.UNINITIALIZED;
                return "RESET command processed.  State set to " + C2SIM_Server.sessionState;

            case "PAUSE":

                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                
                C2SIM_Server.sessionState = SessionState_Enum.PAUSED;
                C2SIM_Server.setRunningState(false);
                C2SIM_Server.setScenarioPausedState(true);
                publishSystemMessage("PauseScenario","","","","","","","","",t);
                return "PAUSE Command processed.  State set to " + C2SIM_Server.sessionState;
                
            case "SHARE":

                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);

                if (C2SIM_Util.initDB.entity.isEmpty()) {
                    C2SIM_Server.debugLogger.debug("SHARE command entered but no initialization data is present");
                    return "SHARE command entered but no initialization data is present";
                }

                publishSystemMessage("ShareScenario","","","","","","","","",t);
                C2SIM_C2SIM.shareC2SIM(t);
                return "SHARE command processed and published " + C2SIM_Util.numC2SIM_Units + " Units  and " +
                    C2SIM_Util.numC2SIM_Routes + " Routes";

            case "RESUME":

                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                
                C2SIM_Server.sessionState = SessionState_Enum.RUNNING;
                C2SIM_Server.setRunningState(true);
                C2SIM_Server.setScenarioPausedState(false);
                publishSystemMessage("ResumeScenario","","","","","","","","",t);
                return "RESUME Command processed.  State set to " + C2SIM_Server.sessionState;
                
            case "INITCOMP":
                checkCommandState(cmd, parm1, parm2, t);
                systemsInitialized++;
                publishSystemMessage("InitializationComplete received from " + systemsInitialized + " client systems",
                    "","","","","","","","",t);
                return "SHARE command processed and published " + C2SIM_Util.numC2SIM_Units + " Units  and " +
                    C2SIM_Util.numC2SIM_Routes + " Routes";


            case "STATUS":
                // Server Status via REST only
                String serverStat = "Server is stopped";
                if(C2SIM_Server.getScenarioPausedState())serverStat = "Server is paused";
                if(C2SIM_Server.getRunningState())serverStat = "Server is running";
                return C2SIM_Server.createResultMsgOK("OK", serverStat, 0, 0.0);

            case "LOAD": // REST only
                // Load initialization data from File instead of from a WebServices submission
                try {
                // File name supplied?
                if (parm1.equals(""))
                    return "LOAD command submitted with no file name";
                
                // Check the password
                verifyPassword(parm1);  
                checkCommandState(cmd, parm1, parm2, t);
                 
                // Build file path and check that it exists
                C2SIM_Util.initDB_Name = C2SIM_Server.props.getProperty("server.bmlFiles") + 
                    C2SIM_Server.props.getProperty("server.initDB") + parm1;
                File initDBFile = new File(C2SIM_Util.initDB_Name);
                if (!initDBFile.exists())
                    return "LOAD commamd submitted for file: " + parm1 + ".  File does not exist";

                // Load the file
                String initXML = C2SIM_Util.readInputFile(C2SIM_Util.initDB_Name);
                C2SIM_Util.initDB_Name = parm1;
                C2SIM_Server.debugLogger.debug("LOAD executed for file: " + parm1);

                // Create a document to hold the contents of the file
                Document d = C2SIM_Mapping.parseMessage(initXML);             
                return "Initialization file " + C2SIM_Util.initDB_Name + " loaded. " 
                        + C2SIM_Util.forceSideMap.size() + " ForceSides and " 
                        + C2SIM_Util.unitMap.size() + " Entities (Units)) configured";
                }
                catch (Exception e) {
                    throw new C2SIMException(
                        "Error while loading and processing Initialization Data (ObjectInitialization) file" + e);
                }

            case "NEW":

//                if (parm1.equals(""))
//                    return "NEW Command requires a name";
//
//                // File should not already exist
//                if (unitDBExists(C2SIM_ServerProcess.unitDB_Folder + "/" + parm1)) {
//                    return "NEW Command submitted.  File: " + parm1 + " already exists";
//                }
//                // Create new DB object
//                C2SIM_Server.unitDB = new HashMap<String, Document>();
//
//                // Record the name
//                C2SIM_Server.unitDB_Name = parm1;
//
//                // Save the empty DB 
//                saveUnitDB(C2SIM_Server.unitDB, C2SIM_ServerProcess.unitDB_Folder + "/" + parm1);
//
//                C2SIM_Server.initUnitDB = null;
//
//                C2SIM_Server.debugLogger.debug("NEW Unit database created as Name: " + parm1);
//                return "NEW unitDB created File Name: " + parm1;
                return "NEW commaand not surrently supported";

            case "SAVE":
            case "SAVEAS":

//                // Is DB open?
//                if (C2SIM_Server.unitDB_Name.equals(""))
//                    return "SAVE Command submitted but database is opened.  Please do a SAVEAS with a filename";
//
//                // If no name submitted  then use current name
//                if (parm1.equals("")) {
//                    saveUnitDB(C2SIM_Server.unitDB, C2SIM_Server.unitDB_Name);
//                    C2SIM_Server.debugLogger.debug("Unit Database saved as " + C2SIM_Server.unitDB_Name);
//                    return "SAVE - Current database saved under name " + C2SIM_Server.unitDB_Name;
//                }
//                else {
//                    // Must be a Save As,  use name supplied as a parameter
//                    saveUnitDB(C2SIM_Server.unitDB, C2SIM_ServerProcess.unitDB_Folder + "/" + parm1);
//                    C2SIM_Server.debugLogger.debug("Unit Database saved as " + parm1);
//                    return "Unit Database saved as " + parm1;
                return "SAVE and SAVEAS commands not currently supported";

//                }

            case "DELETE":

//                if (parm1.equals("")) {
//                    C2SIM_Server.debugLogger.error("DELETE submitted with no name");
//                    return "DELETE submitted with no name";
//                }   // if
//
//                // Are we deleting the current file?
//                if (parm1.equals(C2SIM_Server.unitDB_Name))
//                    C2SIM_Server.unitDB_Name = "defaultDB";
//                C2SIM_Server.unitDB = new HashMap<String, Document>();
//
//                // Does the file exist?
//                if (!unitDBExists(C2SIM_ServerProcess.unitDB_Folder + "/" + parm1)) {
//                    C2SIM_Server.debugLogger.error("DELETE submitted for file: " + parm1 + ". File doesn't exist");  // Shouldn't happen
//                    return "DELETE submitted for file: " + parm1 + ". File doesn't exist";
//                }   // if
//
//                // Create File object and delete the file
//                File f = new File(C2SIM_ServerProcess.unitDB_Folder + "/" + parm1);
//                f.delete();
//
//                C2SIM_Server.debugLogger.debug("File: " + parm1 + " deleted");
//                return "File: " + parm1 + " deleted";
                return "DELETE command not currently supported";

            /*
                Query for latest position report for a particular unit
             */
            case "QUERYUNIT":
                return "QUERYUNIT not supported under this server version";

            /*
               Query for Initialization Data
             */

            case "QUERYINIT":// REST only
                String xmlResp = C2SIM_C2SIM.formatInit(t);
                C2SIM_Server.debugLogger.debug("Returning (Late) Initialization for " 
                    + C2SIM_Util.initDB.entity.size() + " Units to submitter " + t.getSubmitterID());
                return xmlResp;
                
            //**********************************************************************
            // from here down added new for C2SIM 1.0.2/CWIX2023 from here down JMP 14Apr2022 & 23Feb22023
                
            // MagicMove object location
            case "MAGIC":
                 // Direct instant movement of an object to a location
                try {
                    // Parameters supplied?
                    if (parm1.equals("") || parm2.equals("") || parm3.equals(""))
                        return "MagicMode command submitted missing object and/or location";

                    // build SystenCommand message for MagicMove
                    Document d = new Document();
                    Element root = new Element("MessageBody", c2sim_NS);

                   // SystemMessageBody and SystemMessageTypeCode
                    Element systemCommandBody = new Element("SystemMessageBody", c2sim_NS);
                    root.addContent(systemCommandBody);
                    Element magicMove = new Element("MagicMove", c2sim_NS);
                    systemCommandBody.addContent(magicMove);
                    Element systemCommandTypeCode = (new Element("SystemMessageTypeCode", c2sim_NS).setText("MagicMove"));
                    magicMove.addContent(systemCommandTypeCode);
                    
                    // MagicMove parameters: EntityReference, Location/GeodeticCoordinate/Latitude and Longitude
                    magicMove.addContent(new Element("EntityReference", c2sim_NS).setText(parm1));
                    
                    Element location = new Element("Location", c2sim_NS);
                    magicMove.addContent(location);
                    Element geodet = new Element("GeodeticCoordinate", c2sim_NS);
                    location.addContent(geodet);
                    Element latitude = (new Element("Latitude", c2sim_NS)).setText(parm2);
                    geodet.addContent(latitude);
                    Element longitude = (new Element("Longitude", c2sim_NS)).setText(parm3);
                    geodet.addContent(longitude);
                
                    // add it all to the doc
                    d.addContent(root);

                    // Create a transaction for the Magic Move message
                    C2SIM_Transaction trans = new C2SIM_Transaction();
                    trans.msTemp = "C2SIM_Command";
                    trans.setSender("SERVER");
                    trans.setReceiver("ALL");
                    trans.setSubmitterID(t.getSubmitterID());
                    String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
                    trans.setc2SIM_Version(c2simVer);
                    trans.setMsgnumber(C2SIM_Server.msgNumber);
                    trans.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
                    trans.setSource("Generated");
                    trans.setForwarders("");
                    trans.setDocument(d);
                    trans.setProtocol(SISOSTD);
                    trans.setSubmitterID(t.getSubmitterID());
                    trans.setMessageDef(C2SIM_Util.mdIndex.get("C2SIM_Command"));
                    trans.messageDef.messageDescriptor = "C2SIM_Command";
                    trans.setMsTemp("");       
                    
                    String xml = C2SIM_Util.xmlToStringD(d, t);
                    trans.setXmlText(xml);

                    C2SIM_Server_STOMP.publishMessage(trans);
                    
                    return "MagicMove directed for object:" + parm1 + " to lat/lon:" + parm2 + "/" + parm3;  
                }
                catch (Exception e) {
                    throw new C2SIMException("Error while loading and processing Magic Move command" + e);
                }
                
            // Send to Coalition: CheckpointRestore
            case "CPRESTORE":
                if(C2SIM_Server.getRunningState())
                    return "CheckpointRestore response: cannot run - pause or stop playback first";
                publishSystemMessage("CheckpointRestore","","","","","","","","", t) ;
                return "Published CheckpointRestorecommaand";
                
            // Send to Coalition: CheckpointRestore
            case "CPSAVE":
                if(C2SIM_Server.getRunningState())
                    return "CheckpointSave response: cannot run - pause or stop playback first";
                publishSystemMessage("CheckpointSAve","","","","","","","","", t) ;
                return "Published CheckpointSavecommaand";
                
            // Send to Coalition: RestartScenario
            case "RESTART":
                if(C2SIM_Server.getRunningState())
                    return "RestartServer response: cannot restart acenario - pause or stop playback first";
                publishSystemMessage("RestartScenario","","","","","","","","", t) ;
                return "Published RestartScenario commaand";

            // RequestSimulationRealtimeMultiple
            case "GETSIMMULT":
                String timeMultiple = C2SIM_Server.getScenarioTimeMultiple();
                publishSystemMessage(
                    "RequestSimulationRealtimeMultiple",
                    "","","","","","","","",
                    t);    
                publishSystemMessage(
                    "SimulationRealtimeMultipleReport",
                    "SimulationRealtimeMultiple",
                    timeMultiple,
                    "","","","","","",
                    t);
                return "RequestSimulationRealtimeMultiple response: SimulationRealtimeMultipleReport: " + timeMultiple;
                
            // SetSimulationRealtimeMultiple
            case "SETSIMMULT":
                C2SIM_Server.setScenarioTimeMultiple(parm1);
;               publishSystemMessage(
                    "SetSimulationRealtimeMultiple",
                    "ScenarioRealtimeMultiple",
                    parm1,
                    "","","","","","",
                    t);
                return "SetSimulationRealtimeMultiple response: SimulationRealtimeMultipleReport: " + parm1;
 
            // RequestPlaybackStatus
            case "GETPLAYSTAT":
                publishSystemMessage(
                    "RequestPlaybackStatus",
                     "Name",
                    C2SIM_Server.playbackFileName,
                    "PlaybackPosition",
                    "not available",
                    "PlaybackRealtimeMultiple",
                    C2SIM_Server.playbackMultipleString,
                    "PlaybackStatusCode",
                    C2SIM_Server.playbackStatus,
                    t);    
                publishSystemMessage(
                    "PlaybackStatusReport",
                    "Name",
                    C2SIM_Server.playbackFileName,
                    "PlaybackPosition",
                    "not available",
                    "PlaybackRealtimeMultiple",
                    C2SIM_Server.playbackMultipleString,
                    "PlaybackStatusCode",
                    C2SIM_Server.playbackStatus,
                    t);
                return "RequestPlaybackStatus response: PlaybackStatusReport: " + C2SIM_Server.playbackStatus;
                
            // PausePlayback
            case "PAUSEPLAY":
                if(!C2SIM_Server.playbackStatus.equals("PLAYBACK_RUNNING"))
                    return "|"+C2SIM_Server.playbackStatus+"|"+"PausePlayback not posssible - Player is not running";
                if(C2SIM_Server.playbackStatus.equals("PLAYBACK_PAUSED"))
                    return "PausePlayback not posssible - Player is already paused";
                C2SIM_Server.playbackStatus = "PLAYBACK_PAUSED";
                publishSystemMessage(
                    "PausePlayback",
                    "","","","","","","","",
                    t); 
                return "PausePlayback response: PlaybackStatusReport: " + C2SIM_Server.playbackStatus;
                
            // ResumePlayback
            case "RESUMEPLAY":      
                if(!C2SIM_Server.playbackStatus.equals("PLAYBACK_PAUSED"))
                    return "RestartPlayback response: cannot restart - Playback is not paused";
                C2SIM_Server.playbackStatus = "PLAYBACK_RUNNING";
                C2SIM_Player.playerIsRunning = true;
                publishSystemMessage(
                    "ResumePlayback",
                    "","","","","","","","",
                    t);
                return "ResumePlayback response: PlaybackStatusReport: " + C2SIM_Server.playbackStatus;
                
            // StartPlayback
            case "STARTPLAY":
                if(!C2SIM_Server.playbackStatus.equals("NO_PLAYBACK_IN_PROGRESS"))
                    return "StartPlayback response: cannot start - Playback is not stopped (if paused, use Restart";
                C2SIM_Server.playbackStatus = "PLAYBACK_RUNNING";
                C2SIM_Player.playerIsRunning = true;
                if(!parm2.trim().equals("0") || parm2.trim().equals(""))
                    C2SIM_Server.playbackFileName = parm2; else C2SIM_Server.playbackFileName = "replay.log";
                C2SIM_Server.playbackFilePathName = C2SIM_Server.playbackPath + C2SIM_Server.playbackFileName;
                if(!(new File(C2SIM_Server.playbackFilePathName).exists())){
                    setPlayStopped(t.getSubmitterID());
                    return "File " + C2SIM_Server.playbackFilePathName + " does not exist";
                }
                
                // run the player
                if(parm2.equals("0"))C2SIM_Server.playbackStartTime = "0000-00-00T00:00:00.000Z";
                else C2SIM_Server.playbackStartTime = parm2;
                publishSystemMessage(
                    "StartPlayback",
                    "Name",
                    C2SIM_Server.playbackFilePathName,
                    "StartTime",
                    C2SIM_Server.playbackStartTime,
                    "","","","",
                    t);
                new C2SIM_Player(t.getSubmitterID(),t.getConversationid());
                return "StartPlayback response: PlaybackStatusReport :" + C2SIM_Server.playbackStatus;
                 
            // StopPlayback
            case "STOPPLAY":
                setPlayStopped(t.getSubmitterID());
                return "StopPlayback response: PlaybackStatusReport: " + C2SIM_Server.playbackStatus;

            // RequestPlaybackRealtimeMultiple multiple
            case "GETPLAYMULT":
                publishSystemMessage(
                    "RequestPlaybackRealtimeMultiple",
                    "","","","","","","","",
                    t);
//                publishSystemMessage(
//                    "PlaybackRealtimeMultiple", 
//                    C2SIM_Player.timeScale,
//                    "","","","","","","",
//                    t);
                return "RequestPlaybackRealtimeMultiple response:" +
                    "PlaybackRealtimeMultipleReport: " + C2SIM_Server.playbackMultipleString;
                
            // SetPlaybackRealtimeMultiple
            case "SETPLAYMULT":
                C2SIM_Player.setTimeScale(parm1);
;               publishSystemMessage(
                    "SetPlaybackRealtimeMultiple",
                    "PlaybackRealtimeMultiple",
                    parm1,
                    "","","","","","",
                    t);
                return "SetPlaybackRealtimeMultiple response: PlaybackRealtimeMultipleReport: " + parm1;

            // RequestRecordingStatus
            case "GETRECSTAT":
                publishSystemMessage(
                    "RequestRecordingStatus",
                    "FileSize",
                    Long.toString(new File("/home/bmluser/c2simFiles/c2simReplay/replay.log").length()),
                    "Name",
                    C2SIM_Server.recordingName,
                    "RecordingStatusCode",
                    C2SIM_Server.recordingStatus,
                    "","",
                    t);
                publishSystemMessage(
                    "RecordingStatusReport",
                    "FileSize",
                    Long.toString(new File("/home/bmluser/c2simFiles/c2simReplay/replay.log").length()),
                    "Name",
                    C2SIM_Server.recordingName,
                    "RecordingStatusCode",
                    C2SIM_Server.recordingStatus,
                    "","",
                    t);
                return "RequestRecordingStatus response: RecordingStatusReport: " + C2SIM_Server.recordingStatus;
                
            // PauseRecording
            case "PAUSEREC":
                if(!C2SIM_Server.recordingStatus.equals("RECORDING_IN_PROGRESS"))
                    return "PauseRecording not posssible - Recorder is not running";
                C2SIM_Server.recordingStatus = "RECORDING_PAUSED";
                publishSystemMessage(
                    "PauseRecording",
                    "","","","","","","","",
                    t); 
                return "PauseRecording response: RecordingStatusReport: " + C2SIM_Server.recordingStatus;   
                
            // ResumeRecording
            case "RESTARTREC":
                if(!C2SIM_Server.recordingStatus.equals("RECORDING_PAUSED"))
                    return "RestartRecording not posssible - Recorder is not paused";
                C2SIM_Server.recordingStatus = "RECORDING_IN_PROGRESS";
                publishSystemMessage(
                    "ResumeRecording",
                    "","","","","","","","",
                    t);
                return "ResumeRecording response: RecordingStatusReport: " + C2SIM_Server.recordingStatus;
                
            // StartRecording
            case "STARTREC":
                if(!C2SIM_Server.recordingStatus.equals("NOT_RECORDING"))
                    return "StartRecording not posssible - Recorder is not stopped (if paused, use Restart";
                C2SIM_Server.recordingStatus = "RECORDING_IN_PROGRESS";
                publishSystemMessage(
                    "StartRecording",
                    "","","","","","","","",
                    t);
                 return "StartRecording response: RecordingStatusReport: " + C2SIM_Server.recordingStatus; 
                
            // StopRecording
            case "STOPREC":
                if(!C2SIM_Server.recordingStatus.equals("RECORDING_IN_PROGRESS"))
                    return "StopRecording not posssible - Recorder is not running";
                C2SIM_Server.recordingStatus = "NOT_RECORDING";
                publishSystemMessage(
                    "StopRecording",
                    "","","","","","","","",
                    t);
                return "StopRecording response: RecordingSatatusReport: " + C2SIM_Server.recordingStatus;
                
            /*
                Command not recognized
             */
            default:
                C2SIM_Server.debugLogger.debug("Command: " + cmd + " received from " + t.getSubmitterID() + " not recognized");
                return "Command: " + cmd + " received from " + t.getSubmitterID() + " not recognized";

        }   // switch


    }   // commandProcess()


    /****************************/
    /*  publishStateUpdate      */
    /****************************/
    /**
    * publishStateUpdate - Format and publish a message indicating a change in server state
    @param systemCommand - String new C3SIM State message
    @param newState String - New server state
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    public static void publishStateUpdate(String systemCommand, String newState, C2SIM_Transaction trans) throws C2SIMException {

        // Create document
        Document tempDoc;
        Element tempEl;
        Document doc = new Document();
        Element root = new Element("MessageBody", c2sim_NS);

        // SystemMessageBody
        Element systemCommandBody = new Element("SystemMessageBody", c2sim_NS);
        root.addContent(systemCommandBody);
;
        // Session
        systemCommandBody.addContent(new Element(systemCommand, c2sim_NS).setText(newState));
        doc.addContent(root);
        C2SIM_Transaction t = new C2SIM_Transaction();
        C2SIMMessageDefinition md = C2SIM_Util.mdIndex.get("C2SIM_Command");
        t.setMessageDef(md);
        md.messageDescriptor = "C2SIM_Command";

        // Set parameters in the C2SIM_Transaction object
        t.msTemp = "C2SIM_Command";
        t.setProtocol(SISOSTD);
        t.setSender("SERVER");
        t.setReceiver("ALL");
        t.setSubmitterID(trans.getSubmitterID());
        String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
        t.setc2SIM_Version(c2simVer);
        t.setMsgnumber(C2SIM_Server.msgNumber);
        t.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
        t.setSource("Generated");
        t.setForwarders("");
        String xml = C2SIM_Util.xmlToStringD(doc, t);
        t.setXmlText(xml);

        // Publish the message
        C2SIM_Server_STOMP.publishMessage(t);
    }// publishStateUpdate
    
    /****************************/
    /*  publishSystemMessage    */
    /****************************/
    /**
    *  - Format and publish a message directing action to C2SIM Coalition
    @param systemCommand - String new C3SIM State command
    @param parmName - name of single parameter or ""
    @param parmValue - value of the parameter
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    public static void publishSystemMessage(
        String systemCommand, 
        String parm1Name,
        String parm1Value,
        String parm2Name,
        String parm2Value,
        String parm3Name,
        String parm3Value,
        String parm4Name,
        String parm4Value,
        C2SIM_Transaction trans) {

        // Create document
        Document tempDoc;
        Element tempEl;

        // Create output document and add root element
        Document doc = new Document();
        Element root = new Element("MessageBody", c2sim_NS);

        // SystemMessageBody
        Element systemCommandBody = new Element("SystemMessageBody", c2sim_NS);
        root.addContent(systemCommandBody);
        Element systemMessageType = new Element(systemCommand, c2sim_NS);
        systemCommandBody.addContent(systemMessageType);
   
        // Parameters (max 4)
        if(!parm1Name.equals(""))
            systemCommandBody.addContent(new Element(parm1Name, c2sim_NS).setText(parm1Value));
        if(!parm2Name.equals(""))
            systemCommandBody.addContent(new Element(parm2Name, c2sim_NS).setText(parm2Value));
        if(!parm3Name.equals(""))
            systemCommandBody.addContent(new Element(parm3Name, c2sim_NS).setText(parm3Value));
        if(!parm4Name.equals(""))
            systemCommandBody.addContent(new Element(parm4Name, c2sim_NS).setText(parm4Value));
        doc.addContent(root);
        C2SIM_Transaction t = new C2SIM_Transaction();

        // Set parameters in the C2SIM_Transaction object
        t.msTemp = "C2SIM_Command";
        t.setProtocol(SISOSTD);
        t.setSender("SERVER");
        t.setReceiver("ALL");
        t.setSubmitterID(trans.getSubmitterID());
        String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
        t.setc2SIM_Version(c2simVer);
        t.setMsgnumber(C2SIM_Server.msgNumber);
        t.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
        t.setSource("Generated");
        t.setForwarders("");
        
        // set the message descriptor to Command
        C2SIMMessageDefinition md = new C2SIMMessageDefinition();
        md.messageDescriptor = "C2SIM_Command";
        t.setMessageDef(md);

        String xml = C2SIM_Util.xmlToStringD(doc, t);
        t.setXmlText(xml);

        // Publish the message
        try{
            C2SIM_Server_STOMP.publishMessage(t);
        } catch(C2SIMException c2x) {
            C2SIM_Server.debugLogger.error("C2SIMException in C2SIM_Server_STOMP.publishMessage");
        }
     
    }// publishSystemMessage


    /********************/
    /* verifyPassword   */
    /********************/
    /**
    * verifyPassword - Check if the password submitted with a command matches
    * the configured password from the properties file
    @param pw - String password
    @throws C2SIMException 
    */
    static void verifyPassword(String pw) throws C2SIMException {

        // Password not provided?
        if (pw.equals(""))
            throw new C2SIMException("Command requires password");

        // Password wrong?
        if (!pw.equals(C2SIM_Server.commandPassword))
            throw new C2SIMException("Password is invalid");

    }   // verifyPassword()
    
    /******************/
    /* setPlayStopped */
    /******************/
    static void setPlayStopped(String submitterID) {
        
        // make a neew transaction
        C2SIM_Transaction t = new C2SIM_Transaction();
        
        // Set parameters in the C2SIM_Transaction object
        t.msTemp = "C2SIM_Command";
        t.setProtocol(SISOSTD);
        t.setSender("SERVER");
        t.setReceiver("ALL");
        t.setSubmitterID(submitterID);
        String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
        t.setc2SIM_Version(c2simVer);
        t.setMsgnumber(C2SIM_Server.msgNumber);
        t.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
        t.setSource("Generated");
        t.setForwarders("");
        
        C2SIM_Server.playbackStatus = "NO_PLAYBACK_IN_PROGRESS";
        C2SIM_Player.playerIsRunning = false;
        publishSystemMessage(
            "StopPlayback",
            "","","","","","","","",
            t);
        
    }// setPlayStopped
    
    /***************************/
    /* allowCommandWhenRunning */
    /***************************/
    /**
    * allowCommandWhenRunning - filter for commands allowed in RUNNING state
    */
    public static HashMap<String, Boolean> allowedCommands = new HashMap<>();
    public static boolean allowCommandWhenRunning(String testCommand) {
        
        // on first invocation build a HashMap of allow commands
        // to speed checking in future invocations
        if(allowedCommands.isEmpty()){
            allowedCommands.put("PAUSE",true);
            allowedCommands.put("STOP",true);
            allowedCommands.put("MAGIC",true);
            allowedCommands.put("GETSIMMULT",true);
            allowedCommands.put("SETSIMMULT",true);
            allowedCommands.put("GETPLAYSTAT",true);
            allowedCommands.put("PAUSEPLAY",true);
            allowedCommands.put("RESTARTPLAY",true);
            allowedCommands.put("STARTPLAY",true);
            allowedCommands.put("STOPPLAY",true);
            allowedCommands.put("GETPLAYMULT",true);
            allowedCommands.put("SETPLAYMULT",true);
            allowedCommands.put("GETRECSTAT",true);
            allowedCommands.put("PAUSEREC",true);
            allowedCommands.put("RESTARTREC",true);
            allowedCommands.put("STARTREC",true);
            allowedCommands.put("STOPREC",true);
        }
        // use the HashMap to check the provided Command
        return allowedCommands.containsKey(testCommand);
    }// allowCommandWhenRunning 

    /************************/
    /* checkCommandState    */
    /************************/
    /**
    * checkCommandState - Determine if submitted command is allowed in current server state
    @param cmd  - String command submitted
    @param parm1 - First parameter
    @param parm2 - Second parameter
    @param t - C2SIM_Transaction
    @throws C2SIMException 
    */ 
    // Check server state for allowed commands
    static void checkCommandState(String cmd, String parm1, String parm2, C2SIM_Transaction t) throws C2SIMException {

        String state = C2SIM_Server.sessionState.toString();

        // UNINITIALIZED permits INITIALIZE
        if ((state.equalsIgnoreCase("UNINITIALIZED")) && (!cmd.equalsIgnoreCase("INITIALIZE"))) {
            commandStateError(cmd, state);
        }

        // INITIALIZING permits RESET or SHARE or LOAD or INITCOMP
        if (state.equalsIgnoreCase("INITIALIZING"))
            if ((!cmd.equalsIgnoreCase("RESET")) && (!cmd.equalsIgnoreCase("SHARE") && 
                (!cmd.equalsIgnoreCase("LOAD"))) && (!cmd.equalsIgnoreCase("INITCOMP")))
                commandStateError(cmd, state);

        // INITIALIZED permits START or EDIT
        if (state.equalsIgnoreCase("INITIALIZED"))
            if ((!cmd.equalsIgnoreCase("START")) && (!cmd.equalsIgnoreCase("RESET")))
                commandStateError(cmd, state);

        // RUNNING permits PAUSE or STOP
        if (state.equalsIgnoreCase("RUNNING"))
            //if ((!cmd.equalsIgnoreCase("PAUSE")) && (!cmd.equalsIgnoreCase("STOP")))
            if(!allowCommandWhenRunning(cmd.toUpperCase()))
                commandStateError(cmd, state);

        // PAUSED permits START
        if ((state.equalsIgnoreCase("PAUSED")) && (!cmd.equalsIgnoreCase("START")) &&
            (!cmd.equalsIgnoreCase("RESUME")))
            commandStateError(cmd, state);

    }   // checkCommandState()


    /************************/
    /*  commandStateError   */
    /************************/

    /**
    * commandStateError  - checkServerState found an error.  Log in debug log and throw an exception
    @param cmd - String - Command submitted
    @param state - String - Current state
    @throws C2SIMException 
    */
    static void commandStateError(String cmd, String state) throws C2SIMException {
        
        C2SIM_Server.debugLogger.error(cmd + " not permitted in state " + state);
        throw new C2SIMException(cmd + " not permitted in state " + state);

    }   // commandStateError
 

}   // class C2SIM_unitProcessing

