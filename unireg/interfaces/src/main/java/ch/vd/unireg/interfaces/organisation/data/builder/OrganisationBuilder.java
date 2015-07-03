package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

/**
 * Classe de builder pour la construction facilité d'entités d'organisation. Les autres builders de
 * ce package fonctionnent selon les mêmes principes.
 *
 * Le principe est d'assembler les informations une à une, au fil de l'eau. Chaque méthode renvoie la référence
 * vers le builder, ce qui permet de chaîner les appels et de réduire la gène visuelle.
 *
 *  En dernier, l'appel à build() crée l'entité définitive en appelant son constructeur, validant ainsi les
 *  éventuelles contraintes.
 *
 * Il y a deux types de méthodes:
 * - les méthodes commençant par "add":   Ajouter des éléments un par un à une propriété collection de l'entité.
 * - les méthodes commençant par "with":  Spécifier d'un coup la valeur définitive de la propriété.
 *
 * Ainsi, on peut facilement créer des entités avec une notation naturelle et lisible.
 */
public class OrganisationBuilder implements DataBuilder<Organisation> {
	private final long cantonalId;

	private Map<String, List<DateRanged<String>>> identifiants;

	private List<DateRanged<String>> nom;
	private List<DateRanged<String>> nomsAdditionnels;
	private List<DateRanged<FormeLegale>> formeLegale;

	private List<DateRanged<Long>> sites;
	private List<SiteOrganisation> donneesSites;

	private List<DateRanged<Long>> transfereA;
	private List<DateRanged<Long>> transferDe;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	@Override
	public Organisation build() {
		return new Organisation(cantonalId, identifiants, nom, nomsAdditionnels, formeLegale, sites,
		                        donneesSites, transfereA, transferDe, remplacePar, enRemplacementDe
		);
	}

	public OrganisationBuilder(long cantonalId) {
		this.cantonalId = cantonalId;
	}

	public OrganisationBuilder(long cantonalId, @NotNull List<DateRanged<String>> nom) {
		this.cantonalId = cantonalId;
		this.nom = nom;
	}

	public OrganisationBuilder addNom(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		BuilderHelper.addValueToList(nom, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addIdentifiant(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		BuilderHelper.addValueToMapOfList(identifiants, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addNomAdditionnel(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		BuilderHelper.addValueToList(nomsAdditionnels, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addFormeLegale(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull FormeLegale valeur) {
		BuilderHelper.addValueToList(formeLegale, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addSite(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		BuilderHelper.addValueToList(sites, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addDonneesSite(@NotNull SiteOrganisation site) {
		BuilderHelper.addValueToList(donneesSites, site);
		return this;
	}

	public OrganisationBuilder addTransfereA(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		BuilderHelper.addValueToList(transfereA, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addTransferDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		BuilderHelper.addValueToList(transferDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder AddRemplacePar(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		BuilderHelper.addValueToList(remplacePar, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addEnRemplacementDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		BuilderHelper.addValueToList(enRemplacementDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder withDonneesSites(List<SiteOrganisation> donneesSites) {
		this.donneesSites = donneesSites;
		return this;
	}

	public OrganisationBuilder withEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
		return this;
	}

	public OrganisationBuilder withFormeLegale(List<DateRanged<FormeLegale>> formeLegale) {
		this.formeLegale = formeLegale;
		return this;
	}

	public OrganisationBuilder withIdentifiants(Map<String, List<DateRanged<String>>> identifiants) {
		this.identifiants = identifiants;
		return this;
	}

	public OrganisationBuilder withNomsAdditionnels(List<DateRanged<String>> nomsAdditionnels) {
		this.nomsAdditionnels = nomsAdditionnels;
		return this;
	}

	public OrganisationBuilder withRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
		return this;
	}

	public OrganisationBuilder withSites(List<DateRanged<Long>> sites) {
		this.sites = sites;
		return this;
	}

	public OrganisationBuilder withTransferDe(List<DateRanged<Long>> transferDe) {
		this.transferDe = transferDe;
		return this;
	}

	public OrganisationBuilder withTransfereA(List<DateRanged<Long>> transfereA) {
		this.transfereA = transfereA;
		return this;
	}

	/*
		Getters réservés au Mock
	 */

	protected long getCantonalId() {
		return cantonalId;
	}

	protected List<SiteOrganisation> getDonneesSites() {
		return donneesSites;
	}

	protected List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	protected List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	protected Map<String, List<DateRanged<String>>> getIdentifiants() {
		return identifiants;
	}

	protected List<DateRanged<String>> getNom() {
		return nom;
	}

	protected List<DateRanged<String>> getNomsAdditionnels() {
		return nomsAdditionnels;
	}

	protected List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	protected List<DateRanged<Long>> getSites() {
		return sites;
	}

	protected List<DateRanged<Long>> getTransferDe() {
		return transferDe;
	}

	protected List<DateRanged<Long>> getTransfereA() {
		return transfereA;
	}
}
