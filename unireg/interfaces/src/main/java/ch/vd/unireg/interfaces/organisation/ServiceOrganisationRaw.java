package ch.vd.unireg.interfaces.organisation;

import java.io.Serializable;
import java.util.Map;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;

public interface ServiceOrganisationRaw {

	/**
	 * Container des identifiants d'une organisation et de l'un de ses sites
	 * (qui peut être principal ou pas)
	 */
	class Identifiers implements Serializable {

		private static final long serialVersionUID = 348072279122408475L;

		public final long idCantonalOrganisation;
		public final long idCantonalSite;

		public Identifiers(long idCantonalOrganisation, long idCantonalSite) {
			this.idCantonalOrganisation = idCantonalOrganisation;
			this.idCantonalSite = idCantonalSite;
		}
	}

	String SERVICE_NAME = "ServiceOrganisation";

	/**
	 * Recherche tous les états d'une organisation.
	 *
	 * @param noOrganisation Identifiant cantonal de l'organisation
	 * @return les données retournées par RCEnt
	 * @throws ServiceOrganisationException
	 */
	Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException;

	/**
	 * Obtenir un numéro d'organisation à partir d'un numéro de site.
	 *
	 * @param noSite Identifiant cantonal du site.
	 * @return L'identifiant cantonal de l'organisation détenant le site.
	 * @throws ServiceOrganisationException
	 */
	Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException;

	/**
	 * @param noide numéro IDE (sous la forme sans point ni tiret)
	 * @return les identifiants de l'organisation et de son site qui correspondent à ce numéro IDE
	 * @throws ServiceOrganisationException en cas de souci quelque part
	 */
	Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException;

	/**
	 * Recherche les données de l'événement, en particulier des états avant et après pour chaque organisation touchée.
	 *
	 * L'objet retourné contient, en plus de la pseudo histoire correspondant à chaque organisation, les
	 * métadonnées éventuellement disponibles (RC et FOSC).
	 *
	 * @param noEvenement Identifiant de l'événement organisation
	 * @return les données de l'événement sous forme de map indexée par no cantonal.
	 * @throws ServiceOrganisationException
	 */
	Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException;


	/**
	 * <p>
	 *     Faire contrôler la validité d'un modèle d'annonce par le registre civil avant de l'envoyer. Cette étape est obligatoire.
	 * </p>
	 *
	 * @param modele le modèle de l'annonce.
	 * @return le statut résultant avec les erreurs éventuelles ajouté par le registre civil.
	 */
	BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceOrganisationException;

	/**
	 * Obtenir le contenu et le statut d'une annonce à l'IDE.
	 *
	 * Il faut garder à l'esprit que RCEnt peut ne pas connaître une annonce qui lui a été adressé via l'esb, de par
	 * le caractère asynchrone de ce genre d'envoi.
	 *
	 * @param numero le numéro de l'annonce recherchée
	 * @return l'annonce IDE, ou null si RCEnt ne connait pas d'annonce pour ce numéro.
	 * @throws ServiceOrganisationException en cas de problème d'accès ou de cohérence des données retournées.
	 */
	AnnonceIDEEnvoyee getAnnonceIDE(long numero) throws ServiceOrganisationException;

	/**
	 * Méthode qui permet de tester que le service organisation répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws ServiceOrganisationException en cas de non-fonctionnement du service organisation
	 */
	void ping() throws ServiceOrganisationException;
}
