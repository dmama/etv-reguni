package ch.vd.uniregctb.migration.pm.engine;

import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.log.LoggedMessages;

public class MigrationException extends Exception {

	private final String graphe;
	private final LoggedMessages messages;

	public MigrationException(Throwable cause, Graphe graphe, LoggedMessages messages) {
		super(cause);
		this.graphe = graphe.toString();
		this.messages = messages;
	}

	@Override
	public String getMessage() {
		return String.format("Dans le traitement du graphe %s\nMessages collectés jusque là :\n%s\n", graphe, messages);
	}
}
