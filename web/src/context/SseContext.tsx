import { createContext, useContext, useEffect, useState } from 'react';
import { EventSourcePolyfill } from 'event-source-polyfill';

type SseData = string | null;

const SseContext = createContext<SseData>(null);

export const useSse = () => useContext(SseContext);

export const SseProvider = ({ children }: { children: React.ReactNode }) => {
    const [data, setData] = useState<SseData>(null);

    useEffect(() => {
        const es = new EventSourcePolyfill('/api/sse/stream', {
            heartbeatTimeout: 30000,
        });

        es.onmessage = (event) => setData(event.data);

        return () => es.close();
    }, []);

    return (
        <SseContext.Provider value={data}>
            {children}
        </SseContext.Provider>
    );
};
