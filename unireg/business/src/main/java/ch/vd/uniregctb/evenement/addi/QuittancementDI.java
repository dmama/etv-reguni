package ch.vd.uniregctb.evenement.addi;

import ch.vd.registre.base.date.RegDate;

public class QuittancementDI extends EvenementAddi{
	private String businessId;
	private long numeroContribuable;
	private int periodeFiscale;
	private RegDate date;
	private String source;

	public long  getNumeroContribuable() {
		return numeroContribuable;
	}

	public void setNumeroContribuable(long numeroContribuable) {
		this.numeroContribuable = numeroContribuable;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public RegDate getDate() {
		return date;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	@Override
	public String toString() {
		return "QuittancementDI{" +
				"businessId='" + businessId + '\'' +
				", Date=" + date +
				", noContribuable=" + numeroContribuable + '\'' +
				", periodeFiscale=" + periodeFiscale + '\'' +
				", source=" + source + '\'' +
				'}';
	}
}
