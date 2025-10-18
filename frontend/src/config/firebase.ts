// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth, GoogleAuthProvider, EmailAuthProvider } from "firebase/auth";
// Firebase products configuration
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyCSXcYunx1yGwX8L1qog1Zij0iEQnAMP7Q",
  authDomain: "tripaiplanner-4c951.firebaseapp.com",
  projectId: "tripaiplanner-4c951",
  storageBucket: "tripaiplanner-4c951.firebasestorage.app",
  messagingSenderId: "464825394931",
  appId: "1:464825394931:web:default"
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

