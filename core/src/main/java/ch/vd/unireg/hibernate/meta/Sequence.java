package ch.vd.unireg.hibernate.meta;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.query.NativeQuery;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.hibernate.HibernateTemplate;

public class Sequence {

	private String sequenceName;
	private Class<? extends IdentifierGenerator> generatorClass;

	public Sequence(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public Sequence(Class<? extends IdentifierGenerator> generatorClass) {
		this.generatorClass = generatorClass;
	}

	public Object nextValue(final ServiceRegistry serviceRegistry, HibernateTemplate hibernateTemplate, final HibernateEntity entity) {
		if (sequenceName != null) {
			final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
			final Dialect dialect = jdbcEnvironment.getDialect();
			final String sql = dialect.getSequenceNextValString(sequenceName);
			return hibernateTemplate.execute(session -> {
				final NativeQuery query = session.createNativeQuery(sql);
				return query.uniqueResult();
			});
		}
		else {
			if (generatorClass == null) {
				throw new IllegalArgumentException();
			}
			try {
				final IdentifierGenerator generator = generatorClass.newInstance();
				((Configurable) generator).configure(StandardBasicTypes.LONG, new Properties(), serviceRegistry);
				return hibernateTemplate.execute(session -> generator.generate((SessionImplementor) session, entity));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
