activateAddons [ 
  "vbs2_vehicles_land_wheeled_datsun_620_iq_datsun_620_technicals",
  "vbs2_vehicles_land_wheeled_mowag_piranha_lav3_nz_lav3",
  "vbs2_vehicles_water_boats_boghammar_ir_boghammar",
  "vbs2_vehicles_air_planes_aai_rq7_us_rq7",
  "vbs2_people_invisible_man",
  "vbs2_people_nz_nzdf_ifvcrew",
  "vbs2_people_wp_wp_rifleman",
  "vbs2_iq_insurgents"
];

_missionVersion = 9;
setMissionVersion 9;
if (!isNil "_map") then
{
	call compile preProcessFile "\vbs2\editor\Data\Scripts\init_global.sqf";
	initAmbientLife;
};

_func_COC_Create_Unit = fn_vbs_editor_unit_create;
_func_COC_Update_Unit = fn_vbs_editor_unit_update;
_func_COC_Delete_Unit = fn_vbs_editor_unit_delete;
_func_COC_Import_Unit = fn_vbs_editor_unit_import;
_func_COC_UpdatePlayability_Unit = fn_vbs_editor_unit_updatePlayability;
_func_COC_Create_Group = fn_vbs_editor_group_create;
_func_COC_Update_Group = fn_vbs_editor_group_update;
_func_COC_Delete_Group = fn_vbs_editor_group_delete;
_func_COC_Delete_Group_Only = fn_vbs_editor_group_deleteOnlyGroup;
_func_COC_Attach_Group = fn_vbs_editor_group_attach;
_func_COC_Group_OnCatChanged = fn_vbs_editor_group_onCatChanged;
_func_COC_Group_OnTypeChanged = fn_vbs_editor_group_onTypeChanged;
_func_COC_Group_OnNewCatChanged = fn_vbs_editor_group_onNewCatChanged;
_func_COC_Group_OnNewTypeChanged = fn_vbs_editor_group_onNewTypeChanged;
_func_COC_Group_OnCreateInit = fn_vbs_editor_group_createOnInit;
_func_COC_Group_Selected = fn_vbs_editor_group_groupSelected;
_func_COC_SubTeam_Join = fn_vbs_editor_subteam_join;
_func_COC_Waypoint_Assign = fn_vbs_editor_waypoint_assign;
_func_COC_Waypoint_Update = fn_vbs_editor_waypoint_update;
_func_COC_Waypoint_Draw = fn_vbs_editor_waypoint_draw;
_func_COC_Waypoint_Delete = fn_vbs_editor_waypoint_delete;
_func_COC_Waypoint_Move = fn_vbs_editor_waypoint_move;
_func_COC_Waypoint_Load_Branched = fn_vbs_editor_waypoint_loadBranched;
_func_COC_Waypoint_Find_Config = fn_vbs_editor_waypoint_findConfigEntry;
_func_COC_Marker_Create = fn_vbs_editor_marker_create;
_func_COC_Marker_Update = fn_vbs_editor_marker_update;
_func_COC_Marker_SetDrawIcons = fn_vbs_editor_marker_setDrawIcons;
_func_COC_Marker_DlgChanged = fn_vbs_editor_marker_dlgChanged;
_func_COC_Marker_Tactical_Create = fn_vbs_editor_marker_tactical_create;
_func_COC_Marker_Tactical_Update = fn_vbs_editor_marker_tactical_update;
_func_COC_Marker_Tactical_SetDrawIcons = fn_vbs_editor_marker_tactical_setDrawIcons;
_getCrew = fn_vbs_editor_vehicle_getCrew;
_func_COC_Vehicle_Create = fn_vbs_editor_vehicle_create;
_func_COC_Vehicle_Update = fn_vbs_editor_vehicle_update;
_func_COC_Vehicle_Occupy = fn_vbs_editor_vehicle_occupy;
_func_COC_Vehicle_Delete = fn_vbs_editor_vehicle_delete;
_func_COC_Vehicle_UnJoin = fn_vbs_editor_vehicle_unJoinGroup;
_func_COC_Vehicle_GetInEH = fn_vbs_editor_vehicle_getInEH;
_func_COC_Vehicle_GetOutEH = fn_vbs_editor_vehicle_getOutEH;
_func_COC_Vehicle_OnTypeChanged = fn_vbs_editor_vehicle_onTypeChanged;
_func_COC_Vehicle_UpdatePlayability = fn_vbs_editor_vehicle_updatePlayability;
_func_COC_Import_Vehicle = fn_vbs_editor_vehicle_import;
_func_COC_Vehicle_Set_Arcs = fn_vbs_editor_vehicle_setArcs;
_func_COC_Trigger_SetDisplayName = fn_vbs_editor_trigger_setDisplayName;
_func_COC_Trigger_Create = fn_vbs_editor_trigger_create;
_func_COC_IED_Create = fn_vbs_editor_IED_create;
_func_COC_Set_Display_Names = fn_vbs_editor_setDisplayNames;
_func_COC_Set_Color = fn_vbs_editor_setColor;
_func_COC_PlaceObjOnObj = fn_vbs_editor_placeObjOnObj;
_func_COC_Draw_Distance = fn_vbs_editor_distance_draw;
_func_COC_LookAt_Create = fn_vbs_editor_lookAt_create;
private["_allWaypoints"];

_group_0 = ["_group_0","1-1-A-1",[2296.92270, 2683.65332, 21.64701],"WEST","","","",0,[],"",false,false,"m2_1",""] call fn_vbs_editor_group_create;

_group_2 = ["_group_2","1-1-A-1",[3290.51495, 2761.98739, 24.19324],"GUER","","","",0,[],"",false,false,"m2_3",""] call fn_vbs_editor_group_create;

_group_3 = ["_group_3","1-1-A-2",[2798.45581, 3534.56268, 0.37401],"EAST","","","",0,[],"",false,false,"m2_4",""] call fn_vbs_editor_group_create;

_azimuth = -2.817;
if (false) then
{
	_azimuth = 0;
};
_vehicle_4 = [
 '_vehicle_4', true, "VBS2_IQ_Insurg_datsuntruck_pickup_02_pkm_X", [3290.51495, 2761.98739, 24.19324], [], 0, "CAN_COLLIDE", _azimuth, 'BadGuyGroup',
 1, 1, 1, "UNKNOWN", "UNLOCKED", "", 10, '_group_2', "BadGuyGroup", "YELLOW", "AWARE",
 true, 1, 'on', 'off', [], [], [], [],
 '', "", -1, -1, [], [],
 [-0.048496,0.98557,0.16215], [-0.28241,-0.16925,0.94425], "FALSE",
 [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
 "", "", "Ai",
 5169,
 '""',
 ["", "", "", false], "Pending"
] call fn_vbs_editor_vehicle_create;

_azimuth = 87.97;
if (false) then
{
	_azimuth = 0;
};
_vehicle_0 = [
 '_vehicle_0', true, "VBS2_NZ_Army_Lav3_W_25_X", [2296.92270, 2683.65332, 21.64701], [], 0, "CAN_COLLIDE", _azimuth, 'TESTMOBSQUAD',
 1, 1, 1, "UNKNOWN", "UNLOCKED", "", 10, '_group_0', "TESTMOBSQUAD", "YELLOW", "AWARE",
 true, 1, 'on', 'off', [], [], [], [],
 '', "", -1, -1, [], [],
 [0.98941,0.035074,-0.14085], [0.14213,-0.037076,0.98915], "FALSE",
 [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
 "", "", "Ai",
 5175,
 '""',
 ["", "", "", false], "Pending"
] call fn_vbs_editor_vehicle_create;

_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_vehicle_5 = [
 '_vehicle_5', true, "vbs2_ir_boghammar_x", [2798.45581, 3534.56268, 0.37401], [], 0, "CAN_COLLIDE", _azimuth, 'BadGuyBoat',
 1, 1, 1, "UNKNOWN", "DEFAULT", "", 0, '_group_3', "BadGuyBoat", "NO CHANGE", "AWARE",
 true, 1, 'on', 'off', [], [], [], [],
 '', "", -1, -1, [], [],
 [0,1,0], [-0,-0,1], "FALSE",
 [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
 "", "", "Ai",
 5178,
 '""',
 ["", "", "", false], "Pending"
] call fn_vbs_editor_vehicle_create;

_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_vehicle_6 = [
 '_vehicle_6', true, "vbs_us_army_rq7_gry_X", [2539.14674, 2414.94352, 13.63458], [], 0, "CAN_COLLIDE", _azimuth, 'TESTUAS',
 1, 1, 1, "UNKNOWN", "LOCKED", "", 0, '', "", "YELLOW", "AWARE",
 true, 1, 'on', 'off', [], [], [], [],
 '', "", -1, -1, [], [],
 [0,0.999,0.044743], [0,-0.044743,0.999], "FALSE",
 [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
 "", "", "Ai",
 5181,
 '""',
 ["", "", "", false], "Pending"
] call fn_vbs_editor_vehicle_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strCommander = "_vehicle_0";
_strGunner = "_vehicle_0";
_azimuth = 87.97;
if (false) then
{
	_azimuth = 0;
};
_unit_0 = (
[
	"_unit_0", true, "VBS2_NZ_Army_IFVCrew_W_SteyrIWc", [2296.92270, 2683.65332, 21.64701], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", true, "_group_0", "WEST", "LAV III Commander", [0,0], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '', 1,
 5184
] + [_group_0]) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strDriver = "_vehicle_5";
_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_unit_13 = (
[
	"_unit_13", true, "vbs2_wp_soldier_ak74", [2798.45581, 3534.56268, 0.37401], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", false, "_group_3", "EAST", "Boghammar Driver", [], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '', 1,
 5189
] + [_group_3]) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strGunner = "_vehicle_5";
_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_unit_14 = (
[
	"_unit_14", true, "vbs2_wp_soldier_ak74", [2798.45581, 3534.56268, 0.37401], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", false, "_group_3", "EAST", "Boghammar Gunner", [0], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '_unit_13', 1,
 5194
] + [_group_3]) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strDriver = "_vehicle_0";
_azimuth = 87.97;
if (false) then
{
	_azimuth = 0;
};
_unit_1 = (
[
	"_unit_1", true, "VBS2_NZ_Army_IFVCrew_W_SteyrIWc", [2296.92270, 2683.65332, 21.64701], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", true, "_group_0", "WEST", "LAV III Driver", [], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '_unit_0', 1,
 5199
] + [_group_0]) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strGunner = "_vehicle_0";
_azimuth = 87.97;
if (false) then
{
	_azimuth = 0;
};
_unit_2 = (
[
	"_unit_2", true, "VBS2_NZ_Army_IFVCrew_W_SteyrIWc", [2296.92270, 2683.65332, 21.64701], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", true, "_group_0", "WEST", "LAV III Gunner", [0], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '_unit_0', 1,
 5203
] + [_group_0]) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_unit_15 = (
[
	"_unit_15", true, "vbs2_invisible_man_freeCamera", [2292.40869, 2670.83313, 20.69625], [], 0, "CAN_COLLIDE", _azimuth, "invisibleCam", 1,
	1, -1, "UNKNOWN", "this moveInCargo TESTMOBSQUAD", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", true, "", "civ", "", [], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '', 1,
 5208
] + []) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strGunner = "_vehicle_4";
_azimuth = -2.817;
if (false) then
{
	_azimuth = 0;
};
_unit_11 = (
[
	"_unit_11", true, "vbs2_iq_insurg_03_akm", [3290.51495, 2761.98739, 24.19324], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", true, "_group_2", "EAST", "Technical - PKM / Datsun pickup, white, red stripe Gunner", [0], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '', 1,
 5212
] + [_group_2]) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strDriver = "_vehicle_6";
_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_unit_16 = (
[
	"_unit_16", true, "vbs2_invisible_man_west", [2539.14674, 2414.94352, 13.63458], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", false, "", "WEST", "RQ-7 Shadow Pilot", [], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '', 1,
 5217
] + []) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strGunner = "_vehicle_6";
_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_unit_17 = (
[
	"_unit_17", true, "vbs2_invisible_man_west", [2539.14674, 2414.94352, 13.63458], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", false, "", "WEST", "RQ-7 Shadow Operator", [0], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '', 1,
 5221
] + []) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_strDriver = "_vehicle_4";
_azimuth = -2.817;
if (false) then
{
	_azimuth = 0;
};
_unit_12 = (
[
	"_unit_12", true, "vbs2_iq_insurg_03_akm", [3290.51495, 2761.98739, 24.19324], [], 0, "CARGO", _azimuth, "", 1,
	1, -1, "UNKNOWN", "", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Ai", true, "_group_2", "EAST", "Technical - PKM / Datsun pickup, white, red stripe Driver", [], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '_unit_11', 1,
 5225
] + [_group_2]) call fn_vbs_editor_unit_create;

private ["_strCommander", "_strDriver", "_strGunner", "_strCargo"];
_strCommander = ""; _strDriver = ""; _strGunner = ""; _strCargo = "";
_azimuth = 0;
if (false) then
{
	_azimuth = 0;
};
_unit_18 = (
[
	"_unit_18", true, "vbs2_invisible_man_admin", [2804.21295, 3531.87607, 0.47099], [], 0, "CAN_COLLIDE", _azimuth, "invisBoat", 1,
	1, -1, "UNKNOWN", "this moveInCargo BadGuyBoat", "PRIVATE", 1, _strCommander, _strDriver, _strGunner, _strCargo, "Player", true, "", "civ", "", [], "", "YELLOW", "SAFE", "Auto", 1,
	0.77778, 0.2, 0.51778, 0.2, [], "", [], 0.75, 1.82, 0, false, "", 1, 0, '', 1,
 5230
] + []) call fn_vbs_editor_unit_create;

call compile preprocessFile "\vbs2\editor\data\scripts\group\finalizeGroups.sqf";
call compile preprocessFile "\vbs2\editor\data\scripts\waypoint\waypointsPrepare.sqf";
call compile preprocessFile "\vbs2\editor\data\scripts\waypoint\waypointsPrepareSynch.sqf";

if (isNil "_map") then {processInitCommands};
