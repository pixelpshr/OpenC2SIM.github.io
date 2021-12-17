////////////////////////////////////////////////////////////////////
// Mission briefing initialization file.                          //
// DO NOT MODIFY.                                                 //
// For custom briefing initialization use file init_briefing.sqf. //
////////////////////////////////////////////////////////////////////

if (isServer) then
{
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

_unit_13 = unit_13;
_marker_3 = (["_marker_3","m2_4","1-1-A-2","TacticalMarker","","ColorRed",[20, 20],0,[2798.45581, 3534.56268, 0.37401],"playerSide == east","\vbs2\ui\tacticmarkers\data\Frames\Hostile_Surface","\vbs2\ui\tacticmarkers\data\Icons\H_Infantry","\vbs2\ui\tacticmarkers\data\Modifiers\S_Team","\vbs2\ui\tacticmarkers\data\SubRoles\Blanc",[0,0],1,[0,0],1,[0,0],1,[0,0],1,true,1] + ["_group_3"]) call fn_vbs_editor_marker_tactical_create;

_unit_0 = unit_0;
_marker_0 = (["_marker_0","m2_1","1-1-A-1","TacticalMarker","","ColorBlue",[20, 20],0,[2296.92270, 2683.65332, 21.64701],"playerSide == west","\vbs2\ui\tacticmarkers\data\Frames\Friend_Units","\vbs2\ui\tacticmarkers\data\Icons\Blanc","\vbs2\ui\tacticmarkers\data\Modifiers\S_Team","\vbs2\ui\tacticmarkers\data\SubRoles\Motorised",[0,0],1,[0,0],1,[0,0],1,[0,0],1,true,1] + ["_group_0"]) call fn_vbs_editor_marker_tactical_create;

_unit_11 = unit_11;
_marker_2 = (["_marker_2","m2_3","1-1-A-1","TacticalMarker","","ColorRed",[20, 20],0,[3290.51495, 2761.98739, 24.19324],"playerSide == resistance","\vbs2\ui\tacticmarkers\data\Frames\Hostile_Surface","\vbs2\ui\tacticmarkers\data\Icons\Blanc","\vbs2\ui\tacticmarkers\data\Modifiers\S_Team","\vbs2\ui\tacticmarkers\data\SubRoles\Motorised",[0,0],1,[0,0],1,[0,0],1,[0,0],1,true,1] + ["_group_2"]) call fn_vbs_editor_marker_tactical_create;

}
