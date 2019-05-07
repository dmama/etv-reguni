package ch.vd.unireg.dbutils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class SqlFileExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SqlFileExecutor.class);

	/**
	 * Exécute les statements SQL contenus dans un fichier
	 *
	 * @param transactionManager le transaction manager
	 * @param ds                 la data-source
	 * @param fileResource       le chemin vers le fichier qui contient les statements à exécuter
	 */
	public static void execute(PlatformTransactionManager transactionManager, DataSource ds, final String fileResource) throws IOException {

		// on lit le fichier
		final List<String> lines = new ArrayList<>();
		try (final InputStream stream = SqlFileExecutor.class.getResourceAsStream(fileResource);
		     final InputStreamReader reader = new InputStreamReader(stream);
		     final BufferedReader input = new BufferedReader(reader)) {

			String line;
			while ((line = input.readLine()) != null) {
				lines.add(line);
			}
		}

		// on exécute les statements
		execute(transactionManager, ds, lines);
	}

	/**
	 * Exécute les statements SQL fournis par un itérateur
	 *
	 * @param transactionManager le transaction manager
	 * @param ds                 la data-source
	 * @param statements         une liste de statements
	 */
	public static void execute(PlatformTransactionManager transactionManager, DataSource ds, final List<String> statements) {

		final JdbcTemplate template = new JdbcTemplate(ds);
		template.setIgnoreWarnings(false);

		final TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
		tmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		tmpl.execute(status -> {
			for (String statement : statements) {

				if (statement.isEmpty() || statement.startsWith("#") || statement.startsWith("--")) {
					continue;
				}

				if (statement.endsWith(";")) {
					statement = statement.substring(0, statement.length() - 1);
				}

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("SQL: " + statement);
				}
				try {
					template.execute(statement);
				}
				catch (Exception e) {
					LOGGER.warn("Exception lors de l'exécution du statement = [" + statement + "]");
					throw e;
				}
			}
			return null;
		});
	}

}
