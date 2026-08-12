import type { ImportJobOwnershipSummary } from "../../types/types"
import {
    Box,
    Typography,
    Button,
    Card,
    Chip
} from "@mui/material"
import {
    CheckCircle as CheckCircleIcon,
    Lock as LockIcon,
    FolderOpen as FolderOpenIcon
} from "@mui/icons-material"

export interface JobClaimCardProps {
    job: ImportJobOwnershipSummary;
    onClaimJob: (jobId: string) => void;
}

export function JobClaimCard({ job, onClaimJob }: JobClaimCardProps) {
    const isClaimed = Boolean(job.claimedBy)

    return (
        <Card
            variant="outlined"
            sx={{
                borderRadius: 3,
                display: 'flex',
                flexDirection: { xs: 'column', sm: 'row' },
                alignItems: { xs: 'flex-start', sm: 'center' },
                justifyContent: 'space-between',
                p: 2.5,
                gap: 2,
                transition: 'border-color 0.2s',
                '&:hover': { borderColor: 'primary.main' }
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                <Box sx={{ p: 1, borderRadius: 2, bgcolor: 'action.selected', display: 'flex' }}>
                    <FolderOpenIcon fontSize="small" />
                </Box>
                <Box>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', fontWeight: 600 }}>
                        Jobb: {job.jobId}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        Enhet: <Box component="span" sx={{ fontFamily: 'monospace', color: 'text.primary' }}>{job.deviceId}</Box> | Filer: {job.totalFiles}
                    </Typography>
                </Box>
            </Box>

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center',
                    gap: 2,
                    width: { xs: '100%', sm: 'auto' },
                    justifyContent: { xs: 'space-between', sm: 'flex-end' }
                }}
            >
                {isClaimed ? (
                    <Chip
                        icon={<LockIcon fontSize="small" />}
                        label="Eies"
                        color="warning"
                        size="small"
                        variant="outlined"
                    />
                ) : (
                    <Chip
                        icon={<CheckCircleIcon fontSize="small" />}
                        label="Tilgjengelig"
                        color="success"
                        size="small"
                        variant="outlined"
                    />
                )}

                {job.claimable && (
                    <Button
                        variant="contained"
                        size="small"
                        onClick={() => onClaimJob(job.jobId)}
                        sx={{ textTransform: 'none', borderRadius: 2, boxShadow: 'none' }}
                    >
                        Ta eierskap
                    </Button>
                )}
            </Box>
        </Card>
    )
}