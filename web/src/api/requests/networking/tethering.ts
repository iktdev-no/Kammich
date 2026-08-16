import type { WifiInterfaceTether, WifiTetherAP } from "../../../types/types"
import { apiDelete, apiGet, apiPost } from "../../client"


const endpoint = "/v1/wifi/tethering"


export function getInterfaces() {
    return apiGet<Array<WifiInterfaceTether>>(`${endpoint}`)
}


export function startTethering(ifName: string) {
    return apiPost<null, void>(`${endpoint}/start/${ifName}`, null)
}

export function stopTethering(ifName: string) {
    return apiPost<null, void>(`${endpoint}/stop/${ifName}`, null)
}

export function removeTetherDevice(ifName: string) {
    return apiDelete<boolean>(`${endpoint}/release`, { body: ifName })
}

export function useTetherDevice(ifName: string) {
    return apiPost<string, void>(`${endpoint}/use`, ifName)
}

export function setAp(ap: WifiTetherAP) {
    return apiPost<WifiTetherAP, void>(`${endpoint}/ap`, ap)
}

export function getAp() {
    return apiGet<WifiTetherAP | undefined>(`${endpoint}/ap`)
}