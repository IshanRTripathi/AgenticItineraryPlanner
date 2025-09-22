#!/bin/bash

# Setup script for Firestore emulator
# This script downloads and sets up the Firebase emulator for local development

set -e

echo "🚀 Setting up Firestore emulator for local development..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js first."
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed. Please install npm first."
    exit 1
fi

# Install Firebase CLI globally if not already installed
if ! command -v firebase &> /dev/null; then
    echo "📦 Installing Firebase CLI..."
    npm install -g firebase-tools
else
    echo "✅ Firebase CLI is already installed"
fi

# Create firebase.json configuration file
echo "📝 Creating Firebase configuration..."
cat > firebase.json << EOF
{
  "emulators": {
    "firestore": {
      "port": 8080,
      "host": "localhost"
    },
    "ui": {
      "enabled": true,
      "port": 4000
    }
  }
}
EOF

# Create .firebaserc file
echo "📝 Creating Firebase project configuration..."
cat > .firebaserc << EOF
{
  "projects": {
    "default": "tripplanner-bdd3c"
  }
}
EOF

echo "✅ Firestore emulator setup complete!"
echo ""
echo "To start the emulator, run:"
echo "  firebase emulators:start --only firestore"
echo ""
echo "The emulator will be available at:"
echo "  - Firestore: localhost:8080"
echo "  - Emulator UI: http://localhost:4000"
echo ""
echo "To use the emulator in your application, set these environment variables:"
echo "  FIRESTORE_USE_EMULATOR=true"
echo "  FIRESTORE_EMULATOR_HOST=localhost:8080"
echo "  APP_DATABASE_TYPE=firestore"
