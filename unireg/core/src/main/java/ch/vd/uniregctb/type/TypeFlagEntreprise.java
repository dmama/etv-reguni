package ch.vd.uniregctb.type;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Les différents type de flags d'entreprise :
 * <ul>
 *     <li>
 *         Groupe SI/Service/LIASF
 *         <ul>
 *              <li>utilité publique (LIASF)</li>
 *              <li>société immobilière ordinaire</li>
 *              <li>société immobilière à caractère social</li>
 *              <li>société immobilière subventionnée</li>
 *              <li>société immobilière d'actionnaires-locataires (SIAL)</li>
 *              <li>APM société immobilière subventionnée</li>
 *              <li>société de service</li>
 *         </ul>
 *     </li>
 *     <li>
 *         Groupe libre
 *         <ul>
 *             <li>audit</li>
 *             <li>expertise</li>
 *             <li>imin</li>
 *         </ul>
 *     </li>
 * </ul>
 * @see ch.vd.uniregctb.common.LengthConstants#FLAG_ENTREPRISE_TYPE
 */
public enum TypeFlagEntreprise {

	UTILITE_PUBLIQUE(GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE),
	SOC_IMM_ORDINAIRE(GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE),
	SOC_IMM_SUBVENTIONNEE(GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE),
	SOC_IMM_CARACTERE_SOCIAL(GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE),
	SOC_IMM_ACTIONNAIRES_LOCATAIRES(GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE),
	APM_SOC_IMM_SUBVENTIONNEE(GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE),
	SOC_SERVICE(GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE),

	AUDIT(GroupeFlagsEntreprise.LIBRE),
	EXPERTISE(GroupeFlagsEntreprise.LIBRE),
	IMIN(GroupeFlagsEntreprise.LIBRE);

	private final GroupeFlagsEntreprise groupe;

	private static final Map<GroupeFlagsEntreprise, Set<TypeFlagEntreprise>> PAR_GROUPE = buildMapParGroupe();

	private static Map<GroupeFlagsEntreprise, Set<TypeFlagEntreprise>> buildMapParGroupe() {
		// constuction de la map
		final Map<GroupeFlagsEntreprise, Set<TypeFlagEntreprise>> map = new EnumMap<>(GroupeFlagsEntreprise.class);
		for (TypeFlagEntreprise type : TypeFlagEntreprise.values()) {
			Set<TypeFlagEntreprise> set = map.get(type.groupe);
			if (set == null) {
				set = EnumSet.noneOf(TypeFlagEntreprise.class);
				map.put(type.groupe, set);
			}
			set.add(type);
		}

		// passage en mode "read-only"
		for (Map.Entry<GroupeFlagsEntreprise, Set<TypeFlagEntreprise>> entry : map.entrySet()) {
			entry.setValue(Collections.unmodifiableSet(entry.getValue()));
		}

		// pas la peine de passer en read-only sur la map elle-même qui reste en interne
		return map;
	}

	TypeFlagEntreprise(GroupeFlagsEntreprise groupe) {
		this.groupe = groupe;
	}

	public GroupeFlagsEntreprise getGroupe() {
		return groupe;
	}

	/**
	 * @param groupe un groupe
	 * @return l'ensemble des types associés au groupe
	 */
	@NotNull
	public static Set<TypeFlagEntreprise> ofGroupe(GroupeFlagsEntreprise groupe) {
		final Set<TypeFlagEntreprise> set = PAR_GROUPE.get(groupe);
		return set == null ? Collections.emptySet() : set;
	}
}
