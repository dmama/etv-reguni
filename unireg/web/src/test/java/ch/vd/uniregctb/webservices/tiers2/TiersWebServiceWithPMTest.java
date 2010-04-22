package ch.vd.uniregctb.webservices.tiers2;

import java.util.HashSet;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.webservices.common.NoOfsTranslatorImpl;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.impl.pm.TiersWebServiceWithPM;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.*;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-webut-ws.xml"
})
public class TiersWebServiceWithPMTest extends WebTest {

	private TiersWebServiceWithPM service;
	private UserLogin login;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersWebService bean = getBean(TiersWebService.class, "tiersService2Bean");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		service = new TiersWebServiceWithPM();
		service.setServicePM(servicePM);
		service.setTarget(bean);
		service.setTiersDAO(tiersDAO);
		service.setNoOfsTranslator(new NoOfsTranslatorImpl());

		login = new UserLogin("iamtestuser", 22);
		serviceCivil.setUp(new DefaultMockServiceCivil());
		servicePM.setUp(new DefaultMockServicePM());
	}

	@Test
	public void testSetBlocageRemboursementAutomatiquePMInconnueDansUnireg() throws Exception {

		final long noBCV = MockPersonneMorale.BCV.getNumeroEntreprise();

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noBCV;

		// on s'assure que l'entreprise n'existe pas dans la base
		assertNull(tiersDAO.get(noBCV));

		// on s'assure que le code de blocage de remboursement est à true (valeur par défaut)
		{
			final PersonneMorale bcv = (PersonneMorale) service.getTiers(params);
			assertNotNull(bcv);
			assertTrue(bcv.blocageRemboursementAutomatique);
		}

		// on change le code de remboursement
		final SetTiersBlocRembAuto paramsBloc = new SetTiersBlocRembAuto();
		paramsBloc.blocage = false;
		paramsBloc.login = login;
		paramsBloc.tiersNumber = noBCV;
		service.setTiersBlocRembAuto(paramsBloc);

		// on s'assure que le code de blocage de remboursement est à maintenant à false
		{
			final PersonneMorale bcv = (PersonneMorale) service.getTiers(params);
			assertNotNull(bcv);
			assertFalse(bcv.blocageRemboursementAutomatique);
		}

		// on s'assure que l'entreprise a été crée dans la base
		final Entreprise bcv = (Entreprise) tiersDAO.get(noBCV);
		assertNotNull(bcv);
	}

	@Test
	public void testSetBlocageRemboursementAutomatiquePMConnueDansUnireg() throws Exception {

		final long noNestle = MockPersonneMorale.NestleSuisse.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				addEntreprise(noNestle);
				return null;
			}
		});

		// on s'assure que l'entreprise existe dans la base
		final Entreprise ent = (Entreprise) tiersDAO.get(noNestle);
		assertNotNull(ent);

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noNestle;

		// on s'assure que le code de blocage de remboursement est à true (valeur par défaut)
		{
			final PersonneMorale nestle = (PersonneMorale) service.getTiers(params);
			assertNotNull(nestle);
			assertTrue(nestle.blocageRemboursementAutomatique);
		}

		// on change le code de remboursement
		final SetTiersBlocRembAuto paramsBloc = new SetTiersBlocRembAuto();
		paramsBloc.blocage = false;
		paramsBloc.login = login;
		paramsBloc.tiersNumber = noNestle;
		service.setTiersBlocRembAuto(paramsBloc);

		// on s'assure que le code de blocage de remboursement est à maintenant à false
		{
			final PersonneMorale nestle = (PersonneMorale) service.getTiers(params);
			assertNotNull(nestle);
			assertFalse(nestle.blocageRemboursementAutomatique);
		}
	}

	/**
	 * [UNIREG-2302]
	 */
	@Test
	public void testGetAdresseEnvoiPersonneMorale() throws Exception {

		final long noBCV = MockPersonneMorale.BCV.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				addEntreprise(noBCV);
				return null;
			}
		});

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noBCV;
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.ADRESSES_ENVOI);

		// on s'assure que la formule d'appel d'une PM est bien renseignée
		{
			final PersonneMorale bcv = (PersonneMorale) service.getTiers(params);
			assertNotNull(bcv);
			assertNotNull(bcv.adresseEnvoi);

			// l'adresse d'envoi n'a pas de salutations
			assertNull(bcv.adresseEnvoi.salutations);
			assertEquals("BCV", bcv.adresseEnvoi.ligne1);
			assertEquals("pa Comptabilité financière", bcv.adresseEnvoi.ligne2);
			assertEquals("Saint-François, place 14", bcv.adresseEnvoi.ligne3);
			assertEquals("1003 Lausanne Secteur de dist.", bcv.adresseEnvoi.ligne4);
			assertNull(bcv.adresseEnvoi.ligne5);
			assertNull(bcv.adresseEnvoi.ligne6);

			// par contre, la formule d'appel est renseignée
			assertEquals("Madame, Monsieur", bcv.adresseEnvoi.formuleAppel);
		}
	}
}
