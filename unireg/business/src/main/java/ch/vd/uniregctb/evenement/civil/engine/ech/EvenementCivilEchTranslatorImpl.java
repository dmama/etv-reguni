package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchSourceHelper;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.annulation.arrivee.AnnulationArriveeTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.deces.AnnulationDecesTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.divorce.AnnulationDivorceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.mariage.AnnulationMariageTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.reconciliation.AnnulationReconciliationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.separation.AnnulationSeparationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.veuvage.AnnulationVeuvageTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulationpermis.AnnulationPermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulationpermis.SuppressionNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveeTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification.CorrectionAdresseTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification.ModificationAdresseNotificationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.identificateur.DonneesUpiTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.nom.ChangementNomTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.origine.CorrectionOrigineTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.correction.identification.CorrectionAutresNomsTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.correction.identification.CorrectionIdentificationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.correction.relation.CorrectionRelationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.deces.DecesTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.demenagement.DemenagementTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.depart.DepartEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.divorce.DissolutionPartenariatTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.divorce.DivorceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.mariage.MariageTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.naissance.NaissanceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.obtentionpermis.ObtentionNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.obtentionpermis.ObtentionPermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.reconciliation.ReconciliationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.testing.TestingTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.veuvage.VeuvageTranslationStrategy;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Convertisseur d'événements e-CH reçus de RCPers en événements civils internes
 */
public class EvenementCivilEchTranslatorImpl implements EvenementCivilEchTranslator, InitializingBean {

	/**
	 * Clé de détermination de la stratégie de conversion à appliquer
	 */
	protected static final class EventTypeKey {

		public final TypeEvenementCivilEch type;
		public final ActionEvenementCivilEch action;

		protected EventTypeKey(EvenementCivilEchFacade evt) {
			this(evt.getType(), evt.getAction());
		}

		protected EventTypeKey(TypeEvenementCivilEch type, ActionEvenementCivilEch action) {
			if (type == null || action == null) {
				throw new NullPointerException();
			}
			this.type = type;
			this.action = action;
		}

		@SuppressWarnings("RedundantIfStatement")
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final EventTypeKey key = (EventTypeKey) o;

			if (action != key.action) return false;
			if (type != key.type) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + action.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return String.format("type=%s, action=%s", type, action);
		}
	}

	private EvenementCivilEchStrategyParameters parameters;
	private Map<EventTypeKey, EvenementCivilEchTranslationStrategy> strategies;

	/**
	 * Stratégie par défaut tant que certains traitements ne sont pas encore implémentés (de manière politiquement correcte, il faut dire "implémentés en traitement manuel")
	 */
	private static final EvenementCivilEchTranslationStrategy NOT_IMPLEMENTED = new TraitementManuelTranslationStrategy();

	/**
	 * Stratégie utilisable pour les événements dont le seul traitement est une indexation
	 */
	private static final EvenementCivilEchTranslationStrategy INDEXATION_ONLY = new IndexationPureTranslationStrategy();

	private static Map<EventTypeKey, EvenementCivilEchTranslationStrategy> buildStrategies(EvenementCivilContext context, EvenementCivilEchStrategyParameters params) {

		final EvenementCivilEchTranslationStrategy defaultCorrectionStrategy = new DefaultCorrectionTranslationStrategy(context.getServiceCivil(), context.getServiceInfra(), context.getTiersService());
		final EvenementCivilEchTranslationStrategy cacheCleaningCorrectionStrategy = embedInRelationshipCacheCleanupStrategy(defaultCorrectionStrategy, context);
		final EvenementCivilEchTranslationStrategy correctionRelationTranslationStrategy = new CorrectionRelationTranslationStrategy(context.getServiceCivil(), context.getDataEventService(), context.getTiersService());

		final Map<EventTypeKey, EvenementCivilEchTranslationStrategy> strategies = new HashMap<>();
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new NaissanceTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DecesTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.ANNULATION), new AnnulationDecesTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ABSENCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DecesTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ABSENCE, ActionEvenementCivilEch.ANNULATION), new AnnulationDecesTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ABSENCE, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new MariageTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.ANNULATION), new AnnulationMariageTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.SEPARATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new SeparationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.SEPARATION, ActionEvenementCivilEch.ANNULATION), new AnnulationSeparationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.SEPARATION, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CESSATION_SEPARATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ReconciliationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CESSATION_SEPARATION, ActionEvenementCivilEch.ANNULATION), new AnnulationReconciliationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CESSATION_SEPARATION, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DivorceTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION), new AnnulationDivorceTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new VeuvageTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE, ActionEvenementCivilEch.ANNULATION), new AnnulationVeuvageTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_MARIAGE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_MARIAGE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_MARIAGE, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ObtentionNationaliteTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.ANNULATION), new SuppressionNationaliteTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.OBENTION_DROIT_CITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.OBENTION_DROIT_CITE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.OBENTION_DROIT_CITE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.PERTE_DROIT_CITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.PERTE_DROIT_CITE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.PERTE_DROIT_CITE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECHEANCE_NATIONALITE_SUISSE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECHEANCE_NATIONALITE_SUISSE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECHEANCE_NATIONALITE_SUISSE, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ObtentionPermisTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER, ActionEvenementCivilEch.ANNULATION), new AnnulationPermisTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ObtentionNationaliteTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE, ActionEvenementCivilEch.ANNULATION), new SuppressionNationaliteTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ArriveeTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION), new AnnulationArriveeTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DepartEchTranslationStrategy(params.getDecalageMaxPourDepart()));
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DemenagementTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CONTACT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ModificationAdresseNotificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CONTACT, ActionEvenementCivilEch.ANNULATION), new ModificationAdresseNotificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CONTACT, ActionEvenementCivilEch.CORRECTION), new ModificationAdresseNotificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NOM, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ChangementNomTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NOM, ActionEvenementCivilEch.ANNULATION), new ChangementNomTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NOM, ActionEvenementCivilEch.CORRECTION), new ChangementNomTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELIGION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELIGION, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELIGION, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_ABSENCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_ABSENCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_ABSENCE, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new MariageTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT, ActionEvenementCivilEch.ANNULATION), new AnnulationMariageTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DissolutionPartenariatTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT, ActionEvenementCivilEch.ANNULATION), new AnnulationDivorceTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATION_ANNONCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new CorrectionAdresseTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATION_ANNONCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATION_ANNONCE, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.PREMIERE_LIVRAISON), correctionRelationTranslationStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.ANNULATION), correctionRelationTranslationStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.CORRECTION), correctionRelationTranslationStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_DROIT_CITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_DROIT_CITE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_DROIT_CITE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new CorrectionIdentificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.ANNULATION), new CorrectionIdentificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.CORRECTION), new CorrectionIdentificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new CorrectionAutresNomsTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.ANNULATION), new CorrectionAutresNomsTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.CORRECTION), new CorrectionAutresNomsTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_NATIONALITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_NATIONALITE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_NATIONALITE, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ModificationAdresseNotificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.ANNULATION), new ModificationAdresseNotificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.CORRECTION), new ModificationAdresseNotificationTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELIGION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELIGION, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELIGION, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ORIGINE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new CorrectionOrigineTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ORIGINE, ActionEvenementCivilEch.ANNULATION), new CorrectionOrigineTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ORIGINE, ActionEvenementCivilEch.CORRECTION), new CorrectionOrigineTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ObtentionPermisTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER, ActionEvenementCivilEch.ANNULATION), new AnnulationPermisTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ETAT_CIVIL, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ETAT_CIVIL, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ETAT_CIVIL, ActionEvenementCivilEch.CORRECTION), cacheCleaningCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_LIEU_NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_LIEU_NAISSANCE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_LIEU_NAISSANCE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DATE_DECES, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DATE_DECES, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DATE_DECES, ActionEvenementCivilEch.CORRECTION), defaultCorrectionStrategy);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ATTRIBUTION_DONNEES_UPI, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ATTRIBUTION_DONNEES_UPI, ActionEvenementCivilEch.ANNULATION), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ATTRIBUTION_DONNEES_UPI, ActionEvenementCivilEch.CORRECTION), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DONNEES_UPI, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DONNEES_UPI, ActionEvenementCivilEch.ANNULATION), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DONNEES_UPI, ActionEvenementCivilEch.CORRECTION), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_DONNEES_UPI, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_DONNEES_UPI, ActionEvenementCivilEch.ANNULATION), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_DONNEES_UPI, ActionEvenementCivilEch.CORRECTION), new DonneesUpiTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.INACTIVATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.INACTIVATION, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.INACTIVATION, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.REACTIVATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.REACTIVATION, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.REACTIVATION, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);

		// pour les tests uniquement
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new TestingTranslationStrategy());

		return strategies;
	}

	private static EvenementCivilEchTranslationStrategy embedInRelationshipCacheCleanupStrategy(EvenementCivilEchTranslationStrategy strategy, EvenementCivilContext context) {
		return new TranslationStrategyWithRelationshipCacheCleanupFacade(strategy, context.getServiceCivil(), context.getDataEventService(), context.getTiersService());
	}

	private ServiceCivilService serviceCivilService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private TiersDAO tiersDAO;
	private DataEventService dataEventService;
	private TiersService tiersService;
	private MetierService metierService;
	private AdresseService adresseService;
	private GlobalTiersIndexer indexer;
	private EvenementFiscalService evenementFiscalService;
	private ParametreAppService parametreAppService;

	private EvenementCivilContext context;
	
	@Override
	public EvenementCivilInterne toInterne(EvenementCivilEchFacade event, EvenementCivilOptions options) throws EvenementCivilException {
		return getStrategy(event).create(event, context, options);
	}

	@Override
	public boolean isIndexationOnly(EvenementCivilEch event) {
		try {
			return getStrategy(event).isPrincipalementIndexation(event, context);
		}
		catch (EvenementCivilException e) {
			return false;
		}
	}

	@NotNull
	private EvenementCivilEchTranslationStrategy getStrategy(EvenementCivilEchFacade event) throws EvenementCivilException {

		// TODO [ech99] jde : à enlever dès que possible...
		if (EvenementCivilEchSourceHelper.isFromEch99(event)) {
			return ech99Strategy;
		}

		final EventTypeKey key = new EventTypeKey(event);
		final EvenementCivilEchTranslationStrategy strategy = getStrategy(key);
		if (strategy == null) {
			throw new EvenementCivilException("Aucune stratégie de traduction n'existe pour l'événement e-CH [" + key + ']');
		}
		return strategy;
	}

	@Nullable
	protected EvenementCivilEchTranslationStrategy getStrategy(EventTypeKey key) {
		return getStrategyFromMap(key);
	}

	@Nullable
	protected final EvenementCivilEchTranslationStrategy getStrategyFromMap(EventTypeKey key) {
		return strategies.get(key);
	}

	private EvenementCivilEchTranslationStrategy ech99Strategy;

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementCivilContext(serviceCivilService, serviceInfrastructureService, dataEventService, tiersService, indexer, metierService, tiersDAO, adresseService, evenementFiscalService, parametreAppService);
		strategies = buildStrategies(context, parameters);

		// TODO [ech99] jde : à enlever dès que possible
		ech99Strategy = new EvenementCivilEchIssuDe99Strategy(tiersService);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParameters(EvenementCivilEchStrategyParameters parameters) {
		this.parameters = parameters;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}
}
