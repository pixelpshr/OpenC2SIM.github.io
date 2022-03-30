package edu.gmu.c4i.c2simserver4.c2simserver;

/*
 * translate C2SIM 0.0.9 to C2SIM 1.0.0 and return
 *
 * this class does 1.0.0 Order to 0.0.9
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
public class Translate100Order {
    
    String inputXml;
    String outputXml = "";
    String task;
    
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
        
        // XML prolog up to <AbtractObject>
        outputXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<MessageBody xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://www.sisostds.org/schemas/C2SIM/1.1 " +
            "C2SIMv9_SMXv9_LOXplusv6.xsd\"" +
            " xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\"><DomainMessageBody>";
        
        // select the internal header and copy to output (same in both formats)
        outputXml +=  removeChunk("<OrderBody","</ToReceiver>");
         
        // OrderID block has time issued and DrderID - get those for below
        String orderIdBlock = removeChunk("<IssuedTime>","</OrderID>");
        String issuedTime = extractValue(orderIdBlock,"<IsoDateTime>");
        String orderId = extractValue(orderIdBlock,"<OrderID>");
        
        // process Tasks one at a time
        while(processTask());

        // complete order
        outputXml +=
            "<IssuedTime><DateTime><IsoDateTime>" + issuedTime + 
            "</IsoDateTime></DateTime></IssuedTime><OrderID>" + orderId + "</OrderID>" +
            removeChunk("</OrderBody","</MessageBody>");
        
        return outputXml;
        
    }// end translate()
    
    // methods to restructure 009 into 100
    
    /**
     * ingest one Task, collect its data, translate to 100 task 
     * @returns false when no more Tasks in Order
     */
    boolean processTask(){
        
        // pull the Task from inputXml
        task = removeChunk("<Task>","</Task>");
        if(task.equals(""))return false;
        String taskOut = "<Task><ManeuverWarfareTask>";
        
        // extract other values associated with the Task
        String effectCode = extractValue(task,"<DesiredEffectCode>");
        String actionCode = extractValue(task,"<TaskNameCode>");
        String affected = extractValue(task,"AffectedEntity");
        String performing = extractValue(task,"<PerformingEntity>");
        String uuid = extractValue(task,"<UUID>");
        String name = extractValue(task,"<Name>");
        
        // there are two elements anems "Name" so we must get at StartTime in pieces
        String dateTime = copySubChunk(task,"<DateTime>","</DateTime>");
        String startTime = copySubChunk(dateTime,"<Name>","</IsoDateTime>");
        
        // buid remainder of Task statement
        taskOut +=
            "<DesiredEffectCode>" + effectCode + "</DesiredEffectCode><TaskNameCode>" +
            actionCode + "</TaskNameCode><StartTime>" + startTime + 
            "</StartTime><AffectedEntity>" + affected + 
            "</AffectedEntity><PerformingEntity>" + performing + "</PerformingEntity>";
                
        // collect Location, regroup and move to output
        String location = removeTaskChunk("<Location","</Location>");
        String latitude = extractValue(location,"<Latitude>");
        String longitude = extractValue(location,"<Longitude>");
        String altitude = extractValue(location,"<AltitudeMSL>");
        if(!altitude.equals(""))
            altitude = "<AltitudeMSL>" + altitude + "</AltitudeMSL>";
        taskOut += "<Location><Coordinate><GeodeticCoordinate>" + 
            "<Latitude>" + latitude + "</Latitude><Longitude>" + longitude + 
            "</Longitude>" + altitude + "</GeodeticCoordinate></Coordinate></Location>";
        
        // collect any more Location, regroup and make them RouteLocations
        location = removeTaskChunk("<Location","</Location>");
        if(!location.equals(""))taskOut += "<Route>";
        while(!location.equals("")){
            latitude = extractValue(location,"<Latitude>");
            longitude = extractValue(location,"<Longitude>");
            altitude = extractValue(location,"<AltitudeMSL>");
            if(!altitude.equals(""))
                altitude = "<AltitudeMSL>" + altitude + "</AltitudeMSL>";
            taskOut += "<RouteLocation><GeodeticCoordinate>" + 
                "<Latitude>" + latitude + "</Latitude><Longitude>" + longitude + 
                "</Longitude>" + altitude + "</GeodeticCoordinate></RouteLocation>";
            location = removeTaskChunk("<Location","</Location>");
            if(location.equals(""))taskOut += "</Route>";
        }
                
        // remaining values
        taskOut +=
            "<UUID>" + uuid + "</UUID><Name>" + name + "</Name>" +
            "</ManeuverWarfareTask></Task>";
        outputXml += taskOut;
        
        return true;
        
    }//end processTask()
    
    /**
     * a version of removeCHnk that is specific to task not inputXml
     */
        String removeTaskChunk(String startTag, String endTag){
        
        // look for indices where the tag starts and ends
        int startIndex = scanForTag(task,startTag,0);
        if(startIndex < 0)return "";
        int endIndex = scanForTag(task,endTag,startIndex+startTag.length());
        if(endIndex < 0){
            System.err.println("****** start tag found:" + startTag +
                " end tag not found:" + endTag);
            return "";
        }
        endIndex += endTag.length();
  
        // divide up xmlString to beginning, chunk sought, and end
        String startPart = task.substring(0,startIndex);
        String endPart = task.substring(endIndex);
        String chunk = task.substring(startIndex, endIndex);
        task = startPart+endPart;
        return chunk;
        
    }// end removeTaskChunk()
    
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
    
}// end class Translate100Order
