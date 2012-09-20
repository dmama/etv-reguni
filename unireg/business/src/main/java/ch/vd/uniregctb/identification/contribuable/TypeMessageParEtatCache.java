package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.List;

public class TypeMessageParEtatCache {
	public List<String> typeMessageTraites;
	public List<String> typeMessageNonTraites;

	public TypeMessageParEtatCache() {
		typeMessageTraites = new ArrayList<String>();
		typeMessageNonTraites = new ArrayList<String>();
	}


	public TypeMessageParEtatCache(List<String> typeMessageTraites, List<String> typeMessageNonTraites) {
		this.typeMessageTraites = typeMessageTraites;
		this.typeMessageNonTraites = typeMessageNonTraites;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final TypeMessageParEtatCache that = (TypeMessageParEtatCache) o;

		if (typeMessageNonTraites != null ? !typeMessageNonTraites.equals(that.typeMessageNonTraites) : that.typeMessageNonTraites != null) return false;
		if (typeMessageTraites != null ? !typeMessageTraites.equals(that.typeMessageTraites) : that.typeMessageTraites != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = typeMessageTraites != null ? typeMessageTraites.hashCode() : 0;
		result = 31 * result + (typeMessageNonTraites != null ? typeMessageNonTraites.hashCode() : 0);
		return result;
	}

	public void setTypeMessageTraites(List<String> typeMessageTraites) {
		this.typeMessageTraites = typeMessageTraites;
	}

	public void setTypeMessageNonTraites(List<String> typeMessageNonTraites) {
		this.typeMessageNonTraites = typeMessageNonTraites;
	}
}
