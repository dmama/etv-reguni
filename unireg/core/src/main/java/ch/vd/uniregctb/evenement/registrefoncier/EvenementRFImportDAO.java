package ch.vd.uniregctb.evenement.registrefoncier;

import java.io.IOException;
import java.sql.Blob;

import ch.vd.registre.base.dao.GenericDAO;

public interface EvenementRFImportDAO extends GenericDAO<EvenementRFImport, Long> {

	Blob createBlob(byte[] bytes) throws IOException;
}
