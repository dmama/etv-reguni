package ch.vd.uniregctb.evenement.civil;

import java.io.Serializable;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

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

	private TYPE_EVT type;
	private EtatEvenementCivil etat;
	private ActionEvenementCivilEch action; // seulement utile pour les evts ech
	private Date dateTraitementDebut;
	private Date dateTraitementFin;
	private RegDate dateEvenementDebut;
	private RegDate dateEvenementFin;
	private Long numeroIndividu;
	private Long numeroCTB;
	private TypeRechercheDuNom typeRechercheDuNom;
	private String nomCourrier;
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

	@SuppressWarnings("UnusedDeclaration")
	public void setDateTraitementDebut(Date dateTraitementDebut) {
		this.dateTraitementDebut = dateTraitementDebut;
	}

	public Date getDateTraitementFin() {
		return dateTraitementFin;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDateTraitementFin(Date dateTraitementFin) {
		this.dateTraitementFin = dateTraitementFin;
	}

	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(@Nullable Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	@SuppressWarnings("UnusedDeclaration")
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

	@SuppressWarnings("UnusedDeclaration")
	public boolean isAutresNoms() {
		return autresNoms;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setAutresNoms(boolean autresNoms) {
		this.autresNoms = autresNoms;
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(@Nullable Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public RegDate getRegDateEvenementDebut() {
		return dateEvenementDebut;
	}

	public void setRegDateEvenementDebut(RegDate dateEvenementDebut) {
		this.dateEvenementDebut = dateEvenementDebut;
	}

	public RegDate getRegDateEvenementFin() {
		return dateEvenementFin;
	}

	public void setRegDateEvenementFin(RegDate dateEvenementFin) {
		this.dateEvenementFin = dateEvenementFin;
	}

	public boolean isJoinOnPersonnePhysique() {
		// si on fait une recherche par numero de ctb alors la requete devra avoir une jointure sur Tiers
		return numeroCTB != null;
	}
}
