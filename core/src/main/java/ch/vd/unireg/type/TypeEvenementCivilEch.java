package ch.vd.unireg.type;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Les types d'événements civils issus de la norme eVD-0006
 *
 * NB: Pensez à rajouter/supprimer/corriger le libellé du type de l'évenement
 *     dans UniregOptionResources.properties après la modification de cette enum
 */
public enum TypeEvenementCivilEch {

	NAISSANCE(1, 1),
	DECES(2, 23),
	ABSENCE(3, 21),
	MARIAGE(4, 8),
	SEPARATION(6, 9),
	CESSATION_SEPARATION(7, 10),
	DIVORCE(8, 11),
	CHGT_ETAT_CIVIL_PARTENAIRE(10, 14),
	ANNULATION_MARIAGE(11, 5),
	NATURALISATION(12, 17),
	OBENTION_DROIT_CITE(13, null),
	PERTE_DROIT_CITE(14, null),
	DECHEANCE_NATIONALITE_SUISSE(15, 18),
	CHGT_CATEGORIE_ETRANGER(16, 19),
	CHGT_NATIONALITE_ETRANGERE(17, null),
	ARRIVEE(18, 2),
	DEPART(19, 3),
	DEMENAGEMENT_DANS_COMMUNE(20, 4),
	CONTACT(21, null),
	CHGT_BLOCAGE_ADRESSE(22, 25),
	CHGT_RELATION_ANNONCE(23, 15),
	CHGT_NOM(29, null),
	CHGT_RELIGION(31, null),
	ANNULATION_ABSENCE(34, 22),
	ENREGISTREMENT_PARTENARIAT(36, 12),
	DISSOLUTION_PARTENARIAT(37, 13),
	CORR_RELATION_ANNONCE(42, 16),
	CORR_RELATIONS(44, 26),
	CHGT_DROIT_CITE(46, null),
	CORR_IDENTIFICATION(50, null),
	CORR_AUTRES_NOMS(51, null),
	CORR_NATIONALITE(52, null),
	CORR_CONTACT(53, null),
	CORR_RELIGION(54, null),
	CORR_ORIGINE(55, null),
	CORR_CATEGORIE_ETRANGER(56, 20),
	CORR_ETAT_CIVIL(57, 7),
	CORR_LIEU_NAISSANCE(58, null),
	CORR_DATE_DECES(59, 24),
	ATTRIBUTION_DONNEES_UPI(1001, null),
	CORR_DONNEES_UPI(1002, null),
	ANNULATION_DONNEES_UPI(1003, null),
	INACTIVATION(1004, null),
	REACTIVATION(1005, null),
	TESTING(499999, null);

	private final int codeECH;
	private final Integer priorite;

	private static final Map<Integer, TypeEvenementCivilEch> typesByCode;

	static {
		typesByCode = new HashMap<>(TypeEvenementCivilEch.values().length);
		for (TypeEvenementCivilEch mod : TypeEvenementCivilEch.values()) {
			final TypeEvenementCivilEch old = typesByCode.put(mod.codeECH, mod);
			if (old != null) {
				throw new IllegalArgumentException(String.format("Code %d utilisé plusieurs fois!", old.codeECH));
			}
		}
	}

	TypeEvenementCivilEch(int codeECH, Integer priorite) {
		this.codeECH = codeECH;
		this.priorite = priorite;
	}

	public static TypeEvenementCivilEch fromEchCode(int code) {
		return typesByCode.get(code);
	}

	public static TypeEvenementCivilEch fromEchCode(String code) {
		return fromEchCode(Integer.valueOf(code));
	}

	public int getCodeECH() {
		return codeECH;
	}

	@Nullable
	public Integer getPriorite() {
		return priorite;
	}
}
