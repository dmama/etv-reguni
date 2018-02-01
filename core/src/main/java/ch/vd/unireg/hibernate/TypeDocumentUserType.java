package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeDocument;

/**
 * Classe de transtypage pour Hibernate : TypeDocument <--> varchar
 */
public class TypeDocumentUserType extends EnumUserType<TypeDocument> {

	public TypeDocumentUserType() {
		super(TypeDocument.class);
	}
}
