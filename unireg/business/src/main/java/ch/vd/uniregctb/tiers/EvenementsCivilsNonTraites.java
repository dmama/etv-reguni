package ch.vd.uniregctb.tiers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class EvenementsCivilsNonTraites {

	public static enum Source {
		REGPP("RegPP"),
		RCPERS("RCPers");

		private final String libelle;

		private Source(String libelle) {
			this.libelle = libelle;
		}

		public String getLibelle() {
			return libelle;
		}
	}

	private final Map<Source, Set<Long>> nosIndividus = new HashMap<Source, Set<Long>>(Source.values().length);

	public void addAll(Source src, Collection<Long> individus) {
		if (individus != null && individus.size() > 0) {
			final Set<Long> toBeFilled;
			if (nosIndividus.containsKey(src)) {
				toBeFilled = nosIndividus.get(src);
			}
			else {
				toBeFilled = new TreeSet<Long>();
				nosIndividus.put(src, toBeFilled);
			}
			toBeFilled.addAll(individus);
		}
	}

	public boolean hasForSource(Source src) {
		return nosIndividus.containsKey(src);
	}

	public Set<Long> getForSource(Source src) {
		return nosIndividus.get(src);
	}

	public boolean isEmpty() {
		for (Source src : Source.values()) {
			if (hasForSource(src)) {
				return false;
			}
		}
		return true;
	}
}
