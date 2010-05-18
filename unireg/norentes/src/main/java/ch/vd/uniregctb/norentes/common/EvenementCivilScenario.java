package ch.vd.uniregctb.norentes.common;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.engine.EvenementCivilProcessor;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public abstract class EvenementCivilScenario extends EvenementScenario {

	protected ServiceCivilService serviceCivilService;

	protected EvenementCivilProcessor evenementCivilProcessor;

	protected EvenementCivilDAO evtDAO;

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
		EvenementCivilData evt = new EvenementCivilData();
		evt.setId(nextEvtId);
		evt.setDateEvenement(date);
		evt.setEtat(EtatEvenementCivil.A_TRAITER);
		evt.setNumeroIndividuPrincipal(numeroIndividuPrincipal);
		evt.setHabitantPrincipalId(tiersDAO.getNumeroPPByNumeroIndividu(numeroIndividuPrincipal, true));
		if (numeroIndividuConjoint != null) {
			evt.setNumeroIndividuConjoint(numeroIndividuConjoint);
			evt.setHabitantConjointId(tiersDAO.getNumeroPPByNumeroIndividu(numeroIndividuConjoint, true));
		}
		evt.setType(type);
		evt.setNumeroOfsCommuneAnnonce(ofs);


		evtDAO.save(evt);

		nextEvtId++;

		return evt.getId();
	}

	protected EvenementCivilData getEvenementCivilRegoupeForHabitant(long id) {
		List<EvenementCivilData> list = evtDAO.getAll();

		EvenementCivilData evt = null;
		for (EvenementCivilData e : list) {
			Long h1 = e.getHabitantPrincipalId();
			if (h1 != null) {
				if (h1.equals(id)) {
					evt = e;
					break;
				}
			}
			Long h2 = e.getHabitantConjointId();
			if (h2 != null) {
				if (h2.equals(id)) {
					evt = e;
					break;
				}
			}
		}
		return evt;
	}

	protected void checkEtatEvtCivils(int nb, EtatEvenementCivil etat) {
		List<EvenementCivilData> evts =  evtDAO.getAll();
		assertNotNull(evts, "aucun événement en base");
		assertEquals(nb, evts.size(), "pas le bon nombre d'événement");
		for (EvenementCivilData evt : evts) {
			assertEquals(etat, evt.getEtat(), "pas le bon état");
		}
	}


	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setEvtDAO(EvenementCivilDAO evtDAO) {
		this.evtDAO = evtDAO;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
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
		assertEquals(Integer.valueOf(nbEnfants), sf.getNombreEnfants(), message);
		assertEquals(etatCivil, sf.getEtatCivil(), message);
	}

	protected void traiteEvenementsAnciens(long id) throws Exception {
		evenementCivilProcessor.traiteEvenementCivil(id);
	}

}
