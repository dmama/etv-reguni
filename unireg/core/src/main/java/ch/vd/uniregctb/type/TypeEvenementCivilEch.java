package ch.vd.uniregctb.type;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Les types d'événements civils issus de la norme eCH-0020
 */
public enum TypeEvenementCivilEch {

	ETAT_COMPLET(0, null),
	NAISSANCE(1, 1),
	DECES(2, 98),
	ABSENCE(3, 96),
	MARIAGE(4, 20),
	SEPARATION(6, 21),
	CESSATION_SEPARATION(7, 22),
	DIVORCE(8, 23),
	CHGT_ETAT_CIVIL_PARTENAIRE(10, 80),
	ANNULATION_MARIAGE(11, 6),
	NATURALISATION(12, 30),
	OBENTION_DROIT_CITE(13, 32),
	PERTE_DROIT_CITE(14, 33),
	DECHEANCE_NATIONALITE_SUISSE(15, 31),
	CHGT_CATEGORIE_ETRANGER(16, 34),
	CHGT_NATIONALITE_ETRANGERE(17, 35),
	ARRIVEE(18, 2),
	DEPART(19, 3),
	DEMENAGEMENT_DANS_COMMUNE(20, 4),
	CONTACT(21, 5),
	CHGT_BLOCAGE_ADRESSE(22, null),
	CHGT_RELATION_ANNONCE(23, null),
	CHGT_NOM(29, null),
	CHGT_RELIGION(31, null),
	ANNULATION_ABSENCE(34, 97),
	ENREGISTREMENT_PARTENARIAT(36, 24),
	DISSOLUTION_PARTENARIAT(37, 25),
	CORR_RELATION_ANNONCE(42, null),
	CORR_ADRESSE(43, 6),
	CORR_RELATIONS(44, 40),
	CHGT_DROIT_CITE(46, 36),
	CORR_IDENTIFICATION(50, 51),
	CORR_AUTRES_NOMS(51, 52),
	CORR_NATIONALITE(52, 53),
	CORR_CONTACT(53, 54),
	CORR_RELIGION(54, null),
	CORR_ORIGINE(55, 55),
	CORR_CATEGORIE_ETRANGER(56, 56),
	CORR_ETAT_CIVIL(57, 57),
	CORR_LIEU_NAISSANCE(58, null),
	CORR_DATE_DECES(59, 99);

	private final int codeECH;
	private final Integer priorite;

	private static final Map<Integer, TypeEvenementCivilEch> typesByCode;

	static {
		typesByCode = new HashMap<Integer, TypeEvenementCivilEch>(TypeEvenementCivilEch.values().length);
		for (TypeEvenementCivilEch mod : TypeEvenementCivilEch.values()) {
			final TypeEvenementCivilEch old = typesByCode.put(mod.codeECH, mod);
			if (old != null) {
				throw new IllegalArgumentException(String.format("Code %d utilisé plusieurs fois!", old.codeECH));
			}
		}
	}

	private TypeEvenementCivilEch(int codeECH, Integer priorite) {
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
