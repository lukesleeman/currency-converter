# Currency Converter App

A modern Android currency converter app built with Jetpack Compose that allows users to convert
between multiple currencies in real-time.

## Features

- **Real-time Currency Conversion**: Automatically converts amounts as you type
- **Multiple Currencies**: Support for 20+ major world currencies with flag emojis
- **Add/Remove Currencies**: Easily add or remove currencies from your conversion list
- **Tap to Switch Base Currency**: Tap any currency to make it the base for conversion
- **Offline Support**: Works with default exchange rates when network is unavailable
- **Modern UI**: Clean, Material Design 3 interface with smooth animations
- **Automatic Refresh**: Fresh exchange rates from exchangerate-api.com

## Technical Details

- **Architecture**: MVVM with Repository pattern
- **UI Framework**: Jetpack Compose with Material Design 3
- **Networking**: Retrofit with OkHttp for API calls
- **State Management**: StateFlow and Compose State
- **API**: exchangerate-api.com for live exchange rates
- **Offline**: Default exchange rates for offline functionality

## Setup

### API Key Configuration

This app uses the [exchangerate-api.com](https://exchangerate-api.com) API for live exchange rates. To set up:

1. Get a free API key from [exchangerate-api.com](https://exchangerate-api.com)
2. Create a file named `apikeys.properties` in the root directory
3. Add your API key to the file:
   ```
   EXCHANGE_RATE_API_KEY=your_api_key_here
   ```

**Note**: The `apikeys.properties` file is gitignored and will not be committed to version control for security.

## Building the App

1. Clone the repository
2. Set up the API key (see above)
3. Open in Android Studio
4. Build and run on an Android device or emulator (API 24+)
