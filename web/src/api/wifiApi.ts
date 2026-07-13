import type { WifiConnectionResult, WifiInterface, WifiInterfaceState, WifiNetwork, WifiScanState } from "../types/types";
import { apiGet, apiPost } from "./client";

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