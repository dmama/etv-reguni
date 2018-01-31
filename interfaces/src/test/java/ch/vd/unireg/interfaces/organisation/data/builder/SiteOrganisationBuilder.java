package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesREE;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class SiteOrganisationBuilder implements DataBuilder<SiteOrganisationRCEnt> {

	private List<DateRanged<String>> nom;

	public DonneesRC rc;
	public DonneesRegistreIDE ide;
	public DonneesREE ree;

	private final long numeroSite;
	private Map<String, List<DateRanged<String>>> identifiants;
	private List<DateRanged<String>> nomAdditionnel;
	private List<DateRanged<TypeDeSite>> typesDeSite;
	private List<DateRanged<FormeLegale>> formeLegale;
	private List<Domicile> domiciles;
	private Map<String, List<DateRanged<FonctionOrganisation>>> fonction;

	private List<DateRanged<Long>> transfereA;
	private List<DateRanged<Long>> transferDe;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	public SiteOrganisationBuilder(long numeroSite) {
		this.numeroSite = numeroSite;
	}

	@NotNull
	public SiteOrganisationRCEnt build() {
		return new SiteOrganisationRCEnt(numeroSite, identifiants, nom, nomAdditionnel, typesDeSite, formeLegale, domiciles, fonction, rc, ide, ree, null, remplacePar, enRemplacementDe, transfereA, transferDe);
	}

	public SiteOrganisationBuilder addIdentifiant(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		identifiants = BuilderHelper.addValueToMapOfList(identifiants, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addNom(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nom = BuilderHelper.addValueToList(nom, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addNomAdditionnel(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nomAdditionnel = BuilderHelper.addValueToList(nomAdditionnel, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addFormeLegale(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull FormeLegale valeur) {
		formeLegale = BuilderHelper.addValueToList(formeLegale, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addTransfereA(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		transfereA = BuilderHelper.addValueToList(transfereA, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addTransferDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		transferDe = BuilderHelper.addValueToList(transferDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addRemplacePar(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		remplacePar = BuilderHelper.addValueToList(remplacePar, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addEnRemplacementDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		enRemplacementDe = BuilderHelper.addValueToList(enRemplacementDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addTypeDeSite(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull TypeDeSite valeur) {
		typesDeSite = BuilderHelper.addValueToList(typesDeSite, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addSiege(@NotNull Domicile valeur) {
		domiciles = BuilderHelper.addValueToList(domiciles, valeur);
		return this;
	}

	public SiteOrganisationBuilder addFonction(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull FonctionOrganisation valeur) {
		fonction = BuilderHelper.addValueToMapOfList(fonction, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder withRC(@NotNull DonneesRC rc) {
		this.rc = rc;
		return this;
	}

	public SiteOrganisationBuilder withIde(@NotNull DonneesRegistreIDE ide) {
		this.ide = ide;
		return this;
	}

	public SiteOrganisationBuilder withRee(@NotNull DonneesREE ree) {
		this.ree = ree;
		return this;
	}

	public SiteOrganisationBuilder withFonctions(Map<String, List<DateRanged<FonctionOrganisation>>> fonction) {
		this.fonction = fonction;
		return this;
	}

	public SiteOrganisationBuilder withIdentifiants(Map<String, List<DateRanged<String>>> autresIdentifiants) {
		this.identifiants = autresIdentifiants;
		return this;
	}

	public SiteOrganisationBuilder withRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
		return this;
	}

	public SiteOrganisationBuilder withEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
		return this;
	}

	public SiteOrganisationBuilder withNomAdditionnel(List<DateRanged<String>> nomAdditionnel) {
		this.nomAdditionnel = nomAdditionnel;
		return this;
	}

	public SiteOrganisationBuilder withSieges(List<Domicile> domiciles) {
		this.domiciles = domiciles;
		return this;
	}

	public SiteOrganisationBuilder withTypesDeSite(List<DateRanged<TypeDeSite>> typesDeSite) {
		this.typesDeSite = typesDeSite;
		return this;
	}
}
