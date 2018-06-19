package ch.vd.unireg.evenement.organisation;

import java.io.Serializable;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

/*
 * Classe générique qui convient pour les critères de recherche sur les évenements entreprise.
 */

public class EvenementEntrepriseCriteria<TYPE_EVT extends Enum<TYPE_EVT> > implements Serializable {

	private static final long serialVersionUID = 428357327439825312L;

	public enum TypeRechercheDuNom {
		CONTIENT,
		PHONETIQUE,
		EST_EXACTEMENT
	}

	private TYPE_EVT type;
	private EtatEvenementEntreprise etat;
	private FormeJuridiqueEntreprise formeJuridique;
	private Date dateTraitementDebut;
	private Date dateTraitementFin;
	private RegDate dateEvenementDebut;
	private RegDate dateEvenementFin;
	private Long numeroEntrepriseCivile;
	private Long numeroCTB;
	private TypeRechercheDuNom typeRechercheDuNom;

	public TYPE_EVT getType() {
		return type;
	}

	public void setType(TYPE_EVT type) {
		this.type = type;
	}

	public EtatEvenementEntreprise getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementEntreprise etat) {
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

	public Long getNumeroEntrepriseCivile() {
		return numeroEntrepriseCivile;
	}

	public void setNumeroEntrepriseCivile(@Nullable Long numeroEntrepriseCivile) {
		this.numeroEntrepriseCivile = numeroEntrepriseCivile;
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
