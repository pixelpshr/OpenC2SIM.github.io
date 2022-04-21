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

import java.util.HashMap;
import java.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;


/**
 * <h1>C2SIM_InitDB</h1> <p>
 * Stores C2SIM Initialization Data

 * @author Douglas Corner - George Mason University C4I and  Center
 */

public class C2SIM_InitDB {

    HashMap<String, Document> cwixUnits;
    Vector<InitElement> initDataFile;
    Vector<InitElement> abstractObject;
    Vector<InitElement> action;
    Vector<InitElement> entity;
    Vector<InitElement> plan;
    Vector<InitElement> systemEntityList;
    Vector<InitElement> scenario;
    Vector<InitElement> unit;
    Vector<InitElement> route;

    C2SIM_InitDB() {
        
        initDataFile = new Vector<>();
        abstractObject = new Vector<>();
        action = new Vector<>();
        entity = new Vector<>();
        plan = new Vector<>();
        systemEntityList = new Vector<>();
        scenario = new Vector<>();
        
    }   // C2SIM_InitDB Constructor

    /**
    * Internal Class defining a structure for containing individual elements
    */
    public static class InitElement {

        Element element;    // The top level of the initialization element
        String type;
        Element position;       // Reference to Entity physical position within document
        String source;         // Host submitting this entity
        String location;       // Simulator Host responsible for this entity

    }   // initElements class    

}   // C2SIM_InitDB Class
