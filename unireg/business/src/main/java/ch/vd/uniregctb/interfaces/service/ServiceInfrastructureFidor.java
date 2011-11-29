package ch.vd.uniregctb.interfaces.service;

import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorBusinessException_Exception;
import ch.vd.fidor.ws.v2.FidorDate;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;
import ch.vd.uniregctb.interfaces.model.impl.CommuneImpl;
import ch.vd.uniregctb.interfaces.model.impl.LogicielImpl;
import ch.vd.uniregctb.interfaces.model.impl.PaysImpl;
import ch.vd.uniregctb.webservice.fidor.FidorClient;

/**
 * Implémentation Fidor du service d'infrastructure [UNIREG-2187].
 */
public class ServiceInfrastructureFidor implements ServiceInfrastructureRaw {

	private static final Logger LOGGER = Logger.getLogger(ServiceInfrastructureFidor.class);

	private Map<ApplicationFiscale, String> urlsApplication = null;
	private long lastTentative = 0;
	private static final long fiveMinutes = 5L * 60L * 1000000000L; // en nanosecondes

	private FidorClient fidorClient;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setFidorClient(FidorClient fidorClient) {
		this.fidorClient = fidorClient;
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		try {
			final Collection<ch.vd.fidor.ws.v2.Pays> list = fidorClient.getTousLesPays();
			if (list == null || list.isEmpty()) {
				return Collections.emptyList();
			}
			else {
				final List<Pays> pays = new ArrayList<Pays>();
				for (ch.vd.fidor.ws.v2.Pays o : list) {
					pays.add(PaysImpl.get(o));
				}
				return Collections.unmodifiableList(pays);
			}
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
		try {
			ch.vd.fidor.ws.v2.Pays p = fidorClient.getPaysDetail(numeroOFS);
			return PaysImpl.get(p);
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getToutesLesCommunes();
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				if (commune.getSigleCanton().equals(canton.getSigleOFS())) {
					communes.add(CommuneImpl.get(commune));
				}
			}
			return communes;
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
		catch (FidorBusinessException_Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getToutesLesCommunes();
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				final List<CommuneFiscale> fractions = commune.getFractions();
				if ((fractions == null || fractions.isEmpty()) && ServiceInfrastructureService.SIGLE_CANTON_VD.equals(commune.getSigleCanton())) {
					communes.add(CommuneImpl.get(commune));
				}
			}
			return communes;
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
		catch (FidorBusinessException_Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getToutesLesCommunes();
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				communes.add(CommuneImpl.get(commune));
			}
			return communes;
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
		catch (FidorBusinessException_Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		try {
			final List<Commune> list = new ArrayList<Commune>();
			final List<CommuneFiscale> l = fidorClient.getCommunesParNoOFS(noOfsCommune);
			for (CommuneFiscale c : l) {
				list.add(CommuneImpl.get(c));
			}
			return list;
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
		catch (FidorBusinessException_Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {

		try {
			final CommuneFiscale commune = fidorClient.getCommuneParBatiment(egid, reg2fidor(date));
			if (commune == null) {
				return null;
			}

			return commune.getNoOfs();
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	private static FidorDate reg2fidor(RegDate date) {
		if (date == null) {
			return null;
		}
		FidorDate d = new FidorDate();
		d.setYear(date.year());
		d.setMonth(date.month());
		d.setDay(date.day());
		return d;
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		if (urlsApplication == null) {
			initUrls();
		}
		if (urlsApplication == null) {
			return null;
		}
		final String url = urlsApplication.get(application);
		return resolve(url, tiersId, AuthenticationHelper.getCurrentOID());
	}

	private static String resolve(String url, Long numero, Integer oid) {
		if (url == null) {
			return null;
		}
		Assert.notNull(numero);
		Assert.notNull(oid);
		return url.replaceAll("\\{NOCTB\\}", numero.toString()).replaceAll("\\{OID\\}", oid.toString());
	}

	/**
	 * Initialise les URLs des applications fiscales.
	 * <p/>
	 * <b>Note:</b> il est absolument nécessaire d'initialiser le client <i>après</i> le contexte Spring, car il y a une dépendence implicite sur le bus CXF qui risque d'être initialisé plus tard que ce
	 * bean. Dans ce dernier, cas on reçoit une NPE dans le constructeur du service.
	 */
	private void initUrls() {
		final long now = System.nanoTime();
		if (lastTentative > 0 && lastTentative + fiveMinutes > now) {
			// on attend cinq minutes avant d'essayer de recontacter FiDoR, pour éviter de remplir les logs pour rien
			return;
		}
		synchronized (this) {
			try {
				if (urlsApplication == null) {
					final String patternTaoPP = getUrl("TAOPP", "synthese");
					final String patternTaoBA = getUrl("TAOBA", "dossier");
					final String patternTaoIS = getUrl("TAOIS", "default");
					final String patternSipf = getUrl("SIPF", "explorer");

					final Map<ApplicationFiscale, String> map = new EnumMap<ApplicationFiscale, String>(ApplicationFiscale.class);
					map.put(ApplicationFiscale.TAO_PP, patternTaoPP);
					map.put(ApplicationFiscale.TAO_BA, patternTaoBA);
					map.put(ApplicationFiscale.TAO_IS, patternTaoIS);
					map.put(ApplicationFiscale.SIPF, patternSipf); // [UNIREG-2409]
					LOGGER.info("URLs externes (FiDoR) :\n" +
							" * TAOPP = " + patternTaoPP + '\n' +
							" * TAOBA = " + patternTaoBA + '\n' +
							" * TAOIS = " + patternTaoIS + '\n' +
							" * SIPF = " + patternSipf);

					urlsApplication = map;
				}
			}
			catch (Exception e) {
				LOGGER.error("Impossible de contacter FiDoR : allez lui donner un coup de pied !");
				lastTentative = now;
			}
		}
	}

	private String getUrl(String app, String target) {
		final String url = fidorClient.getUrl(app, Acces.INTERNE, target, null);
		if (url == null) {
			LOGGER.error(String.format("Il manque l'url d'accès à %s (target %s) dans FiDoR !", app, target));
		}
		return url;
	}

	@Override
	public Logiciel getLogiciel(Long id) throws ServiceInfrastructureException {
		if (id == null) {
			return null;
		}
		try {
			return LogicielImpl.get(fidorClient.getLogicielDetail(id));
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		try {
			final Collection<ch.vd.fidor.ws.v2.Logiciel> list = fidorClient.getTousLesLogiciels();
			if (list == null || list.isEmpty()) {
				return Collections.emptyList();
			}
			else {
				final List<Logiciel> logiciels = new ArrayList<Logiciel>();
				for (ch.vd.fidor.ws.v2.Logiciel logicielFidor : list) {
					logiciels.add(LogicielImpl.get(logicielFidor));
				}
				return Collections.unmodifiableList(logiciels);
			}
		}
		catch (WebServiceException e) {
			throw new ServiceInfrastructureException(e.getMessage(), e);
		}
	}
}
