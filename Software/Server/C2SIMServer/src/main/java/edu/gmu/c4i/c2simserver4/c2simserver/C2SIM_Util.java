/*----------------------------------------------------------------*
|    Copyright 2001-2022 Networking and Simulation Laboratory     |
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

import edu.gmu.c4i.c2simserver4.schema.C2SIMDB;
import edu.gmu.c4i.c2simserver4.schema.C2SIMMessageDefinition;
import edu.gmu.c4i.c2simclientlib2.C2SIMHeader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;


/**
 * <h1>C2SIM_Util</h1>
 * Class containing  utility methods called by various parts of the server
 * @author Douglas Corner - George Mason University C4i/Cyber Center
 */
// General utility methods
public class C2SIM_Util {

    /********************/
    /* Global variables */
    /********************/
    static Namespace c2sim_NS = Namespace.getNamespace("http://www.sisostds.org/schemas/C2SIM/1.1");
    static Namespace ibml09_NS = Namespace.getNamespace("http://netlab.gmu.edu/IBML");
    static Namespace cbml_NS = Namespace.getNamespace("http://www.sisostds.org/schemas/c-bml/1.0");
    static Namespace core_NS = Namespace.getNamespace("http://www.sisostds.org/schemas/c2sim/1.0");
    static Namespace msdl_NS = Namespace.getNamespace("urn:sisostds:scenario:military:data:draft:msdl:1");

    public static HashMap<String, C2SIMMessageDefinition> mdIndex;
    public static String SISOSTD = "SISO-STD-C2SIM";
    public static C2SIMDB db;

    // Datetime formats for SimpleDateFormat
    public static String c2simFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static String ibml09Format = "yyyyMMddHHmmss'.'SSS";

    // Initialization and Unit database
    public static C2SIM_InitDB initDB;
    public static String initDB_Name = "default";

    public static HashMap<String, Element> unitMap = new HashMap<>();
    public static HashMap<String, String> uuidToName = new HashMap<>();
    public static HashMap<String, String> nameToUUID = new HashMap<>();
    
    public static HashMap<String, Element> forceSideMap = new HashMap<>();

    public static Integer numC2SIM_Units = 0;
    public static Integer numC2SIM_Routes = 0;
    public static Integer numC2SIM_ForceSides = 0;
    public static Integer numInitMsgs = 0;
    

    /****************/
    /* simplePath   */
    /****************/
    /**
    * simplePath - Reformat a path using only the localPart's separated by slashes
    @param path - Vector of QName element names that include namespaces
    @return String containing element names separated by slashes
    */
    public static String simplePath(Vector<QName> path) {
        // numC2SIM_Units
        String result = "";

        // Loop through vector of QName's 
        for (QName n : path) {
            result += ("/" + n.getLocalPart());
        }   // for

        // Remove leading "?" from result
        result.replaceFirst("/", "");

        return result;
    }   // simplePath


    /******************************/
    /*  stripNamespace            */
    /******************************/
    /**
     * stripNamespace - Strip namespace prefixes from xml
    @param xmlIn
    @return XML string without namespace prefixes
    * <abc:def>xxx</abc:def> -> <def>xxx</def>
    */
    public static String stripNamespace(String xmlIn) {
        String REGEX1 = "(<\\/?)[a-z0-9]+:";
        String REPLACE = "$1";

        Pattern p = Pattern.compile(REGEX1);
        Matcher m = p.matcher(xmlIn); // get a matcher object
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, REPLACE);
        }   // while
        m.appendTail(sb);
        return sb.toString();

    }   // stripNamespace()    


    /********************/
    /*  toBoolean      */
    /*******************/
    /**
     * toBoolean - Convert various string/char representations to Boolean
    @param s String containing t, true, y, yes all will be converted to boolean true
    @return Boolean object 
    */
    // Convert various string/char representations to Boolean
    public static Boolean toBoolean(String s) {

        // Did we get the property?
        if (s == null)
            return false;

        String lc = s.toLowerCase();

        switch (lc) {
            case "t":
            case "true":
            case "y":
            case "yes":
                return true;
        }
        return false;

    }   // toBoolean()



    /****************************/
    /* findSingleElementSimple  */
    /****************************/
    /**
    * findSingleElementSimple - Search down from an JDOM element using a path.  Return a single element as a result
    @param path - String - Simple path without namespaces
    @param startingPoint - Element to start search from
    @param ns - Namespace for all elements in search path
    @return - Element being searched path or null if not found
    */
    // Call findElementSimple with the expectation that the result is a single Element
    static Element findSingleElementSimple(String path, Element startingPoint, Namespace ns) {
        List<Element> le = findElementSimple(path, startingPoint, ns);
        if (le.size() != 0)
            return le.get(0);
        else
            return null;
    }   // findSingleElementSimple())


    /***************************/
    /*  findElementSimple      */
    /***************************/
    /**
    * findElementSimple - Search down from an JDOM element using a path.  Return a list of elements as a result
    @param path - String - Simple path withouit namespaces
    @param startingPoint - Element to start search from
    @param ns - Namespace for all elements in search path
    @return - List of Elements being searched for or null if not found
    */
    static List<Element> findElementSimple(String path, Element startingPoint, Namespace ns) {
        Element e;
        Element next;
        List<Element> lastElements = new ArrayList<Element>();

        String[] els = null;    // List of separated element tags

        // Parse the path into separate elements
        els = path.split("/");
        e = startingPoint;

        // Work down using the list of element names until the last element name has been used.  If an elements aren;t found then return null
        for (int i = 0; i < els.length - 1; ++i) {
            next = e.getChild(els[i], ns);
            if (next == null)
                return lastElements;        // Target wasn't found return empty list
            e = next;
        } // for

        // Get the target element (The last one in the list).
        String n = els[els.length - 1];
        lastElements = e.getChildren(els[els.length - 1], ns);

        // We must have found the target
        return lastElements;
    } // findElementElement


    /***********************/
    /* createElementStack  */
    /***********************/
    // Create linked Elemets from slash "/" separated string of Element names using provided namespace
    //  Attach list to attachmentPoint
    //  Return the last element in the element list
    /**
    * createElementStack = Create a set of elements started from a given point.
    @param elements - String of elements names separated by slashes
    @param attachmentPoint - Element to attach new elements to
    @param ns - Namespace used for all elements
    @return - Final element in list
    */
    public static Element createElementStack(String elements, Element attachmentPoint, Namespace ns) {
        String elArray[];
        Element n = null;

        // Separate list of elements to be linked
        elArray = elements.split("/");

        // Top of stack is first elemenet name 
        Element upper = new Element(elArray[0], ns);
        attachmentPoint.addContent(upper);

        // Work through list of element names starting with second name
        for (int i = 1; i < elArray.length; ++i) {

            // Create new element
            n = new Element(elArray[i], ns);

            // Add it to previous element
            upper.addContent(n);

            // New Element is now previous element
            upper = n;
        }   // createElementStack()

        // Return last element in stack
        return n;

    }   // createElementStack()


    /********************/
    /* convertDate      */
    /********************/
    /**
    * convertDate - Convert dates using SmipleDateFormat between two standard formats MIP and ISO
    @param fromDate - String of date being converted
    @param fromType - Type of date to convert from.  Either "C2SIM" or "IBML09" (MIP)
    @param toType - Type of date to convert to.  Either "C2SIM" or "IBML09" (MIP)
    @return - String converted date.
    @throws C2SIMException 
    */
    public static String convertDate(String fromDate, String fromType, String toType) throws C2SIMException {
        String fromFormat = "";
        String toFormat = "";
        String c2simFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        String ibml09Format = "yyyyMMddHHmmss'.'SSS";
        String toDate = "";
        // Determine the formats to use
        // IBML09 uses "00" or possibly "0" to mean now.  Translate to a "0"
        if ((fromDate.equals("0")) || (fromDate.equals("00")))
            return "0";
        if (fromType.equalsIgnoreCase("C2SIM"))
            fromFormat = c2simFormat;
        else if (fromType.equalsIgnoreCase("IBML09"))
            fromFormat = ibml09Format;
        if (toType.equalsIgnoreCase("C2SIM"))
            toFormat = c2simFormat;
        else if (toType.equalsIgnoreCase("IBML09"))
            toFormat = ibml09Format;
        // Parse the date converting it to a Date object
        SimpleDateFormat fromSDF = new SimpleDateFormat(fromFormat);
        SimpleDateFormat toSDF = new SimpleDateFormat(toFormat);
        Date date = null;
        try {
            date = fromSDF.parse(fromDate);
            toDate = toSDF.format(date);
        }
        catch (ParseException pe) {
            C2SIM_Server.debugLogger.error("Exception parsing date " + pe.getCause());
            return "0";
        }
        return toDate;
    } // convertDate()


    /********************/
    /*  readInputFile   */
    /********************/
    /**
    * readInputFile - Read inut file into string
    @param filename - Name of file to be read
    @return - String containing contents of file
    @throws Exception 
    */
    // Read inut file into string
    static String readInputFile(String filename) throws Exception {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str + "\n");
            }   // while
        }   // try
        catch (FileNotFoundException fnf) {
            throw new Exception("File not found");
        }   // FileNotFoundException
        catch (IOException io) {
            throw new Exception("I/O Exception reading file");
        }   // IOException
        return sb.toString();

    }   // readInputFile()


    /*******************/
    /*  xmlToStringD   */
    /*******************/
    /**
    * xmlToStringD - Convert a JDOM Document to an xml string
    @param doc - JDOM DOcument
    @param t - C2SIM_Transaction
    @return - XML String
    */
    // Convert XML document to text
    public static String xmlToStringD(Document doc, C2SIM_Transaction t) {
        Format fmt = Format.getCompactFormat();
        fmt.setLineSeparator(LineSeparator.NL);
        XMLOutputter xmlOut = new XMLOutputter(fmt);
      
        // If not otherwise specified set C2SIM Version to default
        if (t.getc2SIM_Version() == null){
            String c2simVer = C2SIM_Server.props.getProperty("server.defaultC2SIM_Version");
            t.setc2SIM_Version(c2simVer);
        }
        String newXML = xmlOut.outputString(doc);

        // If t is null the call is probably for debugging.  Just return the xml without dealing with headers.
        if (t == null)
            return newXML;
  
        // If this is a C2SIM
        if (t.getProtocol().equals(SISOSTD)) 
            newXML = C2SIMHeader.insertC2SIM(newXML, t.getSender(), t.getReceiver(), "Inform", t.getc2SIM_Version());
        
        return newXML;
    } // xmlToStringD()


}   // C2SIM_Util Class
