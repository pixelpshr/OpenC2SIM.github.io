/// C2SIM SDK Sample App
/// Copyright Hyssos Tech

using System.Linq;
using System.Xml.Linq;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Console;
using C2SIM;

#region Main method
// Create and run the C2SIM console service
// For an overview of the dependency injection, logging and configuration enacted
// behind the scenes, see for example:
// https://docs.microsoft.com/en-us/dotnet/core/extensions/generic-host 
// https://snede.net/get-started-with-net-generic-host/
// https://dfederm.com/building-a-console-app-with-.net-generic-host/
await CreateHostBuilder(args).Build().RunAsync();

/// <summary>
/// Create a  builder providing default configuration loading, logging, lifecycle
/// </summary>
static IHostBuilder CreateHostBuilder(string[] args) 
{
    // Create the main console service, passing in Logger and C2SIM SDK object configured
    // according to appsettings.json parameters, which can be overwritten by command line arguments
    return Host.CreateDefaultBuilder(args)
        .ConfigureServices((hostContext, services) =>
        {
            services
                .AddHostedService<C2SIMConsole>()
                .AddSingleton<IC2SIMSDK, C2SIMSDK>();
            services.AddOptions<C2SIMSDKSettings>()
                .Bind(hostContext.Configuration.GetSection("Application"));
        });
}
#endregion

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
/// 
/// </summary>
class C2SIMConsole : IHostedService
{
    private ILogger _logger { get; }
    private IC2SIMSDK _c2SimSDK { get; }

    public C2SIMConsole(ILogger<C2SIMConsole> logger, IC2SIMSDK c2SimSDK)
    {
        _logger = logger;
        _c2SimSDK = c2SimSDK;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        // Subscribe to C2SIM notification (STOMP) events
        _c2SimSDK.StatusChangdReceived += C2SimSDK_StatusChangdReceived;
        _c2SimSDK.InitializationReceived += C2SimSDK_InitializationReceived;
        _c2SimSDK.OderReceived += C2SimSDK_OderReceived;
        _c2SimSDK.ReportReceived += C2SimSDK_ReportReceived;
        _c2SimSDK.Error += C2SimSDK_Error;

        // Run the interactive session, accepting and executing user commands 
        // until cancelled or user enters the "quit" command
        return Task.Run(async () =>
        {
            // Connect to start receiving notifications
            try
            {
                await _c2SimSDK.Connect();
            }
            catch (Exception e)
            {
                string msg = e.InnerException != null ? e.InnerException.Message : e.Message;
                Console.WriteLine($"Error connecting to notification server: {msg}\n");
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
                    Console.WriteLine($"Error: {msg}\n");
                }
            }
        }, cancellationToken);
    }
    public Task StopAsync(CancellationToken cancellationToken)
    {
        return Task.CompletedTask;
    }

    #region Event handlers
    static void C2SimSDK_StatusChangdReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimCommand.MessageBodyType)e.Body);
    }

    static void C2SimSDK_InitializationReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimInit.MessageBodyType)e.Body);
    }

    static void C2SimSDK_OderReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimOrder.MessageBodyType)e.Body);
    }

    static void C2SimSDK_ReportReceived(object sender, C2SIMSDK.C2SIMNotificationEventParams e)
    {
        DisplayXml((C2SimReport.MessageBodyType)e.Body);
    }

    static void DisplayXml(object e)
    {
        Console.WriteLine();
        Console.Write("Server Notification: ");
        System.Xml.Serialization.XmlSerializer x = new System.Xml.Serialization.XmlSerializer(e.GetType());
        x.Serialize(Console.Out, e);
        Console.WriteLine();
        Prompt();
    }

    static void C2SimSDK_Error(object sender, Exception e)
    {
        Console.WriteLine($"Error processing C2SIM messages: {e.Message}. Application restart is recommended");
    }
    #endregion

    #region Methods
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

    static void DisplayCommands()
    {
        Console.WriteLine($"Commands: {string.Join(", ", Enum.GetNames(typeof(C2SIMSDK.C2SIMCommands))) + ", PUSH init|order|report <path to xml>, QUIT"}");
    }

    static void Prompt()
    {
        Console.Write("Command>");
    }

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
