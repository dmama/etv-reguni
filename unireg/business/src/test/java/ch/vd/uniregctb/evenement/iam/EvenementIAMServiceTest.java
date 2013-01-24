package ch.vd.uniregctb.evenement.iam;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EvenementIAMServiceTest extends BusinessTest {

	private EvenementIAMServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = new EvenementIAMServiceImpl();
		service.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		service.setValidationService(getBean(ValidationService.class, "validationService"));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifierInfoEmployeur() throws Exception {

		// Création d'un débiteur
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// Un tiers tout ce quil y a de plus ordinaire
				final DebiteurPrestationImposable siggenAirlines = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2010, 1, 1));
				return siggenAirlines.getNumero();
			}
		});

		// Simule la réception d'un enregistrement de debiteur
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final InfoEmployeur infoEmployeur = new InfoEmployeur();
				infoEmployeur.setLogicielId(18L);
				infoEmployeur.setNoEmployeur(id);
				infoEmployeur.setModeCommunication(ModeCommunication.ELECTRONIQUE);
				List<InfoEmployeur> listeEmp = new ArrayList<InfoEmployeur>();
				listeEmp.add(infoEmployeur);
				final EnregistrementEmployeur enregistrementEmployeur = new EnregistrementEmployeur();
				enregistrementEmployeur.setEmployeursAMettreAJour(listeEmp);
				service.onEnregistrementEmployeur(enregistrementEmployeur);
				return null;
			}
		});

		// Vérifie que les informations du débiteur ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final DebiteurPrestationImposable siggenAirlines = (DebiteurPrestationImposable) hibernateTemplate.get(DebiteurPrestationImposable.class, id);
				assertEquals(18L, siggenAirlines.getLogicielId().longValue());
				assertEquals(ModeCommunication.ELECTRONIQUE, siggenAirlines.getModeCommunication());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifierInfoEmployeurSansLogiciel() throws Exception {

		// Création d'un débiteur
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// Un tiers tout ce quil y a de plus ordinaire
				final DebiteurPrestationImposable siggenAirlines = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2010, 1, 1));
				return siggenAirlines.getNumero();
			}
		});

		// Simule la réception d'un enregistrement de debiteur
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final InfoEmployeur infoEmployeur = new InfoEmployeur();
				infoEmployeur.setNoEmployeur(id);
				infoEmployeur.setModeCommunication(ModeCommunication.ELECTRONIQUE);
				List<InfoEmployeur> listeEmp = new ArrayList<InfoEmployeur>();
				listeEmp.add(infoEmployeur);
				final EnregistrementEmployeur enregistrementEmployeur = new EnregistrementEmployeur();
				enregistrementEmployeur.setEmployeursAMettreAJour(listeEmp);
				service.onEnregistrementEmployeur(enregistrementEmployeur);
				return null;
			}
		});

		// Vérifie que les informations du débiteur ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final DebiteurPrestationImposable siggenAirlines = (DebiteurPrestationImposable) hibernateTemplate.get(DebiteurPrestationImposable.class, id);
				assertEquals(null, siggenAirlines.getLogicielId());
				assertEquals(ModeCommunication.ELECTRONIQUE, siggenAirlines.getModeCommunication());
				return null;
			}
		});
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifierInfo2Employeur() throws Exception {

		// Création d'un débiteur 1
		final Long id1 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// Un tiers tout ce quil y a de plus ordinaire
				final DebiteurPrestationImposable siggenAirlines = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2010, 1, 1));
				siggenAirlines.setModeCommunication(ModeCommunication.SITE_WEB);
				siggenAirlines.setLogicielId(1L);
				return siggenAirlines.getNumero();
			}
		});

		// Création d'un débiteur 2
		final Long id2 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// Un tiers tout ce quil y a de plus ordinaire
				final DebiteurPrestationImposable siggenHolding = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.ANNUEL, date(2010, 1, 1));
				siggenHolding.setModeCommunication(ModeCommunication.PAPIER);
				return siggenHolding.getNumero();
			}
		});


		// Simule la réception d'un enregistrement de debiteur
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				List<InfoEmployeur> listeEmp = new ArrayList<InfoEmployeur>();

				final InfoEmployeur infoEmployeurAirlines = new InfoEmployeur();
				infoEmployeurAirlines.setLogicielId(18L);
				infoEmployeurAirlines.setNoEmployeur(id1);
				infoEmployeurAirlines.setModeCommunication(ModeCommunication.ELECTRONIQUE);
				listeEmp.add(infoEmployeurAirlines);


				final InfoEmployeur infoEmployeurHolding = new InfoEmployeur();
				infoEmployeurHolding.setLogicielId(12L);
				infoEmployeurHolding.setNoEmployeur(id2);
				infoEmployeurHolding.setModeCommunication(ModeCommunication.SITE_WEB);
				listeEmp.add(infoEmployeurHolding);

				final EnregistrementEmployeur enregistrementEmployeur = new EnregistrementEmployeur();
				enregistrementEmployeur.setEmployeursAMettreAJour(listeEmp);
				service.onEnregistrementEmployeur(enregistrementEmployeur);
				return null;
			}
		});

		// Vérifie que les informations des débiteurs ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final DebiteurPrestationImposable siggenAirlines = (DebiteurPrestationImposable) hibernateTemplate.get(DebiteurPrestationImposable.class, id1);
				assertEquals(18L, siggenAirlines.getLogicielId().longValue());
				assertEquals(ModeCommunication.ELECTRONIQUE, siggenAirlines.getModeCommunication());

				final DebiteurPrestationImposable siggenHolding = (DebiteurPrestationImposable) hibernateTemplate.get(DebiteurPrestationImposable.class, id2);
				assertEquals(12L, siggenHolding.getLogicielId().longValue());
				assertEquals(ModeCommunication.SITE_WEB, siggenHolding.getModeCommunication());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifierInfoEmployeurIncoherenceAction() throws Exception {

		// Création d'un débiteur
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// Un tiers tout ce quil y a de plus ordinaire
				final DebiteurPrestationImposable siggenAirlines = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2010, 1, 1));
				return siggenAirlines.getNumero();
			}
		});

		// Simule la réception d'un enregistrement de debiteur

		final InfoEmployeur infoEmployeur = new InfoEmployeur();
		infoEmployeur.setNoEmployeur(id);
		infoEmployeur.setModeCommunication(ModeCommunication.ELECTRONIQUE);
		List<InfoEmployeur> listeEmp = new ArrayList<InfoEmployeur>();
		listeEmp.add(infoEmployeur);
		final EnregistrementEmployeur enregistrementEmployeur = new EnregistrementEmployeur();

		try {
			service.onEnregistrementEmployeur(enregistrementEmployeur);
			fail();
		}
		catch (EvenementIAMException e) {
			assertEquals("Informations employeurs absentes pour une action create ou update", e.getMessage());
		}


	}

}