import type { NetworkInterface } from "../../types/types";
import { apiGet } from "../client";

export function getNetworkInterfaces(): Promise<Array<NetworkInterface>> {
    return apiGet<Array<NetworkInterface>>("/v1/networking/interfaces")
}