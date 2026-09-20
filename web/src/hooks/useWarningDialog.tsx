import { useState } from "react";
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
} from "@mui/material";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlineOutlined';
import { useTranslation } from "react-i18next";

export type WarningSeverity = "info" | "warning" | "critical";

interface WarningOptions {
    title: string;
    message: string;
    severity?: WarningSeverity;
    onConfirm?: () => void | Promise<void>;
    confirmText?: string;
}

export function useWarningDialog() {
    const { t } = useTranslation();
    const [warning, setWarning] = useState<WarningOptions | null>(null);

    const showWarning = (options: WarningOptions) => {
        setWarning(options);
    };

    const closeWarning = () => {
        setWarning(null);
    };

    const dialog = warning && (
        <Dialog
            open
            onClose={closeWarning}
        >
            <DialogTitle
                sx={
                    {
                        display: "flex",
                        alignItems: "center",
                        gap: 1.5,
                    }
                }
            >
                {
                    warning.severity === "info" && (
                        <InfoOutlinedIcon color="info" />
                    )
                }

                {
                    warning.severity === "warning" && (
                        <WarningAmberIcon color="warning" />
                    )
                }

                {
                    warning.severity === "critical" && (
                        <ErrorOutlineIcon color="error" />
                    )
                }

                {warning.title}
            </DialogTitle>

            < DialogContent >
                <DialogContentText>
                    {warning.message}
                </DialogContentText>
            </DialogContent>

            < DialogActions sx={{ p: 2, pt: 0 }
            }>
                {
                    warning.severity !== "info" && (
                        <Button
                            onClick={closeWarning}
                            color="inherit"
                        >
                            {t("common.cancel")}
                        </Button>
                    )}

                <Button
                    onClick={
                        async () => {
                            await warning.onConfirm?.();
                            closeWarning();
                        }
                    }
                    variant="contained"
                    color={
                        warning.severity === "critical"
                            ? "error"
                            : warning.severity === "warning"
                                ? "warning"
                                : "info"
                    }
                    autoFocus
                >
                    {
                        warning.severity === "info"
                            ? t("common.ok")
                            : warning.confirmText ?? t("common.confirm")
                    }
                </Button>
            </DialogActions>
        </Dialog>
    );

    return {
        showWarning,
        closeWarning,
        warningDialog: dialog,
    };
}