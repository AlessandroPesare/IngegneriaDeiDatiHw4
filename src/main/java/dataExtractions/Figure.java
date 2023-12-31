package dataExtractions;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.StringWriter;
import java.util.*;

public class Figure {

    public NodeList extractIDs(String xmlFile) throws Exception {
        Document document = this.loadXmlDocument(xmlFile);
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expr = xpath.compile("//fig[@id]/@id");
        NodeList figuresID = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        return figuresID;
    }
    public String extractCaptions(String xmlFile, String figureId) throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        String xpathExpression = "//fig[@id='" + figureId + "']/caption";
        XPathExpression expr = xpath.compile(xpathExpression);
        NodeList captionNodes = (NodeList) expr.evaluate(this.loadXmlDocument(xmlFile), XPathConstants.NODESET);
        String extractedContent = "";
        String extractedContentNode = "";

        for(int i = 0; i<captionNodes.getLength(); i++) {
            Node node = captionNodes.item(i);
            if(node!=null){
                extractedContentNode = this.serializeNodeToString(node);
                extractedContentNode = extractedContentNode.replaceAll("<\\?xml.*\\?><caption>", "");
                extractedContentNode = extractedContentNode.replaceAll("</caption>", "");
                extractedContent += extractedContentNode;
            }
        }
        return extractedContent;
    }
    private String serializeNodeToString(Node node) {
        try {
            // Usa un Transformer per la serializzazione
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // Configura l'output per indentare la stringa XML
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            // Crea un DOMSource dal nodo
            DOMSource source = new DOMSource(node);

            // Crea uno StringWriter per la stringa di output
            StringWriter stringWriter = new StringWriter();

            // Crea un StreamResult per la stringa di output
            StreamResult result = new StreamResult(stringWriter);

            // Esegue la trasformazione
            transformer.transform(source, result);

            // Restituisci la stringa risultante
            return stringWriter.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    public List<String> extractSources(String xmlFile, String figureId, String filename) throws Exception {
        List<String> sources = new ArrayList<>();
        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        String xpathExpression = "//fig[@id='"+ figureId +"']/graphic/@*[local-name()='href']";
        XPathExpression expr = xpath.compile(xpathExpression);
        NodeList sourceNodes = (NodeList) expr.evaluate(this.loadXmlDocument(xmlFile), XPathConstants.NODESET);
        
        filename = filename.replaceAll(".xml", "");
        
        for (int i = 0; i < sourceNodes.getLength(); i++) {
        	Node node = sourceNodes.item(i);
        	String sourceUrl = "https://www.ncbi.nlm.nih.gov/pmc/articles/" + filename + "/bin/";
        	if(node != null) {
        		sourceUrl = sourceUrl + node.getTextContent() + ".jpg";
        		sources.add(sourceUrl);
        	}	
        }
        return sources;
    }
    
    public Document loadXmlDocument(String xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xmlFile);
    }
    
	public List<String> extractCaptionCitations(String xmlFile, String figureId) throws Exception {
		List<String> citations = new ArrayList<>();
		
		XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        String xpathExpression = "//fig[@id='" + figureId + "']/caption/xref[@ref-type='bibr']/@rid";
        XPathExpression expr = xpath.compile(xpathExpression);
        
        NodeList captionCitationsNodes = (NodeList) expr.evaluate(this.loadXmlDocument(xmlFile), XPathConstants.NODESET);
        
        for (int i = 0; i < captionCitationsNodes.getLength(); i++) {
        	Node node = captionCitationsNodes.item(i);
        	if (node != null) {
        		String extractedContent = this.extractBibr(node.getTextContent(), xmlFile);
        		if (!extractedContent.isEmpty())
        			citations.add(extractedContent);
        	}
        }
		
		return citations;
	}
    
    private String extractBibr(String textContent, String xmlFile) throws XPathExpressionException, Exception {
    	XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xpath = xPathFactory.newXPath();
		String xpathExpression = "//ref[@id='" + textContent + "']";
		XPathExpression expr = xpath.compile(xpathExpression);
		String extractContent = "";
		
		Node citationNode = (Node) expr.evaluate(this.loadXmlDocument(xmlFile), XPathConstants.NODE);
		if(citationNode != null) {
			extractContent = this.serializeNodeToString(citationNode);
			extractContent = extractContent.replaceAll("<\\?xml.*\\?>", "");
		}
		return extractContent;
	}

    public List<String> extractParagraphCitations(String xmlFile,Document document) throws Exception {
        Set<String> citations = new HashSet<>();
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        String xpathExpression = "//xref[@ref-type='bibr']/@rid";
        XPathExpression expr = xpath.compile(xpathExpression);

        NodeList paragraphsCitationsNodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

        for (int i = 0; i < paragraphsCitationsNodes.getLength(); i++) {
            Node node = paragraphsCitationsNodes.item(i);
            if (node != null) {
                String extractedContent = this.extractBibr(node.getTextContent(), xmlFile);
                if (!extractedContent.isEmpty())
                    citations.add(extractedContent);
            }
        }
        return new ArrayList<String>(citations);
    }
    
    
	public List<Map<String, Object>> extractParagraphs(String xmlFile, String figureId) throws Exception {
    	List<Map<String, Object>> paragraphs = new ArrayList<>();
    	
    	//Dato il figureId, estrai tutti i paragrafi -> primo step
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        String xpathExpression = "//xref[@ref-type='fig' and @rid='" + figureId + "']/..";
        XPathExpression expr = xpath.compile(xpathExpression);
        NodeList citationsNodes = (NodeList) expr.evaluate(this.loadXmlDocument(xmlFile), XPathConstants.NODESET);
        String extractedContentNode = "";

        for(int i = 0; i<citationsNodes.getLength(); i++) {
            Node node = citationsNodes.item(i);
            if(node != null) {
                Map<String,Object> contentParagraph = new LinkedHashMap<>();
                
                extractedContentNode = this.serializeNodeToString(node);
                extractedContentNode = extractedContentNode.replaceAll("<\\?xml.*\\?>", "");
                contentParagraph.put("text", extractedContentNode);
                
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document paragraphDocument = builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(extractedContentNode)));
                contentParagraph.put("citations", this.extractParagraphCitations(xmlFile,paragraphDocument));
                
                paragraphs.add(contentParagraph);
            }
        }
        
        return paragraphs;
    }


    
    
    
}
