using System;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;

namespace C2SIM;

public interface IC2SIMSDK
{
    string RestEndpoint { get; }
    string StompEndpoint { get; }
    string Protocol { get; }
    string ProtocolVersion { get; }

    event EventHandler<C2SIMSDK.C2SIMNotificationEventParams> StatusChangdReceived;
    event EventHandler<C2SIMSDK.C2SIMNotificationEventParams> InitializationReceived;
    event EventHandler<C2SIMSDK.C2SIMNotificationEventParams> OderReceived;
    event EventHandler<C2SIMSDK.C2SIMNotificationEventParams> ReportReceived;
    event EventHandler<Exception> Error;

    Task AssertStatus(C2SIMSDK.C2SIMServerStatus postCondition);
    Task Connect();
    Task Disconnect();
    Task<C2SIMSDK.C2SIMServerStatus> GetStatus();
    Task<string> JoinSession();
    Task<string> PushCommand(C2SIMSDK.C2SIMCommands command);
    Task<string> PushInitializationMessage(string xmlMessage);
    Task<string> PushMessage(string xmlMessage, string performative);
    Task<string> PushOrderMessage(string xmlMessage);
    Task<string> PushReportMessage(string xmlMessage);
    Task ResetToInitializing();
    Task StompPublish(string cmd, List<string> headers, string xml);
    Task SwitchToRunning();
}
