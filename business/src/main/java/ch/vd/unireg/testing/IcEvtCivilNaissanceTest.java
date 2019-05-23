package ch.vd.unireg.testing;

import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilProcessor;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.TypeEvenementCivil;

public class IcEvtCivilNaissanceTest extends InContainerTest {

	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	private EvenementCivilProcessor evenementCivilProcessor;
	private ServiceCivilService serviceCivil;

	@Test
	@Rollback
	@NotTransactional
	public void execute() throws Exception {

		final long noInd = 333527L;

		if (serviceCivil instanceof ProxyServiceCivil) {
			((ProxyServiceCivil) serviceCivil).setUp(new MockIndividuConnector() {
				@Override
				protected void init() {
					addIndividu(noInd, RegDate.get(2002, 3, 2), "Annette", "Dulac", false);
				}
			});
		}

		TransactionTemplate tmpl = new TransactionTemplate(getTransactionManager());
        tmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		tmpl.execute(status -> {
			EvenementCivilRegPP evt = new EvenementCivilRegPP();
			evt.setId(9006L);
			evt.setDateEvenement(RegDate.get(2008, 2, 14));
			evt.setType(TypeEvenementCivil.NAISSANCE);
			evt.setNumeroOfsCommuneAnnonce(5516);
			evt.setNumeroIndividuPrincipal(noInd);

			evenementCivilRegPPDAO.save(evt);
			return null;
		});

		evenementCivilProcessor.traiteEvenementCivil(9006L);

		tmpl.execute(status -> {
			PersonnePhysique tiers = getTiersDAO().getHabitantByNumeroIndividu(noInd);
			if (tiers == null) {
				throw new IllegalArgumentException("Pas de Tiers créé");
			}
			return null;
		});
	}

	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setEvenementCivilRegPPDAO(EvenementCivilRegPPDAO evenementCivilRegPPDAO) {
		this.evenementCivilRegPPDAO = evenementCivilRegPPDAO;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}
}
