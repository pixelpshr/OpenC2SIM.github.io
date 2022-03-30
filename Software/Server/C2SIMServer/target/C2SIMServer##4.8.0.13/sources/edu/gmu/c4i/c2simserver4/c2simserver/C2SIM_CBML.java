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
/**
 * 
*/
package edu.gmu.c4i.c2simserver4.c2simserver;

import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_C2SIM.findChildIndex;
import static edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_IBML09.processIBML09_GSR;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;


/**
 * <h1>C2SIM_CBML</h1> <p>
 * Performs processing of CBML Messages
 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_CBML {
    static boolean translateToCBML = C2SIM_Util.toBoolean(C2SIM_Server.props.getProperty("server.TranslateToCBML"));   

    
    /************/
    /* process  */
    /************/
    /**
    * process - Perform initial processing of CBML Message
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    static void process(C2SIM_Transaction trans) throws C2SIMException {
        if (trans.messageDef.messageDescriptor.equalsIgnoreCase("CBML_Report"))
            processCBML_GSR(trans);
        else if (trans.messageDef.messageDescriptor.equalsIgnoreCase("CBML_Order"))
            processCBML_Order(trans);
        else
            throw new C2SIMException("Unknown CBML message descriptor");
        return;
    }


    /********************/
    /* processCBMLOrder */
    /********************/
    /**
    * processCBML_Order
    @param t - C2SIM_Transaction
    @throws C2SIMException 
    */
    public static void processCBML_Order(C2SIM_Transaction t) throws C2SIMException {

        Document orderCore;
        Document saveOrderCore;
        Document d = t.getDocument();
        Element root;
        String lat = "";
        String lon = "";
        String el = "";
        Element eLine = null;
        Element eWhereClass;
        Element eWhereCategory;
        Element eAtWhere;
        Element ePoint;


        // Publish the original message
        C2SIM_Server_STOMP.publishMessage(t);

        // Map CBML Order to Core
        orderCore = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("CBML_Order"), d, "F");

        // Convert dates in order to C2SIM Format
        C2SIM_IBML09.convertOrderDates(orderCore, "IBML09", "C2SIM");

        // Convert the Location informaation from CBML to OrderCore
        translateCBMLLocationToCore(d.getRootElement(), orderCore);
        
        // Save copy of orderCore for other dialects
        saveOrderCore = orderCore.clone();

        // Add orderCore to transaction
        t.setCoreDocument(orderCore);
        
        // Translate core to C2SIM Order and publish
        C2SIM_C2SIM.translateCoreToC2SIM_Order(t);

        // Translate core to IBML Order and publish
        C2SIM_IBML09.translateCoreToIBML09_Order(t);

    }   // processIBML09_Order()  


    /***********************************/
    /* translateCBMLLocationToCore     */
    /***********************************/
    /**
    * translateCBMLLocationToCore - Translate elements of CBML Location to core Location
    @param in - Element - CBML Locatoin
    @param out - Document Core Document
    */
    // Map location information from input CBML Order (Positioned at Order Body) to output (orderCore)
    private static void translateCBMLLocationToCore(Element in, Document out) {

        Element orderCoreRoot = out.getRootElement();
        Element orderCoreTask = null;

        // Make list of OrderCore tasks
        List<Element> orderCoreTasks = C2SIM_Util.findElementSimple("Task", orderCoreRoot, C2SIM_Util.core_NS);

        // Make list of CBML tasks
        List<Element> cbmlTasks = C2SIM_Util.findElementSimple("Body/Order/Execution/Tasks/Task", in, C2SIM_Util.cbml_NS);

        // Keep track of which task we are working on.  The number of OrderCore tasks and CBML tasks MUST be the same!!!        
        int taskNum = 0;

        // Loop through CBML Tasks
        for (Element t : cbmlTasks) {

            // Point to corresponding OrderCore task
            orderCoreTask = orderCoreTasks.get(taskNum);

            // Do we have a CBML Point?
            Element cbmlPoint = C2SIM_Util.findSingleElementSimple("AtWhere/LocationLight/PointLight/GDC/SpecificPoint", t, C2SIM_Util.cbml_NS);
            if (cbmlPoint != null) {

                // Creaate point element and connect it to the task
                Element orderCorePoint = C2SIM_Util.createElementStack("Where/AtWhere/Point", orderCoreTask, C2SIM_Util.core_NS);

                // Get lat/lon/elev from CBML
                Element lat = cbmlPoint.getChild("Latitude", C2SIM_Util.cbml_NS);
                Element lon = cbmlPoint.getChild("Longitude", C2SIM_Util.cbml_NS);
                Element elevation = cbmlPoint.getChild("ElevationAGL", C2SIM_Util.cbml_NS);

                // Add orderCorePoint to OrderCore task
                orderCorePoint.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                orderCorePoint.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                if (elevation != null)
                    orderCorePoint.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));
            }   // cbmlPoint

            // Do we have a CBML Line ?
            List<Element> cbmlLine = C2SIM_Util.findElementSimple("AtWhere/LocationLight/Line/GDC", t, C2SIM_Util.cbml_NS);
            if (cbmlLine.size() != 0) {

                // Creaate line element and connect it to the task
                Element orderCoreLine = C2SIM_Util.createElementStack("Where/AtWhere/Line", orderCoreTask, C2SIM_Util.core_NS);

                // Loop through the points in the line
                for (Element cbmlLinePoint : cbmlLine) {

                    Element lat = C2SIM_Util.findSingleElementSimple("SpecificPoint/Latitude", cbmlLinePoint, C2SIM_Util.cbml_NS);
                    Element lon = C2SIM_Util.findSingleElementSimple("SpecificPoint/Longitude", cbmlLinePoint, C2SIM_Util.cbml_NS);
                    Element elevation = C2SIM_Util.findSingleElementSimple("SpecificPoint/ElevationAGL", cbmlLinePoint, C2SIM_Util.cbml_NS);

                    // Create Coords in OrderCore
                    Element orderCoreCoords = new Element("Coords", C2SIM_Util.core_NS);
                    orderCoreLine.addContent(orderCoreCoords);

                    // Add to OrderCoreCoord
                    orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                    orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                    if (elevation != null)
                        orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));
                }   // linePoints
            }   // CBML Line

            // Do we have a CBML Surface
            List<Element> cbmlSurface = C2SIM_Util.findElementSimple("AtWhere/LocationLight/Surface/BoundingLineLight/GDC", t, C2SIM_Util.cbml_NS);
            if (cbmlSurface.size() != 0) {

                // Create area element and connect it to the task
                Element orderCoreArea = C2SIM_Util.createElementStack("Where/AtWhere/Area", orderCoreTask, C2SIM_Util.core_NS);

                // Loop through the points in the BoundingLine

                for (Element cbmlBoundingLinePoint : cbmlSurface) {

                    Element lat = C2SIM_Util.findSingleElementSimple("SpecificPoint/Latitude", cbmlBoundingLinePoint, C2SIM_Util.cbml_NS);
                    Element lon = C2SIM_Util.findSingleElementSimple("SpecificPoint/Longitude", cbmlBoundingLinePoint, C2SIM_Util.cbml_NS);
                    Element elevation = C2SIM_Util.findSingleElementSimple("SpecificPoint/ElevationAGL", cbmlBoundingLinePoint, C2SIM_Util.cbml_NS);

                    // Create Coords in OrderCore
                    Element orderCoreCoords = new Element("OrderCoords", C2SIM_Util.core_NS);
                    orderCoreArea.addContent(orderCoreCoords);

                    // Add to OrderCoreCoord
                    orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                    orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                    if (elevation != null)
                        orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));
                }   // linePoints

            }   // CBML Surface

            // Do we have a CBML Route
            Element cbmlFrom = C2SIM_Util.findSingleElementSimple("RouteWhereLight/SpecificRoute/FromWhere", t, C2SIM_Util.cbml_NS);
            if (cbmlFrom != null) {
                // A route must have a FromWhere and a ToWhere.  It may have ViaWhere's

                Element lat;
                Element lon;
                Element elevation;
                Element orderCoreCoords;

                // All Route elements are written to Via in Order Core.  They will be separated into From, Via, To later if needed
                Element orderCoreVia = C2SIM_Util.createElementStack("Where/RouteWhere/Via", orderCoreTask, C2SIM_Util.core_NS);

                // Get contents of CBML From
                Element cbmlFromPoint = C2SIM_Util.findSingleElementSimple("LocationLight/PointLight/GDC/SpecificPoint", cbmlFrom, C2SIM_Util.cbml_NS);

                lat = C2SIM_Util.findSingleElementSimple("Latitude", cbmlFromPoint, C2SIM_Util.cbml_NS);
                lon = C2SIM_Util.findSingleElementSimple("Longitude", cbmlFromPoint, C2SIM_Util.cbml_NS);
                elevation = C2SIM_Util.findSingleElementSimple("ElevationAGL", cbmlFromPoint, C2SIM_Util.cbml_NS);

                // Add to OrderCoreVia
                orderCoreCoords = new Element("Coords", C2SIM_Util.core_NS);
                orderCoreVia.addContent(orderCoreCoords);
                
                // Add to Coords
                orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                if (elevation != null)
                    orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));


                // Get contents of CBML Via
                List<Element> cbmlVias = C2SIM_Util.findElementSimple("RouteWhereLight/SpecificRoute/ViaWhere", t, C2SIM_Util.cbml_NS);
                for (Element cbmlVia : cbmlVias) {
                    Element cbmlViaPoint = C2SIM_Util.findSingleElementSimple("LocationLight/PointLight/GDC/SpecificPoint", cbmlVia, C2SIM_Util.cbml_NS);

                    lat = cbmlViaPoint.getChild("Latitude", C2SIM_Util.cbml_NS);
                    lon = cbmlViaPoint.getChild("Longitude", C2SIM_Util.cbml_NS);
                    elevation = cbmlViaPoint.getChild("ElevationAMSL", C2SIM_Util.cbml_NS);

                    // Add to OrderCoreVia
                    orderCoreCoords = new Element("Coords", C2SIM_Util.core_NS);
                    orderCoreVia.addContent(orderCoreCoords);

                    // Add CBML Via to OrderCoreCoord
                    orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                    orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                    if (elevation != null)
                        orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));
                }

                // Get contents of CBML To
                Element cbmlTo = C2SIM_Util.findSingleElementSimple("RouteWhereLight/SpecificRoute/ToWhere", t, C2SIM_Util.cbml_NS);
                Element cbmlToPoint = C2SIM_Util.findSingleElementSimple("LocationLight/PointLight/GDC/SpecificPoint", cbmlTo, C2SIM_Util.cbml_NS);

                lat = cbmlToPoint.getChild("Latitude", C2SIM_Util.cbml_NS);
                lon = cbmlToPoint.getChild("Longitude", C2SIM_Util.cbml_NS);
                elevation = cbmlToPoint.getChild("ElevationAMSL", C2SIM_Util.cbml_NS);

                // Add To information to OrderCoreVia
                orderCoreCoords = new Element("Coords", C2SIM_Util.core_NS);
                orderCoreVia.addContent(orderCoreCoords);
                
                // Add to OrderCoreCoord
                orderCoreCoords.addContent(new Element("Latitude", C2SIM_Util.core_NS).setText(lat.getText()));
                orderCoreCoords.addContent(new Element("Longitude", C2SIM_Util.core_NS).setText(lon.getText()));
                if (elevation != null)
                    orderCoreCoords.addContent(new Element("Elevationfl", C2SIM_Util.core_NS).setText(elevation.getText()));

            }   // Route Points

            ++taskNum;
        }   // tasks      


    }   // translateCBMLLocationToCore())


    /*********************************/
    /* translateCoreLocationToCBML   */
    /*********************************/
    /**
    * translateCoreLocationToCBMLOrder Translate Location information in Core Document to CBML Document
    @param in - Core Document
    @param out - CBML Document
    @throws C2SIMException 
    */
    // Retrieve the location element from OrderCore document and add to CBML document
    private static void translateCoreLocationToCBMLOrder(Document in, Document out) throws C2SIMException {

        try {
            // Get root elements
            Element orderCoreRoot = in.getRootElement();
            Element cbmlRoot = out.getRootElement();

            // Used to position output elements within already existing CBML Document
            int peIndex;
            Element cbmlAtWhere = null;
            Element cbmlSpecificRoute = null;

            // Keep track of which taask we aare working on
            int taskNum = 0;

            // Make list of OrderCore tasks and C2SIM Tasks
            List<Element> orderCoreTasks = C2SIM_Util.findElementSimple("Task", orderCoreRoot, C2SIM_Util.core_NS);
            List<Element> cbmlTasks = C2SIM_Util.findElementSimple("Body/Order/Execution/Tasks/Task", cbmlRoot, C2SIM_Util.cbml_NS);

            // Loop through the OrderCore tasks
            for (Element orderCoreTask : orderCoreTasks) {

                // Locate the position in the proper CBML Task where the Location element will go
                Element cbmlTask = cbmlTasks.get(taskNum);

                // Get the index of TaskWhen Light.  Location information goes after
                peIndex = findChildIndex("TaskWhenLight", cbmlTask, C2SIM_Util.cbml_NS);

                // Does this OrderCore document have a route?
                List<Element> orderCoreRoute = C2SIM_Util.findElementSimple("Where/RouteWhere/Via/Coords", orderCoreTask, C2SIM_Util.core_NS);

                // We need to insert either a RouteWhere or an AtWhere at the appropriate place within the existing CBML Order document

                if (orderCoreRoute.size() != 0) {
                    // Translate OrderCore Route to CBML                
                    Element routeWhereLight = new Element("RouteWhereLight", C2SIM_Util.cbml_NS);
                    cbmlSpecificRoute = new Element("SpecificRoute", C2SIM_Util.cbml_NS);
                    routeWhereLight.addContent(cbmlSpecificRoute);
                    cbmlTask.addContent(peIndex + 1, routeWhereLight);

                    // Work through the elements of the OrderCore Route
                    for (int i = 0; i < orderCoreRoute.size(); ++i) {
                        Element orderCoreRouteElement = orderCoreRoute.get(i);

                        // FromWhere?
                        if (i == 0) {

                            // Get OrderCore point information
                            Element lat = orderCoreRouteElement.getChild("Latitude", C2SIM_Util.core_NS);
                            Element lon = orderCoreRouteElement.getChild("Longitude", C2SIM_Util.core_NS);
                            Element elevation = orderCoreRouteElement.getChild("ElevationAGL", C2SIM_Util.core_NS);

                            Element cbmlRouteFromWhere = C2SIM_Util.createElementStack("FromWhere/LocationLight/PointLight/GDC/SpecificPoint", cbmlSpecificRoute, C2SIM_Util.cbml_NS);

                            // Add location informataion to cbmlSpecificPoint
                            cbmlRouteFromWhere.addContent(new Element("Latitude", C2SIM_Util.cbml_NS).setText(lat.getText()));
                            cbmlRouteFromWhere.addContent(new Element("Longitude", C2SIM_Util.cbml_NS).setText(lon.getText()));
                            if (elevation != null)
                                cbmlRouteFromWhere.addContent(new Element("ElevationAMSL", C2SIM_Util.cbml_NS).setText(elevation.getText()));

                        }   // FromWhere

                        // ToWhere
                        else if (i == orderCoreRoute.size() - 1) {

                            // Get OrderCore point information
                            Element lat = orderCoreRouteElement.getChild("Latitude", C2SIM_Util.core_NS);
                            Element lon = orderCoreRouteElement.getChild("Longitude", C2SIM_Util.core_NS);
                            Element elevation = orderCoreRouteElement.getChild("ElevationAGL", C2SIM_Util.core_NS);

                            Element cbmlRouteToWhere = C2SIM_Util.createElementStack("ToWhere/LocationLight/PointLight/GDC/SpecificPoint", cbmlSpecificRoute, C2SIM_Util.cbml_NS);

                            // Add location informataion to cbmlSpecificPoint
                            cbmlRouteToWhere.addContent(new Element("Latitude", C2SIM_Util.cbml_NS).setText(lat.getText()));
                            cbmlRouteToWhere.addContent(new Element("Longitude", C2SIM_Util.cbml_NS).setText(lon.getText()));
                            if (elevation != null)
                                cbmlRouteToWhere.addContent(new Element("ElevationAMSL", C2SIM_Util.cbml_NS).setText(elevation.getText()));

                        }   // ToWhere

                        // Must be ViaWhere 
                        else {

                            // Get OrderCore point information
                            Element lat = orderCoreRouteElement.getChild("Latitude", C2SIM_Util.core_NS);
                            Element lon = orderCoreRouteElement.getChild("Longitude", C2SIM_Util.core_NS);
                            Element elevation = orderCoreRouteElement.getChild("ElevationAGL", C2SIM_Util.core_NS);

                            Element cbmlRouteViaWhere = C2SIM_Util.createElementStack("ViaWhere/LocationLight/PointLight/GDC/SpecificPoint", cbmlSpecificRoute, C2SIM_Util.cbml_NS);

                            // Add location informataion to cbmlSpecificPoint
                            cbmlRouteViaWhere.addContent(new Element("Latitude", C2SIM_Util.cbml_NS).setText(lat.getText()));
                            cbmlRouteViaWhere.addContent(new Element("Longitude", C2SIM_Util.cbml_NS).setText(lon.getText()));
                            if (elevation != null)
                                cbmlRouteViaWhere.addContent(new Element("ElevationAMSL", C2SIM_Util.cbml_NS).setText(elevation.getText()));

                        }   // ViaWhere

                    }   // Route elements

                }   // OrderCore Route
                else {
                    // Must be an AtWhere not a Route
                    cbmlAtWhere = new Element("AtWhere", C2SIM_Util.cbml_NS);
                    cbmlTask.addContent(peIndex + 1, cbmlAtWhere);

                    // Does this OrderCore document have a Point?          
                    Element orderCorePoint = C2SIM_Util.findSingleElementSimple("Where/AtWhere/Point", orderCoreTask, C2SIM_Util.core_NS);
                    if (orderCorePoint != null) {

                        // Get OrderCore point information
                        Element lat = orderCorePoint.getChild("Latitude", C2SIM_Util.core_NS);
                        Element lon = orderCorePoint.getChild("Longitude", C2SIM_Util.core_NS);
                        Element elevation = orderCorePoint.getChild("ElevationAGL", C2SIM_Util.core_NS);

                        // Create CBML Specific Point and write location information to it.
                        Element cbmlSpecificPoint = C2SIM_Util.createElementStack("LocationLight/PointLight/GDC/SpecificPoint", cbmlAtWhere, C2SIM_Util.cbml_NS);

                        // Add location informataion to cbmlSpecificPoint
                        cbmlSpecificPoint.addContent(new Element("Latitude", C2SIM_Util.cbml_NS).setText(lat.getText()));
                        cbmlSpecificPoint.addContent(new Element("Longitude", C2SIM_Util.cbml_NS).setText(lon.getText()));
                        if (elevation != null)
                            cbmlSpecificPoint.addContent(new Element("ElevationAMSL", C2SIM_Util.cbml_NS).setText(elevation.getText()));
                    }   // cbmlPoint

                    // Does this OrderCore document have a Line?
                    List<Element> orderCoreLineCoords = C2SIM_Util.findElementSimple("Where/AtWhere/Line/Coords", orderCoreTask, C2SIM_Util.core_NS);
                    if (orderCoreLineCoords.size() != 0) {

                        // Process OrderCore Line
                        Element cbmlLine = C2SIM_Util.createElementStack("LocationLight/Line", cbmlAtWhere, C2SIM_Util.cbml_NS);

                        for (Element orderCoreCoord : orderCoreLineCoords) {

                            // Create CBML SpecificPoint element
                            Element cbmlSpecificPoint = C2SIM_Util.createElementStack("GDC/SpecificPoint", cbmlLine, C2SIM_Util.cbml_NS);

                            // Get lat/lon information
                            Element lat = orderCoreCoord.getChild("Latitude", C2SIM_Util.core_NS);
                            Element lon = orderCoreCoord.getChild("Longitude", C2SIM_Util.core_NS);
                            Element el = orderCoreCoord.getChild("Elevation", C2SIM_Util.core_NS);

                            // Move to CBML Order
                            cbmlSpecificPoint.addContent(new Element("Latitude", C2SIM_Util.cbml_NS).setText(lat.getText()));
                            cbmlSpecificPoint.addContent(new Element("Longitude", C2SIM_Util.cbml_NS).setText(lon.getText()));
                            if (el != null)
                                cbmlSpecificPoint.addContent(new Element("ElevationAMSL", C2SIM_Util.cbml_NS).setText(el.getText()));
                        }   // Coordinates in line

                    }   // Line

                    // Does this OrderCore document have a Surface(Area) ?
                    List<Element> orderCoreAreaCoords = C2SIM_Util.findElementSimple("Where/AtWhere/Area/Coords", orderCoreTask, C2SIM_Util.core_NS);
                    if (orderCoreAreaCoords.size() != 0) {

                        // Process OrderCore Area
                        Element cbmlSurface = C2SIM_Util.createElementStack("LocationLight/Surfaac", cbmlAtWhere, C2SIM_Util.cbml_NS);

                        for (Element orderCoreCoord : orderCoreAreaCoords) {

                            // Create CBML SpecificPoint element
                            Element cbmlSpecificPoint = C2SIM_Util.createElementStack("GDC/SpecificPoint", cbmlSurface, C2SIM_Util.cbml_NS);

                            // Get lat/lon information
                            Element lat = orderCoreCoord.getChild("Latitude", C2SIM_Util.core_NS);
                            Element lon = orderCoreCoord.getChild("Longitude", C2SIM_Util.core_NS);
                            Element el = orderCoreCoord.getChild("Elevation", C2SIM_Util.core_NS);

                            // Move to CBML Order
                            cbmlSpecificPoint.addContent(new Element("Latitude", C2SIM_Util.cbml_NS).setText(lat.getText()));
                            cbmlSpecificPoint.addContent(new Element("Longitude", C2SIM_Util.cbml_NS).setText(lon.getText()));
                            if (el != null)
                                cbmlSpecificPoint.addContent(new Element("ElevationAMSL", C2SIM_Util.cbml_NS).setText(el.getText()));
                        }   // Coordinates in area
                    }   // CBML Area
                }   // AtWhere
                ++peIndex;
                ++taskNum;
            }   // Tasks

        }   // try
        catch (Exception e) {
            throw new C2SIMException("Exception in translateCoreLocationtoCBML " + e);
        }   // catch   // catch

    }   // translateLocationtoCBML    


    /************************/
    /* processCBML_GSR    */
    /************************/

    /**
    * processCBML_GSR
        /* Process message containing multiple CBML General Status Reports
        Translate each GSR to Unit_Core
        Store individual GSR (Unit_Core) with unit name in unitDB
        Translate Unit_Core document to CWIX_PSR
        Publish CWIX_PSR
     
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    static void processCBML_GSR(C2SIM_Transaction trans) throws C2SIMException {

        // Publish the original message
        C2SIM_Server_STOMP.publishMessage(trans);

        // Translate CBML09_GSR to Unit core
        Document d = trans.getDocument();
        Document unitCoreDoc;

        // Get root (BMLReport)
        Element e = d.getRootElement();

        // Get list of individual General Status Reports
        List<Element> c = e.getChildren("Report", C2SIM_Util.cbml_NS);

        // Iterate through the list of individual reports
        for (Element el : c) {

            Element e2 = el.clone();

            // Create Document using this Report as the root
            Document d2 = new Document(e2);

            // translate the report from the incoming IBML09_GSR into Unit_Core
            try {
                unitCoreDoc = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("CBML_GSR"), d2, "F");
            }   // try

            catch (C2SIMException ex) {
                throw new C2SIMException("Exception while translating CBML GSR", ex);
            }   // catch   // catch

            Element root = unitCoreDoc.getRootElement();

            // Convert dates from C2SIM to CBML/IBML
            C2SIM_IBML09.convertReportDate(unitCoreDoc, "IBML09", "C2SIM");

            // Update latest position
            C2SIM_C2SIM.updatePosition(unitCoreDoc);

            // If this is a PositionStatusReport convert it to a GeneralStatusReport for consistency            
            Element tor = root.getChild("TypeOfReport", C2SIM_Util.core_NS);
            tor.setText("GeneralStatusReport");

            // Add unit core document to transaction
            trans.setCoreDocument(unitCoreDoc);

            // Translate core to C2SIM and publish
            C2SIM_C2SIM.translateCoreToC2SIM_PR(trans);

            // Translate core to IBML09 and publish
            C2SIM_IBML09.translateCoreToIBML09_GSR(trans);

        }   // for

    }   // processCBML_GSR()


    /*******************************/
    /* translateCoreToCBML_GSR     */
    /*******************************/
    /**
    *( translateCoreToCBML_GSR - Translate core Position (or GSR) to CBML
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    static void translateCoreToCBML_GSR(C2SIM_Transaction trans) throws C2SIMException {
        
        // Are we translating to CBML?
        if (!translateToCBML)
            return;

        // Translate unitCoreDoc to CBML GSR
        Document d = trans.getCoreDocument().clone();

        Document cbml = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("CBML_GSR"), d, "R");

        // Translate C2SIM date format to IBML09 date format in the translated IBML09 document
        C2SIM_IBML09.convertReportDate(cbml, "C2SIM", "IBML09");

        // Imbed the single Report into a CBML Report document
        Element ir = new Element("CBMLReport", C2SIM_Util.cbml_NS);
        ir.addContent(cbml.detachRootElement());
        Document newD = new Document(ir);

        // Modify transaction object for publishing translated IBML09 GSR
        trans.setProtocol("BML");
        String xml = C2SIM_Util.xmlToStringD(newD, trans);
        trans.setMessageDef(C2SIM_Util.mdIndex.get("CBML_GSR"));
        trans.setMsTemp("CBML_PositionReport");
        trans.setXmlText(xml);
        trans.setSource("Translated");

        // Publish the CBML GSR 
        C2SIM_Server_STOMP.publishMessage(trans);

    }   // translateCoreToCBML_PR()


    /********************************/
    /* translateCoreToCBML_Order    */
    /********************************/
    /**
    * translateCoreToCBML_Order 
    @param t - C2SIM_Transaction - Translate core Order to CBML
    @throws C2SIMException 
    */
    static void translateCoreToCBML_Order(C2SIM_Transaction t) throws C2SIMException {
        Element root;
        String lat = "";
        String lon = "";
        String el = "";
        Element eLine = null;
        Element eWhereClass;
        Element eWhereCategory;
        Element eAtWhere;
        Element ePoint;
        Document cbmlOrder;
        
        // Are we translating to CBML?
        
        if (!translateToCBML)
            return;

        Document d = t.getCoreDocument().clone();

        // Get the root element of the order
        root = d.getRootElement();

        // Convert dates from C2SIM to IBML09/CBML in cloned document
        C2SIM_IBML09.convertOrderDates(d, "C2SIM", "IBML09");

        try {
            // Now translate from OrderCore to C2SIM Order
            cbmlOrder = C2SIM_Mapping.mapMessage(C2SIM_Util.mdIndex.get("CBML_Order"), d, "R");
        }


        catch (Exception e) {
            throw new C2SIMException("Error while mapping Order Core to CBML Order");
        }

        translateCoreLocationToCBMLOrder(t.getCoreDocument(), cbmlOrder);

        t.setProtocol("CBML");
        t.setSender("Server");
        t.setReceiver("ALL");
        String xml = C2SIM_Util.xmlToStringD(cbmlOrder, t);

        // Set parameters in the Transaction object for this message
        t.setMessageDef(C2SIM_Util.mdIndex.get("CBML_Order"));
        t.setXmlText(xml);
        t.setSource("Translated");

        // Publish the C2SIM Order
        C2SIM_Server_STOMP.publishMessage(t);

    }   // translatecoreToCBML_Order()


}   // C2SIM_CBML Class
