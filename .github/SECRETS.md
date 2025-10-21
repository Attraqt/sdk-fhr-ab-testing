# GitHub Actions Secrets Configuration

To publish via GitHub Actions with JReleaser, you need to configure the following secrets in your GitHub repository.

## Required GitHub Secrets

Go to: **Settings → Secrets and variables → Actions → New repository secret**

### 1. Maven Central Credentials

#### `JRELEASER_MAVENCENTRAL_USERNAME`
- **Value**: Your Sonatype Central Portal token username
- **How to get**: 
  1. Log in to https://central.sonatype.com/
  2. Go to "View Account" → "Generate User Token"
  3. Copy the **username** part

#### `JRELEASER_MAVENCENTRAL_TOKEN`
- **Value**: Your Sonatype Central Portal token password
- **How to get**: 
  1. Same as above
  2. Copy the **password** part

### 2. GPG Signing Keys

#### `JRELEASER_GPG_PASSPHRASE`
- **Value**: Your GPG key passphrase

#### `JRELEASER_GPG_SECRET_KEY`
- **Value**: Base64-encoded GPG private key
- **How to get**:
  ```bash
  # Export your private key (replace YOUR_KEY_ID with your actual key ID)
  gpg --export-secret-keys YOUR_KEY_ID | base64
  
  # Or if you want it as a single line (recommended):
  gpg --export-secret-keys YOUR_KEY_ID | base64 | tr -d '\n'
  ```
  **Note (macOS users)**: If you see a `%` at the end of the output, **do NOT include it** - it's just a zsh shell indicator, not part of the actual key.

#### `JRELEASER_GPG_PUBLIC_KEY`
- **Value**: Base64-encoded GPG public key
- **How to get**:
  ```bash
  # Export your public key
  gpg --export YOUR_KEY_ID | base64
  
  # Or as single line:
  gpg --export YOUR_KEY_ID | base64 | tr -d '\n'
  ```
  **Note (macOS users)**: If you see a `%` at the end of the output, **do NOT include it** - it's just a zsh shell indicator, not part of the actual key.

### 3. GitHub Token

#### `GITHUB_TOKEN`
- **No action needed**: This is automatically provided by GitHub Actions
- Used by JReleaser for creating GitHub releases (optional)
