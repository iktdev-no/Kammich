import HomeIcon from "@mui/icons-material/Home"
import { Breadcrumbs, Chip, Stack } from "@mui/material"

export interface BreadcrumbPathProps {
    path: string
    onNavigate: (path: string) => void
}

function splitPath(path: string): string[] {
    if (!path || path === "/") return []
    return path.split("/").filter(Boolean)
}

function buildPath(parts: string[], index: number) {
    return "/" + parts.slice(0, index + 1).join("/")
}

// ... dine eksisterende helpers ...

export function BreadcrumbPath({ path, onNavigate }: BreadcrumbPathProps) {
    const segments = splitPath(path);

    return (
        <Stack direction="row" spacing={1}  sx={{ mb: 2 }}>
            <Chip
                icon={<HomeIcon />}
                label="Home"
                clickable
                onClick={() => onNavigate("/")}
            />
            
            <Breadcrumbs separator="›">

                {segments.map((segment, index) => {
                    const p = buildPath(segments, index);
                    const isLast = index === segments.length - 1;
                    
                    return (
                        <Chip
                            key={p}
                            label={segment}
                            clickable
                            onClick={() => onNavigate(p)}
                            // Gjør den siste chippen litt mer fremhevet
                            variant={isLast ? "filled" : "outlined"}
                            color={isLast ? "primary" : "default"}
                            sx={{ fontWeight: 500 }}
                        />
                    );
                })}
            </Breadcrumbs>
        </Stack>
    );
}