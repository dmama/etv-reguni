package ch.vd.unireg.interfaces.infra.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

/**
 * Mock du Host Infrastructure Service.
 * <p/>
 * Pour utiliser cette classe, le plus simple est d'instancier une classe anonyme et d'implémenter la méthode init() de manière à charger les données de test voulues.
 * <p/>
 * Par exemple:
 * <p/>
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
public abstract class MockServiceInfrastructureService implements ServiceInfrastructureRaw {

	// private static final Logger LOGGER = Logger.getLogger(MockServiceInfrastructureService.class);

	protected final List<Pays> pays = new ArrayList<Pays>();
	protected final List<Canton> cantons = new ArrayList<Canton>();
	protected final List<Localite> localites = new ArrayList<Localite>();
	protected final List<Commune> communesVaud = new ArrayList<Commune>();
	protected final List<Commune> communesHorsCanton = new ArrayList<Commune>();
	protected final List<Commune> communes = new ArrayList<Commune>();
	protected final List<Rue> rues = new ArrayList<Rue>();
	protected final Map<Integer, CollectiviteAdministrative> collectivitesAdministrative = new HashMap<Integer, CollectiviteAdministrative>();
	protected final Map<Integer, OfficeImpot> oidByNoOfsCommune = new HashMap<Integer, OfficeImpot>();
	protected final Map<Integer, OfficeImpot> oidByNoColAdm = new HashMap<Integer, OfficeImpot>();
	protected Map<Integer, List<MockLienCommuneBatiment>> batimentsParEgid = null;

	public MockServiceInfrastructureService() {
		init();
	}

	protected abstract void init();

	protected void copyFrom(MockServiceInfrastructureService right) {
		this.pays.addAll(right.pays);
		this.cantons.addAll(right.cantons);
		this.localites.addAll(right.localites);
		this.communesVaud.addAll(right.communesVaud);
		this.communesHorsCanton.addAll(right.communesHorsCanton);
		this.communes.addAll(right.communes);
		this.rues.addAll(right.rues);
		for (Map.Entry<Integer, CollectiviteAdministrative> entry : right.collectivitesAdministrative.entrySet()) {
			add((MockCollectiviteAdministrative) entry.getValue());
		}
		for (Map.Entry<Integer, OfficeImpot> entry : right.oidByNoOfsCommune.entrySet()) {
			this.oidByNoOfsCommune.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<Integer, OfficeImpot> entry : right.oidByNoColAdm.entrySet()) {
			this.oidByNoColAdm.put(entry.getKey(), entry.getValue());
		}
	}

	protected void add(MockLocalite loc) {
		localites.add(loc);
	}

	protected void add(MockCanton c) {
		cantons.add(c);
	}

	protected void add(MockPays p) {
		pays.add(p);
	}

	protected void add(MockRue r) {
		rues.add(r);
	}

	protected void add(MockCommune c) {
		if (c.isVaudoise()) {
			communesVaud.add(c);
		}
		else {
			communesHorsCanton.add(c);
		}
		communes.add(c);

		OfficeImpot office = c.getOfficeImpot();
		if (office != null) { // les communes hors-canton ne possèdent pas d'oid
			oidByNoOfsCommune.put(c.getNoOFSEtendu(), office);
			oidByNoColAdm.put(office.getNoColAdm(), office);
		}
	}

	private Map<Integer, List<MockLienCommuneBatiment>> getBatimentsParEgid() {
		initBatiments();
		return batimentsParEgid;
	}

	private void initBatiments() {
		if (batimentsParEgid == null) {
			loadBatiments();
		}
	}

	private synchronized void loadBatiments() {
		if (batimentsParEgid == null) {
			final HashMap<Integer, List<MockLienCommuneBatiment>> map = new HashMap<Integer, List<MockLienCommuneBatiment>>();
			for (Commune c : communes) {
				addLiensBatiments(map, (MockCommune) c);
			}
			batimentsParEgid = map;
		}
	}

	protected void addLiensBatiments(Map<Integer, List<MockLienCommuneBatiment>> map, MockCommune c) {
		for (MockLienCommuneBatiment lien : c.getLiensBatiments()) {
			final Integer egid = lien.getBatiment().getEgid();
			List<MockLienCommuneBatiment> list = map.get(egid);
			if (list == null) {
				list = new ArrayList<MockLienCommuneBatiment>();
				map.put(egid, list);
			}
			list.add(lien);
		}
	}

	protected void add(MockCollectiviteAdministrative coladm) {
		final Integer noColAdm = coladm.getNoColAdm();
		if (collectivitesAdministrative.containsKey(noColAdm)) {
			throw new RuntimeException("La collectivité avec le numéro [" + noColAdm + "] existe déjà.");
		}
		collectivitesAdministrative.put(noColAdm, coladm);
	}


	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return cantons;
	}

	public List<Commune> getCommunesDeVaud() throws ServiceInfrastructureException {
		return communesVaud;
	}

	public List<Commune> getCommunesHorsCanton() throws ServiceInfrastructureException {
		return communesHorsCanton;
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return communes;
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return communesVaud;
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		return communesVaud;
	}

	public List<Commune> getListeCommunes(String cantonAsString) throws ServiceInfrastructureException {
		if ("VD".equals(cantonAsString))
			return communesVaud;
		else
			return communesHorsCanton;
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return localites;
	}

	@Override
	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		Localite localite = null;
		for (Localite loc : getLocalites()) {
			if (loc.getNoOrdre() == onrp) {
				localite = loc;
				break;
			}
		}
		return localite;
	}

	@Override
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		Localite localite = null;
		for (Localite loc : getLocalites()) {
			if (loc.getNPA() == npa) {
				localite = loc;
				break;
			}
		}
		return localite;
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return pays;
	}

	@Override
	public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
		for (Pays p : pays) {
			if (p.getNoOFS() == numeroOFS) {
				return p;
			}
		}
		return null;
	}

	@Override
	public Pays getPays(String codePays) throws ServiceInfrastructureException {
		for (Pays p : pays) {
			if (p.getCodeIso2().equals(codePays)) {
				return p;
			}
		}
		return null;
	}

	@Override
	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		List<Rue> locRues = new ArrayList<Rue>();
		for (Rue r : rues) {
			if (r.getNoLocalite().equals(localite.getNoOrdre())) {
				locRues.add(r);
			}
		}
		return locRues;
	}

	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		Rue rue = null;
		for (Rue r : rues) {
			if (r.getNoRue().equals(numero)) {
				rue = r;
			}
		}
		return rue;
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		return MockPays.Suisse;
	}

	public Canton getVaud() throws ServiceInfrastructureException {
		return MockCanton.Vaud;
	}

	public CollectiviteAdministrative getACI() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.ACI;
	}

	public CollectiviteAdministrative getACIImpotSource() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.ACIIMPOTSOURCE;
	}

	public CollectiviteAdministrative getACISuccessions() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.ACISUCCESSIONS;
	}

	public CollectiviteAdministrative getCEDI() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.CEDI;
	}

	public CollectiviteAdministrative getCAT() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.CAT;
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		final List<Commune> list = new ArrayList<Commune>(2);
		for (Commune c : communesVaud) {
			int no = c.getNoOFSEtendu();
			if (no == noOfsCommune) {
				list.add(c);
			}
		}
		for (Commune c : communesHorsCanton) {
			if (c.getNoOFSEtendu() == noOfsCommune) {
				list.add(c);
			}
		}
		return list;
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		Commune commune = null;
		int numOrdreP = localite.getNoOrdre();
		int noCommune = localite.getNoCommune();
		if (numOrdreP == 540) {//1341 Orient -> fraction l'orient 8002
			noCommune = 8002;
		}
		else if (numOrdreP == 541) {//1346 les Bioux -> fraction les Bioux 8012
			noCommune = 8012;
		}
		else if (numOrdreP == 542) {//1344 l'Abbaye -> commune de l'Abbaye 5871
			noCommune = 5871;
		}
		else if (numOrdreP == 543) {//1342 le Pont -> commune de l'Abbaye 5871
			noCommune = 5871;
		}
		else if (numOrdreP == 546) {//1347 le Sentier -> fraction le Sentier 8000
			noCommune = 8000;
		}
		else if (numOrdreP == 547) {//1347 le Solliat -> commune Chenit 5872
			noCommune = 5872;
		}
		else if (numOrdreP == 550) {//1348 le Brassus -> fraction le Brassus 8001
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

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		final List<MockLienCommuneBatiment> liens = getBatimentsParEgid().get(egid);
		if (liens == null || liens.isEmpty()) {
			return null;
		}

		MockCommune commune = null;
		for (MockLienCommuneBatiment lien : liens) {
			if (lien.isValidAt(date)) {
				if (commune != null) {
					throw new RuntimeException(
							"Le bâtiment avec l'egid [" + egid + "] est enregistré dans deux communes en même temps : numéro Ofs [" + commune.getNoOFSEtendu() + "] et numéro Ofs [" +
									lien.getCommune().getNoOFSEtendu() + ']');
				}
				commune = lien.getCommune();
			}
		}

		if (commune == null) {
			return null;
		}

		return commune.getNoOFSEtendu();
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return collectivitesAdministrative.get(noColAdm);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		return oidByNoOfsCommune.get(noCommune);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return new ArrayList<OfficeImpot>(oidByNoOfsCommune.values());
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return new ArrayList<CollectiviteAdministrative>(collectivitesAdministrative.values());
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {

		final Set<String> sigles = new HashSet<String>();
		for (EnumTypeCollectivite e : typesCollectivite) {
			sigles.add(e.getName());
		}

		final List<CollectiviteAdministrative> list = new ArrayList<CollectiviteAdministrative>();
		for (CollectiviteAdministrative ca : collectivitesAdministrative.values()) {
			if (sigles.contains(ca.getSigle())) {
				list.add(ca);
			}
		}
		return list;
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		return null;
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		return null;
	}
}
