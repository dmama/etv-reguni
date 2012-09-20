package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public abstract class AbstractAdresseResidenceComparisonStrategyTest extends AbstractAdresseComparisonStrategyTest {

	protected abstract TypeAdresseCivil getTypeAdresseResidence();

	protected abstract AdresseResidenceComparisonStrategy buildStrategy();

	protected abstract String getNomAttribut();

	private AdresseResidenceComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = buildStrategy();
	}

	protected static Adresse buildAdresse(TypeAdresseCivil type, String numero, Integer egid, DateRange range, Localisation localisationPrecedente, Localisation localisationSuivante) {
		final MockAdresse adr = new MockAdresse("Rue du bignou", numero, "1096", "Villette");
		adr.setTypeAdresse(type);
		adr.setEgid(egid);
		if (range != null) {
			adr.setDateDebutValidite(range.getDateDebut());
			adr.setDateFinValidite(range.getDateFin());
		}
		adr.setLocalisationPrecedente(localisationPrecedente);
		adr.setLocalisationSuivante(localisationSuivante);
		return adr;
	}

	@Test
	public void testSansAdresse() throws Exception {

		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testSansAdresseResidenceInteressante() throws Exception {

		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		final TypeAdresseCivil typeInteressant = getTypeAdresseResidence();
		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			@Override
			public void buildAdresses(MockIndividu individu) {
				for (TypeAdresseCivil type : TypeAdresseCivil.values()) {
					if (type != typeInteressant) {
						individu.getAdresses().add(buildAdresse(type, "12", 123, null, null, null));
					}
				}
			}
		}, noEvt2, null);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testApparitionAdresse() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, null, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", 123, range, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testDisparitionAdresse() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			@Override
			public void buildAdresses(MockIndividu individu) {
				individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", 123, range, precedente, suivante));
			}
		}, noEvt2, null);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testMemeAdresse() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", 123, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", 123, range, precedente, suivante));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testMemesAdresses() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final DateRange range2 = new DateRangeHelper.Range(date(2005, 1, 20), date(2011, 5, 4));
		final Localisation precedente1 = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante1 = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);
		final Localisation precedente2 = new Localisation(LocalisationType.CANTON_VD, MockCommune.ChateauDoex.getNoOFSEtendu(), null);
		final Localisation suivante2 = null;    // on ne quitte pas la commune ici

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", 123, range1, precedente1, suivante1));
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", 123, range2, precedente2, suivante2));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", 123, range1, precedente1, suivante1));
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", 123, range2, precedente2, suivante2));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testMemesAdressesMelangees() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final DateRange range2 = new DateRangeHelper.Range(date(2005, 1, 20), date(2011, 5, 4));
		final Localisation precedente1 = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante1 = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);
		final Localisation precedente2 = new Localisation(LocalisationType.CANTON_VD, MockCommune.ChateauDoex.getNoOFSEtendu(), null);
		final Localisation suivante2 = null;    // on ne quitte pas la commune ici

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", 123, range1, precedente1, suivante1));
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", 123, range2, precedente2, suivante2));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", 123, range2, precedente2, suivante2));
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", 123, range1, precedente1, suivante1));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testEgid() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid1 = MockBatiment.Villette.BatimentCheminDeCreuxBechet.getEgid();
		final int egid2 = 1;
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid1, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid2, range, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testEgidSansChangementDeCommune() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid1 = MockBatiment.Villette.BatimentCheminDeCreuxBechet.getEgid();
		final int egid2 = MockBatiment.Villette.BatimentCheminDesGranges.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid1, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid2, range, precedente, suivante));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testApparitionEgid() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", null, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testDisparitionEgid() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", null, range, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testDateDebut() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 4, 12), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range1, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range2, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testDateFin() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 2, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range1, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range2, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testLocalisationPrecedenteOfsHS() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente1 = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation precedente2 = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Albanie.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente1, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente2, suivante));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testLocalisationPrecedenteOfsHC() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente1 = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null);
		final Localisation precedente2 = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bale.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente1, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente2, suivante));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testLocalisationPrecedenteOfsVD() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente1 = new Localisation(LocalisationType.CANTON_VD, MockCommune.Echallens.getNoOFS(), null);
		final Localisation precedente2 = new Localisation(LocalisationType.CANTON_VD, MockCommune.CheseauxSurLausanne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente1, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente2, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testLocalisationSuivanteOfsHS() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante1 = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Albanie.getNoOFS(), null);
		final Localisation suivante2 = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Colombie.getNoOFS(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, suivante1));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, suivante2));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testLocalisationSuivanteOfsHC() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante1 = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bale.getNoOFSEtendu(), null);
		final Localisation suivante2 = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, suivante1));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, suivante2));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testLocalisationSuivanteOfsVD() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante1 = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);
		final Localisation suivante2 = new Localisation(LocalisationType.CANTON_VD, MockCommune.Bex.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, suivante1));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, suivante2));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testLocalisationPrecedenteType() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente1 = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation precedente2 = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFSEtendu(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente1, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente2, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testLocalisationSuivanteType() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante1 = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);
		final Localisation suivante2 = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, suivante1));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, suivante2));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testApparitionLocalisationPrecedente() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFSEtendu(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, null, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testApparitionLocalisationSuivante() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFSEtendu(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testDisparitionLocalisationPrecedente() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFSEtendu(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, null, suivante));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testDisparitionLocalisationSuivante() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final int egid = MockBatiment.Villette.BatimentRouteDeLausanne.getEgid();
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFSEtendu(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "12", egid, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(getTypeAdresseResidence(), "13", egid, range, precedente, null));
			           }
		           }
		);

		assertNonNeutre(strategy, noEvt1, noEvt2, getNomAttribut());
	}

	@Test
	public void testMelangeAdressesSecondaireEtPrincipale() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final DateRange range2 = new DateRangeHelper.Range(date(2001, 4, 1), date(2006, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", 123, range1, precedente, null));
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.SECONDAIRE, "12", 12345, range2, null, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.SECONDAIRE, "13", 12345, range2, null, suivante));
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", 123, range1, precedente, null));
			           }
		           }
		);

		assertNeutre(strategy, noEvt1, noEvt2);
	}
}
