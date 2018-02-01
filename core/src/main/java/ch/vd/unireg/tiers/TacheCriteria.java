package ch.vd.unireg.tiers;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

/**
 * Critères permettant de filtrer une requête au DAO des tâches. Un critère <b>null</b> signifie 'pas de filtre'.
 */
public class TacheCriteria {

	private TypeTache typeTache;
	private boolean invertTypeTache;
	private TypeEtatTache etatTache;
	private Date dateCreationDepuis;
	private Date dateCreationJusqua;
	private Integer annee;
	private RegDate dateEcheanceJusqua;
	private Contribuable contribuable;
	private Long numeroCTB;
	private Integer oid;
	private Integer[] oidUser;
	private boolean inclureTachesAnnulees;
	private DeclarationImpotOrdinaire declarationAnnulee;
	private String commentaire;

	public TypeTache getTypeTache() {
		return typeTache;
	}

	public void setTypeTache(TypeTache typeTache) {
		this.typeTache = typeTache;
	}

	public boolean isInvertTypeTache() {
		return invertTypeTache;
	}

	public void setInvertTypeTache(boolean invertTypeTache) {
		this.invertTypeTache = invertTypeTache;
	}

	public TypeEtatTache getEtatTache() {
		return etatTache;
	}

	public void setEtatTache(TypeEtatTache etatTache) {
		this.etatTache = etatTache;
	}

	public Date getDateCreationDepuis() {
		return dateCreationDepuis;
	}

	public void setDateCreationDepuis(Date dateCreationDepuis) {
		this.dateCreationDepuis = dateCreationDepuis;
	}

	public Date getDateCreationJusqua() {
		return dateCreationJusqua;
	}

	public void setDateCreationJusqua(Date dateCreationJusqua) {
		this.dateCreationJusqua = dateCreationJusqua;
	}

	public Integer getAnnee() {
		return annee;
	}

	/**
	 * Filtre les tâches d'envoi et d'annulation des déclarations d'impôts en ne retournant que celles ayant une période de validité comprise entre le 1er janvier et le 31 décembre de l'année spécifiée.
	 */
	public void setAnnee(Integer annee) {
		this.annee = annee;
	}

	public RegDate getDateEcheanceJusqua() {
		return dateEcheanceJusqua;
	}

	public void setDateEcheanceJusqua(RegDate dateEcheanceJusqua) {
		this.dateEcheanceJusqua = dateEcheanceJusqua;
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public Integer getOid() {
		return oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	public Integer[] getOidUser() {
		return oidUser;
	}

	public void setOidUser(Integer[] oidUser) {
		this.oidUser = oidUser;
	}

	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable contribuable) {
		this.contribuable = contribuable;
	}

	public boolean isInclureTachesAnnulees() {
		return inclureTachesAnnulees;
	}

	public void setInclureTachesAnnulees(boolean inclureTachesAnnulees) {
		this.inclureTachesAnnulees = inclureTachesAnnulees;
	}

	public DeclarationImpotOrdinaire getDeclarationAnnulee() {
		return declarationAnnulee;
	}

	public void setDeclarationAnnulee(DeclarationImpotOrdinaire declarationAnnulee) {
		this.declarationAnnulee = declarationAnnulee;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}
}
