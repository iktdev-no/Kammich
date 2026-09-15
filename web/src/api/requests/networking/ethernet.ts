import type { EthernetConnection } from "../../../types/types";
import { apiGet, apiPost, apiPut } from "../../client";

export const ethernetApi = {
    getAll: () =>
        apiGet<EthernetConnection[]>("/v1/ethernet"),

    get: (interfaceName: string) =>
        apiGet<EthernetConnection>(`/v1/ethernet/${interfaceName}`),

    startClient: (interfaceName: string) =>
        apiPost<void, void>(`/v1/ethernet/${interfaceName}/client`),

    startTether: (interfaceName: string) =>
        apiPost<void, void>(`/v1/ethernet/${interfaceName}/tether`),

    disconnect: (interfaceName: string) =>
        apiPost<void, void>(`/v1/ethernet/${interfaceName}/disconnect`),

    reset: (interfaceName: string) =>
        apiPost<void, void>(`/v1/ethernet/${interfaceName}/reset`),

    setEmergency: (interfaceName: string, enabled: boolean) =>
        apiPut<{ enabled: boolean }, void>(
            `/v1/ethernet/${interfaceName}/emergency`,
            { enabled },
        ),
};