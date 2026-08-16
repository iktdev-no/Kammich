import type { ImmichLoginRequest, ImmichServerConfig, ImmichServerConnection, ImmichServerFeatures, ImmichServerStorage, ImmichServerVersion, ImmichSupportedMediaTypes, ImmichUserAccesses, ImmichUserMe } from "../../types/types";
import { apiDelete, apiGet, apiPost } from "../client";

export function immichLoginNormalFLow(loginRequest: ImmichLoginRequest) {
    return apiPost<ImmichLoginRequest, ImmichUserMe>(`/v1/immich/login`, loginRequest)
}

export function immichAccessAll() {
    return apiGet<Array<ImmichUserAccesses>>(`/v1/immich/access/all`)
}

export function immichDeleteApiKey(apiKeyId: string) {
    return apiDelete(`/v1/immich/api-keys/${apiKeyId}`)
}

export function immichUrl() {
    return apiGet<ImmichServerConnection>("/v1/immich/server/url")
}

export function immichVersion() {
    return apiGet<ImmichServerVersion>("/v1/immich/server/version")
}

export function immichMediaTypes() {
    return apiGet<ImmichSupportedMediaTypes>("/v1/immich/server/supported-media-types")
}

export function immichFeatures() {
    return apiGet<ImmichServerFeatures>("/v1/immich/server/features")
}

export function immichConfig() {
    return apiGet<ImmichServerConfig>("/v1/immich/server/config")
}

export function immichStorage() {
    return apiGet<ImmichServerStorage>("/v1/immich/server/storage")
}

export function immichUsers() {
    return apiGet<Array<ImmichUserMe>>("/v1/immich/users")
}

export function immichChangeUser(userId: string) {
    return apiPost<null, boolean>(`/v1/immich/change/user/${userId}`, null)
}