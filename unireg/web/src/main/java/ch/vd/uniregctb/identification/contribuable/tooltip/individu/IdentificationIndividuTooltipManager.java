package ch.vd.uniregctb.identification.contribuable.tooltip.individu;

public interface IdentificationIndividuTooltipManager {

	/**
	 * Détermine et retourne le numéro d'individu à partir d'un numéro de contribuable.
	 *
	 * @param noCtb un numéro de contribuable
	 * @return le numéro d'individu du contribuable si le contribuable spécifié est un habitant; <b>null</b> dans tous les autres cas.
	 */
	Long getNumeroIndividuFromCtb(Long noCtb);
}
