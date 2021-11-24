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
    /// <param name="submitterId"></param>
    /// <param name="restUrl"></param>
    /// <param name="restPassword"></param>
    /// <param name="stompUrl"></param>
    /// <param name="protocol"></param>
    /// <param name="protocolVersion"></param>
    public C2SIMSDKSettings(string submitterId, string restUrl, string restPassword, string stompUrl, string protocol, string protocolVersion)
    {
        SubmitterId = submitterId;
        RestUrl = restUrl;
        RestPassword = restPassword;
        StompUrl = stompUrl;
        Protocol = protocol;
        ProtocolVersion = protocolVersion;
    }
    
    /// <summary>
    /// Parameterless constructor - used by Dependency Injection
    /// </summary>
    public C2SIMSDKSettings()
    { 
    }
}
