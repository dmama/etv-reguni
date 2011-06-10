package ch.vd.uniregctb.declaration;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class ModeleFeuilleDocumentDAOImpl extends GenericDAOImpl<ModeleFeuilleDocument, Long> implements ModeleFeuilleDocumentDAO {

	public ModeleFeuilleDocumentDAOImpl() {
		super(ModeleFeuilleDocument.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ModeleFeuilleDocument> getByModeleDocument(ModeleDocument modeleDocument) {
		Object[] values = new Object[] {
			modeleDocument
		};
		List<?> list = find("FROM ModeleFeuilleDocument m WHERE m.modeleDocument = ?", values, null);
		return (List<ModeleFeuilleDocument>) list;
	}

}
