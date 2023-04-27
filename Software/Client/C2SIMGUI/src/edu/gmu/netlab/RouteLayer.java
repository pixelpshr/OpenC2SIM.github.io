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

import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import nl.tno.sims.ListEditableOMGraphics;

import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.Layer;
import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.event.MapMouseEvent;
import com.bbn.openmap.event.ProjectionEvent;
import com.bbn.openmap.event.SelectMouseMode;
import com.bbn.openmap.omGraphics.EditableOMCircle;
import com.bbn.openmap.omGraphics.EditableOMGraphic;
import com.bbn.openmap.omGraphics.EditableOMLine;
import com.bbn.openmap.omGraphics.EditableOMPoint;
import com.bbn.openmap.omGraphics.EditableOMPoly;
import com.bbn.openmap.omGraphics.EditableOMScalingRaster;
import com.bbn.openmap.omGraphics.GrabPoint;
import com.bbn.openmap.omGraphics.OMCircle;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMPoint;
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.omGraphics.OMScalingIcon;
import com.bbn.openmap.omGraphics.OMTextLabeler;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.tools.symbology.milStd2525.PNGSymbolImageMaker;
import com.bbn.openmap.tools.symbology.milStd2525.SymbolReferenceLibrary;
import static edu.gmu.netlab.MilOrg.bml;
import java.awt.BasicStroke;

import java.util.HashMap;

import edu.gmu.netlab.C2SIMGUI.IconType;

/**
 * The Location Layer implementation. 
 *
 * @version		C2SIMGUI (Initially BMLGUI)
 * @author 		Mohammad Ababneh, C4I Center, George Mason University
 * @since		4/9/2010
 */
public class RouteLayer extends Layer implements MapMouseListener {

    C2SIMGUI bml = C2SIMGUI.bml;
    private ListEditableOMGraphics omgraphics;// A list of graphics to be painted on the map.
    private Projection projection;            // The current projection.
    private OMGraphic selectedGraphic;        // The currently selected graphic.
    private GrabPoint selectedPoint;
    private boolean mouseButtonDown = false;    
    private float latOpen, lonOpen;
    private boolean isNonOp = false;
    float[] areaPoints = new float[16]; // array of points related to an area
    private String[] stringArray;       // Array of split order or report csv string
    String bmlDocumentType = "";
    float[] tempArray;                  // coordinates for graphics
    float[] destLat, destLon;           // coordinates of dest (last) point for Task
    int tempArrayIndex;
    int numberOfTasks = 0;
    String layerItemId = "";            // index of layer in layerItemMap
    String[] prevTaskUuid;
    String[] taskUuid;
    
    // variables to hold UnitID to be sent to SBMLClient in order to get unit info
    private String sbmlUnitID;          // Unit ID from BMLC2SIM document : Order or Report
    private String unitHostility = null;// Unit Hostility from SBML Client
    
    // position for a report in this RouteLayer
    // used to avoid posting repretitions of the same report
    String layerLastLatitude = "";
    String layerLastLongitude = "";
    String lastOmGraphicNameAdded = "";
    
    // role of icons in this layer
    C2SIMGUI.IconType iconType;
    String unitID = "";

    /**
     * Constructor (Default)
     */
    public RouteLayer(C2SIMGUI.IconType iconTypeRef) {
        iconType = iconTypeRef;
        omgraphics = new ListEditableOMGraphics();
    	//bml.mapBean.setScale(600000f);
        omgraphics.setProjection(projection);   
    }
    /**
     * Default Constructor with String 
     *
     */
    public RouteLayer(String pName, C2SIMGUI.IconType iconTypeRef) {
        iconType = iconTypeRef;
        omgraphics = new ListEditableOMGraphics();
    	//bml.mapBean.setScale(600000f);
        omgraphics.setProjection(projection);   
    }
    
    /**
     * Constructor for orders/reports case
     *
     * @param bmlStringArray contains parsed order or report 
     * @param bmlDocumentType value determines how is it processed
     */
    public RouteLayer(
        String[] bmlStringArray, 
        String bmlDocumentTypeRef,
        String xmlString,
        C2SIMGUI.IconType iconTypeRef,
        String unitIDRef,
        int indexRef) {
        iconType = iconTypeRef;
        unitID = unitIDRef;
        layerItemId = unitID;
        bmlDocumentType = bmlDocumentTypeRef;
        if(bml.debugMode)bml.printDebug("making new RouteLayer for order or report:");
        for(int i=0; i<bmlStringArray.length; ++i)
            if(bml.debugMode)bml.printDebug(bmlStringArray[i] + "|");
        if(bml.debugMode)bml.printDebug("docType:" + bmlDocumentType);
        if(bml.debugMode)bml.printDebug("XML:" + xmlString);
        if(bml.debugMode)bml.printDebug("layer index:" + indexRef);
        addToRouteLayer(bmlStringArray, bmlDocumentType, xmlString, indexRef);
    }
    
    /**
     * returns the IconType associated with this instance of RouteLayer
     */
    C2SIMGUI.IconType getIconType(){
        return iconType;
    }
    
    /**
     * @param bmlStringArray contains parsed order or report 
     * @param bmlDocumentType value determines how is it processed
     */
    void addToRouteLayer(
        String[] bmlStringArray, 
        String bmlDocumentTypeRef,
        String xmlString,
        int index) {
  
        bmlDocumentType = bmlDocumentTypeRef;
        if(bml.debugMode)bml.printDebug(
            "adding to RouteLayer for order or report with existing \n" +
            "index:" + index + " name:" +
            lastOmGraphicNameAdded + "| latitude:" + layerLastLatitude +
            "| longitude:" + layerLastLongitude + "|(if none this is new layer)");
    	stringArray = bmlStringArray;
    	if(bml.debugMode)bml.printDebug("--------------------------------------------------");
    	if(bml.debugMode)bml.printDebug(
            "RouteLayer Passed BML Document Type is : " + bmlDocumentType);
    	if(bml.debugMode)bml.printDebug("--------------------------------------------------");     
    	omgraphics = new ListEditableOMGraphics();
        omgraphics.setProjection(projection);        
        if (bmlDocumentType == "C2SIM Report"){
            if(createReportGraphicsC2SIM(
                omgraphics, bmlDocumentType, xmlString) == null)
                return;
        }
        else if (bmlDocumentType == "OldGeneralStatusReport" || 
            bmlDocumentType == "IBML09 Report" || 
            bmlDocumentType == "CBML Light Report" || 
            bmlDocumentType == "PositionStatusReport"){
            createGeneralStatusReportGraphics(
                omgraphics, bmlDocumentType, xmlString);
        }
        else {
            if (bmlDocumentType == "CBML Light Order"){
                createOrderGraphics(omgraphics);
    	}
        else if (bmlDocumentType == "C2SIM Order"){
            createOrderGraphicsC2SIM(omgraphics, xmlString);
    	} 
    	else if (bmlDocumentType == "IBML09 Order"){
            createOrderGraphicsIBML09(omgraphics);
    	}
        else if (bmlDocumentType == "BRIDGEREP" || bmlDocumentType == "MINOBREP" || 
        bmlDocumentType == "SPOTREP" || bmlDocumentType == "NATOSPOTREP" || 
        bmlDocumentType == "TRKREP")
            createReportGraphicsSIMCI(omgraphics,  bmlDocumentType);
        else
          bml.printError(
            "RouteLayer has no process for bmlDocumentType:" + bmlDocumentType);  
        }// end else/if bmlDocumentType
        
    } // end RouteLayer constructor
    
    /**
     * Draws initialization graphics for C2SIM on the omgraphics object
     * Clears and then fills the given OMGraphicList. 
     */
    public RouteLayer(
        String unitIDRef,
        String unitName,
        String hostility,
        String symbolID, 
        String latitude, 
        String longitude,
        C2SIMGUI.IconType iconTypeRef){
        iconType = iconTypeRef;
        unitID = unitIDRef;
        layerItemId = unitIDRef;

        // make a new graphic object to hold the unit icon
        omgraphics = new ListEditableOMGraphics();
        omgraphics.setProjection(projection);
        
        // convert to drawable values
        if(latitude.length() == 0 || longitude.length() == 0)return;
        latOpen = stringToFloat(latitude);
        lonOpen = stringToFloat(longitude);

        // Set map center to location
        // Auto Zoom
        // Set map center to first point of location and Auto Zoom
        scaleAndCenterIfEmpty(latOpen, lonOpen, 5000000f);

        // Draw 2525b Symbol
        if(bml.debugMode)bml.printDebug(
            "Drawing C2SIM init 2525b for unitID:" + unitID + " unitName:" +
            unitName + " hostility:" + hostility + " coords:" + latitude + "/" + longitude);
        String icon = icon2525b(unitID, hostility);
        addIcon("     " + unitName, icon);
    }
    
    /**
     * returns the unitID associated with icon for this layer
     */
    String getUnitID(){
        return unitID;
    }
    
    // recent the map at these coordinates
    // if the map is empty
    void scaleAndCenterIfEmpty(float lat, float lon, float scale){
        if(bml.anIconIsOnTheMap)return;
        bml.anIconIsOnTheMap = true;
        bml.mapBean.setCenter(lat,lon);
    }
    void scaleAndCenterIfEmpty(float lat, float lon) {
        if(bml.anIconIsOnTheMap)return;
        bml.anIconIsOnTheMap = true;
        bml.mapBean.setCenter(lat,lon);
    }
    
    /**
     * returns float from String
     */
    float stringToFloat(String arg) {
        return (float) Double.parseDouble(arg);
    }
    
    /**
     * returns the lastLatitude associated with this RouteLAyer
     */
    String getLatitude() {
        return layerLastLatitude;
    }
    
    /**
     * returns the lastLongitude associated with this RouteLAyer
     */
    String getLongitude() {
        return layerLastLongitude;
    }
    
    /**
     * add (lat,lon) to track array by extending its length
     * note that the array alternates lat and lon, then
     * converts to float[] for use with createPolyLine
     */
    HashMap<String,ArrayList<Float>> tracksMap = new HashMap<String,ArrayList<Float>>();
    float[] addTrackPoint(String unitID, float latParm, float lonParm){
        ArrayList<Float> trackPoints;
        Float lat = new Float(latParm);
        Float lon = new Float(lonParm);
        
        // retrieve the trackPoints for this unit
        // if none, make one
        trackPoints = tracksMap.get(unitID);
        if(trackPoints == null){
            trackPoints = new ArrayList<Float>();
            tracksMap.put(unitID, trackPoints);
        }
       
        // add new values for this point
        trackPoints.add(lat);
        trackPoints.add(lon);

        // make a float[] to return and copy data to it
        float[] returnArray = new float[trackPoints.size()];
        Iterator iter = trackPoints.iterator();
        int index = 0;
        while(iter.hasNext())
            returnArray[index++] = (Float)iter.next();
        return returnArray;
        
    }// end addTrackPoint
     
    /**
     * Draws report graphics for all report types. 
     * Clears and then fills the given OMGraphicList. 
     */
    public ListEditableOMGraphics createReportGraphicsSIMCI(
        ListEditableOMGraphics graphics, 
        String simciReportType) {
        graphics.clear();
        String[][] gdcArray = new String [100][3];
        int [] descArray = new int[23]; // Array of a max of 23 locations
        boolean locationFound =false;
        int pointCount = 0;	// number of points in all locations
        int locationCount = 0; // number of location objects
        int locationPointCount = 0; // number of points in a location
        for (int i=0; i < stringArray.length; i++){
            if (stringArray[i].trim().equals("GDC")){ 	// Look for location Information
                if(bml.debugMode)bml.printDebug(
                    "A Location has been discovered #: " + 
                    pointCount + "  It started at : " + i);
                locationFound = true;

                while (locationFound){
                    pointCount++;
                    locationPointCount++;
                    gdcArray[pointCount][0]= stringArray[i+1].toString();
                    gdcArray[pointCount][1]= stringArray[i+2].toString();
                    gdcArray[pointCount][2]= stringArray[i+3].toString();
                    if (stringArray[i+4].toString().trim().equals("GDC")){
                        if(bml.debugMode)bml.printDebug("One more GDC");
                    }
                    else {
                        if(bml.debugMode)bml.printDebug("No  more GDC");
                        locationCount++;
                        descArray[locationCount] = locationPointCount; 
                        locationPointCount = 0;
                    }
                    if (stringArray[i].trim() != "GDC"){
                            locationFound = false;
                    }
                }// end while
            } // end if
        } // end for int i
		
        for (int h = 1 ; h <= locationCount; h++ ){
            // creating a temp array for each graphical location
            // descArray[h]][2] is number of points in a location
            int tempArrayLength = ((descArray [h]) * 2);
            
            // temp array with rows equal to number of locations and Lat Lon Values
            tempArray = new float[tempArrayLength]; 
            tempArrayIndex = 0;
            for (int pointIndex = 1; pointIndex <= descArray[h] ; pointIndex++ ){
                    tempArray[tempArrayIndex]= stringToFloat(gdcArray[pointIndex][0]);	
                    tempArray[tempArrayIndex+1]= stringToFloat(gdcArray[pointIndex][1]);
                    tempArrayIndex+=2;	
            } //gdc array for end

            // Read temp Array
            if(bml.debugMode)bml.printDebug("=====Printing temp Array =============");
            if(bml.debugMode)bml.printDebug("temp  Array length =  " + tempArray.length);
            for (int tempIndex =0; tempIndex < tempArray.length ; tempIndex++){
                if(bml.debugMode)bml.printDebug(
                    "temp  Array [ " + tempIndex + " ] = " + tempArray[tempIndex]);
            }

            // Set map center to first point of location
            //Auto Zoom
            latOpen = tempArray[0];
            lonOpen = tempArray[1];
            // Set map center to first point of location and Auto Zoom
            scaleAndCenterIfEmpty(latOpen, lonOpen, 48000f);

            // Draw 2525b Symbol          S * G * EV EB -- ** ** *
            String reportLabel = "UNKNOWN---*****";
            if (simciReportType == "BRIDGEREP"){
                    reportLabel = "SFGPEVEB--*****";
            }	
            else if (simciReportType == "MINOBREP"){
                    reportLabel = "SFGPEXM---*****";									
            }
            else if (simciReportType == "SPOTREP" || simciReportType == "TRKREP"){
                    reportLabel = "SUGPU-----*****";
            }
            graphics.add("SIMCIReport "+h, createPoint(reportLabel));				
            graphics.add("SIMCIReport "+h, createPoly(tempArray, 0, 1));	
        } // desc array for end
        
        return graphics;    
        
    } // end createReportGraphicsSIMCI()
    
    /**
     * Draws report graphics for C2SIM report types. 
     * Clears and then fills the given OMGraphicList. 
     */
    public ListEditableOMGraphics createReportGraphicsC2SIM(
        ListEditableOMGraphics graphics, 
        String simciReportType,
        String reportText) {
        graphics.clear();

        if(bml.debugMode)bml.printDebug("making new C2SIM report graphics");
        String lat = "", lon = "", hostility = "", unitID = "", unitName="", reporterID="";
        String isoDateTime = "", reportID = "", observationName = "";
        boolean isPositionReport = false, isObservationReport = false;
        isNonOp = false;

        // find the lat/lon reported
        for(int i=0; i < stringArray.length-1; ++i) {
           
            if(stringArray[i].equals("PositionReportContent"))
                isPositionReport = true;
            if(stringArray[i].equals("ObservationReportContent"))
                isObservationReport = true;
            if(stringArray[i].equals("OperationalStatusCode"))
                isNonOp = stringArray[i+1].equals("NotOperational");
            if(stringArray[i].equals("Latitude"))
                lat = stringArray[i+1];
            if(stringArray[i].equals("Longitude"))
                lon = stringArray[i+1];
            if(stringArray[i].contains("Hostility"))
                hostility = stringArray[i+1];
            if(stringArray[i].equals("UUID"))
                reportID = stringArray[i+1];
            if(stringArray[i].equals(bml.c2simReporteeWhoTag))// SubjectEntity
                unitID = stringArray[i+1];
            if(stringArray[i].equals("Name"))
                observationName = stringArray[i+1];
            if(isPositionReport)
                if(stringArray[i].equals(bml.c2simReporterWhoTag))
                    reporterID = stringArray[i+1];   
            if(isObservationReport)
                if(stringArray[i].equals(bml.c2simObserveeTag))
                    reporterID = stringArray[i+1];
            if(stringArray[i].equals("IsoDateTime"))
                isoDateTime = stringArray[i+1];
        }

        // confirm report has a UUID 
        if(isObservationReport){
            unitID = reporterID;
            unitName = observationName;
        }
        else if(unitID.equals("")){
            bml.printError("ignoring received report with no " +
                bml.c2simReporteeWhoTag);
            return null;
        } else if(bml.autoDisplayReports.equalsIgnoreCase("ALL")){ 
            
            // ignore 00000000-0000-0000-0000-000000000000
            // which is generated in server translation
            if(unitID.equals("00000000-0000-0000-0000-000000000000")){
                bml.printError("ignoring received report with unitID:" + unitID);
                return null;
            }
        }
        
        // check for report out of time sequence  
        if(bml.getReportTime(unitID) == null)
            bml.putReportTime(unitID, "0000-00-00T00:00:00Z");
        String previousIsoDateTime = bml.getReportTime(unitID);
        if(bml.warnOnReportSeq.equals("1"))
        if(isoDateTime.compareTo(previousIsoDateTime) < 0) {
            bml.printError("report to be displayed next contains time:" + isoDateTime +
                " which is earlier than previous report time:" + previousIsoDateTime +
                " from the same source - this could be due cyber attack");
            JOptionPane.showMessageDialog(
                bml, 
                "report to be displayed next contains time:" + isoDateTime +
                "\nwhich is earlier than previous report time:" + previousIsoDateTime +
                "\nfrom the same source - this could be due cyber attack",
                "Report Time Sequence Warning",	
                JOptionPane.WARNING_MESSAGE);
        }
        bml.putReportTime(unitID,isoDateTime);
    
        // convert the unitID UUID to unit name
        if(isPositionReport && unitID.length() > 0) {
            unitName = bml.getMilOrgName(unitID);
            if(unitName == null) {
                if(bml.debugMode)bml.printDebug(
                    "don't have unit Name for C2SIM UUID:" +unitID);
                unitName = unitID;
            }
        }
        else unitName = "UNKNOWN";
         
        // start track with initial position
        MilOrg milOrg = bml.getUnit(unitID);
        if(milOrg != null)
            if(tracksMap.get(unitID) == null){
                addTrackPoint(
                    unitID,
                    stringToFloat(milOrg.getLatitude()),
                    stringToFloat(milOrg.getLongitude()));
            }
        else if(lat.equals("") || lon.equals(""))return null;
        
        // lookup hostility in MilOrg if not in the report
        if(hostility.length() == 0){
            hostility = bml.getMilOrgHostility(unitID);
            if(hostility == null)hostility = "UNK";
        }

        // filter reports to be displayed, per configuration
        if(hostility.equals("HO")){// red position; red observation of blue
            if(isPositionReport && !bml.getDisplayRedPosition())return null;
            if(isObservationReport && !bml.getDisplayBlueObservation())return null;
        } else if(hostility.equals("FR")){// blue position; blue observation of red
            if(isPositionReport && !bml.getDisplayBluePosition())return null;
            if(isObservationReport && !bml.getDisplayRedObservation())return null;
        } else {// all other hostility
            if(isPositionReport && !bml.getDisplayOtherPosition())return null;
            if(isObservationReport && !bml.getDisplayOtherObservation())return null;
        }
 
        // confirm all necessary data found
        if( lat.length() == 0 ||
            lon.length() == 0 ||
            hostility.length() == 0 ||
            (unitID.length() == 0 && isPositionReport)) {
            if(bml.debugMode)bml.printDebug("insufficent data to draw report graphics");
            return graphics;
        }

        // convert to drawable values
        latOpen = stringToFloat(lat);
        lonOpen = stringToFloat(lon);

        // Set map center to location
        scaleAndCenterIfEmpty(latOpen, lonOpen, 5000000f);

        // Draw 2525b Symbol  
        String labelName = "     " + unitName;
        String isObs = "   ";
        if(isObservationReport){
            isObs="OBS";
            labelName = "     " + observationName;
        }
       
        // deal with unimposed hostilityCode
        if(hostility.equals("UNK")){
            milOrg = bml.getUnit(unitID);
            if(milOrg != null){
                milOrg.hostilityCode = hostility;
                milOrg.symbolIdentifier = milOrg.symbolIdentifier.substring(0,1) +
                    "U" + milOrg.symbolIdentifier.substring(2);
            }
            else { // could be findable by name instead - update that version
                milOrg = bml.getUnitByName(unitName);
                if(milOrg != null){
                    milOrg.hostilityCode = hostility;
                    milOrg.symbolIdentifier = milOrg.symbolIdentifier.substring(0,1) +
                        "U" + milOrg.symbolIdentifier.substring(2);
                    unitID = milOrg.uuid;
                    
                    // now go back and update the unitIDmap version
                    MilOrg milOrg2 = bml.getUnit(unitID);
                    milOrg2.hostilityCode = hostility;
                    milOrg2.symbolIdentifier = milOrg.symbolIdentifier.substring(0,1) +
                        "U" + milOrg.symbolIdentifier.substring(2);
                }
            }// end else/if(milOrg != null)
        }// end if(hostility.equals("UNK"))
        
        if(bml.debugMode)
            bml.printDebug("Drawing C2SIM 2525b for name:" + labelName + 
            " hostility:" + hostility +" lat:" + lat + " lon:" + lon +
            " lastName:" + lastOmGraphicNameAdded + " isObservation:" + 
            isObservationReport + " isNonOp:" + isNonOp);
        String icon;
        if(isObservationReport)
            icon = icon2525b(observationName, hostility);
        else
            icon = icon2525b(unitID, hostility);
        
        // if this repeats previous icon, delete previous before drawing
        if(lastOmGraphicNameAdded.equals(labelName)) {
            if(bml.debugMode)bml.printDebug("removing graphic:" + labelName + "|");
            this.removeGraphic(labelName);
            bml.listRemoveIconUuid(unitID);        
            bml.listAddIconUuid(unitID, lat, lon, IconType.REPORT);
            
           
            // if showing tracks, post the track as
            // a sequence of points connected by lines
            if(bml.showTracks && !isObservationReport) {
                
                // redraw the track point and lines
                this.add("TRACK"+labelName,
                    createPolyLine(addTrackPoint(unitID, latOpen, lonOpen),
                        OMGraphic.DECIMAL_DEGREES, 
                        OMGraphic.DECLUTTERTYPE_SPACE));
            } 
        }
        
        // add the point; then visit scale and repaint so it shows
        addIcon(labelName, icon);
        visitScale();
        repaint();
                        
        // if the unit is inoperative add red circle-slash overlay
        if(isNonOp){     
            BasicStroke bs = new BasicStroke(3);
            
            // make circle
            OMCircle nonOpCircle = new OMCircle(latOpen, lonOpen, 30, 30);
            nonOpCircle.setLinePaint(Color.RED);
            EditableOMCircle eNonOpCircle = new EditableOMCircle(nonOpCircle);
            nonOpCircle.setStroke(bs);
            eNonOpCircle.init();
            this.add(unitName + "-nop", eNonOpCircle);
        }
        
        // save a copy of report in map for later fetch
        bml.reportAddText(reportText, latOpen, lonOpen);
        
        // save out values to compare next time
        layerLastLatitude = lat;
        layerLastLongitude = lon;
        lastOmGraphicNameAdded = labelName;
        scaleAndCenterIfEmpty(stringToFloat(lat), stringToFloat(lon));
        bml.loadReportButton.setEnabled(true);
       
        return graphics;    
        
    } // end createReportGraphicsC2SIM
    
    /**
     * Draw order graphics. Clears and then fills the given OMGraphicList. 
     * 
     * Start parsing the Array representing the XML document
     * First: create a description array
     * Second: use the description array to get each graphics type
     *   starting C2SIMv1.0.1 this can include Routes before Tasks
     * Third: call the appropriate drawing method (Point, Line, Area)
     * If it is an area then the last and first points should be the same
     *
     * @param graphics The OMGraphicList to clear and populate
     * @return the graphics list, after being cleared and filled
     */
    public ListEditableOMGraphics createOrderGraphicsC2SIM(
        ListEditableOMGraphics graphics, String xmlString) {
        boolean foundUnitID = false;// to accept only first UnitID for a Task
        boolean foundLat = false;   // to determine if the location includes coordinates 
        boolean foundLon = false;
        int taskIndex = 0;          // number of locations
        int startGDC = 0;           // start index in the array of the location
        int endGDC = 0;             // end index in the array of the location
        int lengthGDC = 0;          // length of the location 
        int noOfPointGDC = 0;       // number of points in the location. 
                                    // If it is one then it is a point
                                    // if it is more than one then it is a line or an area
                                    // the line differs from the area in that the area has 
                                    // the same last and first points
        String latitudeTag = bml.c2simLatitudeTag;
        String longitudeTag = bml.c2simLongitudeTag;
        float[][] holdTempArrays;   // holds copies of tempArray to graph later
        int[] holdTempArraySizes;   // keep sizes of the tempArrays held
        int tasksStartInStringArray = -1;
        String[] taskMapGraphicID;

        // count the Tasks in the stringArray
        // use to create storage for building graphics
        String c2simTaskTagWithPrefix = bml.c2simns + bml.c2simTaskTag;
        String asxTaskTagWithPrefix = bml.c2simns + bml.c2simAsxTag;
        for (int i = 0; i < stringArray.length; i++){
            String testString = stringArray[i];
            if(testString.equals(bml.c2simTaskTag) ||
               testString.equals(c2simTaskTagWithPrefix) ||
               testString.equals(bml.c2simAsxTag) ||
               testString.equals(asxTaskTagWithPrefix)) {
                ++numberOfTasks;
               if(tasksStartInStringArray < 0)
                   tasksStartInStringArray = i;
            }
        }
        if(numberOfTasks == 0){
            bml.printError(
                "C2SIMGUI graphics processing found no C2SIM Tasks to process");
            return null;
        }
        if(bml.debugMode)bml.printDebug(
            "in createOrderGraphicsC2SIM numberOfTasks=" + numberOfTasks);
        destLat = new float[numberOfTasks];
        destLon = new float[numberOfTasks];
        holdTempArrays = new float[numberOfTasks][];
        holdTempArraySizes = new int[numberOfTasks];
        
        // count the locations in this Task

        // number , startGDC, endGDC, lengthGDC, noOfPointGDC, 
        // task Name - control measures
        int [][] descArray = new int [numberOfTasks][5];
    
        // arrays to save TaskID so as to display unit icon
        // after ControlMeasures have been ingested
        String[] taskUnitID = new String[numberOfTasks];
        taskUnitID[0] = "";
        String[] taskNames = new String[numberOfTasks];
        taskUuid = new String[numberOfTasks];
        taskUuid[0] = "";
        taskMapGraphicID = new String[numberOfTasks];
        taskMapGraphicID[0] = "";
        prevTaskUuid = new String[numberOfTasks];
        prevTaskUuid[0] = "";
        float[] taskUnitIDLat = new float[numberOfTasks];
        float[] taskUnitIDLon = new float[numberOfTasks];
        boolean[] isControlMeasure = new boolean[numberOfTasks];
        isControlMeasure[0] = false;
        
        //The parser used for Task does not support parsing
        // Route without major changes; so to implement
        // C2SIM v1.0.1 we use the newer parsing scheme from 
        // MilOrg class
        //
        // Scan the raw XML for <Entity>...<Route>...<UUID>...
        // <GeodeticCoordinate> values before parsing Tasks
        String parseOrder = xmlString;
        
        // extract all of next Entity 
        int entityIndex = bml.c2simTagIndexOf(parseOrder, "<Entity>");
        while(entityIndex > 0) {         
            int endEntityIndex = bml.c2simTagIndexOf(parseOrder, "</Entity>");            
            if(endEntityIndex < 0) {
                bml.printError("missing Entity endTag in C2SIM Order");
                return null;
            }
            String parseEntity = parseOrder.substring(entityIndex, 
                endEntityIndex);
            
            // find Route UUID and make a PhysicalRoute to hold its data
            String routeUuid = 
                bml.extractC2simData(parseEntity, "UUID", true);
            if(routeUuid == "")return null;
            PhysicalRoute physicalRoute = new PhysicalRoute(routeUuid); 
       
            // extract PhysicalState containing the Locations
            int physStateIndex = bml.c2simTagIndexOf(parseEntity, "<PhysicalState>");
            int endPhysStateIndex = 
                bml.c2simTagIndexOf(parseEntity, "</PhysicalState>", physStateIndex);
            String physicalState = 
                parseEntity.substring(physStateIndex,endPhysStateIndex);
          
            // find the Locations latitude/longitude/elevation in PhysicalState
            int endGeodeticCoord = 
                bml.c2simTagIndexOf(physicalState,"</GeodeticCoordinate>");
            while(endGeodeticCoord > 0) {
                
                // extract lat/lon/elev
                String latitude = 
                    bml.extractC2simData(physicalState, "Latitude", true);
                String longitude = 
                    bml.extractC2simData(physicalState, "Longitude", true);
                String elevation = 
                    bml.extractC2simData(physicalState, "ElevationMSL", false);
                if(latitude == "" || longitude == "") {
                    bml.printError("missing latitude or longitude in Route:" +
                        routeUuid);
                }
                
                // package the coordinate as a PhysicalLocation 
                // and insert in PhysicalRoute
                PhysicalLocation thisLocation = 
                    new PhysicalLocation(latitude, longitude, elevation);
                physicalRoute.locations.add(thisLocation);
                
                // remove this GeospatialCoordinate from the Entity
                // and go back to parse the next one
                physicalState = physicalState.substring(endGeodeticCoord + 1);
                endGeodeticCoord = 
                    bml.c2simTagIndexOf(physicalState,"</GeodeticCoordinate>");
            }// end while(endGeospatialCoord > 0)
            
            // insert the complete Route in allPhysicalRoutes - allow update
            bml.insertRoute(physicalRoute, true);
            if(bml.debugMode) {
                int routeCount = bml.allPhysicalRoutes.size();
                bml.printDebug("Parsed " + routeCount + " Routes:");
                Iterator it = bml.allPhysicalRoutes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    PhysicalRoute nextRoute = (PhysicalRoute)pair.getValue();
                    bml.printDebug("Route UUID:" + nextRoute.routeUuid);
                    for(int i = 0; i<nextRoute.locations.size();++i)
                        bml.printDebug("  " + nextRoute.getLocation(i));
                }
            }
            
            // remove this Entity from parseOrder and go back to parse again
            parseOrder = parseOrder.substring(endEntityIndex+1);
            entityIndex = bml.c2simTagIndexOf(parseOrder, "<Entity>");
        }// end while(entityIndex > 0)
	
        // Parse order string Tasks and draw location information 
        if(bml.debugMode)
            bml.printDebug("============   Drawing C2SIM Order  ==================");
        PhysicalRoute physicalRoute = null;
        for (int stringArrayIndex = 1; 
                stringArrayIndex < stringArray.length; 
                stringArrayIndex++){

            // Look for location Information in a Task
            if (bml.c2simTagCompare(stringArray[stringArrayIndex],latitudeTag)){
                taskUnitIDLat[taskIndex] = 
                        stringToFloat(stringArray[stringArrayIndex+1]);
                if(!foundLat) {// first geo point in Task
                
                    // this is left for future LOX Control Measure
                    if(isControlMeasure[taskIndex])
                        if(bml.debugMode)bml.printDebug(
                            "A ControlMeasure Location has been discovered;" + 
                            " coordinates start at : " + stringArrayIndex);
                    else
                        if(bml.debugMode)bml.printDebug(
                            "A Task Location has been discovered;:" + 
                            " coordinates start at : " + stringArrayIndex);

                    // initialize for the Task
                    startGDC = stringArrayIndex;
                    foundLat = true;
                }
            }
         
            // look for the unitID
            if (bml.c2simTagCompare(stringArray[stringArrayIndex],bml.c2simTaskeeTag) &&
                !foundUnitID) {
                taskUnitID[taskIndex] = stringArray[stringArrayIndex+1];
                foundUnitID = true;
            }
            
            // look for the Task UUID (there could be more than one UUID in Task; 
            // this is the last)
            if (bml.c2simTagCompare(stringArray[stringArrayIndex],"UUID"))
                taskUuid[taskIndex] = stringArray[stringArrayIndex+1];
            
            // look for the MapGraphicID that indicates a separately provided Route
            if(stringArrayIndex > tasksStartInStringArray){
                if(bml.c2simTagCompare(stringArray[stringArrayIndex],"MapGraphicID"))
                    taskMapGraphicID[taskIndex] = stringArray[stringArrayIndex +1];
            }
           
            // look for the Task Name (there is more than one Name in Task; this is the last)
            if (bml.c2simTagCompare(stringArray[stringArrayIndex],"Name")) {
                taskNames[taskIndex] = stringArray[stringArrayIndex+1];
            }
            
            // look for predecessor Task = this one will start at its last coords
            if(bml.c2simTagCompare(stringArray[stringArrayIndex],"TemporalAssociationWithAction"))
                prevTaskUuid[taskIndex] = stringArray[stringArrayIndex+1];

            // effective with C2SIMv11 there is either a separate Route
            // or a sequence of locations to be followed
            
            // look for each pair of lat/lon - assume they are in pairs
            if (bml.c2simTagCompare(stringArray[stringArrayIndex],longitudeTag)){
                taskUnitIDLon[taskIndex] = 
                    stringToFloat(stringArray[stringArrayIndex+1]);
                endGDC = stringArrayIndex+1;
                foundLon = true;
            }

            // look for end of Task
            if ( ((stringArrayIndex == stringArray.length-1) ||
                (stringArrayIndex > 0 &&
                    bml.c2simTagCompare(
                        stringArray[stringArrayIndex],bml.c2simTaskTag)))){

                // did we scan at least one coordinate pair
                int mapGraphicCount = 0;
                if(taskMapGraphicID[taskIndex] != "") {
                    physicalRoute = 
                        bml.retrieveRoute(taskMapGraphicID[taskIndex]);
                    if(physicalRoute != null)
                        mapGraphicCount = physicalRoute.getSize();
                    else {
                        bml.printError("C2SIM Order Task mapGraphicID does not match a route:"+
                            taskMapGraphicID[taskIndex]);
                        return null;
                    }
                }
                if(startGDC < endGDC && foundLat && foundLon ||
                    mapGraphicCount > 0) {

                    // there are coordinates to be captured
                    lengthGDC = (endGDC - startGDC + 1);
                    if(mapGraphicCount > 0)noOfPointGDC = mapGraphicCount + 1;
                    else noOfPointGDC = lengthGDC/4;
                    if(bml.debugMode)bml.printDebug(
                        "Task " + taskIndex + " coordinates start at:" + startGDC +
                        "  end at : " + endGDC + " its GDC Length is: " + 
                        lengthGDC + " No of Points is : " + noOfPointGDC + 
                        " it has MapGraphicID:" + taskMapGraphicID[taskIndex]);

                    // verify there was some data
                    if(!foundUnitID){
                        bml.printError("Task at " + stringArrayIndex +
                            "has no unit UUID");
                        continue;
                    }
                    if(lengthGDC == 0) {

                        // no coordinates to capture
                        startGDC = 0;
                        endGDC = 0;
                        lengthGDC = 0;
                        noOfPointGDC = 0;
                        bml.printError("Task at " + stringArrayIndex +
                              " for UUID:" + taskUnitID[taskIndex] + 
                              " has no coordinates");
                        continue;// for stringArrayIndex  
                    }// end else

                    // capturing parameters in array for drawing
                    descArray[taskIndex][0]=taskIndex;
                    descArray[taskIndex][1]=startGDC;
                    descArray[taskIndex][2]=endGDC;
                    descArray[taskIndex][3]=lengthGDC;
                    descArray[taskIndex][4]=noOfPointGDC;

                    // initialize for next location
                    startGDC = 0;
                    foundUnitID = false;
                    foundLat = false;
                    foundLon = false;
                    taskIndex++;
                    if(taskIndex < numberOfTasks){
                        prevTaskUuid[taskIndex] = "";
                        taskUuid[taskIndex] = "";
                        taskMapGraphicID[taskIndex] = "";
                        taskUnitID[taskIndex] = "";
                    }
                }//end if(startGDC
            }// end if(((stringArrayIndex
        }// end for stringArrayIndex  
        if(taskIndex == 0)return null;
        
        // creating a temp array for each graphical location
        // the length of the array will be twice as the number of lat,lon points	
        // Area: the last lat,lon should be the same lat,lon
 
        // generating graphics for each location previously captured
        for (taskIndex=0; taskIndex < numberOfTasks; taskIndex++){
            int tempArraySize = (descArray[taskIndex][4])*2;// noOfPointGDC
            if(bml.debugMode)bml.printDebug(" tempArraySize = " + tempArraySize  );
            int latLocation = 0, lonLocation = 0;
            
            // considering whether to use a pre-stored route
            String thisTaskMapGraphicID = taskMapGraphicID[taskIndex];
            int mapGraphicCount = 0;
            if(!taskMapGraphicID[taskIndex].equals("")) {
                physicalRoute = 
                    bml.retrieveRoute(taskMapGraphicID[taskIndex]);
                if(physicalRoute != null)
                    mapGraphicCount = physicalRoute.getSize();
                else {
                    bml.printError("C2SIM Order Task mapGraphicID does not match a route:"+
                        taskMapGraphicID[taskIndex]);
                    return null;
                }
            }
            int pointsInRoute = descArray[taskIndex][4];
            if(thisTaskMapGraphicID != "")pointsInRoute = physicalRoute.getSize();
            tempArraySize = (pointsInRoute + 1) * 2;
            
            // get class instance of Unit initial values
            MilOrg milOrg = bml.getUnit(taskUnitID[taskIndex]);
          
            // if there was a predecessor task, take first point
            // from destination, or endpoint of its route
            int prevTaskIndex = -1;
            if(!prevTaskUuid[taskIndex].equals("")){
                
                // get the task number for the predecessor
                for(int i = 0; i < numberOfTasks; ++i){
                    if(prevTaskUuid[taskIndex].equals(taskUuid[i])){
                        prevTaskIndex = i;
                        break;
                    }
                }
            }// start with points from predecessor task
            
            if(prevTaskIndex >= 0){
                // add starting point (two array locations)
                // if the predecessor task has no MapGraphic route
                // use last point in the task
                tempArraySize += 2;
                tempArray = new float[tempArraySize];
                tempArrayIndex = 2;
                endGDC = descArray[taskIndex][2];
                if(taskMapGraphicID[taskIndex].equals("")){
                    tempArray[0] = stringToFloat(stringArray[endGDC-2]);
                    tempArray[1] = stringToFloat(stringArray[endGDC]);
                } else {
                    PhysicalRoute physRoute = 
                        bml.retrieveRoute(taskMapGraphicID[taskIndex]);
                    if(physRoute == null){ // route not found
                        tempArray[0] = stringToFloat(stringArray[endGDC-2]);
                        tempArray[1] = stringToFloat(stringArray[endGDC]);
                    } else {// task last point from the route
                        int lastPoint = physRoute.getSize()-1;
                        tempArray[0] = stringToFloat(physRoute.getLatitude(lastPoint));
                        tempArray[1] = stringToFloat(physRoute.getLongitude(lastPoint));
                    }
                }
            } else {
            // to add other shapes here see comparable IBML09 code
                if(milOrg == null){
                    tempArray = new float[tempArraySize];
                    tempArrayIndex = 0;
                }else{
                    tempArraySize += 2;
                    tempArray = new float[tempArraySize];
                    tempArrayIndex = 2;
                }
            }
            
            // start tempArray with current position of the Unit
            if(milOrg != null) {
                String lat = milOrg.getLatitude();
                String lon = milOrg.getLongitude();
                if(lat.equals("") || lon.equals(""))
                    tempArrayIndex = 0;
                else {
                    tempArray[0] = stringToFloat(lat);
                    tempArray[1] = stringToFloat(lon);
                    tempArrayIndex = 2;
                }
            }
             
            // creating a temp array for each graphical location
            float iconLat = 0f, iconLon = 0f;
            for (int h = 0; h < pointsInRoute; h++) {	
                
                // pull lat and lon from task array or prestored route
                if(thisTaskMapGraphicID == "") {
                    latLocation = (descArray[taskIndex][1])+(1+(4*h));
                    lonLocation = (descArray[taskIndex][1])+(3+(4*h));
                    tempArray[tempArrayIndex] = 
                        stringToFloat(stringArray[latLocation].trim());               
                    tempArray[tempArrayIndex+1]= 
                        stringToFloat(stringArray[lonLocation].trim());
                }// end if(mapGraphicID...
                else {// prestored route
                    tempArray[tempArrayIndex] = stringToFloat(
                        physicalRoute.getLatitude(h));
                    tempArray[tempArrayIndex+1] = stringToFloat(
                        physicalRoute.getLongitude(h));
                    if(bml.debugMode) {
                        bml.printDebug("Task " + taskIndex + " uses MapGraphicID:" +
                            thisTaskMapGraphicID + " with Locations:");
                        PhysicalRoute debugRoute = bml.retrieveRoute(thisTaskMapGraphicID);
                        for(int i = 0; i<debugRoute.locations.size(); ++i)
                            bml.printDebug(debugRoute.getLocation(i));
                    }
                }
                if(bml.debugMode)bml.printDebug(" tempArray["+
                        tempArrayIndex+"] " + 
                        tempArray[tempArrayIndex] );
                if(bml.debugMode)bml.printDebug(" tempArray["+
                        (tempArrayIndex+1)+"] " + 
                        tempArray[tempArrayIndex+1] );
                
                // save the last two values in tempArray for centering
                iconLat = tempArray[tempArrayIndex];
                iconLon = tempArray[tempArrayIndex+1];
                tempArrayIndex += 2;
        
            } // end for h

            // Set map center to first point of location and Auto Zoom
            scaleAndCenterIfEmpty(iconLat, iconLon, 600000f);
            
            // save initial position data to add unit icon later
            if(milOrg == null || tempArrayIndex == 2){
                taskUnitIDLat[taskIndex] = tempArray[0];
                taskUnitIDLon[taskIndex] = tempArray[1];
            }
            else {
                String lat = milOrg.getLatitude();
                String lon = milOrg.getLongitude();
                if(lat.equals("") || lon.equals("")){
                    lat = "0";
                    lon = "0";
                }
                taskUnitIDLat[taskIndex] = stringToFloat(lat);
                taskUnitIDLon[taskIndex] = stringToFloat(lon);
            }
            
            // save endpoint lat/lon for use as starting point of other Task
            destLat[taskIndex] = iconLat;
            destLon[taskIndex] = iconLon;
            
            // keep the tempArray for later use when
            // all tasks will be ready to draw
            float[] holdArray = new float[tempArray.length];
            System.arraycopy(tempArray, 0, holdArray, 0, tempArray.length);
            holdTempArrays[taskIndex] = holdArray;
            holdTempArraySizes[taskIndex] = tempArrayIndex;
            
        }// end for taskIndex processing location information of an Order
        
        // now go back and update starting lat/lon in tempArray
        // for Tasks that depend on end position of other Tasks
        for (taskIndex=0; taskIndex < numberOfTasks ; taskIndex++){
           
            // look for predecessor Task in routeLayers
            // Java doesn't store subarray sizes so we must remember
            tempArray = new float[holdTempArraySizes[taskIndex]];
            System.arraycopy(holdTempArrays[taskIndex], 0, tempArray, 0, tempArray.length);
            String execAfterTask = prevTaskUuid[taskIndex];
            if(!execAfterTask.equals("")){
                for(int layer=0; layer <= bml.routeLayerIndex; ++layer){

                    // finess the fact this is being done in constructor
                    // therefore this RouteLayer is not yet in routeLayers[]
                    RouteLayer prevRouteLayer;
                    if(layer < bml.routeLayerIndex)
                        prevRouteLayer = bml.routeLayers[layer];
                    else prevRouteLayer = this;
                    
                    // look at only RouteLayers holding C2SIM Orders
                    if(!prevRouteLayer.bmlDocumentType.equals("C2SIM Order"))
                        continue;

                    // find the one predecessor Task, start with its final coordinates
                    for(int scanTasks=0; scanTasks < prevRouteLayer.numberOfTasks; ++scanTasks){
                        if(!prevRouteLayer.taskUuid[scanTasks].equals(execAfterTask))
                            continue;

                        // j holds index of the predecessor Task in prevRouteLayer 
                        tempArray[0] = prevRouteLayer.destLat[scanTasks];
                        tempArray[1] = prevRouteLayer.destLon[scanTasks];
                        break;
                        
                    }// end for(int j
                }// end for(int layer
            }// end if(!execAfterTask.equals(""))
            
            if(bml.debugMode){
                bml.printDebug("drawing a polyline for Task " + taskIndex);
                for (int i = 0; i < tempArray.length; i+=2)
                    bml.printDebug("   lat:" + tempArray[i] +
                        " lon:" + tempArray[i+1]); 
            }

            // at this point C2SIM has defined only a
            // Route, so draw a Line between each pair of points 
            // the last point and the first one are not the same
            this.add(
                taskNames[taskIndex], 
                createPolyLine(tempArray,
                    OMGraphic.DECIMAL_DEGREES, 
                    OMGraphic.DECLUTTERTYPE_SPACE));
		
        } // end for taskIndex processing location information of an Order
 
        // add an icon for each unit based on its
        // control measure; lat/lon could be in
        // Task or references to ControlMeasure
        for(taskIndex = 0; taskIndex < numberOfTasks; ++taskIndex) {
            if(isControlMeasure[taskIndex])continue;
            String unitID = taskUnitID[taskIndex];
            if (!unitID.equals("")){
                String icon = null;
                String hostility = bml.getMilOrgHostility(unitID);             
                if(hostility == null)icon = icon2525b(unitID, "UNK");
                else {
                    if(hostility.equals("HO:") || hostility.equals("AHO"))
                        icon = icon2525b(unitID, "HO");
                    else if(hostility.equals("FR:") || hostility.equals("AFR"))
                        icon  = icon2525b(unitID, "FR");
                    else icon = icon2525b(unitID, "UNK");
                }
                latOpen = taskUnitIDLat[taskIndex];
                lonOpen = taskUnitIDLon[taskIndex];
                bml.listAddIconUuid(unitID,latOpen,lonOpen,IconType.ORDER);   
/* this needs worked to deal with C2SIM Location Reference
   could be  EntityDefinedLocation to a UUID 
   or RelativeLocation offset from another UUID 
   leaving this for a time when it is needed - JMP 24May2019
                // if the Task had WhereID not lat/lon
                if(latOpen == 0f && lonOpen == 0f) {
                    int j = 0;
                    for(j = 0; j < numberOfTasks; ++j){
                        if(whereID[j].equals(whereID[taskIndex]) &&
                        isControlMeasure[j])break;
                    }
                    if(j >= numberOfTasks) {
                        bml.printError(
                            "unable to match Task WhereID:" + whereID[taskIndex] +
                            " with a ControlMeasure to draw unit icon");
                        break;
                    }
                    latOpen = taskUnitIDLat[j];
                    lonOpen = taskUnitIDLon[j];
                }// end if(latOpen
end needs rework for C2SIM Location ReferenceEntity */
                String unitName = bml.getMilOrgName(unitID);
                if(unitName == null) {
                    if(bml.debugMode)bml.printDebug(
                        "don't have unit Name for C2SIM UUID:" +unitID);
                    unitName = unitID;
                }

                // add the icon to display
                addIcon("     " + unitName, icon);

                // remove any additional instances of this UnitID
                // so we don't add it to map more than once
                // TODO: ideally we would post icon for the Task 
                // with lowest DateTime
                for(int k = taskIndex+1; k < numberOfTasks; ++k) {
                    if(taskUnitID[k].equals(taskUnitID[taskIndex]) && !isControlMeasure[k])
                        taskUnitID[k] = "";
                }
            }// end if (!unitID
        }// end for(int taskIndex

        return graphics;
        
    } // end createOrderGraphicsC2SIM()

    public ListEditableOMGraphics createOrderGraphicsIBML09(ListEditableOMGraphics graphics) {
        graphics.clear();
        boolean foundUnitID = false;// to accept only first UnitID for a Task
        boolean foundLat = false;   // to determine if the location includes coordinates  
        int locationCount = 0;      // number of locations
        int startGDC = 0;           // start index in the array of the location
        int endGDC = 0;             // end index in the array of the location
        int lengthGDC = 0;          // length of the location 
        int noOfPointGDC = 0;       // number of points in the location. 
                                    // If it is one then it is a point
                                    // if it is more than one then it is a line or an area
                                    // the line differs from the area in that the area has 
                                    // the same last and first points
  
        // count the Locations in the stringArray
        // use to create storage for building graphics
        int numberOfLocations = 0;
        for (int i = 0; i < stringArray.length; i++){	
            if(stringArray[i].equals("GroundTask") ||
                stringArray[i].equals("AirTask") ||
                stringArray[i].equals("ControlMeasure"))
                ++numberOfLocations;
        }
        if(numberOfLocations == 0){
            bml.printError("C2SIMGUI graphics processing found no IBML09 Tasks to process");
            return null;
        }
        if(bml.debugMode)bml.printDebug(
            "in createOrderGraphicsIBML09 numberOfLocations=" + numberOfLocations);

        // number , startGDC, endGDC, lengthGDC, noOfPointGDC - control measures
        int [][] descArray = new int [numberOfLocations][5];
    
        // arrays to save TaskID so as to display unit icon
        // after ControlMeasures have been ingested
        String[] taskUnitID = new String[numberOfLocations];
        float[] taskUnitIDLat = new float[numberOfLocations];
        float[] taskUnitIDLon = new float[numberOfLocations];
        String[] whereID = new String[numberOfLocations];
        boolean[] isControlMeasure = new boolean[numberOfLocations];
    
        // WhereClass, WhereLabel, WhereQualifier, UnitID 
        String[][] strDescArray = new String [numberOfLocations][5];	
        for(int j=0; j<5; ++j)strDescArray[0][j] = "";
        isControlMeasure[0] = false;
	
        // Parsing an order string and drawing location information 
        if(bml.debugMode)bml.printDebug(
            "============   Drawing IBML09 Order   ==================");
        for (int i = 0; i < stringArray.length; i++){	
    
            // Look for location Information that starts a Task
            if (bml.ibmlTagCompare(stringArray[i],"Latitude") &&
                startGDC == 0) {
                if(isControlMeasure[locationCount])
                if(bml.debugMode)bml.printDebug(
                    "A ControlMeasure Location has been discovered #: " + 
                locationCount + " coordinates start at : " + i);
            else
                if(bml.debugMode)bml.printDebug(
                    "A Task Location has been discovered #: " + 
                    locationCount + " coordinates start at : " + i);

                // initialize for the location
                startGDC = i;
                foundLat = true;
        }// end for(int i
      			
        // Storing the latest text desc in strDescArray
        if (bml.ibmlTagCompare(stringArray[i],"WhereClass"))
            strDescArray[locationCount][0] = stringArray[i+1];// type: AREA, LINE, POINT 
        //if (ibmlTagCompare(stringArray[i],"WhereCategory"))
        //  strDescArray[locationCount][0] = stringArray[i+1];// role: ROUTE, ASSEMBLY AREA
        if (bml.ibmlTagCompare(stringArray[i],"WhereLabel"))
        strDescArray[locationCount][1] = stringArray[i+1];// assigned by order author
        if (bml.ibmlTagCompare(stringArray[i],"WhereQualifier"))
        strDescArray[locationCount][2] = stringArray[i+1];// how: AT, ALONG, etc.
        if (bml.ibmlTagCompare(stringArray[i],"UnitID")){
            if(!foundUnitID) 
            strDescArray[locationCount][3] = stringArray[i+1];// UnitID
            foundUnitID = true;
        }
        if (bml.ibmlTagCompare(stringArray[i],"WhereID"))
        strDescArray[locationCount][4] = stringArray[i+1];// identifies the Where

        // look for end of lat/lon = start of next 
        // GroundTask, AirTask or ControlMeasure
        if ( ((i == stringArray.length-1) ||
            bml.ibmlTagCompare(stringArray[i],"GroundTask") ||
            bml.ibmlTagCompare(stringArray[i],"AirTask") ||
            bml.ibmlTagCompare(stringArray[i],"ControlMeasure"))){
            if(startGDC > 0 && startGDC < i && foundLat) {

                // there are coordinates to be captured
                endGDC = i;
                if(i == stringArray.length-1)++endGDC;
                    lengthGDC = (endGDC - startGDC);
                    noOfPointGDC = (endGDC - startGDC)/4;
                } else {
                    // no coordinates to capture
                    startGDC = 0;
                    endGDC = 0;
                    lengthGDC = 0;
                    noOfPointGDC = 0;
                    if(bml.debugMode)bml.printDebug("Discovered a Location #:" + locationCount +
                          " with no coordinates; WhereID:" + strDescArray[locationCount][4]);
                }// end else

                // special case for first location
                if(i == 0)
                    isControlMeasure[0] = bml.ibmlTagCompare(stringArray[i],"ControlMeasure");

                // capturing parameters in array for drawing
                descArray[locationCount][0]=locationCount;
                descArray[locationCount][1]=startGDC;
                descArray[locationCount][2]=endGDC;
                descArray[locationCount][3]=lengthGDC;
                descArray[locationCount][4]=noOfPointGDC;

                // initialize for next location
                startGDC = 0;
                foundUnitID = false;
                foundLat = false;
                if(i > 0)locationCount++;
                if(locationCount < numberOfLocations) {
                    for(int j=0; j<5; ++j)strDescArray[locationCount][j] = "";
                    isControlMeasure[locationCount] =
                    bml.ibmlTagCompare(stringArray[i],"ControlMeasure");
                }
            }// end if(((i
        }// end for i  
		
        int latLocation = 0, lonLocation = 0;
        int tempArraySize = 0;
        if(bml.debugMode)bml.printDebug(" locationCount   : " + locationCount );

        // generating graphics for each location previously captured
        for (int i=0; i < locationCount ; i++){
            whereOutput(strDescArray[i]); //to System.out

            // creating a temp array for each graphical location
            // the length of the array will be twice as the number of lat,lon points	
            //Area : the last lat,;on should be the same lat,lon
            tempArraySize = (descArray[i][4])*2; // noOfPointGDC	
            if (strDescArray[i][0].trim().equals("LN")){
                    if(bml.debugMode)bml.printDebug(" Reading a line  Info " );
            }
            else if (strDescArray[i][0].trim().equals("PT")){
                    if(bml.debugMode)bml.printDebug(" Reading a point  Info " );
            }
            else { // A line // Along Route
                    if(bml.debugMode)bml.printDebug(" Reading a Surface Info " );
                    tempArraySize += 2;	
            }
            if(bml.debugMode)bml.printDebug(" tempArraySize = " + tempArraySize  );
            tempArray = new float[tempArraySize];
            int tempArrayIndex = 0;

            // creating a temp array for each graphical location
            float iconLat = 0f, iconLon = 0f;
                for (int h = 0; h < descArray[i][4]; h++) {							
                    latLocation = (descArray[i][1])+(1+(4*h));
                    lonLocation = (descArray[i][1])+(3+(4*h));
                    tempArray[tempArrayIndex]= 
                        stringToFloat(stringArray[latLocation].trim());
                    if(bml.debugMode)bml.printDebug(
                        " tempArray["+tempArrayIndex+"] " + 
                        tempArray[tempArrayIndex] );
                    tempArray[tempArrayIndex+1]= 
                        stringToFloat(stringArray[lonLocation].trim());
                    if(bml.debugMode)bml.printDebug(
                        " tempArray["+(tempArrayIndex+1)+"] " + 
                        tempArray[tempArrayIndex+1] );
                if(h == 0) {
                    iconLat = tempArray[tempArrayIndex];
                    iconLon = tempArray[tempArrayIndex+1];
                }
                tempArrayIndex += 2;
        
            } // end for h

            // Auto Center
            if(tempArraySize >1)
                scaleAndCenterIfEmpty(tempArray[0], tempArray[1], 6000000f);

            // use WhereLabel or WhereID whichever has content
            String featureLabel = strDescArray[i][1];
            if(featureLabel.equals(""))featureLabel = strDescArray[i][4];

            // draw whatever shape the Location calls for	
            if (strDescArray[i][0].trim().equals("LN")){	
                if(bml.debugMode)bml.printDebug(" Drawing a Line Info " + strDescArray[i][2]);

                // the shape is a Route, so draw a Line between each pair of points 
                // the last point and the first one are not the same
                this.add(featureLabel, createPolyLine(tempArray,
                    OMGraphic.DECIMAL_DEGREES, OMGraphic.DECLUTTERTYPE_SPACE));
            }

            // if the location object is a surface then add a point
            // at the end of tempArray that has the values of the first point
            // Draw a Polygon with the last point and the first one are the same
            if (strDescArray[i][0].trim().equals("SURFAC")){
                if(bml.debugMode)bml.printDebug(" Drawing  Surface  " + strDescArray[i][2]);
                tempArray[tempArrayIndex]=tempArray[0];	
                tempArray[tempArrayIndex+1]=tempArray[1];	
                tempArrayIndex += 2;

                // the shape is not a point so draw a Poly
                graphics.add(featureLabel,createPoly(tempArray, 0, 1));
            }

            // if the object is a point call the draw point method
            if (tempArray.length == 2) {
                if(bml.debugMode)bml.printDebug(" Drawing Point Info " + strDescArray[i][2]);
                latOpen = tempArray[0];
                lonOpen = tempArray[1];
                this.add(featureLabel,createPoint(Color.red));
            }

            // save data to add unit icon later
            whereID[i] = strDescArray[i][4];
            taskUnitID[i] = strDescArray[i][3];
            if(!isControlMeasure[i] && descArray[i][4] == 0) {
      
                // Task has no coordinates - 
                // only WhereID
                iconLat = 0f;
                iconLon = 0f;
            } 
            taskUnitIDLat[i] = iconLat;
            taskUnitIDLon[i] = iconLon;
            if(whereID.equals("") && iconLat == 0f && iconLon == 0f)
                bml.printError("order has Task with neither WhereID or Latitude/Longitude");
		
        } // end for i processing location information of an Order09
  
        // add an icon for each unit based on its
        // control measure; lat/lon could be in
        // Task or references to ControlMeasure
        for(int i = 0; i < numberOfLocations; ++i) {
            if(isControlMeasure[i])continue;
            String unitID = taskUnitID[i];
            if (!unitID.equals("")){
                String icon = icon2525b(unitID, "FR");
                latOpen = taskUnitIDLat[i];
                lonOpen = taskUnitIDLon[i];

                // if the Task had WhereID not lat/lon
                if(latOpen == 0f && lonOpen == 0f) {
                    int j = 0;
                    for(j = 0; j < numberOfLocations; ++j){
                        if(whereID[j].equals(whereID[i]) &&
                        isControlMeasure[j])break;
                    }
                    if(j >= numberOfLocations) {
                        bml.printError(
                            "unable to match Task WhereID:" + whereID[i] +
                            " with a ControlMeasure to draw unit icon");
                        break;
                    }
                    latOpen = taskUnitIDLat[j];
                    lonOpen = taskUnitIDLon[j];
                    
                }// end if(latOpen

                // add the icon to display
                addIcon("     " + unitID, icon);

                // remove any additional instances of this UnitID
                // so we don't add it to map more than once
                // TODO: ideally we would post icon for the Task 
                // with lowest DateTime
                for(int k = i+1; k < numberOfLocations; ++k) {
                    if(taskUnitID[k].equals(taskUnitID[i]) && !isControlMeasure[k])
                        taskUnitID[k] = "";
                }
            }// end if (!unitID
        }// end for(int i
        
        return graphics;
        
    } // end createOrderGraphicsIBML09()
    
    public void clearGraphics() {
    	omgraphics.clear();
    }

    public void add(String pName, EditableOMGraphic eg) {
        String useName = pName;
        if(pName == null) useName = "????";
        if(eg == null) {
            if(bml.debugMode)bml.printDebug(
                "Map icon skipped because object missing at " + 
                    latOpen + "/" + lonOpen);
            return;
        }
        com.bbn.openmap.omGraphics.OMLabeler label = 
            new com.bbn.openmap.omGraphics.OMTextLabeler(useName);
    	eg.getGraphic().putAttribute(OMGraphic.LABEL, label);
    	omgraphics.add(useName, eg);
    }
    
    public EditableOMGraphic getGraphic(String sName) {
    	return omgraphics.getGraphic(sName);
    }
    
    public void removeGraphic(String sName) {
    	omgraphics.removeGraphic(sName);
    }
    
/**
 * calculate unit symbol code milstd2525b to draw the icon on the map
 */
String icon2525b(String sbmlUnitID, String unitHostility){
    
        String milStd2525bSymbolCode = bml.getMilOrgSymbolID(sbmlUnitID);   
        if(milStd2525bSymbolCode == null){
            
            // some special cases from VRForces - TODO: add more
            // these come from observation reports (sbmlUnitID not in MilOrg maap)
            String checkFunction = sbmlUnitID.toUpperCase();
            if(checkFunction.contains("ARM"))return "SFGPUCA----E---";
            if(checkFunction.contains("INF"))return "SFGPUCI----E---";
            if(checkFunction.contains("USA"))return "SFGPUC*----E---";
            if(checkFunction.contains("MobileIrregular"))return "SHGPUCI----B---";
            if(unitHostility.equals("HO"))return "SHGPU-----*****";
            return "***************";
        }
        
        // check for bad data
        int symbolChars = milStd2525bSymbolCode.length();
        if(symbolChars != 15){
            bml.printError("icon2525b can't graph milStd2525bSymbolCode for unit:" +
                sbmlUnitID + " - length is " + symbolChars + 
                " must be 15");
            return "***************";
        }
            
        // handle case for unitID not in the MilOrg Map
        if(milStd2525bSymbolCode == null) {
			
            // Start Calculating the Symbol Code of the SymbolIcon to be displayed
            // Modify the create point method to acceptd a String representing the 
            // Symbol Code milstd2525b code     

            // CodeScheme Position 1
            String milStd2525bCodeScheme ="S";   

            // Affiliation Position 2  
            if(bml.debugMode)bml.printDebug("icon2525b unitHostility: " + unitHostility);
            String milStd2525bAffliation = getHostility(unitHostility); 

            // Battle Dimension Position 3
            String milStd2525bDimention ="G";   

            // Unit Status ( Anticipated / Present)Position 4 
            String milStd2525bStatus ="P"; 

            // Unit Function ID Position 5-10
            String unitArm_Cat_Code = unitIdToArmCatCode(sbmlUnitID);
            if(bml.debugMode)bml.printDebug("icon2525b UnitID:" + sbmlUnitID + 
                    " unitArm_Cat_Code: " + unitArm_Cat_Code);
            String milStd2525bFunctionID = getFunctionID(unitArm_Cat_Code);  

            // Modifier Position 11-12
            String milStd2525bModifier ="--";   

            // Country Code Position 13-14
            String milStd2525bCountryCode ="**";

            // Order of Battle Position 15
            String milStd2525bOrderOfBattle ="*";

            // debug printing
            if(bml.debugMode)bml.printDebug(
                "milStd2525bCodeScheme S  : 1 " + milStd2525bCodeScheme);
            if(bml.debugMode)bml.printDebug(
                "milStd2525bAffliation    : 2 " + milStd2525bAffliation);
            if(bml.debugMode)bml.printDebug(
                "milStd2525bDimention  G  : 3 " + milStd2525bDimention);
            if(bml.debugMode)bml.printDebug(
                "milStd2525bStatus     P  : 4 " + milStd2525bStatus);
            if(bml.debugMode)bml.printDebug(
                "milStd2525bFunctionID    : 5 " + milStd2525bFunctionID);
            if(bml.debugMode)bml.printDebug(
                "milStd2525bModifier  --  :11 " + milStd2525bModifier);
            if(bml.debugMode)bml.printDebug(
                "milStd2525bCountryCode **:13 " + milStd2525bCountryCode);
            if(bml.debugMode)bml.printDebug(
                "milStd2525bOrderOfBattle*:15 " + milStd2525bOrderOfBattle);

            // assemble the icon code
            milStd2525bSymbolCode = 
                milStd2525bCodeScheme + 
                milStd2525bAffliation +
                milStd2525bDimention + 
                milStd2525bStatus + 
                milStd2525bFunctionID +
                milStd2525bModifier + 
                milStd2525bCountryCode + 
                milStd2525bOrderOfBattle;
        
        }// end if(milStd2525bSymbolCode == null)
        
        // hacks for known flaws in rendering the SymbolCode
        else {
            // deal with SISO/MSDL cleverness in rejecting ****
            if(milStd2525bSymbolCode.endsWith("---"))
                milStd2525bSymbolCode = milStd2525bSymbolCode.substring(0,12) + "***";
        
            if(milStd2525bSymbolCode.charAt(11) == 'B'){
                String part1 = milStd2525bSymbolCode.substring(0,11);
                String part2 = milStd2525bSymbolCode.substring(12);
                milStd2525bSymbolCode = part1 + '*' + part2;
            }
            if(milStd2525bSymbolCode.charAt(1) == '-'){
                String part1 = milStd2525bSymbolCode.substring(0,1);
                String part2 = milStd2525bSymbolCode.substring(2);
                char hostilityCode = '*';
                if(unitHostility.equals("FR"))hostilityCode = 'F';
                if(unitHostility.equals("AFR"))hostilityCode = 'F';
                if(unitHostility.equals("HO"))hostilityCode = 'H';
                if(unitHostility.equals("AHO"))hostilityCode = 'H';
                milStd2525bSymbolCode = part1 + hostilityCode + part2;
            }
        }
        
        return milStd2525bSymbolCode;
        
  }// end icon2525b()

    /**
     * adds a unit icon with name
     * if name contains a blank and icon has known
     * functionID, truncate name at first blank after text
     * e.g. "1/100 ARMOR" becomes "1/100"
     * returns the truncated name
     */
     void addIcon(String unitID, String iconCode) {
         // find start of text
         int startText = 0;
         for (; startText < unitID.length(); ++ startText)
             if(unitID.charAt(startText) != ' ')break;
         
         // trim off any chars starting first blank after text
         String labelID = unitID;
         if(!iconCode.substring(4,10).equals("------")){
             int blankPosition = labelID.indexOf(' ');
             if(blankPosition > 0)
                 labelID = labelID.substring(0,blankPosition);
         }
         
         // add the icon to the map
         this.add(labelID, createPoint(iconCode,unitID));
         
     }// end addIcon()

    /**
     * Draws STOMP Subscriber GeneralStatusReport Graphics. 
     * Clears and then fills the given OMGraphicList.
     * 
     * @param graphics The OMGraphicList to clear and populate
     * @return the graphics list, after being cleared and filled
     * 
     * mababneh
     * 11/9/2014
     */
    public void createSubscriberGraphics(String[] subUnitArray, String subDocType) {
    	
    	String subUnitName = null;
    	String subUnitSymbolID = null;
    	float subUnitLat = 0;
    	float subUnitLon = 0;
       
        subUnitLat = stringToFloat(subUnitArray[0].trim());
        subUnitLon = stringToFloat(subUnitArray[1].trim());
        subUnitName = subUnitArray[2].trim();// "SubUnit"; //subUnitArray[0];
        subUnitSymbolID = subUnitArray[3].trim();//"SFGPUCAA--*****"; //subUnitArray[1];

        if(bml.debugMode)bml.printDebug("Unit Name: " + subUnitName);
        if(bml.debugMode)bml.printDebug("Unit Symbol ID: " + subUnitSymbolID);
        if(bml.debugMode)bml.printDebug("Unit Lat: " + subUnitLat);
        if(bml.debugMode)bml.printDebug("Unit Lon: " + subUnitLon);   	

        // Draw a point - to be replaced with 2525b icon
        latOpen = subUnitLat;
        lonOpen = subUnitLon;
        addIcon(subUnitName, subUnitSymbolID);
        
        // Set focus on first subUnit lat lon
        scaleAndCenterIfEmpty(latOpen, lonOpen, 600000f);
        if(bml.debugMode)bml.printDebug(
            "=== Printing Units in--------------- ---- RouteLayer");

    } // end createSubscriberGraphics()
    
    /**
     * Draws MSDL Graphics. Clears and then fills the given OMGraphicList.
     * 
     * @param graphics The OMGraphicList to clear and populate
     * @return the graphics list, after being cleared and filled
     * 
     * mababneh
     * 11/9/2011
     */
    public void createMSDLGraphics(String[][] orgArray, String[]environmentArray) {
    	
    	String orgName = null;
    	String orgSymbolID = null;
    	float orgLat = 0;
    	float orgLon = 0;
    	
    	String areaName = null;
    	
    	float areaUpperRightLat = 0;
    	float areaUpperRightLon = 0;
    	float areaLowerLefttLat = 0;
    	float areaLowerLeftLon = 0;
    	float tempAreaArray[] = new float[10]; // array to hold area of interest points.
    	
    	float firstLat = 0, firstLon = 0;
    	// Auto Center
    	firstLat = stringToFloat(orgArray[0][2].trim());
    	firstLon = stringToFloat(orgArray[0][3].trim());

    	// Set focus on first unit lat lon
    	scaleAndCenterIfEmpty(firstLat, firstLon, 600000f);

        if(bml.debugMode)bml.printDebug(
            "=== Printing Units and Equipments ---- RouteLayer");

        // Environment drawing

        areaName = environmentArray[0];

        areaUpperRightLat = stringToFloat(environmentArray[1].trim());
    	areaUpperRightLon = stringToFloat(environmentArray[2].trim());
    	areaLowerLefttLat = stringToFloat(environmentArray[3].trim());
    	areaLowerLeftLon = stringToFloat(environmentArray[4].trim());   	
	
    	if(bml.debugMode)bml.printDebug("areaName : " + areaName);
        if(bml.debugMode)bml.printDebug("areaUpperRightLat: " + areaUpperRightLat);
        if(bml.debugMode)bml.printDebug("areaUpperRightLon: " + areaUpperRightLon);
        if(bml.debugMode)bml.printDebug("areaLowerLefttLat " + areaLowerLefttLat);
        if(bml.debugMode)bml.printDebug("areaLowerLefttLon " + areaLowerLeftLon);

        tempAreaArray[0] = areaLowerLefttLat;
        tempAreaArray[1] = areaUpperRightLon;
        tempAreaArray[2] = areaUpperRightLat;
        tempAreaArray[3] = areaUpperRightLon;
        tempAreaArray[4] = areaUpperRightLat;
        tempAreaArray[5] = areaLowerLeftLon;
        tempAreaArray[6] = areaLowerLefttLat;
        tempAreaArray[7] = areaLowerLeftLon;
        tempAreaArray[8] = areaLowerLefttLat;
        tempAreaArray[9] = areaUpperRightLon;
        
        this.add(
            areaName, 
            createPolyLine(
                tempAreaArray, 
                OMGraphic.DECIMAL_DEGREES, 
                OMGraphic.DECLUTTERTYPE_SPACE)
        );
        
        // Unit & Equipment drawing
    	for (int i=0; i<orgArray.length;i++){
            orgName = orgArray[i][0];
            orgSymbolID = orgArray[i][1];
            orgLat = stringToFloat(orgArray[i][2].trim());
            orgLon = stringToFloat(orgArray[i][3].trim());	
            if(bml.debugMode)bml.printDebug("Org Name: " + orgName);
            if(bml.debugMode)bml.printDebug("Org Symbol ID: " + orgSymbolID);
            if(bml.debugMode)bml.printDebug("Org Lat: " + orgLat);
            if(bml.debugMode)bml.printDebug("Org Lon: " + orgLon);   	
        	
            // Draw a point - to be replaced with 2525b icon
            latOpen = orgLat;
            lonOpen = orgLon;
            addIcon(orgName, orgSymbolID);
    	
    	} // end for (int i
        
    } // End of method create MSDL Graphics
    
    /**
     * Draws OPORD Graphics. Clears and then fills the given OMGraphicList.
     * 
     * @param graphics The OMGraphicList to clear and populate
     * @return the graphics list, after being cleared and filled
     */
    public void createOPORDGraphics(
            String shapeName, 
            String[] shapeStringArray, 
            String shapeType, 
            int noOfCoords) {
        if(bml.debugMode)bml.printDebug(	" Shape Name     : " + shapeName +
                        "\n Shape Type     : " + shapeType +
                        "\n Coords count   : " + noOfCoords );
        tempArray = new float[shapeStringArray.length];

        //For each drawing object 
        // convert the string Array to float array
        // the first point is empty, fix later , test now
        for (int i = 0; i < shapeStringArray.length; i++){
            tempArray[i]= stringToFloat(shapeStringArray[i].trim());
            if(bml.debugMode)bml.printDebug(" tempArray [" + i + "] " + tempArray[i] );
        }

        // Auto Center
        scaleAndCenterIfEmpty(tempArray[0],tempArray[1], 600000f);

        float firstLat = 0, firstLon = 0;
        float lastLat = 0, lastLon = 0;

        if (shapeType.equals("LN")||shapeType.equals("LINE")){
            // the shape is a Line, so draw a PolyLine
            if(bml.debugMode)bml.printDebug(" Drawing  line " );				
            this.add(
                shapeName, 
                createPolyLine(
                    tempArray, 
                    OMGraphic.DECIMAL_DEGREES, 
                    OMGraphic.DECLUTTERTYPE_SPACE));
        }
        // if the location object is a surface then add a
        // point at the end of tempArray that has the values of the first point
        // Draw a Polygon with the last point and the first one are the same
        else if (shapeType.equals("SURFAC")){
            if(bml.debugMode)bml.printDebug(" Drawing  surface  " );
            this.add(
                shapeName, 
                createPoly(
                    tempArray, 
                    OMGraphic.DECIMAL_DEGREES, 
                    OMGraphic.DECLUTTERTYPE_SPACE));
        }
        // if the object is a point call the draw point method
        else if (shapeType.equals("PT")){
            if(bml.debugMode)bml.printDebug(" Drawing point  " );
            latOpen = tempArray[0];
        lonOpen = tempArray[1];
            this.add(shapeName, createPoint(Color.red));
        }
        else {
            if(bml.debugMode)bml.printDebug(
                " Drawing Unknown type, assuming polyline " );
            this.add(shapeName,
                createPolyLine(
                    tempArray, 
                    OMGraphic.DECIMAL_DEGREES, 
                    OMGraphic.DECLUTTERTYPE_SPACE));
        }
        
    } // end createOPORDGraphics()
    
    /**
     * Draws General StatusReport Graphics from old schema and sample. 
     * Clears and then fills the given OMGraphicList. 
     * 
     * @param graphics The OMGraphicList to clear and populate
     * @return the graphics list, after being cleared and filled
     */
    public ListEditableOMGraphics createGeneralStatusReportGraphics(
        ListEditableOMGraphics graphics, 
        String bmlDocumentType,
        String reportText) {

        graphics.clear();        
        if(bml.debugMode)bml.printDebug(
            "Processing GeneralStatusReport String, flavor " + 
          bml.generalBMLType + "................ ");
        if(bml.debugMode)bml.printDebug(
            "The string length is : " + stringArray.length);
        boolean isCbml = (bml.generalBMLType == "CBML");
        boolean isIbml = (bml.generalBMLType == "IBML");
        String latitude = "", longitude = "";
        String whenTime = ""; 

        // scan stringArray created by XmlParse to get the data associated with 
        // tags UnitID, Hostility, Latitude and Longitude
        int scanRange = stringArray.length - 1;
        int reportStart = 0;

        // loop though multiple reports
        while(reportStart < scanRange){
            latOpen = 0f;
            lonOpen = 0f;
            unitHostility = "";
            sbmlUnitID = "";
            boolean isGsr = false;
            int i;

            // look for GSR that starts report
            for(;reportStart < scanRange; ++reportStart){
              if(bml.cbmlTagCompare(stringArray[reportStart],"GeneralStatusReport") ||
                bml.ibmlTagCompare(stringArray[reportStart],"GeneralStatusReport"))
              {
                isGsr = true;
                break;
              }
            }
            if(reportStart >= scanRange)break;
            ++reportStart;

            // capture report elements
            for (i=reportStart; i < scanRange; i++){

                if(bml.debugMode)bml.printDebug(
                    "Report String Array  [ " + i + " ] == " + stringArray[i]);

                // look for CBML lat/lon, and Hostility
                if(isGsr && isCbml){
                    if(bml.cbmlTagCompare(stringArray[i],"Latitude")){
                        latitude = stringArray[i+1];
                        if(latOpen == 0f)
                            latOpen = stringToFloat(stringArray[i+1].trim());
                    }
                    if(bml.cbmlTagCompare(stringArray[i],"Longitude")){
                        longitude = stringArray[i+1];
                        if(lonOpen == 0f)
                            lonOpen = stringToFloat(stringArray[i+1].trim());
                    }
                    if(bml.cbmlTagCompare(stringArray[i],"Hostility"))
                      unitHostility = stringArray[i+1];
                    if(bml.cbmlTagCompare(stringArray[i],"When"))
                        whenTime = stringArray[i+1];                    
                }// end if(isCbml

                // look for IBML lat/lon, and Hostility
                if(isGsr && isIbml){
                    if(bml.ibmlTagCompare(stringArray[i],"Latitude")) {
                        latitude = stringArray[i+1];
                        if(latOpen == 0f)
                            latOpen = stringToFloat(stringArray[i+1].trim());
                    }
                    if(bml.ibmlTagCompare(stringArray[i],"Longitude")) {
                        longitude = stringArray[i+1];
                        if(lonOpen == 0f)
                            lonOpen = stringToFloat(stringArray[i+1].trim());
                    }
                    if(bml.ibmlTagCompare(stringArray[i],"Hostility"))
                      unitHostility = stringArray[i+1];
                    if(bml.ibmlTagCompare(stringArray[i],"When"))
                        whenTime = stringArray[i+1];

                }// end if(isIbml

                if(bml.cbmlTagCompare(stringArray[i],"GeneralStatusReport") ||
                    bml.ibmlTagCompare(stringArray[i],"GeneralStatusReport"))break;

            } // end For i going through the Report String (data)
            int reportEnd = i;

            // scan again up to reportEnd for last UnitID now that we have Hostility
            String milStd2525bSymbolCode = null;
            for (i=reportStart; i < reportEnd; i++){	
                if((isGsr && isCbml && bml.cbmlTagCompare(stringArray[i],"UnitID")) || // IBML09
                (isGsr && isIbml && bml.ibmlTagCompare(stringArray[i],"UnitID"))) {    // CBML
      
                    // unitID from the CBML GUI Editor to be sent to SBML Client
                    sbmlUnitID = stringArray[i+1].trim();  

                    // look for CenterUnit if one is designated in config
                    if(bml.centerUnit.length() > 0 && !bml.anIconIsOnTheMap) {
                        if(!sbmlUnitID.equals(bml.centerUnit)) {
                            if(bml.debugMode)bml.printDebug(
                                "ignoring this report because Executer UnitID:" + sbmlUnitID +
                                " does not match CenterUnit:" + bml.centerUnit);
                            isGsr = false;
                            break;
                        }
                    }
                    
                    // at this point UnitID probably is a name; try to look up UUID
                    MilOrg milOrg = bml.getUnit(sbmlUnitID);
                    if(milOrg == null)milOrg = bml.getUnitByName(sbmlUnitID);
                    if(milOrg != null)if(milOrg.uuid != null)sbmlUnitID = milOrg.uuid;
                    if(unitHostility.equals(""))if(milOrg != null)
                        unitHostility = milOrg.getHostility();
                    
                    // Set map center to first point of location and Auto Zoom
                    scaleAndCenterIfEmpty(latOpen, lonOpen);

                    if(bml.debugMode)bml.printDebug(
                        "Executer unit hostility value is :" + unitHostility );
                    if(bml.debugMode)bml.printDebug(
                        "Executer unit ID value is :" + sbmlUnitID );
                    if(bml.debugMode)bml.printDebug(
                        "============================================================");

                    // use SBML Client to get UnitID, details, hostility and 
                    // Unit Symbol Icon Code
                    //   (0) get unitID
                    //   (1) get unit information using UnitID
                    //   (2) get unit arm_cat_code
                    //   (3) get hostility
                    //   (4) calculate unit symbol code milstd2525b to draw the icon on the map
                    //   (5) call createPoint method to draw the point in the specifid location
                    milStd2525bSymbolCode = icon2525b(sbmlUnitID, unitHostility);
                    if(bml.debugMode)bml.printDebug(
                        "The associated Symbol Code for this unit is : " + 
                        milStd2525bSymbolCode);
                }// end if(isGsr
                
            } // end For i going through the Report String (data)

            // confirm we have all the data needed to draw GSR
            if(!isGsr)continue;
            if((unitHostility.length()==0) || (sbmlUnitID.length()==0) ||
                ((latOpen == 0f) && (lonOpen == 0f))) {
                bml.printError("critical report data missing - can't make report graphic");
                bml.printError("isGSR:"+isGsr+" UnitID:"+sbmlUnitID+
                  " Hostility:"+unitHostility+" Latitude:"+latOpen+" Longitude:"+lonOpen);
                continue;
            }
            if(latOpen != 0f || lonOpen != 0f){
                if(bml.debugMode)bml.printDebug(
                    "Location  Lat/Lon: " + latOpen + "/" + lonOpen);
                if(bml.debugMode)bml.printDebug(
                    "UnitID:"+sbmlUnitID+" show tracks:" + bml.showTracks +
                    " GSR:" + isGsr);
            }
        
            // check for report out of time sequence  
            if(bml.getReportTime(sbmlUnitID) == null)
                bml.putReportTime(sbmlUnitID, "0000-00-00T00:00:00Z");
            String previousWhenTime = bml.getReportTime(sbmlUnitID);
            if(bml.warnOnReportSeq.equals("1"))
            if(whenTime.compareTo(previousWhenTime) < 0) {
                bml.printError("report to be displayed next contains time:" + whenTime +
                    " which is earlier than previous report time:" + previousWhenTime +
                    " from the same source - this could be due cyber attack");
                JOptionPane.showMessageDialog(
                    bml, 
                    "report to be displayed next contains time:" + whenTime +
                    "\nwhich is earlier than previous report time:" + previousWhenTime +
                    "\nfrom the same source - this could be due cyber attack",
                    "Report Time Sequence Warning",	
                    JOptionPane.WARNING_MESSAGE);
            }
            bml.putReportTime(sbmlUnitID,whenTime);
            
            // if showing tracks, post the track as
            // a sequence of points connected by lines
            if(bml.showTracks && latOpen != 0f && lonOpen != 0f) {         
                
                // redraw the track point and lines
                this.add("TRACK "+sbmlUnitID,
                    createPolyLine(addTrackPoint(sbmlUnitID, latOpen, lonOpen),
                        OMGraphic.DECIMAL_DEGREES, 
                        OMGraphic.DECLUTTERTYPE_SPACE));
            } 
            
            // if this repeats previous icon, delete it
            String labelName = "     "+sbmlUnitID;

            // add the point; then visit scale and repaint so it shows
            if(sbmlUnitID.length() > 0)
                addIcon(labelName, milStd2525bSymbolCode);
            visitScale();
            repaint();

            // save out values to compare next time
            bml.anIconIsOnTheMap = true;
            
            // save the report for fetch
            bml.reportAddText(reportText, latOpen, lonOpen);
            bml.loadReportButton.setEnabled(true);
            
        }// end while(reportStart < scanRange)
        
        return graphics;

    }// end createGeneralStatusReportGraphics()

    /**
     * Returns the unit Function ID part of the milstd2525b symbol Code
     *	
     * Arm Cat Codes : AIRDEF ARMANT ARMOUR ARTLRY AV AVAFW AVARW INF NKN RECCE 
     * @param arm_cat_code A milstd2525b symbol that we get through the SBMLClient query.
     */
    public String getFunctionID(String arm_cat_code){
    	String symbolFunctionID ="";
    	if(bml.debugMode)bml.printDebug("==============================================");
    	if(bml.debugMode)bml.printDebug("Inside get Function ID part of the symbol Code ");
    	if(bml.debugMode)bml.printDebug("Passed arm_cat_code value is : " + arm_cat_code);
    	if(bml.debugMode)bml.printDebug("==============================================");
    	
    	if (arm_cat_code.equals("AIRDEF")){         // AIR DEFENSE
    		symbolFunctionID = "UCD---";
    	}else if (arm_cat_code.equals("ARMANT")){   // ANTI ARMOR
    		symbolFunctionID = "UCAA--";
    	}else if (arm_cat_code.equals("ARMOUR")){   // ARMOR
    		symbolFunctionID = "UCA---";
    	}else if (arm_cat_code.equals("ARTLRY")){   // FIELD ARTILLERY
    		symbolFunctionID = "UCF---";
    	}else if (arm_cat_code.equals("AV")){       // AVIATION
    		symbolFunctionID = "UCV---";
    	}else if (arm_cat_code.equals("AVAFW")){    // FIXED WING
    		symbolFunctionID = "UCVF--";
    	}else if (arm_cat_code.equals("AVARW")){    // ROTARY WING
    		symbolFunctionID = "UCVR--";
    	}else if (arm_cat_code.equals("INF")){      // INFANTRY
    		symbolFunctionID = "UCI---";
    	}else if (arm_cat_code.equals("NKN")){      // Not known
    		symbolFunctionID = "------";	
    	}else if (arm_cat_code.equals("RECCE")){    // RECONNAISSANCE
    		symbolFunctionID = "UCR---";
    	}else {					    // UNKNOWN
    		if(bml.debugMode)bml.printDebug("(Unknown) ");
    		symbolFunctionID = "------";
    	}
		if(bml.debugMode)bml.printDebug(arm_cat_code + " unit");

    	return symbolFunctionID; 
    } // End of getFunctionID

    /**
     * Returns the unit hostility or affiliation part of the milstd2525b symbol Code
     *
     * @param hostility		A value that we get through the SBMLClient query
     */
    public String getHostility(String hostility){
 
        //default to hostile aka unknown or AHO, AIV, ANT, IV
    	String symbolAffiliation = "*"; 
    	if (hostility.equals("AFR")){      
    		symbolAffiliation = "A";    // Assumed Friend
    	}else if (hostility.equals("FAKER")){
    		symbolAffiliation = "K";	
    	}else if (hostility.equals("FR")){  // Friend
    		symbolAffiliation = "F";
    	}else if (hostility.equals("HO")){  // Hostile
    		symbolAffiliation = "H";
        }else if (hostility.equals("AHO")){ //Assumed Hostile
    		symbolAffiliation = "H";
    	}else if (hostility.equals("JOKER")){    		
    		symbolAffiliation = "J";	
    	}else if (hostility.equals("NEUTRL")){
    		symbolAffiliation = "N";	
    	}else if (hostility.equals("PENDNG")){
    		symbolAffiliation = "P";	
    	}else if (hostility.equals("SUSPCT")){
    		symbolAffiliation = "S";	
    	}else if (hostility.equals("UNK")){  // UNKNOWN
    		symbolAffiliation = "U";	
    	}else {
    		if(bml.debugMode)bml.printDebug("Unrecognized Hostility");
    	}  
        
        if(bml.debugMode)bml.printDebug("==============================================");
    	if(bml.debugMode)bml.printDebug("Inside get Hostility part of the symbol Code ");
    	if(bml.debugMode)bml.printDebug("Passed hostility value is : " + hostility);
        if(bml.debugMode)bml.printDebug("Returning symbolAffiliation : " + symbolAffiliation);
    	if(bml.debugMode)bml.printDebug("==============================================");
    	return symbolAffiliation;
    	
    } // End of getHostility      

    /**
     * Creates an OMLine from the given parameters.
     * 
     * @param tempArray	Contains two pair of lat/long
     * @param color		The line's color
     * @param selColor	The line's selected color
     * @return An OMLine with the given properties
     */
    public EditableOMLine createLine(
        float[] tempArray, int j, Color color, Color selColor) {
            OMLine line = new OMLine(
            tempArray[j], tempArray[j+1], tempArray[j+2], tempArray[j+3], 1);
        line.setLinePaint(color);
        line.setSelectPaint(selColor);
        line.setFillColor(Color.BLUE);
        EditableOMLine eline = new EditableOMLine(line);
        return eline;
    }
    
    public EditableOMPoint createPoint(Color color) {       
    	OMPoint point = new OMPoint(latOpen, lonOpen);
    	point.setLinePaint(color);
    	point.setFillColor(Color.RED);
    	EditableOMPoint epoint = new EditableOMPoint(point);
    	epoint.setProjection(projection);	
    	return epoint;
    }

    /**
     * Draws a milstd2525b symbol icon on the map in the globally
     * specified lat/lon location, with inOp overlay if not operational
     */
    public EditableOMScalingRaster createPoint(String symbolCode){
        return createPoint(symbolCode, "");
    }
    public EditableOMScalingRaster createPoint(
            String symbolCode, 
            String errorUnitID) {
        Dimension di = new Dimension(100,100);
    	ImageIcon ii = new ImageIcon("");
    	
    	// testing
    	if(bml.debugMode)bml.printDebug(" The passed symbol is : " + symbolCode);
    	
        PNGSymbolImageMaker pngsim = 
            new PNGSymbolImageMaker(
                bml.guiFolderLocation+"milstd2525b/milStd2525_png");
        SymbolReferenceLibrary srl = new SymbolReferenceLibrary(pngsim);
     
        if (symbolCode.length()!= 15){
            if(bml.debugMode)bml.printDebug(
                "  Error in Symbol Code, Length not 15 Character");
            if(bml.debugMode)bml.printDebug(
                "  Display another symbol temporarily");
        	
            // Test to be removed when symbol code is always correct
            ii = srl.getIcon("SUGPUCAA--*****", di); 
        }
        else { 	
            // remove HQ code if present
            if(symbolCode.substring(10,11).equals("A"))
                symbolCode = symbolCode.substring(0,10) + "-" + symbolCode.substring(11);
            ii = srl.getIcon(symbolCode, di);

            // fallback if result is bad
            if(ii == null)
            {
                // try dropping echelon code
                if(bml.debugMode)
                bml.printDebug("createPoint can't produce icon for unit:" + 
                    errorUnitID + " symbol:" + symbolCode);
                String substitute = symbolCode.substring(0,11) + "****";
                if(bml.debugMode)
                    bml.printDebug("substituting symbol " + substitute);
                ii = srl.getIcon(substitute, di);
                
                // final recourse - unknown symbol
                if(ii == null) {
                    if(bml.debugMode)
                        bml.printDebug("can't produce icon for unit:" + errorUnitID + 
                            " symbol:" + symbolCode); 
                    char hostilityChar = symbolCode.charAt(1);
                    if(hostilityChar == 'F'){
                        if(bml.debugMode)
                            bml.printDebug("substituting symbol SUGPUC----*****");
                        ii = srl.getIcon("SFGPU-----*****", di);
                    }
                    else if(hostilityChar != 'H') {
                        if(bml.debugMode)
                            bml.printDebug("substituting symbol SUGPUC----*****");
                        ii = srl.getIcon("SUGPU-----*****", di);
                    }
                    else {
                        if(bml.debugMode)
                            bml.printDebug("substituting symbol SHGPUC----*****");
                        ii = srl.getIcon("SHGPUC----*****", di);
                    }
                }
            }
            if(ii == null)
            {
                bml.printError("fallback symbol code failed");
                return null;
            }
            if(latOpen == 0f && lonOpen == 0f){
                bml.showErrorPopup( 
                    "coordinate data missing", 
                    "Couldn't draw icon on map");
                return null;
            }
            if(bml.debugMode)bml.printDebug(" Drawing milstd 2525b symbol " + 
                ii.toString() + " at "+latOpen+"/"+lonOpen);
            if (ii.getImageLoadStatus()!=8){
                if(bml.debugMode)bml.printDebug(
                    "Symbol Code not found in MilStd2525b image library........ ");
                ii = srl.getIcon("SFGPUCAAD-*****", di);
                //SymbolPart sp = new SymbolPart("fff");
            }
        }
        
        //ii = srl.getIcon("SFGPUCAAD-*****", di); 
        				  
        // Unknown WAR.GRDTRK.UNT.CBT.AARM "SUGPUCAA--*****"
        
        // draw a unit symbol on the map
        OMScalingIcon omsi = new OMScalingIcon(latOpen, lonOpen, ii); 
        EditableOMScalingRaster eomsr = new EditableOMScalingRaster(omsi);

        return eomsr;
        
    }// end createPoint(String,String)
    
    public EditableOMCircle createCircle(
        float lat1, float lon1, float dia1, Color color) {
    	OMCircle circle = new OMCircle(lat1,lon1, dia1);
    	circle.setLinePaint(color);
    	EditableOMCircle ecircle = new EditableOMCircle(circle);
    	return ecircle;
    }

    public EditableOMPoly createPolyLine(float[] llPoints, int units, int lType) {
        
    	OMPoly poly = new OMPoly(llPoints, units, lType);
    	poly.setLinePaint(Color.BLUE);
    	EditableOMPoly  epoly = new EditableOMPoly(poly);
    	epoly.setProjection(projection);
    	return epoly;
    }

    public EditableOMPoly createPoly(float[] llPoints, int units, int lType) {
    	OMPoly poly = new OMPoly(llPoints, units, lType);
    	poly.setLinePaint(Color.BLUE);
    	poly.setFillPaint(Color.yellow);
    	EditableOMPoly epoly = new EditableOMPoly(poly);
    	return epoly;
    }
      
    String unitIdToArmCatCode(String unitID) {
    
        // ToDo: when RESTful server supports query we will
        // fetch arm_cat_code from it here
        // For now, need to add other common codes here
        if(unitID.endsWith("INF"))return "INF";
        if(unitID.endsWith("ARMOR"))return "ARMOUR";
        if(unitID.endsWith("ARTY"))return "ARTLRY";
        if(unitID.endsWith("AV"))return "AV";

        // next three are specific to MSG-085 demo not generic
        if(unitID.startsWith("SPARTAN"))return "AV";
        if(unitID.startsWith("VORTEX"))return "AV";
        if(unitID.startsWith("BLACKDOG"))return "AV";
        
        // this is a hack to work with IABG KORA
        if(unitID.contains("PGB"))return "RECCE";
        
        return "NKN";
    }
 
    /**
     * Layer overrides (multiple methods)
     */

    public void paint(Graphics g) {
        omgraphics.render(g);
    }

    public MapMouseListener getMapMouseListener() {
        return this;
    }

    /**
     * ProjectionListener interface implementation 
     */
    public void projectionChanged(ProjectionEvent e) {
        projection = (Projection) e.getProjection().makeClone();
        omgraphics.setProjection(projection);
        repaint();
    }

    /**   
     * MapMouseListener interface implementation (multiple methods)
     * 
     * @see com.bbn.openmap.*
     * @see com.bbn.openmap.event.*
     */
    public String[] getMouseModeServiceList() {
        String[] ret = new String[1];
        ret[0] = SelectMouseMode.modeID; // "Gestures"
        return ret;
    }

    public boolean mousePressed(MouseEvent e) {
    	mouseButtonDown=true;
       
        // if in process of selecting a report, export the coordinates
        if(bml.loadingReport) {
            LatLonPoint llp = bml.mapBean.getCoordinates(e);
            if(llp != null)
                bml.setReportCoordinates(llp.getLatitude(), llp.getLongitude());
            bml.loadReportButton.setText("LOADING REPORT");
        }
        
        // if in process of getting map coordinates, export the coordinates
        if(bml.debugMode)bml.printDebug("received mouseclick "+bml.enteringCoords);
        if(bml.enteringCoords && (bml.transferIndex >= bml.coordIndex))return false;
        if(bml.gettingCoords || bml.enteringCoords) {
            LatLonPoint llp = bml.mapBean.getCoordinates(e);
            if(llp != null)
                bml.setCoordinates(llp.getLatitude(), llp.getLongitude());
            if(bml.gettingCoords)
                bml.getCoordsButton.setText("GETTING COORDS");
            if(bml.enteringCoords)
                bml.movingCoords = false;
        }
        
// .    previous code:
//    	selectedPoint = omgraphics.getGrabPoint(e);
//    	if (selectedPoint!=null) {
//            selectedPoint.select();
//    	}
        return false;
    }

    public boolean mouseReleased(MouseEvent e) {
    	mouseButtonDown=false;
    	if (selectedPoint!=null) {
    		selectedPoint.set(e.getX(), e.getY());
        	EditableOMGraphic g = omgraphics.getSelectedGraphic();
    		g.setGrabPoints();
    	}
    	bml.mapBean.setScale(bml.mapBean.getScale());    	
        return false;
    }
    
    // visits the mapBean scale which causes rendering
    private void visitScale(){
        bml.mapBean.setScale(bml.mapBean.getScale());
    }

    public boolean mouseClicked(MouseEvent e) {
//        selectedGraphic = omgraphics.selectClosest(e.getX(),
//                e.getY(),
//                10.0f);
//    	selectedPoint = omgraphics._getMovingPoint(e);
        if (selectedGraphic != null) {
            switch (e.getClickCount()) {
            case 1:
//            if(bml.debugMode)bml.printDebug("Show Info: " + 
//              ((OMTextLabeler)selectedGraphic.getAttribute(OMGraphic.LABEL)).getData());
              break;
            case 2:
//            if(bml.debugMode)bml.printDebug("Request URL: " + selectedGraphic);
              break;
            default:
              break;
            }
            return true;
        } else {
            return false;
        }
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public boolean mouseDragged(MouseEvent e) {
//    	if(bml.debugMode)bml.printDebug("RouteLayer::MouseDragged to " + 
//          projection.inverse(e.getPoint()));
    	return false;
    }

    public boolean mouseMoved(MouseEvent e) {
//    	selectedPoint = omgraphics.getGrabPoint(e);
//    	if (selectedPoint!=null) {
//          selectedPoint.select();
//          if(bml.debugMode)bml.printDebug("RouteLayer::MouseMoved selected: " + 
//              selectedPoint.getDescription());
//    	}
    	
        return true;
    }

    public void mouseMoved() {
//    	if (!mouseButtonDown) {
//    		omgraphics.deselectAll();
//    	}
        repaint();
    }
    
    public String getSelectedObject() {
    	EditableOMGraphic g = omgraphics.getSelectedGraphic();
    	if (g!=null){
            selectedGraphic = g.getGraphic();
            return ((OMTextLabeler)selectedGraphic.getAttribute(OMGraphic.LABEL)).getData();
    	}
    	else return null;
    }
    
    public int getSelectedPointIndex() {
    	return omgraphics.getSelectedPointIndex();
    }
    
    public LatLonPoint getProjectedPoint(Point e) {
    	return projection.inverse(e);
    }
    
    /**
     * Prints formatted output
     */    
    public void whereOutput(String[] whereOut){ 
    	if(bml.debugMode)bml.printDebug(" WHERE Type  : " + whereOut[0] +
                         "\n WHERE Class : " + whereOut[1] +
                         "\n WHERE Label : " + whereOut[2]);
      if(whereOut.length >3)
        if(bml.debugMode)bml.printDebug(" UNITID      : " + whereOut[3]);
    }
    
    /**
     * Draws IBML_CBML_OPORD Graphics. Clears and then fills the given OMGraphicList.
     * 
     * Start parsing the Array representing the XML document
     * First: create a description array
     * Second: use the description array to get each graphics type
     * Third: call the appropriate drawing method (Point, Line, Area)
     *
     * @param graphics The OMGraphicList to clear and populate
     * @return the graphics list, after being cleared and filled
     */

    /**
     * Draw order graphics. Clears and then fills the given OMGraphicList. 
     * 
     * Start parsing the Array representing the XML document
     * First: create a description array
     * Second: use the description array to get each graphics type
     * Third: call the appropriate drawing method (Point, Line, Area)
     * If it is an area then the last and first points should be the same
     *
     * @param graphics The OMGraphicList to clear and populate
     * @return the graphics list, after being cleared and filled
     */
    public ListEditableOMGraphics createOrderGraphics(ListEditableOMGraphics graphics) {
        graphics.clear();
        boolean foundOID = false;// to accept only first OID from an AtWhere or RouteWhere
        boolean foundLat = false;// to determine if the location includes coordinates 
        int locationCount = 0;	 // number of locations
        int startGDC = 0;  	 // start index in the array of the location
        int endGDC = 0;	  	 // end index in the array of the location
        int lengthGDC = 0; 	 // length of the location 
        int noOfPointGDC = 0;    // number of points in the location. 
                                 // If it is one then it is a point
                                 // if it is more than one then it is a line or an area
                                 // the line differs from the area in that the area has 
                                 // the same last and first points

        if(bml.debugMode)bml.printDebug("processing C-BML Order to map display");

        // count the Locations in the stringArray
        // use to create storage for building graphics
        int numberOfLocations = 0;
        for (int i = 0; i < stringArray.length; i++){	
          if(stringArray[i].equals("Task") ||
            stringArray[i].equals("ControlMeasure"))
            ++numberOfLocations;
        }
        if(numberOfLocations == 0){
          bml.printError("C2SIMGUI graphics processing found no C-BML Tasks to process");
          return null;
        }
        if(bml.debugMode)bml.printDebug(
            "in createOrderGraphics (C-BML) numberOfLocations=" + numberOfLocations);

        // number , startGDC, endGDC, lengthGDC, noOfPointGDC - control measures
        int [][] descArray = new int [numberOfLocations][5];

        // arrays to save TaskID so as to display unit icon
        // after ControlMeasures have been ingested
        String[] taskUnitID = new String[numberOfLocations];
        float[] taskUnitIDLat = new float[numberOfLocations];
        float[] taskUnitIDLon = new float[numberOfLocations];
        String[] whereID = new String[numberOfLocations];
        boolean[] isControlMeasure = new boolean[numberOfLocations];

        // WhereClass, WhereLabel, WhereQualifier, UnitID 
        String[][] strDescArray = new String [numberOfLocations][5];	
        for(int j=0; j<5; ++j)strDescArray[0][j] = "";
        isControlMeasure[0] = false;
	
        // Parsing an order string and drawing location information 

        if(bml.debugMode)bml.printDebug("============   Drawing CBML Order   ==================");
        for (int i = 0; i < stringArray.length; i++){	
    
            // Look for location Information that starts a Task
            if (bml.cbmlTagCompare(stringArray[i],"Latitude") &&
                startGDC == 0) {
                if(isControlMeasure[locationCount])
                    if(bml.debugMode)bml.printDebug(
                        "A ControlMeasure Location has been discovered #: " + 
                        locationCount + " coordinates start at : " + i);
                else
                    if(bml.debugMode)bml.printDebug("A Task Location has been discovered #: " + 
                locationCount + " coordinates start at : " + i);
        
                // initialize for the location
                startGDC = i;
                foundLat = true;
            }

            // Storing the latest text desc in strDescArray
            if(bml.cbmlTagCompare(stringArray[i],"PointLight"))
                strDescArray[locationCount][0] = "PT";
            if(bml.cbmlTagCompare(stringArray[i],"Line"))
                strDescArray[locationCount][0] = "LN";
            if(bml.cbmlTagCompare(stringArray[i],"Surface"))
                strDescArray[locationCount][0] = "SURFAC";
            if(bml.cbmlTagCompare(stringArray[i],"SpecificRoute"))
                // type: AREA, LINE, POINT 
                strDescArray[locationCount][2] = "ROUTE";
            if (bml.cbmlTagCompare(stringArray[i],"TaskeeWhoRef")) 
                // UnitID
                strDescArray[locationCount][3] = stringArray[i+1];
            if (bml.cbmlTagCompare(stringArray[i],"OID")){
                String oidData = stringArray[i+1];
                if(!foundOID)
                    // identifies the Where
                    strDescArray[locationCount][4] = stringArray[i+1];
                foundOID = true;
            }
            // look for end of lat/lon = start of next 
            // GroundTask, AirTask or ControlMeasure
            if ( ((i == stringArray.length-1) ||
                // look for end of lat/lon = start of next 
                // GroundTask, AirTask or ControlMeasure
                bml.cbmlTagCompare(stringArray[i],"Task") ||
                bml.cbmlTagCompare(stringArray[i],"ControlMeasure"))){

                if(startGDC > 0 && startGDC < i && foundLat) {

                // there are coordinates to be captured
                endGDC = i;
                if(i == stringArray.length-1)++endGDC;
                lengthGDC = (endGDC - startGDC);
                noOfPointGDC = (endGDC - startGDC)/4;
                if(bml.debugMode)bml.printDebug(
                    "  It ends at : " + i + " its Length is: " + 
                    lengthGDC + " No of Points is : " + noOfPointGDC);
            } else {

                // no coordinates to capture
                startGDC = 0;
                endGDC = 0;
                lengthGDC = 0;
                noOfPointGDC = 0;
                if(bml.debugMode)bml.printDebug("Discovered a Location #:" + locationCount +
                    " with no coordinates; OID:" + strDescArray[locationCount][4]);
            }// end else 

            // special case for first location
            if(i == 0)
                isControlMeasure[0] = 
                    bml.cbmlTagCompare(stringArray[i],"ControlMeasure");

            // capturing parameters in array for drawing
            descArray[locationCount][0]=locationCount;
            descArray[locationCount][1]=startGDC;
            descArray[locationCount][2]=endGDC;
            descArray[locationCount][3]=lengthGDC;
            descArray[locationCount][4]=noOfPointGDC;

            // initialize for next location
            startGDC = 0;
            foundOID = false;
            foundLat = false;
            if(i > 0)locationCount++;
            if(locationCount < numberOfLocations) {
                for(int j=0; j<5; ++j)strDescArray[locationCount][j] = "";
                isControlMeasure[locationCount] =
                    bml.cbmlTagCompare(stringArray[i],"ControlMeasure");
            }
          }// end if(((i
        }// end for i  
		
        int latLocation = 0, lonLocation = 0;
        int tempArraySize = 0;
        if(bml.debugMode)bml.printDebug(" locationCount   : " + locationCount );

        // generating graphics for each location previously captured
        for (int i=0; i < locationCount ; i++){

            // creating a temp array for each graphical location
            // the length of the array will be twice as the number of lat,lon points	
            //Area : the last lat,;on should be the same lat,lon
            tempArraySize = (descArray[i][4])*2;// noOfPointGDC	
            if (strDescArray[i][0].trim().equals("Line")){
                if(bml.debugMode)bml.printDebug(" Reading a line  Info " );
            }
            else if (strDescArray[i][0].trim().equals("PointLight")){
                if(bml.debugMode)bml.printDebug(" Reading a point  Info " );
            }
            else if (strDescArray[i][0].trim().equals("Surface") ||
                strDescArray[i][0].trim().equals("Surface")){				
                // An area
                if(bml.debugMode)bml.printDebug(" Reading a Surface Info " );
                tempArraySize += 2;	
            }
            if(bml.debugMode)bml.printDebug(" tempArraySize = " + tempArraySize  );
            tempArray = new float[tempArraySize];
            int tempArrayIndex = 0;

            // creating a temp array for each graphical location
            float iconLat = 0f, iconLon = 0f;
            for (int h = 0; h < descArray[i][4]; h++) {							
                latLocation = (descArray[i][1])+(1+(4*h));
                lonLocation = (descArray[i][1])+(3+(4*h));
                tempArray[tempArrayIndex]= 
                    stringToFloat(stringArray[latLocation].trim());
                if(bml.debugMode)bml.printDebug(" tempArray["+tempArrayIndex+"] " + 
                    tempArray[tempArrayIndex] );
                tempArray[tempArrayIndex+1]= 
                    stringToFloat(stringArray[lonLocation].trim());
                if(bml.debugMode)bml.printDebug(" tempArray["+(tempArrayIndex+1)+"] " + 
                    tempArray[tempArrayIndex+1] );
                if(h == 0) {
                  iconLat = tempArray[tempArrayIndex];
                  iconLon = tempArray[tempArrayIndex+1];
                }
                tempArrayIndex += 2;
        
            } // end for h

            // Auto Center
            if(tempArraySize >1)
                scaleAndCenterIfEmpty(tempArray[0], tempArray[1], 6000000f);

            // OID is WhereLabel
            String featureLabel = strDescArray[i][1];
            featureLabel = strDescArray[i][4];
      
            // draw whatever shape the Location calls for	
            if (strDescArray[i][0].trim().equals("LN")){	
                if(bml.debugMode)bml.printDebug(" Drawing a Line Info " + strDescArray[i][2]);
				
                // the shape is a Route, so draw a Line between each pair of points 
                // the last point and the first one are not the same
                this.add(
                    featureLabel, 
                    createPolyLine(tempArray, 
                    OMGraphic.DECIMAL_DEGREES, 
                    OMGraphic.DECLUTTERTYPE_SPACE));
            }
			
            // if the location object is a surface
            // then add a point at the end of tempArray that has the values of the first point
            // Draw a Polygon with the last point and the first one are the same
            if (strDescArray[i][0].trim().equals("SURFAC")){
                if(bml.debugMode)bml.printDebug(" Drawing  Surface  " + strDescArray[i][2]);
                tempArray[tempArrayIndex]=tempArray[0];	
                tempArray[tempArrayIndex+1]=tempArray[1];	
                tempArrayIndex += 2;

                // the shape is not a point so draw a Poly
                graphics.add(featureLabel, createPoly(tempArray, 0, 1));
            }

            // if the object is a point call the draw point method
            if (tempArray.length == 2) {
                if(bml.debugMode)bml.printDebug(" Drawing Point Info " + strDescArray[i][2]);
                latOpen = tempArray[0];
                lonOpen = tempArray[1];
                this.add(featureLabel, createPoint(Color.red));
            }

            // save data to add unit icon later
            whereID[i] = strDescArray[i][4];
            taskUnitID[i] = strDescArray[i][3];
            if(!isControlMeasure[i] && descArray[i][4] == 0) {

                // Task has no coordinates - 
                // only WhereID
                iconLat = 0f;
                iconLon = 0f;
            } 
            taskUnitIDLat[i] = iconLat;
            taskUnitIDLon[i] = iconLon;
            if(whereID.equals("") && iconLat == 0f && iconLon == 0f)
                bml.printError(
                    "order has Task with neither WhereID or Latitude/Longitude");
		
        } // end for i processing location information of an Order
  
        // add an icon for each unit based on its
        // control measure; lat/lon could be in
        // Task or references to ControlMeasure
        for(int i = 0; i < numberOfLocations; ++i) {
            if(isControlMeasure[i])continue;
            String unitID = taskUnitID[i];
            if (!unitID.equals("")){
                String icon = icon2525b(unitID, "FR");
                latOpen = taskUnitIDLat[i];
                lonOpen = taskUnitIDLon[i];

                // if the Task had WhereID not lat/lon
                if(latOpen == 0f && lonOpen == 0f) {
                    int j = 0;
                    for(j = 0; j < numberOfLocations; ++j){
                      if(whereID[j].equals(whereID[i]) &&
                        isControlMeasure[j])break;
                    }
                    if(j >= numberOfLocations) {
                        bml.printError(
                            "unable to match Task WhereID:" + whereID[i] +
                            " with a ControlMeasure to draw unit icon");
                        break;
                    }
                    latOpen = taskUnitIDLat[j];
                    lonOpen = taskUnitIDLon[j];
                }// end if(latOpen

                // add the icon to display
                addIcon("     " + unitID, icon);

                // remove any additional instances of this UnitID
                // so we don't add it to map more than once
                // TODO: ideally we would post icon for the Task 
                // with lowest DateTime
                for(int k = i+1; k < numberOfLocations; ++k) {
                    if(taskUnitID[k].equals(taskUnitID[i]) && !isControlMeasure[k])
                        taskUnitID[k] = "";
                }
            }// end if (!unitID
        }// end for(int i
        
        return graphics;
    } // end createOrderGraphics()

} // end class RouteLayers