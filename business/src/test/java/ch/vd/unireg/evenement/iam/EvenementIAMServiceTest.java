package ch.vd.unireg.evenement.iam;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;

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
				List<InfoEmployeur> listeEmp = new ArrayList<>();
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
	public void testIAMSansInformationsEmployeur() throws Exception {

		// Création d'un débiteur
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// Un tiers tout ce quil y a de plus ordinaire
				final DebiteurPrestationImposable siggenAirlines = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2010, 1, 1));
				return siggenAirlines.getNumero();
			}
		});

		try {
			// Simule la réception d'un enregistrement de debiteur
			doInNewTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					final InfoEmployeur infoEmployeur = new InfoEmployeur();
					final EnregistrementEmployeur enregistrementEmployeur = new EnregistrementEmployeur();
					enregistrementEmployeur.setBusinessId("business-id-de-test");
					service.onEnregistrementEmployeur(enregistrementEmployeur);
					return null;
				}
			});
		}catch (EvenementIAMException e){
			Assert.fail("On ne devrait plus avoir d'exception en cas dinformations employeur absentes");
		}



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
				List<InfoEmployeur> listeEmp = new ArrayList<>();
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

				List<InfoEmployeur> listeEmp = new ArrayList<>();

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


}