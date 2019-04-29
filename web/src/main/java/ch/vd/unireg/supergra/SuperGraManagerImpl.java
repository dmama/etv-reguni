package ch.vd.unireg.supergra;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.StandardBasicTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseAutreTiers;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.ReflexionUtils;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;
import ch.vd.unireg.data.FiscalDataEventListener;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.etiquette.ActionAutoEtiquette;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.EtiquetteTiers;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.foncier.AllegementFoncier;
import ch.vd.unireg.hibernate.ActionAutoEtiquetteUserType;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.hibernate.meta.MetaEntity;
import ch.vd.unireg.hibernate.meta.MetaException;
import ch.vd.unireg.hibernate.meta.Property;
import ch.vd.unireg.hibernate.meta.Sequence;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.json.InfraCategory;
import ch.vd.unireg.mouvement.MouvementDossier;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscale;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DescriptionBatimentRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleAvecQuotePartRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ImplantationRF;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.PrincipalCommunauteRF;
import ch.vd.unireg.registrefoncier.QuotePartRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.RegroupementCommunauteRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;
import ch.vd.unireg.registrefoncier.dataimport.processor.CommunauteRFProcessor;
import ch.vd.unireg.reqdes.ErreurTraitement;
import ch.vd.unireg.reqdes.PartiePrenante;
import ch.vd.unireg.reqdes.RolePartiePrenante;
import ch.vd.unireg.reqdes.UniteTraitement;
import ch.vd.unireg.supergra.delta.AttributeUpdate;
import ch.vd.unireg.supergra.delta.Delta;
import ch.vd.unireg.supergra.delta.DisableEntity;
import ch.vd.unireg.supergra.view.AttributeView;
import ch.vd.unireg.supergra.view.CollectionView;
import ch.vd.unireg.supergra.view.EntityView;
import ch.vd.unireg.taglibs.formInput.MultilineString;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.AdministrationEntreprise;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AnnuleEtRemplace;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.AssujettissementParSubstitution;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ConseilLegal;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.Curatelle;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.DonneeCivileEntreprise;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.FusionEntreprises;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.IdentificationPersonne;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.tiers.RepresentationConventionnelle;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.tiers.ScissionEntreprise;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.SocieteDirection;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.TransfertPatrimoine;
import ch.vd.unireg.tiers.Tutelle;
import ch.vd.unireg.validation.ValidationInterceptor;

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
	private FiscalDataEventListener autorisationCache;
	private CommunauteRFProcessor communauteRFProcessor;

	private List<String> annotatedClass;
	private final Map<EntityType, List<Class<? extends HibernateEntity>>> concreteClassByType = new EnumMap<>(EntityType.class);

	/**
	 * Les propriétés et entités qui ne doivent pas être changées, même en mode SuperGra.
	 */
	private static final Set<String> readonlyProps = buildReadOnlyPropSet();
	private static final Set<Class<? extends HibernateEntity>> readonlyEntities = Collections.singleton(RegroupementCommunauteRF.class);
	private static final Map<Class<? extends HibernateEntity>, Set<String>> readonlyPropsByEntity = buildReadOnlyPropByEntitySet();

	private static Set<String> buildReadOnlyPropSet() {
		final Set<String> readonlyProps = new HashSet<>();

		// général
		readonlyProps.add("annulationDate");
		readonlyProps.add("annulationUser");
		readonlyProps.add("logCreationDate");
		readonlyProps.add("logCreationUser");
		readonlyProps.add("logModifDate");
		readonlyProps.add("logModifUser");

		return readonlyProps;
	}

	private static Map<Class<? extends HibernateEntity>, Set<String>> buildReadOnlyPropByEntitySet() {
		final HashMap<Class<? extends HibernateEntity>, Set<String>> map = new HashMap<>();
		map.put(Declaration.class, new HashSet<>(Arrays.asList("modeleDocument", "periode")));
		map.put(CommunauteRF.class, new HashSet<>(Collections.singletonList("regroupements")));
		map.put(ModeleCommunauteRF.class, new HashSet<>(Arrays.asList("regroupements", "membres", "membresHashCode")));
		return map;
	}

	/**
	 * Les propriétés qui représentent des données techniques, non-métier et pas indispensables à afficher en mode condensé.
	 */
	private static final Set<String> detailsProps = buildDetailsPropSet();

	private static Set<String> buildDetailsPropSet() {
		final Set<String> detailsProps = new HashSet<>();
		detailsProps.add("annulationDate");
		detailsProps.add("annulationUser");
		detailsProps.add("logCreationDate");
		detailsProps.add("logCreationUser");
		detailsProps.add("logModifDate");
		detailsProps.add("logModifUser");
		return detailsProps;
	}

	/**
	 * Les relations enfant->parent connues.
	 */
	private static final Map<Class<? extends HibernateEntity>, Class<? extends HibernateEntity>> childToParentRelationships = buildChildToParentRelationshipMap();

	private static Map<Class<? extends HibernateEntity>, Class<? extends HibernateEntity>> buildChildToParentRelationshipMap() {
		final Map<Class<? extends HibernateEntity>, Class<? extends HibernateEntity>> childToParentRelationships = new HashMap<>();

		// à compléter...
		childToParentRelationships.put(AdresseTiers.class, Tiers.class);
		childToParentRelationships.put(CoordonneesFinancieres.class, Tiers.class);
		childToParentRelationships.put(Declaration.class, Tiers.class);
		childToParentRelationships.put(ForFiscal.class, Tiers.class);
		childToParentRelationships.put(Remarque.class, Tiers.class);
		childToParentRelationships.put(EtiquetteTiers.class, Tiers.class);
		childToParentRelationships.put(MouvementDossier.class, Contribuable.class);
		childToParentRelationships.put(IdentificationEntreprise.class, Contribuable.class);
		childToParentRelationships.put(AdresseMandataire.class, Contribuable.class);
		childToParentRelationships.put(DecisionAci.class, Contribuable.class);
		childToParentRelationships.put(DroitAcces.class, Contribuable.class);
		childToParentRelationships.put(AllegementFoncier.class, Contribuable.class);
		childToParentRelationships.put(Periodicite.class, DebiteurPrestationImposable.class);
		childToParentRelationships.put(RegimeFiscal.class, Entreprise.class);
		childToParentRelationships.put(DonneeCivileEntreprise.class, Entreprise.class);
		childToParentRelationships.put(AllegementFiscal.class, Entreprise.class);
		childToParentRelationships.put(Bouclement.class, Entreprise.class);
		childToParentRelationships.put(EtatEntreprise.class, Entreprise.class);
		childToParentRelationships.put(FlagEntreprise.class, Entreprise.class);
		childToParentRelationships.put(AutreDocumentFiscal.class, Entreprise.class);
		childToParentRelationships.put(SituationFamille.class, ContribuableImpositionPersonnesPhysiques.class);
		childToParentRelationships.put(IdentificationPersonne.class, PersonnePhysique.class);
		childToParentRelationships.put(DomicileEtablissement.class, Etablissement.class);
		childToParentRelationships.put(EtatDeclaration.class, Declaration.class);
		childToParentRelationships.put(DelaiDeclaration.class, Declaration.class);
		childToParentRelationships.put(ModeleFeuilleDocument.class, ModeleDocument.class);
		childToParentRelationships.put(ParametrePeriodeFiscale.class, PeriodeFiscale.class);
		childToParentRelationships.put(EvenementCivilEchErreur.class, EvenementCivilEch.class);
		childToParentRelationships.put(EvenementCivilRegPPErreur.class, EvenementCivilRegPP.class);
		childToParentRelationships.put(EvenementEntrepriseErreur.class, EvenementEntreprise.class);
//		childToParentRelationships.put(MouvementDossier.class, BordereauMouvementDossier.class);
		childToParentRelationships.put(DescriptionBatimentRF.class, BatimentRF.class);
		childToParentRelationships.put(ImplantationRF.class, BatimentRF.class);
		childToParentRelationships.put(SituationRF.class, ImmeubleRF.class);
		childToParentRelationships.put(SurfaceTotaleRF.class, ImmeubleRF.class);
		childToParentRelationships.put(EstimationRF.class, ImmeubleRF.class);
		childToParentRelationships.put(RaisonAcquisitionRF.class, DroitProprieteRF.class);
		childToParentRelationships.put(QuotePartRF.class, ImmeubleAvecQuotePartRF.class);
		childToParentRelationships.put(CommunauteRF.class, RegroupementCommunauteRF.class);
		childToParentRelationships.put(ModeleCommunauteRF.class, PrincipalCommunauteRF.class);
		childToParentRelationships.put(RolePartiePrenante.class, PartiePrenante.class);
		childToParentRelationships.put(ErreurTraitement.class, UniteTraitement.class);

		return childToParentRelationships;
	}

	/**
	 * Les propriétés qui ne doivent pas apparaître du tout
	 */
	private static final Map<Class<? extends HibernateEntity>, Set<String>> invisibleProps = buildInvisiblePropMap();

	private static Map<Class<? extends HibernateEntity>, Set<String>> buildInvisiblePropMap() {
		final Map<Class<? extends HibernateEntity>, Set<String>> invisibleProps = new HashMap<>();
		addInvisibleProperty(invisibleProps, Tiers.class, "droitsAccesAppliques");
		return invisibleProps;
	}

	private static void addInvisibleProperty(Map<Class<? extends HibernateEntity>, Set<String>> map,
	                                         Class<? extends HibernateEntity> clazz, String propName) {
		final Set<String> set = map.computeIfAbsent(clazz, c -> new HashSet<>());
		set.add(propName);
	}

	/**
	 * @param clazz une classe (concrete ou pas) d'objet persistent
	 * @return la liste aggrégée des attributs qui ne doivent pas apparaître dans la liste des attributs d'une instance de la classe donnée
	 * parce qu'ils ont été indiqués comme invisible soit sur la classe elle-même, soit sur une de ses classes parentes
	 */
	private static Set<String> getInvisibleProperties(Class<? extends HibernateEntity> clazz) {
		final Set<String> set = new HashSet<>();
		Class<?> cursor = clazz;
		while (HibernateEntity.class.isAssignableFrom(cursor)) {
			//noinspection unchecked,SuspiciousMethodCalls
			final Set<String> locallyInvisible = invisibleProps.get(cursor);
			if (locallyInvisible != null) {
				set.addAll(locallyInvisible);
			}
			cursor = cursor.getSuperclass();
		}
		return set.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(set);
	}

	/**
	 * Les builders custom d'attributs qui permettent de spécialiser l'affichage de certains attributs.
	 * (construit en lazy-init pour éviter le ralentissement applicatif au démarrage)
	 *
	 * @see #getCustomAttributeBuilder(ch.vd.unireg.supergra.SuperGraManagerImpl.AttributeKey)
	 */
	private Map<AttributeKey, AttributeBuilder> attributeCustomBuilders = null;

	private interface AttributeBuilder {
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
			if (entityClass == null) {
				throw new IllegalArgumentException();
			}
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

	public void setAutorisationCache(FiscalDataEventListener autorisationCache) {
		this.autorisationCache = autorisationCache;
	}

	public void setCommunauteRFProcessor(CommunauteRFProcessor communauteRFProcessor) {
		this.communauteRFProcessor = communauteRFProcessor;
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
					final List<Class<? extends HibernateEntity>> list = concreteClassByType.computeIfAbsent(t, k -> new ArrayList<>());
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
		return template.execute(status -> {
			status.setRollbackOnly();
			return hibernateTemplate.execute(action);
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
			return template.execute(status -> hibernateTemplate.execute(action));
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Override
	public void fillView(final EntityKey key, final EntityView view, final SuperGraSession session) {

		simulate(s -> {

			// Reconstruit l'état en cours de modification des entités
			final SuperGraContext context = new SuperGraContext(s, false, validationInterceptor, communauteRFProcessor);
			applyDeltas(session.getDeltas(), context);
			refreshEntityStates(session, context);

			view.setKey(key);

			final HibernateEntity entity = context.getEntity(key);
			if (entity != null) {
				fillView(entity, view, context);

				if (TOP_ENTITY_TYPES.contains(key.getType()) && !context.isNewlyCreated(key)) {
					// si on affiche une top entité, on en profite pour mémoriser sa clé de manière à pouvoir revenir sur elle après le commit
					session.setLastKnownTopEntity(key);
				}
			}
			return null;
		});
	}

	/**
	 * Les types principaux d'entités sur lesquelles on va déclencher une validation
	 */
	private static final Set<EntityType> TOP_ENTITY_TYPES = EnumSet.of(EntityType.Tiers,
	                                                                   EntityType.AyantDroitRF,
	                                                                   EntityType.DroitRF,
	                                                                   EntityType.ImmeubleRF,
	                                                                   EntityType.BatimentRF,
	                                                                   EntityType.RapprochementRF);

	/**
	 * Met-à-jour l'état des entités modifiées dans une session SuperGra.
	 *
	 * @param session une session SuperGra.
	 * @param context le context DAO spécifique au mode SuperGra.
	 */
	private void refreshEntityStates(SuperGraSession session, SuperGraContext context) {

		// Récupère tous les entités principales impactés par les deltas
		final Map<EntityKey, HibernateEntity> mainEntities = new HashMap<>();
		final List<Delta> deltas = session.getDeltas();
		for (Delta d : deltas) {
			final List<EntityKey> keys = d.getAllKeys();
			for (EntityKey key : keys) {
				final HibernateEntity entity = context.getEntity(key);
				// l'entité elle-même s'il s'agit d'une entité principale
				if (isAnyInstanceOf(entity, TOP_ENTITY_TYPES)) {
					if (!mainEntities.containsKey(key)) {
						mainEntities.put(key, entity);
					}
				}
				// [SIFISC-29450] les éventuelles entités principales liées à l'entité courante doivent être prises en compte même si l'entité courante est une entité principale
				//                (par exemple, un droit RF qui pointe vers une communauté RF : les deux entités sont des entités principales)
				if (entity instanceof LinkedEntity) {
					for (EntityType entityType : TOP_ENTITY_TYPES) {
						addLinkedEntities(mainEntities, (LinkedEntity) entity, entityType.getHibernateClass(), entityType, isAnnulation(d));
					}
				}
			}
		}

		// Détermine la validité de toutes les entités
		final List<EntityState> entityStates = new ArrayList<>(mainEntities.size());
		for (HibernateEntity e : mainEntities.values()) {
			final ValidationResults res = validationService.validate(e);
			entityStates.add(new EntityState(new EntityKey(EntityType.fromHibernateClass(e.getClass()), (Long) e.getKey()), res));
		}

		// Met-à-jour la session
		session.setEntityStates(entityStates);
	}

	private <T extends HibernateEntity> void addLinkedEntities(@NotNull Map<EntityKey, HibernateEntity> mainEntities, @NotNull LinkedEntity entity, @NotNull Class<T> clazz, @NotNull EntityType entityType, boolean includeAnnuled) {
		final Set<T> linked = tiersService.getLinkedEntities(entity, clazz, new LinkedEntityContext(LinkedEntityPhase.VALIDATION, hibernateTemplate), includeAnnuled);
		for (T t : linked) {
			if (t != null) {
				final EntityKey key = new EntityKey(entityType, (Long) t.getKey());
				if (!mainEntities.containsKey(key)) {
					mainEntities.put(key, t);
				}
			}
		}
	}

	private static boolean isAnyInstanceOf(HibernateEntity entity, Set<EntityType> topEntityTypes) {
		if (entity != null) {
			for (EntityType entityType : topEntityTypes) {
				if (entityType.getHibernateClass().isAssignableFrom(entity.getClass())) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isAnnulation(Delta delta) {
		boolean isAnnulation = false;
		if (delta instanceof AttributeUpdate) {
			AttributeUpdate update = (AttributeUpdate) delta;
			isAnnulation = "annulationDate".equals(update.getName()) && update.getOldValue() == null && update.getNewValue() != null;
		}
		else if (delta instanceof DisableEntity) {
			isAnnulation = true;
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

		final Set<String> readonlyProps = getReadonlyPropsFor(entity);

		final List<AttributeView> attributes = new ArrayList<>();
		try {
			final MetaEntity meta = MetaEntity.determine(entity.getClass());
			final List<Property> props = meta.getProperties();
			final Set<String> invisibleProperties = getInvisibleProperties(entity.getClass());
			for (int i = 0, propsSize = props.size(); i < propsSize; i++) {
				final Property p = props.get(i);
				if (invisibleProperties.contains(p.getName())) {
					continue;
				}

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
						final boolean readonly = p.isPrimaryKey() || readonlyEntities.contains(entity.getClass()) || readonlyProps.contains(propName) || isPropertyToParent(entity.getClass(), p);
						attributeView = new AttributeView(propName, p.getType().getJavaType(), value, p.isEntityForeignKey(), false, readonly);
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

	@NotNull
	private static Set<String> getReadonlyPropsFor(@NotNull HibernateEntity entity) {
		Set<String> set = readonlyPropsByEntity.get(entity.getClass());
		if (set == null || set.isEmpty()) {
			return readonlyProps;
		}
		else {
			return Stream.concat(set.stream(), readonlyProps.stream())
					.collect(Collectors.toSet());
		}
	}

	/**
	 * @param entityClass la classe d'une entité
	 * @param property    une propriété de la classe spécifiée
	 * @return <b>vrai</b> si la propriété de l'entité pointe vers son entité parente (= qui la possède au sens conceptuel); <b>faux</b> autrement.
	 */
	public static boolean isPropertyToParent(Class<? extends HibernateEntity> entityClass, Property property) {

		Class<?> clazz = entityClass;
		while (clazz != HibernateEntity.class) {
			final Class<? extends HibernateEntity> parentClass = childToParentRelationships.get(clazz);
			if (parentClass != null) {
				final Class<?> propClass = property.getType().getJavaType();
				return parentClass.isAssignableFrom(propClass);
			}
			clazz = clazz.getSuperclass();
		}
		return false;
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

		simulate(s -> {

			// Reconstruit l'état en cours de modification des entités
			final SuperGraContext context = new SuperGraContext(s, false, validationInterceptor, communauteRFProcessor);
			applyDeltas(session.getDeltas(), context);
			refreshEntityStates(session, context);

			view.setKey(key);
			view.setName(collName);

			final HibernateEntity entity = context.getEntity(key);
			if (entity != null) {

				final Set<String> readonlyProps = getReadonlyPropsFor(entity);

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
					view.setReadonly(readonlyEntities.contains(entity.getClass()) || readonlyProps.contains(collName));
				}
			}
			return null;
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
					if (!p.isDiscriminator() && !p.isEntityForeignKey() && !p.isCollection() && !p.isPrimaryKey()) {
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
		Class<?> primaryKeyType;
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
			return (Collection<?>) PropertyUtils.getProperty(entity, collName);
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
		entities.sort(Comparator.comparingLong(o -> o.getKey().getId()));

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
		view.setCommunauteRF(entity instanceof CommunauteRF);
		view.setReadonly(readonlyEntities.contains(entity.getClass()));

		final ValidationResults res = validationService.validate(entity);
		view.setValidationResults(res);
	}

	@Override
	public Long nextId(final Class<? extends HibernateEntity> clazz) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(status -> {
			try {
				final MetaEntity m = MetaEntity.determine(clazz);
				final Sequence sequence = m.getSequence();
				if (sequence == null) {
					throw new IllegalArgumentException();
				}

				final Number id = (Number) sequence.nextValue(dialect, hibernateTemplate, clazz.newInstance());
				return id.longValue();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void commitDeltas(final List<Delta> deltas) {
		execute(session -> {
			// Reconstruit l'état en cours de modification des entités
			final SuperGraContext context = new SuperGraContext(session, true, validationInterceptor, communauteRFProcessor);
			applyDeltas(deltas, context);
			context.finish();
			return null; // la transaction est committé automatiquement par le template
		});
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

	@Override
	public void transformPp2Mc(final long ppId, final RegDate dateDebut, @Nullable final RegDate dateFin, final long idPrincipal, @Nullable final Long idSecondaire) {

		execute(session -> {
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
		});

		// on demande une réindexation du tiers modifié (+ réindexation implicite des tiers liés)
		globalTiersIndexer.schedule(ppId);
		//On invalide les caches de sécurité
		autorisationCache.onTiersChange(ppId);
	}

	@Override
	public void transformMc2Pp(final long mcId, final long indNo) {

		execute(session -> {

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
		});

		// on demande une réindexation du tiers modifié (+ réindexation implicite des tiers liés)
		globalTiersIndexer.schedule(mcId);
		//On invalide les caches de sécurité
		autorisationCache.onTiersChange(mcId);
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
		addRapportEntreTiersBuilder(builders, AppartenanceMenage.class, "personne physique", PersonnePhysique.class, "ménage commun", MenageCommun.class);

		// Contact impôt source
		addRapportEntreTiersBuilder(builders, ContactImpotSource.class, "sourcier", PersonnePhysique.class, "employeur", DebiteurPrestationImposable.class);

		// Annule et remplace
		addRapportEntreTiersBuilder(builders, AnnuleEtRemplace.class, "tiers remplacé", Tiers.class, "tiers remplaçant", Tiers.class);

		// Curatelle
		addRapportEntreTiersBuilder(builders, Curatelle.class, "pupille", PersonnePhysique.class, "curateur", PersonnePhysique.class, "autorité tutélaire", CollectiviteAdministrative.class);

		// Tutelle
		addRapportEntreTiersBuilder(builders, Tutelle.class, "pupille", PersonnePhysique.class, "tuteur", PersonnePhysique.class, "autorité tutélaire", CollectiviteAdministrative.class);

		// Conseil légal
		addRapportEntreTiersBuilder(builders, ConseilLegal.class, "pupille", PersonnePhysique.class, "conseiller légal", PersonnePhysique.class, "autorité tutélaire", CollectiviteAdministrative.class);

		// Représentation conventionnel
		addRapportEntreTiersBuilder(builders, RepresentationConventionnelle.class, "représenté", Tiers.class, "représentant", Tiers.class);

		// Assujettissement par substitution
		addRapportEntreTiersBuilder(builders, AssujettissementParSubstitution.class, "substitué", Tiers.class, "substituant", Tiers.class);

		// Rapport de prestation imposable
		addRapportEntreTiersBuilder(builders, RapportPrestationImposable.class, "contribuable", Contribuable.class, "débiteur", DebiteurPrestationImposable.class);

		// Rapport de parenté
		addRapportEntreTiersBuilder(builders, Parente.class, "enfant", PersonnePhysique.class, "parent", PersonnePhysique.class);

		// Activité économique
		addRapportEntreTiersBuilder(builders, ActiviteEconomique.class, "personne", Contribuable.class, "établissement", Etablissement.class);

		// Mandat
		addRapportEntreTiersBuilder(builders, Mandat.class, "mandant", Contribuable.class, "mandataire", Tiers.class);

		// Héritage
		addRapportEntreTiersBuilder(builders, Heritage.class, "héritier", PersonnePhysique.class, "défunt", PersonnePhysique.class);

		// Fusion d'entreprises
		addRapportEntreTiersBuilder(builders, FusionEntreprises.class, "avant fusion", Entreprise.class, "après fusion", Entreprise.class);

		// Société de direction
		addRapportEntreTiersBuilder(builders, SocieteDirection.class, "propriétaire", Entreprise.class, "fonds de placement", Entreprise.class);

		// Scission d'entreprises
		addRapportEntreTiersBuilder(builders, ScissionEntreprise.class, "avant scission", Entreprise.class, "après scission", Entreprise.class);

		// Administration d'entreprise
		addRapportEntreTiersBuilder(builders, AdministrationEntreprise.class, "entreprise administrée", Entreprise.class, "administrateur", PersonnePhysique.class);

		// Transfert de patrimoine
		addRapportEntreTiersBuilder(builders, TransfertPatrimoine.class, "entreprise émettrice", Entreprise.class, "entreprise réceptrice", Entreprise.class);

		// Situation de famille ménage-commun
		builders.put(new AttributeKey(SituationFamilleMenageCommun.class, "contribuablePrincipalId"), (p, value, context) -> {
			final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
			return new AttributeView(p.getName(), "contribuable principal", PersonnePhysique.class, entity, false, false, false);
		});

		// Adresse autre tiers
		builders.put(new AttributeKey(AdresseAutreTiers.class, "autreTiersId"), (p, value, context) -> {
			final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
			return new AttributeView(p.getName(), "autre tiers", Tiers.class, entity, false, false, false);
		});

		// Déclaration impôt ordinaire
		builders.put(new AttributeKey(DeclarationImpotOrdinaire.class, "retourCollectiviteAdministrativeId"), (p, value, context) -> {
			final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
			return new AttributeView(p.getName(), "retour collectivité administrative", CollectiviteAdministrative.class, entity, false, false, false);
		});

		// [SIFISC-927] auto-completion du numéro d'ordre poste dans les adresses suisses.
		builders.put(new AttributeKey(AdresseSuisse.class, "numeroOrdrePoste"), (p, value, context) -> new AttributeView("localite", p.getName(), "localité", Integer.class, value, InfraCategory.LOCALITE, false));

		// [SIFISC-927] auto-completion du numéro de rue dans les adresses suisses.
		builders.put(new AttributeKey(AdresseSuisse.class, "numeroRue"), (p, value, context) -> new AttributeView("rue", p.getName(), "rue", Integer.class, value, InfraCategory.RUE, false));

		// [SIFISC-12519] le texte des remarques des tiers est éditable dans une textarea
		builders.put(new AttributeKey(Remarque.class, "texte"), (p, value, context) -> new AttributeView("texte", MultilineString.class, value, false, false, false));

		// [SIFISC-28363] action sur décès liée à une étiquette
		builders.put(new AttributeKey(Etiquette.class, "actionSurDeces"), (p, value, context) -> {
			final String valueAsString = value == null ? "" : ActionAutoEtiquetteUserType.ACTION_RENDERER.toString((ActionAutoEtiquette) value);
			// readonly : il s'agit d'une règle métier qui pourrait être éditer, mais il faudrait une vérification de la syntaxe
			return new AttributeView(p.getName(), String.class, valueAsString, false, false, true);
		});

		return builders;
	}

	private static <T extends RapportEntreTiers> void addRapportEntreTiersBuilder(Map<AttributeKey, AttributeBuilder> map,
	                                                                              Class<T> rapportClass,
	                                                                              String displayNameSujet,
	                                                                              Class<? extends Tiers> tiersSujetClass,
	                                                                              String displayNameObjet,
	                                                                              Class<? extends Tiers> tiersObjetClass) {
		final AttributeKey sujetKey = new AttributeKey(rapportClass, "sujetId");
		map.put(sujetKey, (p, value, context) -> {
			final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
			return new AttributeView(p.getName(), displayNameSujet, tiersSujetClass, entity, false, false, false);
		});

		final AttributeKey objectKey = new AttributeKey(rapportClass, "objetId");
		map.put(objectKey, (p, value, context) -> {
			final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
			return new AttributeView(p.getName(), displayNameObjet, tiersObjetClass, entity, false, false, false);
		});
	}

	private static <T extends RepresentationLegale> void addRapportEntreTiersBuilder(Map<AttributeKey, AttributeBuilder> map,
	                                                                                 Class<T> rapportClass,
	                                                                                 String displayNameSujet,
	                                                                                 Class<? extends Tiers> tiersSujetClass,
	                                                                                 String displayNameObjet,
	                                                                                 Class<? extends Tiers> tiersObjetClass,
	                                                                                 String displayNameAutoriteTutelaire,
	                                                                                 Class<? extends Tiers> autoriteTutelaireClass) {

		addRapportEntreTiersBuilder(map, rapportClass, displayNameSujet, tiersSujetClass, displayNameObjet, tiersObjetClass);

		final AttributeKey autoriteTutelaireId = new AttributeKey(rapportClass, "autoriteTutelaireId");
		map.put(autoriteTutelaireId, (p, value, context) -> {
			final HibernateEntity entity = (value == null ? null : context.getEntity(new EntityKey(EntityType.Tiers, (Long) value)));
			return new AttributeView(p.getName(), displayNameAutoriteTutelaire, autoriteTutelaireClass, entity, false, false, false);
		});
	}
}
