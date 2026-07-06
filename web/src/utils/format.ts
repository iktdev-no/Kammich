export function formatBytes(bytes: number, decimals = 1): string {
  if (bytes === 0) return "0 B";

  const k = 1024;
  const sizes = ["B", "KiB", "MiB", "GiB", "TiB", "PiB"];

  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const value = bytes / Math.pow(k, i);

  return `${value.toFixed(decimals)} ${sizes[i]}`;
}


export const formatNotificationTime = (millis: number) => {
  const now = Date.now();
  const diffInSeconds = Math.floor((now - millis) / 1000);
  
  // Relativ tid (f.eks. "5 minutter siden")
  let relativeTime = "";
  if (diffInSeconds < 60) relativeTime = "Akkurat nå";
  else if (diffInSeconds < 3600) relativeTime = `${Math.floor(diffInSeconds / 60)} min siden`;
  else if (diffInSeconds < 86400) relativeTime = `${Math.floor(diffInSeconds / 3600)} timer siden`;
  else relativeTime = `${Math.floor(diffInSeconds / 86400)} dager siden`;

  // Nøyaktig tid (f.eks. "05.07.2026, 15:30")
  const exactTime = new Date(millis).toLocaleString('no-NO', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });

  return { relativeTime, exactTime };
};