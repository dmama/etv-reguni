package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.Sexe;

/**
 * Contient les critères de recherche sur le contribuable lui-même dans le traitement d'une requête d'identification d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Embeddable
public class CriteresPersonne {

	/**
	 * Le NAVS13 est optionnel. Celui-ci doit être compris entre 7560000000001 et 7569999999999.
	 */
	private String NAVS13;

	/**
	 * Le NAVS11 est optionnel. Celui-ci doit être compris entre 10000000000 et 99999999999.
	 */
	private String NAVS11;

	/**
	 * C’est le nom officiel de la personne. Il est obligatoire.
	 */
	private String nom;

	/**
	 * C’est la liste des prénoms de la personne. Cette donnée est obligatoire.
	 */
	private String prenoms;

	/**
	 * Cette donnée est optionnelle.
	 */
	private Sexe sexe;

	/**
	 * La date de naissance est obligatoire mais peut être partiellement connue.
	 */
	private RegDate dateNaissance;

	/**
	 * L'adresse de la personne.
	 */
	private CriteresAdresse adresse;

	@Column(name = "NAVS13", length = 13)
	public String getNAVS13() {
		return NAVS13;
	}

	public void setNAVS13(String navs13) {
		NAVS13 = navs13;
	}

	@Column(name = "NAVS11", length = 11)
	public String getNAVS11() {
		return NAVS11;
	}

	public void setNAVS11(String navs11) {
		NAVS11 = navs11;
	}

	@Column(name = "NOM", length = 100)
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "PRENOMS", length = 100)
	public String getPrenoms() {
		return prenoms;
	}

	public void setPrenoms(String prenoms) {
		this.prenoms = prenoms;
	}

	@Column(name = "SEXE", length = LengthConstants.TIERS_SEXE)
	@Type(type = "ch.vd.uniregctb.hibernate.SexeUserType")
	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	@Column(name = "DATE_NAISSANCE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType", parameters = {
		@Parameter(name = "allowPartial", value = "true")
	})
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	@Embedded
	public CriteresAdresse getAdresse() {
		return adresse;
	}

	public void setAdresse(CriteresAdresse adresse) {
		this.adresse = adresse;
	}
}
