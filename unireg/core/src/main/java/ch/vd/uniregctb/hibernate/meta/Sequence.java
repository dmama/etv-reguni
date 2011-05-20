package ch.vd.uniregctb.hibernate.meta;

import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.impl.SessionFactoryImpl;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntity;

public class Sequence {

	private String sequenceName;
	private Class<? extends IdentifierGenerator> generatorClass;

	public Sequence(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public Sequence(Class<? extends IdentifierGenerator> generatorClass) {
		this.generatorClass = generatorClass;
	}

	public Object nextValue(final HibernateTemplate template, final HibernateEntity entity) {

		final SessionFactoryImpl factory = (SessionFactoryImpl) template.getSessionFactory();
		final Dialect dialect = factory.getDialect();

		if (sequenceName != null) {
			return template.execute(new HibernateCallback<Object>() {
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					String sql = dialect.getSequenceNextValString(sequenceName);
					SQLQuery query = session.createSQLQuery(sql);
					return query.uniqueResult();
				}
			});
		}
		else {
			Assert.notNull(generatorClass);
			try {
				final IdentifierGenerator generator = generatorClass.newInstance();
				((Configurable)generator).configure(Hibernate.LONG, new Properties(), dialect);
				return template.execute(new HibernateCallback<Object>() {
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						return generator.generate((SessionImplementor) session, entity);
					}
				});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
