package ch.vd.uniregctb.evenement.organisation.engine.translator;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneComposite;
import ch.vd.uniregctb.evenement.organisation.interne.Indexation;
import ch.vd.uniregctb.evenement.organisation.interne.IndexationPure;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.demenagement.DemenagementSiegeStrategy;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * TODO: triple check everything
 * Convertisseur d'événements reçus de RCEnt en événements organisation internes
 */
public class EvenementOrganisationTranslatorImpl implements EvenementOrganisationTranslator, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationTranslatorImpl.class);

	/**
	 * Stratégie par défaut tant que certains traitements ne sont pas encore implémentés (de manière politiquement correcte, il faut dire "implémentés en traitement manuel")
	 */
	private static final EvenementOrganisationTranslationStrategy NOT_IMPLEMENTED = new TraitementManuelOrganisationTranslationStrategy();

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
	private List<EvenementOrganisationTranslationStrategy> strategies;

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementOrganisationContext(serviceOrganisationService, serviceInfrastructureService, dataEventService, tiersService, indexer, metierServicePM, tiersDAO, adresseService,
		                                           evenementFiscalService, parametreAppService);

		// Construction des stratégies
		strategies = new ArrayList<>();

		/*
			L'ordre des stratégies est important.
		 */
		strategies.add(new CreateOrganisationStrategy());
		strategies.add(new DemenagementSiegeStrategy());
	}

	/**
	 * Traduit un événement organisation en zéro, un ou plusieurs événements externes correspondant (Dans ce cas, un événement composite est retourné.
	 * <p>
	 * AVERTISSEMENT: Contrairement au traitement des événements Civil Ech, pour lesquel un cablage explicite des stratégies est en vigueur, le traitement des événements organisation fait l'objet d'un
	 * essai de chaque stratégie disponible, chacune détectant un cas particulier de changement et donnant lieu à un événement interne. Au cas ou aucune stratégie ne fonctionne, un événement
	 * d'indexation est AUTOMATIQUEMENT ajouté. De fait, les nouveaux types d'événements organisation sont donc silencieusement ignorés jusqu'à ce qu'une stratégie soit codée pour les reconnaître.
	 *
	 * @param event   un événement organisation externe
	 * @param options les options d'exécution de l'événement
	 * @return Un événement interne correspondant à l'événement passé en paramètre
	 * @throws EvenementOrganisationException En cas d'erreur dans la création de l'événements interne, null s'il n'y a pas lieu de créer un événement
	 */
	@Override
	public EvenementOrganisationInterne toInterne(EvenementOrganisation event, EvenementOrganisationOptions options) throws EvenementOrganisationException {
		final Organisation organisation = serviceOrganisationService.getOrganisationHistory(event.getNoOrganisation());
		final Entreprise entreprise = context.getTiersDAO().getEntrepriseByNumeroOrganisation(organisation.getNumeroOrganisation());

		Audit.info(event.getId(), String.format("Organisation trouvée: %s", createOrganisationDescription(organisation, event.getDateEvenement())));

		final List<EvenementOrganisationInterne> evenements = new ArrayList<>();
		/*
			Essayer chaque stratégie. Chacune est responsable de détecter l'événement dans les données.
		 */
		for (EvenementOrganisationTranslationStrategy strategy : strategies) {
			EvenementOrganisationInterne e = strategy.matchAndCreate(event, organisation, entreprise, context, options);
			if (e != null) {
				evenements.add(e);
			}
		}

		/*
		 * Aucun événement n'est créé, indexation seulement, le status sera TRAITE.
		 */
		if (evenements.size() == 0) {
			return new IndexationPure(event, organisation, entreprise, context, options);
		}
		/* Indexation obligatoire pour toute entité connue d'Unireg. Le status sera inchangé. */
		evenements.add(new Indexation(event, organisation, entreprise, context, options));
		return new EvenementOrganisationInterneComposite(event, organisation, evenements.get(0).getEntreprise(), context, options, evenements);
	}

	/**
	 * Description générique de l'organisation pour une certaine date.
	 *
	 * Note: cette méthode serait mieux ailleurs, mais où, sachant qu'elle a besoin d'accéder au service infra?
	 * @param organisation
	 * @param date
	 * @return
	 */
	@NotNull
	private String createOrganisationDescription(Organisation organisation, RegDate date) {
		Siege siege = organisation.getSiegePrincipal(date);
		String commune = "";
		if (siege != null) {
			commune = context.getServiceInfra().getCommuneByNumeroOfs(siege.getNoOfs(), date).getNomOfficielAvecCanton();
		}
		FormeLegale formeLegale = organisation.getFormeLegale(date);
		String nom = organisation.getNom(date);
		return String.format("%s (civil: %d), %s %s, forme juridique %s.",
		                     nom != null ? nom : "[inconnu]",
		                     organisation.getNumeroOrganisation(),
		                     commune,
		                     siege != null ? "(ofs:" + siege.getNoOfs() + ")" : "[inconnue]",
		                     formeLegale != null ? formeLegale : "[inconnue]");
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
