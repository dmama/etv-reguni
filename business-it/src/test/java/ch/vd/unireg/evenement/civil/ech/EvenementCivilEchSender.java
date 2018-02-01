package ch.vd.uniregctb.evenement.civil.ech;

/**
 * Cette classe permet de simuler l'envoi d'événements civils au format e-CH
 */
public interface EvenementCivilEchSender {

	/**
	 * Envoi un événement civil dans l'ESB
	 * @param evt l'événement à envoyer
	 * @param businessUser le business user qui fait l'envoi
	 * @throws Exception en cas de problème
	 */
	void sendEvent(EvenementCivilEch evt, String businessUser) throws Exception;
}
