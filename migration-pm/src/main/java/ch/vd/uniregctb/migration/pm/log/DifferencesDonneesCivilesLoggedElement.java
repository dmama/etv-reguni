package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.migration.pm.engine.data.CommuneOuPays;

public class DifferencesDonneesCivilesLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.RAISON_SOCIALE_REGPM,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE_RCENT,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE_DIFF_FLAG,
	                                                                                                     LoggedElementAttribute.FORME_JURIDIQUE_REGPM,
	                                                                                                     LoggedElementAttribute.FORME_JURIDIQUE_RCENT,
	                                                                                                     LoggedElementAttribute.FORME_JURIDIQUE_DIFF_FLAG,
	                                                                                                     LoggedElementAttribute.IDE_REGPM,
	                                                                                                     LoggedElementAttribute.IDE_RCENT,
	                                                                                                     LoggedElementAttribute.IDE_DIFF_FLAG,
	                                                                                                     LoggedElementAttribute.SIEGE_REGPM,
	                                                                                                     LoggedElementAttribute.SIEGE_RCENT,
	                                                                                                     LoggedElementAttribute.SIEGE_DIFF_FLAG));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public DifferencesDonneesCivilesLoggedElement(String regpmRaisonSociale, String rcentRaisonSociale, boolean raisonSocialeDifferente,
	                                              String regpmFormeJuridique, FormeLegale rcentFormeJuridique, boolean formeJuridiqueDifferente,
	                                              String regpmNumeroIde, String rcentNumeroIde, boolean ideDifferent,
	                                              CommuneOuPays regpmSiege, CommuneOuPays rcentSiege, boolean siegeDifferent) {

		this.values = buildItemValues(regpmRaisonSociale, rcentRaisonSociale, raisonSocialeDifferente,
		                              regpmFormeJuridique, rcentFormeJuridique, formeJuridiqueDifferente,
		                              regpmNumeroIde, rcentNumeroIde, ideDifferent,
		                              regpmSiege, rcentSiege, siegeDifferent);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(String regpmRaisonSociale, String rcentRaisonSociale, boolean raisonSocialeDifferente,
	                                                                   String regpmFormeJuridique, FormeLegale rcentFormeJuridique, boolean formeJuridiqueDifferente,
	                                                                   String regpmNumeroIde, String rcentNumeroIde, boolean ideDifferent,
	                                                                   CommuneOuPays regpmSiege, CommuneOuPays rcentSiege, boolean siegeDifferent) {

		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_REGPM, regpmRaisonSociale);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_RCENT, rcentRaisonSociale);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_DIFF_FLAG, raisonSocialeDifferente);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_REGPM, regpmFormeJuridique);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_RCENT, rcentFormeJuridique);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_DIFF_FLAG, formeJuridiqueDifferente);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.IDE_REGPM, regpmNumeroIde);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.IDE_RCENT, rcentNumeroIde);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.IDE_DIFF_FLAG, ideDifferent);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.SIEGE_REGPM, regpmSiege != null ? regpmSiege.toString() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.SIEGE_RCENT, rcentSiege != null ? rcentSiege.toString() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.SIEGE_DIFF_FLAG, siegeDifferent);
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
