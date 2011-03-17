package ch.vd.uniregctb.dbunit;

import java.util.HashMap;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTableIterator;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;

/**
 * Wrapper qui expose un dataset en ajoutant la colonne PP_Habitant et en modifiant la valeur de TIERS_TYPE.
 */
public class ModifPPDataSet implements IDataSet {

	private final IDataSet dataSet;
	private final String addColumnName = "PP_HABITANT";
	private final DataType addColumnType = DataType.BOOLEAN;
	private final String modifColumnName = "TIERS_TYPE";
	private final String oldColumnName = "NH_DATE_DECES";
	private final String newColumnName = "DATE_DECES";
	private final String tableName = "TIERS";
	private HashMap<String, ITable> tablesByName;
	private ITable[] tables;
	
	public ModifPPDataSet(IDataSet dataSet) {
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
			ITable table = null;
			if (name.equalsIgnoreCase(tableName)) {
				table = new ModifPPTable(dataSet.getTable(name), addColumnName, addColumnType, modifColumnName, oldColumnName, newColumnName);
			}
			else {
				table = dataSet.getTable(name);
			}
			tablesByName.put(name, table);
			tables[i] = table;
		}
	}
	
	public ITable getTable(String tableName) throws DataSetException {
		if (tablesByName == null) {
			initTables();
		}
		return dataSet.getTable(tableName);
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
