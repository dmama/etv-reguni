package ch.vd.unireg.listes.suisseoupermiscresident;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.common.ListesThread;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersService;

public class ListeContribuablesResidentsSansForVaudoisThread extends ListesThread<ListeContribuablesResidentsSansForVaudoisResults> {

	private final RegDate dateTraitement;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final ServiceInfrastructureService serviceInfrastructure;
	private static final Set<TiersDAO.Parts> PARTS_FISCALES;

	static {
		final Set<TiersDAO.Parts> parts = EnumSet.noneOf(TiersDAO.Parts.class);
		parts.addAll(ListesThread.PARTS_FISCALES);
		parts.add(TiersDAO.Parts.ADRESSES);

		PARTS_FISCALES = Collections.unmodifiableSet(parts);
	}

	public ListeContribuablesResidentsSansForVaudoisThread(BlockingQueue<List<Long>> queue, RegDate dateTraitement, int nombreThreads, Interruptible interruptible, AtomicInteger compteur, PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                                                       TiersDAO tiersDAO, TiersService tiersService, AdresseService adresseService, ServiceInfrastructureService serviceInfrastructure, ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		super(queue, interruptible, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate,
		      new ListeContribuablesResidentsSansForVaudoisResults(dateTraitement, nombreThreads, tiersService, adresseService));
		this.dateTraitement = dateTraitement;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.serviceInfrastructure = serviceInfrastructure;
	}

	@Override
	protected Set<TiersDAO.Parts> getFiscalPartsToFetch() {
		return PARTS_FISCALES;
	}

	@Override
	protected void fillAttributesIndividu(Set<AttributeIndividu> attributes) {
		super.fillAttributesIndividu(attributes);
		attributes.add(AttributeIndividu.ADRESSES);
		attributes.add(AttributeIndividu.NATIONALITES);
		attributes.add(AttributeIndividu.PERMIS);
	}

	private boolean isDecede(PersonnePhysique pp, RegDate dateReference) {
		return RegDateHelper.isAfterOrEqual(dateReference, tiersService.getDateDeces(pp), NullDateBehavior.LATEST);
	}

	@Override
	protected void traiteContribuable(Contribuable ctb) throws Exception {

		final boolean suisseOuPermisC;

		if (ctb instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctb;
			if (isDecede(pp, dateTraitement)) {
				getResults().addContribuableIgnore(pp, ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.DECEDE);
				suisseOuPermisC = false;
			}
			else {
				suisseOuPermisC = isSuisseOuPermisC(pp, true, true);
			}
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, dateTraitement);

			if ((ensemble.getPrincipal() == null && ensemble.getConjoint() == null) ||
				(ensemble.getPrincipal() != null && isDecede(ensemble.getPrincipal(), dateTraitement)) ||
				(ensemble.getConjoint() != null && isDecede(ensemble.getConjoint(), dateTraitement))) {

				// les couples fermés (séparation, décès...) sont ignorés
				getResults().addContribuableIgnore(mc, ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.MENAGE_FERME);
				suisseOuPermisC = false;
			}
			else {
				// il suffit que l'un des membres soit suisse ou titulaire du permis C
				suisseOuPermisC = (ensemble.getPrincipal() != null && isSuisseOuPermisC(ensemble.getPrincipal(), false, false)) ||
								  (ensemble.getConjoint() != null && isSuisseOuPermisC(ensemble.getConjoint(), false, false));

				if (!suisseOuPermisC) {
					getResults().addContribuableIgnore(mc, ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.ETRANGER_SANS_PERMIS_C);
				}
			}
		}
		else {
			throw new RuntimeException("Contribuable de classe non attendue : " + ctb.getClass().getName());
		}

		if (suisseOuPermisC) {
			final AdressesCiviles adresses = adresseService.getAdressesCiviles(ctb, dateTraitement, false);
			if (adresses != null && adresses.principale != null && serviceInfrastructure.estDansLeCanton(adresses.principale)) {

				// dans ce cas-là seulement, on met le contribuable dans le rapport :
				// 1. suisse ou permis C majeur (ou couple)
				// 2. sans for vaudois (vu dans la requête initiale)
				// 3. avec adresse de domicile dans le canton

				super.traiteContribuable(ctb);
			}
			else {
				getResults().addContribuableIgnore(ctb, ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.DOMICILE_NON_VAUDOIS);
			}
		}
	}

	private boolean isSuisseOuPermisC(PersonnePhysique pp, boolean ignoreMineurs, boolean logIgnores) throws TiersException {

		boolean suisseOuPermisC = false;
		boolean okAvecMajorite = !ignoreMineurs;

		final boolean isSuisse = tiersService.isSuisse(pp, dateTraitement);
		if (!isSuisse) {
			final boolean isSansPermisC = tiersService.isEtrangerSansPermisC(pp, dateTraitement);
			if (!isSansPermisC) {
				suisseOuPermisC = true;
			}
		}
		else {
			suisseOuPermisC = true;
		}

		if (!suisseOuPermisC) {
			if (logIgnores) {
				getResults().addContribuableIgnore(pp, ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.ETRANGER_SANS_PERMIS_C);
			}
		}
		else if (!okAvecMajorite) {
			if (pp.isHabitantVD() && pp.getDateNaissance() == null) {
				final Individu individu = tiersService.getIndividu(pp);
				okAvecMajorite = FiscalDateHelper.isMajeurAt(individu, dateTraitement);
			}
			else {
				final RegDate dateNaissance = pp.getDateNaissance();
				okAvecMajorite = dateNaissance == null || FiscalDateHelper.isMajeur(dateTraitement, dateNaissance);
			}
			if (!okAvecMajorite && logIgnores) {
				getResults().addContribuableIgnore(pp, ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.MINEUR);
			}
		}

		return suisseOuPermisC && okAvecMajorite;
	}
}
