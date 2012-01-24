package ch.vd.uniregctb.testing;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilProcessor;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class IcEvtCivilNaissanceTest extends InContainerTest {

	private EvenementCivilRegPPDAO evenementCivilExterneDAO;
	private EvenementCivilProcessor evenementCivilProcessor;


	@Test
	@Rollback
	@NotTransactional
	public void execute() throws Exception {

		final long noInd = 333527L;

		TransactionTemplate tmpl = new TransactionTemplate(getTransactionManager());
        tmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		tmpl.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				EvenementCivilRegPP evt = new EvenementCivilRegPP();
				evt.setId(9006L);
				evt.setDateEvenement(RegDate.get(2008, 2, 14));
				evt.setType(TypeEvenementCivil.NAISSANCE);
				evt.setNumeroOfsCommuneAnnonce(5516);
				evt.setNumeroIndividuPrincipal(noInd);

				evenementCivilExterneDAO.save(evt);
				return null;
			}
		});

		evenementCivilProcessor.traiteEvenementCivil(9006L);

		tmpl.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique tiers = getTiersDAO().getHabitantByNumeroIndividu(noInd);
				Assert.notNull(tiers, "Pas de Tiers créé");
				return null;
			}
		});
	}

	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setEvenementCivilExterneDAO(EvenementCivilRegPPDAO evenementCivilExterneDAO) {
		this.evenementCivilExterneDAO = evenementCivilExterneDAO;
	}

}
