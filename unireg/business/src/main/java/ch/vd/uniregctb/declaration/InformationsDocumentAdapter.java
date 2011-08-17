package ch.vd.uniregctb.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeDocument;

public class InformationsDocumentAdapter {

	private Tiers tiers;
	private Integer idDocument;
	private Integer annee;
	private RegDate delaiRetourImprime;
	private RegDate delaiAccorde;
	private RegDate dateReference;
	private int noOfsCommune;
	private Long collId;
	private Qualification qualification;
	private String modifUser;
	private TypeDocument typeDocument;
	private boolean isDeclarationImpotOrdinaire;
	private int nbAnnexes;


	public InformationsDocumentAdapter() {
	}

	public InformationsDocumentAdapter(Tiers tiers, Integer idDocument, Integer annee, RegDate delaiRetourImprime, RegDate delaiAccorde, RegDate dateReference, int noOfsCommune, Long collId,
	                                   Qualification qualification, String modifUser, TypeDocument typeDocument,int nbAnnexes, boolean isDeclarationImpotOrdinaire) {
		this.tiers = tiers;
		this.idDocument = idDocument;
		this.annee = annee;
		this.delaiRetourImprime = delaiRetourImprime;
		this.delaiAccorde = delaiAccorde;
		this.dateReference = dateReference;
		this.noOfsCommune = noOfsCommune;
		this.collId = collId;
		this.qualification = qualification;
		this.modifUser = modifUser;
		this.typeDocument = typeDocument;
		this.isDeclarationImpotOrdinaire = isDeclarationImpotOrdinaire;
		this.nbAnnexes = nbAnnexes;

	}

	public InformationsDocumentAdapter(DeclarationImpotOrdinaire declaration) {
		this.typeDocument = declaration.getTypeDeclaration();
		tiers = declaration.getTiers();
		idDocument = declaration.getNumero();
		annee = declaration.getPeriode().getAnnee();
		delaiRetourImprime = declaration.getDelaiRetourImprime();
		delaiAccorde = declaration.getDelaiAccordeAu();
		dateReference = declaration.getDateFin();
		noOfsCommune = declaration.getNumeroOfsForGestion();
		collId = declaration.getRetourCollectiviteAdministrativeId();
		qualification = declaration.getQualification();
		modifUser = declaration.getLogModifUser();
		if (declaration instanceof DeclarationImpotOrdinaire) {
			isDeclarationImpotOrdinaire = true;
		}
		nbAnnexes =0 ;
	}


	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	public Integer getIdDocument() {
		return idDocument;
	}

	public void setIdDocument(Integer idDocument) {
		this.idDocument = idDocument;
	}

	public Integer getAnnee() {
		return annee;
	}

	public void setAnnee(Integer annee) {
		this.annee = annee;
	}

	public RegDate getDelaiRetourImprime() {
		return delaiRetourImprime;
	}

	public void setDelaiRetourImprime(RegDate delaiRetourImprime) {
		this.delaiRetourImprime = delaiRetourImprime;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}

	public RegDate getDateReference() {
		return dateReference;
	}

	public void setDateReference(RegDate dateReference) {
		this.dateReference = dateReference;
	}

	public int getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(int noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}

	public Long getCollId() {
		return collId;
	}

	public void setCollId(Long collId) {
		this.collId = collId;
	}

	public Qualification getQualification() {
		return qualification;
	}

	public void setQualification(Qualification qualification) {
		this.qualification = qualification;
	}

	public String getModifUser() {
		return modifUser;
	}

	public void setModifUser(String modifUser) {
		this.modifUser = modifUser;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public boolean isDeclarationImpotOrdinaire() {
		return isDeclarationImpotOrdinaire;
	}

	public void setDeclarationImpotOrdinaire(boolean declarationImpotOrdinaire) {
		isDeclarationImpotOrdinaire = declarationImpotOrdinaire;
	}

	public int getNbAnnexes() {
		return nbAnnexes;
	}

	public void setNbAnnexes(int nbAnnexes) {
		this.nbAnnexes = nbAnnexes;
	}
}
