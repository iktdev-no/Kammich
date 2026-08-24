import type { UploadSummary, UploadJobSummary } from "../../types/types";
import { apiGet, apiPost } from "../client";

export function checkQueue(userId: string) {
    return apiGet<void>(`/v1/upload/user/${userId}`)
}

export function resetUserQueue(userId: string) {
    return apiPost<undefined, void>(`/v1/upload/user/${userId}/reset`, undefined)
}

export function resetJobQueue(userId: string, jobId: string) {
    return apiPost<undefined, void>(`/v1/upload/user/${userId}/reset/${jobId}`, undefined)
}


export function getStats(userId: string) {
    return apiGet<UploadSummary>(`/v1/upload/user/${userId}/stats`)
}

export function getJobs(userId: string) {
    return apiGet<UploadJobSummary[]>(`/v1/upload/user/${userId}/jobs`)
}

export function uploadFile(userId: string, fileId: number) {
    return apiPost<undefined, void>(`/v1/upload/user/${userId}/upload/${fileId}`, undefined)
}