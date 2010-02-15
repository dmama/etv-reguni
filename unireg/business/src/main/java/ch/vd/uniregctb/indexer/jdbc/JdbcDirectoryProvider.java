package ch.vd.uniregctb.indexer.jdbc;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.indexer.Directory;
import ch.vd.uniregctb.indexer.DirectoryProvider;
import ch.vd.uniregctb.indexer.IndexerException;
import org.apache.log4j.Logger;

import javax.sql.DataSource;

public class JdbcDirectoryProvider extends DirectoryProvider {

	private static final Logger LOGGER = Logger.getLogger(JdbcDirectoryProvider.class);

	private DataSource dataSource;
	private String tableName;

	public JdbcDirectoryProvider(DataSource ds, String name) throws Exception {
		dataSource = ds;
		tableName = name;

		LOGGER.info("L'index Lucene est sauvé dans une base JDBC dans la table " + tableName);
	}

	@Override
	public Directory getNewDirectory() throws Exception {

		MyJdbcDirectory dir = new MyJdbcDirectory(dataSource, tableName);
		Assert.isTrue(dir.tableExists(), "La table de l'indexer (" + tableName + ") doit exister!");
		dir.deleteMarkDeleted();

		return new Directory(dir);
	}

	@Override
	public String getIndexPath() throws IndexerException {
		return "<JDBC:" + tableName + ">";
	}
}
