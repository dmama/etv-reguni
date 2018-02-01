package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.Qualification;

/**
 * Classe de transtypage pour Hibernate : Qualification <--> varchar
 */
public class QualificationUserType extends EnumUserType<Qualification> {

	public QualificationUserType() {
		super(Qualification.class);
	}
}
