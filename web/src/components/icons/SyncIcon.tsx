import { SvgIcon, type SvgIconProps } from "@mui/material";

export default function SyncIcon(props: SvgIconProps) {
    return (
        <SvgIcon {...props} viewBox="0 0 24 24">
            <path
                fill="currentColor"
                d="M4,12C4,9.67 5.02,7.58 6.62,6.12L9,8.5L9,2.5L3,2.5L5.2,4.7C3.24,6.52 2,9.11 2,12C2,17.19 5.95,21.45 11,21.95L11,19.93C7.06,19.44 4,16.07 4,12M22,12C22,6.81 18.05,2.55 13,2.05L13,4.07C16.94,4.56 20,7.93 20,12C20,14.33 18.98,16.42 17.38,17.88L15,15.5L15,21.5L21,21.5L18.8,19.3C20.76,17.48 22,14.89 22,12"
            />
        </SvgIcon>
    );
}