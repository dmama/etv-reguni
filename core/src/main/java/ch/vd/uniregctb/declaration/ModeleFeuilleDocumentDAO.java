package ch.vd.uniregctb.declaration;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

public interface ModeleFeuilleDocumentDAO extends GenericDAO<ModeleFeuilleDocument, Long> {

	List<ModeleFeuilleDocument> getByModeleDocument(ModeleDocument modeleDocument);
	
}
