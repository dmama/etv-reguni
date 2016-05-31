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
	private final boolean sourceResolvable;
	private final boolean destinationResolvable;

	public RapportEntreTiersLoggedElement(EntityLinkCollector.EntityLink<S, D, R> link) {
		this(link, true, true);
	}

	public RapportEntreTiersLoggedElement(EntityLinkCollector.EntityLink<S, D, R> link, boolean sourceResolvable, boolean destinationResolvable) {
		this.link = link;
		this.sourceResolvable = sourceResolvable;
		this.destinationResolvable = destinationResolvable;
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

		final EntityKey sourceKey = link.getSourceKey();
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_ENTREPRISE_ID, sourceKey != null && sourceKey.getType() == EntityKey.Type.ENTREPRISE ? sourceKey.getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_ETABLISSEMENT_ID, sourceKey != null && sourceKey.getType() == EntityKey.Type.ETABLISSEMENT ? sourceKey.getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_INDIVIDU_ID, sourceKey != null && sourceKey.getType() == EntityKey.Type.INDIVIDU ? sourceKey.getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_SRC_UNIREG_ID, sourceResolvable ? link.resolveSource().getId() : null);

		final EntityKey destinationKey = link.getDestinationKey();
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_ENTREPRISE_ID, destinationKey != null && destinationKey.getType() == EntityKey.Type.ENTREPRISE ? destinationKey.getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_ETABLISSEMENT_ID, destinationKey != null && destinationKey.getType() == EntityKey.Type.ETABLISSEMENT ? destinationKey.getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_INDIVIDU_ID, destinationKey != null && destinationKey.getType() == EntityKey.Type.INDIVIDU ? destinationKey.getId() : null);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RET_DEST_UNIREG_ID, destinationResolvable ? link.resolveDestination().getId() : null);

		return Collections.unmodifiableMap(map);
	}
}
