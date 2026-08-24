import type { NetworkInterface } from "../../../types/types";
import { apiGet, apiPost } from "../../client";

export function getNetworkInterfaces(): Promise<Array<NetworkInterface>> {
    return apiGet<Array<NetworkInterface>>("/v1/networking/interfaces")
}

export function resetNetworkInterface(nif: string) {
    return apiPost<null, void>(`/v1/networking/interfaces/${nif}/reset`, null)
}