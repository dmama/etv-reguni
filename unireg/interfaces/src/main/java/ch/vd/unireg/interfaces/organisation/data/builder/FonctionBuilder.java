package ch.vd.unireg.interfaces.organisation.data.builder;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Adresse;
import ch.vd.unireg.interfaces.organisation.data.Autorisation;
import ch.vd.unireg.interfaces.organisation.data.Fonction;
import ch.vd.unireg.interfaces.organisation.data.LieuDeResidence;
import ch.vd.unireg.interfaces.organisation.data.Partie;
import ch.vd.unireg.interfaces.organisation.data.Personne;
import ch.vd.uniregctb.type.Sexe;

public class FonctionBuilder implements DataBuilder<Fonction> {

	/*
	  Partie
	 */
	private Integer cantonalIdPersonne;

	@NotNull
	private final String nom;
	private Personne.SourceDonnees sourceDesDonnees;
	private Long noAvs;
	private String prenom;
	private Sexe sexe;
	/**
	 * Date de naissance. Peut être partielle:
	 *   AAAA-MM-JJ, par exemple 1999-05-01 ;
	 *   AAAA-MM, par exemple 1999-05 ;
	 *   AAAA, par exemple 1999.
	 */
	private RegDate dateDeNaissance;


	private Adresse adresse;

	/*
	  Lieu de résidence
	 */
	@NotNull
	private final String nomLieuDeResidence;

	/**
	 * municipalityId, si disponible
	 */
	private Integer commune;

	/**
	 * countryId, si disponible
	 */
	private Integer pays;

	/*
	  Fonction
	 */
	private String texteFonction;
	private Autorisation autorisation;
	private String restrictionAutorisation;

	public FonctionBuilder(@NotNull String nom, @NotNull String nomLieuDeResidence) {
		this.nom = nom;
		this.nomLieuDeResidence = nomLieuDeResidence;
	}

	@Override
	public Fonction build() {
		LieuDeResidence residence = new LieuDeResidence(nomLieuDeResidence, commune, pays);
		Personne personne = new Personne(dateDeNaissance, noAvs, nom, prenom, sexe, sourceDesDonnees);
		Partie partie = new Partie(cantonalIdPersonne, personne, adresse, residence);
		return new Fonction(autorisation, partie, texteFonction, restrictionAutorisation);
	}

	public FonctionBuilder withAdresse(Adresse adresse) {
		this.adresse = adresse;
		return this;
	}

	public FonctionBuilder withAutorisation(Autorisation autorisation) {
		this.autorisation = autorisation;
		return this;
	}

	public FonctionBuilder withCantonalIdPersonne(Integer cantonalIdPersonne) {
		this.cantonalIdPersonne = cantonalIdPersonne;
		return this;
	}

	public FonctionBuilder withCommune(Integer commune) {
		this.commune = commune;
		return this;
	}

	public FonctionBuilder withDateDeNaissance(RegDate dateDeNaissance) {
		this.dateDeNaissance = dateDeNaissance;
		return this;
	}

	public FonctionBuilder withNoAvs(Long noAvs) {
		this.noAvs = noAvs;
		return this;
	}

	public FonctionBuilder withPays(Integer pays) {
		this.pays = pays;
		return this;
	}

	public FonctionBuilder withPrenom(String prenom) {
		this.prenom = prenom;
		return this;
	}

	public FonctionBuilder withRestrictionAutorisation(String restrictionAutorisation) {
		this.restrictionAutorisation = restrictionAutorisation;
		return this;
	}

	public FonctionBuilder withSexe(Sexe sexe) {
		this.sexe = sexe;
		return this;
	}

	public FonctionBuilder withSourceDesDonnees(Personne.SourceDonnees sourceDesDonnees) {
		this.sourceDesDonnees = sourceDesDonnees;
		return this;
	}

	public FonctionBuilder withTexteFonction(String texteFonction) {
		this.texteFonction = texteFonction;
		return this;
	}
}
