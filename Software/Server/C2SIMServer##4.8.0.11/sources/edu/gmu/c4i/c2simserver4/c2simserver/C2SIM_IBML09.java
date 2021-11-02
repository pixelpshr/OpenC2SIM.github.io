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

import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_C2SIM.findChildIndex;
import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Transaction;
import java.time.LocalDateTime;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;


/**
 * <h1>C2SIM_IBML09</h1> <p>
 * Performs processing of IBML09 messages

 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_IBML09 {

    static Boolean translateToIBML09 = C2SIM_Util.toBoolean(C2SIM_Server.props.getProperty("server.TranslateToIBML09"));

    /************/
    /* process  */
    /************/
    /**
    * process - Main process method for IBML09.  Determine type and which method should perform processing
    @param trans
    @throws C2SIMException 
    */
    static void process(C2SIM_Transaction trans) throws C2SIMException {
        if (trans.messageDef.messageDescriptor.equalsIgnoreCase("IBML09_Report"))
            processIBML09_GSR(trans);
        else if (trans.messageDef.messageDescriptor.equalsIgnoreCase("IBML09_Order"))
            processIBML09_Order(trans);
        else
            throw new C2SIMException("Unknown IBML09 message descriptor");
        return;
    }


    /********************/
    /* processIBMLOrder */
    /********************/
    /**
    * Process IBML09 Order. Manage translation to other protocols 
    @param t - C2SIM_Transaction
    @throws C2SIMException 
    */
    public static void processIBML09_Order(C2SIM_Transaction t) throws C2SIMException {

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


        // Publish the original order
        C2SIM_Server_STOMP.publishMessage(t);

        // Get so we can make IBML09 specific modifications
        d = t.getDocument();

        // Map IBML09 Order to Core
        orderCore = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("IBML_Order"), d, "F");

        /*
        Mapping moves name into Tasker, Taskee - Convert them to UUIDs
         */

        // Get orderCore Root
        Element ocRoot = orderCore.getRootElement();

        // Get the taskerWho text and convert to UUID
        Element taskerE = ocRoot.getChild("TaskerWho", C2SIM_Util.core_NS);
        if (taskerE != null) {
            String tasker = taskerE.getText();
            taskerE.setText(C2SIM_Util.nameToUUID.get(tasker));
            if (taskerE.getText().equals("")) {
                taskerE.setText("00000000-0000-0000-0000-000000000000");
//                C2SIM_Server.debugLogger.error("Message number" + t.getMsgnumber() + "  Translation aborted - Required element missing");
//                return;
            }
        }

        // Get list of tasks and convert taskeeWho and affectedWho in each  If not in nameToUUID map return without publishing.
        List<Element> tasks = ocRoot.getChildren("Task", C2SIM_Util.core_NS);

        for (Element task : tasks) {
            Element taskeeE = task.getChild("TaskeeWho", C2SIM_Util.core_NS);
            if (taskeeE != null) {
                String taskee = taskeeE.getText();
                taskeeE.setText(C2SIM_Util.nameToUUID.get(taskee));
                if (taskeeE.getText().equals("")) {
                    taskerE.setText("00000000-0000-0000-0000-000000000000");
//                    C2SIM_Server.debugLogger.error("Message number" + t.getMsgnumber() + "  Translation aborted - Required element missing");
//                    return;
                }
            }

            Element affectedE = task.getChild("AffectedWho", C2SIM_Util.core_NS);
            if (affectedE != null) {
                String affected = affectedE.getText();
                affectedE.setText(C2SIM_Util.nameToUUID.get(affected));
                if (affectedE.getText().equals("")) {
                    C2SIM_Server.debugLogger.error("Message number" + t.getMsgnumber() + "  Translation aborted - Required element missing");
                    return;
                }
            }
        }

        // Convert dates to C2SIM format
        convertOrderDates(orderCore, "IBML09", "C2SIM");

        // Translate location information
        translateIBML09LocationToCore(d.getRootElement(), orderCore);

        // Add orderCore to transaction
        t.setCoreDocument(orderCore);

        // Translate core to C2SIM Order and publish
        C2SIM_C2SIM.translateCoreToC2SIM_Order(t);

        // Translate core to CBML Order and publish
        C2SIM_CBML.translateCoreToCBML_Order(t);

    }   // processIBML09_Order()    


    /************************************/
    /* translateIBML09LocationToCore     */
    /************************************/
    // 
    /**
    * translateIBML09LocationToCore - Map location information from input IBML Order output (orderCore)
    @param in - Element IBMLLocation 
    @param out - Document - Core
    @throws C2SIMException 
    */
    private static void translateIBML09LocationToCore(Element in, Document out) throws C2SIMException {

        try {
            Element orderCoreRoot = out.getRootElement();

            // Make list of OrderCore tasks
            List<Element> orderCoreTasks = C2SIM_Util.findElementSimple("Task", orderCoreRoot, C2SIM_Util.core_NS);

            // Get list of IBML tasks
            List<Element> ibmlTasks = C2SIM_Util.findElementSimple("OrderPush/Task", in, C2SIM_Util.ibml09_NS);

            // Keep track of which task we are working on.  The number of OrderCore tasks and IBML tasks MUST be the same!!!        
            int taskNum = 0;

            // IBML Tasks can be either Ground Tasks or Air Tasks

            // Loop through IBML09 Tasks
            for (Element ibmlTask : ibmlTasks) {

                Element orderCoreTask;

                // Get list of Ground Tasks
                List<Element> groundTasks = ibmlTask.getChildren("GroundTask", C2SIM_Util.ibml09_NS);

                // Loop through Ground Tasks
                for (Element groundTask : groundTasks) {
                    orderCoreTask = orderCoreTasks.get(taskNum);
                    translateIBML09LocationTask(groundTask, orderCoreTask);
                    ++taskNum;

                }   // GroundTasks

                // Get list of Air Tasks
                List<Element> airTasks = ibmlTask.getChildren("AirTask", C2SIM_Util.ibml09_NS);

                // Loop through Air Tasks
                for (Element airTask : airTasks) {
                    orderCoreTask = orderCoreTasks.get(taskNum);
                    translateIBML09LocationTask(airTask, orderCoreTask);
                    ++taskNum;

                }   // AirTasks                

            }   // Tasks


        }   // try
        catch (Exception e) {
            throw new C2SIMException("Error in translateIBML09LocationToCore " + e);
        }   // catch   // catch


    }   // translateIBML09LocationToCore())


    /************************************/
    /*  translateIBML09TaskLocation     */
    /************************************/
    /**
    * translateIBML09LocationTask - Translate Task Location (From order) to Order Core
    @param ibmlTask - Element ibml09Task
    @param orderCoreTask - Element orderCoreTask
    @throws C2SIMException 
    */
    private static void translateIBML09LocationTask(Element ibmlTask, Element orderCoreTask) throws C2SIMException {

        // See if we have a Route or an AtWhere

        Element ibmlAtWhere = C2SIM_Util.findSingleElementSimple("Where/AtWhere", ibmlTask, C2SIM_Util.ibml09_NS);
        Element ibmlRouteWhere = C2SIM_Util.findSingleElementSimple("Where/RouteWhere", ibmlTask, C2SIM_Util.ibml09_NS);

        if (ibmlAtWhere != null) {

            // Get the Where Category
            Element ibmlWhereClass = C2SIM_Util.findSingleElementSimple("JBMLAtWhere/WhereClass", ibmlAtWhere, C2SIM_Util.ibml09_NS);

            if (ibmlWhereClass == null)
                throw new C2SIMException("IBML Order does not contain WhereCategory");
            String whereClassString = ibmlWhereClass.getText();

            // Get the IBML Locations                
            List<Element> ibmlLocations = C2SIM_Util.findElementSimple("JBMLAtWhere/WhereValue/WhereLocation", ibmlAtWhere, C2SIM_Util.ibml09_NS);

            // Point? (There should only be one Location)
            if (whereClassString.equalsIgnoreCase("PT")) {

                // Create Point and attach to Order Core task
                Element orderCorePoint = C2SIM_Util.createElementStack("Where/AtWhere/Point", orderCoreTask, C2SIM_Util.core_NS);

                // Get the elements from IBML
                Element loc = ibmlLocations.get(0);
                Element gdc = loc.getChild("GDC", C2SIM_Util.ibml09_NS);

                Element lat = gdc.getChild("Latitude", C2SIM_Util.ibml09_NS);
                Element lon = gdc.getChild("Longitude", C2SIM_Util.ibml09_NS);
                Element el = gdc.getChild("ElevationAGL", C2SIM_Util.ibml09_NS);

                // Add them to OrderCore Point
                orderCorePoint.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                orderCorePoint.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                if (el != null)
                    orderCorePoint.addContent(new Element("ElevationAGL", C2SIM_Util.core_NS).setText(el.getText()));
            }   // Point


            // Line?
            else if (whereClassString.equalsIgnoreCase("LN")) {
                // Process Line

                // The Where element already exists.  Find it and add to it.
                Element orderCoreWhere = orderCoreTask.getChild("Where", C2SIM_Util.core_NS);

                // Create AtWhere/Line and attach to Order Core Where
                Element orderCoreLine = C2SIM_Util.createElementStack("AtWhere/Line", orderCoreWhere, C2SIM_Util.core_NS);

                for (Element ibmlLocation : ibmlLocations) {

                    Element ibmlGDC = ibmlLocation.getChild("GDC", C2SIM_Util.ibml09_NS);

                    Element lat = ibmlGDC.getChild("Latitude", C2SIM_Util.ibml09_NS);
                    Element lon = ibmlGDC.getChild("Longitude", C2SIM_Util.ibml09_NS);
                    Element el = ibmlGDC.getChild("ElevationAGL", C2SIM_Util.ibml09_NS);

                    // Add them to OrderCore Line
                    Element orderCoreCoord = new Element("Coords", C2SIM_Util.core_NS);
                    orderCoreLine.addContent(orderCoreCoord);

                    orderCoreCoord.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                    orderCoreCoord.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                    if (el != null)
                        orderCoreCoord.addContent(new Element("ElevationAGL", C2SIM_Util.core_NS).setText(el.getText()));
                }   // Line Locations
            }   // Line

            // Surface?    
            else if (whereClassString.equalsIgnoreCase("SURFAC")) {

                // Create Area and attach to Order Core task
                Element orderCoreArea = C2SIM_Util.createElementStack("Where/AtWhere/Area", orderCoreTask, C2SIM_Util.core_NS);

                for (Element ibmlLocation : ibmlLocations) {


                    Element gdc = ibmlLocation.getChild("GDC", C2SIM_Util.ibml09_NS);

                    Element lat = gdc.getChild("Latitude", C2SIM_Util.ibml09_NS);
                    Element lon = gdc.getChild("Longitude", C2SIM_Util.ibml09_NS);
                    Element el = gdc.getChild("ElevationAGL", C2SIM_Util.ibml09_NS);

                    // Add them to OrderCore Area
                    Element orderCoreCoord = new Element("Coords", C2SIM_Util.core_NS);
                    orderCoreArea.addContent(orderCoreCoord);

                    orderCoreCoord.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                    orderCoreCoord.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                    if (el != null)
                        orderCoreCoord.addContent(new Element("ElevationAGL", C2SIM_Util.core_NS).setText(el.getText()));
                }   // Line Locations

            }   // Surface                

        }   // AtWhere

        if (ibmlRouteWhere != null) {

            Element lat;
            Element lon;
            Element elevation;
            Element orderCoreCoords;

            // All Route elements are written to Via in Order Core.  They will be separated into From, Via, and To later if needed
            Element orderCoreVia = C2SIM_Util.createElementStack("Where/RouteWhere/Via", orderCoreTask, C2SIM_Util.core_NS);

            // Get contents of IBML From
            Element ibmlFromPoint = C2SIM_Util.findSingleElementSimple("From-Via-To/From/Coords/GDC", ibmlRouteWhere, C2SIM_Util.ibml09_NS);

            lat = ibmlFromPoint.getChild("Latitude", C2SIM_Util.ibml09_NS);
            lon = ibmlFromPoint.getChild("Longitude", C2SIM_Util.ibml09_NS);
            elevation = ibmlFromPoint.getChild("ElevationAGL", C2SIM_Util.ibml09_NS);

            // Add Coords to OrderCoreVia
            orderCoreCoords = new Element("Coords", C2SIM_Util.core_NS);
            orderCoreVia.addContent(orderCoreCoords);

            // Add to Coords
            orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
            orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
            if (elevation != null)
                orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));


            // Get contents of IBML Via
            List<Element> ibmlVias = C2SIM_Util.findElementSimple("From-Via-To/Via/Waypoint", ibmlRouteWhere, C2SIM_Util.ibml09_NS);

            for (Element ibmlVia : ibmlVias) {
                Element ibmlViaPoint = C2SIM_Util.findSingleElementSimple("Location/Coords/GDC", ibmlVia, C2SIM_Util.ibml09_NS);

                lat = ibmlViaPoint.getChild("Latitude", C2SIM_Util.ibml09_NS);
                lon = ibmlViaPoint.getChild("Longitude", C2SIM_Util.ibml09_NS);
                elevation = ibmlViaPoint.getChild("ElevationAMSL", C2SIM_Util.ibml09_NS);

                // Add Coords to OrderCoreVia
                orderCoreCoords = new Element("Coords", C2SIM_Util.core_NS);
                orderCoreVia.addContent(orderCoreCoords);

                // Add IBML Via to OrderCoreCoord
                orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                if (elevation != null)
                    orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));
            }   // Via Points

            // Get contents of IBML To
            Element ibmlTo = C2SIM_Util.findSingleElementSimple("From-Via-To/To/Coords/GDC", ibmlRouteWhere, C2SIM_Util.ibml09_NS);

            lat = ibmlTo.getChild("Latitude", C2SIM_Util.ibml09_NS);
            lon = ibmlTo.getChild("Longitude", C2SIM_Util.ibml09_NS);
            elevation = ibmlTo.getChild("ElevationAMSL", C2SIM_Util.ibml09_NS);

            // Add To information to OrderCoreVia
            orderCoreCoords = new Element("Coords", C2SIM_Util.core_NS);
            orderCoreVia.addContent(orderCoreCoords);

            // Add to OrderCoreCoord
            orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
            orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
            if (elevation != null)
                orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));
        }   // ibmlRouteWhere

    } // translateIBML09Task()


    /************************************/
    /* translateCoreLocationToIBML009   */
    /************************************/
    /**
    * translateCoreLocationToIBML09 - Retrieve the location element from OrderCore document and add to IBML09 document 
    @param in
    @param out
    @throws C2SIMException 
    */
   
    private static void translateCoreLocationToIBML09(Document in, Document out) throws C2SIMException {

        Element lat;
        Element lon;
        Element elevation = null;


        try {
            // Get root elements
            Element orderCoreRoot = in.getRootElement();
            Element ibmlRoot = out.getRootElement();
            int whatIndex;

            // Keep track of which taask we aare working on
            int taskNum = 0;

            // Make list of OrderCore tasks and IBML Tasks
            List<Element> orderCoreTasks = C2SIM_Util.findElementSimple("Task", orderCoreRoot, C2SIM_Util.core_NS);
            List<Element> ibmlTasks = C2SIM_Util.findElementSimple("OrderPush/Task/GroundTask", ibmlRoot, C2SIM_Util.ibml09_NS);

            // Loop through the OrderCore tasks
            for (Element orderCoreTask : orderCoreTasks) {
                


                // Locate the position in the proper C2SIM Task where the Line or Route will go
                Element ibmlTask = ibmlTasks.get(taskNum);

                                // Set the ActionTaskActivityCode to a fixed value
                Element action = C2SIM_Util.findSingleElementSimple("What/WhatCode", ibmlTask, C2SIM_Util.ibml09_NS);
                action.setText("ADVANC");
                        
                // Get the index of IBML What Where/Location information goes after
                whatIndex = findChildIndex("What", ibmlTask, C2SIM_Util.ibml09_NS);

                // Does this Order/Core document have a Point?
                Element orderCorePoint = C2SIM_Util.findSingleElementSimple("Where/AtWhere/Point", orderCoreTask, C2SIM_Util.core_NS);

                if (orderCorePoint != null) {

                    // Get Where/AtWhere/JBMLAtWhere with children filled in
                    Element ibmlWhere = buildIBMLAtWhere("PT");

                    // Attach Where to task
                    ibmlTask.addContent(whatIndex + 1, ibmlWhere);

                    // Locate WhereValue
                    Element ibmlWhereValue = C2SIM_Util.findSingleElementSimple("AtWhere/JBMLAtWhere/WhereValue", ibmlWhere, C2SIM_Util.ibml09_NS);

                    // Get location from OrderCore
                    lat = orderCorePoint.getChild("Latitude", C2SIM_Util.core_NS);
                    lon = orderCorePoint.getChild("Longitude", C2SIM_Util.core_NS);
                    elevation = orderCorePoint.getChild("ElevationAGL", C2SIM_Util.core_NS);

                    // Add GDC
                    Element ibmlGDC = C2SIM_Util.createElementStack("WhereLocation/GDC", ibmlWhereValue, C2SIM_Util.ibml09_NS);

                    // Add location information to GDC
                    ibmlGDC.addContent(new Element("Latitude", C2SIM_Util.ibml09_NS).setText(lat.getText()));
                    ibmlGDC.addContent(new Element("Longitude", C2SIM_Util.ibml09_NS).setText(lon.getText()));
                    if (elevation != null)
                        ibmlGDC.addContent(new Element("ElevationAGL", C2SIM_Util.ibml09_NS).setText(elevation.getText()));

                }   // Order Core Point

                // Does this Order Core doument have a line?
                List<Element> orderCoreLine = C2SIM_Util.findElementSimple("Where/AtWhere/Line/Coords", orderCoreTask, C2SIM_Util.core_NS);
                if (orderCoreLine.size() != 0) {

                    // Get Where/AtWhere/JBMLAtWhere with children filled in
                    Element ibmlWhere = buildIBMLAtWhere("LN");

                    // Attach Where to task
                    ibmlTask.addContent(whatIndex + 1, ibmlWhere);

                    // Locate WhereValue
                    Element ibmlWhereValue = C2SIM_Util.findSingleElementSimple("AtWhere/JBMLAtWhere/WhereValue", ibmlWhere, C2SIM_Util.ibml09_NS);

                    for (Element orderCoreLinePoint : orderCoreLine) {

                        // Get location from OrderCore
                        lat = orderCoreLinePoint.getChild("Latitude", C2SIM_Util.core_NS);
                        lon = orderCoreLinePoint.getChild("Longitude", C2SIM_Util.core_NS);
                        elevation = orderCoreLinePoint.getChild("ElevationAGL", C2SIM_Util.core_NS);

                        // Add GDC
                        Element ibmlGDC = C2SIM_Util.createElementStack("WhereLocation/GDC", ibmlWhereValue, C2SIM_Util.ibml09_NS);

                        // Add location information to GDC
                        ibmlGDC.addContent(new Element("Latitude", C2SIM_Util.ibml09_NS).setText(lat.getText()));
                        ibmlGDC.addContent(new Element("Longitude", C2SIM_Util.ibml09_NS).setText(lon.getText()));
                        if (elevation != null)
                            ibmlGDC.addContent(new Element("ElevationAGL", C2SIM_Util.ibml09_NS).setText(elevation.getText()));

                    }   // OrdeCore Line Points

                }   // OrderCore Line

                // Does this Order Core doument have a Area?
                List<Element> orderCoreArea = C2SIM_Util.findElementSimple("Where/AtWhere/Area/Coords", orderCoreTask, C2SIM_Util.core_NS);
                if (orderCoreArea.size() != 0) {

                    // Get Where/AtWhere/JBMLAtWhere with children filled in
                    Element ibmlWhere = buildIBMLAtWhere("SURFAC");

                    // Attach Where to task
                    ibmlTask.addContent(whatIndex + 1, ibmlWhere);

                    // Locate WhereValue
                    Element ibmlWhereValue = C2SIM_Util.findSingleElementSimple("AtWhere/JBMLAtWhere/WhereValue", ibmlWhere, C2SIM_Util.ibml09_NS);

                    for (Element orderCoreAreaPoint : orderCoreLine) {

                        // Get location from OrderCore
                        lat = orderCoreAreaPoint.getChild("Latitude", C2SIM_Util.core_NS);
                        lon = orderCoreAreaPoint.getChild("Longitude", C2SIM_Util.core_NS);
                        lat = orderCoreAreaPoint.getChild("ElevationAGL", C2SIM_Util.core_NS);

                        // Add GDC
                        Element ibmlGDC = C2SIM_Util.createElementStack("WhereLocation/GDC", ibmlWhereValue, C2SIM_Util.ibml09_NS);

                        // Add location information to GDC
                        ibmlGDC.addContent(new Element("Latitude", C2SIM_Util.ibml09_NS).setText(lat.getText()));
                        ibmlGDC.addContent(new Element("Longitude", C2SIM_Util.ibml09_NS).setText(lon.getText()));
                        if (lat != null)
                            ibmlGDC.addContent(new Element("ElevationAGL", C2SIM_Util.ibml09_NS).setText(elevation.getText()));

                    }   // OrderCoreArea Points

                }   // OrderCore Area


                // Does this OrderCore document have a RouteWhere
                List<Element> orderCoreRouteCoords = C2SIM_Util.findElementSimple("Where/RouteWhere/Via/Coords", orderCoreTask, C2SIM_Util.core_NS);
                if (orderCoreRouteCoords.size() != 0) {
                    // Create RouteWhere and attach it to the IBML Task

                    // Set up Where/RouteWhere/From-Via-To
                    Element ibmlWhere = new Element("Where", C2SIM_Util.ibml09_NS);

                    Element rWhere = new Element("RouteWhere", C2SIM_Util.ibml09_NS);
                    ibmlWhere.addContent(rWhere);

                    Element fvt = new Element("From-Via-To", C2SIM_Util.ibml09_NS);
                    rWhere.addContent(fvt);

                    // Set Via and attach to fvt
                    Element via = new Element("Via", C2SIM_Util.ibml09_NS);
                    fvt.addContent(via);

                    // Attach ibmlWhere to IBML Task
                    ibmlTask.addContent(whatIndex + 1, ibmlWhere);

                    // Work through the list of OrderCore coords
                    Element orderCoreRouteCoord;
                    for (int i = 0; i < orderCoreRouteCoords.size(); ++i) {

                        orderCoreRouteCoord = orderCoreRouteCoords.get(i);

                        // From ?
                        if (i == 0) {
                            // Get lat/lon information
                            lat = orderCoreRouteCoord.getChild("Latitude", C2SIM_Util.core_NS);
                            lon = orderCoreRouteCoord.getChild("Longitude", C2SIM_Util.core_NS);
                            elevation = orderCoreRouteCoord.getChild("ElevationAGL", C2SIM_Util.core_NS);

                            // Create IBML GDC and attach Location informaataion
                            Element ibmlGDC = C2SIM_Util.createElementStack("From/Coords/GDC", fvt, C2SIM_Util.ibml09_NS);
                            ibmlGDC.addContent(new Element("Latitude", C2SIM_Util.ibml09_NS).setText(lat.getText()));
                            ibmlGDC.addContent(new Element("Longitude", C2SIM_Util.ibml09_NS).setText(lon.getText()));
                            if (elevation != null)
                                ibmlGDC.addContent(new Element("ElevationAGL", C2SIM_Util.ibml09_NS).setText(elevation.getText()));

                        }   // From

                        // To ?
                        else if (i == orderCoreRouteCoords.size() - 1) {

                            // Get lat/lon information
                            lat = orderCoreRouteCoord.getChild("Latitude", C2SIM_Util.core_NS);
                            lon = orderCoreRouteCoord.getChild("Longitude", C2SIM_Util.core_NS);
                            elevation = orderCoreRouteCoord.getChild("ElevationAGL", C2SIM_Util.core_NS);

                            // Create IBML GDC and attach Location informaataion
                            Element ibmlGDC = C2SIM_Util.createElementStack("To/Coords/GDC", fvt, C2SIM_Util.ibml09_NS);
                            ibmlGDC.addContent(new Element("Latitude", C2SIM_Util.ibml09_NS).setText(lat.getText()));
                            ibmlGDC.addContent(new Element("Longitude", C2SIM_Util.ibml09_NS).setText(lon.getText()));
                            if (elevation != null)
                                ibmlGDC.addContent(new Element("ElevationAGL", C2SIM_Util.ibml09_NS).setText(elevation.getText()));
                        }   // To

                        // Must be Via                        else {
                        else {
                            // Add a Via point
                            // Get lat/lon information
                            Element coord = orderCoreRouteCoords.get(i);

                            lat = coord.getChild("Latitude", C2SIM_Util.core_NS);
                            lon = coord.getChild("Longitude", C2SIM_Util.core_NS);
                            elevation = coord.getChild("ElevationAGL", C2SIM_Util.core_NS);

                            // Create IBML GDC and attach Location informaataion
                            Element ibmlGDC = C2SIM_Util.createElementStack("Waypoint/Location/Coords/GDC", via, C2SIM_Util.ibml09_NS);

                            ibmlGDC.addContent(new Element("Latitude", C2SIM_Util.ibml09_NS).setText(lat.getText()));
                            ibmlGDC.addContent(new Element("Longitude", C2SIM_Util.ibml09_NS).setText(lon.getText()));
                            if (elevation != null)
                                ibmlGDC.addContent(new Element("ElevationAGL", C2SIM_Util.ibml09_NS).setText(elevation.getText()));
                        }   // else

                    }   // Route Coords
                }   // Route
                ++taskNum;
            }   // Tasks

        }   // try
        catch (Exception e) {
            throw new C2SIMException("Exception in translateCoreLocationTo IBML09 " + e);
        }   // catch   // catch

    }   // translateLocationtoIBML009    


    /************************/
    /*  buildIBMLAtWhere    */
    /************************/
    /**
     * buildIBMLAtWhere - Build IBML09 AtWhere
    @param wClassStr
    @return atWhere Element
    */
    private static Element buildIBMLAtWhere(String wClassStr) {
        // Build a skelaton IBML09 AtWhere

        // Build Where/AtWhere/JBMLAtWhere
        //  Add WhereLabel, WhereCategory, Wherelass, WhereValue, WhereQualifier - Children of JBMLAtWhere 

        Element where = new Element("Where", C2SIM_Util.ibml09_NS);

        Element atWhere = new Element("AtWhere", C2SIM_Util.ibml09_NS);
        where.addContent(atWhere);

        Element jAtWhere = new Element("JBMLAtWhere", C2SIM_Util.ibml09_NS);
        atWhere.addContent(jAtWhere);

        Element wLab = new Element("WhereLabel", C2SIM_Util.ibml09_NS).setText("Translated");
        jAtWhere.addContent(wLab);

        Element wCat = new Element("WhereCategory", C2SIM_Util.ibml09_NS).setText("CONTROLPOINT");
        jAtWhere.addContent(wCat);

        Element wClass = new Element("WhereClass", C2SIM_Util.ibml09_NS).setText(wClassStr);
        jAtWhere.addContent(wClass);

        Element wValue = new Element("WhereValue", C2SIM_Util.ibml09_NS);
        jAtWhere.addContent(wValue);

        Element wQual = new Element("WhereQualifier", C2SIM_Util.ibml09_NS).setText("AT");
        jAtWhere.addContent(wQual);

        return where;

    }   // buildIBMLAtWhere()


    /************************/
    /* processIBML09_GSR    */
    /************************/

    /**
    * processIBML09_GSR
        Process message containing multiple IBML09 General Status Reports
        Translate each GSR to Unit_Core
        Store individual GSR (Unit_Core) with unit name in unitDB
        Translate Unit_Core document to CWIX_PSR
        Publish CWIX_PSR
     
    @param trans
    @throws C2SIMException 
    */
    static void processIBML09_GSR(C2SIM_Transaction trans) throws C2SIMException {

        // Publish the original message
        C2SIM_Server_STOMP.publishMessage(trans);

        // Translate IBML09_GSR to Unit core
        Document d = trans.getDocument();
        Document unitCoreDoc;

        // Get root (BMLReport)
        Element e = d.getRootElement();

        // Get list of individual General Status Reports
        List<Element> c = e.getChildren("Report", C2SIM_Util.ibml09_NS);

        // Iterate through the list of individual reports
        for (Element el : c) {

            Element e2 = el.clone();

            // Create Document using this Report as the root
            Document d2 = new Document(e2);

            // translate the report from the incoming IBML09_GSR into Unit_Core
            try {
                unitCoreDoc = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("IBML_GSR_Single"), d2, "F");
            }   // try

            catch (C2SIMException ex) {
                C2SIM_Server.debugLogger.error("Exception while translating Unit to IBML09");
                throw new C2SIMException("Exception while translating Unit to IBML09", ex);
            }   // catch   // catch


            /*
                Mapping moves ReporterWho to ReporterWho and Executor to ID in UnitCore- Both containing Names
                    Convert ReporterWho and ID to UUIDs using cross reference created during initialization
             */
            Element ucoreE = unitCoreDoc.getRootElement();

            // Translate OperationalStatus to Core (C2SIM)
            Element opStatus = C2SIM_Util.findSingleElementSimple("OperationalStatus", ucoreE, C2SIM_Util.core_NS);
            if (opStatus != null) {
                String os = opStatus.getText();
                String osN = "";

                switch (os) {
                    case "OPR":
                        osN = "FullyOperational";
                        break;
                    case "MOPS":
                        osN = "MostlyOperational";
                        break;
                    case "NOP":
                        osN = "NotOperational";
                        break;
                    case "SOPS":
                        osN = "PartlyOperational";
                        break;
                }   // switch
                opStatus.setText(osN);

            }   // if            

            // Convert name in ID to UUID if present in table
            Element idE = ucoreE.getChild("ID", C2SIM_Util.core_NS);

            if (idE != null) {
                String id = idE.getText();
                idE.setText(C2SIM_Util.nameToUUID.get(id));

                if (idE.getText().equals("")) {
                idE.setText("00000000-0000-0000-0000-000000000000");
//                    C2SIM_Server.debugLogger.error("Message number" + trans.getMsgnumber() + "  Translation aborted - Required element missing");
//                    continue;
                }
            }

            // Convert Name in ReporterWho to UUID if in table
            Element repE = ucoreE.getChild("ReporterWho", C2SIM_Util.core_NS);

            if (repE != null) {
                String rep = repE.getText();
                repE.setText(C2SIM_Util.nameToUUID.get(rep));
                if (repE.getText().equals("")) {
                    repE.setText("00000000-0000-0000-0000-000000000000");
                    //C2SIM_Server.debugLogger.error("Message number" + trans.getMsgnumber() + "  Translation aborted - Required element missing");
                    //continue;
                }
            }

            // Translate IBML09 date to C2SIM Date on core document
            convertReportDate(unitCoreDoc, "IBML09", "C2SIM");

            // Add unit core document to transaction
            trans.setCoreDocument(unitCoreDoc);

            // Update latest position
            C2SIM_C2SIM.updatePosition(unitCoreDoc);

            // Translate core to C2SIM and publish
            C2SIM_C2SIM.translateCoreToC2SIM_PR(trans);

            // Translate core to CBML and publish
            C2SIM_CBML.translateCoreToCBML_GSR(trans);

        }   // for

    }   // processIBML09_GSR()


    /*******************************/
    /* transleCoreToIBML009_GSR    */
    /*******************************/
    /**
    * translateCoreToIBML09_GSR - Translate Core Report (Unit) to IBML09 Core Status Report
    @param trans
    @throws C2SIMException 
    */
    static void translateCoreToIBML09_GSR(C2SIM_Transaction trans) throws C2SIMException {

        // Are we translating IBML Reports?
        if (!translateToIBML09)
            return;

        Document d = trans.getCoreDocument().clone();
        /*
            Convert ReporterWho and ID in Core from UUID to Name using cross reference created during initialization
         */
        Element ucoreE = d.getRootElement();

//        // Convert name in ID to UUID if present in table
//        Element idE = ucoreE.getChild("ID", C2SIM_Util.core_NS);
//        if (idE != null) {
//            String id = idE.getText();
//            idE.setText(C2SIM_Util.nameToUUID.get(id));
//            if (idE.getText().equals("")) {
//                C2SIM_Server.debugLogger.error("Message number" + trans.getMsgnumber() + "  Translation aborted - Required element missing");
//                return;
//            }
//        }
//
//        // Convert Name in ReporterWho to UUID if present in table
//        Element repE = ucoreE.getChild("ReporterWho", C2SIM_Util.core_NS);
//        if (repE != null) {
//            String rep = repE.getText();
//            repE.setText(C2SIM_Util.uuidToName.get(rep));
//            if (repE.getText().equals("")) {
//                C2SIM_Server.debugLogger.error("Message number" + trans.getMsgnumber() + "  Translation aborted - Required element missing");
//                return;
//            }
//        }

        // Convert date to MIP (IBML09)
        convertReportDate(d, "C2SIM", "IBML09");

        // Translate unitCoreDoc to IBML09 GSR
        Document ibml = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("IBML_GSR_Single"), d, "R");
        Element ibmlR = ibml.getRootElement();

        // Translate Operational Status to IBML
        Element opStatus = C2SIM_Util.findSingleElementSimple("StatusReport/GeneralStatusReport/OpStatus", ibmlR, C2SIM_Util.ibml09_NS);

        if (opStatus != null) {
            String os = opStatus.getText();
            String osN = "";

            switch (os) {
                case "FullyOperational":
                    osN = "OPR";
                    break;
                case "MostlyOperational":
                    osN = "MOPS";
                    break;
                case "NotOperational":
                    osN = "NOP";
                    break;
                case "PartlyOperational":
                    osN = "SOPS";
                    break;
            }   // switch
            opStatus.setText(osN);

        }   // if
        // Imbed the single Report into a BMLReport document
        Element ir = new Element("BMLReport", C2SIM_Util.ibml09_NS);
        ir.addContent(ibml.detachRootElement());
        Document newD = new Document(ir);

        // Modify transaction object for publishing translated IBML09 GSR
        trans.setProtocol("BML");
        String xml = C2SIM_Util.xmlToStringD(newD, trans);
        trans.setMessageDef(C2SIM_Util.mdIndex.get("IBML_GSR_Single"));
        trans.setMsTemp("IBML09_PositionReport");
        trans.setXmlText(xml);
        trans.setSource("Translated");

        // Publish the IBML 09 GSR 
        C2SIM_Server_STOMP.publishMessage(trans);

    }   // translateCoreToIBML09_GSR()


    /********************************/
    /* translateCoreToIBML09_Order  */
    /********************************/
    /**
    * translateCoreToIBML09_Order - Translate Core Order to IBML09
    @param t - C2SIM_Transaction
    @throws C2SIMException 
    */
    static void translateCoreToIBML09_Order(C2SIM_Transaction t) throws C2SIMException {

        Document ibml09Order;
        Document d;
        Element root;
        Element eWhere;
        Element eAtWhere;
        Element ePoint;
        Element eLine;
        Element eArea;

        // Are we translating IBML09 Orders?
        if (!translateToIBML09)
            return;

        d = t.getCoreDocument().clone();
        root = d.getRootElement();

        // Convert dates to IBML09 format
        convertOrderDates(d, "C2SIM", "IBML09");

        /*
        Core contains UUIDs for TaskerWho and TaskeeWho - Convert them to Names in a copy of order core
         */

        // Get orderCore Root
        Element ocRoot = d.getRootElement();

        // Get the taskerWho text
        String tasker;
        Element taskerE = ocRoot.getChild("TaskerWho", C2SIM_Util.core_NS);
        if (taskerE != null) {
            tasker = taskerE.getText();
            // Set the OrderCore TaskerWho to the Name
            taskerE.setText(C2SIM_Util.uuidToName.get(tasker));
            if (taskerE.getText().equals("")) {
//                C2SIM_Server.debugLogger.error("Message number" + t.getMsgnumber() + "  Translation aborted - Required element missing");
//                return;
            }
        }

        // Get list of tasks and convert taskeeWho and affectedWho in each
        List<Element> tasks = ocRoot.getChildren("Task", C2SIM_Util.core_NS);

        for (Element task : tasks) {
            Element taskeeE = task.getChild("TaskeeWho", C2SIM_Util.core_NS);
            if (taskeeE != null) {
                String taskee = taskeeE.getText();
                taskeeE.setText(C2SIM_Util.uuidToName.get(taskee));
                if (taskeeE.getText().equals("")) {
//                    C2SIM_Server.debugLogger.error("Message number" + t.getMsgnumber() + "  Translation aborted - Required element missing");
//                    return;
                }
            }


            Element affectedE = task.getChild("AffectedWho", C2SIM_Util.core_NS);
            if (affectedE != null) {
                String affected = affectedE.getText();
                affectedE.setText(C2SIM_Util.uuidToName.get(affected));
                if (affectedE.getText().equals("")) {
//                    C2SIM_Server.debugLogger.error("Message number" + t.getMsgnumber() + "  Translation aborted - Required element missing");
//                    return;
                }
            }

        }

        // Now translate from OrderCore to IBML09 Order
        Document ibmlOrder = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("IBML_Order"), d, "R");

        translateCoreLocationToIBML09(d, ibmlOrder);

        // Set up the C2SIM Transaction
        t.setProtocol("BML");
        String xml = C2SIM_Util.xmlToStringD(ibmlOrder, t);

        // Set parameters in the Transaction object for this message
        t.setMessageDef(C2SIM_Util.mdIndex.get("IBML_Order"));
        t.setXmlText(xml);
        t.setSource("Translated");

        // Publish the C2SIM Order
        C2SIM_Server_STOMP.publishMessage(t);

    }   //translateCoreToIBML09_Order()


    /************************/
    /* convertOrderDates    */
    /************************/
    // Convert the dates in OrderCore documents from IBML/CBML format  
    /**
    * convertOrderDates - Convert dates in OrderCore Document between MIP and ISO format
    @param d - Document - Order Core Document
    @param source - String Source Format (IBML09 or C2SIM)
    @param target - String Target Format (IBML09 or C2SIM)
    @throws C2SIMException 
    */
    public static void convertOrderDates(Document d, String source, String target) throws C2SIMException {

        Element root = d.getRootElement();
        String convDate;
        Element oiw;
        Element sw;
        Element ew;

        // OrderIssuedWnen - At root of order
        oiw = C2SIM_Util.findSingleElementSimple("OrderIssuedWhen", root, C2SIM_Util.core_NS);

        if (oiw != null) {
            convDate = C2SIM_Util.convertDate(oiw.getText(), source, target);
            oiw.setText(convDate);
        }   // if

        // Get list of tasks and convert StartTime and EndTime in each task
        List<Element> taskL = C2SIM_Util.findElementSimple("Task", root, C2SIM_Util.core_NS);
        for (Element task : taskL) {

            // StartWhen     
            sw = C2SIM_Util.findSingleElementSimple("StartWhen/WhenDateTime", task, C2SIM_Util.core_NS);
            if (sw != null) {
                convDate = C2SIM_Util.convertDate(sw.getText(), source, target);
                sw.setText(convDate);
            }

            // EndtWhen     
            ew = C2SIM_Util.findSingleElementSimple("EndWhen/WhenDateTime", task, C2SIM_Util.core_NS);
            if (ew != null) {
                convDate = C2SIM_Util.convertDate(ew.getText(), "IBML09", "C2SIM");
                ew.setText(convDate);
            }
        }   // for tasks  

    }   // convertOrderDates()


    /************************/
    /* convertReportDate    */
    /************************/
    /**
    * convertReportDate - Convert date in report between MIP and ISO format
    @param d - Document Report 
    @param source - String - Source format (IBML09 or C2SIM)
    @param target - String - Target format (IBML09 or C2SIM)
    @throws C2SIMException 
    */
    public static void convertReportDate(Document d, String source, String target) throws C2SIMException {
        Element root = d.getRootElement();
        Element date = root.getChild("ReportedWhenStart", C2SIM_Util.core_NS);
        if (date != null) {
            String convDate = C2SIM_Util.convertDate(date.getText(), source, target);
            date.setText(convDate);
        }   // if
    }   // convertReportDate()


}   // C2SIM_IBML09 Class
