package ch.vd.unireg.interfaces.entreprise.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.DonneesRC;
import ch.vd.unireg.interfaces.entreprise.data.DonneesREE;
import ch.vd.unireg.interfaces.entreprise.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivilRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;

public class EtablissementBuilder implements DataBuilder<EtablissementCivilRCEnt> {

	private List<DateRanged<String>> nom;

	public DonneesRC rc;
	public DonneesRegistreIDE ide;
	public DonneesREE ree;

	private final long numeroSite;
	private Map<String, List<DateRanged<String>>> identifiants;
	private List<DateRanged<String>> nomAdditionnel;
	private List<DateRanged<TypeEtablissementCivil>> typesDeSite;
	private List<DateRanged<FormeLegale>> formeLegale;
	private List<Domicile> domiciles;
	private Map<String, List<DateRanged<FonctionOrganisation>>> fonction;

	private List<DateRanged<Long>> transfereA;
	private List<DateRanged<Long>> transferDe;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	public EtablissementBuilder(long numeroSite) {
		this.numeroSite = numeroSite;
	}

	@NotNull
	public EtablissementCivilRCEnt build() {
		return new EtablissementCivilRCEnt(numeroSite, identifiants, nom, nomAdditionnel, typesDeSite, formeLegale, domiciles, fonction, rc, ide, ree, null, remplacePar, enRemplacementDe, transfereA, transferDe);
	}

	public EtablissementBuilder addIdentifiant(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		identifiants = BuilderHelper.addValueToMapOfList(identifiants, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addNom(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nom = BuilderHelper.addValueToList(nom, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addNomAdditionnel(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nomAdditionnel = BuilderHelper.addValueToList(nomAdditionnel, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addFormeLegale(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull FormeLegale valeur) {
		formeLegale = BuilderHelper.addValueToList(formeLegale, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addTransfereA(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		transfereA = BuilderHelper.addValueToList(transfereA, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addTransferDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		transferDe = BuilderHelper.addValueToList(transferDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addRemplacePar(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		remplacePar = BuilderHelper.addValueToList(remplacePar, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addEnRemplacementDe(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		enRemplacementDe = BuilderHelper.addValueToList(enRemplacementDe, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addTypeDeSite(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull TypeEtablissementCivil valeur) {
		typesDeSite = BuilderHelper.addValueToList(typesDeSite, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder addSiege(@NotNull Domicile valeur) {
		domiciles = BuilderHelper.addValueToList(domiciles, valeur);
		return this;
	}

	public EtablissementBuilder addFonction(@NotNull String cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull FonctionOrganisation valeur) {
		fonction = BuilderHelper.addValueToMapOfList(fonction, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public EtablissementBuilder withRC(@NotNull DonneesRC rc) {
		this.rc = rc;
		return this;
	}

	public EtablissementBuilder withIde(@NotNull DonneesRegistreIDE ide) {
		this.ide = ide;
		return this;
	}

	public EtablissementBuilder withRee(@NotNull DonneesREE ree) {
		this.ree = ree;
		return this;
	}

	public EtablissementBuilder withFonctions(Map<String, List<DateRanged<FonctionOrganisation>>> fonction) {
		this.fonction = fonction;
		return this;
	}

	public EtablissementBuilder withIdentifiants(Map<String, List<DateRanged<String>>> autresIdentifiants) {
		this.identifiants = autresIdentifiants;
		return this;
	}

	public EtablissementBuilder withRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
		return this;
	}

	public EtablissementBuilder withEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
		return this;
	}

	public EtablissementBuilder withNomAdditionnel(List<DateRanged<String>> nomAdditionnel) {
		this.nomAdditionnel = nomAdditionnel;
		return this;
	}

	public EtablissementBuilder withSieges(List<Domicile> domiciles) {
		this.domiciles = domiciles;
		return this;
	}

	public EtablissementBuilder withTypesDeSite(List<DateRanged<TypeEtablissementCivil>> typesDeSite) {
		this.typesDeSite = typesDeSite;
		return this;
	}
}
