package ch.vd.unireg.interfaces.organisation;

import java.io.Serializable;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;

public interface ServiceOrganisationRaw {

	/**
	 * Container des identifiants d'une organisation et de l'un de ses établissements civils
	 * (qui peut être principal ou pas)
	 */
	class Identifiers implements Serializable {

		private static final long serialVersionUID = 348072279122408475L;

		public final long idCantonalOrganisation;
		public final long idCantonalEtablissement;

		public Identifiers(long idCantonalOrganisation, long idCantonalEtablissement) {
			this.idCantonalOrganisation = idCantonalOrganisation;
			this.idCantonalEtablissement = idCantonalEtablissement;
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
	 * Obtenir un numéro d'organisation à partir d'un numéro d'établissement civil.
	 *
	 * @param noEtablissementCivil Identifiant cantonal de l'établissement civil.
	 * @return L'identifiant cantonal de l'organisation détenant l'établissement civil.
	 * @throws ServiceOrganisationException
	 */
	Long getNoOrganisationFromNoEtablissement(Long noEtablissementCivil) throws ServiceOrganisationException;

	/**
	 * @param noide numéro IDE (sous la forme sans point ni tiret)
	 * @return les identifiants de l'organisation et de son établissement civil qui correspondent à ce numéro IDE
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
	 * Recherche des demandes d'annonces à l'IDE.
	 *
	 * @param query          les critères de recherche des annonces
	 * @param order          l'ordre de tri demandé pour les résultats
	 * @param pageNumber     le numéro de page demandée (0-based)
	 * @param resultsPerPage le nombre d'éléments par page
	 * @return une page avec les annonces correspondantes
	 */
	@NotNull
	Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException;

	/**
	 * Méthode qui permet de tester que le service organisation répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws ServiceOrganisationException en cas de non-fonctionnement du service organisation
	 */
	void ping() throws ServiceOrganisationException;
}
