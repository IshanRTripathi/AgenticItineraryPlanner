// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth, GoogleAuthProvider, EmailAuthProvider } from "firebase/auth";
// Firebase products configuration
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyD8v1WcTJ4U5IbttjI4W9hy-aDAGArVQGk",
  authDomain: "tripplanner-bdd3c.firebaseapp.com",
  projectId: "tripplanner-bdd3c",
  storageBucket: "tripplanner-bdd3c.firebasestorage.app",
  messagingSenderId: "815103657721",
  appId: "1:815103657721:web:069d08463b62088d72c428"
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

