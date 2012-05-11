package ch.vd.uniregctb.evenement.common.manager;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.evenement.common.view.EvenementCivilDetailView;
import ch.vd.uniregctb.evenement.common.view.TiersAssocieView;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.individu.WebCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Element de code factorisés entre les managers des evt ech et regPP
 */
abstract public class EvenementCivilManagerImpl implements MessageSourceAware {

	protected AdresseService adresseService;
	protected TiersService tiersService;
	protected TiersDAO tiersDAO;
	protected ServiceCivilService serviceCivilService;
	protected ServiceInfrastructureService serviceInfrastructureService;
	protected MessageSource messageSource;
	protected WebCivilService webCivilService;
	private final Logger LOGGER = Logger.getLogger(EvenementCivilManagerImpl.class);

	@SuppressWarnings("unused")
	public void setWebCivilService(WebCivilService webCivilService) {
		this.webCivilService = webCivilService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
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

	protected String retieveLocaliteOuPays(Tiers tiers) throws AdresseException {

		String localiteOuPays = "";

		AdresseGenerique adresseCourrier = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false);
		if (adresseCourrier != null) {
			Integer noOfsPays = adresseCourrier.getNoOfsPays();
			Pays pays;
			pays = (noOfsPays == null ? null : serviceInfrastructureService.getPays(noOfsPays));
			if (pays != null && !pays.isSuisse()) {
				localiteOuPays = pays.getNomMinuscule();
			}
			else {
				localiteOuPays = adresseCourrier.getLocalite();
			}
		}
		return localiteOuPays;
	}

	protected TiersAssocieView createTiersAssocieView(Tiers tiers) throws AdresseException, ServiceInfrastructureException {
		final TiersAssocieView tiersAssocie = new TiersAssocieView();
		tiersAssocie.setNumero(tiers.getNumero());
		final List<String> nomCourrier = adresseService.getNomCourrier(tiers, null, false);
		tiersAssocie.setNomCourrier(nomCourrier);

		tiersAssocie.setLocaliteOuPays(retieveLocaliteOuPays(tiers));
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getDernierForFiscalPrincipal();
		if (forFiscalPrincipal != null) {
			final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
			final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
			if (commune != null) {
				tiersAssocie.setForPrincipal(commune.getNomMinuscule());
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

	protected void retrieveTiersAssocieMenage(Long idEvenement, Long numeroIndividu, EvenementCivilDetailView evtView) throws AdresseException {
		try {
			final PersonnePhysique habitantPrincipal = tiersDAO.getPPByNumeroIndividu(numeroIndividu);
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
			final PersonnePhysique habitantPrincipal = tiersDAO.getPPByNumeroIndividu(numeroIndividu);
			if (habitantPrincipal != null) {
				final TiersAssocieView tiersAssocie = createTiersAssocieView(habitantPrincipal);
				tiersAssocie.setLocaliteOuPays(retieveLocaliteOuPays(habitantPrincipal));
				final ForFiscalPrincipal forFiscalPrincipal = habitantPrincipal.getDernierForFiscalPrincipal();
				if (forFiscalPrincipal != null) {
					final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
					final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
					if (commune != null) {
						tiersAssocie.setForPrincipal(commune.getNomMinuscule());
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
			final PersonnePhysique habitantConjoint = tiersDAO.getPPByNumeroIndividu(individuConjoint);
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
