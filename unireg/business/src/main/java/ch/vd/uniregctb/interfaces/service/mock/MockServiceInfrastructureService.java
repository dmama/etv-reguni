package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.AbstractServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

/**
 * Mock du Host Infrastructure Service.
 * <p>
 * Pour utiliser cette classe, le plus simple est d'instancier une classe anonyme et d'implémenter la méthode init() de manière à charger
 * les données de test voulues.
 * <p>
 * Par exemple:
 *
 * <pre>
 *  ServiceInfrastructureService service = new MockServiceInfrastructureService() {
 *  protected void init() {
 *  // Pays
 *  pays.add(MockPays.Suisse);
 *  pays.add(MockPays.France);
 *  // Cantons
 *  cantons.add(MockCanton.Vaud);
 *  cantons.add(MockCanton.Geneve);
 *  ...
 *  }
 *  };
 * </pre>
 */
public abstract class MockServiceInfrastructureService extends AbstractServiceInfrastructureService {

	// private static final Logger LOGGER = Logger.getLogger(MockServiceInfrastructureService.class);

	protected List<Pays> pays = new ArrayList<Pays>();

	protected List<Canton> cantons = new ArrayList<Canton>();

	protected List<Localite> localites = new ArrayList<Localite>();

	protected List<Commune> communesVaud = new ArrayList<Commune>();

	protected List<Commune> communesHorsCanton = new ArrayList<Commune>();

	protected List<Commune> communes = new ArrayList<Commune>();

	protected List<CollectiviteAdministrative> collectivitesAdministrative = new ArrayList<CollectiviteAdministrative>();

	protected List<Rue> rues = new ArrayList<Rue>();

	public MockServiceInfrastructureService() {
		init();
	}

	protected abstract void init();

	public List<Canton> getAllCantons() throws InfrastructureException {
		return cantons;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCommunesDeVaud()
	 */
	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		return communesVaud;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCommunesHorsCanton()
	 */
	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		return communesHorsCanton;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCommunesHorsCanton()
	 */
	public List<Commune> getCommunes() throws InfrastructureException {
		return communes;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getListeCommunes(ch.vd.infrastructure.model.Canton)
	 */
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		return communesVaud;
	}

	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		return communesVaud;
	}

	/**
	 * @param cantonAsString
	 * @return
	 * @throws InfrastructureException
	 */
	public List<Commune> getListeCommunes(String cantonAsString) throws InfrastructureException {
		Canton canton = getCantonBySigle(cantonAsString);
		if (canton.equals("VD"))
			return communesVaud;
		else
			return communesHorsCanton;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getLocalites()
	 */
	public List<Localite> getLocalites() throws InfrastructureException {
		return localites;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getLocaliteByONRP(int)
	 */
	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException {
		Localite localite = null;
		for (Localite loc : getLocalites()) {
			if (loc.getNoOrdre().intValue() == onrp) {
				localite = loc;
				break;
			}
		}
		return localite;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getPays()
	 */
	public List<Pays> getPays() throws InfrastructureException {
		return pays;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getAllRues()
	 */
	public List<Rue> getAllRues() throws InfrastructureException {
		return rues;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getRues(ch.vd.infrastructure.model.Canton)
	 */
	public List<Rue> getRues(Canton canton) throws InfrastructureException {
		Assert.fail("Not implemented");
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getRues(ch.vd.infrastructure.model.Localite)
	 */
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		List<Rue> locRues = new ArrayList<Rue>();
		for (Rue r : rues) {
			if (r.getNoLocalite().equals(localite.getNoOrdre())) {
				locRues.add(r);
			}
		}
		return locRues;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public Rue getRueByNumero(int numero) throws InfrastructureException {
		Rue rue = null;
		for (Rue r : getAllRues()) {
			if (r.getNoRue().equals(numero)) {
				rue = r;
			}
		}
		return rue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getSuisse()
	 */
	public Pays getSuisse() throws ServiceInfrastructureException {
		return MockPays.Suisse;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getVaud()
	 */
	public Canton getVaud() throws InfrastructureException {
		return MockCanton.Vaud;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getACI()
	 */
	public CollectiviteAdministrative getACI() throws InfrastructureException {
		return MockCollectiviteAdministrative.ACI;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCEDI()
	 */
	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		return MockCollectiviteAdministrative.CEDI;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getACI()
	 */
	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		return MockCollectiviteAdministrative.CAT;
	}



	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCommuneByNoTechnique(int)
	 */
	public Commune getCommuneByNumeroOfsEtendu(int noCommune) throws InfrastructureException {
		Commune commune = null;
		for (Commune c : communesVaud) {
			int no = c.getNoOFSEtendu();
			if (no == noCommune) {
				commune = c;
				break;
			}
		}
		if (commune == null) {
			for (Commune c : communesHorsCanton) {
				if (c.getNoOFSEtendu() == noCommune) {
					commune = c;
				}
			}
		}
		return commune;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCommuneByLocalite(Localite)
	 */
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		Commune commune = null;
		int numOrdreP = localite.getNoOrdre();
		int noCommune = localite.getNoCommune();
		if(numOrdreP == 540){//1341 Orient -> fraction l'orient 8002
			noCommune = 8002;
		}
		else if(numOrdreP == 541){//1346 les Bioux -> fraction les Bioux 8012
			noCommune = 8012;
		}
		else if(numOrdreP == 542){//1344 l'Abbaye -> commune de l'Abbaye 5871
			noCommune = 5871;
		}
		else if(numOrdreP == 543){//1342 le Pont -> commune de l'Abbaye 5871
			noCommune = 5871;
		}
		else if(numOrdreP == 546){//1347 le Sentier -> fraction le Sentier 8000
			noCommune = 8000;
		}
		else if(numOrdreP == 547){//1347 le Solliat -> commune Chenit 5872
			noCommune = 5872;
		}
		else if(numOrdreP == 550){//1348 le Brassus -> fraction le Brassus 8001
			noCommune = 8001;
		}

		for (Commune c : communesVaud) {
			int no = c.getNoOFSEtendu();
			if (no == noCommune) {
				commune = c;
				break;
			}
		}
		if (commune == null) {
			for (Commune c : communesHorsCanton) {
				if (c.getNoOFSEtendu() == localite.getNoCommune()) {
					commune = c;
				}
			}
		}
		return commune;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCollectivite(int)
	 */
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		CollectiviteAdministrative collectivite = null;
		for (CollectiviteAdministrative c : collectivitesAdministrative) {
			if (c.getNoColAdm() == noColAdm) {
				collectivite = c;
				break;
			}
		}

		return collectivite;
	}

	/**
	 * {@inheritDoc}
	 */
	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		// l'office du pont
		OfficeImpot office=null;
		if (noCommune==5586 ) {//lAbbaye, le Pont
			MockAdresse adresse = new MockAdresse();
			office = new MockOfficeImpot(7, adresse, "Office d'impôt des districts de lausanne", "Lausanne et Ouest lausannois", "", "OID LAUSANNE");
		}
		return office;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		// CollectiviteAdministrative collectivite = null;
		List<OfficeImpot> officesImpot = new ArrayList<OfficeImpot>();

		// for (CollectiviteAdministrative c : collectivitesAdministrative) {
		// TODO (FDE) Test sur noTech
		// if (c.getType().getNoTechnique().intValue() == 2) {
		// officesImpot.add(c);
		// }
		// }

		return officesImpot;
	}

	/**
	 * Renvoie toutes les collectivites administratives du canton de Vaud
	 *
	 * @return
	 * @throws InfrastructureException
	 */
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		List<CollectiviteAdministrative> collectivitesAdministratives = new ArrayList<CollectiviteAdministrative>();
		return collectivitesAdministratives;
	}

	/**
	 * Renvoie toutes les collectivites administratives
	 *
	 * @return
	 * @throws InfrastructureException
	 */
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws InfrastructureException {
		List<CollectiviteAdministrative> collectivitesAdministratives = new ArrayList<CollectiviteAdministrative>();
		return collectivitesAdministratives;
	}


	/**
	 *
	 * @see ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getPaysInconnu()
	 */
	public Pays getPaysInconnu() throws InfrastructureException {
		return getPays(8999);
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {
		return null;
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException {
		return null;
	}
}
