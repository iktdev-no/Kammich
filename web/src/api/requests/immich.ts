import type { ImmichLoginRequest, ImmichServerConfig, ImmichServerConnection, ImmichServerFeatures, ImmichServerStorage, ImmichServerVersion, ImmichSupportedMediaTypes, ImmichUserAccesses, ImmichUserMe } from "../../types/types";
import { apiDelete, apiGet, apiPost } from "../client";


export const immichApi = {
    login: (loginRequest: ImmichLoginRequest) =>
        apiPost<ImmichLoginRequest, ImmichUserMe>("/v1/immich/login", loginRequest),

    getAccessMe: () =>
        apiGet<ImmichUserAccesses | null>("/v1/immich/access/me"),

    getAccessAll: () =>
        apiGet<ImmichUserAccesses[]>("/v1/immich/access/all"),

    deleteApiKey: (apiKeyId: string) =>
        apiDelete(`/v1/immich/api-keys/${apiKeyId}`),

    getServerUrl: () =>
        apiGet<ImmichServerConnection>("/v1/immich/server/url"),

    getServerVersion: () =>
        apiGet<ImmichServerVersion>("/v1/immich/server/version"),

    getServerMediaTypes: () =>
        apiGet<ImmichSupportedMediaTypes>("/v1/immich/server/supported-media-types"),

    getServerFeatures: () =>
        apiGet<ImmichServerFeatures>("/v1/immich/server/features"),

    getServerConfig: () =>
        apiGet<ImmichServerConfig>("/v1/immich/server/config"),

    getServerStorage: () =>
        apiGet<ImmichServerStorage>("/v1/immich/server/storage"),

    getUsers: () =>
        apiGet<ImmichUserMe[]>("/v1/immich/users"),

    changeUser: (userId: string) =>
        apiPost<null, boolean>(`/v1/immich/change/user/${userId}`, null),
};