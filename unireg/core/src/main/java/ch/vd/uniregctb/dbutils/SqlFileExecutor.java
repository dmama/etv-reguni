package ch.vd.uniregctb.dbutils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class SqlFileExecutor {

    private final static Logger LOGGER = Logger.getLogger(SqlFileExecutor.class);

    public static void execute(PlatformTransactionManager transactionManager, DataSource ds, final String fileResource) throws Exception {

        final JdbcTemplate template = new JdbcTemplate(ds);
        template.setIgnoreWarnings(false);

        TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
        tmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        tmpl.execute(new TransactionCallback<Object>() {

            @Override
            public Object doInTransaction(TransactionStatus status) {

                try {
                    InputStream sqlFile = SqlFileExecutor.class.getResourceAsStream(fileResource);
                    BufferedReader input = new BufferedReader( new InputStreamReader(sqlFile) );
                    String statStr;
                    while ((statStr = input.readLine()) != null) {

                        if (!statStr.isEmpty() && !statStr.startsWith("#") && !statStr.startsWith("--")) {

                            if (statStr.endsWith(";")) {
                                statStr = statStr.substring(0, statStr.length()-1);
                            }

                            if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("SQL: " + statStr);
							}
                            template.execute(statStr);
                        }
                    }

                    // For BP
                    sqlFile = null;
                }
                catch (IOException e) {
                	LOGGER.error(e, e);
                    throw new RuntimeException(e);
                }
                return null;
            }

        });
    }

}
