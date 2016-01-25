package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFormeJuridique;
import ch.vd.uniregctb.migration.pm.utils.OrganisationDataHelper;

public class DifferencesDonneesCivilesLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.RAISON_SOCIALE_REGPM,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE_RCENT,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE_DIFF_FLAG,
	                                                                                                     LoggedElementAttribute.FORME_JURIDIQUE_REGPM,
	                                                                                                     LoggedElementAttribute.FORME_JURIDIQUE_RCENT,
	                                                                                                     LoggedElementAttribute.FORME_JURIDIQUE_DIFF_FLAG,
	                                                                                                     LoggedElementAttribute.IDE_REGPM,
	                                                                                                     LoggedElementAttribute.IDE_RCENT,
	                                                                                                     LoggedElementAttribute.IDE_DIFF_FLAG));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public DifferencesDonneesCivilesLoggedElement(RegpmEntreprise regpm, Organisation rcent, boolean raisonSocialeDifferente, boolean formeJuridiqueDifferente, boolean ideDifferent) {
		this.values = buildItemValues(regpm, rcent, raisonSocialeDifferente, formeJuridiqueDifferente, ideDifferent);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(RegpmEntreprise regpm, Organisation rcent, boolean raisonSocialeDifferente, boolean formeJuridiqueDifferente, boolean ideDifferent) {
		final String raisonSocialeRegpm = Stream.of(regpm.getRaisonSociale1(), regpm.getRaisonSociale2(), regpm.getRaisonSociale3())
				.filter(Objects::nonNull)
				.collect(Collectors.joining(" "));
		final String formeJuridiqueRegpm = regpm.getFormesJuridiques().stream()
				.max(Comparator.naturalOrder())
				.map(RegpmFormeJuridique::getType)
				.map(RegpmTypeFormeJuridique::getCode)
				.orElse(null);

		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_REGPM, raisonSocialeRegpm);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_RCENT, OrganisationDataHelper.getLastValue(rcent.getNom()));
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_DIFF_FLAG, raisonSocialeDifferente);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_REGPM, formeJuridiqueRegpm);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_RCENT, OrganisationDataHelper.getLastValue(rcent.getFormeLegale()));
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_DIFF_FLAG, formeJuridiqueDifferente);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.IDE_REGPM, regpm.getNumeroIDE());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.IDE_RCENT, OrganisationDataHelper.getLastValue(rcent.getNumeroIDE()));
		LoggedElementHelper.addValue(map, LoggedElementAttribute.IDE_DIFF_FLAG, ideDifferent);
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
