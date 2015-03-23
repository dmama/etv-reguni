package ch.vd.uniregctb.migration.adresses;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weblogic.jndi.WLInitialContextFactory;

import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.infrastructure.service.ServiceInfrastructureHome;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientImpl;

/**
 * Classe principale pour la migration
 */
public class Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(Job.class);

	/**
	 * Méthode d'entrée du batch
	 * @param args les paramètres de la ligne de commande
	 */
	public static void main(String[] args) throws Exception {

		// le seul paramètre sur la ligne de commande est le chemin vers un fichier de propriétés
		if (args == null || args.length != 1) {
			dumpSyntax();
			return;
		}

		initLog4j();

		// chargement des propriétés
		final String propertiesPath = args[0];
		final File propertiesFile = new File(propertiesPath);
		final Properties props = loadProperties(propertiesFile);
		dumpProperties(props);

		final int nbThreads = Integer.parseInt(props.getProperty("process.nbThreads"));

		// nous avons besoin :
		// 1. un client fidor
		// 2. un client ifoii
		// 3. une connexion DB pour la gestion des adresses
		final FidorClient fidorClient = buildFidorClient(props);
		final ServiceInfrastructure ifoiiClient = buildIfoiiClient(props);
		try (final ConnectionPool connectionPool = buildDbConnectionPool(props)) {

			LOGGER.info("Récupération des identifiants des adresses à migrer...");

			// on récupère d'abord les données des adresses suisses connues dans Unireg
			final List<DataAdresse> adresses = doInNewConnection(connectionPool, new ConnectionCallback<List<DataAdresse>>() {
				@Override
				public List<DataAdresse> doInConnection(Connection con) throws SQLException {
					final String sql = "SELECT ID, DATE_FIN, RUE, NUMERO_ORDRE_POSTE, NUMERO_RUE FROM ADRESSE_TIERS WHERE ADR_TYPE='AdresseSuisse' ORDER BY ID";
					final List<DataAdresse> list = new LinkedList<>();
					try (PreparedStatement st = con.prepareStatement(sql)) {
						try (ResultSet rs = st.executeQuery()) {
							while (rs.next()) {
								final long id = rs.getBigDecimal(1).longValue();
								final Integer indexDateFin = getNullableInt(rs, 2);
								final RegDate dateFin = indexDateFin == null ? null : RegDate.fromIndex(indexDateFin, false);
								final String rue = rs.getString(3);
								final Integer noOrdreP = getNullableInt(rs, 4);
								final Integer noRue = getNullableInt(rs, 5);
								list.add(new DataAdresse(id, dateFin, rue, noOrdreP, noRue));
							}
						}
					}
					return list;
				}
			});

			LOGGER.info("Récupération de " + adresses.size() + " adresses terminée.");
			LOGGER.info("Lancement du traitement sur " + nbThreads + " thread(s).");

			final ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
			try {
				// postage des tâches
				final ExecutorCompletionService<MigrationResult> completionService = new ExecutorCompletionService<>(executor);
				for (DataAdresse adresse : adresses) {
					completionService.submit(new MigrationTask(fidorClient, ifoiiClient, adresse));
				}

				// c'est fini, on n'accepte plus rien
				executor.shutdown();

				// on attend les résultats
				final List<Pair<Long, Map<String, Object>>> resultats = new LinkedList<>();
				int remaining = adresses.size();
				while (remaining > 0) {
					try {
						final Future<MigrationResult> future = completionService.poll(1, TimeUnit.SECONDS);
						if (future != null) {
							--remaining;

							try {
								final MigrationResult res = future.get();
								if (res instanceof MigrationResult.Erreur) {
									LOGGER.error(res.toString(), ((MigrationResult.Erreur) res).e);
								}
								else {
									LOGGER.debug(res.toString());
								}

								final Map<String, Object> sqlActions = res.getFieldModifications();
								if (sqlActions != null && !sqlActions.isEmpty()) {
									resultats.add(Pair.of(res.data.id, sqlActions));
								}
							}
							catch (ExecutionException  e) {
								final Throwable cause = e.getCause();
								throw new RuntimeException(cause);
							}
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}
				}

				// on persiste tout cela en base
				doInNewConnection(connectionPool, new ConnectionCallback<Object>() {
					@Override
					public Object doInConnection(Connection con) throws SQLException {
						for (Pair<Long, Map<String, Object>> action : resultats) {

							final StringBuilder b = new StringBuilder("UPDATE ADRESSE_TIERS SET ");
							boolean first = true;
							for (Map.Entry<String, Object> field : action.getRight().entrySet()) {
								if (!first) {
									b.append(", ");
								}
								b.append(field.getKey());
								if (field.getValue() == null) {
									b.append("=NULL");
								}
								else {
									b.append("=?");
								}
								first = false;
							}
							b.append(" WHERE ID=").append(action.getLeft());
							final String sql = b.toString();

							LOGGER.info(String.format("%s, params = %s", sql, Job.toString(action.getRight())));

							try (PreparedStatement ps = con.prepareStatement(sql)) {
								int index = 1;
								for (Map.Entry<String, Object> field : action.getRight().entrySet()) {
									final Object value = field.getValue();
									if (value != null) {
										if (value instanceof String) {
											ps.setString(index, (String) value);
										}
										else if (value instanceof Integer) {
											ps.setInt(index, (Integer) value);
										}
										else if (value instanceof Long) {
											ps.setLong(index, (Long) value);
										}
										else {
											throw new IllegalArgumentException("What is that??? " + value.getClass());
										}
										++ index;
									}
								}

								final int changed = ps.executeUpdate();
								if (changed != 1) {
									throw new RuntimeException("Adresse ID " + action.getLeft() + " : could not be modified...");
								}
							}
						}
						return null;
					}
				});
			}
			finally {
				LOGGER.info("Arrêt de l'exécuteur.");
				executor.shutdownNow();
				while (!executor.isTerminated()) {
					LOGGER.info("L'exécuteur n'est pas encore arrêté....");
					Thread.sleep(1000);
				}
				LOGGER.info("Fin du programme de migration.");
			}
		}
	}

	private static String toString(Map<String, Object> map) {
		// on ne met que les valeurs non-nulles
		final StringBuilder b = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			final Object value = entry.getValue();
			if (value != null) {
				if (!first) {
					b.append(", ");
				}
				b.append(entry.getKey()).append("=");
				if (value instanceof String) {
					b.append(enquote((String) value));
				}
				else {
					b.append(value);
				}
				first = false;
			}
		}
		b.append("}");
		return b.toString();
	}

	private static Integer getNullableInt(ResultSet rs, int index) throws SQLException {
		final int value = rs.getInt(index);
		return rs.wasNull() ? null : value;
	}

	private static FidorClient buildFidorClient(Properties properties) {
		final String url = properties.getProperty("fidor.url");
		final String user = properties.getProperty("fidor.user");
		final String pwd = properties.getProperty("fidor.password");

		final FidorClientImpl impl = new FidorClientImpl();
		impl.setServiceUrl(url);
		impl.setUsername(user);
		impl.setPassword(pwd);

		final CacheFactory<FidorClient> cacheFactory = new CacheFactory<>(FidorClient.class);
		final TracingFactory<FidorClient> tracingFactory = new TracingFactory<>(FidorClient.class);
		return cacheFactory.buildCache(tracingFactory.buildTracing(LOGGER, impl));
	}

	private static ServiceInfrastructure buildIfoiiClient(Properties properties) throws NamingException, RemoteException, CreateException {
		final String url = properties.getProperty("ifoii.url");
		final String user = properties.getProperty("ifoii.user");
		final String pwd = properties.getProperty("ifoii.password");
		final String jndiName = properties.getProperty("ifoii.jndi");

		final Hashtable<String, String> environnement = new Hashtable<>();
		environnement.put("java.naming.provider.url", url);
		environnement.put("java.naming.security.principal", user);
		environnement.put("java.naming.security.credentials", pwd);

		final WLInitialContextFactory contextFactory = new WLInitialContextFactory();
		final Context ctxt = contextFactory.getInitialContext(environnement);

		final Object found = ctxt.lookup(jndiName);
		final ServiceInfrastructureHome home = (ServiceInfrastructureHome) PortableRemoteObject.narrow(found, ServiceInfrastructureHome.class);

		final TracingFactory<ServiceInfrastructure> tracingFactory = new TracingFactory<>(ServiceInfrastructure.class);
		return tracingFactory.buildTracing(LOGGER, home.create());
	}

	private static interface ConnectionCallback<T> {
		T doInConnection(Connection con) throws SQLException;
	}

	private static <T> T doInNewConnection(ObjectPool<Connection> pool, ConnectionCallback<T> callback) throws SQLException {
		try {
			Connection con = pool.borrowObject();
			try {
				con.setAutoCommit(false);
				try {
					final T result = callback.doInConnection(con);
					con.commit();
					return result;
				}
				catch (RuntimeException | SQLException e) {
					con.rollback();
					throw e;
				}
			}
			finally {
				pool.returnObject(con);
			}
		}
		catch (RuntimeException | SQLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final class ConnectionFactory extends BasePoolableObjectFactory<Connection> {
		private final String dbUrl;
		private final String dbUser;
		private final String dbPwd;

		public ConnectionFactory(String dbUrl, String dbUser, String dbPwd) {
			this.dbUrl = dbUrl;
			this.dbUser = dbUser;
			this.dbPwd = dbPwd;
		}

		@Override
		public Connection makeObject() throws SQLException {
			return DriverManager.getConnection(dbUrl, dbUser, dbPwd);
		}

		@Override
		public void destroyObject(Connection obj) throws SQLException {
			obj.close();
		}
	}

	private static final class ConnectionPool extends GenericObjectPool<Connection> implements AutoCloseable {
		private ConnectionPool(String url, String dbUser, String dbPwd, int size) {
			super(new ConnectionFactory(url, dbUser, dbPwd), size);
		}
	}

	private static ConnectionPool buildDbConnectionPool(Properties properties) throws ClassNotFoundException {
		final String driverClass = properties.getProperty("jdbc.driver.class");
		final String url = properties.getProperty("jdbc.url");
		final String user = properties.getProperty("jdbc.user");
		final String pwd = properties.getProperty("jdbc.password");
		final int poolSize = Integer.parseInt(properties.getProperty("jdbc.pool.size", "10"));

		// chargement du driver JDBC
		Class.forName(driverClass);
		return new ConnectionPool(url, user, pwd, poolSize);
	}

	private static void dumpSyntax() {
		System.err.println("Le job prend un paramètre qui correspond au chemin d'accès au fichier de configuration.");
	}

	private static void dumpProperties(Properties props) {
		final Pattern pwdPattern = Pattern.compile("\\bpassword\\b", Pattern.CASE_INSENSITIVE);
		final List<String> propertyNames = new ArrayList<>(props.stringPropertyNames());
		Collections.sort(propertyNames);
		final StringBuilder b = new StringBuilder("Dump des propriétés du traitement :").append(System.lineSeparator());
		for (String key : propertyNames) {
			final Matcher matcher = pwdPattern.matcher(key);
			final String value;
			if (matcher.find()) {
				value = "********";
			}
			else {
				value = enquote(props.getProperty(key));
			}
			b.append(String.format("\t%s -> %s", key, value)).append(System.lineSeparator());
		}
		LOGGER.info(b.toString());
	}

	private static String enquote(String str) {
		if (str == null) {
			return "null";
		}
		return String.format("'%s'", str);
	}

	private static Properties loadProperties(File file) throws IOException {
		try (InputStream is = new FileInputStream(file); Reader r = new InputStreamReader(is, "UTF-8")) {
			final Properties props = new Properties();
			props.load(r);
			return props;
		}
	}

	/**
	 * Initialise Log4j
	 */
	private static void initLog4j() {
		final Properties properties = new Properties();
		properties.setProperty("log4j.logger.ch.vd.uniregctb", "DEBUG");
		properties.setProperty("log4j.rootLogger", "ERROR, stdout");
		properties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		properties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		properties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%-5.5p [%8.8t] [%d{yyyy-MM-dd HH:mm:ss.SSS}] %m%n");
		PropertyConfigurator.configure(properties);

		// Ces deux classes semblent avoir l'oreille un peu dure...
		java.util.logging.Logger l = java.util.logging.Logger.getLogger("org.apache.cxf.bus.spring.BusApplicationContext");
		l.setLevel(Level.WARNING);
		l = java.util.logging.Logger.getLogger("org.apache.cxf.service.factory.ReflectionServiceFactoryBean");
		l.setLevel(Level.WARNING);
	}
}
