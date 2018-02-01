package ch.vd.uniregctb.registrefoncier.dataimport.helper;

/**
 * Contient les identifiants des éléments à ne pas imnporter du registe foncier pour une raison ou une autre.
 */
public interface BlacklistRFHelper {
	/**
	 * @param idRF l'idRF d'un immeuble
	 * @return <b>vrai</b> si l'immeuble est blacklisté et doit être ignoré lors de l'import; <b>faux</b> autrement.
	 */
	boolean isBlacklisted(String idRF);
}
