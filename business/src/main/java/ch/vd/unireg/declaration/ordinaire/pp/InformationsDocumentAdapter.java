package ch.vd.unireg.declaration.ordinaire.pp;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.Qualification;
import ch.vd.unireg.type.TypeDocument;

public class InformationsDocumentAdapter {

	public final ContribuableImpositionPersonnesPhysiques tiers;
	public final Integer idDocument;
	public final int annee;
	public final RegDate delaiRetourImprime;
	public final RegDate delaiAccorde;
	public final RegDate dateReference;
	public final int noOfsCommune;
	public final Long collId;
	public final Qualification qualification;
	public final Integer codeSegment;
	public final TypeDocument typeDocument;
	public final String codeControle;

	public InformationsDocumentAdapter(ContribuableImpositionPersonnesPhysiques tiers, Integer idDocument, int annee, RegDate delaiRetourImprime, RegDate delaiAccorde, RegDate dateReference, int noOfsCommune, Long collId,
	                                   Qualification qualification, Integer codeSegment, TypeDocument typeDocument, String codeControle) {
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
		this.typeDocument = typeDocument;
		this.codeControle = codeControle;

	}

	public InformationsDocumentAdapter(DeclarationImpotOrdinairePP declaration, @Nullable TypeDocument typeDocumentOverride) {
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
		typeDocument = typeDocumentOverride != null ? typeDocumentOverride : declaration.getTypeDeclaration();
		codeControle = declaration.getCodeControle();
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

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public String getCodeControle() {
		return codeControle;
	}
}
