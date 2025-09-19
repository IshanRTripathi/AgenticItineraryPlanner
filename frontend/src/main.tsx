 
import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './state/query/client'
import { Toaster } from './components/ui/sonner'
import { BrowserRouter } from 'react-router-dom'
 
 createRoot(document.getElementById("root")!).render(
   <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <App />
    </BrowserRouter>
    <Toaster richColors position="top-right" />
   </QueryClientProvider>
 );
  