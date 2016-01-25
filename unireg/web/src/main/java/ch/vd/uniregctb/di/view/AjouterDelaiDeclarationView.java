package ch.vd.uniregctb.di.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

@SuppressWarnings("UnusedDeclaration")
public class AjouterDelaiDeclarationView {

	// information en lecture-seule
	private Long tiersId;
	private int declarationPeriode;
	private DateRange declarationRange;
	private RegDate ancienDelaiAccorde;
	private RegDate dateExpedition;

	// champs du formulaire
	private Long idDeclaration;
	private RegDate dateDemande;
	private RegDate delaiAccordeAu;
	private boolean confirmationEcrite;

	public AjouterDelaiDeclarationView() {
	}

	public AjouterDelaiDeclarationView(DeclarationImpotOrdinaire di, RegDate delaiAccordeAu) {
		setDiInfo(di);
		this.delaiAccordeAu = delaiAccordeAu;
		this.dateDemande = RegDate.get();
		this.confirmationEcrite = false;
	}

	public void setDiInfo (DeclarationImpotOrdinaire di) {
		this.tiersId = di.getTiers().getId();
		this.declarationPeriode = di.getDateDebut().year();
		this.declarationRange = new DateRangeHelper.Range(di);
		this.dateExpedition = di.getDateExpedition();
		this.idDeclaration = di.getId();
		this.ancienDelaiAccorde = di.getDelaiAccordeAu();
	}

	public Long getTiersId() {
		return tiersId;
	}

	public int getDeclarationPeriode() {
		return declarationPeriode;
	}

	public DateRange getDeclarationRange() {
		return declarationRange;
	}

	public RegDate getAncienDelaiAccorde() {
		return ancienDelaiAccorde;
	}

	public void setAncienDelaiAccorde(RegDate ancienDelaiAccorde) {
		this.ancienDelaiAccorde = ancienDelaiAccorde;
	}

	public RegDate getDateExpedition() {
		return dateExpedition;
	}

	public Long getIdDeclaration() {
		return idDeclaration;
	}

	public void setIdDeclaration(Long idDeclaration) {
		this.idDeclaration = idDeclaration;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate delaiAccordeAu) {
		this.delaiAccordeAu = delaiAccordeAu;
	}

	public boolean isConfirmationEcrite() {
		return confirmationEcrite;
	}

	public void setConfirmationEcrite(boolean confirmationEcrite) {
		this.confirmationEcrite = confirmationEcrite;
	}
}
