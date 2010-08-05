package ch.vd.uniregctb.supergra;

import ch.vd.uniregctb.common.HibernateEntity;

public abstract class Delta {

	abstract EntityKey getKey();

	abstract void apply(HibernateEntity entity, SuperGraContext context);
}
