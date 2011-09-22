package ch.vd.uniregctb.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeDocument;

public class InformationsDocumentAdapter {

	public final Tiers tiers;
	public final Integer idDocument;
	public final int annee;
	public final RegDate delaiRetourImprime;
	public final RegDate delaiAccorde;
	public final RegDate dateReference;
	public final int noOfsCommune;
	public final Long collId;
	public final Qualification qualification;
	public final Integer codeSegment;
	public final String modifUser;
	public final TypeDocument typeDocument;

	public InformationsDocumentAdapter(Tiers tiers, Integer idDocument, int annee, RegDate delaiRetourImprime, RegDate delaiAccorde, RegDate dateReference, int noOfsCommune, Long collId,
	                                   Qualification qualification, Integer codeSegment, String modifUser, TypeDocument typeDocument) {
		this.tiers = tiers;
		this.idDocument = idDocument;
		this.annee = annee;
		this.delaiRetourImprime = delaiRetourImprime;
		this.delaiAccorde = delaiAccorde;
		this.dateReference = dateReference;
		this.noOfsCommune = noOfsCommune;
		this.collId = collId;
		this.qualification = qualification;
		this.codeSegment = codeSegment;
		this.modifUser = modifUser;
		this.typeDocument = typeDocument;

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
		codeSegment = declaration.getCodeSegment();
		modifUser = declaration.getLogModifUser();
	}

	public Tiers getTiers() {
		return tiers;
	}

	public Integer getIdDocument() {
		return idDocument;
	}

	public int getAnnee() {
		return annee;
	}

	public RegDate getDelaiRetourImprime() {
		return delaiRetourImprime;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public RegDate getDateReference() {
		return dateReference;
	}

	public int getNoOfsCommune() {
		return noOfsCommune;
	}

	public Long getCollId() {
		return collId;
	}

	public Qualification getQualification() {
		return qualification;
	}

	public Integer getCodeSegment() {
		return codeSegment;
	}

	public String getModifUser() {
		return modifUser;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}
}
