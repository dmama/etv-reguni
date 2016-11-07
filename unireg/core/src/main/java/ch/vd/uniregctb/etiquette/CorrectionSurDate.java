package ch.vd.uniregctb.etiquette;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.RegDate;

public enum CorrectionSurDate implements Function<RegDate, RegDate> {

	FIN_ANNEE_PRECEDENTE("EOLY") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(RegDate::year)
					.map(annee -> RegDate.get(annee - 1, 12, 31))
					.orElse(null);
		}
	},

	DEBUT_ANNEE("BOY") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(RegDate::year)
					.map(annee -> RegDate.get(annee, 1, 1))
					.orElse(null);
		}
	},

	FIN_MOIS_PRECEDENT("EOLM") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(ref -> RegDate.get(ref.year(), ref.month(), 1))
					.map(RegDate::getOneDayBefore)
					.orElse(null);
		}
	},

	DEBUT_MOIS("BOM") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(ref -> RegDate.get(ref.year(), ref.month(), 1))
					.orElse(null);
		}
	},

	SANS_CORRECTION("NONE") {
		@Override
		public RegDate apply(RegDate date) {
			return date;
		}
	},

	FIN_MOIS("EOM") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(RegDate::getLastDayOfTheMonth)
					.orElse(null);
		}
	},

	DEBUT_MOIS_SUIVANT("BONM") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(RegDate::getLastDayOfTheMonth)
					.map(RegDate::getOneDayAfter)
					.orElse(null);
		}
	},

	FIN_ANNEE("EOY") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(RegDate::year)
					.map(annee -> RegDate.get(annee, 12, 31))
					.orElse(null);
		}
	},

	DEBUT_ANNEE_SUIVANTE("BONY") {
		@Override
		public RegDate apply(RegDate date) {
			return Optional.ofNullable(date)
					.map(RegDate::year)
					.map(annee -> RegDate.get(annee + 1, 1, 1))
					.orElse(null);
		}
	};

	private final String code;

	private static final Map<String, CorrectionSurDate> byCode = buildByCodeMap();

	private static Map<String, CorrectionSurDate> buildByCodeMap() {
		return Stream.of(CorrectionSurDate.values())
				.collect(Collectors.toMap(CorrectionSurDate::getCode, Function.identity()));        // ça pête si le même code est utilisé plusieurs fois...
	}

	CorrectionSurDate(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static CorrectionSurDate valueOfCode(String code) {
		return byCode.get(code);
	}
}
