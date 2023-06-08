/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gmu.netlab;

import java.util.ArrayList;
/**
 * combine GeodeticCoordinates for one point in a PhysicalRoute
 * @author JMP
 */
 public class PhysicalLocation {
     
    String latitude;
    String longitude;
    String elevation;
    public PhysicalLocation(
        String lat,
        String lon,
        String el) {
        latitude = lat;
        longitude = lon;
        elevation = el;
    }
}// end class PhysicalLocation


