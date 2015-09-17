package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;

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

	/**
	 * @param identifiants map d'identifiants datés triés par une clé qui indique leur type (numéro IDE, identifiant cantonal...)
	 * @return l'identifiant cantonal trouvé
	 * @throws ServiceOrganisationException en cas d'absence de ce numéro
	 */
	public static long extractIdCantonal(Map<String, List<DateRanged<String>>> identifiants) {
		final List<DateRanged<String>> ids = extractIdentifiant(identifiants, OrganisationConstants.CLE_ID_CANTONAL);
		if (ids == null || ids.isEmpty()) {
			throw new ServiceOrganisationException("Donnée reçue sans identifiant cantonal...");
		}

		final DateRanged<String> first = ids.get(0);
		if (first == null) {
			throw new ServiceOrganisationException("Donnée reçue avec identifiant cantonal vide...");
		}

		return Long.parseLong(ids.get(0).getPayload());
	}
}
