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
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivileEvent;

public interface ServiceEntrepriseRaw {

	/**
	 * Container des identifiants d'une entreprise et de l'un de ses établissements civils
	 * (qui peut être principal ou pas)
	 */
	class Identifiers implements Serializable {

		private static final long serialVersionUID = 348072279122408475L;

		public final long idCantonalEntreprise;
		public final long idCantonalEtablissement;

		public Identifiers(long idCantonalEntreprise, long idCantonalEtablissement) {
			this.idCantonalEntreprise = idCantonalEntreprise;
			this.idCantonalEtablissement = idCantonalEtablissement;
		}
	}

	String SERVICE_NAME = "ServiceEntreprise";

	/**
	 * Recherche tous les états d'une entreprise.
	 *
	 * @param noEntreprise Identifiant cantonal de l'entreprise
	 * @return les données retournées par RCEnt
	 * @throws ServiceEntrepriseException
	 */
	EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws ServiceEntrepriseException;

	/**
	 * Obtenir un numéro d'entreprise à partir d'un numéro d'établissement civil.
	 *
	 * @param noEtablissementCivil Identifiant cantonal de l'établissement civil.
	 * @return L'identifiant cantonal de l'entreprise détenant l'établissement civil.
	 * @throws ServiceEntrepriseException
	 */
	Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws ServiceEntrepriseException;

	/**
	 * @param noide numéro IDE (sous la forme sans point ni tiret)
	 * @return les identifiants de l'entreprise et de son établissement civil qui correspondent à ce numéro IDE
	 * @throws ServiceEntrepriseException en cas de souci quelque part
	 */
	Identifiers getEntrepriseByNoIde(String noide) throws ServiceEntrepriseException;

	/**
	 * Recherche les données de l'événement, en particulier des états avant et après pour chaque entreprise touchée.
	 *
	 * L'objet retourné contient, en plus de la pseudo histoire correspondant à chaque entreprise, les
	 * métadonnées éventuellement disponibles (RC et FOSC).
	 *
	 * @param noEvenement Identifiant de l'événement entreprise
	 * @return les données de l'événement sous forme de map indexée par no cantonal.
	 * @throws ServiceEntrepriseException
	 */
	Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws ServiceEntrepriseException;


	/**
	 * <p>
	 *     Faire contrôler la validité d'un modèle d'annonce par le registre civil avant de l'envoyer. Cette étape est obligatoire.
	 * </p>
	 *
	 * @param modele le modèle de l'annonce.
	 * @return le statut résultant avec les erreurs éventuelles ajouté par le registre civil.
	 */
	BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceEntrepriseException;

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
	Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException;

	/**
	 * Méthode qui permet de tester que le service entreprise répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws ServiceEntrepriseException en cas de non-fonctionnement du service entreprise
	 */
	void ping() throws ServiceEntrepriseException;
}
