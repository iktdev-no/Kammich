import { RouterProvider, createBrowserRouter } from 'react-router-dom';
import Settings from './pages/Settings';
import AppLayout from './components/layouts/AppLayout';
import Upload from './pages/Upload';
import Photo from './pages/Photo';
import Devices from './pages/Devices';
import Networking from './pages/Networking';
import { Import } from './pages/import/Import';
import Device from './routes/devices/Device';
import Immich from './pages/immich/Immich';
import ImmichLogin from './components/immich/ImmichLogin';
import { Album } from './pages/Album';
import ImportOwnership from './pages/import/ImportOwnership';
import ImmichProfiles from './pages/immich/ImmichProfiles';
import ImmichMe from './pages/immich/ImmichMe';
import { System } from './pages/System';
import { useEffect } from 'react';

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