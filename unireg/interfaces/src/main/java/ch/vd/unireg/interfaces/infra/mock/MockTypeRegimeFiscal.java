package ch.vd.unireg.interfaces.infra.mock;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.Predicate;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public class MockTypeRegimeFiscal implements TypeRegimeFiscal, Serializable {

	private static final long serialVersionUID = -2759963025198461926L;

	private final String code;
	private final Integer premierePF;
	private final Integer dernierePF;
	private final String libelle;
	private final boolean cantonal;
	private final boolean federal;
	private final boolean pourPM;
	private final boolean pourAPM;
	private final boolean defaultPM;
	private final boolean defaultAPM;

	public static final MockTypeRegimeFiscal ORDINAIRE_PM = new MockTypeRegimeFiscal("01", 1994, null, "01 - Ordinaire", true, true, true, false, true, false);
	public static final MockTypeRegimeFiscal PARTICIPATIONS = new MockTypeRegimeFiscal("11", 1994, null, "11 - Participations", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal PARTICIPATIONS_PART_IMPOSABLE = new MockTypeRegimeFiscal("12", 2001, null, "12 - Participations part imposable", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal FONDS_PLACEMENT = new MockTypeRegimeFiscal("50", 1992, null, "50 - Fonds de placement", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal TRANSPORT_CONCESSIONNE = new MockTypeRegimeFiscal("60", 1994, null, "60 - Transport concessionné", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal ORDINAIRE_APM = new MockTypeRegimeFiscal("70", 1995, null, "70 - Association/fondation", true, true, false, true, false, true);
	public static final MockTypeRegimeFiscal EXO_90G = new MockTypeRegimeFiscal("109", 2003, null, "109 - PM exonérée 90g", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal EXO_90CEFH = new MockTypeRegimeFiscal("190", 2003, null, "190 - PM exonérée 90cefh", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal ART90G = new MockTypeRegimeFiscal("709", 1994, null, "709 - 90g", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90D = new MockTypeRegimeFiscal("715", 2001, null, "715 - 90d", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90H = new MockTypeRegimeFiscal("719", 1994, null, "719 - 90h", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90E = new MockTypeRegimeFiscal("729", 1994, null, "729 - 90e", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90F = new MockTypeRegimeFiscal("739", 2001, null, "739 - 90f", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90AI = new MockTypeRegimeFiscal("749", 2001, null, "749 - 90 a et i", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90B = new MockTypeRegimeFiscal("759", 2001, null, "759 - 90b", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90C = new MockTypeRegimeFiscal("769", 2001, null, "769 - 90c", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal ART90J = new MockTypeRegimeFiscal("779", 1992, null, "779 - 90j", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal BASE_MIXTE_CANTON = new MockTypeRegimeFiscal("41C", 2001, null, "41C - Base mixte canton", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal BASE_DOMICILE_ICC = new MockTypeRegimeFiscal("42C", 2001, null, "42C - Base domicile ICC", true, true, true, false, false, false);

	public static final MockTypeRegimeFiscal[] ALL = buildAllMocks();

	private static MockTypeRegimeFiscal[] buildAllMocks() {
		final List<MockTypeRegimeFiscal> mocks = new ArrayList<>();
		for (Field field : MockTypeRegimeFiscal.class.getFields()) {
			if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
				if (MockTypeRegimeFiscal.class.isAssignableFrom(field.getType())) {
					try {
						final MockTypeRegimeFiscal value = (MockTypeRegimeFiscal) field.get(null);
						if (value != null) {
							mocks.add(value);
						}
					}
					catch (IllegalAccessException e) {
						 throw new RuntimeException(e);
					}
				}
			}
		}

		// petites vérifications de consistance
		checkOneAndOnlyOne(mocks, new Predicate<MockTypeRegimeFiscal>() {
			@Override
			public boolean evaluate(MockTypeRegimeFiscal object) {
				return object.isDefaultPourPM();
			}
		});
		checkOneAndOnlyOne(mocks, new Predicate<MockTypeRegimeFiscal>() {
			@Override
			public boolean evaluate(MockTypeRegimeFiscal object) {
				return object.isDefaultPourAPM();
			}
		});

		return mocks.toArray(new MockTypeRegimeFiscal[mocks.size()]);
	}

	private static <T extends TypeRegimeFiscal> void checkOneAndOnlyOne(List<T> elements, Predicate<? super T> predicate) {
		boolean foundOne = false;
		for (T element : elements) {
			if (predicate.evaluate(element)) {
				if (foundOne) {
					// un deuxième ??
					throw new IllegalArgumentException("Au moins deux éléments satisfont le prédicat...");
				}
				foundOne = true;
			}
		}
		if (!foundOne) {
			// aucun ??
			throw new IllegalArgumentException("Aucun élément ne satisfait au prédicat...");
		}
	}

	private MockTypeRegimeFiscal(String code, Integer premierePF, Integer dernierePF, String libelle, boolean cantonal, boolean federal, boolean pourPM, boolean pourAPM, boolean defaultPM, boolean defaultAPM) {
		this.code = code;
		this.premierePF = premierePF;
		this.dernierePF = dernierePF;
		this.libelle = libelle;
		this.cantonal = cantonal;
		this.federal = federal;
		this.pourPM = pourPM;
		this.pourAPM = pourAPM;
		this.defaultPM = defaultPM;
		this.defaultAPM = defaultAPM;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public Integer getPremierePeriodeFiscaleValidite() {
		return premierePF;
	}

	@Nullable
	@Override
	public Integer getDernierePeriodeFiscaleValidite() {
		return dernierePF;
	}

	@Override
	public String getLibelle() {
		return libelle;
	}

	@Override
	public boolean isCantonal() {
		return cantonal;
	}

	@Override
	public boolean isFederal() {
		return federal;
	}

	@Override
	public boolean isPourPM() {
		return pourPM;
	}

	@Override
	public boolean isPourAPM() {
		return pourAPM;
	}

	@Override
	public boolean isDefaultPourPM() {
		return defaultPM;
	}

	@Override
	public boolean isDefaultPourAPM() {
		return defaultAPM;
	}

	@Override
	public String toString() {
		return libelle;
	}
}
