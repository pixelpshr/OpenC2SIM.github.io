namespace C2SimClientLib;

/// <summary>
/// C2Sim exception wrapper - may contain details in an InnerException indicating the real cause
/// </summary>
public class C2SIMClientException : Exception
{
    /// <summary>
    /// Constructor (no Inner Exception)
    /// </summary>
    /// <param name="msg">Exception description</param>
    public C2SIMClientException(string msg) : base(msg)
    {
    }

    /// <summary>
    /// Constructor (no Inner Exception)
    /// </summary>
    /// <param name="msg">Exception description</param>
    /// <param name="e">Inner Exception</param>
    public C2SIMClientException(string msg, Exception e) : base(msg, e)
    {
    }
}