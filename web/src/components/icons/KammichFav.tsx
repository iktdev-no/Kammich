import { SvgIcon, type SvgIconProps } from "@mui/material";

export function KammichFav(props: SvgIconProps) {
    return (
        <SvgIcon {...props} viewBox="0 0 64 64">
            <circle cx="32" cy="32" r="29" fill="#1b1b1b" />
            <circle cx="32" cy="32" r="26" fill="#383838" />

            <circle cx="32" cy="32" r="22" fill="none" stroke="#bdbdbd" stroke-width="3" />
            <circle cx="32" cy="32" r="17" fill="#161616" />
            <circle cx="32" cy="32" r="14" fill="none" stroke="#555" stroke-width="2" />


            <circle cx="32" cy="32" r="11" fill="#242424" />

            <circle cx="32" cy="32" r="7" fill="#101010" />

            <text x="32" y="39" text-anchor="middle" font-family="system-ui, sans-serif" font-size="20" font-weight="700"
                fill="#d6d6d6">K</text>
            <path d="
    M 32 21
    A 11 11 0 1 1 32 43
    A 11 11 0 1 1 32 21
    Z

    M 32 25
    A 7 7 0 1 0 32 39
    A 7 7 0 1 0 32 25
    Z
  " fill="#242424" fill-rule="evenodd" />

            <circle cx="28" cy="27" r="2" fill="#777" />
        </SvgIcon>
    );
}