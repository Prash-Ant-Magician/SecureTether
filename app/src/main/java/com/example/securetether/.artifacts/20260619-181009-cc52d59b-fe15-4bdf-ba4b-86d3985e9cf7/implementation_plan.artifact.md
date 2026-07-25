# Secure Vault Implementation Plan

## Problem Audit
The current implementation has several security flaws and functional issues:
1. **Lack of In-Memory Viewing**: While `VaultRepositoryImpl` encrypts files during import, there is no corresponding method to decrypt them for viewing. The UI likely tries to access the `encryptedPath` directly, which fails because the file is not a valid image/PDF.
2. **Metadata Vulnerability**: The `displayName` and `mimeType` are stored in plaintext in the database. While acceptable for some, a truly secure vault might want to encrypt these as well.
3. **Internal Storage Use**: Currently, it uses `context.filesDir/vault`, which is good (app-private), but we need to ensure no other components (like `FileProvider`) are accidentally exposing this.
4. **PDF Rendering**: PDF rendering from memory is non-trivial in Android as `PdfRenderer` requires a `ParcelFileDescriptor`.

## Proposed Changes

### 1. Storage Architecture
- Maintain use of `context.filesDir/vault` for encrypted blobs.
- Ensure all files are named with random UUIDs to avoid information leakage via filenames.

### 2. Encryption Flow
- **Encryption**: `KeystoreManager` already uses AES-256-GCM. I will verify it correctly prepends the IV.
- **Decryption**: Ensure `KeystoreManager.decrypt` is robust.

### 3. File Import Implementation
- Refine `VaultRepositoryImpl.importFile` to ensure that if the input stream is read into a `ByteArray`, it is cleared after encryption (if possible, though `ByteArray` is immutable, we can overwrite it if we use a mutable collection or just rely on GC for short-lived buffers).

### 4. File Viewing Implementation
- Add `decryptFile(fileId: String): ByteArray?` to `VaultRepository`.
- Update `VaultViewModel` to provide a state for the currently viewing file's decrypted data.
- **Images**: Render using `BitmapFactory.decodeByteArray`.
- **PDFs**: Use a `ContentProvider` or a `Pipe` to feed `PdfRenderer` without creating a permanent file. Alternatively, use `File.createTempFile` in the internal cache directory, open it, and **immediately delete it** (the file descriptor remains valid until closed). This is a standard secure way to handle `PdfRenderer`.

## File Changes

#### [VaultRepository.kt](file:///C:/Users/prash/AndroidStudioProjects/SecureTether/app/src/main/java/com/example/securetether/domain/repository/VaultRepository.kt)
- Add `suspend fun getDecryptedFile(fileId: String): ByteArray?`

#### [VaultRepositoryImpl.kt](file:///C:/Users/prash/AndroidStudioProjects/SecureTether/app/src/main/java/com/example/securetether/data/repository/VaultRepositoryImpl.kt)
- Implement `getDecryptedFile` using `KeystoreManager.decrypt`.

#### [VaultViewModel.kt](file:///C:/Users/prash/AndroidStudioProjects/SecureTether/app/src/main/java/com/example/securetether/ui/viewmodel/VaultViewModel.kt)
- Add `val decryptedFile = MutableStateFlow<ByteArray?>(null)`
- Add `fun viewFile(fileId: String)` which fetches and decrypts the file.
- Handle clearing the decrypted data when the view is closed.

## Verification Plan
1. **Automated Tests**: Add unit tests for `KeystoreManager` to ensure GCM integrity and correct IV handling.
2. **Manual Verification**:
   - Import an image, verify it can be viewed in the app.
   - Verify the file on disk (via Device Explorer) is unreadable (encrypted).
   - Import a PDF, verify it can be viewed.
   - Ensure no temporary files are left behind after viewing.
