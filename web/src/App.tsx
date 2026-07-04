import { RouterProvider, createBrowserRouter } from 'react-router-dom';
import Home from './routes/Home';
import Settings from './routes/Settings';
import AppLayout from './layouts/AppLayout';
import Upload from './routes/Home';
import Camera from './routes/Camera';

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'camera/:port/*', element: <Camera />},
      { path: 'upload', element: <Upload />},
      { path: 'settings', element: <Settings /> },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
