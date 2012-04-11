package ch.vd.uniregctb.dbunit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;

/**
 * Wrapper qui expose une table en ajoutant une colonne et en modifiant la valeur d'une colonne.
 */
public class ModifPPTable implements ITable {

	private final ITable table;
	private final String addColumnName;
	private final String modifColumnName;
	private final String oldColumnName;
	private final String newColumnName;
	private final ModifPPTable.AddColumnTableMetaData addColumnMetaData;
	
	public ModifPPTable(ITable table, String addColumnName, DataType addColumnType, String modifColumnName, String oldColumnName, String newColumnName){
		this.table = table;
		this.addColumnName = addColumnName;
		this.modifColumnName = modifColumnName;
		this.oldColumnName = oldColumnName;
		this.newColumnName = newColumnName;
		this.addColumnMetaData = new AddColumnTableMetaData(table.getTableMetaData(), addColumnName, addColumnType, oldColumnName, newColumnName);
	}
	
	@Override
	public int getRowCount() {
		return table.getRowCount();
	}

	@Override
	public ITableMetaData getTableMetaData() {
		return addColumnMetaData;
	}

	@Override
	public Object getValue(int row, String column) throws DataSetException {
		if (addColumnName.equals(column)) {
			if (table.getValue(row, modifColumnName).equals("Habitant")) {
				return 1;
			}
			else if (table.getValue(row, modifColumnName).equals("NonHabitant")) {
				return 0;
			}
			else {
				return null;
			}
		}
		if (modifColumnName.equals(column)) {
			if (table.getValue(row, column).equals("Habitant") || table.getValue(row, column).equals("NonHabitant")) {
				return "PersonnePhysique";
			}
			return table.getValue(row, column);
		}
		if (newColumnName.equals(column)) {
			return table.getValue(row, oldColumnName);
		}
		return table.getValue(row, column);
	}

	private static class AddColumnTableMetaData implements ITableMetaData {

		private final ITableMetaData metaData;
		private final String addColumnName;
		private final DataType addColumnType;
		private final String oldColumnName;
		private final String newColumnName;
		private Column[] columns;
		
		public AddColumnTableMetaData(ITableMetaData metaData, String addColumnName, DataType addColumnType, String oldColumnName, String newColumnName) {
			this.metaData = metaData;
			this.addColumnName = addColumnName;
			this.addColumnType = addColumnType;
			this.oldColumnName = oldColumnName;
			this.newColumnName = newColumnName;
			this.columns = null;
		}
		
		@Override
		public Column[] getColumns() throws DataSetException {
			if (columns == null) {
				columns = addColumns(metaData, addColumnName, addColumnType);
			}
			return columns;
		}
		
		private Column[] addColumns(ITableMetaData metaData, String addColumnName, DataType addColumnType) throws DataSetException {
			List<Column> l = new ArrayList<Column>(Arrays.asList(metaData.getColumns()));
			for(Column col : l) {
				if (col.getColumnName().equals(oldColumnName)) {
					Column newColumn = new Column(newColumnName, col.getDataType());
					l.remove(col);
					l.add(newColumn);
					break;
				}
			}
			Column addColumn = new Column(addColumnName, addColumnType);
			l.add(addColumn);
			return l.toArray(new Column[l.size()]);
		}

		@Override
		public Column[] getPrimaryKeys() throws DataSetException {
			return metaData.getPrimaryKeys();
		}

		@Override
		public String getTableName() {
			return metaData.getTableName();
		}
		
	}
}
