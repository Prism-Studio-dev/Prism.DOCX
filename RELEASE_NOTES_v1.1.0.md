# Prism.DOCX v1.1.0

This feature release brings more ways to personalize Prism.DOCX and a consistent, theme-aware workflow for opening documents and saving copies.

## What's new

- Seven built-in color themes with instant switching and persistent preferences.
- A dedicated Settings dialog for choosing the application theme.
- New Open and Save Copy dialogs that match the selected theme.

## Themes

Choose from Prism, Light, Dark, Graphite, Violet, Emerald, and Nord. The selected theme applies throughout the main interface and file dialogs. The Prism.DOCX logo now uses a single SVG that takes its color from the active theme.

## File workflow

The new Open dialog offers folder navigation, shortcuts to common locations and Windows drives, search within the current folder, and a scrollable list that shows folders before DOCX files. DOCX matching is case-insensitive, and the last-used directory is remembered. Select a file with one click, or open it with a double-click or Enter.

The Save Copy dialog lets you choose a folder and filename. It adds `.docx` when needed, validates file names, asks before replacing an existing file, and prevents saving a copy over the original document. Invalid destinations and save failures produce clear error messages.

## Improvements

- Local settings handle missing or invalid configuration more reliably while preserving existing preferences. Unknown theme values fall back to Prism.
- Document size is now read asynchronously to keep the interface responsive.
- Regression coverage has been expanded for themes, settings, and file selection.

## Compatibility

DOCX processing and metadata editing remain unchanged. Standard and custom properties, along with the XML editor for the `core`, `app`, and `custom` metadata sections, continue to work as before. Existing settings files remain compatible.

## Downloads

Windows installers for this release:

- `PrismDOCX-1.1.0.exe`
- `PrismDOCX-1.1.0.msi`
