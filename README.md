# Android QR Code Generator

This Android app hooks into the built-in "share" functionality of the device. When a user shares a URL with the app, it generates a QR code for that URL and displays it on the screen.

## To install

Download the APK from the releases tab and install it

## Features

- **Share Integration**: Easily accessible via the Android share menu.
- **QR Code Generation**: Instantly generates a QR code from any shared URL or file.

## Usage

1. Share a URL or file from any app using the Android share button.
2. Select the app from the list of share options.
3. View the generated QR code on your screen.

## Building the App

1. Clone the repo
2. Open the project in Android Studio.
3. Run 'Build > Make Project' to compile the app.
4. Connect an Android device or use the emulator to run the app.

## Dependencies

- ZXing ("Zebra Crossing") library for QR code generation.
- NanoHTTPD for hosting files
- JSCH for making the files available over the internet

## Contributing

Feel free to send a PR

## License

see the `LICENSE` file for details.

