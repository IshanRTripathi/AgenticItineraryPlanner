// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth, GoogleAuthProvider, EmailAuthProvider } from "firebase/auth";
// Firebase products configuration
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration from environment variables
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyCSXcYunx1yGwX8L1qog1Zij0iEQnAMP7Q",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "tripaiplanner-4c951.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "tripaiplanner-4c951",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "tripaiplanner-4c951.firebasestorage.app",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "464825394931",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:464825394931:web:default"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firestore
export const db = getFirestore(app);

// Initialize Firebase Authentication
export const auth = getAuth(app);

// Configure Authentication Providers
export const googleProvider = new GoogleAuthProvider();
export const emailProvider = new EmailAuthProvider();

// Configure Google Auth Provider
googleProvider.setCustomParameters({
  prompt: 'select_account'
});

export default app;

