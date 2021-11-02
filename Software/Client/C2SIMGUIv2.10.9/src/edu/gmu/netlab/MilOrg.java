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

import static edu.gmu.netlab.Subscriber.bml;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * parses C2SIMInitializationBody and saves results in this class
 * @author mpullen
 */
public class MilOrg {
    
    static C2SIMGUI bml = C2SIMGUI.bml;
    
    // MessageBody
    // (public, to avoid need for getters)
    //String id;
    String uuid;
    String name;
    //String healthStatus;
    //String operationalStatusCode = "";
    //String strengthPercentage;
    String hostilityCode;
    //String echelon;
    //String superiorUnit;
    String latitude;
    String longitude;
    //String elevationAGL;
    String symbolIdentifier;
    //String forceSide;
    String forceSideUuid = "";
    
    // our ForceSide properties
    String blueForceSideName = "";
    String blueForceSideUuid = "";
    String blueForceSideOtherSide1HostilityCode = "";
    String blueForceSideOtherSide1Uuid = "";
    String blueForceSideOtherSide2HostilityCode = "";
    String blueForceSideOtherSide2Uuid = "";
    
    // constructor
    public MilOrg(){};
    
    // set/reset data values - must be called
    // first time immediately after class instantiation
    void setValues(
        //String idValue,
        String uuidValue,
        String nameValue,
        //String healthStatusValue,
        //String operationalStatusCodeValue,
        //String strengthPercentageValue,
        String hostilityCodeValue,
        //String echelonValue,
        //String superiorUnitValue,
        String latitudeValue,
        String longitudeValue,
        //String elevationAGLValue,
        String symbolIdentifierValue
        //String forceSideValue
        )
    {
        uuid = uuidValue;
        //id = idValue;
        name = nameValue;
        //healthStatus = healthStatusValue;
        //operationalStatusCode = operationalStatusCodeValue;
        //strengthPercentage = strengthPercentageValue;
        hostilityCode = hostilityCodeValue;
        //echelon = echelonValue;
        //superiorUnit = superiorUnitValue;
        latitude = latitudeValue;
        longitude = longitudeValue;
        //elevationAGL = elevationAGLValue;
        symbolIdentifier = symbolIdentifierValue;
        //forceSide = forceSideValue;
    }// end setValues()
    
    void setUuid(String uuidRef){
        uuid = uuidRef;
    }
 
    void setName(String nameRef){
        name = nameRef;
    }
    
    void setLatitude(String latitudeRef){
        latitude = latitudeRef;
    }
    
    void setLongitude(String longitudeRef){
        longitude = longitudeRef;
    }
    
    void setSymbolIdentifier(String symbolIdRef){
        symbolIdentifier = symbolIdRef;
    }
    
    void setHostility(String hostilityCodeRef){
        hostilityCode = hostilityCodeRef;
    }
    
    String getName(){
        return name;
    }
    
    String getLatitude(){
        return latitude;
    }
    
    String getLongitude(){
        return longitude;
    }
    
    String getSymbolIdentifier(){
        return symbolIdentifier;
    }
    
    String getHostility(){
        return hostilityCode;
    }
    
    // parse a C2SIMInitializationBody - count the entities and
    // extract for each its Name, UUID and SymbolID, Lat and Lon
    // returns entityCount in the message
    // we presume the XML does not use prefixes
    int parseC2SIMInit(String messageBody) {

        // read the C2SIMInitializationBody and save it
        // for use in mapping the unit
        int entityCount = 0;
        MilOrg milOrg;
        String uuid = "";
        HashMap<String,String> hostilityMap = new HashMap<>();
        
        // capture a copy of the input
        /*
        try{
            (new File("initcopy.xml")).delete();
            BufferedWriter out = new BufferedWriter(
                new FileWriter("initcopy.xml"));
            out.write(messageBody);
            out.close();
        }
        catch(IOException ioe){
            bml.printError("IOException capturing initialization XML:" +
                ioe.getMessage());
        }
        */
        // pull out the friendly and hostile sides 
        // the name of BLUE ForceSide could be in Config 
        // bml.blueSideName; if that is empty, assume the 
        // first ForceSide in the schema
        //
        // if bml.blueSideName contains a name, locate the
        // AbstractObject containing that ForceSide -
        // make a copy of messageBody and scan through all 
        // its AbstractObjects to find one with Name matching
        // bml.blueSideName; also get first ForceSide name
        String parseMessage = messageBody;
        String abstract1 = extractC2simData(parseMessage,"AbstractObject",true);
        if(abstract1.equals(""))return -1;
        parseMessage = parseMessage.substring(
            c2simTagIndexOf(parseMessage,"</AbstractObject>")+17);
        
        // nibble away at parseMessage to get possible second and third Abstract
        String abstract2 = "";
        abstract2 = extractC2simData(parseMessage,"AbstractObject",false);
        String abstract3 = "";
        if(!abstract2.equals("")){
            parseMessage = parseMessage.substring(
                c2simTagIndexOf(parseMessage,"</AbstractObject>")+17);
            abstract3 = extractC2simData(parseMessage,"AbstractObject",false);
        }

        // look first for new 1.0.0 AbstractObject format
        // unless bml.blueSideName is configured the first one is Blue
        if(abstract1.contains("<HostilityStatusCode>") || abstract1.contains("<OtherSide>")){
            
            // v1.0.0 case
            parseMessage = messageBody;
            String thisAbstract = "", sideName = "", firstName = "";
            while(c2simTagIndexOf(parseMessage,"<AbstractObject>") >= 0){
                thisAbstract = extractC2simData(parseMessage,"AbstractObject",false);
                sideName = extractC2simData(thisAbstract,"Name", false);
                if(firstName.equals(""))firstName = sideName;
                if(bml.blueSideName.length() > 0 && sideName.equals(bml.blueSideName))break;
                parseMessage = parseMessage.substring(
                    c2simTagIndexOf(parseMessage,"</AbstractObject>")+17);
            }

            // if we did not find a name matching blueSideName
            // start over with the first AbstractObject
            if(sideName.equals(bml.blueSideName)){
                blueForceSideName = sideName;
                parseMessage = thisAbstract;
            }
            else {
                blueForceSideName = firstName;
                parseMessage = messageBody;
                parseMessage = extractC2simData(parseMessage,"AbstractObject",true);
            }
            blueForceSideUuid = extractC2simData(parseMessage,"UUID", true);

            // now parseMessage holds contents of either the first 
            // AbstractObject or the AbstractObject containing bml.blueSideName  
            // but without start and end tags

            // now we need to extract the other one or two ForceSide's uuid and hostility
            blueForceSideOtherSide1Uuid = extractC2simData(parseMessage,"OtherSide",true);
            blueForceSideOtherSide1HostilityCode = 
                extractC2simData(parseMessage,"HostilityStatusCode",true);
            parseMessage = parseMessage.substring(c2simTagIndexOf(
                parseMessage,"</ForceSideRelation>")+7);
            blueForceSideOtherSide2Uuid = extractC2simData(parseMessage,"OtherSide",false);
            blueForceSideOtherSide2HostilityCode = 
                extractC2simData(parseMessage,"HostilityStatusCode",false);
            System.out.println("HOSTILITY:"+blueForceSideOtherSide1HostilityCode+"|"+
                   blueForceSideOtherSide2Uuid+"|" );
        }
        else {// the old C2SIMv9 version
            
            // if bml.blueSideName is not configured take the first AbstractObject name
            String blueAbstract = abstract1, otherAbstractA = "", otherAbstractB = ""; 
            String name2 = "", name3 = "", name1 = extractC2simData(abstract1,"Name", true);
            if(name1.equals(""))return -1;
            if(bml.blueSideName.equals(""))blueForceSideName = name1;
            else {
                // otherwise we must compare all three AbstractObject names
                if(name1.equals(bml.blueSideName)){
                    blueForceSideName = name1;
                    otherAbstractA = abstract2;
                    otherAbstractB = abstract3;
                }
                else {
                    if(!abstract2.equals(""))name2 = extractC2simData(abstract2,"Name", true);
                    if(name2.equals(""))return -1;
                    if(name2.equals(bml.blueSideName)){
                        blueForceSideName = name;
                        blueAbstract = abstract2;
                        otherAbstractA = abstract1;
                        otherAbstractB = abstract3;
                    }
                    else if(!abstract3.equals("")){
                        name3 = extractC2simData(abstract3,"Name", true);
                        if(name3.equals(""))return -1;
                        if(name3.equals(bml.blueSideName)){
                            blueForceSideName = name;
                            blueAbstract = abstract3;
                        otherAbstractA = abstract1;
                        otherAbstractB = abstract2;
                        }
                    }
                }
                
                // check that we found a Blue name and AbstractObject
                if(blueForceSideName.equals("") || blueAbstract.equals("")){
                    bml.showErrorPopup(
                        "missing AbstractObject data",
                        "initialization error");
                    return -1;
                }
                
                // get the ForceSide UUID and Hostility for the other one or two
                if(!otherAbstractA.equals("")){
                    blueForceSideOtherSide1Uuid = extractC2simData(otherAbstractA,"UUID",true);
                    if(blueForceSideOtherSide1Uuid.equals(""))return -1;
                    blueForceSideOtherSide1HostilityCode = 
                        extractC2simData(otherAbstractA,"SideHostilityCode",true);
                    if(blueForceSideOtherSide1HostilityCode.equals(""))return -1;
                }
               if(!otherAbstractB.equals("")){
                    blueForceSideOtherSide1Uuid = extractC2simData(otherAbstractB,"UUID",true);
                    if(blueForceSideOtherSide1Uuid.equals(""))return -1;
                    blueForceSideOtherSide1HostilityCode = 
                        extractC2simData(otherAbstractB,"SideHostilityCode",true);
                    if(blueForceSideOtherSide1HostilityCode.equals(""))return -1;
                }
                
            }// end else/if(bml.bluesideName
        }// end if(abstract1.contains(
        
        // load our data into the hostility Map
        hostilityMap.put(blueForceSideUuid, "FR");
        hostilityMap.put(blueForceSideOtherSide1Uuid,blueForceSideOtherSide1HostilityCode);
        hostilityMap.put(blueForceSideOtherSide2Uuid,blueForceSideOtherSide2HostilityCode);
  
        // parse out the MilOrg data we want
        parseMessage = messageBody;
        int startEntity = c2simTagIndexOf(parseMessage,"<Entity>")+8;
        while(startEntity > 8) {// 8 = length of "<Entity>"

            // strip off message text up to next Entity tag
            parseMessage = parseMessage.substring(startEntity);
            int endEntity = c2simTagIndexOf(parseMessage,"</Entity>");
            milOrg = new MilOrg();

            // collect MilOrg data
            
            // Latitude
            String latitude = extractC2simData(parseMessage,"Latitude",false);
            
            // Longitude
            String longitude = extractC2simData(parseMessage,"Longitude",false);

            // UUID
            uuid = extractC2simData(parseMessage,"UUID",false);
            milOrg.setUuid(uuid);

            // Name
            String name  = extractC2simData(parseMessage,"Name",false);
            milOrg.setName(name);
            milOrg.setLatitude(latitude);
            milOrg.setLongitude(longitude);

            // Hostility - get Side then pull from hostilityMap
            String side = extractC2simData(parseMessage,"Side",false);
            String hostility = hostilityMap.get((Object)side);
            if(hostility == null)hostility = "UNK";
            milOrg.setHostility(hostility);

            // Symbol Identifier
            String symbolIdentifier = extractC2simData(parseMessage,"SIDCString",false);
            milOrg.setSymbolIdentifier(symbolIdentifier);
      
            // if we didn't get hostility from initialization message,
            // take it from the symbolIdentifier
            if(symbolIdentifier.length() != 15){
                bml.printError("bad SIDC String |" + symbolIdentifier +
                    "| for UnitID:" + uuid);
                symbolIdentifier = "***************";   
            }
            else if(hostility.equals("UNK")){
                String hostilityChar = symbolIdentifier.substring(1,2); 
                if(hostilityChar.equals("F"))hostility = "FR";
                if(hostilityChar.equals("H"))hostility = "HO";
                milOrg.setHostility(hostility);
            }

            // add it to the hashmap and graphic map
            bml.addUnit(milOrg.uuid, milOrg);
            if(uuid.length() > 0 && name.length() > 0 && hostility.length() > 0 &&
                latitude.length() > 0 && longitude.length() > 0)
                bml.drawLocation(null, null, null, -2, -2, milOrg);

            // move to next Entity
            startEntity = c2simTagIndexOf(parseMessage,"<Entity>",endEntity)+8;
            entityCount++;

        }// end while(startName > 0) 
              
        bml.initStatusLabel.setText(Integer.toString(entityCount));
        return entityCount;
        
    }// end parseC2SIMInit()
    
    /**
     * performs String.indexOf function for C2SIM XML tag
     * with and without the c2sim namespace prefix
     * assumed the tags are direct from XML e.g. <ns:tag>
     */
    int c2simTagIndexOf(String toSearch, String lookFor, int startAt){
        if(startAt < 0)return -1;
        int partialIndex = c2simTagIndexOf(toSearch.substring(startAt),lookFor);
        if(partialIndex < 0)return -1;
        int result = partialIndex + startAt;
        return result;
    }
    int c2simTagIndexOf(String toSearch, String lookFor){
        int index = toSearch.indexOf(lookFor);
        if(index >=0)return index;
        if(lookFor.startsWith("</"))
            return toSearch.indexOf("</" + bml.c2simns + lookFor.substring(2));
        return toSearch.indexOf("<" + bml.c2simns + lookFor.substring(1));
    }
    
    /**
     * extracts data from a terminal element given
     * string to parse and tagname (no < >)to seek, assuming the
     * tag may have a prefix of bml.c2simns
     * returns "" if tags not found in order
     */
    String extractC2simData(String parseString, String tag, boolean required){
        
        // locate beginning index of data
        String withPrefix = "<" + bml.c2simns + tag + ">";
        String withoutPrefix = "<" + tag + ">";
        int startTag = parseString.indexOf(withoutPrefix);
        int startData = startTag + withoutPrefix.length();
        if(startTag < 0){
            startTag = parseString.indexOf(withPrefix);
            startData = startTag + withPrefix.length();
        }
        if(startTag < 0){
            if(required)bml.showErrorPopup(
                "missing tag for " + tag,
                "initialization error");
            return "";
        }
        
        // locate end index of data
        withPrefix = "</" +  bml.c2simns + tag + ">";
        withoutPrefix = "</" + tag + ">";
        int endTag = parseString.indexOf(withoutPrefix);
        if(endTag < 0)endTag = parseString.indexOf(withPrefix);
        if(endTag < startTag){// include endTag < 0
            if(required)bml.showErrorPopup(
                "initialization error",
                "missing tag for " + tag);
            return "";
        }
        
        // extract and return data
        String result = parseString.substring(startData, endTag);
        if(bml.debugMode)bml.printDebug("extracted result:" + result + "|");
        return result;
    
    }// end extractC2simData()
    
}// end class MilOrg
