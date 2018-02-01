package ch.vd.unireg.evenement.retourdi.pm;

import java.util.Map;

import ch.vd.unireg.jms.EsbBusinessException;

/**
 * Service de prise en compte des données extraites d'une déclaration déposée par un contribuable PM
 */
public interface RetourDIPMService {

	/**
	 * Traitement des informations contenues dans la déclaration d'impôt déposée
	 * @param retour les informations en question
	 * @param incomingHeaders les méta-données autour du message entrant
	 * @throws EsbBusinessException en cas de souci métier
	 */
	void traiterRetour(RetourDI retour, Map<String, String> incomingHeaders) throws EsbBusinessException;
}
