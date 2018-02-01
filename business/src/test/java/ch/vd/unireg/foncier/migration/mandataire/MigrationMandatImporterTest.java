package ch.vd.unireg.foncier.migration.mandataire;

import java.text.ParseException;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.unireg.interfaces.infra.mock.MockGenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseMandataireSuisse;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeMandat;

public class MigrationMandatImporterTest extends BusinessTest {

	private MigrationMandatImporter importer;

	private DonneesMandat buildMandat(int noCtb, boolean avecCourrier, String formulePolitesse, String nom1, String nom2, String attentionDe, String rue, Integer npa, String localite, String noTelephone) throws ParseException {
		final String line = String.format("%d;;;;%s;%s;%s;%s;%s;%s;%d;%s;%s",
		                                  noCtb,
		                                  avecCourrier ? "Oui" : "Non",
		                                  StringUtils.trimToEmpty(formulePolitesse),
		                                  StringUtils.trimToEmpty(nom1),
		                                  StringUtils.trimToEmpty(nom2),
		                                  StringUtils.trimToEmpty(attentionDe),
		                                  StringUtils.trimToEmpty(rue),
		                                  npa,
		                                  StringUtils.trimToEmpty(localite),
		                                  StringUtils.trimToEmpty(noTelephone));
		return DonneesMandat.valueOf(line);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		this.importer = new MigrationMandatImporter(serviceInfra, transactionManager, hibernateTemplate);
	}

	@Test
	public void testFichierInputVide() {
		final MigrationMandatImporterResults res = importer.importData(Collections.emptyList(), date(2010, 1, 1), MockGenreImpotMandataire.IFONC, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(date(2010, 1,1 ), res.dateDebutMandats);
		Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), res.genreImpot.getCode());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getMandatsCrees().size());
		Assert.assertEquals(0, res.getLignesIgnorees().size());
	}

	@Test
	public void testContribuableInconnu() throws Exception {
		final DonneesMandat mandat = buildMandat(42,
		                                         true,
		                                         "Monsieur",
		                                         "Albert Durant",
		                                         null,
		                                         null,
		                                         "Avenue de la gare 12",
		                                         MockLocalite.Bussigny.getNPA(),
		                                         MockLocalite.Bussigny.getNom(),
		                                         null);
		final MigrationMandatImporterResults res = importer.importData(Collections.singletonList(mandat), date(2010, 1, 1), MockGenreImpotMandataire.IFONC, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(date(2010, 1,1 ), res.dateDebutMandats);
		Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), res.genreImpot.getCode());
		Assert.assertEquals(1, res.getErreurs().size());
		Assert.assertEquals(0, res.getMandatsCrees().size());
		Assert.assertEquals(0, res.getLignesIgnorees().size());

		{
			final MigrationMandatImporterResults.Erreur erreur = res.getErreurs().get(0);
			Assert.assertNotNull(erreur);
			Assert.assertEquals("Contribuable inconnu.", erreur.erreur);
			Assert.assertSame(mandat, erreur.mandat);
		}
	}

	@Test
	public void testContribuableExistantMaisMauvaisType() throws Exception {

		// mise en place fiscale
		final int id = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albertine", "Durant", date(1945, 3, 1), Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(1960, 3, 1), null);
			return couple.getMenage().getNumero().intValue();
		});

		final DonneesMandat mandat = buildMandat(id,
		                                         true,
		                                         "Monsieur",
		                                         "Albert Durant",
		                                         null,
		                                         null,
		                                         "Avenue de la gare 12",
		                                         MockLocalite.Bussigny.getNPA(),
		                                         MockLocalite.Bussigny.getNom(),
		                                         null);
		final MigrationMandatImporterResults res = importer.importData(Collections.singletonList(mandat), date(2010, 1, 1), MockGenreImpotMandataire.IFONC, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(date(2010, 1,1 ), res.dateDebutMandats);
		Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), res.genreImpot.getCode());
		Assert.assertEquals(1, res.getErreurs().size());
		Assert.assertEquals(0, res.getMandatsCrees().size());
		Assert.assertEquals(0, res.getLignesIgnorees().size());

		{
			final MigrationMandatImporterResults.Erreur erreur = res.getErreurs().get(0);
			Assert.assertNotNull(erreur);
			Assert.assertEquals("Le contribuable visé n'est pas d'un type acceptable ici (MenageCommun)", erreur.erreur);
			Assert.assertSame(mandat, erreur.mandat);
		}
	}

	@Test
	public void testCoupleNPALocaliteInconnu() throws Exception {

		// mise en place fiscale
		final int idPP = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albertine", "Durant", date(1945, 3, 1), Sexe.FEMININ);
			return pp.getNumero().intValue();
		});

		final DonneesMandat mandat = buildMandat(idPP,
		                                         true,
		                                         "Madame",
		                                         "Albertine Durant",
		                                         null,
		                                         null,
		                                         "Avenue de la gare 42",
		                                         9999,
		                                         "Malaga",
		                                         null);
		final MigrationMandatImporterResults res = importer.importData(Collections.singletonList(mandat), date(2010, 1, 1), MockGenreImpotMandataire.IFONC, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(date(2010, 1,1 ), res.dateDebutMandats);
		Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), res.genreImpot.getCode());
		Assert.assertEquals(1, res.getErreurs().size());
		Assert.assertEquals(0, res.getMandatsCrees().size());
		Assert.assertEquals(0, res.getLignesIgnorees().size());

		{
			final MigrationMandatImporterResults.Erreur erreur = res.getErreurs().get(0);
			Assert.assertNotNull(erreur);
			Assert.assertEquals("Couple NPA/localité inconnu.", erreur.erreur);
			Assert.assertSame(mandat, erreur.mandat);
		}

		// ... et en base ?
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get((long) idPP);
				Assert.assertNotNull(pp);
				Assert.assertEquals(Collections.emptySet(), pp.getAdressesMandataires());
			}
		});
	}

	@Test
	public void testNouvelleAdresseAvecEnvoiCourrier() throws Exception {
		// mise en place fiscale
		final int idPP = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Félicité", "Duschmol", date(1945, 3, 1), Sexe.FEMININ);
			return pp.getNumero().intValue();
		});

		final DonneesMandat mandat = buildMandat(idPP,
		                                         true,
		                                         "Madame",
		                                         "Albertine Durant",
		                                         "Dufour",
		                                         "Sa maman",
		                                         "Avenue de la gare 42",
		                                         MockLocalite.Vallorbe.getNPA(),
		                                         MockLocalite.Vallorbe.getNom(),
		                                         "021/8748741");
		final MigrationMandatImporterResults res = importer.importData(Collections.singletonList(mandat), date(2011, 1, 1), MockGenreImpotMandataire.IFONC, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(date(2011, 1,1 ), res.dateDebutMandats);
		Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), res.genreImpot.getCode());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getMandatsCrees().size());
		Assert.assertEquals(0, res.getLignesIgnorees().size());

		{
			final DonneesMandat cree = res.getMandatsCrees().get(0);
			Assert.assertSame(mandat, cree);
		}

		// et en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get((long) idPP);
				Assert.assertNotNull(pp);
				final Set<AdresseMandataire> adresses = pp.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				{
					final AdresseMandataire adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals("Madame", adresse.getCivilite());
					Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), adresse.getCodeGenreImpot());
					Assert.assertEquals("Sa maman", adresse.getComplement());
					Assert.assertEquals(date(2011, 1, 1), adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
					Assert.assertEquals("Albertine Durant Dufour", adresse.getNomDestinataire());
					Assert.assertEquals("021/8748741", adresse.getNoTelephoneContact());
					Assert.assertNull(adresse.getNumeroCasePostale());
					Assert.assertNull(adresse.getNumeroMaison());
					Assert.assertNull(adresse.getPersonneContact());
					Assert.assertEquals("Avenue de la gare 42", adresse.getRue());
					Assert.assertNull(adresse.getTexteCasePostale());
					Assert.assertEquals(TypeMandat.SPECIAL, adresse.getTypeMandat());
					Assert.assertTrue(adresse.isPermanente());
					Assert.assertTrue(adresse.isWithCopy());

					Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());
					final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
					Assert.assertNull(adresseSuisse.getNpaCasePostale());
					Assert.assertEquals(MockLocalite.Vallorbe.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertNull(adresseSuisse.getNumeroRue());
				}
			}
		});
	}

	@Test
	public void testNouvelleAdresseSansEnvoiCourrier() throws Exception {
		// mise en place fiscale
		final int idPP = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Félicité", "Duschmol", date(1945, 3, 1), Sexe.FEMININ);
			return pp.getNumero().intValue();
		});

		final DonneesMandat mandat = buildMandat(idPP,
		                                         false,
		                                         "Madame",
		                                         "Albertine Durant",
		                                         "Dufour",
		                                         "Sa maman",
		                                         "Avenue de la gare 42",
		                                         MockLocalite.Vallorbe.getNPA(),
		                                         MockLocalite.Vallorbe.getNom(),
		                                         "021/8748741");
		final MigrationMandatImporterResults res = importer.importData(Collections.singletonList(mandat), date(2011, 1, 1), MockGenreImpotMandataire.IFONC, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(date(2011, 1,1 ), res.dateDebutMandats);
		Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), res.genreImpot.getCode());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getMandatsCrees().size());
		Assert.assertEquals(0, res.getLignesIgnorees().size());

		{
			final DonneesMandat cree = res.getMandatsCrees().get(0);
			Assert.assertSame(mandat, cree);
		}

		// et en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get((long) idPP);
				Assert.assertNotNull(pp);
				final Set<AdresseMandataire> adresses = pp.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				{
					final AdresseMandataire adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals("Madame", adresse.getCivilite());
					Assert.assertEquals(MockGenreImpotMandataire.IFONC.getCode(), adresse.getCodeGenreImpot());
					Assert.assertEquals("Sa maman", adresse.getComplement());
					Assert.assertEquals(date(2011, 1, 1), adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
					Assert.assertEquals("Albertine Durant Dufour", adresse.getNomDestinataire());
					Assert.assertEquals("021/8748741", adresse.getNoTelephoneContact());
					Assert.assertNull(adresse.getNumeroCasePostale());
					Assert.assertNull(adresse.getNumeroMaison());
					Assert.assertNull(adresse.getPersonneContact());
					Assert.assertEquals("Avenue de la gare 42", adresse.getRue());
					Assert.assertNull(adresse.getTexteCasePostale());
					Assert.assertEquals(TypeMandat.SPECIAL, adresse.getTypeMandat());
					Assert.assertTrue(adresse.isPermanente());
					Assert.assertFalse(adresse.isWithCopy());

					Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());
					final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
					Assert.assertNull(adresseSuisse.getNpaCasePostale());
					Assert.assertEquals(MockLocalite.Vallorbe.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertNull(adresseSuisse.getNumeroRue());
				}
			}
		});
	}
}
