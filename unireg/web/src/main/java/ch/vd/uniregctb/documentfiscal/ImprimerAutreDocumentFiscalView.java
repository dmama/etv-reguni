package ch.vd.uniregctb.documentfiscal;

import ch.vd.registre.base.date.RegDate;

public class ImprimerAutreDocumentFiscalView {

	private long noEntreprise;
	private TypeAutreDocumentFiscalEmettableManuellement typeDocument;
	private RegDate dateReference;
	private Integer periodeFiscale;

	public ImprimerAutreDocumentFiscalView(long noEntreprise, TypeAutreDocumentFiscalEmettableManuellement typeDocument) {
		this.noEntreprise = noEntreprise;
		this.typeDocument = typeDocument;
	}

	public ImprimerAutreDocumentFiscalView() {
	}

	public long getNoEntreprise() {
		return noEntreprise;
	}

	public void setNoEntreprise(long noEntreprise) {
		this.noEntreprise = noEntreprise;
	}

	public TypeAutreDocumentFiscalEmettableManuellement getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeAutreDocumentFiscalEmettableManuellement typeDocument) {
		this.typeDocument = typeDocument;
	}

	public RegDate getDateReference() {
		return dateReference;
	}

	public void setDateReference(RegDate dateReference) {
		this.dateReference = dateReference;
	}

	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}
}
