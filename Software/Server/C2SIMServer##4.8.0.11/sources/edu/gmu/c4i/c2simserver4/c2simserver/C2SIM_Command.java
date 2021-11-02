/*----------------------------------------------------------------*
|    Copyright 2001-2020 Networking and Simulation Laboratory     |
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
package edu.gmu.c4i.c2simserver4.c2simserver;

import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Server.SessionState_Enum;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import org.jdom2.Document;
import java.io.File;

import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Cyber.c2sim_NS;
import edu.gmu.c4i.c2simclientlib2.*;
//import edu.gmu.c4i.c2simclientlib2.CWIXHeader;
//import edu.gmu.c4i.c2simclientlib2.C2SIMHeader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;
import javax.xml.namespace.QName;
import org.jdom2.*;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;
import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Server.*;
import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;


/**
 * <h1>C2SIM_Command</h1> <p>
 * Performs server command of IBML09 messages

 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_Command {
    public static String SISOSTD = "SISO-STD-C2SIM";
    
    /*******************/
    /* commandProcess  */
    /*******************/

    /**
    * commandProcess - Process commands sent from client 
    @param cmd - String the command to be processed
    @param parm1 - First parameter Depends on specific command
    @param parm2 - Second parameter Depends on specific command
    @param t - C2SIM_Transaction
    @return - String - Message to submitter on success or failure
    @throws C2SIMException 
    */
    static String commandProcess(String cmd, String parm1, String parm2, C2SIM_Transaction t) throws C2SIMException {

        Namespace n;
        Collection<Document> vals = new Vector<>();
        C2SIMMessageDefinition md;
        Document doc = new Document();

        switch (cmd.toUpperCase()) {

            /*
                Commands that change stare of the server and the exercise 
             */
            case "START":
                
                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                C2SIM_Server.sessionState = SessionState_Enum.RUNNING;
                publishStateUpdate("StartScenario",C2SIM_Server.SessionState_Enum.RUNNING.toString(), t);
                return "START Command processed.  State set to " + C2SIM_Server.sessionState;

            case "INITIALIZE":
                
                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                C2SIM_Server.sessionState = SessionState_Enum.INITIALIZING;
                publishStateUpdate("SubmitInitialization",SessionState_Enum.INITIALIZING.toString(), t);
                return "INITIALIZE Command processed.  State set to " + C2SIM_Server.sessionState;

            case "STOP":
                
                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                C2SIM_Server.sessionState = SessionState_Enum.INITIALIZED;
                publishStateUpdate("StopScenario", C2SIM_Server.SessionState_Enum.INITIALIZED.toString(), t);
                return "STOP Command processed.  State set to " + C2SIM_Server.sessionState;

            case "RESET":

                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                
                // Reset all initialization data
                C2SIM_Util.initDB = new C2SIM_InitDB();
                C2SIM_Util.unitMap = new HashMap<>();
                C2SIM_Util.forceSideMap = new HashMap<>();
                
                // Set new state aand publish StateUPdate
                C2SIM_Server.sessionState = SessionState_Enum.UNINITIALIZED;
                publishStateUpdate("ResetScenario", C2SIM_Server.SessionState_Enum.UNINITIALIZED.toString(), t);
                
                // Done, return
                return "RESET command processed.  State set to " + C2SIM_Server.sessionState;

            case "PAUSE":

                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);
                
                C2SIM_Server.sessionState = SessionState_Enum.PAUSED;
                publishStateUpdate("PauseScenario", C2SIM_Server.SessionState_Enum.PAUSED.toString(), t);
                return "PAUSE Command processed.  State set to " + C2SIM_Server.sessionState;

            case "SHARE":

                verifyPassword(parm1);
                checkCommandState(cmd, parm1, parm2, t);

                if (C2SIM_Util.initDB.entity.isEmpty()) {
                    C2SIM_Server.debugLogger.debug("SHARE command entered but no initialization data is present");
                    return "SHARE command entered but no initialization data is present";
                }

                C2SIM_C2SIM.shareC2SIM(t);
                return "SHARE command processed and published " + C2SIM_Util.initDB.entity.size() + " Units ";


            case "STATUS":
                // Server Status
                return C2SIM_Server.createResultMsgOK("OK", "Server is up", 0, 0.0);

            case "LOAD":
                // Load initialization data from File instead of from a WebServices submission
                try {
//                // File name supplied?
                if (parm1.equals(""))
                    return "LOAD command submitted with no file name";
                
                // Check the password
                verifyPassword(parm2);  
                checkCommandState(cmd, parm1, parm2, t);
                 
//                // Build file path and check that it exists
                C2SIM_Util.initDB_Name = C2SIM_Server.props.getProperty("server.bmlFiles") + C2SIM_Server.props.getProperty("server.initDB") + parm1;
                File initDBFile = new File(C2SIM_Util.initDB_Name);
                if (!initDBFile.exists())
                    return "LOAD commamd submitted for file: " + parm1 + ".  File does not exist";

//                // Load the file
                String initXML = C2SIM_Util.readInputFile(C2SIM_Util.initDB_Name);
                C2SIM_Util.initDB_Name = parm1;
                C2SIM_Server.debugLogger.debug("LOAD executed for file: " + parm1);

                // Create a document to hold the contents of the file
                Document d = C2SIM_Mapping.parseMessage(initXML);
                
                // Create a transaction for the Notification message
                C2SIM_Transaction trans = new C2SIM_Transaction();
                trans.setXmlText(initXML);
                trans.setDocument(d);
                trans.setProtocol(SISOSTD);
                trans.setSubmitterID(t.getSubmitterID());
                
                C2SIM_C2SIM.process_C2SIM_Initialization(trans);
                
                return "Initialization file " + C2SIM_Util.initDB_Name + " loaded. " 
                        + C2SIM_Util.forceSideMap.size() + " ForceSides and " + C2SIM_Util.unitMap.size() + " Entities (Units)) configured";
                }
                catch (Exception e) {
                    throw new C2SIMException("Error while loadig and processing Initialization Data (ObjectInitialization) file" + e);
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
                return "QUERYUNIT not supported under this version";

            /*
               Query for Initialization Data
             */

            case "QUERYINIT":
                String xmlResp = C2SIM_C2SIM.formatInit(t);
                C2SIM_Server.debugLogger.debug("Returning (Late) Initialization for " + C2SIM_Util.initDB.entity.size() + " Units to submitter " + t.getSubmitterID());
                return xmlResp;
                

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
    @param systemCommand - String new C3SIM State command
    @param newState String - New server state
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    public static void publishStateUpdate(String systemCommand, String newState, C2SIM_Transaction trans) throws C2SIMException {

        // Create document
        Document tempDoc;
        Element tempEl;

        // Create output document and add root element
        Document doc = new Document();
        Element root = new Element("MessageBody", c2sim_NS);

        // SystemCommandBody
        Element systemCommandBody = new Element("SystemCommandBody", c2sim_NS);
        root.addContent(systemCommandBody);

        // SystemCommand
        systemCommandBody.addContent(new Element("SystemCommandTypeCode", c2sim_NS).setText(systemCommand));
;
        // SessionState
        systemCommandBody.addContent(new Element("SessionStateCode", c2sim_NS).setText(newState));

        doc.addContent(root);

        C2SIM_Transaction t = new C2SIM_Transaction();


        // Set parameters in the C2SIM_Transaction object
        t.msTemp = "C2SIM_Simulation_Control";
        t.setProtocol(SISOSTD);
        t.setSender("SERVER");
        t.setReceiver("ALL");
        t.setSubmitterID(trans.getSubmitterID());
        t.setc2SIM_Version("1.0.0");

        t.setMsgnumber(C2SIM_Server.msgNumber);
        t.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
        t.setSource("Generated");
        t.setForwarders("");

        String xml = C2SIM_Util.xmlToStringD(doc, t);


        t.setXmlText(xml);

        // Publish the message
        C2SIM_Server_STOMP.publishMessage(t);
    }


    /********************/
    /* verifyPassword   */
    /********************/
    /**
    * verifyPassword - Check if the password submitted with a command matches the configured password from the properties file
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

        // INITIALIZING permits RESET or SHARE or LOAD
        if (state.equalsIgnoreCase("INITIALIZING"))
            if ((!cmd.equalsIgnoreCase("RESET")) && (!cmd.equalsIgnoreCase("SHARE") && (!cmd.equalsIgnoreCase("LOAD"))))
                commandStateError(cmd, state);

        // INITIALIZED permits START or EDIT
        if (state.equalsIgnoreCase("INITIALIZED"))
            if ((!cmd.equalsIgnoreCase("START")) && (!cmd.equalsIgnoreCase("RESET")))
                commandStateError(cmd, state);

        // RUNNING permits PAUSE or STOP
        if (state.equalsIgnoreCase("RUNNING"))
            if ((!cmd.equalsIgnoreCase("PAUSE")) && (!cmd.equalsIgnoreCase("STOP")))
                commandStateError(cmd, state);

        // PAUSED permits START
        if ((state.equalsIgnoreCase("PAUSED")) && (!cmd.equalsIgnoreCase("START")))
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

