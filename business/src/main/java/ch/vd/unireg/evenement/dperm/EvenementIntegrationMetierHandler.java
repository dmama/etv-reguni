package ch.vd.uniregctb.evenement.dperm;

import javax.xml.transform.Source;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

/**
 * Interface implémentée par les handlers spécifiques
 */
public interface EvenementIntegrationMetierHandler {

	/**
	 * Traitement du message
	 * @param xmlInterne xml interne (= encapsulé dedans) à l'événement d'intégration métier
	 * @param metaDonnees les méta-données du message entrant
	 * @return le document à renvoyer dans la réponse (ou <code>null</code> s'il ne faut pas faire de réponse)
	 */
	@Nullable
	Document handleMessage(Source xmlInterne, Map<String, String> metaDonnees) throws Exception;

}
