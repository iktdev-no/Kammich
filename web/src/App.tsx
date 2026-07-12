import { RouterProvider, createBrowserRouter } from 'react-router-dom';
import Settings from './routes/Settings';
import AppLayout from './layouts/AppLayout';
import Camera from './routes/Camera';
import Upload from './routes/Upload';
import Photo from './routes/Photo';
import Devices from './routes/Devices';
import WifiSettings from './routes/settings/WifiSettings';

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Photo /> },
      { path: "photo/:sn", element: <Photo /> },
      { path: 'camera/:sn/*', element: <Camera />},
      { path: 'devices', element: <Devices />},
      { path: 'upload', element: <Upload />},
      { path: 'settings', element: <Settings /> },
      { path: 'settings/wifi', element: <WifiSettings /> },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
