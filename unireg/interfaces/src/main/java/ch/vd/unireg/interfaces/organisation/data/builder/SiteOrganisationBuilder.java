package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class SiteOrganisationBuilder implements DataBuilder<SiteOrganisationRCEnt> {

	private List<DateRanged<String>> nom;

	public DonneesRC rc;
	public DonneesRegistreIDE ide;

	private final long numeroSite;
	private Map<String, List<DateRanged<String>>> autresIdentifiants;
	private Map<String, List<DateRanged<String>>> nomsAdditionnels;
	private List<DateRanged<TypeDeSite>> typesDeSite;
	private List<Siege> sieges;
	private Map<String, List<DateRanged<FonctionOrganisation>>> fonction;

	public SiteOrganisationBuilder(long numeroSite) {
		this.numeroSite = numeroSite;
	}

	@NotNull
	public SiteOrganisationRCEnt build() {
		return new SiteOrganisationRCEnt(numeroSite, autresIdentifiants, nom, rc, ide, nomsAdditionnels, typesDeSite, sieges, fonction);
	}

	public SiteOrganisationBuilder addAutreIdentifiant(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		autresIdentifiants = BuilderHelper.addValueToMapOfList(autresIdentifiants, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addNom(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nom = BuilderHelper.addValueToList(nom, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addNomAdditionnel(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nomsAdditionnels = BuilderHelper.addValueToMapOfList(nomsAdditionnels, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addTypeDeSite(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull TypeDeSite valeur) {
		typesDeSite = BuilderHelper.addValueToList(typesDeSite, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public SiteOrganisationBuilder addSiege(@NotNull Siege valeur) {
		sieges = BuilderHelper.addValueToList(sieges, valeur);
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

	public SiteOrganisationBuilder withFonctions(Map<String, List<DateRanged<FonctionOrganisation>>> fonction) {
		this.fonction = fonction;
		return this;
	}

	public SiteOrganisationBuilder withAutresIdentifiants(Map<String, List<DateRanged<String>>> autresIdentifiants) {
		this.autresIdentifiants = autresIdentifiants;
		return this;
	}

	public SiteOrganisationBuilder withNomsAdditionnels(Map<String, List<DateRanged<String>>> nomsAdditionnels) {
		this.nomsAdditionnels = nomsAdditionnels;
		return this;
	}

	public SiteOrganisationBuilder withSieges(List<Siege> sieges) {
		this.sieges = sieges;
		return this;
	}

	public SiteOrganisationBuilder withTypesDeSite(List<DateRanged<TypeDeSite>> typesDeSite) {
		this.typesDeSite = typesDeSite;
		return this;
	}
}
