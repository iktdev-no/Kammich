import SignalWifi1BarLockRoundedIcon from '@mui/icons-material/SignalWifi1BarLockRounded';
import SignalWifi2BarLockRoundedIcon from '@mui/icons-material/SignalWifi2BarLockRounded';
import SignalWifi3BarLockRoundedIcon from '@mui/icons-material/SignalWifi3BarLockRounded';
import SignalWifi4BarLockRoundedIcon from '@mui/icons-material/SignalWifi4BarLockRounded';

import SignalWifi1BarRoundedIcon from '@mui/icons-material/SignalWifi1BarRounded';
import SignalWifi2BarRoundedIcon from '@mui/icons-material/SignalWifi2BarRounded';
import SignalWifi3BarRoundedIcon from '@mui/icons-material/SignalWifi3BarRounded';
import SignalWifi4BarRoundedIcon from '@mui/icons-material/SignalWifi4BarRounded';

export function WifiSignalIcon({
  isSecure,
  strength,
  strengthColor = true,
  customColor,
}: {
  isSecure: boolean;
  strength: number;
  strengthColor?: boolean;
  customColor?: "inherit" | "action" | "disabled" | "primary" | "secondary" | "error" | "info" | "success" | "warning";
}) {
  // Map strength (0-100) til 1-4
  const level = strength > 75 ? 4 : strength > 50 ? 3 : strength > 25 ? 2 : 1;

  // Bestem farge basert på prioritering: 
  // 1. customColor, 2. dynamisk strengthColor, 3. undefined (bruker standard/inherit)
  const getColor = () => {
    if (customColor) return customColor;
    if (strengthColor) {
      if (level >= 3) return "success";
      if (level === 2) return "warning";
      return "error";
    }
    return undefined;
  };

  const color = getColor();

  // Velg ikon basert på secure-status og nivå
  const Icon = isSecure
    ? [
        SignalWifi1BarLockRoundedIcon,
        SignalWifi1BarLockRoundedIcon, // level 1
        SignalWifi2BarLockRoundedIcon, // level 2
        SignalWifi3BarLockRoundedIcon, // level 3
        SignalWifi4BarLockRoundedIcon, // level 4
      ][level]
    : [
        SignalWifi1BarRoundedIcon,
        SignalWifi1BarRoundedIcon, // level 1
        SignalWifi2BarRoundedIcon, // level 2
        SignalWifi3BarRoundedIcon, // level 3
        SignalWifi4BarRoundedIcon, // level 4
      ][level];

  return <Icon color={color} />;
}