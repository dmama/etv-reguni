package ch.vd.unireg.declaration.snc;

import java.util.Collections;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.snc.liens.associes.DonneesLienAssocieEtSNC;
import ch.vd.unireg.declaration.snc.liens.associes.ImportLienAssociesSNCEnMasseProcessor;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCEnMasseImporterResults;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCService;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCServiceImpl;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapportEntreTiers;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
@PrepareForTest({CSVRecord.class})
public class ImportLienAssociesSNCEnmasseProcessorTest extends BusinessTest {

	private ImportLienAssociesSNCEnMasseProcessor processor;
	private TiersService tiersService;
	protected TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		final LienAssociesSNCService lienAssociesSNCService = getBean(LienAssociesSNCService.class, "lienAssociesSNCService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		processor = new ImportLienAssociesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, (LienAssociesSNCServiceImpl) lienAssociesSNCService);
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}

	@Test
	public void testCreationLienAssocieSNC_OK() throws Exception {

		try {
			// mise en place fiscale
			final Pair<String, String> pairSncAssocie = doInNewTransactionAndSession(status -> {
				final RegDate dateDebut = date(2008, 5, 1);
				final Entreprise snc = addEntrepriseInconnueAuCivil();
				addRaisonSociale(snc, dateDebut, null, "Ensemble pour aller plus loin");
				addFormeJuridique(snc, dateDebut, null, FormeJuridiqueEntreprise.SNC);
				addRegimeFiscalVD(snc, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(snc, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

				// Contribuable avec un for fiscal principal ouvert à Lausanne
				final PersonnePhysique associe = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
				addForPrincipal(associe, date(1980, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return new ImmutablePair<>(String.valueOf(snc.getNumero()), String.valueOf(associe.getNumero()));
			});

			//mise en place de la relation
			final CSVRecord csvRecordMock = PowerMockito.mock(CSVRecord.class);
			PowerMockito.when(csvRecordMock.get(DonneesLienAssocieEtSNC.SUJET)).thenReturn(pairSncAssocie.getKey());
			PowerMockito.when(csvRecordMock.get(DonneesLienAssocieEtSNC.OBJECT)).thenReturn(pairSncAssocie.getValue());

			final DonneesLienAssocieEtSNC lienAssocieEtSNC = DonneesLienAssocieEtSNC.valueOf(csvRecordMock);
			final LienAssociesSNCEnMasseImporterResults res = processor.run(Collections.singletonList(lienAssocieEtSNC), RegDate.get(), null);

			//vérification de la création du lien
			Assert.assertNotNull(res);
			Assert.assertEquals("une relation associe SNC a bien été établi en base", 1, res.getLiensCrees().size());

			//Vérification du lien créé
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Tiers tiersObjet = tiersService.getTiers(Long.valueOf(pairSncAssocie.getKey()));
					Assert.assertNotNull(tiersObjet.getRapportsSujet());
					final RapportEntreTiers rapportEntreTiers = tiersObjet.getRapportsSujet().iterator().next();
					Assert.assertEquals("le rapport est de type associe SNC", TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC, rapportEntreTiers.getType());
					Assert.assertEquals("le tier sujet est la SNC", Long.valueOf(pairSncAssocie.getKey()), rapportEntreTiers.getSujetId());
					Assert.assertEquals("le tier objet est l'associe", Long.valueOf(pairSncAssocie.getValue()), rapportEntreTiers.getObjetId());
					Assert.assertEquals("La date de début du lien doit être au 01.01.2018", date(2018, 1, 1), rapportEntreTiers.getDateDebut());
				}
			});
		}
		catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}


	@Test
	public void testCreationLienAssocieSNCAvecForOuvertApres2018_OK() throws Exception {

		try {
			final RegDate dateDebut = date(2018, 5, 1);
			// mise en place fiscale
			final Pair<String, String> pairSncAssocie = doInNewTransactionAndSession(status -> {
				final Entreprise snc = addEntrepriseInconnueAuCivil();
				addRaisonSociale(snc, dateDebut, null, "Ensemble pour aller plus loin");
				addFormeJuridique(snc, dateDebut, null, FormeJuridiqueEntreprise.SNC);
				addRegimeFiscalVD(snc, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(snc, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

				// Contribuable avec un for fiscal principal ouvert à Lausanne
				final PersonnePhysique associe = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
				addForPrincipal(associe, date(1980, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return new ImmutablePair<>(String.valueOf(snc.getNumero()), String.valueOf(associe.getNumero()));
			});

			//mise en place de la relation
			final CSVRecord csvRecordMock = PowerMockito.mock(CSVRecord.class);
			PowerMockito.when(csvRecordMock.get(DonneesLienAssocieEtSNC.SUJET)).thenReturn(pairSncAssocie.getKey());
			PowerMockito.when(csvRecordMock.get(DonneesLienAssocieEtSNC.OBJECT)).thenReturn(pairSncAssocie.getValue());

			final DonneesLienAssocieEtSNC lienAssocieEtSNC = DonneesLienAssocieEtSNC.valueOf(csvRecordMock);
			final LienAssociesSNCEnMasseImporterResults res = processor.run(Collections.singletonList(lienAssocieEtSNC), RegDate.get(), null);

			//vérification de la création du lien
			Assert.assertNotNull(res);
			Assert.assertEquals("une relation associe SNC a bien été établi en base", 1, res.getLiensCrees().size());

			//Vérification du lien créé
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Tiers tiersObjet = tiersService.getTiers(Long.valueOf(pairSncAssocie.getKey()));
					Assert.assertNotNull(tiersObjet.getRapportsSujet());
					final RapportEntreTiers rapportEntreTiers = tiersObjet.getRapportsSujet().iterator().next();
					Assert.assertEquals("le rapport est de type associe SNC", TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC, rapportEntreTiers.getType());
					Assert.assertEquals("le tier sujet est la SNC", Long.valueOf(pairSncAssocie.getKey()), rapportEntreTiers.getSujetId());
					Assert.assertEquals("le tier objet est l'associe", Long.valueOf(pairSncAssocie.getValue()), rapportEntreTiers.getObjetId());
					Assert.assertEquals("La date de début du lien doit être celle du 1er For sur 2018", dateDebut, rapportEntreTiers.getDateDebut());
				}
			});
		}
		catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

}
