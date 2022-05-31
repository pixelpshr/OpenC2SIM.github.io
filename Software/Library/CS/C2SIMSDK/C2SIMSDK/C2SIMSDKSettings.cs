namespace C2SIM;

/// <summary>
/// C2SIM SDK configuration settings
/// </summary>
public class C2SIMSDKSettings
{
    /// <summary>
    /// Id string of the submitter
    /// </summary>
    public string SubmitterId { get; set; }
    /// <summary>
    /// Full C2SIM server endpoint, including host:port/path, e.g. "http://10.2.10.30:8080/C2SIMServer
    /// </summary>
    public string RestUrl { get; set; }
    /// <summary>
    /// C2SIM server password
    /// </summary>
    public string RestPassword { get; set; }
    /// <summary>
    /// Full notification service (STOMP) endpoint, including host:port/destination, e.g. "http://10.2.10.30:61613/topic/C2SIM"
    /// </summary>
    public string StompUrl { get; set; }
    /// <summary>
    ///  STOMP server heart beat message frequency in milliseconds
    /// </summary>
    public int StompHeartBeat { get; set; }
    /// <summary>
    /// SISO-STD-C2SIM" (or "BML")
    /// </summary>
    public string Protocol { get; set; }
    /// <summary>
    /// "1.0.0" for published standard, or legacy version (e.g. v9="0.0.9")
    /// </summary>
    public string ProtocolVersion { get; set; }

    /// <summary>
    /// Construct Settings object
    /// </summary>
    /// <param name="submitterId">Id string of the submitter</param>
    /// <param name="restUrl">Full C2SIM server endpoint, including host:port/path, e.g. "http://10.2.10.30:8080/C2SIMServer</param>
    /// <param name="restPassword">C2SIM server password</param>
    /// <param name="stompUrl">Full notification service (STOMP) endpoint, including host:port/destination, e.g. "http://10.2.10.30:61613/topic/C2SIM"</param>
    /// <param name="protocol"> SISO-STD-C2SIM" (or "BML")</param>
    /// <param name="protocolVersion">"1.0.x" for published standard, or legacy version (e.g. v9="0.0.9")</param>
    /// <param name="stompHeartBeat">Frequency of heartbeat/keepalive messages from the STOMP server</param>
    public C2SIMSDKSettings(string submitterId, string restUrl, string restPassword, string stompUrl, string protocol, string protocolVersion, int stompHeartBeat=10000)
    {
        SubmitterId = submitterId;
        RestUrl = restUrl;
        RestPassword = restPassword;
        StompUrl = stompUrl;
        StompHeartBeat = stompHeartBeat;
        Protocol = protocol;
        ProtocolVersion = protocolVersion;
    }
    
    /// <summary>
    /// Parameterless constructor - used by Dependency Injection
    /// </summary>
    public C2SIMSDKSettings()
    {
        // Set the defaults to use in case the corresponding keys are missing from appsettings.json
        Protocol = "SISO-STD-C2SIM";
        ProtocolVersion = "1.0.2";
        StompHeartBeat = 10000; 
    }
}
