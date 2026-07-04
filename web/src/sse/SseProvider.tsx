import { createContext, useContext, useEffect, useMemo } from 'react';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { SseDispatcher } from './dispatcher';
import { initialSseState } from './state';
import type { SseEvent } from './events';

const SseContext = createContext<SseDispatcher | null>(null);

export const useSseDispatcher = () => useContext(SseContext)!;

export const SseProvider = ({ children }: { children: React.ReactNode }) => {
  const dispatcher = useMemo(() => new SseDispatcher(initialSseState), []);

  useEffect(() => {
    const es = new EventSourcePolyfill('/api/sse/stream', {
      heartbeatTimeout: 30000,
    });

    es.onmessage = (event) => {
      try {
        const parsed: SseEvent = JSON.parse(event.data);
        dispatcher.dispatch(parsed);
      } catch {
        dispatcher.dispatch({ type: 'custom', payload: event.data });
      }
    };

    return () => es.close();
  }, [dispatcher]);

  return (
    <SseContext.Provider value={dispatcher}>
      {children}
    </SseContext.Provider>
  );
};
