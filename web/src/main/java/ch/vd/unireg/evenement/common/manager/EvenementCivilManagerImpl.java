package ch.vd.unireg.evenement.common.manager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoi;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.evenement.common.view.EvenementCivilDetailView;
import ch.vd.unireg.evenement.common.view.TiersAssocieView;
import ch.vd.unireg.individu.IndividuView;
import ch.vd.unireg.individu.WebCivilService;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.utils.WebContextUtils;

/**
 * Element de code factorisés entre les managers des evt ech et regPP
 */
abstract public class EvenementCivilManagerImpl implements MessageSourceAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilManagerImpl.class);

	protected AdresseService adresseService;
	protected TiersService tiersService;
	protected ServiceCivilService serviceCivilService;
	protected ServiceInfrastructureService serviceInfrastructureService;
	protected MessageSource messageSource;
	protected WebCivilService webCivilService;

	@SuppressWarnings("unused")
	public void setWebCivilService(WebCivilService webCivilService) {
		this.webCivilService = webCivilService;
	}

	@SuppressWarnings("unused")
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings("unused")
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	protected String retrieveLocaliteOuPays(Tiers tiers) throws AdresseException {

		String localiteOuPays = "";

		final AdresseGenerique adresseCourrier = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false);
		if (adresseCourrier != null) {
			final Integer noOfsPays = adresseCourrier.getNoOfsPays();
			final Pays pays = (noOfsPays == null ? null : serviceInfrastructureService.getPays(noOfsPays, adresseCourrier.getDateDebut()));
			if (pays != null && !pays.isSuisse()) {
				localiteOuPays = pays.getNomCourt();
			}
			else {
				localiteOuPays = adresseCourrier.getLocalite();
			}
		}
		return localiteOuPays;
	}

	protected TiersAssocieView createTiersAssocieView(ContribuableImpositionPersonnesPhysiques ctb) throws AdresseException, ServiceInfrastructureException {
		final TiersAssocieView tiersAssocie = new TiersAssocieView();
		tiersAssocie.setNumero(ctb.getNumero());
		final List<String> nomCourrier = adresseService.getNomCourrier(ctb, null, false);
		tiersAssocie.setNomCourrier(nomCourrier);

		tiersAssocie.setLocaliteOuPays(retrieveLocaliteOuPays(ctb));
		final ForFiscalPrincipal forFiscalPrincipal = ctb.getDernierForFiscalPrincipal();
		if (forFiscalPrincipal != null) {
			final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
			final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
			if (commune != null) {
				tiersAssocie.setForPrincipal(commune.getNomOfficiel());
			}
			tiersAssocie.setDateOuvertureFor(forFiscalPrincipal.getDateDebut());
			tiersAssocie.setDateFermetureFor(forFiscalPrincipal.getDateFin());
		}

		return tiersAssocie;
	}



	/**
	 * Construit l'exception à lancer dans le cas ou on ne trouve pas un evenement par son id
	 * @param id de l'evt non trouvé
	 * @return l'exception construite
	 */
	protected ObjectNotFoundException newObjectNotFoundException(Long id) {
		return new ObjectNotFoundException (
				messageSource.getMessage("error.id.evenement.inexistant", new Object[]{id.toString()},
						WebContextUtils.getDefaultLocale()
				));
	}

	protected AdresseEnvoi retrieveAdresse(Long numeroIndividu) throws AdresseException {
		final Individu individu = serviceCivilService.getIndividu(numeroIndividu, null);
		return adresseService.getAdresseEnvoi(individu, RegDate.get(), false);
	}

	protected IndividuView retrieveIndividu(Long numeroIndividu) {
		return webCivilService.getIndividu(numeroIndividu);
	}

	protected IndividuView retrieveIndividu(Long numeroIndividu, Long numeroEvenement) {
		return webCivilService.getIndividu(numeroIndividu, numeroEvenement);
	}

	protected void retrieveTiersAssocieMenage(Long idEvenement, Long numeroIndividu, EvenementCivilDetailView evtView) throws AdresseException {
		try {
			final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
			if (habitantPrincipal != null) {
				final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(habitantPrincipal, RegDate.get());
				if (ensembleTiersCouple != null) {
					final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
					if (menageCommun != null) {
						final TiersAssocieView tiersAssocie = createTiersAssocieView(menageCommun);
						evtView.addTiersAssocies(tiersAssocie);
					}
				}
			}
		} catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
			LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil %d : %s", idEvenement, e.getMessage()));
			evtView.addErreursTiersAssocies(e.getMessage());
		}

	}

	protected void retrieveTiersAssociePrincipal(Long idEvenement, Long numeroIndividu, EvenementCivilDetailView evtView) throws AdresseException {
		try {
			final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
			if (habitantPrincipal != null) {
				final TiersAssocieView tiersAssocie = createTiersAssocieView(habitantPrincipal);
				tiersAssocie.setLocaliteOuPays(retrieveLocaliteOuPays(habitantPrincipal));
				final ForFiscalPrincipal forFiscalPrincipal = habitantPrincipal.getDernierForFiscalPrincipal();
				if (forFiscalPrincipal != null) {
					final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
					final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
					if (commune != null) {
						tiersAssocie.setForPrincipal(commune.getNomOfficiel());
					}
					tiersAssocie.setDateOuvertureFor(forFiscalPrincipal.getDateDebut());
					tiersAssocie.setDateFermetureFor(forFiscalPrincipal.getDateFin());
				}
				evtView.addTiersAssocies(tiersAssocie);
			}
		}
		catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
			LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil %d : %s", idEvenement, e.getMessage()));
			evtView.addErreursTiersAssocies(e.getMessage());
		}

	}

	protected void retrieveTiersAssocieConjoint(Long id, Long individuConjoint,EvenementCivilDetailView evtView) throws AdresseException {
		try {
			final PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(individuConjoint);
			if (habitantConjoint != null) {
				final TiersAssocieView tiersAssocie = createTiersAssocieView(habitantConjoint);
				evtView.addTiersAssocies(tiersAssocie);
			}
		}
		catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
			LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil %d : %s", id, e.getMessage()));
			evtView.addErreursTiersAssocies(e.getMessage());
		}
	}

}
