# XML compatibility investigation

Rust XML validation remains experimental. Automatic application validation uses
the Kotlin/JAXP backend because the expanded corpus has confirmed differences.
Loading and packaging Rust Core does not imply semantic compatibility.

## Corpus and oracle

`src/test/kotlin/com/prismdocx/nativecore/XmlValidationSamples.kt` contains **182
samples**. Every native comparison invokes both `XmlSupport.parseWithKotlin`
(the actual production JAXP settings) and `PrismNativeCore.isXmlWellFormed`.
The Kotlin reference is independent of the selected backend.

The corpus covers ordinary and DOCX-shaped metadata XML, declarations, Unicode,
UTF-16 surrogate rejection, XML 1.0 and 1.1 character rules, comments, CDATA,
processing instructions, entities and attributes, root/document structure,
namespace scope/redeclaration, reserved prefixes/URIs, duplicates, malformed and
truncated documents, large attributes, repeated siblings, and 200 nested elements.
Samples are retained when parsers disagree.

Before changing the validator, each candidate was run against the same 181
UTF-8-representable samples. A disposable Java oracle copied `XmlSupport`'s exact
`DocumentBuilderFactory` settings: namespace awareness, secure processing,
DTD rejection, no XInclude, no external DTD/schema access, and `StringReader`
input. The additional unpaired UTF-16 surrogate is tested at the Kotlin bridge;
it cannot be represented by a valid Rust `&str`.

## Candidate results

| Candidate/configuration | Matches / 181 | Differences |
|---|---:|---:|
| xml-rs (`xml` 1.4.0), previous validator configuration | 168 | 13 |
| roxmltree 0.21.1, `Document::parse`, DTD disabled by default | 160 | 21 |
| quick-xml 0.42.0, `NsReader` plus documented checks below | 139 | 42 |
| xmlparser 0.13.6 tokenizer plus document structure checks | 143 | 38 |
| Selected xml-rs plus the reserved-URI compatibility rule | **170** | **11** |

The final JVM corpus includes the surrogate sample: **171/182 match**, with
**11 confirmed differences**. Debug and Release integration tests report the
same limitations rather than claiming full compatibility.

quick-xml was configured to check comments and closing tag names. The probe
also enforced one root, complete nesting, no DTD or root-level text/CDATA,
resolved element/attribute namespace prefixes, checked duplicate attribute
syntax, decoded attribute references, and resolved predefined entities and
numeric references. Its event reader still requires additional validation of
XML names, declarations, reserved namespaces, character rules, and expanded
attribute uniqueness. Its own [reader configuration documentation](https://docs.rs/quick-xml/0.42.0/quick_xml/reader/struct.Config.html)
describes the checks provided by the reader.

xmlparser provides lexical tokens, not a complete document or namespace
validator. The probe enforced a matching element stack, one root, no DTD,
duplicate raw attribute names, and no root-level text/CDATA. It still requires
entity and namespace validation. See the [tokenizer documentation](https://docs.rs/xmlparser/0.13.6/xmlparser/struct.Tokenizer.html).

roxmltree was tested as a complete parser. It disagreed on declaration values,
reserved processing instruction targets, namespaces, XML 1.1, BOM handling of
decoded strings, and some character references. A separate Debug executable
also exhausted the default Windows Rust stack with 200 nested elements; the
whole-corpus investigation used a 16 MiB worker stack to finish comparison.
xml-rs processed that depth successfully on the default stack. See
[roxmltree parsing policy](https://github.com/RazrFalcon/roxmltree/blob/master/docs/parsing.md).

xml-rs is retained because it produced the most matches, supports the important
XML 1.1 character-reference distinction, uses safe Rust and a streaming parser,
and adds no transitive dependencies. A complete custom validity grammar on top
of quick-xml/xmlparser or a vendored parser fork would be a larger change than
the allowed small compatibility layer.

## General compatibility rule added

xml-rs already checks rebinding the reserved `xml` and `xmlns` prefixes, but
allows another prefix to bind their reserved namespace URIs. `xml.rs` now checks
every namespace map emitted by the parser: the XML URI can only belong to `xml`,
and the XMLNS URI can only belong to `xmlns`. This covers arbitrary aliases,
including nested bindings. No input string is rewritten or specially recognized.
Unit tests exercise several unrelated aliases for both URIs.

## Remaining differences

| Corpus sample | Kotlin | Rust | Cause |
|---|---|---|---|
| `predefined xml namespace element`: `<xml:root/>` | valid | invalid | xml-rs prohibits the `xml` element prefix. |
| `xml element nested` | valid | invalid | Same prefix prohibition inside a document. |
| `xml element explicit binding` | valid | invalid | Same prohibition despite the correct explicit XML namespace URI. |
| `duplicate namespace declaration` | invalid | valid | Namespace declarations are collapsed into a map without duplicate rejection. |
| `namespace duplicate same URI` | invalid | valid | Duplicate rejection is missing even when both URIs are equal. |
| `duplicate default namespace` | invalid | valid | The duplicate-declaration problem also affects the default namespace. |
| `declaration version 1.2` | invalid | valid | xml-rs accepts XML `1.x` declarations as XML 1.0; this JAXP implementation only accepts 1.0/1.1. |
| `empty prefix binding XML 1.1` | valid | invalid | JAXP supports XML 1.1 namespace undeclaration; xml-rs rejects empty prefixed bindings. |
| `namespace empty prefix name`: `<:root/>` | valid | invalid | This JAXP implementation accepts an empty QName prefix; xml-rs rejects it. |
| `XML 1.1 next line outside root` | valid | invalid | JAXP normalizes U+0085 as an XML 1.1 newline; xml-rs rejects it in the prolog. |
| `Unicode supplementary name`: `<🦀/>` | invalid | valid | The JAXP XML 1.0 implementation uses older name rules; xml-rs follows the broader modern XML name ranges. |

The Rust validator must not become the automatic application backend until these
differences are resolved through general parser behavior and the entire corpus
matches. XML parse errors are distinct from native availability/ABI failures;
fallback behavior must not hide these semantic differences.
