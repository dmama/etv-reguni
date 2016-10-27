package ch.vd.unireg.interfaces.infra.mock;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.PlagePeriodesFiscales;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public class MockTypeRegimeFiscal implements TypeRegimeFiscal, Serializable {

	private static final long serialVersionUID = 8537806759331758822L;

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
	private final List<PlagePeriodesFiscales> exonerations;

	public static final MockTypeRegimeFiscal ORDINAIRE_PM = new MockTypeRegimeFiscal("01", 1994, null, "Ordinaire", true, true, true, false, true, false);
	public static final MockTypeRegimeFiscal PARTICIPATIONS = new MockTypeRegimeFiscal("11", 1994, null, "Société de participations", true, false, true, false, false, false);
	public static final MockTypeRegimeFiscal PARTICIPATIONS_PART_IMPOSABLE = new MockTypeRegimeFiscal("12", 2001, null, "Société de participations, part imposable", true, false, true, false, false, false);
	public static final MockTypeRegimeFiscal COMMUNAUTE_PERSONNES_ETRANGERES_PM = new MockTypeRegimeFiscal("13", 2016, null, "Communauté de personnes étrangères - assimilé PM", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal FONDS_PLACEMENT = new MockTypeRegimeFiscal("50", 1992, null, "Placement collectif avec immeuble(s)", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal TRANSPORT_CONCESSIONNE = new MockTypeRegimeFiscal("60", 1994, null, "Transport concessionné", true, true, true, false, false, false);
	public static final MockTypeRegimeFiscal ORDINAIRE_APM = new MockTypeRegimeFiscal("70", 1995, null, "Ordinaire Assoc-Fond.", true, true, false, true, false, true);
	public static final MockTypeRegimeFiscal COMMUNAUTE_PERSONNES_ETRANGERES_APM = new MockTypeRegimeFiscal("71", 2016, null, "Communauté de personnes étrangères - assimilé APM", true, true, false, true, false, false);
	public static final MockTypeRegimeFiscal EXO_90G = new MockTypeRegimeFiscal("109", 2003, null, "PM avec exonération (Art. 90g LI)", true, true, true, false, false, false, new PlagePeriodesFiscales(2003, null));
	public static final MockTypeRegimeFiscal EXO_90CEFH = new MockTypeRegimeFiscal("190", 2003, null, "PM avec exonération (Art. 90cefh LI)", true, true, true, false, false, false, new PlagePeriodesFiscales(2003, null));
	public static final MockTypeRegimeFiscal ART90G = new MockTypeRegimeFiscal("709", 1994, null, "Pure utilité publique (Art. 90 let g LI 2001)", true, true, false, true, false, false, new PlagePeriodesFiscales(1994, null));
	public static final MockTypeRegimeFiscal ART90D = new MockTypeRegimeFiscal("715", 2001, null, "Fondation ecclésiastique (Art. 90 let d LI 2001)", true, true, false, true, false, false, new PlagePeriodesFiscales(2001, null));
	public static final MockTypeRegimeFiscal ART90H = new MockTypeRegimeFiscal("719", 1994, null, "Buts culturels (Art. 90 let h LI 2001)", true, true, false, true, false, false, new PlagePeriodesFiscales(1994, null));
	public static final MockTypeRegimeFiscal ART90E = new MockTypeRegimeFiscal("729", 1994, null, "Institutions de prévoyance (Art. 90 let e LI 2001)", true, true, false, true, false, false, new PlagePeriodesFiscales(1994, null));
	public static final MockTypeRegimeFiscal ART90F = new MockTypeRegimeFiscal("739", 2001, null, "Caisses assurances sociales (Art 90 let f LI 2001)", true, true, false, true, false, false, new PlagePeriodesFiscales(2001, null));
	public static final MockTypeRegimeFiscal ART90AI = new MockTypeRegimeFiscal("749", 2001, null, "Confédération + Etats étrangers 90a et i", true, true, false, true, false, false, new PlagePeriodesFiscales(2001, null));
	public static final MockTypeRegimeFiscal ART90B = new MockTypeRegimeFiscal("759", 2001, null, "Canton + établiss. (Art. 90 let b LI 2001)", true, true, false, true, false, false, new PlagePeriodesFiscales(2001, null));
	public static final MockTypeRegimeFiscal ART90C = new MockTypeRegimeFiscal("769", 2001, null, "Communes + établiss. (Art. 90 let c LI 2001)", true, true, false, true, false, false, new PlagePeriodesFiscales(2001, null));
	public static final MockTypeRegimeFiscal ART90J = new MockTypeRegimeFiscal("779", 1992, null, "Placement collectif exonéré (Art. 90j LI)", true, true, false, true, false, false, new PlagePeriodesFiscales(1992, null));
	public static final MockTypeRegimeFiscal STE_BASE_MIXTE = new MockTypeRegimeFiscal("41C", 2001, null, "Société de base (mixte)", true, false, true, false, false, false);
	public static final MockTypeRegimeFiscal STE_DOMICILE = new MockTypeRegimeFiscal("42C", 2001, null, "Société de domicile", true, false, true, false, false, false);

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
		checkOneAndOnlyOne(mocks, MockTypeRegimeFiscal::isDefaultPourPM);
		checkOneAndOnlyOne(mocks, MockTypeRegimeFiscal::isDefaultPourAPM);

		return mocks.toArray(new MockTypeRegimeFiscal[mocks.size()]);
	}

	private static <T extends TypeRegimeFiscal> void checkOneAndOnlyOne(List<T> elements, Predicate<? super T> predicate) {
		final long nbMatching = elements.stream()
				.filter(predicate)
				.count();
		if (nbMatching > 1) {
			// plusieurs ?
			throw new IllegalArgumentException("Plus d'un élément (= " + nbMatching + ") satisfait au prédicat");
		}
		else if (nbMatching == 0) {
			// aucun ??
			throw new IllegalArgumentException("Aucun élément ne satisfait au prédicat...");
		}
	}

	private MockTypeRegimeFiscal(String code, Integer premierePF, Integer dernierePF, String libelle, boolean cantonal, boolean federal, boolean pourPM, boolean pourAPM, boolean defaultPM, boolean defaultAPM, PlagePeriodesFiscales... exonerations) {
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

		if (exonerations == null || exonerations.length == 0) {
			this.exonerations = Collections.emptyList();
		}
		else {
			this.exonerations = Stream.of(exonerations)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		}
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
	public boolean isExoneration(int periodeFiscale) {
		return exonerations.stream()
				.filter(exo -> exo.isDansPlage(periodeFiscale))
				.findFirst()
				.isPresent();
	}

	@Override
	public String toString() {
		return libelle;
	}
}
