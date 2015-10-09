package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

/**
 * Quelques méthodes pratiques d'interprétation des données dans le cadre des organisations
 */
public abstract class OrganisationHelper {

	/**
	 * @param identifiants map d'identifiants datés triés par une clé qui indique leur type (numéro IDE, identifiant cantonal...)
	 * @param cle la clé en question (CT.VD.PARTY pour l'identifiant cantonal, CH.IDE pour le numéro IDE)
	 * @return la liste historisée des valeurs de ce type
	 */
	@Nullable
	public static List<DateRanged<String>> extractIdentifiant(Map<String, List<DateRanged<String>>> identifiants, String cle) {
		final List<DateRanged<String>> extracted = identifiants.get(cle);
		return extracted == null || extracted.isEmpty() ? null : extracted;
	}

	public static Siege siegePrincipalPrecedant(Organisation organisation, RegDate evtDate) {
		final RegDate theDate = evtDate != null ? evtDate : RegDate.get();
		return DateRangeHelper.rangeAt(organisation.getSiegesPrincipaux(), theDate.getOneDayBefore());
	}
}
