using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Linq;
using Microsoft.Extensions.Logging;

namespace C2SimClientLib;

/// <summary>
/// C2SIM message header
/// From original Java code implemented by  Douglas Corner - George Mason University C4I Center
public class C2SIMHeader : IC2SIMHeader
{
    #region Constants
    const string SISOSTD = "SISO-STD-C2SIM";
    const string SISOSTDProtocolVersion = "1.0.0";
    const string C2SimDateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    const string XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    #endregion

    #region Private properties
    // Instance variables
    /// <summary>
    /// CommunicativeActTypeCode - Communicative act; one of the values 
    /// from enumCommunicativeActCategoryCode indicating type of message
    /// </summary>
    internal string _communicativeActTypeCode = string.Empty;
    /// <summary>
    /// securityClassificationCode -  Indicates the security 
    /// classification of this message
    /// </summary>
    internal string _securityClassificationCode = string.Empty;
    /// <summary>
    /// conversationID - Unique identifier for the conversation. 
    /// Should be kept identical for all replies.<BR> 
    ///   The conversationID may be used to associate a number of 
    /// messages into a logical grouping.
    /// </summary>
    internal string _conversationID = string.Empty;
    /// <summary>
    /// messageID - Unique identifier for the message. Other messages refer to the message using this ID. 
    /// </summary>
    internal string _messageID = string.Empty;
    /// <summary>
    /// protocol - The protocol of the this message
    /// </summary>
    internal string _protocol = SISOSTD;
    /// <summary>
    /// protocolVersion - The version of the protocol of the this message
    /// </summary>
    internal string _protocolVersion = SISOSTDProtocolVersion;
    /// <summary>
    /// - replyToSystem - Specifies what system to reply to.<BR>
    /// </summary>
    internal string _replyToSystem = string.Empty;
    /// <summary>
    /// Sending Time - ISO DateTime format yyyy-MM-ddTHH:mm:ssZ
    /// </summary>
    internal string _sendingTime = DateTime.Now.ToString(C2SimDateFormat);
    /// <summary>
    /// fromSendingSystem - ID of sending system (UUID)
    /// </summary>
    internal string _fromSendingSystem = string.Empty;
    /// <summary>
    /// toReceivingSystem -  ID of destination system (UUID))
    /// </summary>
    internal string _toReceivingSystem = string.Empty;
    /// <summary>
    /// inReplyToMessageID - ID of message being replied to (UUID)
    /// </summary>
    internal string _inReplyToMessageID = string.Empty;
    #endregion

    #region Construction / teardown
    /// <summary>
    /// Constructs a header object
    /// </summary>
    /// <param name="logger">Logger to use</param>
    public C2SIMHeader()
    {
    }
    #endregion

    #region Public methods
    /// <summary>
    /// Given an xml document with the XML header and content create and insert a C2SIM header into it
    /// </summary>
    /// <param name="xml">The xml string that header is to be inserted into</param>
    /// <param name="sender">The C2SIM Sender</param>
    /// <param name="receiver">The C2SIM Receiver</param>
    /// <param name="performative">One of the C2SIM performatives
    /// Possible values for CommunicativeActTypeCode are: Inform, Confirm, Refuse, Accept, Agree, Request
    /// </param>
    /// <returns>The updated XML string with the header inserted</returns>
    public static string InsertC2SIM(string xml, string sender, string receiver, string performative, string version)
    {
        // Create a new C2SIMHeader and add the parameters passed to this routine
        C2SIMHeader c2s = new C2SIMHeader
        {
            FromSendingSystem = sender,
            ToReceivingSystem = receiver,
            CommunicativeActTypeCode = !string.IsNullOrWhiteSpace(performative) ? performative : "Inform",
            ProtocolVersion = version,
        };

        // Add Message ID and Conversation ID to the header
        c2s.GenerateConversationID();
        c2s.GenerateMessageID();
        string c2sHeader = c2s.ToXMLString();
        // We need to construct a C2SIM message, XMLPreamble + C2SIMHeader + XML
        // Get the C2SIM Header in xml text
        string temp;
        // Is the XML preamble present?
        if (xml.StartsWith("<?xml version"))
        {
            // Locate the actual beginning of the xml (Find the second "<")
            int start = xml.IndexOf("?>") + "?>".Count();
            // Get the xml after the XML preamble
            temp = xml.Substring(start);
        }
        else
            temp = xml;
        // Now build the message and frame with with C2SIM_Message
        string newXML = XML_PREAMBLE +
            "<Message xmlns=\"http://www.sisostds.org/schemas/C2SIM/1.1\">" +
            c2sHeader + temp + "</Message>";
        return newXML;
    }

    /// <summary>
    /// Remove C2SIM message envelope and return core xml message with xml header
    /// </summary>
    /// <param name="xml">string - Input xml message with C2SIM envelope</param>
    /// <returns>string - Reconstructed message without C2SIM components</returns>
    [Obsolete("The .NET version parses the C2SIM header as the message is received from STOMP, so this method is not needed")]
    public static string RemoveC2SIM(string xml)
    {
        // Get rid of NL
        //string temp = xml.Replace("\n", string.Empty);
        int start = xml.IndexOf("</C2SIMHeader>") + "</C2SIMHeader>".Count();
        int end = xml.IndexOf("</Message>");
        string msg = XML_PREAMBLE + xml.Substring(start, end);
        return msg;
    }

    /// <summary>
    ///   xmlDoc - return a DOM Document representing this message header <BR>
    ///   NOT IMPLEMENTED AT THIS TIME
    /// </summary>
    /// <returns>null</returns>
    public XDocument ToDoc()
    {
        return null;
    }

    /// <summary>
    /// Populate a C2SIMHeader object from an XML string.
    /// </summary>
    /// <param name="xmlString"></param>
    /// <returns>The C2SIMHeader object that was passed as a parameter</returns>
    /// <exception cref="C2SIMClientException"</exception>
    public static C2SIMHeader PopulateC2SIM(string xmlString)
    {
        if (string.IsNullOrWhiteSpace(xmlString))
        {
            return null;
        }
        XDocument c2sDoc;
        C2SIMHeader c2s = new C2SIMHeader();
        // Parse the message
        try
        {
            c2sDoc = XDocument.Parse(xmlString);
        }
        catch (Exception e)
        {
            string emsg = "Exception while parsing input document";
            throw new C2SIMClientException(emsg, e);
        }

        /// Work through the parsed document and build a C2SIMHeader object
        XNamespace c2simNS = "http://www.sisostds.org/schemas/C2SIM/1.1";
        XElement header = c2sDoc?.Root;
        if (header.Name.LocalName != "C2SIMHeader")
        {
            // Look for a more deeply nested element
            header = header.Element(c2simNS + "C2SIMHeader");
        }


        if (header == null)
        {
            string emsg = "Error in C2SIM header - could not find C2SIMHeader element";
            throw new C2SIMClientException(emsg);
        }
        // get incoming header values and set corresponding in new header
        string temp;
        temp = header.Element(c2simNS + "CommunicativeActTypeCode")?.Value;
        if (!(temp == null))
            c2s.CommunicativeActTypeCode = temp;
        temp = header.Element(c2simNS + "ConversationID")?.Value;
        if (!(temp == null))
            c2s.ConversationID = temp;
        temp = header.Element(c2simNS + "MessageID")?.Value;
        if (!(temp == null))
            c2s.MessageID = temp;
        temp = header.Element(c2simNS + "Protocol")?.Value;
        if (!(temp == null))
            c2s.Protocol = temp;
        temp = header.Element(c2simNS + "ProtocolVersion")?.Value;
        if (!(temp == null))
            c2s.ProtocolVersion = temp;
        temp = header.Element(c2simNS + "ReplyToSystem")?.Value;
        if (!(temp == null))
            c2s.ReplyToSystem = temp;
        temp = header.Element(c2simNS + "SendingTime")?.Value;
        if (!(temp == null))
            c2s.SendingTime = temp;
        temp = header.Element(c2simNS + "FromSendingSystem")?.Value;
        if (!(temp == null))
            c2s.FromSendingSystem = temp;
        temp = header.Element(c2simNS + "ToReceivingSystem")?.Value;
        if (!(temp == null))
            c2s.ToReceivingSystem = temp;
        temp = header.Element(c2simNS + "InReplyToMessageID")?.Value;
        if (!(temp == null))
            c2s.InReplyToMessageID = temp;
        return c2s;
    }

    /// <summary>
    /// XML representation of the C2SIM header object
    /// </summary>
    /// <returns>XML string containing the contents of this C2SIM header object</returns>
    public string ToXMLString()
    {
        string xmlString = "<C2SIMHeader>";
        // CommunicativeActTypeCode
        if (!string.IsNullOrWhiteSpace(_communicativeActTypeCode))
            xmlString += "<CommunicativeActTypeCode>" +
                _communicativeActTypeCode + "</CommunicativeActTypeCode>";
        // SecurityClassificationCode
        if (!string.IsNullOrWhiteSpace(_securityClassificationCode))
            xmlString += "<SecurityClassificationCode>" +
                _securityClassificationCode + "</securityClassificationCode>";
        // ConversationID
        if (!string.IsNullOrWhiteSpace(_conversationID))
            xmlString += "<ConversationID>" + _conversationID + "</ConversationID>";
        // messageID
        if (!string.IsNullOrWhiteSpace(_messageID))
            xmlString += "<MessageID>" + _messageID + "</MessageID>";
        // protocol
        if (!string.IsNullOrWhiteSpace(_protocol))
            xmlString += "<Protocol>" + _protocol + "</Protocol>";
        // ProtocolVerion
        if (!string.IsNullOrWhiteSpace(_protocolVersion))
            xmlString += "<ProtocolVersion>" + _protocolVersion + "</ProtocolVersion>";
        // ReplyToSystem
        if (!string.IsNullOrWhiteSpace(_replyToSystem))
            xmlString += "<ReplyToSystem>" + _replyToSystem + "</ReplyToSystem>";
        // SendingTime
        if (!string.IsNullOrWhiteSpace(_sendingTime))
            xmlString += "<SendingTime>" + _sendingTime + "</SendingTime>";
        // FromSendingSystem
        if (!string.IsNullOrWhiteSpace(_fromSendingSystem))
            xmlString += "<FromSendingSystem>" +
                _fromSendingSystem + "</FromSendingSystem>";
        // ToReceivingSystem
        if (!string.IsNullOrWhiteSpace(_toReceivingSystem))
            xmlString += "<ToReceivingSystem>" +
                _toReceivingSystem + "</ToReceivingSystem>";
        // InReplyTo - Message ID
        if (!string.IsNullOrWhiteSpace(_inReplyToMessageID))
            xmlString += "<InReplyToMessageID>" +
                _inReplyToMessageID + "</InReplyToMessageID>";
        xmlString += "</C2SIMHeader>";
        return xmlString;
    }

    /// <summary>
    /// Generate a new Conversation ID (UIID format) 
    /// for this C2SIM Header
    /// </summary>
    public void GenerateConversationID()
    {
        _conversationID = Guid.NewGuid().ToString();
    }

    /// <summary>
    /// Generate ID for this C2SIM Message Header
    /// </summary>
    public void GenerateMessageID()
    {
        _messageID = Guid.NewGuid().ToString();
    }
    #endregion

    #region Public properties
    /// <summary>
    /// Indicates processing/performative requested for this message
    /// </summary>
    public string CommunicativeActTypeCode { get => _communicativeActTypeCode; set => this._communicativeActTypeCode = value; }

    /// <summary>
    /// Security classification (UUID) code
    /// </summary>
    public string SecurityClassificationCode { get => _securityClassificationCode; set => this._securityClassificationCode = value; }

    /// <summary>
    /// UUID used to connect a series of messages into one conversation
    /// </summary>
    public string ConversationID { get => _conversationID; set => this._conversationID = value; }

    /// <summary>
    /// C2SIM header message ID
    /// </summary>
    public string MessageID { get => _messageID; set => this._messageID = value; }

    /// <summary>
    /// C2SIM protocol
    /// </summary>
    public string Protocol { get => _protocol; set => this._protocol = value; }

    /// <summary>
    /// Protocol version
    /// </summary>
    public string ProtocolVersion { get => _protocolVersion; set => this._protocolVersion = value; }

    /// <summary>
    /// Id of the system to be replied to
    /// </summary>
    public string ReplyToSystem { get => _replyToSystem; set => this._replyToSystem = value; }

    /// <summary>
    /// Sending Time - time  C2SIMHeader object was instantiated in ISO Format 
    /// </summary>
    public string SendingTime { get => _sendingTime; set => this._sendingTime = value; }

    /// <summary>
    /// ID of system where message originated 
    /// </summary>
    public string FromSendingSystem { get => _fromSendingSystem; set => this._fromSendingSystem = value; }

    /// <summary>
    /// ID of system this message is directed to
    /// </summary>
    public string ToReceivingSystem { get => _toReceivingSystem; set => this._toReceivingSystem = value; }

    /// <summary>
    /// ID of the message being replied to
    /// </summary>
    public string InReplyToMessageID { get => _inReplyToMessageID; set => this._inReplyToMessageID = value; }
    #endregion

}
