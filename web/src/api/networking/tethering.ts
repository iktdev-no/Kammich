import type { WifiNetworkTether, WifiTetherAP, WirelessInterface } from "../../types/types"
import { apiDelete, apiGet, apiPost } from "../client"


const endpoint = "/v1/wifi/tethering"


export function getState() {
    return apiGet<Array<WifiNetworkTether>>(`${endpoint}/state`)
}

export function getInterfaces() {
    return apiGet<Array<WirelessInterface>>(`${endpoint}/interfaces`)
}

export function startTethering(ifName: string) {
    return apiPost<null, void>(`${endpoint}/start/${ifName}`, null)
}

export function stopTethering(ifName: string) {
    return apiPost<null, void>(`${endpoint}/stop/${ifName}`, null)
}

export function removeTetherDevice(ifName: string) {
    return apiDelete<boolean>(`${endpoint}/remove`, { body: ifName })
}

export function addTetherDevice(ifName: string) {
    return apiPost<string, void>(`${endpoint}/add`, ifName)
}

export function setAp(ap: WifiTetherAP) {
    return apiPost<WifiTetherAP, void>(`${endpoint}/ap`, ap)
}

export function getAp() {
    return apiGet<WifiTetherAP | undefined>(`${endpoint}/ap`)
}