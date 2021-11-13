# C2SIM SDK Sample App

This app illustrates the use of the [C2SIM SDK](..). It provides a simple command line interface for interacting with C2SIM servers:

- Issue commands - send "stop", "reset", "initialize", "share", "start", "queryinit", "status" 
- Push messages - `push <type> <path to xml>` - where type is "init", "order", or "report"; the XML must be properly formatted according to the 
[C2SIM standard](https://github.com/hyssostech/OpenC2SIM.github.io/blob/f1a345912a3fbbbc87f26ed25a0863b6939bf813/Standard)
- Observe server notifications - the STOMP message stream is displayed

To exit, enter `quit`

For a description of commands and messages, see the [C2SIM Server Reference Implementation Documentation](https://github.com/hyssostech/OpenC2SIM.github.io/blob/9bd71a494be97a8da2f4320aae9adc2923e72d3a/Software/Server/C2SIM Server Reference Implementation Documentation 4.8.0.X .pdf#L1))

## Configuration settings

Default parameters are set in [appsettings.json](./appsettings.json):

* C2SIMSubmitterId - Id string of the submitter</param>
* C2SIMRestUrl - Full C2SIM server endpoint, including host:port/path, e.g. "http://10.2.10.30:8080/C2SIMServer</param>
* C2SIMRestPassword - C2SIM server password</param>
* C2SIMStompUrl - Full notification service (STOMP) endpoint, including host:port/destination, e.g. "http://10.2.10.30:61613/topic/C2SIM"</param>
* C2SIMProtocol - "SISO-STD-C2SIM" (or "BML")</param>
* C2SIMProtocolVersion - "1.0.0" for published standard, or legacy version (e.g. v9="0.0.9")</param>

These settings can be overriden via command line parameters (described below)

## Building and Running

Install the [.NET 6 runtime](https://dotnet.microsoft.com/download/dotnet/6.0) for the required OS (Linux, macOS, Windows) if not yet installed

```
cd C2SIMSDKSampleApp
dotnet build
dotnet run 
```
Or using command line parameters to override the default `appconfig.json` settins:

```
dotnet run C2SIMRestUrl="http://10.2.10.70:8080/C2SIMServer"
```
