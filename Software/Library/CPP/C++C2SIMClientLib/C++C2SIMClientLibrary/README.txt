/*----------------------------------------------------------------*
|      Copyright 2022 Networking and Simulation Laboratory        |
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

This directory contains the beta open source Windows implementation 
of the C++ C2SIMClientSTOMP_Lib and C2SIMClientREST_Lib from the GMU
C4I & Cyber Center. These are C++ clones of the Java versions
on https://openc2sim.github.io

To use the library copy the .lib files from build64 and use header
file C2SIMClientLib2.h.

To build you will need to provide subdirectory boost.

version 4.8.0.6 correct order of C2SIMHeader parameters to match
C2SIM_SMX_LOX_v1.0.1 schema

