package ch.vd.uniregctb.declaration;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class ModeleFeuilleDocumentDAOImpl extends BaseDAOImpl<ModeleFeuilleDocument, Long> implements ModeleFeuilleDocumentDAO {

	public ModeleFeuilleDocumentDAOImpl() {
		super(ModeleFeuilleDocument.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ModeleFeuilleDocument> getByModeleDocument(ModeleDocument modeleDocument) {
		return find("FROM ModeleFeuilleDocument m WHERE m.modeleDocument = :md", buildNamedParameters(Pair.of("md", modeleDocument)), null);
	}

}
