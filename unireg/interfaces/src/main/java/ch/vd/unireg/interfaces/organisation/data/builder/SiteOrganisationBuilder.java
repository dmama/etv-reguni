package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class SiteOrganisationBuilder implements DataBuilder<SiteOrganisationRCEnt> {

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
	private List<DateRanged<FonctionOrganisation>> fonction;

	@NotNull
	public SiteOrganisationRCEnt build() {
		return new SiteOrganisationRCEnt(nom, rc, ide, identifiants, nomsAdditionnels, typeDeSite, siege, fonction);
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
		siege = BuilderHelper.addValueToList(siege, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addFonction(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull FonctionOrganisation valeur) {
		fonction = BuilderHelper.addValueToList(fonction, new DateRanged<>(dateDebut, dateDeFin, valeur));
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

	public SiteOrganisationBuilder setFonction(List<DateRanged<FonctionOrganisation>> fonction) {
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

	public SiteOrganisationBuilder setSiege(List<DateRanged<Integer>> siege) {
		this.siege = siege;
		return this;
	}

	public SiteOrganisationBuilder setTypeDeSite(List<DateRanged<TypeDeSite>> typeDeSite) {
		this.typeDeSite = typeDeSite;
		return this;
	}
}
