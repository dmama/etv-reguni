package ch.vd.unireg.interfaces.organisation.data.builder;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Autorisation;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;

public class FonctionOrganisationBuilder implements DataBuilder<FonctionOrganisation> {

	/*
		Le numéro cantonal de l'individu (RCPers)
	 */
	private Integer idCantonalIndividu;
	@NotNull
	private final String nom;
	private String prenom;

	private String lieuDeResidence;

	private String textFonction;
	private Autorisation autorisation;
	private String restrictionAutorisation;

	public FonctionOrganisationBuilder(@NotNull String nom) {
		this.nom = nom;
	}

	public FonctionOrganisation build() {
		return new FonctionOrganisation(idCantonalIndividu, nom, prenom, lieuDeResidence, textFonction, autorisation, restrictionAutorisation);
	}

	private FonctionOrganisationBuilder withRestrictionAutorisation(String restrictionAutorisation) {
		this.restrictionAutorisation = restrictionAutorisation;
		return this;
	}

	private FonctionOrganisationBuilder withAutorisation(Autorisation autorisation) {
		this.autorisation = autorisation;
		return this;
	}

	private FonctionOrganisationBuilder withTextFonction(String textFonction) {
		this.textFonction = textFonction;
		return this;
	}

	private FonctionOrganisationBuilder withLieuDeResidence(String lieuDeResidence) {
		this.lieuDeResidence = lieuDeResidence;
		return this;
	}

	private FonctionOrganisationBuilder withPrenom(String prenom) {
		this.prenom = prenom;
		return this;
	}

	private FonctionOrganisationBuilder withIdCantonalIndividu(Integer idCantonalIndividu) {
		this.idCantonalIndividu = idCantonalIndividu;
		return this;
	}
}
