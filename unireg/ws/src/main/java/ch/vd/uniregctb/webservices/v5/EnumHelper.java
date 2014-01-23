package ch.vd.uniregctb.webservices.v5;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.unireg.ws.security.v1.AllowedAccess;
import ch.vd.unireg.xml.party.v3.PartyType;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.Niveau;

public abstract class EnumHelper {

	public static AllowedAccess toXml(Niveau niveau) {
		if (niveau == null) {
			return AllowedAccess.NONE;
		}
		switch (niveau) {
			case ECRITURE:
				return AllowedAccess.READ_WRITE;
			case LECTURE:
				return AllowedAccess.READ_ONLY;
			default:
				throw new IllegalArgumentException("Unsupported value: " + niveau);
		}
	}

	public static TiersCriteria.TypeRecherche toCore(SearchMode searchMode) {
		if (searchMode == null) {
			return null;
		}
		switch (searchMode) {
			case IS_EXACTLY:
				return TiersCriteria.TypeRecherche.EST_EXACTEMENT;
			case CONTAINS:
				return TiersCriteria.TypeRecherche.CONTIENT;
			case PHONETIC:
				return TiersCriteria.TypeRecherche.PHONETIQUE;
			default:
				throw new IllegalArgumentException("Unsupported value: " + searchMode);
		}
	}

	public static Set<TiersCriteria.TypeTiers> toCore(Set<PartyType> types) {
		if (types == null || types.isEmpty()) {
			return null;
		}
		final Set<TiersCriteria.TypeTiers> res = EnumSet.noneOf(TiersCriteria.TypeTiers.class);
		for (PartyType type : types) {
			res.add(ch.vd.uniregctb.xml.EnumHelper.xmlToCore(type));
		}
		return res;
	}

	public static Set<CategorieImpotSource> getCategoriesImpotSourceAutorisees() {
		return ch.vd.uniregctb.xml.EnumHelper.CIS_SUPPORTEES_V3;
	}
}
