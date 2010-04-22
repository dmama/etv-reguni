package ch.vd.uniregctb.dbunit;

import java.util.HashMap;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTableIterator;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;

/**
 * Wrapper qui expose un dataset en supprimant une colonne de toutes les tables.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
class DropColumnDataSet implements IDataSet {

	private final IDataSet dataSet;
	private final String dropColumnName;
	private HashMap<String, ITable> tablesByName;
	private ITable[] tables;

	public DropColumnDataSet(IDataSet dataSet, String dropColumnName) {
		this.dataSet = dataSet;
		this.dropColumnName = dropColumnName;
		this.tablesByName = null;
		this.tables = null;
	}

	public ITable getTable(String tableName) throws DataSetException {
		if (tablesByName == null) {
			initTables();
		}
		return dataSet.getTable(tableName);
	}

	private void initTables() throws DataSetException {

		final String[] names = dataSet.getTableNames();

		tablesByName = new HashMap<String, ITable>();
		tables = new ITable[names.length];

		for (int i = 0; i < names.length; ++i) {
			final String name = names[i];
			ITable table = new DropColumnTable(dataSet.getTable(name), dropColumnName);
			tablesByName.put(name, table);
			tables[i] = table;
		}
	}

	public ITableMetaData getTableMetaData(String tableName) throws DataSetException {
		return getTable(tableName).getTableMetaData();
	}

	public String[] getTableNames() throws DataSetException {
		return dataSet.getTableNames();
	}

	public ITable[] getTables() throws DataSetException {
		if (tables == null) {
			initTables();
		}
		return tables;
	}

	public ITableIterator iterator() throws DataSetException {
		if (tables == null) {
			initTables();
		}
		return new DefaultTableIterator(tables);
	}

	public ITableIterator reverseIterator() throws DataSetException {
		if (tables == null) {
			initTables();
		}
		return new DefaultTableIterator(tables, true);
	}

}
