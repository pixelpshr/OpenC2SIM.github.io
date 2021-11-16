using System.Xml.Linq;

namespace C2SimClientLib;

/// <summary>
/// C2SIMHeader Interface
/// </summary>
public interface IC2SIMHeader
{
    /// <summary>
    /// Communicative act
    /// </summary>
    /// <remarks>
    /// One of the values 
    /// from enumCommunicativeActCategoryCode indicating type of message
    /// </remarks>
    string CommunicativeActTypeCode { get; set; }
    /// <summary>
    /// Unique identifier for the conversation. 
    /// </summary>
    /// <remarks>
    /// Should be kept identical for all replies.
    ///   The conversationID may be used to associate a number of 
    /// messages into a logical grouping.
    /// </remarks>
    string ConversationID { get; set; }
    /// <summary>
    /// ID of sending system (UUID)
    /// </summary>
    string FromSendingSystem { get; set; }
    /// <summary>
    /// ID of message being replied to (UUID)
    /// </summary>
    string InReplyToMessageID { get; set; }
    /// <summary>
    /// Unique identifier for the message. 
    /// </summary>
    /// <remarks>
    /// Other messages refer to the message using this ID. 
    /// </remarks>
    string MessageID { get; set; }
    /// <summary>
    /// The protocol of the this message
    /// </summary>
    string Protocol { get; set; }
    /// <summary>
    /// The version of the protocol of the this message
    /// </summary>
    string ProtocolVersion { get; set; }
    /// <summary>
    /// Specifies what system to reply to
    /// </summary>
    string ReplyToSystem { get; set; }
    /// <summary>
    /// Indicates the security classification of this message
    /// </summary>
    string SecurityClassificationCode { get; set; }
    /// <summary>
    /// Sending Time - ISO DateTime format yyyy-MM-ddTHH:mm:ssZ
    /// </summary>
    string SendingTime { get; set; }
    /// <summary>
    /// ID of destination system (UUID))
    /// </summary>
    string ToReceivingSystem { get; set; }

    /// <summary>
    /// Generate a new Conversation ID (UIID format) for this C2SIM Header
    /// </summary>
    void GenerateConversationID();
    /// <summary>
    /// Generate a new Message ID (UIID format) for this C2SIM Header
    /// </summary>
    void GenerateMessageID();
    /// <summary>
    ///   Return a DOM Document representing this message header 
    /// </summary>
    /// <remarks>
    ///   NOT IMPLEMENTED AT THIS TIME
    /// </remarks>
    XDocument ToDoc();
     /// <summary>
    /// XML representation of the C2SIM header object
    /// </summary>
    /// <returns>XML string containing the contents of this C2SIM header object</returns>
   string ToXMLString();
}
