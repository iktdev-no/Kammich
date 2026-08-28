import type { PowerPermissionsDto, ActionResponse, Version } from "../../types/types";
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

export function getKammichBackendVersion() {
    return apiGet<Version>(`/v1/update`)
}

export function requestKammichBackendUpdate() {
    return apiPost<null, never>(`/v1/update`, null)
}