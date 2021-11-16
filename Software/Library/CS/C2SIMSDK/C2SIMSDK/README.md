# C2SIM SDK

Resources for controlling C2SIM servers and handling C2SIM messages. This class is built on top of the [C2SIMClientLib](https://github.com/hyssostech/OpenC2SIM.github.io/tree/0def573bcde1e3ff40248bc327775200e6eba095/Software/Library/CS/C2SIMSDK/C2SIMClientLib), and 
encapsulates common parameters required to interact with a C2SIM environment running a REST and STOMP services, and potentially 
connected to other systems, such as C2 and Simulators. For an overview of this interaction, see the 
[C2SIM Server Reference Implementation Documentation](https://bit.ly/30y40RI)


The focus is specifically on the C2SIM protocol, rather than legacy ones (e.g. CBML, IBML, IBML9, MSDL) which may be supported by C2SIM environments as well.

The methods in this class make use of generic capabilities offered by the (ported) Client Library to offer higher-level functionality, 
for example:

- Wrapping a series of basic Library Commands to make a server transition to a state where it is ready to accept Initializations,
or Orders, or to join an ongoing session
- Issuing Library messages with required parameters for pushing Orders, Initializations and Reports
- Serializing and deserializing XML messages from/into plain old C# objects
- Exposing generic Library STOMP messages as finer grained events signaling the reception of Initialization, 
Orders, Reports, and server status changes

To obtain these effects, the Library functionality is invoked with specific parameters, which were obtained by examining the 
[C2SIM GUI Editor code](https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Client/C2SIMGUIv2.10.9) 
use of the Java [C2SIMClientLib](https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Library/Java/C2SIMClientLib)


## Nuget Installation

A nuget proving support for .NET 6 and .NET Standard 2.0 (compatible with versions of the .NET Framework) is available to install

.NET CLI

```
dotnet add package HyssosTeck.Sdk.C2SIM
```

Package Manager

```
Install-Package HyssosTeck.Sdk.C2SIM
```


## Quick Start

1. Create a `C2SIMSDK` object and subscribe to events

    ```cs
    // ... obtain reference to logger that should be used by the SDK
    ILogger logger = null; // Create an appropriate logger here
    C2SIMSDK c2SimSDK = new C2SIMSDK(
        logger,
        new C2SIMSDKSettings(
            // Id string of this app - use C2SIMSDK.GetMachineID() to get a unique id based on the client hardware
            <submitter id>, 
            // Full C2SIM server endpoint, including host:port/path, e.g. "http://10.2.10.30:8080/C2SIMServer"
            <rest endpoint>, 
            // C2SIM server password
            <rest password>        
            // Full STOMP service endpoint, including host:port/destination, e.g. "http://10.2.10.30:61613/topic/C2SIM"
            <stomp endpoint>, 
            // Protocol - could also be "BML" for example, but the SDK focuses on C2SIM
            "SISO-STD-C2SIM",
            // Version of the protocol - 1.0.0 is the published standard
            "1.0.0"
        )
    );
    ```

    *NOTE*: the SDK supports Dependency Injection as part os a [.NET Generic Host](https://docs.microsoft.com/en-us/dotnet/core/extensions/generic-host), which simplifies handling of logging and configuration settings. See the [Sample App](https://github.com/hyssostech/OpenC2SIM.github.io/blob/0def573bcde1e3ff40248bc327775200e6eba095/Software/Library/CS/C2SIMSDK/C2SIMSDKSampleApp) for an example of how to implement that.


1. Subscribe to notification events, implementing  handlers

    ```cs
    c2SimSDK.StatusChangdReceived += C2SimSDK_StatusChangdReceived;
    c2SimSDK.InitializationReceived += C2SimSDK_InitializationReceived;
    c2SimSDK.OderReceived += C2SimSDK_OderReceived;
    c2SimSDK.ReportReceived += C2SimSDK_ReportReceived;
    ```

1. Establish the connection to the notification service to start the message flow

    ```cs
    try
    {
        // Connect to the notification service to start receiving messages
        await c2SimSDK.Connect();
    }
    catch (Exception e)
    {
        string msg = e.InnerException != null ? e.InnerException.Message : e.Message;
        // Handle error
        // ...
    }
    ```

1. Send commands and messages as required
	1. Change the state of the server
		- ResetToInitializing() - get the server to an `Initializing` state, issuing `STOP, RESET, INITIALIZE` individual commands as needed
		- SwitchToRunning() - get the server to a `Running` state, issuing `SHARE, START` individual commands as needed 
        - JoinSession() - get the server to issue a late join notification, eventually causing an `InitializationReceived` event to be triggered 
	1. Push messages
        - PushInitializationMessage()
        - PushOrderMessage() 
        - PushReportMessage()
    1. Access to raw Library functionality
		- PushCommand() -  C2SIM server commands: STOP, RESET, INITIALIZE, SHARE, START, PAUSE, STATUS, QUERYINIT (see [C2SIM Server Message Flow](https://github.com/hyssostech/OpenC2SIM.github.io/blob/master/Software/Server/C2SIM%20Server%20Message%20Flow_20200325.pdf) for details)
        - PushMessage() - client configured XML messages

## Sample app

The methods above are used in a [sample app](https://github.com/hyssostech/OpenC2SIM.github.io/blob/0def573bcde1e3ff40248bc327775200e6eba095/Software/Library/CS/C2SIMSDK/C2SIMSDKSampleApp), which accepts commands and messages interactively and sends them to a server.
The app also displays notifications received from a server, and can provide insight into the traffic generated by the sample itself or other apps
connected to the same server (e.g. the [Sketch-Thru-Plan planning workstation](http://www.hyssos.com))

## Objects generated from the C2SIM XSD

Classes representing the four main types of C2SIM messages were generated from the [Schemas](Schema)using the `xsd` tool:

- C2SimXSDInitObject - `xsd C2SIM_SMX_LOX_v1.0.0_Init_flat.xsd /c /namespace:C2SimInit`
- C2SimXSDOrderObject - `xsd C2SIM_SMX_LOX_ASX_v1.0.0_Order_flat.xsd /c /namespace:C2SimOrder` 
- C2SimXSDReportObject - `xsd C2SIM_SMX_LOX_ASX_v1.0.0_Report_flat.xsd /c /namespace:C2SimReport`
- C2SimXSDCommandObject - `xsd C2SIM_SMX_LOX_v1.0.0_Command_flat.xsd /c /namespace:C2SimCommand`

`C2SIM_SMX_LOX_v1.0.0_Command_flat.xsd` is not part of the original OpenC2SIM repository, and it was extracted from the all 
encompassing `C2SIM_SMX_LOX_v1.0.0.xsd`. Additional elements had then to be included to 
make it comply with the actual content of messages received from a
reference server


