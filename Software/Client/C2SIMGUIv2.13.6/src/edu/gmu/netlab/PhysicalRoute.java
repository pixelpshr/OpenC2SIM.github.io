/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gmu.netlab;

import java.util.ArrayList;

/**
 * collect sequence of locations for a 
 * <PhysicalEntity><MapGraphic>...<Location><GeodeticCoordinate>
 * @author JMP
 */
class PhysicalRoute {

    public PhysicalRoute(String newRoutename){
        routeUuid = newRoutename;
        locations = new ArrayList<PhysicalLocation>();
    }
    ArrayList<PhysicalLocation> locations;
    String routeUuid;

    // getters to extract values
    String getUuid() {
        return routeUuid;
    }
    int getSize() {
        return locations.size();
    }
    String getLatitude(int index) {
        return locations.get(index).latitude;
    }
    String getLongitude(int index) {
        return locations.get(index).longitude;
    }
    String getElevation(int index) {
        return locations.get(index).latitude;
    }
    String getLocation(int index) {
        return " Latitude:" + getLatitude(index) +
               " Longitude:" + getLongitude(index) +
               " ElevationMSL:" + getElevation(index);
    }

    // setter to populate the ArrayList
    // returns index of PhysicalLocation inserted
    int insertLocation(PhysicalLocation physLoc) {
        locations.add(physLoc);
        return locations.size();
    }
}// end class PhysicalRoute
