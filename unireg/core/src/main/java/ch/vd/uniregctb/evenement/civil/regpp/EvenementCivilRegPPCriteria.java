package ch.vd.uniregctb.evenement.civil.regpp;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilRegPPCriteria implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -733844545319723617L;

	public enum TypeRechercheDuNom {
		CONTIENT,
		PHONETIQUE,
		EST_EXACTEMENT
	}

	private EtatEvenementCivil etat;

	public EtatEvenementCivil getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementCivil etat) {
		this.etat = etat;
	}

	/**
	 * Le type de l'evenement
	 */
	private TypeEvenementCivil type;

	/**
	 * La date de traitement debut
	 */
	private Date dateTraitementDebut;

	/**
	 * La date de traitement fin
	 */
	private Date dateTraitementFin;

	protected RegDate dateEvenementDebut;

	protected RegDate dateEvenementFin;

	/**
	 * Le type de recherche par nom
	 */
	private Long numeroIndividu;

	/**
	 * Le numero de CTB
	 */
	private Long numeroCTB;

	/**
	 * Le type de recherhce par nom
	 */
	private TypeRechercheDuNom typeRechercheDuNom;

	/**
	 * Le nom courrier
	 */
	private String nomCourrier;

	/**
	 * Les autres noms
	 */
	private boolean autresNoms;

	public TypeEvenementCivil getType() {
		return type;
	}

	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

	public Date getDateTraitementDebut() {
		return dateTraitementDebut;
	}

	public void setDateTraitementDebut(Date dateTraitementDebut) {
		this.dateTraitementDebut = dateTraitementDebut;
	}

	public Date getDateTraitementFin() {
		return dateTraitementFin;
	}

	public void setDateTraitementFin(Date dateTraitementFin) {
		this.dateTraitementFin = dateTraitementFin;
	}

	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	public TypeRechercheDuNom getTypeRechercheDuNom() {
		return typeRechercheDuNom;
	}

	public void setTypeRechercheDuNom(TypeRechercheDuNom typeRechercheDuNom) {
		this.typeRechercheDuNom = typeRechercheDuNom;
	}

	public String getNomCourrier() {
		return nomCourrier;
	}

	public void setNomCourrier(String nomCourrier) {
		this.nomCourrier = nomCourrier;
	}

	public boolean isAutresNoms() {
		return autresNoms;
	}

	public void setAutresNoms(boolean autresNoms) {
		this.autresNoms = autresNoms;
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public RegDate getRegDateEvenementDebut() {
		return dateEvenementDebut;
	}

	public void setDateEvenementDebut(RegDate dateEvenementDebut) {
		this.dateEvenementDebut = dateEvenementDebut;
	}

	public RegDate getRegDateEvenementFin() {
		return dateEvenementFin;
	}

	public void setDateEvenementFin(RegDate dateEvenementFin) {
		this.dateEvenementFin = dateEvenementFin;
	}

	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		return type == null
				&& (numeroCTB == null)
				&& (nomCourrier == null || "".equals(nomCourrier));
	}

}
