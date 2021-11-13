using System.Xml.Linq;

namespace C2SimClientLib;

public interface IC2SIMHeader
{
    string CommunicativeActTypeCode { get; set; }
    string ConversationID { get; set; }
    string FromSendingSystem { get; set; }
    string InReplyToMessageID { get; set; }
    string MessageID { get; set; }
    string Protocol { get; set; }
    string ProtocolVersion { get; set; }
    string ReplyToSystem { get; set; }
    string SecurityClassificationCode { get; set; }
    string SendingTime { get; set; }
    string ToReceivingSystem { get; set; }

    void GenerateConversationID();
    void GenerateMessageID();
    XDocument ToDoc();
    string ToXMLString();
}
