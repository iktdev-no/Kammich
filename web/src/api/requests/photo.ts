import type { PagedResponse, PhotoDevice, RemoteFile } from "../../types/types";
import { apiGet } from "../client";

export function getPhotos(page: number, size: number, sn?: string) {
    const endpoint = sn ? `/v1/photo/${sn}` : `/v1/photo`;
    return apiGet<PagedResponse<RemoteFile>>(endpoint, {
        page,
        size
    });
}

export function getPhotoUrl(
    photo: RemoteFile,
    options?: { width?: number; fit?: string; auto?: string }
): string {
    // Vi bygger base-URLen basert på din korrigerte v1-path
    let url = `/api/v1/photo/${photo.deviceId}/${photo.fileName}`;

    const queryParams: string[] = [];

    if (options?.width) queryParams.push(`w=${options.width}`);
    if (options?.fit) queryParams.push(`fit=${options.fit}`);
    if (options?.auto) queryParams.push(`auto=${options.auto}`);

    if (queryParams.length > 0) {
        url += `?${queryParams.join("&")}`;
    }

    return url;
}

export function getPhotoThumbUrl(
    photo: RemoteFile,
    options?: { width?: number; fit?: string; auto?: string }
): string {
    // Vi bygger base-URLen basert på din korrigerte v1-path
    let url = `/api/v1/photo/${photo.deviceId}/thumb/${photo.fileName}`;

    const queryParams: string[] = [];

    if (options?.width) queryParams.push(`w=${options.width}`);
    if (options?.fit) queryParams.push(`fit=${options.fit}`);
    if (options?.auto) queryParams.push(`auto=${options.auto}`);

    if (queryParams.length > 0) {
        url += `?${queryParams.join("&")}`;
    }

    return url;
}

export function getPhotoDevices() {
    return apiGet<Array<PhotoDevice>>(`/v1/photo/devices`)
}