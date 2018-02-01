package ch.vd.unireg.qsnc;

import ch.vd.registre.base.date.RegDate;

public class QuestionnaireSNCAddView {

	private long entrepriseId;
	private int periodeFiscale;
	private boolean depuisTache;
	private RegDate delaiAccorde;

	public QuestionnaireSNCAddView() {
	}

	public QuestionnaireSNCAddView(long entrepriseId, int periodeFiscale) {
		this.entrepriseId = entrepriseId;
		this.periodeFiscale = periodeFiscale;
	}

	public long getEntrepriseId() {
		return entrepriseId;
	}

	public void setEntrepriseId(long entrepriseId) {
		this.entrepriseId = entrepriseId;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public boolean isDepuisTache() {
		return depuisTache;
	}

	public void setDepuisTache(boolean depuisTache) {
		this.depuisTache = depuisTache;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}
}
