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
*----------------------------------------------------------------*/
package edu.gmu.c4i.c2simclientlib2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <h1>C2SIMClientREST_Lib</h1> <p>
 * BML Server Web Services REST Client<p>
 * This client does the following:<p>
 *      Open a connection with the server on specified port (Default is 8080)<p>
 *      Build an HTTP POST transaction from parameters and BML XML document<p>
 *      Submit the transaction<p>
 *      Read the result<p>
 *      Disconnect from the server<p>
 *      Return the result received from the server to the caller<p>
 * *author Douglas Corner - George Mason University C4I Cyber Center
 *  code version 4.8.0.4
 */
public class C2SIMClientREST_Lib {

    static String SISOSTD = "SISO-STD-C2SIM";
    
    // Instance variables
    private static String clientVersion = "";
    private String host = "localhost";
    private String port = "8080";
    private String path = "C2SIMServer/c2sim";
    private String submitter = "NotSet";

    private static String protocol = "";   // C2SIM 
    private String firstForwarders = "";   // C2SIM
    private String stompServer = "";       // C2SIM

    private String domain;      // Not used any more.  Older client 
                                // code may still set it

    private C2SIMHeader c2s;

    protected static final String XML_PREAMBLE = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /****************************************/
    /* C2SIMClientREST_Lib() - Constructors    */
    /****************************************/
    /**
     * BMLClientREST_Lib Constructor w/o parameters <BR>
     * This constructor is used when submitting non C2SIM documents
     */
    public C2SIMClientREST_Lib() {

        // Make sure we have version of ClientLib

        if (clientVersion.equals(""))
            clientVersion = getVersion();

        protocol = "BML";
    }   // BMLClientREST()


    /**
     * Constructor used when the intention is to send a C2SIM or CWIX document. <BR>
     * The supplied parameters will be saved and used to create the C2SIM message 
     * header when bmlRequest is called to make the submission
    @param sender   -   CWIXHeader field - Sender of document
    @param receiver -   CWIXHeader field - Receiver of document
    @param performative - CWIXHeader field - Action that receiver is to  
    *                     perform as specified by the C2SIM specification 
    @param protocolVersion - as specified by C2SIM standard; 
    *                        in Jun2020 "1.0.1"
    */
        
    public C2SIMClientREST_Lib(
        String sender, 
        String receiver, 
        String performative, 
        String protocolVersion) {

        // Make sure we have version of ClientLib 

        if (clientVersion.equals(""))
            clientVersion = getVersion();

        // Instantiate C2SIM Header and fill in with values supplied with call
        protocol = SISOSTD;
        c2s = new C2SIMHeader();
        c2s.setFromSendingSystem(sender);
        c2s.setToReceivingSystem(receiver);
        c2s.setCommunicativeActTypeCode(performative);
        c2s.setProtocol(SISOSTD);
        c2s.setProtocolVersion(protocolVersion);

        // Generate conversationID and messageID - These are UUID32's
        c2s.generateConversationID();
        c2s.generateMessageID();

    }


    /********************/
    /* serverStatus     */
    /********************/
    /**
     * Get status of C2SIM Server. - Confirm that server is running and return initialization status<BR>
     * setHost()  and setSubmitter() must have must have been executed before calling this method.<BR>
  
    @return - XML Document indicating current status of the server.<BR>
        Sample output:<BR>
        <pre>{@code 
        <?xml clientVersion="1.0" encoding="UTF-8"?>
        <result>
           <status>OK</status>
           <message>Server is operating</message>
           <serverInitialized>false</serverInitialized>
           <sessionInitialized>false</sessionInitialized>
           <unitDatabaseName>defaultDB</unitDatabaseName>
           <unitDatabaseSize>0</unitDatabaseSize>
           <msgNumber>0</msgNumber>
           <time> 0.000</time>
        </result>
        }</pre>
    @throws C2SIMClientException - Primary and secondary causes will be available in 
     */
    public String serverStatus() throws C2SIMClientException {
        URL url;
        HttpURLConnection conn;
        OutputStream os;
        BufferedReader br;
        String output;
        String result = "";
        String path = "C2SIMServer/status";


        try {
            String u = "http://" + host + ":" + port + "/" + path;

            url = new URL(u);

            // Set up parameters to do a POST of the xml BML transaction
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.addRequestProperty("Accept", "text/plain");

            // Read the response, creating a single string
            // Create BufferedReader from the HttpURLConnection
            br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            // Do readLine's until a null is returned indicating the end of the message
            while ((output = br.readLine()) != null) {
                result += output + "\n";
            }

            // RESTful WS is stateless - Disconnect our connection with host
            conn.disconnect();
        } // try // try

        catch (MalformedURLException e) {
            throw new C2SIMClientException("Malformed URL Exception ", e);
        }   // MalformedURLException        
        catch (IOException e) {
            throw new C2SIMClientException("I/O Exception", e);
        }   // IOException   // IOException

        // Did we get an error from the server? (The server returns XML)
        if (result.contains("<status>Error</status>"))
            throw new C2SIMClientException("Error received from server\n" + result);

        return result;

    }


    /************************/
    /* c2simCommand         */
    /************************/
    /**
     * c2simCommand pass a command to the C2SIM Server <BR>
     * Current commands are NEW, LOAD, SAVE, SAVEAS, DELETE, SHARE, QUERYUNIT, QUERYINIT<BR>
 See the latest clientVersion of <i>C2SIM Server Reference Implementation</i> for details.<BR>
     * Result is an XML document which may contain {@code<status>OK</status>} or may be actual data depending on the command submitted.<BR>
     * 
    @param cmd      Command to be processed.  
    @param parm1    Optional first parameter
    @param parm2    Optional second parameter
    @return String result - XML Document giving results of command and server status similar to serverStatus method.
    @throws C2SIMClientException = Primary and secondary causes are transmitted within the C2SIMClientException object.
     */
    public String c2simCommand(String cmd, String parm1, String parm2) throws C2SIMClientException {

        URL url;
        HttpURLConnection conn;
        OutputStream os;
        BufferedReader br;
        String output;
        String result = "";
        String path = "C2SIMServer/command";
        String xml = 
            "<C2SIM_Statistics xmlns=\"http://www.sisostds.org/schemas/c2sim/1.0\"/>";

        // Make sure we have version of ClientLib 

        if (clientVersion.equals(""))
            clientVersion = getVersion();

        // Make sure the required parameters have been provided
        if (submitter.equals(""))
            throw new C2SIMClientException("Error - Submitter not specified");

        if ((cmd == null) || (cmd.equals("")))
            throw new C2SIMClientException("No command specified");

        if (parm1 == null)
            parm1 = "";

        if (parm2 == null)
            parm2 = "";

        if (submitter == null)
            submitter = "";

        // Build the parameter string to include
        //       try {
        String u
                = "http://" + host + ":" + port
                + "/" + path
                + "?submitterID=" + submitter
                + "&command=" + cmd
                + "&parm1=" + parm1
                + "&parm2=" + parm2
                + "&version=" + clientVersion;

        result = sendTrans(u, xml);

        return result;

    }   // c2simCommand


    /********************/
    /* c2simRequest     */
    /********************/
    /**
     * Submit a request to a BML/C2SIM Server <BR>
     * This method performs the same function as the bmlRequest method and is included as part of the migration from BML to C2SIM 
    @param xml - The xml document being submitted
    @return - Indication of success of operation along with server status.  See serverStatus method.
    @throws C2SIMClientException - Primary and secondary causes will be included in C2SIMClientException object
     */
    public String c2simRequest(String xml) throws C2SIMClientException {
        return bmlRequest(xml);
    }   // c2simRequest()


    /********************/
    /*  bmlRequest()    */
    /********************/
    /**
     * Submit a BML transaction to a BML/C2SIM Server host
    
     * As a minimum setHost() and setSubmitter() must have been executed before 
     * calling this method.
     * @param xml - An XML string containing a BML or C2SIM xml document.<BR>
     *    If the document is C2SIM the C2SIM message envelope should not be 
     *    included it will be generated by this method
     * @return  XML - The response received from the host BML server
     * @see edu.gmu.c4i.c2simclientlib2.C2SIMClientException
     * @throws edu.gmu.c4i.c2simclientlib2.C2SIMClientException - Various causes
     */
    public String bmlRequest(String xml) throws C2SIMClientException {

        String output;
        String result = "";
        Long startTime;
        Long endTime;
        Date startDate;
        Date endDate;
        String header;
        String body;
        String msg = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");


        // Make sure the required parameters have been provided
        if (submitter.equals(""))
            throw new C2SIMClientException("Error - Submitter not specified");

        // Determine the protocol for this message based on the root element,  Override whatever the user may have have set
        protocol = determineProtocol(xml);

        // If protocol is BML then use the message as received
        if (protocol.equalsIgnoreCase("BML")) {
            msg = xml;
        }   // BML
        
        if (protocol.equalsIgnoreCase(SISOSTD)) {
            
            header = c2s.toXMLString();
        
            // Locate the message body (After the xml preamble)
            body = locateXmlBody(xml);

            // Now build the message and frame with with C2SIM_Message
            msg = XML_PREAMBLE + "<Message xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" + header + body + "</Message>";

        }   // C2SIM
        
        // Build the parameter string

        String u
                = "http://" + host + ":" + port
                + "/" + path
                + "?submitterID=" + submitter
                + "&protocol=" + protocol
                + "&version=" + clientVersion;

        if (protocol.equals("C2SIM") || protocol.equals(SISOSTD) ) {
            u += "&sender=" + c2s.getFromSendingSystem()
                    + "&receiver=" + c2s.getToReceivingSystem()
                    + "&conversationid=" + c2s.getConversationID();
        }   // is C2SIM

        // If first forwarder is set add it
        if (!firstForwarders.equals(""))
            u += "&forwarders=" + firstForwarders;

        // Record the start time
        startTime = System.currentTimeMillis();
        startDate = new Date();

        result = sendTrans(u, msg);

        // Record the end time for the transaction
        endTime = System.currentTimeMillis();
        endDate = new Date();
        Double elapsedTime = (1.0 * endTime - startTime) / 1000;
        String resultRT = "";

        // If the server indicates that reponse time statistics should be collected, send them.
        if (parseXML(result, "collectResponseTime").equalsIgnoreCase("T")) {

            // Send the response time to the stats collector on the BML Server
            u = "http://" + host + ":" + port
                    + "/" + "C2SIMServer/stats"
                    + "?submitterID=" + submitter;

            // Set up the xml with the response time of the first transaction
            String responseTimeResult = XML_PREAMBLE
                    + "<C2SIM_Statistics xmlns=\"http://www.sisostds.org/schemas/c2sim/1.0\">"
                    + "<REST_ResponseTime>"
                    + "<submitterID>" + submitter + "</submitterID>"
                    + "<msgNumber>" + parseXML(result, "msgNumber") + "</msgNumber>"
                    + "<startTime>" + sdf.format(startDate) + "</startTime>"
                    + "<endTime>" + sdf.format(endDate) + "</endTime>"
                    + "<elapsedTime>" + elapsedTime + "</elapsedTime>"
                    + "<serverTime>" + parseXML(result, "time") + "</serverTime>"
                    + "</REST_ResponseTime></C2SIM_Statistics>";

            // Send the response time to be recorded on the server
            sendTrans(u, responseTimeResult);
        }
        // Did we get an error from the server, (The server returns XML)
        if (result.contains("<status>Error</status>"))
            throw new C2SIMClientException("Error received from server\n" + result);

        return result;

    }   // bmlRequest()


    /****************/
    /* sendTrans    */
    /****************/
    private static String sendTrans(String u, String xml) throws C2SIMClientException {

        HttpURLConnection conn;
        OutputStream os;
        BufferedReader br;
        String output;
        String result = "";

        try {
            URL url = new URL(u);

            // Set up parameters to do a POST of the xml BML transaction
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("Content-Type", "application/xml");
            conn.addRequestProperty("Accept", "application/xml");

            // Add a nl to end of message just in case ...
            xml += "\n";

            // Send the transaction and flush it
            os = conn.getOutputStream();
            os.write(xml.getBytes());
            os.flush();

            // Read the response, creating a single string
            // Create BufferedReader from the HttpURLConnection
            br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            // Do readLine's until a null is returned indicating the end of the message
            while ((output = br.readLine()) != null) {
                result += output + "\n";
            }

            // RESTful WS is stateless - Disconnect our connection with host
            conn.disconnect();
        } // try 

        catch (MalformedURLException e) {
            throw new C2SIMClientException("Malformed URL Exception ", e);
        }   // MalformedURLException        
        catch (IOException e) {
            throw new C2SIMClientException("I/O Exception", e);
        }   // IOException   // IOException
        return result;
    }


    /**********************/
    /* Get clientVersion  */
    /**********************/
    String getVersion() {
        Properties props = new Properties();
        String ver = "";
        InputStream in = this.getClass().getResourceAsStream("/META-INF/maven/edu.gmu.c4i/C2SIMClientLib2/pom.properties");

        // We won't find the Resource when debugging.
        if (in == null)
            ver = "UNKNOWN";
        else {
            try {
                //props.load(new FileInputStream(PROPERTIES_FILE_NAME));
                props.load(in);
            }
            catch (IOException e) {
                clientVersion = "UNKNOWN";
            }
            ver = (String) props.get("version");
        }
        return ver;
    }   // getVersion()


    /************************/
    /* determineProtocol    */
    /************************/
    // Extract the root element and separate it from a namespace prefix if present
    // Determine the protocol code to use with this root element
    static String determineProtocol(String xml) {
        String temp = locateXmlBody(xml);
        String root = "";
        String prot = "";

        // Locate the root tag accounting for possible namespace prefix.
        Pattern p = Pattern.compile("\\s*<(\\w+:)?(\\w+)");
        Matcher m = p.matcher(temp);
        if (m.find()) {
            // Tag is second match.  First match is prefix: which is optional
            root = m.group(2);
        }

        switch (root) {

            case "MessageBody":
                prot = protocol;
                break;

            default:
                prot = "BML";
                break;

        }   // switch

        // Return the protocol
        return prot;

    }   // determineProtocol()


    /********************/
    /*  locateXMLBody   */
    /********************/
    // Locate the xml body following 
    static String locateXmlBody(String xml) {
        String body = "";

        // Is the XML preamble present?
        if (xml.startsWith("<?xml version")) {
            // Locate the actual beginning of the xml (Find the second "<")
            int start = xml.indexOf("<", 1);

            // Get the xml after the XML preamble
            body = xml.substring(start);
        }
        else
            body = xml;
        return body;
    }   // locateXmlBody()


    /****************************/
    /*  getters and setters     */
    /****************************/
    /**
     * Return the domain property (Not used)
     * @return the current setting of the domain property. <BR>
     *   This property is no longer used.
     */
    public String getDomain() {
        return domain;
    }   // getDomain()


    /**
     * Set the domain property
     * @param domain - Not used, kept for compatibility with earlier versions
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }   // setDomain()


    /**
     * Return the protocol property
     * @return the current setting of the protocol property.
     */
    public String getProtocol() {
        return protocol;
    }   // getProtocol()


    /**
     * Set the protocol property
     * @param protocol - Differentiates between BML and C2SIM.<BR>
     *   The user should use the constructor with C2SIM parameters to initiate a C2SIM request 
     */
    public void setProtocol(String protocol) throws C2SIMClientException {
        
        if(c2s == null)throw new C2SIMClientException(
            "C2SIMClientREST_Lib must be instantiated with parameters before setProtocol()");
        this.protocol = protocol;
        c2s.setProtocol(protocol);
    }   // setProtocol()


    /**
     * Get current setting of host property
     * @return  String - Name or IP address of BML/C2SIM server host
     */
    public String getHost() {
        return host;
    }   // getHost()


    /**
     * Set the host name or address
     * @param host String = Name or IP address of BML/C2SIM server host
     */
    public void setHost(String host) {
        this.host = host;
    }   // setHost()


    /**
     * Get the current setting of the TCP port property
     * @return String - TCP port number to be used 
     */
    public String getPort() {
        return port;
    }   // getPort()


    /**
     * Set the port number (as a string)
     * @param port String - Port number on server to be used.<BR>
     *    Defaults to 8080
     */
    public void setPort(String port) {
        this.port = port;
    }   // setPort()


    /**
     * Get the current setting of the path property
     * @return  String - Path used in URL
     */
    public String getPath() {
        return path;
    }   // getPath()


    /**
     * Set the URL path <BR>
     * @param path String - Path to be used in URL <BR>
     *   Defaults to BMLServer/bml
     */
    public void setPath(String path) {
        this.path = path;
    }   // setPath


    /**
     * Get current setting of Requestor property<BR>
     *    This is the same as getSubmitter and provides compatibility with earlier versions of the library.
     * @return  String = ID of sender
     */
    public String getRequestor() {
        return submitter;
    }   // getRequestor()


    /**
     * Set the Requestor property indicating the identity of the client <BR>
     *    This is the same as setSubmitter and provides compatibility with earlier versions of the library.
     * @param requestor - String ID of submitter
     */
    public void setRequestor(String requestor) {
        this.submitter = requestor;
    }   // setRequestor()


    /**
     * Get current setting of Submitter property
     * @return  String = ID of sender
     */
    public String getSubmitter() {
        return submitter;
    }   // getSubmitter()


    /**
     * Set the Submitter property indicating the identity of the client
     * @param submitter - String ID of submitter
     */
    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }   // setSubmitter()


    /**
     * Get the FirstForwarder property indicating first server to
     * handle the XML document
     * @return  String - Host name of first server to handle this message  
     */
    public String getFirstForwarders() {
        return firstForwarders;
    }   // getForwarders()


    /**
     * Set the FirstForwarder property indicating first server to
     * handle the XML document
     * @param firstForwarders    String - Host name of first server to handle this message
     */
    public void setFirstForwarders(String firstForwarders) {
        this.firstForwarders = firstForwarders;
    }   // setFirstRarwarder()


//    /**
//     * Set the value of C2SIMHeader to be used with submission of C2SIM transaction <BR>
//     * Should not normally be used as the header is automatically generated by the client code.
//    @param  c -Reference to a CWIXHeader object
//     */
//    public void setC2SIMHeader(CWIXHeader c) {
//        c2s = c;
//    }   // setC2SIMHeader
//
//
    /**
     * Return value of C2SIM Header
    @return CWIXHeader the current C2SIM header. 
     */
    public C2SIMHeader getC2SIMHeader() {
        return c2s;
    }   // getC2SIMHeader
    
    /**
    *   Search an xml string looking for the first instance of a tag. <BR>
    *   If found, return the value associated with that tag <BR>
    *   It is recognized that this is NOT a suitable way for searching XML documents..<BR>
    *   It is used here in case because it is simple
    *
    * @param xml -       The xml string to be searched
    * @param target -    The string (Tag) being searched for 
    * @return -          The value of the element named by that tag
    */
    public static String parseXML(String xml, String target) {
        String result = "";
        int start = xml.indexOf("<" + target + ">") + target.length() + 2;
        int end = xml.indexOf("</" + target + ">");
        if ((start > 0) && (end > 0))
            result = xml.substring(start, end);
        return result;
    }   // parseXML()  

}   // Class BMLClientREST
