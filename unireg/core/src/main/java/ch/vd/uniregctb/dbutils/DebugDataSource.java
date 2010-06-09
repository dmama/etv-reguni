package ch.vd.uniregctb.dbutils;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * Datasource de débugging qui permet de dumper les stack-traces lorsque un nombre maximum de connections a été atteint.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DebugDataSource implements DataSource {

	private static final Logger LOGGER = Logger.getLogger(DebugDataSource.class);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

	private DataSource target;
	private int maxConnections;
	private Date lastDump;
	private boolean enabled;

	private final Set<DebugConnection> connections = new HashSet<DebugConnection>();

	/**
	 * @param target
	 *            La datasource réelle à débugger
	 */
	public void setTarget(DataSource target) {
		this.target = target;
	}

	/**
	 * @param enabled
	 *            <i>vrai</i> pour activer le débugging; <i>faux</i> pour simplement déléguer les appels à la target.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection() throws SQLException {
		if (enabled) {
			synchronized (connections) {
				if (connections.size() >= maxConnections) {
					dumpConnections();
				}
			}
			return new DebugConnection(this, target.getConnection());
		}
		else {
			return target.getConnection();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection(String username, String password) throws SQLException {
		if (enabled) {
			synchronized (connections) {
				if (connections.size() >= maxConnections) {
					dumpConnections();
				}
			}
			return new DebugConnection(this, target.getConnection(username, password));
		}
		else {
			return target.getConnection(username, password);
		}
	}

	/**
	 * Méthode appelée par les connexions de debug lors de l'ouverture de la connexion.
	 *
	 * @param c la connection à enregister
	 */
	protected void register(DebugConnection c) {
		synchronized (connections) {
			connections.add(c);
		}
	}

	/**
	 * Méthode appelée par les connexions de debug lors de la fermeture de la connexion.
	 *
	 * @param c la connection à désenregister
	 */
	protected void unregister(DebugConnection c) {
		synchronized (connections) {
			connections.remove(c);
		}
	}

	/**
	 * Affiche toutes les connexions ouvertes en imprimant la callstack de l'appel d'ouverture des connexions.
	 */
	private void dumpConnections() {
		final Date now = new Date();
		if (lastDump == null || now.getTime() - lastDump.getTime() > 300000) { // on log au maximum toutes les 5 minutes
			StringBuilder s = new StringBuilder();
			s.append("Le nombre maximal (").append(maxConnections).append(
					") de connexions est atteint. Les connexions ont été allouées aux endroits suivants :\n");
			int i = 0;
			for (DebugConnection c : connections) {
				if (c == null) {
					continue;
				}
				final String openingDate = DATE_FORMAT.format(c.getOpeningDate());
				final long openDuration = (now.getTime() - c.getOpeningDate().getTime()) / 1000; // en secondes
				s.append(String.format(" --- connection #%d (%d) opened at %s (%d seconds ago) ---\n", i++, c.hashCode(), openingDate, openDuration));
				s.append(c.getCallerTrace()).append("\n");
			}
			s.append(" ---------------------------");
			LOGGER.warn(s.toString());
			lastDump = now;
		}
		else {
			LOGGER.warn("Nombre maximal de connexions atteint.");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public PrintWriter getLogWriter() throws SQLException {
		return target.getLogWriter();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getLoginTimeout() throws SQLException {
		return target.getLoginTimeout();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLogWriter(PrintWriter out) throws SQLException {
		target.setLogWriter(out);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLoginTimeout(int seconds) throws SQLException {
		target.setLoginTimeout(seconds);
	}
}
