import { createRoot } from 'react-dom/client';
import { App } from './App';
import { DialogProvider } from './components/dialog';

createRoot(document.getElementById('root') as HTMLElement).render(
  <DialogProvider>
    <App />
  </DialogProvider>,
);
