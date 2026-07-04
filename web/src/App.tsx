import { RouterProvider, createBrowserRouter } from 'react-router-dom';
import Home from './routes/Home';
import Settings from './routes/Settings';
import AppLayout from './layouts/AppLayout';

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'settings', element: <Settings /> },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
