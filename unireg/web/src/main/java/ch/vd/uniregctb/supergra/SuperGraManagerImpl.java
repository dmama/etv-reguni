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
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.meta.MetaEntity;
import ch.vd.uniregctb.hibernate.meta.Property;
import ch.vd.uniregctb.hibernate.meta.Sequence;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tutelle;

public class SuperGraManagerImpl implements SuperGraManager, InitializingBean {

	protected final Logger LOGGER = Logger.getLogger(SuperGraManagerImpl.class);

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private List<String> annotatedClass;
	private Map<EntityType, List<Class<? extends HibernateEntity>>> concreteClassByType = new HashMap<EntityType, List<Class<? extends HibernateEntity>>>();

	/**
	 * Les propriétés qui ne doivent pas être changées, même en mode SuperGra.
	 */
	private static final Set<String> readonlyProps = new HashSet<String>();

	static {
		readonlyProps.add("logCreationDate");
		readonlyProps.add("logCreationUser");
		readonlyProps.add("logModifDate");
		readonlyProps.add("logModifUser");
	}

	/**
	 * Les propriétés qui représentent des données techniques, non-métier et pas indispensables à afficher en mode condensé.
	 */
	private static final Set<String> detailsProps = new HashSet<String>();

	static {
		detailsProps.add("annulationDate");
		detailsProps.add("annulationUser");
		detailsProps.add("logCreationDate");
		detailsProps.add("logCreationUser");
		detailsProps.add("logModifDate");
		detailsProps.add("logModifUser");
	}

	/**
	 * Les builders custom d'attributs qui permettent de spécialiser l'affichage de certains attributs.
	 */
	private static final Map<AttributeKey, AttributeBuilder> attributeCustomBuilders = new HashMap<AttributeKey, AttributeBuilder>();

	private static interface AttributeBuilder {
		AttributeView build(Property p, Object value);
	}

	/**
	 * La clé qui permet d'identifier un attribut particulier d'une classe particulière.
	 */
	private static class AttributeKey {
		private final Class<?> entityClass;
		private final String attributeName;

		/**
		 * Construit une clé d'attribut pour une classe concrète et un nom d'attribut donnés.
		 *
		 * @param entityClass   une classe concrète
		 * @param attributeName le nom de l'attribut. Spécifier <b>null</b> pour référencer le discriminant de l'entité.
		 */
		private AttributeKey(Class<?> entityClass, String attributeName) {
			Assert.notNull(entityClass);
			this.entityClass = entityClass;
			this.attributeName = attributeName;
		}

		@SuppressWarnings({"RedundantIfStatement"})
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final AttributeKey that = (AttributeKey) o;

			if (attributeName != null ? !attributeName.equals(that.attributeName) : that.attributeName != null) return false;
			if (!entityClass.equals(that.entityClass)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = entityClass.hashCode();
			result = 31 * result + (attributeName != null ? attributeName.hashCode() : 0);
			return result;
		}
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

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"unchecked"})
	public void afterPropertiesSet() throws Exception {
		for (String classname : annotatedClass) {
			final Class clazz = Class.forName(classname);
			if (Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			if (!HibernateEntity.class.isAssignableFrom(clazz)) {
				LOGGER.debug("Impossible d'enregistrer la classe [" + clazz + "] parce qu'elle n'hérite pas de HibernateEntity.");
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

	public void fillView(final EntityKey key, final EntityView view, final SuperGraSession session) {

		simulate(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Reconstruit l'état en cours de modification des entités
				final SuperGraContext context = new SuperGraContext(hibernateTemplate);
				applyDeltas(session.getDeltas(), context);
				refreshTiersState(session, context);

				view.setKey(key);

				final HibernateEntity entity = context.getEntity(key);
				if (entity != null) {
					fillView(entity, view);
				}
				return null;
			}
		});
	}

	/**
	 * Met-à-jour l'état des tiers modifiés dans une session SuperGra.
	 *
	 * @param session une session SuperGra.
	 * @param context le context DAO spécifique au mode SuperGra.
	 */
	private void refreshTiersState(SuperGraSession session, SuperGraContext context) {

		// Récupère tous les tiers impactés par les deltas
		final Map<Long, Tiers> tiers = new HashMap<Long, Tiers>();
		final List<Delta> deltas = session.getDeltas();
		for (Delta d : deltas) {
			final HibernateEntity entity = context.getEntity(d.getKey());
			if (entity instanceof Tiers) {
				final Tiers t = (Tiers) entity;
				if (!tiers.containsKey(t.getId())) {
					tiers.put(t.getId(), t);
				}
			}
			else if (entity instanceof LinkedEntity) {
				final Set<Tiers> linked = tiersService.getLinkedTiers((LinkedEntity) entity);
				for (Tiers t : linked) {
					if (t != null && !tiers.containsKey(t.getId())) {
						tiers.put(t.getId(), t);
					}
				}
			}
		}

		// Détermine la validité de tous les tiers
		final List<TiersState> tiersStates = new ArrayList<TiersState>(tiers.size());
		for (Tiers t : tiers.values()) {
			final ValidationResults res = t.validate();
			tiersStates.add(new TiersState(new EntityKey(EntityType.Tiers, t.getId()), res));
		}

		// Met-à-jour la session
		session.setTiersStates(tiersStates);
	}

	private void applyDeltas(List<Delta> deltas, SuperGraContext context) {
		if (deltas == null) {
			return;
		}

		for (Delta d : deltas) {
			final EntityKey key = d.getKey();
			final HibernateEntity entity = context.getEntity(key);
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
				final AttributeBuilder customBuilder = getCustomAttributeBuilder(new AttributeKey(entity.getClass(), p.getName()));
				final String propName = p.getName();
				final Object value = (propName == null ? p.getDiscriminatorValue() : ObjectGetterHelper.getValue(entity, propName));

				final AttributeView attributeView;
				if (customBuilder != null) {
					// si un custom builder est spécifié, on l'utilise
					attributeView = customBuilder.build(p, value);
				}
				else if (p.isDiscriminator()) {
					// le discriminator ne possède pas de getter/setter, et ne peux donc pas être édité.
					attributeView = new AttributeView("<discriminator>", p.getType().getJavaType(), p.getDiscriminatorValue(), false, false, true);
				}
				else if (p.isCollection()) {
					// on cas de collection, on affiche un lien vers la page d'affichage de la collection
					final Collection<?> coll = (Collection<?>) value;
					attributeView = new AttributeView(propName, p.getType().getJavaType(), value == null ? "" : coll.size() + " éléments", false, true, false);
				}
				else {
					// cas général, on affiche l'éditeur pour l'attribut
					final boolean readonly = p.isPrimaryKey() || readonlyProps.contains(propName);
					attributeView = new AttributeView(propName, p.getType().getJavaType(), value, p.isParentForeignKey(), false, readonly);
				}

				attributes.add(attributeView);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return attributes;
	}

	/**
	 * @param key la clé qui permet d'identifier un attribut d'une classe.
	 * @return le custom attribute builder qui correspond à la clé spécifiée; ou <b>null</b> si aucun builder custom n'a été spécifié.
	 */
	private AttributeBuilder getCustomAttributeBuilder(AttributeKey key) {
		if (attributeCustomBuilders.isEmpty()) { // lazy init pour éviter de ralentir le démarrage de l'application
			synchronized (attributeCustomBuilders) {
				if (attributeCustomBuilders.isEmpty()) {
					initCustomAttributeBuilder();
				}
			}
		}
		return attributeCustomBuilders.get(key);
	}

	@SuppressWarnings({"unchecked"})
	public void fillView(final EntityKey key, final String collName, final CollectionView view, final SuperGraSession session) {

		simulate(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Reconstruit l'état en cours de modification des entités
				final SuperGraContext context = new SuperGraContext(hibernateTemplate);
				applyDeltas(session.getDeltas(), context);
				refreshTiersState(session, context);

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

						if (!session.getOptions().isShowDetails()) {
							// On filtre les attributs non-métier (pour limiter le nombre de colonnes autant que possible)
							attributeNames.removeAll(detailsProps);
						}

						final EntityType keyType = EntityType.fromHibernateClass(primaryKeyType);
						final List<Class<? extends HibernateEntity>> concreteClasses = concreteClassByType.get(keyType);

						assert primaryKey != null;
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
			final EntityView v = new EntityView();
			fillView(e, v);
			entities.add(v);
		}

		return entities;
	}

	/**
	 * Renseigne la clé, les attributs et les éventuels résultats de validation pour l'entité spécifiée.
	 *
	 * @param entity l'entité de référence
	 * @param view   la vue à remplir
	 */
	private void fillView(HibernateEntity entity, EntityView view) {
		final Long id = (Long) entity.getKey();
		final EntityType type = EntityType.fromHibernateClass(entity.getClass());

		view.setKey(new EntityKey(type, id));
		view.setAttributes(buildAttributes(entity));

		if (entity instanceof Validateable) {
			final Validateable val = (Validateable) entity;
			view.setValidationResults(val.validate());
		}
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

	public void commitDeltas(final List<Delta> deltas) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				// Reconstruit l'état en cours de modification des entités
				final SuperGraContext context = new SuperGraContext(hibernateTemplate);
				applyDeltas(deltas, context);
				// On commit la transaction (fait automatiquement par le template)
				return null;
			}
		});
	}

	/**
	 * Cette méthode contient la définition des builders d'attributs custom.
	 */
	private void initCustomAttributeBuilder() {
		// Appartenance ménage
		attributeCustomBuilders.put(new AttributeKey(AppartenanceMenage.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "personne physique", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(AppartenanceMenage.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "ménage commun", p.getType().getJavaType(), value, false, false, false);
			}
		});

		// Contact impôt source
		attributeCustomBuilders.put(new AttributeKey(ContactImpotSource.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "sourcier", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(ContactImpotSource.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "débiteur", p.getType().getJavaType(), value, false, false, false);
			}
		});

		// Annule et remplace
		attributeCustomBuilders.put(new AttributeKey(AnnuleEtRemplace.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "tiers remplacé", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(AnnuleEtRemplace.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "tiers remplaçant", p.getType().getJavaType(), value, false, false, false);
			}
		});

		// Curatelle
		attributeCustomBuilders.put(new AttributeKey(Curatelle.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "pupille", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(Curatelle.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "curateur", p.getType().getJavaType(), value, false, false, false);
			}
		});

		// Tutelle
		attributeCustomBuilders.put(new AttributeKey(Tutelle.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "pupille", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(Tutelle.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "tuteur", p.getType().getJavaType(), value, false, false, false);
			}
		});

		// Conseil légal
		attributeCustomBuilders.put(new AttributeKey(ConseilLegal.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "pupille", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(ConseilLegal.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "conseiller légal", p.getType().getJavaType(), value, false, false, false);
			}
		});

		// Représentation conventionnel
		attributeCustomBuilders.put(new AttributeKey(RepresentationConventionnelle.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "représenté", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(RepresentationConventionnelle.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "représentant", p.getType().getJavaType(), value, false, false, false);
			}
		});

		// Rapport de prestation imposable
		attributeCustomBuilders.put(new AttributeKey(RapportPrestationImposable.class, "sujetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "contribuable", p.getType().getJavaType(), value, false, false, false);
			}
		});
		attributeCustomBuilders.put(new AttributeKey(RapportPrestationImposable.class, "objetId"), new AttributeBuilder() {
			public AttributeView build(Property p, Object value) {
				return new AttributeView(p.getName(), "débiteur", p.getType().getJavaType(), value, false, false, false);
			}
		});
	}
}
