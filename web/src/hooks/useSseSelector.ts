import { useEffect, useState } from 'react';
import type { SseState } from '../sse/state';
import { useSseDispatcher } from '../sse/SseProvider';

export function useSseSelector<T>(selector: (state: SseState) => T): T {
    const dispatcher = useSseDispatcher();
    const [value, setValue] = useState(() => selector(dispatcher.getState()));

    useEffect(() => {
        const unsubscribe = dispatcher.subscribe((state) => {
            setValue(selector(state));
        });

        return () => {
            unsubscribe(); // ignorer returverdien
        };
    }, [dispatcher, selector]);

    return value;
}
