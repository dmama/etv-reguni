package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("unused")
public abstract class AbstractEditionDelaiDeclarationView {

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
	private EtatDelaiDocumentFiscal decision;

	public AbstractEditionDelaiDeclarationView() {
	}

	public AbstractEditionDelaiDeclarationView(@NotNull Declaration declaration, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		setDiInfo(declaration);
		this.dateDemande = dateDemande;
		this.delaiAccordeAu = delaiAccordeAu;
		this.decision = decision;
	}

	public AbstractEditionDelaiDeclarationView(@NotNull Declaration declaration) {
		setDiInfo(declaration);
	}

	public void setDiInfo(@NotNull Declaration di) {
		this.tiersId = di.getTiers().getId();
		this.declarationPeriode = di.getDateFin().year();
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

	public EtatDelaiDocumentFiscal getDecision() {
		return decision;
	}

	public void setDecision(EtatDelaiDocumentFiscal decision) {
		this.decision = decision;
	}
}
