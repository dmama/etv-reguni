package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;

public class EtablissementLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.ETABLISSEMENT_ID,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_ID_UNIREG,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_NO_IDE,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_ID_CANTONAL,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_ENTREPRISE_ID,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_INDIVIDU_ID));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private Map<LoggedElementAttribute, Object> values = null;

	private final RegpmEtablissement etablissement;
	private final IdMapping idMapper;

	public EtablissementLoggedElement(RegpmEtablissement etablissement, IdMapping idMapper) {
		this.etablissement = etablissement;
		this.idMapper = idMapper;
	}

	@NotNull
	private synchronized Map<LoggedElementAttribute, Object> buildItemValues() {
		if (values == null) {
			final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
			LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ID, etablissement.getId());
			LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ID_UNIREG, extractNumeroUniregIfAvailable());
			LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_NO_IDE, etablissement.getNumeroIDE());
			LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ID_CANTONAL, etablissement.getNumeroCantonal());
			if (etablissement.getEntreprise() != null) {
				LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ENTREPRISE_ID, etablissement.getEntreprise().getId());
			}
			else if (etablissement.getIndividu() != null) {
				LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_INDIVIDU_ID, etablissement.getIndividu().getId());
			}
			return Collections.unmodifiableMap(map);
		}
		return values;
	}

	@Nullable
	private Long extractNumeroUniregIfAvailable() {
		if (idMapper.hasMappingForEtablissement(etablissement.getId())) {
			return idMapper.getIdUniregEtablissement(etablissement.getId());
		}
		return null;
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return NAMES;
	}

	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		if (values == null) {
			values = buildItemValues();
		}
		return values;
	}
}
