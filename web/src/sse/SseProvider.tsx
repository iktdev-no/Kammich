import { createContext, useContext, useEffect, useMemo } from 'react';
import { SseDispatcher } from './dispatcher';
import { initialSseState } from './state';
import type { SseEvent } from './events';


const SseContext = createContext<SseDispatcher | null>(null);

export const useSseDispatcher = () => useContext(SseContext)!;

export const SseProvider = ({ children }: { children: React.ReactNode }) => {
  const dispatcher = useMemo(() => new SseDispatcher(initialSseState), []);

  useEffect(() => {
    const es = new EventSource('/api/sse/stream');

    console.log("Connecting")
    dispatcher.dispatch({ type: "sse-connecting" });

    es.onopen = () => {
      console.log("SSe Online")
      dispatcher.dispatch({ type: "sse-online" });
      console.log("SSe Online")

    };

    es.onmessage = (event) => {
      try {
        const parsed: SseEvent = JSON.parse(event.data);
        dispatcher.dispatch(parsed);
      } catch {
        dispatcher.dispatch({ type: 'custom', payload: event.data });
      }
    };

    es.onerror = () => {
      console.log("SSE Offline")
      dispatcher.dispatch({ type: "sse-offline" });
    };

    return () => es.close();
  }, [dispatcher]);


  return (
    <SseContext.Provider value={dispatcher}>
      {children}
    </SseContext.Provider>
  );
};
