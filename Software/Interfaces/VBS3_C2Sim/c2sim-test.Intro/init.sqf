sleep 2;

raddr = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-SERVER=192.168.24.201"];
rns = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-C2SNS=false"];
rt = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-MinT=5000"];
rsysn = pluginFunction ["VBS3-C2SIM-plugin", "C2SIM-SYSNAME=VBS3"];
rinit = pluginFunction ["VBS3-C2Sim-plugin", "C2SIM-INIT"];



hint format["C2Sim Plugin started"];