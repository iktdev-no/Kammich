import type { NetworkInterface } from "../../../types/types";
import { apiGet, apiPost } from "../../client";

export const networkingApi = {
    getInterfaces: (): Promise<NetworkInterface[]> =>
        apiGet<NetworkInterface[]>("/v1/networking/interfaces"),

    resetInterface: (nif: string): Promise<void> =>
        apiPost<null, void>(`/v1/networking/interfaces/${nif}/reset`, null),
};