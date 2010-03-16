package ch.vd.uniregctb.norentes.common;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupeDAO;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaireDAO;
import ch.vd.uniregctb.evenement.engine.EvenementCivilProcessor;
import ch.vd.uniregctb.evenement.engine.EvenementCivilRegrouper;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public abstract class EvenementCivilScenario extends EvenementScenario {

	protected ServiceCivilService serviceCivilService;

	protected EvenementCivilProcessor evenementCivilProcessor;
	protected EvenementCivilRegrouper evenementCivilRegrouper;

	protected EvenementCivilUnitaireDAO evtUnitaireDAO;
	protected EvenementCivilRegroupeDAO evtRegroupeDAO;

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

	protected void regroupeEtTraiteEvenements(long id) throws Exception {
		long idEvtReg = evenementCivilRegrouper.regroupeUnEvenementById(id, null);
		evenementCivilProcessor.traiteEvenementCivilRegroupe(idEvtReg);
	}

	protected long addEvenementCivil(TypeEvenementCivil type, long numeroIndividu, RegDate date, int ofs) {
		EvenementCivilUnitaire evt = new EvenementCivilUnitaire();
		evt.setId(nextEvtId);
		evt.setDateEvenement(date);
		evt.setEtat(EtatEvenementCivil.A_TRAITER);
		evt.setNumeroIndividu(numeroIndividu);
		evt.setType(type);
		evt.setNumeroOfsCommuneAnnonce(ofs);

		evtUnitaireDAO.save(evt);

		nextEvtId++;

		return evt.getId();
	}

	protected EvenementCivilRegroupe getEvenementCivilRegoupeForHabitant(long id) {
		List<EvenementCivilRegroupe> list = evtRegroupeDAO.getAll();

		EvenementCivilRegroupe evt = null;
		for (EvenementCivilRegroupe e : list) {
			PersonnePhysique h1 = e.getHabitantPrincipal();
			if (h1 != null) {
				if (h1.getNumero().equals(id)) {
					evt = e;
					break;
				}
			}
			PersonnePhysique h2 = e.getHabitantConjoint();
			if (h2 != null) {
				if (h2.getNumero().equals(id)) {
					evt = e;
					break;
				}
			}
		}
		return evt;
	}

	protected void checkEtatEvtCivils(int nb, EtatEvenementCivil etat) {
		List<EvenementCivilRegroupe> evts =  evtRegroupeDAO.getAll();
		assertNotNull(evts, "aucun événement en base");
		assertEquals(nb, evts.size(), "pas le bon nombre d'événement");
		for (EvenementCivilRegroupe evt : evts) {
			assertEquals(etat, evt.getEtat(), "pas le bon état");
		}
	}


	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setEvenementCivilRegrouper(EvenementCivilRegrouper evenementCivilRegrouper) {
		this.evenementCivilRegrouper = evenementCivilRegrouper;
	}

	public void setEvtUnitaireDAO(EvenementCivilUnitaireDAO evtUnitaireDAO) {
		this.evtUnitaireDAO = evtUnitaireDAO;
	}

	public void setEvtRegroupeDAO(EvenementCivilRegroupeDAO evtRegroupeDAO) {
		this.evtRegroupeDAO = evtRegroupeDAO;
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

}
