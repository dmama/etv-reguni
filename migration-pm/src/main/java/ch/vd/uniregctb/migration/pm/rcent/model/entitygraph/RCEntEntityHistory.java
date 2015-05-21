package ch.vd.uniregctb.migration.pm.rcent.model.entitygraph;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntHistoryElement;
import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntHistoryList;

/**
 * Behaviour allowing the retrieval of an RCEnt entity's content.
 */
public interface RCEntEntityHistory {

	@NotNull
	RCEntHistoryList<? extends RCEntHistoryElement> getHistory();

	/**
	 * @return the current value of the entity, if exist.
	 */
	Object getCurrent();
}
