import { useEffect, useRef } from "react";
import { Box, Typography, Stack, Chip } from "@mui/material";
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlineOutlined';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { motion, AnimatePresence } from "framer-motion";
import type { FileImportState, ImportProgressEvent } from "../../types/types";

function getFileStatusChip(state: FileImportState) {
    switch (state) {
        case "Success":
            return <Chip icon={<CheckCircleIcon />} label="Success" size="small" color="success" variant="outlined" />;
        case "InProgress":
            return <Chip label="Importerer..." size="small" color="primary" />;
        case "Failure":
            return <Chip icon={<ErrorOutlineIcon />} label="Feilet" size="small" color="error" variant="outlined" />;
        case "Pending":
        default:
            return <Chip icon={<RadioButtonUncheckedIcon />} label="Venter" size="small" variant="outlined" />;
    }
}

export default function ImportFileStream({
    files,
    currentFile,
}: {
    files: ImportProgressEvent["files"];
    currentFile: string | null;
}) {
    const scrollRef = useRef<HTMLDivElement>(null);

    // Smooth scroll to active file
    useEffect(() => {
        if (!scrollRef.current || !currentFile) return;
        const activeElement = scrollRef.current.querySelector(
            `[data-file="${CSS.escape(currentFile)}"]`
        );
        if (activeElement) {
            activeElement.scrollIntoView({
                behavior: "smooth",
                block: "center",
            });
        }
    }, [currentFile]);

    if (!files || files.length === 0) return null;

    return (
        <Box sx={{ height: "220px" }}>

            <Box
                ref={scrollRef}
                sx={{
                    maxHeight: "210px",
                    overflowY: "hidden",   // 👈 scrollbar fjernet
                    pr: 0.5,
                }}
            >
                <Stack sx={{ gap: 0.5 }}> {/* 👈 mindre gap */}
                    <AnimatePresence initial={false}>
                        {files.map((fileItem) => {
                            const isCurrent = fileItem.file === currentFile;

                            return (
                                <motion.div
                                    key={fileItem.file}
                                    data-file={fileItem.file}
                                    layout
                                    initial={{ opacity: 0, y: -8 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    exit={{ opacity: 0, y: 8 }}
                                    transition={{
                                        duration: 0.22,
                                        ease: "easeOut",
                                    }}
                                >
                                    <Box
                                        sx={{
                                            display: "flex",
                                            flexDirection: "row",
                                            alignItems: "center",
                                            justifyContent: "space-between",
                                            px: 1.5,
                                            py: 0.75,           // 👈 trimmet høyde
                                            minHeight: 32,      // 👈 kompakt
                                            borderRadius: 1,

                                            transition: "border-color 0.2s ease",
                                        }}
                                    >
                                        <Typography
                                            variant="body2"
                                            sx={{
                                                p: 0.5,
                                                fontWeight: isCurrent ? 600 : 400,
                                                color: isCurrent
                                                    ? "text.primary"
                                                    : "text.secondary",
                                            }}
                                            noWrap
                                        >
                                            {fileItem.file}
                                        </Typography>

                                        <Stack
                                            sx={{
                                                flexDirection: "row",
                                                gap: 0.5,
                                                alignItems: "center",
                                                flexShrink: 0,
                                            }}
                                        >
                                            {getFileStatusChip(fileItem.state)}
                                        </Stack>
                                    </Box>
                                </motion.div>
                            );
                        })}
                    </AnimatePresence>
                </Stack>
            </Box>
        </Box>
    );
}
