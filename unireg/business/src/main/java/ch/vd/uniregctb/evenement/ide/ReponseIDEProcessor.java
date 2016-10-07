package ch.vd.uniregctb.evenement.ide;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;

/**
 * Classe chargé du traitement métier des réponses reçues à la suite de l'émission d'annonce à l'IDE.
 *
 * Ces réponses proviennent soit de RCEnt (une erreur est survenue avant l'envoi à l'IDE), soit du registre IDE suite à la reception et/ou au traitement.
 *
 * @author Raphaël Marmier, 2016-10-06, <raphael.marmier@vd.ch>
 */
public interface ReponseIDEProcessor {

	/**
	 * Traiter l'annonce à l'IDE reçue en réponse à l'émission par Unireg d'une annonce IDE. (RCent nous renvoie notre annonce avec les informations de status et les erreurs)
	 * @param annonceIDE l'annonce à l'IDE reçue, avec l'information de statut et d'erreur
	 */
	void traiterReponseAnnonceIDE(AnnonceIDEEnvoyee annonceIDE) throws ReponseIDEProcessorException;
}
