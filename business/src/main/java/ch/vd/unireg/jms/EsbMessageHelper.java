package ch.vd.uniregctb.jms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ch.vd.technical.esb.EsbMessage;

public abstract class EsbMessageHelper {

	/**
	 * @param message dont on veut extraire les headers customs
	 * @return une map des headers customs contenus dans le message fourni
	 */
	@NotNull
	public static Map<String, String> extractCustomHeaders(EsbMessage message) {
		final Set<String> names = message.getCustomHeadersNames();
		final Map<String, String> map = new HashMap<>(names.size());
		for (String name : names) {
			map.put(name, message.getHeader(name));
		}
		return map;
	}

	/**
	 * Assigne au message fourni les headers présents dans la map
	 * @param message le message de destination des headers
	 * @param headers les headers à assigner
	 * @param overwrite <code>true</code> si tous les headers fournis doivent être assignés, ou <code>false</code> si seulement ceux qui n'ont pas déjà une valeur doivent l'être
	 * @throws Exception si le message refuse l'ajout d'un des headers fournis
	 */
	public static void setHeaders(EsbMessage message, Map<String, String> headers, boolean overwrite) throws Exception {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			final String name = entry.getKey();
			if (overwrite || message.getHeader(name) == null) {
				message.addHeader(name, headers.get(name));
			}
		}
	}

	/**
	 * Extrait de la map fournie (et <i>a priori</i> construite à l'aide de la méthode {@link #extractCustomHeaders(ch.vd.technical.esb.EsbMessage) extractCustomHeaders})
	 * la valeur du champ "processDefinitionId"
	 * @param headers map des headers à fouiller
	 * @return la valeur du champ (<code>null</code> si aucune valeur n'est présente)
	 */
	@Nullable
	public static String getProcessDefinitionId(Map<String, String> headers) {
		return headers.get(EsbMessage.PROCESS_DEFINITION_ID);
	}

	/**
	 * Extrait de la map fournie (et <i>a priori</i> construite à l'aide de la méthode {@link #extractCustomHeaders(ch.vd.technical.esb.EsbMessage) extractCustomHeaders})
	 * la valeur du champ "processInstanceId"
	 * @param headers map des headers à fouiller
	 * @return la valeur du champ (<code>null</code> si aucune valeur n'est présente)
	 */
	@Nullable
	public static String getProcessInstanceId(Map<String, String> headers) {
		return headers.get(EsbMessage.PROCESS_INSTANCE_ID);
	}

	private static final String ERROR_VALUE = "???";

	/**
	 * Essaie d'extraire le <i>namespace</i> du <i>root element</i> du message passé en paramètre. S'il n'existe pas, une chaîne vide est retournée.
	 * @param msg message ESB dont on veut connaître le <i>namespace</i>
	 * @param logger logger sur lequel sera envoyé (en WARN) un éventuel problème à l'extraction du <i>namespace</i>
	 * @return l'URI du <i>namespace</i> extrait, une chaîne vide en absence de <i>namespace</i> et "???" en cas d'erreur à l'extraction
	 */
	public static String extractNamespaceURI(EsbMessage msg, Logger logger) {
		try {
			return extractNamespaceURI(msg.getBodyAsDocument(), msg.getBusinessId(), logger);
		}
		catch (Exception e) {
			logger.warn(String.format("Exception lors de l'extraction du namespace du message '%s'", msg.getBusinessId()), e);
			return ERROR_VALUE;
		}
	}

	/**
	 * Essaie d'extraire le <i>namespace</i> du <i>root element</i> du document passé en paramètre. S'il n'existe pas, une chaîne vide est retournée.
	 * @param doc DOM du message ESB dont on veut connaître le <i>namespace</i>
	 * @param logger logger sur lequel sera envoyé (en WARN) un éventuel problème à l'extraction du <i>namespace</i>
	 * @return l'URI du <i>namespace</i> extrait, une chaîne vide en absence de <i>namespace</i> et "???" en cas d'erreur à l'extraction
	 */
	public static String extractNamespaceURI(Document doc, String businessId, Logger logger) {
		if (doc == null) {
			return StringUtils.EMPTY;
		}

		try {
			return StringUtils.trimToEmpty(doc.getDocumentElement().getNamespaceURI());
		}
		catch (Exception e) {
			logger.warn(String.format("Exception lors de l'extraction du namespace du message '%s'", businessId), e);
			return ERROR_VALUE;
		}
	}

	private static final char SEPARATOR = ':';
	private static final String XMLNS_PREFIX = "xmlns" + SEPARATOR;
	private static final String SCHEMA_INSTANCE_NS = "http://www.w3.org/2001/XMLSchema-instance";
	private static final String TYPE_SUFFIX = SEPARATOR + "type";

	/**
	 * Essaie d'extraire le type de l'élément racine d'après l'attribut xsi:type, si xsi représente bien http://www.w3.org/2001/XMLSchema-instance
	 * @param doc DOM dont on veut connaître le type de l'élément racine
	 * @param logger logger sur lequel sera envoyé (en WARN) un éventuel problème à l'extraction de la donnée>
	 * @return type de l'élément racine si l'information xsi:type est disponible, <code>null</code> si cette information est indisponible et "???" en cas de problème
	 */
	public static String extractRootElementType(Document doc, String businessId, Logger logger) {
		if (doc == null) {
			return null;
		}

		try {
			// parsing des attributs de l'élément racine
			final Element rootElement = doc.getDocumentElement();
			final NamedNodeMap attributeMap = rootElement.getAttributes();
			final int nbAttributes = attributeMap.getLength();
			final Map<String, String> attributes = new LinkedHashMap<>(nbAttributes);
			for (int i = 0 ; i < nbAttributes ; ++ i) {
				final Node node = attributeMap.item(i);
				final String fullName = node.getNodeName();
				final String key = fullName.startsWith(XMLNS_PREFIX) ? node.getLocalName() : node.getNodeName();
				attributes.put(key, node.getNodeValue());
			}

			// quel est le namespace de XMLSchema-instance ?
			for (Map.Entry<String, String> entry : attributes.entrySet()) {
				if (SCHEMA_INSTANCE_NS.equals(entry.getValue())) {
					// namespace possible -> y a-t-il un attribut "type" qui utilise ce namespace ?
					final String typeFullName = attributes.get(entry.getKey() + TYPE_SUFFIX);
					if (typeFullName != null) {
						// voilà le type qui nous intéresse, reste maintenant à le formatter correctement
						// (résolution de namespace)
						final int indexSemiColon = typeFullName.indexOf(SEPARATOR);
						if (indexSemiColon >= 0) {
							final String typeNs = typeFullName.substring(0, indexSemiColon);
							final String fullNs = attributes.get(typeNs);
							if (fullNs == null) {
								return typeFullName;
							}
							else {
								final String localTypeName = typeFullName.substring(indexSemiColon);
								return fullNs + localTypeName;
							}
						}
					}
				}
			}
			return null;
		}
		catch (Exception e) {
			logger.warn(String.format("Exception lors de l'extraction du type de l'élément racine du message '%s'", businessId), e);
			return ERROR_VALUE;
		}
	}

	/**
	 * Essaie d'extraire le type de l'élément racine d'après l'attribut xsi:type, si xsi représente bien http://www.w3.org/2001/XMLSchema-instance
	 * @param msg message ESB dont on veut connaître le type de l'élément racine
	 * @param logger logger sur lequel sera envoyé (en WARN) un éventuel problème à l'extraction de la donnée>
	 * @return type de l'élément racine si l'information xsi:type est disponible, <code>null</code> si cette information est indisponible et "???" en cas de problème
	 */
	public static String extractRootElementType(EsbMessage msg, Logger logger) {
		try {
			return extractRootElementType(msg.getBodyAsDocument(), msg.getBusinessId(), logger);
		}
		catch (Exception e) {
			logger.warn(String.format("Exception lors de l'extraction du type de l'élément racine du message '%s'", msg.getBusinessId()), e);
			return ERROR_VALUE;
		}
	}

	/**
	 * Nettoie les attributs xmlns:* inutilisés de l'élément racine du document
	 * @param rootElement element racine du document qui nous intéresse
	 */
	public static void cleanupDocumentNamespaceDefinitions(Element rootElement) {
		// petit blindage innocent...
		if (rootElement != null) {
			final Map<String, String> unused = new HashMap<>();
			final Set<String> xsiAliases = new HashSet<>();

			// gather all declarations (and find out which alias the XSI has here)
			final NamedNodeMap attributes = rootElement.getAttributes();
			for (int i = 0 ; i < attributes.getLength() ; ++ i) {
				final Node node = attributes.item(i);
				final String nodeName = node.getNodeName();
				final String prefix = extractPrefix(nodeName);
				if ("xmlns".equals(prefix)) {
					unused.put(node.getLocalName(), nodeName);
					if (SCHEMA_INSTANCE_NS.equals(node.getNodeValue())) {
						xsiAliases.add(node.getLocalName());
					}
				}
			}

			// remove the used named from the "unused" map according to recursive usage inspection
			removeUsedNamespaces(rootElement, xsiAliases, unused);

			// the remaining elements ought to be removed from document
			for (String declaredAndNotUsed : unused.values()) {
				rootElement.removeAttribute(declaredAndNotUsed);
			}
		}
	}

	/**
	 * Méthode récursive qui passe en revue le noeud et ses enfants en enlevant de la map les éléments correspondants à un namespace utilisé
	 * @param node noeud de base pour le passage en revue
	 * @param unused une map dont les clés correspondent à des alias de namespace
	 */
	private static void removeUsedNamespaces(Node node, Set<String> xsiAliases, Map<String, String> unused) {

		// the node itself
		final String nodeName = node.getNodeName();
		final String prefix = extractPrefix(nodeName);
		if (prefix != null) {
			unused.remove(prefix);
		}

		// the node's attributes
		final NamedNodeMap attributes = node.getAttributes();
		if (attributes != null && attributes.getLength() > 0) {
			for (int i = 0 ; i < attributes.getLength() ; ++ i) {
				final Node attribute = attributes.item(i);
				final String attributeName = attribute.getNodeName();
				final String attributeNamePrefix = extractPrefix(attributeName);
				if (attributeNamePrefix != null) {
					unused.remove(attributeNamePrefix);
					if (xsiAliases.contains(attributeNamePrefix) && attributeName.endsWith(TYPE_SUFFIX)) {
						final String attributeValue = attribute.getNodeValue();
						final String typePrefix = extractPrefix(attributeValue);     // il s'agit d'un type, qui possède potentiellement un préfixe
						if (typePrefix != null) {
							unused.remove(typePrefix);
						}
					}
				}
			}
		}

		// the node's children (recursively)
		final NodeList childNodes = node.getChildNodes();
		for (int i = 0 ; i < childNodes.getLength() ; ++ i) {
			final Node childNode = childNodes.item(i);
			removeUsedNamespaces(childNode, xsiAliases, unused);
		}
	}

	/**
	 * @param nodeName nom (complet) d'un noeud
	 * @return le préfixe utilisé, ou <code>null</code> si aucun préfixe n'est présent
	 */
	private static String extractPrefix(String nodeName) {
		final int semicolonPosition = nodeName.indexOf(SEPARATOR);
		if (semicolonPosition > 0) {
			return nodeName.substring(0, semicolonPosition);
		}
		else {
			return null;
		}
	}
}
