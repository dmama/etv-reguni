package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class AdresseResidencePrincipaleComparisonStrategyTest extends BusinessTest {

	private AdresseResidencePrincipaleComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new AdresseResidencePrincipaleComparisonStrategy(serviceInfra);
	}

	private static interface AddressBuilder {
		void buildAdresses(MockIndividu individu);
	}

	private static Adresse buildAdresse(TypeAdresseCivil type, String numero, Integer egid, DateRange range, Localisation localisationPrecedente, Localisation localisationSuivante) {
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

	private void setupCivil(final long noIndividu, final long noEvt1, final AddressBuilder b1, final long noEvt2, final AddressBuilder b2) {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Fraise", "Tartala", false);
				if (b1 != null) {
					b1.buildAdresses(ind);
				}
				addIndividuFromEvent(noEvt1, ind, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu indCorrige = createIndividu(noIndividu, null, "Frèze", "Tart'ala", false);
				if (b2 != null) {
					b2.buildAdresses(indCorrige);
				}
				addIndividuFromEvent(noEvt2, indCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt1);
			}
		});
	}

	@Test
	public void testSansAdresse() throws Exception {

		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, null);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertTrue(sans);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testSansAdresseResidencePrincipale() throws Exception {

		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			@Override
			public void buildAdresses(MockIndividu individu) {
				individu.getAdresses().add(buildAdresse(TypeAdresseCivil.COURRIER, "12", 123, null, null, null));
				individu.getAdresses().add(buildAdresse(TypeAdresseCivil.TUTEUR, "12", 123, null, null, null));
				individu.getAdresses().add(buildAdresse(TypeAdresseCivil.SECONDAIRE, "12", 123, null, null, null));
			}
		}, noEvt2, null);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertTrue(sans);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testMemeAdressePrincipale() throws Exception {
		final long noIndividu = 1236745674L;
		final long noEvt1 = 3278456435L;
		final long noEvt2 = 43757536526L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 4, 1), date(2005, 1, 19));
		final Localisation precedente = new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null);
		final Localisation suivante = new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFSEtendu(), null);

		setupCivil(noIndividu, noEvt1, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", 123, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", 123, range, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertTrue(sans);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testMemesAdressesPrincipales() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", 123, range1, precedente1, suivante1));
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", 123, range2, precedente2, suivante2));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", 123, range1, precedente1, suivante1));
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", 123, range2, precedente2, suivante2));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertTrue(sans);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testMemesAdressesPrincipalesMelangees() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", 123, range1, precedente1, suivante1));
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", 123, range2, precedente2, suivante2));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", 123, range2, precedente2, suivante2));
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", 123, range1, precedente1, suivante1));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertTrue(sans);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testAdressePrincipaleEgid() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid1, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid2, range, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleEgidSansChangementDeCommune() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid1, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid2, range, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertTrue(sans);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testAdressePrincipaleApparitionEgid() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", null, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleDisparitionEgid() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", null, range, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleDateDebut() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range1, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range2, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleDateFin() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range1, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range2, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleLocalisationPrecedenteOfs() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente1, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente2, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleLocalisationSuivanteOfs() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente, suivante1));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente, suivante2));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleLocalisationPrecedenteType() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente1, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente2, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleLocalisationSuivanteType() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente, suivante1));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente, suivante2));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleApparitionLocalisationPrecedente() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, null, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleApparitionLocalisationSuivante() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente, null));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleDisparitionLocalisationPrecedente() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, null, suivante));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
	}

	@Test
	public void testAdressePrincipaleDisparitionLocalisationSuivante() throws Exception {
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
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "12", egid, range, precedente, suivante));
			           }
		           }, noEvt2, new AddressBuilder() {
			           @Override
			           public void buildAdresses(MockIndividu individu) {
				           individu.getAdresses().add(buildAdresse(TypeAdresseCivil.PRINCIPALE, "13", egid, range, precedente, null));
			           }
		           }
		);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertFalse(sans);
		Assert.assertEquals("adresse de résidence principale", dh.get());
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

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean sans = strategy.sansDifferenceFiscalementImportante(iae1, iae2, dh);
		Assert.assertTrue(sans);
		Assert.assertNull(dh.get());
	}
}
