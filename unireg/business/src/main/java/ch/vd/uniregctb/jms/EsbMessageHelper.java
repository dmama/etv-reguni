package ch.vd.uniregctb.jms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.technical.esb.EsbMessage;

public abstract class EsbMessageHelper {

	/**
	 * @param message dont on veut extraire les headers customs
	 * @return une map des headers customs contenus dans le message fourni
	 */
	public static Map<String, String> extractCustomHeaders(EsbMessage message) {
		final Set<String> names = message.getCustomHeadersNames();
		final Map<String, String> map = new HashMap<String, String>(names.size());
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
	 * Renvoie une map dans laquelle seules les clés présentes dans le tableau 'toKeep' sont reprises de la map 'src'
	 * @param src map de laquelle les valeurs seront extraites
	 * @param toKeep tableau des clés à reprendre de la map source
	 * @return map reprenant le contenu de la map 'src' pour les clés fournies dans 'toKeep'
	 */
	public static Map<String, String> filterHeaders(Map<String, String> src, String[] toKeep) {
		if (src == null || src.size() == 0) {
			return src;
		}
		else if (toKeep == null || toKeep.length == 0) {
			return Collections.emptyMap();
		}
		else {
			final Map<String, String> dest = new HashMap<String, String>(Math.min(toKeep.length, src.size()));
			for (String key : toKeep) {
				if (src.containsKey(key)) {
					dest.put(key, src.get(key));
				}
			}
			return dest;
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
}
