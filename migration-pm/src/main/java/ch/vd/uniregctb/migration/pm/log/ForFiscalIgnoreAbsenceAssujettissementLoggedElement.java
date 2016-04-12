package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;

public class ForFiscalIgnoreAbsenceAssujettissementLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.ENTREPRISE_ID,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE,
	                                                                                                     LoggedElementAttribute.TYPE_AUTORITE_FISCALE,
	                                                                                                     LoggedElementAttribute.NO_OFS,
	                                                                                                     LoggedElementAttribute.DATE_DEBUT_FOR,
	                                                                                                     LoggedElementAttribute.DATE_FIN_FOR,
	                                                                                                     LoggedElementAttribute.TYPE_ENTITE,
	                                                                                                     LoggedElementAttribute.MOTIF_RATTACHEMENT));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public ForFiscalIgnoreAbsenceAssujettissementLoggedElement(RegpmEntreprise entreprise, ForFiscal forFiscal) {
		this.values = buildItemValues(entreprise, forFiscal);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(RegpmEntreprise entreprise, ForFiscal forFiscal) {
		// on prend la dernière raison sociale officielle de l'entreprise
		final String raisonSociale = Stream.of(entreprise.getRaisonSociale1(), entreprise.getRaisonSociale2(), entreprise.getRaisonSociale3())
				.filter(Objects::nonNull)
				.collect(Collectors.joining(" "));

		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ENTREPRISE_ID, entreprise.getId());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE, raisonSociale);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.TYPE_AUTORITE_FISCALE, forFiscal.getTypeAutoriteFiscale());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.NO_OFS, forFiscal.getNumeroOfsAutoriteFiscale());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.DATE_DEBUT_FOR, forFiscal.getDateDebut());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.DATE_FIN_FOR, forFiscal.getDateFin());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.TYPE_ENTITE, forFiscal.getClass());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.MOTIF_RATTACHEMENT, forFiscal instanceof ForFiscalRevenuFortune ? ((ForFiscalRevenuFortune) forFiscal).getMotifRattachement() : null);
		return Collections.unmodifiableMap(map);
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return NAMES;
	}

	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		return values;
	}
}
