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
/**
 * Reads an XML string from file and parses it to enable extracting elements
 */
package edu.gmu.netlab;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;

/**
 * @author jmarkpullen with input from 
 * https://www.tutorialspoint.com/java_xml/java_dom_parse_document.htm
 */
public class XmlParse {
  
  C2SIMGUI bml = C2SIMGUI.bml;
  String xmlString;   // XML read from file
  Document xmlDoc;    // XML as a DOM Document
  String newLine = System.getProperty("line.separator");

 /**
  * constructor parses the XML into structure for extraction
  */
  public XmlParse(String xmlString) {
 
    // create a DocumentBuilder
    DocumentBuilderFactory dbFactory;
    DocumentBuilder dBuilder;
    try {
      dbFactory = DocumentBuilderFactory.newInstance();
      dBuilder = dbFactory.newDocumentBuilder();
    }
    catch(ParserConfigurationException pce)
    { 
        bml.showErrorPopup( 
            "Exception parsing XML file:" + pce.getMessage(), 
            "XML Error");
      return;
    }
    
    // create a document
    StringBuilder sb = new StringBuilder();
    sb.append(xmlString);
    ByteArrayInputStream bais = null;
    try {
      bais = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
    } catch(UnsupportedEncodingException uee)
    { // can't happen with literal
    }
    try {
      xmlDoc = dBuilder.parse(bais);
    }
    catch (Exception e)
    { 
        bml.showErrorPopup( 
            "Exception parsing XML file:" + e.getMessage(), 
            "XML Error");
        return;
    }
    xmlDoc.getDocumentElement().normalize();
  }// end constructor xmlParse
  
 /**
  * returns root Element
  */
  String getRootName()
  {
    return xmlDoc.getDocumentElement().getNodeName();
  }
  
 /**
   * returns the XML read from file as a String
   */
  String getXml()
  {
    return xmlString;
  }
  
/**
  * returns array of data values for Elements of given name
  *
  **/
  String[] getElementDataByTagName(String elementName)
  {
    NodeList dataNodes = xmlDoc.getElementsByTagName(elementName);
    int listLength = dataNodes.getLength();
    
    // scan Nodes in the list extracting data
    int outputLength = 0;
    String[] tempData = new String[listLength];
    for(int index=0; index < listLength; ++index){
      Node testNode = dataNodes.item(index);
      if(testNode.getNodeType() != Node.ELEMENT_NODE)continue;
      tempData[outputLength++] = 
        removeAllButFirstValue(testNode.getTextContent());
    }// end for index
    
    // return only elements with values
    String[] returnData = new String[outputLength];
    System.arraycopy(tempData,0,returnData,0,outputLength);
    return returnData;
    
  }// end getElementDataByTagName
  
  /**
   * multiple subelements can produce a lot of data;
   * cut off any values after the first
   */
  String removeAllButFirstValue(String contentString) {
    String trimmedContent = contentString.trim();
    int newLineIndex = trimmedContent.indexOf(newLine);
    //int blankIndex = trimmedContent.indexOf(' ');
    //if(newLineIndex < 0)newLineIndex = blankIndex;
    if(newLineIndex < 0)newLineIndex = trimmedContent.length();
    return trimmedContent.substring(0, newLineIndex);
  }
  
    /**
      * returns array of names & data values associated with Nodes of given name
      * that are Elements; sequence is Node name followed
      * by name then data of each sub-Node that matches nodeName in subNodeNames
      * including groups with groupNodeNames in index order (used for 
      * groups of location point attributes: lat/lon/elev)
      * 
      * All nodename comparisons are done with and without namespace prefix
      *
      * parameters:
      * nodeName is the tag at the XML tree branching point, below which to extract
      * subNodeNames are tags at nodes beyond nodeName in the tree
      * groupNodeNames are tags that can appear more than once below a node
      *     (e.g. multiple values of data like a Route)
      *     (there must be the same number of each of these)
      **/
    String[] getElementNameDataByTagName(
      String nodeName,
      String[] subNodeNames,
      String[] groupNodeNames,
      String nsPrefix)
    {
        int listLength;
        NodeList dataNodes;
        try{
            // find top-level list of elements
            dataNodes = xmlDoc.getElementsByTagName(nodeName);
            if(dataNodes == null)return null;
            listLength = dataNodes.getLength();
            if(listLength == 0) {
              dataNodes = xmlDoc.getElementsByTagName(nsPrefix+nodeName);
              if(dataNodes == null)return null;
              listLength = dataNodes.getLength();
              if(listLength == 0) {
                bml.printError(
                  "error in XML: getElementsByTagName returns empty list for |" + 
                    nsPrefix+nodeName + "|");
                return null;
              }
            }
        }
        catch(Exception e){
            bml.showErrorPopup( 
                "Exception parsing XML file:" + e.getMessage(), 
                "XML Error");
            return null;
        }

        // make array to hold results
        int subNodesLength = subNodeNames.length;
        int groupNodesLength = groupNodeNames.length;
        int subNodeIndex = 0;

        // count all name and data requirements in tempData
        String[] tempData = null;
        int tempDataSize = 0;

        // look at each subNode in dataNodes
        for(
            int topLevelIndex = 0;
            topLevelIndex < listLength;
            ++topLevelIndex){
            Element topLevelElement = (Element)dataNodes.item(topLevelIndex);

            // each top level element in dataNodes adds one to array size
            tempDataSize++;

            // each of the subNodeNames in topLevelElements
            // takes 2 array positions
            for(
                subNodeIndex = 0;
                subNodeIndex < subNodesLength;
                ++subNodeIndex){
                NodeList subNodeList = 
                  topLevelElement.getElementsByTagName(subNodeNames[subNodeIndex]);
                if(subNodeList == null)return null;
                int subNodeCount = subNodeList.getLength();
                if(subNodeCount == 0)subNodeList = 
                  topLevelElement.getElementsByTagName(nsPrefix+subNodeNames[subNodeIndex]);
                if(subNodeList == null)return null;
                subNodeCount = subNodeList.getLength();
                tempDataSize += 2*subNodeCount;
                
            }// end for subnodeIndex

            // each of the groupNodeNames in topLevelElements
            // takes 2 times number of elements in any one group
            // times number of groupNodeNames (same number will
            // be present for each of the names)
            if(groupNodesLength > 0){
                NodeList groupList = 
                    topLevelElement.getElementsByTagName(groupNodeNames[0]);
                int groupListCount = groupList.getLength();
                if(groupListCount == 0){
                    groupList = 
                        topLevelElement.getElementsByTagName(nsPrefix+groupNodeNames[0]);
                    if(groupList == null)return null;
                    groupListCount = groupList.getLength();
                }
                tempDataSize += 2 * groupListCount * groupNodesLength;
            }// end if(grounNodesPength...
        }//end for topLevelIndex

        // make string array
        tempData = new String[tempDataSize];
        int outputLength = 0;

        // scan through each node in dataNodes extracting name and data
        // for selected nodes, first by subNodeNames, then by groupNodeNames
        for(
            int topLevelIndex = 0;
            topLevelIndex <  listLength;
            ++topLevelIndex) {
            Element topLevelElement=(Element)dataNodes.item(topLevelIndex);
            if(topLevelElement.getNodeType() != Node.ELEMENT_NODE)continue;

            // emit nodeName for next group of data
            tempData[outputLength++] = nodeName;

            // extract the subNode elements
            for(
                subNodeIndex = 0;
                subNodeIndex < subNodesLength;
                ++subNodeIndex) {
                NodeList subNodeList = 
                  topLevelElement.getElementsByTagName(subNodeNames[subNodeIndex]);
                if(subNodeList == null)return null;
                int subNodeListLength = subNodeList.getLength();
                if(subNodeListLength == 0){
                  subNodeList = 
                    topLevelElement.
                        getElementsByTagName(nsPrefix+subNodeNames[subNodeIndex]);
                  if(subNodeList == null)return null;
                  subNodeListLength = subNodeList.getLength();
                }
                if(subNodeListLength == 0)continue;

                // emit the nodes
                for(
                    int instance = 0;
                    instance < subNodeListLength;
                    ++instance){ 
                    Element subNodeElement = (Element)subNodeList.item(instance);
                    tempData[outputLength++] = subNodeElement.getNodeName();
                    tempData[outputLength++] = 
                        removeAllButFirstValue(subNodeElement.getTextContent());
                }
                
            }// end for subNodeIndex

            // scan through all groupNodeNames in the topLevelElement
            // copying the appropriate subtrees to an array
            int groupDataLength = 0;
            NodeList[] groupNodeList = new NodeList[groupNodesLength];
            for(
                int groupNodeIndex = 0; 
                groupNodeIndex < groupNodesLength; 
                ++groupNodeIndex){

                // get the NodeList for each element of the group
                groupNodeList[groupNodeIndex] = 
                    topLevelElement.getElementsByTagName(groupNodeNames[groupNodeIndex]);
                if(groupNodeList[groupNodeIndex] == null)return null;
                if(groupNodeList[groupNodeIndex].getLength() == 0)
                    groupNodeList[groupNodeIndex] =
                        topLevelElement.getElementsByTagName(nsPrefix+groupNodeNames[groupNodeIndex]);
                if(groupNodeList[groupNodeIndex] == null)return null;

                // all must be of same length; confirm that
                groupDataLength = groupNodeList[0].getLength();
                if(groupNodeList[groupNodeIndex].getLength() != groupDataLength)
                {
                    bml.printError(
                        "error in XML: number of elements for " + 
                        groupNodeNames[0] + " and " + groupNodeNames[groupNodeIndex] +
                        " do not match");
                    return null;
                }
            }// end for groupNodeIndex

            // emit the data into output stringArray in index order
            for(
                int groupDataIndex = 0;
                groupDataIndex < groupDataLength;
                groupDataIndex++){
                for(
                    int groupEmitIndex = 0;
                    groupEmitIndex < groupNodesLength;
                    ++groupEmitIndex){
                    Node groupSubNode = 
                        groupNodeList[groupEmitIndex].item(groupDataIndex);
                    if(groupSubNode.getNodeType() != Node.ELEMENT_NODE)continue;
                    tempData[outputLength++] = groupSubNode.getNodeName();
                    tempData[outputLength++] = 
                    groupSubNode.getTextContent();

                }// end for groupEmitIndex
            }// end for groupDataIndex       
        }// end for topLevelIndex

        // if tempData is full return it; otherwise copy
        // it and return only elements with values
        if(tempData.length == outputLength)return tempData;
        String[] returnData = new String[outputLength];
        System.arraycopy(tempData,0,returnData,0,outputLength);
        return returnData;
    
    }// end getElementNameDataByTagName()
  
}// end class xmlParse
