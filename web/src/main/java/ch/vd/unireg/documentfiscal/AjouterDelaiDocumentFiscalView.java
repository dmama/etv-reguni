package ch.vd.unireg.documentfiscal;

import ch.vd.registre.base.date.RegDate;

/**
 * Données nécessaires à l'ajout d'un délai sur un document fiscal.
 */
public class AjouterDelaiDocumentFiscalView {

	private Long tiersId;
	private int periode;
	private RegDate ancienDelaiAccorde;
	private RegDate delaiAccordeAu;
	private RegDate dateDemande;

	// champs du formulaire
	private Long idDocumentFiscal;

	// nécessaire pour Spring
	@SuppressWarnings("unused")
	public AjouterDelaiDocumentFiscalView() {
	}

	public AjouterDelaiDocumentFiscalView(DocumentFiscal doc, RegDate delaiAccordeAu) {
		resetDocumentInfo(doc);
		this.delaiAccordeAu = delaiAccordeAu;
	}

	public void resetDocumentInfo(DocumentFiscal doc) {
		this.tiersId = doc.getTiers().getId();
		this.idDocumentFiscal = doc.getId();
		this.periode = doc.getAnneePeriodeFiscale();
		this.ancienDelaiAccorde = doc.getDernierDelaiAccorde().getDelaiAccordeAu();
		this.dateDemande = RegDate.get();
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public int getPeriode() {
		return periode;
	}

	public void setPeriode(int periode) {
		this.periode = periode;
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
