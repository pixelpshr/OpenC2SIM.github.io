namespace C2SimClientLib;

/// <summary>
/// STOMP service settings
/// </summary>
public class C2SIMClientSTOMPSettings
{
    /// <summary>
    /// STOMP service host
    /// </summary>
    public string Host { get; set; }
    /// <summary>
    /// STOMP service port
    /// </summary>
    public string Port { get; set; }
    /// <summary>
    /// STOMP service topic/destination
    /// </summary>
    public string Destination { get; set; }

    /// <summary>
    ///  Server heart beat message frequency in milliseconds
    /// </summary>
    public int ServerHeartBeat { get; set; }


    /// <summary>
    /// Construct Settings object
    /// </summary>
    /// <param name="host"></param>
    /// <param name="port"></param>
    /// <param name="destination"></param>
    /// <param name="serverHeartBeat"></param>
    public C2SIMClientSTOMPSettings(string host, string port, string destination, int serverHeartBeat = 100000)
    {
        Host = host;
        Port = port;
        Destination = destination;
        ServerHeartBeat = serverHeartBeat;
    }
}
