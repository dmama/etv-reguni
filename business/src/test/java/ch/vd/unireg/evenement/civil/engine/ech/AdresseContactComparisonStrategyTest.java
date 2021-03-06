package ch.vd.unireg.evenement.civil.engine.ech;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.type.TypeAdresseCivil;

public class AdresseContactComparisonStrategyTest extends AbstractAdresseComparisonStrategyTest {

	private AdresseContactComparisonStrategy strategy;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		strategy = new AdresseContactComparisonStrategy();
	}

	private static MockAdresse buildAdresse(TypeAdresseCivil type, String titre, String rue, String numeroPostal, String numero, String localite, @Nullable DateRange range) {
		final MockAdresse adr = new MockAdresse(rue, numero, numeroPostal, localite);
		adr.setTypeAdresse(type);
		adr.setTitre(titre);
		if (range != null) {
			adr.setDateDebutValidite(range.getDateDebut());
			adr.setDateFinValidite(range.getDateFin());
		}
		return adr;
	}

	@Test(timeout = 10000L)
	public void testSansAdresses() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testSansAdressesContact() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, individu -> {
			individu.addAdresse(buildAdresse(TypeAdresseCivil.SECONDAIRE, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			individu.addAdresse(buildAdresse(TypeAdresseCivil.PRINCIPALE, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			individu.addAdresse(buildAdresse(TypeAdresseCivil.TUTEUR, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
		});
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testMemeAdresseCourrierSansRange() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null)), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null)));
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testMemeAdresseCourrierAvecRange() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10)))), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10)))));
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testAutreTitre() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null)), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Chabadabada", "Rue du pont de bois", "1234", "12", "Parlabas", null)));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (titre)");
	}

	@Test(timeout = 10000L)
	public void testAutreRue() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null)), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de fer", "1234", "12", "Parlabas", null)));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (rue)");
	}

	@Test(timeout = 10000L)
	public void testAutreNumeroPostal() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null)), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1235", "12", "Parlabas", null)));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (numéro postal)");
	}

	@Test(timeout = 10000L)
	public void testAutreNumero() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null)), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "24", "Parlabas", null)));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (numéro)");
	}

	@Test(timeout = 10000L)
	public void testAutreLocalite() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null)), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", null)));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (localité)");
	}

	@Test(timeout = 10000L)
	public void testAutreDateDebut() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10)))), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", new DateRangeHelper.Range(date(2001, 1, 1), date(2009, 12, 10)))));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (dates)");
	}

	@Test(timeout = 10000L)
	public void testAutreDateFin() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10)))), noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 9)))));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (dates)");
	}

	@Test(timeout = 10000L)
	public void testApparitionAdresse() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2,
		           individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 9)))));
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (apparition)");
	}

	@Test(timeout = 10000L)
	public void testDisparitionAdresse() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, individu -> individu.addAdresse(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10)))), noEvt2, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact (disparition)");
	}
}
