package ch.vd.uniregctb.documentfiscal;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2017-11-30, <raphael.marmier@vd.ch>
 */
public class EditionDelaiAutreDocumentFiscalView {

	private Long tiersId;
	private int perdiode;
	private RegDate ancienDelaiAccorde;
	private RegDate delaiAccordeAu;
	private RegDate dateDemande;

	// champs du formulaire
	private Long idDocumentFiscal;

	public EditionDelaiAutreDocumentFiscalView() {
	}

	public EditionDelaiAutreDocumentFiscalView(AutreDocumentFiscal doc, RegDate delaiAccordeAu) {
		resetDocumentInfo(doc);
		this.delaiAccordeAu = delaiAccordeAu;
	}

	protected void resetDocumentInfo(AutreDocumentFiscal doc) {
		this.tiersId = doc.getTiers().getId();
		this.idDocumentFiscal = doc.getId();
		this.perdiode = doc.getPeriodeFiscale();
		this.ancienDelaiAccorde = doc.getDernierDelaiAccorde().getDelaiAccordeAu();
		this.dateDemande = RegDate.get();
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public int getPerdiode() {
		return perdiode;
	}

	public void setPerdiode(int perdiode) {
		this.perdiode = perdiode;
	}

	public RegDate getAncienDelaiAccorde() {
		return ancienDelaiAccorde;
	}

	public void setAncienDelaiAccorde(RegDate ancienDelaiAccorde) {
		this.ancienDelaiAccorde = ancienDelaiAccorde;
	}

	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate delaiAccordeAu) {
		this.delaiAccordeAu = delaiAccordeAu;
	}

	public Long getIdDocumentFiscal() {
		return idDocumentFiscal;
	}

	public void setIdDocumentFiscal(Long idDocumentFiscal) {
		this.idDocumentFiscal = idDocumentFiscal;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}
}
