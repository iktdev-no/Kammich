import type { SvgIconProps } from "@mui/material";

import SignalWifi1BarLockRoundedIcon from "@mui/icons-material/SignalWifi1BarLockRounded";
import SignalWifi2BarLockRoundedIcon from "@mui/icons-material/SignalWifi2BarLockRounded";
import SignalWifi3BarLockRoundedIcon from "@mui/icons-material/SignalWifi3BarLockRounded";
import SignalWifi4BarLockRoundedIcon from "@mui/icons-material/SignalWifi4BarLockRounded";

import SignalWifi1BarRoundedIcon from "@mui/icons-material/SignalWifi1BarRounded";
import SignalWifi2BarRoundedIcon from "@mui/icons-material/SignalWifi2BarRounded";
import SignalWifi3BarRoundedIcon from "@mui/icons-material/SignalWifi3BarRounded";
import SignalWifi4BarRoundedIcon from "@mui/icons-material/SignalWifi4BarRounded";

export function WifiSignalIcon({
  isSecure,
  strength,
  strengthColor = true,
  customColor,
  sx,
}: {
  isSecure: boolean;
  strength: number;
  strengthColor?: boolean;
  customColor?:
  | "inherit"
  | "action"
  | "disabled"
  | "primary"
  | "secondary"
  | "error"
  | "info"
  | "success"
  | "warning";
  sx?: SvgIconProps["sx"];
}) {
  const level =
    strength > 75
      ? 4
      : strength > 50
        ? 3
        : strength > 25
          ? 2
          : 1;

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

  const Icon = isSecure
    ? [
      SignalWifi1BarLockRoundedIcon,
      SignalWifi1BarLockRoundedIcon,
      SignalWifi2BarLockRoundedIcon,
      SignalWifi3BarLockRoundedIcon,
      SignalWifi4BarLockRoundedIcon,
    ][level]
    : [
      SignalWifi1BarRoundedIcon,
      SignalWifi1BarRoundedIcon,
      SignalWifi2BarRoundedIcon,
      SignalWifi3BarRoundedIcon,
      SignalWifi4BarRoundedIcon,
    ][level];

  return <Icon color={color} sx={sx} />;
}