import { Outlet } from "react-router-dom";
import ImmichLogin from "../components/immich/ImmichLogin";
import { useSseSelector } from "../sse/useSseSelector";


export default function Immich() {
    const immichUser = useSseSelector(state => state.immichUserMe)

    return (<>
        {!immichUser && (
            <ImmichLogin />
        )}


        <Outlet />
    </>)
}

