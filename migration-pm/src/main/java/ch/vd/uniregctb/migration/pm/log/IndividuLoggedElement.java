package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;

public class IndividuLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.INDIVIDU_ID,
	                                                                                                     LoggedElementAttribute.INDIVIDU_ID_UNIREG,
	                                                                                                     LoggedElementAttribute.INDIVIDU_NOM,
	                                                                                                     LoggedElementAttribute.INDIVIDU_PRENOM,
	                                                                                                     LoggedElementAttribute.INDIVIDU_SEXE,
	                                                                                                     LoggedElementAttribute.INDIVIDU_DATE_NAISSANCE));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final RegpmIndividu individu;
	private final IdMapping idMapper;

	public IndividuLoggedElement(RegpmIndividu individu, IdMapping idMapper) {
		this.individu = individu;
		this.idMapper = idMapper;
	}

	@Nullable
	private Long extractNumeroUniregIfAvailable() {
		if (idMapper.hasMappingForIndividu(individu.getId())) {
			return idMapper.getIdUniregIndividu(individu.getId());
		}
		return null;
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return NAMES;
	}

	/**
	 * Ici, on ne peut pas faire de cache, parce que le migrateur d'individus a tendance à envoyer pas mal
	 * de logs avant même de créer la personne physique (ou de savoir avec laquelle il faut se rattacher...)
	 */
	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_ID, individu.getId());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_ID_UNIREG, extractNumeroUniregIfAvailable());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_NOM, individu.getNom());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_PRENOM, individu.getPrenom());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_SEXE, individu.getSexe());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_DATE_NAISSANCE, individu.getDateNaissance());
		return Collections.unmodifiableMap(map);
	}
}
