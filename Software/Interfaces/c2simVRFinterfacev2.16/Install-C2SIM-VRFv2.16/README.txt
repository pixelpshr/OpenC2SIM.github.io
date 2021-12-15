INSTALLING C2SIMv1.0.0 INTERFACE AND ADDITIONS TO VRFORCESv4.9 using c2simVRF v2.16

Thanks for help from Doug Reece of MAK.

Note that this version was built using Microsoft Visual Studio 2015 and requires VR-Link5.6 installed.

This folder contains interface program c2simVRF and also some configuration data for creating 
mobile forces: armored cavalry section, mobile infantry squad, mobile irregular team, a script 
for a Chinook to perform an evacuation mission, and another for generating reports input for C2SIM.

1. Copy the c2simVRFv2.16 directory to wherever you want it to run, for example C:\. It 
contains a batch file runc2simVRF.bat to run the program. You can make a Windows shortcut from
this file and place it on your desktop to start the interface. The parameters are defined in
program module main.cxx. The system name e.g. VRFORCES must match the SystemName in C2SIM 
initialzation. The first parameter is the IP address of the C2SIM server; you should edit it 
to point to the server you are using.

2. Add to your Windows Path Environment Variable:
C:/MAK/vrforces4.9/bin64 and C:/MAK/vrlink5.6/bin64

3. Copy the appData, bin, data and userData folders from VRFadditionalFiles into the top level 
of VRForces 4.9 installation (e.g. c:\MAK\vrforces4.9). These were provided by Dr. Doug Reece of
MAK. They will change file appData\settings\vrfGui\default_AggregateDisplaySettings.adsx and
bin\vrfLuaDIS.dll, and also add folder/files to data\simulationModelSets and userData\Scenarios. 
You can use the scenario that is loaded (Bogaland2.scnx) or make a new one that uses SimulationModelSet C2simEx.

4. To use the aggregated friendly (Scout Platoon) or adversary (Mobile Irregular) from the VRForces 
GUI you would look for AR Scout on the entity creation menu. Or from the C2SIM Interface, you 
would call createAggregate instead of createEntity. See for example createScoutUnit in 
c2simVRFv2.10/C2SIMinterface.cpp.

5. The Tracking_Reports scripts cause VRForces to emit entity position information in textIf.cpp
which are used by the reportCallback function in c2simVrfv2.9/textIf.cpp to create Position 
reports. The platform_send_observation and make_c2_reports scripts produce Observation reports.
Alternately, a function is provided in c2simVRF that generates Position reports 
without the scripts. The alternate reports are activated by providing a value greater than zero 
in the ninth command line parameter where c2simVRF is invoked, the reportInterval.

6. To use the scenario data loaded: in the VRForces GUI Scenario Startup panel select 
"read from disk" and click on Bogaland2. You will have to wait while VRForces downloads the
terrain from Internet - when it arrives it should include a green playbox. When you shutdown 
VRForces do not choose to save as Bogaland2 scenario - that would replace it with whatever you 
have been running. If you want to save your latest scenario, give it a different name.

7. When starting VRForces, make sure that the Simulation Connections Configuration block
Network Interface Address shows the actual IP address of the VRForces, not the loopback 
address 127.0.0.1.

8. To run the c2simVRF interface, start VRForces4.9 and then click on runC2simVRF.bat. If the 
parameters there describe an initialized C2SIM server that is reachable from your network, the 
interface will perform a late-joiner C2SIM initialization, connect to VRForces, and load into
VRForces simulation objects from the initialization, matching the SystemName in the .bat file.

c2simVRFv16 uses these command line parameters;
any can be omitted unless providing one of the later ones:
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

Treatment of reports described above assumes VRForces hostile forces will be under command 
of an exercise OPFOR who are giving orders to red units and see observations about blue 
units as their own opposition. Thus red observations of the blue force are reported as 
"FR" because that is the blue force role in the exercise.


NOTES ABOUT VR-FORCES4.9: 

a. Visualization of units is controlled by button "Show/Hide Units" in the row of graphic 
buttons immediately above the VRForces map. You can set this for platforms or unit icons.

b. It is possible to save time restarting VR-Forces by using the VR-Forces GUI File menue to
Load Recent Scenario. Sometimes VRForces balks at this but it does save significant time.

c. The C2SIM Reference Implmentation Server typically runs in a mode where the positions
provided during late-joiner initialization (which is what c2simVRF uses) are those where the 
objects in question were last reported to be located. To restart at the initial positions in
the initialization file, a user with the server password must run a new initialization cycle.
On the C2SIM Sandbox the C2SIMGUI has the Sandbox server password configured and the 
initialization cycle can be run using the "STOP/REST SERVER" button followed by the 
"INIT/SHARE/START SERVER" button.

d. If multiple instances of VRForces are to be run in the same exercise as independent
C2SIM systems, it is necessary to set the Session ID of each instance to a different
value. This is done in the startup panel for VRForces and the value in the 12th 
command-line parameter for c2simVRF interface must match it. If they do not match you
will get a message "No backends found" when running c2simVRF.

e. Some VRForces GUI options you may want to set:
- radio messages: visual indicator turned on/off by Settings->Display->green zigzag icon

- ground clamping: surface objects (e.g.vehicles) drop to ground by 
  Settings->Display->Enable gound clamping

- unit names on screen: Settings->toggle Entity Labels

- route names on screen: Settings->toggle Tactical Graphics

- miles vs km: Settings->Display->km/mi icon

- grid lines: Settings->Display->yellow grid icon
