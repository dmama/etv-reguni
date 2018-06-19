package ch.vd.unireg.evenement.ide;

import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public interface AnnonceIDESender {

	/**
	 * @param annonce l'annonce à expédier
	 * @param msgBusinessId la businessId à assigner au message ESB
	 * @throws AnnonceIDEException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	void sendEvent(AnnonceIDEEnvoyee annonce, String msgBusinessId) throws AnnonceIDEException;
}
