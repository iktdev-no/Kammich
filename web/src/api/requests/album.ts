import type { AlbumCreateRequest, AlbumUpdateRequest, Album } from "../../types/types";
import { apiGet, apiPost, apiPatch, apiDelete } from "../client";

export function getAlbums() {
    return apiGet<Album[]>("/v1/album");
}

export function createAlbum(data: AlbumCreateRequest) {
    return apiPost<AlbumCreateRequest, number>("/v1/album", data);
}

export function updateAlbum(id: number, data: AlbumUpdateRequest) {
    return apiPatch<AlbumUpdateRequest, void>(`/v1/album/${id}`, data);
}

export function deleteAlbum(id: number) {
    return apiDelete<void>(`/v1/album/${id}`);
}

export function syncAlbumWithFIles(id: number) {
    return apiPost<null, void>(`/v1/album/${id}/sync-timeslot`, null)
}