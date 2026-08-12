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
import ImmichAccess from './routes/immich/ImmichAccess';
import ImmichLogin from './components/immich/ImmichLogin';
import { Album } from './routes/Album';
import ImportOwnership from './routes/ImportOwnership';

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
      { path: 'ownership', element: <ImportOwnership /> },
      { path: 'settings/networking', element: <Networking /> },
      { path: 'settings/wifi', element: <WifiSettings /> },
      { path: 'settings/ap', element: <WifiApSettings /> },
      { path: 'settings/immich', element: <Immich /> },
      { path: 'settings/immich/login', element: <ImmichLogin /> },
      { path: 'settings/immich/access', element: <ImmichAccess /> },

    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
