package ch.vd.unireg.webservices.v7;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.common.v2.PartialDate;
import ch.vd.unireg.xml.party.v5.PartyType;

public abstract class DataHelper {

	public static Date coreToWeb(java.util.Date date) {
		return ch.vd.unireg.xml.DataHelper.coreToXMLv2(date);
	}

	public static Date coreToWeb(RegDate date) {
		return ch.vd.unireg.xml.DataHelper.coreToXMLv2(date);
	}

	public static RegDate webToRegDate(Date date) {
		return ch.vd.unireg.xml.DataHelper.xmlToCore(date);
	}

	public static RegDate webToRegDate(PartialDate date) {
		return ch.vd.unireg.xml.DataHelper.xmlToCore(date);
	}

	@NotNull
	public static PartyType getPartyType(@NotNull TypeTiers type) {
		switch (type) {
		case PERSONNE_PHYSIQUE:
			return PartyType.NATURAL_PERSON;
		case MENAGE_COMMUN:
			return PartyType.HOUSEHOLD;
		case ENTREPRISE:
			return PartyType.CORPORATION;
		case ETABLISSEMENT:
			return PartyType.ESTABLISHMENT;
		case COLLECTIVITE_ADMINISTRATIVE:
			return PartyType.ADMINISTRATIVE_AUTHORITY;
		case AUTRE_COMMUNAUTE:
			return PartyType.OTHER_COMMUNITY;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			return PartyType.DEBTOR;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + "]");
		}
	}
}