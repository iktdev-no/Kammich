import VisibilityMuiIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { IconButton, type SxProps, type Theme } from '@mui/material';
import { useState } from 'react';

interface VisibilityProps {
    defaultVisible?: boolean;
    visible?: boolean; // Valgfritt: Hvis du vil styre den utenfra
    onChange?: (visible: boolean) => void;
    sx?: SxProps<Theme>; // Legger til støtte for sx
}

export function VisibilityIcon({
    defaultVisible = false,
    visible: controlledVisible,
    onChange,
    sx
}: VisibilityProps) {
    const [internalVisible, setInternalVisible] = useState(defaultVisible);

    // Bruk kontrollert verdi hvis den finnes, ellers intern state
    const isVisible = controlledVisible !== undefined ? controlledVisible : internalVisible;

    const handleToggle = (e: React.MouseEvent) => {
        e.stopPropagation(); // Hindrer at klikket bobler opp (f.eks. hvis den ligger inni et klikkbart kort/accordion)
        const nextState = !isVisible;
        if (controlledVisible === undefined) {
            setInternalVisible(nextState);
        }
        onChange?.(nextState);
    };

    return (
        <IconButton
            onClick={handleToggle}
            aria-label="toggle visibility"
            sx={sx} // Sender sx videre hit
        >
            {isVisible ? <VisibilityMuiIcon /> : <VisibilityOffIcon />}
        </IconButton>
    );
}