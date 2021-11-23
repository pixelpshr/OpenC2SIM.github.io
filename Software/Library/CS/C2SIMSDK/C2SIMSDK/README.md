# C2SIM SDK

Resources for controlling state and handling messages in a C2SIM environment. For an overview of C2SIM and its capabilities to support multiple connected Command and Control (C2) and Simulation systems, see the [C2SIM Server Reference Implementation Documentation](https://bit.ly/30y40RI)

The focus of the SDK is specifically on the C2SIM protocol, rather than legacy ones (e.g. CBML, IBML, IBML9, MSDL) which may be supported by C2SIM environments as well.

The SDK is built on top of generic capabilities offered by a [(ported) Client Library](https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Library/CS/C2SIMSDK/C2SIMClientLib), configured with specific parameters obtained by examining the 
[C2SIM GUI Editor code](https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Client/C2SIMGUIv2.10.9) 
use of the [Java C2SIMClientLib](https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Library/Java/C2SIMClientLib).

In a nutshell, the SDK:

- Wraps a series of basic Library Commands required to make a C2SIM server transition to a state where it is ready to accept Initializations, or Orders, or to join an ongoing session
- Issues Library messages with required parameters for pushing specific messages containing Orders, Initializations and Reports
- Serializes and deserializes XML messages from/into plain old C# objects
- Exposes generic Library (STOMP) notification messages as finer grained events signaling the reception of Initialization, 
Orders, Reports, and server status changes


## Nuget Install

The [HyssosTeck.Sdk.C2SIM](https://www.nuget.org/packages/HyssosTech.Sdk.C2SIM/) nuget provides support for: 

* .NET 6 projects
* .NET Standard 2.0 - compatible with recent .NET Framework projects

As such, it should be compatible with Windows, macOs and Linux platforms, provided these have the required [.NET Runtime/SDK](https://dotnet.microsoft.com/download/dotnet/6.0) installed

## Quick Start

1. Create a `C2SIMSDK` object pointing to a C2SIM server

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

    *NOTE*: the SDK supports Dependency Injection as part of a [.NET Generic Host](https://docs.microsoft.com/en-us/dotnet/core/extensions/generic-host), which simplifies handling of logging and configuration settings. See the [Sample App](https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Library/CS/C2SIMSDK/C2SIMSDKSampleApp) for an example of how to implement that.


1. Subscribe to notification events

    ```cs
    c2SimSDK.StatusChangedReceived += C2SimSDK_StatusChangedReceived;
    c2SimSDK.InitializationReceived += C2SimSDK_InitializationReceived;
    c2SimSDK.OderReceived += C2SimSDK_OderReceived;
    c2SimSDK.ReportReceived += C2SimSDK_ReportReceived;
    ```

1. Establish the connection to the C2SIM notification service to start the message flow

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

    See the [C2SIM .NET SDK Reference](<https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Library/CS/C2SIMSDK/docs/C2SIM .NET SDK Reference.docx>) for details of the methods summarized below

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

The methods above are used in a [sample app](https://github.com/hyssostech/OpenC2SIM.github.io/tree/master/Software/Library/CS/C2SIMSDK/C2SIMSDKSampleApp), which accepts commands and messages interactively and sends them to a server.
The app also displays notifications received from a server, and can provide insight into the traffic generated by the sample itself or other apps
connected to the same server, e.g. the [Sketch-Thru-Plan Planning Workstation](http://www.hyssos.com), as can be seen in this [demo](https://vimeo.com/641689328).

## Objects generated from the C2SIM XSD

Classes representing the four main types of C2SIM messages were generated from the [Schemas](Schema) using the `xsd` tool:

```
xsd schemas\C2SIM_SMX_LOX_V1.0.1.xsd /c /l:CS
```

`MessageBody.Item` contains a reference to one of the messages of interest, differentiated by the following types:

* Schemas.SystemCommandBodyType - Command results notifications (see the NOTE below)
* Schemas.C2SIMInitializationBodyType - Initialization message notification
* Schemas.DomainMessageBodyType - Contains sub-types of interest
    * Schemas.OrderBodyType - Order message notification
    * Schemas.ReportBodyType - Report message notification

*NOTE*: `SystemCommandBodyType`'s schema definition does not match the actual content of messages received from 
the current C2SIM Reference Server (v4.8.0.11):
* `SystemCommandBodyType` includes an additional element - `SessionStateCode`
* `SystemCommandTypeCodeType` includes an additional `ResetScenario` enumeration 

A separate augmented `C2SIM_SMX_LOX_v1.0.0_Command.xsd` containing these elements has been created, and a corresponding set of classes is used here instead of the standard one so that the server responses can be processed.

```
xsd schemas\C2SIM_SMX_LOX_v1.0.0_Command.xsd /c /l:CS /n:CustomSchemas
```
