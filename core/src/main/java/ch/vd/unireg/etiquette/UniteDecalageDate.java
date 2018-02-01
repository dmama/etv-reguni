package ch.vd.unireg.etiquette;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.RegDate;

public enum UniteDecalageDate implements DecalageDate {

	ANNEE("Y") {
		@Override
		public RegDate apply(RegDate date, int decalage) {
			return Optional.ofNullable(date)
					.map(ref -> ref.addYears(decalage))
					.orElse(null);
		}
	},

	MOIS("M") {
		@Override
		public RegDate apply(RegDate date, int decalage) {
			return Optional.ofNullable(date)
					.map(ref -> ref.addMonths(decalage))
					.orElse(null);
		}
	},

	SEMAINE("W") {
		@Override
		public RegDate apply(RegDate date, int decalage) {
			return Optional.ofNullable(date)
					.map(ref -> ref.addDays(7 * decalage))
					.orElse(null);
		}
	},

	JOUR("D") {
		@Override
		public RegDate apply(RegDate date, int decalage) {
			return Optional.ofNullable(date)
					.map(ref -> ref.addDays(decalage))
					.orElse(null);
		}
	};

	private final String code;

	private static final Map<String, UniteDecalageDate> byCode = buildByCodeMap();

	private static Map<String, UniteDecalageDate> buildByCodeMap() {
		return Stream.of(UniteDecalageDate.values())
				.collect(Collectors.toMap(UniteDecalageDate::getCode, Function.identity()));        // ça pête si le même code est utilisé plusieurs fois...
	}

	UniteDecalageDate(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static UniteDecalageDate valueOfCode(String code) {
		return byCode.get(code);
	}
}
