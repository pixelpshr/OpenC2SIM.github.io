/*
 * translate C2SIM 0.0.9 to C2SIM 1.0.0 and return
 *
 * this class does 0.0.9 Initialization to 1.0.0
 *
 * we make no attempt here to cover full schemas; only the subset
 * used in CWIX 2019 since no further 0.0.9 implementations are
 * anticipated*
 */
package edu.gmu.c4i.c2simserver4.c2simserver;

/**
 * @author jmarkpullen
 */
public class Translate009Init {
    
    String inputXml;
    String outputXml = "";
    
    // this will be the tag immediately below <MessageBody> in the outputXml
    String bodyTag = "<C2SIMInitializationBody>";
    
    /**
     * in comments below 009 is C2SIM 0.0.9 and 100 is C2SIM 1.0.0
     * 
     * inputXMl is the String to be translated
     * 
     * outputXml will not have prefixes; we delete them while
     * removing whitespace
     * 
     * there are purposely several methods here with side-effects;
     * they consume inputXml and build up outptuXml
     * 
     * Supporting functions are repeated in the various C2SIM
     * 009 to 100 classes, to retain full modularity for server use.
     */
        
    /**
     * returns inputXML translated from C2SIM 0.0.9 to C2SIM 1.0.0
     */
    String translate(String xml){
 
        // pack out whitespace so our edits work
        inputXml = removeWhitespace(xml);
       
        // determine which body tag to use
        if(inputXml.contains("<ObjectInitialization>"))
            bodyTag = "<ObjectInitializationBody>";
        
        // XML prolog up to <AbtractObject>
        outputXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<MessageBody xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://www.sisostds.org/schemas/C2SIM/1.1 " +
            "C2SIM_SMX_LOX_v1.0.0.xsd\"" +
            " xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" + bodyTag +
            "<ObjectDefinitions>";
        
        // strip off down to the AbstractObject we need to edit
        int preambleEnd = inputXml.indexOf("<AbstractObject>");
        inputXml = inputXml.substring(preambleEnd);     

        // edit <AbstractObject>
        editAbsObj(); 
        
        // scan through each Entity editing them
        while(editEntity());
        
        // move  <PlanPhaseReference> over and close out <ObjectDefinitions>
        outputXml += removeChunk("<PlanPhaseReference>","</PlanPhaseReference>");
        outputXml += removeChunk("</ObjectDefinitions>","</ObjectDefinitions>");
        
        // capture <ScenarioSetting> and move to outputXml
        String scenarioSetting = removeChunk("<ScenarioSetting>","</ScenarioSetting>");
        String ssDateTime = extractValue(scenarioSetting,"<IsoDateTime>");
        outputXml += 
            "<ScenarioSetting><DateTime><IsoDateTime>" + ssDateTime + 
            "</IsoDateTime></DateTime><Version>1.0.0</Version></ScenarioSetting>";
        
        String systemEntityList = removeChunk("<SystemEntityList>","</SystemEntityList>");
        while(!systemEntityList.equals("")){
            outputXml += systemEntityList;
            systemEntityList = removeChunk("<SystemEntityList>","</SystemEntityList>");
        }
        
        // last bit to add
        outputXml += makeEndTag(bodyTag) + "</MessageBody>";
        
        return outputXml;
        
    }// end translateInit009()
    
    // methods to restructure 009 into 100
    
    /**
     * pull all 009 AbstractObject from inputXml;
     * reassemble in 100 and add to outputXML
     * 
     * we expect three ForcesSides; if less than that the other two 
     * will be empty of data; we don't handle more
     */
    void editAbsObj(){
        
        // from CWIX 2019 there can be up to three AbstractObjects in 009
        // need all three of them to form the 100 version
        String name1 = "", name2 = "", name3 = "";
        String uuid1 = "", uuid2 = "", uuid3 = "";
        String hostility1 = "", hostility2 = "", hostility3 = "";
 
        // pull a chunk from 009 inputXML and extract its data values
        String absObj1 = removeChunk("<AbstractObject>","</AbstractObject>");
        if(absObj1.equals("")){
            System.err.println("****** can't find any AbstractObject");
            return;
        }
        name1 = extractValue(absObj1,"<Name>");
        uuid1 = extractValue(absObj1,"<UUID>");
        hostility1 = extractValue(absObj1,"<SideHostilityCode>");
      
        // repeat twice but allow <AbstractObject> to be missing
        String absObj2 = removeChunk("<AbstractObject>","</AbstractObject>");
        name2 = extractValue(absObj2,"<Name>");
        uuid2 = extractValue(absObj2,"<UUID>");
        hostility2 = extractValue(absObj2,"<SideHostilityCode>");
 
        String absObj3 = removeChunk("<AbstractObject>","</AbstractObject>");
        name3 = extractValue(absObj3,"<Name>");
        uuid3 = extractValue(absObj3,"<UUID>");
        hostility3 = extractValue(absObj3,"<SideHostilityCode>");

        // build new output following 100 schema
        // first <AbstractObject>:
        String absObj = "<AbstractObject><ForceSide><Name>" + name1 + "</Name><UUID>" +
            uuid1 + "</UUID><ForceSideRelation><HostilityStatusCode>" + hostility2 +
            "</HostilityStatusCode><OtherSide>" + uuid2 + "</OtherSide></ForceSideRelation>" +
            "<ForceSideRelation><HostilityStatusCode>" + hostility3 +
            "</HostilityStatusCode><OtherSide>" + uuid3 + "</OtherSide>" +
            "</ForceSideRelation></ForceSide></AbstractObject>";
        
        // second <AbstractObject>:
        absObj += "<AbstractObject><ForceSide><Name>" + name2 + "</Name><UUID>" +
            uuid2 + "</UUID><ForceSideRelation><HostilityStatusCode>" + hostility2 +
            "</HostilityStatusCode><OtherSide>" + uuid1 + "</OtherSide></ForceSideRelation>" +
            "<ForceSideRelation><HostilityStatusCode>" + hostility3 +
            "</HostilityStatusCode><OtherSide>" + uuid3 + "</OtherSide>" +
            "</ForceSideRelation></ForceSide></AbstractObject>";
        
        // third <AbstractObject>:
        absObj += "<AbstractObject><ForceSide><Name>" + name3 + "</Name><UUID>" +
            uuid3 + "</UUID><ForceSideRelation><HostilityStatusCode>" + hostility3 +
            "</HostilityStatusCode><OtherSide>" + uuid1 + "</OtherSide></ForceSideRelation>" +
            "<ForceSideRelation><HostilityStatusCode>" + hostility3 +
            "</HostilityStatusCode><OtherSide>" + uuid3 + "</OtherSide>" +
            "</ForceSideRelation></ForceSide></AbstractObject>";
            
        outputXml += absObj;
        
    }// end editAbsObj()
    
    /**
     * pull an Entity from inputXml
     * 
     * returns true if it found and restructured an Entity
     */
    boolean editEntity(){
        
        // get chunk for the Entity
        String entity = removeChunk("<Entity>", "</Entity>");
        if(entity.equals(""))return false;
        
        // get values for <Latitude>, <Longitude> and <AltitudeMSL>
        String latitude = extractValue(entity,"<Latitude>");
        String longitude = extractValue(entity,"<Longitude>");
        String altitudeMSL = copySubChunk(entity,"<AltitudeMSL>","</AltitudeMSL>");
        
        // get values for <Superior>,<Side>,<SIDCString>,<UUID>, <Name>
        // <OperationalStatusCode> and <EntityTypeString>
        String superior = extractValue(entity,"<Superior>");
        String side = extractValue(entity,"<Side>");
        String sidcString = extractValue(entity,"<SIDCString>");
        String uuid = extractValue(entity,"<UUID>");
        String name = extractValue(entity,"<Name>");
        String opStatus = extractValue(entity,"<OperationalStatusCode>");
        String entityTypeString = extractValue(entity,"EntityTypeString>");
        
        // get value for <EntityTypeString> and turn it into an XML structure
        String disEntityTypeArray = 
            disEntityXml(extractValue(entity,"<EntityTypeString>"));
        
        // get <DirectionOfMovement> and <Orientation> if present and convert
        String direction = 
            addHeadingAngle(
                copySubChunk(entity,"<DirectionOfMovement>","</DirectionOfMovement>"),
                "<DirectionOfMovement>");
        String orientation = 
            addHeadingAngle(
                copySubChunk(entity,"<Orientation>","</Orientation>"),
                "<Orientation>");
        
        // assemble the data in 100 format
        entity = 
            "<Entity><ActorEntity><CollectiveEntity><MilitaryOrganization>" +
            "<EntityDescriptor><Side>" + side + "</Side><Superior>" +
            superior + "</Superior></EntityDescriptor><CurrentState><PhysicalState>" +
            direction + "<EntityHealthStatus><OperationalStatus><OperationalStatusCode>" +
            opStatus + "</OperationalStatusCode></OperationalStatus></EntityHealthStatus>" +
            "<Location><GeodeticCoordinate>" + altitudeMSL + "<Latitude>" + latitude + 
            "</Latitude><Longitude>" + longitude + "</Longitude></GeodeticCoordinate>" +
            "</Location>" + orientation + "</PhysicalState></CurrentState>" +
            "<EntityType><APP6-SIDC><SIDCString>" + sidcString + "</SIDCString></APP6-SIDC>" +
            "</EntityType><EntityType>" + disEntityTypeArray + "</EntityType><EntityType>" +
            "<NamedEntityType><EntityTypeString>" + entityTypeString + "</EntityTypeString>" +
            "</NamedEntityType></EntityType><Name>" + name + "</Name><UUID>" + uuid +
            "</UUID></MilitaryOrganization></CollectiveEntity></ActorEntity></Entity>";

        // add to the overall output and return
        outputXml += entity;
        return true;
        
    }// end editEntity()
    
    /**
     * adds a zero heading angle to <Orientation> and <DirectionOfMovement>
     * to conform to CwSIM 1.0.0
     */
    String addHeadingAngle(String chunk, String tag){

        if(chunk.equals(""))return "";
        String part1 = copySubChunk(chunk,tag,"<EulerAngles>");
        String part2 = copySubChunk(chunk,"<Phi>",makeEndTag(tag));
        return part1 + "<HeadingAngle>0</HeadingAngle>" + part2;
        
    }
    
    /**
     * parse a DISEntityString into values and assemble as 7-part DISEntityType XML
     */
    String disEntityXml(String disEntityString){
        
        // scan across the DISEntityString to separate the values
        String out = "<DISEntityType>";
        String[] entityValues = disEntityToArray(disEntityString);
        out += "<DISCategory>"+entityValues[3]+"</DISCategory>";
        out += "<DISCountry>"+entityValues[2]+"</DISCountry>";
        out += "<DISDomain>"+entityValues[1]+"</DISDomain>";
        out += "<DISExtra>"+entityValues[6]+"</DISExtra>";
        out += "<DISKind>"+entityValues[0]+"</DISKind>";
        out += "<DISSpecific>"+entityValues[5]+"</DISSpecific>";
        out += "<DISSubCategory>"+entityValues[4]+"</DISSubCategory>";
        out += "</DISEntityType>";
        return out;
        
    }// end disEntityXml()
    
    /**
     * extracts substrings of a "." separated DIS EntityType string
     * into a String array with 7 values and returns that
     */
    String[] disEntityToArray(String disEntityInput){
        
        String[] result = new String[7];
        String disEntityType = disEntityInput;
        
        // figure out how the values are separated colon, dot or blank
        char separator = '.';
        if(disEntityType.indexOf(separator) < 0){// it's not dot
            separator = ':';
            if(disEntityType.indexOf(separator) < 0){// it's not colon
                separator = ' ';
                if(disEntityType.indexOf(separator) < 0){// it's not blank
                    
                    // give up
                    System.err.println("****** DISEntityString format bad:" +
                        disEntityInput + " -- using zeros for DISEntityType");
                    result[0] = "0";
                    result[1] = "0";
                    result[2] = "0";
                    result[3] = "0";
                    result[4] = "0";
                    result[5] = "0";
                    result[6] = "0";
                }
            }
        }
        
        try{
            // load the array in order presented
            // (we will reorder on output)
            for(int i=0; i<6; ++i){
                int dotIndex = disEntityType.indexOf(separator);
                result[i] = disEntityType.substring(0,dotIndex);
                disEntityType = disEntityType.substring(dotIndex+1);
            }
            result[6] = disEntityType;
        }
        catch(StringIndexOutOfBoundsException sioode){
            System.err.println("****** DISEntityString format bad:" +
                disEntityInput + " -- using zeros for DISEntityType");
            result[0] = "0";
            result[1] = "0";
            result[2] = "0";
            result[3] = "0";
            result[4] = "0";
            result[5] = "0";
            result[6] = "0";
        }// end catch
        
        return result;
        
    }// end disEntityToArray()
    
    // from here down are utility methods to aid parsing
    
    /**
     * extract data value for a leaf node given start tag
     * 
     * (not necessarily done in schema order) given a chunk of XML
     * 
     * return the data value
     */
    String extractValue(String chunk, String startTag){
        
        String endTag = "</" + startTag.substring(1);
        
        // look for indices where the tag starts and ends
        int startIndex = scanForTag(chunk,startTag,0);
        if(startIndex < 0)return "";
        int endIndex = scanForTag(chunk,endTag,startIndex+startTag.length());
        if(endIndex < 0){
            System.err.println("****** start tag found:" + startTag +
                " end tag not found:" + endTag);
            return "";
        }
        return chunk.substring(startIndex+startTag.length(),endIndex);
        
    }// end extractValue()
    
    /**
     * converts a start tag <tag> to end tag </tag>
     */
    String makeEndTag(String startTag){
        
        // deal with bad input
        if(startTag == null)return startTag;
        if(startTag.length() < 3)return "";
        
        // edit the tag
        return "</" + startTag.substring(1);
      
    }// end makeEndTag()
    
    /**
     * copies a chunk from inputXml given start and end tags
     * either of which might be of form <tag> or </tag>
     */
     String copySubChunk(String chunk,String startTag,String endTag){
        
        // look for indices where the tag pair starts and ends
        int startIndex = scanForTag(chunk,startTag,0);
        if(startIndex < 0)return "";
        int endIndex = scanForTag(chunk,endTag,startIndex+startTag.length());
        if(endIndex < 0){
            System.err.println("****** start tag found:" + startTag +
                " end tag not found:" + endTag);
            return "";
        }
        endIndex += endTag.length();
        return chunk.substring(startIndex, endIndex);
                
     }//end copyChunk()
     
    /**
     * extracts a chunk from inputxml string based on tag
     * returns the chunk and has the side-effect of
     * removing the chunk from inputXml
     * 
     * both start and end must be valid XML tags with no
     * prefix; either may be the end tag for an element
     * we insert a prefix if there is one and they may
     * be the same tag to remove only the tag
     * 
     * returns "" if inputXml has no matching tag
     */
    String removeChunk(String startTag, String endTag){
        
        // look for indices where the tag starts and ends
        int startIndex = scanForTag(inputXml,startTag,0);
        if(startIndex < 0)return "";
        int endIndex = scanForTag(inputXml,endTag,startIndex);
        if(endIndex < 0){
            System.err.println("****** start tag found:" + startTag +
                " end tag not found:" + endTag);
            return "";
        }
        endIndex += endTag.length();
  
        // divide up xmlString to beginning, chunk sought, and end
        String startPart = inputXml.substring(0,startIndex);
        String endPart = inputXml.substring(endIndex);
        String chunk = inputXml.substring(startIndex, endIndex);
        inputXml = startPart+endPart;
        return chunk;
        
    }// end removeChunk()
    
    /**
     * locate first index of an XML tag in xml, taking into consideration
     * that the tag might match some data so leaf element text between any
     * <tag> and </tag> should not be considered in scanning for the tagName
     * 
     * xml must start and end with a properly formatted <startTag> or </endTag>
     * 
     * returns the index of the tag, or -1 if not found
     * 
     * for end-style tag </tag> returns next index after the tag
     */
    int scanForTag(String xml, String tag, int startScan){
        
        // don't search past end of xml
        int tagLength = tag.length();
        int endScan = xml.length() - tag.length() + 1;
        
        // keep track of context as we scan 
        boolean insideLeaf = false;
        boolean insideTag = false;
        boolean insideQuote = false;
        int scanIndex;
        
        // scan the string looking for tag
        for(scanIndex = startScan; scanIndex < endScan; ++scanIndex){
            char scan = xml.charAt(scanIndex);
            
            // first check whether in a quoted string
            if(insideQuote){
                if(scan != '\"')continue;
                insideQuote = false;
                continue;
            }
        
            // then check whether inside a < > tag
            if(insideTag){
                if(scan != '>')continue;
                insideTag = false;
                continue;
            }
            
            // finally, could be between two leaf-node tags 
            if(insideLeaf){
                if(!insideQuote && scan == '<'){
                    insideLeaf = false;
                    insideTag = true;
                    if(xml.substring(scanIndex,scanIndex+tagLength).equals(tag))
                        return scanIndex;
                }
            }
            else {          
                // could find the tag if outside quote, tag and leaf
                if(xml.substring(scanIndex,scanIndex+tagLength).equals(tag))
                    return scanIndex;
                
                // if not the tag sought it could be starting a quote
                if(scan == '\"')insideQuote = true;
                
                // or starting another tag
                else if(scan == '<')insideTag = true;
                
                // otherwise it must be data since whitepace has been removed
                else insideLeaf = true;

            }// end else/if(insideLeaf)

        }// end for(scanindex...
        
        return -1;
        
    }// end scanForTag()
    
    /**
     * removes the whitespace characters ' ', '\n' and '\r' from a string 
     * unless they are between < and > or " and "
     * in the process strip input prefix 
     */
    String removeWhitespace(String input){
        boolean inTag = false, inQuotes = false;
        int includeSlash = -1, colonIndex = -1;
        String result = "";
        int scanIndex, inputLength = input.length();
        for(scanIndex = 0; scanIndex < inputLength; ++scanIndex){
            char scan = input.charAt(scanIndex);
            
            // check for slash following <
            if(scanIndex == includeSlash){
                result += scan;
                continue;
            }
            
            // drop prefix characters
            if(colonIndex >= scanIndex)continue;
            colonIndex = -1;        
            
            // bypass contents of tags and quotes
            if(inQuotes){
                result += scan;
                if(scan == '\"')inQuotes = false;
                continue;
            }
            if(inTag){
                result += scan;
                if(scan == '>')inTag = false;
                continue;
            }
            
            // check whether starting quotes
            if(scan == '\"')inQuotes = true;
         
            // if starting tag, check about removing prefix
            if(scan == '<'){
                inTag = true;
                
                // look forward to end of tag for prefix
                includeSlash = -1;
                for(int preIndex=scanIndex+1; preIndex<inputLength; ++preIndex){
                    char preCheck = input.charAt(preIndex);
                    if(preCheck == '>' || preCheck == ' ')break;
                    if(preCheck == '/' && preIndex == scanIndex+1)
                        includeSlash = preIndex;
                    if(preCheck == ':'){
                        colonIndex = preIndex;
                        break;
                    }
                }// end for(preIndex...
            }// end if(scan == '<')
            
            if(scan != ' ' && scan != '\n' && scan != '\r')
                result += scan;
            
        }// end for(scanIndex...
        
        return result;
        
    }// end removeWhitespace()
    
    /**
     * print up to 400 chars of a String for debug
     */
    void print400(String title, String toPrint){
        
        if(toPrint.length() > 400)System.out.println(title + toPrint.substring(0,400));
        else System.out.println(title + toPrint);
    }
    
}// end class Translate009Init
