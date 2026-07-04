import { useSse } from '../context/SseContext';

export default function Settings() {
    const msg = useSse();

    return <div>SSE: {msg}</div>;
}
