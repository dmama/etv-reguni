package org.dbunit.dataset;

import ch.vd.uniregctb.common.StatusManager;

/**
 * Dataset qui prend un status manager.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ManagedDataSet implements IDataSet {

	private final IDataSet target;
	private final StatusManager status;

	public ManagedDataSet(IDataSet target, StatusManager status) {
		this.target = target;
		this.status = status;
	}

	public String[] getTableNames() throws DataSetException {
		return target.getTableNames();
	}

	public ITableMetaData getTableMetaData(String tableName) throws DataSetException {
		return target.getTableMetaData(tableName);
	}

	public ITable getTable(String tableName) throws DataSetException {
		return target.getTable(tableName);
	}

	public ITable[] getTables() throws DataSetException {
		//noinspection deprecation
		return target.getTables();
	}

	public ITableIterator iterator() throws DataSetException {
		return new ManagedTableIterator(target.iterator(), status);
	}

	public ITableIterator reverseIterator() throws DataSetException {
		return new ManagedTableIterator(target.reverseIterator(), status);
	}
}
