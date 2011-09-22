package ch.vd.uniregctb.database;

import java.sql.SQLException;

import org.enhydra.jdbc.standard.StandardXADataSource;
import org.tranql.connector.ExceptionSorter;
import org.tranql.connector.jdbc.AbstractXADataSourceMCF;

/**
 * Data-source 'managed' spécialisée pour le testing avec une base de données en mémoire (Hsql, Derby, ...), qui - par définition -
 * possèdent un schéma vide au démarrage.
 * <p>
 * Cette classe reprend le comportement de la classe {@link org.jencks.tranql.DataSourceMCF} en définissant un exception sorter qui ne flag
 * pas comme fatales les exceptions levées par Hibernate lors du drop de la database.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class InMemoryDataSourceMCF extends AbstractXADataSourceMCF<StandardXADataSource> {

	private static final long serialVersionUID = -6861985984667770096L;

	private static final ExceptionSorter exceptionSorter = new ExceptionSorter() {
		@Override
		public boolean isExceptionFatal(Exception e) {
			boolean fatal = true;
			final String message = e.getMessage();
			if (message.endsWith("because it does not exist.")) {
				// la table n'existe pas alors que Hibernate essaie de dropper le schéma (avec Derby)
				fatal = false;
			}
			if (message.startsWith("Table not found:") && message.contains("in statement [alter table")) {
				// la table n'existe pas alors que Hibernate essaie de dropper le schéma (avec Hsql)
				fatal = false;
			}
			if (message.startsWith("Sequence not found in statement [drop sequence")) {
				// la séquence n'existe pas alors que Hibernate essaie de dropper le schéma (avec Hsql)
				fatal = false;
			}
			else if (message.contains("already exists in Schema")) {
				// la séquence/table existe déjà
				fatal = false;
			}
			return fatal;
		}

		@Override
		public boolean rollbackOnFatalException() {
			return true;
		}
	};

	public InMemoryDataSourceMCF() {
		super(new StandardXADataSource(), exceptionSorter);
	}

	/**
	 * @see org.tranql.connector.UserPasswordManagedConnectionFactory#getUserName()
	 */
	@Override
	public String getUserName() {
		return ((StandardXADataSource) xaDataSource).getUser();
	}

	/**
	 * @see org.tranql.connector.UserPasswordManagedConnectionFactory#getPassword()
	 */
	@Override
	public String getPassword() {
		return ((StandardXADataSource) xaDataSource).getPassword();
	}

	/*
	 *
	 */
	public void setDriverName(String driverName) {
		try {
			((StandardXADataSource) xaDataSource).setDriverName(driverName);
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public void setUrl(String url) {
		((StandardXADataSource) xaDataSource).setUrl(url);
	}

	public void setUser(String user) {
		((StandardXADataSource) xaDataSource).setUser(user);
	}

	public void setPassword(String password) {
		((StandardXADataSource) xaDataSource).setPassword(password);
	}
}
