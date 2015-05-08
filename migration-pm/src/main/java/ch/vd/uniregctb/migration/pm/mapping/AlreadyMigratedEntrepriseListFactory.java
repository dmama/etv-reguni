package ch.vd.uniregctb.migration.pm.mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.transaction.TransactionTemplate;

public class AlreadyMigratedEntrepriseListFactory implements FactoryBean<Set<Long>>, InitializingBean {

	private SessionFactory uniregSessionFactory;
	private PlatformTransactionManager uniregTransactionManager;
	private Set<Long> ids;

	public void setUniregSessionFactory(SessionFactory uniregSessionFactory) {
		this.uniregSessionFactory = uniregSessionFactory;
	}

	public void setUniregTransactionManager(PlatformTransactionManager uniregTransactionManager) {
		this.uniregTransactionManager = uniregTransactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final TransactionTemplate template = new TransactionTemplate(uniregTransactionManager);
		template.setReadOnly(true);
		this.ids = Collections.unmodifiableSet(template.execute(status -> loadIds()));
	}

	private Set<Long> loadIds() {
		final Session currentSession = uniregSessionFactory.getCurrentSession();

		//noinspection JpaQlInspection
		final Query query = currentSession.createQuery("select m.idRegpm from MigrationPmMapping m where m.typeEntite = :typeEntite");
		query.setParameter("typeEntite", MigrationPmMapping.TypeEntite.ENTREPRISE);

		//noinspection unchecked
		final List<Long> ids = query.list();
		return new HashSet<>(ids);
	}

	@Override
	public Set<Long> getObject() throws Exception {
		return ids;
	}

	@Override
	public Class<Set> getObjectType() {
		return Set.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
