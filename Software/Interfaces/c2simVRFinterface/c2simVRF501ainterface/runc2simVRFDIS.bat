REM second IP addess below must be on broadcast or multicast network for DIS

cd C:\MAK\vrforces5.0.1a\bin64

C:\Users\c2sim\Desktop\C2SIMarchive\c2simVRFinterfacev2.27\bin64\c2simVRFDIS.exe 10.2.10.70 8080 61613 NLD 0 0 1 10.2.10.51 0 0 0 1 3201 1 0 0 1

REM Parameters optional; defaults given below
REM 1 server IP:10.2.10.70
REM 2 REST port:8080
REM 3 STOMP port:61613
REM 4 client ID:NLD
REM 5 skipinit:0
REM 6 IBML: 0
REM 7 red/blue tracking:0
REM 8 VRF address: 10.2.10.51
REM 9 internal report interval:0
REM 10 blue force name: 0
REM 11 debug output:0
REM 12 VRF session ID:2
REM 13 VRF app number:3201
REM 14 VRF site:2
REM 15 blue/red obs:0
REM 16 respond to C2SIM time mult:0
REM 17 bundle reports
REM 18 HLA Federation: (0 if none)
REM 19 name for this order sender (0 if none)

PAUSE

