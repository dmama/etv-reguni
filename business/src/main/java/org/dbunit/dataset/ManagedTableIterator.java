package org.dbunit.dataset;

import ch.vd.uniregctb.common.StatusManager;

/**
 * Dataset iterator qui prend un status manager.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ManagedTableIterator implements ITableIterator {

	private final ITableIterator target;
	private final StatusManager status;

	public ManagedTableIterator(ITableIterator target, StatusManager status) {
		this.target = target;
		this.status = status;
	}

	@Override
	public boolean next() throws DataSetException {
		return target.next() && !status.isInterrupted();
	}

	@Override
	public ITableMetaData getTableMetaData() throws DataSetException {
		return target.getTableMetaData();
	}

	@Override
	public ITable getTable() throws DataSetException {
		ITable table = target.getTable();
		status.setMessage("Traitement de la table " + table.getTableMetaData().getTableName() + "...");
		return table;
	}
}
