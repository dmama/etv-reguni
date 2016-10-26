package ch.vd.uniregctb.evenement.registrefoncier;

import java.io.IOException;
import java.sql.Blob;

import org.hibernate.Hibernate;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class EvenementRFImportDAOImpl extends BaseDAOImpl<EvenementRFImport, Long> implements EvenementRFImportDAO {
	protected EvenementRFImportDAOImpl() {
		super(EvenementRFImport.class);
	}

	@Override
	public Blob createBlob(byte[] bytes) throws IOException {
		return Hibernate.getLobCreator(getCurrentSession()).createBlob(bytes);
	}
}
