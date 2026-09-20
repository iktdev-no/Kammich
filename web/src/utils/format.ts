import type { TFunction } from "i18next";

export function formatBytes(
  bytes: number,
  decimals = 1
): string {
  if (bytes === 0) {
    return "0 B";
  }

  const k = 1024;
  const sizes = [
    "B",
    "KiB",
    "MiB",
    "GiB",
    "TiB",
    "PiB",
  ];

  const i = Math.floor(
    Math.log(bytes) / Math.log(k)
  );

  const value =
    bytes / Math.pow(k, i);

  return `${value.toFixed(decimals)} ${sizes[i]}`;
}

export function formatNotificationTime(
  millis: number,
  t: TFunction
) {
  const now = Date.now();

  const diffInSeconds = Math.floor(
    (now - millis) / 1000
  );

  let relativeTime = "";

  if (diffInSeconds < 60) {
    relativeTime = t(
      "common.time.just_now"
    );
  } else if (diffInSeconds < 3600) {
    relativeTime = t(
      "common.time.minutes_ago",
      {
        count: Math.floor(
          diffInSeconds / 60
        ),
      }
    );
  } else if (diffInSeconds < 86400) {
    relativeTime = t(
      "common.time.hours_ago",
      {
        count: Math.floor(
          diffInSeconds / 3600
        ),
      }
    );
  } else {
    relativeTime = t(
      "common.time.days_ago",
      {
        count: Math.floor(
          diffInSeconds / 86400
        ),
      }
    );
  }

  const exactTime = new Date(
    millis
  ).toLocaleString(
    "no-NO",
    {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }
  );

  return {
    relativeTime,
    exactTime,
  };
}