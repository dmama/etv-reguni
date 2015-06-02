package ch.vd.uniregctb.extraction;

/**
 * Résultat fourni par l'exécuteur de l'extraction
 */
public abstract class ExtractionResult {

	public static enum State {
		NOT_RUN,
		OK,
		ERROR
	}

	/**
	 * Première vision globale : erreur ou pas
	 */
	public abstract State getSummary();
}
