package ch.vd.uniregctb.webservices.party3.cache;

import ch.vd.unireg.xml.common.v1.Date;

public class GetTaxOfficesKey {

	private int municipalityFSOId;
	private Date date;

	public GetTaxOfficesKey(int municipalityFSOId, Date date) {
		this.municipalityFSOId = municipalityFSOId;
		this.date = date;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final GetTaxOfficesKey that = (GetTaxOfficesKey) o;

		if (municipalityFSOId != that.municipalityFSOId) return false;
		//noinspection RedundantIfStatement
		if (date != null ? !date.equals(that.date) : that.date != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = municipalityFSOId;
		result = 31 * result + (date != null ? date.hashCode() : 0);
		return result;
	}
}
