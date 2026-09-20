import type {
    WifiInterfaceState,
    WifiNetwork,
    WifiTetherAP,
} from "../../../types/types";
import { apiDelete, apiGet, apiPost } from "../../client";

const clientEndpoint = "/v1/wifi/client";
const tetherEndpoint = "/v1/wifi/tethering";

export const wifiApi = {
    reset: (interfaceName: string) =>
        apiPost<void, void>(`/v1/wifi/${interfaceName}/reset`),

    release: (interfaceName: string) =>
        apiPost<void, void>(`/v1/wifi/${interfaceName}/release`),


    // Client
    getAll: () =>
        apiGet<WifiInterfaceState[]>("/v1/wifi"),

    getConnection: (ifName: string) =>
        apiGet<WifiInterfaceState>(`${clientEndpoint}/${ifName}`),
    getClientInterfaces: () =>
        apiGet<Array<WifiInterfaceState>>(`${clientEndpoint}`),

    getNetworks: (ifName: string) =>
        apiGet<Array<WifiNetwork>>(`${clientEndpoint}/${ifName}/scan`),

    startNetworkScan: (ifName: string) =>
        apiPost<null, never>(`${clientEndpoint}/${ifName}/scan/start`, null),

    stopNetworkScan: (ifName: string) =>
        apiPost<null, never>(`${clientEndpoint}/${ifName}/scan/stop`, null),

    useClientDevice: (ifName: string) =>
        apiPost<string, void>(
            `${clientEndpoint}/use`,
            ifName,
        ),

    connect: (ifName: string, bssid: string, password?: string) => {
        const params = new URLSearchParams({ bssid });

        if (password) {
            params.append("password", password);
        }

        return apiPost<Record<string, never>, boolean>(
            `${clientEndpoint}/${ifName}/connect?${params.toString()}`,
            {},
        );
    },

    disconnect: (ifName: string) =>
        apiPost<null, boolean>(
            `${clientEndpoint}/${ifName}/disconnect`,
            null,
        ),

    // Access Point / Tethering
    getTether: (ifName: string) =>
        apiGet<WifiInterfaceState>(`${tetherEndpoint}/${ifName}`),
    getTetherInterfaces: () =>
        apiGet<Array<WifiInterfaceState>>(`${tetherEndpoint}`),

    startTethering: (ifName: string) =>
        apiPost<null, void>(
            `${tetherEndpoint}/start/${ifName}`,
            null,
        ),

    stopTethering: (ifName: string) =>
        apiPost<null, void>(
            `${tetherEndpoint}/stop/${ifName}`,
            null,
        ),

    removeTetherDevice: (ifName: string) =>
        apiDelete<boolean>(
            `${tetherEndpoint}/release`,
            { body: ifName },
        ),

    useTetherDevice: (ifName: string) =>
        apiPost<string, void>(
            `${tetherEndpoint}/use`,
            ifName,
        ),

    getAccessPoint: () =>
        apiGet<WifiTetherAP | undefined>(
            `${tetherEndpoint}/ap`,
        ),

    setAccessPoint: (ap: WifiTetherAP) =>
        apiPost<WifiTetherAP, void>(
            `${tetherEndpoint}/ap`,
            ap,
        ),
};