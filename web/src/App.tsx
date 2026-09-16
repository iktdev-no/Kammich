import { RouterProvider, createBrowserRouter } from 'react-router-dom';
import Settings from './routes/Settings';
import AppLayout from './layouts/AppLayout';
import Upload from './routes/Upload';
import Photo from './routes/Photo';
import Devices from './routes/Devices';
import WifiSettings from './routes/settings/WifiSettings';
import WifiApSettings from './routes/settings/WifiApSettings';
import Networking from './routes/settings/Networking';
import { Import } from './routes/Import';
import Device from './routes/devices/Device';
import Immich from './routes/Immich';
import ImmichLogin from './components/immich/ImmichLogin';
import { Album } from './routes/Album';
import ImportOwnership from './routes/ImportOwnership';
import ImmichProfiles from './routes/immich/ImmichProfiles';
import ImmichMe from './routes/immich/ImmichMe';
import { System } from './routes/System';
import { useEffect } from 'react';
import Tailscale from './routes/settings/Tailscale';
import Ethernet from './routes/settings/Ethernet';

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Photo /> },
      { path: "photo/:sn", element: <Photo /> },
      { path: 'devices/:sn/*', element: <Device /> },
      { path: 'devices', element: <Devices /> },
      { path: 'album', element: <Album /> },
      { path: 'upload', element: <Upload /> },
      { path: 'import', element: <Import /> },
      { path: 'settings', element: <Settings /> },
      { path: 'settings/info', element: <Settings /> },
      { path: 'ownership', element: <ImportOwnership /> },
      { path: 'settings/networking', element: <Networking /> },
      { path: 'settings/networking/tailscale', element: <Tailscale /> },
      { path: 'settings/networking/ethernet', element: <Ethernet /> },
      { path: 'settings/networking/wifi', element: <WifiSettings /> },
      { path: 'settings/networking/ap', element: <WifiApSettings /> },
      { path: 'settings/immich', element: <Immich /> },
      { path: 'settings/immich/login', element: <ImmichLogin /> },
      { path: 'settings/immich/me', element: <ImmichMe /> },
      { path: 'settings/immich/users', element: <ImmichProfiles /> },
      { path: 'settings/system', element: <System /> },

    ],
  },
]);

export default function App() {

  useEffect(() => {
    const splash = document.getElementById("splash");

    if (!splash) {
      return;
    }

    splash.classList.add("hidden");

    const timeout = setTimeout(() => {
      splash.remove();
    }, 300);

    return () => clearTimeout(timeout);
  }, []);

  return <RouterProvider router={router} />;
}