package ch.vd.uniregctb.evenement.organisation.engine.translator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneComposite;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Convertisseur d'événements reçus de RCEnt en événements organisation internes
 */
public class EvenementOrganisationTranslatorImpl implements EvenementOrganisationTranslator, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationTranslatorImpl.class);

	/**
	 * Stratégie par défaut tant que certains traitements ne sont pas encore implémentés (de manière politiquement correcte, il faut dire "implémentés en traitement manuel")
	 */
	private static final EvenementOrganisationTranslationStrategy NOT_IMPLEMENTED = new TraitementManuelOrganisationTranslationStrategy();

	/**
	 * Stratégie utilisable pour les événements dont le seul traitement est une indexation
	 */
	private static final EvenementOrganisationTranslationStrategy INDEXATION_ONLY = new IndexationPureOrganisationTranslationStrategy();

	private ServiceOrganisationService serviceOrganisationService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private TiersDAO tiersDAO;
	private DataEventService dataEventService;
	private TiersService tiersService;
	private MetierServicePM metierServicePM;
	private AdresseService adresseService;
	private GlobalTiersIndexer indexer;
	private EvenementFiscalService evenementFiscalService;
	private ParametreAppService parametreAppService;

	/*
	 * Non injecté mais créé ci-dessous dans afterPropertiesSet()
	 */
	private EvenementOrganisationContext context;
	List<EvenementOrganisationTranslationStrategy> strategies;

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementOrganisationContext(serviceOrganisationService, serviceInfrastructureService, dataEventService, tiersService, indexer, metierServicePM, tiersDAO, adresseService, evenementFiscalService, parametreAppService);

		// Construction des stratégies
		strategies = new ArrayList<>();
		// TODO: elle viennent ces stratégies?
	}

	/**
	 * Traduit un événement organisation en zéro, un ou plusieurs événements externes correspondant (Dans ce cas, un
	 * événement composite est retourné.
	 *
	 * AVERTISSEMENT: Contrairement au traitement des événements Civil Ech, pour lesquel un cablage explicite des stratégies est en vigueur, le traitement
	 * des événements organisation fait l'objet d'un essai de chaque stratégie disponible, chacune détectant un cas particulier de changement et
	 * donnant lieu à un événement interne. Au cas ou aucune stratégie ne fonctionne, un événement d'indexation est AUTOMATIQUEMENT ajouté. De fait,
	 * les nouveaux types d'événements organisation sont donc silencieusement ignorés jusqu'à ce qu'une stratégie soit codée pour les reconnaître.
	 *
 	 * @param event   un événement organisation externe
	 * @param options les options d'exécution de l'événement
	 * @return Un événement interne correspondant à l'événement passé en paramètre
	 * @throws EvenementOrganisationException En cas d'erreur dans la création de l'événements interne, null s'il n'y a pas lieu de créer un événement
	 */
	@Override
	public EvenementOrganisationInterne toInterne(EvenementOrganisation event, EvenementOrganisationOptions options) throws EvenementOrganisationException {
		Organisation organisation = serviceOrganisationService.getOrganisationHistory(event.getNoOrganisation());

		List<EvenementOrganisationInterne> evenements = createEvents(event, organisation, strategies, context, options);
		if (evenements.size() == 0) {
			return INDEXATION_ONLY.create(event, organisation, context, options);
		} else if (evenements.size() == 1) {
			return evenements.get(0);
		}
		return new EvenementOrganisationInterneComposite(event, organisation, context, options, evenements);
	}

	private List<EvenementOrganisationInterne> createEvents(EvenementOrganisation event, Organisation organisation, List<EvenementOrganisationTranslationStrategy> strategies, EvenementOrganisationContext context,
	                                                       EvenementOrganisationOptions options) throws EvenementOrganisationException {
		List<EvenementOrganisationInterne> evenements = new ArrayList<>();
		for (EvenementOrganisationTranslationStrategy strategy : strategies) {
			evenements.add(strategy.match(event, organisation, context, options));
		}
		return evenements;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
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
	public void setMetierServicePM(MetierServicePM metierServicePM) {
		this.metierServicePM = metierServicePM;
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
	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}
}
