package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveeTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.deces.DecesTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.mariage.MariageTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.naissance.NaissanceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.obtentionpermis.ObtentionNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.obtentionpermis.ObtentionPermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.testing.TestingTranslationStrategy;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
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
	private static final class EventTypeKey {

		public final TypeEvenementCivilEch type;
		public final ActionEvenementCivilEch action;

		private EventTypeKey(EvenementCivilEch evt) {
			this(evt.getType(), evt.getAction());
		}

		private EventTypeKey(TypeEvenementCivilEch type, ActionEvenementCivilEch action) {
			if (type == null || action == null) {
				throw new NullPointerException();
			}
			this.type = type;
			this.action = action;
		}

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

	private static final Map<EventTypeKey, EvenementCivilEchTranslationStrategy> strategies = new HashMap<EventTypeKey, EvenementCivilEchTranslationStrategy>();

	/**
	 * Stratégie par défaut tant que certains traitements ne sont pas encore implémentés
	 */
	private static final EvenementCivilEchTranslationStrategy NOT_IMPLEMENTED = new EvenementCivilEchTranslationStrategy() {
		@Override
		public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
			throw new EvenementCivilException("Traitement non implémenté.");
		}
	};

	/**
	 * Stratégie utilisable pour les événements dont le seul traitement est une indexation
	 */
	private static final EvenementCivilEchTranslationStrategy INDEXATION_ONLY = new EvenementCivilEchTranslationStrategy() {
		@Override
		public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
			return new EvenementCivilInterne(event, context, options) {
				@NotNull
				@Override
				public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					final PersonnePhysique pp = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());
					if (pp != null) {
						context.getIndexer().schedule(pp.getNumero());
					}
					return HandleStatus.TRAITE;
				}

				@Override
				protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					// rien à valider
				}

				@Override
				protected boolean isContribuableObligatoirementConnuAvantTraitement() {
					return false;
				}
			};
		}
	};

	static {
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new NaissanceTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new DecesTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ABSENCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ABSENCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ABSENCE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new MariageTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.SEPARATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.SEPARATION, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.SEPARATION, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CESSATION_SEPARATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CESSATION_SEPARATION, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CESSATION_SEPARATION, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_MARIAGE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_MARIAGE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_MARIAGE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ObtentionNationaliteTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.OBENTION_DROIT_CITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.OBENTION_DROIT_CITE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.OBENTION_DROIT_CITE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.PERTE_DROIT_CITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.PERTE_DROIT_CITE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.PERTE_DROIT_CITE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECHEANCE_NATIONALITE_SUISSE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECHEANCE_NATIONALITE_SUISSE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DECHEANCE_NATIONALITE_SUISSE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ObtentionPermisTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ObtentionNationaliteTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new ArriveeTranslationStrategy());
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CONTACT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CONTACT, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CONTACT, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NOM, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NOM, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_NOM, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELIGION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELIGION, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_RELIGION, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_ABSENCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_ABSENCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ANNULATION_ABSENCE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATION_ANNONCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATION_ANNONCE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATION_ANNONCE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ADRESSE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ADRESSE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ADRESSE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_DROIT_CITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_DROIT_CITE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CHGT_DROIT_CITE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_NATIONALITE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_NATIONALITE, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_NATIONALITE, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELIGION, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELIGION, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_RELIGION, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ORIGINE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ORIGINE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ORIGINE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ETAT_CIVIL, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ETAT_CIVIL, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_ETAT_CIVIL, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_LIEU_NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_LIEU_NAISSANCE, ActionEvenementCivilEch.ANNULATION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_LIEU_NAISSANCE, ActionEvenementCivilEch.CORRECTION), INDEXATION_ONLY);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DATE_DECES, ActionEvenementCivilEch.PREMIERE_LIVRAISON), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DATE_DECES, ActionEvenementCivilEch.ANNULATION), NOT_IMPLEMENTED);
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.CORR_DATE_DECES, ActionEvenementCivilEch.CORRECTION), NOT_IMPLEMENTED);

		// pour les tests uniquement
		strategies.put(new EventTypeKey(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.PREMIERE_LIVRAISON), new TestingTranslationStrategy());
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

	private EvenementCivilContext context;

	@Override
	public EvenementCivilInterne toInterne(EvenementCivilEch event, EvenementCivilOptions options) throws EvenementCivilException {
		return getStrategy(event).create(event, context, options);
	}

	@Override
	public boolean isIndexationOnly(EvenementCivilEch event) {
		try {
			return getStrategy(event) == INDEXATION_ONLY;
		}
		catch (EvenementCivilException e) {
			return false;
		}
	}

	@NotNull
	private static EvenementCivilEchTranslationStrategy getStrategy(EvenementCivilEch event) throws EvenementCivilException {
		final EventTypeKey key = new EventTypeKey(event);
		final EvenementCivilEchTranslationStrategy strategy = strategies.get(key);
		if (strategy == null) {
			throw new EvenementCivilException("Aucune stratégie de traduction n'existe pour l'événement e-CH de type = [" + key + ']');
		}
		return strategy;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementCivilContext(serviceCivilService, serviceInfrastructureService, dataEventService, tiersService, indexer, metierService, tiersDAO, adresseService, evenementFiscalService);
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

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

	public void setContext(EvenementCivilContext context) {
		this.context = context;
	}
}
