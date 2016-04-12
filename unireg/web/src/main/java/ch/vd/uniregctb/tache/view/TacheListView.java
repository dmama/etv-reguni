package ch.vd.uniregctb.tache.view;

import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Vue nécessaire pour la liste de taches
 *
 * @author xcifde
 *
 */
public class TacheListView implements Annulable {

	private Long id;

	private String typeTache;

	private Date dateEnregistrement;

	private Long numero;

	private final NomCourrierViewPart nomCourrier = new NomCourrierViewPart();

	private Integer numeroForGestion;

	private String officeImpot;

	private TypeEtatTache etatTache;

	private Long idDI;

	// Seulement valable pour le type de tache 'envoi declaration'
	private RegDate dateDebutImposition;
	private RegDate dateFinImposition;
	private Integer longueurPeriodeImposition;
	private TypeContribuable typeContribuable;
	private TypeDocument typeDocument;
	private Integer delaiRetourEnJours;
	private Integer annee;

	boolean annulee;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTypeTache() {
		return typeTache;
	}

	public void setTypeTache(String typeTache) {
		this.typeTache = typeTache;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public String getNomCourrier1() {
		return this.nomCourrier.getNomCourrier1();
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier.setNomCourrier1(nomCourrier1);
	}

	public String getNomCourrier2() {
		return this.nomCourrier.getNomCourrier2();
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier.setNomCourrier2(nomCourrier2);
	}

	public Integer getNumeroForGestion() {
		return numeroForGestion;
	}

	public void setNumeroForGestion(Integer numeroForGestion) {
		this.numeroForGestion = numeroForGestion;
	}

	public String getOfficeImpot() {
		return officeImpot;
	}

	public void setOfficeImpot(String officeImpot) {
		this.officeImpot = officeImpot;
	}

	public TypeEtatTache getEtatTache() {
		return etatTache;
	}

	public void setEtatTache(TypeEtatTache etatTache) {
		this.etatTache = etatTache;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier.setNomCourrier(nomCourrier);
	}

	public RegDate getDateDebutImposition() {
		return dateDebutImposition;
	}

	public void setDateDebutImposition(RegDate dateDebutImposition) {
		this.dateDebutImposition = dateDebutImposition;
	}

	public RegDate getDateFinImposition() {
		return dateFinImposition;
	}

	public void setDateFinImposition(RegDate dateFinImposition) {
		this.dateFinImposition = dateFinImposition;
	}

	@Override
	public boolean isAnnule() {
		return annulee;
	}

	public void setAnnule(boolean annulee) {
		this.annulee = annulee;
	}

	public Integer getLongueurPeriodeImposition() {
		return longueurPeriodeImposition;
	}

	public void setLongueurPeriodeImposition(Integer longueurPeriodeImposition) {
		this.longueurPeriodeImposition = longueurPeriodeImposition;
	}

	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable typeContribuable) {
		this.typeContribuable = typeContribuable;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public Integer getDelaiRetourEnJours() {
		return delaiRetourEnJours;
	}

	public void setDelaiRetourEnJours(Integer delaiRetourEnJours) {
		this.delaiRetourEnJours = delaiRetourEnJours;
	}

	public Integer getAnnee() {
		return annee;
	}

	public void setAnnee(Integer annee) {
		this.annee = annee;
	}

	public Long getIdDI() {
		return idDI;
	}

	public void setIdDI(Long idDI) {
		this.idDI = idDI;
	}

	public Date getDateEnregistrement() {
		return dateEnregistrement;
	}

	public void setDateEnregistrement(Date dateEnregistrement) {
		this.dateEnregistrement = dateEnregistrement;
	}
}
