package edu.gmu.c4i.c2simserver4.c2simserver;

/*
 * translate C2SIM 0.0.9 to C2SIM 1.0.0 and return
 *
 * this class does 1.0.0 Initialization to 0.0.9
 *
 * we make no attempt here to cover full schemas; only the subset
 * used in CWIX 2019 since no further 0.0.9 implementations are
 * anticipated
 *
 */


/**
 *
 * @author jmarkpullen
 */
public class Translate100Init {
        
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
     * returns inputXML translated from C2SIM 1.0.0 to C2SIM 0.0.9
     */
    String translate(String xml){
        
        // pack out whitespace so our edits work
        inputXml = removeWhitespace(xml);
        
        // determine which body tag to use
        if(inputXml.contains("<ObjectInitializationBody>"))
            bodyTag = "<ObjectInitialization>";
       
        // XML prolog up to <AbtractObject>
        outputXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<MessageBody xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://www.sisostds.org/schemas/C2SIM/1.1 " +
            "C2SIMv9_SMXv9_LOXplusv6.xsd\"" +
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

        // capture <ScenarioSetting> to move later to outputXml
        String scenarioSetting = removeChunk("<ScenarioSetting>","</ScenarioSetting>");
        String ssDateTime = extractValue(scenarioSetting,"<IsoDateTime>");

        // copy <SystemEntityList> in chunks - no need to edit
        String systemEntityList = removeChunk("<SystemEntityList>","</SystemEntityList>");
        while(!systemEntityList.equals("")){
            outputXml += systemEntityList;
            systemEntityList = removeChunk("<SystemEntityList>","</SystemEntityList>");
        }
        
        // <ScenarioSetting> goes at end in 009
        outputXml += 
            "<ScenarioSetting><DateTime><IsoDateTime>" + ssDateTime + 
            "</IsoDateTime></DateTime>" + 
            "<PhysicalExtent><BoundingBox><Length></Length><Width></Width></BoundingBox>" +
            "</PhysicalExtent><Version>0.0.9</Version></ScenarioSetting>";
        
        // last bit to add
        outputXml += makeEndTag(bodyTag) + "</MessageBody>";
        
        
        return outputXml;
        
    }// end translateInit100()
    
    // methods to restructure 100 into 009
    
    /**
     * pull all 100 AbstractObject from inputXml;
     * reassemble in 009 and add to outputXML
     *
     * however we expect three; if less than that the other two 
     * will be empty of data; we don't handle more
     */
    void editAbsObj(){
        
        // from CWIX 2019 there can be up to three AbstractObjects in 009
        // need all three of them to form the 100 version
        String name1 = "", name2 = "", name3 = "";
        String uuid1 = "", uuid2 = "", uuid3 = "";
        String uuid1a = "", uuid1b = "";
        String uuid2a = "", uuid2b = "";
        String uuid3a = "", uuid3b = "";
        String hostility1a = "", hostility1b = ""; 
        String hostility2a = "", hostility2b = "";
        String hostility3a = "", hostility3b = "";
 
        // pull a chunk from 100 inputXML and extract its data values
        // because the <ForceSideRelations> have identical structure
        // we pull then out in separate pieces
        String absObj1 = removeChunk("<AbstractObject>","</UUID>");
        if(absObj1.equals("")){
            System.err.println("****** can't find any AbstractObject");
            return;
        }
        name1 = extractValue(absObj1,"<Name>");
        uuid1 = extractValue(absObj1,"<UUID>");
        
        // the two force sde relations differ only in data
        String absObj1a = removeChunk("<ForceSideRelation>","</ForceSideRelation>");
        hostility1a = extractValue(absObj1a,"<HostilityStatusCode>");
        uuid1a = extractValue(absObj1a,"<OtherSide>");
        String absObj1b = removeChunk("<ForceSideRelation>","</AbstractObject>");
        hostility1b = extractValue(absObj1b,"<HostilityStatusCode>");
        uuid1b = extractValue(absObj1b,"<OtherSide>");
      
        // repeat twice but allow <AbstractObject> to be missing
        String absObj2 = removeChunk("<AbstractObject>","</UUID>");
        name2 = extractValue(absObj2,"<Name>");
        uuid2 = extractValue(absObj2,"<UUID>");
        String absObj2a = removeChunk("<ForceSideRelation>","</ForceSideRelation>");
        hostility2a = extractValue(absObj2a,"<HostilityStatusCode>");
        uuid1a = extractValue(absObj1a,"<OtherSide>");
        String absObj2b = removeChunk("<ForceSideRelation>","</AbstractObject>");
        hostility2b = extractValue(absObj2b,"<HostilityStatusCode>");
        uuid2b = extractValue(absObj2b,"<OtherSide>");
 
        // repeat for third ForceSide
        String absObj3 = removeChunk("<AbstractObject>","</UUID>");
        name3 = extractValue(absObj3,"<Name>");
        uuid3 = extractValue(absObj3,"<UUID>");
        String absObj3a = removeChunk("<ForceSideRelation>","</ForceSideRelation>");
        hostility3a = extractValue(absObj3a,"<HostilityStatusCode>");
        uuid3a = extractValue(absObj3a,"<OtherSide>");
        String absObj3b = removeChunk("<ForceSideRelation>","</AbstractObject>");
        hostility3b = extractValue(absObj3b,"<HostilityStatusCode");
        uuid3b = extractValue(absObj3b,"<OtherSide>");

        // build new output following 100 schema
        // first <AbstractObject>:
        String absObj = "<AbstractObject><ForceSide><Name>" + name1 + "</Name><UUID>" +
            uuid1 + "</UUID><SideHostilityCode>" + hostility2a +
            "</SideHostilityCode></ForceSide></AbstractObject>";
        
        // second <AbstractObject>:
        absObj += "<AbstractObject><ForceSide><Name>" + name2 + "</Name><UUID>" +
            uuid2 + "</UUID><SideHostilityCode>" + hostility1a +
            "</SideHostilityCode></ForceSide></AbstractObject>";
        
        // third <AbstractObject>:
        absObj += "<AbstractObject><ForceSide><Name>" + name3 + "</Name><UUID>" +
            uuid3 + "</UUID><SideHostilityCode>" + hostility3a +
            "</SideHostilityCode></ForceSide></AbstractObject>";
            
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
        // <OperationalStatusCode> 
        String superior = extractValue(entity,"<Superior>");
        String side = extractValue(entity,"<Side>");
        String sidcString = extractValue(entity,"<SIDCString>");
        String uuid = extractValue(entity,"<UUID>");
        String name = extractValue(entity,"<Name>");
        String opStatus = extractValue(entity,"<OperationalStatusCode>");
        
        // get values for the 7 components of DISEntityType and make a DISEntityString
        String disCategory = extractValue(entity,"<DISCategory>");
        String disCountry = extractValue(entity,"<DISCountry>");
        String disDomain = extractValue(entity,"<DISDomain>");
        String disExtra = extractValue(entity,"<DISExtra>");
        String disKind = extractValue(entity,"<DISKind>");
        String disSpecific = extractValue(entity,"<DISSpecific>");
        String disSubCategory = extractValue(entity,"<DISSubCategory>");
        String disEntityString = disKind  + '.' + disDomain + '.' + disCountry + '.' +
            disCategory + '.' + disSubCategory + '.' + disSpecific + '.' + disExtra;
        if(!disEntityString.matches("([0-9]{1,3}\\.){6}[0-9]{1,3}"))
            disEntityString="0.0.0.0.0.0.0";
         
        // get <DirectionOfMovement> and <Orientation> if present and convert
        String direction = 
            removeHeadingAngle(
                copySubChunk(entity,"<DirectionOfMovement>","</DirectionOfMovement>"),
                "<DirectionOfMovement>");
        String orientation = 
            removeHeadingAngle(
                copySubChunk(entity,"<Orientation>","</Orientation>"),
                "<Orientation>");
        
        // assemble the data in 009 format
        entity = 
            "<Entity><ActorEntity><CollectiveEntity><MilitaryOrganization>" +
            "<CurrentState><PhysicalState><Location><Coordinate><GeodeticCoordinate>" +
            "<Latitude>" + latitude + "</Latitude><Longitude>" + longitude + "</Longitude>" +
            altitudeMSL + "</GeodeticCoordinate></Coordinate></Location><EntityHealthStatus>" +
            "<OperationalStatus><OperationalStatusCode>" + opStatus + "</OperationalStatusCode>" +
            "</OperationalStatus></EntityHealthStatus></PhysicalState></CurrentState>" +
            "<EntityDescriptor><Superior>" + superior + "</Superior><Side>" + side + 
            "</Side></EntityDescriptor><EntityType><APP6-SIDC><EntityTypeString>" + 
            disEntityString + "</EntityTypeString><SIDCString>" + sidcString + 
            "</SIDCString></APP6-SIDC></EntityType><UUID>" + uuid + "</UUID><Name>" + name + 
            "</Name></MilitaryOrganization></CollectiveEntity></ActorEntity></Entity>";

        // add to the overall output and return
        outputXml += entity;
        return true;
        
    }// end editEntity()
    
    /**
     * deletes heading angle from <Orientation> and <DirectionOfMovement>
     * since it is nnt used in 0.0.9
     */
    String removeHeadingAngle(String chunk, String tag){

        if(chunk.equals(""))return "";
        String part1 = copySubChunk(chunk,tag,"<EulerAngles>");
        String part2 = copySubChunk(chunk,"<Phi>",makeEndTag(tag));
        return part1 + part2;
        
    }

    
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
     * we insert a prefix if there is one
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
    
}// end class Translate100Init
