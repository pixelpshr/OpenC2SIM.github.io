using System.Xml.Linq;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Console;
using C2SIM;

/// <summary>
/// Accepts user commands and executes them, showing server responses and async notificaitons
/// Accepts:
/// - Server commands:
///     STOP, RESET, INITIALIZE, SHARE, START, PAUSE, STATUS, QUERYINIT
/// - Message push commands:
///     PUSH path to xml file containing a C2SIM formatted Initialization, Order or Report message
///     The state of the C2SIM server is automatically transitioned to Initializing and Running
///     before Initialization and Orders/Reports are sent, respectivelly
///     NOTE: this is fine for servers that are not being shared - for a shared server, coordination needs to 
///     take place so that this app does not overrun the state expected by other clients
/// - QUIT to exit
/// </summary>
class C2SIMConsole : IHostedService
{
    private ILogger _logger { get; }
    private readonly IHostApplicationLifetime _appLifetime; 
    private IC2SIMSDK _c2SimSDK { get; }

    /// <summary>
    /// Create a C2SIM console service object
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="appLifetime"></param>
    /// <param name="c2SimSDK"></param>
    public C2SIMConsole(ILogger<C2SIMConsole> logger, IHostApplicationLifetime appLifetime, IC2SIMSDK c2SimSDK)
    {
        _logger = logger;
        _appLifetime = appLifetime;
        _c2SimSDK = c2SimSDK;
    }

    /// <summary>
    /// Get the service started
    /// </summary>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    public Task StartAsync(CancellationToken cancellationToken)
    {
        // Subscribe to C2SIM notification (STOMP) events
        _c2SimSDK.StatusChangedReceived += C2SimSDK_StatusChangdReceived;
        _c2SimSDK.InitializationReceived += C2SimSDK_InitializationReceived;
        _c2SimSDK.OderReceived += C2SimSDK_OderReceived;
        _c2SimSDK.ReportReceived += C2SimSDK_ReportReceived;
        _c2SimSDK.Error += C2SimSDK_Error;

        // Run the interactive session on ApplicationStart, accepting and executing user commands 
        // until cancelled or user enters the "quit" command
        Task.Run(async () =>
        {
            // Connect to start receiving notifications
            try
            {
                Console.WriteLine("Connecting to C2SIM services...");
                await _c2SimSDK.Connect();
            }
            catch (Exception e)
            {
                string msg = e.InnerException != null ? e.InnerException.Message : e.Message;
                _logger.LogError($"Error connecting to notification server: {msg}\nServices may not be available");
            }

            // Display context and instructions
            Console.WriteLine($"REST endpoint: {_c2SimSDK.RestEndpoint}");
            Console.WriteLine($"Notifications endpoint: {_c2SimSDK.StompEndpoint}");
            Console.WriteLine($"{_c2SimSDK.Protocol} v{_c2SimSDK.ProtocolVersion}");
            DisplayCommands();

            // Process user commands
            while (!cancellationToken.IsCancellationRequested)
            {
                try
                {
                    Prompt();
                    string cmd = Console.ReadLine();
                    if (cmd.Equals("quit", StringComparison.InvariantCultureIgnoreCase))
                    {
                        break;
                    }
                    else if (cmd.StartsWith("push", StringComparison.InvariantCultureIgnoreCase))
                    {
                        (string type, string xmlMessage) = LoadMessage(cmd);
                        if (xmlMessage != null)
                        {
                            string resp = null;
                            if (type.Equals("initialization", StringComparison.InvariantCultureIgnoreCase) || type.Equals("init", StringComparison.InvariantCultureIgnoreCase))
                            {
                                await _c2SimSDK.ResetToInitializing();
                                resp = await _c2SimSDK.PushInitializationMessage(xmlMessage);
                            }
                            else if (type.Equals("order", StringComparison.InvariantCultureIgnoreCase))
                            {
                                await _c2SimSDK.SwitchToRunning();
                                resp = await _c2SimSDK.PushOrderMessage(xmlMessage);
                            }
                            else if (type.Equals("report", StringComparison.InvariantCultureIgnoreCase))
                            {
                                await _c2SimSDK.SwitchToRunning();
                                resp = await _c2SimSDK.PushReportMessage(xmlMessage);
                            }
                            else
                            {
                                Console.WriteLine("Message type should be Init, Order or Report");
                            }
                            if (!string.IsNullOrWhiteSpace(resp))
                            {
                                Console.WriteLine(FormatResponse(resp));
                            }
                        }
                    }
                    else if (Enum.TryParse<C2SIMSDK.C2SIMCommands>(cmd.ToUpperInvariant(), out C2SIMSDK.C2SIMCommands c2SimCmd))
                    {
                        string resp = await _c2SimSDK.PushCommand(c2SimCmd);
                        Console.WriteLine(FormatResponse(resp));
                    }
                    else
                    {
                        DisplayCommands();
                        continue;
                    }
                }
                catch (OperationCanceledException)
                {
                    // Just ignore - task will be canceled on app exit
                }
                catch (AggregateException ae)
                {
                    foreach (var e in ae.InnerExceptions)
                    {
                        Console.WriteLine($"Error: {e.Message}\n");
                        Console.WriteLine();
                    }
                }
                catch (Exception e)
                {
                    string msg = e.InnerException != null ? e.InnerException.Message : e.Message;
                    _logger.LogError($"Error: {msg}\n");
                }
            }
            // Request termination
            _appLifetime.StopApplication();
        }, cancellationToken);
        return Task.CompletedTask;
    }

    /// <summary>
    /// Service is ending
    /// </summary>
    /// <param name="cancellationToken"></param>
    /// <returns></returns>
    public Task StopAsync(CancellationToken cancellationToken)
    {
        return Task.CompletedTask;
    }

    #region Event handlers
    /// <summary>
    /// Server changed status
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    static void C2SimSDK_StatusChangdReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimCommand.MessageBodyType)e.Body);
    }

    /// <summary>
    /// Initializaiton message was received
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    static void C2SimSDK_InitializationReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimInit.MessageBodyType)e.Body);
    }

    /// <summary>
    /// Order message was received
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    static void C2SimSDK_OderReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimOrder.MessageBodyType)e.Body);
    }

    /// <summary>
    /// Report message received
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    static void C2SimSDK_ReportReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimReport.MessageBodyType)e.Body);
    }

    /// <summary>
    /// Pipes XML to the console for display
    /// </summary>
    /// <param name="e"></param>
    static void DisplayXml(object e)
    {
        Console.WriteLine();
        Console.Write("Server Notification: ");
        System.Xml.Serialization.XmlSerializer x = new System.Xml.Serialization.XmlSerializer(e.GetType());
        x.Serialize(Console.Out, e);
        Console.WriteLine();
        Prompt();
    }

    /// <summary>
    /// Error notification received 
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    static void C2SimSDK_Error(object sender, Exception e)
    {
        Console.WriteLine($"Error processing C2SIM messages: {e.Message}. Application restart is recommended");
    }
    #endregion

    #region Methods
    /// <summary>
    /// Load a message from file
    /// </summary>
    /// <param name="cmd"></param>
    /// <returns></returns>
    /// <exception cref="ArgumentException"></exception>
    static (string, string) LoadMessage(string cmd)
    {
        // Get path from second parameter
        string[] parts = cmd.Split(' ');
        if (parts.Count() != 3)
        {
            throw new ArgumentException("Expected PUSH init|order|report <path to xml>");
        }
        string msgType = parts[1].Trim();
        string path = parts[2].Trim();
        if (!File.Exists(path))
        {
            throw new ArgumentException($"Could find xml file at {path}");
        }
        string xmlMessage;
        try
        {
            xmlMessage = File.ReadAllText(path);
        }
        catch (Exception e)
        {
            throw new ArgumentException($"Could not load xml file from {path}: {e.Message}");
        }
        return (msgType, xmlMessage);
    }

    /// <summary>
    /// Display list of commands 
    /// </summary>
    static void DisplayCommands()
    {
        Console.WriteLine($"Commands: {string.Join(", ", Enum.GetNames(typeof(C2SIMSDK.C2SIMCommands))) + ", PUSH init|order|report <path to xml>, QUIT"}");
    }

    /// <summary>
    /// Prompt user for input
    /// </summary>
    static void Prompt()
    {
        Console.Write("Command>");
    }

    /// <summary>
    /// Prettyprint xml
    /// </summary>
    /// <param name="resp"></param>
    /// <returns></returns>
    static string FormatResponse(string resp)
    {
        // Try to format as xml, returning formatted string
        try
        {
            return XDocument.Parse(resp).ToString();
        }
        catch (Exception)
        {
            // Not xml - just ignore
        }
        // Return the string itself with no formatting
        return resp;
    }
    #endregion
}
