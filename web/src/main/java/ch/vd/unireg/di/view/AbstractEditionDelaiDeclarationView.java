package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.QuestionnaireSNC;

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

	public void setDiInfo(DeclarationImpotOrdinaire di) {
		this.tiersId = di.getTiers().getId();
		this.declarationPeriode = di.getDateFin().year();
		this.declarationRange = new DateRangeHelper.Range(di);
		this.dateExpedition = di.getDateExpedition();
		this.idDeclaration = di.getId();
		this.ancienDelaiAccorde = di.getDelaiAccordeAu();
	}

	public AbstractEditionDelaiDeclarationView(QuestionnaireSNC qsnc) {
		this.tiersId = qsnc.getTiers().getId();
		this.declarationPeriode = qsnc.getDateFin().year();
		this.declarationRange = new DateRangeHelper.Range(qsnc);
		this.dateExpedition = qsnc.getDateExpedition();
		this.idDeclaration = qsnc.getId();
		this.ancienDelaiAccorde = qsnc.getDelaiAccordeAu();
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
