package ch.vd.uniregctb.dbunit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.NoSuchColumnException;

/**
 * Wrapper qui expose une table en y supprimant une colonne.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
class DropColumnTable implements ITable {

	private final ITable table;
	private final String dropColumnName;
	private final DropColumnTable.DropColumnTableMetaData dropColumnMetaData;

	private static class DropColumnTableMetaData implements ITableMetaData {

		private final ITableMetaData metaData;
		private final String dropColumnName;
		private Column[] columns;

		public DropColumnTableMetaData(ITableMetaData metaData, String dropColumnName) {
			this.metaData = metaData;
			this.dropColumnName = dropColumnName;
			this.columns = null;
		}

		private Column[] filterColumns(ITableMetaData metaData, String dropColumnName) throws DataSetException {
			List<Column> l = new ArrayList<Column>(Arrays.asList(metaData.getColumns()));
			for (int i = l.size() - 1; i >= 0; --i) {
				if (dropColumnName.equals(l.get(i).getColumnName())) {
					l.remove(i);
				}
			}
			return l.toArray(new Column[] {});
		}

		public Column[] getColumns() throws DataSetException {
			if (columns == null) {
				columns = filterColumns(metaData, dropColumnName);
			}
			return columns;
		}

		public Column[] getPrimaryKeys() throws DataSetException {
			return metaData.getPrimaryKeys();
		}

		public String getTableName() {
			return metaData.getTableName();
		}

	}

	public DropColumnTable(ITable table, String dropColumnName) {
		this.table = table;
		this.dropColumnName = dropColumnName;
		this.dropColumnMetaData = new DropColumnTableMetaData(table.getTableMetaData(), dropColumnName);
	}

	public int getRowCount() {
		return table.getRowCount();
	}

	public ITableMetaData getTableMetaData() {
		return dropColumnMetaData;
	}

	public Object getValue(int row, String column) throws DataSetException {
		if (dropColumnName.equals(column)) {
			throw new NoSuchColumnException(dropColumnMetaData.getTableName() + "." + column);
		}
		return table.getValue(row, column);
	}

}
