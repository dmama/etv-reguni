package ch.vd.uniregctb.foncier.migration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

public class DonneesFusionsCommunes {

	public static class OffsetCommune {
		public final int offset;
		public final int ofsCommuneDestination;

		public OffsetCommune(int offset, int ofsCommuneDestination) {
			this.offset = offset;
			this.ofsCommuneDestination = ofsCommuneDestination;
		}
	}

	private Map<Integer, SortedMap<RegDate, OffsetCommune>> map;

	public DonneesFusionsCommunes(List<DonneeBruteFusionCommunes> donneesBrutes) {
		final Map<Integer, SortedMap<RegDate, OffsetCommune>> map = new HashMap<>();
		for (DonneeBruteFusionCommunes data : donneesBrutes) {
			final SortedMap<RegDate, OffsetCommune> dataMap = map.computeIfAbsent(data.ofsAncienneCommune, k -> new TreeMap<>());
			dataMap.put(data.dateFusion, new OffsetCommune(data.offsetParcelle, data.ofsNouvelleCommune));
		}
		this.map = Collections.unmodifiableMap(map);
	}

	@Nullable
	public OffsetCommune getOffsetAndDestination(int ofsAncienneCommune) {
		final SortedMap<RegDate, OffsetCommune> localMap = map.get(ofsAncienneCommune);
		if (localMap == null) {
			return null;
		}

		return localMap.values().stream()
				.reduce((offset1, offset2) -> new OffsetCommune(offset1.offset + offset2.offset, offset2.ofsCommuneDestination))
				.orElse(null);
	}
}
