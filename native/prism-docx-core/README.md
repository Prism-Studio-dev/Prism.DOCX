# Rust Core — Prism.DOCX Unstable

Экспериментальный Windows-компонент. Rust Core доставляется вместе с приложением,
но **автоматическая production XML validation остаётся Kotlin/JAXP**: текущий
Rust validator совпадает с Kotlin на 171 из 182 compatibility samples.
Подробности, сравнение четырёх parsers и все 11 расхождений:
[XML_COMPATIBILITY.md](XML_COMPATIBILITY.md).

Реализованы только инфраструктура, ABI version, тестовое сложение и XML
well-formedness validation. ZIP/DOCX, DOM, метаданные, модификация XML и сохранение
документов остаются в Kotlin. Linux/macOS, XSD, async и передача ownership не реализованы.

```text
Compose / Editor → XmlValidation → Kotlin/JAXP (automatic backend)
                      │
                      └─ explicit experimental Rust backend
                            → PrismNativeCore → JNA → C ABI → Rust/xml-rs
```

## Crate

```text
native/prism-docx-core/
├── Cargo.toml
├── Cargo.lock
├── README.md
├── XML_COMPATIBILITY.md
└── src/
    ├── lib.rs       # C ABI, buffer/UTF-8 checks, panic guard, tests
    └── xml.rs       # safe parsing and namespace compatibility rule
```

Package `prism-docx-core`, library `prism_docx_core`, edition 2024, `cdylib`.
Единственная Rust dependency: `xml = "1.4.0"` (xml-rs), без транзитивных зависимостей.
Единственная JVM FFI dependency: JNA 5.19.1. Cargo.lock фиксируется для воспроизводимости.
`target/`, `build/` и DLL игнорируются Git.

## ABI 1

```rust
pub extern "C" fn prism_core_api_version() -> u32
pub extern "C" fn prism_core_add(a: i32, b: i32) -> i32
pub unsafe extern "C" fn prism_core_validate_xml_utf8(data: *const u8, len: u32) -> i32
```

Экспорты используют `#[unsafe(no_mangle)]`. Версия ABI — **1**; bridge проверяет
её и доступность остальных символов до использования backend. Несовместимая DLL
даёт понятную native-layer ошибку; automatic mode продолжает с Kotlin.
`add` использует `wrapping_add`: `Int.MAX_VALUE + 1 == Int.MIN_VALUE` в обоих profiles.

| XML status | Значение |
|---|---:|
| VALID | 1 |
| INVALID_XML | 0 |
| INVALID_ARGUMENT | -1 |
| INVALID_UTF8 | -2 |
| INTERNAL_ERROR | -3 |

Rust проверяет UTF-8, единый корневой элемент, синтаксис/структуру и namespaces;
DTD запрещены. Declaration encoding не перекодирует уже декодированную строку,
как у Kotlin StringReader. Это не XSD validation. Ограничения семантики перечислены
в compatibility report; validator пока нельзя считать эквивалентом JAXP.

Buffer только читается во время синхронного вызова: Rust не сохраняет pointer,
не освобождает и не изменяет память JVM/JNA. `null + nonzero length` отвергается;
нулевая длина означает пустой, невалидный XML. Проверяются размер и переполнение
адресного диапазона. Достоверность произвольного адреса невозможно проверить:
вызывающая сторона обязана предоставить живой читаемый buffer указанной длины.
UTF-8 проверяется до parser. `catch_unwind` превращает unwind panic в INTERNAL_ERROR;
константная ABI version и wrapping arithmetic не паникуют. Abort/OOM не являются
перехватываемыми Rust unwinds.

## Kotlin API и backend

```kotlin
PrismNativeCore.add(2, 3)
PrismNativeCore.isXmlWellFormed("<root/>")
PrismNativeCore.apiVersion
PrismNativeCore.libraryPath
```

Bridge строгий и не делает fallback. Он явно кодирует UTF-8, отвергает unpaired
UTF-16 surrogates и централизует status codes. Только VALID/INVALID_XML становятся
Boolean; технические ошибки и неизвестные статусы вызывают NativeCoreException.
JNA types не выходят за пределы PrismNativeCore.kt.

Внутреннее свойство `prism.docx.xml.backend`:

- `auto` (default): загружает/проверяет Rust Core, но сохраняет Kotlin validator
  до полной compatibility; отсутствие/ошибка DLL логируется без stack trace.
- `kotlin`: явно использует JAXP, без требования native library.
- `rust`: требует настоящий experimental Rust backend, без fallback.

Fallback возможен только для native-layer failure, не для невалидного XML.
Абстракция умеет переключаться с native backend на Kotlin при технической ошибке;
automatic Rust selection заблокирована подтверждёнными semantic differences.
DOM construction и вся документная логика в любом режиме остаются Kotlin/JAXP.

## Сборка и тесты

Требуются JDK 21, Windows x64 Rust MSVC toolchain, Cargo и C++ linker.
Установленному приложению Rust toolchain/Cargo не нужны.

Из каталога crate:

```powershell
cargo fmt --check
cargo clippy --all-targets --all-features -- -D warnings
cargo test
cargo test --release
cargo build --locked
cargo build --locked --release
```

Из корня репозитория:

```powershell
.\gradlew.bat buildRustCore prepareRustCore
.\gradlew.bat buildRustCoreRelease prepareRustCoreRelease
.\gradlew.bat test
.\gradlew.bat rustCoreIntegrationTest
.\gradlew.bat rustCoreReleaseIntegrationTest
.\gradlew.bat benchmarkRustCoreXml
.\gradlew.bat run
.\gradlew.bat build packageExe packageMsi
```

| Profile | Cargo DLL | Prepared DLL |
|---|---|---|
| Debug | `native/prism-docx-core/target/debug/prism_docx_core.dll` | `build/native/rust-core/debug/prism_docx_core.dll` |
| Release | `native/prism-docx-core/target/release/prism_docx_core.dll` | `build/native/rust-core/release/prism_docx_core.dll` |

Обычная `test` не собирает Rust и явно использует Kotlin. Native integration tasks
собирают соответствующую DLL, требуют Rust-only, проверяют ABI/overflow/XML и
реальный DOCX edit/save/reopen workflow. Comparison тест сообщает фактические
расхождения и проверяет, что automatic backend остаётся Kotlin; зелёный результат
**не означает** 100% XML compatibility. Новые, не описанные в compatibility report,
расхождения ломают integration test.

## Загрузка и distribution

Порядок выбора DLL:

1. Непустое system property `prism.docx.core.library.path`.
2. Непустая environment variable `PRISM_DOCX_RUST_CORE_PATH`.
3. При наличии `compose.application.resources.dir` —
   `<resources>/rust-core/prism_docx_core.dll`.
4. Без Compose resources — `build/native/rust-core/debug/prism_docx_core.dll`
   относительно working directory.
5. Понятная ошибка strict bridge / diagnostic Kotlin fallback в automatic mode.

В packaged mode отсутствующая DLL не ищется в source tree. Override задаёт файл,
не каталог; пользовательские пути не зашиты в код. Например:

```powershell
$env:PRISM_DOCX_RUST_CORE_PATH = (Resolve-Path .\build\native\rust-core\release\prism_docx_core.dll).Path
```

`preparePackagedRustCore` зависит от `prepareRustCoreRelease`. Compose
`prepareAppResources` зависит от неё, поэтому `run`, `createDistributable`, EXE/MSI
получают свежую **Release** DLL. В установленной/распределяемой версии:

```text
PrismDOCXUnstable/
├── PrismDOCXUnstable.exe
├── runtime/
└── app/resources/rust-core/prism_docx_core.dll
```

Compose выставляет `compose.application.resources.dir`; извлечение DLL из JAR не
нужно. Startup diagnostic указывает реальный абсолютный путь и ABI. Installer
identity/upgrade UUID отделены от stable PrismDOCX, numeric package version
остаётся 1.1.1; заголовок окна содержит Unstable.

## Development benchmark

Warm-up и median семи batches; одинаковый XML для всех backend. Rust measurement
включает String → UTF-8, JNA/FFI, parsing и возврат результата. Kotlin measurement
включает штатные JAXP configuration и DOM construction. Это отчёт, не unit test
и не performance threshold. Task печатает результаты для Small, Medium и Large
XML; конкретные локальные измерения и сведения о машине не публикуются.

Development benchmark показал разные результаты в зависимости от размера XML:
Release Rust был быстрее для Small и медленнее для Medium/Large. Это не доказывает
общее преимущество Rust для production workload и не отменяет compatibility blocker.
