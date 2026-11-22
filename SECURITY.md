# Security Policy

## Supported versions

| Version | Supported |
| ------- | --------- |
| 1.0.x   | Yes       |

## Reporting a vulnerability

Please do not open a public issue for security problems. Use GitHub's private
vulnerability reporting instead: open the repository's **Security** tab and
choose **Report a vulnerability**. Include the app version, your Android
version, and steps to reproduce.

## Security model

- The app requests no permissions and has no internet access, so it cannot send data anywhere.
- Saved combos and fee settings live in app-private storage on the device.
- Auto backup and device-to-device transfer are disabled.
- Cleartext traffic is disabled and lint treats exported components and insecure settings as errors.
- Release builds are minified and signed. Release APKs are published on GitHub Releases with a SHA-256 checksum in the release notes.
- Dependencies come only from AndroidX, Google and JetBrains.
