import { useEffect, useState } from "react";

export function useCountdown(target: string | null): number | null {
    const [remaining, setRemaining] = useState<number | null>(null);

    useEffect(() => {
        if (target === null) {
            setRemaining(null);
            return;
        }

        const targetTime: number = new Date(target).getTime();

        function update(): void {
            setRemaining(Math.max(
                0,
                Math.ceil((targetTime - Date.now()) / 1000)
            ));
        }

        update();

        const interval: number = window.setInterval(update, 1000);

        return () => window.clearInterval(interval);
    }, [target]);

    return remaining;
}