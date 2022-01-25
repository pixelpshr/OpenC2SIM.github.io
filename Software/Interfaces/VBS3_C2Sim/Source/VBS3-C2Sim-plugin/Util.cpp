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

#include "stdafx.h"
#include "Util.h"
#include <sstream>
#include <chrono>

using namespace Util;

void Util::vbsArrayToStrVector(char* vs, std::vector<std::string> & vec) {

	std::string ans(vs);
	ans.erase(ans.find(']'), 1);
	ans.erase(ans.find('['), 1);
	std::istringstream ss(ans);
	std::string value;
	while (std::getline(ss, value, ',')) {
		vec.push_back(value);
	}
}

void Util::vbsArrayToDoubles(char*vs, double* ds, int len) {

	std::string st(vs);
	// replace the [ ] with ' '
	st[st.find(']')] = ' ';
	st[st.find('[')] = ' ';
	std::istringstream ss(st);
	std::string val;
	int i = 0;
	while (std::getline(ss, val, ',') && (len < i) ){
		ds[i++] = std::stod(val);
	}

}

void Util::vbsParseLatLonArray(char* vs, double* lat, double* lon) {

	std::string st(vs);
	for (int i = 0; i < st.length(); i++) {
		if (st[i] == '[' || st[i] == ']' || st[i] == '\"' || st[i] == ',') {
			st[i] = ' ';
		}
	}

	std::istringstream ss(st);

	double slat, slon;
	std::string north, east;
	ss >> slat >> north >> slon >> east;

	if (north[0] == 'S') {
		slat *= (-1);
	}
	if (east[0] == 'W') {
		slon *= (-1);
	}

	*lat = slat;
	*lon = slon;

}

std::string Util::latLonString(double lat, double lon, bool includeBrackets=true){

	std::string ll;

	if (includeBrackets) ll = "[\"";
	else ll = "\"";
	ll.append(std::to_string(std::abs(lat))).append(" ");
	if (lat >= 0) ll.append("N");
	else ll.append("S");
	ll.append("\", \"");
	ll.append(std::to_string(std::abs(lon))).append(" ");
	if (lon >= 0) ll.append("E");
	else ll.append("W");
	if (includeBrackets) ll.append("\"]");
	else ll.append("\"");

	return ll;

}

std::string Util::makeUUID(int i) {
	char end[3];
	sprintf_s(end, 4, "%03X", i);
	std::string uuid = "00000000-0000-0001-";
	uuid.append(end).append("0-000000000000");
	return uuid;
}

std::string Util::isoTime(std::chrono::system_clock::time_point now) {

	time_t time = std::chrono::system_clock::to_time_t(now);
	struct tm newtime;
	char buf[26];

	_gmtime64_s(&newtime, &time);
	strftime(buf, 26, "%FT%TZ", &newtime);

	std::string timeString(buf);
	return timeString;
}

std::string Util::isoTimeNow() {
	auto now = std::chrono::system_clock::now();
	return isoTime(now);
}

std::chrono::system_clock::time_point Util::sysTimeChrono() {
	return std::chrono::system_clock::now();
}

long long Util::sysTimeMillis() {
	auto now = std::chrono::system_clock::now();
	std::chrono::milliseconds millis = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());
	return millis.count();
}

std::chrono::system_clock::time_point Util::timeFromMillis(long long millis) {
	std::chrono::milliseconds duration(millis);
	std::chrono::time_point<std::chrono::system_clock> timePt(duration);
	return timePt;
}

