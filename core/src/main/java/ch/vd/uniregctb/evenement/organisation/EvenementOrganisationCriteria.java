package ch.vd.uniregctb.evenement.organisation;

import java.io.Serializable;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/*
 * Classe générique qui convient pour les critères de recherche sur les évenements organisation.
 */

public class EvenementOrganisationCriteria<TYPE_EVT extends Enum<TYPE_EVT> > implements Serializable {

	private static final long serialVersionUID = 428357327439825312L;

	public enum TypeRechercheDuNom {
		CONTIENT,
		PHONETIQUE,
		EST_EXACTEMENT
	}

	private TYPE_EVT type;
	private EtatEvenementOrganisation etat;
	private FormeJuridiqueEntreprise formeJuridique;
	private Date dateTraitementDebut;
	private Date dateTraitementFin;
	private RegDate dateEvenementDebut;
	private RegDate dateEvenementFin;
	private Long numeroOrganisation;
	private Long numeroCTB;
	private TypeRechercheDuNom typeRechercheDuNom;

	public TYPE_EVT getType() {
		return type;
	}

	public void setType(TYPE_EVT type) {
		this.type = type;
	}

	public EtatEvenementOrganisation getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementOrganisation etat) {
		this.etat = etat;
	}


	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
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

	public Long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	public void setNumeroOrganisation(@Nullable Long numeroOrganisation) {
		this.numeroOrganisation = numeroOrganisation;
	}

	@SuppressWarnings("UnusedDeclaration")
	public TypeRechercheDuNom getTypeRechercheDuNom() {
		return typeRechercheDuNom;
	}

	public void setTypeRechercheDuNom(TypeRechercheDuNom typeRechercheDuNom) {
		this.typeRechercheDuNom = typeRechercheDuNom;
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

	public boolean isJoinOnEntreprise() {
		// si on fait une recherche par numero de ctb alors la requete devra avoir une jointure sur Tiers
		return numeroCTB != null;
	}
}
