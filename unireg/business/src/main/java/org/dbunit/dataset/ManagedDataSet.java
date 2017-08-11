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

	@Override
	public String[] getTableNames() throws DataSetException {
		return target.getTableNames();
	}

	@Override
	public ITableMetaData getTableMetaData(String tableName) throws DataSetException {
		return target.getTableMetaData(tableName);
	}

	@Override
	public ITable getTable(String tableName) throws DataSetException {
		return target.getTable(tableName);
	}

	@Override
	public ITable[] getTables() throws DataSetException {
		//noinspection deprecation
		return target.getTables();
	}

	@Override
	public ITableIterator iterator() throws DataSetException {
		return new ManagedTableIterator(target.iterator(), status);
	}

	@Override
	public ITableIterator reverseIterator() throws DataSetException {
		return new ManagedTableIterator(target.reverseIterator(), status);
	}
}
