# C2SIM Standard Materials

## Overview

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
* [C2SIM_SMX_LOX_v1.0.0.xsd](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51853) -  XML Schema Document generated from the merged C2SIM core, SMX, and LOX ontologies (merged using Protégé)
* [C2SIMOntologyToC2SIMSchemaV1.0.0.xslt](https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=51854)- extensible stylesheet language transformations (XSLT) file used to render the ontology into the xsd

Other schemas:
* [C2SIM_SMX_LOX_v1.0.0_Init_flat.xsd](Schemas/C2SIM_SMX_LOX_v1.0.0_Init_flat.xsd) - flattened schema covering Initialization documents
* [C2SIM_SMX_LOX_ASX_v1.0.0_Order_flat.xsd](Schemas/C2SIM_SMX_LOX_ASX_v1.0.0_Order_flat.xsd) - flattened schema covering Order documents (includes Autonomous Systems Extension)
* [C2SIM_SMX_LOX_ASX_v1.0.0_Report_flat.xsd](Schemas/C2SIM_SMX_LOX_ASX_v1.0.0_Order_flat.xsd) - flattened schema covering Report documents (includes Autonomous Systems Extension)
