import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { EntitlementsProvider } from './context/EntitlementsContext';
import { ToastProvider } from './context/ToastContext';
import { NotificationProvider } from './context/NotificationContext';
import { AppRoutes } from './routes/AppRoutes';

export function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <EntitlementsProvider>
            <ToastProvider>
              <NotificationProvider>
                <AppRoutes />
              </NotificationProvider>
            </ToastProvider>
          </EntitlementsProvider>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}

export default App;