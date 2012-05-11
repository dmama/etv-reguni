package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class DemenagementEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	private ServiceInfrastructureService infraService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
	}

	@Test(timeout = 10000L)
	public void testDemenagementCelibataire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateDemenagement = date(2011, 10, 31);
		final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
		final RegDate dateMajorite = date(1974, 4, 23);
		final RegDate dateNaissance = date(1956, 4, 23);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				
				final MockIndividu osvalde = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);

				final MockAdresse adresseAvant = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, veilleDemenagement);
				final MockAdresse adresseApres = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateDemenagement, null);

				addNationalite(osvalde, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique osvalde = addHabitant(noIndividu);
				addForPrincipal(osvalde, dateMajorite,MotifFor.MAJORITE, MockCommune.Cossonay);
				addAdresseSuisse(osvalde, TypeAdresseTiers.DOMICILE,dateMajorite,null,MockRue.CossonayVille.AvenueDuFuniculaire);
				return null;
			}
		});
		
		// événement demenagement
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDemenagement);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateMajorite, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementSecondaire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateDemenagement = date(2011, 10, 31);
		final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
		final RegDate dateMajorite = date(1974, 4, 23);
		final RegDate dateNaissance = date(1956, 4, 23);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {

				final MockIndividu osvalde = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);
				final MockAdresse adressePrincipal  = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateMajorite, null);
				final MockAdresse adresseAvant = addAdresse(osvalde, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, veilleDemenagement);
				final MockAdresse adresseApres = addAdresse(osvalde, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateDemenagement, null);

				addNationalite(osvalde, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique osvalde = addHabitant(noIndividu);
				addForPrincipal(osvalde, dateMajorite,MotifFor.MAJORITE, MockPays.Espagne);
				return null;
			}
		});

		// événement demenagement
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDemenagement);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementSecondaireSansChangementAdresse() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateDemenagement = date(2011, 10, 31);
		final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
		final RegDate dateMajorite = date(1974, 4, 23);
		final RegDate dateNaissance = date(1956, 4, 23);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {

				final MockIndividu osvalde = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);
				final MockAdresse adressePrincipal  = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateMajorite, null);
				final MockAdresse adresseAvant = addAdresse(osvalde, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);

				addNationalite(osvalde, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique osvalde = addHabitant(noIndividu);
				addForPrincipal(osvalde, dateMajorite,MotifFor.MAJORITE, MockPays.Espagne);
				return null;
			}
		});

		// événement demenagement
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDemenagement);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				return null;
			}
		});
	}
}
