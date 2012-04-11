package ch.vd.moscow.database;

import java.util.Date;
import java.util.List;

public class CallStatsKey {

	private final List<Object> coord;
	private final Date date;

	public CallStatsKey(List<Object> coord, Date date) {
		this.coord = coord;
		this.date = date;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final CallStatsKey that = (CallStatsKey) o;

		if (coord != null ? !coord.equals(that.coord) : that.coord != null) return false;
		//noinspection RedundantIfStatement
		if (!date.equals(that.date)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = coord != null ? coord.hashCode() : 0;
		result = 31 * result + date.hashCode();
		return result;
	}
}
