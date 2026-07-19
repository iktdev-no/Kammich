import type { WirelessInterface, WifiNetworkConnection, WifiNetwork } from "../../types/types"
import { apiGet, apiPost } from "../client"

const endpoint = "/v1/wifi/client"

export function getState() {
    return apiGet<Array<WifiNetworkConnection>>(`${endpoint}/state`)
}

export function getInterfaces() {
    return apiGet<Array<WirelessInterface>>(`${endpoint}/interfaces`)
}

export function getNetworks(ifName: string) {
    return apiGet<Array<WifiNetwork>>(`${endpoint}/${ifName}/scan`)
}

export function getNewNetworks(ifName: string) {
    return apiPost<null, Array<WifiNetwork>>(`${endpoint}/${ifName}/scan`, null)
}


export function connectToWifi(ifName: string, bssid: string, password?: string) {
    // Siden baksiden forventer @RequestParam, bygger vi dem inn i query-stringen
    const params = new URLSearchParams({ bssid });
    if (password) params.append("password", password);

    return apiPost<Record<string, never>, boolean>(`${endpoint}/${ifName}/connect?${params.toString()}`, {});
}

export function disconnectFromWifi(ifName: string) {
    return apiPost<null, boolean>(`${endpoint}/${ifName}/disconnect`, null)
}