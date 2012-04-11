package org.dbunit.operation;

import java.util.BitSet;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;

import ch.vd.uniregctb.common.StatusManager;

/**
 * Operation DBUnit d'insert qui prend un status manager.
 */
public class ManagedInsertOperation extends InsertOperation {

	final StatusManager statusManager;

	public ManagedInsertOperation(StatusManager statusManager) {
		this.statusManager = statusManager;
	}

	/**
	 * [Hack] On rédéfini cette méthode uniquement pour logger (et éventuellement interrompre) le chargement d'un script DBUnit. Son
	 * comportement de base est inchangé.
	 */
	@Override
	protected boolean equalsIgnoreMapping(BitSet ignoreMapping, ITable table, int row) throws DataSetException {
		if (statusManager != null) {
			if (statusManager.interrupted()) {
				throw new RuntimeException("Import de la base interrompu !");
			}
			if (row % 100 == 0) {
				statusManager
						.setMessage("Insertion de la ligne " + row + " dans la table " + table.getTableMetaData().getTableName() + '.');
			}
		}
		return super.equalsIgnoreMapping(ignoreMapping, table, row);
	}

}
