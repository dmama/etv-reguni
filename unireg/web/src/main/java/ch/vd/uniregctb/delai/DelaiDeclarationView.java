package ch.vd.uniregctb.delai;

import java.sql.Timestamp;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DelaiDeclaration;

public class DelaiDeclarationView implements Comparable<DelaiDeclarationView> {

	private Long id;

	private RegDate dateDemande;

	private RegDate dateTraitement;

	private RegDate delaiAccordeAu;

	private RegDate oldDelaiAccorde;

	private RegDate dateExpedition;

	private Boolean confirmationEcrite;

	private Long idDeclaration;

	private Long tiersId;

	private int declarationPeriode;

	private DateRange declarationRange;

	private String logModifUser;

	private Timestamp logModifDate;

	private boolean annule;

	private boolean first;

	public DelaiDeclarationView() {
	}

	public DelaiDeclarationView(DelaiDeclaration delai) {
		this.id = delai.getId();
		this.annule = delai.isAnnule();
		this.confirmationEcrite = delai.getConfirmationEcrite();
		this.dateDemande = delai.getDateDemande();
		this.dateTraitement = delai.getDateTraitement();
		this.delaiAccordeAu = delai.getDelaiAccordeAu();
		this.logModifDate = delai.getLogModifDate();
		this.logModifUser = delai.getLogModifUser();
		this.idDeclaration = delai.getDeclaration().getId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate delaiAccordeAu) {
		this.delaiAccordeAu = delaiAccordeAu;
	}

	public Boolean getConfirmationEcrite() {
		return confirmationEcrite;
	}

	public void setConfirmationEcrite(Boolean confirmationEcrite) {
		this.confirmationEcrite = confirmationEcrite;
	}

	public Long getIdDeclaration() {
		return idDeclaration;
	}

	public void setIdDeclaration(Long idDeclaration) {
		this.idDeclaration = idDeclaration;
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

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public RegDate getDateExpedition() {
		return dateExpedition;
	}

	public void setDateExpedition(RegDate dateExpedition) {
		this.dateExpedition = dateExpedition;
	}

	public RegDate getOldDelaiAccorde() {
		return oldDelaiAccorde;
	}

	public void setOldDelaiAccorde(RegDate oldDelaiAccorde) {
		this.oldDelaiAccorde = oldDelaiAccorde;
	}

	public String getLogModifUser() {
		return logModifUser;
	}

	public void setLogModifUser(String logModifUser) {
		this.logModifUser = logModifUser;
	}

	public Timestamp getLogModifDate() {
		return logModifDate;
	}

	public void setLogModifDate(Timestamp logModifDate) {
		this.logModifDate = logModifDate;
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	/**
	 * Compare d'apres la date de DelaiDeclarationView
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(DelaiDeclarationView delaiDeclarationView) {
		RegDate autreDelaiAccordeAu = delaiDeclarationView.getDelaiAccordeAu();
		int value = (-1) * delaiAccordeAu.compareTo(autreDelaiAccordeAu);
		return value;
	}


}
