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

import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;
import edu.gmu.c4i.c2simserver4.schema.C2SIMSchema;
import edu.gmu.c4i.c2simserver4.schema.C2SIMSchemaElement;
import edu.gmu.c4i.c2simserver4.schema.C2SIMSchemaMapping;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.xml.namespace.QName;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;


/**
 * <h1>C2SIM_Mapping</h1> <p>
 * Performs identification and translation of messages

 * @author Douglas Corner - George Mason University C4I and  Center
 */

public class C2SIM_Mapping {
    
   /************************/
    /*  parseMessage        */
    /************************/
    /**
    * parseMessage - Parse an XML string creating a JDOM Document
    @param xml - String input xml
    @return - JDOM Document
    @throws C2SIMException 
    */
    static Document parseMessage(String xml) throws C2SIMException {
        SAXBuilder sb;
        Document doc;
        try {
            // Create SaxBuilder and parse the message
            sb = new SAXBuilder();
            doc = sb.build(new StringReader(xml));
        }
        catch (IOException i) {
            C2SIM_Server.debugLogger.error("IO Exception creating new StringReader for SAXBuilder" + i);
            throw new C2SIMException("IO Exception creating new StringReader for SAXBuilder", i);
        }
        catch (JDOMException j) {
            C2SIM_Server.debugLogger.error("JDOM Exception while parsing input XML " + j.toString());
            throw new C2SIMException("JDOM Exception while parsing input XML", j);
        }
        return doc;
    }   // parseMessage()


    /*************************/
    /*  identifyMessage      */
    /*************************/
    /**
    * identifyMesasage - Identify incoming parsed message against set of store C2SIMMessageDefinition objects 
    @param d - Document Parsed message
    @return C2SIMMessageDefinition
    */
    static C2SIMMessageDefinition identifyMessage(Document d) {
        // Search the message comparing against all known message types 

        C2SIMMessageDefinition m2 = null;

        // Using each message definition, look for message ID string, identifying the particular message

        for (C2SIMMessageDefinition m : C2SIM_Util.db.messageDB) {
            Element e;
            // If there is no path this C2SIMMessageDefinition must just be used for mapping 
            if (m.idPath.isEmpty())
                continue;
            e = findElement(m.idPath, d.getRootElement());
            if (e != null) {
                return m;
            }
        }   // for

        return null;

    }   // identifyMessage()

    /************************/
    /*  mapMessage          */
    /************************/
    /**
    * mapMessage - Translate message using C2SIMMessageDefinition
    @param msg - C2SIMMessageDefinition to be used
    @param dx - Document to be translated
    @param direction - String - Direction of translation - F or R
    @return - Translated Document
    @throws C2SIMException 
    */
    static Document mapMessage(C2SIMMessageDefinition msg, Document dx, String direction) throws C2SIMException {

        // Map from xml Document d using mappings in C2SIMMessageDefinition m
        //      Return a new document, d2, mapped from d1

        String data;
        Element targetRoot = null;
        Element sourceRoot;
        Vector<C2SIMSchemaMapping> currMapping;
        C2SIMSchema currSourceSchema;
        C2SIMSchema currTargetSchema;

        // There is a bug somewhere such that the document passed in to be mapped gets modified.
        Document d1 = dx.clone();

        // Get root element in source document
        sourceRoot = d1.getRootElement();


        // The mapping may be used in for Forward direction "from" to "to" using the sourseSchema
        //      and in the reverse direction from "to" to "from" using the targetSchema
        if (direction.equals("F")) {
            currMapping = msg.forwardMapping;
            currSourceSchema = msg.sourceSchema;
            currTargetSchema = msg.targetSchema;
        }

        else {
            currMapping = msg.reverseMapping;
            currSourceSchema = msg.targetSchema;
            currTargetSchema = msg.sourceSchema;
        }   // determine which mapping to use

        // New document
        Document d2 = new Document();

        // Set the namespace
        d2.setBaseURI(currTargetSchema.targetNameSpace);


        // Initialize the root node in the destination document
        // If this is the first element to be added to d2 then add the root
        // Create an element for the root
        targetRoot = new Element(currTargetSchema.rootElement, currTargetSchema.targetNameSpace);

        // Add additional namspaces to the root element
        for (QName qn : currTargetSchema.namespaceList) {
            if (qn.getPrefix() != "")
                targetRoot.addNamespaceDeclaration(Namespace.getNamespace(qn.getPrefix(), qn.getNamespaceURI()));
        }   // for

        // Add the root to the document
        d2.addContent(targetRoot);

        // Process the root array. processArray will call itself for sub arrays
        processArray(sourceRoot, targetRoot, currSourceSchema.schemaMap, currTargetSchema.schemaMap, currMapping);

        // Return new completed document
        return d2;

    }   // mapMessage()


    /*******************/
    /* processArray    */
    /*******************/
    /**
    * processArray - Recursive routine used in translation - Array refers to elements with maxOccurs > 1
    @param source - Element current position in source Document
    @param target - Element current position in target Document
    @param sourcePathMap - Map to be used for locating source Elements in schema - Component of C2SIMMessageDefinition
    @param targetPathMap - Map to be used for locating target Elements in schema - Component of C2SIMMessageDefinition
    @param mapping - Mapping elements for this array
    @throws C2SIMException 
    */
    static void processArray(Element source, Element target, HashMap<String, C2SIMSchemaElement> sourcePathMap, HashMap<String, C2SIMSchemaElement> targetPathMap, Vector<C2SIMSchemaMapping> mapping)
            throws C2SIMException {

        Vector<QName> fPath = null;
        Vector<QName> tPath = null;
        String fName = "";
        String tName = "";
        Vector<Element> elementList;
        Element destElement;
        Vector<C2SIMSchemaMapping> arrayMapping;

        /*
            This is the heart of the XML translation process.  What is defined as an array is a set of xml leaf elements that exist as a group and 
              can have multiple instances (maxOccurs > 1).  The simplest example is a basic report what doesn't repeat but exists as a group of elements.  Paths are
              lists of qname objects that contain the non-leaf elements from the root of the curreht array to the current element.  During the processing of the schema
              the shortest name, starting from the element itself, for each leaf element is determined.  This is the name that is used to specify mapping os source elements
              to target elements.  HashMaps are also generated that map the short names for full paths of qname objects.
              This routine is called for each array encountered including the root array.  Note that for a simple xml document like a report this will be the only array
        
              A mapping table consists of three entries:
                The short name of the source element
                The short name of the target element
                A reference to another mapping table that is to be activated to process this source and target.
       
              The parameters passed are:
                    source - The source element which is generally the root of an array.
                    target - The current element is the document being built.
                    sourcePathMap - Maps source short names to full path.
                    targetPathMap - Maps target short names to full path.
                    mapping - Mapping table to be used.
        
        Overall logic for each type of element mapping entry
        
            source Type     target Type     map         Action
        
        A       Leaf            Leaf        empty       Copy source to target
        
        B       Array           Array       exists      Loop 0n list of source elements
                                                            Create new target element
                                                            processArray(source element, new target, 
                                                                Name maps from source and target arrays
                                                                New element mapping from current mapping entry
        
        C       Array           Blank       exists      Loop on list of source elements
                                                            process array(source element, current target, 
                                                                Name maps from new source array, current target map
                                                                New element mapping from current 
        
        D       Blank           Array       exists      Insert new target node
                                                        processArray(current Source, new target
                                                                Name map from current source, Name map from new target
                                                                New element mapping
        
        E       Array           Leaf        exists      processArray (One Source element, current target) proceed as in C
        
        F       Leaf            Array       exists      Insert one target node
                                                        processArray(current source, new Target) Proceed as in D  
         */

        for (C2SIMSchemaMapping map : mapping) {
            try {

                // Short names for source and target
                fName = map.from;
                tName = map.to;

                // Use the shortNames in the mapping table to lookup the full path to element in source and target

                if (!fName.equals(""))
                    fPath = sourcePathMap.get(fName).path;

                if (!tName.equals(""))
                    tPath = targetPathMap.get(tName).path;

//                // Is this element an array if so, make sure the target is also ?
//                if (sourcePathMap.get(fName).getType().equals("ARRAY")) {
//                    if (!targetPathMap.get(tName).getType().equals("ARRAY")) {
//                        throw new C2SIMException("Source ARRAY doesn't map to target ARRAY");
//                    }   // Exception

                // Does this C2SIMSchemaMapping entry have a child arrayMapping?
                if (!map.arrayMapping.isEmpty()) {

                    // Get the mapping for this array                        
                    arrayMapping = map.arrayMapping;

                    // See if we have "" in either fName or tName and process accordingly
                    if ((fName.equals("")) || (sourcePathMap.get(fName).getType().equals("LEAF"))) {

                        // Add one instance of the array root to the target
                        destElement = addNode(target, tPath);

                        processArray(source, destElement, sourcePathMap, targetPathMap.get(tName).array.schemaMap, arrayMapping);
                    }


                    // See if target name is blank
                    if (tName.equals("")) {
                        // Get the list of elements at this position in the source
                        elementList = getData(fPath, source);

                        for (Element el : elementList) {
                            // Get the data for this array and add it to the target
                            processArray(el, target, sourcePathMap.get(fName).array.schemaMap, targetPathMap, arrayMapping);
                        }
                    }

                    // If the target is a NODE then just process the first in the list of source elements
                    else if (targetPathMap.get(tName).getType().equals("LEAF")) {

                        // Get the list of elements at this position in the source
                        elementList = getData(fPath, source);

                        // Add one instance of the array root to the target
                        destElement = addNode(target, tPath);

                        // Process the first element
                        processArray(elementList.elementAt(0), target, sourcePathMap.get(fName).array.schemaMap, targetPathMap, arrayMapping);
                    }

                    else {
                        // Process the elements one at a time 
                        // Get the list of elements at this position in the source
                        elementList = getData(fPath, source);

                        for (Element el : elementList) {

                            // Add one instance of the array root to the target
                            destElement = addNode(target, tPath);

                            // Get the data for this array and add it to the target
                            processArray(el, destElement, sourcePathMap.get(fName).array.schemaMap, targetPathMap.get(tName).array.schemaMap, arrayMapping);
                        }   // for

                    }   // array to array
                }   // there is an array


                else {  // Not an array, a single element

                    // Get the data  (Should be one element
                    elementList = getData(fPath, source);

                    // If the element if blank or was missing in source then use the default value        
                    if (elementList.isEmpty()) {
                        if (!map.defaultValue.equals("")) {
                            elementList.add(new Element(fPath.lastElement().getLocalPart(), Namespace.getNamespace(fPath.lastElement().getNamespaceURI())));
                            elementList.firstElement().addContent(map.defaultValue);
                        }   // had default
                        else
                            continue;
                    }   // if blank

                    // Add the data to the output document
                    addData(elementList, target, tPath);

                }   // Not an array
            }   // try

            catch (Exception e) {
                C2SIM_Server.debugLogger.error("Exception caught in processArray. Mapping " + fName + " to " + " tName");
                throw new C2SIMException("Exception caught in processArray. Mapping " + fName + " to " + tName);
            }   // catch   // catch
        }  // for each field in source             


    }   // processArray


    /****************/
    /*  getData     */
    /****************/

    /**
    getData - Given a path and a parsed document, find the last element in the path and return the text
    @param fromPath - Vector of QName objects - Path to the element needed
    @param source - Element - Start of searh
    @return - List (Vector) of Elements satisfying the request
    @throws C2SIMException 
    */
    static Vector<Element> getData(Vector<QName> fromPath, Element source) throws C2SIMException {

        Element e;
        Vector<Element> result = new Vector<>();

        try {
            // Locate the element
            e = findParentElement(fromPath, source);

            // If we didn't find the parent the data must not be here
            if (e == null)
                return result;

            // Get child/ children of the parent
            result = new Vector<Element>(e.getChildren(fromPath.lastElement().getLocalPart(), Namespace.getNamespace(fromPath.lastElement().getNamespaceURI())));

            // If result exists remove it from source.
            if (!result.isEmpty())
                e.removeChildren(fromPath.lastElement().getLocalPart(), Namespace.getNamespace(fromPath.lastElement().getNamespaceURI()));
            return result;
        }
        catch (Exception be) {
            C2SIM_Server.debugLogger.error("Exception caught in getData " + be);
            throw new C2SIMException("Exception caught in getData ", be);
        }

    }   // getData()


    /***************************/
    /*  findParentElement      */
    /***************************/

    /**
    * findParentElement - Given a path and a parsed document, find the parent of the element
    @param path - Vector of QName objects - Path to a specific element
    @param base - Element - Starting point of search
    @return - Element target of search
    */
    static Element findParentElement(Vector<QName> path, Element base) {
        String data = "";
        String tempDat = null;
        Element e;
        Element rootElement;


        // Does the base match?
        e = base;

        if (e.getName().equals(path.elementAt(0).getLocalPart())
                && e.getNamespaceURI().equals(path.elementAt(0).getNamespaceURI())) {

            // The element name matches.  If the length of the path is 1 then we are only looking for the root and we have a match       
            if (path.size() == 1) {
                return e;
            }

            // Search through the rest of the path checking matches until the last element 
            for (int i = 1; i < path.size() - 1; ++i) {

                // Is there a child with this path name and namespace URI?      (Create JDOM Namespace)
                e = e.getChild(path.elementAt(i).getLocalPart(), Namespace.getNamespace(path.elementAt(i).getNamespaceURI()));

                // No match return null
                if (e == null) {
                    return e;
                }
            }   // for
            // Have a match at last elemeht
            return e;
        }   // root matches
        return null;    // root didn't match
    }   // findParentElement


    /*********************/
    /*  findElement      */
    /*********************/
    // Given a path and a parsed document, find the parent of the element
    //      (Poor Man's XPath)
    /**
    * findElement Given a path and a parsed document, a single Element 
    @param path - Vector of QName objects - Path to a specific element
    @param base - Element - Starting point of search
    @return - Element target of search
    */
    static Element findElement(Vector<QName> path, Element base) {
        String data = "";
        String tempDat = null;
        Element e;
        Element rootElement;


        // Does the base match?
        e = base;

        if (e.getName().equals(path.elementAt(0).getLocalPart())
                && e.getNamespaceURI().equals(path.elementAt(0).getNamespaceURI())) {

            // The element name matches.  If the length of the path is 1 then we are only looking for the root and we have a match       
            if (path.size() == 1) {
                return e;
            }

            // Search through the rest of the path checking matches until the last element 
            for (int i = 1; i < path.size(); ++i) {

                // Is there a child with this path name and namespace URI?      (Create JDOM Namespace)
                e = e.getChild(path.elementAt(i).getLocalPart(), Namespace.getNamespace(path.elementAt(i).getNamespaceURI()));

                // No match return null
                if (e == null) {
                    return e;
                }
            }   // for
            // Have a match at last elemeht
            return e;
        }   // root matches
        return null;    // root didn't match
    }   // findElementElement



    
    /****************/
    /* addData      */
    /****************/
    // Add data using to an element using base as the root
    /**
    * addData - Add new element along with any intermediate elements required
    @param data - Element - Element with data to be added
    @param base - Element - Starting point
    @param toPath - Vector of QName objects full path to the data
    @throws C2SIMException 
    */
    static void addData(Vector<Element> data, Element base, Vector<QName> toPath) throws C2SIMException {
        // Iterate through the list of path elements inserting nodes as needed
        //      The firse element in the list is the root.
        //      The last element is the leaf with data

        Element currElement = base;

        try {
            for (int i = 1; i < toPath.size() - 1; ++i) {

                // Get element name from toPath
                String eName = toPath.elementAt(i).getLocalPart();

                // Create a JDOM namespace object
                Namespace ns = Namespace.getNamespace(toPath.elementAt(i).getNamespaceURI());

                // Does the child exist?
                if (currElement.getChild(eName, ns) == null) {
                    // No create it
                    currElement.addContent(new Element(eName, ns));
                }   // if

                // Move down the tree, make, possibly new child current
                currElement = currElement.getChild(eName, ns);
            }   // for

            // We have worked our way down the intermediate nodes to the leaf node and now insert the data
            for (Element ed : data) {
                // Get the data from the source, create a new element and add name, namespace and text;
                Element ne = new Element(toPath.lastElement().getLocalPart(), Namespace.getNamespace(toPath.lastElement().getNamespaceURI()));
                ne.addContent(ed.getText());
                currElement.addContent(ne);
            }   // for

        }   // try
        catch (Exception e) {
            throw new C2SIMException("Exception caught in addData, adding " + data.firstElement() + " to " + simplePath(toPath), e);
        }   // catch   // catch

    }   // addData()


    /*****************/
    /* addNode      */
    /*****************/
    /**
    * addNode - Add an Element to the end of a list
    @param base - Element Starting point
    @param path - Vector of QName objects
    @return Element added
    @throws C2SIMException 
    */
    static Element addNode(Element base, Vector<QName> path) throws C2SIMException {

        Element el;
        try {
            for (int i = 1; i < path.size(); ++i) {

                // Get element name from toPath
                String eName = path.elementAt(i).getLocalPart();

                // Create a JDOM namespace object
                Namespace ns = Namespace.getNamespace(path.elementAt(i).getNamespaceURI());

                // Does the child exist or are we at the end of the list?
                if ((base.getChild(eName, ns) == null) || (i == path.size() - 1)) {
                    // Create it
                    el = new Element(eName, ns);
                    base.addContent(el);
                    base = el;
                }   // if
                else
                    // Move down the tree, make, possibly new child current
                    base = base.getChild(eName, ns);
            }   // for

        }   // try
        catch (Exception e) {
            C2SIM_Server.debugLogger.error("Exception in addNode adding " + simplePath(path));
            throw new C2SIMException("Exception in addNode adding " + simplePath(path));
        }
        return base;
    }  // addNode()    

    
    /****************/
    /* simplePath   */
    /****************/
    /**
    * simplePath * Create a string containing list of Element names from list of QName objects
    @param path - Vector of QNames to be used as source of names
    @return - String list of Local Names from QNames separated by "/"
    */
    static String simplePath(Vector<QName> path) {
        
        // Format path for displayinto string containing only local parts separated by "/"
        
        // Get first element
        String res = path.get(0).getLocalPart();
        
        // Get remaining elements each preceded by "/"
        for (int i = 1; i < path.size(); ++i) {
            res += "/" + path.get(i).getLocalPart();
        }   // for
        
        return res;   
    }   // simplePath())
    
}   // C2SIM_Mapping Class
