import type { WifiScanState, WifiInterfaceState, WifiInterface, WifiNetwork, WifiConnectionResult, WifiTetherSetting, WifiTetherInterface } from "../types/types";
import { apiDelete, apiGet, apiPost } from "./client";

/**
 * Henter nåværende aktivitetstilstand (Scanning, Connecting, etc.)
 */
export function getWifiScanState() {
  return apiGet<WifiScanState>("/v1/wifi/state");
}

export function getWifiActiveConnection() {
  return apiGet<WifiInterfaceState>("/v1/wifi/connection");
}

/**
 * Henter tilgjengelige nettverkskort (wlan0, etc.)
 */
export function getWifiInterfaces() {
  return apiGet<WifiInterface[]>("/v1/wifi/interfaces");
}

/**
 * Henter tilgjengelige nettverk for et spesifikt kort.
 * force=true tvinger en reell linux-skanning i bakgrunnen.
 */
export function getAvailableNetworks(interfaceName: string, force = false) {
  return apiGet<WifiNetwork[]>(`/v1/wifi/networks/${interfaceName}`, { force });
}

/**
 * Trigger en oppkobling mot et aksesspunkt
 */
export function connectToWifi(interfaceName: string, ssid: string, password?: string) {
  // Siden baksiden forventer @RequestParam, bygger vi dem inn i query-stringen
  const params = new URLSearchParams({ interfaceName, ssid });
  if (password) params.append("password", password);

  return apiPost<Record<string, never>, WifiConnectionResult>(`/v1/wifi/connect?${params.toString()}`, {});
}


export function disconnectFromWifi(interfaceName: string) {
  const params = new URLSearchParams({ interfaceName });
  return apiPost<Record<string, never>, WifiConnectionResult>(`/v1/wifi/disconnect?${params.toString()}`, {});
}

export function startWifiScan(interfaceName: string) {
  // 1. Vi legger interfaceName i query-stringen siden Spring bruker @RequestParam
  const path = `/v1/wifi/scan?interfaceName=${encodeURIComponent(interfaceName)}`;

  // 2. Vi passer null som body (TRequest) og forventer string tilbake (TResponse) siden status er 202 uten JSON.
  return apiPost<null, string>(path, null);
}

export function getTetherConfig() {
  return apiGet<WifiTetherSetting>(`/v1/wifi/tether/config/ap`);
}

export function updateTetherConfig(tether: WifiTetherSetting): Promise<boolean> {
  return apiPost<WifiTetherSetting, boolean>(`/v1/wifi/tether/config/ap`, tether)
}

export function getWifiTetherInterfaces() {
  return apiGet<Array<WifiTetherInterface>>(`/v1/wifi/tether/config/devices`)
}

export function setWifiTetherInterfaceSelected(deviceId: string) {
  return apiPost<string, boolean>(`/v1/wifi/tether/config/devices/use`, deviceId)
}

export function removeWifiTetherInterfaceSelected(deviceId: string) {
  return apiDelete<boolean>(`/v1/wifi/tether/config/devices/use`, { body: deviceId })
}

export function startTethering() {
  return apiPost<null, void>(`/v1/wifi/tether/start`, null)
}

export function stopTethering() {
  return apiPost<null, void>(`/v1/wifi/tether/stop`, null)
}