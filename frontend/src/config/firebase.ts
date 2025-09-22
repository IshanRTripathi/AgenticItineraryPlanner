// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
// TODO: Add SDKs for Firebase products that you want to use
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

export default app;

