package ch.vd.uniregctb.webservices.tiers.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.webservices.common.NoOfsTranslator;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.webservices.common.WebServiceException;
import ch.vd.uniregctb.webservices.tiers.Date;
import ch.vd.uniregctb.webservices.tiers.Debiteur;
import ch.vd.uniregctb.webservices.tiers.DebiteurHisto;
import ch.vd.uniregctb.webservices.tiers.MenageCommun;
import ch.vd.uniregctb.webservices.tiers.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers.PersonnePhysiqueHisto;
import ch.vd.uniregctb.webservices.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers.TiersHisto;
import ch.vd.uniregctb.webservices.tiers.TiersInfo;
import ch.vd.uniregctb.webservices.tiers.TiersWebService;
import ch.vd.uniregctb.webservices.tiers.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers.params.SetTiersBlocRembAuto;

public class TiersWebServiceImpl implements TiersWebService {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceImpl.class);

	private final Context context = new Context();

	private GlobalTiersSearcher tiersSearcher;

	public void setTiersService(TiersService tiersService) {
		context.tiersService = tiersService;
	}

	public void setSituationService(SituationFamilleService situationService) {
		context.situationService = situationService;
	}

	public void setAdresseService(AdresseService adresseService) {
		context.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		context.infraService = infraService;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		context.ibanValidator = ibanValidator;
	}

	public void setParametreService(ParametreAppService parametreService) {
		context.parametreService = parametreService;
	}

	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	public void setNoOfsTranslator(NoOfsTranslator translator) {
		context.noOfsTranslator = translator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public List<TiersInfo> searchTiers(SearchTiers params) throws WebServiceException {

		try {
			Set<TiersInfo> set = new HashSet<TiersInfo>();

			final List<TiersCriteria> criteria = DataHelper.webToCore(params);
			for (TiersCriteria criterion : criteria) {
				final List<TiersIndexedData> values = tiersSearcher.search(criterion);
				for (TiersIndexedData value : values) {
					final TiersInfo info = DataHelper.coreToWeb(value);
					set.add(info);
				}
			}

			return new ArrayList<TiersInfo>(set);
		}
		catch (Exception e) {
			LOGGER.error("Exception du web-service searchTiers", e);
			throw new WebServiceException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public Tiers getTiers(GetTiers params) throws WebServiceException {

		try {
			final ch.vd.registre.base.date.RegDate date = ch.vd.registre.base.date.RegDate.get(Date.asJavaDate(params.date));
			Tiers data;

			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				data = new PersonnePhysique(personne, params.parts, date, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				data = new MenageCommun(menage, params.parts, date, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
				final ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur = (ch.vd.uniregctb.tiers.DebiteurPrestationImposable) tiers;
				data = new Debiteur(debiteur, params.parts, date, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error("Exception du web-service getTiers", e);
			throw new WebServiceException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			final TiersHisto data;
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				data = new PersonnePhysiqueHisto(personne, params.periode, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				data = new MenageCommunHisto(menage, params.periode, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
				final ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur = (ch.vd.uniregctb.tiers.DebiteurPrestationImposable) tiers;
				data = new DebiteurHisto(debiteur, params.periode, params.parts, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error("Exception du web-service getTiersPeriode", e);
			throw new WebServiceException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public TiersHisto getTiersHisto(GetTiersHisto params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			final TiersHisto data;
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				data = new PersonnePhysiqueHisto(personne, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				data = new MenageCommunHisto(menage, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
				final ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur = (ch.vd.uniregctb.tiers.DebiteurPrestationImposable) tiers;
				data = new DebiteurHisto(debiteur, params.parts, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error("Exception du web-service getTiersHisto", e);
			throw new WebServiceException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public Tiers.Type getTiersType(GetTiersType params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			final Type type = DataHelper.getType(tiers);
			if (type == null) {
				Assert.fail("Type de tiers inconnu = [" + tiers.getClass().getSimpleName());
			}

			return type;
		}
		catch (Exception e) {
			LOGGER.error("Exception du web-service getTiersType", e);
			throw new WebServiceException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void setTiersBlocRembAuto(final SetTiersBlocRembAuto params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				throw new ObjectNotFoundException("Le tiers n°" + params.tiersNumber + " n'existe pas.");
			}

			tiers.setBlocageRemboursementAutomatique(params.blocage);
		}
		catch (Exception e) {
			LOGGER.error("Exception du web-service setTiersBlocRemAuto", e);
			throw new WebServiceException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void doNothing(AllConcreteTiersClasses dummy) {
	}
}
