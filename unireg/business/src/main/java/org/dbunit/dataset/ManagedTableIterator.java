package org.dbunit.dataset;

import ch.vd.uniregctb.common.StatusManager;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;

/**
 * Dataset iterator qui prend un status manager.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ManagedTableIterator implements ITableIterator {

	private ITableIterator target;
	private StatusManager status;

	public ManagedTableIterator(ITableIterator target, StatusManager status) {
		this.target = target;
		this.status = status;
	}

	public boolean next() throws DataSetException {
		return target.next();
	}

	public ITableMetaData getTableMetaData() throws DataSetException {
		return target.getTableMetaData();
	}

	public ITable getTable() throws DataSetException {
		ITable table = target.getTable();
		status.setMessage("Traitement de la table " + table.getTableMetaData().getTableName() + "...");
		return table;
	}
}
