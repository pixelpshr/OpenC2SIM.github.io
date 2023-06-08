/*----------------------------------------------------------------*
|   Copyright 2009-2023 Networking and Simulation Laboratory      |
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

// Version of C2SIMGUI C2SIM Editor that works with REST and STOMP
// and features controls for the Reference Implementation C2SIM Server

// capable of loading, parsing, editing, and pushing CBML Light and IBML09
// reports and orders using REST

// command line parameter 'true' enables debug printing

package edu.gmu.netlab;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Date;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.*;
import com.jaxfront.core.dom.DOMBuilder;
import com.jaxfront.core.dom.Document;
import com.jaxfront.core.help.HelpEvent;
import com.jaxfront.core.help.HelpListener;
import com.jaxfront.core.schema.ValidationException;
import com.jaxfront.core.ui.TypeVisualizerFactory;
import com.jaxfront.core.util.LicenseErrorException;
import com.jaxfront.core.util.URLHelper;
import com.jaxfront.core.util.io.BrowserControl;
import com.jaxfront.core.util.io.cache.XUICache;
import com.jaxfront.pdf.PDFGenerator;
import com.jaxfront.swing.ui.editor.EditorPanel;
import com.jaxfront.swing.ui.editor.ShowXMLDialog;

import com.bbn.openmap.*;
import com.bbn.openmap.event.*;
import com.bbn.openmap.gui.*;
import com.bbn.openmap.proj.ProjectionStack;
import com.bbn.openmap.tools.symbology.milStd2525.PNGSymbolImageMaker;
import com.bbn.openmap.tools.symbology.milStd2525.SymbolChooser;
import com.bbn.openmap.tools.symbology.milStd2525.SymbolReferenceLibrary;
import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.MapBean;
import com.bbn.openmap.event.PanMouseMode;

import java.util.*;
import java.net.*;

// DOM and XPATH
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import javax.xml.namespace.NamespaceContext;
import edu.gmu.c4i.c2simclientlib2.*;
import static edu.gmu.netlab.MilOrg.bml;
import static edu.gmu.netlab.Subscriber.bml;
import org.w3c.dom.*;

public class C2SIMGUI extends JFrame implements WindowListener, HelpListener, 
    ItemListener, ActionListener {
    
    // Version (comatible C2SIM_SMX_LOX_CWIX2023v2.xsd)
    public static String version = "2.13.6";

/**
 * BMLC2 GUI (Initially BMLGUI) now C2SIMGUI
 * 
 * Started : 1/20/2009
 * Used Tools: This project uses two great tools
 * 	1. Xcentric JAXFront. WebSite http://www.jaxfront.org
 	2. BBN OpenMap (Now a Raytheon Company). WebSite http://www.openmap.org/
 Purpose : The purpose of this project is to develop an Easy End-User Interface for Viewing and 
      Editing C2SIM Orders and Reports. It is a Run-Time GUI Generation  Based on BML-XML Schema.
 	Also an important development goal is to make it a Open-Source application.
 Functionality : This Application will provide the user with the following functionality:
      1. Create a new order : 
          a)  The user can create a new order (create the XML file that will be used by C2SIM 
              Web Services that will process it and send it to the JC3IDEM database).
          b)  The user can create any type of order (Ground, Air, �)
          c)	The user can enter as many tasks as he wants (Multiple or single task).
          d)	This is done through an easy Java Swing desktop window
          e)	The resulting order should conform to the C2SIM XML Schema
      2.  View an order :
          a)	The user can open and view an existing order (order that was created previously 
              using this C2SIMGUI tool or by anything else).
          b)	[Validation] The user can run a compatibility (conformability) test to make sure
              that the order (XML file)  is compatible with the C2SIM Schema  (XSD file).
      3.  Edit an order :
          a)	The user can open and edit an existing order (order that was created previously 
      	using this C2SIMGUI tool or by anything else).
          b)	The user can add a new task, modify an existing task or delete some task.
      4.  Validate an Order :	[Validation] The user can run a compatibility (conformability) 
          test to make sure that the order (XML file)  is compatible with the C2SIm Schema  (XSD file).
      5.  Serialize an order:	[Serialization] The user can see XML version of what is he editing
          on the screen (the XML source code)
      6.  Save an order :
          a)	The user can save the order (what he gets on the screen)
          b)	The user can have the option to save the order weather it is valid 
      	(conforms to the C2SIM Schema) or not ( he or somebody else can review it later).
      7.  Print an order :
          a)	The user can print the order (what he gets on the screen) in a form layout.
          b)	The user can save /print the order (what he gets on the screen) in a PDF Format.
          c)	The user can print the order source XML code.
      8.  Reports :	The user can open C2SIM Reports 
      9.  Submit an Order / Report:
          a)	The user can submit the order / Report (invoke the web service
      10. Maps :
          a)	The user can see any coordinates values from the GUI on the Map. Points, lines 
      	and shapes are drawn based on geo data from the GUI.
 * 
 * @author	Mohammad Ababneh, C4I Center, George Mason University
 * @since	4/9/2010
 * 
 * @author	Eric Popelka, C4I Center, George Mason University
 * @since	6/29/2013
 * 
 * @author      J. M. Pullen, C4I & Cyber Cneter, George Mason University
 * @since       1/1/2015
 */
    // Main Application's Frame Height and Width
    private final static int WINDOW_HEIGHT = 768; 
    private final static int WINDOW_WIDTH = 1440;

    // Main Frame Panels
    private JPanel centerPanel;
    private JPanel editorMapPanel;
    private JPanel editorButtonPanel;
    private JPanel topButtonPanel;

    private JSplitPane splitPane;
    EditorPanel editor;
    
    // grt coordinates text
    private JLabel getCoordsLabel;
    private JTextField getLatitude;
    private JTextField getLongitude;
    File xmlTempfile = null;

    // Report Info Combo Box
    public JLabel subscriberStatusLabel;
    public JLabel initStatusLabel;
    public JLabel serverStatusLabel;

    // Buttons
    public JButton serverStopButton = new JButton("STOP/RESET SERVER");
    public JButton serverShareButton = new JButton("INIT/SHARE/START SERVER");
    public String initText = "REMOVE INITIALIZE ICONS";
    public JButton initRemoveButton = new JButton(initText);
    public String orderText = "REMOVE ORDER ICONS";
    public JButton orderRemoveButton = new JButton(orderText);
    public String reportText = "REMOVE REPORT ICONS";
    public JButton reportRemoveButton = new JButton(reportText);;
    public JButton getCoordsButton;
    public String coordsText = "INSERT COORDS IN XML";
    public JButton coordsToXmlButton = new JButton(coordsText);
    public JButton loadReportButton;
    public JButton reportListenerButton;
    public JButton tracksButton;
    public JButton subscribeButton;
    public JButton recordButton;
    public JButton playButton;
    public JButton pauseButton;
    public JLabel recordPlayStatus = new JLabel("RECORDING");

    // message types
    String c2simProtocol = "SISO-STD-C2SIM";
    String c2simProtocolVersion = "CWIX2023v1.0.2";
    String c2simPath = "C2SIMServer/c2sim";
    String conversationID = "";
    String bmlPath = "BMLServer/bml";
    String c2simRootTag = "MessageBody";
    String c2simOrderReportDomain = "DomainMessageBody";
    String c2simSystemCommandDomain = "SystemCommandBody";
    String c2simInitializationDomain = "C2SIMInitializationBody";
    String objectInitializationDomain = "ObjectInitializationBody";
    String c2simOrderMessageType = "OrderBody";
    String c2simReportMessageType = "ReportBody";
    String c2simUnitCountTag = "<unitDatabaseSize>";
    String c2simUnitCountTagEnd = "</unitDatabaseSize>";
    String c2simTaskTag = "ManeuverWarfareTask";
    String c2simAsxTag = "AutonomousSystemManeuverWarfareTask";
    String c2simTaskeeTag = "PerformingEntity";
    String c2simLatitudeTag = "Latitude";
    String c2simLongitudeTag = "Longitude";
    String c2simReporteeWhoTag = "SubjectEntity";
    String c2simReporterWhoTag = "ReportingEntity";
    String c2simObserveeTag = "ActorReference";
    String playbackReportingEntity = "";
    String playbackReportID = "";
    
    // state variables
    JLabel documentTypeLabel;
    private float reportLatitude = 0f;          // for loading report
    private float reportLongitude = 0f;         // for loading report

    public static boolean anIconIsOnTheMap = false;
    Document currentDom = null;                 // Current Jaxfront Dom Document
    String currentXmlString = null;
    String currentLanguage = "en";              // English as default
    boolean pushingInitialize = false;
    boolean initIconsOnScreen = false;
    boolean orderIconsOnScreen = false;
    boolean reportIconsOnScreen = false;

    public static MapHandler mapHandler;  // OpenMap MapHandler
    public static OMToolSet omts;
    public static float panFactor = .2f;
    public static RouteLayer[] routeLayers;// Array of Route Layers
    private final static int routeLayerArraySize = 500; // number of icon layer on map
    public int routeLayerIndex = 0;   	  // Index of Route Layer
    private LayerHandler layerHandler;
    XPath xpathCBML = null;                             // CBML XPath
    XPath xpathMSDL = null;                             // MSDL XPath
    public static RouteLayer routeLayerOPORD = null; 	// Array of Route Layers
    public static RouteLayer routeLayerMSDL = null; 	// MSDL Layer
    private Properties c2mlProps;
    private MapPanel mapPanel;
    public boolean pushingJaxFront = false;
    public boolean stompIsConnected = false;
    public boolean listenToXml = false;
    public boolean showTracks = false;
    public boolean gettingCoords = false;
    private boolean interruptGettingCoords = false;
    public boolean enteringCoords = false;
    private boolean interruptEnteringCoords = false;
    public boolean movingCoords = false;
    public boolean loadingReport = false;
    private boolean interruptReportLoading = false;
    public boolean runRecorder = false;
    public boolean runPlayer = false;
    public boolean pausePlayer = false;
    public boolean runServerPlayer = false;
    public boolean listeningWhenPlayStarted = false;
    public Player player = null;
    public File logFile = null;
    public Recorder recorder  = null;
    public File captureFile = null;
    public boolean playFailed = false;
    public boolean recordFailed = false;
    public boolean runningServerTest = false;
    String startupInitialization = null;
    String lateJoinerInitialization = null;
    ValidateServer serverTest;
    String defaultC2simProtocolVersion = "CWIX2023v1.0.2";
    
    // identity of previous report to avoid graphing duplicates
    public String lastUnitID = "";
    public String lastHostility = "";
    public String lastLatitude = "";
    public String lastLongitude = "";
    static boolean ignoreXmlWithDupID = false;
    static boolean askAboutCheckingID = false;
    static boolean reportsDupIdAsked = false;
    
    // possible things represented by icons on the map
    public enum IconType {
        NULL,
        INITIALIZE,
        ORDER,
        REPORT
    }
    
    // protocols supported:
    enum Variant {
        IBML,   // (or IBML09) from MSG-085
        CBML,   // from SISO C-BML
        C2SIM   // from SISO C2SIM v1.0.1
    }
    
    public boolean c2simProtocolOK(String testProtocol){
        return (testProtocol.compareTo(C2SIMProtocolVsnMax) <= 0) &&
               (testProtocol.compareTo(C2SIMProtocolVsnMin) >= 0);
    }// end c2simProtocolOK()
	
    public static C2SIMGUI bml;                         // bml variable for main GUI frame
  
    public static Webservices ws;                       // to access REST/STOMP

    public static boolean debugMode = false;// to print parameters as GUI runs

    public static MapBean mapBean;                      // Open Map mapBean

    /** Property for space separated layers. "c2ml.layers" */
    public static final String layersProperty = "c2ml.layers";

    /** The name of the resource file. "C2SIMGUI.properties" */
    public static String c2mlResources = "bmlc2gui.properties"; 

    // state information
    String bmlString = new String();    // C2SIm Document converted to a csv
    String[] bmlStringArray;            // array from bmlstring
    String shapeType;			// Shape Type
    int shapeCoords;			// Shape's number of coordinates
    String[] tempShapeOPORD;
    String label;
    String stringOPORDName;
    String[] tempLocationsOPORD;
    int locationCoords;
    int coordIndex, transferIndex;
    String tmpFileString;
    String[] bmlLatCoords;
    String[] bmlLonCoords;
    int numCoords = -1;
    int highlightedCoord = -1;
    int nextCoord = -1;
    int selectedCoord = -1;
    String lattmp = "";
    String lontmp = "";
    boolean redrawing = false;
    int xuiCacheHash = 0;
    String loadedXml = "";
    String compareXml = "";
    static boolean platformIsWindows;
    static boolean platformIsLinux;
    static String localAddress = "";
    static boolean mapOPORD = false;
    static boolean mapMSDL = false;
    static String osName;
    static boolean initializationDone = false;
    float clickLatitude = 0;
    float clickLongitude = 0;
    
    // parameters for XML document
    String root = null; // Document Root node
    URL xsdUrl = null;  // String for Schema File
    URL xmlUrl = null;  // String for XML File / Document
    URL xuiUrl = null;  // String for JaxFront XUI file / Style view file
    URL tmpUrl = null;	// String for Temporary XML File / Document
    public String xmlReport = null;// holds latest report received from server
    public String xmlOrder = null; // holds latest C2SIM order received from server
    public static String orderDomainName = "C2SIM"; // Order push / pull domain
    public String reportBMLType = "";        // cLient Report BML Type
    public String orderBMLType = "";         // cLient Order BML Type
    MouseDelegator mouseDelegator;           // determine how mousde is used
    String holdMouseModeID = "Gestures";     // holds MouseMode while doing getCoords
    Layer[] layers;                          // layers of the mapHandler

    public static boolean mapGraph = false;  // check if a graph exists on the map
    public static org.w3c.dom.Document w3cBmlDom;
	
    private XPathFactory xpathFactory = null;
    private XPath xpath = null;
    private DocumentBuilderFactory w3cDocFactory = null;
    private DocumentBuilder w3cDocBuilder = null;
    private org.w3c.dom.Document w3cReportInfoDoc;

    public Subscriber subscriber = null; // used when online
    public volatile Thread threadSub;
    public RepeatReportC2SIM repeatC2SIM = null;// for repeatReport
    public volatile Thread threadRepeat;
    public boolean sendingRepeat = false;
    public java.util.concurrent.LinkedBlockingQueue<String> localQueue =
        new java.util.concurrent.LinkedBlockingQueue<>();;// used when not online
  
    // Variables to hold configuration file values
    static String mapScale;                  // scale set on map at startup
    static String startSubscribed;           // start gui subscribed to report server
    static String centerUnit;                // unit to show first for reports, thus in center of map
    static String c2simns;                   // namespace string for C2SIM XML documents
    static String ibmlns;                    // namespace string for IBML09 XML documents
    static String cbmlns;                    // namespace string for CBML XML documents
    static String submitterID;               // Client SubmitterID variable
    static String serverName = "";           // Server DNS name
    static String serverPassword;            // InitC2SIM ServerPassword variable
    static String C2SIMProtocolVsnMin;       // 0.0.9 to 1.0.2
    static String C2SIMProtocolVsnMax;       // 0.0.9 to 1.0.2
    static String C2SIMProtocolVersionSend;  // version to send
    static String restPort = "8080";         // assigned port for RESTful transaction
    static String stompPort = "61613";       // assigned port for STOMP connection
    static String generalBMLFunction;        // Order or Report
    static String generalBMLType;            // CBML or IBML 
    static String sbmlUnitInfoDomainName;    // Unit info domain name
    static String lateJoinerMode;            // 1 or 0 - to run in late joiner mode or not
    static String initMapLat;                // Initial Map Latitude
    static String initMapLon;                // Initial Map Longitude
    static String reportOrderScale;          // Report or Order Zoom scale value
    static String autoDisplayInit;           // 1 to autodisplay; anything else not to
    static String autoDisplayReports;        // C2SIM, IBML or CBML to select server reports
    static String autoDisplayOrders;         // C2SIM, IBML or CBML to select server orders
    static String saveAutoDisplayOrders;     // saves previous when receiving XML reports
    // by choosing values of the following we set whether
    // the GUI is showing gound truth or perceived truth 
    static String displayBluePosition;       // 1 or 0 to display blue position reports
    static String displayBlueObservation;    // 1 or 0 to display blue observations from red
    static String displayRedPosition;        // 1 to 0 to display red position reports
    static String displayRedObservation;     // 1 or 0 to display red observations from blue
    static String displayOtherPosition;        // 1 to 0 to display other position reports
    static String displayOtherObservation;     // 1 or 0 to display observations of other
    static String startWithListeningOn;      // 1 or 0 to have ListenToXML and ShowTracks start on
    static String recordingProtocol;         // C2SIM, BML, or ALL
    static String playbackProtocol;          // C2SIM, BML, or ALL
    static int playbackTimescale;            // speedup factor used to divide intermessage time
    static int playbackTimelimit;            // numbe of seconds to play; 0 if no limit
    static String playbackSubmitter;         // identifies source of XML message
    static String guiFolderLocation;         // GUI URL folder location
    static String guiLocationXML;            // XML config file location
    static String guiLocationXSD;            // XSD config file location
    static String guiLocationXUI;            // XUI config file location
    static String delimiter = "/";           // set to "/" for Unix
    static String schemaFolderLocation;      // schema work folder
    static String cbmlOrderSchemaLocation;   // CBML Light Order schema
    static String cbmlReportSchemaLocation;  // CBML Light Report schema
    static String ibml09OrderSchemaLocation; // IBML09 Order schema
    static String ibml09ReportSchemaLocation;// IBML09 Report schema
    static String c2simOrderSchemaLocation;  // C2SIM Order schema
    static String c2simReportSchemaLocation; // C2SIM Report schema
    static String c2simInitSchemaLocation;   // C2SIM Initialize schema
    static String asxReportSchemaLocation;   // C2SIM with ASX schema
    static String asxOrderSchemaLocation;    // C2SIM with ASX schema
    static String c2simCyberSchemaLocation;  // C2SIM Cyber schema
    static String recordingFilesLocation;    // default recordings directory
    static String playbackFilesLocation;     // detault playbacks directory
    static String xuiFolderLocation;         // Location of XUI file
    static String warnOnReportSeq;           // 1 to popup out-of-time-sequence warning
    static String orderIDXPath;              // OrderID string of config file XPath
    static String whereXPathTag;             // At Where Location XPath
    static String routeXPathTag;             // Route Where Location XPath	
    static String whereIdLabelTag;           // Where ID Label
    static String whereShapeTypeTag;         // Where shape type
    static String routeIdLabelTag;           // Route label
    static String latlonParentTag;           // Lat Lon Parent xpath
    static String latTag;                    // Lat xpath
    static String lonTag;                    // Lon Xpath
    static String routeFromViaToTag;         // Route from-via-to xpath
    static int serverTestStompWait;          // time to wait on server test reply
    static String blueSideName;              // name of Blue forseSide
    static String checkForDuplicateID;       // if "1" enable Report & Order ID 
                                             // duplicate checking
    static String askAboutCheckDuplicateID;  // if "1" and duplicate found, popup 
                                             // request to continue checking
    static String reportRepeatms;            // ms between report repeat 0 if no repeat

    NamespaceContext nsContext= new NamespaceContext() {
        public Iterator getPrefixes(String namespaceURI) {
                return null;
        }
        public String getPrefix(String namespaceURI) {
                return null;
        }
        public String getNamespaceURI(String prefix) {
                String uri = null;
        if (prefix.equals("CBML"))
            uri = "urn:sisostds:bml:coalition:draft:cbml:1";
            return uri;
        }
    };
	
    /**
    * Constructor (no-arg only)
    */
    public C2SIMGUI() {
        super();
        init();
    
        // XPath
        xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();
        xpathCBML = xpathFactory.newXPath();
        xpathCBML.setNamespaceContext(nsContext);
        xpathMSDL = xpathFactory.newXPath();
        xpathMSDL.setNamespaceContext(nsContext);

        // webservices
        ws = new Webservices(this);
        
        // add shutdownHook for subscriber
        Runtime.getRuntime().addShutdownHook(new shutdownStomp());
        Runtime.getRuntime().addShutdownHook(new shutdownRecorder());
        Runtime.getRuntime().addShutdownHook(new shutdownPlayer());
        
    }// end  C2SIMGUI()

    public static void main(String[] args) {	
    
        System.out.println("Version " + version + " of C2SIMGUI Editor");

        // command-line argument option: path to the GUI folder
        // if it does not end with a / add one; this way the
        // GUI folder can be in working directory with no path provided 
        if(args.length > 0) {
            guiFolderLocation = args[0];
            if(!guiFolderLocation.endsWith("/") && 
                !guiFolderLocation.endsWith("\\") &&
                (guiFolderLocation.length() != 0)) {
                guiFolderLocation += delimiter;
            }
        } else {
            System.out.println(
                "Unable to run without parameter main folder location");
            System.exit(0);
        }

        // debug output
        if(args.length > 1)
          debugMode = args[1].equals("true");

        // determine host platform
        osName = System.getProperty("os.name");
        if(debugMode)printDebug("PLATFORM:"+osName);
        platformIsWindows = osName.contains("Windows");
        platformIsLinux = osName.contains("Linux");
        if(platformIsWindows) {
            if(debugMode)printDebug("OS Windows");
            delimiter = "\\";
        }
        else {
            if(debugMode)printDebug("OS Unix-like");
            c2mlResources = "bmlc2guiUnix.properties";
            delimiter = "/";
        }
        try{
            localAddress = InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException uhe) {
            printError("can't get local host address");
            return;
        }
        if(debugMode)printDebug("guiFolderLocation:" + guiFolderLocation);

        // make schema directory paths
        schemaFolderLocation = guiFolderLocation + "Schema/";
        cbmlOrderSchemaLocation =  
            guiFolderLocation + "Schema/CBML/OPORD/CBML_Order.xsd";
        cbmlReportSchemaLocation = 
            guiFolderLocation + "Schema/CBML/OPORD/CBML_Reports.xsd";
        ibml09OrderSchemaLocation = 
            guiFolderLocation + "Schema/IBML09/IBMLOrderPushPulls.xsd";
        ibml09ReportSchemaLocation = 
            guiFolderLocation + "Schema/IBML09/IBMLReports.xsd";
        c2simOrderSchemaLocation = 
            guiFolderLocation + "Schema/C2SIM/C2SIM_SMX_LOX_ASX_v1.0.1_Order_flat.xsd";
        c2simReportSchemaLocation = 
            guiFolderLocation + "Schema/C2SIM/C2SIM_SMX_LOX_ASX_v1.0.1_Report_flat.xsd";
        c2simInitSchemaLocation = 
            guiFolderLocation + "Schema/C2SIM/C2SIM_SMX_LOX_v1.0.1_Init_flat.xsd";
        asxReportSchemaLocation =
            guiFolderLocation + "Schema/C2SIM/C2SIM_SMX_LOX_ASX_v1.0.1_Report_flat.xsd.xsd";
        asxOrderSchemaLocation =
           guiFolderLocation + "Schema/C2SIM/C2SIM_SMX_LOX_ASX_v1.0.1_Order_flat.xsd";
        c2simCyberSchemaLocation = 
            guiFolderLocation + "Schema/Cyber/Cyber_Event.xsd";

        // build path-name for the GUI Config XML file
        if(platformIsWindows)
            guiLocationXML = guiFolderLocation + "Config/BMLC2GUIConfig.xml";
        else
            guiLocationXML = guiFolderLocation + "Config/BMLC2GUIConfigUnix.xml";
        if(debugMode)printDebug("guiLocationXML:" + guiLocationXML);
        if(debugMode)printDebug("url2:" + URLHelper.getUserURL(guiLocationXML));

        // Schema File XSD
        guiLocationXSD = 
            guiFolderLocation + "Config" + delimiter + "BMLC2GUIConfig.xsd";
        if(debugMode)printDebug("guiLocationXSD:" + guiLocationXSD);
        guiLocationXUI = 
            guiFolderLocation + "Config" + delimiter + "BMLC2GUIConfigView.xui";	
        if(debugMode)printDebug("guiLocationXUI:" + guiLocationXUI);
        
        // record and playback directories
        recordingFilesLocation = guiFolderLocation + '/' + "RecordingFiles";
        playbackFilesLocation = guiFolderLocation + '/' + "PlaybackFiles";

        // Set look and feel based on OS type
        try {
            if (platformIsWindows) { 
                // For Windows, use JGoodies looks
                //UIManager.setLookAndFeel(new com.jgoodies.looks.windows.WindowsLookAndFeel());	
                UIManager.setLookAndFeel("com.jgoodies.looks.windows.WindowsLookAndFeel");	

        } else {
                // For Linux, etc., use GTK+ looks
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                    if ("com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(info.getClassName())) {   
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                } 
            }	    	
        }
        } catch (Exception e) {
            printError("Error setting look and feel: " + e.getMessage());
        }

        // startup C2SIMGUI 
        bml = new C2SIMGUI();

        // subscribe to server
        if(startSubscribed.equalsIgnoreCase("yes"))
            bml.startServerSubscribeThread();

    }// end main()
    
    /**
     * getters for report display config string
     */
    boolean getDisplayRedPosition(){
        return displayRedPosition.equals("1");
    }
    boolean getDisplayRedObservation(){
        return displayRedObservation.equals("1");
    }
    boolean getDisplayBluePosition(){
        return displayBluePosition.equals("1");
    }
    boolean getDisplayBlueObservation(){
        return displayBlueObservation.equals("1");
    }
    boolean getDisplayOtherPosition(){
        return displayOtherPosition.equals("1");
    }
    boolean getDisplayOtherObservation(){
        return displayOtherObservation.equals("1");
    }
    
   /**
    * map various <Route> from MapGraphic
    */            
    HashMap<String,PhysicalRoute> allPhysicalRoutes = 
        new HashMap<String,PhysicalRoute>();
    
    // insert a PhysicalRoute into allPhysicalRoutes
    // returns true on error; alse if successful
    boolean insertRoute(PhysicalRoute newRoute, boolean allowReplace){
        if(!allowReplace &&
            allPhysicalRoutes.containsKey(newRoute.routeUuid))
            return true;
        allPhysicalRoutes.put(newRoute.routeUuid,newRoute);
        return false;
    }
    
    // retrieve a Route given its UUID
    // returns the Route; null if not in allPhysicalRoutes
    PhysicalRoute retrieveRoute(String routeUuid) {
        return allPhysicalRoutes.get(routeUuid);
    } 
    	
   /**	
    * map UnitID to report time
    */
   HashMap<String,String> reportTimeMap = new HashMap<String,String>();
   void putReportTime(String unitID, String reportTime)
   {
       reportTimeMap.put(unitID, reportTime);
       
   }
   String getReportTime(String unitID)	
   {
       return reportTimeMap.get(unitID);	
   }
    
    /**
     * map UnitID to corresponding MilOrg class
     * we keep a shadow map indexed by name
     * because there is no use of milorg.set* this will
     * have the same value as the primary map
     */
    HashMap<String,MilOrg> unitMap = new HashMap<String,MilOrg>();
    HashMap<String,MilOrg> nameMap = new HashMap<String,MilOrg>();
    HashMap<String,MilOrg> previousUnitMap;
    HashMap<String,MilOrg> previousNameMap;
    
    /**
     * when playing a replayLog recording it is possible to 
     * use a C2SIMInitialization recorded in the log
     */
    void restartORBAT(){
        previousUnitMap = unitMap;
        unitMap = new HashMap<String,MilOrg>();
        previousNameMap = nameMap;
        nameMap = new HashMap<String,MilOrg>();
        removeInitIcons();
    }
    void revertORBAT(){
        removeInitIcons();
        unitMap = previousUnitMap;
        previousUnitMap = null;
        nameMap = previousNameMap;
        previousNameMap = null;
    }
    int getUnitMapSize(){
        return unitMap.size();
    }
    void addUnit(String unitID, MilOrg newUnit)
    {
        unitMap.put(unitID, newUnit);
        String name = newUnit.getName();
        if(name == null)return;
        if(name.equals(""))return;
        nameMap.put(name, newUnit);
    }
    MilOrg getUnit(String unitID) 
    {
        return unitMap.get(unitID);
    }
    MilOrg getUnitByName(String name)
    {
        return nameMap.get(name);
    }
    String getMilOrgSymbolID(String unitID)
    {
        MilOrg milOrg = getUnit(unitID);
        if(milOrg == null)return null;
        return milOrg.symbolIdentifier;
    }
    String getMilOrgName(String unitID)
    {
        MilOrg milOrg = getUnit(unitID);
        if(milOrg == null)return null;
        return milOrg.name;
    }
    String getMilOrgHostility(String unitID)
    {
        MilOrg milOrg = getUnit(unitID);
        if(milOrg == null)return null;
        return milOrg.hostilityCode;
    }
    
    /** internal class to hold identifier from order or report
     *  and whether it was entered from this GUI
     *  parameters:
     *   idRef - order or re[port identifier String
     *   fromthisGuiRef - true if entered here; flase otherwise
     */
    private class IdPosted {
       String id;
       boolean sentFromThisGui;
       public IdPosted(String idRef, boolean fromThisGuiRef){
           id = idRef;
           sentFromThisGui = fromThisGuiRef;
       }
    }// end class IdPosted
    /**
     * map TaskId for an Order and ReportId for a report 
     * to corresponding RouteLayer class instances
     * keeping track of those entered in this GUI
     * layerGetItemId returns null if none in Map
     * otherwise returns IdPostedd class
     */
    HashMap<String,C2SIMGUI.IdPosted> layerItemMap = 
        new HashMap<String,C2SIMGUI.IdPosted>();
    void layerAddItemId(String itemUuid, String itemTag, boolean fromThisGui){
        if(debugMode)printDebug
            ("LAYERADD|"+itemUuid+"|"+itemTag+"|"+fromThisGui+"|");
        IdPosted newIdPosted = new IdPosted(itemUuid, fromThisGui);
        layerItemMap.put(itemUuid, newIdPosted);
    }
    IdPosted layerGetItemId(String itemUuid){
        IdPosted response = layerItemMap.get(itemUuid);
        if(debugMode){
            if(response != null){
                String uuid = response.id;
                boolean sent = response.sentFromThisGui;
                printDebug("LAYERGET |"+itemUuid+"|"+uuid+"|"+sent+"|");
            }
            else printDebug("LAYERGET null itemUuid");
        }
        return layerItemMap.get(itemUuid);
    }
    void layerRemoveItemId(String itemUuid){
        if(debugMode)printDebug
            ("LAYERREMOVE"+itemUuid+"|");
        layerItemMap.remove(itemUuid);
    }

    /**
     * map UnitID to corresponding RouteLayer class instances in order
     * to allow a unit's reports to stay on a single layer
     */
    HashMap<String,Integer> reportUnitMap = new HashMap<String,Integer>();
    void reportAddUnit(String unitID, Integer newLayerIndex)
    {
        reportUnitMap.put(unitID, newLayerIndex);
    }
    void reportRemoveUnit(String unitID)
    {
        reportUnitMap.remove(unitID);
    }
    Integer reportGetUnitIndex(String unitID) 
    {
        return reportUnitMap.get(unitID);
    }
    void reportClearAllUnits() {
        reportUnitMap.clear();
    }
    
    /**
     * holds an instance of icon UUID and its (lat,lon)
     * @author mpullen
     */
    private class IconUuidCoords {

       String iconUuid;
       float iconLat, iconLon;
       IconType iconType;
       public IconUuidCoords(String uuid, float lat, float lon, IconType it){
           iconUuid = uuid;
           iconLat = lat;
           iconLon = lon;
           iconType = it;
       }
    }// end class iconCoords
    
    /**
     * holds all reports on the current map in order added
     */
    Vector<IconUuidCoords> mapIcons = new Vector<IconUuidCoords>();
    void listAddIconUuid(String uuid, String lat, String lon, IconType it){
        listAddIconUuid(uuid, Float.parseFloat(lat), Float.parseFloat(lon), it);
    }
    void listAddIconUuid(String uuid, float lat, float lon, IconType it){
        mapIcons.add(new IconUuidCoords(uuid, lat, lon, it));
    }
    
    /**
     * returns UUID for icon on map geometrically closest to args
     * rather than take square root we compare square of distance
     */
    String mapGetClosestIconUuid(float testLat, float testLon){
        String returnUuid = "";
        float closestLat = 1000000f, closestLon = 1000000f;// larger than any GDC
        float distanceSquared = Float.POSITIVE_INFINITY; 
        for (IconUuidCoords iconCoords : mapIcons) {
            float deltaLat = iconCoords.iconLat - testLat;
            float deltaLon = iconCoords.iconLon - testLon;
            float testDistance = deltaLat*deltaLat + deltaLon*deltaLon;
            if(distanceSquared > testDistance) {
                distanceSquared = testDistance;
                closestLat = testLat;
                closestLon = testLon;
                returnUuid = iconCoords.iconUuid;
            }
        }
        return returnUuid;
    }
    
    /**
     * clear lists
     */
    void iconUuidListClearAllText() {mapIcons.removeAllElements();}
    void iconUuidListClearReports()
            {for (int i=0; i<mapIcons.size(); ++i)
        if(mapIcons.elementAt(i).iconType==IconType.REPORT)
            mapIcons.removeElementAt(i);
    }// end     void iconUuidListClearReports()

    void iconUuidListClearOrders()
            {for (int i=0; i<mapIcons.size(); ++i)
        if(mapIcons.elementAt(i).iconType==IconType.ORDER)
            mapIcons.removeElementAt(i);
    }//end iconUuidListClearOrders()
    void iconUuidListClearInit() 
    {for (int i=0; i<mapIcons.size(); ++i)
        if(mapIcons.elementAt(i).iconType==IconType.INITIALIZE)
            mapIcons.removeElementAt(i);
    }// end iconUuidListClearInit()
    void listRemoveIconUuid(String unitID)    
    {for (int i=0; i<mapIcons.size(); ++i)
        if(mapIcons.elementAt(i).iconUuid.equals(unitID))
            mapIcons.removeElementAt(i);
    }// end listRemoveIconUuid()
    
    /**
     * holds an instance of report XML and its (lat, lon)
     * @author mpullen
     */
    private class XmlCoords {

       String reportXml;
       float reportLat, reportLon;
       public XmlCoords(String reportText, float lat, float lon){
           reportXml = reportText;
           reportLat = lat;
           reportLon = lon;
       }

    }// end class XmlCoords

    /**
     * holds all reports on the current map in order added
     */
    LinkedList<XmlCoords> mapReports = new LinkedList<XmlCoords>();
    void reportAddText(String reportText, float lat, float lon){
        mapReports.add(new XmlCoords(reportText, lat, lon));
    }
    
    /**
     * returns XML for report on map geometrically closest to args
     * rather than take square root we compare square of distance
     */
    String reportGetText(float testLat, float testLon){
        String returnXml = "";
        float closestLat = 1000000f, closestLon = 1000000f;// larger than any GDC
        float distanceSquared = Float.POSITIVE_INFINITY; 
        for (XmlCoords xmlCoords : mapReports) {
            float deltaLat = xmlCoords.reportLat - testLat;
            float deltaLon = xmlCoords.reportLon - testLon;
            float testDistance = deltaLat*deltaLat + deltaLon*deltaLon;
            if(distanceSquared > testDistance) {
                distanceSquared = testDistance;
                closestLat = testLat;
                closestLon = testLon;
                returnXml = xmlCoords.reportXml;
            }
        }
        return returnXml;
    }
    
    /**
     * clears list of reports
     */
    void reportsClearAllText() {mapReports = new LinkedList<XmlCoords>();}

    /**
     * print argument only if in debug mode
     */
    static void printDebug(String toPrint)
    {
      System.out.println(toPrint);
    }
    static void printDebug(int toPrint)
    {
      System.out.println(toPrint);
    }
    
    /**
     *  print argument to System.err
     */
    public static void printError(String toPrint){
        System.err.println("ERROR:"+toPrint);
    }
    
    /**
     * test whether the xmlUrl string has been loaded
     * if not, emit popup message
     * returns true if not pushable
     */
    boolean checkOrderNotPushable(){
        if(xmlUrl == null) {
            showInfoPopup(
                "cannot push - no order is loaded", 
                "Order Push Message");
            return true;
        }
        return false;
        
    }// end checkOrderNotPusbable()
    
    /**
     * test whether the xmlUrl string has been loaded
     * if not, emit popup message
     * returns true if not pushable
     */
    boolean checkReportNotPushable(){
        if(currentXmlString == null){
            showInfoPopup( 
                "cannot push - no report has been loaded", 
                "Report Push Message");
            return true;
        }
        return false;
    }// end checkReportNotPusbable()
    
    /**
     *  checks a C2SIM tag for match; then tries again with namespace prefix
     *  returns true if they are equal in either form
     */
    boolean c2simTagCompare(String checkTag, String soughtTag) {
      String trimmedTag = checkTag.trim();
      if(trimmedTag.equals(soughtTag))return true;
      return trimmedTag.equals(c2simns + soughtTag);
    }
  
    /**
     *  checks a tag for match; then tries again with namespace prefix
     */
    boolean cbmlTagCompare(String checkTag, String soughtTag) {
      String trimmedTag = checkTag.trim();
      if(trimmedTag.equals(soughtTag))return true;
      return trimmedTag.equals(cbmlns + soughtTag);
    }

    /**
     *  checks an IBML tag for match; then tries again with namespace prefix
     */
    boolean ibmlTagCompare(String checkTag, String soughtTag) {
      String trimmedTag = checkTag.trim();
      if(trimmedTag.equals(soughtTag))return true;
      return trimmedTag.equals(ibmlns + soughtTag);
    }
    
    /**
     * updates Title of frame
     */
    public void updateTitle(){
        String serverAddress = "";
        if(getConnected()) serverAddress = " connected server " + serverName;
        setTitle("GMU C4I & Cyber Center C2SIM GUI version " + 
            version + " C2SIM Editor" + serverAddress);
    }
  
    /**
     * Initialize the window frame GUI Frame (Widgets, Layouts, and ActionListeners)
     */
    public void init() {
        try {			
            // Add the title of the C2SIMGUI Frame, also specify sizes and display it
            updateTitle();
            setSize(WINDOW_WIDTH, WINDOW_HEIGHT);	
            setLayout(new BorderLayout());	
            initMenuBar();			// initialize gui components
            JLabel editorLabel = new JLabel("C2SISMGUI Editor................");
            centerPanel = new JPanel(new BorderLayout());
            centerPanel.add(editorLabel,BorderLayout.NORTH);

            JPanel _mapPanel = new JPanel();
            _mapPanel.setSize(500, 768);
            _mapPanel.setBackground(Color.GRAY);

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setDividerSize(10);
            splitPane.setLastDividerLocation(512);
            splitPane.setBorder(null);
            splitPane.setTopComponent(centerPanel);
            add(splitPane, BorderLayout.CENTER);
            splitPane.setLastDividerLocation(WINDOW_WIDTH/5);

            // topButtonPanel	
            topButtonPanel = new JPanel();
            topButtonPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

            // create a Panel for the connection status and headings
            Border border = BorderFactory.createEtchedBorder();
            editorButtonPanel = new JPanel(new GridBagLayout());
            editorButtonPanel.setMinimumSize(new Dimension(320,35));
            editorButtonPanel.setBorder(border);
            GridBagConstraints gbc = new GridBagConstraints();
            
            // insert coordinates fields at top
            getCoordsLabel = new JLabel("CLICKED COORDS:");
            getLatitude = new JTextField("LAT:  0.000");
            getLongitude = new JTextField("LON:  0.000");
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; 
            gbc.gridy = 0;
            gbc.weightx = .5;
            gbc.gridwidth = 3; 
            editorButtonPanel.add(getCoordsLabel);
            gbc.gridx = 1;
            editorButtonPanel.add(getLatitude);
            gbc.gridx = 2;
            editorButtonPanel.add(getLongitude);
            
            // Headers for three status rows
            
            // Subscriber Heading
            JLabel subscriberHeading = new JLabel("SUBSCRIBED", SwingConstants.CENTER);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; 
            gbc.gridy = 1;
            gbc.weightx = .5;
            gbc.gridwidth = 1;
            editorButtonPanel.add(subscriberHeading, gbc);
            
            // Initialize Heading
            JLabel initializeHeading = new JLabel("INITIALIZED", SwingConstants.CENTER);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 1; 
            gbc.gridy = 1;
            gbc.weightx = .5;
            editorButtonPanel.add(initializeHeading, gbc);
            
            // Server State Heading
            JLabel serverStateHeading = new JLabel("SYSTEM STATE", SwingConstants.CENTER);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 2; 
            gbc.gridy = 1;
            gbc.weightx = .5;
            editorButtonPanel.add(serverStateHeading, gbc);
            
            // JLabel for status of STOMP subscription   
            subscriberStatusLabel = new JLabel("NO", SwingConstants.CENTER);
            subscriberStatusLabel.setForeground(Color.RED);
            subscriberStatusLabel.setToolTipText("Status of STOMP server subscription");
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; 
            gbc.gridy = 2;
            gbc.weightx = .5;
            editorButtonPanel.add(subscriberStatusLabel,gbc);
            
            // JLabel for status of Initialization 
            initStatusLabel = new JLabel("0", SwingConstants.CENTER);
            initStatusLabel.setToolTipText(
                "Number of units for which MilitaryOrganization has been received since server reset ");
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 1; 
            gbc.gridy = 2;
            gbc.weightx = .5;
            editorButtonPanel.add(initStatusLabel, gbc);
            
            // JLabel for status of the server
            serverStatusLabel = new JLabel("UNKNOWN", SwingConstants.CENTER);
            serverStatusLabel.setForeground(Color.GRAY);
            serverStatusLabel.setToolTipText("status of C2SIM server");
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 2; 
            gbc.gridy = 2;
            gbc.weightx = .5;
            editorButtonPanel.add(serverStatusLabel, gbc);

            // Label for Document Type 
            documentTypeLabel = new JLabel("Document Type", SwingConstants.CENTER);
            documentTypeLabel.setToolTipText("Document Type");
            documentTypeLabel.setForeground(Color.BLUE);
            documentTypeLabel.setFont(new Font("LucidaGrande",Font.BOLD,15));
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; 
            gbc.gridy = 3;
            gbc.gridwidth = 3;
            gbc.weightx = .5;
            editorButtonPanel.add(documentTypeLabel, gbc);
            
            // add the getCoords button to the toolbar
            getCoordsButton = new JButton("GET COORDS FROM MAP");
            getCoordsButton.setToolTipText(
                "Loads to GUI coordinates associated with point clicked on the map");
            getCoordsButton.addActionListener(this);
            
            // add the coords to XML button to the toolbar
            coordsToXmlButton.setToolTipText(
                "Inserts values in Lat/Lon pairs found in XML");
            coordsToXmlButton.addActionListener(this);

            // add the loadReport button to the toolbar
            loadReportButton = new JButton("LOAD REPORT FROM MAP");
            loadReportButton.setToolTipText(
                "Loads to JaxFront form XML associated with a report on the map");
            loadReportButton.addActionListener(this);

            // add the subscriber button to the toolbar
            subscribeButton = new JButton("SUBSCRIBE STOMP");
            subscribeButton.setToolTipText(
                "Connects to/disconnects from STOMP server");
            subscribeButton.addActionListener(this);

            // add the report listener button to the toolbar
            reportListenerButton = new JButton("LISTEN TO XML");
            tracksButton = new JButton("SHOW TRACKS");
            reportListenerButton.setToolTipText(
                "Displays reports received viaSTOMP on the map");
            reportListenerButton.addActionListener(this);
            tracksButton.setToolTipText(
                "Captures successive positions of each unit as a track");
            tracksButton.addActionListener(this);

            // add Record button to the toolbar
            recordButton = new JButton("RECORD STOMP");
            recordButton.setToolTipText(
                "Creates a file for playback, containing XML received via STOMP");
            recordButton.addActionListener(this);

            // add Play button to the toolbar
            playButton = new JButton("PLAY RECORDING");
            playButton.setToolTipText(
                "Displays log messages from recording made here or on server");
            playButton.addActionListener(this);
            
            // add Pause button to toolbar, hidden
            pauseButton = new JButton("PAUSE PLAY");
            pauseButton.setVisible(false);
            pauseButton.setToolTipText(
                "Pauses playback of recording made here or on server");
            pauseButton.addActionListener(this);
            

            // add the editor buttons to the center panel
            centerPanel.add(editorButtonPanel,BorderLayout.NORTH);
            
            // replace the fonts and insets in all topButtonPanel buttons
            Font currentFont = getCoordsButton.getFont();
            Font newFont = 
                new Font(
                    currentFont.getName(),
                    currentFont.getStyle(), 
                    currentFont.getSize()-3);
            Insets buttonInsets = new Insets(2,0,2,0);
            getCoordsButton.setFont(newFont);
            getCoordsButton.setMargin(buttonInsets);
            coordsToXmlButton.setFont(newFont);
            coordsToXmlButton.setMargin(buttonInsets);
            loadReportButton.setFont(newFont);
            loadReportButton.setMargin(buttonInsets);
            reportListenerButton.setFont(newFont);
            reportListenerButton.setMargin(buttonInsets);
            tracksButton.setFont(newFont);
            tracksButton.setMargin(buttonInsets);
            subscribeButton.setFont(newFont);
            subscribeButton.setMargin(buttonInsets);
            recordButton.setFont(newFont);
            recordButton.setMargin(buttonInsets);
            playButton.setFont(newFont);
            playButton.setMargin(buttonInsets);
            pauseButton.setFont(newFont);
            pauseButton.setMargin(buttonInsets);
            
            // add the top Button panel to the main frame
            topButtonPanel.add(getCoordsButton);
            topButtonPanel.add(coordsToXmlButton);
            loadReportButton.setEnabled(false);
            topButtonPanel.add(loadReportButton);
            reportListenerButton.setEnabled(false);
            topButtonPanel.add(reportListenerButton);
            topButtonPanel.add(tracksButton);
            topButtonPanel.add(subscribeButton);
            topButtonPanel.add(recordButton);
            
            // at least for now, button conflict precludes playback in Linux
            if(!platformIsLinux){
                topButtonPanel.add(playButton);
                topButtonPanel.add(pauseButton);
            }
            recordButton.setEnabled(false);
            add(topButtonPanel,BorderLayout.NORTH);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
            bottomPanel.setSize(10,4*WINDOW_WIDTH/5);
            JLabel jaxfrontLabel = new JLabel(
                " Forms generated by JAXFront free community license, Xcentric Technology & Consulting");
            Font jaxfrontLabelFont = jaxfrontLabel.getFont();
            Font newJaxfrontLabelFont = new Font(
                jaxfrontLabelFont.getName(),
                jaxfrontLabelFont.getStyle(),
                jaxfrontLabelFont.getSize()-3);
            jaxfrontLabel.setFont(newJaxfrontLabelFont);
            jaxfrontLabel.setForeground(Color.GRAY);
            bottomPanel.add(jaxfrontLabel);
            
            // insert buttons used to remove categories of icons on map
            JLabel spacer1 = new JLabel("      ");
            bottomPanel.add(spacer1);
            initRemoveButton.setToolTipText("removes initialization icons from map");
            initRemoveButton.setForeground(Color.RED);
            bottomPanel.add(initRemoveButton);
            initRemoveButton.setVisible(false);
            initRemoveButton.addActionListener(this);
            JLabel spacer2 = new JLabel("      ");
            bottomPanel.add(spacer2);
            orderRemoveButton.setToolTipText("removes order icons from map");
            orderRemoveButton.setForeground(Color.RED);
            bottomPanel.add(orderRemoveButton);
            orderRemoveButton.setVisible(false);
            orderRemoveButton.addActionListener(this);
            JLabel spacer3 = new JLabel("      ");
            bottomPanel.add(spacer3);
            reportRemoveButton.setToolTipText("removes report icons from map");
            reportRemoveButton.setForeground(Color.RED);
            bottomPanel.add(reportRemoveButton);
            reportRemoveButton.setVisible(false);
            reportRemoveButton.addActionListener(this);
            add(bottomPanel,BorderLayout.SOUTH);
            
            
            // display the map
            initMap();                      // Add the Map Panel			
            addWindowListener(this);
            setVisible(true);               // Set GUI Frame visible
            loadConfig();                   // Call loadConfig to activate SBMLServer values
            if(mapScale.length() > 0)
                mapBean.setScale((float) Double.parseDouble(mapScale));//set scale from user config
        } catch (LicenseErrorException licEx) {
            licEx.showLicenseDialog(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        // setup Listen and Tracks buttons based on config
        if(startWithListeningOn.equals("1")){
            reportListenerButton.setText("STOP LISTENING");
            listenToXml = true;
            tracksButton.setText("STOP SHOWING TRACKS");
            showTracks = true;
        }
        else {
            reportListenerButton.setText("LISTEN TO XML");
            listenToXml = false;
            tracksButton.setText("SHOW TRACKS");
            showTracks = false;
        }
    
    } // end init()
  
    /**
     * reads an XML file from the filesystem 
     * @param xmlUrl
     * @return file contents
     */
    public String readAnXmlFile(URL xmlUrl){
        return readAnXmlFile(xmlUrl.toString().substring(6));
    }
    public String readAnXmlFile(String xmlFilename) {
        // correct pathological path bug
        if(xmlFilename.charAt(0) != '.')
            if(xmlFilename.charAt(0) != '/')
                xmlFilename = "/" + xmlFilename;
        // read the file
        FileReader xmlFile;
        String xmlString = "";
        try{
          xmlFile=new FileReader(new File(xmlFilename));
          int charBuf; 
          while((charBuf=xmlFile.read())>0) {
            xmlString+=(char)charBuf;
          }
        }
        catch(FileNotFoundException fnfe){
          printError("file " + xmlFilename + " not found - returning empty string");
          return "";
        }
        catch(Exception e) {
          printError("Exception in reading XML file " + xmlFilename + ":"+e);
          e.printStackTrace();
          return "";
        }
        return xmlString;
  
    }// end readAnXmlFile()
  
    /**
     * starts the Server Subscriber thread
     * returns true if successful
     */
    public boolean startServerSubscribeThread(){
        
        // do nothing if STOMP is running
        if(getConnected())return true;
        if(debugMode)printDebug("Start Subscriber Thread");
        
        // start the subscriber thread
        try {
            subscriber = null;
            subscriber = new Subscriber();
        } catch (Exception e) {
            printError("Exception starting server subscribe thread:"+e);
            e.printStackTrace();
            return false;
        }
        threadSub = new Thread(subscriber);

        // Run the Subscriber in a separate thread
        if(debugMode)
            printDebug("Thread state "  + threadSub.getState().toString());
        threadSub.start();
        return true;
        
    }// end startServerSubscribeThread()
    
    /**
     * starts the Report Repeat thread
     * returns true if successful
     */
    public boolean startReportRepeatThread(){
        
        if(debugMode)printDebug("Start Subscriber Thread");
        
        if(repeatC2SIM != null){
            printError("only one Repeat Report is supported at a time");
            showInfoPopup("only one at a time allowed","Repeat Report");
            return false;
        }
        
        // start the repeat thread
        try {
            repeatC2SIM = new RepeatReportC2SIM();
        } catch (Exception e) {
            printError("Exception starting report repeat thread:"+e);
            e.printStackTrace();
            return false;
        }
        threadRepeat = new Thread(repeatC2SIM);

        // Run the Subscriber in a separate thread
        if(debugMode)
            printDebug("Thread state "  + threadRepeat.getState().toString());
        threadRepeat.start();
        return true;
        
    }// end startReportRepeatThread()
    
        
    /**
     * stops sending of repeated report
     */
    void stopReportRepeatThread(){
        sendingRepeat = false;
        bml.repeatC2SIM = null;
    }// end stopRepeatReportThread()
        
    /**
     * Load an XML string from file into JAXFront
     */
    String saveLabel=null, saveRoot = null;
    URL saveXsdUrl=null, saveXuiUrl=null; 
    void loadJaxFront(
        File xmlfc, 
        String jaxFrontLabel,
        String schemaLocation,
        String bmlRoot)
    {  
        // post the XML to jaxFront
        releaseXUICache();
        documentTypeLabel.setText(jaxFrontLabel);
        xsdUrl = URLHelper.getUserURL(schemaLocation);              // Schema File XSD
        if(xmlfc == null)xmlUrl= null;
        else xmlUrl = URLHelper.getUserURL(xmlfc.getAbsolutePath());// XML file		
        xuiUrl = URLHelper.getUserURL(bml.guiLocationXUI);          // Jaxfront XUI file
        root = bmlRoot;
        
        // save parameters to use with coordsToXmlButton
        saveLabel = jaxFrontLabel;
        saveXsdUrl = xsdUrl;
        saveXuiUrl = xuiUrl;
        saveRoot = root;
        
        // start the JaxFront panel
        initDom("default-context", xsdUrl, xmlUrl, xuiUrl, root);
     
    }// end loadJaxFront()
       
    /**
     * saves the current JAXFront document to XML file
     */
    void saveJaxFront(String folderName){
        
        // get user to select or name a file
        String folderLocation = bml.guiFolderLocation;
        if(folderName != null)
            folderLocation += "/" + folderName;
        JFileChooser xmlFc = new JFileChooser(folderName + "//");
        xmlFc.setDialogTitle("Enter filename for XML from JAXFront");
        xmlFc.showSaveDialog(null);
        String xmlFilePathname = xmlFc.getSelectedFile().getAbsolutePath();
        if(!xmlFilePathname.endsWith(".xml"))xmlFilePathname += ".xml";
        try {
            String filedata = bml.currentDom.serialize().toString();
            BufferedWriter out = new BufferedWriter(new FileWriter(xmlFilePathname));
            out.write(filedata);
            out.close();
        } catch (ValidationException ve) {
            ve.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        // reset cache hash to show file has been saved
        xuiCacheHash = XUICache.getInstance().hashCode();
        loadedXml = bml.getDomXmlLessJaxFront();
        
    }// end saveJaxFront()
    
    /**
     * imports value of coordinates after mouse click in RouteLayer
     */
    void setReportCoordinates(float lat, float lon){
        reportLatitude = lat;
        reportLongitude = lon;
        loadingReport = false;
        mouseDelegator.setActiveMouseModeWithID(holdMouseModeID);
    }
    
    /**
     * makes a text String with 3 significant digitsfFILENAME: from float
     */
    String floatTo3SD(float input){
        return String.format("%.3f", input);
    }
    
        
    /**
     * imports value of coordinates after mouse click in RouteLayer
     */
    void setCoordinates(float lat, float lon){
        if(debugMode)printDebug("mouseclick setCoordinates:"+lat+"/"+lon);
        clickLatitude = lat;
        clickLongitude = lon;
        getLatitude.setText("LAT:" + floatTo3SD(lat));
        getLongitude.setText("LON:" + floatTo3SD(lon));
        getCoordsButton.setText("GET COORDS FROM MAP");
        gettingCoords = false;
        movingCoords = false;
        mouseDelegator.setActiveMouseModeWithID(holdMouseModeID);
    }
        
    /**
     * Action Performed / Action Listener method to respond to button selection
     */
    public void actionPerformed(ActionEvent buttonAction) {

        // Server subscribe  button
        if(buttonAction.getSource() == subscribeButton){
            if(debugMode)printDebug("Button subscribe is selected");
            if(!getConnected()){
                startServerSubscribeThread();
                recordButton.setEnabled(true);
            }
            else {

                // turn off label in top panel and associated buttons
                subscriberStatusLabel.setText("NO");
                subscriberStatusLabel.setForeground(Color.RED);
                subscribeButton.setText("SUBSCRIBE STOMP");
                reportListenerButton.setEnabled(false);
                if(recorder != null)recorder.close();
                recordButton.setEnabled(false);

                // shut off subscriber thread
                if(subscriber == null) {
                    if(debugMode)printDebug("Can't stop server - Thread is null");
                    return;
                }
                if(debugMode)printDebug("Stop Subscriber Thread");
                if(debugMode)printDebug("Thread state "  + threadSub.getState().toString());
                subscriber.stopSub();
                subscribeButton.setEnabled(true);
                updateTitle();
            }
        }
        
        // Listen for reports
        if(buttonAction.getSource() == reportListenerButton){
            if(debugMode)printDebug("Button reportListener is selected");
            listenToXml = !listenToXml;
            if(listenToXml){
                reportListenerButton.setText("STOP LISTENING");
                autoDisplayOrders = "";   
            }
            else {
                reportListenerButton.setText("LISTEN TO XML");
                autoDisplayOrders = saveAutoDisplayOrders;   
            }
        }
        
        // Show tracks
        if(buttonAction.getSource() == tracksButton){
            holdMouseModeID = mouseDelegator.getActiveMouseModeID();
            if(debugMode)printDebug("Button tracksButton is selected; MouseMode:" +holdMouseModeID);
            showTracks = !showTracks;
            if(showTracks)
                tracksButton.setText("STOP SHOWING TRACKS");
            else
                tracksButton.setText("SHOW TRACKS");
        }
        
        // clear initialization from map
        if(buttonAction.getSource() == initRemoveButton){
            initRemoveButton.setVisible(false);
            removeInitIcons();
        }
        
        // clear orders from map
        else if(buttonAction.getSource() == orderRemoveButton){
            orderRemoveButton.setVisible(false);
            orderIconsOnScreen = false;
            removeLayersWithIconType(IconType.ORDER);
            iconUuidListClearOrders();
            lastUnitID = "";
            lastLatitude = "";
            lastLongitude = "";
        }
        
        // clear reports from map
        else if(buttonAction.getSource() == reportRemoveButton){
            reportRemoveButton.setVisible(false);
            reportIconsOnScreen = false;
            removeLayersWithIconType(IconType.REPORT);
            iconUuidListClearReports();
            lastUnitID = "";
            lastLatitude = "";
            lastLongitude = "";
        }
        
        // Load coordinates identified by mouseclick on map
        else if(buttonAction.getSource() == getCoordsButton){
            holdMouseModeID = mouseDelegator.getActiveMouseModeID();
            if(debugMode)printDebug("Button getCoords is selected; MouseMode:" + holdMouseModeID);
            
            // if we come through here again cancel getting coords
            if(gettingCoords){
                interruptGettingCoords = true;
                gettingCoords = false;
                getCoordsButton.setText("GET COORDS FROM MAP");
            } else {
           
                // make sure we have a RouteLayer to take mouseClick
                if(routeLayerIndex == 0){
                    routeLayers[routeLayerIndex++] = new RouteLayer(" ", IconType.NULL);
                    mapHandler.add(routeLayers[0]);
                }
                
                // locate the report on the map
                gettingCoords = true;
                getCoordsButton.setText("CLICK ON THE COORDS");
                getCoordsButton.setSelected(false);
                mouseDelegator.setActiveMouseModeWithID("Gestures");
                (new Thread(new getCoords())).start();
            }
        }
        
        // copy the coords into current XML
        else if(buttonAction.getSource() == coordsToXmlButton){
            interruptEnteringCoords = false;
            if(enteringCoords){
                interruptEnteringCoords = true;
                enteringCoords = false;
                coordsToXmlButton.setText(coordsText);
            } else {

                // make sure we have a RouteLayer to take mouseClick
                if(routeLayerIndex == 0){
                    routeLayers[routeLayerIndex++] = 
                        new RouteLayer(" ", IconType.NULL);
                    mapHandler.add(routeLayers[0]);
                }
                
                // start a thread to capture and insert the coords
                enteringCoords = true;
                coordsToXmlButton.setText("STOP ENTERING COORDS");
                coordsToXmlButton.setSelected(false);
                mouseDelegator.setActiveMouseModeWithID("Gestures");
                (new Thread(new coordsToXml(loadedXml))).start();
                return;
            }
        }
        
        // Load a report identified by mouselick on map
        else if(buttonAction.getSource() == loadReportButton){
            if(debugMode)printDebug("Button loadReport is selected");

            // if we come through here again cancel loading
            if(loadingReport){
                interruptReportLoading = true;
                gettingCoords = false;
                loadReportButton.setText("CANCELING REQUEST");
            } else {
                
                // locate the report on the map
                loadingReport = true;
                loadReportButton.setText("CLICK ON A REPORT");
                loadReportButton.setSelected(false);
                mouseDelegator.setActiveMouseModeWithID("Gestures");
                (new Thread(new loadReport())).start();
            }
        }

        // Start/stop recorder
        else if(buttonAction.getSource() == recordButton){
            if(debugMode)printDebug("Button RecordSTOMP is selected");
            
            // this works only when connected
            if(!runRecorder  && !getConnected()) {
                showInfoPopup( 
                    "cannot record - not connected to server", 
                    "Recording Message");
                return;
            }
            
            runRecorder = !runRecorder;
            if(runRecorder){
                recordButton.setText("STOP RECORDING");
                recordFailed = false;
                recorder = new Recorder();
                if(recordFailed){
                    runRecorder = false;
                    captureFile = null;
                    recorder = null;
                    recordButton.setText("START RECORDING");
                }
            }
            else {
                if(recorder != null)recorder.close();
                recorder = null;
                recordButton.setText("START RECORDING");
            }
        }
              
        // Start/stop playback
        else if(buttonAction.getSource() == playButton){
            if(debugMode)printDebug("Button Playback is selected runPlayer:"+runPlayer);
            runPlayer = !runPlayer;
            if(runPlayer){
                pauseButton.setVisible(true);
                
                // check whether playback will display any icons  
                if(autoDisplayReports.trim().equals("")){
                    showInfoPopup(
                        "Config autoDisplayReports does not designate a format to play",
                        "Playback cancelled");
                    return;
                }
                        
                // inform user whether play will send to server
                if(getConnected())
                    showInfoPopup( 
                        "playback will go to all clients connected to server " +
                            "using current server initialization", 
                        "Playback Message");
                else {
                    showInfoPopup( 
                        "not connected to server - playback will display only locally " +
                            "using recorded initialization", 
                        "Playback Message");
                }
                
                // setup playback and start it
                listeningWhenPlayStarted = listenToXml;
                playButton.setText("STOP PLAY");
                playFailed = false;
                player = new Player();
                if(playFailed){
                    runPlayer = false;
                    pausePlayer = false;
                    if(debugMode)printDebug("playFailed:"+playFailed);
                    playButton.setText("START PLAY");
                    pauseButton.setVisible(false);
                    reportListenerButton.setEnabled(false);
                }
                // must enable listenToXml to see playback
                if(!listenToXml)listenToXml = true;
                reportListenerButton.setEnabled(false);
            }
            else {
                playButton.setText("START PLAY");
                pauseButton.setVisible(false);
                stopPlayback();
                pausePlayer = false;
            }
        }
        else if(buttonAction.getSource() == pauseButton){
            if(debugMode)printDebug("Button Pause is selected");
            pausePlayer = !pausePlayer;
            if(pausePlayer)
                pauseButton.setText("CONTINUE PLAY");
            else
                pauseButton.setText("PAUSE PLAY");
        }
        
    }// end actionPerformed()
    
    // Overall File Menu
    JMenu newMenu   = new JMenu("New         ");
    JMenu openMenu  = new JMenu("Open");
    JMenu pushMenu  = new JMenu("Push");
    JMenu openPushMenu = new JMenu("Open+Push");
    JMenu controlMenu = new JMenu("Server Control");
    JMenu magicMoveMenu = new JMenu("Magic Move");
    JMenu timeMultMenu = new JMenu("Sim & Play Time Multiple");
    JMenu serverRecordMenu = new JMenu("Server Recording");
    JMenu serverPlaybackMenu = new JMenu("Server Playback");
    JMenu saveJaxMenu  = new JMenu("Save JAXFront");
    JMenu pushJaxMenu = new JMenu("Push JAXFront");
    
    /**
     * set STOMP connected state, then
     * enable/disable File menu items that push
     */
    void setConnected(boolean setPush) {
        stompIsConnected = setPush;
        pushMenu.setEnabled(setPush);
        openPushMenu.setEnabled(setPush);
        controlMenu.setEnabled(setPush);
    }
    
    /**
     * returns connected state
     */
    boolean getConnected() {
        return stompIsConnected;
    }
    
    /**
     * checks a String for lack of ERROR returned on server command success 
     */
    boolean serverSuccess(String serverResponse){
        return !serverResponse.contains("ERROR");
    }
    
    /**
     * actions of shortcut button to stop server
     */
    private void commandStopServer(){
        
        // confirm user means it
        int answer = JOptionPane.showConfirmDialog(
                    null,  
                    "REALLY STOP SERVER?", 
                    "Stop server warning",
                    JOptionPane.OK_CANCEL_OPTION);
                if (answer != JOptionPane.OK_OPTION) {
                    return;
                }
        
        // run stop cycle
        serverStopButton.setVisible(false);
        if(debugMode)printDebug("Push C2SIM server control STOP then RESET .......");
        InitC2SIM initC2SIM = new InitC2SIM();
        if(serverSuccess(initC2SIM.pushStopC2SIM()))
            if(serverSuccess(initC2SIM.pushResetC2SIM()))   
                serverShareButton.setVisible(true);
    }
    
    /**
     * actions of shortcut button to initialize and share server
     */
    private void commandShareServer(){
        InitC2SIM initC2SIM = new InitC2SIM();
        
        // might get here uninitialized...
        if(serverStatusLabel.getText().equals("UNINITIALIZED"))
            if(!serverSuccess(initC2SIM.pushInitializeC2SIM()));
        
        // if not, try pushing a C2SIMInitialization 
        serverShareButton.setVisible(false);
        if(debugMode)printDebug("Open+Push C2SIM Initialize ......................");
        configHasBeenLoaded = false;     
        if(initC2SIM.openInitFSC2SIM("Initialize")){
            
            // init worked, also push SHARE and START
            if(serverSuccess(initC2SIM.pushInitC2SIM())){
                if(debugMode)printDebug("Push C2SIM server control SHARE then START...........");
                    if(serverSuccess(initC2SIM.pushShareC2SIM()))
                        initC2SIM.pushStartC2SIM();
            }
        }

        // didn't load file - show button to try again
        else serverShareButton.setVisible(
            stompIsConnected && !serverPassword.equals(""));
    }
	
    /**
     * Create the Menu Bar of the C2SIMGUI Frame/Window 
 (Widgets, Layout, and ActionListeners)
     */
    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu mainMenu = new JMenu("File");// File Menu : New, Open, Exit...

        // MSDL Menu
        JMenuItem newMSDL   = new JMenuItem("New  MSDL");
        JMenuItem openMSDL  = new JMenuItem("Open MSDL");
        JMenuItem pushMSDL  = new JMenuItem("Push MSDL");
        JMenuItem openPushMSDL = new JMenuItem("Open+Push MSDL");

        // C2SIM Order Menu
        JMenuItem newOrderC2SIM  = new JMenuItem("New C2SIM Order");
        JMenuItem openOrderC2SIM = new JMenuItem("Open C2SIM Order");
        JMenuItem pushOrderC2SIM = new JMenuItem("Push C2SIM Order");
        JMenuItem openPushOrderC2SIM = new JMenuItem("Open+Push C2SIM Order");
        JMenuItem saveJaxFrontOrderC2SIM = new JMenuItem("Save C2SIM Order from JAXFront");

        // C2SIM Reports
        JMenuItem newReportC2SIM  = new JMenuItem("New C2SIM Report");
        JMenuItem openReportC2SIM  = new JMenuItem("Open C2SIM Report");
        JMenuItem pushReportC2SIM  = new JMenuItem("Push C2SIM Report");
        JMenuItem openPushReportC2SIM  = new JMenuItem("Open+Push C2SIM Report");
        JMenuItem saveJaxFrontReportC2SIM = new JMenuItem("Save C2SIM Report from JAXFront");
        
        // C2SIM Initialize Menu
        JMenuItem newInitC2SIM  = new JMenuItem("New C2SIM Initialize");
        JMenuItem openInitC2SIM = new JMenuItem("Open C2SIM Initialize");
        JMenuItem pushInitC2SIM = new JMenuItem("Push C2SIM Initialize");
        JMenuItem openPushInitC2SIM = new JMenuItem("Open+Push C2SIM Initialize");
        JMenuItem saveJaxFrontInitC2SIM = new JMenuItem("Save JAXFront C2SIM Initialize");
        
        // C2SIM Cyber enu
        JMenuItem newCyberC2SIM  = new JMenuItem("New C2SIM Cyber Control");
        JMenuItem openCyberC2SIM = new JMenuItem("Open C2SIM Cyber Control");
        JMenuItem pushCyberC2SIM = new JMenuItem("Push C2SIM Cyber Control");
        JMenuItem openPushCyberC2SIM = new JMenuItem("Open+Push C2SIM Cyber Control");
        JMenuItem saveJaxFrontCyberC2SIM = new JMenuItem("Save JAXFront C2SIM Cyber Control");

        // IBML09 Order Menu
        JMenuItem newOrder09  = new JMenuItem("New IBML09 Order");
        JMenuItem openOrder09 = new JMenuItem("Open IBML09 Order");
        JMenuItem pushOrder09 = new JMenuItem("Push IBML09 Order");
        JMenuItem openPushOrder09 = new JMenuItem("Open+Push IBML09 Order");
        
        // CBML Light Order Menu
        JMenuItem newOrder  = new JMenuItem("New CBML Light Order");
        JMenuItem openOrder = new JMenuItem("Open CBML Light Order");
        JMenuItem pushOrder = new JMenuItem("Push CBML Light Order");
        JMenuItem openPushOrder = new JMenuItem("Open+Push CBML Light Order");

        // Reports Menu
        JMenu newReport  = new JMenu("New CBML Light Report");
        JMenuItem generalReport  = new JMenuItem("General Status Report");
        //JMenuItem positionReport  = new JMenuItem("Position Report");
        //JMenuItem bridgeReport  = new JMenuItem("Bridge Report");
        //JMenuItem mineReport  = new JMenuItem("Mine Report");
        //JMenuItem natoReport  = new JMenuItem("Nato Report");
        //JMenuItem spotReport  = new JMenuItem("Spot Report");
        //JMenuItem trackReport  = new JMenuItem("Track Report");

        // IBML09 Reports
        JMenuItem newIBML09Report  = new JMenuItem("New  IBML09 Report");
        JMenuItem openIBML09Report  = new JMenuItem("Open IBML09 Report");
        JMenuItem generalReport09  = new JMenuItem("IBML09 General Status Report");
        JMenuItem taskReport09  = new JMenuItem("IBML09 Task Status Report");
        JMenuItem pushIBML09Report  = new JMenuItem("Push IBML09 Report");
        JMenuItem openPushIBML09Report  = new JMenuItem("Open+Push IBML09 Report");
        //JMenuItem pullbridgeReport  = new JMenuItem("Bridge Report");
        //JMenuItem pullmineReport  = new JMenuItem("Mine Report");
        //JMenuItem pullnatoReport  = new JMenuItem("Nato Report");
        //JMenuItem pullspotReport  = new JMenuItem("Spot Report");
        //JMenuItem pulltrackReport  = new JMenuItem("Track Report");
        
        // Push JAXFront menu
        final JMenuItem pushJaxFrontC2SIMOrder = new JMenuItem("Push C2SIM Order from JAXFront");
        final JMenuItem pushJaxFrontC2SIMReport = new JMenuItem("Push C2SIM Report from JAXFront");
        
        // Server Control
        JMenu serverControl = new JMenu("Push Server Controls");
        JMenuItem pushShareStartC2SIM = new JMenuItem("Push server control SHARE + START");
        JMenuItem pushShareC2SIM = new JMenuItem("Push server control SHARE");
        final JMenuItem pushInitializeC2SIM = new JMenuItem("Push server control INITIALIZE");
        final JMenuItem pushStartC2SIM = new JMenuItem("Push server control START");
        final JMenuItem pushPauseC2SIM = new JMenuItem("Push server control PAUSE");
        final JMenuItem pushResumeC2SIM = new JMenuItem("Push server control RESUME");
        final JMenuItem pushStopC2SIM = new JMenuItem("Push server control STOP");
        final JMenuItem pushResetC2SIM = new JMenuItem("Push server control RESET");
        final JMenuItem pushStopResetInitC2SIM = new JMenuItem("Push server control STOP + RESET + INITIALIZE");
        final JMenuItem pushCheckpointRestore = new JMenuItem("Push server checkpoint RESTORE");
        final JMenuItem pushCheckpointSave = new JMenuItem("Push server checkpoint SAVE");
        
        // Magic Move
        final JMenuItem getMagicMove = new JMenuItem("Get Magic Move input");
        
        // Simulation & Playback Realtime Multiple
        final JMenuItem getSimTimeMult = new JMenuItem("Get simulation realtime multiple");
        final JMenuItem setSimTimeMult = new JMenuItem("Set simulation realtime multiple");
        final JMenuItem getPlayTimeMult = new JMenuItem("Get server playback realtime multiple");
        final JMenuItem setPlayTimeMult = new JMenuItem("Set server playback realtime multiple");

        // Server Recording Control
        final JMenuItem pushServerRecStart = new JMenuItem("Push server recording start");
        final JMenuItem pushServerRecPause = new JMenuItem("Push server recording pause");
        final JMenuItem pushServerRecRestart = new JMenuItem("Push server recording restart");
        final JMenuItem pushServerRecStop = new JMenuItem("Push server recording stop");
        final JMenuItem pushServerRecGetStat = new JMenuItem("Push server recording get status");
        
        // Server playback control
        final JMenuItem pushServerPlayStart = new JMenuItem("Push server playback start");
        final JMenuItem pushServerPlayPause = new JMenuItem("Push server playback pause");
        final JMenuItem pushServerPlayRestart = new JMenuItem("Push server playback restart");
        final JMenuItem pushServerPlayStop = new JMenuItem("Push server playback stop");
        final JMenuItem pushServerPlayGetStat = new JMenuItem("Push server playback get status");
        
        JMenuItem openReport = new JMenuItem("Open CBML Light Report");
        //JMenuItem openReportWSById  = new JMenuItem("By Report ID");
        //JMenuItem openReportWSList  = new JMenuItem("From ID List");
        //JMenuItem openReportOldFS  = new JMenuItem("Demo General Status Report");
        //JMenuItem reportDemo = new JMenuItem("Report Demo");
        JMenuItem pushReport = new JMenuItem("Push CBML Light Report");
        JMenuItem openPushReport  = new JMenuItem("Open+Push CBML Light Report");

        JMenuItem newDocument  = new JMenuItem("New  XML Document");
        JMenuItem openDocument = new JMenuItem("Open XML Document");
        //JMenuItem saveDocument = new JMenuItem("Save");
        JMenuItem closeDocument = new JMenuItem("Close Document");
        JMenuItem runServerTest = new JMenuItem("Run Server Validation");
        JMenu repeatReportMenu = new JMenu("Send Repeated C2SIM Report");
        //JMenuItem printDocument = new JMenuItem("Print");
        JMenuItem exitOrder = new JMenuItem("Exit");
        
        // Repeated Report
        JMenuItem startRepeatReport = new JMenuItem("Start Repeated Report");
        JMenuItem stopRepeatReport = new JMenuItem("Stop Repeated Report");

        // Overall File  Menu
        mainMenu.add(newMenu);
        mainMenu.add(openMenu);
        mainMenu.add(pushMenu);
        mainMenu.add(openPushMenu);
        mainMenu.add(saveJaxMenu);
        mainMenu.add(pushJaxMenu);
        //mainMenu.addSeparator();

        // Order C2SIM Menu		
        newMenu.add(newOrderC2SIM);	
        openMenu.add(openOrderC2SIM);
        pushMenu.add(pushOrderC2SIM); 
        openPushMenu.add(openPushOrderC2SIM); 
        saveJaxMenu.add(saveJaxFrontOrderC2SIM);

        // Report C2SIM Menu
        newMenu.add(newReportC2SIM);
        openMenu.add(openReportC2SIM);
        pushMenu.add(pushReportC2SIM);
        openPushMenu.add(openPushReportC2SIM);
        saveJaxMenu.add(saveJaxFrontReportC2SIM);
        
        // Initialize C2SIM Menu
        newMenu.add(newInitC2SIM);	
        openMenu.add(openInitC2SIM);
        pushMenu.add(pushInitC2SIM);
        openPushMenu.add(openPushInitC2SIM);

        // MSDL Menu
        newMenu.add(newMSDL);
        openMenu.add(openMSDL);
        pushMenu.add(pushMSDL);
        openPushMenu.add(openPushMSDL);

        // IBML09 Orders and Reports Menu		
        newMenu.add(newOrder09);	
        openMenu.add(openOrder09);
        pushMenu.add(pushOrder09);
        openPushMenu.add(openPushOrder09);	

        newMenu.add(newIBML09Report);
        openMenu.add(openIBML09Report);
        //openIBML09Report.add(generalReport09);
        //openIBML09Report.add(taskReport09); Report09 needs reworked for this
        pushMenu.add(pushIBML09Report);
        openPushMenu.add(openPushIBML09Report);
        
        // CBML Light Order Menu
        newMenu.add(newOrder);	
        openMenu.add(openOrder);
        pushMenu.add(pushOrder);
        openPushMenu.add(openPushOrder);

        // General Report Menu
        newMenu.add(newReport);
        newReport.add(generalReport);
        //newReport.add(positionReport); all of these need work
        //newReport.add(bridgeReport);
        //newReport.add(mineReport);
        //newReport.add(natoReport);
        //newReport.add(spotReport);
        //newReport.add(trackReport);
        openMenu.add(openReport);
        pushMenu.add(pushReport);
        openPushMenu.add(openPushReport);
        
        // Cyber menu
        newMenu.add(newCyberC2SIM);
        openMenu.add(openCyberC2SIM);
        pushMenu.add(pushCyberC2SIM);
        openPushMenu.add(openPushCyberC2SIM);
        
        // Push JAXFront menu
        pushJaxMenu.add(pushJaxFrontC2SIMOrder);
        pushJaxMenu.add(pushJaxFrontC2SIMReport);
        
        // Magic Move Menu
        magicMoveMenu.add(getMagicMove);
        
        // Time menu
        timeMultMenu.add(getSimTimeMult);
        timeMultMenu.add(setSimTimeMult);
        timeMultMenu.add(getPlayTimeMult);
        timeMultMenu.add(setPlayTimeMult);
        
        // Server record menu
        serverRecordMenu.add(pushServerRecStart);
        serverRecordMenu.add(pushServerRecPause);
        serverRecordMenu.add(pushServerRecRestart);
        serverRecordMenu.add(pushServerRecStop);
        serverRecordMenu.add(pushServerRecGetStat);
        
        // Server playback menu
        serverPlaybackMenu.add(pushServerPlayStart);
        serverPlaybackMenu.add(pushServerPlayPause);
        serverPlaybackMenu.add(pushServerPlayRestart);
        serverPlaybackMenu.add(pushServerPlayStop);
        serverPlaybackMenu.add(pushServerPlayGetStat);
        
        // server controls
        mainMenu.addSeparator();
        controlMenu.add(pushShareStartC2SIM);
        controlMenu.add(pushShareC2SIM);
        controlMenu.add(pushInitializeC2SIM);
        controlMenu.add(pushStartC2SIM);
        controlMenu.add(pushPauseC2SIM);
        controlMenu.add(pushResumeC2SIM);
        controlMenu.add(pushStopC2SIM);
        controlMenu.add(pushResetC2SIM);
        controlMenu.add(pushStopResetInitC2SIM);
        controlMenu.add(pushCheckpointSave);
        controlMenu.add(pushCheckpointRestore);
        
        // Simulation time multiple
        
        // Repeated Report
        repeatReportMenu.add(startRepeatReport);
        repeatReportMenu.add(stopRepeatReport);

        // use separators to distinguish control functions
        mainMenu.addSeparator(); 
        mainMenu.addSeparator();
        mainMenu.add(controlMenu);
        mainMenu.addSeparator();
        mainMenu.add(magicMoveMenu);
        mainMenu.addSeparator();
        mainMenu.add(timeMultMenu);
        mainMenu.addSeparator();
        mainMenu.add(serverRecordMenu);
        mainMenu.addSeparator();
        mainMenu.add(serverPlaybackMenu);
        mainMenu.addSeparator();

        //newMenu.add(newDocument);   needs work
        //openMenu.add(openDocument); needs work
        //mainMenu.add(saveDocument);
        mainMenu.addSeparator();
        mainMenu.add(closeDocument);
        mainMenu.addSeparator();
        mainMenu.addSeparator();
        
        mainMenu.add(runServerTest);
        mainMenu.addSeparator();
        mainMenu.addSeparator();
        
        mainMenu.add(repeatReportMenu);

        //mainMenu.add(printDocument);
        mainMenu.addSeparator();
        mainMenu.addSeparator();
        mainMenu.add(exitOrder);
        menuBar.add(mainMenu);
        
        // disable the LOAD REPORT button whenever we 
        // start a new form, open one or push one;
        // button will re-emable when a new report is received
        // from the server
        newMenu.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
            }
        });
        openMenu.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
            }
        });
        pushMenu.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
            }
        });        
        
        // server controls
        pushShareStartC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control SHARE then START...........");
                InitC2SIM initC2SIM = new InitC2SIM();
                if(serverSuccess(initC2SIM.pushShareC2SIM()))
                    initC2SIM.pushStartC2SIM();
            }
        });       
        pushShareC2SIM.addActionListener( new ActionListener(){
                public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control SHARE.......................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushShareC2SIM();
            }
        });
        pushInitializeC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control INITIALIZE...................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushInitializeC2SIM();
            }
        });
        pushStartC2SIM.addActionListener( new ActionListener(){
             public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control START..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushStartC2SIM();
                if(recordPlayStatus.getText().equals("RECORDING"))
                    recordPlayStatus.setVisible(true);
                if(recordPlayStatus.getText().equals("PLAYING"))
                    recordPlayStatus.setVisible(false);                
            }
        });
        pushPauseC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control PAUSE..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushPauseC2SIM();
                if(recordPlayStatus.getText().equals("RECORDING"))
                    recordPlayStatus.setVisible(false);
            }
        });
        pushResumeC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control RESUME..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushResumeC2SIM();
                if(recordPlayStatus.getText().equals("RECORDING"))
                    recordPlayStatus.setVisible(true);
                if(recordPlayStatus.getText().equals("PLAYING"))
                    recordPlayStatus.setVisible(false);
            }
        });
        pushStopC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control STOP...................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushStopC2SIM();
                if(recordPlayStatus.getText().equals("RECORDING"))
                    recordPlayStatus.setVisible(false);
            }
        });
        pushResetC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control RESET .................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushResetC2SIM();
            } 
        });
        pushStopResetInitC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server control STOP then RESET .......");
                InitC2SIM initC2SIM = new InitC2SIM();
                if(serverSuccess(initC2SIM.pushStopC2SIM()))
                    if(serverSuccess(initC2SIM.pushResetC2SIM()))
                        initC2SIM.pushInitializeC2SIM();
            } 
        });
        pushCheckpointRestore.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server CheckpointRestore ..............");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushCheckpointRestore();
            } 
        });
        pushCheckpointSave.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM server CheckpointSave ..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushCheckpointSave();
            } 
        });
        
        // Sim time multiple & magic move controls
        getMagicMove.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Input Magic Move data..................");
                (new Thread(new magicMoveInput())).start();
            }
        });
        getSimTimeMult.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server input GETSIMMULT..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.getSimTimeMult();
            }
        });
        setSimTimeMult.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server input SETSIMMULT..................");
                String parm = inputTimeMultPopup("Enter  realtime multiple");             
                if(!parm.equals("0")){
                    InitC2SIM initC2SIM = new InitC2SIM();
                    initC2SIM.pushC2simServerInput("SETSIMMULT",parm,"","");
                }
            }
        });
        getPlayTimeMult.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server input GETSIMMULT.................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.getPlayTimeMult();
            }
        });
        setPlayTimeMult.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server input SETPLAYMULT..................");
                String parm = inputTimeMultPopup("Enter realtime multiple (0 for continuous play)");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushC2simServerInput("SETPLAYMULT",parm,"","");
            }
        });
        pushServerRecStart.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server recording START..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerRecStart();
                recordPlayStatus.setText("RECORDING");
                recordPlayStatus.setVisible(true);
            }
        });
        pushServerRecPause.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server recording PAUSE..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerRecPause();
                if(recordPlayStatus.getText().equals("RECORDING"))
                    recordPlayStatus.setVisible(false);
            }
        });
        pushServerRecRestart.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server recording RESTART..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerRecRestart();
            }
        });
        pushServerRecStop.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server recording STOP..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerRecStop();
                recordPlayStatus.setText("         ");
                recordPlayStatus.setVisible(false);
            }
        });   
        pushServerRecGetStat.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server recording GET STATUS..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerRecGetStatus();
            }
        }); 
        
        // Server playback controls
        pushServerPlayStart.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server playback START..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                String parm1 = inputTimeMultPopup(
                    "Enter playback file name from server (zero for current replay.log)");
                if(parm1 == null)return;
                String parm2 = inputTimeMultPopup(
                    "Enter playback start time in format 'HH:MM:SS' or 'YYYY-MM-DD HH:MM:SS' (0 to play all)");
                if(parm2 == null)return;
                if(debugMode)printDebug("PLAYBACK START PARMS|"+parm1+"|"+parm2+"|");
                
                // verify we have an input of acceptable length
                if(parm1 == null || parm2 == null)return;
                boolean lengthOK = false;
                if(parm2.equals("0"))lengthOK = true;
                if(parm2.length() == 8) {
                    parm2 = thisDate()+"T"+parm2+",000";
                    lengthOK = true;
                }
                else if(parm2.length() == 19) {
                    parm2 = parm2.substring(0,10)+"T"+parm2.substring(11,19)+",000";
                    lengthOK = true;
                }
                if(!lengthOK)showInfoPopup("must be 'HH:MM:SS' or 'YYYY-MM-DD HH:MM:SS'",
                        "DATE FORMAT LENGTH ERROR");
                
                // signal server to start playback
                else initC2SIM.pushC2simServerInput("STARTPLAY",parm1,parm2,"");
                recordPlayStatus.setText("PLAYING");
            }
        });
        pushServerPlayPause.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server playback playback PAUSE...........");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerPlayPause();
            }
        });
        pushServerPlayRestart.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server playback RESTART..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerPlayRestart();
            }
        });
        pushServerPlayStop.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server playback STOP..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerPlayStop();
                recordPlayStatus.setText("       ");
                recordPlayStatus.setVisible(false);
            }
        });
        pushServerPlayGetStat.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push server playback GET STATUS..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushServerPlayGetStatus();
            }
        });

        // MSDL Menu actions
        // Open MSDL file from File System
        openMSDL.addActionListener( new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    if(debugMode)printDebug("Open MSDL from File System...................");
                    configHasBeenLoaded = false;
                    MSDL msdl = new MSDL();
                    msdl.openMSDL_FS("MSDL");
                }
        });

        // New MSDL file from File System
        newMSDL.addActionListener( new ActionListener(){
                public void actionPerformed(ActionEvent e){
                        if(debugMode)printDebug("New MSDL ................................");
                        MSDL msdl = new MSDL();
                    msdl.newMSDL();
                }
        });
        // Push MSDL
        pushMSDL.addActionListener( new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    if(debugMode)printDebug("Push MSDL ...................................");
                    MSDL msdl = new MSDL();
                    msdl.pushMSDL();
                }
        });
        // Open+Push MSDL
        openPushMSDL.addActionListener( new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    if(debugMode)printDebug("Open+Push MSDL ..............................");
                    configHasBeenLoaded = false;
                    MSDL msdl = new MSDL();
                    msdl.openMSDL_FS("MSDL");
                    msdl.pushMSDL();
                }
        });

        // C2SIM Order
        newOrderC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New C2SIM Order .................................");
                OrderC2SIM orderC2SIM = new OrderC2SIM();
                orderC2SIM.newOrderC2SIM();
            }
        });
        openOrderC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open C2SIM Order from File System................");
                configHasBeenLoaded = false;
                OrderC2SIM orderC2SIM = new OrderC2SIM();
                orderC2SIM.openOrderFSC2SIM(
                    "Orders", 
                    autoDisplayOrders.equals("C2SIM") ||
                    autoDisplayOrders.equals("ALL"));
            }
        });
        pushOrderC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM Order ................................");
                OrderC2SIM orderC2SIM = new OrderC2SIM();
                orderC2SIM.pushOrderC2SIM();
            }
        });
        openPushOrderC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open+Push C2SIM Order ...........................");
                configHasBeenLoaded = false;
                if(!serverStatusLabel.getText().equals("RUNNING")){
                    showInfoPopup("Open Push C2SIM Order", "initialization required");
                    return;
                }
                OrderC2SIM orderC2SIM = new OrderC2SIM();
                if(orderC2SIM.openOrderFSC2SIM(
                    "Orders",
                    autoDisplayOrders.equals("C2SIM") ||
                    autoDisplayOrders.equals("ALL"))
                )orderC2SIM.pushOrderC2SIM();
            }
        });
        saveJaxFrontOrderC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Save C2SIM Order from JaxFront form..............");
                OrderC2SIM orderC2SIM = new OrderC2SIM();
                orderC2SIM.saveJaxFrontOrderC2SIM();
            }
        });
        
        pushJaxFrontC2SIMOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                pushingJaxFront = true;
                if(debugMode)printDebug("Push C2SIM Order from JaxFront form..............");
                if(currentDom == null) {
                    showInfoPopup( 
                        "JaxFront form is empty", 
                        "Exception pushing XML");
                    pushingJaxFront = false;
                    return;
                }
                try {
                    String filedata = currentDom.serialize().toString();
                    String pushResultString = 
                        ws.sendC2simREST(filedata,"ORDER",
                            c2simProtocolVersion);
                    showInfoPopup( 
                        pushResultString, 
                        "JAXFront Push Message");

                    // clear the data
                    xmlUrl = null;
                }
                catch(Exception ex) {
                    showInfoPopup( 
                        ex.getMessage(), 
                        "Exception pushing XML");
                }
                pushingJaxFront = false;
            }// end actionPerformed
        });
        pushJaxFrontC2SIMReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                pushingJaxFront = true;
                if(debugMode)printDebug("Push C2SIM Report from JaxFront form.............");
                if(currentDom == null) {
                    showInfoPopup( 
                        "JaxFront form is empty", 
                        "Exception pushing XML");
                    pushingJaxFront = false;
                    return;
                }
                try {
                    String filedata = currentDom.serialize().toString();
                    String pushResultString = 
                        ws.sendC2simREST(filedata,"REPORT",
                            c2simProtocolVersion);
                    showInfoPopup( 
                        pushResultString, 
                        "JAXFront Push Message");

                    // clear the data
                    xmlUrl = null;
                }
                catch(Exception ex) {
                    showInfoPopup( 
                        ex.getMessage(), 
                        "Exception pushing XML");
                    ex.printStackTrace();
                }
                pushingJaxFront = false;
            }// end actionPerformed
        });
        
        // C2SIM Report
        newReportC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New C2SIM Report ................................");
                ReportC2SIM reportC2SIM = new ReportC2SIM();
                reportC2SIM.newReportC2SIM();
            }
        });
        openReportC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open C2SIM Report from File System...............");
                configHasBeenLoaded = false;
                try {
                    ReportC2SIM reportC2SIM = new ReportC2SIM();
                    reportC2SIM.openReportFSC2SIM("Reports"); 
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        pushReportC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM Report ...............................");
                ReportC2SIM reportC2SIM = new ReportC2SIM();
                reportC2SIM.pushReportC2SIM();
            }
        });
        openPushReportC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open+Push C2SIM Report ..........................");
                configHasBeenLoaded = false;
                if(!serverStatusLabel.getText().equals("RUNNING")){
                    showInfoPopup("Open Push C2SIM Report", "initialization required");
                    return;
                }
                try {
                    ReportC2SIM reportC2SIM = new ReportC2SIM();
                    if(reportC2SIM.openReportFSC2SIM("Reports"))
                        reportC2SIM.pushReportC2SIM();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        saveJaxFrontReportC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Save JAXFront C2SIM Report ......................");
                ReportC2SIM reportC2SIM = new ReportC2SIM();
                reportC2SIM.saveJaxFrontReportC2SIM();
            }
        });
        
        // C2SIM Initialize
        newInitC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New C2SIM Initialize ............................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.newInitC2SIM();
            }
        });
        openInitC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open C2SIM Initialize from File System...........");
                configHasBeenLoaded = false;
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.openInitFSC2SIM("Initialize");
            }
        });
        pushInitC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM Initialize ...........................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.pushInitC2SIM();
            }
        });
        openPushInitC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open+Push C2SIM Initialize ......................");
                configHasBeenLoaded = false;
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.openInitFSC2SIM("Initialize");
                initC2SIM.pushInitC2SIM();
            }
        });
        saveJaxFrontInitC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Save JAXFront C2SIM Initialize ..................");
                InitC2SIM initC2SIM = new InitC2SIM();
                initC2SIM.saveJaxFrontInitC2SIM();
            }
        });

        // IBML Order
        newOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New CBML Light Order ............................");
                Order o = new Order();
                o.newOrder();
            }
        });
        
         // C2SIM Cyber
        newCyberC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New C2SIM Cyber .................................");
                CyberC2SIM cyberC2SIM = new CyberC2SIM();
                cyberC2SIM.newCyberC2SIM();
            }
        });
        openCyberC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open C2SIM Cyber from File System................");
                configHasBeenLoaded = false;
                CyberC2SIM cyberC2SIM = new CyberC2SIM();
                cyberC2SIM.openCyberFSC2SIM("Cyber");
            }
        });
        pushCyberC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push C2SIM Cyber ................................");
                CyberC2SIM cyberC2SIM = new CyberC2SIM();
                cyberC2SIM.pushCyberC2SIM();
            }
        });
        openPushCyberC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open+Push C2SIM Cyber ...........................");
                configHasBeenLoaded = false;
                CyberC2SIM cyberC2SIM = new CyberC2SIM();
                cyberC2SIM.openCyberFSC2SIM("Cyber");
                cyberC2SIM.pushCyberC2SIM();
            }
        });
        saveJaxFrontCyberC2SIM.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Save JAXFront C2SIM Cyber .......................");
                CyberC2SIM cyberC2SIM = new CyberC2SIM();
                cyberC2SIM.saveJaxFrontCyberC2SIM();
            }
        });

        // CBML
        openOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open CBML Light Order from File System...........");
                configHasBeenLoaded = false;
                Order o = new Order();
                o.openOrderFS(
                    "Orders",
                    autoDisplayOrders.equals("CBML") ||
                    autoDisplayOrders.equals("ALL"));
            }
        });
        pushOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
            if(debugMode)printDebug("Push CBML Light Order ...............................");
                Order o = new Order();
                o.pushOrder();
            }
        });
        openPushOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
            if(debugMode)printDebug("Open+Push CBML Light Order ..........................");
            configHasBeenLoaded = false;
                Order o = new Order();
                if(o.openOrderFS(
                    "Orders",
                    autoDisplayOrders.equals("CBML") ||
                    autoDisplayOrders.equals("ALL"))
                )o.pushOrder();
            }
        });

        // IBML Order09
        newOrder09.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New IBML09 Order ................................");
                Order09 order09 = new Order09();
                order09.newOrder09();
            }
        });
        openOrder09.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open IBML09 Order from File System...............");
                configHasBeenLoaded = false;
                Order09 order09 = new Order09();
                order09.openOrderFS09(
                    "Orders", 
                    autoDisplayOrders.equals("IBML09") ||
                    autoDisplayOrders.equals("ALL"));
            }
        });
        pushOrder09.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Push IBML 09 Order ..............................");
                Order09 order09 = new Order09();
                order09.pushOrder09();
            }
        });
        openPushOrder09.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open+Push IBML 09 Order .........................");
                configHasBeenLoaded = false;
                Order09 order09 = new Order09();
                if(order09.openOrderFS09(
                    "Orders",
                    autoDisplayOrders.equals("IBML09") ||
                    autoDisplayOrders.equals("ALL"))
                )order09.pushOrder09();
            }
        });

        // IBML Reports
        newReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report ......................................");
            }
        });	
        generalReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report GeneralReport ........................");
                Report r= new Report();
                r.newReport("GeneralStatusReport");
            }
        });
        /*
        positionReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report  Position Report .....................");
                Report r= new Report();
                r.newReport("PositionStatus");
            }
        });
        bridgeReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report  Bridge Report .......................");
                Report r= new Report();
                r.newReport("Bridge");
            }
        });
        mineReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report  Min Ob Report .......................");
                Report r= new Report();
                r.newReport("MinOb");
            }
        });
        spotReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report  Spot Report .........................");
                Report r= new Report();
                r.newReport("Spot");
            }
        });
        natoReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report  Nato Spot Report ....................");
                Report r= new Report();
                r.newReport("NatoSpot");
            }
        });
        trackReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("New Report  Track Report ........................");
                Report r= new Report();
                r.newReport("Track");
            }
        });*/
        openReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open Report from File System.....................");
                configHasBeenLoaded = false;
                try {
                    Report r= new Report();
                    r.openReportFS("Reports");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                }
            }
        });
        openIBML09Report.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open IBML09 Report from File System..............");
                configHasBeenLoaded = false;
                try {
                    Report09 r= new Report09();
                        r.openReportFS_General09("Reports");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
        });
        taskReport09.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open IBML09 Report from File System...............");
                configHasBeenLoaded = false;
                try {
                    Report09 r= new Report09();
                    r.openReportFS_Task09();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        pushIBML09Report.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
              if(debugMode)printDebug("Push IBML09 Report to Web Service..................");
              try {
                    Report09 r= new Report09();
                    r.pushReport09();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
        });
        openPushIBML09Report.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Open+Push IBML09 Report to Web Service..................");
                try {
                    configHasBeenLoaded = false;
                    Report09 r= new Report09();
                    if(r.openReportFS_General09("Reports"))
                        r.pushReport09();    
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        pushReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
              if(debugMode)printDebug("Push CBML Light Report to Web Service..................");
              try {
                    Report r= new Report();
                    r.pushReport();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
        });
        openPushReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
              if(debugMode)printDebug("Open+Push CBML Light Report to Web Service..................");
              configHasBeenLoaded = false;
              try {
                    Report r= new Report();
                    if(r.openReportFS("Reports"))
                        r.pushReport();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
        });
        closeDocument.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Close the Current editor Document ...............");
                closeDocument();
            }
        });
        runServerTest.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Run test on the server ...............");
                
                // if not subscribed, make a connection
                boolean startedTestConnected = stompIsConnected;
                if(!stompIsConnected)startServerSubscribeThread();
                if(getConnected()){
                    serverTest = new ValidateServer();
                    serverTest.runTest();
                    if(!startedTestConnected && stompIsConnected)subscriber.stopSub();
                }
                else {
                    showErrorPopup(
                        "can't run Server Validation - STOMP connection failed",
                        "STOMP connect failure");
                }
            }
        });
        exitOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Exit BML GUI ....................................");
                System.exit(0);
            }
        });		

        // Edit Menu : Serialize, Validate
        JMenu editMenu = new JMenu("Edit");
        JMenuItem serializeOrder  = new JMenuItem("Serialize Order");
        JMenuItem validateOrder = new JMenuItem("Validate Order");
        editMenu.add(serializeOrder);
        editMenu.addSeparator();
        editMenu.add(validateOrder);
        menuBar.add(editMenu);
        serializeOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Serialize Order .............................");
                serializeBmlDoc();
            }
        });
        validateOrder.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Validate Order .............................");
                validateBmlDoc();
            }
        });

        // Configuration Menu : server and domain name setup c2mlguiconfig.xml
        JMenu configMenu = new JMenu("Config");
        JMenuItem loadConfig  = new JMenuItem("Load");
        JMenuItem saveConfig = new JMenuItem("Save");
        JMenuItem closeConfig = new JMenuItem("Close");
        configMenu.add(loadConfig);
        configMenu.add(saveConfig);
        configMenu.add(closeConfig);

        menuBar.add(configMenu);
        loadConfig.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("load Config File .............................");
                openConfig();
            }
        });
        saveConfig.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Save Config File .............................");
                saveConfig();
            }
        });
        closeConfig.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Close Config File .............................");
                closeConfig();
            }
        });

        // Repeated Reports
        startRepeatReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Start Repeated Report.......................");
                startReportRepeatThread();
            }
        });
        stopRepeatReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Stop Repeated Report.......................");
                stopReportRepeatThread();
            }
        });

        // View Menu : Styles
        JMenu styleMenu = new JMenu("Editor Style");
        JMenuItem defaultStyle  = new JMenuItem("Default Style");
        JMenuItem viewStyle1 = new JMenuItem("Tab Style");
        JMenuItem viewStyle2 = new JMenuItem("Serial Style");
        styleMenu.add(defaultStyle);
        styleMenu.add(viewStyle1);
        styleMenu.add(viewStyle2);
        menuBar.add(styleMenu);
        defaultStyle.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Default Style .............................");
            }
        });
        viewStyle1.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("View Style 1 .............................");
            }
        });
        viewStyle2.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("View Style 2 .............................");
            }
        });		

        // Map Menu : 
        JMenu mapMenu = new JMenu("Map");
        JMenuItem mapOptions  = new JMenuItem("Options");
        mapMenu.add(mapOptions);

        // Bookmarks
        JMenu mapViews  = new JMenu("Views");
        JMenuItem mapView1  = new JMenuItem("World");
        JMenuItem mapView2  = new JMenuItem("North America");
        JMenuItem mapView3  = new JMenuItem("South America");
        JMenuItem mapView4  = new JMenuItem("Europe");
        JMenuItem mapView5  = new JMenuItem("Africa");
        JMenuItem mapView6  = new JMenuItem("Asia");
        JMenuItem mapView7  = new JMenuItem("Australia");
        JMenuItem mapView8  = new JMenuItem("Azerbaijan");
        JMenuItem mapView9  = new JMenuItem("Bogaland");

        // Bookmarks
        mapMenu.add(mapViews);
        mapViews.add(mapView1);
        mapViews.add(mapView2);
        mapViews.add(mapView3);
        mapViews.add(mapView4);
        mapViews.add(mapView5);
        mapViews.add(mapView6);
        mapViews.add(mapView7);
        mapViews.add(mapView8);
        mapViews.add(mapView9);

        menuBar.add(mapMenu);
        mapView1.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 1 .............................");
                mapBean.setCenter(new LatLonPoint(0.0f, 0.0f));
                mapBean.setScale(200000000f);// Set the map's scale 
            }
        });
        mapView2.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 2 .............................");
                mapBean.setCenter(new LatLonPoint(50.0f, -100f));
                mapBean.setScale(80000000f);// Set the map's scale 
            }
        });
        mapView3.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 3 .............................");
                mapBean.setCenter(new LatLonPoint(-10.0f, -60f));
                mapBean.setScale(80000000f);// Set the map's scale 
            }
        });
        mapView4.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 4 .............................");
                mapBean.setCenter(new LatLonPoint(50.0f, 30f));
                mapBean.setScale(80000000f);// Set the map's scale 
            }
        });
        mapView5.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 5 .............................");
                mapBean.setCenter(new LatLonPoint(10.0f, 20f));
                mapBean.setScale(80000000f);// Set the map's scale 
            }
        });
        mapView6.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 6 .............................");
                mapBean.setCenter(new LatLonPoint(40.0f, 80f));
                mapBean.setScale(80000000f);// Set the map's scale 
            }
        });
        mapView7.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 7 .............................");
                mapBean.setCenter(new LatLonPoint(-20.0f, 140f));
                mapBean.setScale(80000000f);// Set the map's scale 
            }
        });
        mapView8.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 8 .............................");
                mapBean.setCenter(new LatLonPoint(40.0f, 48.9f));
                mapBean.setScale(3000000f);// Set the map's scale 
            }
        });
        mapView9.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Map View 9 .............................");
                mapBean.setCenter(new LatLonPoint(58.625576f, 16.090435));
                mapBean.setScale(3000000f);// Set the map's scale 
            }
        });

        // Language Menu : 
        JMenu languageMenu = new JMenu("Languages");
        JMenuItem languageEnglish  = new JMenuItem("English");
        JMenuItem languageGerman  = new JMenuItem("German");
        JMenuItem languageFrench  = new JMenuItem("French");
        JMenuItem languageItalian  = new JMenuItem("Italian");
        languageMenu.add(languageEnglish);
        languageMenu.add(languageGerman);
        languageMenu.add(languageFrench);
        languageMenu.add(languageItalian);

        menuBar.add(languageMenu);
        languageEnglish.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Language Option English.............................");
                languageEnglish();	
            }
        });
        languageGerman.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Language Option German  .............................");
                languageGerman();		
            }
        });
        languageFrench.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Language Option French  .............................");
                languageFrench();
            }
        });
        languageItalian.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
            if(debugMode)printDebug("Language Option Italian  .............................");
            languageItalian();
            }
        });

        // Help Menu : 
        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        JMenuItem milstd2525 = new JMenuItem("MILSTD2525b");
        JMenuItem jaxfrontAbout = new JMenuItem("JAXFront");
        JMenuItem openmapAbout = new JMenuItem("OpenMap");	
        helpMenu.add(about);
        helpMenu.addSeparator();
        helpMenu.add(milstd2525);
        helpMenu.addSeparator();
        helpMenu.add(jaxfrontAbout);
        helpMenu.addSeparator();
        helpMenu.add(openmapAbout);
        menuBar.add(helpMenu);

        about.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("About C2SIMGUI .............................");
                JTextArea jta = new JTextArea("George Mason University C4I Center\n"
                    + "Mark Pullen and Mohammad Ababneh\n"
                    + "mpullen@c4i.gmu.edu\n"
                    + "mababneh@c4i.gmu.edu\n"
                    + "2009-2014");
                 JOptionPane.showMessageDialog(
                    bml, jta , "About C2SIMGUI ",JOptionPane.INFORMATION_MESSAGE,null);
            }
        });
        milstd2525.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Help milstd2525 .............................");								
                Dimension di = new Dimension(100,100);
                ImageIcon ii = new ImageIcon(
                    guiFolderLocation + delimiter + "milstd2525b" + delimiter + 
                    "milStd2525_png" + delimiter + "SHAPMFUM----***.png");
                PNGSymbolImageMaker pngsim = new PNGSymbolImageMaker(
                    guiFolderLocation + delimiter + "milstd2525b" + delimiter + 
                    "milStd2525_png"); //\\Referenced Libraries\\
                SymbolReferenceLibrary srl = new SymbolReferenceLibrary(pngsim);    
                ii = srl.getIcon("SHAPMFT-----***", di);    
                if(debugMode)printDebug("The icon height is : " + ii.getIconHeight());
                if(debugMode)printDebug("The icon width is : " + ii.getIconWidth());

                // Display Symbol Chooser
                SymbolChooser sc = new SymbolChooser(srl);
                SymbolChooser.showDialog(
                    editorMapPanel,
                    "Mil Std 2525b Symbols", 
                    srl, 
                    "shapmfum-------");
                if(debugMode)printDebug("The selected Code is : " +sc.getCode());
            }
        });
        setJMenuBar(menuBar);// MenuBar for File, Edit, Config, etc.
        
        // disable push until STOMP connection made
        setConnected(false);
    
    } // end initmenu()
	
    /**
     * Create a MapBean and a MapHandler, which locates and places objects.
     * 
     * The BasicMapPanel automatically creates many default components, 
     * including the MapBean and the MapHandler.
     */
    int lowestPreloadedLayer = 3;// 3 gets us more detailed Europe
                                 // 4 would add Bogaland JPEG
                                 // layers can be turned on in GUI
    public void initMap(){ 
        c2mlProps = new Properties();               //Properties file
        loadResource(c2mlResources, c2mlProps);     // Reading the Properties file 

        // Initialize an array of Route Layers to hold C2SIM Documents Graphics
        routeLayers = new RouteLayer[routeLayerArraySize];
        mapPanel = new BasicMapPanel();		    // OpenMap Map Panel
        mapHandler = mapPanel.getMapHandler();      // Get the default MapHandler the BasicMapPanel created.
        mapBean = mapPanel.getMapBean();            // Get the default MapBean that the BasicMapPanel created.	
        mapBean.setCenter(new LatLonPoint(58.1f, 14.9f));
        mapBean.setScale(24000000f);
    

        // Create and add a LayerHandler to the MapHandler. The LayerHandler
        // manages Layers, whether they are part of the map or not.
        // layer.setVisible(true) will add it to the map. The LayerHandler has
        // methods to do this, too. The LayerHandler will find the MapBean in
        // the MapHandler.	
        if(debugMode)printDebug("Creating Layers...");
        layers = getLayers(c2mlProps);

        // Use the LayerHandler to manage all layers, whether they are
        // on the map or not. You can add a layer to the map by
        // setting layer.setVisible(true). 
        layerHandler = new LayerHandler();

        // add the layers from the property file and do not display them
        // the user can select to them at run time
        for (int i = 0; i < layers.length-lowestPreloadedLayer; i++) {
          layers[i].setVisible(false);
          layerHandler.addLayer(layers[i]);
        }

        // add the last two layers of the shape layers:
        // graticule and political and display them
        for (int i = layers.length-lowestPreloadedLayer; i < layers.length; i++) {
          layers[i].setVisible(true);
          layerHandler.addLayer(layers[i]);
        }
        mapHandler.add(layerHandler);
        if(debugMode)printDebug("Done creating shape layers...");
        mapHandler.add(new ProjectionStack());
        mapHandler.add(new ProjectionStackTool());
        mapHandler.add(new InformationDelegator());
        
        // LayersPanel should be able to receive Location drawings later
        mapHandler.add(new LayersPanel());

        // Add Mouse handling objects. The MouseDelegator manages the
        // MouseModes, controlling which one receives events from the
        // MapBean. The active MouseMode sends events to the layers
        // that want to receive events from it. The MouseDelegator
        // will find the MapBean in the MapHandler, and hook itself up
        // to it.
        mouseDelegator = new MouseDelegator();
        mapHandler.add(mouseDelegator);
        mapHandler.add(new SelectMouseMode());
        mapHandler.add(new PanMouseMode());
        mapHandler.add(new MouseModeButtonPanel());
        mapHandler.add(new DistanceMouseMode());
        mapHandler.add(new NullMouseMode());
        
        // add map controls to GUI
        omts = new OMToolSet();       // Create the directional and zoom control tool
        ToolPanel mapToolBar = new ToolPanel(); // Create an OpenMap toolbar
        mapHandler.add(omts);
        mapHandler.add(mapToolBar);
        
        // buttons to stop and start server
        LineBorder lineBorder = new LineBorder(Color.darkGray);
        mapToolBar.add(serverStopButton);
        mapToolBar.add(new JLabel(" "));// spacer
        mapToolBar.add(serverShareButton);
        serverStopButton.setVisible(false);
        serverStopButton.setBorder(lineBorder);
        serverStopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                if(debugMode)printDebug("Server Stop button selected");
                commandStopServer();
                recordPlayStatus.setVisible(false);
            }
        });
        serverShareButton.setVisible(false);
        serverShareButton.setBorder(lineBorder);
        serverShareButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                selectedCoord = highlightedCoord;
                if(debugMode)printDebug("Server Start button selected");
                commandShareServer();
            }
        });
        mapToolBar.add(recordPlayStatus);
        recordPlayStatus.setForeground(Color.red);
        recordPlayStatus.setBorder(lineBorder);
        recordPlayStatus.setVisible(false);
        splitPane.setRightComponent((Component) mapPanel);

        // Add popup menu to capture coordinates and send them to the editor		
        final JPopupMenu gdcPopup = new JPopupMenu();
        final JMenuItem closestLatLon = new JMenuItem("closestLatLon......................................");
        final JMenuItem closestLatLonB = new JMenuItem("closestLatLonB......................................");
        final JMenuItem nextLatLon = new JMenuItem("nextLatLon.............          ");
        final JMenuItem addLatLon = new JMenuItem("addLatLon.............          ");
        final JMenuItem delLatLon = new JMenuItem("delLatLon.............          ");

        gdcPopup.add(closestLatLon);
        gdcPopup.add(nextLatLon);
        gdcPopup.add(addLatLon);
        gdcPopup.add(delLatLon);
        final JPopupMenu gdcPopupB = new JPopupMenu();
        gdcPopupB.add(closestLatLonB);

        closestLatLon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                selectedCoord = highlightedCoord;
                if(debugMode)printDebug("Lat Lon menu item selected " + selectedCoord + " of " + numCoords);
                if(debugMode)printDebug("Popup Menu Item LatLon Cord : " + e.getActionCommand());
            }
        });
        closestLatLonB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                    selectedCoord = highlightedCoord;
                    if(debugMode)printDebug("Lat Lon menu item selected " + selectedCoord + " of " + numCoords);
                    if(debugMode)printDebug("Popup Menu Item LatLon Cord : " + e.getActionCommand());
            }
        });
        nextLatLon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                    selectedCoord++;
                    if(debugMode)printDebug("Next Lat Lon menu item selected " + selectedCoord + " of " + numCoords);
                    if(debugMode)printDebug("Popup Menu Item LatLon Cord : " + e.getActionCommand());
            }
        });
        addLatLon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                    if(debugMode)printDebug("Add Lat Lon menu item selected " + lattmp + " " + lontmp);
                    if(debugMode)printDebug("Popup Menu Item LatLon Cord : " + e.getActionCommand());
                    redrawing = true;
                    saveTMP(true, false);
            }
        });
        delLatLon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                    if(debugMode)printDebug("Del Lat Lon menu item selected " + lattmp + " " + lontmp);
                    if(debugMode)printDebug("Popup Menu Item LatLon Cord : " + e.getActionCommand());
                    redrawing = true;
                    saveTMP(true, true);
            }
        });
        mapBean.addMouseListener(new MouseAdapter(){
            //Motif Environment
            public void mousePressed(MouseEvent e){
                showPopup(e);
            }
            //Windows Environment
            public void mouseReleased(MouseEvent e){
                showPopup(e);	
            }

            private void showPopup(MouseEvent evt) {		
            if (evt.isPopupTrigger()){
                if((selectedCoord < numCoords - 1) & selectedCoord != -1){
                 gdcPopup.show(evt.getComponent(), evt.getX(), evt.getY());
                }
                else{
                    gdcPopupB.show(evt.getComponent(), evt.getX(), evt.getY());
                }
                if(debugMode)printDebug("Click x = " + evt.getX() + " ....          Click y = " + evt.getY());
                MapMouseEvent mMouseEvent = new MapMouseEvent(null, evt);
                if(debugMode)printDebug(" LAT LON " + mMouseEvent.getLatLon());	
                String latValueString = "", lonValueString = "";	
                latValueString = latValueString + mMouseEvent.getLatLon().getLatitude();
                lonValueString = lonValueString + mMouseEvent.getLatLon().getLongitude();

                int x = evt.getX();
                int y = evt.getY();
                int j=0;

                //if(selectedCoord < numCoords - 1){nextCoord = selectedCoord + 1;}
                LatLonPoint llp = null;
                if (evt.getSource() instanceof MapBean) {
                    llp = ((MapBean) evt.getSource()).getProjection().inverse(x, y);
                    Clipboard systemClipboard = Toolkit.getDefaultToolkit()
                            .getSystemClipboard();

                    // format, UTM convert llp here
                    int lat_i = llp.toString().indexOf("lat=");
                    int lon_i = llp.toString().indexOf("lon=");
                    lattmp = llp.toString().substring(lat_i + 4, lat_i + 11);
                    lontmp = llp.toString().substring(lon_i + 4, lon_i + 11);
                    String latlon = lattmp + " " + lontmp;
                    j = closestTo(lattmp,lontmp);
                    highlightedCoord = j;
                    if(j < numCoords - 1){nextCoord = j + 1;}
                    if(j < 0){
                        if(debugMode)printDebug("closest to " + j + "  " + bmlLatCoords[j] + " " + bmlLonCoords[j]);
                        if(debugMode)printDebug("sending to clipboard: " + latlon);
                        Transferable transferableText = new StringSelection(latlon);
                        systemClipboard.setContents(transferableText, null);
                    }
                }
                closestLatLon.setText("Select closest reference point "+bmlLatCoords[j]+" "+bmlLonCoords[j]);
                closestLatLonB.setText("Select closest reference point "+bmlLatCoords[j]+" "+bmlLonCoords[j]);
                nextLatLon.setText("Select next reference point "+bmlLatCoords[selectedCoord+1]+" "+bmlLonCoords[selectedCoord+1]);
                addLatLon.setText("Add point "+lattmp+" "+lontmp+" after reference");
                delLatLon.setText("Delete selected reference point "+bmlLatCoords[j]+" "+bmlLonCoords[j]);

                }
            }	

        });// end mouseListener()
        
    } // End of initMap method
    
    /**
     * activates/deactivates buttons that lockup in Linux
     */
    void showButtons(boolean doDisplayButtons){
        boolean doShowButtons = doDisplayButtons;
        if(!platformIsLinux)doShowButtons = true;
        initRemoveButton.setVisible(doShowButtons && initIconsOnScreen);
        orderRemoveButton.setVisible(doShowButtons && orderIconsOnScreen);
        reportRemoveButton.setVisible(doShowButtons && reportIconsOnScreen);
    }
    
    /**
     * removes from layers[] all instances of RouteLayer matching IconType parameter
     * @param iconType - the type to remove
     */
    void removeLayersWithIconType(IconType iconType){

        // scan layers, removing those of this iconType
        int checkLayer, updateLayer = 0;
        for (checkLayer = 0; checkLayer < routeLayerIndex; ++checkLayer) {
            if(routeLayers[checkLayer] != null)
            {
                // shuffle down layers we are not removing
                if(routeLayers[checkLayer].getIconType() != iconType){

                    // for reports, update the layer index in the reportUnitMap
                    // as part of the shuffling process
                    if(routeLayers[checkLayer].getIconType() == IconType.REPORT){
                        reportAddUnit(
                            routeLayers[checkLayer].getUnitID(), 
                            new Integer(updateLayer));
                    }
                    drawLocation(null, null, null, checkLayer, updateLayer++, null);
                }
                // remove the layer
                else {
                    // for reports, first remove them from the reportUnitMap
                    if(iconType == IconType.REPORT)
                        reportRemoveUnit(routeLayers[checkLayer].getUnitID());
                    
                    // for orders, find tasks and remove those from layerItemMap
                    if(iconType == IconType.ORDER){
                        for(int taskNumber = 0;
                            taskNumber < routeLayers[checkLayer].numberOfTasks;
                            ++taskNumber)
                        layerRemoveItemId(routeLayers[checkLayer].
                            taskUuid[taskNumber]);
                    }
                    
                    // for reports and orders clear lastUnitID
                    if(iconType == IconType.REPORT || iconType == IconType.ORDER){
                        lastUnitID = "";
                        layerRemoveItemId(routeLayers[checkLayer].getUnitID());
                    }

                    // remove the layer w ehave been checking
                    drawLocation(null, null, null, checkLayer, -1, null);
                }
            }
        }
        routeLayerIndex = updateLayer;
        
        // reset icon-drawing variables
        if(routeLayerIndex <= lowestPreloadedLayer){
            anIconIsOnTheMap = false;
            lastLatitude = "";
            lastLongitude = "";
            anIconIsOnTheMap = false;
        }
        
    }// end removeLayersWithIconType()

    /**
     * Establish and draw Route Layer or delete one
     * also possibly move in routerLayers[]
     * combined in one function so they are synchronized
     */
    public void drawLocation(
        String[] stringArray, 
        String documentType,
        String xmlString,
        int moveFromLayer,
        int moveToLayer,
        MilOrg milOrg){
     
        // updates to routeLayers are done here
        if(moveFromLayer >= 0){
            
            if(debugMode)printDebug("updating routeLayers: "+moveFromLayer+" "+moveToLayer+
                " iconType:"+routeLayers[moveFromLayer].getIconType());
            showButtons(false);
            
            // delete the RouteLayer from the mapHandler
            if(moveToLayer < 0){
                layerRemoveItemId(routeLayers[moveFromLayer].layerItemId);
                mapHandler.remove(routeLayers[moveFromLayer]);
            }
            
            // or move the Routelayer within routeLayers[]
            else routeLayers[moveToLayer] = routeLayers[moveFromLayer];
            showButtons(true);
            return;
        }
        
        // initializations are done here to keep synchronized
        if(milOrg != null){
            if(debugMode)printDebug("adding Initialize layer " + routeLayerIndex + 
                " " + milOrg.uuid + " " + milOrg.getName());
            checkRouteLayerFull();
            routeLayers[routeLayerIndex] = new RouteLayer(
                milOrg.uuid,
                milOrg.getName(), 
                milOrg.getHostility(),
                milOrg.getSymbolIdentifier(),
                milOrg.getLatitude(), 
                milOrg.getLongitude(),
                C2SIMGUI.IconType.INITIALIZE); 
            String sourceName = milOrg.getName();
            if(sourceName == null)sourceName = milOrg.uuid;
            routeLayers[routeLayerIndex].setName(
                "Initial Location of " + sourceName);
            routeLayers[routeLayerIndex].setVisible(true);
            mapHandler.add(routeLayers[routeLayerIndex++]);
            initIconsOnScreen = true;
            return;
        }

        // initialize the route layer for orders and reports
        if(stringArray == null)return;
        showButtons(false);
        if (stringArray.length == 1){
            // special case where stringArrayLength is one
            // (not order or report) - always add a report layer for this
            checkRouteLayerFull();
            routeLayers[routeLayerIndex] = new RouteLayer(IconType.REPORT);
            routeLayers[routeLayerIndex].setName(documentType);
            routeLayers[routeLayerIndex].setVisible(true);
            mapHandler.add(routeLayers[routeLayerIndex]);
            routeLayerIndex++;
        }
        else {
            // capture the unitID, hostility latitude and longitude
            // from the XML Order or Report
            String testUnitID = "";
            String testHostility = "";
            String testLatitude = "";
            String testLongitude = "";

            boolean parsedUnitID = false;
            boolean parsedHostility = false;
            boolean parsedLatitude = false;
            boolean parsedLongitude = false;
            boolean isReport = false;
            boolean isObservationReport = 
                xmlString.contains("<ReportContent><ObservationReportContent>");
            for (int i=0; i<stringArray.length; ++i){

                // check each major document to avoid adding to map more 
                // than once - look for key elements each must have

                // C2SIM Report
                if(documentType.equals("C2SIM Report")) {
                    isReport = true;
                    if(!parsedUnitID){
                        if(isObservationReport){
                            if(stringArray[i].equals("ActorReference")){
                                testUnitID = stringArray[i+1];
                                parsedUnitID = true;
                            }
                        } else {// PositionReport
                            if(stringArray[i].equals("SubjectEntity")){
                                testUnitID = stringArray[i+1];
                                parsedUnitID = true;
                            }
                        }
                    }
                    if(!parsedHostility && stringArray[i].endsWith("Hostility")){
                        testHostility = stringArray[i+1];
                        parsedHostility = true;
                    }
                    if(!parsedLatitude && stringArray[i].endsWith("Latitude")){
                        testLatitude = stringArray[i+1];
                        parsedLatitude = true;
                    }
                    if(!parsedLongitude && stringArray[i].endsWith("Longitude")){
                        testLongitude = stringArray[i+1];
                        parsedLongitude = true;
                    }
                }
                // IBML09 Report
                else if(documentType.equals("IBML09 Report")) {
                    isReport = true;

                    // we want the last UnitID in this one
                    if(stringArray[i].endsWith("UnitID")){
                        testUnitID = stringArray[i+1];
                    }
                    if(!parsedHostility && stringArray[i].endsWith("Hostility")){
                        testHostility = stringArray[i+1];
                        parsedHostility = true;
                    }
                    if(!parsedLatitude && stringArray[i].endsWith("Latitude")){
                        testLatitude = stringArray[i+1];
                        parsedLatitude = true;
                    }
                    if(!parsedLongitude && stringArray[i].endsWith("Longitude")){
                        testLongitude = stringArray[i+1];
                        parsedLongitude = true;
                    }
                }
                // CBML Light Report
                else if(documentType.equals("CBML Light Report")) {
                    isReport = true;

                    // we want the last UnitID in this one
                    if(stringArray[i].endsWith("UnitID")){
                        testUnitID = stringArray[i+1];
                    }
                    if(!parsedHostility && stringArray[i].endsWith("Hostility")){
                        testHostility = stringArray[i+1];
                        parsedHostility = true;
                    }
                    if(!parsedLatitude && stringArray[i].endsWith("Latitude")){
                        testLatitude = stringArray[i+1];
                        parsedLatitude = true;
                    }
                    if(!parsedLongitude && stringArray[i].endsWith("Longitude")){
                        testLongitude = stringArray[i+1];
                        parsedLongitude = true;
                    }
                }
                // C2SIM Order
                else if(documentType.equals("C2SIM Order")) {
                    if(!parsedUnitID && stringArray[i].endsWith("PerformingEntity")){
                        testUnitID = stringArray[i+1];
                        parsedUnitID = true;
                    }
                    lastHostility =  getMilOrgHostility(testUnitID);
                    if(!parsedLatitude && stringArray[i].endsWith("Latitude")){
                        testLatitude = stringArray[i+1];
                        parsedLatitude = true;
                    }
                    if(!parsedLongitude && stringArray[i].endsWith("Longitude")){
                        testLongitude = stringArray[i+1];
                        parsedLongitude = true;
                    }
                }
                // IBML09 Order
                else if(documentType.equals("IBML09 Order")) {
                    if(!parsedUnitID && stringArray[i].endsWith("UnitID")){
                        testUnitID = stringArray[i+1];
                        parsedUnitID = true;
                    }
                    lastHostility = "FR";// assume all orders are friendly
                    if(lastLatitude.length() == 0 && stringArray[i].endsWith("Latitude")){
                        testLatitude = stringArray[i+1];
                        parsedLatitude = true;
                    }
                    if(!parsedLongitude && stringArray[i].endsWith("Longitude")){
                        testLongitude = stringArray[i+1];
                        parsedLongitude = true;
                    }
                }
                // CBML Light Order
                else if(documentType.equals("CBML Light Order")) {
                    if(!parsedUnitID && stringArray[i].endsWith("TaskeeWhoRef")){
                        testUnitID = stringArray[i+1];
                        parsedUnitID = true;
                    }
                    lastHostility = "FR";// assume all orders are friendly
                    if(lastLatitude.length() == 0 && stringArray[i].endsWith("Latitude")){
                        testLatitude = stringArray[i+1];
                        parsedLatitude = true;
                    }
                    if(!parsedLongitude && stringArray[i].endsWith("Longitude")){
                        testLongitude = stringArray[i+1];
                        parsedLongitude = true;
                    }
                }
            }// end for (int i 

            // indicate below map what icon source is
            if(isReport)reportIconsOnScreen = true;
            else orderIconsOnScreen = true;

            // if this and previous match, don't add to map
            if(testUnitID.equals(lastUnitID) && testLatitude.equals(lastLatitude) && 
                    testLongitude.equals(lastLongitude)){
                if(debugMode)printDebug("identical icon not displayed for unit:" + 
                    lastUnitID + " latitude:" + lastLatitude + " longitude:" + 
                    lastLongitude);
                return;
            }
            lastUnitID = testUnitID;
            lastLatitude = testLatitude;
            lastLongitude = testLongitude;

            // create a multi-object graph object on the map
            if(mapGraph & redrawing){
                for(int r = 0; r < routeLayerIndex; r++){
                    routeLayers[r].clearGraphics();
                }
            }

            // lookup this unit to see if it has a report layer already
            // if not, add a new layer
            Integer lookupRouteLayer = reportGetUnitIndex(testUnitID);
            if(lookupRouteLayer == null || !isReport) {
                if(debugMode)printDebug("adding new map layer " + routeLayerIndex + 
                    " for " + testUnitID + " " + documentType);
                String layerRelation = "";
                if(documentType.endsWith("Order")){
                    checkRouteLayerFull();
                    routeLayers[routeLayerIndex] = 
                        new RouteLayer(stringArray, documentType, xmlString, 
                        IconType.ORDER, testUnitID, routeLayerIndex);
                    layerRelation = " for ";
                }
                else if(isReport){
                    if(lookupRouteLayer == null){// no report on this unit yet
                        reportAddUnit(testUnitID, new Integer(routeLayerIndex));
                        checkRouteLayerFull();
                        routeLayers[routeLayerIndex] = 
                            new RouteLayer(stringArray, documentType, 
                                xmlString, IconType.REPORT, testUnitID,
                                routeLayerIndex);
                    }
                    layerRelation = " from ";
                } 
                String sourceUnitName = getMilOrgName(testUnitID);
                if(sourceUnitName == null)
                    sourceUnitName = testUnitID;
                routeLayers[routeLayerIndex].setName(
                    documentType + layerRelation + sourceUnitName);
                routeLayers[routeLayerIndex].setVisible(true);
                mapHandler.add(routeLayers[routeLayerIndex]);
                routeLayerIndex++;
            }
            else {
                // this is a report with already loaded unit and there is an 
                // existing layer for this unit; check if this is a new location
                // if so add the report to this layer
                RouteLayer testRouteLayer = 
                    routeLayers[lookupRouteLayer.intValue()];
                if(testRouteLayer != null){
                    String checkLatitude = testRouteLayer.getLatitude();
                    String checkLongitude = testRouteLayer.getLongitude();
                    if(testLatitude != null && testLongitude != null)
                        if(!checkLatitude.equals(lastLatitude) ||
                           !checkLongitude.equals(lastLongitude))
                           testRouteLayer.addToRouteLayer(
                                stringArray, 
                                documentType,
                                xmlString,
                                lookupRouteLayer);
                }
            }
            mapGraph = true;// a graph is there
        }// end else/if stringArray.length == 1
        
        // impose a 100ms gap between routeLayers changes to avoid tangling
        try{Thread.sleep(100);}catch(InterruptedException ie){}
        showButtons(true);
        
    } // end drawLocation()
    
    
    /*
     *   check whether the routeLayer array is full
     *   and if so clear it out    
     */
    void checkRouteLayerFull()
    {     
        // check if the number of graphical objects is too high
        // warn the user at 90% full array
        if (routeLayerIndex == routeLayerArraySize*.9) {
            (new Thread(new warningPopup())).start();
        }

        // Force a Map cleanup when the maximum number of graphics is on the map
        if (routeLayerIndex >= routeLayerArraySize-1){
            for (int i = 0; i <= routeLayerIndex; i++){
                layerRemoveItemId(routeLayers[i].layerItemId);
                mapHandler.remove(routeLayers[i]);
            }
            routeLayerIndex = 0;	// set number of graphics to 0
            lastLatitude = "";
            lastLongitude = "";
        }
    }// endcheckRouteLayerFull()
    
    
    /**
     *  gets coordinates from point selected by user on map
     *  done in thread so GUI doesn't lock up
     */
    private class getCoords extends Thread{
        public void run() {
    
            // wait until the user clicks on map
            try{
                while(gettingCoords && !interruptGettingCoords){
                   if(!mouseDelegator.getActiveMouseModeID().equals("Gestures")) {
                       interruptGettingCoords = true;
                       break;
                   }
                   Thread.sleep(100);
                   Thread.yield();
                }
            }catch(InterruptedException e){}
            
            // loading was interrupted
            if(gettingCoords && interruptGettingCoords) {
                getLatitude.setText("LAT:  0.000");
                getLongitude.setText("LON:  0.000");
                getCoordsButton.setText("GET COORDS FROM MAP");
                gettingCoords = false;
                interruptGettingCoords = false;
                return;
            }

            // we got coordinates
            if(!gettingCoords) {
                
                // restore button label
                getCoordsButton.setText("GET COORDS FROM MAP");
            }
            gettingCoords = false;
            interruptGettingCoords = false;
        }
    }// end getCoords subclass
    
    /**
     * replaces an element value in XML string
     * works only if data does not contain '<'
     * which is OK for lat/lon
     * 
     */
    String replaceFirstLatLonInXml(String xml, String tag, String newValue){
        int tagIndex = xml.indexOf(tag+'>');
        if(tagIndex < 0)return xml;
        int afterTagIndex = tagIndex + tag.length() + 1;
        int endTagIndex = xml.indexOf('<',afterTagIndex);
        return xml.substring(0,afterTagIndex) + newValue + xml.substring(endTagIndex);
    }
    
    /**
     *  puts coords selected by getCoords() into XMLdocument after button
     *  coordsToXmlButton clicked - done in thread so GUI doesn't lock up
     * assumes Latitude, Longitude appear in pairs with Latitude first
     */
    private class coordsToXml extends Thread{
        String xml;
        public coordsToXml(String xmlRef) {
            xml = xmlRef;
        }
        public void run() {
            
            // count number of lat/lon pairs in the xml
            int numberOfCoordPairs = 0;
            transferIndex = -1;
            String xmlForCount = new String(xml); 
            while(xmlForCount.contains("Latitude>") && 
                  xmlForCount.contains("Longitude>")){
                numberOfCoordPairs++;
                
                // strip off up to start of Longitude tag, then to its end 
                xmlForCount = xmlForCount.substring(
                    xmlForCount.indexOf("Longitude>")+("Longitude>").length());
                xmlForCount = xmlForCount.substring(
                    xmlForCount.indexOf("Longitude>")+("Longitude>").length());
            }
            if(numberOfCoordPairs == 0) {
                showInfoPopup( 
                    "XML contains no coordinate pairs", 
                    "Inserting coordinates not possible");
                
                // restore labels and return
                documentTypeLabel.setText(saveLabel);
                documentTypeLabel.setForeground(Color.BLUE);
                xuiCacheHash = 0;
                coordsToXmlButton.setText(coordsText);   
                return;
            }
            
            // loop through all pairs inserting coodinates
            String frontPart = "", backPart = new String(xml);
            documentTypeLabel.setForeground(Color.RED);
            for(coordIndex = 0; coordIndex < numberOfCoordPairs; ++coordIndex) 
            {
                // inform user of coord pair being entered
                documentTypeLabel.setText(
                    "click map for coordinate pair " + (coordIndex+1) + " of " + 
                        numberOfCoordPairs);    
                
                // wait until the user clicks a new location
                movingCoords = true;
                try{
                    while(movingCoords){
                       if(interruptEnteringCoords)break;
                       Thread.sleep(100);
                       Thread.yield();   
                    }
                }
                catch(InterruptedException e){}
                if(interruptEnteringCoords)break; 
                
                // synchronize point transfer
                transferIndex = coordIndex;
                
                // insert the new Latitude, Longitude pair
                // in first Latitude and Longitude in backPart
                backPart = replaceFirstLatLonInXml(
                    backPart,
                    "Latitude",
                    getLatitude.getText().substring(4));
                backPart = replaceFirstLatLonInXml(
                    backPart,
                    "Longitude",
                    getLongitude.getText().substring(4));                
            
                // strip off processed part, ending at Longitude
                int tagLength = ("Longitude>").length();
                
                // find end of opening Longitude tag
                int breakPoint1 = backPart.indexOf("Longitude>") + tagLength;
                
                // find end of closing Longitude tag
                int breakPoint2 = backPart.substring(breakPoint1).indexOf("Longitude>");
                breakPoint2 += tagLength;
                
                // move beginning part to frontPart then strip it off of backPart
                int breakPoint = breakPoint1 + breakPoint2;
                frontPart += backPart.substring(0,breakPoint);
                backPart = backPart.substring(breakPoint);
                
            }// end for(int coordIndex
            xml = frontPart + backPart;

            // confirm we have saved parameters to allow JaxFront reload
            if(saveLabel==null || saveXsdUrl==null ||
                saveXuiUrl==null || saveRoot==null){
                showInfoPopup( 
                    "cannot edit - XML not loaded", 
                    "move coords to XML failed");
                coordsToXmlButton.setText(coordsText);
                return;
            }
            
            // tell user to wait while JAXFront loads
            coordsToXmlButton.setText("wait...");
            
            // load XML back into JAXFront panel
            try{
                // put it into file as JaxFront demands
                if(xmlTempfile != null)xmlTempfile.delete();
                xmlTempfile = File.createTempFile("xml","TempXML");
                FileWriter tempFile = new FileWriter(xmlTempfile);
                tempFile.write(xml);
                tempFile.close();

                // restart the JaxFront panel with edited XML
                xmlUrl = URLHelper.getUserURL(xmlTempfile.getAbsolutePath());
                initDom("default-context", 
                        saveXsdUrl, 
                        xmlUrl,
                        saveXuiUrl,
                        saveRoot);
            } catch(Exception e) {
                printError("Exception in coordsToXmlButton:"+e);
                e.printStackTrace();
            }

            // restore last JaxFront panel label
            documentTypeLabel.setText(saveLabel);
            documentTypeLabel.setForeground(Color.BLUE);

            // disable update warning
            xuiCacheHash = 0;
            
            // restore label on triggering button
            coordsToXmlButton.setText(coordsText);

        }// end run()
    }//end coordsToXml subclass
             
    /**
     *  loads a report selected by user from map
     *  done in thread so GUI doesn't lock up
     */
    private class loadReport extends Thread{
        public void run() {
    
            // wait until the user clicks loadReportButton
            try{
                while(loadingReport && !interruptReportLoading){
                    if(!mouseDelegator.getActiveMouseModeID().equals("Gestures")) {
                        interruptReportLoading = true;
                        break;
                   }
                   Thread.sleep(100);
                   Thread.yield();
                }
            }catch(InterruptedException e){}
            
            // loading was interrupted
            if(loadingReport && interruptReportLoading) {
                xmlReport = null;
                loadReportButton.setText("LOAD REPORT FROM MAP");
                loadingReport = false;
                interruptReportLoading = false;
                return;
            }

            // we got a report
            if(!loadingReport) {

                // look up the report in map for this layer
                // (parameters are set by RouteLayer.mousePressed())
                xmlReport = reportGetText(reportLatitude, reportLongitude);
                
                // check that we got an acceptable XML string
                if(!xmlReport.contains("ReportBody") &&
                    !xmlReport.contains("BMLReport") &&
                    !xmlReport.contains("CBMLReport"))return;

                // prepare for JaxFront from report received
                loadJaxFrontPanel();

                // restore button label
                loadReportButton.setText("LOAD REPORT FROM MAP");
            }
            loadingReport = false;
            interruptReportLoading = false;
        }
    }// end loadReport subclass
    
    /**
     * called by shutdownHook to shut down 
     * STOMP connection if it is running
     */
    private class shutdownStomp extends Thread {
        public void run() {
            if(xmlTempfile != null)xmlTempfile.delete();
            if(subscriber != null) {
                if(getConnected()){
                    if(debugMode)printDebug("SHUTTING DOWN STOMP SUBSCRIPTION");
                    subscriber.stopSub();
                }
            }
        }
    }
    
    /**
     * called by shutdownHook to shut down 
     * Recorder if it is running
     */
    private class shutdownRecorder extends Thread {
        public void run() {
            if(runRecorder) {
                if(debugMode)printDebug("SHUTTING DOWN RECORDER");
                recorder.close();
                runRecorder = false;
                
                // sleep long enough for Recorder to stop
                try{Thread.sleep(300);}catch(InterruptedException ie){}
            }
        }
    }// end shutdownRecorder class

    /**
     * resets state at end of playback
     */
    void stopPlayback(){
        runPlayer = false;
        pausePlayer = false;
        playButton.setText("PLAY RECORDING");
        pauseButton.setVisible(false);
        pauseButton.setText("PAUSE PLAY");
        if(!listeningWhenPlayStarted){
            listenToXml = false;
            reportListenerButton.setText("START LISTENING");
        } 
        reportListenerButton.setEnabled(true);
    }// end stopPlayback()
    
    /**
     * accepts signal from player there is no more data - starts
     * 5 second timeout to allow final messages to arrive from server
     */
    void playFileDone(){
        
        try{Thread.sleep(5000);}catch(InterruptedException ie){}
        stopPlayback();
    }// end playFileDone()
    
    /**
     * called by shutdownHook to shut down 
     * Player if it is running
     */
    private class shutdownPlayer extends Thread {
        public void run() {
            if(runPlayer) {
                if(debugMode)printDebug("SHUTTING DOWN PLAYER");
                runRecorder = false;
                
                // sleep long enough for Player to stop
                try{Thread.sleep(300);}catch(InterruptedException ie){}
            }
        }
    }
    
    /**
    *   creates a warning popup, in thread so processing continues
    */
    private class warningPopup extends Thread{
        public void run() {
            JOptionPane.showMessageDialog(
                bml, 
                "The number of Graphical Objects on the map is too high. " +
                "\n Please click the Erase button before opening the next document." +
                "\n All graphics will be erased when the maximum is reached",
                "Map Graphics Warning",	JOptionPane.WARNING_MESSAGE);
        }
    } 
    
    /**
     * creates OK/Cancel dialog popup
     * returns true if user clicks OK
     */
    boolean okCancelPopup(String frameText, String message){
        
        if(runningServerTest)return true;
        int answer = JOptionPane.showConfirmDialog(
                    null,  
                    message, 
                    frameText,
                    JOptionPane.OK_CANCEL_OPTION);
        return (answer == JOptionPane.OK_OPTION);
        
    }// end oKCancelPopup
    
    /**
     * creates input dialog popup
     * returns new value 
     * returns zero if user selects cancel
     */
    String inputTimeMultPopup(String popupText){
        
        if(runningServerTest)return "0";
        String answer = JOptionPane.showInputDialog(  
                    popupText, 
                    "0");
        return answer;
        
    }// end inputTimeMultPopup   
	
    /**
     * draws graphics from an OPORD
     * @param sOPORDName
     * @param sShapeName
     * @param tempShape
     * @param shapeType
     * @param shapeCoords 
     */
    public void drawOPORD(String sOPORDName, String sShapeName, String[] tempShape, String shapeType, int shapeCoords){
        mapGraph = true;
        if(debugMode)printDebug("--------------------------------------====================--------------drawOPORD-----");
        if (!mapOPORD) {
            routeLayerOPORD = new RouteLayer(IconType.ORDER);
            routeLayerOPORD.setName(sOPORDName); //bmlDocumentType
            routeLayerOPORD.setVisible(true);
            mapHandler.add(routeLayerOPORD);
            mapOPORD = true;
        }
        routeLayerOPORD.createOPORDGraphics(sShapeName, tempShape, shapeType, shapeCoords);
    }
	
    /**
     * loads an XML file into the JaxFront panel
     * which must already be instantiated from xmlReport
     */
    synchronized void loadJaxFrontPanel() {
        
        String jaxDocumentType = "";
      
        // need a valid file to do this - write it from xmlString
        if(xmlReport == null)return;
        int xmlLength = xmlReport.length();
        if(xmlLength > 400)xmlLength = 400;
        String first400 = xmlReport.substring(0,xmlLength);
        if(first400.contains("CBMLReport"))
        {
            orderDomainName = "CBML";
            generalBMLFunction = "CBML";
            reportBMLType = "CBML";
            jaxDocumentType = "CBML Report From Server";	
            documentTypeLabel.setText(jaxDocumentType);
            xsdUrl = URLHelper.getUserURL(cbmlReportSchemaLocation);// CBML report schema XSD
            root = "CBMLReport";
        }
        else if(first400.contains("ReportBody")){
            orderDomainName = "C2SIM";
            generalBMLFunction = "C2SIM";
            reportBMLType = "C2SIM";
            jaxDocumentType = "C2SIM Report From Server";	
            documentTypeLabel.setText(jaxDocumentType);
            if(xmlReport.contains("Autonomous"))xsdUrl =
                URLHelper.getUserURL(asxReportSchemaLocation);// ASX report schema XSD
            else xsdUrl = 
                URLHelper.getUserURL(c2simReportSchemaLocation);// C2SIM report chema XSD
            root = "MessageBody";
        }
        else if(first400.contains("BMLReport")){
            orderDomainName = "IBML";
            generalBMLFunction = "IBML";
            reportBMLType = "IBML";
            jaxDocumentType = "IBML09 Report From Server";
            documentTypeLabel.setText(jaxDocumentType);
            xsdUrl = 
              URLHelper.getUserURL(ibml09ReportSchemaLocation);//Schema File XSD
            root = "BMLReport";
        }
      
        // clear current JaxFront cache
        releaseXUICache();
  
        // write the XML to a temporary file that can be input to JaxFront
        File tempFile = new File(System.getProperty("java.io.tmpdir")+"/tempXml.xml");
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
            writer.write(xmlReport);
            writer.close();
        }catch(IOException ioe){
            printError("IO Exception in loadJaxFrontPanel");
            return;
        }
 
        // load JaxFront components
        releaseXUICache();
        xmlUrl = URLHelper.getUserURL(tempFile.getAbsolutePath()); // XML file		
        xuiUrl = URLHelper.getUserURL(bml.guiLocationXUI);         // Jaxfront XUI file
        try{
            initDom("default-context", xsdUrl, xmlUrl, xuiUrl, root);
        }
        catch (Exception e){
            loadReportButton.setText("LOAD REPORT FROM MAP");
            loadingReport = false;
            printError("Exception in loadJaxFront for Report from map:"+e);
            e.printStackTrace();
            return;
        }
        
    }// end loadJaxFrontPanel()
	
    /**
     * Various Frame methods (no-op)
     */
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconifieed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void showHelp(HelpEvent event) {}
    public void itemStateChanged(ItemEvent arg0) {}

    public void windowClosed(WindowEvent e) {
            System.exit(0);
    }

    public void windowClosing(WindowEvent e) {
            System.exit(0);
    }

    /**
     * post a server status string 
     * @param serverState 
     */
    public void setServerStateLabel(String serverState) {

        // defend against serverState that accidentally includes whitepace or newline
        serverState.trim();
        if(serverState.endsWith("\n"))
            serverState = serverState.substring(0,serverState.length()-1);    
        
        // translate C2SIM standard SystemComandType to resulting server states
        if(serverState.equals("InitializationComplete"))serverState = "INITIALIZING";
        if(serverState.equals("PauseScenario"))serverState = "PAUSED";
        if(serverState.equals("ResetScenario"))serverState = "UNINITIALIZED";
        if(serverState.equals("ResumeScenario"))serverState = "RUNNING";
        if(serverState.equals("ShareScenario"))serverState = "INITIALIZED";
        if(serverState.equals("StartScenario"))serverState = "RUNNING";
        if(serverState.equals("StopScenario"))serverState = "INITIALIZED";
        if(serverState.equals("SubmitInititalization"))serverState = "INITIALIZING";
        
        // ignore SystemCommandss that do not affect server state
        if(serverState.equals("MagicMove"))return;
        if(serverState.equals("PausePlayback"))return;
        if(serverState.equals("PauseRecording"))return;
        if(serverState.equals("PlaybackRealtimeMultipleReport"))return;
        if(serverState.equals("PlaybackStatusReport"))return;
        if(serverState.equals("RecordingStatusReport"))return;
        if(serverState.equals("RefreshInit"))return;
        if(serverState.equals("RequestPlaybackRealtimeMultiple"))return;
        if(serverState.equals("RequestPlaybackStatus"))return;
        if(serverState.equals("RequestRecordingStatus"))return;
        if(serverState.equals("RequestSimulationRealtimeMultiple"))return;
        if(serverState.equals("ResumePlayback"))return;
        if(serverState.equals("ResumeRecording"))return;
        if(serverState.equals("SetPlaybackRealtimeMultiple"))return;
        if(serverState.equals("SetSimulationRealtimeMultiple"))return;
        if(serverState.equals("SimulationRealtimeMultipleReport"))return;
        if(serverState.equals("StartPlayback"))return;
        if(serverState.equals("StartRecording"))return;
        if(serverState.equals("StopPlayback"))return;
        if(serverState.equals("StopRecording"))return;

        // use the state value to set top frame indicator
        if(serverState.equals("STOPPED")|| 
            serverState.substring(0,5).equals("StopS")) {
            serverStatusLabel.setText("STOPPED");
            serverStatusLabel.setForeground(Color.RED);
        }
        else if(serverState.equals("RUNNING") || 
            serverState.substring(0,5).equals("Start")) {
            serverStatusLabel.setText("RUNNING");
            serverStatusLabel.setForeground(Color.BLUE);
            serverStopButton.setVisible(
                stompIsConnected && !serverPassword.equals(""));
            serverShareButton.setVisible(false);
            if(recordPlayStatus.getText().equals("RECORDING"))
                recordPlayStatus.setVisible(true);
        }
        else if(serverState.equals("PAUSED")|| 
            serverState.substring(0,5).equals("Pause")) {
            serverStatusLabel.setText("PAUSED");
            serverStatusLabel.setForeground(Color.PINK);
        }
        else if(serverState.equals("INITIALIZED")|| 
            serverState.substring(0,5).equals("Share")) {
            serverStatusLabel.setText("INITIALIZED");
            serverStatusLabel.setForeground(Color.ORANGE);
        }
        else if(serverState.equals("INITIALIZING")|| 
            serverState.substring(0,6).equals("Submit")) {
            serverStatusLabel.setText("INITIALIZING");
            serverStatusLabel.setForeground(Color.CYAN);
            serverStopButton.setVisible(false);
            serverShareButton.setVisible(
                stompIsConnected && !serverPassword.equals(""));
        }
        else if(serverState.equals("UNINITIALIZED")|| 
            serverState.substring(0,5).equals("Reset")) {
            serverStatusLabel.setText("UNINITIALIZED");
            serverStatusLabel.setForeground(Color.BLACK);
            initStatusLabel.setText("0");
        }
        else {
            serverStatusLabel.setText("UNKNOWN");
            serverStatusLabel.setForeground(Color.GRAY);
        }
    }// end setServerStateLabel()
    
    /**
     * Create a blank C2SIM with just an xsd
     */
    private void newDocument() {
        String root = null;
        releaseXUICache();
        String newDocumentType = "";

        // create a method to deal with drawing unknown documents
        newDocumentType ="Unknown";
        documentTypeLabel.setText(newDocumentType);

        // Report Schema
        JFileChooser xsdFc = new JFileChooser(guiFolderLocation + delimiter);
        xsdFc.setDialogTitle("Enter Schema XSD file name");
        xsdFc.showOpenDialog(this);
        if(xsdFc.getSelectedFile() == null)return;
        URL url = URLHelper.getUserURL(xsdFc.getSelectedFile().toURI().toString());
        URL xmlUrl = null;  // Empty XML
        URL xuiUrl = null;  // XUI Style // Default View
        initDom("default-context", url, xmlUrl, xuiUrl, root);
        
    }// end newDocument()

    /**
     * Open an existing C2SIM (XML Document)
     */
    private void openDocument() {	
        String orderString = new String();
        String[] orderStringArray;
        String schemaFileName=""; // schema file name
        String schemaFile="";  // schema file name and location; full path // make it URI
        String xmlFileString ="";
        String schemaFileString ="";
        String openDocumentType = "";

        // create a method to deal with drawing unknown documents
        openDocumentType ="Unknown";	
        documentTypeLabel.setText("Unknown Document");	
        releaseXUICache();

        // XML file
        JFileChooser xmlFc = new JFileChooser(guiFolderLocation + delimiter);
        xmlFc.setDialogTitle("Enter XML file name");
        xmlFc.showOpenDialog(this);
        if(xmlFc.getSelectedFile() == null)return;
        xmlFileString = xmlFc.getSelectedFile().toURI().toString();
        URL xmlUrl = URLHelper.getUserURL(xmlFileString);

        // Schema File XSD
        JFileChooser xsdFc = new JFileChooser(guiFolderLocation + delimiter);
        xsdFc.setDialogTitle("Enter Schema XSD file name");
        xsdFc.showOpenDialog(this);
        URL url = URLHelper.getUserURL(xsdFc.getSelectedFile().toURI().toString());	
        URL xuiUrl = null;
		
        // Generate the swing GUI
        drawFromXML(
          "default-context", 
          url, 
          xmlUrl, 
          xuiUrl, 
          root, 
          openDocumentType,
          null,
          null,
          null,
          null,
          null,
          c2simProtocolVersion,
          true
        );
        
    }// end openDocument()

    /**
     * Save an Edited (optionally validated) C2SIM (XML Document)
     */
    private void saveDocument() {	
        if(debugMode)printDebug("Save the current XML Document ");
        String xmlFileString ="";
        JFileChooser xmlFc = new JFileChooser(guiFolderLocation + delimiter);
        xmlFc.setDialogTitle("Enter XML file name to save");
        xmlFc.showSaveDialog(this);
        if(xmlFc.getSelectedFile() == null)return;
        xmlFileString = xmlFc.getSelectedFile().toString();

        //set temporary values to actual targets
        tmpFileString = xmlFileString;
        tmpUrl = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString());

        //hide new temporary values
        URL tmpUrl2 = URLHelper.getUserURL(xmlFc.getSelectedFile().toURI().toString() + "(tmp)");
        String tmp2 = xmlFc.getSelectedFile().toString() + "(tmp)";

        //save document
        saveTMP(false, false);

        //set new temporary values
        tmpFileString = tmp2;
        tmpUrl = tmpUrl2;
        
    }// end saveDocument()
	
    /**
     * Save a Temporary (optionally validated) C2SIM (XML Document)
     */
    private void saveTMP(boolean addLatLon, boolean arg1) {	
        xmlUrl = tmpUrl;
        File file = new File(tmpFileString);

        try {
            String filedata = currentDom.serialize().toString();
            filedata = moveLatLon(filedata);
            if(addLatLon){
                if(debugMode)printDebug("modifying file for "+lattmp+" "+lontmp);
                filedata = addPointToFile(filedata, arg1);
            }
            if(debugMode)printDebug("Saving to " + tmpFileString);
            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFileString));
            out.write(filedata);
            out.close();
        } catch (ValidationException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        }	
    }// end saveTmp()
    
    /**
     * makes a string of '-' same length as its argument
     * (at least one dash)
     */
    String makeDashString(String template) {
        int dashLength = template.length();
        String dashString = "-"; 
        for(int i=1; i<dashLength; ++i)
           dashString += '-'; 
        return dashString;
    }

    /**
     * replaces Latitude and Longitude in file 
     * @param fileIn
     * @return modified file contents
     */
    private String moveLatLon(String fileIn){
        int index1, index2, index3, pad, pad0=13, padA = 42, padB = 46, latpad = 14, lonpad = 15;
        String latlong="<bml:LatLong>",cutOut="<bml:LatLong/>",lat="<bml:Latitude>",lon="<bml:Longitude>";
        String latlongB="<CBML:LatLong",cutOutB="<CBML:LatLong/>",latB="<CBML:LatitudeCoordinate>",lonB="<CBML:LongitudeCoordinate>";

        //if(debugMode)printDebug("----------------------------------------------------------------");
        String cutStr="",latPiece="",lonPiece="",currentLat="",currentLon="",currentLatLon="",newLatLon="";
        index1 = fileIn.indexOf(latlong, 1);
        if(index1 == -1){
            latlong = latlongB;   cutOut = cutOutB;
            lat = latB;           lon = lonB;
            padA = padB;          pad0 = 15;
            latpad = 26;          lonpad = 27;
            if(debugMode)printDebug("Switched to Opord standards--------------------------");
        }
        index1 = 0;
        while ((index1 = fileIn.indexOf(latlong, index1+1)) > 0){
            pad = pad0; // for deletion (default)
            if((index2 = fileIn.indexOf(cutOut, index1)) != index1){
                //split coords
                latPiece = fileIn.substring(index1 + pad, index1 + pad + 7);
                lonPiece = fileIn.substring(index1 + pad + 8, index1 + pad + 15);
                //if(debugMode)printDebug(latPiece + " " + lonPiece + "==========================");

                //get lat and long strings
                index2 = fileIn.lastIndexOf(lon, index1);
                index3 = fileIn.indexOf("<", index2+1);
                currentLon = fileIn.substring(index2 + lonpad, index3);
                index2 = fileIn.lastIndexOf(lat, index1);
                index3 = fileIn.indexOf("<", index2+1);
                currentLat = fileIn.substring(index2 + latpad, index3);
                //if(debugMode)printDebug(currentLat + " " + currentLon + "========--=========");
                index2 = fileIn.lastIndexOf("\n", index2-1); 

                //replace lat and replace lon
                currentLatLon = fileIn.substring(index2, index1);
                newLatLon = currentLatLon;
                newLatLon = newLatLon.replace(
                    currentLat,latPiece).replace(currentLon, lonPiece);
                fileIn = fileIn.replace(currentLatLon, newLatLon);
                pad = padA; // for deletion
            }
            index2 = fileIn.lastIndexOf("\n",index1);
            cutStr = fileIn.substring(index2, index1 + pad);
            fileIn = fileIn.replace(cutStr, "");
        }		
        return fileIn;
            
    }// end moveLAtLon()
	
    private String addPointToFile(String fileIn, boolean del){
		
        int index1,index2,pad=13,ii=0;
		
        // Locate selectedCoord
        String latSearchA = "<bml:Latitude>"+bmlLatCoords[selectedCoord].trim()+"</bml:Latitude>";
        String lonSearchA = "<bml:Longitude>"+bmlLonCoords[selectedCoord].trim()+"</bml:Longitude>";
        String latSearch = latSearchA;
        String lonSearch = lonSearchA;
        index1 = fileIn.indexOf(latSearch);
        index2 = fileIn.indexOf(lonSearch);
		
        while(Math.abs(index2 - index1) > 110 & ii < 20){
            if(index2 > index1){index1 = fileIn.indexOf(latSearch,index1+1);}
            else{index2 = fileIn.indexOf(lonSearch,index2+1);}
            if(debugMode)printDebug("searching "+index1+" "+index2+" "+ii);
            if(debugMode)printDebug(fileIn.substring(index1,index1+14));
            ii++;
        }
		
        //Define bounds of xml Block
        index1 = fileIn.lastIndexOf("<bml:Coords>", index1);
        if(index1 == -1 || (index2-index1) > 400){
            index1 = fileIn.lastIndexOf("<bml:WhereLocation>", index2);
            index1 = fileIn.lastIndexOf("\n", index1);
            index2 = fileIn.indexOf("</bml:WhereLocation>", index1);
            pad=20;
        }
        else{
            index1 = fileIn.lastIndexOf("\n", index1);
            index2 = fileIn.indexOf("</bml:Coords>", index1);
        }
        index1++;
        
        //Copy xml surrounding selectedCoord
        String copySample = fileIn.substring(index1,index2+pad);
        
        //Substitute new coord into copy
        String tmpSample = copySample.replace(
            bmlLatCoords[selectedCoord].trim(),
            lattmp.trim()).replace(bmlLonCoords[selectedCoord].trim(),
            lontmp.trim());
        String newSample;
        if(del){
            newSample = "";
            selectedCoord--;
        }
        else{
            newSample = "\n"+copySample + "\n" + tmpSample;
            selectedCoord++;
        }
        
        //paste into string after selectedCoord
        String returnSample = fileIn.replace("\n"+copySample,newSample);
        return returnSample;
                
    }// end addPointToFile()
	
    /**
     * Retrieve the schema file name for the given report xml file
     * 
     * @param xmlReportFile		It takes the xml file name as an input
     * @return rootNodeString	schema file name to be used in generation of the xml editor gui at run-time
     */
    public String getSchemaFile(String xmlReportFile) throws Exception {
    	String schemaFileName = "";
    	String schemaFileNameString = "";
    	String rootNodeString = "";
    	String line = "";   	
    	File xmlFile = new File(xmlReportFile);
        Scanner xmlFileScanner = new Scanner(xmlFile);  
        if(debugMode)printDebug("=== Inside the getschema method");
        
        // search for the root element and mapping it to its schema    	
        while (xmlFileScanner.hasNext()){    	
            line = xmlFileScanner.next();
            schemaFileNameString = schemaFileNameString + xmlFileScanner.next();

            if (line.equals("GeneralStatusReport") || line.equals("SpotReport") || line.equals("TrackReport") || 
                            line.equals("NatoReport") || line.equals("BridgeReport") || line.equals("MineFieldReport")){
                rootNodeString = line;
                schemaFileName = line + ".xsd";
                break;
            } else {
                if(debugMode)printDebug("===Couldn't decide schema of the xml Report");
            }  	
        }
 
        if(debugMode)printDebug("===String schemaFileNameString result is = " + schemaFileNameString);
        if(debugMode)printDebug("===String rootNodeString  result is = " + rootNodeString);    
        schemaFileNameString = schemaFileName + ".xsd";           
        return rootNodeString;   	
    }

    /**
     * Validate the C2SIM Document against the Schema
     */
    private void validateBmlDoc() {
            if (currentDom != null) {
                    currentDom.validate();
            }
    }
	
    /**
     * Print the C2SIM Document using the PDF Renderer
     */
    private void printBmlDoc() {
        if (currentDom != null) {
            ByteArrayOutputStream bos = PDFGenerator.getInstance().print(currentDom);
            if (bos != null) {
                try {
                    String tempPDFName = guiFolderLocation + "\\bml.pdf";
                    FileOutputStream fos = new FileOutputStream(tempPDFName);
                    bos.writeTo(fos);
                    fos.close();
                    BrowserControl.displayURL(tempPDFName);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }// end printBmlDoc()
	
    /**
     * Generate the JAVA Swing GUI from the 
     * DOM of the XML source of the C2SIM Document
     */
    void visualizeBmlDom() {
        
        // build JaxFront editor panel
        if (editor != null)centerPanel.remove(editor);
        com.jaxfront.core.type.Type lastSelectedType = null;
        if (editor != null)if(editor.getSelectedTreeNode() != null)
            lastSelectedType = editor.getSelectedTreeNode().getType();
        TypeVisualizerFactory.getInstance().releaseCache(currentDom);	
        editor = new EditorPanel(currentDom.getRootType(), this);

        // configure JaxFront editor panel and insert it
        if (lastSelectedType != null)
            editor.selectNode(lastSelectedType);
        editor.setBorder(null);
        editor.addHelpListener(this);	
        JPanel validationErrorPanel = new JPanel(new BorderLayout());
        validationErrorPanel.setBorder(null);		
        editor.setTargetMessageTable(validationErrorPanel);		
        centerPanel.add(editor, BorderLayout.CENTER);
        
    }// end visualizeBmlDom()

    /**
     * Generate the XML source of the C2SIM Order
     */
    private void serializeBmlDoc() {
            ShowXMLDialog dialog = new ShowXMLDialog(currentDom);
            dialog.prettyPrint();
            Dimension dialogDim = dialog.getSize();
            Dimension thisDim = getSize();
            int x = (thisDim.width - dialogDim.width) / 2;
            int y = (thisDim.height - dialogDim.height) / 2;
            if (getLocation().x > 0 || getLocation().y > 0) {
                    x = x + getLocation().x;
                    y = y + getLocation().y;
            }
            dialog.setLocation(((x > 0) ? x : 0), ((y > 0) ? y : 0));
            dialog.setVisible(true);
            
    }// end serializeBmlDoc()
	
    public void releaseXUICache() {
        XUICache.getInstance().releaseCache();
    }

    /**
     * Open the configuration file for editing
     */
    boolean configHasBeenLoaded = false;
    private void openConfig() {
        
        // setup the JaxFront 
        releaseXUICache();
        documentTypeLabel.setText("C2SIMGUI Configuration");
        xsdUrl = URLHelper.getUserURL(guiLocationXSD);	// Schema File XSD
        xmlUrl = URLHelper.getUserURL(guiLocationXML);	// XML file		
        xuiUrl = URLHelper.getUserURL(guiLocationXUI);	// Jaxfront XUI file
        root = null;
        initDom("default-context", xsdUrl, xmlUrl, xuiUrl, root);
        configHasBeenLoaded = true;
        
        // save config hashcode so we can find out whether config has been edited
        xuiCacheHash = XUICache.getInstance().hashCode();
    }
    
    private void closeConfig() {
        releaseXUICache();
        if (editor != null)centerPanel.remove(editor);
        centerPanel.repaint();	
        
    }
	
    private void saveConfig() { 	
        
        // check whether we have loaded a config to save
        // (otherwise we'll overwrite the config with junk)
        if(!configHasBeenLoaded)
        {
            int answer = JOptionPane.showConfirmDialog(
                    null,  
                    "Overwrite config file? (no config has been loaded)", 
                    "Configfile overwrite warning",
                    JOptionPane.OK_CANCEL_OPTION);
                if (answer != JOptionPane.OK_OPTION) {
                    return;
                }
        }
        
        File configFile = new File(guiLocationXML);
        try {
            currentDom.saveAs(configFile);
            
            // reset cache hash to show file has been saved
            xuiCacheHash = XUICache.getInstance().hashCode();
            loadedXml = getDomXmlLessJaxFront();
            
        } catch (ValidationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        xuiCacheHash = 0;
        
        // advise user to restart
        showInfoPopup( 
            "restart C2SIMGUI to use changed configuration values", 
            "Updated Configuration Data");
    }
	
    private void loadConfig() { 	
        // generate a DOM from the XML Config file and 
        // read the elements into the variables
        if(debugMode)printDebug("GuiFolder Location : " + guiFolderLocation);
        if(debugMode)printDebug("Gui Config Location : " + guiLocationXML);
        File configFile = new File(guiLocationXML);
        w3cDocFactory = DocumentBuilderFactory.newInstance();

        try {
            w3cDocBuilder = w3cDocFactory.newDocumentBuilder();
            w3cReportInfoDoc = w3cDocBuilder.parse(configFile);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            printError("Exception in loadJaxFront loading Config:"+e);
            e.printStackTrace();
            return;
        }

        // read report info using xpath
        xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();
        try {   	
            submitterID = xpath.evaluate("//SubmitterID", w3cReportInfoDoc);
            serverName = xpath.evaluate("//ServerName", w3cReportInfoDoc);
            serverPassword = xpath.evaluate("//ServerPassword", w3cReportInfoDoc);
            C2SIMProtocolVsnMin = xpath.evaluate("//C2SIMProtocolVsnMin", w3cReportInfoDoc);
            C2SIMProtocolVsnMax = xpath.evaluate("//C2SIMProtocolVsnMax", w3cReportInfoDoc);
            C2SIMProtocolVersionSend = xpath.evaluate("//C2SIMProtocolVsnSend", w3cReportInfoDoc);
            if(c2simProtocolVersion.equals(""))c2simProtocolVersion = defaultC2simProtocolVersion;
            mapScale = xpath.evaluate("//MapScale",w3cReportInfoDoc);
            startSubscribed = xpath.evaluate("//StartSubscribed",w3cReportInfoDoc);
            centerUnit = xpath.evaluate("//CenterUnit",w3cReportInfoDoc).trim();
            c2simns = xpath.evaluate("/SBMLServer//C2SIMns",w3cReportInfoDoc);
            cbmlns = xpath.evaluate("/SBMLServer//CBMLns",w3cReportInfoDoc);
            ibmlns = xpath.evaluate("/SBMLServer//IBMLns",w3cReportInfoDoc);
            lateJoinerMode = xpath.evaluate("//LateJoinerMode", w3cReportInfoDoc);
            initMapLat = xpath.evaluate("//InitMapLat", w3cReportInfoDoc);
            initMapLon = xpath.evaluate("//InitMapLon", w3cReportInfoDoc);
            reportOrderScale = xpath.evaluate("//ReportOrderScale", w3cReportInfoDoc);
            autoDisplayInit = xpath.evaluate("//AutoDisplayInit", w3cReportInfoDoc);
            autoDisplayReports = xpath.evaluate("//AutoDisplayReports", w3cReportInfoDoc);
            autoDisplayOrders = xpath.evaluate("//AutoDisplayOrders", w3cReportInfoDoc);
            saveAutoDisplayOrders = autoDisplayOrders;
            displayBluePosition = xpath.evaluate("//DisplayBluePosition", w3cReportInfoDoc);
            displayBlueObservation = xpath.evaluate("//DisplayBlueObservation", w3cReportInfoDoc);
            displayRedPosition  = xpath.evaluate("//DisplayRedPosition", w3cReportInfoDoc);
            displayRedObservation  = xpath.evaluate("//DisplayRedObservation", w3cReportInfoDoc);
            displayOtherPosition = xpath.evaluate("//DisplayOtherPosition", w3cReportInfoDoc);
            displayOtherObservation = xpath.evaluate("//DisplayOtherObservation", w3cReportInfoDoc);
            startWithListeningOn = xpath.evaluate("//StartWithListeningOn", w3cReportInfoDoc);
            recordingProtocol = xpath.evaluate("//RecordingProtocol", w3cReportInfoDoc);
            playbackProtocol = xpath.evaluate("//PlaybackProtocol", w3cReportInfoDoc);
            playbackSubmitter = xpath.evaluate("//PlaybackSubmitter", w3cReportInfoDoc);
            String playTimescale = xpath.evaluate("//PlaybackTimescale", w3cReportInfoDoc);
            if(playTimescale.length() > 0)
                playbackTimescale = Integer.parseInt(playTimescale);
            else playbackTimescale = 1;// speedup factor
            String playTimelimit = xpath.evaluate("//PlaybackTimelimit", w3cReportInfoDoc);
            if(playTimelimit.length() > 0)
                playbackTimelimit = Integer.parseInt(playTimelimit);
            else playbackTimelimit = 100000;// 100 sec
            warnOnReportSeq = xpath.evaluate("//WarnOnReportSeq", w3cReportInfoDoc);
            orderIDXPath=xpath.evaluate("//OrderIDXPATH",w3cReportInfoDoc);
            whereXPathTag=xpath.evaluate("//LocationXPATH",w3cReportInfoDoc);
            routeXPathTag=xpath.evaluate("//LocationRouteWhereXPATH", w3cReportInfoDoc);	
            latlonParentTag=xpath.evaluate("//latlonParentTag", w3cReportInfoDoc);
            latTag=xpath.evaluate("//latTag", w3cReportInfoDoc);
            lonTag=xpath.evaluate("//lonTag", w3cReportInfoDoc);
            whereIdLabelTag=xpath.evaluate("//whereIdLabelTag", w3cReportInfoDoc);
            whereShapeTypeTag=xpath.evaluate("//whereShapeTypeTag", w3cReportInfoDoc);
            routeIdLabelTag=xpath.evaluate("//routeIdLabelTag", w3cReportInfoDoc);
            routeFromViaToTag=xpath.evaluate("//routeFromViaToTag", w3cReportInfoDoc);
            String stompWait = xpath.evaluate("//ServerTestStompWait", w3cReportInfoDoc);
            if(stompWait.length() > 0)
                serverTestStompWait = Integer.parseInt(stompWait);
            else serverTestStompWait = 10000; // 10 sec
            blueSideName = xpath.evaluate("//blueSideName", w3cReportInfoDoc);
            checkForDuplicateID = xpath.evaluate("//checkForDuplicateID", w3cReportInfoDoc);
            ignoreXmlWithDupID = !checkForDuplicateID.equalsIgnoreCase("1");
            askAboutCheckDuplicateID  = xpath.evaluate("//askAboutCheckDuplicateID", 
                w3cReportInfoDoc);
            askAboutCheckingID = askAboutCheckDuplicateID.equalsIgnoreCase("1");
            reportRepeatms=xpath.evaluate("//ReportRepeatms", w3cReportInfoDoc);

            // if debugging, print the parameters
            if(debugMode){
                printDebug(" submitterID =" + submitterID);
                printDebug(" serverName =" + serverName);
                printDebug(" serverPassword =" + serverPassword);
                printDebug(" C2SIM protocol min version =" + C2SIMProtocolVsnMin);
                printDebug(" C2SIM protocol max version =" + C2SIMProtocolVsnMax);
                printDebug(" C2SIM protocol send version =" + C2SIMProtocolVersionSend);
                printDebug(" Report BML Type =" + reportBMLType);
                printDebug(" Order BML Type =" + orderBMLType);
                printDebug(" MapScale =" + mapScale);
                printDebug(" StartSubscribed = " + startSubscribed);
                printDebug(" CBML namespace =" + cbmlns);
                printDebug(" IBML namespace =" + ibmlns);
                printDebug(" C2SIM namespace =" + c2simns);
                printDebug(" lateJoinerMode =" + lateJoinerMode);
                printDebug(" whereXPath =" + whereXPathTag);
                printDebug(" initMapLat =" + initMapLat);
                printDebug(" initMapLon =" + initMapLon);
                printDebug(" reportOrderScale =" + reportOrderScale);
                printDebug(" autoDisplayInit =" + autoDisplayInit);
                printDebug(" autoDisplayReportsXX =" + autoDisplayReports);
                printDebug(" autoDisplayOrders =" + autoDisplayOrders);
                printDebug(" displayBluePosition =" + displayBluePosition);
                printDebug(" displayBlueObservation =" + displayBlueObservation);
                printDebug(" displayRedPosition =" + displayRedPosition);
                printDebug(" displayRedObservation =" + displayRedObservation);
                printDebug(" displayOtherPosition =" + displayOtherPosition);
                printDebug(" displayOtherObservation =" + displayOtherObservation);
                printDebug(" startWithListeningOn =" + startWithListeningOn);
                printDebug(" recordingProtocol =" + recordingProtocol);
                printDebug(" playbackProtocol =" + playbackProtocol);
                printDebug(" playbackSubmitter =" + playbackSubmitter);
                printDebug(" playbackTimescale =" + playbackTimescale);
                printDebug(" playbackTimelimit =" +  playbackTimelimit);
                printDebug(" SchemaLocation =" + schemaFolderLocation);
                printDebug(" cbmlOrderSchemaLocation =" + cbmlOrderSchemaLocation);
                printDebug(" cbmlReportSchemaLocation =" + cbmlReportSchemaLocation);
                printDebug(" ibml09OrderSchemaLocation =" + ibml09OrderSchemaLocation);
                printDebug(" ibml09ReportSchemaLocation =" + ibml09ReportSchemaLocation);
                printDebug(" c2simOrderSchemaLocation =" + c2simReportSchemaLocation);
                printDebug(" c2simReportSchemaLocation =" + c2simReportSchemaLocation);
                printDebug(" c2simInitSchemaLocation =" + c2simReportSchemaLocation);
                printDebug(" Order ID path =" + orderIDXPath);
                printDebug(" Locationpath =" + whereXPathTag);
                printDebug(" LocationRouteWhereXPATH ="+routeXPathTag);
                printDebug(" xuiFolderLocation ="+xuiFolderLocation);
                printDebug(" warnOnReportSeq ="+warnOnReportSeq);
                printDebug(" latlonParentTag =" + latlonParentTag);
                printDebug(" latTag =" + latTag);
                printDebug(" lonTag =" + lonTag);
                printDebug(" whereIdLabelTag =" + whereIdLabelTag);
                printDebug(" whereShapeTypeTag =" + whereShapeTypeTag);
                printDebug(" routeIdLabelTag ="+ routeIdLabelTag);
                printDebug(" routeFromViaToTag =" + routeFromViaToTag);
                printDebug(" blueSideName =" + blueSideName);
                printDebug(" checkForDuplicateID =" + checkForDuplicateID);
                printDebug(" askAboutCheckDuplicateID =" + askAboutCheckDuplicateID);
            }// end if(debugMode)
           
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
        }	
    } // end loadConfig()
    
	
    private void printDOM() {
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            DOMSource source = new DOMSource(w3cBmlDom);
            StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }// end printDOM()

    /**
     * Maps raw reportString's to discrete reportType's
     */
    public String getReportDocumentType(String reportString){
        String reportType = "UNKNOWN";
        if (reportString.contains("BRIDGEREP")){
                reportType = "BRIDGEREP";
        }else if (reportString.contains("MINOBREP")){
                reportType = "MINOBREP";
        }else if (reportString.contains("NATOSPOTREP")){
                reportType = "NATOSPOTREP";
        }else if (reportString.contains("SPOTREP")){
                reportType = "SPOTREP";
        }else if (reportString.contains("TRKREP")){
                reportType = "TRKREP";
        }else if (reportString.contains("GeneralStatusReport")){
                reportType = "GeneralStatusReport";
        }else if (reportString.contains("PositionStatusReport")){
                reportType = "PositionStatusReport";
        }	
        return reportType;
        
    }// end getReportDocumentType()

    /**
     * A wrapper around a commonly used DOMBuilder, 
     * which uses the params as-is.  Sets currentDom.
     */
    public void initDom(
        String context, 
        URL url1, 
        URL url2, 
        URL url3, 
        String root){

        // check for changes in the XUI
        if(xuiCacheHash != 0){
            compareXml = getDomXmlLessJaxFront();
            if(loadedXml.compareTo(compareXml) != 0 && !pushingJaxFront) {
                int answer = JOptionPane.showConfirmDialog(
                    null,  
                    "About to overwrite changed data in form - load form anyhow?",
                    "Warning",
                    JOptionPane.OK_CANCEL_OPTION);
                if (answer != JOptionPane.OK_OPTION) {
                    return;
                }
            }
        }
        releaseXUICache();

        // build the DOM
        try {
            currentDom = 
                DOMBuilder.getInstance().build(context, url1, url2, url3, root);
            currentDom.getGlobalDefinition().setIsUsingButtonBar(false);
            currentDom.getGlobalDefinition().setIsUsingStatusBar(true);
            currentDom.getGlobalDefinition().setLanguage(currentLanguage);
            if (editor != null)editor.selectNode((com.jaxfront.core.type.Type) null);
            visualizeBmlDom();
        } catch (Exception ex) {
            ex.printStackTrace();	
        }
        
        // save XML and its hash as loaded
        xuiCacheHash = XUICache.getInstance().hashCode();
        loadedXml = getDomXmlLessJaxFront();

    }// end initDOM()
    
    /**
     * Pulls XML from JaxFront XUI and deletes the header
     */
    String getDomXmlLessJaxFront(){
        
        // get XML from the DOM
        if(currentDom == null)return null;
        String extractXml;
        try{
            extractXml = currentDom.serialize().toString();
        }
        catch(ValidationException ve){
            showErrorPopup(
                "Failed to extract XML from JaxFront panel",
                "Internal Error ");
            ve.printStackTrace();
            return "";
        }

        // remove the XML and JaxFront headers - each ends with '>'
        int headerEnd = extractXml.indexOf('>');
        if(headerEnd < 0)return"";
        extractXml = extractXml.substring(headerEnd+1);
        if(headerEnd < 0)return"";
        headerEnd = extractXml.indexOf('>');
        return extractXml.substring(headerEnd+1);
        
    }// end getXMlLesJaxFront()
    
    /**
     * within one of the three language variant comparisons
     * (with or without a namespace prefix)
     * returns true if parms are equal under the variant
     */
   boolean variantTagCompare(
        Variant variant, // IBML, CBML, or C2SIM
        String parm1,
        String parm2)
   {
       switch(variant){
           case IBML:
               return ibmlTagCompare(parm1, parm2);
               
           case CBML:
               return cbmlTagCompare(parm1, parm2);
               
           case C2SIM:
               return c2simTagCompare(parm1, parm2);
               
           default:
               return false;
       }
   }// end compareVariant()
    
    /**
     * checks XML elements for uniqueness of Task and Report IDs
     * returns true if not unique in this execution of C2SIM GUI
     * or parsing error encountered; otherwise false
     * 
     * also pops up a warning if ID is not unique unless
     * this is server copy of locally entered item
     * 
     * there can be multiple items in the array, e.g. Tasks; we
     * we search each separately for duplicate IDs
     */
    synchronized boolean itemIdNotUnique(
        String[] currentXmlDataArray,
        Variant variant,// C2SIM variant: CBML, IBML, C2SIM
        String itemTag, // tag to start pattern and end to scan
        String idTag,   // tag for data that must be unique
        boolean fromThisGui)// checking from this GUI or server
    {
        if(currentXmlDataArray == null) {
            printError("XML DATA ARRAY NULL IN itemIdNotUnique");
            Thread.dumpStack();
            return false;
        }
        
        // don't do this if not configured
        if(debugMode){
            printDebug("ItemIdNotUnique ignoreXmlWithDupID:" +
                ignoreXmlWithDupID + " fromThisGui:" + fromThisGui);
            printDebug("variant:" + variant + " itemTag:" + itemTag +
                " xmlArray[0]:" + currentXmlDataArray[0]);
        }
        if(ignoreXmlWithDupID && !fromThisGui)return false;
        if(currentXmlDataArray == null)return true;
            
        // disable this during ServerValidation
        if(runningServerTest)return false;

        // scan the data looking for reports
        boolean foundAnItem = false;
        String lastIdFound = "";
        int arrayEnd = currentXmlDataArray.length;
        for(int xmlIndex=0; xmlIndex < arrayEnd; ++xmlIndex){
            if(debugMode)printDebug("xmlIndex:"+xmlIndex+
                " "+currentXmlDataArray[xmlIndex]);
            if(foundAnItem){    
                // we have found the Item tag we seek, or
                // and the end of the currentXmlDataArray
                // now look for the tag with ID value
                if(variantTagCompare(variant,currentXmlDataArray[xmlIndex],idTag)){

                    // the ID we want is in next array position
                    // confirm we are not at array end
                    if((xmlIndex+1) >= arrayEnd){
                        printError("XML file ends prematurely");
                        return true;
                    }
                    lastIdFound = currentXmlDataArray[xmlIndex+1];
                }

                // there can be more than one of the idTag we seek under
                // the item; we want the last one so take values up to the
                // last instance until the next Item or end of array is found
                else {
                    if(variantTagCompare(variant,currentXmlDataArray[xmlIndex],itemTag) 
                        || (xmlIndex+1) >= arrayEnd){

                        // confirm an ID was found for this Item            
                        if(lastIdFound.equals("")){
                            printError("comparing " + variant + " found no " + idTag + 
                                " for at least one " + itemTag + " in the XML document");
                            return true;
                        }
                        // we found next Item or end of array
                        // check that the last ID is not already in HashMap
                        IdPosted layerIdPosted = layerGetItemId(lastIdFound);
                        if(debugMode){
                            if(layerIdPosted != null)printDebug(
                            "itemIdNotUnique lastIdFound:" + lastIdFound +      
                            " layerIdPosted.id:" + layerIdPosted.id +
                            " layerIdPosted.sentFrom:" + layerIdPosted.sentFromThisGui +
                            " from this Gui:" + fromThisGui);
                        }
                        if(layerIdPosted != null){// null means not seen before
                            //found a duplicate
                            // ask the user whether to suppress duplicates
                            boolean okCancelFromThis = false;
                            if((askAboutCheckingID || fromThisGui) && previousUnitMap == null){
                                // sent here - check every time if configured to check
                                if(layerIdPosted.sentFromThisGui && fromThisGui){
                                    okCancelFromThis = okCancelPopup(
                                        "Ignore duplicate ID?",
                                        "about to post to map Order or Report with duplicate ID - " +
                                        "post with duplicate ID?\n" +
                                        "(click OK to post)"
                                    ); 
                                    if(!okCancelFromThis)return true;
                                }// end layerIdSent.sentFromThisGui)
                                else 
                                {   // received from server - ask only once
                                    if(layerIdPosted.sentFromThisGui)return true;
                                    ignoreXmlWithDupID =
                                        okCancelPopup(
                                            "Ignore duplicate ID?", 
                                            "Received duplicate Order or Report ID - ignore?\n" +
                                             "(click OK to ignore dups, cancel to show them\n" +
                                             "for remainder of this C2SIMGUI run)\n" +
                                             "(this can happen when server sends multiple formats)");
                                    askAboutCheckingID = false;
                                }// end else/if(fromThisGui)
                            }// end if(askAboutCheckingID)

                            if(ignoreXmlWithDupID && !okCancelFromThis){
                                printError("duplicate item " + idTag + " in " + itemTag + 
                                    " for UUID " + lastIdFound + " - skipping incoming XML document");
                                return true;
                            }
                        }// end if(layerIdSent != null)

                        // add the ID and start looking again
                        layerAddItemId(lastIdFound, itemTag, fromThisGui);                
                    }// end if(variantTagCompare(...
                }// end else/if(currentXmlDataArray...
            }// end if(foundAnItem)

            // look for the Item tag we're checking
            if(variantTagCompare(variant,currentXmlDataArray[xmlIndex],itemTag)){
                foundAnItem = true;        
                lastIdFound = "";
            }
        }// end for(int xmlIndex
        
        if(!foundAnItem){
            printError("found no " + itemTag + " in the XML document");
            try{int i=5/0;}catch(Exception e){e.printStackTrace();}
            return true;
        }
        
        // return success code
        return false; 
        
    }// end itemIdNotUnique()
	
    /**
     * Draws from currentDom; first calls initDom to set currentDom
     * returns true if XML is good and has a unique UUID for each
     * Task in Orders and each Report
     * 
     * returns false if itemIdNotUnique detects receiving or trying to
     * push duplicate Order or Report ID and user clicks cancel;
     * otherwise returns true
     */
    boolean initializationHasBeenAsked = false;
    public boolean drawFromXML(
        String context, 
        URL url1, 
        URL url2, 
        URL url3, 
        String root, 
        String xmlDocumentType,
        String whereNodeTag,
        String[] whereSingleTags,
        String[] whereGroupSubTags,
        String nsPrefix,
        String xmlString,
        String protocolVersion,
        boolean isFromThisGui){	
        //if(debugMode)printDebug("drawFromXML context:"+context);
        //if(debugMode)printDebug("drawFromXML url1:"+url1);
        //if(debugMode)printDebug("drawFromXML url2:"+url2);
        //if(debugMode)printDebug("drawFromXML url3:"+url3);
        //if(debugMode)printDebug("drawFromXML root:"+root);
        //if(debugMode)printDebug("drawFromXML xmlDocumentType:"+bxmlDocumentType);
        //if(debugMode)printDebug("drawFromXML whereNodeTag:"+whereNodeTag);
        //if(debugMode)printDebug("drawFromXML whereSingletags[0]:"+whereSingleTags[0]);
        //if(debugMode)printDebug("drawFroMXML whereGroupSubtags[0]:"+whereGroupSubTags[0]);
        //if(debugMode)printDebug("drawFroMXML nsPrefix:"+nsPrefix);
        //if(debugMode)printDebug("drawFroMXML xmlString:"+xmlString.substring(0,100));
        //if(debugMode)printDebug("drawFroMXML protocolVersion:"+protocolVersion);
        //if(debugMode)printDebug("drawFroMXML isFromThisGui:"+isFromThisGui);
        
        // XML input parsed according to document type
        String[] currentXmlDataArray = null;
        
        // if the xmlString is empty read the file into it
        String filename = null;
        if(url2 != null) {
            filename = url2.getFile();
            if(platformIsWindows && filename.charAt(0) == '/')
                // hack to clean up unexpected /
                filename = filename.substring(1);
        }
        if(xmlString == null)
            xmlString = readAnXmlFile(filename);
        else if(xmlString.length() == 0)
          xmlString = readAnXmlFile(filename);
        if(debugMode)printDebug("FILENAME:" + filename);
        if(debugMode)printDebug("NS PREFIX:" + nsPrefix);
       // if(debugMode)printDebug("XML:" + xmlString);
        
        // if there is no initialization data, ask 
        // user if they want to load initialization
        if(getUnitMapSize() == 0){
            
            if(!initializationHasBeenAsked){
                
                initializationHasBeenAsked = true;
                if(okCancelPopup("No initialization loaded", "Load initialization?")){
                
                    // load initialization file and parse it
                    InitC2SIM initC2SIM = new InitC2SIM();
                    if(initC2SIM.openInitFSC2SIM("Initialize")){
                        MilOrg milOrg = new MilOrg();
                        milOrg.parseC2SIMInit(loadedXml);
                    }
                }
            }
        }

        // get data for geolocations from XML file
        currentXmlDataArray = null;
        String elementZero = null;
        if(whereNodeTag == null || 
            whereSingleTags == null || 
            whereGroupSubTags == null) {
            printError( "insufficient tag data to parse file " + filename);
            return false;
        }
        XmlParse xrp = new XmlParse(xmlString);
        String nsPrefixRef = nsPrefix;
        
        // deal with prefix missing or not used in the XML
        if(nsPrefixRef == null)nsPrefixRef = new String("");
        else if(!xmlString.contains(nsPrefix))nsPrefixRef = new String("");

        // parse for specific data patterns sought
        if(xmlDocumentType.equals("IBML09 Order")) {
            currentXmlDataArray = 
                xrp.getElementNameDataByTagName(
                    whereNodeTag,
                    whereSingleTags,
                    whereGroupSubTags,
                    nsPrefixRef);

            String[] airTaskXmlDataArray = 
                xrp.getElementNameDataByTagName(
                "AirTask",
                whereSingleTags,
                whereGroupSubTags,
                nsPrefixRef);

            // check if file was parsable 
            // for at least one of these
            if(currentXmlDataArray == null &&
                airTaskXmlDataArray == null) {
                    printError("IBML09 XML file cannot be parsed completely:" + 
                        filename);
                    //showErrorPopup( 
                    //    "file cannot be parsed completely",
                    //    JOptionPane"XML File Error ");      
                return false;
            }
 
            // combine the two arrays
            if(currentXmlDataArray == null){
                  currentXmlDataArray = airTaskXmlDataArray;
            } else {
                if(airTaskXmlDataArray != null) {
                  int totalArrayLength = 
                    currentXmlDataArray.length + airTaskXmlDataArray.length;
                  String[] combinedArray = new String[totalArrayLength];
                  System.arraycopy(
                    currentXmlDataArray, 0,
                    combinedArray, 0,
                    currentXmlDataArray.length);
                  System.arraycopy(
                    airTaskXmlDataArray, 0,
                    combinedArray, currentXmlDataArray.length,
                    airTaskXmlDataArray.length);
                  currentXmlDataArray = combinedArray;
                }
            }// end if(currentXmlDataArray == null){

            
            // check for TaskID uniqueness
            if(itemIdNotUnique(
                currentXmlDataArray, 
                Variant.IBML, "GroundTask", "WhereID",
                isFromThisGui)
            ) return false;
            
        } // end if(bmlDocumentType.equals("IBML09 Order"))
        
        else if(xmlDocumentType.equals("CBML Light Order")) {
            currentXmlDataArray = 
                xrp.getElementNameDataByTagName(
                    whereNodeTag,
                    whereSingleTags,
                    whereGroupSubTags,
                    nsPrefixRef);

            // check if file was parsable
            if(currentXmlDataArray == null) {
                if(debugMode)printDebug("CBML XML file cannot be parsed completely:" + filename);
                //showErrorPopup( 
                //    "file cannot be parsed completely",
                //    "XML File Error ");
                return false;
            }
            
            // check for TaskID uniqueness
            if(itemIdNotUnique(
                currentXmlDataArray, 
                Variant.CBML, "Task", "TaskID",
                isFromThisGui)
            ) return false;
            
        }// end else if{xmlDocumentType.equals("CBML Light Order")
        
        else if(xmlDocumentType.equals("C2SIM Order")){
            currentXmlDataArray = 
                xrp.getElementNameDataByTagName(
                    whereNodeTag,
                    whereSingleTags,
                    whereGroupSubTags,
                    nsPrefixRef);

            // check if file was parsable
            if(currentXmlDataArray == null) {
                printError("C2SIM XML file cannot be parsed completely:" + filename);
                return false;
            }
            
            // check for TaskID uniqueness
            if(debugMode)printDebug("drawFromXML/C2SIM Order whereNodetag:" +
                whereNodeTag);
            if(itemIdNotUnique(
                currentXmlDataArray, 
                Variant.C2SIM, whereNodeTag,"UUID",
                isFromThisGui)
            ) return false;
            
        }// end else if{xmlDocumentType.equals("C2SIM Order")
        
        else if (reportBMLType.equals("IBML") &&
            xmlDocumentType.equals("IBML09 Report")){
                currentXmlDataArray = 
                    xrp.getElementNameDataByTagName(
                        whereNodeTag,
                        whereSingleTags,
                        whereGroupSubTags,
                        nsPrefixRef);
                
                // check for ReportID uniqueness
                if(itemIdNotUnique(
                    currentXmlDataArray, 
                    Variant.IBML, "GeneralStatusReport", "ReportID",
                    isFromThisGui)
                ) return false;
        }// end else if (reportBMLType.equals("IBML") &&
        
        else if (reportBMLType.equals("CBML") &&
            xmlDocumentType.equals("CBML Light Report")){
                currentXmlDataArray = 
                    xrp.getElementNameDataByTagName(
                        whereNodeTag,
                        whereSingleTags,
                        whereGroupSubTags,
                        nsPrefixRef);
        
                // check for ReportID uniqueness
                if(itemIdNotUnique(
                    currentXmlDataArray, 
                    Variant.CBML, "GeneralStatusReport", "OID",
                    isFromThisGui)
                ) return false;
        }// end  else if (reportBMLType.equals("CBML") &&
        
        else if (reportBMLType.equals("C2SIM") &&
            xmlDocumentType.equals("C2SIM Report")){
                currentXmlDataArray = 
                    xrp.getElementNameDataByTagName(
                        whereNodeTag,
                        whereSingleTags,
                        whereGroupSubTags,
                        nsPrefixRef);

            // check for ReportID uniqueness (exempt older C2SIM)
            if(bml.c2simProtocolOK(protocolVersion)){
                if(debugMode)printDebug("drawFromXML/C2SIM Report whereNodetag:" +
                    whereNodeTag + " itemTag:MessageBody idTag:ReportID");
                if(itemIdNotUnique(
                    currentXmlDataArray, 
                    Variant.C2SIM, "MessageBody", "ReportID",
                    isFromThisGui)
                ) return false;
            }
        } // end else if (reportBMLType.equals("C2SIM") &&
        
        else {
            if(debugMode)printDebug(
                "xmlDocumentType is not an option we parse:" + xmlDocumentType);
            return false;
        }
            
        if(currentXmlDataArray == null) {
            printError("NO XML DATA WAS EXTRACTED IN DrawFromXml");
            return false;
        }        
        if(currentXmlDataArray.length == 0) {
            printError("NO XML DATA WAS EXTRACTED IN DrawFromXml");
            return false;
        }

        // verify first XML tag matches a xmlDocumentType
        elementZero = new String(currentXmlDataArray[0]);
        if(xmlDocumentType.equals("C2SIM Report") && 
            !cbmlTagCompare(elementZero,"MessageBody"))
        if(xmlDocumentType.equals("IBML09 Report") && 
            !ibmlTagCompare(elementZero,"GeneralStatusReport"))
        if(xmlDocumentType.equals("CBML Light Report") && 
            !cbmlTagCompare(elementZero,"GeneralStatusReport"))
        if(xmlDocumentType.equals("CBML Light Order") && 
            !cbmlTagCompare(elementZero,"Task"))
        if(xmlDocumentType.equals("C2SIM Order") && 
            !cbmlTagCompare(elementZero,"Task"))
        if(xmlDocumentType.equals("IBML09 Order") &&
            !ibmlTagCompare(elementZero,"GroundTask")) {
            showErrorPopup( 
                "file doesn't match selected type ",
                "XML File Error ");
            return false;
        }
      
        // set document function and dialect
        if(xmlDocumentType.equals("CBML Light Order")) {
          generalBMLFunction = "Order";
          generalBMLType = "CBML";
        }
        else if(xmlDocumentType.equals("IBML09 Order")) {
          generalBMLFunction = "Order";
          generalBMLType = "IBML"; 
        }
        else if(xmlDocumentType.equals("C2SIM Order")) {
          generalBMLFunction = "Order";
          generalBMLType = "C2SIM"; 
        }
        else if(xmlDocumentType.equals("IBML09 Report")) {
          generalBMLFunction = "Report";
          generalBMLType = reportBMLType;
        }
        else if(xmlDocumentType.equals("C2SIM Report")) { 
          generalBMLFunction = "Report";
          generalBMLType = "C2SIM";
        }
        else if(xmlDocumentType.equals("CBML Light Report")) { 
          generalBMLFunction = "Report";
          generalBMLType = "CBML";
        }

        // parse Control Measures from IBML09 Order
        String[] currentXmlControlMeasuresArray = new String[0];
        if(xmlDocumentType.equals("IBML09 Order")) {
            currentXmlControlMeasuresArray = 
                xrp.getElementNameDataByTagName(
                    "ControlMeasure",
                    new String[]{
                      "WhereID",
                      "WhereClass",
                      "WhereCategory",
                      "WhereLabel",
                      "WhereQualifier"},
                    new String[]{
                      "Latitude",
                      "Longitude"},
                    ibmlns);
              if(currentXmlControlMeasuresArray == null){
                    showErrorPopup( 
                    "IBML Order file ControlMeasures cannot be parsed ",
                    "XML File Error ");
                    currentXmlControlMeasuresArray = new String[0];
              }
        }// end if IBML09 Order

        // parse Control Measures from CBML Light Order
        else if(xmlDocumentType.equals("CBML Light Order")) {
            currentXmlControlMeasuresArray = 
                xrp.getElementNameDataByTagName(
                    "ControlMeasure",
                    new String[]{
                        "AtWhere",
                        "RouteWhere",
                        "OID",
                        "PointLight",
                        "Line",
                        "Surface",
                        "CorridorArea"},
                    new String[]{
                        "Latitude",
                        "Longitude"},
                    cbmlns);
            if(currentXmlControlMeasuresArray == null) {
                showErrorPopup( 
                "CBML Light Order file ControlMeasures cannot be parsed ",
                "XML File Error ");
                currentXmlControlMeasuresArray = new String[0];
                
            }// end ifcurrentXmlControlMeasuresArray...
        }// end if(xmlDocumentType...

        // append any Control Measures in Order to end of currentXmlDataArray  
        int currentLength = currentXmlDataArray.length;
        String[] holdXmlDataArray = 
            new String[currentLength + currentXmlControlMeasuresArray.length];
        System.arraycopy(
            currentXmlDataArray, 
            0, 
            holdXmlDataArray, 
            0,
            currentLength);
        System.arraycopy(
            currentXmlControlMeasuresArray, 
            0, 
            holdXmlDataArray, 
            currentLength, 
            currentXmlControlMeasuresArray.length);
        currentXmlDataArray = holdXmlDataArray;
        if(debugMode)printDebug("************ XML DATA:\nroot:" + root);
        for(
            int index = 0; 
            index < currentXmlDataArray.length; 
            ++ index) {
            if(debugMode)printDebug(index+" "+currentXmlDataArray[index]);
        }
        if(debugMode)printDebug("************ END XML DATA ARRAY LENGTH:"+
            currentXmlDataArray.length);

        // package array for drawing
        if(currentXmlDataArray != null)
            bmlStringArray = currentXmlDataArray;
        else 
        {
            // use original DOM approach to extracting data
            initDom(context, url1, url2, url3, root);
            try {
                // reading the whole DOM values of the XML file into a csv string
                bmlString = currentDom.getRootType().getDisplayValue();
                if(debugMode)printDebug("XML Document String : "+ bmlString);

                // convert the csv string to an array of strings
                bmlStringArray = bmlString.split(",");
                for (int i=0; i < bmlStringArray.length;i++){
                    if(debugMode)printDebug("XML Document String [ "+  i  + 
                        " ] = |" + bmlStringArray[i] + "|" );
                }
            } catch (Exception ex) {
                showErrorPopup( 
                    "XML Error ", 
                    "Couldn't Create Document ");
                ex.printStackTrace();
            } // end catch
        }// end else
        
        // draw the resulting graphics on map
        try {
            // call method that starts the drawing 
            // process for the location information
            drawLocation(
                bmlStringArray, 
                xmlDocumentType, 
                xmlString,
                -1, -1, null);
            catalogPoints(bmlStringArray);
        } catch (Exception ex) {
            printError( 
                "Error drawing tactical graphics - Couldn't draw document");
            printError("Error due to XML:"+xmlString);
            ex.printStackTrace();
            
            // if this came from playback user can't stop it
            stopPlayback();
        } // end try/catch
        
        return true;
    
    }// end drawFromXML()
    
    /**
     * displays popup and prints error message 
     */
    public void showErrorPopup(String frameMessage, String errorMessage){
        
        printError(frameMessage+"\n"+errorMessage);
        if(!runningServerTest)
            JOptionPane.showMessageDialog(
                bml, 
                frameMessage, 
                errorMessage,
                JOptionPane.ERROR_MESSAGE);
        
    }// end showErrorPopup()
    
    /**
    *   collects on Magic Move input, in thread so processing continues
    */
    private class magicMoveInput extends Thread{
        public void run() {
            // get UUID of  object
            String uuid = inputTimeMultPopup(
                "Enter UUID to move (or 0 to get UUID by clicking icon");
            if(uuid.equals("0")){
                // wait for icon lat/lon from map click
                gettingCoords = true;
                try{
                    while(gettingCoords)
                        Thread.sleep(100);
                }catch(InterruptedException ie){
                    clickLatitude = 1000;
                    clickLongitude = 1000;
                    gettingCoords = false;
                }
            }
            if(clickLatitude < 999 && clickLongitude < 999){
                uuid = mapGetClosestIconUuid(clickLatitude,clickLongitude);
                if(uuid.equals("0")){
                    showInfoPopup("no UUID found - try again", "Magic Move");
                } else {
                    // tell user to click on coords
                    showInfoPopup("click at new coords", "Magic Move");

                    // wait for lat/lon from map click
                    gettingCoords = true;
                    try{
                        while(gettingCoords)
                        Thread.sleep(100);
                    }catch(InterruptedException ie){
                        clickLatitude = 1000;
                        clickLongitude = 1000;
                        gettingCoords = false;
                    }
                }                         
                // setCoordinates(float lat, float lon)
                if(clickLatitude < 999 && clickLongitude < 999){
                    String lat = floatTo3SD(clickLatitude);
                    String lon = floatTo3SD(clickLongitude);

                    // send input to server
                    InitC2SIM initC2SIM = new InitC2SIM();
                    initC2SIM.pushC2simServerInput("MAGIC",uuid,lat,lon);
                }
            }
        }// end run()
    } // end class magicMoveInput
    
    /**
     * displays popup and prints info message 
     */
    public void showInfoPopup(String frameMessage, String infoMessage){

        if(debugMode)printDebug("Dialog:"+frameMessage+"\n"+infoMessage);
        
        // display the dialog
        if(!runningServerTest)
        JOptionPane.showMessageDialog(
            bml, 
            frameMessage, 
            infoMessage,
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * clears layers of map with Initialize icons
     */
    void removeInitIcons(){
        initIconsOnScreen = false;
        removeLayersWithIconType(IconType.INITIALIZE);
        iconUuidListClearInit();
        lastUnitID = "";
        lastLatitude = "";
        lastLongitude = "";
    }
    
    /**
     * extract data from an XML string by tag fragment
     * that start immediately after < or .
     * this obtains only the first data with that tag
     * and assumes the XML data does not otherwise contain
     * the tag up to the point where the data is found
     * @param xml - string to be extracted fom
     * @param tag - the tag fragment to be matched
     * @returns the data, or "" if tag is not matched
     */
    String extractDataFromXml(String xml, String tag){
        int startID = xml.indexOf(tag + ">");
        if(startID < 0) return "";
        startID += tag.length();
        int endID = xml.indexOf("</",startID);
        if(endID < 0)return "";
        return xml.substring(startID,endID);
    }
    
   /**
    *  start action, to be completed in RouteLayer, to draw
    *  the Map Graphics corresponding to an OPORD on the OpenMap
    */
	
    public void drawOPORD_FS(URL url1, URL url2, URL url3, String root){
        initDom(null, url1, url2, url3, root);
        try {	
            // reading the whole DOM values of the XML file into a csv string
            bmlString = currentDom.getRootType().getDisplayValue();

            // convert the csv string to an array of strings
            bmlStringArray = bmlString.split(",");
            catalogPoints(bmlStringArray);
            mapGraph = true;

            for (int i=0; i < bmlStringArray.length;i++){
                if(debugMode)printDebug("Order String [ "+  i  + " ] = " + bmlStringArray[i] );
            }

            // call method that starts the drawing process of the location information
            /*Option 1 : direct analysis of the string array of the Document
             *			 which uses RouteLayer.createC2CoreOPORDGraphics method
             *			 see method for details
             */			

            /*Option 2 : use of Xpath to find location information
             * Note : -didn't work for IBML_CBML because of jaxfront name space problems
             *		  -unknown if it is going to work with C2Core.
             * 		  -to use this option disable the drawLocation above and 
             *		   enable commented code below --drawOPORD-- method
             */

            //Serialize the JaxFront DOM to to W3C DOM
            try {w3cBmlDom = currentDom.serializeToW3CDocument();}
            catch (Exception e) {
                printError("Exception in drawOPORD_FS:"+e);
                e.printStackTrace();
                return;
            }

            // display DOM in openMap
            if (routeLayerOPORD!=null) routeLayerOPORD.clearGraphics();

            stringOPORDName = xpathCBML.evaluate("//" + orderIDXPath,w3cBmlDom);
            if(debugMode)printDebug("Order ID :" + stringOPORDName);
	    		   	
            // singlePointXPathTag usually represents atWhere
            // multiPointXPathTag usually represents routeWhere
            // Drawing a single point or a shape of multiple points
            NodeList atWhereNodes = (NodeList) xpathCBML.evaluate(
                "//" + whereXPathTag,w3cBmlDom, XPathConstants.NODESET);

            int j = 0; // shape coords array counter , one dimension array    	
            String[] tempShape = new String[shapeCoords*2];
            for (int i=0 ; i < atWhereNodes.getLength() ; i++){
                //WhereLabel
                Node atWhere = atWhereNodes.item(i);
                String label = xpathCBML.evaluate(whereIdLabelTag, atWhere);
                shapeType = xpathCBML.evaluate(whereShapeTypeTag, atWhere);
                    if (label.length()>0) {
                        NodeList atWhereLocationNodes = 
                            (NodeList) xpathCBML.evaluate(latlonParentTag,atWhere, XPathConstants.NODESET);
                        int length = atWhereLocationNodes.getLength();
                        if(debugMode)printDebug("AtWhere Location Node " +  label + " list length is : " + length);
                        tempLocationsOPORD = new String[length*2];
                        for (int x=0 ; x < length ; x++){
                            tempLocationsOPORD[x*2] = 
                                xpathCBML.evaluate(latTag, atWhereLocationNodes.item(x));
                            tempLocationsOPORD[x*2+1] = 
                                xpathCBML.evaluate(lonTag, atWhereLocationNodes.item(x));
                            if(debugMode)printDebug("Lat =" + tempLocationsOPORD[x*2] + 
                            "  ,  Lon = " + tempLocationsOPORD[x*2+1]);
                        }	
                        locationCoords = length*2;
                        drawOPORD(stringOPORDName, label, tempLocationsOPORD, shapeType, locationCoords);
                    }
	    	} // end for(int i
	    	
	    	// Drawing Route - From-Via-To Locations
	    	String locationLat="";
	    	String locationLon="";
                if(debugMode)printDebug(routeXPathTag);
	    	NodeList LocNodeFromViaTo = (NodeList) xpathCBML.evaluate("//" + routeFromViaToTag ,w3cBmlDom, XPathConstants.NODESET);
	    	int iNumberOfRoutes = LocNodeFromViaTo.getLength();
	    	for (int iRoute=0 ; iRoute < iNumberOfRoutes ; iRoute++){
                    NodeList LocNodeFromViaToLocations = (NodeList) xpathCBML.evaluate("//" + routeXPathTag + "["+(iRoute+1)+"]//" + latlonParentTag , LocNodeFromViaTo.item(iRoute), XPathConstants.NODESET);
                    shapeCoords =LocNodeFromViaToLocations.getLength();
                    shapeType ="LN";
                    label =xpathCBML.evaluate("//" + routeXPathTag + "[" +(iRoute+1)+"]/" + routeIdLabelTag ,w3cBmlDom);
                    tempShapeOPORD = new String[shapeCoords*2]; // add 2 coords for from and to
	    	
                    // RouteWhere locations
                    for (int i=0 ; i < shapeCoords ; i++){
                        try {
                            locationLat = xpathCBML.evaluate(latTag, LocNodeFromViaToLocations.item(i));
                            tempShapeOPORD[i*2] = locationLat;
                            locationLon = xpathCBML.evaluate(lonTag, LocNodeFromViaToLocations.item(i));
                            tempShapeOPORD[i*2+1] = locationLon;
                        } catch (XPathExpressionException e) {
                            e.printStackTrace();
                        }
                    } // end for   	
                    drawOPORD(stringOPORDName, label, tempShapeOPORD, shapeType, shapeCoords);
                } // end for	
            } catch (Exception ex) {
                ex.printStackTrace();
            }	
	}// end drawOPORD_FS
	
    /*
    ** enters array of points into catalogArray
    */
    public void catalogPoints(String[] catalogArray){

        // count instances of GDC and VerticalDistance in 
        // parameter array and make new arrays to fit
        numCoords = 0;
        for(int i = 0; i < catalogArray.length - 2; i++){
            if(catalogArray[i].contains("GDC") || 
                catalogArray[i].contains("VerticalDistance"))
            numCoords++;
        }
        bmlLatCoords = new String[numCoords * 2 + 100];
        bmlLonCoords = new String[numCoords * 2 + 100];
        numCoords = 0;
        for(int i = 3; i < catalogArray.length - 2; i++){
            if(catalogArray[i].contains("GDC") && 
                catalogArray[i+1].contains(".") && catalogArray[i+2].contains(".")){
                    bmlLatCoords[numCoords] = catalogArray[i+1];
                    bmlLonCoords[numCoords] = catalogArray[i+2];
                    numCoords++;
            }	
            else if(catalogArray[i].contains("VerticalDistance") && 
                catalogArray[i-2].contains(".") && catalogArray[i-1].contains(".")){
                    bmlLatCoords[numCoords] = catalogArray[i-2];
                    bmlLonCoords[numCoords] = catalogArray[i-1];
                    numCoords++;
            }
        }// end for(int i

    }// end catalogPoints()
	
    /*
     * returns index in bmlLatCoords/bmlLonCoords of point
     * closest to the argument lat/lon
     */
    public int closestTo(String lat, String lon){
        float x1 = Float.valueOf(lat.trim()).floatValue();
        float y1 = Float.valueOf(lon.trim()).floatValue();
        float x2,y2;
        double dx,dy,dif,min;
        min = 1000;
        int index = -1;
        for(int i = 0; i < numCoords; i++){
            x2 = Float.valueOf(bmlLatCoords[i].trim()).floatValue();
            y2 = Float.valueOf(bmlLonCoords[i].trim()).floatValue();
            dx = Math.pow((x1-x2),2);
            dy = Math.pow((y1-y2),2);
            dif = Math.sqrt(dx+dy);
            if(dif < min){index = i; min = dif;}
        }
        return index;
    }// end closestTo()
	
    /**
     * Maps reportTypes to URLs; common preprocessing before a call to drawFromXML
     * 
     * @param reportType
     * @return root		The report type of the root
     */
    public String setUrls(String reportType){
        String root;
        String schemaLocation = schemaFolderLocation + "Reports/";
        if (reportType == "GeneralStatusReport" || reportType == "PositionStatusReport"){
            if(orderDomainName.equals("CBML")){
                root = "CBMLReport";
            } else {
                xsdUrl = URLHelper.getUserURL( schemaLocation.concat("IBMLReports.xsd"));
                root = "BMLREPORT";
            }
            xuiUrl = URLHelper.getUserURL(xuiFolderLocation + "GeneralStatusReportView.xui");
            if(debugMode)printDebug("New " + reportType);
        
        } else {
            if (reportType == "NATOSPOTREP"){ //Alias NatoSpotRep to regular Spot Report
                    root = "SPOTREP";
            }
            xsdUrl = URLHelper.getUserURL(schemaLocation.concat("IBMLSIMCIReports.xsd"));
            root = reportType;
            xuiUrl = URLHelper.getUserURL(xuiFolderLocation + root + "View.xui");
            if(debugMode)printDebug("Pull " + reportType);	
        }
            return root;
    
    }// end setURLs()
	
    private static void loadResource(String resources, Properties props) {
        // Iain Gillies from NZL added this to load from current dir first
        InputStream in = null;
        try{
            in = new FileInputStream(resources);
        }
        catch(FileNotFoundException ex) {
            // default to the props file in the jar
            in = C2SIMGUI.class.getResourceAsStream(resources);
        }
        if (props == null) {
            printError("Unable to locate resources: " + resources);
            printError("Using default resources.");
        } else {
            try {
                props.load(in);
                if(debugMode)printDebug("Resources located ...........: " + resources); 
                if(debugMode)printDebug("Resources from:" + in.toString());
            } catch (java.io.IOException e) {
                printError("Caught IOException loading resources: " + resources);
                printError("Using default resources.");
            }
        }
        
    }// end loadResource

    /**
     * Gets the names of the Layers to be loaded from the properties passed in, 
     * initializes them, and returns them.
     * 
     * @param p the properties, among them the property represented by
     *        the String layersProperty above, which will tell us
     *        which Layers need to be loaded
     * @return an array of Layers ready to be added to the map bean
     * @see #layersProperty
     */
    private Layer[] getLayers(Properties p) {
        // Get the contents of the hello.layers property, which is a
        // space-separated list of marker names...
        String layersValue = p.getProperty(layersProperty);

        // Didn't find it if it's null.
        if (layersValue == null) {
            printError("No property \"" + layersProperty
                    + "\" found in application properties.");
            return null;
        }
        
        // Parse the list
        StringTokenizer tokens = new StringTokenizer(layersValue, " ");
        Vector layerNames = new Vector();
        while (tokens.hasMoreTokens()) {
            layerNames.addElement(tokens.nextToken());
        }
        int nLayerNames = layerNames.size();
        Vector layers = new Vector(nLayerNames);

        // For each layer marker name, find that layer's properties.
        // The marker name is used to scope those properties that
        // apply to a particular layer. If you parse the layers'
        // properties from a file, you can add/remove layers from the
        // application without re-compiling. You could hard-code all
        // the properties being set if you'd rather...
        for (int i = 0; i < nLayerNames; i++) {
            String layerName = (String) layerNames.elementAt(i);

            // Find the .class property to know what kind of layer to create.
            String classProperty = layerName + ".class";
            String className = p.getProperty(classProperty);
            if (className == null) {
                // Skip it if you don't find it.
                printError("Failed to locate property \"" + classProperty + "\"");
                printError("Skipping layer \"" + layerName + "\"");
                continue;
            }
            try {
                // Create it if you do...
                Object obj = java.beans.Beans.instantiate(null, className);
                if (obj instanceof Layer) {
                    Layer l = (Layer) obj;
                    
                    // All layers have a setProperties method, and
                    // should intialize themselves with proper
                    // settings here. If a property is not set, a
                    // default should be used, or a big, graceful
                    // complaint should be issued.
                    l.setProperties(layerName, p);
                    layers.addElement(l);
                }
            } catch (java.lang.ClassNotFoundException e) {
                printError("Layer class not found: \"" + className + "\"");
                printError("Skipping layer \"" + layerName + "\"");
            } catch (java.io.IOException e) {
                printError("IO Exception instantiating class \"" + className + "\"");
                printError("Skipping layer \"" + layerName + "\"");
            }
        }
        int nLayers = layers.size();
        if (nLayers == 0) {
            return null;
        } else {
            Layer[] value = new Layer[nLayers];
            layers.copyInto(value);
            return value;
        }
    }// end getLayers()

    private void closeDocument() {
        
        // check for changes in the XUI before closing
        if(xuiCacheHash != 0){
            compareXml = getDomXmlLessJaxFront();;
            if(loadedXml.compareTo(compareXml) != 0) {
                int answer = JOptionPane.showConfirmDialog(
                    null,  
                    "About to overwrite changed data in form - load form anyhow?",
                    "Warning",
                    JOptionPane.OK_CANCEL_OPTION);
                if (answer != JOptionPane.OK_OPTION) {
                    return;
                }
            }
        }
        releaseXUICache();
        if (editor != null)centerPanel.remove(editor);
        centerPanel.repaint();	
    
    }// end closeDocument()
	
    /**
     * Change current language to English
     */
    private void languageEnglish() {
        currentDom.getGlobalDefinition().setLanguage("en");
        TypeVisualizerFactory.getInstance().releaseCache(currentDom);	//to refresh the views

        //Recreate EditorPanel and add it to the container again
        editor = new EditorPanel(currentDom.getRootType(), this);
        centerPanel.removeAll();
        centerPanel.add(editor, BorderLayout.CENTER);
    }// end languageEnglish()
	
    /**
     * Change current language to French
     */
    private void languageFrench() {
        currentDom.getGlobalDefinition().setLanguage("fr");
        centerPanel.remove(editor);		//to refresh the views

        //Recreate EditorPanel and add it to the container again
        editor = new EditorPanel(currentDom.getRootType(), this);
        centerPanel.add(editor);
    }// end languageFrench()
	
    /**
     * Change current language to German
     */
    private void languageGerman() {
        currentDom.getGlobalDefinition().setLanguage("de");
        TypeVisualizerFactory.getInstance().releaseCache(currentDom);	//to refresh the views

        //Recreate EditorPanel and add it to the container again
        editor = new EditorPanel(currentDom.getRootType(), this);
        centerPanel.removeAll();
        centerPanel.add(editor, BorderLayout.CENTER);
    }// end languageGerman()
	
    /**
     * Change current language to Italian
     */
    private void languageItalian() {
        currentDom.getGlobalDefinition().setLanguage("it");
        TypeVisualizerFactory.getInstance().releaseCache(currentDom);//to refresh the views

        //Recreate EditorPanel and add it to the container again
        editor = new EditorPanel(currentDom.getRootType(), this);
        centerPanel.removeAll();
        centerPanel.add(editor, BorderLayout.CENTER);
    
    }// end languageItalian()

    /*
     * mababneh
     * 11/9/2011
     * drawMSDL : draw the MSDL layer
     */
    public void drawMSDL(URL url1, URL url2, URL url3, String root){

        initDom(null, url1, url2, url3, root);
		
        try {	
            // reading the whole DOM values of the XML file using XPath

            //Serialize the JaxFront DOM to to W3C DOM
            try {w3cBmlDom = currentDom.serializeToW3CDocument();}
            catch (Exception e) {
                printError("Exception in drawMSDL:"+e);
                e.printStackTrace();
                return;
            }
            NodeList unitNodes = 
                (NodeList) xpathMSDL.evaluate("//" + 
                    "Unit" ,w3cBmlDom, XPathConstants.NODESET);
            NodeList equipmentNodes = 
                (NodeList) xpathMSDL.evaluate("//" + 
                    "EquipmentItem" ,w3cBmlDom, XPathConstants.NODESET);

            if(debugMode)printDebug("Unit Node List Length = " + unitNodes.getLength());
            if(debugMode)printDebug("Equipment Node List Length = " + equipmentNodes.getLength());

            int orgArrayLength = unitNodes.getLength()+ equipmentNodes.getLength();
            int orgArrayIndex = 0;
            String [][] orgUnitsAndEquipments = new String[orgArrayLength][4];
            for (int i=0 ; i < unitNodes.getLength() ; i++){
                //WhereLabel
                Node unit = unitNodes.item(i);
                //unitName
                orgUnitsAndEquipments[orgArrayIndex][0] = xpathMSDL.evaluate("Name", unit);
                //unitSymbol
                orgUnitsAndEquipments[orgArrayIndex][1] = xpathMSDL.evaluate("SymbolIdentifier", unit);
                //unitLat
                orgUnitsAndEquipments[orgArrayIndex][2] = xpathMSDL.evaluate("Disposition//Location//CoordinateData//GDC//Latitude", unit);
                //unitLon 
                orgUnitsAndEquipments[orgArrayIndex][3] = xpathMSDL.evaluate("Disposition//Location//CoordinateData//GDC//Longitude", unit);
                orgArrayIndex++;

            } // end for(int i    	

            for (int j=0 ; j < equipmentNodes.getLength() ; j++){
                    //WhereLabel
                    Node equipment = equipmentNodes.item(j);
                    //unitName
                    orgUnitsAndEquipments[orgArrayIndex][0] = xpathMSDL.evaluate("Name", equipment);
                    //unitSymbol
                    orgUnitsAndEquipments[orgArrayIndex][1] = xpathMSDL.evaluate("SymbolIdentifier", equipment);
                    //unitLat
                    orgUnitsAndEquipments[orgArrayIndex][2] = xpathMSDL.evaluate("Disposition//Location//CoordinateData//GDC//Latitude", equipment);
                    //unitLon 
                    orgUnitsAndEquipments[orgArrayIndex][3] = xpathMSDL.evaluate("Disposition//Location//CoordinateData//GDC//Longitude", equipment);
                    orgArrayIndex++;
            } // end for

            if(debugMode)printDebug("=== Printing Units and Equipments found in MSDL Organization");
            for (int j1=0 ; j1< orgUnitsAndEquipments.length; j1++){
                if(debugMode)printDebug("Org Name: " + orgUnitsAndEquipments[j1][0]);
                if(debugMode)printDebug("Org Symbol ID: " + orgUnitsAndEquipments[j1][1]);
                if(debugMode)printDebug("Org Lat: " + orgUnitsAndEquipments[j1][2]);
                if(debugMode)printDebug("Org Lon: " + orgUnitsAndEquipments[j1][3]);   		 		
            }

            String areaOfInterestName, areaofInterestUpperRightLat, areaofInterestUpperRightLon, areaofInterestLowerLeftLat, areaofInterestLowerLeftLon;
            NodeList areaOfInterestNodes = (NodeList) xpathMSDL.evaluate("//" + "AreaOfInterest" ,w3cBmlDom, XPathConstants.NODESET);
            Node areaofInterestNode = areaOfInterestNodes.item(0);
            areaOfInterestName = xpathMSDL.evaluate("Name", areaofInterestNode);
            areaofInterestUpperRightLat = xpathMSDL.evaluate("//UpperRight/CoordinateData//GDC//Latitude", areaofInterestNode);
            areaofInterestUpperRightLon = xpathMSDL.evaluate("//UpperRight/CoordinateData//GDC//Longitude", areaofInterestNode);
            areaofInterestLowerLeftLat = xpathMSDL.evaluate("//LowerLeft/CoordinateData//GDC//Latitude", areaofInterestNode);
            areaofInterestLowerLeftLon = xpathMSDL.evaluate("//LowerLeft/CoordinateData//GDC//Longitude", areaofInterestNode);

            if(debugMode)printDebug("areaOfInterestName: " + areaOfInterestName);
            if(debugMode)printDebug("areaofInterestUpperRightLat: " + areaofInterestUpperRightLat);
            if(debugMode)printDebug("areaofInterestUpperRightLon: " + areaofInterestUpperRightLon); 
            if(debugMode)printDebug("areaofInterestLowerLeftLat : " + areaofInterestLowerLeftLat);
            if(debugMode)printDebug("areaofInterestLowerLeftLon : " + areaofInterestLowerLeftLon );

            String [] areaOfInterestArray = new String[5];
            areaOfInterestArray[0] = areaOfInterestName;
            areaOfInterestArray[1] = areaofInterestUpperRightLat;
            areaOfInterestArray[2] = areaofInterestUpperRightLon;
            areaOfInterestArray[3] = areaofInterestLowerLeftLat;
            areaOfInterestArray[4] = areaofInterestLowerLeftLon;

            // Calling the drawing method
            mapGraph = true;
            if(debugMode)printDebug("------drawMSDL-----");

            if (!mapMSDL) {
                // mapMSDL 
                routeLayerMSDL = new RouteLayer(IconType.INITIALIZE);
                routeLayerMSDL.setName("MSDL"); //xmlDocumentType
                routeLayerMSDL.setVisible(true);
                mapHandler.add(routeLayerMSDL);
                mapMSDL = true;
            }
            routeLayerMSDL.createMSDLGraphics(orgUnitsAndEquipments, areaOfInterestArray);	

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }// end drawMSDL()
    
    // override to make WindowListener happy
    @Override
    public void windowDeiconified(WindowEvent e) {
        if(debugMode)printDebug("received unexpected call to WindowListner.windowDeiconified");
    }
    
    /**
     * performs String.indexOf function for C2SIM XML tag
     * with and without the c2sim namespace prefix
     * assumes the tags are direct from XML e.g. <ns:tag>
     * returns -1 if lookFor not found
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
                "missing tag for " + tag," XML error");
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
    
    /*
    * obtains Java Date() and recorganizes to YYYY-MM-DD
    */
    HashMap<String,String> monthNum = null;
    String thisDate() {
        
        // make a HashMap to turn month code to a number
        // build it only once
        if(monthNum == null){
            monthNum = new HashMap<String,String>();
            monthNum.put("Jan","01");
            monthNum.put("Feb","02");
            monthNum.put("Mar","03");
            monthNum.put("Apr","04");
            monthNum.put("May","05");
            monthNum.put("Jun","06");
            monthNum.put("Jul","07");
            monthNum.put("Aug","08");
            monthNum.put("Sep","09");
            monthNum.put("Oct","10");
            monthNum.put("Nov","11");
            monthNum.put("Dec","12");
        }
        String date = (new Date()).toString();
        String month = monthNum.get(date.substring(4,7));
        return date.substring(24,28)+"-"+month+"-"+date.substring(8,10);
        
    }// end thisDate()
    
    /**
     * returns the part of C2SIM Report message up to ReportContent
     * in his and other functions below we presume if there is namespace 
     * indicator in a tag it will be in all tags 
     * @param report: message to be extracted from 
     */
    String extractReportFirstPart(String report){
        int endReportHeading = report.indexOf("<ReportContent>");

        if(endReportHeading < 0){
            String tagWithNs = "<" + c2simns + ":ReportContent>";
            endReportHeading = report.indexOf(tagWithNs);
        }
        if(endReportHeading < 0){
            printError("missing ReportContent tag in messageBody:" + report);
                return null;
        }
        return report.substring(0,endReportHeading);
    }// end extractReportFirstPart()
    
    /**
     * returns first ReportContent from C2SIM Report message after startIndex
     * NOTE: because of findEndingIndex test before this is invoked, 
     * we know the endTag exists
     * @param1 report: message to be extracted from
     * @param2 startIndex: starting point in report for extracting
     */
    String extractReportContent(String report, int startReportContent){
        String fromReportContent = report.substring(startReportContent);
        int endReportContent = fromReportContent.indexOf("</ReportContent>");
        if(endReportContent >= 0)endReportContent += 16;//("</ReportContent>").length()
        else {
            String tagWithNs = "</" + c2simns + ":ReportContent>";
            endReportContent = fromReportContent.indexOf(tagWithNs);
            if(endReportContent >= 0)endReportContent += tagWithNs.length();
            else return null;
        }
        return fromReportContent.substring(0, endReportContent);
    }// end extractReportContent()
    
    /**
     * finds index of the ending part of C2SIM report
     */
    int findEndingIndex(String report){
        int startEnding = report.lastIndexOf("</ReportContent>");
        if(startEnding >= 0)startEnding += 16;//("</ReportContent>").length()
        else {
            String tagWithNs = "</" + c2simns + ":ReportContent>";
            startEnding = report.lastIndexOf(tagWithNs);
            if(startEnding < 0){
                printError(
                    "missing ReportContent endtag in messageBody:" + report);
                return -1;
            }
            startEnding += tagWithNs.length();
        }
        return startEnding; // ("ReportContent>").length()
    }// end extractReportEndingPart()
    
    /**
     * modifies endPart of a C2SIM report with a unique reportID
     */
    String uniqueEndingPart(String report, String endingPart){
        
        // find the start and ending of ReportID data
        int startReportID = endingPart.indexOf("ReportID>");
        if(startReportID < 0) {
            printError(
                "missing ReportID tag in messageBody:" + report);
            return null;
        }
        startReportID += 9; // ("ReportID>").length() 
        int endReportID = endingPart.indexOf("</",startReportID);
        if(endReportID < 0) {
            printError(
                "missing ReportID endtag in messageBody:" + report);
            return null;
        }
        
        // subtitute a random UUID for ReportID data and return
        String beforeReportID = endingPart.substring(0,startReportID);
        String afterReportID = endingPart.substring(endReportID);
        UUID newReportID = UUID.randomUUID();
        return beforeReportID + newReportID + afterReportID;
  }// end uniqueEndingPart() 

} // end of C2SIMGUI Class
