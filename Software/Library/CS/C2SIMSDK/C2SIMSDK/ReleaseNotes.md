# C2SIM SDK for .NET Release Notes

## Version 1.2.5
* Injecting ILoggerFactory to simplify logger propagation when nested libraries are used
* Documented Status changed events that signal that the server is waiting for Initialization, or for Orders / Reports

## Version 1.2.4
* Improved reporting on connection error
* Fixed issue with larger message numbers in C2SIM server response object

## Version 1.2.3
*  More detailed logging
    
## Version 1.2.2
* Fixed issue with server responses, which may sometimes be just strings rather than xml d

## Version 1.2.1

* Wrapping Initialization, Order and Report messages in MessageBody if not already present
* Returning push message server results as strongly typed  objects

## Version 1.2.0

* Changes to make it possible to handle both versions of the schema - v1.0.0 and v1.0.1

## Version 1.1.0

* Updated to C2SIM schema v1.0.1

## Version 1.0.0

* Initial release