package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.Fonction;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class SiteOrganisationBuilder implements DataBuilder<SiteOrganisation> {

	private final long cantonalId;
	private List<DateRanged<String>> nom;

	public DonneesRC rc;
	public DonneesRegistreIDE ide;

	private Map<String,List<DateRanged<String>>> identifiants;
	private List<DateRanged<String>> nomsAdditionnels;
	private List<DateRanged<TypeDeSite>> typeDeSite;
	/**
	 * municipalityId du SwissMunicipality
	 */
	private List<DateRanged<Integer>> siege;
	private List<DateRanged<Fonction>> fonction;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	public SiteOrganisationBuilder(long cantonalId, @NotNull List<DateRanged<String>> nom) {
		this.cantonalId = cantonalId;
		this.nom = nom;
	}

	public SiteOrganisationBuilder(long cantonalId) {
		this.cantonalId = cantonalId;
	}

	@Override
	public SiteOrganisation build() {
		return new SiteOrganisation(cantonalId, nom, rc, ide, identifiants, nomsAdditionnels, typeDeSite,
		                            siege, fonction, remplacePar, enRemplacementDe);
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
		nomsAdditionnels = BuilderHelper.addValueToList(nomsAdditionnels, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addTypeDeSite(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull TypeDeSite valeur) {
		typeDeSite = BuilderHelper.addValueToList(typeDeSite, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addSiege(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Integer valeur) {
		siege =BuilderHelper.addValueToList(siege, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addFonction(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Fonction valeur) {
		fonction = BuilderHelper.addValueToList(fonction, new DateRanged<>(dateDebut, dateDeFin, valeur));
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

	public SiteOrganisationBuilder withRC(@NotNull DonneesRC rc) {
		this.rc = rc;
		return this;
	}

	public SiteOrganisationBuilder withIde(@NotNull DonneesRegistreIDE ide) {
		this.ide = ide;
		return this;
	}

	public SiteOrganisationBuilder setEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
		return this;
	}

	public SiteOrganisationBuilder setFonction(List<DateRanged<Fonction>> fonction) {
		this.fonction = fonction;
		return this;
	}

	public SiteOrganisationBuilder setIdentifiants(Map<String, List<DateRanged<String>>> identifiants) {
		this.identifiants = identifiants;
		return this;
	}

	public SiteOrganisationBuilder setNomsAdditionnels(List<DateRanged<String>> nomsAdditionnels) {
		this.nomsAdditionnels = nomsAdditionnels;
		return this;
	}

	public SiteOrganisationBuilder setRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
		return this;
	}

	public SiteOrganisationBuilder setSiege(List<DateRanged<Integer>> siege) {
		this.siege = siege;
		return this;
	}

	public SiteOrganisationBuilder setTypeDeSite(List<DateRanged<TypeDeSite>> typeDeSite) {
		this.typeDeSite = typeDeSite;
		return this;
	}
}
