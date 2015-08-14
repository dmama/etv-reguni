package ch.vd.uniregctb.migration.pm.engine;

import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;

public class MigrationException extends Exception {

	private final Graphe graphe;
	private final MigrationResultMessageProvider mr;

	public MigrationException(Throwable cause, Graphe graphe, MigrationResultMessageProvider mr) {
		super(cause);
		this.graphe = graphe;
		this.mr = mr;
	}

	@Override
	public String getMessage() {
		return String.format("Dans le traitement du graphe %s\nMessages collectés jusque là :\n%s\n", graphe, mr.summary());
	}
}
