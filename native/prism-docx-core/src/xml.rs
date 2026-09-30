use ::xml::Encoding;
use ::xml::namespace::{NS_XML_PREFIX, NS_XML_URI, NS_XMLNS_PREFIX, NS_XMLNS_URI};
use ::xml::reader::{ParserConfig, XmlEvent};

/// Checks XML with Kotlin's single-root, DTD and decoded-string encoding policy.
/// Namespace compatibility regressions are checked by the JVM comparison test.
pub(crate) fn validate_xml(text: &str) -> bool {
    // XmlSupport parses StringReader: declaration labels do not re-decode text.
    let mut parser = ParserConfig::new()
        .allow_multiple_root_elements(false)
        .override_encoding(Some(Encoding::Utf8))
        .ignore_invalid_encoding_declarations(true)
        .create_reader(text.as_bytes());

    loop {
        match parser.next() {
            Ok(XmlEvent::Doctype { .. }) | Err(_) => return false,
            Ok(XmlEvent::StartElement { namespace, .. }) => {
                // Reserved namespace URIs cannot be assigned to arbitrary prefixes.
                // xml-rs checks the reserved prefixes, but leaves these URI aliases unchecked.
                if namespace.iter().any(|(prefix, uri)| {
                    (uri == NS_XML_URI && prefix != NS_XML_PREFIX)
                        || (uri == NS_XMLNS_URI && prefix != NS_XMLNS_PREFIX)
                }) {
                    return false;
                }
            }
            Ok(XmlEvent::EndDocument) => return true,
            Ok(_) => {}
        }
    }
}

#[cfg(test)]
mod tests {
    use super::validate_xml;

    #[test]
    fn accepts_xml_declarations_namespaces_and_docx_property_shapes() {
        let samples = [
            r#"<?xml version="1.0" encoding="UTF-8"?><root><value>test</value></root>"#,
            r#"<n:root xmlns:n="urn:prism:test"><n:value>test</n:value></n:root>"#,
            r#"<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Prism</dc:title></cp:coreProperties>"#,
            r#"<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Prism.DOCX</Application><TitlesOfParts><vt:vector size="1" baseType="lpstr"><vt:lpstr>Title</vt:lpstr></vt:vector></TitlesOfParts></Properties>"#,
            r#"<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/custom-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><property fmtid="{D5CDD505-2E9C-101B-9397-08002B2CF9AE}" pid="2" name="Test"><vt:lpwstr>Привет</vt:lpwstr></property></Properties>"#,
            "<root>Привет, мир! 🦀 &amp; &lt; &gt; &quot; &apos; &#x41; &#65;</root>",
            "<root><![CDATA[<!DOCTYPE is only text here]]></root>",
            "<!-- <!DOCTYPE is only a comment here --> <root/>",
            r#"<?xml version="1.0" encoding="UTF-16"?><root>Привет</root>"#,
            r#"<?xml version="1.0" encoding="123"?><root/>"#,
        ];
        for xml in samples {
            assert!(validate_xml(xml), "Rejected valid XML: {xml}");
        }
    }

    #[test]
    fn rejects_malformed_empty_and_doctype_inputs() {
        let samples = [
            "<root>",
            "<root><a></root>",
            "<root><a></root></a>",
            "<root attribute=unquoted/>",
            "<root><value>truncated</val",
            "",
            " \n\t ",
            "<?xml version=\"1.0\"?>",
            "<root/><second/>",
            "<root>&unknown;</root>",
            "<root>&#0;</root>",
            "<root>\0</root>",
            "<n:root/>",
            "<root a=\"1\" a=\"2\"/>",
            "<!DOCTYPE root><root/>",
            "<!DOCTYPE root [<!ENTITY value 'test'>]><root>&value;</root>",
            "<!DOCTYPE root SYSTEM 'file:///must-not-be-read'><root/>",
            "<?xml version=\"wrong\"?><root/>",
            "<root><!-- invalid -- comment --></root>",
        ];
        for xml in samples {
            assert!(!validate_xml(xml), "Accepted invalid XML: {xml}");
        }
    }

    #[test]
    fn rejects_aliases_for_reserved_namespace_uris() {
        for prefix in ["n", "metadata", "another"] {
            for uri in [super::NS_XML_URI, super::NS_XMLNS_URI] {
                let xml = format!(r#"<root xmlns:{prefix}="{uri}"/>"#);
                assert!(
                    !validate_xml(&xml),
                    "Accepted reserved namespace alias: {xml}"
                );
            }
        }
        assert!(validate_xml(
            r#"<root xmlns:xml="http://www.w3.org/XML/1998/namespace" xml:lang="ru"/>"#
        ));
    }
}
