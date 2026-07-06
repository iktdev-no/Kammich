import { RouterProvider, createBrowserRouter } from 'react-router-dom';
import Settings from './routes/Settings';
import AppLayout from './layouts/AppLayout';
import Camera from './routes/Camera';
import Upload from './routes/Upload';
import Photo from './routes/Photo';

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Photo /> },
      { path: 'camera/:sn/*', element: <Camera />},
      { path: 'upload', element: <Upload />},
      { path: 'settings', element: <Settings /> },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
