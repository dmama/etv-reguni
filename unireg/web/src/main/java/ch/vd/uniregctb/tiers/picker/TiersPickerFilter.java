package ch.vd.uniregctb.tiers.picker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersFilter;

public class TiersPickerFilter implements TiersFilter {

	private String description;
	private TiersCriteria.TypeVisualisation typeVisualisation;
	private TiersCriteria.TypeTiers typeTiers;
	private boolean inclureI107;
	private boolean inclureTiersAnnules;
	private boolean tiersAnnulesSeulement;
	private Boolean tiersActif;

	public TiersPickerFilter(Map<String, String> params) {
		String value = params.get("typeVisualisation");
		if (value != null) {
			this.typeVisualisation = TiersCriteria.TypeVisualisation.valueOf(value);
		}
		value = params.get("typeTiers");
		if (value != null) {
			this.typeTiers = TiersCriteria.TypeTiers.valueOf(value);
		}
		value = params.get("inclureI107");
		if (value != null) {
			this.inclureI107 = Boolean.parseBoolean(value);
		}
		value = params.get("inclureTiersAnnules");
		if (value != null) {
			this.inclureTiersAnnules = Boolean.parseBoolean(value);
		}
		value = params.get("tiersAnnulesSeulement");
		if (value != null) {
			this.tiersAnnulesSeulement = Boolean.parseBoolean(value);
		}
		value = params.get("tiersActif");
		if (value != null) {
			this.tiersActif = Boolean.parseBoolean(value);
		}

		this.description = buildDescription();
	}

	private String buildDescription() {
		final StringBuilder s = new StringBuilder();
		if (typeVisualisation == TiersCriteria.TypeVisualisation.LIMITEE) {
			s.append("recherche en mode visualisation limitée ");
		}
		else {
			s.append("recherche ");
		}
		final List<String> list = new ArrayList<String>();
		if (typeTiers != null) {
			list.add("sur les tiers de type " + typeTiers + " uniquement");
		}
		if (inclureI107) {
			list.add("en incluant les i107");
		}
		if (inclureTiersAnnules) {
			list.add("en incluant les tiers annulés");
		}
		if (tiersAnnulesSeulement) {
			list.add("sur les tiers annulés uniquement");
		}
		if (tiersActif != null && tiersActif) {
			list.add("sur les tiers actifs uniquement");
		}
		for (int i = 0, listSize = list.size(); i < listSize; i++) {
			if (i == 0) {
				// rien à faire
			}
			else if (i < listSize - 1) {
				s.append(", ");
			}
			else {
				s.append(" et ");
			}
			s.append(list.get(i));
		}
		s.append('.');
		return s.toString();
	}

	public String getDescription() {
		return description;
	}

	public TiersCriteria.TypeVisualisation getTypeVisualisation() {
		return typeVisualisation;
	}

	public TiersCriteria.TypeTiers getTypeTiers() {
		return typeTiers;
	}

	public boolean isInclureI107() {
		return inclureI107;
	}

	public boolean isInclureTiersAnnules() {
		return inclureTiersAnnules;
	}

	public boolean isTiersAnnulesSeulement() {
		return tiersAnnulesSeulement;
	}

	public Boolean isTiersActif() {
		return tiersActif;
	}
}
