# Issues to investigate regarding the OpenC2SIM content

# C2SIM_SMX_LOX_v1.0.0.xsd

Missing types in MessageBodyType: SystemCommandBody

'ResetScenario' is not a valid value for SystemCommandTypeCodeType

SessionState missing from SystemCommandBodyType

# Client Library

C2SIMHeader is never instantiated - the place where that would happen (at GetNext_*) the population is premised on the existence 
of a protocol header in the header map. The but the latter is never instantiated, so the header remains null and the _messageBody 
property contains the full content C2SIM header and message body



