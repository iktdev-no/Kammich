import { SvgIcon, type SvgIconProps } from '@mui/material';

export default function FilmRollIcon({
    filmColor = 'currentColor',
    containerColor = 'currentColor',
    ...props
}: SvgIconProps & {
    filmColor?: string;
    containerColor?: string;
}) {
    return (
        <SvgIcon {...props} viewBox="0 0 100 100">
            <defs>
                <clipPath id="film-clip">
                    <rect x="12" y="0" width="36" height="24" />
                </clipPath>

                <g id="film-segment">
                    <path
                        d="M20,21L20,5L4,5L4,21L6,21L6,19L8,19L8,21L16,21L16,19L18,19L18,21L20,21ZM8,17L6,17L6,15L8,15L8,17ZM8,13L6,13L6,11L8,11L8,13ZM8,9L6,9L6,7L8,7L8,9ZM18,17L16,17L16,15L18,15L18,17ZM18,13L16,13L16,11L18,11L18,13ZM18,9L16,9L16,7L18,7L18,9Z"
                        transform="matrix(-3.82857e-16,1,-1,-3.82857e-16,24,0)"
                        style={{
                            fillRule: 'nonzero',
                            fill: filmColor,
                        }}
                    />
                </g>

                <linearGradient
                    id="fade-gradient"
                    x1="0%"
                    y1="0%"
                    x2="100%"
                    y2="0%"
                >
                    <stop offset="0%" stopColor="white" stopOpacity="1" />
                    <stop offset="60%" stopColor="white" stopOpacity="1" />
                    <stop offset="100%" stopColor="white" stopOpacity="0" />
                </linearGradient>

                <mask id="fade-mask">
                    <rect
                        x="0"
                        y="0"
                        width="100"
                        height="100"
                        fill="url(#fade-gradient)"
                    />
                </mask>
            </defs>

            {/* FILMSTRIPE */}
            <g mask="url(#fade-mask)">
                <g transform="matrix(4.16667,0,0,4.16667,0,0)">
                    <g clipPath="url(#film-clip)">
                        <g transform="matrix(0.744881,0,0,0.744881,4.59139,3.06142)">
                            <g>
                                <animateTransform
                                    attributeName="transform"
                                    type="translate"
                                    values="0 0; 15.9 0"
                                    dur="0.75s"
                                    repeatCount="indefinite"
                                    calcMode="linear"
                                />

                                <use href="#film-segment" x="-34" />
                                <use href="#film-segment" x="-18.1" />
                                <use href="#film-segment" x="-2.2" />
                                <use href="#film-segment" x="13.7" />
                            </g>
                        </g>
                    </g>
                </g>
            </g>

            {/* TELEFON */}
            <g transform="translate(-15, 0)">
                <path
                    d="M59.435,9.441L63.394,9.441C67.5,9.441 70.833,12.774 70.833,16.88L70.833,83.394C70.833,87.5 67.5,90.833 63.394,90.833L31.606,90.833C27.5,90.833 24.167,87.5 24.167,83.394L24.167,16.88C24.167,12.774 27.5,9.441 31.606,9.441L35.565,9.441L35.565,7.199C35.565,5.849 36.661,4.753 38.011,4.753L56.989,4.753C58.339,4.753 59.435,5.849 59.435,7.199L59.435,9.441Z"
                    style={{ fill: containerColor }}
                />
            </g>
        </SvgIcon>
    );
}