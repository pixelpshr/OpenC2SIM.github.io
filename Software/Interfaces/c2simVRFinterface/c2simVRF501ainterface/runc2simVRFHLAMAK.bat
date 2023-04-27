REM Batch file to start C2SIM VRForces interface
REM if multiple VRForces are participating as separate federates, each should have a different siteId and sessionId

set PATH=C:\MAK\makRti4.6b\bin64;C:\MAK\vrforces5.0.1a;C:\MAK\vrlink5.8
cd C:\MAK\vrforces5.0.1a\bin64
C:\Users\c2sim\Desktop\C2SIMarchive\c2simVRFinterfacev2.27\bin64\c2simVRFHLA1516e.exe 10.2.10.70 8080 61613 COE 0 0 1 127.0.0.1  0 0 0 1 3201 1 0 0 1 CWIX-2022

REM 1 server IP:10.2.10.70
REM 2 REST port:8080
REM 3 STOMP port:61613
REM 4 client ID:NLD
REM 5 skipinit:0
REM 6 IBML: 0
REM 7 red/blue tracking:0
REM 8 VRF address: 127.0.0.1
REM 9 internal report interval:0
REM 10 blue force name: 0
REM 11 debug output:0
REM 12 VRF session ID:1
REM 13 VRF app number:3201
REM 14 VRF site:1
REM 15 blue/red obs:0
REM 16 respond to C2SIM time mult:0
REM 17 bundle reports
REM 18 HLA Federation:CWIX=2022

pause