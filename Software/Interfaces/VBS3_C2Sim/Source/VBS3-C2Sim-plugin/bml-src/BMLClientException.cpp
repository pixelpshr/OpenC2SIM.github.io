/*----------------------------------------------------------------*
|   Copyright 2009-2018 Networking and Simulation Laboratory      |
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
#include "stdafx.h"
#include "C2SIMClientLib.h"

#pragma once

#include <iostream>
#include <exception>
#include <sstream>
#include <fstream>
#include <codecvt>
#include <stdio.h>
#include <tchar.h>

BMLClientException::BMLClientException(std::string m) {
	msg = m;
	//throw exception(m); debugx
}

// BML Exception caused by another exception
BMLClientException::BMLClientException(std::string m, exception e) {
	msg = m;
	cause = e;
	//throw exception(m); debugx
}

/**
* Get message set in this exception when instantiated
* @return String - Message included in constructor
*/
std::string BMLClientException::getMessage() {
	return msg;
}   // getMessage()

/**
* Get message from another exception thrown by
* underlying software and included in this exception
* @return String - Underlying cause message
*/
std::string BMLClientException::getCauseMessage() const throw()
{
	try {
		return std::string(cause.what());
	}
	catch (const std::exception& e)
	{
		// there is no cause message
		return "";
	}
}// getCauseMessage()

BMLClientException::~BMLClientException() {}
