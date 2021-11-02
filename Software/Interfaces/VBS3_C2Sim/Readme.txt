DTA VBS3 C2Sim plugin

The plugin is a single 64 bit dll named "VBS_C2Sim_Plugin.dll". It needs to be copies to the plugins64 directory inside the VBS3 root

For the plugin to work, a scenario file needs to be properly set up.  In VBS missions are saved in the folder <userprofile>\mpmissions\<missionName>. There are several files created by VBS, most named "mission", with a range file extensions: sfq, sqm, biedi, etc.  sqf is VBS's built-in scripting language

The Plugin WILL NOT CREATE entities according to the C2SIM initilize message.  The entities need to already created in the mission with the correct unit name and placed somewhere in the map.  When the initialize message is received, the entities will be moved to their starting locations.

In this folder create a plain txt file named "init.sqf" this will be loaded when the scenario starts.  the C2Sim plugin configuration is done in this file so that it is mission-specific
It mainly uses the script function "pluginFunction", which sends commands to a plugin
pluginFunction [<pluginName>, <command>];

A sample init.sqf file is below

// begin init.sqf

// sleep to pause execution of the script for 2 sec to allow VBS to finish loading
sleep 2; 

//  set the address of the c2sim server
_r = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-SERVER=192.168.24.201"];

// set whether to use the "c2S" namespace on XML
_r = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-C2SNS=false"];

// set the minimum Time to send reports
_r = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-MinT=5000"];

// set the C2Sim system name of VBS3 (important for initilize)
_r = pluginFunction ["VBS3-C2SIM-plugin", "C2SIM-SYSNAME=VBS3"];

// this command should be last.  it tells the plugin to connect to the C2Sim server
// without this command VBS will behave as if there is no plugin
_r = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-INIT"];


// hint is just simple on screen text to say the plugin is started
hint format["C2Sim Plugin started"];

