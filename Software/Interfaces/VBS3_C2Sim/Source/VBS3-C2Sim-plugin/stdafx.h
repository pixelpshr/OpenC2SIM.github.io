// Copyright 2019 Defence Technology Agency,
// New Zealand Defence Force
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// and associated documentation files(the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions :
// 1.	The above copyright notice and this permission notice shall be included in all copies or substantial
//      portions of the Software;
// 2.	the Name of Defence Technology Agency and New Zealand Defence Force not be used in advertising or 
//      publicity  pertaining to distribution of the software without written prior permission.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
// LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
// OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once

#include "targetver.h"

#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
// Windows Header Files
#include <windows.h>
#include <string>
#include <cmath>
#include <set>
#include <map>
#include <chrono>
#include <fstream>

// reference additional headers your program requires here
#define VBSPLUGIN_EXPORT __declspec(dllexport)

VBSPLUGIN_EXPORT void WINAPI RegisterCommandFnc(void *executeCommandFnc);
VBSPLUGIN_EXPORT void WINAPI OnSimulationStep(float deltaT);
VBSPLUGIN_EXPORT const char* WINAPI PluginFunction(const char *input);

// Command function declaration
typedef int (WINAPI * ExecuteCommandType)(const char *command, char *result, int resultLength);

// Command function definition
extern ExecuteCommandType vbsExecuteCommand;

extern std::ofstream outFile;
