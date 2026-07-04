import { useSseSelector } from "../sse/useSseSelector";


export default function Upload() {
    const msg = useSseSelector(state => state.lastPing);

    return <div>SSE: {msg}</div>;
}
