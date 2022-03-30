/*----------------------------------------------------------------*
|    Copyright 2001-20120Networking and Simulation Laboratory     |
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

import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ListIterator;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import java.lang.Math.*;
import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import java.util.ArrayList;
import java.util.Vector;


/**
 * <h1>C2SIM_Cyber</h1> <p>
 * Performs processing Cyber Scripts

 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_Cyber {

    public static String SISOSTD = "SISO-STD-C2SIM";
    static Namespace c2sim_NS = Namespace.getNamespace("http://www.sisostds.org/schemas/C2SIM/1.1");
    static Namespace ibml09_NS = Namespace.getNamespace("http://netlab.gmu.edu/IBML");

    public static int cyberMessageCount = 0;

    // ewa
    static Float ewa_Fraction;
    static Long ewa_Duration;
    static Long ewa_StartTime;
    static Long ewa_StopTime;

    // ewb
    static Float ewb_Fraction;
    static Long ewb_OnTime;
    static Long ewb_OffTime;
    static String ewb_State;
    static Long ewb_ChangeTime;

    // ewc
    static Long ewc_N;
    static Long ewc_Count;

    // cat2a
    static Float cat2a_LatOffset;
    static Float cat2a_LonOffset;

    // ewd
    static Float ewd_LatCenter;
    static Float ewd_LonCenter;
    static Float ewd_Distance;
    static Long ewd_Duration;
    static Long ewd_StopTime;

    // cyber_b
    static Long cyber_b_OffsetSeconds;

    // cyber_c
    static List<String> cyber_c_systemID;

    // cyber_x
    static String cyber_x_systemID;
    static Double cyber_x_Distance;
    static Double cyber_x_Bearing;

    // Flags inticating if specific attacks are currently active
    static Boolean ewa_Active = false;
    static Boolean ewb_Active = false;
    static Boolean ewc_Active = false;
    static Boolean cat_2a_Active = false;
    static Boolean ewd_Active = false;
    static Boolean cyber_b_Active = false;
    static Boolean cyber_c_Active = false;
    static Boolean cyber_x_Active = false;

    // Message accumulated while attacks active
    static String logMessage = "";
    static String logMessageDetail = "";
    static Integer innerReportsModified = 0;
    static Integer innerReportsDiscarded = 0;
    static Boolean messageModified = false;
    static Boolean messageDiscarded = false;

    // Attack statistics
    public static int msgsUnmodified = 0;
    public static int msgsDiscarded = 0;
    public static int msgsModified = 0;
    public static String activeAttacks = "";

    /**
    * cyberProcessCommand - Parse xml document containing Cyber instructions and extract elements
    @param xml - String/XML input
    @param submitter - String - Identification of submitter
    @return - Indicatiopn of success
    @throws C2SIMException 
    */
    public static String cyberProcessCommand(String xml, String submitter) throws C2SIMException {

        C2SIM_Server.cyberLogger.debug("Cyber processCommand submitted by " + submitter);
        C2SIM_Server.cyberLogger.info("Cyber Command:" + "\n" + xml);

        // Parse the document
        Document cDoc = C2SIM_Mapping.parseMessage(xml);

        // Category 1
        ewa_Fraction = new Float(getData(cDoc, "Category1/EWa/Fraction", "0"));
        ewa_Duration = new Long(getData(cDoc, "Category1/EWa/Duration", "0"));


        ewb_Fraction = new Float(getData(cDoc, "Category1/EWb/Fraction", "0"));
        ewb_OnTime = new Long(getData(cDoc, "Category1/EWb/MeanOnTime", "0"));
        ewb_OffTime = new Long(getData(cDoc, "Category1/EWb/MeanOffTime", "0"));

        ewc_N = new Long(getData(cDoc, "Category1/EWc/N", "0"));

        // Category 2
        cat2a_LatOffset = new Float(getData(cDoc, "Category2/Cat_2a/latOffset", "0"));
        cat2a_LonOffset = new Float(getData(cDoc, "Category2/Cat_2a/lonOffset", "0"));

        // Category 3
        ewd_LatCenter = new Float(getData(cDoc, "Category3/EWd/latCenter", "0"));
        ewd_LonCenter = new Float(getData(cDoc, "Category3/EWd/lonCenter", "0"));
        ewd_Distance = new Float(getData(cDoc, "Category3/EWd/distance", "0"));
        ewd_Duration = new Long(getData(cDoc, "Category3/EWd/duration", "0"));
        cyber_b_OffsetSeconds = new Long(getData(cDoc, "Category3/Cyber_b/offsetSeconds", "0"));
        cyber_c_systemID = getDataList(cDoc, "Category3/Cyber_c/systemID", "");
        cyber_x_systemID = getData(cDoc, "Category3/Cyber_x/systemID", "");
        cyber_x_Distance = new Double(getData(cDoc, "Category3/Cyber_x/distance", "0.0"));
        cyber_x_Bearing = new Double(getData(cDoc, "Category3/Cyber_x/bearing", "0.0"));


        String returnMsg = "";

        // Edit the commands - Send back error message if necessary

        // EWa
        if ((ewa_Fraction != 0) && (ewa_Duration == 0))
            returnMsg += "\nEWA Fraction and Duration must both be specified";


        // EWb
        if ((ewb_Fraction != 0) && ((ewb_OnTime == 0) || (ewb_OffTime == 0)))
            returnMsg += "\nEWb - If fraction is specified then OnTime and OffTime must also be specified";

        // Cat_2a
        if (((cat2a_LatOffset != 0) && (cat2a_LonOffset == 0)) || ((cat2a_LatOffset == 0) && (cat2a_LonOffset != 0)))
            returnMsg += "\nCat2a - If one of LatOffset and LonOffset is specified the other must be specified also";

        // EWd
        if (((ewd_LatCenter != 0) || (ewd_LonCenter != 0) || (ewd_Distance != 0) || (ewd_Duration != 0))
                && ((ewd_LatCenter == 0) || (ewd_LonCenter == 0) || (ewd_Distance == 0) || (ewd_Duration == 0)))
            returnMsg += "\nEWd - If one parameter is non zero then all parameters must be non zero.";

        // Cyber X
        if (!cyber_x_Bearing.equals("") || !cyber_x_Distance.equals("") || !cyber_x_systemID.equals(""))
            if (((cyber_x_Bearing.equals("") || cyber_x_Distance.equals("") || cyber_x_systemID.equals(""))))
                returnMsg += "\nCyber_X - If one parameter is specified all must be specified";

        if (!returnMsg.endsWith("")) {
            C2SIM_Server.cyberLogger.debug("Cyber commands rejected " + returnMsg);
            return returnMsg;
        }

        // At this point the parameters are OK - Figure out which attacks are to be run.
        //  It is possible to submit a different set of attacks while the server is running.
        //  Make sure the old X_Active settings are reset if the new file doesn't indicate that it is active

        if (ewa_Fraction != 0) {
            ewa_Active = true;
            ewa_Initialize();
        }
        else
            ewa_Active = false;


        if (ewb_Fraction != 0) {
            ewb_Active = true;
            ewb_Initialize();
        }
        else
            ewb_Active = false;


        if (ewc_N != 0) {
            ewc_Active = true;
            ewc_Initialize();
        }
        else
            ewc_Active = false;


        if (cat2a_LatOffset != 0) {
            cat_2a_Active = true;
            cat_2a_Initialize();
        }
        else
            cat_2a_Active = false;


        if (ewd_LatCenter != 0) {
            ewd_Active = true;
            ewd_Initialize();
        }
        else
            ewd_Active = false;


        if (cyber_b_OffsetSeconds != 0) {
            cyber_b_Active = true;
            cyber_b_initialize();
        }
        else
            cyber_b_Active = false;


        if (!cyber_c_systemID.get(0).equals("")) {
            cyber_c_Active = true;
            cyber_c_initialize();
        }
        else
            cyber_c_Active = false;


        if (!cyber_x_systemID.equals("")) {
            cyber_x_Active = true;
            cyber_x_initialize();
        }
        else
            cyber_x_Active = false;


        // Scan through X_Active variables build up a list of active attack to record in log
        activeAttacks = "";

        if (ewa_Active)
            activeAttacks += "EWa ";

        if (ewb_Active)
            activeAttacks += "EWb ";

        if (ewc_Active)
            activeAttacks += "EWc ";

        if (cat_2a_Active)
            activeAttacks += "Cat 2a ";

        if (ewd_Active)
            activeAttacks += "EWd ";

        if (cyber_b_Active)
            activeAttacks += "Cyber B ";

        if (cyber_c_Active)
            activeAttacks += "Cyber C ";

        if (cyber_x_Active)
            activeAttacks += "Cyber X ";

        if (activeAttacks.equals("")) {
            activeAttacks = "NONE Specified";
            C2SIM_Server.cyberLogger.debug("Cyber commands accepted - Stopping cyber attacks");
            return C2SIM_Server.createResultMsgOK("OK", "Stopping cyber attacks", C2SIM_Server.msgNumber, 0.0);
        }

        C2SIM_Server.cyberLogger.debug("Cyber commands accepted - Starting cyber attacks.  \nActive attacks are: " + activeAttacks);
        return C2SIM_Server.createResultMsgOK("OK", "New Cyber Commands processsed - Active attacks are: "
                + activeAttacks, C2SIM_Server.msgNumber, 0.0);

    } // cyberProcessCommand()


    /********************************/
    /* cyberProcessingMessage()     */
    /********************************/
    // Examine each incoming message and apply various attacks against them
    /**
    * cyberProcessMessage - Examine incoming message against each configured cyber attack
    @param tran - C2SIM_Transaction
    @return - String - Indication for caller, either "KEEP" or "DROP"
    @throws C2SIMException 
    */
    static String cyberProcessMessage(C2SIM_Transaction tran) throws C2SIMException {

        // Clear message tracking variables
        logMessage = "";
        logMessageDetail = "";
        innerReportsModified = 0;
        innerReportsDiscarded = 0;
        msgsDiscarded = 0;
        msgsModified = 0;
        messageModified = false;
        messageDiscarded = false;

        // Variables extracted from the four message types
        List<Element> reports = null;
        Element latitude = null;
        Element longitude = null;
        Element dateTime = null;
        String sourceID = "";
        String protocol = "";
        String msgDescriptor = "";


        try {

            // Only process C2SIM or IBML09
            protocol = tran.getProtocol();
            if (!((protocol.equalsIgnoreCase(SISOSTD)) || (protocol.equalsIgnoreCase("BML"))))
                return "KEEP";

            // Start processing of message
            msgDescriptor = tran.getMessageDef().messageDescriptor;
            Document doc = tran.getDocument();
            Element root = doc.getRootElement();
            Element order = null;
            protocol = tran.getProtocol();

            // Orders are subject only to EWa and Cyber-c  
            if (msgDescriptor.equalsIgnoreCase("C2SIM_Order") || (msgDescriptor.equalsIgnoreCase("IBML09_Order"))) {
                // Get order issuer
                if (msgDescriptor.equalsIgnoreCase("C2SIM_Order")) {
                    Element from = C2SIM_Util.findSingleElementSimple("DomainMessageBody/OrderBody/FromSender", root, c2sim_NS);
                    if (from != null)
                        sourceID = from.getText();
                    else
                        C2SIM_Server.debugLogger.debug("C2SIM Order id not contain FromSender");
                }   // C2SIM_Order

                if (msgDescriptor.equalsIgnoreCase("IBML09_Order")) {
                    Element tasker = C2SIM_Util.findSingleElementSimple("OrderPush/TaskerWho/UnitID", root, ibml09_NS);
                    if (tasker != null)
                        sourceID = tasker.getText();
                    else
                        C2SIM_Server.debugLogger.debug("IBML09 Order id not contain TaskerWho");
                }   // IBML09 Order

                // Execute the two attacks being used for Orders (If active)
                if (ewa_Active)
                    if (ewa_Attack(tran)) {
                        logMessageDetail = "\n\t - Message discarded by EWa ";
                        messageDiscarded = true;
                    }
                if (cyber_c_Active)
                    if (cyber_c_Attack(tran, sourceID)) {
                        logMessageDetail = "\n\t - Message discarded by cyber-c ";
                        messageDiscarded = true;
                    }
            }

            // Process Reports
            else {

                if (protocol.equalsIgnoreCase(SISOSTD)) {

                    // Get list of c2sim reports
                    reports = C2SIM_Util.findElementSimple("DomainMessageBody/ReportBody/ReportContent", root, c2sim_NS);
                    Element re = C2SIM_Util.findSingleElementSimple("DomainMessageBody/ReportBody/FromSender", root, c2sim_NS);
                    if (re == null)
                        return "Keep";

                    // Get reporter (Applies to all reports)
                    sourceID = re.getText();
                }
                else if (protocol.equalsIgnoreCase("BML")) {

                    // Get list of ibml09 reports
                    reports = root.getChildren("Report", ibml09_NS);
                }
                else
                    return "KEEP";

                // Pass each report through the active attack modules,
                ListIterator<Element> i = reports.listIterator();
                Element report;
                while (i.hasNext()) {
                    report = i.next();

                    // Get variables from C2SIM Report
                    if (protocol.equalsIgnoreCase(SISOSTD)) {
                        latitude = C2SIM_Util.findSingleElementSimple("PositionReportContent/Location/Coordinate/GeodeticCoordinate/Latitude", report, c2sim_NS);
                        if (latitude == null)
                            return "Keep";
                        longitude = C2SIM_Util.findSingleElementSimple("PositionReportContent/Location/Coordinate/GeodeticCoordinate/Longitude", report, c2sim_NS);
                        dateTime = C2SIM_Util.findSingleElementSimple("PositionReportContent/TimeOfObservation/IsoDateTime", report, c2sim_NS);
                        if (dateTime == null)
                            return "Keep";
                    }   // C2SIM

                    // Get variables from IBML09 Report
                    else {
                        latitude = C2SIM_Util.findSingleElementSimple("StatusReport/GeneralStatusReport/WhereLocation/GDC/Latitude", report, ibml09_NS);
                        if (latitude == null)
                            return "Keep";
                        longitude = C2SIM_Util.findSingleElementSimple("StatusReport/GeneralStatusReport/WhereLocation/GDC/Longitude", report, ibml09_NS);
                        dateTime = C2SIM_Util.findSingleElementSimple("StatusReport/GeneralStatusReport/When", report, ibml09_NS);
                        if (dateTime == null)
                            return "Keep";
                        Element re = C2SIM_Util.findSingleElementSimple("StatusReport/GeneralStatusReport/ReporterWho/UnitID", report, ibml09_NS);
                        if (re == null)
                            return "Keep";
                        sourceID = re.getText();

                    }   // ibml09

                    // Attack methods return true if inndividual report is to be discarded

                    if (ewa_Active)
                        if (ewa_Attack(tran)) {
                            i.remove();
                            continue;
                        }


                    if (ewb_Active)
                        if (ewb_Attack(tran)) {
                            i.remove();
                            continue;
                        }


                    if (ewc_Active)
                        if (ewc_Attack(tran)) {
                            i.remove();
                            continue;
                        }

                    if (cat_2a_Active)
                        if (cat_2a_Attack(tran, latitude, longitude)) {
                            i.remove();
                            continue;
                        }


                    if (ewd_Active)
                        if (ewd_Attack(tran, latitude, longitude)) {
                            i.remove();
                            continue;
                        }

                    if (cyber_b_Active)
                        if (cyber_b_Attack(tran, dateTime, protocol)) {
                            i.remove();
                            continue;
                        }

                    if (cyber_c_Active)
                        if (cyber_c_Attack(tran, sourceID)) {
                            i.remove();
                            continue;
                        }

                    if (cyber_x_Active)
                        if (cyber_x_Attack(tran, latitude, longitude, sourceID)) {
                            i.remove();
                            continue;
                        }

                }   // report loop

                // Were all the reports in the message discarded?
                if (reports.size() == 0) {
                    // Discard the entire document
                    messageDiscarded = true;
                    logMessageDetail += "\n\t - All inner reports discarded - Parent document is discarded - Message Number: " + tran.msgnumber;
                }   // reports all discarded

                // Were any reports modified or discarded?
                if ((innerReportsModified > 0) || (innerReportsDiscarded > 0))
                    messageModified = true;
            }   // reports      

            // All active attacks have been run  
            // If any of them set messageMpodified or messageDiscarded then take action

            logMessage = "\tMessage: " + tran.getMsgnumber();

            // Was the message neither modified nor discarded?
            if ((!messageModified) && (!messageDiscarded)) {

                logMessage += " was not modified or discarded" + logMessageDetail;
                ++msgsUnmodified;
                C2SIM_Server.cyberLogger.debug(logMessage);
                return "KEEP";
            }   // No attack

            // Was the message discarded?
            if (messageDiscarded) {
                ++msgsDiscarded;
                logMessage += " was discarded \nAttacks:" + logMessageDetail;
                C2SIM_Server.cyberLogger.debug(logMessage);
                return "DROP";
            }   // Discarded

            // Was the message modified?
            if (messageModified) {
                ++msgsModified;
                logMessage += " was modified \nAttacks: " + logMessageDetail;

                // Replace the XML in the transaction so that the modified XML is published.
                tran.setXmlText(C2SIM_Util.xmlToStringD(tran.getDocument(), tran));
                C2SIM_Server.cyberLogger.debug(logMessage);
                return "KEEP";
            }   // Modified

            return "KEEP";

        }   // try
        catch (Exception e) {
            C2SIM_Server.debugLogger.error("Exception in cyberProcessMessage " + e);
            throw new C2SIMException(e.getLocalizedMessage());
        }   // catch   // catch

    }   // cyberProcessMessage()


    /************/
    /*  EWa     */
    /************/
// Block messages randomly for a specified duration
// Block a fraction of messages for a specified duration
    /**
    * ewa_Initialize - Initialize ewa attack
    */
    static void ewa_Initialize() {

        // Fixed duration - Compute time to stop
        ewa_StopTime = System.currentTimeMillis() + ewa_Duration * 1000;
        ewa_Active = true;

    }   // ewa_Initialize()


    /**
    * ewa_Attack - Perform ewa Attack 
    @param t - C2SIM_Transaction
    @return boolean - If attack was successful
    */
    static boolean ewa_Attack(C2SIM_Transaction t) {

        Double r = 0.0;

        // Have we reached the duration?
        if (System.currentTimeMillis() > ewa_StopTime) {
            ewa_Active = false;
            logMessageDetail += "\n\tEWa Attack time expired - Attack EWa is disabled";
            return false;
        }

        // Should we discard this message?
        r = Math.random();
        if (r < ewa_Fraction) {
            logMessageDetail += "\n\tEWa - Report discarded in Message " + t.msgnumber;
            ++innerReportsDiscarded;
            return true;
        }
        return false;

    }   // ewa_Attack()


    /************/
    /*  EWb     */
    /************/
    // Block fraction of messages with random on/off times
    /**
    * ewb_Initialize - Initialize ewb attack
    */
    static void ewb_Initialize() {

        // Start with attack ON
        ewb_State = "ON";

        // Compute time to change sessionState to OFF
        Double changeTime = System.currentTimeMillis() + ewb_OnTime * 1000 * 2.0 * Math.random();
        ewb_ChangeTime = changeTime.longValue();

    }   // ewb_Initialize


    /**
    * ewb_Attack - Perform ewb_Attack
    @param t C2SIM_Transaction
    @return - Boolean Attack succeeded
    */
    static boolean ewb_Attack(C2SIM_Transaction t) {

        // Block messages randomly with random off and on times

        Double changeTime;
        Double r;
        Double x;
        boolean discardMessage = false;

        Document d = t.getDocument();

        if (ewb_State.equalsIgnoreCase("ON")) {

            // State is ON
            // Should we discard this report?
            if (Math.random() < ewb_Fraction) {
                ++innerReportsDiscarded;
                logMessageDetail += "\n\tEWB - Discarded Message";
            } // discard report

            // Time to change states?
            if (ewb_ChangeTime < System.currentTimeMillis()) {
                ewb_State = "OFF";
                r = Math.random();

                // Compute randomized time until turned on (Milliseconds)
                changeTime = System.currentTimeMillis() + ewb_OffTime * 1000 * 2.0 * r;
                ewb_ChangeTime = changeTime.longValue();

                x = ewb_OffTime * 2.0 * r;
                logMessageDetail += "\n\tEWb - Attack turned OFF for " + x.longValue() + " seconds";
            } // Change states

        }   // State was ON

        else {
            // State is OFF
            if (ewb_ChangeTime < System.currentTimeMillis()) {
                ewb_State = "ON";
                r = Math.random();
                x = ewb_OnTime * 2.0 * r;
                logMessageDetail += "\n\tEWb - Attack turned ON for " + x.longValue() + " seconds";
                changeTime = System.currentTimeMillis() + x * 1000;
                ewb_ChangeTime = changeTime.longValue();
            }   // Change states   
        }   // State was off

        return discardMessage;

    }   // ewb_Attack()


    /************/
    /*  EWc     */
    /************/
    // Block (discard) every Nth message
    /**
    * ewc_Initialize 
    */
    static void ewc_Initialize() {

        ewc_Count = ewc_N;

    }   // ewc_Initialize

    /**
    * ewc_Attack - Perform ewc attack 
    @param t C2SIM_Transaction
    @return - Bpolean - Attack succeed
    */
    static boolean ewc_Attack(C2SIM_Transaction t) {

        Document d = t.getDocument();
        Double r;

        // Should we discard this Report?
        if (--ewc_Count == 0) {
            ewc_Count = ewc_N;
            logMessageDetail += "\n\tEWc - Count reached 0, Inner Report Discarded";
            ++innerReportsDiscarded;
            return true;
        }   // Count reached zero

        return false;

    } // ewc_Attack)_


    /****************/
    /*  Cat-2a      */
    /****************/
    // Modify positions in all reports by a lat and lon offset
    /**
    * cat_2a_Initialize
    */
    static void cat_2a_Initialize() {

        // No initialization

    }   // cat_2a_Initialize

    /**
    * cat_2a_Attack - Perform cat_2a Attack Modify positions in reports by lat and lon offset
    @param t
    @param eLat
    @param eLon
    @return 
    */
    static boolean cat_2a_Attack(C2SIM_Transaction t, Element eLat, Element eLon) {
        // Process Position/General Status Reports.  Modify report position latitude and longitude
        ;
        String lat = eLat.getText();
        String lon = eLon.getText();

        // Convert each to Double add the offset, convert back to string and update the elements
        Float fLat = Float.parseFloat(lat);
        Float fLon = Float.parseFloat(lon);

        fLat += cat2a_LatOffset;
        fLon += cat2a_LonOffset;

        lat = fLat.toString();
        lon = fLon.toString();

        eLat.setText(lat);
        eLon.setText(lon);

        logMessageDetail += "\n\tCat-2a - Modified latitude and longitude";
        ++innerReportsModified;

        return false;

    }   // cat_2a_Attack()


    /*************/
    /*   EWd     */
    /*************/
    // 
    /**
    * ewd_Attack - Perform ewd Attack - Block messages from a specific (Circular) area specified by Lat, lon, and distance from the point
    @param t - C2SIM_Transaction
    @param eLat - Element - Current latitude
    @param eLon - Element - Current longitude
    @return Boolean - Attack succeeded
    */
    static boolean ewd_Attack(C2SIM_Transaction t, Element eLat, Element eLon) {

        String lat = eLat.getText();
        String lon = eLon.getText();

        Double fLat = Double.parseDouble(lat);
        Double fLon = Double.parseDouble(lon);

        // Compute distance from given center point and reported point
        Double dist = distance(fLat, ewd_LatCenter, fLon, ewd_LonCenter, 0, 0);
        if (dist < ewd_Distance) {
            ++innerReportsDiscarded;
            logMessageDetail += "\n\tEWd - Distance in report = " + dist.longValue() + " threshold distance = " + ewd_Distance;
            return true;
        }   // discard message

        return false;

    }   // ewd_Attack

    
    /**
    * ewd_Initialize
    */
    static void ewd_Initialize() {

        ewd_StopTime = System.currentTimeMillis() + ewd_Duration / 1000;

    }   // ewd_Attack


    /**
    
    */
    /************/
    /* Cyber_b  */
    /************/
    /**
    * cyber_b_attack - Modify report time by specified number of seconds
    @param t - C2SIM_Transaction
    @param eTime - Element - Current time in report
    @param protocol - Protocol used for message
    @return - Boolean - Success of attack
    @throws C2SIMException 
    */
    static boolean cyber_b_Attack(C2SIM_Transaction t, Element eTime, String protocol) throws C2SIMException {

        SimpleDateFormat sdf = null;

        // Get the ReportingTime element as a string
        String rt = eTime.getText();

        // Parse the date converting it to a Date object

        if (protocol.equalsIgnoreCase(SISOSTD))
            sdf = new SimpleDateFormat(C2SIM_Util.c2simFormat);
        else
            sdf = new SimpleDateFormat(C2SIM_Util.ibml09Format);

        Date date = null;
        try {
            date = sdf.parse(rt);
        }
        catch (ParseException pe) {
            C2SIM_Server.debugLogger.error("Error parsing date in cyber_b" + pe);
            throw new C2SIMException("Parse exception parsing date from Position Report", pe.getCause());
        }

        // Create a Calendar object and set it to the time in the report
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);

        // Add the number of seconds specified to this date/time
        cal.add(GregorianCalendar.SECOND, cyber_b_OffsetSeconds.intValue());

        // Convert back to a string and put it back in the Report
        String adjustedTime = sdf.format(cal.getTime());
        eTime.setText(adjustedTime);

        ++innerReportsModified;
        logMessageDetail += "\n\tCyber B - Report time modified";

        return false;
    }   // cyber_b_Attack()

    /**
    * cyber_b_initialize
    */
    static void cyber_b_initialize() {

        // No initialization

    }   // cyber_b_initialize()


    /*************/
    /* Cyber C  */
    /*************/
    // Block messages from a specific reporter\
    /**
    * cyber_c_initialize
    */
    static void cyber_c_initialize() {

        // No initialization

    }   // cyber_c_initialize()

    /**
    * cyber_c_attack - Block messages from a specific reporter\
    @param t
    @param sourceID
    @return 
    */
    static boolean cyber_c_Attack(C2SIM_Transaction t, String sourceID) {

        // Work through list of systemID's to be attacked and see if this is one of them
        for (String id : cyber_c_systemID) {

            if (sourceID.equalsIgnoreCase(id)) {
                ++innerReportsDiscarded;
                logMessageDetail += "\n\tCyber_C - ReportingEntity matched target name";
                return true;
            }
        }
        return false;

    }   // cyber_c_Attack


    /************/
    /*  Cyber_X */
    /************/
    // Modify reported position to messages from a specific reporter
    /**
    * cyber_x_initialize
    */
    static void cyber_x_initialize() {

        // No initialization

    }   // cyber_x_initialize()

    /**
    * cyber_x_Attack - Modify reported position to messages from a specific reporter
    @param t
    @param eLat - Element current lat
    @param eLon - ELement current lon
    @param reporter - String - reporter name
    @return - Boolean - Success of attack
    @throws C2SIMException 
    */
    static boolean cyber_x_Attack(C2SIM_Transaction t, Element eLat, Element eLon, String reporter) throws C2SIMException {


        if (reporter.equalsIgnoreCase(cyber_x_systemID)) {

            Double latitude = new Double(eLat.getText());
            Double longitude = new Double(eLon.getText());

            // Move the point (This is VERY approximate);

            // Distance in degrees
            Double distDeg = cyber_x_Distance / 111120.0;

            latitude += distDeg * cos(Math.toRadians(cyber_x_Bearing));
            longitude += distDeg * sin(Math.toRadians(cyber_x_Bearing));
            //movePoint(latitude, longitude, cyber_x_Distance, cyber_x_Bearing);
            eLat.setText(latitude.toString());
            eLon.setText(longitude.toString());
            ++innerReportsModified;
            logMessageDetail += "\n\tCyber X - Location Modified for report from " + cyber_x_systemID;
        }
        return false;

    }   // cyber_x_Attack


    /****************/
    /*  getData     */
    /****************/
    
    /**
    * getData - Given a parsed JDOM document and a path (Element names separated by /'s) find and return the data element
    @param d - Document to be searched 
    @param path - String simple String path
    @param defaultVal - String - Default value if not found
    @return String result
    */
    static String getData(Document d, String path, String defaultVal) {

        // Path is node names separated by "/" 
        String[] els = path.split("/");
        Element e1 = d.getRootElement();
        for (String s : els) {
            e1 = e1.getChild(s, c2sim_NS);
            if (e1 == null)
                return defaultVal;
        }
      
        return e1.getText();
    }   // getData()


    /********************/
    /*  getDataList     */
    /********************/
    // Given a parsed JDOM document and a path (Element names separated by /'s) find and return the list of data elements
    /**
    * getDataList - Given a parsed JDOM document and a path (Element names separated by /'s) find and return the list of data elements 
    @param d - Document to be searched 
    @param path - String simple String path
    @param defaultVal - String - Default value if not found
    @return List of String results
    */
    static List<String> getDataList(Document d, String path, String defaultVal) {

        Element el;

        List<String> result = new Vector<>();
        List<Element> resultEl = null;

        // Path is node names separated by "/" 
        String[] els = path.split("/");

        // Get Root
        el = d.getRootElement();
        // Work down to one before the last element
        for (int i = 0; i < els.length - 1; ++i) {
            el = el.getChild(els[i], c2sim_NS);
            
            if (el == null) {
                List l = new Vector<String>();
                l.add(defaultVal);
                return l;
            }   // if null    
            
        }   // for

        // The last element should hava maxOccurs > 1.  Get a list of elements
        resultEl = el.getChildren();
                
        for (Element e : resultEl) {
            //String x = e.getText();
            result.add(e.getText());
        }
        return result;

    }   // getDataList()

    /**
    * distance Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.- 
    @param lat1 - double first latitude
    @param lat2 - double second latitude
    @param lon1 - double first longitude
    @param lon2 - double second longitude
    @param el1    double first elevation
    @param el2    double second elevation
    @return 
    */
    public static double distance(double lat1, double lat2, double lon1,
            double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    /**
    * move point - Change lat/lon by distance and bearing
    @param latitude Double - starting latitude.  Changed by method
    @param longitude Double - starting longitude Changed by method
    @param distance Double distance in meters
    @param bearing DOuble bearing in degrees
    */
    static void movePoint(Double latitude, Double longitude, Double distance, Double bearing) {

        // This is VERY approximate
        Double distDeg = distance / 111120.0;
        Double latChange = distance / 111120.0;  // meters  in 1 degree
        Double lonChange = distance / 111120.0;

        latitude = new Double(latitude + latChange * cos(toRadians(bearing)));
        longitude = new Double(longitude + lonChange * sin(toRadians(bearing)));

    }   // mocePoinr()


}   // class C2SIM_Sever_Cyber

