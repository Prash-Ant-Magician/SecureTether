# Security Policy

## About This Project

SecureTether is a personal/portfolio project exploring secure, offline, device-to-device file sharing on Android using AES encryption and Bluetooth transport. It has **not** undergone a formal third-party security audit. It's a demonstration of secure-by-design principles rather than an audited, production-hardened product — please keep that in mind when relying on it.

## Supported Versions

| Version | Supported |
|---|---|
| Latest `main` branch | ✅ |
| Older / tagged releases | ❌ |

Only the latest code on `main` receives security fixes. There is no long-term support for older versions at this stage of the project.

## Reporting a Vulnerability

If you discover a potential security issue in SecureTether, please report it responsibly:

1. **Do not** open a public Issue with exploit details or proof-of-concept code.
2. Instead, report it privately via one of these channels:
   - GitHub: use the **"Report a vulnerability"** option under the repo's **Security** tab (Security Advisories), if enabled
   - Or email the maintainer directly (see the GitHub profile [Prash-Ant-Magician](https://github.com/Prash-Ant-Magician) / [LinkedIn](https://linkedin.com/in/prashant-kumar-cybersecurity/) for contact)
3. Please include:
   - A description of the vulnerability and its potential impact
   - Steps to reproduce (if applicable)
   - Any relevant device/Android version details

## What to Expect

- Acknowledgement of your report within a reasonable timeframe
- An assessment of the issue and, if valid, a fix or mitigation on `main`
- Credit in the changelog/release notes if you'd like it (or kept anonymous, your choice)

## Scope

In scope:
- The Android app's encryption implementation (vault, key handling)
- The Bluetooth transport/pairing logic
- Local data storage handling

Out of scope:
- Issues requiring physical access to an already-unlocked, compromised device
- Vulnerabilities in third-party libraries (please report those upstream, but flagging them here is still appreciated)

Thank you for helping keep this project secure.
