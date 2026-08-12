import { useEffect, useState, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Box, Typography, CircularProgress, Grid } from "@mui/material";
import { getDeviceInfo, getFiles, updateDeviceSettings } from "../../api/requests/camera";
import type { DeviceInfo, DeviceSettingsDto, WFile, RemovableDevice } from "../../types/types";
import { BreadcrumbPath } from "../../components/BreadcrumbPath";
import { useSseSelector } from "../../sse/useSseSelector";
import { toast } from "react-toastify";

import { DeviceCard } from "../../components/devices/DeviceCard";
import DeviceFileView from "../../components/devices/DeviceFileView";
import DeviceNotConnected from "../../components/devices/DeviceNotConnected";

function useDeviceManager(sn: string | undefined, currentPath: string) {
    const devices = useSseSelector(state => state.devices as RemovableDevice[]);
    const liveDevice = devices.find(d => d.id === sn || d.sn === sn);

    const [info, setInfo] = useState<DeviceInfo | null>(null);
    const [files, setFiles] = useState<WFile[]>([]);
    const [settings, setSettings] = useState<DeviceSettingsDto | null>(null);
    const [isDisconnected, setIsDisconnected] = useState(false);

    const disconnectTimerRef = useRef<number | null>(null);
    const hasBeenConnectedRef = useRef(false);

    // Hent enhetsinfo når enheten er tilstede i SSE-listen
    useEffect(() => {
        if (liveDevice) {
            if (disconnectTimerRef.current) {
                clearTimeout(disconnectTimerRef.current);
                disconnectTimerRef.current = null;
            }
            setIsDisconnected(false);
            hasBeenConnectedRef.current = true;

            if (sn && !info) {
                getDeviceInfo(sn)
                    .then(data => {
                        setInfo(data);
                        setSettings(data.deviceSettings);
                    })
                    .catch(() => { });
            }
        } else {
            // Hvis enheten forsvinner / nulles ut
            if (hasBeenConnectedRef.current) {
                if (!disconnectTimerRef.current) {
                    disconnectTimerRef.current = window.setTimeout(() => {
                        setIsDisconnected(true);
                        setInfo(null);
                        setFiles([]);
                        setSettings(null);
                        disconnectTimerRef.current = null;
                    }, 10000);
                }
            } else {
                setIsDisconnected(true);
            }
        }
    }, [liveDevice, sn]);

    // Hent filer når vi har info, stitilstand og enheten er klar
    useEffect(() => {
        if (info && liveDevice?.isReady && sn) {
            getFiles(sn, currentPath)
                .then(setFiles)
                .catch(console.error);
        }
    }, [currentPath, info, liveDevice?.isReady, sn]);

    // Nullstill alt ved bytte av SN
    useEffect(() => {
        setInfo(null);
        setFiles([]);
        setSettings(null);
        hasBeenConnectedRef.current = false;
        setIsDisconnected(false);
        if (disconnectTimerRef.current) {
            clearTimeout(disconnectTimerRef.current);
            disconnectTimerRef.current = null;
        }
    }, [sn]);

    return {
        liveDevice,
        info,
        files,
        settings,
        setSettings,
        isDisconnected
    };
}

export default function Device() {
    const { sn, "*": splat } = useParams<{ sn: string, "*": string }>();
    const currentPath = splat ? `/${splat}` : "/";
    const navigate = useNavigate();

    const {
        liveDevice,
        info,
        files,
        settings,
        setSettings,
        isDisconnected
    } = useDeviceManager(sn, currentPath);

    const onGoBack = () => { navigate("/"); };

    const handleSettingsUpdate = async (newSettings: Partial<DeviceSettingsDto>) => {
        try {
            const updatedDevice = await updateDeviceSettings(sn!, newSettings);
            setSettings(updatedDevice.deviceSettings);
        } catch (e) {
            toast.error("Kunne ikke oppdatere innstillinger");
        }
    };

    if (isDisconnected || (!liveDevice && !info))
        return <DeviceNotConnected sn={sn!} onGoBack={onGoBack} />;

    return (
        <Box sx={{ p: { xs: 2, md: 3 }, height: "100%", boxSizing: "border-box", overflow: "hidden" }}>
            <Grid container spacing={3} sx={{ height: "100%", flexWrap: "nowrap", flexDirection: { xs: "column", md: "row" }, overflowY: { xs: "auto", md: "hidden" } }}>

                <Grid size={{ xs: 12, md: 3 }} sx={{ flexShrink: 0, height: { xs: "auto", md: "100%" }, overflowY: { md: "auto" } }}>
                    {info ? <DeviceCard device={info} /> : <StatusSpinner message="Henter enhetsinfo..." />}
                </Grid>

                {liveDevice?.isReady ? (
                    <Grid size={{ xs: 12, md: 9 }} sx={{ flexGrow: 1, height: { xs: "auto", md: "100%" }, display: "flex", flexDirection: "column", overflowY: { md: "auto" }, overflowX: "hidden", pb: { xs: 4, md: 0 } }}>
                        <BreadcrumbPath path={currentPath} onNavigate={(p) => navigate(`/devices/${sn}${p}`)} />
                        <Box sx={{ mt: 2, flexGrow: 1 }}>
                            <DeviceFileView
                                files={files}
                                onNavigate={(p) => navigate(`/devices/${sn}${p}`)}
                                settings={settings}
                                onSettingsChange={handleSettingsUpdate}
                            />
                        </Box>
                    </Grid>
                ) : (
                    <Grid size={{ xs: 12, md: 9 }}>
                        <StatusSpinner message="Vennligst trykk 'Tillat' på telefonen." />
                    </Grid>
                )}

            </Grid>
        </Box>
    );
}

function StatusSpinner({ message }: { message: string }) {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 10, gap: 2 }}>
            <CircularProgress size={60} />
            <Typography variant="h5">{message}</Typography>
        </Box>
    );
}