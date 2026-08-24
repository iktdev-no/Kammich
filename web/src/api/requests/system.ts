import type { PowerPermissionsDto, ActionResponse } from "../../types/types";
import { apiGet, apiPost } from "../client";


export function getPowerPermissions() {
    return apiGet<PowerPermissionsDto>(`/v1/system/power-permissions`)
}

export function executePowerOff() {
    return apiPost<null, ActionResponse>(`/v1/system/poweroff`, null)
}

export function executeReboot() {
    return apiPost<null, ActionResponse>(`/v1/system/reboot`, null)
}