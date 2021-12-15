INSTALLING C2SIMv1.0.0 INTERFACE AND ADDITIONS TO VRFORCESv4.9 using c2simVRF v2.15

Thanks for help from Doug Reece of MAK.

Note that this version was built using Microsoft Visual Studio 2015 and requires VR-Link5.6 installed.

This folder contains interface program c2simVRF and also some configuration data for creating 
mobile forces: armored cavalry section, mobile infantry squad, mobile irregular team, a script 
for a Chinook to perform an evacuation mission, and another for generating reports input for C2SIM.

1. Copy the c2simVRFv2.14 directory to wherever you want it to run, for example C:\. It 
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

9. Start the interface module after VRForces iss up and running with a scenario, started for 
either DIS or HLA1516eeither. To start the interface DIS use runc2simVRF.bat or to run with 
HLA, use runc2simVRFHLA1516e.bat.

NOTES ABOUT VR-FORCES4.9: 

a. Visualization of units is controlled by button "Show/Hide Units" in the row of graphic 
buttons immediately above the VRForces map. You can set this for platforms or unit icons.

b. It is possible to save time restarting VR-Forces by using the VR-Forces GUI File menu to
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
will get a message "No backends found" when running c2simVRF. Also the combination of 
Site ID and Application Number must not be duplicated in any instance. You can use Site ID
om the VRForces lanucher and the c2simVRF interface to avoid this by giving each instance
a differnet Site ID.

e. For objects that normally exist on the surface of the Earth or fly over it, VRForces third 
location coordinate (after latitude/longitude) is elevation AboveGroundLevel (AGL), whereas
maritime vessels have MeanSeaLevel (MSL) elevations. VRForces has an option "ground clamping"
that automatically brings a ground object (e.g. a tank) down to local surface elevation
if it is given a higher elevation (see below to turn on ground clamping). When c2simVRF
finds that a C2SIM initial elevation or tasked elevation has not been provided, it inserts
an elevation AGL of 1000 meters to trigger ground clamping. (For aircraft, be sure to 
provide the elevation AGL if you do not want ground level.) NOTE: if the object is "under
ground, i.e. has elevation AGL -0 or less, VRForces will not execute a route from c2simVRF
for that object.

f. A shortcoming of the present c2simVRF interface is that it is unable to determine 
operational status of opposing aggregated unit objects. Thus OBSERVATION reports always 
show the opposing unit as fully operational. This is not true of entity-level objects,
which are shown as NotOperational when killed.

g. Some VRForces GUI options you may want to set:
- radio messages: visual indicator turned on/off by Settings->Display->green zigzag icon

- ground clamping: surface objects (e.g.vehicles) drop to ground by 
  Settings->Display->Enable gound clamping

- unit names on screen: Settings->toggle Entity Labels

- route names on screen: Settings->toggle Tactical Graphics

- miles vs km: Settings->Display->km/mi icon

- grid lines: Settings->Display->yellow grid icon

