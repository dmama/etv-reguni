package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

public class OrganisationRCEnt implements Organisation, Serializable {

	private static final long serialVersionUID = -1369195090559377725L;

	/**
	 * Le numéro technique de l'organisation pour Unireg
	 */
	private final long numeroOrganisation;

	private final List<DateRanged<String>> numeroIDE;

	@NotNull
	private final List<DateRanged<String>> nom;
	private final Map<String, List<DateRanged<String>>> nomsAdditionnels;
	private final List<DateRanged<FormeLegale>> formeLegale;

	@NotNull
	private final Map<Long, SiteOrganisation> donneesSites;

	private final Map<Long, List<DateRanged<Long>>> transfereA;
	private final Map<Long, List<DateRanged<Long>>> transferDe;
	private final List<DateRanged<Long>> remplacePar;
	private final Map<Long, List<DateRanged<Long>>> enRemplacementDe;

	public OrganisationRCEnt(long numeroOrganisation,
	                         @NotNull Map<String, List<DateRanged<String>>> identifiants,
	                         @NotNull List<DateRanged<String>> nom,
	                         Map<String, List<DateRanged<String>>> nomsAdditionnels,
	                         List<DateRanged<FormeLegale>> formeLegale,
	                         @NotNull Map<Long, SiteOrganisation> donneesSites,
	                         Map<Long, List<DateRanged<Long>>> transfereA, Map<Long, List<DateRanged<Long>>> transferDe,
	                         List<DateRanged<Long>> remplacePar, Map<Long, List<DateRanged<Long>>> enRemplacementDe) {
		this.numeroOrganisation = numeroOrganisation;
		this.numeroIDE = OrganisationHelper.extractIdentifiant(identifiants, OrganisationConstants.CLE_IDE);
		this.nom = nom;
		this.nomsAdditionnels = nomsAdditionnels;
		this.formeLegale = formeLegale;
		this.donneesSites = donneesSites;
		this.transfereA = transfereA;
		this.transferDe = transferDe;
		this.remplacePar = remplacePar;
		this.enRemplacementDe = enRemplacementDe;
	}

	@Override
	public long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return numeroIDE;
	}

	/**
	 * Prepare une liste de plages représantant la succession des sièges des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le siege qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages sièges correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de siege.
	 */
	@Override
	public List<Siege> getSiegesPrincipaux() {
		return OrganisationHelper.getSiegesPrincipaux(donneesSites);
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return Le siège, ou null si aucun siège valide à la date donnée
	 */
	@Override
	public Siege getSiegePrincipal(RegDate date) {
		return OrganisationHelper.getSiegePrincipal(this, date);
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return La forme legale, ou null si absente
	 */
	@Override
	public FormeLegale getFormeLegale(RegDate date) {
		return OrganisationHelper.getFormeLegale(this, date);
	}

	/**
	 * Retourne une liste représantant la succession des valeurs de capital de l'entreprise.
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche la plage de capital qui lui est contemporaine.
	 *
	 * On recrée l'information du capital dans une nouvelle plage aux limites de la plage type principale qui a permis
	 * de la trouver.
	 *
	 * @return La succession de plage contenant l'information de capital.
	 */
	@Override
	public List<Capital> getCapitaux() {
		return OrganisationHelper.getCapitaux(donneesSites);
	}

	@Override
	public Capital getCapital(RegDate date) {
		return OrganisationHelper.getCapital(this, date);
	}

	@Override
	@NotNull
	public List<SiteOrganisation> getDonneesSites() {
		return new ArrayList<>(donneesSites.values());
	}

	@Override
	public Map<Long, List<DateRanged<Long>>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	@Override
	public List<Long> getEnRemplacementDe(RegDate date) {
		final RegDate theDate= date != null ? date : RegDate.get();

		if (enRemplacementDe != null) {
			return OrganisationHelper.valuesForDate(enRemplacementDe, theDate);
		}
		return null;
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	@Override
	@NotNull
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	@Override
	public String getNom(RegDate date) {
		return OrganisationHelper.getNom(this, date);
	}

	@Override
	public Map<String, List<DateRanged<String>>> getNomsAdditionnels() {
		return nomsAdditionnels;
	}

	@Override
	public List<String> getNomsAdditionnels(RegDate date) {
		final RegDate theDate= date != null ? date : RegDate.get();

		if (nomsAdditionnels != null) {
			return OrganisationHelper.valuesForDate(nomsAdditionnels, theDate);
		}
		return null;
	}

	@Override
	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	@Override
	public Long getRemplacePar(RegDate date) {
		return date != null ? DateRangeHelper.rangeAt(remplacePar, date).getPayload() : DateRangeHelper.rangeAt(remplacePar, RegDate.get()).getPayload();
	}

	@Override
	public Map<Long, List<DateRanged<Long>>> getTransferDe() {
		return transferDe;
	}

	@Override
	public Map<Long, List<DateRanged<Long>>> getTransfereA() {
		return transfereA;
	}

	@Override
	public List<Adresse> getAdresses() {
		return OrganisationHelper.getAdresses(donneesSites);
	}

	// TODO: A générer dans l'adapter?
	/**
	 * Liste des sites principaux
	 * @return La liste des sites principaux
	 */
	@Override
	public List<DateRanged<SiteOrganisation>> getSitePrincipaux() {
		return OrganisationHelper.getSitePrincipaux(this);
	}

	// TODO: A générer dans l'adapter?
	/**
	 * Le site principal à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date
	 * @return Le site princpial
	 */
	@Override
	public DateRanged<SiteOrganisation> getSitePrincipal(RegDate date) {
		return OrganisationHelper.getSitePrincipal(this, date);
	}

	/**
	 * Liste des sites secondaire pour une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date pour laquelle on désire la liste des sites secondaires
	 * @return La liste des sites secondaire
	 */
	@Override
	public List<SiteOrganisation> getSitesSecondaires(RegDate date) {
		return OrganisationHelper.getSitesSecondaires(this, date);
	}
}
