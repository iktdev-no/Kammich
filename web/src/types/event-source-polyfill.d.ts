declare module 'event-source-polyfill' {
    export interface EventSourcePolyfillInit extends EventSourceInit {
        heartbeatTimeout?: number;
        pollingInterval?: number;
        lastEventId?: string;
    }

    export class EventSourcePolyfill extends EventSource {
        constructor(url: string, eventSourceInitDict?: EventSourcePolyfillInit);
    }
}
