import type { ImmichLoginRequest, ImmichUserAccesses, ImmichUserMe } from "../types/types";
import { apiDelete, apiGet, apiPost } from "./client";

export function immichLoginNormalFLow(loginRequest: ImmichLoginRequest) {
    return apiPost<ImmichLoginRequest, ImmichUserMe>(`/v1/immich/login`, loginRequest)
}

export function immichAccessAll() {
    return apiGet<Array<ImmichUserAccesses>>(`/v1/immich/access/all`)
}

export function immichDeleteApiKey(apiKeyId: string) {
    return apiDelete(`/v1/immich/api-keys/${apiKeyId}`)
}