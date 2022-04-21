/*----------------------------------------------------------------*
|    Copyright 2001-2019 Networking and Simulation Laboratory     |
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

//import edu.gmu.c4i.c2simclientlib2.C2SIMHeader;
import edu.gmu.c4i.c2simclientlib2.C2SIMHeader;
import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;
import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_InitDB.*;


/**
 * <h1>C2SIM_C2SIM</h1> <p>
 * Performs processing of C2SIM messages

 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_C2SIM {

    public static String SISOSTD = "SISO-STD-C2SIM";
    public static String c2simVersion = "";
            
    static String fromSender;
    static String toReceiver;
    static boolean translate9To1 = C2SIM_Util.toBoolean(C2SIM_Server.props.getProperty("server.Translate9To1"));

    static {

        fromSender = "00000000-0000-0000-0000-000000000000";
        toReceiver = "00000000-0000-0000-0000-000000000001";
    }


    /************/
    /* process  */
    /************/
    /*
        Perform initial processing on C2SIM Message
        Determine messaage type and call appropriate method
     */
    /**
     * process - Start processing C2SIM Message
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
     */
    public static void process(C2SIM_Transaction trans) throws C2SIMException {

        // If this a version 9 message?
        if (trans.c2SIM_Version.equals("0.0.9")) {

            // Is version 0.0.9 processing enabled?
            if (!translate9To1)
                throw new C2SIMException("C2SIM Version 0.0.9 processing is disabled in server");

            /* If this is an Order or a Report:
                Publish the original V9 message.  
                Translate to V10
                Publish the V10 message
                Don't publish V9 Initialization,
             */
            if ((trans.messageDef.messageDescriptor.equals("C2SIM_Order")) || (trans.messageDef.messageDescriptor.equals("C2SIM_Report"))) {
                C2SIM_Server.debugLogger.info("publishing version:" + trans.getc2SIM_Version() + " xml:"+trans.getXmlMsg());
                C2SIM_Server_STOMP.publishMessage(trans);
                trans = c2SIM_Translate_9To100(trans);
                C2SIM_Server.debugLogger.info("also publishing version:" + trans.getc2SIM_Version() + " xml:"+trans.getXmlMsg());
                C2SIM_Server_STOMP.publishMessage(trans);
            }
            // If not a report or order then this must be a Initialization message.  Translate V9 to V100.  
            //  It will be processed and added to accumulated initialization data

            else
                trans = c2SIM_Translate_9To100(trans);

        }   // Version 9

        else {

            // If this is a V1.0.x Order or  Report then translate to V9 and publish  Don't publish or process translated V9 Initialization message
            if ((trans.messageDef.messageDescriptor.equals("C2SIM_Order")) || (trans.messageDef.messageDescriptor.equals("C2SIM_Report"))
                    || (trans.messageDef.messageDescriptor.equals("C2SIM_ASX_Report"))) {
                C2SIM_Server_STOMP.publishMessage(trans);
                if (translate9To1) {
                    C2SIM_Transaction trans9 = c2SIM_Translate_100To9(trans);
                    C2SIM_Server_STOMP.publishMessage(trans9);
                }
            }   // Order / Report translated to V9
        }   // Version 11

        // Continue to process V11 message.

        if (trans.messageDef.messageDescriptor.equalsIgnoreCase("C2SIM_Initialization"))
            process_C2SIM_Initialization(trans);

        else if (trans.messageDef.messageDescriptor.equalsIgnoreCase("C2SIM_Order"))
            process_C2SIM_Order(trans);

        else if (trans.messageDef.messageDescriptor.equalsIgnoreCase("C2SIM_Report"))
            process_C2SIM_Report(trans);

        else if (trans.messageDef.messageDescriptor.equalsIgnoreCase("C2SIM_ASX_Report"))
            process_C2SIM_Report(trans);

        else
            // Publish the original message
            C2SIM_Server_STOMP.publishMessage(trans);
        return;
    }   // process())


    /****************/
    /* identify()   */
    /****************/
    // Identify the type of C2SIM message and return the appropriaate BMLMessaageDefinition object
    /**
     * identify - ID the specifc C2SMIM message typt
    @param d - Document containing message
    @return C2SIMMessageDefinition
    @throws C2SIMException 
     */
    static C2SIMMessageDefinition identify(Document d) throws C2SIMException {

        Element root = d.getRootElement();
        C2SIMMessageDefinition md = null;

        // Is this ObjectInitialization or ObjectInitializationBody or C2SIMInitialization ?        

        Element objectInitialization = C2SIM_Util.findSingleElementSimple("ObjectInitialization", root, C2SIM_Util.c2sim_NS);
        if (objectInitialization == null)
            objectInitialization = C2SIM_Util.findSingleElementSimple("ObjectInitializationBody", root, C2SIM_Util.c2sim_NS);
        if (objectInitialization == null)
            objectInitialization = C2SIM_Util.findSingleElementSimple("C2SIMInitializationBody", root, C2SIM_Util.c2sim_NS);

        if (objectInitialization != null) {
            md = C2SIM_Util.mdIndex.get("C2SIM_Initialization");
            return md;
        }

        // Is thie an order - See if we have any tasks
        List<Element> tl = C2SIM_Util.findElementSimple("DomainMessageBody/OrderBody/Task", root, C2SIM_Util.c2sim_NS);
        if (tl.size() != 0) {
            // We have tasks.  Set default to C2SIM_Order
            md = C2SIM_Util.mdIndex.get("C2SIM_Order");

            // See if it's an ASX order
            for (Element e : tl) {
                Element mw = e.getChild("ManeuverWarfareTask", C2SIM_Util.c2sim_NS);
                if (mw != null)
                    continue;
                // Do we have an AS MW task?
                Element asmwt = e.getChild("AutonomousSystemManeuverWarfareTask", C2SIM_Util.c2sim_NS);
                if (asmwt != null) {
                    // This is an ASX Order
                    md = C2SIM_Util.mdIndex.get("C2SIM_ASX_Order");
                    return md;
                }   // C2SIM_ASX Order
            }   // List of tasks
            return md;
        }   // C2SIM Order

        // Is this a C2SIM Report?
        List<Element> rl = C2SIM_Util.findElementSimple("DomainMessageBody/ReportBody/ReportContent", root, C2SIM_Util.c2sim_NS);
        if (rl.size() != 0) {
            // Set default to non ASX Report
            md = C2SIM_Util.mdIndex.get("C2SIM_Report");
            // See if any of the reports are ASX reports 
            for (Element r : rl) {
                Element prc = r.getChild("PositionReportContent", C2SIM_Util.c2sim_NS);
                // Position Report?
                if (prc != null) {
                    // Is it an ASX Position Report?

                    Element asprc = prc.getChild("AutonomousSystemPositionReportContent", C2SIM_Util.c2sim_NS);
                    if (asprc != null) {
                        md = C2SIM_Util.mdIndex.get("C2SIM_ASX_Report");
                        return md;
                    } // Automonous System Position Report
                    md = C2SIM_Util.mdIndex.get("C2SIM_PositionReport_Single");
                    return md;
                }   // Position Report

                // Is it an Observation Report?
                Element orc = r.getChild("ObservationReportContent", C2SIM_Util.c2sim_NS);
                if (orc != null) {
                    // Is it an ASX Observation Report
                    Element asorc = orc.getChild("AutonomousSystemObservationReportContent", C2SIM_Util.c2sim_NS);
                    if (asorc != null) {
                        md = C2SIM_Util.mdIndex.get("C2SIM_ASX_Report");
                        return md;
                    }   // Automonous System Observation Report

                    // It is a regular Observation Report - Treat it like a Position Report ?!?!
                    md = C2SIM_Util.mdIndex.get("C2SIM_PositionReport_Single");
                    return md;

                }   // Observation Report

            }   // Reports
            // Regular C2SIM Position Repor
            return md;
        }   // C2SIM Report

        // Nothing matched.  Set MD to C2SIM_Other
        md = C2SIM_Util.mdIndex.get("C2SIM_Other");
        return md;

    }   // identify()


    /********************************/
    /* process_C2SIM_Initialization */
    /********************************/
    /**
     * process_C2SIM_Initialization - Process contents of C2SIM_Initialization message and add contents to initDB
    @param t
    @throws C2SIMException 
     */
    static void process_C2SIM_Initialization(C2SIM_Transaction t) throws C2SIMException {

        try {

            // If this is the first Initialization transaction, save the C2SIM Version
            if (c2simVersion == "")
                c2simVersion = t.getc2SIM_Version();
            else
                c2simVersion = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
            
            // Find all initialization elements in this message and save them pending a SHARE command 
            Element root;       // Root element (MessageBody)
            Element objectInitialization;         //
            Namespace ns = C2SIM_Util.c2sim_NS;

            // Get the root element - Should be MessageBody
            root = t.doc.getRootElement();

            // Get the ObjectInitialization element, the root of initialization data being submitted
            objectInitialization = C2SIM_Util.findSingleElementSimple("ObjectInitialization", root, ns);

            if (objectInitialization == null)
                objectInitialization = C2SIM_Util.findSingleElementSimple("ObjectInitializationBody", root, ns);

            if (objectInitialization == null)
                objectInitialization = C2SIM_Util.findSingleElementSimple("C2SIMInitializationBody", root, ns);

            /*
        Work through each type of initialization element in ObjectOnitialization
            InitializationDataFile
            AbstractObject
            Action
            Entity
            PlanPhaseReference
            ScenarioSetting
             */
            // InitializationDataFile
            List<Element> initDataFiles = C2SIM_Util.findElementSimple("InitializationDataFile", objectInitialization, ns);
            for (Element initDataFile : initDataFiles) {
                C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
                ie.element = initDataFile.clone();
                ie.source = t.sender;
                C2SIM_Util.initDB.initDataFile.add(ie);
                //BMLServer.initDB.initDataFile.
            }

            List<Element> objDefs = C2SIM_Util.findElementSimple("ObjectDefinitions", objectInitialization, ns);

            for (Element objDef : objDefs) {
                
                // AbstractDataObject

                List<Element> abstObjs = C2SIM_Util.findElementSimple("AbstractObject", objDef, ns);
                for (Element abstObj : abstObjs) {
                    C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
                    ie.element = abstObj.clone();
                    ie.source = t.sender;
                    C2SIM_Util.initDB.abstractObject.add(ie);

                    // Is it a ForceSice?
                    Element forceSide = C2SIM_Util.findSingleElementSimple("ForceSide", ie.element, ns);
                    if (forceSide != null)
                        C2SIM_Util.forceSideMap.put(forceSide.getChildText("UUID"), forceSide);

                }   // AbstractDataObject

                // Action
                List<Element> act = C2SIM_Util.findElementSimple("Action", objDef, ns);
                while (!act.isEmpty()) {
                    C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
                    ie.element = act.get(0).detach();
                    ie.source = t.sender;
                    C2SIM_Util.initDB.action.add(ie);
                }   // Action

                // Entity
                List<Element> ent = C2SIM_Util.findElementSimple("Entity", objDef, ns);
                while (!ent.isEmpty()) {
                    C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
                    ie.element = ent.get(0).detach();
                    ie.source = t.sender;
                    C2SIM_Util.initDB.entity.add(ie);
                    
                    // If this is a Route increment route count
                    Element route = C2SIM_Util.findSingleElementSimple("PhysicalEntity/MapGraphic/TacticalGraphic/Line/Route", ie.element, ns);
                    if (route != null) {
                        String uuid = route.getChildText("UUID", ns);
                        if(uuid != null)C2SIM_Util.numC2SIM_Routes++;
                        // Add to the InitDB
                        continue;
                    }

                    // If this is a MilitaryOrganization index it by its UUID
                    Element mo = C2SIM_Util.findSingleElementSimple("ActorEntity/CollectiveEntity/MilitaryOrganization", ie.element, ns);
                    if (mo != null) {
                        String uuid = mo.getChildText("UUID", ns);
                        // Add to the unitMap
                        C2SIM_Util.unitMap.put(uuid, mo);
                        C2SIM_Util.numC2SIM_Units++;

                        // If name exists build cross reference for name and UUID
                        String name = "";
                        Element nameE = mo.getChild("Name", ns);
                        if (nameE != null)
                            name = nameE.getText();
                        if (!name.equalsIgnoreCase("")) {
                            C2SIM_Util.nameToUUID.put(name, uuid);
                            C2SIM_Util.uuidToName.put(uuid, name);
                        }
                    }   // if this is a MO

                }   // while

                // Plan Phase Reference
                List<Element> ppr = C2SIM_Util.findElementSimple("PlanPhaseReference", objDef, ns);
                while (!ppr.isEmpty()) {
                    C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
                    ie.element = ppr.get(0).detach();
                    ie.source = t.sender;
                    C2SIM_Util.initDB.plan.add(ie);
                }   // PlanPhaseReference

            }   // ObjectDefinitions
            
            // SystemEntityList
            List<Element> selL = C2SIM_Util.findElementSimple("SystemEntityList", objectInitialization, ns);
            while (!selL.isEmpty()) {
                C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
                ie.element = selL.get(0).detach();
                ie.source = t.sender;
                C2SIM_Util.initDB.systemEntityList.add(ie);
            }   // SystemEntityList

            // ScenarioSetting
            List<Element> ss = C2SIM_Util.findElementSimple("ScenarioSetting", objectInitialization, ns);
            while (!ss.isEmpty()) {
                C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
                ie.element = ss.get(0).detach();
                ie.source = t.sender;
                C2SIM_Util.initDB.scenario.add(ie);
            }   // Action

            C2SIM_Util.numC2SIM_ForceSides = C2SIM_Util.forceSideMap.size();
            ++C2SIM_Util.numInitMsgs;

            C2SIM_Util.publishNotification("New_C2SIM_ObjectInitialization", t);
        }   // try
        catch (Exception e) {
            StackTraceElement[] st = e.getStackTrace();
            throw new C2SIMException("Error processing C2SIM Initialization " + e);
        }   // catch   // catch
    }   // processC2SIM_Initialization())


    /************************/
    /* process_C2SIM_Order  */
    /************************/
    /**
    process_C2SIM_Order - Process Order and manage translation to other protocols
    @param t - C2SIM_Transaction
    @throws C2SIMException 
     */
    public static void process_C2SIM_Order(C2SIM_Transaction t) throws C2SIMException {


        Document orderCore;
        Document saveOrderCore;
        Document d;
        Element root;
        String lat = "";
        String lon = "";
        String el = "";
        Element eLine = null;
        Element eWhereClass;
        Element eWhereCategory;
        Element eAtWhere;
        Element ePoint;


        // Find the order body
        Element order = C2SIM_Util.findSingleElementSimple("DomainMessageBody/OrderBody", t.getDocument().getRootElement(), C2SIM_Util.c2sim_NS);

        // Put order rooted at OrderBody into a document
        d = new Document();
        d.addContent(order.detach());

        // Map C2SIM Order to Core
        orderCore = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("C2SIM_Order"), d, "F");

        // Map location information from C2SIM Order to Core
        translateC2SIMLocationToCore(order, orderCore);

        // Save copy of orderCore for other dialects
        saveOrderCore = orderCore.clone();

        // Add orderCore to transaction
        t.setCoreDocument(orderCore);

        // Translate core to IBML09 Order and publish
        C2SIM_IBML09.translateCoreToIBML09_Order(t);

        // Translate core to CBML Order and publish
        C2SIM_CBML.translateCoreToCBML_Order(t);

    }   // process_C2SIM_Order()


    /************************************/
    /* translateC2SIMLocationToCore     */
    /************************************/
    // Map location information from input C2SIM Order (Positioned at Order Body) to output (orderCore)
    /**
     * translateC2SIMLocationToCore - Translate Location adding to existing Core Document
    @param in
    @param out
    @throws C2SIMException 
     */
    private static void translateC2SIMLocationToCore(Element in, Document out) throws C2SIMException {


        try {
            Element orderCoreRoot = out.getRootElement();

            // Make list of OrderCore tasks
            List<Element> orderCoreTasks = C2SIM_Util.findElementSimple("Task", orderCoreRoot, C2SIM_Util.core_NS);

            // Get list of C2SIM tasks
            List<Element> c2simTasks = C2SIM_Util.findElementSimple("Task", in, C2SIM_Util.c2sim_NS);

            // Keep track of which task we are working on.  The number of OrderCore tasks and C2SIM tasks MUST be the same!!!        
            int taskNum = 0;

            // Loop through C2SIM Tasks
            for (Element c2simTask : c2simTasks) {

                Element orderCoreTask = orderCoreTasks.get(taskNum);
                Element mwt = C2SIM_Util.findSingleElementSimple("ManeuverWarfareTask", c2simTask, C2SIM_Util.c2sim_NS);

                // Get list of Location Elements
                List<Element> c2simLocs = C2SIM_Util.findElementSimple("Location", mwt, C2SIM_Util.c2sim_NS);

                // Is it a point?
                if (c2simLocs.size() == 1) {
                    Element c2simLoc = c2simLocs.get(0);
                    Element orderCorePoint = C2SIM_Util.createElementStack("Where/AtWhere/Point", orderCoreTask, C2SIM_Util.core_NS);

                    // Get the elements from C2SIM
                    Element lat = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/Latitude", c2simLoc, C2SIM_Util.c2sim_NS);
                    Element lon = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/Longitude", c2simLoc, C2SIM_Util.c2sim_NS);
                    Element alt = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/AltitudeAGL", c2simLoc, C2SIM_Util.c2sim_NS);

                    // Add them to OrderCore Point
                    if (lat != null)
                        orderCorePoint.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                    if (lon != null)
                        orderCorePoint.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                    if (alt != null)
                        orderCorePoint.addContent(new Element("ElevationAGL", C2SIM_Util.core_NS).setText(alt.getText()));
                }   // Point

                // Is it a line?
                if (c2simLocs.size() > 1) {

                    // Create Line and add the points
                    Element orderCoreLine = C2SIM_Util.createElementStack("Where/AtWhere/Line", orderCoreTask, C2SIM_Util.core_NS);

                    // Loop through list of points in C2SIM and Create points in AtWhere
                    for (Element c2sLoc : c2simLocs) {
                        // Get the elements from C2SIM
                        Element lat = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/Latitude", c2sLoc, C2SIM_Util.c2sim_NS);
                        Element lon = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/Longitude", c2sLoc, C2SIM_Util.c2sim_NS);
                        Element alt = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/AltitudeAGL", c2sLoc, C2SIM_Util.c2sim_NS);

                        // Add them to OrderCore
                        Element coords = new Element("Coords", C2SIM_Util.core_NS);
                        orderCoreLine.addContent(coords);

                        if (lat != null)
                            coords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                        if (lon != null)
                            coords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                        if (alt != null)
                            coords.addContent(new Element("ElevationAGL", C2SIM_Util.core_NS).setText(alt.getText()));

                    } // list of points

                }   // Order Core Line

                // Do we have a Route? - NOTE Routes were eliminated in C2SIM V11.  Keep this code around for awhile.
//                List<Element> rl = C2SIM_Util.findElementSimple("Route/RouteLocation", mwt, C2SIM_Util.c2sim_NS);
//                if (rl.size() != 0) {
//
//                    // Create route add elements for OrderCore
//                    Element via = C2SIM_Util.createElementStack("Where/RouteWhere/Via", orderCoreTask, C2SIM_Util.core_NS);
//
//                    // Work through the list of Route elements in C2SIM.  Add them to OrderCore Route Via
//                    for (Element rt : rl) {
//                        Element lat = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/Latitude", rt, C2SIM_Util.c2sim_NS);
//                        Element lon = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/Longitude", rt, C2SIM_Util.c2sim_NS);
//                        Element alt = C2SIM_Util.findSingleElementSimple("GeodeticCoordinate/AltitudeAGL", rt, C2SIM_Util.c2sim_NS);
//
//                        // Create coords and add to via
//                        Element coords = new Element("Coords", C2SIM_Util.core_NS);
//                        via.addContent(coords);
//
//                        if (lat != null)
//                            coords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
//                        if (lon != null)
//                            coords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lat.getText()));
//                        if (alt != null)
//                            coords.addContent(new Element("ElevationAGL", C2SIM_Util.core_NS).setText(alt.getText()));
//
//                    }// list of route elements
//
//                }   // RouteLocation elements
                ++taskNum;
            }   // tasks 

        }   // try
        catch (Exception e) {
            throw new C2SIMException("Error in translateC2SIMLocationToCore " + e);
        }   // catch   // catch

    }   // translateC2SIMLocationToCore())


    /**********************************/
    /* translateCoreLocationToC2SIM   */
    /**********************************/
    // Retrieve the location element from OrderCore document and add to C2SIM document
    /**
     * translateCoreLocationToC2SIM - Process existing Core Order and add location information to existing C2SIM Document
    @param in - Document - Core 
    @param out - Document C2SIM
    @throws C2SIMException 
     */
    private static void translateCoreLocationToC2SIM(Document in, Document out) throws C2SIMException {


        try {
            // Get root elements
            Element orderCoreRoot = in.getRootElement();
            Element c2simRoot = out.getRootElement();

            // Keep track of which taask we aare working on
            int taskNum = 0;

            // Make list of OrderCore tasks and C2SIM Tasks
            List<Element> orderCoreTasks = C2SIM_Util.findElementSimple("Task", orderCoreRoot, C2SIM_Util.core_NS);
            List<Element> c2simTasks = C2SIM_Util.findElementSimple("Task", c2simRoot, C2SIM_Util.c2sim_NS);

            // Loop through the OrderCore tasks
            for (Element orderCoreTask : orderCoreTasks) {
                // Locate the position in the proper C2SIM Task where the Line or Route will go
                Element c2simTask = c2simTasks.get(taskNum);
                int insertionPoint;

                // Position to ManeuverWarfare in the C2SIM document
                Element mw = c2simTask.getChild("ManeuverWarfareTask", C2SIM_Util.c2sim_NS);

                // Get the index of PerformingEntity.  Location information goes after.
                //   Actually ActionTemporalRelationship (0.inf) is after PorformingEntity bue we won't
                //     have this element when translating to C2SIM from some other protocol

                insertionPoint = findChildIndex("PerformingEntity", mw, C2SIM_Util.c2sim_NS);

                // Does this OrderCore document have an AtWhere
                List<Element> orderCoreCoords = C2SIM_Util.findElementSimple("Where/AtWhere/Line/Coords", orderCoreTask, C2SIM_Util.core_NS);
                if (orderCoreCoords.size() != 0) {

                    for (Element orderCoreCoord : orderCoreCoords) {

                        // Create C2SIM Location/Coordinate/Geodetic elements
                        Element c2simLoc = new Element("Location", C2SIM_Util.c2sim_NS);
                        Element c2simGeodetic = new Element("GeodeticCoordinate", C2SIM_Util.c2sim_NS);
                        c2simLoc.addContent(c2simGeodetic);

                        // Get lat/lon information
                        Element lat = orderCoreCoord.getChild("Latitude", C2SIM_Util.core_NS);
                        Element lon = orderCoreCoord.getChild("Longitude", C2SIM_Util.core_NS);
                        Element el = orderCoreCoord.getChild("Elevation", C2SIM_Util.core_NS);

                        // Move to C2SIM

                        if (lat != null)
                            c2simGeodetic.addContent(new Element("Latitude", C2SIM_Util.c2sim_NS).setText(lat.getText()));
                        if (lon != null)
                            c2simGeodetic.addContent(new Element("Longitude", C2SIM_Util.c2sim_NS).setText(lon.getText()));
                        if (el != null)
                            c2simGeodetic.addContent(new Element("AltitudeMSL", C2SIM_Util.c2sim_NS).setText(el.getText()));

                        mw.addContent(insertionPoint + 1, c2simLoc);
                        ++insertionPoint;
                    }   // Coordinates in line

                }   // Line

                // Does this OrderCore document have a RouteWhere?
                orderCoreCoords = C2SIM_Util.findElementSimple("Where/RouteWhere/From", orderCoreTask, C2SIM_Util.core_NS);
                if (orderCoreCoords.size() != 0) {

                    // C2SIM V11 doesn't have a Route.  Map the RouteWhere into a line
                    //  Group all elements from OrderCore Route into a single array of elements

                    // From Element
                    List<Element> tempPoints = null;
                    tempPoints.add(orderCoreCoords.get(0));

                    // Via Elements
                    orderCoreCoords = C2SIM_Util.findElementSimple("Where/RouteWhere/Via", orderCoreTask, C2SIM_Util.core_NS);
                    for (Element viaE : orderCoreCoords) {
                        tempPoints.add(viaE);
                    }   // for

                    // To Element
                    orderCoreCoords = C2SIM_Util.findElementSimple("Where/RouteWhere/To", orderCoreTask, C2SIM_Util.core_NS);
                    tempPoints.add(orderCoreCoords.get(0));

                    // Now build the Location/GeodeticCoordinats elements from this list of points from OrderCore Route

                    for (Element tempPoint : tempPoints) {

                        // Create C2SIM Location/Coordinate/Geodetic elements
                        Element c2simLoc = new Element("Location", C2SIM_Util.c2sim_NS);
                        Element c2simGeodetic = C2SIM_Util.createElementStack("GeodeticCoordinate", c2simLoc, C2SIM_Util.c2sim_NS);

                        // Get lat/lon information
                        Element lat = tempPoint.getChild("Latitude", C2SIM_Util.core_NS);
                        Element lon = tempPoint.getChild("Longitude", C2SIM_Util.core_NS);
                        Element el = tempPoint.getChild("Elevation", C2SIM_Util.core_NS);

                        // Move to C2SIM

                        if (lat != null)
                            c2simGeodetic.addContent(new Element("Latitude", C2SIM_Util.c2sim_NS).setText(lat.getText()));
                        if (lon != null)
                            c2simGeodetic.addContent(new Element("Longitude", C2SIM_Util.c2sim_NS).setText(lon.getText()));
                        if (el != null)
                            c2simGeodetic.addContent(new Element("AltitudeMSL", C2SIM_Util.c2sim_NS).setText(el.getText()));

                        mw.addContent(insertionPoint + 1, c2simLoc);
                        ++insertionPoint;
                    }   // Coordinates in line                    

                    // Create Route Element and add it to C2SIM Document at the correct location
                    Element c2simRoute = new Element("Route", C2SIM_Util.c2sim_NS);
                    mw.addContent(insertionPoint + 1, c2simRoute);

                    for (Element orderCoreCoord : orderCoreCoords) {

                        // Create RouteLocaation in C2SIM
                        Element c2simRouteLoc = new Element("RouteLocation", C2SIM_Util.c2sim_NS);
                        c2simRoute.addContent(c2simRouteLoc);

                        // Create GeodeticCoordinate
                        Element c2simGeodetic = new Element("GeodeticCoordinate", C2SIM_Util.c2sim_NS);
                        c2simRouteLoc.addContent(c2simGeodetic);

                        // Get lat/lon information
                        Element lat = orderCoreCoord.getChild("Latitude", C2SIM_Util.core_NS);
                        Element lon = orderCoreCoord.getChild("Longitude", C2SIM_Util.core_NS);
                        Element el = orderCoreCoord.getChild("Elevation", C2SIM_Util.core_NS);


                        c2simGeodetic.addContent(new Element("Latitude", C2SIM_Util.c2sim_NS).setText(lat.getText()));
                        c2simGeodetic.addContent(new Element("Longitude", C2SIM_Util.c2sim_NS).setText(lon.getText()));
                        if (el != null)
                            c2simGeodetic.addContent(new Element("AltitudeAGL", C2SIM_Util.c2sim_NS).setText(el.getText()));

                    }   // Coordinates in Route
                }   // Route

                ++taskNum;
            }   // Tasks

        }   // try
        catch (Exception e) {
            throw new C2SIMException("Exception in translateCoreLocationTo C2SIM " + e);
        }   // catch   // catch

    }   // translateLocationtoC2SIM    


    /********************/
    /* findChildIndex   */
    /********************/
    // 
    /**
     * findChildIndex - Locate the child of an Element having a particular name and return its index (position) 
    @param child - String Name of child being searched for 
    @param e - Clement Child name being searched for
    @param ns - Namespace of child
    @return Integer - Index 0f child of 
     */
    public static Integer findChildIndex(String child, Element e, Namespace ns) {
        List<Element> le = e.getChildren();
        int i = 0;
        for (Element ch : le) {
            if (ch.getName().equals(child))
                return i;
            ++i;
        }   // for loop of children

        // Not found
        return -1;

    }   // findChildIndex())


    /*************************/
    /* process_C2SIM_Report  */
    /********************(****/
    /**
     * process_C2SIM_Report - Process C2SIM Report including managing translation to other protocols
            C2SIM Reports are structured as:
            <MessageBody><DomainMessageBody><ReportBody>(<ReportContent><PositionReportContent> . . .)   () - Repeats
            <ReportContent> may be <PositionReportContent> or <ObservationReportContent>
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
     */
    public static void process_C2SIM_Report(C2SIM_Transaction trans) throws C2SIMException {

        // C2SIM Report Document
        Document c2simDoc = trans.getDocument();

        // Unit_Core Document
        Document unitCoreDoc = null;

        // Get root (MessageBody)
        Element msgBody = c2simDoc.getRootElement();


        // Get the reporting entity
        //    Get reportingEntity and reportID at the batch level
        Element reportingEntity = C2SIM_Util.findSingleElementSimple("DomainMessageBody/ReportBody/ReportingEntity", msgBody, C2SIM_Util.c2sim_NS);
        Element reportID = C2SIM_Util.findSingleElementSimple("DomainMessageBody/ReportBody/ReportID", msgBody, C2SIM_Util.c2sim_NS);

        // Get list of individual Reports
        List<Element> repts = C2SIM_Util.findElementSimple("DomainMessageBody/ReportBody/ReportContent", msgBody, C2SIM_Util.c2sim_NS);

        // Work through the list and create a separate core report for each C2SIM PositionReportContent.  Skip other reports
        for (Element r : repts) {
            Element prx = r.getChild("PositionReportContent", C2SIM_Util.c2sim_NS);
            if (prx == null)
                prx = r.getChild("ObservationReportContent", C2SIM_Util.c2sim_NS);
            if (prx != null) {
                Element pr = prx.clone();

                // Is this an ASX report?  If so don't translate it.
                if (pr.getChild("AutonomousSystemPositionReportContent", C2SIM_Util.c2sim_NS) != null)
                    continue;
                // Create Document using this Report as the root
                Document d2 = new Document(pr);

                // translate the report from the incoming C2SIM Position Report into Unit_Core

                // IF thie is Observation Report build the Core Document with code
                if (pr.getName().equals("ObservationReportContent")) {
                    Element locCore = null;
                    unitCoreDoc = new Document();
                    Element unit = new Element("Unit", C2SIM_Util.core_NS);
                    unitCoreDoc.addContent(unit);

                    // Actor / Unit being reported on
                    Element act = C2SIM_Util.findSingleElementSimple("Observation/LocationObservation/ActorReference", pr, C2SIM_Util.c2sim_NS);
                    if (act != null)
                        unit.addContent(new Element("ID", C2SIM_Util.core_NS).setText(act.getText()));

                    // Hostility
                    unit.addContent(new Element("HostilityCode", C2SIM_Util.core_NS).setText("HO"));

                    // Location
                    Element lat = C2SIM_Util.findSingleElementSimple("Observation/LocationObservation/Location/GeodeticCoordinate/Latitude", pr, C2SIM_Util.c2sim_NS);
                    Element lon = C2SIM_Util.findSingleElementSimple("Observation/LocationObservation/Location/GeodeticCoordinate/Longitude", pr, C2SIM_Util.c2sim_NS);
                    if (lat != null) {
                        locCore = new Element("Location", C2SIM_Util.core_NS);
                        unit.addContent(locCore);
                        locCore.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                    }   // Latitude
                    if (lon != null)
                        locCore.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));

                    // When
                    Element tm = C2SIM_Util.findSingleElementSimple("TimeOfObservation/DateTime/IsoDateTime", pr, C2SIM_Util.c2sim_NS);
                    if (tm != null)
                        unit.addContent(new Element("ReportedWhenStart", C2SIM_Util.core_NS).setText(tm.getText()));

                }   // Observation Report
                else
                    try {
                        unitCoreDoc = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("C2SIM_PositionReport_Single"), d2, "F");
                    }   // try                
                    catch (C2SIMException ex) {
                        C2SIM_Server.debugLogger.error("Exception while translating Unit to IBML09");
                        throw new C2SIMException("Exception while translating Unit to COre", ex);
                    }   // catch   // catch

                // Get root of unit core document
                Element unitCoreRoot = unitCoreDoc.getRootElement();

                // Add OperationalStatusCode and StrengthPercentage to core manually because of changes in C2SIM V11
                List<Element> ehs = C2SIM_Util.findElementSimple("EntityHealthStatus", pr, C2SIM_Util.c2sim_NS);
                for (Element ehsE : ehs) {
                    // Do we have an OperationalStatusCode?
                    if (ehsE.getChild("OperationalStatus", C2SIM_Util.c2sim_NS) != null) {
                        Element osc = C2SIM_Util.findSingleElementSimple("OperationalStatus/OperationalStatusCode", ehsE, C2SIM_Util.c2sim_NS);
                        if (osc != null)
                            unitCoreRoot.addContent(new Element("OperationalStatus", C2SIM_Util.core_NS).setText(osc.getText()));

                    }
                    if (ehsE.getChild("Strength", C2SIM_Util.c2sim_NS) != null) {
                        Element sp = C2SIM_Util.findSingleElementSimple("Strength/StrengthPercentage", ehsE, C2SIM_Util.c2sim_NS);
                        unitCoreRoot.addContent(new Element("StrengthPercentage", C2SIM_Util.core_NS).setText(sp.getText()));
                    }   // if
                }   // for


                // Add reporting entity and reportID to core
                if (reportingEntity != null)
                    unitCoreRoot.addContent(new Element("ReporterWho", C2SIM_Util.core_NS).setText(reportingEntity.getText()));

                if (reportID != null)
                    unitCoreRoot.addContent(new Element("ReportID", C2SIM_Util.core_NS).setText(reportID.getText()));

                // Update the initizliaztion data with this new position
                updatePosition(unitCoreDoc);

                // Add unit core document to transaction
                trans.setCoreDocument(unitCoreDoc);

                // Translate core to IBML09 and publish
                C2SIM_IBML09.translateCoreToIBML09_GSR(trans);

                // Translate core to CBML and publish
                C2SIM_CBML.translateCoreToCBML_GSR(trans);

            }   // C2SIM PositionReport
        }   // Report List

    }   // process_C2SIM_Report


    /*****************************/
    /* transleCoreToC2SIM_PR     */
    /*****************************/
    /**
     * translateCoreToC2SIM_PR - Translate Core report to C2SIM Position Report
      Mapping produces document starting with PositionReport Content.  
      Must create MessageBody/DomainMessageBody/ReportBody/ReportContent
         and connect PositionReport to ReportContent
      Also must connect ReportingEntithy (From ReporterWho in ReportCore) to ReportBody
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
     */
    static void translateCoreToC2SIM_PR(C2SIM_Transaction transIn) throws C2SIMException {

        /*

         */
        Document posRept;
        Element posReptRoot;

        C2SIM_Transaction trans = transIn.clone();

        // First map ReportCored to PositionReport
        posRept = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("C2SIM_PositionReport_Single"), trans.getCoreDocument(), "R");
        posReptRoot = posRept.getRootElement();

        Element coreRoot = trans.getCoreDocument().getRootElement();

        // Handle OperationalStatus and Strength Percentage manually because of changes in C2SIM V11
        Element coreOS = coreRoot.getChild("OperationalStatus", C2SIM_Util.core_NS);
        if (coreOS != null) {

            Element ehs = new Element("EntityHealthStatus", C2SIM_Util.c2sim_NS);

            // Position one after the <TimeOfObservation>
            posReptRoot.addContent(1, ehs);
            Element os = new Element("OperationalStatus", C2SIM_Util.c2sim_NS);
            ehs.addContent(os);
            os.addContent(new Element("OperationalStatusCode", C2SIM_Util.c2sim_NS).setText(coreOS.getText()));
        }

        Element coreStr = coreRoot.getChild("Strength", C2SIM_Util.core_NS);
        if (coreStr != null) {

            Element ehs = new Element("EntityHealthStatus", C2SIM_Util.c2sim_NS);
            posReptRoot.addContent(1, ehs);
            Element os = new Element("Strength", C2SIM_Util.c2sim_NS);
            ehs.addContent(os);
            os.addContent(new Element("StrengthPercentage", C2SIM_Util.c2sim_NS).setText(coreOS.getText()));
        }

        // Now create document with MessageBody/Domain/MessageBody/ReportBody/ReportContent

        // Document (MessageBody)
        Document mb = new Document();
        Element root = new Element("MessageBody", C2SIM_Util.c2sim_NS);

        // DomainMessageBody
        Element dmb = new Element("DomainMessageBody", C2SIM_Util.c2sim_NS);
        root.addContent(dmb);

        // ReportBody
        Element rb = new Element("ReportBody", C2SIM_Util.c2sim_NS);
        dmb.addContent(rb);

        // Add DomainMessageBodyGroup elements
        rb.addContent(new Element("FromSender", C2SIM_Util.c2sim_NS).setText(fromSender));
        rb.addContent(new Element("ToReceiver", C2SIM_Util.c2sim_NS).setText(toReceiver));

        // ReportContent
        Element rc = new Element("ReportContent", C2SIM_Util.c2sim_NS);
        rb.addContent(rc);

        // Get ReporterWho from ReportCore and attach to ReportBody as ReportingEntity
        Element uRoot = trans.getCoreDocument().getRootElement();

        String rWho = uRoot.getChild("ReporterWho", C2SIM_Util.core_NS).getText();
        String reportID = uRoot.getChild("ReportID", C2SIM_Util.core_NS).getText();

        rb.addContent(new Element("ReportingEntity", C2SIM_Util.c2sim_NS).addContent(rWho));
        rb.addContent(new Element("ReportID", C2SIM_Util.c2sim_NS).addContent(reportID));

        // Now add mapped PositionReportContent to ReportContent
        rc.addContent(posRept.detachRootElement());

        // Add root to document
        mb.addContent(root);

        trans.setProtocol(SISOSTD);
        trans.setSender("Server");
        trans.setReceiver("ALL");
        String xml = C2SIM_Util.xmlToStringD(mb, trans);

        // Set parameters in the Transaction object for this message
        trans.setMessageDef(C2SIM_Util.mdIndex.get("C2SIM_Report"));
        trans.setMsTemp("C2SIM_PositionReport");
        trans.setXmlText(xml);
        trans.setSource("Translated");
        String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
        trans.setc2SIM_Version(c2simVer);
        trans.setXmlMsg(C2SIMHeader.removeC2SIM(xml));

        // Publish
        C2SIM_Server_STOMP.publishMessage(trans);

        // Translate to V9 and publish
        trans.setc2SIM_Version("0.0.9");
        C2SIM_Transaction t9 = C2SIM_C2SIM.c2SIM_Translate_100To9(trans);
        C2SIM_Server_STOMP.publishMessage(t9);

    }   // translateCoreToC2SIM_PR()


    /********************************/
    /* translateCoreToC2SIM_Order    */
    /********************************/
    /**
     * translateCoreToC2SIM_Order - Translate Core Order to C2SIM Order
    @param t - C2SIM_Transaction
    @throws C2SIMException 
     */
    static void translateCoreToC2SIM_Order(C2SIM_Transaction t2) throws C2SIMException {

        Element root;
        String lat = "";
        String lon = "";
        String el = "";
        Element eLine = null;
        Element eWhereClass;
        Element eWhereCategory;
        Element eAtWhere;
        Element ePoint;

        // Make a copy of the transaction
        C2SIM_Transaction t = t2.clone();

        // Get the root element of the order
        root = t.getCoreDocument().getRootElement();

        // Translate from OrderCore to C2SIM Order
        Document c2simOrder = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("C2SIM_Order"), t.getCoreDocument(), "R");

        // Now translate the location information
        translateCoreLocationToC2SIM(t.getCoreDocument(), c2simOrder);

        // Translated order is based at OrderBody.  Pull out root and encapsulate in <MessageBody><DomainMessageBody>
        Element r = c2simOrder.getRootElement().clone();

        // Add default values for DomainMessageBodyGroup
        r.addContent(0, new Element("ToReceiver", C2SIM_Util.c2sim_NS).setText(toReceiver));
        r.addContent(0, new Element("FromSender", C2SIM_Util.c2sim_NS).setText(fromSender));


//        int numChildren = r.getChildren().size();
//        r.addContent(numChildren, new Element("UUID", C2SIM_Util.c2sim_NS).setText("00000000-0000-0000-0000-000000000000"));
        Element mb = new Element("MessageBody", C2SIM_Util.c2sim_NS);
        Element dmb = new Element("DomainMessageBody", C2SIM_Util.c2sim_NS);
        mb.addContent(dmb);
        dmb.addContent(r);
        c2simOrder = new Document(mb);


        // Indicate a C2SIM Order
        t.setProtocol(SISOSTD);
        t.setSender("Server");
        t.setReceiver("ALL");
        String xml = C2SIM_Util.xmlToStringD(c2simOrder, t);
        t.setXmlMsg(C2SIMHeader.removeC2SIM(xml));

        // Set parameters in the Transaction object for this message
        t.setMessageDef(C2SIM_Util.mdIndex.get("C2SIM_Order"));
        t.setXmlText(xml);
        t.setSource("Translated");
        String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
        t.setc2SIM_Version(c2simVer);

        // Publish the C2SIM OrderNN
        C2SIM_Server_STOMP.publishMessage(t);

        // Translate to V9 and publish
        t.setc2SIM_Version("0.0.9");
        C2SIM_Transaction t9 = C2SIM_C2SIM.c2SIM_Translate_100To9(t);
        C2SIM_Server_STOMP.publishMessage(t9);

    }   //translateCoreToC2SIM_Order() 


    /****************/
    /* shareC2SIM   */
    /****************/
    // 
    /**
     * shareC2SIM - Assemble all unitialization in initDB into a single message and publish it
    @param t
    @throws C2SIMException 
     */
    public static void shareC2SIM(C2SIM_Transaction t) throws C2SIMException {

        try {
            // Create the xml string for all initialization data
            String xml = formatInit(t);

            // Set parameters in the C2SIM_Transaction object
            t.setMessageDef(C2SIM_Util.mdIndex.get("C2SIM_Initialization"));
            t.setXmlText(xml);
            t.setXmlMsg(C2SIMHeader.removeC2SIM(xml));
            t.setMsgnumber(C2SIM_Server.msgNumber);
            t.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
            t.setSource("Generated");
            // c2simVersion was saved from the first Initialization transaction
            String c2simVerProp = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
            if(c2simVerProp != null){
                if(!c2simVerProp.trim().equals(""))
                    t.setc2SIM_Version(c2simVerProp);
                else t.setc2SIM_Version(c2simVersion);
            }
            else t.setc2SIM_Version(c2simVersion);
            c2simVersion = "";

            // Publish the message
            C2SIM_Server_STOMP.publishMessage(t);

            /*
                Translate stored Initialization data to V9 and publish
            
             */
            String c2sim009Prop = C2SIM_Server.props.getProperty("server.Translate9To1");
            if(c2sim009Prop.equalsIgnoreCase("T")){
                C2SIM_Transaction t9 = t.clone();
                Translate100Init t100 = new Translate100Init();
                String xmlIn = t9.getXmlMsg();
                String xml9 = t100.translate(xmlIn);

                // Set parameters in the C2SIM_Transaction object
                t9.setMessageDef(C2SIM_Util.mdIndex.get("C2SIM_Initialization"));
                t9.setXmlMsg(xml9);
                t9.setXmlText(C2SIMHeader.insertC2SIM(xml9, t9.getSender(), t9.getReceiver(), "Inform", "0.0.9"));
                t9.setMsgnumber(C2SIM_Server.msgNumber);
                t9.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
                t9.setc2SIM_Version("0.0.9");
                t9.setSource("Generated");

                // Publish the message
                C2SIM_Server_STOMP.publishMessage(t9);
            }

            /*
                Translate C2SIM Initialization to MSDL and publish
             */

            // Translate C2SIM to MSDL
            String c2simMSDLProp = C2SIM_Server.props.getProperty("server.TranslateMSDL");
                if(c2simMSDLProp.equalsIgnoreCase("T")){
                xml = C2SIM_MSDL.convertC2SIMToMSDL(t);

                // Set parameters in the C2SIM_Transaction object
                t.setMessageDef(C2SIM_Util.mdIndex.get("MSDL"));
                t.setc2SIM_Version("");
                t.setXmlText(xml);
                t.setMsgnumber(C2SIM_Server.msgNumber);
                t.setMsgTime(LocalDateTime.now().format(C2SIM_Server.dtf));
                t.setSource("Generated");


                // Publish the message
                C2SIM_Server_STOMP.publishMessage(t);
            }

            // Set sessionState to initialized
            C2SIM_Server.sessionInitialized = true;
            C2SIM_Server.sessionState = C2SIM_Server.SessionState_Enum.INITIALIZED;
            C2SIM_Command.publishStateUpdate("InitializationComplete", C2SIM_Server.SessionState_Enum.INITIALIZED.toString(), t);

            C2SIM_Server.debugLogger.debug("SHARE published " + C2SIM_Util.initDB.entity.size() + " Units ");

        }   // try
        catch (Exception e) {
            StackTraceElement[] st = e.getStackTrace();
            throw new C2SIMException("Exception thrown in shareC2SIM Exception thrown while formatting Initialization Data", e);
        }   // catch   // catch


    }   // shareC2SIM()


    /****************/
    /* formatInit   */
    /****************/
    /**
     * formatInit - Format data in initDB into a C2SIMInitializartionBody message
    @param t - C2SIM_Transaction
    @return - String/XML
    @throws C2SIMException 
     */
    static String formatInit(C2SIM_Transaction t) throws C2SIMException {
        try {
            Namespace ns = C2SIM_Util.c2sim_NS;
            Element objectDefinitions = null;

            // Create document and add MessageBody/C2SIMInitializationBody
            Document d = new Document();
            Element root = new Element("MessageBody", ns);
            Element initializationBody = new Element("C2SIMInitializationBody", ns);
            root.addContent(initializationBody);

            /*
        There are five possible children of C2SIMInitializationBody.
            InitializationDataFile
            ObjectDefinitions/AbstractObject
            ObjectDefinitions/Action
            ObjectDefinitions/Entity
            ObjectDifinitions/PlanPhaseReference
            SystemEntityList
            ScenarioSetting
        Check each to see if we have entries and add them to the document 
             */

            // InitializationDataFile
            List<InitElement> initDataFiles = C2SIM_Util.initDB.initDataFile;
            for (InitElement initDF : initDataFiles) {
                initializationBody.addContent(initDF.element.clone());
            }   // InitializationDataFile      

            // Create ObjectDefinitions
            objectDefinitions = new Element("ObjectDefinitions", ns);

            // Add to C2SIMInitializationBody
            initializationBody.addContent(objectDefinitions);

            // AbstractObject
            if (!C2SIM_Util.initDB.abstractObject.isEmpty()) {

                // Add AbstractObjects to ObjectDefinitions
                for (C2SIM_InitDB.InitElement e : C2SIM_Util.initDB.abstractObject) {
                    objectDefinitions.addContent(e.element.clone());
                }   // AbstractObject
            }   // AbstractDataObject


            // Action
            if (!C2SIM_Util.initDB.action.isEmpty()) {

                // Add Actions to ObjectDefinitions
                for (C2SIM_InitDB.InitElement e : C2SIM_Util.initDB.action) {
                    objectDefinitions.addContent(e.element.clone());
                }   // add Actions
            }   // Action


            // Entity
            if (!C2SIM_Util.initDB.entity.isEmpty()) {

                // Add Entity(s) to ObjectDefinitions
                for (C2SIM_InitDB.InitElement e : C2SIM_Util.initDB.entity) {
                    objectDefinitions.addContent(e.element.clone());
                }   // add Entitys
            }   // Entity

            // PlanPhaseReference
            if (!C2SIM_Util.initDB.plan.isEmpty()) {

                // Add PlanPhaseBody(s) to ObjectDefinitions
                for (C2SIM_InitDB.InitElement e : C2SIM_Util.initDB.plan) {
                    objectDefinitions.addContent(e.element.clone());
                }   // add PlanPhaseReference            
            }   // Plan Phase Reference

            // ScenarioSetting
            if (!C2SIM_Util.initDB.scenario.isEmpty()) {
                for (C2SIM_InitDB.InitElement e : C2SIM_Util.initDB.scenario) {

                    // Add to C2SIMInitializationBody
                    initializationBody.addContent(e.element.clone());
                }   // add ScenarioSetting(s)
            }   // ScenarioSetting

            // SystemEntityList
            if (!C2SIM_Util.initDB.systemEntityList.isEmpty()) {
                for (C2SIM_InitDB.InitElement e : C2SIM_Util.initDB.systemEntityList) {

                    // Add to C2SIMInitializationBody
                    initializationBody.addContent(e.element.clone());
                }   // SystemEntityList
            }


            // Add the root document
            d.addContent(root);

            // Generate xml string
            String xml = C2SIM_Util.xmlToStringD(d, t);

            return xml;

        }   // try
        catch (Exception e) {
            throw new C2SIMException("Exception thrown while formatting Initialization Data  cause = ", e);
        }   // catch   // catch
    }   // formatInit()      


    /********************/
    /* updatePosition   */
    /********************/
    /**
     * updatePosition - Update the position a unit in the Initialization data from data in a Report Core document
    @param doc - Document containing the report
    @throws C2SIMException 
     */
    public static void updatePosition(Document doc) throws C2SIMException {

        // Are we updating the position from reports in the Initialization data?
        if (!C2SIM_Server.props.getProperty("server.CaptureUnitPosition").equalsIgnoreCase("T"))
            return;

        Element root = doc.getRootElement();

        // Get the location information from the report core document
        String id = root.getChildText("ID", C2SIM_Util.core_NS);

        if (id == null) {
            C2SIM_Server.debugLogger.error("Unit (Report) has no ID");
            return;
        }   // Unit ID is null

        Element coreLocation = root.getChild("Location", C2SIM_Util.core_NS);
        if (coreLocation == null) {
            C2SIM_Server.debugLogger.error("Unit (Report) has no location.  ID = " + id);
            return;
        }

        Element coreLatitude = coreLocation.getChild("Latitude", C2SIM_Util.core_NS);
        Element coreLongitude = coreLocation.getChild("Longitude", C2SIM_Util.core_NS);
        Element coreElevation = coreLocation.getChild("Elevation", C2SIM_Util.core_NS);

        // Locate the  Entity(MilitaryOrganization) and updata the position
        Element mo = C2SIM_Util.unitMap.get(id);
        if (mo == null) {
            C2SIM_Server.debugLogger.error("MilitaryOrganization object not found for ID in Position Report - ID = " + id);
            return;
        }

        // Does CurrentState exist for this MO?
        Element c2simCurrentState = mo.getChild("CurrentState", C2SIM_Util.c2sim_NS);

        // If cs is null then the current state wasn't included during initialization and must be creatted
        if (c2simCurrentState == null) {
            // Create "CurrentState/PhysicalState/Location/Coordinate/GedeticCoordinate
            c2simCurrentState = new Element("CurrentState", C2SIM_Util.c2sim_NS);
            mo.addContent(0, c2simCurrentState);

            Element c2simPhysicalState = new Element("PhysicalState", C2SIM_Util.c2sim_NS);
            c2simCurrentState.addContent(c2simPhysicalState);

            // Note that C2SIM V11 dropped Coordinate

            Element c2simLocation = new Element("Location", C2SIM_Util.c2sim_NS);
            c2simPhysicalState.addContent(c2simLocation);

            Element c2simGeodeticCoordinate = new Element("GeodeticCoordinate", C2SIM_Util.c2sim_NS);
            c2simLocation.addContent(c2simGeodeticCoordinate);

            // Add new elements for Latitude, Longitude and Altitude
            if (coreLatitude != null)
                c2simGeodeticCoordinate.addContent(new Element("Latitude", C2SIM_Util.c2sim_NS).setText(coreLatitude.getText()));
            if (coreLongitude != null)
                c2simGeodeticCoordinate.addContent(new Element("Longitude", C2SIM_Util.c2sim_NS).setText(coreLongitude.getText()));
            if (coreElevation != null)
                c2simGeodeticCoordinate.addContent(new Element("AltitudeAGL", C2SIM_Util.c2sim_NS).setText(coreElevation.getText()));

        }   // Create CurrentState and children

        // CurrentState does exist - Update the location
        else {

            List<Element> gcl = C2SIM_Util.findElementSimple("CurrentState/PhysicalState/Location/GeodeticCoordinate", mo, C2SIM_Util.c2sim_NS);
            if (gcl.isEmpty())
                throw new C2SIMException("Geodetic Coordinate not found for Unit ID " + id);

            // Get the Geodetic Coordinate element
            Element gc = gcl.get(0);

            // Update the location information
            if (coreLatitude != null)
                gc.getChild("Latitude", C2SIM_Util.c2sim_NS).setText(coreLatitude.getText());
            if (coreLongitude != null)
                gc.getChild("Longitude", C2SIM_Util.c2sim_NS).setText(coreLongitude.getText());
            if (coreElevation != null) {
                // Do we have AltitudeAGL in the stored Entity?  IF not then create it
                if (gc.getChild("AltitudeAGL", C2SIM_Util.c2sim_NS) == null)
                    gc.addContent(new Element("AltitudeAGL", C2SIM_Util.c2sim_NS));
                // Now, add the value of elevation as AltitudeAGL
                gc.getChild("AltitudeAGL", C2SIM_Util.c2sim_NS).setText(coreElevation.getText());
            }
        }
    }   // updatePosition()


    /****************************/
    /*  publishNotification     */
    /****************************/
    /**
     * publishNotification - Publish notification of server state change
    @param type
    @param t
    @throws C2SIMException 
     */
    public static void publishNotification(String type, C2SIM_Transaction t) throws C2SIMException {

        // Create document
        Document tempDoc;
        Element tempEl;

        // Create output document and add root element
        Document doc = new Document();
        Element root = new Element("MessageBody", C2SIM_Util.c2sim_NS);

        // C2SIMSimulationNotification
        Element simulationNotification = new Element("C2SIMSimulationNotification", C2SIM_Util.c2sim_NS);
        root.addContent(simulationNotification);

        // Notification_Type
        simulationNotification.addContent(new Element("Notification_Type", C2SIM_Util.c2sim_NS).setText(type));

        // Number of Object Initialization files or messages
        simulationNotification.addContent(new Element("NumberObjectInitialization", C2SIM_Util.c2sim_NS)
                .setText(C2SIM_Util.numInitMsgs.toString()));

        // Number of Units
        simulationNotification.addContent(new Element("NumberOfUnits", C2SIM_Util.c2sim_NS).setText(C2SIM_Util.numC2SIM_Units.toString()));

        doc.addContent(root);

        // Set parameters in the C2SIM_Transaction object
        t.msTemp = "C2SIM_Notification";
        t.setSource("Generated");

        String xml = C2SIM_Util.xmlToStringD(doc, t);
        t.setXmlText(xml);

        // Publish the message
        C2SIM_Server_STOMP.publishMessage(t);
    }


    /********************************/
    /* addDomainMessageBodyGroup    */
    /********************************/
    // These elements are required by C2SIM but we don't have 
    /**
     * addDomainMessageBodyGroup - Add DomainMessageBodyGroup to document that doesn't have one
    @param d  - Document to be modified
     */
    public static void addDomainMessageBodyGroup(Document d) {


        // The root should be either OrderBody or ReportBody.
        Element root = d.getRootElement();
        // Add ToReceiver at 0 and then FromSender at 0.  The sequence will be FromSender then ToReceiver
        if (root.getChild("ToReceiver", C2SIM_Util.c2sim_NS) == null)
            root.addContent(0, new Element("toReceiver", C2SIM_Util.c2sim_NS).setText(toReceiver));
        if (root.getChild("FromSender", C2SIM_Util.c2sim_NS) == null)
            root.addContent(0, new Element("FromSender", C2SIM_Util.c2sim_NS).setText(fromSender));

    }   // addDomainMessageBodyGroup()


    /****************************/
    /* c2sim_Translate_9To100   */
    /****************************/
    // Translate C2SIM Message from Version9 to Version 1.00
    /**
     * c2SIM_Translate_9To100 - Translate C2SIM Message from Version 0.0.9 to Version 1.0.0
    @param t9 - C2sIM_Transaction containing version 0.0.9 message
    @return C2SIM_Transaction containing 1.0.0 message
    @throws C2SIMException 
     */
    public static C2SIM_Transaction c2SIM_Translate_9To100(C2SIM_Transaction t9) throws C2SIMException {

        try {
            // make a copy of the original transaction
            C2SIM_Transaction t100 = t9.clone();
            String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
            t100.setc2SIM_Version(c2simVer);
            t100.setSource("Translated");

            if (t9.messageDef.messageDescriptor.equals("C2SIM_Order")) {
                // Translate to V100 and store in Transactiop
                Translate009Order t = new Translate009Order();
                String newXml = t.translate(t9.getXmlMsg());
                t100.setXmlMsg(newXml);

                // Create a C2SIMHeader to make the message publishable
                t100.setXmlText(C2SIMHeader.insertC2SIM(newXml, t100.getSender(), t100.getReceiver(), "Inform", "1.0.0"));

                // Create a Document for further translation processing
                t100.setDocument(C2SIM_Mapping.parseMessage(newXml));
            }

            else if (t9.messageDef.messageDescriptor.equals("C2SIM_Report")) {
                // Translate to V100 and store in Transactiop
                Translate009Report t = new Translate009Report();
                String newXml = t.translate(t9.getXmlMsg());
                t100.setXmlMsg(newXml);

                // Create a C2SIMHeader to make the message publishable
                t100.setXmlText(C2SIMHeader.insertC2SIM(newXml, t100.getSender(), t100.getReceiver(), "Inform", "1.0.0"));

                // Create a Document for further translation processing
                t100.setDocument(C2SIM_Mapping.parseMessage(newXml));
            }

            else if (t9.messageDef.messageDescriptor.equals("C2SIM_Initialization")) {
                // This won't be published but will be processed and stored
                Translate009Init t = new Translate009Init();
                String newXml = t.translate(t9.getXmlMsg());
                t100.setXmlText(newXml);
                t100.setXmlMsg(newXml);
                t100.setDocument(C2SIM_Mapping.parseMessage(newXml));
                c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
                t100.setc2SIM_Version(c2simVer);
            }

            else
                return null;

            return t100;
        }

        catch (Exception e) {
            throw new C2SIMException("Exeption thrown in C2SIM_Translate_9To100 " + e.getMessage());

        }

    }   // c2SIM_Translate_9To100()   


    /****************************/
    /* c2sim_Translate_100To9    */
    /****************************/
    // Translate C2SIM Message from Version9 to Version 11
    public static C2SIM_Transaction c2SIM_Translate_100To9(C2SIM_Transaction t100) {

        // make a copy of the original transaction
        C2SIM_Transaction t9 = t100.clone();
        t9.setc2SIM_Version("0.0.9");
        t9.setSource("Translated");

        if (t100.messageDef.messageDescriptor.equals("C2SIM_Order")) {
            Translate100Order t = new Translate100Order();
            String xml9 = t.translate(t9.getXmlMsg());
            t9.setXmlMsg(xml9);
            t9.setc2SIM_Version("0.0.9");
        }   // V100 Order

        else if (t100.messageDef.messageDescriptor.equals("C2SIM_Report")) {
            Translate100Report t = new Translate100Report();
            String xml9 = t.translate(t9.getXmlMsg());
            t9.setXmlMsg(xml9);
            t9.setc2SIM_Version("0.0.9");
        }   // V100 Order

        // Add C2SIM Message Header
        t9.setXmlText(C2SIMHeader.insertC2SIM(t9.getXmlMsg(), t9.getSender(), t9.getReceiver(), "Inform", t9.getc2SIM_Version()));
        return t9;

    }   // c2SIM_Translate_100To9


}   // C2SIM_C2SIM Class

