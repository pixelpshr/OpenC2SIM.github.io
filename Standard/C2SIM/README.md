# C2SIM Standard Materials

## Overview

Command and Control Systems to Simulation Systems Interoperation (C2SIM) is defined in [SISO-STD-019-2020]((https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51765) 
as a standard for expressing 
and  exchanging  Command  and  Control  (C2)  information  among  C2  systems,  simulation  systems,  and  
robotic and autonomous (RAS) systems in a coalition context. 
Military operations in today’s world are increasingly driven towards coalition participation and as such are 
dependent  on  effective  interoperation  among  participating  coalition  systems.  The  growth  of  digitized  C2  
systems  and  the  need  for  coalition  interoperation  has  created  a  need  for  standards  to  represent  and  
exchange digitized C2 information and for these systems to interoperate. 

The C2SIM stadard is defined by a *Core* that can be extended. Current extensions include:
1. SMX - Standard Military Extention
1. LOX - Land Operations Extension
1. ASX - Autonomous Systems Extension

## SISO Standard Products

[SISO Standard Products](https://www.sisostds.org/DigitalLibrary.aspx?EntryId=51761)
* [SISO-GUIDE-010-2020](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51762)- C2SIM standard guidance
* [SISO-STD-019-2020](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51765) - General C2SIM standard definitions
* [SISO-STD-020-2020 LOX](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51764) - Land Operations Extension definitions

## Ontologies

The master documents are the rdf ontologies , available on the [SISO Digital Library site](https://www.sisostds.org/DigitalLibrary.aspx?EntryId=51848):
* [C2SIM_v1.0.0](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51850) - Core ontology
* [C2SIM_SMX_v1.0.0](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51851) - Standard Military Extension
* [C2SIM_LOX_v1.0.0](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51852) - Land Operations Extension

[Protégé](https://protege.stanford.edu/) can be used to browse these ontologies. The higher ontology layers SMX and LOX require access to the lower layers (for example the Core C2SIM_v1.0.0.rdf). When using Protégé, this can be achieved by including C2SIM_SMX_v1.0.0.rdf in the same directory/folder as C2SIM_SMX_v1.0.0.rdf and C2SIM_LOX_v1.0.0.rdf. Protégé will then find the lower layers automatically

## XML Schemas

In brief, C2SIM, including SMX and LOX extensions, define three primary types of documents:
1. Initialization - symbol definitions, including type and location
1. Orders - tasking information
1. Reports - execution reports

Main published XML schema (xsd):
* [C2SIM_SMX_LOX_v1.0.1.xsd](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=53123) -  XML Schema Document generated from the merged C2SIM core, SMX, and LOX ontologies (merged using Protégé) - [local unzipped copy](https://github.com/hyssostech/OpenC2SIM.github.io/blob/4ad6ccd41d12b0003a78f6c03ef7cbc157e699c1/Standard/C2SIM/Schemas/C2SIM_SMX_LOX_V1.0.1.xsd)
* [C2SIMOntologyToC2SIMSchemaV1.0.1.xslt](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=53123)- extensible stylesheet language transformations (XSLT) file used to render the ontology into the xsd - [local unzipped copy](https://github.com/hyssostech/OpenC2SIM.github.io/blob/4ad6ccd41d12b0003a78f6c03ef7cbc157e699c1/Standard/C2SIM/Schemas/C2SIMOntologyToC2SIMSchemaV1.0.1.xslt)

Other schemas:
* [C2SIM_SMX_LOX_v1.0.0_Init_flat.xsd](Schemas/C2SIM_SMX_LOX_v1.0.0_Init_flat.xsd) - flattened schema covering Initialization documents
* [C2SIM_SMX_LOX_ASX_v1.0.0_Order_flat.xsd](Schemas/C2SIM_SMX_LOX_ASX_v1.0.0_Order_flat.xsd) - flattened schema covering Order documents (includes Autonomous Systems Extension)
* [C2SIM_SMX_LOX_ASX_v1.0.0_Report_flat.xsd](Schemas/C2SIM_SMX_LOX_ASX_v1.0.0_Order_flat.xsd) - flattened schema covering Report documents (includes Autonomous Systems Extension)
* [C2SIM_SMX_LOX_v1.0.0_Command_augmented.xsd](https://github.com/hyssostech/OpenC2SIM.github.io/blob/d9a1a01522201f8d5af1106e42049dc65d4539d2/Standard/C2SIM/Schemas/C2SIM_SMX_LOX_v1.0.0_Command_augmented.xsd) - not part of the original OpenC2SIM repository, and it was extracted from the all encompassing C2SIM_SMX_LOX_v1.0.0.xsd. Additional elements had then to be included to make it comply with the actual content of messages received from a reference server
