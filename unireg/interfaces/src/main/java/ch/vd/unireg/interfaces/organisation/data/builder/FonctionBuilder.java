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

	public void setAdresse(Adresse adresse) {
		this.adresse = adresse;
	}

	public void setAutorisation(Autorisation autorisation) {
		this.autorisation = autorisation;
	}

	public void setCantonalIdPersonne(Integer cantonalIdPersonne) {
		this.cantonalIdPersonne = cantonalIdPersonne;
	}

	public void setCommune(Integer commune) {
		this.commune = commune;
	}

	public void setDateDeNaissance(RegDate dateDeNaissance) {
		this.dateDeNaissance = dateDeNaissance;
	}

	public void setNoAvs(Long noAvs) {
		this.noAvs = noAvs;
	}

	public void setPays(Integer pays) {
		this.pays = pays;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public void setRestrictionAutorisation(String restrictionAutorisation) {
		this.restrictionAutorisation = restrictionAutorisation;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	public void setSourceDesDonnees(Personne.SourceDonnees sourceDesDonnees) {
		this.sourceDesDonnees = sourceDesDonnees;
	}

	public void setTexteFonction(String texteFonction) {
		this.texteFonction = texteFonction;
	}
}
