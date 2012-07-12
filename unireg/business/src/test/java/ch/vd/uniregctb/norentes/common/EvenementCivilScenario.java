package ch.vd.uniregctb.norentes.common;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilProcessor;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public abstract class EvenementCivilScenario extends EvenementScenario {

	protected ProxyServiceCivil serviceCivilService;

	protected EvenementCivilProcessor evenementCivilProcessor;

	protected EvenementCivilRegPPDAO evtExterneDAO;

	protected TacheDAO tacheDAO;

	private long nextEvtId = 1;

	@Override
	public void onInitialize() {
		super.onInitialize();

		initServiceCivil();
	}

	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil());
	}

	@Override
	public void onFinalize() {
		super.onFinalize();
		serviceCivilService.tearDown();
	}

	protected void traiteEvenements(long id) throws Exception {
		evenementCivilProcessor.traiteEvenementCivil(id);
	}

	protected long addEvenementCivil(TypeEvenementCivil type, long numeroIndividu, RegDate date, int ofs) {
		return addEvenementCivil(type, numeroIndividu, null, date, ofs);
	}


	protected long addEvenementCivil(TypeEvenementCivil type, long numeroIndividuPrincipal,Long numeroIndividuConjoint, RegDate date, int ofs) {
		EvenementCivilRegPP evt = new EvenementCivilRegPP();
		evt.setId(nextEvtId);
		evt.setDateEvenement(date);
		evt.setEtat(EtatEvenementCivil.A_TRAITER);
		evt.setNumeroIndividuPrincipal(numeroIndividuPrincipal);
		evt.setNumeroIndividuConjoint(numeroIndividuConjoint);
		evt.setType(type);
		evt.setNumeroOfsCommuneAnnonce(ofs);


		evtExterneDAO.save(evt);

		nextEvtId++;

		return evt.getId();
	}

	protected EvenementCivilRegPP getEvenementCivilRegoupeForHabitant(long id) {
		
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(id);
		final Long numeroInd = pp.getNumeroIndividu();

		final List<EvenementCivilRegPP> list = evtExterneDAO.findEvenementByIndividu(numeroInd);
		if (list == null || list.isEmpty()) {
			return null;
		}

		return list.get(0);
	}

	protected List<EvenementCivilRegPP> getEvenementsCivils(long habitant, TypeEvenementCivil type) {
		final EvenementCivilCriteria criterion = new EvenementCivilCriteria();
		criterion.setNumeroIndividu(habitant);
		criterion.setType(type);
		return evtExterneDAO.find(criterion, null);
	}

	protected void checkEtatEvtCivils(int nb, EtatEvenementCivil etat) {
		List<EvenementCivilRegPP> evts =  evtExterneDAO.getAll();
		assertNotNull(evts, "aucun événement en base");
		assertEquals(nb, evts.size(), "pas le bon nombre d'événement");
		for (EvenementCivilRegPP evt : evts) {
			assertEquals(etat, evt.getEtat(), "pas le bon état");
		}
	}


	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setEvtExterneDAO(EvenementCivilRegPPDAO evtExterneDAO) {
		this.evtExterneDAO = evtExterneDAO;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setServiceCivilService(ProxyServiceCivil serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	protected static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);

	}
	/**
	 * Vérifie que l'état du blocage des remboursements automatiques sur le tiers est bien celui attendu
	 * (une valeur non-assignée sur le tiers est équivalente à <b>false</b>)
	 */
	protected static void assertBlocageRemboursementAutomatique(boolean blocageAttendu, Tiers tiers) {

		final Boolean remboursementBloque = tiers.getBlocageRemboursementAutomatique();
		if (blocageAttendu) {
			assertNotNull(remboursementBloque, "Etat de blocage des remboursements automatique non assigné pour tiers " + tiers.getNumero());
			assertTrue(remboursementBloque, "Remboursements automatiques non bloqués pour tiers " + tiers.getNumero());
		}
		else {
			assertTrue(remboursementBloque == null || !remboursementBloque, "Remboursements automatiques bloqués pour tiers " + tiers.getNumero());
		}
	}

	/**
	 * Vérifie que la situation de famille spécifiée correspondant aux valeurs spécifiées.
	 */
	protected static void assertSituationFamille(RegDate dateDebut, RegDate dateFin, EtatCivil etatCivil, int nbEnfants,
			SituationFamille sf, String message) {
		assertNotNull(sf, message + " la situation de famille devrait exister");

		message += String.format("\n - attendu: période du %s au %s, nombre d'enfants %d, état civil %s", dateDebut, dateFin, nbEnfants,
				etatCivil);
		message += String.format("\n - trouvé: période du %s au %s, nombre d'enfants %d, état civil %s.", sf.getDateDebut(), sf.getDateFin(), sf
				.getNombreEnfants(), sf.getEtatCivil());
		assertEquals(dateDebut, sf.getDateDebut(), message);
		assertEquals(dateFin, sf.getDateFin(), message);
		assertEquals(nbEnfants, sf.getNombreEnfants(), message);
		assertEquals(etatCivil, sf.getEtatCivil(), message);
	}

	protected void traiteEvenementsAnciens(long id) throws Exception {
		evenementCivilProcessor.traiteEvenementCivil(id);
	}

	protected static interface IndividuModification {
		void modifyIndividu(MockIndividu individu);
	}

	protected void doModificationIndividu(long noIndividu, IndividuModification modifier) {
		final MockIndividu ind = ((MockServiceCivil) serviceCivilService.getTarget()).getIndividu(noIndividu);
		modifier.modifyIndividu(ind);
	}
}
