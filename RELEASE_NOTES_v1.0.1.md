# Prism.DOCX v1.0.1

Maintenance release focused on internal performance and code quality. No new user-facing features.

## Improvements

- Removed redundant XML serialization and parsing while opening DOCX packages.
- Reused parsed metadata when refreshing standard fields, custom properties, and validation results.
- Kept derived editor state in one update so its values stay in sync.

## Notes

- DOCX editing, XML editing, and saving to a separate copy retain their v1.0.0 behavior.
- No file-format or dependency changes.
