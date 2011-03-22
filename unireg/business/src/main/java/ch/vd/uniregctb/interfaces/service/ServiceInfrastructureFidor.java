package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorBusinessException_Exception;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
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
public class ServiceInfrastructureFidor extends ServiceInfrastructureBase {

	private static final Logger LOGGER = Logger.getLogger(ServiceInfrastructureFidor.class);

	private Map<ApplicationFiscale, String> urlsApplication = null;
	private long lastTentative = 0;
	private static final long fiveMinutes = 5L * 60L * 1000000000L; // en nanosecondes

	private FidorClient fidorClient;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setFidorClient(FidorClient fidorClient) {
		this.fidorClient = fidorClient;
	}

	public List<Pays> getPays() throws InfrastructureException {
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

	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public CollectiviteAdministrative getACI() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public CollectiviteAdministrative getACISuccessions() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<Canton> getAllCantons() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getCommunes(null);
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				if (commune.getCanton().equals(canton.getSigleOFS())) {
					communes.add(CommuneImpl.get(commune));
				}
			}
			return communes;
		}
		catch (FidorBusinessException_Exception e) {
			throw new InfrastructureException(e.getMessage(), e);
		}
	}

	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getCommunes(null);
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				final List<CommuneFiscale> fractions = commune.getFractions();
				if ((fractions == null || fractions.isEmpty()) && commune.getCanton().equals(ServiceInfrastructureService.SIGLE_CANTON_VD)) {
					communes.add(CommuneImpl.get(commune));
				}
			}
			return communes;
		}
		catch (FidorBusinessException_Exception e) {
			throw new InfrastructureException(e.getMessage(), e);
		}
	}

	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getCommunes(null);
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				if (commune.getCanton().equals(ServiceInfrastructureService.SIGLE_CANTON_VD)) {
					communes.add(CommuneImpl.get(commune));
				}
			}
			return communes;
		}
		catch (FidorBusinessException_Exception e) {
			throw new InfrastructureException(e.getMessage(), e);
		}
	}

	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getCommunes(null);
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				if (!commune.getCanton().equals(ServiceInfrastructureService.SIGLE_CANTON_VD)) {
					communes.add(CommuneImpl.get(commune));
				}
			}
			return communes;
		}
		catch (FidorBusinessException_Exception e) {
			throw new InfrastructureException(e.getMessage(), e);
		}
	}

	public List<Commune> getCommunes() throws InfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getCommunes(null);
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<Commune>();
			for (CommuneFiscale commune : all) {
				communes.add(CommuneImpl.get(commune));
			}
			return communes;
		}
		catch (FidorBusinessException_Exception e) {
			throw new InfrastructureException(e.getMessage(), e);
		}
	}

	public List<Localite> getLocalites() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<Rue> getRues(Canton canton) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public Rue getRueByNumero(int numero) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public Canton getVaud() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException {
		try {
			final CommuneFiscale c = fidorClient.getCommuneParNoOFS(noCommune, XmlUtils.regdate2xmlcal(date));
			return CommuneImpl.get(c);
		}
		catch (FidorBusinessException_Exception e) {
			throw new InfrastructureException(e.getMessage(), e);
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date, int hintNoOfsCommune) throws InfrastructureException {
		final CommuneFiscale commune = fidorClient.getCommuneParBatiment(hintNoOfsCommune, egid, XmlUtils.regdate2xmlcal(date));
		if (commune == null) {
			return null;
		}

		return commune.getNoOfs();
	}

	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public Pays getPaysInconnu() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public Localite getLocaliteByNPA(int npa) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public List<TypeEtatPM> getTypesEtatsPM() throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public TypeEtatPM getTypeEtatPM(String code) throws InfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		if (urlsApplication == null) {
			initUrls();
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

					final Map<ApplicationFiscale, String> map = new HashMap<ApplicationFiscale, String>();
					map.put(ApplicationFiscale.TAO_PP, patternTaoPP);
					map.put(ApplicationFiscale.TAO_BA, patternTaoBA);
					map.put(ApplicationFiscale.TAO_IS, patternTaoIS);
					map.put(ApplicationFiscale.SIPF, patternSipf); // [UNIREG-2409]
					LOGGER.info("URLs externes (FiDoR) :\n" +
							" * TAOPP = " + patternTaoPP + "\n" +
							" * TAOBA = " + patternTaoBA + "\n" +
							" * TAOIS = " + patternTaoIS + "\n" +
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

	public Logiciel getLogiciel(Long id) {
		if (id == null) {
			return null;
		}
		return LogicielImpl.get(fidorClient.getLogicielDetail(id));
	}

	public List<Logiciel> getTousLesLogiciels() {
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
}
