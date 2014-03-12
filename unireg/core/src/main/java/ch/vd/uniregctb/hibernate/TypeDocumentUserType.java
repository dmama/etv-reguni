package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeDocument;

/**
 * Classe de transtypage pour Hibernate : TypeDocument <--> varchar
 */
public class TypeDocumentUserType extends EnumUserType<TypeDocument> {

	public TypeDocumentUserType() {
		super(TypeDocument.class);
	}
}
