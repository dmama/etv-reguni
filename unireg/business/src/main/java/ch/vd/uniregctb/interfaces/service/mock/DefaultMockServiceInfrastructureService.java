package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;

public class DefaultMockServiceInfrastructureService extends MockServiceInfrastructureService {

	protected static List<Pays> PAYS = new ArrayList<Pays>();

	protected static List<Canton> CANTONS = new ArrayList<Canton>();

	protected static List<Localite> LOCALITES = new ArrayList<Localite>();

	protected static List<Commune> COMMUNES_VAUD = new ArrayList<Commune>();

	protected static List<Commune> COMMUNES_HC = new ArrayList<Commune>();

	protected static List<Commune> COMMUNES = new ArrayList<Commune>();

	protected static List<Rue> RUES = new ArrayList<Rue>();

	protected static Map<Integer, OfficeImpot> oidByNoOfsCommune = new HashMap<Integer, OfficeImpot>();

	protected static Map<Integer, OfficeImpot> oidByNoColAdm = new HashMap<Integer, OfficeImpot>();

	@Override
	protected void init() {

		MockPays.forceLoad();
		MockCanton.forceLoad();
		MockCommune.forceLoad();
		MockLocalite.forceLoad();
		MockRue.forceLoad();
		MockCollectiviteAdministrative.forceLoad();

		pays = PAYS;

		// Cantons
		cantons = CANTONS;

		// Communes VD
		communesVaud = COMMUNES_VAUD;

		// Communes hors canton
		communesHorsCanton = COMMUNES_HC;

		// Communes
		communes = COMMUNES;

		// Localites
		localites = LOCALITES;

		// Rues
		rues = RUES;

		// Collectivites : ajout du tuteur general
		collectivitesAdministrative.add(MockCollectiviteAdministrative.OTG);
		collectivitesAdministrative.add(MockCollectiviteAdministrative.CAT);
		collectivitesAdministrative.add(MockCollectiviteAdministrative.CEDI);
		collectivitesAdministrative.add(MockCollectiviteAdministrative.ACI);

		collectivitesAdministrative.add(MockOfficeImpot.OID_AIGLE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_ROLLE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_AVENCHE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_COSSONAY);
		collectivitesAdministrative.add(MockOfficeImpot.OID_ECHALLENS);
		collectivitesAdministrative.add(MockOfficeImpot.OID_GRANDSON);
		collectivitesAdministrative.add(MockOfficeImpot.OID_LAUSANNE_OUEST);
		collectivitesAdministrative.add(MockOfficeImpot.OID_LA_VALLEE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_LAVAUX);
		collectivitesAdministrative.add(MockOfficeImpot.OID_MORGES);
		collectivitesAdministrative.add(MockOfficeImpot.OID_MOUDON);
		collectivitesAdministrative.add(MockOfficeImpot.OID_NYON);
		collectivitesAdministrative.add(MockOfficeImpot.OID_ORBE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_ORON);
		collectivitesAdministrative.add(MockOfficeImpot.OID_PAYERNE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_PAYS_D_ENHAUT);
		collectivitesAdministrative.add(MockOfficeImpot.OID_ROLLE_AUBONNE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_VEVEY);
		collectivitesAdministrative.add(MockOfficeImpot.OID_YVERDON);
		collectivitesAdministrative.add(MockOfficeImpot.OID_LAUSANNE_VILLE);
		collectivitesAdministrative.add(MockOfficeImpot.OID_PM);
		collectivitesAdministrative.add(MockOfficeImpot.OID_ST_CROIX);
	}

	public static void addLocalite(MockLocalite loc) {
		LOCALITES.add(loc);
	}
	public static void addCanton(MockCanton c) {
		CANTONS.add(c);
	}
	public static void addPays(MockPays p) {
		PAYS.add(p);
	}
	public static void addRue(MockRue r) {
		RUES.add(r);
	}
	public static void addCommune(MockCommune c) {
		if (c.isVaudoise()) {
			COMMUNES_VAUD.add(c);
		}
		else {
			COMMUNES_HC.add(c);
		}
		COMMUNES.add(c);

		OfficeImpot office = c.getOfficeImpot();
		if (office != null) { // les communes hors-canton ne possèdent pas d'oid
			oidByNoOfsCommune.put(c.getNoOFS(), office);
			oidByNoColAdm.put(office.getNoColAdm(), office);
		}
	}

	@Override
	public Pays getPaysInconnu() throws InfrastructureException {
		return MockPays.PaysInconnu;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		return oidByNoOfsCommune.get(noCommune);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		return oidByNoColAdm.get(noColAdm);
	}


}
