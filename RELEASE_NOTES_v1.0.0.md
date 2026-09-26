# Prism.DOCX v1.0.0

First public release of Prism.DOCX.

## Highlights

- Edit 39 standard DOCX metadata properties and create or manage custom properties.
- Inspect and edit the `core`, `app`, and `custom` metadata XML parts.
- Open a DOCX file from a file picker or by dragging one file into the application.
- Search metadata fields, validate common value types, and switch between light and dark themes.
- Save changes to a separate copy while keeping the source document intact.

## Included

- Windows x64 EXE and MSI installers.
- Start Menu shortcut and Windows installation registration from the installers.
- Source code and instructions to build the application with the Gradle Wrapper.

## Notes

- The editor changes document properties; it does not edit document text, comments, tracked changes, or arbitrary `customXml` parts.
- XML editing checks syntax and common value types but does not perform full OOXML schema validation.
- Word may recalculate stored document statistics when it next saves the document.
- Editing the `DigSig` XML part does not create a valid digital signature.
