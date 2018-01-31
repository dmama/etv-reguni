package ch.vd.uniregctb.evenement.docsortant;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.xml.event.docsortant.retour.v3.DocumentQuittance;
import ch.vd.unireg.xml.event.docsortant.retour.v3.Quittance;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRappelee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.documentfiscal.AutorisationRadiationRC;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecSuivi;
import ch.vd.uniregctb.documentfiscal.DemandeBilanFinal;
import ch.vd.uniregctb.documentfiscal.EtatDocumentFiscalAvecDocumentArchive;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.uniregctb.efacture.DocumentEFacture;
import ch.vd.uniregctb.efacture.DocumentEFactureDAO;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;

public class RetourDocumentSortantHandlerImpl implements RetourDocumentSortantHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(RetourDocumentSortantHandlerImpl.class);

	private HibernateTemplate hibernateTemplate;
	private DocumentEFactureDAO documentEFactureDAO;
	private Map<TypeDocumentSortant, TraitementRetour> traitements;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setDocumentEFactureDAO(DocumentEFactureDAO documentEFactureDAO) {
		this.documentEFactureDAO = documentEFactureDAO;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.traitements = buildTraitementsById(this.hibernateTemplate, this.documentEFactureDAO);
	}

	@Override
	public void onQuittance(Quittance quittance, Map<String, String> headers) throws EsbBusinessException {

		// plusieurs étapes :
		// - retrouver le document sortant correspondant
		// - lui attribuer la clé REPELEC pour la visualisation

		final String missingHeaders = Stream.of(TYPE_DOCUMENT_HEADER_NAME, ID_ENTITE_DOCUMENT_ANNONCE_HEADER_NAME)
				.filter(name -> StringUtils.isBlank(headers.get(name)))
				.collect(Collectors.joining(", "));
		if (StringUtils.isNotBlank(missingHeaders)) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Meta-donnée(s) manquante(s) : " + missingHeaders, null);
		}

		final String typeDocumentString = headers.get(TYPE_DOCUMENT_HEADER_NAME);
		final String idString = headers.get(ID_ENTITE_DOCUMENT_ANNONCE_HEADER_NAME);
		final TypeDocumentSortant typeDocument = EnumUtils.getEnum(TypeDocumentSortant.class, typeDocumentString);
		final TraitementRetour traitement = typeDocument != null ? traitements.get(typeDocument) : null;
		if (traitement == null) {
			// pas de traitement connu... on ne fait rien
			LOGGER.info("Aucun traitement connu pour la quittance du type de document sortant " + typeDocument);
		}
		else {

			// trouvons maintenant la clé RepElec
			final Optional<String> key = quittance.getDocumentsQuittances().getDocumentQuittance().stream()
					.map(DocumentQuittance::getIdentifiantRepelecDossier)
					.findFirst();
			if (!key.isPresent()) {
				throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Aucun identifiant RepElec trouvé.", null);
			}

			// maintenant, on a tout, il faut faire le boulot et rentrer
			traitement.assignKey(idString, key.get());

			// un peu de log et c'est fini
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Assignation de l'identifiant RepElec '" + key.get() + "' au document '" + idString + "' de type " + typeDocument);
			}
		}
	}

	/**
	 * Interface d'assignation d'une clé RepElec (1-256 charactères) à une entité
	 * @param <T> type de l'entité à laquelle la clé doit être assignée
	 */
	@FunctionalInterface
	private interface KeyAssignator<T> {
		void assignKey(T entity, String key);
	}

	/**
	 * Interface des composants capable de récupérer une entité (à laquelle associer la clé RepElec au travers d'un composant {@link KeyAssignator&lt;T&gt;})
	 * @param <T> type de l'entité à récupérer
	 */
	@FunctionalInterface
	private interface EntityFetcher<T> {
		T fetchEntity(String id);
	}

	private static class EntityFetcherById<E> implements EntityFetcher<E> {
		private final Function<String, ? extends E> fetcher;

		public <K> EntityFetcherById(Function<String, K> entityIdExtractor, Function<? super K, ? extends E> entityFetcher) {
			this.fetcher = entityIdExtractor.andThen(entityFetcher);
		}

		@Override
		public E fetchEntity(String id) {
			return fetcher.apply(id);
		}
	}

	private static class HibernateEntityFetcherById<T extends HibernateEntity> extends EntityFetcherById<T> {
		public HibernateEntityFetcherById(HibernateTemplate hibernateTemplate, Class<T> clazz) {
			super(Long::valueOf,
			      id -> hibernateTemplate.get(clazz, id));
		}
	}

	private static class DocumentEFactureFetcher extends EntityFetcherById<DocumentEFacture> {
		public DocumentEFactureFetcher(DocumentEFactureDAO dao) {
			super(DocumentEFactureHelper::decodeIdentifiant,
			      id -> dao.findByTiersEtCleArchivage(id.getLeft(), id.getRight()));
		}
	}

	/**
	 * Interface du moteur de traitement d'une quittance d'annonce de document sortant
	 */
	@FunctionalInterface
	private interface TraitementRetour {
		void assignKey(String idEntity, String key);
	}

	private static class TraitementRetourImpl<E> implements TraitementRetour {
		private final EntityFetcher<E> entityFetcher;
		private final KeyAssignator<? super E> repelecKeyAssignator;

		public TraitementRetourImpl(EntityFetcher<E> entityFetcher, KeyAssignator<? super E> repelecKeyAssignator) {
			this.entityFetcher = entityFetcher;
			this.repelecKeyAssignator = repelecKeyAssignator;
		}

		@Override
		public void assignKey(String idEntity, String key) {
			final E entity = entityFetcher.fetchEntity(idEntity);
			repelecKeyAssignator.assignKey(entity, key);
		}
	}

	/**
	 * Ajoute des mappings TypeDocumentSortant -> traitementRetour pour tous les types donnés dans la map donnée
	 * @param map map à remplir
	 * @param types types auxquels associer le traitement
	 * @param traitementRetour traitement à associer aux types
	 * @throws IllegalArgumentException si l'un des types de document sortant n'est pas marqué comme {@link TypeDocumentSortant#isQuittanceAnnonceDemandee()}, ou si un mapping existe déjà pour l'un des types donnés
	 */
	private static void addToTraitementMap(Map<TypeDocumentSortant, TraitementRetour> map,
	                                       Set<TypeDocumentSortant> types,
	                                       TraitementRetour traitementRetour) {
		types.stream()
				.peek(type -> {
					if (!type.isQuittanceAnnonceDemandee()) {
						throw new IllegalArgumentException("Le type de document sortant " + type + " n'a rien à faire là puisqu'il n'a pas de demande de quittance");
					}
					if (map.containsKey(type)) {
						throw new IllegalArgumentException("Le type de document sortant " + type + " est proposé pour plusieurs mappings...");
					}
				})
				.forEach(type -> map.put(type, traitementRetour));
	}

	private static Map<TypeDocumentSortant, TraitementRetour> buildTraitementsById(HibernateTemplate hibernateTemplate, DocumentEFactureDAO documentEFactureDAO) {

		final Map<TypeDocumentSortant, TraitementRetour> map = new EnumMap<>(TypeDocumentSortant.class);

		// demande de bilan final
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.DEMANDE_BILAN_FINAL),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, DemandeBilanFinal.class),
		                                              new AutreDocumentFiscalEnvoiKeyAssignator()));

		// autorisation de radiation au RC
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.AUTORISATION_RADIATION_RC),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, AutorisationRadiationRC.class),
		                                              new AutreDocumentFiscalEnvoiKeyAssignator()));

		// lettre type information de liquidation
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.LETTRE_TYPE_INFO_LIQUIDATION),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, LettreTypeInformationLiquidation.class),
		                                              new AutreDocumentFiscalEnvoiKeyAssignator()));

		// rappel de questionnaire SNC
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.RAPPEL_QSNC),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, EtatDeclarationRappelee.class),
		                                              new EtatDeclarationAvecDocumentArchiveKeyAssignator()));

		// accord, refus de délai, sursis, confirmation de délai
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.ACCORD_DELAI_PM,
		                              TypeDocumentSortant.REFUS_DELAI_PM,
		                              TypeDocumentSortant.SURSIS,
		                              TypeDocumentSortant.CONFIRMATION_DELAI),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, DelaiDeclaration.class),
		                                              new DelaiDeclarationKeyAssignator()));

		// sommation de DI, sommation de LR
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.SOMMATION_DI_ENTREPRISE,
		                              TypeDocumentSortant.SOMMATION_DI_PP,
		                              TypeDocumentSortant.SOMMATION_LR),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, EtatDeclarationSommee.class),
		                                              new EtatDeclarationAvecDocumentArchiveKeyAssignator()));

		// édition originale de LR, DI, QSNC
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.LR,
		                              TypeDocumentSortant.QSNC,
		                              TypeDocumentSortant.DI_APM,
		                              TypeDocumentSortant.DI_PM,
		                              TypeDocumentSortant.DI_PP_COMPLETE,
		                              TypeDocumentSortant.DI_PP_DEPENSE,
		                              TypeDocumentSortant.DI_PP_HC_IMMEUBLE,
		                              TypeDocumentSortant.DI_PP_VAUDTAX),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, EtatDeclarationEmise.class),
		                                              new EtatDeclarationAvecDocumentArchiveKeyAssignator()));

		// lettre de bienvenue
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.LETTRE_BIENENUE_APM,
		                              TypeDocumentSortant.LETTRE_BIENVENUE_PM_HC_ETABLISSEMENT,
		                              TypeDocumentSortant.LETTRE_BIENVENUE_PM_HC_IMMEUBLE,
		                              TypeDocumentSortant.LETTRE_BIENVENUE_RC_VD),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, LettreBienvenue.class),
		                                              new AutreDocumentFiscalEnvoiKeyAssignator()));

		// rappel de lettre de bienvenue
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.RAPPEL_LETTRE_BIENVENUE),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, LettreBienvenue.class),
		                                              new AutreDocumentFiscalRappelKeyAssignator()));

		// documents e-facture
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.E_FACTURE_CONTACT,
		                              TypeDocumentSortant.E_FACTURE_SIGNATURE),
		                   new TraitementRetourImpl<>(new DocumentEFactureFetcher(documentEFactureDAO),
		                                              new DocumentEFactureKeyAssignator()));

		// demande de dégrèvement
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.DEMANDE_DEGREVEMENT_ICI),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, DemandeDegrevementICI.class),
		                                              new AutreDocumentFiscalEnvoiKeyAssignator()));

		// rappel de demande de dégrèvement
		addToTraitementMap(map,
		                   EnumSet.of(TypeDocumentSortant.RAPPEL_DEMANDE_DEGREVEMENT_ICI),
		                   new TraitementRetourImpl<>(new HibernateEntityFetcherById<>(hibernateTemplate, DemandeDegrevementICI.class),
		                                              new AutreDocumentFiscalRappelKeyAssignator()));

		return map;
	}

	private static final class AutreDocumentFiscalEnvoiKeyAssignator implements KeyAssignator<AutreDocumentFiscal> {
		@Override
		public void assignKey(AutreDocumentFiscal entity, String key) {
			entity.setCleDocument(key);
		}
	}

	private static final class AutreDocumentFiscalRappelKeyAssignator implements KeyAssignator<AutreDocumentFiscalAvecSuivi> {
		@Override
		public void assignKey(AutreDocumentFiscalAvecSuivi entity, String key) {
			entity.setCleDocumentRappel(key);
		}
	}

	private static final class EtatDeclarationAvecDocumentArchiveKeyAssignator implements KeyAssignator<EtatDocumentFiscalAvecDocumentArchive> {
		@Override
		public void assignKey(EtatDocumentFiscalAvecDocumentArchive entity, String key) {
			entity.setCleDocument(key);
		}
	}

	private static final class DelaiDeclarationKeyAssignator implements KeyAssignator<DelaiDeclaration> {
		@Override
		public void assignKey(DelaiDeclaration entity, String key) {
			entity.setCleDocument(key);
		}
	}

	private static final class DocumentEFactureKeyAssignator implements KeyAssignator<DocumentEFacture> {
		@Override
		public void assignKey(DocumentEFacture entity, String key) {
			entity.setCleDocument(key);
		}
	}

}
