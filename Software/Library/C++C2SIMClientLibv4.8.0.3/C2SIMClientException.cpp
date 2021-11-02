/*----------------------------------------------------------------*
|   Copyright 2009-2020 Networking and Simulation Laboratory      |
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
#include "C2SIMClientLib2.h"

#pragma once

#include <iostream>
#include <exception>
#include <sstream>
#include <fstream>
#include <codecvt>
#include <stdio.h>
#include <tchar.h>

namespace {
	std::string msg;
	std::exception cause;
}

C2SIMClientException::C2SIMClientException(std::string& m) {
	msg = m;
}

// C2SIM Exception caused by another exception
C2SIMClientException::C2SIMClientException(std::string& m, exception e) {
	msg = m;
	cause = e;
}

/**
* Get message set in this exception when instantiated
* @return String - Message included in constructor
*/
const char*  C2SIMClientException::getMessage() {
	return msg.c_str();
}   // getMessage()

/**
* Get message from another exception thrown by
* underlying software and included in this exception
* @return String - Underlying cause message
*/
const char*  C2SIMClientException::getCauseMessage() const throw()
{
	return cause.what();

}// getCauseMessage()

C2SIMClientException::~C2SIMClientException() {}
