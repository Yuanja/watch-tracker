import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './contexts/AuthContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { App } from './App';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Retry failed requests once before surfacing errors
      retry: 1,
      // Data is considered fresh for 1 minute
      staleTime: 60_000,
      // Don't refetch on window focus in production to avoid excess API calls
      refetchOnWindowFocus: false,
    },
    mutations: {
      // Don't retry mutations — they have side effects
      retry: 0,
    },
  },
});

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element #root not found in the document.');
}

createRoot(rootElement).render(
  <StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <WebSocketProvider>
            <App />
          </WebSocketProvider>
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </StrictMode>
);
