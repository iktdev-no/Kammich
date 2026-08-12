import type { DeviceImport, DeviceImportJobSummary } from "../../types/types";
import { apiGet, apiPost } from "../client";


export function getActiveImports() {
    return apiGet<Array<DeviceImport>>("/v1/import")
}

export function getHistoricalImports() {
    return apiGet<Array<DeviceImportJobSummary>>("/v1/import/history")
}

export function getHistoricalImportFiles(jobId: string) {
    return apiGet<Array<string>>(`/v1/import/history/${jobId}`)
}


export function cancelImportFor(deviceId: string) {
    return apiPost<null, void>(`/v1/import/cancel/device/${deviceId}`, null)
}

export function cancelImportForAll() {
    return apiPost<null, void>(`/v1/import/cancel/all`, null)
}