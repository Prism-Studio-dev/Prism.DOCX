package com.prismdocx.nativecore

import com.prismdocx.metadata.XmlSupport
import org.xml.sax.SAXException

internal data class XmlValidationSample(val name: String, val xml: String, val expected: Boolean)

// Always use the production JAXP settings, independently of the selected backend.
internal fun kotlinXmlWellFormed(xml: String): Boolean = try {
    XmlSupport.parseWithKotlin(xml)
    true
} catch (_: SAXException) {
    false
}

internal val corePropertiesXml = """
    <?xml version="1.0" encoding="UTF-8"?>
    <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dcterms="http://purl.org/dc/terms/"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <dc:title>Prism.DOCX — Пример &amp; XML</dc:title>
        <dc:creator>Developer</dc:creator>
        <dcterms:created xsi:type="dcterms:W3CDTF">2026-09-30T12:00:00Z</dcterms:created>
    </cp:coreProperties>
""".trimIndent()

internal val appPropertiesXml = """
    <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
        xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
        <Application>Prism.DOCX</Application><Pages>1</Pages>
        <TitlesOfParts><vt:vector size="1" baseType="lpstr"><vt:lpstr>Example</vt:lpstr></vt:vector></TitlesOfParts>
    </Properties>
""".trimIndent()

internal val customPropertiesXml = """
    <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/custom-properties"
        xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
        <property fmtid="{D5CDD505-2E9C-101B-9397-08002B2CF9AE}" pid="2" name="Project">
            <vt:lpwstr>Prism — Кириллица</vt:lpwstr>
        </property>
        <property fmtid="{D5CDD505-2E9C-101B-9397-08002B2CF9AE}" pid="3" name="Count"><vt:i4>42</vt:i4></property>
    </Properties>
""".trimIndent()

// The expected values were checked against XmlSupport's actual JAXP configuration.
// Includes XML 1.1 and legacy JAXP name rules; do not narrow this corpus to fit Rust.
// The expected values were checked against XmlSupport's actual JAXP configuration.
// Includes XML 1.1 and legacy JAXP name rules; do not narrow this corpus to fit Rust.
internal val xmlValidationSamples = listOf(
    XmlValidationSample("simple valid", "<root><value>42</value></root>", true),
    XmlValidationSample("malformed nesting", "<root><value></root>", false),
    XmlValidationSample("unclosed root", "<root>", false),
    XmlValidationSample("crossed closing tags", "<root><a></root></a>", false),
    XmlValidationSample("unquoted attribute", "<root value=42/>", false),
    XmlValidationSample("truncated XML", "<root><value>42</val", false),
    XmlValidationSample("namespace XML", "<n:root xmlns:n=\"urn:prism:test\"><n:value n:id=\"1\"/></n:root>", true),
    XmlValidationSample("core-like XML", corePropertiesXml, true),
    XmlValidationSample("app-like XML", appPropertiesXml, true),
    XmlValidationSample("custom-like XML", customPropertiesXml, true),
    XmlValidationSample("empty input", "", false),
    XmlValidationSample("whitespace only", " \n\t ", false),
    XmlValidationSample("XML declaration", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>", true),
    XmlValidationSample("declaration only", "<?xml version=\"1.0\"?>", false),
    XmlValidationSample("declaration after root", "<root/><?xml version=\"1.0\"?>", false),
    XmlValidationSample("unsupported XML version", "<?xml version=\"2.0\"?><root/>", false),
    XmlValidationSample("ignored encoding name", "<?xml version=\"1.0\" encoding=\"123\"?><root/>", true),
    XmlValidationSample("encoding label on decoded String", "<?xml version=\"1.0\" encoding=\"UTF-16\"?><root>\u041f\u0440\u0438\u0432\u0435\u0442</root>", true),
    XmlValidationSample("Unicode and Cyrillic", "<\u043a\u043e\u0440\u0435\u043d\u044c \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435=\"\u041f\u0440\u0438\u0432\u0435\u0442\">\u041a\u0438\u0440\u0438\u043b\u043b\u0438\u0446\u0430 \ud83e\udd80</\u043a\u043e\u0440\u0435\u043d\u044c>", true),
    XmlValidationSample("escaped entities", "<root value=\"&quot;&apos;\">&amp;&lt;&gt;&#65;&#x41;</root>", true),
    XmlValidationSample("unknown entity", "<root>&unknown;</root>", false),
    XmlValidationSample("invalid character reference", "<root>&#0;</root>", false),
    XmlValidationSample("literal NUL", "<root>\u0000</root>", false),
    XmlValidationSample("multiple roots", "<root/><another/>", false),
    XmlValidationSample("text after root", "<root/>trailing", false),
    XmlValidationSample("undeclared namespace prefix", "<n:root/>", false),
    XmlValidationSample("duplicate attribute", "<root a=\"1\" a=\"2\"/>", false),
    XmlValidationSample("empty DOCTYPE", "<!DOCTYPE root><root/>", false),
    XmlValidationSample("internal entity DTD", "<!DOCTYPE root [<!ENTITY x 'test'>]><root>&x;</root>", false),
    XmlValidationSample("external DTD", "<!DOCTYPE root SYSTEM 'file:///must-not-be-read'><root/>", false),
    XmlValidationSample("DOCTYPE text inside CDATA", "<root><![CDATA[<!DOCTYPE is text here]]></root>", true),
    XmlValidationSample("DOCTYPE text inside comment", "<!-- <!DOCTYPE is text here --> <root/>", true),
    XmlValidationSample("invalid comment", "<root><!-- not -- allowed --></root>", false),
    XmlValidationSample("processing instruction", "<?prism test?><root/>", true),
    XmlValidationSample("XML 1.1", "<?xml version=\"1.1\"?><root/>", true),
    XmlValidationSample("predefined xml namespace attribute", "<root xml:lang=\"ru\"/>", true),
    XmlValidationSample("predefined xml namespace element", "<xml:root/>", true),
    XmlValidationSample("duplicate namespace declaration", "<root xmlns:n=\"urn:a\" xmlns:n=\"urn:b\"/>", false),
    XmlValidationSample("self-closing root", "<root/>", true),
    XmlValidationSample("single quoted attributes", "<root a='one' b='two'/>", true),
    XmlValidationSample("mixed content", "<root>one<child/>two<child>three</child>four</root>", true),
    XmlValidationSample("spaces around root", " \t\r\n<root/> \r\n", true),
    XmlValidationSample("text before root", "before<root/>", false),
    XmlValidationSample("bare text", "text", false),
    XmlValidationSample("comment only", "<!-- no root -->", false),
    XmlValidationSample("PI only", "<?prism no-root?>", false),
    XmlValidationSample("CDATA only", "<![CDATA[text]]>", false),
    XmlValidationSample("CDATA before root", "<![CDATA[text]]><root/>", false),
    XmlValidationSample("CDATA after root", "<root/><![CDATA[text]]>", false),
    XmlValidationSample("valid CDATA", "<root><![CDATA[<tag a=\"1\">&unknown;</tag>]]></root>", true),
    XmlValidationSample("adjacent CDATA", "<root><![CDATA[first]]><![CDATA[second]]></root>", true),
    XmlValidationSample("unterminated CDATA", "<root><![CDATA[truncated</root>", false),
    XmlValidationSample("CDATA delimiter in text", "<root>text]]>text</root>", false),
    XmlValidationSample("empty comment", "<!----><root/>", true),
    XmlValidationSample("comments around root", "<!-- before --><root/><!-- after -->", true),
    XmlValidationSample("comment inside element", "<root><!-- child --><child/><!-- final --></root>", true),
    XmlValidationSample("comment trailing hyphen", "<root><!-- bad ---></root>", false),
    XmlValidationSample("unterminated comment", "<root><!-- truncated</root>", false),
    XmlValidationSample("comment nested opener", "<root><!-- <!-- nested --> --></root>", false),
    XmlValidationSample("PI inside element", "<root><?prism key=\"value\"?></root>", true),
    XmlValidationSample("PI after root", "<root/><?prism after?>", true),
    XmlValidationSample("empty PI content", "<?prism?><root/>", true),
    XmlValidationSample("PI reserved uppercase target", "<?XML version=\"1.0\"?><root/>", false),
    XmlValidationSample("PI reserved mixedcase target", "<?Xml?><root/>", false),
    XmlValidationSample("PI invalid target", "<?1bad?><root/>", false),
    XmlValidationSample("PI colon target", "<?n:prism value?><root/>", true),
    XmlValidationSample("PI unterminated", "<?prism value<root/>", false),
    XmlValidationSample("xml-stylesheet PI", "<?xml-stylesheet href=\"style.xsl\"?><root/>", true),
    XmlValidationSample("declaration single quotes", "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><root/>", true),
    XmlValidationSample("declaration standalone no", "<?xml version=\"1.0\" standalone=\"no\"?><root/>", true),
    XmlValidationSample("declaration invalid standalone", "<?xml version=\"1.0\" standalone=\"maybe\"?><root/>", false),
    XmlValidationSample("declaration invalid version", "<?xml version=\"wrong\"?><root/>", false),
    XmlValidationSample("declaration version 1.2", "<?xml version=\"1.2\"?><root/>", false),
    XmlValidationSample("declaration missing version", "<?xml encoding=\"UTF-8\"?><root/>", false),
    XmlValidationSample("declaration missing whitespace", "<?xml version=\"1.0\"encoding=\"UTF-8\"?><root/>", false),
    XmlValidationSample("declaration wrong order", "<?xml encoding=\"UTF-8\" version=\"1.0\"?><root/>", false),
    XmlValidationSample("declaration duplicate version", "<?xml version=\"1.0\" version=\"1.0\"?><root/>", false),
    XmlValidationSample("declaration extra attribute", "<?xml version=\"1.0\" extra=\"x\"?><root/>", false),
    XmlValidationSample("declaration leading whitespace", " <?xml version=\"1.0\"?><root/>", false),
    XmlValidationSample("declaration newline separator", "<?xml\nversion=\"1.0\"?><root/>", true),
    XmlValidationSample("declaration tab separator", "<?xml\tversion=\"1.0\"?><root/>", true),
    XmlValidationSample("declaration encoding empty", "<?xml version=\"1.0\" encoding=\"\"?><root/>", true),
    XmlValidationSample("declaration encoding unknown", "<?xml version=\"1.0\" encoding=\"NOT-A-REAL-ENCODING\"?><root/>", true),
    XmlValidationSample("declaration encoding entity", "<?xml version=\"1.0\" encoding=\"UTF&#45;8\"?><root/>", true),
    XmlValidationSample("BOM before declaration", "\ufeff<?xml version=\"1.0\"?><root/>", false),
    XmlValidationSample("BOM before root", "\ufeff<root/>", false),
    XmlValidationSample("BOM inside text", "<root>\ufeff</root>", true),
    XmlValidationSample("BOM after whitespace", " \ufeff<root/>", false),
    XmlValidationSample("entities in attributes", "<root a=\"&amp;&lt;&gt;&quot;&apos;&#10;&#x9;\"/>", true),
    XmlValidationSample("unknown entity in attribute", "<root a=\"&unknown;\"/>", false),
    XmlValidationSample("bare ampersand text", "<root>a & b</root>", false),
    XmlValidationSample("bare ampersand attribute", "<root a=\"a & b\"/>", false),
    XmlValidationSample("incomplete entity", "<root>&amp</root>", false),
    XmlValidationSample("uppercase entity", "<root>&AMP;</root>", false),
    XmlValidationSample("empty entity", "<root>&;</root>", false),
    XmlValidationSample("empty char reference", "<root>&#;</root>", false),
    XmlValidationSample("empty hex reference", "<root>&#x;</root>", false),
    XmlValidationSample("out of range char reference", "<root>&#x110000;</root>", false),
    XmlValidationSample("surrogate reference", "<root>&#xD800;</root>", false),
    XmlValidationSample("noncharacter reference", "<root>&#xFFFF;</root>", false),
    XmlValidationSample("supplementary reference", "<root>&#x1F980;</root>", true),
    XmlValidationSample("XML newline characters", "<root>\t\n\r</root>", true),
    XmlValidationSample("attribute literal less-than", "<root a=\"<\"/>", false),
    XmlValidationSample("attribute newline", "<root a=\"one\ntwo\"/>", true),
    XmlValidationSample("attribute malformed quotes", "<root a=\"one'/>", false),
    XmlValidationSample("attribute missing equals", "<root a \"one\"/>", false),
    XmlValidationSample("attribute missing value", "<root a=/>", false),
    XmlValidationSample("attribute duplicate escaped name", "<root a=\"one\" a=\"two\"/>", false),
    XmlValidationSample("attribute invalid initial digit", "<root 1bad=\"one\"/>", false),
    XmlValidationSample("tag initial digit", "<1root/>", false),
    XmlValidationSample("tag dot first", "<.root/>", false),
    XmlValidationSample("tag hyphen first", "<-root/>", false),
    XmlValidationSample("tag valid punctuation", "<_root-2.name/>", true),
    XmlValidationSample("tag invalid space", "<ro ot/>", false),
    XmlValidationSample("tag unmatched end", "</root>", false),
    XmlValidationSample("tag mismatch case", "<root></Root>", false),
    XmlValidationSample("end tag attributes", "<root></root a=\"1\">", false),
    XmlValidationSample("end tag self-closing", "<root></root/>", false),
    XmlValidationSample("tag empty name", "<>text</>", false),
    XmlValidationSample("tag whitespace after opener", "< root/>", false),
    XmlValidationSample("tag whitespace closing", "<root></root   >", true),
    XmlValidationSample("truncated attribute", "<root a=\"abc", false),
    XmlValidationSample("truncated entity", "<root>&amp", false),
    XmlValidationSample("truncated end tag", "<root></root", false),
    XmlValidationSample("truncated declaration", "<?xml version=\"1.0\"", false),
    XmlValidationSample("default namespace", "<root xmlns=\"urn:root\"><child/></root>", true),
    XmlValidationSample("empty default namespace", "<root xmlns=\"\"><child/></root>", true),
    XmlValidationSample("default namespace undeclaration", "<root xmlns=\"urn:root\"><child xmlns=\"\"><grandchild/></child></root>", true),
    XmlValidationSample("nested prefix redeclaration", "<n:root xmlns:n=\"urn:a\"><n:child xmlns:n=\"urn:b\"/><n:child/></n:root>", true),
    XmlValidationSample("namespace prefix declared later attribute", "<n:root n:id=\"1\" xmlns:n=\"urn:a\"/>", true),
    XmlValidationSample("namespace duplicate same URI", "<root xmlns:n=\"urn:a\" xmlns:n=\"urn:a\"/>", false),
    XmlValidationSample("duplicate default namespace", "<root xmlns=\"urn:a\" xmlns=\"urn:b\"/>", false),
    XmlValidationSample("namespace unrelated same URI", "<root xmlns:a=\"urn:x\" xmlns:b=\"urn:x\"><a:x/><b:x/></root>", true),
    XmlValidationSample("duplicate expanded attribute", "<root xmlns:a=\"urn:x\" xmlns:b=\"urn:x\" a:id=\"1\" b:id=\"2\"/>", false),
    XmlValidationSample("different namespace attributes", "<root xmlns:a=\"urn:a\" xmlns:b=\"urn:b\" a:id=\"1\" b:id=\"2\"/>", true),
    XmlValidationSample("default namespace does not scope attributes", "<root xmlns=\"urn:x\" xmlns:n=\"urn:x\" id=\"1\" n:id=\"2\"/>", true),
    XmlValidationSample("undeclared attribute prefix", "<root n:id=\"1\"/>", false),
    XmlValidationSample("namespace scope ends", "<root><child xmlns:n=\"urn:x\"/><n:child/></root>", false),
    XmlValidationSample("empty prefix binding XML 1.0", "<root xmlns:n=\"\"/>", false),
    XmlValidationSample("empty prefix binding XML 1.1", "<?xml version=\"1.1\"?><root xmlns:n=\"\"/>", true),
    XmlValidationSample("unbound prefix after XML 1.1 undeclaration", "<?xml version=\"1.1\"?><root xmlns:n=\"urn:x\"><child xmlns:n=\"\"><n:x/></child></root>", false),
    XmlValidationSample("xml element nested", "<root><xml:child xml:lang=\"ru\"/></root>", true),
    XmlValidationSample("xml element explicit binding", "<xml:root xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"/>", true),
    XmlValidationSample("xml prefix wrong URI", "<root xmlns:xml=\"urn:wrong\"/>", false),
    XmlValidationSample("xml prefix empty binding", "<root xmlns:xml=\"\"/>", false),
    XmlValidationSample("xml URI default namespace", "<root xmlns=\"http://www.w3.org/XML/1998/namespace\"/>", false),
    XmlValidationSample("xml URI ordinary prefix", "<root xmlns:n=\"http://www.w3.org/XML/1998/namespace\"/>", false),
    XmlValidationSample("xmlns element prefix", "<xmlns:root/>", false),
    XmlValidationSample("xmlns URI default namespace", "<root xmlns=\"http://www.w3.org/2000/xmlns/\"/>", false),
    XmlValidationSample("xmlns URI ordinary prefix", "<root xmlns:n=\"http://www.w3.org/2000/xmlns/\"/>", false),
    XmlValidationSample("xmlns prefix rebind", "<root xmlns:xmlns=\"urn:x\"/>", false),
    XmlValidationSample("namespace multi-colon declaration", "<root xmlns:a:b=\"urn:x\"/>", false),
    XmlValidationSample("namespace multi-colon element", "<a:b:c xmlns:a=\"urn:x\"/>", false),
    XmlValidationSample("namespace multi-colon attribute", "<root xmlns:a=\"urn:x\" a:b:c=\"1\"/>", false),
    XmlValidationSample("namespace empty local name", "<root xmlns:a=\"urn:x\"><a:/></root>", false),
    XmlValidationSample("namespace empty prefix name", "<:root/>", true),
    XmlValidationSample("namespace URI with amp escape", "<n:root xmlns:n=\"urn:x&amp;y\"/>", true),
    XmlValidationSample("namespace entity URI duplicate expanded attribute", "<root xmlns:a=\"urn:x&amp;y\" xmlns:b=\"urn:x&#38;y\" a:id=\"1\" b:id=\"2\"/>", false),
    XmlValidationSample("relative namespace URI", "<n:root xmlns:n=\"relative\"/>", true),
    XmlValidationSample("namespace URI with spaces", "<n:root xmlns:n=\"urn:space value\"/>", true),
    XmlValidationSample("xml arbitrary attribute", "<root xml:custom=\"value\"/>", true),
    XmlValidationSample("unprefixed xmlns element", "<xmlns/>", true),
    XmlValidationSample("XML 1.0 literal control", "<root>\u0001</root>", false),
    XmlValidationSample("XML 1.0 referenced control", "<root>&#1;</root>", false),
    XmlValidationSample("XML 1.1 literal control", "<?xml version=\"1.1\"?><root>\u0001</root>", false),
    XmlValidationSample("XML 1.1 referenced control", "<?xml version=\"1.1\"?><root>&#1;</root>", true),
    XmlValidationSample("XML 1.1 referenced DEL", "<?xml version=\"1.1\"?><root>&#x7F;</root>", true),
    XmlValidationSample("XML 1.1 literal DEL", "<?xml version=\"1.1\"?><root>\u007f</root>", false),
    XmlValidationSample("XML 1.1 next line text", "<?xml version=\"1.1\"?><root>one\u0085two</root>", true),
    XmlValidationSample("XML 1.1 line separator text", "<?xml version=\"1.1\"?><root>one\u2028two</root>", true),
    XmlValidationSample("XML 1.0 next line outside root", "\u0085<root/>", false),
    XmlValidationSample("XML 1.1 next line outside root", "<?xml version=\"1.1\"?>\u0085<root/>", true),
    XmlValidationSample("Unicode combining name", "<a\u0300/>", true),
    XmlValidationSample("Unicode combining start name", "<\u0300a/>", false),
    XmlValidationSample("Unicode supplementary name", "<\ud83e\udd80/>", false),
    XmlValidationSample("Unicode name beyond allowed range", "<\udb80\udc00/>", false),
    XmlValidationSample("DTD comment before root", "<!-- safe --><!DOCTYPE root><root/>", false),
    XmlValidationSample("doctype as text escaped", "<root>&lt;!DOCTYPE root&gt;</root>", true),
    XmlValidationSample("large attribute", "<root value=\"" + "Привет&amp;".repeat(200) + "\"/>", true),
    XmlValidationSample("many sibling elements", "<root>" + "<value n=\"1\">42</value>".repeat(500) + "</root>", true),
    XmlValidationSample("moderately deep XML", "<node>".repeat(200) + "leaf" + "</node>".repeat(200), true),
    XmlValidationSample("unpaired UTF-16 surrogate", "<root>\uD800</root>", false),
)
