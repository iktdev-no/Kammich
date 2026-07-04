import { useSse } from '../context/SseContext';

export default function Home() {
    const msg = useSse();

    return <div>SSE: {msg}</div>;
}
