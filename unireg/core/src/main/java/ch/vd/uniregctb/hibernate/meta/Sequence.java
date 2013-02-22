package ch.vd.uniregctb.hibernate.meta;

import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.type.StandardBasicTypes;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;

public class Sequence {

	private String sequenceName;
	private Class<? extends IdentifierGenerator> generatorClass;

	public Sequence(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public Sequence(Class<? extends IdentifierGenerator> generatorClass) {
		this.generatorClass = generatorClass;
	}

	public Object nextValue(final Dialect dialect, HibernateTemplate hibernateTemplate, final HibernateEntity entity) {
		if (sequenceName != null) {
			final String sql = dialect.getSequenceNextValString(sequenceName);
			return hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					final SQLQuery query = session.createSQLQuery(sql);
					return query.uniqueResult();
				}
			});
		}
		else {
			Assert.notNull(generatorClass);
			try {
				final IdentifierGenerator generator = generatorClass.newInstance();
				((Configurable)generator).configure(StandardBasicTypes.LONG, new Properties(), dialect);
				return hibernateTemplate.execute(new HibernateCallback<Object>() {
					@Override
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
