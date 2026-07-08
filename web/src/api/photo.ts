import type { PagedResponse, RemoteFile } from "../types/types";
import { apiGet } from "./client";

export function getPhotos(page: number, size: number) {
    return apiGet<PagedResponse<RemoteFile>>(`/photo`, {
        page, size
    });
}