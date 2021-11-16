using System.Xml.Linq;
using C2SimClientLib;

namespace C2SIM;

/// <summary>
///  Wrap the Library header to avoid an extra reference in code using the SDK
/// </summary>
public class NotificationHeader : IC2SIMHeader
{
    private readonly C2SIMHeader _h;

    /// <summary>
    /// Communicative act
    /// </summary>
    /// <remarks>
    /// One of the values 
    /// from enumCommunicativeActCategoryCode indicating type of message
    /// </remarks>
    public string CommunicativeActTypeCode { get => _h.CommunicativeActTypeCode; set => _h.CommunicativeActTypeCode = value; }
    /// <summary>
    /// Unique identifier for the conversation. 
    /// </summary>
    /// <remarks>
    /// Should be kept identical for all replies.
    ///   The conversationID may be used to associate a number of 
    /// messages into a logical grouping.
    /// </remarks>
    public string ConversationID { get => _h.ConversationID; set => _h.ConversationID = value; }
    /// <summary>
    /// ID of sending system (UUID)
    /// </summary>
    public string FromSendingSystem { get => _h.FromSendingSystem; set => _h.FromSendingSystem = value; }
    /// <summary>
    /// ID of message being replied to (UUID)
    /// </summary>
    public string InReplyToMessageID { get => _h.InReplyToMessageID; set =>_h.InReplyToMessageID = value; }
    /// <summary>
    /// Unique identifier for the message. 
    /// </summary>
    /// <remarks>
    /// Other messages refer to the message using this ID. 
    /// </remarks>
    public string MessageID { get => _h.MessageID; set => _h.MessageID = value; }
    /// <summary>
    /// The protocol of the this message
    /// </summary>
    public string Protocol { get => _h.Protocol; set => _h.Protocol = value; }
    /// <summary>
    /// The version of the protocol of the this message
    /// </summary>
    public string ProtocolVersion { get => _h.ProtocolVersion; set => _h.ProtocolVersion = value; }
    /// <summary>
    /// Specifies what system to reply to
    /// </summary>
    public string ReplyToSystem { get => _h.ReplyToSystem; set => _h.ReplyToSystem = value; }
    /// <summary>
    /// Indicates the security classification of this message
    /// </summary>
    public string SecurityClassificationCode { get => _h.SecurityClassificationCode; set => _h.SecurityClassificationCode = value; }
    /// <summary>
    /// Sending Time - ISO DateTime format yyyy-MM-ddTHH:mm:ssZ
    /// </summary>
    public string SendingTime { get => _h.SendingTime; set => _h.SendingTime = value; }
    /// <summary>
    /// ID of destination system (UUID))
    /// </summary>
    public string ToReceivingSystem { get => _h.ToReceivingSystem; set => _h.ToReceivingSystem = value; }

    /// <summary>
    /// Constructor
    /// </summary>
    /// <param name="h">Header to wrap</param>
    public NotificationHeader(C2SIMHeader h)
    {
        _h = h;
    }

    /// <summary>
    /// Generate a new Conversation ID (UIID format) for this C2SIM Header
    /// </summary>
    public void GenerateConversationID()
    {
        _h.GenerateConversationID();
    }

    /// <summary>
    /// Generate a new Message ID (UIID format) for this C2SIM Header
    /// </summary>
    public void GenerateMessageID()
    {
        _h.GenerateMessageID();
    }

    /// <summary>
    ///   xmlDoc - return a DOM Document representing this message header 
    /// </summary>
    /// <remarks>
    ///   NOT IMPLEMENTED AT THIS TIME
    /// </remarks>
    /// <returns>null</returns>
    public XDocument ToDoc()
    {
        return _h.ToDoc();
    }

    /// <summary>
    /// XML representation of the C2SIM header object
    /// </summary>
    /// <returns>XML string containing the contents of this C2SIM header object</returns>
    public string ToXMLString()
    {
        return _h.ToXMLString();
    }
}
