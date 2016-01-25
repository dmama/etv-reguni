package ch.vd.uniregctb.di.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

public abstract class AbstractEditionDelaiDeclarationView {

	// information en lecture-seule
	private Long tiersId;
	private int declarationPeriode;
	private DateRange declarationRange;
	private RegDate ancienDelaiAccorde;
	private RegDate dateExpedition;

	// champs du formulaire
	private Long idDeclaration;

	public AbstractEditionDelaiDeclarationView() {
	}

	public AbstractEditionDelaiDeclarationView(DeclarationImpotOrdinaire di) {
		setDiInfo(di);
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

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public int getDeclarationPeriode() {
		return declarationPeriode;
	}

	public void setDeclarationPeriode(int declarationPeriode) {
		this.declarationPeriode = declarationPeriode;
	}

	public DateRange getDeclarationRange() {
		return declarationRange;
	}

	public void setDeclarationRange(DateRange declarationRange) {
		this.declarationRange = declarationRange;
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

	public void setDateExpedition(RegDate dateExpedition) {
		this.dateExpedition = dateExpedition;
	}

	public Long getIdDeclaration() {
		return idDeclaration;
	}

	public void setIdDeclaration(Long idDeclaration) {
		this.idDeclaration = idDeclaration;
	}
}
