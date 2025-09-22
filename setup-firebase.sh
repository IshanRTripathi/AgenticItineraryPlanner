#!/bin/bash

echo "Setting up Firebase for local development..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "Firebase CLI not found. Installing..."
    npm install -g firebase-tools
fi

# Login to Firebase (if not already logged in)
echo "Logging in to Firebase..."
firebase login --reauth

# Set the project
echo "Setting Firebase project to tripplanner-bdd3c..."
firebase use tripplanner-bdd3c

# Start the Firestore emulator
echo "Starting Firestore emulator..."
firebase emulators:start --only firestore

echo "Firebase setup complete!"
echo "Firestore emulator is running on http://localhost:8080"
echo "You can now run your Spring Boot application with FIRESTORE_USE_EMULATOR=true"