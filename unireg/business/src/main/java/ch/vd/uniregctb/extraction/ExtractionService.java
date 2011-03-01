package ch.vd.uniregctb.extraction;

/**
 * Service asynchrone d'extraction de données (génération de gros CSV par exemple)
 */
public interface ExtractionService {

	/**
	 * Poste une demande d'exécution de l'extracteur donné pour le compte de l'utilisateur dont le visa est donné.</p>
	 * Il s'agit d'un appel asynchrone
	 * @param visa VISA de l'utilisateur pour le compte duquel cette extraction est lancée
	 * @param extractor extracteur
	 * @return clé pour récupérer le document résultant de l'extraction
	 */
	ExtractionKey postExtractionQuery(String visa, PlainExtractor extractor);

	/**
	 * Poste une demande d'exécution de l'extracteur donné pour le compte de l'utilisateur dont le visa est donné.</p>
	 * Il s'agit d'un appel asynchrone
	 * @param visa VISA de l'utilisateur pour le compte duquel cette extraction est lancée
	 * @param extractor extracteur
	 * @return clé pour récupérer le document résultant de l'extraction
	 */
	ExtractionKey postExtractionQuery(String visa, BatchableExtractor extractor);

	/**
	 * Poste une demande d'exécution de l'extracteur donné pour le compte de l'utilisateur dont le visa est donné.</p>
	 * Il s'agit d'un appel asynchrone
	 * @param visa VISA de l'utilisateur pour le compte duquel cette extraction est lancée
	 * @param extractor extracteur
	 * @return clé pour récupérer le document résultant de l'extraction
	 */
	ExtractionKey postExtractionQuery(String visa, BatchableParallelExtractor extractor);

	/**
	 * Récupère le résultat d'une extraction demandée par {@link #postExtractionQuery(String, ExtractorIntf) postExtractionQuery}
	 * @param key la clé fournie lors de la demande
	 * @return le résultat de l'extraction, <code>null</code> si l'extraction n'est pas encore terminée ou si la clé n'est plus valide
	 */
	ExtractionResult getExtractionResult(ExtractionKey key);

}
