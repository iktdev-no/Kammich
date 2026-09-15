import type { TailscaleStatus, TailscaleServe, TailscaleNetcheck, TailscaleDns } from "../../../types/types";
import { apiGet } from "../../client";

export const tailscaleApi = {
    isInstalled: () =>
        apiGet<boolean>("/v1/tailscale/installed"),

    getStatus: () =>
        apiGet<TailscaleStatus | null>("/v1/tailscale/status"),

    getServe: () =>
        apiGet<TailscaleServe[]>("/v1/tailscale/serve"),

    getNetcheck: () =>
        apiGet<TailscaleNetcheck>("/v1/tailscale/netcheck"),

    getDns: () =>
        apiGet<TailscaleDns>("/v1/tailscale/dns"),
};