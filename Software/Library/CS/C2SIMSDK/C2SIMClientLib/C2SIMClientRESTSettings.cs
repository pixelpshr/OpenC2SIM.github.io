namespace C2SimClientLib;

/// <summary>
/// REST services settings
/// </summary>
public class C2SIMClientRESTSettings
{
    /// <summary>
    /// Identifier of the client submitting requests
    /// </summary>
    public string SubmitterId { get; set; }
    /// <summary>
    /// REST service host
    /// </summary>
    public string Host { get; set; }
    /// <summary>
    /// REST service port
    /// </summary>
    public string Port { get; set; }
    /// <summary>
    /// REST service path (e.g. C2SIMServer
    /// </summary>
    public string Path { get; set; }
    /// <summary>
    /// Type of message request (INFORM, ORDER, REPORT) 
    /// NOTE: The schema has a different enumeration: Accept, Agree, Confirm, Inform, 
    /// Propose, Refuse, Request
    /// </summary>
    public string Performative { get; set; }
    /// <summary>
    /// "SISO-STD-C2SIM" (or "BML")
    /// </summary>
    public string Protocol { get; set; }
    /// <summary>
    /// "1.0.0" for published C2SIM standard, or legacy version (e.g. v9="0.0.9")
    /// </summary>
    public string ProtocolVersion { get; set; }

    /// <summary>
    /// Construct Settings object
    /// </summary>
    /// <param name="submitterId"></param>
    /// <param name="host"></param>
    /// <param name="port"></param>
    /// <param name="path"></param>
    /// <param name="performative"></param>
    /// <param name="protocol"></param>
    /// <param name="protocolVersion"></param>
    public C2SIMClientRESTSettings(string submitterId, string host, string port, string path, string performative, string protocol, string protocolVersion)
    {
        SubmitterId = submitterId;
        Host = host;
        Port = port;
        Path = path;
        Performative = performative;
        Protocol = protocol;
        ProtocolVersion = protocolVersion;
    }

}

