package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.MotifFor;

public class MotifForUserType extends EnumUserType<MotifFor> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public MotifForUserType() {
		super(MotifFor.class);
	}

}

