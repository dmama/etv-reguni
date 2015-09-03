package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;

public class RapportEntreTiersLoggedElement<S extends Tiers, D extends Tiers, R extends RapportEntreTiers> implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.RET_TYPE,
	                                                                                                     LoggedElementAttribute.RET_DATE_DEBUT,
	                                                                                                     LoggedElementAttribute.RET_DATE_FIN,
	                                                                                                     LoggedElementAttribute.RET_SRC_ENTREPRISE_ID,
	                                                                                                     LoggedElementAttribute.RET_SRC_ETABLISSEMENT_ID,
	                                                                                                     LoggedElementAttribute.RET_SRC_INDIVIDU_ID,
	                                                                                                     LoggedElementAttribute.RET_SRC_UNIREG_ID,
	                                                                                                     LoggedElementAttribute.RET_DEST_ENTREPRISE_ID,
	                                                                                                     LoggedElementAttribute.RET_DEST_ETABLISSEMENT_ID,
	                                                                                                     LoggedElementAttribute.RET_DEST_INDIVIDU_ID,
	                                                                                                     LoggedElementAttribute.RET_DEST_UNIREG_ID));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final EntityLinkCollector.EntityLink<S, D, R> link;

	public RapportEntreTiersLoggedElement(EntityLinkCollector.EntityLink<S, D, R> link) {
		this.link = link;
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return NAMES;
	}

	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_TYPE, link.getType());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DATE_DEBUT, link.getDateDebut());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DATE_FIN, link.getDateFin());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_ENTREPRISE_ID, link.getSourceKey() != null && link.getSourceKey().getType() == EntityKey.Type.ENTREPRISE ? link.getSourceKey().getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_ETABLISSEMENT_ID, link.getSourceKey() != null && link.getSourceKey().getType() == EntityKey.Type.ETABLISSEMENT ? link.getSourceKey().getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_INDIVIDU_ID, link.getSourceKey() != null && link.getSourceKey().getType() == EntityKey.Type.INDIVIDU ? link.getSourceKey().getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_UNIREG_ID, link.resolveSource().getId());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_ENTREPRISE_ID, link.getDestinationKey() != null && link.getDestinationKey().getType() == EntityKey.Type.ENTREPRISE ? link.getDestinationKey().getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_ETABLISSEMENT_ID, link.getDestinationKey() != null && link.getDestinationKey().getType() == EntityKey.Type.ETABLISSEMENT ? link.getDestinationKey().getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_INDIVIDU_ID, link.getDestinationKey() != null && link.getDestinationKey().getType() == EntityKey.Type.INDIVIDU ? link.getDestinationKey().getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_UNIREG_ID, link.resolveDestination().getId());
		return Collections.unmodifiableMap(map);
	}
}
