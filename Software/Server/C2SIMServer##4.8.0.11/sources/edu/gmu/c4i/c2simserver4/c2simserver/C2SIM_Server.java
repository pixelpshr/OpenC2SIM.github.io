/*----------------------------------------------------------------*
|    Copyright 2001-2018 Networking and Simulation Laboratory     |
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

import edu.gmu.c4i.c2simserver4.schema.C2SIMDB;
import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;
import java.io.InputStream;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import org.apache.logging.log4j.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Properties;
import javax.ws.rs.PathParam;
import org.jdom2.Document;


/**
 * <h1>C2SIM_Server</h1>
 Main class for C2SIM_Server application <p>
 * RESTful Web Services application running under Tomcat <p>
 * Built using JAX-RS <p>
 * Receives BML messages via a WS POST transaction <p>
 * May translate from incoming BML dialect to other dialects <p>
 All messages are then transmitted to a STOMP messaging server - Currently Apache Apollo
 Active URLs
  http://C2SIM_Server/c2sim        Submit xml to server
  http://C2SIM_Server/c2sim        Submit xml to server
 * @author Douglas Corner - George Mason University C4I and Cyber Center
 */
@Path("")
public class C2SIM_Server {

    // Static Variables
    public static String hostName;
    public static String ipAddress;

    public static boolean enforceVersion;
    public static String serverVersion;
    public static String minimumClientVersion;
    public static Integer minimumClientVersionValue;

    public static Integer msgNumber = 0;
    public static boolean serverInitialized = false;
    public static boolean sessionInitialized = false;
    public static boolean collectStatistics = false;
    public static String commandPassword = "";
    public static Logger debugLogger;
    public static Logger replayLogger;
    public static Logger cyberLogger;
    public static Properties props;
    public static C2SIM_ServerProcess proc;
    public static final String PROPERTIES_FILE_NAME = "/c2simServer.properties";
    public static boolean cyberAttack = false;
    static LocalDateTime now;
    static String msgTime;
    public String elapsedTime;
    static public DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    static String XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    public static String SISOSTD = "SISO-STD-C2SIM";
    
    // States are UNINITIALIZED, INITIALIZED, RUNNING, PAUSED
    public static C2SIM_Server.SessionState_Enum sessionState = SessionState_Enum.UNINITIALIZED;

    
    /****************************/
    /* postC2SIM                  */
    /***************************/
    //@param submitterID
    //@param firstForwarder
    //@param bml
    //@return
    //@throws InterruptedException 
    // @return XML message with result of POST/
    // POST method
    // Main routine for BML Server
    // Call processMessage where the processing is done
    // Computer processing time and return in WS response
    /**
     * postC2SIM
     * @param - submitterID - Identification of submitter
     * @param - protocol as determined by the client (BML or C2SIM or SISO-STD-C2SIM
     * @param - sender -C2SIM Sender ID
     * @param - communicativeActTypeCode - C2SIM
     * @param - receiver - C2SIM Receiver ID
     * @param - conversationid - C2SIM Conversation ID
     * @param - domain - Domain not used.  Kept for compatability with older versions
     * @param - clientVersion - Version of the ClientLib that originiated the message
    v* @param forwarders - List of servers that have already handled this transaction
     * @param xmlText - The xml Text including a C2SIM Header
     * @return XML - Indication of success or failure
     * @throws InterruptedException 
     */
    

    @Path("/c2sim")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String postC2SIM(
            @QueryParam("submitterID") String submitterID,
            @QueryParam("protocol") String protocol,
            @QueryParam("sender") String sender,
            @QueryParam("communicativeActTypeCode") String communicativeActTypeCode,
            @QueryParam("receiver") String receiver,
            @QueryParam("conversationid") String conversationid,
            @QueryParam("domain") String domain, // No longer used.  Kept for compatibility with older clients
            @QueryParam("version") String clientVersion,
            @DefaultValue("") @QueryParam("forwarders") String forwarders,
            String xmlText) throws InterruptedException, C2SIMException {

        long startTime;
        long endTime;
        Double timeInSecs;
        C2SIM_Transaction trans;


        // Call initialization methed.  If already initialized it will just return
        initialize();

        // Process the message
        try {

            // Create object to hold parameters (forwarders is set later)
            trans = new C2SIM_Transaction(xmlText, submitterID, protocol, sender, receiver, communicativeActTypeCode, "");
            trans.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
            trans.setSource("Client");  
            if (sender == null)
                trans.setSender("");
            if (receiver == null)
                trans.setReceiver("");
                
            
            // Number messages
            trans.setMsgnumber(++msgNumber);

            debugLogger.info("XML Message Received - MSG_Number: " + trans.getMsgnumber()
                    + " from submitter: " + trans.getSubmitterID() + " Protocol: " + protocol);

            // Check if server and client versions agree
            checkVersion(clientVersion);

            // Measure processing time
            startTime = System.currentTimeMillis();
            now = LocalDateTime.now();
            msgTime = String.format(dtf.format(now));  //2016/11/16 12:08:43

            
            /*
                In a multi-server environment each server tags each forwarded message with its IP address.  
                Servers examine each incoming message looking for its own aaddress.  
                If a match is found the message is discarded in order to eliminate server loops.
                Servers don't directly communicate with each other.   A "back to back" client application 
                    is used which connects to a pair of servers performs the forwarding.
             */
            if (!forwarders.equals("")) {
                // Addresses in a list are separated by colons ":"
                String[] forwardersArray = forwarders.split(":");

                // Search the array of aaddresses for this hosts address
                for (String addr : forwardersArray) {
                    if (addr.equals(ipAddress)) {
                        debugLogger.debug("Message number " + msgNumber + " discarded - This host has already handled it");
                        return "Message discarded as a duplicate";
                    }   // address = ipAAddress
                }   // for loop address array

            }   // forwarders

            // Add this server to the list
            if (forwarders.equals(""))
                trans.setForwarders(ipAddress);
            else
                trans.setForwarders(forwarders + ":" + ipAddress);
            
            
            // Log to replay log file, removing all NL's
            replayLogger.info("Receive  " + msgNumber + "  " + submitterID + "  " + xmlText.replaceAll("[\n\r]", "").replaceAll(">\\s*<", "><"));

            // Process the message
            C2SIM_ServerProcess.processMessage(trans);

        }   // try      // try      // try      // try   
        catch (C2SIMException e) {
            debugLogger.error("Error - Message Number: " + msgNumber + " - Error: " + e.getMessage()
                    + " Cause: " + e.getCauseMessage() + e.getST());
            return createResultMsgError("ERROR", "Error processing message", msgNumber, e);
        }

        catch (Exception e) {
            StackTraceElement[] st = e.getStackTrace();
            String ret = "\n";

            for (int i = 0; i < 5; ++i) {
                ret += "\t" + st[i].toString() + "\n";
            }

            debugLogger.error("Unknown Error - Message Number: " + msgNumber + " - Error: " + e.toString() + ret);

            return "Unknown Error - See debugLog + msgNumber = " + msgNumber;
        }

        // Computer processing time and return in response

        endTime = System.currentTimeMillis();
        timeInSecs = (endTime * 1.0 - startTime * 1.0) / 1000;
        elapsedTime = timeInSecs.toString();

        // Log response to debug logter
        debugLogger.info("Response: " + elapsedTime + "  " + "Message Number: " + msgNumber + "  " + " Submitter: " + submitterID);

        return createResultMsgOK("OK", "Message processed successfully", msgNumber, timeInSecs);

    }   // postC2SIM()


    /***************************/
    /*  Command Processing     */
    /***************************/
    /**
    * postCommand - HTTP Method used to post a server command
    @param cmd - The command being submitted
    @param parm1 - The first parameter, Varies by specific command.
    @param parm2 - The second parameter.  Varies by specific command
    @return String = Contains indication of success or failure
     */
    @Path("/command")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    
    public String postCommand(
            @QueryParam("command") String cmd,
            @QueryParam("parm1") String parm1,
            @QueryParam("parm2") String parm2,
            @QueryParam("submitterID") String submitter,
            @QueryParam("version") String clientVersion)
            throws C2SIMException {

        // Call initialization methed.  If already initialized it will just return
        initialize();

        String result = "";

        try {

            // Set up dummy C2SIM_Transaction = More will be filled in in commandProcess
            C2SIM_Transaction t = new C2SIM_Transaction("", submitter, SISOSTD, "SERVER", "ALL", "00000000-0000-0000-0000-000000000000", "");

            // Count messages
            ++msgNumber;
            t.setMsgnumber(msgNumber);

            // Log the command
            replayLogger.info("Command " + msgNumber + "  " + submitter + "  " + cmd + " " + parm1 + " " + parm2);

            debugLogger.info("Command Received - MSG_Number: : " + t.getMsgnumber()
                    + " from submitter: " + t.getSubmitterID()
                    + " Command " + cmd + " Parms: " + parm1 + ", " + parm2);

            // Check if server and client versions agree
            checkVersion(clientVersion);


            result = C2SIM_Command.commandProcess(cmd, parm1, parm2, t);
        }   // try
        catch (C2SIMException e) {
            debugLogger.error("Exception processing Unit Command " + e.getCause());
            return createResultMsgError("ERROR", "Error processing command", msgNumber, e);
        }   // catch

        return result;

    }   // postCommand()


    /****************/
    /* checkVersion */
    /****************/
    /**    
    Check that the version of the ClientLibrary used to submit this transaction is at least at the level of
      the props property minimumClientVersionValue.
    @param clientVersion - Value submitted with transaction
    @return boolean - True if version of ClientLib used is later than the minimumClientVersion
    @throws C2SIMException 
    */
    boolean checkVersion(String clientVersion) throws C2SIMException {

        // If we are not enforcing version number compliance return true;
        if (!enforceVersion)
            return true;

        // Check client and server version numbers

        // Make sure the clientVersion was included
        if (clientVersion == null)
            clientVersion = "UNKNOWN";


        // If we don't know one or both of the version numbers then accept what was submitted
        if ((clientVersion.equalsIgnoreCase("UNKNOWN")) || (serverVersion.equalsIgnoreCase("UNKNOWN"))) {
            return true;
        }

        // If the client version is less than the minimum client version throw an exception 
        if (convertVersion(clientVersion) < minimumClientVersionValue) {
            String msg = "Client version is less than minimum allowed - Received Client Version = " + clientVersion
                    + " Minimum Client Version = " + displayVersion(minimumClientVersionValue);
            throw new C2SIMException(msg);
        }   // if versions different

        return true;
    }   // checkVersion()


    /********************/
    /* convertVersion   */
    /********************/
    /**
    Convert version number from a.b.c.d string to Integer
    @param ver
    @return Integer value of version number
    */
    static Integer convertVersion(String ver) {

        // Split version into individual digits
        String[] v = ver.split("\\.");

        Integer versionValue = 0;
        Integer factor = 1000;

        // Convert the individual digits into an integer
        for (int i = 0; i < v.length; ++i) {
            versionValue += Integer.parseInt(v[i].trim()) * factor;
            factor = factor / 10;
        }    // for      

        return versionValue;

    }   // convertVersion()


    /********************/
    /* displayVersion   */
    /********************/
    /**
    Convert Integer version number to String
    @param i Integer value of version number
    @return String version as a.b.c.d
    */
    String displayVersion(Integer i) {
        String s = i.toString();
        int len = s.length();
        String out = s.substring(len - 4, len - 3) + "." + s.substring(len - 3, len - 2) + "." + s.substring(len - 2, len - 1) + "." + s.substring(len - 1, len);
        return out;

    }   // displayVersion()


    /****************************/
    /* Server status check      */
    /****************************/
    @Path("/status")
    @GET
    @Produces("text/plain")
    /**
    WebServices call via URL
    Return operating status of server.  Values are:
        Is server initialized
        Session state of server @see SessionState_Enum
    */
    public String getrStatus() {

        String msg = XML_PREAMBLE + "\n<result>";
        msg += "\n\t<status>" + "OK" + "</status>";
        msg += "\n\t<serverInitialized>" + serverInitialized + "</serverInitialized>";
        msg += "\n\t<sessionState>" + sessionState + "</sessionState>";
        msg += "\n\t<cyberActive>" + cyberAttack + "</cyberActive>";
        if (cyberAttack) {
            msg += "\n\t\t<activeAttacks>" + C2SIM_Cyber.activeAttacks + "</activeAttacks>";
            msg += "\n\t\t<messagesModified>" + C2SIM_Cyber.msgsModified + "</messagesModified>";
            msg += "\n\t\t<messagesDiscarded>" + C2SIM_Cyber.msgsDiscarded + "</messagesDiscarded>";
            msg += "\n\t\t<messagesUnModified>" + C2SIM_Cyber.msgsUnmodified + "</messagesModified>";
        }
        msg += "\n\t<unitDatabaseName>" + C2SIM_Util.initDB_Name + "</unitDatabaseName>";
        if (C2SIM_Util.initDB != null)
            msg += "\n\t<unitDatabaseSize>" + C2SIM_Util.initDB.entity.size() + "</unitDatabaseSize>";
        else
            msg += "\n\t<unitDatabaseSize>" + "0" + "</unitDatabaseSize>";
        msg += "\n\t<msgNumber>" + C2SIM_Server.msgNumber + "</msgNumber>";

        msg += "\n</result>";
        return msg;
    }


    /*********************************/
    /*  Cyber Command Processing     */
    /*********************************/
    /**
    * postCyber - Process cyber script included in message
    @param submitterID - Identification of submitter
    @param xmlText - XML Cyber script
    @return - Indication of success processing script
    @throws C2SIMException 
    */
    @Path("/cyber")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String postCyber(
            @QueryParam("submitterID") String submitterID, String xmlText)
            throws C2SIMException {

        // Call initialization methed.  If already initialized it will just return
        initialize();

        String result;

        ++msgNumber;

        // Log the command
        replayLogger.info("Cyber  " + msgNumber + "  " + submitterID + "  " + xmlText.replaceAll("\n", "").replaceAll(">\\s*<", "><"));
        debugLogger.info("Cyber Message Received - MSG_Number: " + msgNumber
                + " from submitter: " + submitterID
                + " " + xmlText.replaceAll("\n", "").replaceAll(">\\s*<", "><"));
        try {
            result = C2SIM_Cyber.cyberProcessCommand(xmlText, submitterID);
        }   // try
        catch (C2SIMException e) {
            debugLogger.error("Exception processing Cyber Command " + e.getCause());
            throw new C2SIMException("Exception processing Cyber Command", e.getCause());
        }   // catch   // catch

        return result;

    }   // postCyber()


    /**************************************/
    /*  Collect Statistics from Agent     */
    /***************************************/
    /**
    * postStats - Receive performance statistics from last transaction from this client
    *   Log to debug log
    @param submitterID - Identification of submitter
    @param xmlStats - Response time statistics from ast transaction
    @return - OK Message
    @throws C2SIMException 
    */
    @Path("/stats")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String postStats(
            @QueryParam("submitterID") String submitterID,
            String xmlStats)
            throws C2SIMException {

        // Call initialization methed.  If already initialized it will just return
        initialize();

        debugLogger.debug("Stats  " + ++msgNumber + " " + submitterID + " " + xmlStats);

        return createResultMsgOK("OK", "Statistics Recorded", msgNumber, 0.0);

    }   // postStats()   


    /************************************************/
    /* HTTP Get received                            */
 /*      Has no funciton at this time            */
 /*      Might in future be used for queries     */
    /************************************************/
    /**
    * getC2SIM - Method called when a GET HTTP command is sent to server
    *   Return HTML message indicating that C2SIM XML should be submitted via a POST
    @return 
    */
    @GET
    @Produces("text/html")
    public String getC2SIM() {
        String msg
                = "<!DOCTYPE html><html><head><title>Start Page</title><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head>"
                + "<body><center><h1>George Mason University  C4I-Cyber Center</h1></center>"
                + "<br><center><img src=\"/WEB-INF/classes/images/C4I-Cyber-Center.png\" alt=\"C4I Logo\"></center>"
                + "<br> <center><h2>C2SIM-BML Server - Version 4.4</h2></center>"
                + "<br><br> <center><h2>C2SIM / BML Functions are accessed via RESTful Web Services</h2></center>"
                + " </body></html>";
        return msg;
    }   // getC2SIM


    /********************/
    /* initialize()     */
    /********************/
    /**
    * initialize - Perform all initialization for C2SIM Server
    @throws C2SIMException 
    */
    static void initialize() throws C2SIMException {

        // The first transaction my cause this method to run.
        // If already initialized then return
        if (serverInitialized)
            return;

        try {
            debugLogger = LogManager.getLogger("edu.gmu.c4i.c2simserver4.debug");
            replayLogger = LogManager.getLogger("edu.gmu.c4i.c2simserver4.replay");
            cyberLogger = LogManager.getLogger("edu.gmu.c4i.c2simserver4.cyber");
        }
        catch (Exception e) {
            throw new C2SIMException("Error during log4j initialization", e);
        }

        // Mark beginning of run in debug log

        debugLogger.info("***********************  SERVER IS STARTING ***********************");

        // Set up properties object for runtime parameters
        try {
            props = new Properties();
            InputStream in = C2SIM_Server.class.getResourceAsStream(PROPERTIES_FILE_NAME);
            if (in == null)
                throw new C2SIMException("c2simServer.properties not found ");
            //props.load(new FileInputStream(PROPERTIES_FILE_NAME));
            props.load(in);
        }   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try   // try

        catch (Exception e) {
            throw new C2SIMException("Error while opening properties", e);
        }   // catch   // catch


        // Read in the schema DB
        try {

            //db = BMLSchemaProcess.readSchemaDB(dbFolder + props.getProperty("server.schema_db_name"));
            debugLogger.info("Reading Schema Database: " + props.getProperty("server.schema_db_name"));
            InputStream in = C2SIM_Server.class.getResourceAsStream("/" + props.getProperty("server.schema_db_name"));
            ObjectInputStream obj_in = new ObjectInputStream(in);
            C2SIM_Util.db = (C2SIMDB) obj_in.readObject();

        }   // try    // try    // try    // try 
        catch (Exception e) {
            debugLogger.error("Error reading schema database in C2SIM_ServerProess" + e);
            throw new C2SIMException("Error reading schema database", e);
        }   // catch      // catch   

        // Index the MessageDefinition Databaase
        C2SIM_Util.mdIndex = new HashMap<>();
        for (C2SIMMessageDefinition md : C2SIM_Util.db.messageDB) {
            C2SIM_Util.mdIndex.put(md.mdName, md);
        }

        // Get the minimum client version
        enforceVersion = C2SIM_Util.toBoolean(props.getProperty("server.enforceVersion", "T"));
        minimumClientVersion = props.getProperty("server.minimumClientVersion");
        minimumClientVersionValue = convertVersion(minimumClientVersion);

        // Get the server version from pom.properties
        Properties pomPoperties = new Properties();

        String ver = "";
        InputStream in = C2SIM_Server.class.getResourceAsStream("/../../META-INF/maven/edu.gmu.c4i.c2simserver4/C2SIMServer/pom.properties");

        if (in == null)
            ver = "UNKNOWN";
        else {
            try {
                pomPoperties.load(in);
                ver = pomPoperties.getProperty("version");

            }   // try
            catch (IOException e) {
                ver = "UNKNOWN";
            }   // catch

        }   // else

        if (ver.equalsIgnoreCase("UNKNOWN"))
            debugLogger.error("Unable to determine server version number");

        serverVersion = ver;

        replayLogger.debug("Reset  " + msgNumber + " " + "BML" + "************************************************* ");

        if (cyberAttack)
            cyberLogger.debug("Starting execution");

        // Logging setup is complete
        debugLogger.info("Logging setup complete");

        // Log the hostname and IP address
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getHostName();
            ipAddress = addr.getHostAddress();
        }
        catch (Exception e) {
            throw new C2SIMException("Error while determining hostname and address", e);
        }


        debugLogger.info("BMLServer running on " + hostName + " IP Address = " + ipAddress);
        debugLogger.info("Server Version =  " + serverVersion);
        debugLogger.info("Client Version Enforcement = " + enforceVersion + "  Minimum Client Version = " + minimumClientVersion);

        // See if we are to collect response time statistics
        collectStatistics = C2SIM_Util.toBoolean(props.getProperty("server.collectResponseTime"));

        // Get cyberAttack property
        String cA = props.getProperty("server.cyberAttack");

        // Default to cyberAttack OFF if not specified
        if (cA == null)
            cA = "0";

        // Decode cyberAttack and generate appropriate debug message
        cyberAttack = C2SIM_Util.toBoolean(cA);
        if (cyberAttack)
            debugLogger.info("Cyber attach is ACTIVE");
        else
            debugLogger.info("Cyber attack is not ACTIVE");

        // Get password for commands (Sent in clear)
        commandPassword = props.getProperty("server.c2sim_password");

        // Instantiate STOMP interface object - Read from properties file        
        C2SIM_Server_STOMP.initialize(C2SIM_Server.props, C2SIM_Server.debugLogger);

        // Initialize unitDB (C2SIM) and initDB
        C2SIM_Util.initDB = new C2SIM_InitDB();

        // Initialization Complete
        serverInitialized = true;
        sessionState = SessionState_Enum.UNINITIALIZED;

    }   // initialize()


    /****************************/
    /* createResultMsgOK()         */
    /*****************************/
    /**
    * createResultOK - Create OK message including current status of server
    @param status - "OK" indicating success of transaction
    @param message - Message describing result
    @param msgNumber - Current message number
    @param time - Clock time spend process message
    @return - XML Message to be returned to submitter by calling method
    */
    public static String createResultMsgOK(String status, String message, Integer msgNumber, Double time) {

        String msg = XML_PREAMBLE + "\n<result>";
        msg += "\n\t<status>" + status + "</status>";
        if (!message.equals(""))
            msg += "\n\t<message>" + message + "</message>";
        msg += "\n\t<serverInitialized>" + serverInitialized + "</serverInitialized>";
        msg += "\n\t<serverVersion>" + serverVersion + "</serverVersion>";
        msg += "\n\t<sessionState>" + sessionState + "</sessionState>";
        msg += "\n\t<unitDatabaseName>" + C2SIM_Util.initDB_Name + "</unitDatabaseName>";
        if (C2SIM_Util.initDB != null)
            msg += "\n\t<unitDatabaseSize>" + C2SIM_Util.initDB.entity.size() + "</unitDatabaseSize>";
        else
            msg += "\n\t<unitDatabaseSize>" + "0" + "</unitDatabaseSize>";
        msg += "\n\t<msgNumber>" + C2SIM_Server.msgNumber + "</msgNumber>";
        msg += "\n\t<time>" + String.format("%1$6.3f", time) + "</time>";

        // Do we want to the client to return statistics?
        if (collectStatistics)
            msg += "\n\t<collectResponseTime>T</collectResponseTime>";
        else
            msg += "\n\t<collectResponseTime>F</collectResponseTime>";

        msg += "\n</result>";

        return msg;

    }   // createResultMsgOK()


    /****************************/
    /* createResultMsgError()   */
    /*****************************/
    /**
    * createResultMsgError - Create error message to be returned to submitter
    @param status - Indication of success or failure
    @param message - Deailed message describing error
    @param msgNumber - Current message number
    @param ex - C2SIM Exception object contain details on error
    @return - FOrmatted XML message to be returned to submitter
    */
    public static String createResultMsgError(String status, String message, Integer msgNumber, C2SIMException ex) {
        String XML_START = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

        String msg = XML_START + "\n<result>";
        msg += "\n\t<status>" + status + "</status>";
        if (!message.equals(""))
            msg += "\n\t<message>" + message + "</message>";
        msg += "\n\t<serverInitialized>" + serverInitialized + "</serverInitialized>";
        msg += "\n\t<serverVersion>" + serverVersion + "</serverVersion>";
        msg += "\n\t<sessionState>" + sessionState + "</sessionState>";
        if (C2SIM_Util.initDB != null)
            msg += "\n\t<unitDatabaseSize>" + C2SIM_Util.initDB.entity.size() + "</unitDatabaseSize>";
        else
            msg += "\n\t<unitDatabaseSize>" + "0" + "</unitDatabaseSize>";
        msg += "\n\t<msgNumber>" + C2SIM_Server.msgNumber + "</msgNumber>";
        msg += "\n\t<time>" + "0.0" + "</time>";
        msg += "\n\t<error>" + ex.getMessage() + "</error>";
        msg += "\n\t<cause>" + ex.getCauseMessage() + "</cause>";
        msg += "\n</result>";

        return msg;

    }   // createResultMsgError()

    /**
    * SessionState - Enumeration of possiblt state of C2SIM Server
    */
    public enum SessionState_Enum {
        /*InitializationComplete, ShareScenario, StartScenario, SubmitInitialization*/
        UNINITIALIZED, INITIALIZING, INITIALIZED, RUNNING, PAUSED
    }

}   // class C2SIM_Server

// SessionState_Enum - enum Describes sessionState of Simulation Exercise 

