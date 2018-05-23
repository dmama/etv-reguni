package ch.vd.unireg.documentfiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeLettreBienvenue;

public class ImprimerAutreDocumentFiscalView {

	private long noEntreprise;
	private TypeAutreDocumentFiscalEmettableManuellement typeDocument;
	private RegDate dateReference;
	private Integer periodeFiscale;
	// pour la lettre de bienvenue :
	private RegDate delaiRetour;
	private TypeLettreBienvenue typeLettreBienvenue;
	private String page;

	public ImprimerAutreDocumentFiscalView(long noEntreprise, TypeAutreDocumentFiscalEmettableManuellement typeDocument, String page) {
		this.noEntreprise   = noEntreprise;
		this.typeDocument   = typeDocument;
		this.page           = page;
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

	public RegDate getDelaiRetour() {
		return delaiRetour;
	}

	public void setDelaiRetour(RegDate delaiRetour) {
		this.delaiRetour = delaiRetour;
	}

	public TypeLettreBienvenue getTypeLettreBienvenue() {
		return typeLettreBienvenue;
	}

	public void setTypeLettreBienvenue(TypeLettreBienvenue typeLettreBienvenue) {
		this.typeLettreBienvenue = typeLettreBienvenue;
	}

	public String getPage() { return page; }

	public void setPage(String page) { this.page = page; }
}
