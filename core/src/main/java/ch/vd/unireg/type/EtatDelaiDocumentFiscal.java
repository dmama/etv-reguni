package ch.vd.uniregctb.type;

/**
 * Les différents états possibles d'un délai de déclaration
 */
public enum EtatDelaiDocumentFiscal {
	/**
	 * Délai demandé, la décision d'octroi n'a pas encore été prise
	 */
	DEMANDE,

	/**
	 * Délai accordé
	 */
	ACCORDE,

	/**
	 * Délai refusé
	 */
	REFUSE
}
