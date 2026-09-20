import {
    Box,
    Stack,
    Typography,
} from "@mui/material";

interface PageLayoutProps {
    title: string;
    body?: string;
    actions?: React.ReactNode;
    children: React.ReactNode;
}

export default function PageLayout({
    title,
    body,
    actions,
    children,
}: PageLayoutProps) {
    return (
        <Box>
            <Box
                sx={{
                    position: "sticky",
                    top: 0,
                    zIndex: 10,
                    px: {
                        xs: 1.5,
                        sm: 3,
                    },
                    py: 1.5,
                    minHeight: {
                        xs: 80,
                        sm: 96,
                    },
                    mb: 2,
                    backgroundColor: "background.default",
                    display: "flex",
                    alignItems: "center",
                }}
            >
                <Stack
                    direction="row"
                    spacing={2}
                    sx={{
                        width: "100%",
                        minWidth: 0,
                        alignItems: "center",
                    }}
                >
                    <Box
                        sx={{
                            minWidth: 0,
                            flex: 1,
                        }}
                    >
                        <Typography
                            variant="h4"
                            noWrap
                            sx={{
                                minWidth: 0,
                                overflow: "hidden",
                                textOverflow: "ellipsis",
                                fontWeight: 600,
                                color: "text.primary",
                                fontSize: {
                                    xs: "1.5rem",
                                    md: "2.125rem",
                                },
                            }}
                        >
                            {title}
                        </Typography>

                        {body && (
                            <Typography
                                variant="body2"
                                color="text.secondary"
                                noWrap
                                sx={{
                                    overflow: "hidden",
                                    textOverflow: "ellipsis",
                                }}
                            >
                                {body}
                            </Typography>
                        )}
                    </Box>

                    {actions && (
                        <Box
                            sx={{
                                flexShrink: 0,
                                display: "flex",
                                alignItems: "center",
                            }}
                        >
                            {actions}
                        </Box>
                    )}
                </Stack>
            </Box>

            <Box
                sx={{
                    px: {
                        xs: 1.5,
                        sm: 3,
                    },
                    pb: 3,
                    pt: 1,
                }}
            >
                {children}
            </Box>
        </Box>
    );
}