This directory holds the XML Schema Definitions (XSD) for
the current draft Integrated Battle Management Language schema 
version 2 under development by George Mason University C4I Center.

There are three types of xsd files:

1. Files with no version number are the same in IBMLv1.0,
IBMLv2.0, and IBMLv2.1.

2. Files marked v2.1 contain the latest version of 
IBMLv2.1. This was achieved by updating the previous 
IBMLv2.0 in accordance with recommendations in ACS technical 
report "Comparison of Army and NATO OPORDs31Mar2010".

3. IBMLv2.0 that was updated had been derived as follows:

a. posting all changes recommended by the July 2009 review

b. validating that review by sending the v2.0draft to 
all participants, and posting their recommended changes
in FiveWTypes:

- extend <ActionTemporalAssociationCategoryCodeXmlType> by 
  adding enumerations STRBEF, STRDUR, STRAFT, ENDBEF, ENDDUR, 
  ENDAFT and restricting the resulting coding, for BML use, 
  to those five plus STRSTR, STREND, ENDSTR, and ENDEND

- extend <ActionObjectiveQualifierCodeXmlType> by  adding
  enumerations AT, ALONG, BETWEEN, FROM, IN, ON and TO and
  restricting coding, for BML use, to those seven

- adding to <EventType><Who> attribute minOccurs="0"

- removing <RouteWhereType><Along> and adding an element 
  <RouteWhereType><RouteID> that can either (1) serve as 
  a way to refer to the route in <From-Via-To> or 
  (2) refer to a previously saved route, so the 
  <From-Via-To> is not needed

- adding <TaskQualifierType> as an enumerated type and 
  adding to <WhyType> an optional element <TaskQualifier> 
  of <TaskQualifierTYpe>

c. in JBMLOrderTypes_IBML, modifying OrderType to include
additional elements found to be important by MSG-048 as
reflected in their 2009 schema

d. folding the GDC coordinates from Coordinates.xsd into FiveWTypes

