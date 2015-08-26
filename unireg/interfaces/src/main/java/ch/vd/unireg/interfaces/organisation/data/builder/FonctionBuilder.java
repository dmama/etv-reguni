package ch.vd.unireg.interfaces.organisation.data.builder;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Autorisation;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;

public class FonctionBuilder implements DataBuilder<FonctionOrganisation> {

	/*
		Le numéro cantonal de l'individu (RCPers)
	 */
	private Integer numeroIndividu;
	@NotNull
	private final String nom;
	private String prenom;

	private String lieuDeResidence;

	private String textFonction;
	private Autorisation autorisation;
	private String restrictionAutorisation;

	public FonctionBuilder(@NotNull String nom) {
		this.nom = nom;
	}

	public FonctionOrganisation build() {
		return new FonctionOrganisation(numeroIndividu, nom, prenom, lieuDeResidence, textFonction, autorisation, restrictionAutorisation);
	}

	private FonctionBuilder withRestrictionAutorisation(String restrictionAutorisation) {
		this.restrictionAutorisation = restrictionAutorisation;
		return this;
	}

	private FonctionBuilder withAutorisation(Autorisation autorisation) {
		this.autorisation = autorisation;
		return this;
	}

	private FonctionBuilder withTextFonction(String textFonction) {
		this.textFonction = textFonction;
		return this;
	}

	private FonctionBuilder withLieuDeResidence(String lieuDeResidence) {
		this.lieuDeResidence = lieuDeResidence;
		return this;
	}

	private FonctionBuilder withPrenom(String prenom) {
		this.prenom = prenom;
		return this;
	}

	private FonctionBuilder withNumeroIndividu(Integer numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
		return this;
	}
}
