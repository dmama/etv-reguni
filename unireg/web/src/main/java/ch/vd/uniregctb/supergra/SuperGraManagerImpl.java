package ch.vd.uniregctb.supergra;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ObjectGetterHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.meta.MetaEntity;
import ch.vd.uniregctb.hibernate.meta.Property;
import ch.vd.uniregctb.hibernate.meta.Sequence;

public class SuperGraManagerImpl implements SuperGraManager, InitializingBean {

	protected final Logger LOGGER = Logger.getLogger(SuperGraManagerImpl.class);

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private List<String> annotatedClass;
	private Map<EntityType, List<Class<? extends HibernateEntity>>> concreteClassByType = new HashMap<EntityType, List<Class<? extends HibernateEntity>>>();

	private static final Set<String> readonlyProps = new HashSet<String>();

	static {
		readonlyProps.add("logCreationDate");
		readonlyProps.add("logCreationUser");
		readonlyProps.add("logModifDate");
		readonlyProps.add("logModifUser");
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAnnotatedClass(List<String> annotatedClass) {
		this.annotatedClass = annotatedClass;
	}

	@SuppressWarnings({"unchecked"})
	public void afterPropertiesSet() throws Exception {
		for (String classname : annotatedClass) {
			final Class clazz = Class.forName(classname);
			if (Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			if (!HibernateEntity.class.isAssignableFrom(clazz)) {
				LOGGER.warn("Impossible d'enregistrer la classe [" + clazz + "] parce qu'elle n'hérite pas de HibernateEntity.");
				continue;
			}
			for (EntityType t : EntityType.values()) {
				if (t.getHibernateClass().isAssignableFrom(clazz)) {
					List<Class<? extends HibernateEntity>> list = concreteClassByType.get(t);
					if (list == null) {
						list = new ArrayList<Class<? extends HibernateEntity>>();
						concreteClassByType.put(t, list);
					}
					list.add(clazz);
				}
			}
		}
	}

	private Object simulate(final TransactionCallback callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				status.setRollbackOnly();
				return callback.doInTransaction(status);
			}
		});
	}

	public void fillView(final EntityKey key, final EntityView view, final List<Delta> deltas) {

		simulate(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final SuperGraContext context = new SuperGraContext(hibernateTemplate);
				applyDeltas(deltas, context);

				view.setKey(key);

				final HibernateEntity entity = context.getEntity(key);
				if (entity != null) {
					final List<AttributeView> attributes = buildAttributes(entity);
					view.setAttributes(attributes);
				}
				return null;
			}
		});
	}

	private void applyDeltas(List<Delta> deltas, SuperGraContext context) {
		if (deltas == null) {
			return;
		}

		for (Delta d : deltas) {
			final EntityKey key = d.getKey();
			final HibernateEntity entity = context.getEntity (key);
			if (entity != null) {
				d.apply(entity, context);
			}
		}
	}

	private List<AttributeView> buildAttributes(HibernateEntity entity) {

		final List<AttributeView> attributes = new ArrayList<AttributeView>();
		try {
			final MetaEntity meta = MetaEntity.determine(entity.getClass());
			final List<Property> props = meta.getProperties();
			for (Property p : props) {
				if (p.isDiscriminator()) {
					// le discriminator ne possède pas de getter/setter, et ne peux donc pas être édité.
					attributes.add(new AttributeView("<discriminator>", p.getType().getJavaType(), p.getDiscriminatorValue(), false, false, true));
				}
				else {
					final String propName = p.getName();
					final Object value = ObjectGetterHelper.getValue(entity, propName);
					if (p.isCollection()) {
						final Collection<?> coll = (Collection<?>) value;
						attributes.add(new AttributeView(propName, p.getType().getJavaType(), value == null ? "" : coll.size() + " éléments", false, true, false));
					}
					else {
						final boolean readonly = p.isPrimaryKey() || readonlyProps.contains(propName);
						attributes.add(new AttributeView(propName, p.getType().getJavaType(), value, p.isParentForeignKey(), false, readonly));
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return attributes;
	}

	@SuppressWarnings({"unchecked"})
	public void fillView(final EntityKey key, final String collName, final CollectionView view, final List<Delta> deltas) {

		simulate(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final SuperGraContext context = new SuperGraContext(hibernateTemplate);
				applyDeltas(deltas, context);

				view.setKey(key);

				final HibernateEntity entity = context.getEntity(key);
				if (entity != null) {
					final Collection<HibernateEntity> coll = (Collection<HibernateEntity>) getCollection(collName, entity);

					if (coll != null) {
						final List<EntityView> entities = buildEntities(coll);

						// Construit la liste de noms de tous les attributs existants
						final ArrayList<String> attributeNames = new ArrayList<String>();

						Property primaryKey = null;
						Class primaryKeyType;
						try {
							PropertyDescriptor collDescr = new PropertyDescriptor(collName, entity.getClass());
							Method getter = collDescr.getReadMethod();
							primaryKeyType = MetaEntity.getGenericParamReturnType(getter);
							MetaEntity en = MetaEntity.determine(primaryKeyType);
							for (Property p : en.getProperties()) {
								if (!p.isDiscriminator() && !p.isParentForeignKey() && !p.isCollection()) {
									attributeNames.add(p.getName());
								}
								if (p.isPrimaryKey()) {
									primaryKey = p;
								}
							}
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}

						final EntityType keyType = EntityType.fromHibernateClass(primaryKeyType);
						final List<Class<? extends HibernateEntity>> concreteClasses = concreteClassByType.get(keyType);

						Assert.notNull(primaryKey);
						view.setPrimaryKeyAtt(primaryKey.getName());
						view.setPrimaryKeyType(keyType);
						view.setEntities(entities);
						view.setAttributeNames(attributeNames);
						view.setConcreteEntityClasses(concreteClasses);
					}
				}
				return null;
			}
		});
	}

	private Collection<?> getCollection(String collName, HibernateEntity entity) {
		try {
			return (Collection<?>) ObjectGetterHelper.getValue(entity, collName);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<EntityView> buildEntities(Collection<? extends HibernateEntity> coll) {

		if (coll == null || coll.isEmpty()) {
			return Collections.emptyList();
		}

		final List<EntityView> entities = new ArrayList<EntityView>(coll.size());
		for (HibernateEntity e : coll) {
			final Long id = (Long) e.getKey();
			final EntityType type = EntityType.fromHibernateClass(e.getClass());
			final EntityView v = new EntityView();
			v.setKey(new EntityKey(type, id));
			v.setAttributes(buildAttributes(e));
			entities.add(v);
		}

		return entities;
	}

	public Long nextId(final Class<? extends HibernateEntity> clazz) {

		return (Long) simulate(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					final MetaEntity m = MetaEntity.determine(clazz);
					final Sequence sequence = m.getSequence();
					Assert.notNull(sequence);

					final Number id = (Number) sequence.nextValue(hibernateTemplate, clazz.newInstance());
					return id.longValue();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
