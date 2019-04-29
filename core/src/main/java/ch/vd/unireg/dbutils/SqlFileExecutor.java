package ch.vd.unireg.dbutils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class SqlFileExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFileExecutor.class);

    public static void execute(PlatformTransactionManager transactionManager, DataSource ds, final String fileResource) throws Exception {

        final JdbcTemplate template = new JdbcTemplate(ds);
        template.setIgnoreWarnings(false);

        TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
        tmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        tmpl.execute(status -> {
	        try {
		        final InputStream sqlFile = SqlFileExecutor.class.getResourceAsStream(fileResource);
		        try (InputStreamReader isr = new InputStreamReader(sqlFile); BufferedReader input = new BufferedReader(isr)) {
			        String statStr;
			        while ((statStr = input.readLine()) != null) {

				        if (!statStr.isEmpty() && !statStr.startsWith("#") && !statStr.startsWith("--")) {

					        if (statStr.endsWith(";")) {
						        statStr = statStr.substring(0, statStr.length() - 1);
					        }

					        if (LOGGER.isTraceEnabled()) {
						        LOGGER.trace("SQL: " + statStr);
					        }
					        template.execute(statStr);
				        }
			        }
		        }
	        }
	        catch (IOException e) {
		        LOGGER.error(e.getMessage(), e);
		        throw new RuntimeException(e);
	        }
	        return null;
        });
    }

}
