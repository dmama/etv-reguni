package ch.vd.uniregctb.editique;

/**
 * Interface pour les résultat effectivement issu d'une réception d'événement
 * (ce qui exclut donc toute forme de timeout)
 */
public interface EditiqueResultatRecu extends EditiqueResultat {

	/**
	 * @return le timestamp de la réception de l'événement (en millisecondes, calculé avec {@link ch.vd.uniregctb.common.TimeHelper#getPreciseCurrentTimeMillis()})
	 */
	long getTimestampReceived();
}
