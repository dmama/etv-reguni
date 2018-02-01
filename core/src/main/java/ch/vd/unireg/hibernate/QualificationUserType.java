package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.Qualification;

/**
 * Classe de transtypage pour Hibernate : Qualification <--> varchar
 */
public class QualificationUserType extends EnumUserType<Qualification> {

	public QualificationUserType() {
		super(Qualification.class);
	}
}
