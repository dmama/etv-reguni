package ch.vd.uniregctb.dbunit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;

/**
 * Wrapper qui ajoute les colonnes AUTRE_TYPE et TYPE_PM et supprime la colonne TYPE de la table ADRESSE_TIERS
 */
public class ModifAdresseTable implements ITable {

	private final ITable table;
	private final DuplicateTypeColumnTableMetaData duplicateTypeColumnMetaData;

	public ModifAdresseTable(ITable table){
		this.table = table;
		this.duplicateTypeColumnMetaData = new DuplicateTypeColumnTableMetaData(table.getTableMetaData());
	}
	
	@Override
	public int getRowCount() {
		return table.getRowCount();
	}

	@Override
	public ITableMetaData getTableMetaData() {
		return duplicateTypeColumnMetaData;
	}

	@Override
	public Object getValue(int row, String column) throws DataSetException {
		final String type = (String)table.getValue(row, "ADR_TYPE");

		if ("AUTRE_TYPE".equals(column)) {
			if ("AdresseAutreTiers".equals(type)) {
				return table.getValue(row, "TYPE");
			}
			else {
				return null;
			}
		}
		else if ("TYPE_PM".equals(column)) {
			if ("AdressePM".equals(type)) {
				return table.getValue(row, "TYPE");
			}
			else {
				return null;
			}
		}
		else {
			return table.getValue(row, column);
		}
	}

	private static class DuplicateTypeColumnTableMetaData implements ITableMetaData {

		private final ITableMetaData metaData;
		private Column[] columns;
		
		public DuplicateTypeColumnTableMetaData(ITableMetaData metaData) {
			this.metaData = metaData;
			this.columns = null;
		}
		
		@Override
		public Column[] getColumns() throws DataSetException {
			if (columns == null) {
				columns = addColumns(metaData);
			}
			return columns;
		}
		
		private Column[] addColumns(ITableMetaData metaData) throws DataSetException {
			List<Column> l = new ArrayList<Column>(Arrays.asList(metaData.getColumns()));
			for(Column col : l) {
				if (col.getColumnName().equals("TYPE")) {
					l.remove(col);
					l.add(new Column("AUTRE_TYPE", col.getDataType()));
					l.add(new Column("TYPE_PM", col.getDataType()));
					break;
				}
			}
			return l.toArray(new Column[] {});
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
