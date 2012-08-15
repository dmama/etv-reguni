package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseContactComparisonStrategyTest extends AbstractAdresseComparisonStrategyTest {

	private AdresseContactComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new AdresseContactComparisonStrategy();
	}

	private static Adresse buildAdresse(TypeAdresseCivil type, String titre, String rue, String numeroPostal, String numero, String localite, @Nullable DateRange range) {
		final MockAdresse adr = new MockAdresse(rue, numero, numeroPostal, localite);
		adr.setTypeAdresse(type);
		adr.setTitre(titre);
		if (range != null) {
			adr.setDateDebutValidite(range.getDateDebut());
			adr.setDateFinValidite(range.getDateFin());
		}
		return adr;
	}

	@Test
	public void testSansAdresses() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testSansAdressesContact() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, new AddressBuilder() {
			@Override
			public void buildAdresses(MockIndividu individu) {
				individu.getAdresses().add(buildAdresse(TypeAdresseCivil.SECONDAIRE, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
				individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
				individu.getAdresses().add(buildAdresse(TypeAdresseCivil.TUTEUR, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			}
		});
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testMemeAdresseCourrierSansRange() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           });
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testMemeAdresseCourrierAvecRange() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10))));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10))));
			           }
		           });
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testAutreTitre() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Chabadabada", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testAutreRue() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de fer", "1234", "12", "Parlabas", null));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testAutreNumeroPostal() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1235", "12", "Parlabas", null));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testAutreNumero() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "24", "Parlabas", null));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testAutreLocalite() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", null));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testAutreDateDebut() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10))));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", new DateRangeHelper.Range(date(2001, 1, 1), date(2009, 12, 10))));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testAutreDateFin() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10))));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 9))));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testApparitionAdresse() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Paricy", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 9))));
			           }
		           });
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}

	@Test
	public void testDisparitionAdresse() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "Monsieur Tartempion", "Rue du pont de bois", "1234", "12", "Parlabas", new DateRangeHelper.Range(date(2000, 1, 1), date(2009, 12, 10))));
			           }
		           }, noEvt2, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "adresse de contact");
	}
}
