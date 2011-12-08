package ch.vd.uniregctb.dbunit;

import java.util.HashMap;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTableIterator;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;

/**
 * Wrapper qui expose un dataset en modifiant les valeurs de la table ADRESSE_TIERS à la volée.
 */
public class ModifAdresseDataSet implements IDataSet {

	private final IDataSet dataSet;
	private HashMap<String, ITable> tablesByName;
	private ITable[] tables;

	public ModifAdresseDataSet(IDataSet dataSet) {
		this.dataSet = dataSet;
		this.tablesByName = null;
		this.tables = null;
	}

	private void initTables() throws DataSetException {

		final String[] names = dataSet.getTableNames();

		tablesByName = new HashMap<String, ITable>();
		tables = new ITable[names.length];

		for (int i = 0; i < names.length; ++i) {
			final String name = names[i];
			ITable table;
			if (name.equalsIgnoreCase("ADRESSE_TIERS")) {
				table = new ModifAdresseTable(dataSet.getTable(name));
			}
			else {
				table = dataSet.getTable(name);
			}
			tablesByName.put(name, table);
			tables[i] = table;
		}
	}
	
	@Override
	public ITable getTable(String tableName) throws DataSetException {
		if (tablesByName == null) {
			initTables();
		}
		return dataSet.getTable(tableName);
	}

	@Override
	public ITableMetaData getTableMetaData(String tableName) throws DataSetException {
		return getTable(tableName).getTableMetaData();
	}

	@Override
	public String[] getTableNames() throws DataSetException {
		return dataSet.getTableNames();
	}

	@Override
	public ITable[] getTables() throws DataSetException {
		if (tables == null) {
			initTables();
		}
		return tables;
	}

	@Override
	public ITableIterator iterator() throws DataSetException {
		if (tables == null) {
			initTables();
		}
		return new DefaultTableIterator(tables);
	}

	@Override
	public ITableIterator reverseIterator() throws DataSetException {
		if (tables == null) {
			initTables();
		}
		return new DefaultTableIterator(tables, true);
	}
}
