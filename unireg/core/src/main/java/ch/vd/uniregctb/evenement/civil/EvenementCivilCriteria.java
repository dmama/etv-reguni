package ch.vd.uniregctb.evenement.civil;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/*
 * Classe générique qui convient pour les critères de recherche sur les
 * évenements ech ou regpp. Cette classe a été générisée lors de la creation de l'ecran de recherche
 * pour les evt ech. Si la recherche des evts ech devenait trop spécifique on aurait meilleur
 * temps de respécialiser cette classe pour les evts regpp (comme à l'origine) et recreer une nouvelle
 * classe critère pour la recherche ech.
 */

public class EvenementCivilCriteria<TYPE_EVT extends Enum<TYPE_EVT> > implements Serializable {

	private static final long serialVersionUID = -733844545319723616L;

	public enum TypeRechercheDuNom {
		CONTIENT,
		PHONETIQUE,
		EST_EXACTEMENT
	}

	/**
	 * Le type de l'evenement
	 */
	private TYPE_EVT type;

	/**
	 * L'état de l'évenement
	 */
	private EtatEvenementCivil etat;

	/**
	 * L'action de l'evenement (seulement pour les evt ech)
	 */
	private ActionEvenementCivilEch action;

	/**
	 * La date de traitement debut
	 */
	private Date dateTraitementDebut;

	/**
	 * La date de traitement fin
	 */
	private Date dateTraitementFin;

	/**
	 * La date du début de l'évenement
	 */
	protected RegDate dateEvenementDebut;

	/**
	 * La date de fin de l'évenement
	 */
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

	public TYPE_EVT getType() {
		return type;
	}

	public void setType(TYPE_EVT type) {
		this.type = type;
	}

	public EtatEvenementCivil getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementCivil etat) {
		this.etat = etat;
	}

	public ActionEvenementCivilEch getAction() {
		return action;
	}

	public void setAction(ActionEvenementCivilEch action) {
		this.action = action;
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
