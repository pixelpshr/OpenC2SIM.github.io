REM Batch file to start VRF GUI and SIM with pRTI HLA


set PATH=C:\Program Files\prti1516e\lib\vc141_64;C:\Program Files\prti1516e\lib;C:\Program Files\prti1516e\jre\bin\server;C:\MAK\vrforces4.9

set CLASSPATH=C:\Program Files\prti1516e\lib\prti1516e.jar

cd C:\MAK\vrforces4.9\bin64

start C:/MAK/vrforces4.9/bin64/vrfGui.exe --hla1516e --execName CWIX-2022 --fedFileName RPR_FOM_v2.0_1516-2010.xml --rprFomVersion 2.0 --rprFomRevision 3 --fomModules RPR_FOM_v2.0_1516-2010.xml --hostAddressString 10.2.10.51 --appNumber 3101 --siteId 2 --sessionId 2

C:/MAK/vrforces4.9/bin64/vrfSimHLA1516e.exe --frontEndPID 588 --execName CWIX-2022 --fedFileName RPR_FOM_v2.0_1516-2010.xml --rprFomVersion 2.0 --rprFomRevision 3 --fomModules RPR_FOM_v2.0_1516-2010.xml --hostAddressString 10.2.10.51 --appNumber 3001 --siteId 2 --sessionId 2

pause

