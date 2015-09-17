package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisationRCEnt;

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
 * Il y a deux types de méthodes:1
 * - les méthodes commençant par "add":   Ajouter des éléments un par un à une propriété collection de l'entité.
 * - les méthodes commençant par "with":  Spécifier d'un coup la valeur définitive de la propriété.
 *
 * Ainsi, on peut facilement créer des entités avec une notation naturelle et lisible.
 */
public class OrganisationBuilder implements DataBuilder<Organisation> {

	private Map<String, List<DateRanged<String>>> identifiants;

	private List<DateRanged<String>> nom;
	private List<DateRanged<String>> nomsAdditionnels;
	private List<DateRanged<FormeLegale>> formeLegale;

	private Map<Long, SiteOrganisation> donneesSites;

	private List<DateRanged<Long>> transfereA;
	private List<DateRanged<Long>> transferDe;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	@Override
	public OrganisationRCEnt build() {
		return new OrganisationRCEnt(identifiants, nom, nomsAdditionnels, formeLegale,
		                             donneesSites, transfereA, transferDe, remplacePar, enRemplacementDe
		);
	}

	public OrganisationBuilder addNom(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nom = BuilderHelper.addValueToList(nom, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addIdentifiant(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		identifiants = BuilderHelper.addValueToMapOfList(identifiants, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addNomAdditionnel(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nomsAdditionnels = BuilderHelper.addValueToList(nomsAdditionnels, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addFormeLegale(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull FormeLegale valeur) {
		formeLegale = BuilderHelper.addValueToList(formeLegale, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addDonneesSite(@NotNull SiteOrganisationRCEnt site) {
		donneesSites = BuilderHelper.addValueToMap(donneesSites, site.getNumeroSite(), site);
		return this;
	}

	public OrganisationBuilder addTransfereA(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		transfereA = BuilderHelper.addValueToList(transfereA, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addTransferDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		transferDe = BuilderHelper.addValueToList(transferDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addRemplacePar(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		remplacePar = BuilderHelper.addValueToList(remplacePar, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addEnRemplacementDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		enRemplacementDe = BuilderHelper.addValueToList(enRemplacementDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder withDonneesSites(Map<Long, SiteOrganisation> donneesSites) {
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

	public OrganisationBuilder withTransferDe(List<DateRanged<Long>> transferDe) {
		this.transferDe = transferDe;
		return this;
	}

	public OrganisationBuilder withTransfereA(List<DateRanged<Long>> transfereA) {
		this.transfereA = transfereA;
		return this;
	}
}
