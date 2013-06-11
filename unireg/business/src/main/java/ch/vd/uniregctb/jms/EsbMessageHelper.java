package ch.vd.uniregctb.jms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.technical.esb.EsbMessage;

public abstract class EsbMessageHelper {

	/**
	 * @param message dont on veut extraire les headers customs
	 * @return une map des headers customs contenus dans le message fourni
	 */
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

	/**
	 * Essaie d'extraire le <i>namespace</i> du <i>root element</i> du message passé en paramètre. S'il n'existe pas, une chaîne vide est retournée.
	 * @param msg message ESB dont on veut connaître le <i>namespace</i>
	 * @param logger logger sur lequel sera envoyé (en WARN) un éventuel problème à l'extraction du <i>namespace</i>
	 * @return l'URI du <i>namespace</i> extrait, une chaîne vide en absence de <i>namespace</i> et "???" en cas d'erreur à l'extraction
	 */
	public static String extractNamespaceURI(EsbMessage msg, Logger logger) {
		try {
			return StringUtils.trimToEmpty(msg.getBodyAsDocument().getDocumentElement().getNamespaceURI());
		}
		catch (Exception e) {
			logger.warn(String.format("Exception lors de l'extraction du namespace du message '%s'", msg.getBusinessId()), e);
			return "???";
		}
	}
}
