package com.prismdocx.metadata

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Единые правила чтения XML из недоверенного документа и сериализации в UTF-8. */
internal object XmlSupport {
    const val MAX_XML = 4 * 1024 * 1024

    fun newPart(part: MetadataPart): Document = builder().newDocument().apply {
        appendChild(createElementNS(part.namespace, part.root))
        documentElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:vt", VT_NS)
    }

    fun checkRoot(doc: Document, part: MetadataPart) {
        require(doc.documentElement.namespaceURI == part.namespace && doc.documentElement.localName == part.root.substringAfter(':')) {
            "Неверный корневой элемент ${part.path}."
        }
    }

    // Запрещаем DTD и внешние ресурсы: чтение свойств не должно обращаться к сети или другим файлам.
    private fun builder() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isXIncludeAware = false
        isExpandEntityReferences = false
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    }.newDocumentBuilder().apply {
        setErrorHandler(object : DefaultHandler() {
            override fun error(e: SAXParseException) { throw e }
            override fun fatalError(e: SAXParseException) { throw e }
        })
    }

    fun parse(xml: String): Document = builder().parse(InputSource(StringReader(xml)))

    fun parse(bytes: ByteArray): Document = builder().parse(ByteArrayInputStream(bytes))

    fun children(element: Element): List<Element> =
        (0 until element.childNodes.length).mapNotNull { element.childNodes.item(it) as? Element }

    fun serialize(doc: Document, indent: Boolean = false): String = StringWriter().use { buffer ->
        TransformerFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "")
        }.newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, if (indent) "yes" else "no")
            if (indent) setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }.transform(DOMSource(doc), StreamResult(buffer))
        // Декларация должна соответствовать UTF-8 при последующей записи ZIP-части.
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + buffer.toString()
    }
}
