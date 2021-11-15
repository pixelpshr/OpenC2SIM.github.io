using System.Xml.Linq;
using C2SimClientLib;

namespace C2SIM;

/// <summary>
///  Wrap the Library header to avoid an extra reference in code using the SDK
/// </summary>
public class NotificationHeader : IC2SIMHeader
{
    private readonly C2SIMHeader _h;

    public string CommunicativeActTypeCode { get => _h.CommunicativeActTypeCode; set => _h.CommunicativeActTypeCode = value; }
    public string ConversationID { get => _h.ConversationID; set => _h.ConversationID = value; }
    public string FromSendingSystem { get => _h.FromSendingSystem; set => _h.FromSendingSystem = value; }
    public string InReplyToMessageID { get => _h.InReplyToMessageID; set =>_h.InReplyToMessageID = value; }
    public string MessageID { get => _h.MessageID; set => _h.MessageID = value; }
    public string Protocol { get => _h.Protocol; set => _h.Protocol = value; }
    public string ProtocolVersion { get => _h.ProtocolVersion; set => _h.ProtocolVersion = value; }
    public string ReplyToSystem { get => _h.ReplyToSystem; set => _h.ReplyToSystem = value; }
    public string SecurityClassificationCode { get => _h.SecurityClassificationCode; set => _h.SecurityClassificationCode = value; }
    public string SendingTime { get => _h.SendingTime; set => _h.SendingTime = value; }
    public string ToReceivingSystem { get => _h.ToReceivingSystem; set => _h.ToReceivingSystem = value; }

    public NotificationHeader(C2SIMHeader h)
    {
        _h = h;
    }

    public void GenerateConversationID()
    {
        _h.GenerateConversationID();
    }

    public void GenerateMessageID()
    {
        _h.GenerateMessageID();
    }

    public XDocument ToDoc()
    {
        return _h.ToDoc();
    }

    public string ToXMLString()
    {
        return _h.ToXMLString();
    }
}
