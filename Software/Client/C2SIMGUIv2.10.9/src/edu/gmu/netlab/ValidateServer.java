/*----------------------------------------------------------------*
|   Copyright 2009-2021 Networking and Simulation Laboratory      |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for all purposes is hereby       |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
*----------------------------------------------------------------*/
package edu.gmu.netlab;

import edu.gmu.c4i.c2simclientlib2.C2SIMClientException;
import edu.gmu.c4i.c2simclientlib2.C2SIMClientREST_Lib;
import java.io.BufferedWriter;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * C2SIM Server Validation Option for C2SIMGUI
 *
 * This is based on a now-defunct quality assurance tool the GMU C4I & Cyber Center 
 * developed several versions back. The idea is that after software changes are made, 
 * the tool will run a list of transactions (files in a directory that user selects), 
 * obtain the responses, and compare to set of files in another directory that 
 * contain correct responses. Output to console will be one line per file, giving 
 * input and output filenames and any error where output is not as expected. This 
 * is the best test we know of to confirm that changes to server software did not 
 * break some previous function. This tests the server, using parts of C2SSIMGUI.
 * It should not be considered a test of the whole C2SIMGUI.
 * 
 * The output will appear on the GUI command-line console and also will be copied 
 * into a file named for date-time of the test.
 * 
 * A configuration parameter will set the length of time to wait for all STOMP 
 * output to be received (if translation is running there can be multiple XML files)
 * 
 * Input is contained in .txt files in the ValidateServer directory. Each line
 * in each file causes one test transaction. The line contains either a server
 * command (such as STOP) or PUSH with a filename for XML file to push, which
 * will be contained in subdirectory SendXML.
 * 
 * Test should be run when there is no other server traffic.
 * 
 * 1. for each file in ValidateServer/Scripts directory, in order by filename:
 * 
 * - for comment lines (starting with #) output the line in result file and ignore
 * 
 * - script lines for server commands cause issue of a PUSH to server REST input 
 *   and capture of the HTTP response XML
 *   * the response will be written in a file Of the same name plus -RESPONSE in 
 *     the ResponseXML subdirectory
 *   * server commands are STOP, RESET, INITIALIZE, SHARE and START
 *   * there must be a PUSH (see below) of initialization file immediately
 *     after INITIALIZE
 *   * for success the response to each command must not contain ERROR
 *   * and, after the sequence, server must start in RUNNING state
 *   * for all but SHARE there is nothing else on the script line; SHARE is
 *     followed on its line by:
 *     - the format of input file, either 100INIT for version 1.0.0 or
 *       0900INIT for version 0.0.9 or MSDLINIT for MSDL
 *     - the filename in CompareXML to which the server response is to be compared
 *     - then at end of script line one or two other formats optionally of
 *       100, 009 or MSDL indicating translated formats also to be compared
 * 
 * - the other type of script is PUSH, which provides the type of a C2SIM 
 *   document to be pushed (one of INIT, C2SIMORDER, C2SIMREPORT, CBMLORDER, 
 *   CBMLREPORT, IBMLORDER or IBMLREPORT) followed by the filename to be pushed,
 *   which must be in subdirectory SendXML and then, optionally one, two or three
 *   translated formats expect to follow a copy of the pushed file (the possible
 *   formats are 100, 009, CBML and IBML were 100 is C2SIMv1.0.0 and 009 is
 *   C2SIMv9).
 *   * validation will submit the file by REST and capture both the HTTP response 
 *     (placed in subdirectory ResponseXML, with -RESPONSE appended to input 
 *     filename before .xml) and the XML coming from server STOMP (placed in 
 *     subdirectory ReceiveXML, with format appended to its name before .xml)
 *   * for success, the HTTP response must not contain ERROR and the contents of 
 *     the STOMP XML output must match the contents of the file of same name in 
 *     the CompareXML subdirectory 
 *   * we suggest that the file placed in CompareXML for comparison should be one 
 *     that was received from the server, as a correct returned file might have 
 *     differences in whitespace
 *   * files in CompareXML must have format (e.g. -C2SIMORDER or 100REPORT) 
 *     added to filename, before .xml)
 *   * if subdirectory CopyXML exists, it gets copies of all files received,
 *     over-writing previous files of the same name; these might be useful
 *     for CompareXML if they are confirmed to be valid
 *   * for simplicity we require that tags in the files not have a prefix
 *     (having it would make no difference in the translation)
 * 
 *-  for each format received, an outLine will include length of time until 
 *   that format is received (up to a configured limit) and the name of file 
 *   containing response
 * 
 *-  both SHARE and PUSH wait up to serverTestStompWait milliseconds for the
 *   various responses to come from the server before checking results
 * 
 * 2. At end of validation run, the number of files passed, failed and total 
 * is output; also count of failed transactions by type of failure; the output
 * information also will be written in a .txt file with date-time in its name,
 * in subdirectory ValidationResults.
 *
 * @author JMP 15May2020 evised 21May2020 to add C2SIMv1.0.0
 */
public class ValidateServer {
    
    C2SIMGUI bml = C2SIMGUI.bml;
    InitC2SIM initC2SIM = new InitC2SIM();
    String workingDirectory;
    File printResults, inputXML, outputXML, responseXML, compareXML, 
        validationResults, scripts, thisScript;
    BufferedWriter writeResults;
    String[] scriptList;
    String scriptLine;
    String filename;
    int numberOfFormats = 4;
    boolean continueScript = true;
    
    // statistics
    int stopOK = 0;
    int stopError = 0;
    int resetOK = 0;
    int resetError = 0;
    int initializeOK = 0;
    int initializeError = 0;
    int startOK = 0;
    int startError = 0;
    int share100OK = 0;
    int share100Error = 0;
    int share009OK = 0;
    int share009Error = 0;
    int shareMsdlOK = 0;
    int shareMsdlError = 0;
    int[][] countInitOK = new int
       [3] // from:{100,009,MSDL}
       [3];// to:  {100,009,MSDL}
    int[][] countInitError = new int
       [3] // from:{100,009,MSDL}
       [3];// to:  {100,009,MSDL}
    int[][] countOrderOK = new int
       [4] // from:{100,009,CBML,IBML}
       [4];// to:  {100,009,CBML,IBML}
   int[][] countOrderError = new int
       [4] // from:{100,009,CBML,IBML}
       [4];// to:  {100,009,CBML,IBML}
   int[][] countReportOK = new int
       [4] // from:{100,009,CBML,IBML}
       [4];// to:  {100,009,CBML,IBML}
   int[][] countReportError = new int
       [4] // from:{100,009,CBML,IBML}
       [4];// to:  {100,009,CBML,IBML}

    /**
     * runs a ServerValidation per the above description
     */
    void runTest(){
        
        // ask user to confirm test is to be run
        if(!bml.okCancelPopup(
            "Test server warning",
            "ARE YOU SURE YOU WANT TO RUN SERVER VALIDATION?"+
                "\n(IF SO MAKE SURE THERE IS NO OTHER SERVER USE NOW)"))
            return;
        bml.runningServerTest = true;  
        
        // make sure we have required subdirecttories:
        // SendXML, ReceiveXML, ResponseXML, CompareXML, ValidationResults
        workingDirectory = 
            bml.guiFolderLocation + bml.delimiter + "ValidateServer" + bml.delimiter;
                
        // directory ValidationResults - make if not present
        validationResults = new File(workingDirectory + "ValidationResults");
        if(!validationResults.exists())
            validationResults.mkdir(); 
        
        // File to hold results of this run - must be new
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date nowDate = new Date();
        String outDate = sdf.format(nowDate);
        printResults = new File(
            validationResults,
            "validationResults-" + outDate + ".txt");
        try{
            FileWriter fileWriter = new FileWriter(printResults);
            writeResults = new BufferedWriter(fileWriter);
        }
        catch(IOException ioe) {
             bml.showErrorPopup(
                "error opening file to write results:" + ioe, 
                "Making results file");
            ioe.printStackTrace();
            bml.runningServerTest = false;
            return;
        }
        
        // directory SendXML must exist
        inputXML = new File(workingDirectory + "SendXML");
        if(!inputXML.isDirectory()){
            bml.showErrorPopup(
                "can't run Server Validation - directory ValidateServer/SendXML not found",
                "Directory error");
            outputLine("can't run Server Validation - directory ValidateServer/SendXML not found");
            bml.runningServerTest = false;
            return;
        }
        
        // directory ReceiveXML - make if not present
        outputXML = new File(workingDirectory + "ReceiveXML");
        if(!outputXML.exists())
            outputXML.mkdir();
        
        // directory ResponseXML - make if not present
        responseXML = new File(workingDirectory + "ResponseXML");
        if(!responseXML.exists())
            responseXML.mkdir();
        
        // directory CompareXML - must exist
        compareXML = new File(workingDirectory + "CompareXML");
        if(!compareXML.isDirectory()){
            bml.showErrorPopup(
                "can't run Server Validation - directory ValidateServer/CompareXML not found",
                "Directory error");
            outputLine("can't run Server Validation - directory ValidateServer/CompareXML not found");
            bml.runningServerTest = false;
            return;
        }        
        
        // direcctory Scripts - must exist
        scripts = new File(workingDirectory + "Scripts");
        if(!scripts.isDirectory()){
            bml.showErrorPopup(
                "can't run Server Validation - directory ValidateServer/Scripts not found",
                "Directory error");
            outputLine("can't run Server Validation - directory ValidateServer/Scripts not found");
            bml.runningServerTest = false;
            return;
        }
        scriptList = scripts.list();
        
        // announce ourself in output and print directories to be used
        outputLine("------C2SIM SERVER VALIDATION (DATE-TIME " + outDate + ")------");
        outputLine("XML input comes from:" + inputXML.getName());
        outputLine("XML output goes to:" + outputXML.getName());
        outputLine("Server response XML goes to:" + responseXML.getName());
        outputLine("Correct XML for comparison comes from:" + compareXML.getName());
        outputLine("List of results goes in file:" + printResults.getName());
        outputLine("  which file goes in:" + validationResults.getName());
        outputLine("Validation scripts come from:" + scripts.getName());
        if((new File(workingDirectory+"CopyXML")).isDirectory())
            outputLine("Copy of all received input goes in " + workingDirectory +
                "CopyXML");
        outputLine("-------------------");
        
        // process every script in the Scripts directory
        zeroArrays();
        for(int scriptNumber = 0;
            scriptNumber < scriptList.length;
            ++scriptNumber){
            
            // open the script
            String scriptFullname = 
                workingDirectory + "Scripts" + bml.delimiter + 
                    scriptList[scriptNumber];
            outputLine("\nRunning script:" + scriptList[scriptNumber]);
            int timesToRun = bml.serverTestStompWait/1000;
            outputLine("   response will be monitored up to " + timesToRun +
                " seconds for each XML file pushed to server");
            BufferedReader scriptFileReader;
            try{
                scriptFileReader = 
                    new BufferedReader(new FileReader(scriptFullname));
                
                // process all lines in the script one at a time
                while(continueScript){
                         
                    // read next script line
                    scriptLine = scriptFileReader.readLine();
                    if(scriptLine == null)break;
                    if(!scriptLine.equals(""))
                        outputLine("SCRIPT LINE:" + scriptLine);

                    // process the script line
                    String unmodifiedLine = scriptLine;
                    String action = getNextToken();
                    if(action == null)break;
                    String pushResponse = "";
                    if(!action.equals("")){
                        if(action.equals("STOP")){
                            pushResponse = initC2SIM.pushStopC2SIM();
                            if(pushResponse.contains("ERROR"))++stopError;
                            else ++stopOK;
                        }
                        else if(action.equals("RESET")){
                            pushResponse = initC2SIM.pushResetC2SIM();
                            if(pushResponse.contains("ERROR"))++resetError;
                            else ++resetOK;
                        }
                        else if(action.equals("INITIALIZE")){
                            pushResponse = initC2SIM.pushInitializeC2SIM();
                            if(pushResponse.contains("ERROR"))++initializeError;
                            else ++initializeOK;
                        }
                        else if(action.equals("SHARE")){
                            // read SHARE comparison format
                            String shareFormat = getNextToken();
                            if(testToken(shareFormat))continue;
                            if(shareFormat.equals("")){
                                outputLine("   SHARE script line missing compare format");
                                collectStats(false,shareFormat,shareFormat,"INIT");
                                continue;
                            }
                            if(!shareFormat.equals("100INIT") &&
                               !shareFormat.equals("009INIT") &&
                               !shareFormat.equals("MSDLINIT")){
                                outputLine("   SHARE script file format must be 100INIT, 009INIT or MSDLINIT");
                                collectStats(false,shareFormat,shareFormat,"INIT");
                                continue;
                            }
                            
                            // read SHARE initialization filename
                            filename = getNextToken();
                            if(filename.equals("")){
                                outputLine("   SHARE script line missing compare filename");
                                collectStats(false,shareFormat,shareFormat,"INIT");
                                continue;
                            }
                            
                            // delete current shareFormat file in ReceivedXML
                            String shareOutput = 
                                combineFilename(shareFormat, "ReceiveXML");
                            (new File(shareOutput)).delete();
  
                            // read and test optional translated formats
                            String shareFormat2 = getNextToken();
                            String shareOutput2 = "";
                            if(testToken(shareFormat2))continue;
                            String shareFormat3 = getNextToken();
                            String shareOutput3 = "";
                            if(testToken(shareFormat3))continue;
                            
                            // if there is a shareFormat2 delete current ReceiveXML copy
                            if(!shareFormat2.equals("")){
                                shareFormat2 += "INIT";
                                shareOutput2 =
                                    combineFilename(shareFormat2, "ReceiveXML");
                                (new File(shareOutput2)).delete(); 
                            }
                            
                            // if there is a shareFormat3 delete current ReceiveXML copy
                            if(!shareFormat3.equals("")){
                                shareFormat3 += "INIT";
                                shareOutput3 =
                                    combineFilename(shareFormat3, "ReceiveXML");
                                (new File(shareOutput3)).delete();
                            }
    
                            // push the SHARE command and record results
                            pushResponse = initC2SIM.pushShareC2SIM();
                            if(pushResponse.contains("ERROR")){
                                if(shareFormat.equals("100INIT"))++share100Error;
                                if(shareFormat.equals("009INIT"))++share009Error;
                                if(shareFormat.equals("MSDLINIT"))++shareMsdlError;
                                continue;
                            }
                            else {
                                if(shareFormat.equals("100INIT"))++share100OK;
                                if(shareFormat.equals("009INIT"))++share009OK;
                                if(shareFormat.equals("MSDLINIT"))++shareMsdlOK;
                                
                                // wait for configured time while output comes
                                try{Thread.sleep(bml.serverTestStompWait);}
                                catch(InterruptedException ie){}
                                
                                // confirm comparefiles are there then compare them
                                if(shareOutput.equals("")){
                                    outputLine("EXPECTED FILE NOT RECEIVED:" + shareOutput);
                                    continue;
                                }
                                String shareCompare = 
                                    combineFilename(shareFormat, "CompareXML");
                                boolean success;
                                if(noCompareFile(shareOutput))
                                    collectStats(false,shareFormat,shareFormat,"INIT");
                                else {
                                    // compare basic file to serve output
                                    success = compareFiles(shareOutput, shareCompare, shareFormat, shareFormat);
                                    collectStats(success,shareFormat,shareFormat,"INIT");
                                }
                  
                                // if comparing translated file, 
                                // confirm comparefile is there then compare it
                                if(!shareFormat2.equals("")){
                                    String shareCompare2 = 
                                        combineFilename(shareFormat2, "CompareXML");
                                    if(noCompareFile(shareOutput2)){
                                        collectStats(false,shareFormat,shareFormat2,"INIT");
                                    }
                                    else {
                                        success = compareFiles(shareOutput2, shareCompare2, shareFormat2, shareFormat);
                                        collectStats(success,shareFormat,shareFormat2,"INIT");
                                    }
                                }// end if(!shareFormat2...
                                if(!shareFormat3.equals("")){
                                    String shareCompare3 = 
                                        combineFilename(shareFormat3, "CompareXML");
                                    if(noCompareFile(shareOutput3)){
                                        collectStats(false,shareFormat,shareFormat3,"INIT");
                                    }
                                    else {
                                        success = compareFiles(shareOutput3, shareCompare3, shareFormat3, shareFormat);
                                        collectStats(success,shareFormat,shareFormat3,"INIT");
                                    }
                                }// end if(!shareFormat3.equals...
                            }// end if(pushResponse.contains...
                        }// end if(action.equals("SHARE"))
                        else if(action.equals("START")){
                            pushResponse = initC2SIM.pushStartC2SIM();
                            if(pushResponse.contains("ERROR"))++startError;
                            else ++startOK;
                        }
                        else if(action.equals("PUSH")){
                            
                            // pull the tokens off of script line
                            String format = getNextToken();
                            filename = getNextToken();
                            String format2 = getNextToken();
                            String format3 = getNextToken();
                            String format4 = getNextToken();
 
                            // check the tokens for correctness
                            if(testToken(format))continue;
                            if(testToken(format2))continue;
                            if(testToken(format3))continue;
                            if(testToken(format4))continue;
                            
                            // check for incomplete PUSH script line
                            if(format == null ||filename == null){
                                outputError("in script line:" + unmodifiedLine);
                                continue;
                            }
                            
                            // check for existence of file
                            String fullFilename = workingDirectory + "SendXML" + 
                                bml.delimiter + filename;
                            if(!(new File(fullFilename)).exists()){
                                outputError("can't find file in SendXML:" + 
                                    filename);
                                continue;
                            }
                             
                            // create filenames in ReceiveXML and CompareXML
                            String type = "";
                            if(format.endsWith("ORDER"))type = "ORDER";
                            if(format.endsWith("REPORT"))type = "REPORT";
                            
                            // main format is run every time
                            String mainOutput = 
                                combineFilename(format, "ReceiveXML");
                            String mainCompare = 
                                combineFilename(format, "CompareXML");
                            String secondFormat = "";
                            String secondOutput = "";
                            String secondCompare = "";
                            String thirdFormat = "";
                            String thirdOutput = "";
                            String thirdCompare = "";
                            String fourthFormat = "";
                            String fourthOutput = "";
                            String fourthCompare = "";
                            
                            // replace unused formats with empty strings
                            if(!format.equals("INIT")){
                                if(format2.length() == 0){
                                    secondFormat = "";
                                    secondOutput = "";
                                    secondCompare = "";
                                }
                                else
                                {
                                    secondFormat = format2 + type;
                                    secondOutput = 
                                        combineFilename(secondFormat, "ReceiveXML");
                                    secondCompare = 
                                        combineFilename(secondFormat, "CompareXML");
                                }
                                if(format3.length() == 0){
                                    thirdFormat = "";
                                    thirdOutput = "";
                                    thirdCompare = "";   
                                }
                                else
                                {
                                    thirdFormat = format3 + type;
                                    thirdOutput = 
                                        combineFilename(thirdFormat, "ReceiveXML");
                                    thirdCompare = 
                                        combineFilename(thirdFormat, "CompareXML"); 
                                }
                                if(format4.length() == 0){
                                    fourthFormat = "";
                                    fourthOutput = "";
                                    fourthCompare = "";   
                                }
                                else
                                {
                                    fourthFormat = format4 + type;
                                    fourthOutput = 
                                        combineFilename(fourthFormat, "ReceiveXML");
                                    fourthCompare = 
                                        combineFilename(fourthFormat, "CompareXML"); 
                                }
                            }
                            
                            // files to come in ReceiveXML for this script line
                            File testMain = new File(mainOutput);
                            File testSecond = new File(secondOutput);
                            File testThird = new File(thirdOutput);
                            File testFourth = new File(fourthOutput);

                            // delete old ReceiveXML files
                            (new File(mainOutput)).delete();
                            (new File(secondOutput)).delete();
                            (new File(thirdOutput)).delete();
                            (new File(fourthOutput)).delete();
    
                            // issue PUSH matching format
                            System.out.println("listening for receive of XML:");// console only
                            if(format.equals("100INIT")){  
                                pushResponse = pushXml("100", "INIT", filename);
                                type = "";
                            }
                            if(format.equals("009INIT")){  
                                pushResponse = pushXml("009", "INIT", filename);
                                type = "";
                            }
                            if(format.equals("MSDLINIT")){  
                                pushResponse = pushXml("MSDL", "INIT", filename);
                                type = "";
                            }
                            else if(format.equals("100ORDER")){
                                pushResponse = pushXml("100", "ORDER", filename);
                                type = "ORDER";
                            }
                            else if(format.equals("100REPORT")){
                                pushResponse = pushXml("100", "REPORT", filename);
                                type = "REPORT";
                            }
                            else if(format.equals("009ORDER")){
                                pushResponse = pushXml("009", "ORDER", filename);
                                type = "ORDER";
                            }
                            else if(format.equals("009REPORT")){
                                pushResponse = pushXml("009", "REPORT", filename);
                                type = "REPORT";
                            }
                            else if(format.equals("CBMLORDER")){
                                pushResponse = pushXml("CBML", "ORDER", filename);
                                type = "ORDER";
                            }
                            else if(format.equals("CBMLREPORT")){
                                pushResponse = pushXml("CBML", "REPORT", filename);
                                type = "REPORT";
                            }
                            else if(format.equals("IBMLORDER")){
                                pushResponse = pushXml("IBML", "ORDER", filename);
                                type = "ORDER";
                            }
                            else if(format.equals("IBMLREPORT")){
                                pushResponse = pushXml("IBML", "REPORT", filename);
                                type = "REPORT";
                            }
           
                            // check for server OK
                            if(pushResponse == null){
                                outputLine("format "+format+" server push response NULL");
                                collectStats(false, format, format, type);
                                continueScript = false;
                                break;
                            }
                            if(pushResponse.contains("ERROR")){
                                outputLine("format "+format+" server push response ERROR");
                                collectStats(false, format, format, type);
                                continue;
                            }
                            
                            // Subscriber class will write the ReceiveXML files;
                            // test once per second until they all arrive
                            // or bml.serverTestStompWait seconds expire
                            if(format.endsWith("INIT"))continue;// INIT does not get response
                            try{
                                for(int iterations = 0;
                                    iterations < timesToRun;
                                    iterations++){
                                    Thread.sleep(1000);// one second
                                    System.out.print(iterations+" ");// console only
                                    if(testMain.exists())
                                        if((format2.length() == 0) || testSecond.exists())
                                            if((format3.length() == 0) || testThird.exists())
                                                if((format4.length() == 0) || testFourth.exists())
                                                    break;
                                }// end for...
                                System.out.println("");// console only
                            }
                            catch(InterruptedException ie){}
                            
                            // confirm that we have a file to compare
                            if(noCompareFile(mainOutput))continue;
                            if(noCompareFile(secondOutput))format2 = "";
                            if(!format2.equals(""))format2 += type;
                            if(noCompareFile(thirdOutput))format3 = "";
                            if(!format3.equals(""))format3 += type;
                            if(noCompareFile(fourthOutput))format4 = "";
                            if(!format4.equals(""))format4 += type;
                            
                            
                            // compare files in ReceiveXML with same name in CompareXML
                            // INIT does not produce a response; that comes from SHARE
                            if(format.endsWith("INIT"))continue;
                            if(!(new File(mainOutput)).exists()){
                                outputLine("  -no output of format " + format + " received");
                                collectStats(false, format, format, type);
                            }
                            else collectStats(
                               compareFiles(mainOutput, mainCompare, format, format), format, format, type);
                            if(format2.length()>0)if(!(new File(secondOutput)).exists()){
                                outputLine("  -no output of format " + format2 + " received");
                                collectStats(false, format, format2, type);   
                            }
                            else collectStats(
                                compareFiles(secondOutput, secondCompare, format2, format), format, format2, type);
                            if(format3.length()>0)if(!(new File(thirdOutput)).exists()){
                                outputLine("  -no output of format " + format3 + " received");
                                collectStats(false, format, format3, type);   
                            }
                            else collectStats(
                                compareFiles(thirdOutput, thirdCompare, format3, format), format, format3, type);
                            if(format4.length()>0)if(!(new File(fourthOutput)).exists()){
                                outputLine("  -no output of format " + format4 + " received");
                                collectStats(false, format, format4, type);   
                            }
                            else collectStats(
                                compareFiles(fourthOutput, fourthCompare, format4, format), format, format4, type);
                            
                        }// end else if(action.equals("PUSH"))
                    }//end if(!action...
                }// end while(true)
                scriptFileReader.close();
            }catch(IOException ioe) {
                bml.showErrorPopup(
                    "error reading file: Scripts/" + scriptList[scriptNumber], 
                    "Script file");
                ioe.printStackTrace();
                break;
            }
                
        }// end for(int scriptNumber = 0;
                
        // output stats
        int send;
        String[] rowLabel = {" 100 "," 009 ","CBML ","IBML "};
        outputLine("-------------------");
        outputLine("RESULTS OF VALIDATION:");
        outputLine(" ");
        outputLine("Server Commands OK/Error");
        outputLine("STOP:       " + stopOK +"/" + stopError);
        outputLine("RESET:      " + resetOK +"/" + resetError);
        outputLine("INITIALIZE: " + initializeOK +"/" + initializeError);
        outputLine("SHARE 100:  " + share100OK +"/" + share100Error);
        outputLine("SHARE 009:  " + share009OK +"/" + share009Error);
        outputLine("SHARE MSDL: " + shareMsdlOK + "/" + shareMsdlError);
        outputLine("START:      " + startOK +"/" + startError);
        outputLine(" ");
        outputLine("C2SIM Initialization OK/Error:");
        outputLine("vertical is send; horizontal is receive");
        outputLine("        100   009   MSDL");
        outputLine(" 100    " + countInitOK[0][0] + "/" + countInitError[0][0] +
                   "   " + countInitOK[0][1] + "/" + countInitError[0][1] +
                   "   " + countInitOK[0][2] + "/" + countInitError[0][2]);
        outputLine(" 009    " + countInitOK[1][0] + "/" + countInitError[1][0] +
                   "   " + countInitOK[1][1] + "/" + countInitError[1][1] +
                   "   " + countInitOK[1][2] + "/" + countInitError[1][2]);
        outputLine("MSDL    " + countInitOK[2][0] + "/" + countInitError[2][0] +
                   "   " + countInitOK[2][1] + "/" + countInitError[2][1] +
                   "   " + countInitOK[2][2] + "/" + countInitError[2][2]);
        outputLine(" ");
        outputLine("Order distribution and translation OK/Error:");
        outputLine("vertical is send; horizontal is receive");
        outputLine("        100   009   CBML  IBML");
        for(send = 0; send < numberOfFormats; ++send){
            outputLine(rowLabel[send]+    
                  "   "+countOrderOK[send][0]+"/"+countOrderError[send][0]+
                  "   "+countOrderOK[send][1]+"/"+countOrderError[send][1]+
                  "   "+countOrderOK[send][2]+"/"+countOrderError[send][2]+
                  "   "+countOrderOK[send][3]+"/"+countOrderError[send][3]);     
        }
        outputLine(" ");
        outputLine("Report distribution and translation OK/Error:");
        outputLine("vertical is send; horizontal is receive");
        outputLine("        100   009   CBML  IBML");
        for(send = 0; send < numberOfFormats; ++send){
            outputLine(rowLabel[send]+    
                  "   "+countReportOK[send][0]+"/"+countReportError[send][0]+
                  "   "+countReportOK[send][1]+"/"+countReportError[send][1]+
                  "   "+countReportOK[send][2]+"/"+countReportError[send][2]+
                  "   "+countReportOK[send][3]+"/"+countReportError[send][3]);
        }
        outputLine(" ");

        // mark end of this script
        outputLine("-------------------");
         
        // close the results file
        try{
            writeResults.close();
        }
        catch(IOException ioe) {
            bml.showErrorPopup(
                "error closing results file:" + ioe, 
                "Closing results file");
            ioe.printStackTrace();
        }// end close writeResults
        
        // print collected statistics
        
        bml.runningServerTest = false;
        
    }// end runTest()
    
    /**
     * pulls blank-separated tokens from left end of s String
     * returns the token and remove it from scriptLine as a side-effect
     * returns "" if no more tokens
     */
    String getNextToken(){
        
        if(scriptLine.equals(""))return "";
        int tokenEnd = scriptLine.indexOf(' ');
        String token = "";
        if (tokenEnd < 0) {
            token = scriptLine;
            scriptLine = "";
        }
        else {
            token = scriptLine.substring(0,tokenEnd);
            scriptLine = scriptLine.substring(tokenEnd+1);
        }
        if(token.endsWith(".xml"))return token;
        return token.toUpperCase();
        
    }// end getNextToken()
    
    /**
     * tests the format of a token to confirm it is 
     * one of those we accept
     * returns true if test not passed
     */
    boolean testToken(String token){
        
        if(token.equals(""))return false;
        String tokenType = "";
        
        // initialization formats
        if(token.startsWith("100") || token.startsWith("009") ||
            token.startsWith("MSDL")){
            if(token.length() == 3 || token.equals("MSDL"))return false;
            if(token.startsWith("MSDL"))tokenType = token.substring(4);
            else tokenType = token.substring(3);
            if(tokenType.equals("INIT") || tokenType.equals("ORDER") || 
                tokenType.equals("REPORT"))return false;
        }
        
        // tasking-reporting formats
        if(token.startsWith("CBML") || token.startsWith("IBML")){
            if(token.length() == 4)return false;
            tokenType = token.substring(4);
            if(tokenType.equals("INIT") || tokenType.equals("ORDER") || 
                tokenType.equals("REPORT"))return false;
        }
        outputLine("FILE FORMAT ERROR:" + token + " is not one ouf our types");
        return true;
        
    }// end testToken()
    
    /**
     * displays a line of output and also writes it to file printResults
     * returns true on IOError
     */
    boolean outputLine(String lineToPrint){

        try{
            writeResults.write(lineToPrint);
            writeResults.newLine();
        }
        catch(IOException ioe) {
            System.out.println("ERROR in ValidateServer:" +
                "file error writing results:" + ioe.getMessage());
            return true;
        }
        System.out.println(lineToPrint);
        return false;
        
    }// end outputLine()
    
    /**
     * checks whether comparison file exists
     * returns true for failure
     */
    boolean noCompareFile(String filename){
        String testShortname = (new File(filename)).getName();
        String compareFilename = workingDirectory + "CompareXML" + 
            bml.delimiter + testShortname; 
        if(!(new File(compareFilename)).exists()){
            outputLine("   CONFIGURATION ERROR:file "+filename+
                " is not present in CompareXML for comparison");
            outputLine("   put a copy of a good received XML file there");
            outputLine("   be sure to include format such as -100ORDER before .xml");
            return true; 
        }
        return false;
    }// end noCompareFile()
    
    /**
     * compare contents of two files with message to outputLine
     * returns true if file contents match exactly
     */
    boolean compareFiles(
        String outputFilename, 
        String compareFilename, 
        String outFormat,
        String sendFormat){

        // verify we have a matching file in CompareXML
        if(!(new File(compareFilename)).exists()){
            outputLine("   CONFIGURATION ERROR:file "+filename+
                " is not present in CompareXML for comparison");
            outputLine("   put a copy of matching good received XML file there");
            outputLine("   be sure to include format such as -C2SIMORDER before .xml");
            return false;
        }
     
        // read the files contents
        String xml1 = bml.readAnXmlFile(outputFilename);
        String xml2 = bml.readAnXmlFile(compareFilename);
        
        // correct for changing ReportID that should not match 
        int sendStartId = 0, sendEndId = 0, compareStartId = 0, compareEndId = 0;
        if(outFormat.startsWith("009") || outFormat.startsWith("100") ||
            outFormat.startsWith("IBML") ){
            sendStartId = xml1.indexOf("<ReportID>") + ("<ReportID>").length();
            sendEndId = xml1.indexOf("</ReportID>");
            compareStartId = xml2.indexOf("<ReportID>") + ("<ReportID>").length();
            compareEndId = xml2.indexOf("</ReportID>");   
        } else if(outFormat.startsWith("CBML")){
            sendStartId = xml1.indexOf("<ReportingData><OID>") + 
                ("<ReportingData><OID>").length();
            sendEndId = xml1.indexOf("</OID><ReportingDataCategoryCode>");
            compareStartId = xml2.indexOf("<ReportingData><OID>") + 
                ("<ReportingData><OID>").length();
            compareEndId = xml2.indexOf("</OID><ReportingDataCategoryCode>"); 
        }
            
        // remove ReportID data
        if(sendStartId > 0 && sendEndId > 0)
            xml1 = xml1.substring(0,sendStartId) + xml1.substring(sendEndId);
        if(compareStartId > 0 && compareEndId > 0)
            xml2 = xml2.substring(0,compareStartId) + xml2.substring(compareEndId);
  
        // compare the files contents
        if(xml1.equals(xml2)){
            outputLine("  -"+outFormat+" received file matches reference");
            return true;
        }
        else {
            outputLine("  -"+outFormat+" received file does not match reference");
            return false;
        }
    }// end compareFiles()
    
    /**
     * collect statistics on success and failure of Orders
     */
    void collectStats(boolean success, String sendFormat, String receiveFormat, String type){
        
        // deal with INIT as a special case
        if(type.equals("INIT")){
            int i=0, j=0;
            if(success){
                if(sendFormat.startsWith("009"))i=1;
                if(receiveFormat.startsWith("009"))j=1;
                if(sendFormat.startsWith("MSDL"))i=2;
                if(receiveFormat.startsWith("MSDL"))j=2;
                ++countInitOK[i][j];
            }
            else {
                if(sendFormat.startsWith("009"))i=1;
                if(receiveFormat.startsWith("009"))j=1;
                if(sendFormat.startsWith("MSDL"))i=2;
                if(receiveFormat.startsWith("MSDL"))j=2;
                ++countInitError[i][j];
            }
            return;
        }
        
        // assume if not an Order it must be Report
        int sendFormatNumber = formatNumber(sendFormat);
        if(sendFormatNumber < 0)return;
        int receiveFormatNumber = formatNumber(receiveFormat);
        if(receiveFormatNumber < 0)return;
        if(type.equals("ORDER")){
            if(success)++countOrderOK[sendFormatNumber][receiveFormatNumber];
            else ++countOrderError[sendFormatNumber][receiveFormatNumber];
        } else {
            if(success)++countReportOK[sendFormatNumber][receiveFormatNumber];
            else ++countReportError[sendFormatNumber][receiveFormatNumber];
        }
    }
    
    /**
     * convert the formats to number to index the statistics matrix
     * returns the index
     */
    int formatNumber(String format){
        if(format.startsWith("100"))return 0;
        if(format.startsWith("009"))return 1;
        if(format.startsWith("CBML"))return 2;
        if(format.startsWith("IBML"))return 3;
        outputLine("SCRIPT ERROR - " + format + " not a usable code");
        return -1;
    }
    
    /**
     * write and output line with ERROR before text
     */
    void outputError(String lineToPrint){
        outputLine("ERROR in ValidateServer:" + lineToPrint);
    }
    
    /**
     * creates a String from current filename being processed, with
     * type code (INIT, C2SIMORDER, ...) inserted before .xml,
     * located in designated subdirectory
     */
    String combineFilename(String docType, String subdirectory){
        return workingDirectory + subdirectory + 
            bml.delimiter + filename.substring(0,filename.length()-4) +
            "-" + docType + ".xml";
    }
    String outputFilename(String docType){
        return combineFilename(docType, "ReceiveXML");
    }
    
    /**
     * write an XML string of current file, as returned by server,
     * to ReceiveXML directory - this is invoked from Subscriber call
     * input parameter is type of document as detected by 
     * Subscriber.interpretMessage() - it is appended to filename
     */
    void writeOutputXml(String xmlDocument, String docType){
        
        String outputName = outputFilename(docType);
        outputLine("  -received XML file (length " + xmlDocument.length() + "):" +
            (new File(outputName)).getName());
        try{
            BufferedWriter out = new BufferedWriter(
                new FileWriter(outputName));
            out.write(xmlDocument);
            out.close();
            
            // if Copy directory exists write copy there
            String copyDirectory = workingDirectory + "CopyXML";
            File copyTest = new File(copyDirectory);
            if(copyTest.exists() && copyTest.isDirectory()){
                String filename = 
                    outputName.substring((workingDirectory+"ReceiveXML").length());
                String copyName = copyDirectory + filename;
                BufferedWriter copy = new BufferedWriter(
                    new FileWriter(copyName));
                copy.write(xmlDocument);
                copy.close(); 
            }// end if(copyTest...
        }
        catch(IOException ioe){
            outputError("IOException in ValidateServer writing XML:" +
                ioe.getMessage());
        }
    }// end writeOutputXml()
    
    /**
     * send an XML document to server REST input and return its response
     */
    String pushXml(String docType, String function, String xmlFilename)
        throws IOException {
        
        // check that it is an XML file
        if(!xmlFilename.endsWith("xml")){
             outputError("invalid file to PUSH - must be XML"); 
            return null;
        }
                
        // check for submitter code
        if(bml.submitterID.length() == 0) {
            outputError("cannot push C2SIM INIT - submitterID Config required");
            return null;
        }
	
        // read C2SIM from the parameter file
        FileReader xmlFile;
        String pushInputString = "";
        String pushResponseString = "";
        try{
          xmlFile=new FileReader(
              workingDirectory + "SendXML" + bml.delimiter + xmlFilename);
          int charBuf; 
          while((charBuf = xmlFile.read())>0) {
            pushInputString += (char)charBuf;
          }
        }
        catch(Exception e) {
          bml.printError("Exception in pushing XML file " +xmlFilename + ":"+e);
          e.printStackTrace();
          return null;
        }
        
        // IBML and CBML use a different send service
        if(docType.equals("IBML") || docType.equals("CBML")){
            String bmlDescription = "";
            if(docType.equals("IBML")){
                if(function.equals("ORDER"))
                    bmlDescription = "IBML Order Push";
                else bmlDescription = "IBML Report Push";
            }
            else {
                if(function.equals("ORDER"))
                    bmlDescription = "CBML Order Push";
                else bmlDescription = "CBML Report Push";
            }
            pushResponseString =
                bml.ws.processBML(
                    pushInputString,
                    bml.orderDomainName,
                    "BML",
                    bmlDescription);
        }
        else {
            // send C2SIM XML; set pushingInitialize so can use with INIT
            String sendVersion = bml.c2simProtocolVersion;
            if(docType.equals("009"))sendVersion = "0.0.9";
            if(function.equals("INIT"))bml.pushingInitialize = true;
            pushResponseString = 
                bml.ws.sendC2simREST(
                    pushInputString,
                    "INFORM",
                    sendVersion);
            if(function.equals("INIT"))bml.pushingInitialize = false;
        }
        
        // write result to file
        String responseFilename = xmlFilename.substring(0,xmlFilename.length()-4) + 
            "-RESPONSE.xml";
        (new File(workingDirectory + "ResponseXML/" + responseFilename)).delete();
        BufferedWriter out = 
            new BufferedWriter(
                new FileWriter(workingDirectory + "ResponseXML/" + responseFilename));
        out.write(pushResponseString);
        out.close();
        bml.pushingInitialize = false;
        return pushResponseString;
        
    }// end pushXml()
    
    void zeroArrays(){
        int i, j;
        for(i=0; i<3; ++i)for(j=0; j<3; ++j){
            countInitOK[i][j] = 0;
            countInitError[i][j]= 0;
        }
        for(i=0; i<4; ++i)for(j=0; j<4; ++j){
            countOrderOK[i][j] = 0;
            countOrderError[i][j] = 0;
            countReportOK[i][j] = 0;
            countReportError[i][j] = 0;
        }
    }//end zeroArrays()
    
}// end class ValidateServer
