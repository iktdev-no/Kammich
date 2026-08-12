import { useEffect, useState } from "react"
import {
    getDevices,
    getImportJobs,
    claimDeviceBySerial,
    claimImportJob
} from "../api/requests/claim"
import type { DeviceOwnershipSummary, ImportJobOwnershipSummary } from "../types/types"
import {
    Box,
    Typography,
    Card,
    CircularProgress,
    Alert,
    Chip,
    Grid
} from "@mui/material"
import { VerifiedUser as VerifiedUserIcon } from "@mui/icons-material"
import { DeviceClaimCard } from "../components/claim/DeviceClaimCard"
import { JobClaimCard } from "../components/claim/JobClaimCard"

export default function ImportOwnership() {
    const [devices, setDevices] = useState<DeviceOwnershipSummary[]>([])
    const [importJobs, setImportJobs] = useState<ImportJobOwnershipSummary[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const loadData = async (isInitial = false) => {
        try {
            if (isInitial) setLoading(true)
            const [deviceRes, jobRes] = await Promise.all([getDevices(), getImportJobs()])
            setDevices(deviceRes || [])
            setImportJobs(jobRes || [])
            setError(null)
        } catch (err) {
            console.error("Feil ved henting av eierskapsoversikt:", err)
            setError("Klarte ikke å laste inn eierskapsoversikt.")
        } finally {
            if (isInitial) setLoading(false)
        }
    }

    useEffect(() => {
        loadData(true)
    }, [])

    const handleClaimDevice = async (serial: string) => {
        try {
            const res = await claimDeviceBySerial(serial)
            if (res) {
                // Oppdater lokalt med en gang, eller hent i bakgrunnen uten å trigge full loading-state
                setDevices((prev) =>
                    prev.map((d) => (d.deviceId === serial ? { ...d, claimedBy: "current-user" } : d))
                )
                loadData(false) // Synk i bakgrunnen
            }
        } catch (err) {
            console.error("Klarte ikke å claime enhet:", err)
        }
    }

    const handleClaimJob = async (jobId: string) => {
        try {
            const res = await claimImportJob(jobId)
            if (res) {
                // Oppdater lokalt med en gang, eller hent i bakgrunnen
                setImportJobs((prev) =>
                    prev.map((j) => (j.jobId === jobId ? { ...j, claimedBy: "current-user" } : j))
                )
                loadData(false) // Synk i bakgrunnen
            }
        } catch (err) {
            console.error("Klarte ikke å claime import-jobb:", err)
        }
    }

    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
                <CircularProgress />
            </Box>
        )
    }

    if (error) {
        return (
            <Box sx={{ maxWidth: 'lg', mx: 'auto', p: 4 }}>
                <Alert severity="error" variant="outlined">{error}</Alert>
            </Box>
        )
    }

    return (
        <Box sx={{ maxWidth: 'lg', mx: 'auto', p: 4, display: 'flex', flexDirection: 'column', gap: 6 }}>
            {/* Header */}
            <Box>
                <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1, display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <VerifiedUserIcon color="primary" fontSize="large" />
                    Eierskap & Administrasjon
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Knytt enheter og import-jobber til din brukerprofil for å administrere tilgang og opplasting.
                </Typography>
            </Box>

            {/* Enheter */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                        Tilgjengelige enheter
                    </Typography>
                    <Chip label={`${devices.length} enheter`} size="small" variant="outlined" />
                </Box>

                {devices.length === 0 ? (
                    <Card variant="outlined" sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default', borderRadius: 3 }}>
                        <Typography variant="body2" color="text.secondary">Ingen enheter funnet.</Typography>
                    </Card>
                ) : (
                    <Grid container spacing={3}>
                        {devices.map((device) => (
                            <Grid size={{ xs: 12, md: 6 }} key={device.deviceId}>
                                <DeviceClaimCard
                                    device={device}
                                    onClaimDevice={handleClaimDevice}
                                />
                            </Grid>
                        ))}
                    </Grid>
                )}
            </Box>

            {/* Import-jobber */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                        Import-jobber
                    </Typography>
                    <Chip label={`${importJobs.length} jobber`} size="small" variant="outlined" />
                </Box>

                {importJobs.length === 0 ? (
                    <Card variant="outlined" sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default', borderRadius: 3 }}>
                        <Typography variant="body2" color="text.secondary">Ingen import-jobber funnet.</Typography>
                    </Card>
                ) : (
                    <Grid container spacing={3}>
                        {importJobs.map((job) => (
                            <Grid size={{ xs: 12, md: 6 }} key={job.jobId}>
                                <JobClaimCard
                                    job={job}
                                    onClaimJob={handleClaimJob}
                                />
                            </Grid>
                        ))}
                    </Grid>
                )}
            </Box>
        </Box>
    )
}