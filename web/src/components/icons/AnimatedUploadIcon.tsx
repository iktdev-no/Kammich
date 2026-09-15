import { SvgIcon, type SvgIconProps } from '@mui/material';

interface AnimatedCloudUploadIconProps extends SvgIconProps {
    accentColor?: string;
}

export default function AnimatedCloudUploadIcon({
    accentColor = 'currentColor',
    ...props
}: AnimatedCloudUploadIconProps) {
    const arrow = (
        <path
            d="M9.5 15.5 12 13l2.5 2.5"
            fill="none"
            stroke={accentColor}
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        />
    );

    return (
        <SvgIcon {...props} viewBox="0 0 24 24">
            {/* Cloud */}
            <path
                fill="currentColor"
                d="
                    M19.35 8.04
                    C18.67 4.59 15.64 2 12 2
                    C9.11 2 6.6 3.64 5.35 6.04
                    C2.34 6.36 0 8.91 0 12
                    C0 15.31 2.69 18 6 18
                    H19
                    C21.76 18 24 15.76 24 13
                    C24 10.36 21.95 8.22 19.35 8.04
                    Z
                "
            />

            {/* Upload stream */}
            {[0, 0.5, 1].map(delay => (
                <g key={delay}>
                    {arrow}

                    <animateTransform
                        attributeName="transform"
                        type="translate"
                        values="0 8; 0 -4"
                        dur="1.5s"
                        begin={`${delay}s`}
                        repeatCount="indefinite"
                        calcMode="spline"
                        keySplines="0.4 0 0.2 1"
                    />

                    <animate
                        attributeName="opacity"
                        values="0;1;1;0"
                        dur="1.5s"
                        begin={`${delay}s`}
                        repeatCount="indefinite"
                    />
                </g>
            ))}
        </SvgIcon>
    );
}