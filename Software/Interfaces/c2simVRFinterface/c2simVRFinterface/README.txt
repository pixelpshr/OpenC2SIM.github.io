/*----------------------------------------------------------------*
|     Copyright 2022 Networking and Simulation Laboratory         |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for academic purposes is hereby  |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
*-----------------------------------------------------------------*/

VERSION 17 uses C++ClientLibv4.8.0.3 with libboost-*-vc140-*

This directory contains all the code developed by GMU as open source
to provide a prototype C2SIM interface for VT-MAK VR-Forces.
Open source supporting code also is provided.

This version works with VRForces4.9 and VRLink5.6 and is compiled
with Microsoft Visual C++ 15.

The project file provided assumes that VRForces4.9 (including 
toolkit) is installed in C:\MAK\vrforces4.9 and VRLink5.6 is 
installed C:\MAK\vrlink4.6. These are MAK's default install 
directories. These can be adjusted by changing VrfDir and VrlDir
in the project file c2simVRF.vcxproj.

You must add to your Windows Path Environment Variable:
C:/MAK/vrforces4.9/bin64 and C:/MAK/vrlink5.6/bin64

The top-level directory c2simVRFv2.17 can be installed in C:\
or elsewhere. This is changed from v2.6, which had to be located 
the vrforces4.6.1/examples directory. 

v2.17 includes binaries for C++C2SIMClientLibv4.8.0.3, which is 
compatible with C2SIM Reference Implemenation Server v4.8.0 and 
is *not* compatible with earlier versions of the server.

v2.17 is compatible with C2SIM v1.0.1. It accepts multiple
Routes in C2SIMInitializationBody and ObjectinitializationBody
messages. To provide routes after initialization, use an
Entity containing a Route in the C2SIM Order.

see main.cxx for list of c2simVRF command-line parameters
(not to be confused with VRForces command-line parameters that
are listed in main.cxx and used to in the VRForces config panel)

c2simVRFv2.17 accepts a command line parameter (number 9) that
gives a value in seconds for the interval between Position Reports.
If this parameter has value greater than zero the scripted reports
procedures are ignored and Position Reports (only - no Observation
Reports) are generated at this interval. If this parameter is zero
(the default value) the scripted approach to reports is used. 

See folder VRFadditionalFiles for files to be added to VRForces to
support the scenario used by NATO MSG-145 in CWIX 2019.

Effective with c2simVRFv2.17, it is possible to use the C2SIM 
interface with either DIS or HLA. To run with HLA start the 
interface using runc2simVRFHLA1516e.bat.

c2simVRFv16 uses these command line parameters;
any can be omitted to use default unless providing one of the later ones:
1. server IP address
2. REST port number
3. STOMP port number
4. clientID name 
5. 1 to skip initialize, 0 otherwise (default 0)
6. 1 to use IBML instad of C2SIM, 0 otherwise (default 0)
7. 0 to send blue tracking (default)
   1 to send red and blue tracking, 
   2 to send only red tracking
   3 to send no tracking
8. VRForces Local IP Address (defaults to loopback)
9. report generation interval in seconds
10. blue force name for initialization
11. 1 to print debug data, 0 otherwise (default 0)
12. VRForces Session ID (default 1)
13. remote control interface application number (default 3201)
14. VRForces Site ID (default 1)
15. 0 to send blue observations (default)
    1 to send red and blue observations, 
    2 to send only red observations 
    3 to send no observations

Treatment of reports described above assumes VRForces hostile
forces will be under command of an exercise OPFOR who are
giving orders to red units and see observations about blue units
as their own opposition. Thus red observations of the bleu force
are reported as "FR" because that is the blue force role in the
exercise.
