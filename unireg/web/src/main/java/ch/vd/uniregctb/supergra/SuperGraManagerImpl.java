package ch.vd.uniregctb.supergra;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.StandardBasicTypes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ObjectGetterHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.ReflexionUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.meta.MetaEntity;
import ch.vd.uniregctb.hibernate.meta.MetaException;
import ch.vd.uniregctb.hibernate.meta.Property;
import ch.vd.uniregctb.hibernate.meta.Sequence;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.json.InfraCategory;
import ch.vd.uniregctb.supergra.delta.AttributeUpdate;
import ch.vd.uniregctb.supergra.delta.Delta;
import ch.vd.uniregctb.supergra.view.AttributeView;
import ch.vd.uniregctb.supergra.view.CollectionView;
import ch.vd.uniregctb.supergra.view.EntityView;
import ch.vd.uniregctb.taglibs.formInput.MultilineString;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AssujettissementParSubstitution;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.validation.ValidationInterceptor;
import ch.vd.uniregctb.validation.ValidationService;

public class SuperGraManagerImpl implements SuperGraManager, InitializingBean {

	protected final Logger LOGGER = LoggerFactory.getLogger(SuperGraManagerImpl.class);

	private static final String DISCRIMINATOR_ATTNAME = "<discriminator>";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private ValidationService validationService;
	private ValidationInterceptor validationInterceptor;
	private GlobalTiersIndexer globalTiersIndexer;
	private Dialect dialect;


	private List<String> annotatedClass;
	private final Map<EntityType, List<Class<? extends HibernateEntity>>> concreteClassByType = new EnumMap<>(EntityType.class);

	/**
	 * Les propriétés qui ne doivent pas être changées, même en mode SuperGra.
	 */
	private static final Set<String> readonlyProps = new HashSet<>();

	static {
		// général
		readonlyProps.add("annulationDate");
		readonlyProps.add("annulationUser");
		readonlyProps.add("logCreationDate");
		readonlyProps.add("logCreationUser");
		readonlyProps.add("logModifDate");
		readonlyProps.add("logModifUser");

		// déclaration
		readonlyProps.add("modeleDocument");
		readonlyProps.add("periode");
	}

	/**
	 * Les propriétés qui représentent des données techniques, non-métier et pas indispensables à afficher en mode condensé.
	 */
	private static final Set<String> detailsProps = new HashSet<>();

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
	 * (construit en lazy-init pour éviter le ralentissement applicatif au démarrage)
	 * @see #getCustomAttributeBuilder(ch.vd.uniregctb.supergra.SuperGraManagerImpl.AttributeKey)
	 */
	private Map<AttributeKey, AttributeBuilder> attributeCustomBuilders = null;

	private static interface AttributeBuilder {
		AttributeView build(Property p, Object value, SuperGraContext context);
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		this.validationInterceptor = validationInterceptor;
	}

	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
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
						list = new ArrayList<>();
						concreteClassByType.put(t, list);
					}
					list.add(clazz);
				}
			}
		}
	}

	/**
	 * Simule des modifications sur des entités de la base de données. Au sortir de cette méthode, aucune modification n'est appliquée dans la base de données.
	 * <p/>
	 * Pour ce faire, une session hibernate est créée à l'intérieur d'une transaction marquée <i>rollback-only</i>.
	 *
	 * @param action un callback permettant d'exécuter des actions à l'intérieur de la session/transaction.
	 * @return la valeur retournée par le callback.
	 */
	private <T> T simulate(final HibernateCallback<T> action) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TransactionCallback<T>() {
			@Override
			public T doInTransaction(TransactionStatus status) {
				status.setRollbackOnly();
				return hibernateTemplate.execute(action);
			}
		});
	}

	/**
	 * Exécute des modifications sur des entités de la base de données. Au sortir de cette méthode, les modifications sont appliquées dans la base de données (sauf en cas d'exception).
	 *
	 * @param action un callback permettant d'exécuter des actions à l'intérieur de la session/transaction.
	 * @return la valeur retournée par le callback.
	 */
	private <T> T execute(final HibernateCallback<T> action) {
		AuthenticationHelper.pushPrincipal(getSuperGraPrincipalName());
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			return template.execute(new TransactionCallback<T>() {
				@Override
				public T doInTransaction(TransactionStatus status) {
					return hibernateTemplate.execute(action);
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Override
	public void fillView(final EntityKey key, final EntityView view, final SuperGraSession session) {

		simulate(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session s) throws HibernateException, SQLException {

				// Reconstruit l'état en cours de modification des entités
				final SuperGraContext context = new SuperGraContext(s, false, validationInterceptor);
				applyDeltas(session.getDeltas(), context);
				refreshTiersState(session, context);

				view.setKey(key);

				final HibernateEntity entity = context.getEntity(key);
				if (entity != null) {
					fillView(entity, view, context);

					if (entity instanceof Tiers) {
						// on affiche un tiers, on en profite pour mémoriser son numéro de manière à pouvoir revenir sur lui plus tard
						session.setLastKnownTiersId((Long) entity.getKey());
					}
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
		final Map<Long, Tiers> tiers = new HashMap<>();
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
				final Set<Tiers> linked = tiersService.getLinkedTiers((LinkedEntity) entity, isAnnulation(d));
				for (Tiers t : linked) {
					if (t != null && !tiers.containsKey(t.getId())) {
						tiers.put(t.getId(), t);
					}
				}
			}
		}

		// Détermine la validité de tous les tiers
		final List<TiersState> tiersStates = new ArrayList<>(tiers.size());
		for (Tiers t : tiers.values()) {
			final ValidationResults res = validationService.validate(t);
			tiersStates.add(new TiersState(new EntityKey(EntityType.Tiers, t.getId()), res));
		}

		// Met-à-jour la session
		session.setTiersStates(tiersStates);
	}

	private static boolean isAnnulation(Delta delta) {
		boolean isAnnulation = false;
		if (delta instanceof AttributeUpdate) {
			AttributeUpdate update = (AttributeUpdate) delta;
			isAnnulation = "annulationDate".equals(update.getName()) && update.getOldValue() == null && update.getNewValue() != null;
		}
		return isAnnulation;
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

	private List<AttributeView> buildAttributes(HibernateEntity entity, SuperGraContext context) {

		final List<AttributeView> attributes = new ArrayList<>();
		try {
			final MetaEntity meta = MetaEntity.determine(entity.getClass());
			final List<Property> props = meta.getProperties();
			for (int i = 0, propsSize = props.size(); i < propsSize; i++) {
				final Property p = props.get(i);
				final AttributeView attributeView;
				if (p.isDiscriminator()) {
					attributeView = new AttributeView(DISCRIMINATOR_ATTNAME, p.getType().getJavaType(), p.getDiscriminatorValue(), false, false, true);
				}
				else {
					final AttributeBuilder customBuilder = getCustomAttributeBuilder(new AttributeKey(entity.getClass(), p.getName()));
					final String propName = p.getName();
					final Object value = ReflexionUtils.getPathValue(entity, propName);

					if (customBuilder != null) {
						// si un custom builder est spécifié, on l'utilise
						attributeView = customBuilder.build(p, value, context);
					}
					else if (p.isDiscriminator()) {
						// le discriminator ne possède pas de getter/setter, et ne peux donc pas être édité.
						attributeView = new AttributeView(DISCRIMINATOR_ATTNAME, p.getType().getJavaType(), p.getDiscriminatorValue(), false, false, true);
					}
					else if (p.isCollection()) {
						// on cas de collection, on affiche un lien vers la page d'affichage de la collection
						final Collection<?> coll = (Collection<?>) value;
						attributeView = new AttributeView(propName, p.getType().getJavaType(), value == null ? "" : coll.size() + " éléments", false, true, false);
					}
					else {
						// cas général, on affiche l'éditeur pour l'attribut
						final boolean readonly = p.isPrimaryKey() || p.isParentForeignKey() || readonlyProps.contains(propName);
						attributeView = new AttributeView(propName, p.getType().getJavaType(), value, p.isParentForeignKey(), false, readonly);
					}
				}

				// on renseigne l'id (au sens HTML) s'il n'est pas déjà renseigné
				if (attributeView.getId() == null) {
					attributeView.setId("attributes_" + i);
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
		if (attributeCustomBuilders == null) {  // lazy init pour éviter de ralentir le démarrage de l'application
			synchronized (this) {
				if (attributeCustomBuilders == null) {
					attributeCustomBuilders = initCustomAttributeBuilder();
				}
			}
		}
		return attributeCustomBuilders.get(key);
	}

	@Override
	public void fillView(final EntityKey key, final String collName, final CollectionView view, final SuperGraSession session) {

		simulate(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session s) throws HibernateException, SQLException {

				// Reconstruit l'état en cours de modification des entités
				final SuperGraContext context = new SuperGraContext(s, false, validationInterceptor);
				applyDeltas(session.getDeltas(), context);
				refreshTiersState(session, context);

				view.setKey(key);
				view.setName(collName);

				final HibernateEntity entity = context.getEntity(key);
				if (entity != null) {
					//noinspection unchecked
					final Collection<HibernateEntity> coll = (Collection<HibernateEntity>) getCollection(collName, entity);

					if (coll != null) {
						final List<EntityView> entities = buildEntities(coll, context);
						final CollMetaData collData = analyzeCollection(entity, collName, coll, session);

						final EntityType keyType = EntityType.fromHibernateClass(collData.getPrimaryKeyType());
						final List<Class<? extends HibernateEntity>> concreteClasses = concreteClassByType.get(keyType);

						view.setPrimaryKeyAtt(collData.getPrimaryKey().getName());
						view.setPrimaryKeyType(keyType);
						view.setEntities(entities);
						view.setAttributeNames(collData.getAttributeNames());
						view.setConcreteEntityClasses(concreteClasses);
					}
				}
				return null;
			}
		});
	}

	private static class CollMetaData {

		private List<String> attributeNames;
		private Property primaryKey;
		private Class primaryKeyType;

		public List<String> getAttributeNames() {
			return attributeNames;
		}

		public void setAttributeNames(List<String> attributeNames) {
			this.attributeNames = attributeNames;
		}

		public Property getPrimaryKey() {
			return primaryKey;
		}

		public void setPrimaryKey(Property primaryKey) {
			this.primaryKey = primaryKey;
		}

		public Class getPrimaryKeyType() {
			return primaryKeyType;
		}

		public void setPrimaryKeyType(Class primaryKeyType) {
			this.primaryKeyType = primaryKeyType;
		}
	}

	/**
	 * Analyse la collection et retourne la liste complète des attributs des entités dans la collection, ainsi que d'autres informations intéressantes.
	 *
	 * @param entity   l'entité qui possède la collection à analyser
	 * @param collName le nom de la collection à analyser sur l'entité
	 * @param coll     les entités contenues dans la collection
	 * @param session  la session SuperGra courante
	 * @return la liste des noms d'attributs, le nom de la clé primaire ainsi que son type.
	 */
	private CollMetaData analyzeCollection(HibernateEntity entity, String collName, Collection<HibernateEntity> coll, SuperGraSession session) {

		// Détermine les classes concrètes des éléments
		final Set<Class<? extends HibernateEntity>> classes = new HashSet<>();
		for (HibernateEntity e : coll) {
			classes.add(e.getClass());
		}

		// Détermine l'ensemble des attributs existants
		final Set<String> attributeNames = new HashSet<>();
		Property discriminator = null;
		try {
			for (Class<? extends HibernateEntity> clazz : classes) {
				final MetaEntity meta = MetaEntity.determine(clazz);
				for (Property p : meta.getProperties()) {
					if (!p.isDiscriminator() && !p.isParentForeignKey() && !p.isCollection() && !p.isPrimaryKey()) {
						attributeNames.add(p.getName());
					}
					if (p.isDiscriminator()) {
						discriminator = p;
					}
				}
			}
		}
		catch (MetaException e) {
			throw new RuntimeException(e);
		}

		// Détermine la classe de base des entités
		Class primaryKeyType;
		Property primaryKey = null;
		try {
			PropertyDescriptor collDescr = new PropertyDescriptor(collName, entity.getClass());
			Method getter = collDescr.getReadMethod();
			primaryKeyType = MetaEntity.getGenericParamReturnType(getter);

			final MetaEntity meta = MetaEntity.determine(primaryKeyType);
			for (Property p : meta.getProperties()) {
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

		final List<String> orderedNames = new ArrayList<>(attributeNames);
		Collections.sort(orderedNames);
		if (primaryKey != null) {
			orderedNames.add(0, primaryKey.getName());
		}
		if (discriminator != null) {
			orderedNames.add(1, DISCRIMINATOR_ATTNAME);
		}

		CollMetaData d = new CollMetaData();
		d.setAttributeNames(orderedNames);
		d.setPrimaryKey(primaryKey);
		d.setPrimaryKeyType(primaryKeyType);
		return d;
	}

	private Collection<?> getCollection(String collName, HibernateEntity entity) {
		try {
			return (Collection<?>) ObjectGetterHelper.getValue(entity, collName);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<EntityView> buildEntities(Collection<? extends HibernateEntity> coll, SuperGraContext context) {

		if (coll == null || coll.isEmpty()) {
			return Collections.emptyList();
		}

		final List<EntityView> entities = new ArrayList<>(coll.size());
		for (HibernateEntity e : coll) {
			final EntityView v = new EntityView();
			fillView(e, v, context);
			entities.add(v);
		}

		return entities;
	}

	/**
	 * Renseigne la clé, les attributs et les éventuels résultats de validation pour l'entité spécifiée.
	 *
	 * @param entity  l'entité de référence
	 * @param view    la vue à remplir
	 * @param context le context SuperGra
	 */
	private void fillView(HibernateEntity entity, EntityView view, SuperGraContext context) {
		final Long id = (Long) entity.getKey();
		final EntityType type = EntityType.fromHibernateClass(entity.getClass());

		view.setKey(new EntityKey(type, id));
		view.setAttributes(buildAttributes(entity, context));
		view.setPersonnePhysique(entity instanceof PersonnePhysique);
		view.setMenageCommun(entity instanceof MenageCommun);

		final ValidationResults res = validationService.validate(entity);
		view.setValidationResults(res);
	}

	@Override
	public Long nextId(final Class<? extends HibernateEntity> clazz) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				try {
					final MetaEntity m = MetaEntity.determine(clazz);
					final Sequence sequence = m.getSequence();
					Assert.notNull(sequence);

					final Number id = (Number) sequence.nextValue(dialect, hibernateTemplate, clazz.newInstance());
					return id.longValue();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Override
	public void commitDeltas(final List<Delta> deltas) {
		execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				// Reconstruit l'état en cours de modification des entités
				final SuperGraContext context = new SuperGraContext(session, true, validationInterceptor);
				applyDeltas(deltas, context);
				context.finish();
				return null; // la transaction est committé automatiquement par le template
			}
		});
	}

	@Override
	public void transformPp2Mc(final long ppId, final RegDate dateDebut, @Nullable final RegDate dateFin, final long idPrincipal, @Nullable final Long idSecondaire) {

		execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final String user = AuthenticationHelper.getCurrentPrincipal();

				// Transformation de la personne physique en ménage commun
				final SQLQuery query0 = session.createSQLQuery("UPDATE TIERS SET TIERS_TYPE='MenageCommun', LOG_MDATE=CURRENT_DATE, LOG_MUSER=:muser, " +
						                                               "PP_HABITANT=NULL, " +
						                                               "NUMERO_INDIVIDU=NULL, " +
						                                               "ANCIEN_NUMERO_SOURCIER = null," +
						                                               "NH_NUMERO_ASSURE_SOCIAL = null," +
																	   "NH_NOM_NAISSANCE = null," +
						                                               "NH_NOM = null," +
						                                               "NH_PRENOM = null," +
						                                               "NH_DATE_NAISSANCE = null," +
						                                               "NH_SEXE = null," +
						                                               "NH_NO_OFS_NATIONALITE = null," +
						                                               "NH_LIBELLE_COMMUNE_ORIGINE = null," +
						                                               "NH_LIBELLE_ORIGINE = null," +
						                                               "NH_CANTON_ORIGINE = null," +
						                                               "NH_CAT_ETRANGER = null," +
						                                               "NH_DATE_DEBUT_VALID_AUTORIS = null," +
						                                               "DATE_DECES = null," +
						                                               "MAJORITE_TRAITEE = null, " +
						                                               "INDEX_DIRTY=" + dialect.toBooleanValueString(true) + " WHERE NUMERO=:id AND TIERS_TYPE='PersonnePhysique'");
				query0.setParameter("id", ppId);
				query0.setParameter("muser", user);
				query0.executeUpdate();

				final SQLQuery query1 = session.createSQLQuery("DELETE FROM SITUATION_FAMILLE WHERE CTB_ID=:id OR TIERS_PRINCIPAL_ID=:id");
				query1.setParameter("id", ppId);
				query1.executeUpdate();

				final SQLQuery query2 = session.createSQLQuery("DELETE FROM RAPPORT_ENTRE_TIERS WHERE TIERS_SUJET_ID=:id AND RAPPORT_ENTRE_TIERS_TYPE='AppartenanceMenage'");
				query2.setParameter("id", ppId);
				query2.executeUpdate();

				final SQLQuery query3 = session.createSQLQuery("DELETE FROM RAPPORT_ENTRE_TIERS WHERE (TIERS_SUJET_ID=:id OR TIERS_OBJET_ID=:id) AND RAPPORT_ENTRE_TIERS_TYPE='Parente'");
				query3.setParameter("id", ppId);
				query3.executeUpdate();

				final SQLQuery query4 = session.createSQLQuery("DELETE FROM IDENTIFICATION_PERSONNE WHERE NON_HABITANT_ID=:id");
				query4.setParameter("id", ppId);
				query4.executeUpdate();

				final SQLQuery query5 = session.createSQLQuery("DELETE FROM DROIT_ACCES WHERE TIERS_ID=:id");
				query5.setParameter("id", ppId);
				query5.executeUpdate();

				// Création des rapports entre tiers de type 'appartenance ménage'
				addRapportAppartenanceMenage(ppId, idPrincipal, dateDebut, dateFin, session, user);
				if (idSecondaire != null) {
					addRapportAppartenanceMenage(ppId, idSecondaire, dateDebut, dateFin, session, user);
				}

				return null;
			}

			private void addRapportAppartenanceMenage(long menageId, Long ppId, RegDate dateDebut, RegDate dateFin, Session session, String user) {
				final String sql =
						"INSERT INTO RAPPORT_ENTRE_TIERS (RAPPORT_ENTRE_TIERS_TYPE, ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, TIERS_SUJET_ID, TIERS_OBJET_ID)" +
								"VALUES ('AppartenanceMenage', " + dialect.getSelectSequenceNextValString("hibernate_sequence") + ", CURRENT_DATE, :muser, CURRENT_DATE, :muser, :dateDebut, :dateFin, :idPrincipal, :id)";
				final SQLQuery query5 = session.createSQLQuery(sql);
				query5.setParameter("muser", user);
				query5.setParameter("id", menageId);
				query5.setParameter("idPrincipal", ppId);
				query5.setParameter("dateDebut", dateDebut.index());
				query5.setParameter("dateFin", (dateFin == null ? null : dateFin.index()), StandardBasicTypes.INTEGER);
				query5.executeUpdate();
			}
		});

		// on demande une réindexation du tiers modifié (+ réindexation implicite des tiers liés)
		globalTiersIndexer.schedule(ppId);
	}

	@Override
	public void transformMc2Pp(final long mcId, final long indNo) {

		execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final String user = AuthenticationHelper.getCurrentPrincipal();

				// Transformation du ménage commun en personne physique
				final SQLQuery query0 = session.createSQLQuery("UPDATE TIERS SET TIERS_TYPE='PersonnePhysique', LOG_MDATE=CURRENT_DATE, LOG_MUSER=:muser, INDEX_DIRTY=" +
						                                               dialect.toBooleanValueString(true) + " WHERE NUMERO=:id AND TIERS_TYPE='MenageCommun'");
				query0.setParameter("id", mcId);
				query0.setParameter("muser", user);
				query0.executeUpdate();

				final SQLQuery query1 = session.createSQLQuery("DELETE FROM SITUATION_FAMILLE WHERE CTB_ID=:id");
				query1.setParameter("id", mcId);
				query1.executeUpdate();

				final SQLQuery query2 = session.createSQLQuery("DELETE FROM RAPPORT_ENTRE_TIERS WHERE TIERS_OBJET_ID=:id AND RAPPORT_ENTRE_TIERS_TYPE='AppartenanceMenage'");
				query2.setParameter("id", mcId);
				query2.executeUpdate();

				// Association de la personne physique avec l'individu
				final SQLQuery query3 = session.createSQLQuery("UPDATE TIERS SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=:muser, PP_HABITANT=" +
						                                               dialect.toBooleanValueString(true) + ", NUMERO_INDIVIDU=:indNo, INDEX_DIRTY=" +
						                                               dialect.toBooleanValueString(true) + " WHERE NUMERO=:id");
				query3.setParameter("muser", user);
				query3.setParameter("id", mcId);
				query3.setParameter("indNo", indNo);
				query3.executeUpdate();

				return null;
			}
		});

		// on demande une réindexation du tiers modifié (+ réindexation implicite des tiers liés)
		globalTiersIndexer.schedule(mcId);
	}

	private static String getSuperGraPrincipalName() {
		return String.format("%s-SuperGra", AuthenticationHelper.getCurrentPrincipal());
	}

	/**
	 * Cette méthode contient la définition des builders d'attributs custom.
	 */
	private Map<AttributeKey, AttributeBuilder> initCustomAttributeBuilder() {

		final Map<AttributeKey, AttributeBuilder> builders = new HashMap<>();

		// Appartenance ménage
		builders.put(new AttributeKey(AppartenanceMenage.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "personne physique", PersonnePhysique.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(AppartenanceMenage.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "ménage commun", MenageCommun.class, entity, false, false, false);
			}
		});

		// Contact impôt source
		builders.put(new AttributeKey(ContactImpotSource.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "sourcier", PersonnePhysique.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(ContactImpotSource.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "débiteur", DebiteurPrestationImposable.class, entity, false, false, false);
			}
		});

		// Annule et remplace
		builders.put(new AttributeKey(AnnuleEtRemplace.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "tiers remplacé", Tiers.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(AnnuleEtRemplace.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "tiers remplaçant", Tiers.class, entity, false, false, false);
			}
		});

		// Curatelle
		builders.put(new AttributeKey(Curatelle.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "pupille", PersonnePhysique.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(Curatelle.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "curateur", PersonnePhysique.class, entity, false, false, false);
			}
		});

		// Tutelle
		builders.put(new AttributeKey(Tutelle.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "pupille", PersonnePhysique.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(Tutelle.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "tuteur", PersonnePhysique.class, entity, false, false, false);
			}
		});

		// Conseil légal
		builders.put(new AttributeKey(ConseilLegal.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "pupille", PersonnePhysique.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(ConseilLegal.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "conseiller légal", PersonnePhysique.class, entity, false, false, false);
			}
		});

		// Représentation conventionnel
		builders.put(new AttributeKey(RepresentationConventionnelle.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "représenté", Tiers.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(RepresentationConventionnelle.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "représentant", Tiers.class, entity, false, false, false);
			}
		});

		// Assujettissement par substitution
		builders.put(new AttributeKey(AssujettissementParSubstitution.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "substitué", Tiers.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(AssujettissementParSubstitution.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "substituant", Tiers.class, entity, false, false, false);
			}
		});

		// Activité économique
		builders.put(new AttributeKey(ActiviteEconomique.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "personne", Tiers.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(ActiviteEconomique.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "établissement", Etablissement.class, entity, false, false, false);
			}
		});

		// Rapport de prestation imposable
		builders.put(new AttributeKey(RapportPrestationImposable.class, "sujetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "contribuable", Contribuable.class, entity, false, false, false);
			}
		});
		builders.put(new AttributeKey(RapportPrestationImposable.class, "objetId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "débiteur", DebiteurPrestationImposable.class, entity, false, false, false);
			}
		});

		// Situation de famille ménage-commun
		builders.put(new AttributeKey(SituationFamilleMenageCommun.class, "contribuablePrincipalId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "contribuable principal", PersonnePhysique.class, entity, false, false, false);
			}
		});

		// Adresse autre tiers
		builders.put(new AttributeKey(AdresseAutreTiers.class, "autreTiersId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "autre tiers", Tiers.class, entity, false, false, false);
			}
		});

		// Déclaration impôt ordinaire
		builders.put(new AttributeKey(DeclarationImpotOrdinaire.class, "retourCollectiviteAdministrativeId"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
				return new AttributeView(p.getName(), "retour collectivité administrative", CollectiviteAdministrative.class, entity, false, false, false);
			}
		});

		// [SIFISC-927] auto-completion du numéro d'ordre poste dans les adresses suisses.
		builders.put(new AttributeKey(AdresseSuisse.class, "numeroOrdrePoste"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				return new AttributeView("localite", p.getName(), "localité", Integer.class, value, InfraCategory.LOCALITE, false);
			}
		});

		// [SIFISC-927] auto-completion du numéro de rue dans les adresses suisses.
		builders.put(new AttributeKey(AdresseSuisse.class, "numeroRue"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				return new AttributeView("rue", p.getName(), "rue", Integer.class, value, InfraCategory.RUE, false);
			}
		});

		// [SIFISC-12519] le texte des remarques des tiers est éditable dans une textarea
		builders.put(new AttributeKey(Remarque.class, "texte"), new AttributeBuilder() {
			@Override
			public AttributeView build(Property p, Object value, SuperGraContext context) {
				return new AttributeView("texte", MultilineString.class, value, false, false, false);
			}
		});

		return builders;
	}
}
