import { useSseSelector } from "../sse/useSseSelector";


export default function Settings() {
    const msg = useSseSelector(state => state.lastPing);


    return <div>SSE: {msg}</div>;
}
