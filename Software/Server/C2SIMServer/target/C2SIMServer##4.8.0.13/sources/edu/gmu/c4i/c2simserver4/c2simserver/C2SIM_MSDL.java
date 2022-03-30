/*----------------------------------------------------------------*
|    Copyright 2001-2020 Networking and Simulation Laboratory     |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for academic purposes is hereby  |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
 *-----------------------------------------------------------------*/
package edu.gmu.c4i.c2simserver4.c2simserver;

import edu.gmu.c4i.c2simserver4.c2simserver.C2SIMException;
import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_Transaction;
import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_InitDB;
import edu.gmu.c4i.c2simserver4.c2simserver.C2SIM_InitDB.InitElement;
import java.util.List;
import java.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;


/**
 * <h1>C2SIM_MSDL</h1> <p>
 * Performs processing of MSDL messages
 *  Translates between MSDL and C2SIM
 * @author Douglas Corner - George Mason University C4I and  Center
 */
public class C2SIM_MSDL {

    /********************/
    /* process          */
    /********************/   
    /**
    * process - process incoming MSDL message
    @param trans - C2SIM_Transaction
    @throws C2SIMException 
    */
    static void process(C2SIM_Transaction trans) throws C2SIMException {

        addMSDLToC2SIM(trans);

    }   // process()


    /********************/
    /* addMSDLToC2SIM   */
    /********************/
    /*
        Process incoming MSDL.  
        Extract MSDL Units and ForceSides and convert to C2SIM
        MSDL Unit -> C2SIM Entity MilitaryOrganization
        MSDL ForceSide -> C2SIM Abstract Object ForceSice
     */
    /**
    * addMSDLToC2SIM - Extract data from MSDL message and add to stored C2SIM Initialization data
    @param t - C2SIM_Transaction
    */
    private static void addMSDLToC2SIM(C2SIM_Transaction t) {

        // Get the parsed document received in XML 
        Document msdlDoc = t.getDocument();
        Element msdlRoot = msdlDoc.getRootElement();


        // Get Namespace for MSDL and C2SIM
        Namespace nsMSDL = C2SIM_Util.msdl_NS;
        Namespace nsC2SIM = C2SIM_Util.c2sim_NS;

        // Get the Scenario Time
        Element scenarioTime = C2SIM_Util.findSingleElementSimple("Environment/ScenarioTime", msdlRoot, nsMSDL);      
        
        // Get list of MSDL ForceSies
        List<Element> msdlForceSides = C2SIM_Util.findElementSimple("ForceSides/ForceSide", msdlRoot, nsMSDL);

        // Loop through the Force Sides
        for (Element msdlForceSide : msdlForceSides) {

            // AbstractObject/ForceSide
            Element c2simAbstractObject = new Element("AbstractObject", nsC2SIM);
            Element c2simForceSide = new Element("ForceSide", nsC2SIM);
            c2simAbstractObject.addContent(c2simForceSide);

            // MSDL ForceSideName -> C2SIM Name
            Element msdlName = C2SIM_Util.findSingleElementSimple("ForceSideName", msdlForceSide, nsMSDL);
            if (msdlName != null)
                c2simForceSide.addContent(new Element("Name", nsC2SIM).setText(msdlName.getText()));

            // MSDL ObjectHandle -> C2SIM UUID
            Element msdlHandle = C2SIM_Util.findSingleElementSimple("ObjectHandle", msdlForceSide, nsMSDL);
            if (msdlHandle != null)
                c2simForceSide.addContent(new Element("UUID", nsC2SIM).setText(msdlHandle.getText()));

            // SideHostility Code MSDL doesn't have Hostility on a ForceSide so default to Unknown
            Element c2simFSRelation = new Element("ForceSideRelation", nsC2SIM);
            c2simFSRelation.addContent(new Element("HostilityStatusCode", nsC2SIM).setText("UNK"));
            c2simForceSide.addContent(c2simFSRelation);
            

            C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
            ie.element = c2simAbstractObject;
            ie.source = t.sender;
            C2SIM_Util.initDB.abstractObject.add(ie);

        }   // MSDL Force Sides

        // Get List of MSDL Units
        List<Element> msdlUnits = C2SIM_Util.findElementSimple("Organizations/Units/Unit", msdlRoot, nsMSDL);

        // Loop through the MSDL creating C2SIM MilitaryOrganizations
        for (Element msdlUnit : msdlUnits) {

            // Entity/ActorEntity/CollectiveEntity/MilitaryOrganization
            Element c2simEntity = new Element("Entity", nsC2SIM);
            Element c2simMilitaryOrganization = C2SIM_Util.createElementStack("ActorEntity/CollectiveEntity/MilitaryOrganization", c2simEntity, nsC2SIM);

            // CurrentState/PhysicalState
            Element c2simCurrentState = new Element("CurrentState", nsC2SIM);
            c2simMilitaryOrganization.addContent(c2simCurrentState);

            Element c2simPhysicalState = new Element("PhysicalState", nsC2SIM);
            c2simCurrentState.addContent(c2simPhysicalState);

            // DateTimeGroup - Present time (ISO)
            Element dt = new Element("IsoDateTime", nsC2SIM).setText(scenarioTime.getText());
            c2simPhysicalState.addContent(new Element("DateTime", nsC2SIM).addContent(dt));
             

            // Entity Health
            String msdlEffectivenessString = "";
            String c2simOperationalStatusCodeString = "";

            Element msdlCombatEffectiveness = C2SIM_Util.findSingleElementSimple("UnitSymbolModifiers/CombatEffectiveness", msdlUnit, nsMSDL);
            
            if (msdlCombatEffectiveness != null) {
                msdlEffectivenessString = msdlCombatEffectiveness.getText();

                // Map MSDL CombatEffectiveness to C2SIM OperationalStatus
                switch (msdlEffectivenessString) {

                    case "GREEN":
                        c2simOperationalStatusCodeString = "FullyOperational";
                        break;

                    case "AMBER":
                        c2simOperationalStatusCodeString = "MostlyOperational";
                        break;

                    case "RED":
                        c2simOperationalStatusCodeString = "PartlyOperational";
                        break;

                    case "BLACK":
                        c2simOperationalStatusCodeString = "NotOperational";
                        break;
                }   // switch OperationalStatus        

                // Add result to PhysicalState/EntityHealthStatus/OperationalHealthStatus/OperationalStatusCode
                Element c2simOperationalStatusCode = C2SIM_Util.createElementStack("EntityHealthStatus/OperationalStatus/OperationalStatusCode", c2simPhysicalState, nsC2SIM);
                c2simOperationalStatusCode.setText(c2simOperationalStatusCodeString);

            } // Entity Health
            
            // Get MSDL Location
            Element msdlLatitude = C2SIM_Util.findSingleElementSimple("Disposition/Location/CoordinateData/GDC/Latitude", msdlUnit, nsMSDL);
            Element msdlLongitude = C2SIM_Util.findSingleElementSimple("Disposition/Location/CoordinateData/GDC/Longitude", msdlUnit, nsMSDL);

            if (msdlLatitude != null) {

                // Location/Coordinate/GeodeticCoordinaate
                Element c2simGeodetic = C2SIM_Util.createElementStack("Location/GeodeticCoordinate", c2simPhysicalState, nsC2SIM);
                c2simGeodetic.addContent(new Element("Latitude", nsC2SIM).setText(msdlLatitude.getText()));
                if (msdlLongitude != null)
                    c2simGeodetic.addContent(new Element("Longitude", nsC2SIM).setText(msdlLongitude.getText()));

            }   // Location

            // EntityDescriptor/Side
            Element msdlForceSideHandle = C2SIM_Util.findSingleElementSimple("Relations/ForceRelation/ForceRelationData/ForceSideHandle", msdlUnit, nsMSDL);

            if (msdlForceSideHandle != null) {

                Element c2simEntityDescriptor = new Element("EntityDescriptor", nsC2SIM);
                c2simMilitaryOrganization.addContent(c2simEntityDescriptor);

                c2simEntityDescriptor.addContent(new Element("Side", nsC2SIM).setText(msdlForceSideHandle.getText()));
            }   // Entity Descriptor

            // EntityType

            Element msdlSymbolIdentifier = C2SIM_Util.findSingleElementSimple("SymbolIdentifier", msdlUnit, nsMSDL);
            if (msdlSymbolIdentifier != null) {

                // EntityType
                Element c2simEntityType = new Element("EntityType", nsC2SIM);
                c2simMilitaryOrganization.addContent(c2simEntityType);

                // APP6-SIDC/SIDCString
                Element c2simAPP6SIDC = new Element("APP6-SIDC", nsC2SIM);
                c2simEntityType.addContent(c2simAPP6SIDC);
                c2simAPP6SIDC.addContent(new Element("SIDCString", nsC2SIM).setText(msdlSymbolIdentifier.getText()));
            }

            Element msdlObjectHandle = C2SIM_Util.findSingleElementSimple("ObjectHandle", msdlUnit, nsMSDL);
            if (msdlObjectHandle != null) {
                c2simMilitaryOrganization.addContent(new Element("UUID", nsC2SIM).setText(msdlObjectHandle.getText()));
            }

            Element msdlName = C2SIM_Util.findSingleElementSimple("Name", msdlUnit, nsMSDL);
            if (msdlName != null)
                c2simMilitaryOrganization.addContent(new Element("Name", nsC2SIM).setText(msdlName.getText()));          

            // Add to Init Database
            C2SIM_InitDB.InitElement ie = new C2SIM_InitDB.InitElement();
            ie.element = c2simEntity;
            ie.source = t.sender;
            C2SIM_Util.initDB.entity.add(ie);

        }   // msdlUnits

    }   // addMSDLToC2SIM


    /************************/
    /* convertS2SIMToMSDL   */
    /************************/
    // Convert stored C2SIM Informaation in C2SIM_InitDB to a MSDL Document
    /**
    * convertC2SIMToMSDL - Convert stored C2SIM to MSDL for publication
    @param trans - C2SIM_Transaction
    @return - String MSDL translated from C2sIM
    @throws Exception 
    */
    public static String convertC2SIMToMSDL(C2SIM_Transaction trans) throws Exception {
        try {
            // Get Namespace for MSDL and C2SIM
            Namespace nsMSDL = C2SIM_Util.msdl_NS;
            Namespace nsC2SIM = C2SIM_Util.c2sim_NS;

            Document msdlDoc = new Document();

            // Root Element
            Element militaryScenario = new Element("MilitaryScenario", nsMSDL);
            msdlDoc.addContent(militaryScenario);

            /*
                Add (Constant) Option data to MSDL Root
            */

            // Options/ScenarioDataStandards
            Element scenarioDataStandards = C2SIM_Util.createElementStack("Options/ScenarioDataStandards", militaryScenario, nsMSDL);

            // ScenarioDataStandards/SymbologyDataStandard
            Element symbologyDataStandard = new Element("SymbologyDataStandard", nsC2SIM);
            scenarioDataStandards.addContent(symbologyDataStandard);

            // Add StandardName to symbologyDataStandard
            symbologyDataStandard.addContent(new Element("StandardName", nsC2SIM).setText("NATO_APPS-6"));

            // Add Major Version to symbologyDataStandard
            symbologyDataStandard.addContent(new Element("MajorVersion", nsC2SIM).setText("B"));

            // ScenarioDataStandard/CoordinateDataStandard 
            Element coordinateDataStandard = new Element("CoordinateDataStandard", nsMSDL);
            scenarioDataStandards.addContent(coordinateDataStandard);

            // CoordinateDataStandard/CoordinateSystemType
            coordinateDataStandard.addContent(new Element("CoordinateSystemType", nsMSDL).setText("GDC"));

            // CoordinateDataStandard/CoordinageSystemDatum
            coordinateDataStandard.addContent(new Element("CoordinateSystemDatum", nsMSDL).setText("WGS84"));

            // Add Environment Data to root       
            Element environment = new Element("Environment", nsMSDL);
            militaryScenario.addContent(environment);
            environment.addContent(new Element("ScenarioTime", nsMSDL).setText("2019-01-01T06:00:00Z"));

            /*
                Add ForceSides to Root
             */

            Element msdlForceSides = new Element("ForceSides", nsMSDL);
            militaryScenario.addContent(msdlForceSides);

            // Get Vector of C2SIM Abstract Objects from Init DB and work through the Vector
            Vector<InitElement> c2simAbstractObjects = C2SIM_Util.initDB.abstractObject;

            for (InitElement abstractObject : c2simAbstractObjects) {
                Element c2simForceSide = abstractObject.element.getChild("ForceSide", nsC2SIM);
                if (c2simForceSide != null) {

                    // Get UUID and Name(s)
                    Element c2simForceSideUUID = C2SIM_Util.findSingleElementSimple("UUID", c2simForceSide, nsC2SIM);
                    List<Element> c2simForceSideName = C2SIM_Util.findElementSimple("Name", c2simForceSide, nsC2SIM);

                    // Add UUID and first Name to MSDL                   
                    Element msdlForceSide = new Element("ForceSide", nsMSDL);
                    msdlForceSides.addContent(msdlForceSide);

                    if (c2simForceSideUUID != null)
                        msdlForceSide.addContent(new Element("ObjectHandle", nsMSDL).setText(c2simForceSideUUID.getText()));

                    if (c2simForceSideName.size() != 0)
                        msdlForceSide.addContent(new Element("ForceSideName", nsMSDL).setText(c2simForceSideName.get(0).getText()));

                }   // ForceSide
            }   // AbstractObjects
            /*
                Add Units to Root
             */

            // Add Organizations to Root
            Element msdlOrganizations = new Element("Organizations", nsMSDL);
            militaryScenario.addContent(msdlOrganizations);

            // Add Units to Organizations
            Element msdlUnits = new Element("Units", nsMSDL);
            msdlOrganizations.addContent(msdlUnits);

            // Get Vector of C2SIM Entities from Init DB and work through the Vector
            Vector<InitElement> c2simEntities = C2SIM_Util.initDB.entity;

            for (InitElement c2simEntity : c2simEntities) {

                // Position to the MilitaryOrganization
                Element c2simMilitaryOrganization = C2SIM_Util.findSingleElementSimple("ActorEntity/CollectiveEntity/MilitaryOrganization", c2simEntity.element, nsC2SIM);

                // Is this entity a MilitaryOrganization?
                if (c2simMilitaryOrganization != null) {

                    // Add MSDL Unit to MSDL Units
                    Element msdlUnit = new Element("Unit", nsMSDL);
                    msdlUnits.addContent(msdlUnit);

                    // Object Handle
                    Element c2simUUID = C2SIM_Util.findSingleElementSimple("UUID", c2simMilitaryOrganization, nsC2SIM);
                    if (c2simUUID != null)
                        msdlUnit.addContent(new Element("ObjectHandle", nsMSDL).setText(c2simUUID.getText()));

                    // Symbol Identifier
                    Element c2simSymbol = C2SIM_Util.findSingleElementSimple("EntityType/APP6-SIDC/SIDCString", c2simMilitaryOrganization, nsC2SIM);
                    if (c2simSymbol != null)
                        msdlUnit.addContent(new Element("SymbolIdentifier", nsMSDL).setText(c2simSymbol.getText()));

                    // Unit Name
                    Element c2simUnitName = C2SIM_Util.findSingleElementSimple("Name", c2simMilitaryOrganization, nsC2SIM);
                    if (c2simUnitName != null)
                        msdlUnit.addContent(new Element("Name", nsMSDL).setText(c2simUnitName.getText()));

                    // Unit Symbol Modifiers
                    Element msdlUnitSymbolModifiers = new Element("UnitSymbolModifiers", nsMSDL);
                    msdlUnit.addContent(msdlUnitSymbolModifiers);

                    // Echelon
                    msdlUnitSymbolModifiers.addContent(new Element("Echelon", nsMSDL).setText("COMPANY"));

                    // Operational Status / Combat Effectiveness
                    Element c2simOperationalStatus = C2SIM_Util.findSingleElementSimple("CurrentState/PhysicalState/EntityHealthStatus/OperationalStatus", c2simMilitaryOrganization, nsC2SIM);
                    if (c2simOperationalStatus != null) {

                        String msdlOperationalStatusString = "";
                        String c2simOperationalStatusString = c2simOperationalStatus.getText();

                        // Map C2SIM OperationalStatus to MSDL
                        switch (c2simOperationalStatusString) {
                            case "FullyOperational":
                                msdlOperationalStatusString = "GREEN";
                                break;
                            case "MostlyOperational":
                                msdlOperationalStatusString = "AMBER";
                                break;
                            case "PartlyOperational":
                                msdlOperationalStatusString = "RED";
                                break;
                            case "NotOperational":
                                msdlOperationalStatusString = "BLACK";
                                break;
                            default:
                                msdlOperationalStatusString = "WHITE";
                        }   // switch OperationalStatus

                        msdlUnitSymbolModifiers.addContent(new Element("CombatEffectiveness", nsMSDL).setText(msdlOperationalStatusString));
                    }   // c2simOperationalHealthStatus                

                    // Higher Formation
                    String c2simSuperiorUUID = "";
                    Element c2simSuperiorName = null;
                    Element c2simSuperiorUnit = null;

                    Element c2simSuperior = C2SIM_Util.findSingleElementSimple("EntityDescriptor/Superior", c2simMilitaryOrganization, nsC2SIM);
                    if (c2simSuperior != null) {
                        c2simSuperiorUUID = c2simSuperior.getText();
                        // Get the superior Entity via the unitMap
                        c2simSuperiorUnit = C2SIM_Util.unitMap.get(c2simSuperiorUUID);

                        // Get the Superior unit name from the Entity
                        if (c2simSuperiorUnit != null) {
                            c2simSuperiorName = C2SIM_Util.findSingleElementSimple("Name", c2simSuperiorUnit, nsC2SIM);
                            if (c2simSuperiorName != null)
                                msdlUnitSymbolModifiers.addContent(new Element("HigherFormation", nsMSDL).setText(c2simSuperiorName.getText()));
                        }   // c2simSuperiorUnit
                    }   // c2simSuperior / HigherFormation

                    if (c2simUnitName != null)
                        msdlUnitSymbolModifiers.addContent(new Element("UniqueDesignation", nsMSDL).setText(c2simUnitName.getText()));

                    // Get c2sim latitude and longitude 
                    Element c2simLatitude = C2SIM_Util.findSingleElementSimple("CurrentState/PhysicalState/Location/GeodeticCoordinate/Latitude", c2simMilitaryOrganization, nsC2SIM);
                    Element c2simLongitude = C2SIM_Util.findSingleElementSimple("CurrentState/PhysicalState/Location/GeodeticCoordinate/Longitude", c2simMilitaryOrganization, nsC2SIM);

                    // Do we have location imformation
                    if (c2simLatitude != null) {

                        // Disposition/Location/Coordinates
                        Element msdlDisposition = new Element("Disposition", nsMSDL);
                        msdlUnit.addContent(msdlDisposition);

                        Element msdlLocation = new Element("Location", nsMSDL);
                        msdlDisposition.addContent(msdlLocation);

                        msdlLocation.addContent(new Element("CoordinateChoice", nsMSDL).setText("GDC"));

                        Element msdlCoordinateData = new Element("CoordinateData", nsMSDL);
                        msdlLocation.addContent(msdlCoordinateData);

                        Element msdlGDC = new Element("GDC", nsMSDL);
                        msdlLocation.addContent(msdlGDC);

                        msdlGDC.addContent(new Element("Latitude", nsMSDL).setText(c2simLatitude.getText()));

                        if (c2simLongitude != null)
                            msdlGDC.addContent(new Element("Longigude", nsMSDL).setText(c2simLongitude.getText()));

                    }   // Location
                    /*
                    Relations
                     */

                    if (c2simSuperiorUnit != null) {
                        Element msdlRelations = new Element("Relations", nsMSDL);
                        msdlUnit.addContent(msdlRelations);

                        // ForceRelation
                        Element msdlForceRelation = new Element("ForceRelation", nsMSDL);
                        msdlRelations.addContent(msdlForceRelation);

                        // ForceRelationChoice
                        msdlForceRelation.addContent(new Element("ForceRelationChoice", nsMSDL).setText("UNIT"));

                        // Force Relations Data
                        Element msdlForceRelationData = new Element("ForceRelationData", nsMSDL);
                        msdlForceRelation.addContent(msdlForceRelationData);

                        // Command Relation
                        Element msdlCommandRelation = new Element("CommandRelation", nsMSDL);
                        msdlForceRelation.addContent(msdlCommandRelation);

                        msdlCommandRelation.addContent(new Element("CommandingSuperiorHandle", nsMSDL).setText(c2simSuperiorUUID));
                        msdlCommandRelation.addContent(new Element("CommandRelationshipType", nsMSDL).setText("ATTACHED"));
                        // Command Superior Handle  
                    }   // Force Relations

                    /*
                    Model
                     */
                    Element msdlModel = new Element("Model", nsMSDL);
                    msdlUnit.addContent(msdlModel);

                    msdlModel.addContent(new Element("Resolution", nsMSDL).setText("STANDARD"));
                    msdlModel.addContent(new Element("AggregaateBased", nsMSDL).setText("true"));

                }   // Military Organization
            }   // Entities

            // Debugging 
            //String xml = C2SIM_Util.xmlToStringD(msdlDoc, null);


            // Generate xml string
            String xml = C2SIM_Util.xmlToStringD(msdlDoc, trans);
            return xml;

        }   // try
        catch (Exception e) {
            StackTraceElement[] st = e.getStackTrace();
            throw new C2SIMException("Error while converting C2SIM to MSDL" + e);
        }   // catch   // catch

    }   // convertC2SIMToMSDL())


}   // C2SIM_MSDL CLASS
