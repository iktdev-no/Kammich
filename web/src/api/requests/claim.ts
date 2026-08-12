import type { DeviceOwnershipSummary, ImportJobOwnershipSummary } from "../../types/types"
import { apiGet, apiPost } from "../client"

export function claimImportJob(importJobId: string) {
    return apiPost<null, any>(`/v1/claim/import-job/${importJobId}`, null)
}

export function claimDeviceBySerial(deviceSerial: string) {
    return apiPost<null, any>(`/v1/claim/device/${deviceSerial}`, null)
}

export function getDevices() {
    return apiGet<Array<DeviceOwnershipSummary>>("/v1/claim/device")
}

export function getImportJobs() {
    return apiGet<Array<ImportJobOwnershipSummary>>("/v1/claim/import-job")
}