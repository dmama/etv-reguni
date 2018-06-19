package ch.vd.unireg.interfaces.service;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseRaw;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;

public interface ServiceEntreprise {

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
	 * @param noide numéro IDE (sous la forme sans point ni tiret)
	 * @return les identifiants de l'entreprise et de son établissement civil qui correspondent à ce numéro IDE
	 * @throws ServiceEntrepriseException en cas de souci quelque part
	 */
	ServiceEntrepriseRaw.Identifiers getEntrepriseIdsByNoIde(String noide) throws ServiceEntrepriseException;

	/**
	 * Obtenir un numéro d'entreprise à partir d'un numéro d'établissement civil.
	 *
	 * @param noEtablissement Identifiant cantonal de l'établissement civil.
	 * @return L'identifiant cantonal de l'entreprise détenant l'établissement civil.
	 * @throws ServiceEntrepriseException
	 */
	Long getNoEntrepriseCivileFromNoEtablissementCivil(Long noEtablissement) throws ServiceEntrepriseException;

	/**
	 * @param noEntrepriseCivile l'identifiant cantonal d'une entreprise
	 * @return l'historique des adresses de cette entreprise
	 * @throws ServiceEntrepriseException en cas de souci
	 */
	AdressesCivilesHisto getAdressesEntrepriseHisto(long noEntrepriseCivile) throws ServiceEntrepriseException;

	/**
	 * @param noEtablissementCivil l'identifiant cantonal d'un établissement civil
	 * @return l'historique des adresses de cet établissement civil; ou <b>null</b> si l'établissement civil n'existe pas.
	 * @throws ServiceEntrepriseException en cas de souci
	 */
	@Nullable
	AdressesCivilesHisto getAdressesEtablissementCivilHisto(long noEtablissementCivil) throws ServiceEntrepriseException;

	/**
	 * Obtenir le contenu et le statut d'une annonce à l'IDE. findAnnoncesIDE() est utilisé en arrière plan avec le paramètre userId=<userId>.
	 * En effet, dans RCEnt il peut y avoir plusieurs annonces pour un même numéro émises par des utilisateur différents.
	 * <p>
	 * Attention: RCEnt ne connait pas nécessairement une annonce qu'on lui a envoyé, du fait du caractère asynchrone de la
	 * transmition par l'esb.
	 * </p>
	 *
	 * @param numero le numéro de l'annonce recherchée
	 * @param userId l'identifiant IAM de l'utilisateur, ou <code>null</code> pour une annonce émise par Unireg.
	 * @return l'annonce à l'IDE, ou null si RCEnt ne connait pas d'annonce pour ce numéro.
	 * @throws ServiceEntrepriseException en cas de problème d'accès ou de cohérence des données retournées.
	 */
	@Nullable
	AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) throws ServiceEntrepriseException;

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
	 * Demander la validation d'une annonce à l'IDE par le registre civil avant l'envoi.
	 * @param annonceIDE l'annonce candidate
	 * @return le statut résultant contenant les éventuelles erreurs rapportées par le service civil.
	 */
	BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) throws ServiceEntrepriseException;

	@NotNull
	String createEntrepriseDescription(EntrepriseCivile entrepriseCivile, RegDate date);

	String afficheAttributsEtablissement(@Nullable EtablissementCivil etablissement, @Nullable RegDate date);
}
