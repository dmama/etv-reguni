package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockNationalite;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.EvenementFiscalFinAutoriteParentale;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServiceCivilImpl;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.StatutMenageCommun;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.ValidationService;

import static ch.vd.unireg.interfaces.InterfacesTestHelper.newLocalisation;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class TiersServiceTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(TiersServiceTest.class);

	private static final long NUMERO_INDIVIDU = 12345L;
	private TiersService tiersService;
	private AdresseService adresseService;
	private TiersDAO tiersDAO;
	private ForFiscalDAO forFiscalDAO;
	private RapportEntreTiersDAO rapportEntreTiersDAO;
	private ValidationService validationService;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		forFiscalDAO = getBean(ForFiscalDAO.class, "forFiscalDAO");
		rapportEntreTiersDAO = getBean(RapportEntreTiersDAO.class, "rapportEntreTiersDAO");
		validationService = getBean(ValidationService.class, "validationService");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		adresseService = getBean(AdresseService.class, "adresseService");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInsertTiers() {

		serviceCivil.setUp(new DefaultMockServiceCivil());
		LOGGER.debug("Début de testInsertTiers");
		tiersService = getBean(TiersService.class, "tiersService");

		PersonnePhysique tiers = new PersonnePhysique(true);
		tiers.setNumeroIndividu(54321L);
		Tiers tiersSaved = tiersDAO.save(tiers);
		assertNotNull(tiersSaved);
		LOGGER.debug("Tiers saved:" + tiersSaved.getNumero());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInsertContribuable() {

		serviceCivil.setUp(new DefaultMockServiceCivil());
		PersonnePhysique tiers = new PersonnePhysique(false);
		tiers.setNom("Bla");

		AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(RegDate.get());
		adresse.setNumeroOrdrePoste(528);
		adresse.setRue("Av machin");
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		tiers.addAdresseTiers(adresse);

		Tiers tiersSaved = tiersDAO.save(tiers);
		assertNotNull(tiersSaved);
		LOGGER.debug("Tiers saved:" + tiersSaved.getNumero());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInsertAndUpdateTiers() {

		serviceCivil.setUp(new DefaultMockServiceCivil());
		LOGGER.debug("Début de testInsertTiers");
		Long noIndividu = 54321L;

		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			tiers.setNumeroIndividu(noIndividu);
			tiers = (PersonnePhysique) tiersDAO.save(tiers);

			ForFiscalPrincipal premierForFiscal = new ForFiscalPrincipal();
			premierForFiscal.setAnnule(false);
			premierForFiscal.setDateFin(null);
			premierForFiscal.setDateDebut(RegDate.get(2008, 1, 1));
			premierForFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			premierForFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			premierForFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			premierForFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			premierForFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
			premierForFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			tiers.addForFiscal(premierForFiscal);
		}

		{
			PersonnePhysique oldTiers = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			ForFiscalPrincipal forFiscalPrincipal = oldTiers.getForFiscalPrincipalAt(null);
			forFiscalPrincipal.setDateFin(RegDate.get().getOneDayBefore());
			forFiscalPrincipal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);

			ForFiscalPrincipal nouveauForFiscal = new ForFiscalPrincipal();
			nouveauForFiscal.setAnnule(false);
			nouveauForFiscal.setDateFin(null);
			nouveauForFiscal.setDateDebut(RegDate.get());
			nouveauForFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			nouveauForFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Bex.getNoOFS());
			nouveauForFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			nouveauForFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			nouveauForFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			nouveauForFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			oldTiers.addForFiscal(nouveauForFiscal);
		}

		{
			Tiers tiersReloaded = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertNotNull(tiersReloaded);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_AvecNationaliteSuisse() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1953, 11, 2), null);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_AvecNationaliteEtrangere_PermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.France, RegDate.get(1953, 11, 2), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1953, 11, 2), null, false);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_AvecNationaliteEtrangere_PermisCAnnule() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.France, RegDate.get(1953, 11, 2), null);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1953, 11, 2), null, true);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_AvecNationaliteEtrangere_PermisNonC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.France, RegDate.get(1953, 11, 2), null);
				addPermis(pierre, TypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, false);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué comme etranger sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineSuisse() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, TypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, false);
				addOrigine(pierre, MockCommune.Cossonay);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("L'origine (texte libre) ne devrait pas être utilisée pour déterminer la nationalité de Pierre en l'absence de nationalité", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineRenseigne_SansPermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, TypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, false);
				addOrigine(pierre, MockPays.France.getNomCourt());
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué comme etranger sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineRenseigne_AvecPermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1953, 11, 2), null, false);
				addOrigine(pierre, MockPays.France.getNomCourt());
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineNonRenseigne_AvecPermisC() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1953, 11, 2), null, false);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetIndividu() {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1953, 11, 2), null, false);
			}
		});

		{
			PersonnePhysique hab = new PersonnePhysique(true);
			hab.setNumeroIndividu(NUMERO_INDIVIDU);
			Individu ind = tiersService.getIndividu(hab);
			assertNotNull(ind);
		}

		try {
			//noinspection ConstantConditions
			tiersService.getIndividu(null);
			fail();
		}
		catch (Exception e) {
			// OK
		}

		{
			PersonnePhysique hab = new PersonnePhysique(true);
			hab.setNumeroIndividu(1L);
			Individu ind = tiersService.getIndividu(hab);
			assertNull(ind);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetIndividuParAnnee() {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1953, 11, 2), date(1979, 12, 31));
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeLaGare, null, date(1980, 1, 1), null);
				addPermis(pierre, TypePermis.FRONTALIER, RegDate.get(1953, 11, 2), RegDate.get(1979, 12, 31), false);
				addPermis(pierre, TypePermis.ETABLISSEMENT, RegDate.get(1980, 1, 1), null, false);
			}
		});

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(NUMERO_INDIVIDU);

		{
			// Vue de l'habitant pour 1960
			final Individu ind = tiersService.getIndividu(hab, date(1960, 12, 31), AttributeIndividu.ADRESSES);
			assertNotNull(ind);
			assertEquals("Av de Beaulieu", ind.getAdresses().iterator().next().getRue());
		}

		{
			// Vue de l'habitant pour 2000
			final Individu ind = tiersService.getIndividu(hab, date(2000, 12, 31), AttributeIndividu.ADRESSES);
			assertNotNull(ind);

			final Collection<Adresse> adresses = ind.getAdresses();
			assertEquals(2, adresses.size());

			final Iterator<Adresse> iter = adresses.iterator();
			assertEquals("Av de Beaulieu", iter.next().getRue());
			assertEquals("Avenue de la Gare", iter.next().getRue());
		}

		{
			PersonnePhysique h = new PersonnePhysique(true);
			h.setNumeroIndividu(null);
			Individu ind = tiersService.getIndividu(h);
			assertNull(ind);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineNonRenseigne_SansPermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, TypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, false);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué comme étranger sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineNonRenseigne_SansAucunPermis() {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		try {
			tiersService.isEtrangerSansPermisC(habitant, null);
			fail("une erreur aurait du se produire : impossible de determiner la nationalité de Pierre");
		}
		catch (Exception e) {
			assertTrue("Message d'erreur incorrect", e.getMessage().equals(
					"Impossible de déterminer la nationalité de l'individu " + NUMERO_INDIVIDU));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPrincipal() {

		final long numeroPierre = 1;
		final long numeroPaul = 2;
		final long numeroLisette = 3;
		final long numeroGudrun = 4;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(numeroPierre, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addIndividu(numeroPaul, RegDate.get(1953, 10, 23), "Etoile", "Paul", true);
				addIndividu(numeroLisette, RegDate.get(1963, 1, 24), "Lafrite", "Lisette", false);
				addIndividu(numeroGudrun, RegDate.get(1963, 4, 20), "Schnitzel", "Gudrun", false);
			}
		});

		PersonnePhysique pierre = new PersonnePhysique(true);
		pierre.setNumeroIndividu(numeroPierre);
		PersonnePhysique paul = new PersonnePhysique(true);
		paul.setNumeroIndividu(numeroPaul);
		PersonnePhysique lisette = new PersonnePhysique(true);
		lisette.setNumeroIndividu(numeroLisette);
		PersonnePhysique gudrun = new PersonnePhysique(true);
		gudrun.setNumeroIndividu(numeroGudrun);

		PersonnePhysique alf = new PersonnePhysique(false);
		alf.setPrenom("Alf");
		alf.setNom("Alf");
		alf.setSexe(null);

		PersonnePhysique esc = new PersonnePhysique(false);
		esc.setPrenom("Escar");
		esc.setNom("Got");
		esc.setSexe(null);

		// aucune personne
		assertNull(tiersService.getPrincipal(null, null));

		// une seule personne (homme ou femme)
		assertEquals(pierre, tiersService.getPrincipal(pierre, null));
		assertEquals(pierre, tiersService.getPrincipal(null, pierre));
		assertEquals(lisette, tiersService.getPrincipal(lisette, null));
		assertEquals(lisette, tiersService.getPrincipal(null, lisette));

		// couple
		assertEquals(pierre, tiersService.getPrincipal(pierre, lisette));
		assertEquals(pierre, tiersService.getPrincipal(lisette, pierre));
		assertEquals(paul, tiersService.getPrincipal(paul, gudrun));
		assertEquals(paul, tiersService.getPrincipal(gudrun, paul));

		// pacs
		assertEquals(lisette, tiersService.getPrincipal(lisette, gudrun));
		assertEquals(lisette, tiersService.getPrincipal(gudrun, lisette));
		assertEquals(pierre, tiersService.getPrincipal(pierre, paul));
		assertEquals(pierre, tiersService.getPrincipal(paul, pierre));

		// un des deux sexe inconnu
		assertEquals(alf, tiersService.getPrincipal(alf, lisette));
		assertEquals(alf, tiersService.getPrincipal(lisette, alf));
		assertEquals(pierre, tiersService.getPrincipal(alf, pierre));
		assertEquals(pierre, tiersService.getPrincipal(pierre, alf));

		// les deux sexe inconnus
		assertEquals(alf, tiersService.getPrincipal(alf, esc));
		assertEquals(alf, tiersService.getPrincipal(esc, alf));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateEnsembleTiersCouple() {

		final String nomPierre = "Pierre";
		final String nomPaul = "Paul";
		final String nomLisette = "Lisette";
		final String nomGudrun = "Gudrun";

		final PersonnePhysique pierre = new PersonnePhysique(false);
		pierre.setNom(nomPierre);
		pierre.setSexe(Sexe.MASCULIN);
		final PersonnePhysique paul = new PersonnePhysique(false);
		paul.setNom(nomPaul);
		paul.setSexe(Sexe.MASCULIN);
		final PersonnePhysique lisette = new PersonnePhysique(false);
		lisette.setNom(nomLisette);
		lisette.setSexe(Sexe.FEMININ);
		final PersonnePhysique gudrun = new PersonnePhysique(false);
		gudrun.setNom(nomGudrun);
		gudrun.setSexe(Sexe.FEMININ);

		// Mariés
		{
			EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(paul, gudrun, RegDate.get(2000, 1, 1), null);
			assertNotNull(ensemble);

			final PersonnePhysique principal = ensemble.getPrincipal();
			assertNotNull(principal);
			assertEquals(nomPaul, principal.getNom());

			final PersonnePhysique second = ensemble.getConjoint();
			assertNotNull(second);
			assertEquals(nomGudrun, second.getNom());

		}

		// Mariée seul
		{
			EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(lisette, null, RegDate.get(2000, 1, 1), null);
			assertNotNull(ensemble);

			final PersonnePhysique principal = ensemble.getPrincipal();
			assertNotNull(principal);
			assertEquals(nomLisette, principal.getNom());

			assertNull(ensemble.getConjoint());
		}

		// Pacsés
		{
			EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(pierre, paul, RegDate.get(2000, 1, 1), null);
			assertNotNull(ensemble);

			final PersonnePhysique principal = ensemble.getPrincipal();
			assertNotNull(principal);
			assertEquals(nomPaul, principal.getNom());

			final PersonnePhysique second = ensemble.getConjoint();
			assertNotNull(second);
			assertEquals(nomPierre, second.getNom());

		}

		// Pacsé seul
		{
			EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(pierre, null, RegDate.get(2000, 1, 1), null);
			assertNotNull(ensemble);

			final PersonnePhysique principal = ensemble.getPrincipal();
			assertNotNull(principal);
			assertEquals(nomPierre, principal.getNom());

			assertNull(ensemble.getConjoint());
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOpenForFiscalPrincipal() throws Exception {

		final RegDate dateOuverture = RegDate.get(1990, 7, 1);

		serviceCivil.setUp(new DefaultMockServiceCivil());

		final Set<ForFiscal> forsFiscaux = new HashSet<>();
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(54321L);
		habitant.setForsFiscaux(forsFiscaux);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);
		assertEquals(0, habitant.getForsFiscaux().size());

		tiersService.openForFiscalPrincipal(habitant, dateOuverture, MotifRattachement.DOMICILE, MockCommune.Cossonay.getNoOFS(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HC);
		assertEquals(1, habitant.getForsFiscaux().size());

		ForFiscalPrincipal ff = (ForFiscalPrincipal) habitant.getForsFiscaux().toArray()[0];
		assertEquals(new Integer(MockCommune.Cossonay.getNoOFS()), ff.getNumeroOfsAutoriteFiscale());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
		assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());
		assertEquals(MotifRattachement.DOMICILE, ff.getMotifRattachement());
		assertEquals(dateOuverture, ff.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, ff.getModeImposition());
	}

	private AppartenanceMenage buildAppartenanceMenage(MenageCommun mc, PersonnePhysique pp, RegDate dateDebut, @Nullable RegDate dateFin, boolean isAnnule) {
		final AppartenanceMenage am = new AppartenanceMenage(dateDebut, dateFin, pp, mc);
		am.setAnnule(isAnnule);
		return (AppartenanceMenage) rapportEntreTiersDAO.save(am);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCloseAppartenanceMenageAvecRapportAnnule() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());

		PersonnePhysique momo = new PersonnePhysique(true);
		momo.setNumeroIndividu(54321L);
		momo = (PersonnePhysique) tiersDAO.save(momo);

		PersonnePhysique bea = new PersonnePhysique(true);
		bea.setNumeroIndividu(23456L);
		bea = (PersonnePhysique) tiersDAO.save(bea);

		MenageCommun mc = new MenageCommun();
		mc = (MenageCommun) tiersDAO.save(mc);

		final RegDate debutMariage = RegDate.get(2007, 1, 1);
		final RegDate finMariage = RegDate.get(2009, 6, 12);

		final Set<RapportEntreTiers> rapports = new HashSet<>();
		rapports.add(buildAppartenanceMenage(mc, momo, debutMariage, null, true));
		rapports.add(buildAppartenanceMenage(mc, bea, debutMariage, null, true));
		rapports.add(buildAppartenanceMenage(mc, momo, debutMariage, null, false));
		rapports.add(buildAppartenanceMenage(mc, bea, debutMariage, null, false));
		rapports.add(buildAppartenanceMenage(mc, momo, debutMariage, null, true));
		rapports.add(buildAppartenanceMenage(mc, bea, debutMariage, null, true));
		mc.setRapportsObjet(rapports);

		// maintenant, on ferme les rapports d'appartenance ménage
		tiersService.closeAppartenanceMenage(momo, mc, finMariage);
		tiersService.closeAppartenanceMenage(bea, mc, finMariage);

		assertEquals(rapports.size(), mc.getRapportsObjet().size());
		for (RapportEntreTiers ret : mc.getRapportsObjet()) {
			assertTrue("Rapport entre tiers non-annulé avec mauvaise date de fin : " + ret.getDateFin(), ret.isAnnule() || finMariage.equals(ret.getDateFin()));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetHabitant() throws Exception {

		final long noNouveauHabitant = 3324;
		final long noHabitantNonExistant = 77276;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noNouveauHabitant, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addIndividu(noHabitantNonExistant, RegDate.get(1952, 1, 23), "Meylan", "Jean", true);
			}
		});

		/*
		 * Base vide
		 */
		{
			assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noHabitantNonExistant));
		}

		/*
		 * Base remplie
		 */
		{
			// préparation
			PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noHabitantNonExistant);
			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			assertEquals(habitant, tiersService.getPersonnePhysiqueByNumeroIndividu(noHabitantNonExistant));

			// test
			PersonnePhysique h = tiersService.getPersonnePhysiqueByNumeroIndividu(noHabitantNonExistant);
			assertNotNull(h);
			assertEquals(new Long(noHabitantNonExistant), h.getNumeroIndividu());
			assertEquals(h, habitant);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetByNoIndividuSurContribuableAnnule() throws Exception {

		final long noIndividu = 3244521L;

		// registre civil
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get(1963, 4, 12), "Dupont", "Albert", true);
			}
		});

		class Ids {
			public Long noCtbPourAnnulation = null;
			public Long noCtbAutre = null;
			public Long noCtbDoublonNonAnnule = null;
		}

		// création d'un tiers sur cet individu
		final Ids ids = new Ids();
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.noCtbPourAnnulation = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbPourAnnulation);

		// premier essai, le "get par numéro d'individu" doit fonctionner
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals(ids.noCtbPourAnnulation, pp.getNumero());

				// et on l'annule maintenant
				pp.setAnnule(true);
				return null;
			}
		});

		// deuxième essai, maintenant que le tiers a été annulé, le "get par numéro d'individu" ne doit plus fonctionner
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNull(pp);
				return null;
			}
		});

		// création d'un deuxième tiers avec le même numéro d'individu -> il devait alors sortir du "get par numéro d'individu"
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.noCtbAutre = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbAutre);
		assertTrue(ids.noCtbAutre.longValue() != ids.noCtbPourAnnulation.longValue());

		// le "get par numéro d'individu" doit maintenant retourner le deuxième tiers
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals(ids.noCtbAutre, pp.getNumero());

				// rajoutons encore un tiers avec ce même numéro d'individu...
				ids.noCtbDoublonNonAnnule = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbDoublonNonAnnule);
		assertTrue(ids.noCtbAutre.longValue() != ids.noCtbDoublonNonAnnule.longValue());
		assertTrue(ids.noCtbDoublonNonAnnule.longValue() != ids.noCtbPourAnnulation.longValue());

		// le "get par numéro d'individu" doit maintenant exploser avec une exception bien précise...
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				try {
					tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
					fail();
				}
				catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
					final long[] noCtbDoublons = new long[]{ids.noCtbAutre, ids.noCtbDoublonNonAnnule};
					final String msg = String.format("Plusieurs tiers non-annulés partagent le même numéro d'individu %d (%s)", noIndividu, Arrays.toString(noCtbDoublons));
					assertEquals(msg, e.getMessage());
				}
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetByNumeroIndividuSurContribuableDesactive() throws Exception {

		final long noIndividu = 3244521L;

		// registre civil
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get(1963, 4, 12), "Dupont", "Albert", true);
			}
		});

		class Ids {
			public Long noCtbPourDesactivation = null;
			public Long noCtbAutre = null;
			public Long noCtbDoublonNonAnnule = null;
		}

		// création d'un tiers sur cet individu
		final Ids ids = new Ids();
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique habitant = addHabitant(noIndividu);
				addForPrincipal(habitant, date(2001, 9, 11), MotifFor.ARRIVEE_HS, MockCommune.Renens);

				ids.noCtbPourDesactivation = habitant.getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbPourDesactivation);

		// premier essai, le "get par numéro d'individu" doit fonctionner
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals(ids.noCtbPourDesactivation, pp.getNumero());

				// et on le désactive maintenant
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				ffp.setMotifFermeture(MotifFor.ANNULATION);
				ffp.setDateFin(date(2009, 9, 12));
				return null;
			}
		});

		// deuxième essai, maintenant que le tiers a été désactivé, le "get par numéro d'individu" ne doit plus fonctionner
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNull(pp);
				return null;
			}
		});

		// création d'un deuxième tiers avec le même numéro d'individu -> il devait alors sortir du "get par numéro d'individu"
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique habitant = addHabitant(noIndividu);
				addForPrincipal(habitant, date(2001, 9, 11), MotifFor.ARRIVEE_HS, MockCommune.Echallens);

				ids.noCtbAutre = habitant.getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbAutre);
		assertTrue(ids.noCtbAutre.longValue() != ids.noCtbPourDesactivation.longValue());

		// le "get par numéro d'individu" doit maintenant retourner le deuxième tiers
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals(ids.noCtbAutre, pp.getNumero());

				// rajoutons encore un tiers avec ce même numéro d'individu...
				ids.noCtbDoublonNonAnnule = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbDoublonNonAnnule);
		assertTrue(ids.noCtbAutre.longValue() != ids.noCtbDoublonNonAnnule.longValue());
		assertTrue(ids.noCtbDoublonNonAnnule.longValue() != ids.noCtbPourDesactivation.longValue());

		// le "get par numéro d'individu" doit maintenant exploser avec une exception bien précise...
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				try {
					tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
					fail();
				}
				catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
					final long[] noCtbDoublons = new long[]{ids.noCtbAutre, ids.noCtbDoublonNonAnnule};
					final String msg = String.format("Plusieurs tiers non-annulés partagent le même numéro d'individu %d (%s)", noIndividu, Arrays.toString(noCtbDoublons));
					assertEquals(msg, e.getMessage());
				}
				return null;
			}
		});

		// si on annule maintenant un des deux, le "get par numéro individu" devrait fonctionner à nouveau
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.noCtbAutre);
				assertNotNull(pp);
				pp.setAnnule(true);
				return null;
			}
		});

		// le "get par numéro d'individu" doit maintenant retourner le deuxième tiers
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals(ids.noCtbDoublonNonAnnule, pp.getNumero());

				// réactivons maintenant le tout premier tiers désactivé
				final PersonnePhysique reactive = (PersonnePhysique) tiersService.getTiers(ids.noCtbPourDesactivation);
				assertNotNull(reactive);

				addForPrincipal(reactive, date(2010, 3, 24), MotifFor.REACTIVATION, MockCommune.Bussigny);
				return null;
			}
		});

		// le "get par numéro d'individu" doit maintenant exploser avec une exception bien précise...
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				try {
					tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
					fail();
				}
				catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
					final long[] noCtbDoublons = new long[]{ids.noCtbPourDesactivation, ids.noCtbDoublonNonAnnule};
					final String msg = String.format("Plusieurs tiers non-annulés partagent le même numéro d'individu %d (%s)", noIndividu, Arrays.toString(noCtbDoublons));
					assertEquals(msg, e.getMessage());
				}
				return null;
			}
		});

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEnsembleTiersCouple() throws Exception {

		final long NO_PIERRE = 1;
		final long NO_MOMO = 2;
		final long NO_ENGUERRAND = 3;
		final long NO_ARNOLD = 4;
		final long NO_GUDRUN = 5;
		final RegDate avantMariage = RegDate.get(1974, 1, 1);
		final RegDate dateMariage = RegDate.get(1976, 5, 3);
		final RegDate apresMariage = RegDate.get(1978, 1, 1);

		/*
		 * Mise en place des données
		 */

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne seul
				addIndividu(NO_PIERRE, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// mariée seul
				MockIndividu momo = addIndividu(NO_MOMO, RegDate.get(1953, 11, 2), "Dupneu", "Monique", false);
				marieIndividu(momo, dateMariage);

				// marié seul
				MockIndividu enguerrand = addIndividu(NO_ENGUERRAND, RegDate.get(1953, 11, 2), "Dumaillet", "Enguerrand", false);
				marieIndividu(enguerrand, dateMariage);

				// couple complet
				MockIndividu arnold = addIndividu(NO_ARNOLD, RegDate.get(1953, 11, 2), "Dubug", "Arnold", true);
				MockIndividu gudrun = addIndividu(NO_GUDRUN, RegDate.get(1953, 11, 2), "Dubug", "Gudrun", false);
				marieIndividus(arnold, gudrun, dateMariage);
			}
		});

		final class Numeros {
			long NO_CTB_PIERRE;
			long NO_CTB_MOMO;
			long NO_CTB_ENGUERRAND;
			long NO_CTB_ARNOLD;
			long NO_CTB_GUDRUN;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				{
					// Personne seule
					PersonnePhysique pierre = new PersonnePhysique(true);
					pierre.setNumeroIndividu(NO_PIERRE);
					pierre = (PersonnePhysique) tiersDAO.save(pierre);
					numeros.NO_CTB_PIERRE = pierre.getNumero();
				}

				{
					// Mariée seule
					MenageCommun menage = new MenageCommun();
					PersonnePhysique momo = new PersonnePhysique(true);
					momo.setNumeroIndividu(NO_MOMO);
					RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, momo, dateMariage, null);
					numeros.NO_CTB_MOMO = rapport.getSujetId();
				}

				{
					// Marié seul
					MenageCommun menage = new MenageCommun();
					PersonnePhysique enguerrand = new PersonnePhysique(true);
					enguerrand.setNumeroIndividu(NO_ENGUERRAND);
					RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, enguerrand, dateMariage, null);
					numeros.NO_CTB_ENGUERRAND = rapport.getSujetId();
				}

				{
					// Couple complet
					MenageCommun menage = new MenageCommun();

					PersonnePhysique arnold = new PersonnePhysique(true);
					arnold.setNumeroIndividu(NO_ARNOLD);
					RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, arnold, dateMariage, null);
					numeros.NO_CTB_ARNOLD = rapport.getSujetId();
					menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());

					PersonnePhysique gudrun = new PersonnePhysique(true);
					gudrun.setNumeroIndividu(NO_GUDRUN);
					rapport = tiersService.addTiersToCouple(menage, gudrun, dateMariage, null);
					numeros.NO_CTB_GUDRUN = rapport.getSujetId();
				}
				return null;
			}
		});

		{
			{
				assertNull(tiersService.getEnsembleTiersCouple((MenageCommun) null, avantMariage));
				assertNull(tiersService.getEnsembleTiersCouple((MenageCommun) null, dateMariage));
				assertNull(tiersService.getEnsembleTiersCouple((MenageCommun) null, apresMariage));
				assertNull(tiersService.getEnsembleTiersCouple((PersonnePhysique) null, avantMariage));
				assertNull(tiersService.getEnsembleTiersCouple((PersonnePhysique) null, dateMariage));
				assertNull(tiersService.getEnsembleTiersCouple((PersonnePhysique) null, apresMariage));
			}

			{
				// Personne seule
				final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(numeros.NO_CTB_PIERRE);
				assertNotNull(pierre);

				assertNull(tiersService.getEnsembleTiersCouple(pierre, avantMariage));
				assertNull(tiersService.getEnsembleTiersCouple(pierre, dateMariage));
				assertNull(tiersService.getEnsembleTiersCouple(pierre, apresMariage));
			}

			{
				// Mariée seule
				final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(numeros.NO_CTB_MOMO);
				assertNotNull(momo);

				assertNull(tiersService.getEnsembleTiersCouple(momo, avantMariage));

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(momo, dateMariage);
				assertNotNull(ensemble);
				assertEquals(momo, ensemble.getPrincipal());
				assertNull(ensemble.getConjoint());
				assertNotNull(ensemble.getMenage());
			}

			{
				// Marié seul
				final PersonnePhysique enguerrand = (PersonnePhysique) tiersDAO.get(numeros.NO_CTB_ENGUERRAND);
				assertNotNull(enguerrand);

				assertNull(tiersService.getEnsembleTiersCouple(enguerrand, avantMariage));

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(enguerrand, dateMariage);
				assertNotNull(ensemble);
				assertEquals(enguerrand, ensemble.getPrincipal());
				assertNull(ensemble.getConjoint());
				assertNotNull(ensemble.getMenage());
			}

			{
				// Couple complet
				final PersonnePhysique arnold = (PersonnePhysique) tiersDAO.get(numeros.NO_CTB_ARNOLD);
				final PersonnePhysique gudrun = (PersonnePhysique) tiersDAO.get(numeros.NO_CTB_GUDRUN);
				assertNotNull(arnold);
				assertNotNull(gudrun);

				assertNull(tiersService.getEnsembleTiersCouple(arnold, avantMariage));
				assertNull(tiersService.getEnsembleTiersCouple(gudrun, avantMariage));

				final EnsembleTiersCouple ensembleDepuisArnold = tiersService.getEnsembleTiersCouple(arnold, dateMariage);
				assertNotNull(ensembleDepuisArnold);
				assertEquals(arnold, ensembleDepuisArnold.getPrincipal());
				assertEquals(gudrun, ensembleDepuisArnold.getConjoint());
				assertNotNull(ensembleDepuisArnold.getMenage());

				final EnsembleTiersCouple ensembleDepuisGudrun = tiersService.getEnsembleTiersCouple(gudrun, dateMariage);
				assertNotNull(ensembleDepuisGudrun);
				assertEquals(arnold, ensembleDepuisGudrun.getPrincipal());
				assertEquals(gudrun, ensembleDepuisGudrun.getConjoint());
				assertNotNull(ensembleDepuisGudrun.getMenage());
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindMenageCommun() throws Exception {

		class Ids {
			long personne;
			long menage1;
			long menage2;
		}
		final Ids ids = new Ids();

		/**
		 * Une personne avec deux ménages :
		 *
		 *  o [1990,1,1] à [2000,1,1]
		 *  o [2004,1,1] à ...
		 */
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique personne = addNonHabitant("Jean", "Sairien", date(1990, 9, 9), Sexe.MASCULIN);
				MenageCommun menage1 = (MenageCommun) tiersDAO.save(new MenageCommun());
				MenageCommun menage2 = (MenageCommun) tiersDAO.save(new MenageCommun());

				ids.personne = personne.getId();
				ids.menage1 = menage1.getId();
				ids.menage2 = menage2.getId();

				hibernateTemplate.merge(new AppartenanceMenage(RegDate.get(1990, 1, 1), RegDate.get(2000, 1, 1), personne, menage1));
				hibernateTemplate.merge(new AppartenanceMenage(RegDate.get(2004, 1, 1), null, personne, menage2));

				return null;
			}
		});

		final PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(ids.personne);
		final MenageCommun menage1 = (MenageCommun) tiersDAO.get(ids.menage1);
		final MenageCommun menage2 = (MenageCommun) tiersDAO.get(ids.menage2);

		assertNull(tiersService.findMenageCommun(personne, RegDate.get(1980, 1, 1)));
		assertSame(menage1, tiersService.findMenageCommun(personne, RegDate.get(1995, 1, 1)));
		assertNull(tiersService.findMenageCommun(personne, RegDate.get(2002, 1, 1)));
		assertSame(menage2, tiersService.findMenageCommun(personne, RegDate.get(2004, 1, 1)));
		assertSame(menage2, tiersService.findMenageCommun(personne, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetSexe() {

		final long NO_PIERRE = 1;
		final long NO_MOMO = 2;

		/*
		 * Mise en place des données
		 */

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne seul
				addIndividu(NO_PIERRE, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// mariée seul
				addIndividu(NO_MOMO, RegDate.get(1953, 11, 2), "Dupneu", "Monique", false);
			}
		});

		final long NO_CTB_PIERRE;
		final long NO_CTB_MOMO;
		final long NO_CTB_ARNOLD;
		final long NO_CTB_NOLWEN;
		final long NO_CTB_ALF;
		{
			PersonnePhysique pierre = new PersonnePhysique(true);
			pierre.setNumeroIndividu(NO_PIERRE);
			pierre = (PersonnePhysique) tiersDAO.save(pierre);
			NO_CTB_PIERRE = pierre.getNumero();

			PersonnePhysique momo = new PersonnePhysique(true);
			momo.setNumeroIndividu(NO_MOMO);
			momo = (PersonnePhysique) tiersDAO.save(momo);
			NO_CTB_MOMO = momo.getNumero();

			PersonnePhysique arnold = new PersonnePhysique(false);
			arnold.setPrenom("Arnold");
			arnold.setNom("Schwarzie");
			arnold.setSexe(Sexe.MASCULIN);
			arnold = (PersonnePhysique) tiersDAO.save(arnold);
			NO_CTB_ARNOLD = arnold.getNumero();

			PersonnePhysique nolwen = new PersonnePhysique(false);
			nolwen.setPrenom("Nowlen");
			nolwen.setNom("Raflss");
			nolwen.setSexe(Sexe.FEMININ);
			nolwen = (PersonnePhysique) tiersDAO.save(nolwen);
			NO_CTB_NOLWEN = nolwen.getNumero();

			PersonnePhysique alf = new PersonnePhysique(false);
			alf.setPrenom("Alf");
			alf.setNom("Alf");
			alf.setSexe(null);
			alf = (PersonnePhysique) tiersDAO.save(alf);
			NO_CTB_ALF = alf.getNumero();
		}

		{
			final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(NO_CTB_PIERRE);
			assertNotNull(pierre);
			assertEquals(Sexe.MASCULIN, tiersService.getSexe(pierre));

			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(NO_CTB_MOMO);
			assertNotNull(momo);
			assertEquals(Sexe.FEMININ, tiersService.getSexe(momo));

			final PersonnePhysique arnold = (PersonnePhysique) tiersDAO.get(NO_CTB_ARNOLD);
			assertNotNull(arnold);
			assertEquals(Sexe.MASCULIN, tiersService.getSexe(arnold));

			final PersonnePhysique nolwen = (PersonnePhysique) tiersDAO.get(NO_CTB_NOLWEN);
			assertNotNull(nolwen);
			assertEquals(Sexe.FEMININ, tiersService.getSexe(nolwen));

			final PersonnePhysique alf = (PersonnePhysique) tiersDAO.get(NO_CTB_ALF);
			assertNotNull(alf);
			assertNull(tiersService.getSexe(alf));
		}
	}

	/**
	 * Case JIRA UNIREG-586: l'annulation d'un for fiscal principal doit réouvrir le for précédent s'il celui-ci est adjacent.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnuleForPrincipalAvecPrecedentAdjacent() throws Exception {

		class Ids {
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux adjacent
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

				ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 3, 31),
						MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				ids.premierForPrincipalId = premierForPrincipal.getId();
				premierForPrincipal.setTiers(eric);

				ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 4, 1), MotifFor.DEMENAGEMENT_VD,
						MockCommune.Cossonay);
				ids.secondForPrincipalId = secondForPrincipal.getId();
				secondForPrincipal.setTiers(eric);
				return null;
			}
		});

		final ForFiscalPrincipal secondForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);

		// annulation du second for principal
		tiersService.annuleForFiscal(secondForPrincipal);

		// vérification que le second for est bien annulé
		assertTrue(secondForPrincipal.isAnnule());

		// vérification que le premier for est bien ré-ouvert
		final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.premierForPrincipalId);
		assertNotNull(premierForPrincipal);
		assertEquals(date(1983, 4, 13), premierForPrincipal.getDateDebut());
		assertEquals(MotifFor.MAJORITE, premierForPrincipal.getMotifOuverture());
		assertNull(premierForPrincipal.getDateFin());
		assertNull(premierForPrincipal.getMotifFermeture());
		assertFalse(premierForPrincipal.isAnnule());
	}

	/**
	 * [UNIREG-1334] Lors de l'annulation d'un for secondaire, on recupère le dernier for principal et non le for principal ouvert car il peut ne pas y en avoir.
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnuleForSecondaireContribuableDecede() throws Exception {

		class Id {
			Long forSecondaire;
		}

		final Id id = new Id();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux non-adjacents
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

				ForFiscalPrincipal forPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 2, 28), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				forPrincipal.setTiers(eric);

				ForFiscalSecondaire forFiscalSecondaire =
						addForSecondaire(eric, date(1990, 4, 13), MotifFor.ACHAT_IMMOBILIER, date(2008, 2, 28), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne.getNoOFS(),
								MotifRattachement.IMMEUBLE_PRIVE);
				id.forSecondaire = forFiscalSecondaire.getId();
				forFiscalSecondaire.setTiers(eric);
				return null;
			}
		});

		final ForFiscalSecondaire forFiscalSecondaire = hibernateTemplate.get(ForFiscalSecondaire.class, id.forSecondaire);
		assertNotNull(forFiscalSecondaire);

		// annulation du for fiscal secondaire
		tiersService.annuleForFiscal(forFiscalSecondaire);

	}

	/**
	 * [UNIREG-1443] Correction bug lors de l'ouverture d'un for fiscal secondaire si le contribuable est décédé.
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOpenForSecondaireContribuableDecede() throws Exception {

		class Id {
			Long idEric;
		}

		final Id id = new Id();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable décédé
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				id.idEric = eric.getId();
				ForFiscalPrincipal forPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 2, 28), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				forPrincipal.setTiers(eric);
				return null;
			}
		});

		// Un for secondaire (associé a son unique for principal) lui est ajouté à posteriori
		Tiers eric = tiersService.getTiers(id.idEric);
		ForFiscalSecondaire ffs = tiersService.addForSecondaire(
				(Contribuable) eric,
				date(2004, 10, 5), date(2006, 3, 15),
				MotifRattachement.IMMEUBLE_PRIVE, MockCommune.Lausanne.getNoOFS(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER);
		tiersService.closeForFiscalSecondaire(
				(Contribuable) eric,
				ffs,
				date(2006, 3, 15),
				MotifFor.VENTE_IMMOBILIER);
	}

	/**
	 * Case JIRA UNIREG-586: l'annulation d'un for fiscal principal ne doit pas réouvrir le for précédent s'il celui-ci n'est pas adjacent.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnuleForPrincipalAvecPrecedentNonAdjacents() throws Exception {

		class Ids {
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux non-adjacents
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

				ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 2, 28),
						MotifFor.DEPART_HC, MockCommune.Lausanne);
				ids.premierForPrincipalId = premierForPrincipal.getId();
				premierForPrincipal.setTiers(eric);

				ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 11, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				ids.secondForPrincipalId = secondForPrincipal.getId();
				secondForPrincipal.setTiers(eric);
				return null;
			}
		});

		final ForFiscalPrincipal secondForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class,
				ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);

		// annulation du second for principal
		tiersService.annuleForFiscal(secondForPrincipal);

		// vérification que le second for est bien annulé
		assertTrue(secondForPrincipal.isAnnule());

		// vérification que le premier for n'est pas ré-ouvert
		final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class,
				ids.premierForPrincipalId);
		assertNotNull(premierForPrincipal);
		assertEquals(date(1983, 4, 13), premierForPrincipal.getDateDebut());
		assertEquals(MotifFor.MAJORITE, premierForPrincipal.getMotifOuverture());
		assertEquals(date(2008, 2, 28), premierForPrincipal.getDateFin());
		assertEquals(MotifFor.DEPART_HC, premierForPrincipal.getMotifFermeture());
		assertFalse(premierForPrincipal.isAnnule());
	}

	/**
	 * L'annulation d'un for fiscal principal alors qu'il n'est pas le dernier doit lever un exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnuleForPrincipalAvecSuivant() throws Exception {

		class Ids {
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux adjacent
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

				ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 3, 31),
						MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				ids.premierForPrincipalId = premierForPrincipal.getId();
				premierForPrincipal.setTiers(eric);

				ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 4, 1), MotifFor.DEMENAGEMENT_VD,
						MockCommune.Cossonay);
				ids.secondForPrincipalId = secondForPrincipal.getId();
				secondForPrincipal.setTiers(eric);
				return null;
			}
		});

		final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.premierForPrincipalId);
		assertNotNull(premierForPrincipal);
		final ForFiscalPrincipal secondForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);

		// annulation du premier for principal
		try {
			tiersService.annuleForFiscal(premierForPrincipal);
			fail();
		}
		catch (ValidationException ignored) {
			// ok
		}

		// vérification que le premier for n'est pas annulé
		assertNotNull(premierForPrincipal);
		assertEquals(date(1983, 4, 13), premierForPrincipal.getDateDebut());
		assertEquals(MotifFor.MAJORITE, premierForPrincipal.getMotifOuverture());
		assertEquals(date(2008, 3, 31), premierForPrincipal.getDateFin());
		assertEquals(MotifFor.DEMENAGEMENT_VD, premierForPrincipal.getMotifFermeture());
		assertFalse(premierForPrincipal.isAnnule());

		// vérification que le second for n'est pas changé
		assertEquals(date(2008, 4, 1), secondForPrincipal.getDateDebut());
		assertEquals(MotifFor.DEMENAGEMENT_VD, secondForPrincipal.getMotifOuverture());
		assertNull(secondForPrincipal.getDateFin());
		assertNull(secondForPrincipal.getMotifFermeture());
		assertFalse(secondForPrincipal.isAnnule());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetDernierForGestionConnuDebiteur() {
		// les DPI n'ont pas de for de gestion car ils sont gérés par une OID spéciale

		// Un débiteur sans for
		DebiteurPrestationImposable d1 = new DebiteurPrestationImposable();
		assertNull(tiersService.getDernierForGestionConnu(d1, date(1993, 2, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d1, date(2000, 12, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d1, date(2034, 5, 12)));
		assertNull(tiersService.getDernierForGestionConnu(d1, null));

		// Un débiteur avec un for dpi
		DebiteurPrestationImposable d2 = new DebiteurPrestationImposable();
		ForDebiteurPrestationImposable for2 = new ForDebiteurPrestationImposable();
		for2.setDateDebut(date(2000, 1, 1));
		for2.setDateFin(null);
		d2.addForFiscal(for2);
		assertNull(tiersService.getDernierForGestionConnu(d2, date(1993, 2, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d2, date(2000, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d2, date(2003, 2, 5)));
		assertNull(tiersService.getDernierForGestionConnu(d2, date(2011, 12, 15)));
		assertNull(tiersService.getDernierForGestionConnu(d2, null));

		// Un débiteur avec deux fors dpi
		DebiteurPrestationImposable d3 = new DebiteurPrestationImposable();
		ForDebiteurPrestationImposable for31 = new ForDebiteurPrestationImposable();
		for31.setDateDebut(date(2000, 1, 1));
		for31.setDateFin(date(2002, 12, 31));
		d3.addForFiscal(for31);
		ForDebiteurPrestationImposable for32 = new ForDebiteurPrestationImposable();
		for32.setDateDebut(date(2003, 1, 1));
		for32.setDateFin(null);
		d3.addForFiscal(for32);
		assertNull(tiersService.getDernierForGestionConnu(d3, date(1993, 2, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d3, date(1999, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d3, date(2000, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d3, date(2002, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d3, date(2003, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d3, date(2063, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d3, null));

		// Un débiteur avec deux fors dpi dans l'ordre inverse
		DebiteurPrestationImposable d4 = new DebiteurPrestationImposable();
		ForDebiteurPrestationImposable for41 = new ForDebiteurPrestationImposable();
		ForDebiteurPrestationImposable for42 = new ForDebiteurPrestationImposable();
		for42.setDateDebut(date(2003, 1, 1));
		for42.setDateFin(null);
		d4.addForFiscal(for42);
		for41.setDateDebut(date(2000, 1, 1));
		for41.setDateFin(date(2002, 12, 31));
		d4.addForFiscal(for41);
		assertNull(tiersService.getDernierForGestionConnu(d4, date(1999, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d4, date(2000, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d4, date(2002, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d4, date(2003, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d4, date(2003, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d4, date(2033, 2, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d4, null));

		// Un débiteur avec un for dpi et un autre d'une autre type (note: ce cas ne validerait pas normalement)
		DebiteurPrestationImposable d5 = new DebiteurPrestationImposable();
		ForDebiteurPrestationImposable for51 = new ForDebiteurPrestationImposable();
		for51.setDateDebut(date(2000, 1, 1));
		for51.setDateFin(null);
		d5.addForFiscal(for51);
		ForFiscalAutreImpot for52 = new ForFiscalAutreImpot();
		for52.setDateDebut(date(2005, 1, 1));
		for52.setDateFin(null);
		d5.addForFiscal(for52);
		assertNull(tiersService.getDernierForGestionConnu(d5, date(1999, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d5, date(2000, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d5, date(2028, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d5, null));

		// Un débiteur avec deux fors dpi, dont le plus récent est annulé
		DebiteurPrestationImposable d6 = new DebiteurPrestationImposable();
		ForDebiteurPrestationImposable for61 = new ForDebiteurPrestationImposable();
		for61.setDateDebut(date(2000, 1, 1));
		for61.setDateFin(date(2002, 12, 31));
		d6.addForFiscal(for61);
		ForDebiteurPrestationImposable for62 = new ForDebiteurPrestationImposable();
		for62.setDateDebut(date(2005, 1, 1));
		for62.setDateFin(null);
		for62.setAnnulationDate(DateHelper.getDate(2005, 3, 1));
		d6.addForFiscal(for62);
		assertNull(tiersService.getDernierForGestionConnu(d6, date(1999, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d6, date(2000, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d6, date(2002, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d6, date(2045, 2, 17)));
		assertNull(tiersService.getDernierForGestionConnu(d6, null));

		// Un débiteur avec deux fors dpis disjoints et fermés
		DebiteurPrestationImposable d7 = new DebiteurPrestationImposable();
		ForDebiteurPrestationImposable for71 = new ForDebiteurPrestationImposable();
		for71.setDateDebut(date(2000, 1, 1));
		for71.setDateFin(date(2002, 12, 31));
		d7.addForFiscal(for71);
		ForDebiteurPrestationImposable for72 = new ForDebiteurPrestationImposable();
		for72.setDateDebut(date(2005, 1, 1));
		for72.setDateFin(date(2007, 6, 30));
		d7.addForFiscal(for72);
		assertNull(tiersService.getDernierForGestionConnu(d7, date(1999, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d7, date(2000, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d7, date(2001, 3, 19)));
		assertNull(tiersService.getDernierForGestionConnu(d7, date(2002, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d7, date(2003, 1, 1))); // le for71 reste le for de gestion, même fermé
		assertNull(tiersService.getDernierForGestionConnu(d7, date(2004, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(d7, date(2005, 1, 1))); // le for72 prend la relève à partir de là
		assertNull(tiersService.getDernierForGestionConnu(d7, date(2052, 1, 1)));
		assertNull(tiersService.getDernierForGestionConnu(d7, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableAucunFor() {

		// Contribuable sans for
		PersonnePhysique c = new PersonnePhysique(false);
		assertNull(tiersService.getForGestionActif(c, null));
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(2008, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertEmpty(tiersService.getForsGestionHisto(c));

		c.addForFiscal(newForChien(date(2000, 1, 1), null));
		assertNull(tiersService.getForGestionActif(c, null));
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(2008, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertEmpty(tiersService.getForsGestionHisto(c));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipal() {

		// Contribuable avec un for principal ouvert
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForPrincipal(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.DOMICILE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, 1234, histo.get(0));
		}

		// Contribuable avec un for principal fermé
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForPrincipal(c, date(2000, 1, 1), date(2008, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.DOMICILE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), date(2008, 12, 31), 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), date(2008, 12, 31), 1234, tiersService.getForGestionActif(c, date(2004, 7, 3)));
			assertForGestion(date(2000, 1, 1), date(2008, 12, 31), 1234, tiersService.getForGestionActif(c, date(2008, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, 1234, histo.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalHorsCanton() {

		// Contribuable avec un for principal ouvert hors-canton
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForPrincipal(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, null));
			assertEmpty(tiersService.getForsGestionHisto(c));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalSourcier() {

		// Contribuable avec un for principal ouvert dans le canton mais sourcier
		{
			PersonnePhysique c = new PersonnePhysique(false);
			ForFiscalPrincipal for0 = addForPrincipal(c, date(2000, 1, 1), null, 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
			for0.setModeImposition(ModeImposition.SOURCE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, null));
			assertEmpty(tiersService.getForsGestionHisto(c));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDeuxForsPrincipaux() {

		// Contribuable avec deux fors principaux dans le canton
		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(2000, 1, 1), date(2002, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForPrincipal(c, date(2003, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getForGestionActif(c, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2008, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, histo.get(0));
		assertForGestion(date(2003, 1, 1), null, 4321, histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForSecondaire() {

		// Contribuable avec un for secondaire activite independante
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForSecondaire(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.ACTIVITE_INDEPENDANTE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, 1234, histo.get(0));
		}

		// Contribuable avec un for secondaire immeuble
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForSecondaire(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.IMMEUBLE_PRIVE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, 1234, histo.get(0));
		}

		// Contribuable avec un for administrateur
		{
			PersonnePhysique c = new PersonnePhysique(false);
			ForFiscalAutreElementImposable for0 = new ForFiscalAutreElementImposable();
			for0.setDateDebut(date(2000, 1, 1));
			for0.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			for0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			for0.setNumeroOfsAutoriteFiscale(1234);
			for0.setMotifRattachement(MotifRattachement.ADMINISTRATEUR);
			c.addForFiscal(for0);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertNull(tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, null));

			assertEmpty(tiersService.getForsGestionHisto(c));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDeuxForsSecondairesConsecutifs() {

		// Contribuable avec deux fors secondaires
		PersonnePhysique c = new PersonnePhysique(false);
		addForSecondaire(c, date(2000, 1, 1), date(2002, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c, date(2003, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);

		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c, null));
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getForGestionActif(c, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2008, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, histo.get(0));
		assertForGestion(date(2003, 1, 1), null, 4321, histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDeuxForsSecondairesSeRecoupant() {

		// Contribuable avec deux fors secondaires se recoupant
		PersonnePhysique c = new PersonnePhysique(false);
		addForSecondaire(c, date(2000, 1, 1), date(2007, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c, date(2003, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);

		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c, null));
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), 1234, tiersService.getForGestionActif(c, date(2005, 9, 12)));
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), 1234, tiersService.getForGestionActif(c, date(2007, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2008, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), 1234, histo.get(0));
		assertForGestion(date(2008, 1, 1), null, 4321, histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalOuvertEtUnForSecondaireOuvert() {

		// Contribuable avec un for principal ouvert et un for secondaire ouvert
		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForSecondaire(c, date(2000, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, 1234, histo.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalFermeEtUnForSecondaireOuvert() {

		// Contribuable avec un for principal fermé et un for secondaires ouvert
		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(2000, 1, 1), date(2004, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForSecondaire(c, date(2000, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), 1234, tiersService.getForGestionActif(c, date(2004, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2005, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), 1234, histo.get(0));
		assertForGestion(date(2005, 1, 1), null, 4321, histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalOuvertEtUnForSecondaireFerme() {

		// Contribuable avec un for principal ouvert et un for secondaire fermé
		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForSecondaire(c, date(2000, 1, 1), date(2004, 12, 31), 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2004, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, date(2005, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, 1234, histo.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalHorsCantonEtUnForSecondaire() {

		// Contribuable avec un for principal hors-canton et un for secondaire ouvert dans le canton
		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		addForSecondaire(c, date(2000, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, 4321, histo.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalHorsCantonEtDeuxForsSecondairesSeRecoupant() {

		// Contribuable avec un for principal hors-canton et deux fors secondaires dans le canton se recoupant
		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		addForSecondaire(c, date(2002, 1, 1), null, 1111, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c, date(2000, 1, 1), date(2003, 12, 31), 2222, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), 2222, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), 2222, tiersService.getForGestionActif(c, date(2003, 12, 31)));
		assertForGestion(date(2002, 1, 1), null, 1111, tiersService.getForGestionActif(c, date(2004, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, 1111, tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2002, 1, 1), null, 1111, tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, 1111, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), 2222, histo.get(0));
		assertForGestion(date(2004, 1, 1), null, 1111, histo.get(1));
	}

	/**
	 * Contribuable avec un for principal dans le canton fermé suivi d'un for principal hors-canton ouvert et deux fors secondaires dans le canton se recoupant
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalCantonFermeUnForPrincipalHorsCantonOuvertEtDeuxForsSecondairesSeRecoupant() {

		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(1990, 1, 1), date(2000, 12, 31), 1111, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForPrincipal(c, date(2001, 1, 1), date(2001, 1, 1), 2222, TypeAutoriteFiscale.COMMUNE_HC,
				MotifRattachement.DOMICILE);
		addForSecondaire(c, date(2000, 1, 1), date(2003, 12, 31), 3333, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c, date(2002, 1, 1), null, 4444, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1989, 12, 31)));
		assertForGestion(date(1990, 1, 1), date(2000, 12, 31), 1111, tiersService.getForGestionActif(c, date(1990, 1, 1)));
		assertForGestion(date(1990, 1, 1), date(2000, 12, 31), 1111, tiersService.getForGestionActif(c, date(2000, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), 3333, tiersService.getForGestionActif(c, date(2001, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), 3333, tiersService.getForGestionActif(c, date(2003, 12, 31)));
		assertForGestion(date(2002, 1, 1), null, 4444, tiersService.getForGestionActif(c, date(2004, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, 4444, tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2002, 1, 1), null, 4444, tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, 4444, tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(3, histo.size());
		assertForGestion(date(1990, 1, 1), date(2000, 12, 31), 1111, histo.get(0));
		assertForGestion(date(2001, 1, 1), date(2003, 12, 31), 3333, histo.get(1));
		assertForGestion(date(2004, 1, 1), null, 4444, histo.get(2));
	}

	/**
	 * Contribuable avec un for principal dans le canton fermé suivi d'un for principal hors-canton ouvert et deux fors secondaires dans le canton qui débutent en même temps
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalCantonFermeUnForPrincipalHorsCantonOuvertEtDeuxForsSecondairesDebutantEnMemeTemps() {

		final int aubonne = MockCommune.Aubonne.getNoOFS(); // 5422
		final int bex = MockCommune.Bex.getNoOFS(); // 5402

		// Cas du numéro Ofs du fors secondaire identique au dernier for principal vaudois
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForPrincipal(c, date(1990, 1, 1), date(1998, 12, 31), bex, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.DOMICILE);
			addForPrincipal(c, date(2001, 1, 1), date(2001, 1, 1), 2222, TypeAutoriteFiscale.COMMUNE_HC,
					MotifRattachement.DOMICILE);
			addForSecondaire(c, date(2000, 1, 1), date(2003, 12, 31), aubonne, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(c, date(2000, 1, 1), null, bex, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1989, 12, 31)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), bex, tiersService.getForGestionActif(c, date(1990, 1, 1)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), bex, tiersService.getForGestionActif(c, date(1998, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2001, 1, 1))); // numéro Ofs de for3
			// identique à celui
			// de for0
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2003, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2004, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(1990, 1, 1), null, bex, histo.get(0));
		}

		// Cas où aucun for secondaire ne possède du numéro Ofs identique au dernier for principal vaudois
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForPrincipal(c, date(1990, 1, 1), date(1998, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.DOMICILE);
			addForPrincipal(c, date(2001, 1, 1), date(2001, 1, 1), 2345, TypeAutoriteFiscale.COMMUNE_HC,
					MotifRattachement.DOMICILE);
			addForSecondaire(c, date(2000, 1, 1), date(2003, 12, 31), bex, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.ACTIVITE_INDEPENDANTE);
			addForSecondaire(c, date(2000, 1, 1), null, aubonne, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.ACTIVITE_INDEPENDANTE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1989, 12, 31)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), 1234, tiersService.getForGestionActif(c, date(1990, 1, 1)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), 1234, tiersService.getForGestionActif(c, date(1998, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			// [UNIREG-1029] aubonne est placé devant bex dans l'ordre alphabétique
			assertForGestion(date(2000, 1, 1), null, aubonne, tiersService.getForGestionActif(c, date(2001, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, aubonne, tiersService.getForGestionActif(c, date(2003, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, aubonne, tiersService.getForGestionActif(c, date(2004, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, aubonne, tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, aubonne, tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, aubonne, tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(2, histo.size());
			assertForGestion(date(1990, 1, 1), date(1999, 12, 31), 1234, histo.get(0));
			assertForGestion(date(2000, 1, 1), null, aubonne, histo.get(1));
		}

		// Cas où il n'y a pas de dernier for principal vaudois
		{
			PersonnePhysique c = new PersonnePhysique(false);
			addForPrincipal(c, date(2001, 1, 1), date(2001, 1, 1), 2345, TypeAutoriteFiscale.COMMUNE_HC,
					MotifRattachement.DOMICILE);
			addForSecondaire(c, date(2000, 1, 1), date(2003, 12, 31), aubonne, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.ACTIVITE_INDEPENDANTE);
			addForSecondaire(c, date(2000, 1, 1), null, bex, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
					MotifRattachement.ACTIVITE_INDEPENDANTE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			// [UNIREG-1029] aubonne est placé devant bex dans l'ordre alphabétique
			assertForGestion(date(2000, 1, 1), date(2003, 12, 31), aubonne, tiersService.getForGestionActif(c, date(2001, 1, 1)));
			assertForGestion(date(2000, 1, 1), date(2003, 12, 31), aubonne, tiersService.getForGestionActif(c, date(2003, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2004, 1, 1))); // for1 est maintenant fermé
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, bex, tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(2, histo.size());
			assertForGestion(date(2000, 1, 1), date(2003, 12, 31), aubonne, histo.get(0));
			assertForGestion(date(2004, 1, 1), null, bex, histo.get(1));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDeuxForsPrincipauxDontUnAnnule() {

		// Contribuable avec deux fors principaux dont le plus récent est annulé
		PersonnePhysique c = new PersonnePhysique(false);
		addForPrincipal(c, date(2000, 1, 1), date(2002, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		ForFiscalPrincipal for1 = addForPrincipal(c, date(2005, 1, 1), null, 4321,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
		for1.setAnnulationDate(DateHelper.getDate(2005, 3, 1));

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getForGestionActif(c, date(2002, 12, 31)));
		assertNull(tiersService.getForGestionActif(c, date(2003, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, 1234, histo.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetDernierForGestionConnuContribuable() {

		// Contribuable sans for
		PersonnePhysique c1 = new PersonnePhysique(false);
		assertNull(tiersService.getDernierForGestionConnu(c1, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c1, date(2005, 1, 23)));
		assertNull(tiersService.getDernierForGestionConnu(c1, date(2030, 12, 31)));

		// Contribuable avec un for principal
		PersonnePhysique c2 = new PersonnePhysique(false);
		addForPrincipal(c2, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		assertNull(tiersService.getDernierForGestionConnu(c2, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c2, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c2, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c2, date(2043, 3, 11)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c2, null));

		// Contribuable avec deux fors principaux
		PersonnePhysique c3 = new PersonnePhysique(false);
		addForPrincipal(c3, date(2000, 1, 1), date(2002, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForPrincipal(c3, date(2003, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		assertNull(tiersService.getDernierForGestionConnu(c3, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c3, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c3, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c3, date(2001, 9, 21)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c3, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c3, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c3, date(2033, 6, 6)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c3, null));

		// Contribuable avec un for secondaire
		PersonnePhysique c4 = new PersonnePhysique(false);
		addForSecondaire(c4, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c4, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c4, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c4, null));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c4, date(2018, 12, 31)));

		// Contribuable avec deux fors secondaires
		PersonnePhysique c5 = new PersonnePhysique(false);
		addForSecondaire(c5, date(2000, 1, 1), date(2002, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c5, date(2003, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c5, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c5, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c5, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c5, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c5, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c5, null));

		// Contribuable avec un for principal et un for secondaire
		PersonnePhysique c6 = new PersonnePhysique(false);
		addForPrincipal(c6, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForSecondaire(c6, date(2000, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c6, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c6, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c6, date(2022, 2, 7)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c6, null));

		// Contribuable avec un for principal fermé et un for secondaires ouvert
		PersonnePhysique c7 = new PersonnePhysique(false);
		addForPrincipal(c7, date(2000, 1, 1), date(2004, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForSecondaire(c7, date(2000, 1, 1), null, 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c7, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), 1234, tiersService.getDernierForGestionConnu(c7, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), 1234, tiersService.getDernierForGestionConnu(c7, date(2004, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c7, date(2005, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c7, date(2021, 7, 17)));
		assertForGestion(date(2000, 1, 1), null, 4321, tiersService.getDernierForGestionConnu(c7, null));

		// Contribuable avec un for principal ouvert et un for secondaire fermé
		PersonnePhysique c8 = new PersonnePhysique(false);
		addForPrincipal(c8, date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForSecondaire(c8, date(2000, 1, 1), date(2004, 12, 31), 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c8, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c8, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c8, date(2012, 7, 8)));
		assertForGestion(date(2000, 1, 1), null, 1234, tiersService.getDernierForGestionConnu(c8, null));

		// Contribuable avec deux fors principaux dont le plus récent est annulé
		PersonnePhysique c9 = new PersonnePhysique(false);
		addForPrincipal(c9, date(2000, 1, 1), date(2002, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		ForFiscalPrincipal for9_2 = addForPrincipal(c9, date(2005, 1, 1), null, 4321,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
		for9_2.setAnnulationDate(DateHelper.getDate(2005, 3, 1));
		assertNull(tiersService.getDernierForGestionConnu(c9, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c9, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c9, date(2002, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c9, date(2003, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c9, date(2052, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c9, null));

		// Contribuable avec deux fors principaux disjoints et fermés
		PersonnePhysique c10 = new PersonnePhysique(false);
		addForPrincipal(c10, date(2000, 1, 1), date(2002, 12, 31), 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		addForPrincipal(c10, date(2005, 1, 1), date(2007, 6, 30), 4321, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MotifRattachement.DOMICILE);
		assertNull(tiersService.getDernierForGestionConnu(c10, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c10, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c10, date(2001, 3, 19)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c10, date(2002, 12, 31)));
		// le for10_1 reste le for de gestion, même fermé
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c10, date(2003, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), 1234, tiersService.getDernierForGestionConnu(c10, date(2004, 12, 31)));
		// le for10_2 prend la relève à partir de là
		assertForGestion(date(2005, 1, 1), date(2007, 6, 30), 4321, tiersService.getDernierForGestionConnu(c10, date(2005, 1, 1)));
		assertForGestion(date(2005, 1, 1), date(2007, 6, 30), 4321, tiersService.getDernierForGestionConnu(c10, date(2052, 1, 1)));
		assertForGestion(date(2005, 1, 1), date(2007, 6, 30), 4321, tiersService.getDernierForGestionConnu(c10, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsSuisse() throws Exception {

		// individu avec nationalité suisse
		{
			MockIndividu ind = new MockIndividu();
			ind.setNationalites(Arrays.<Nationalite>asList(new MockNationalite(null, null, MockPays.Suisse)));
			assertTrue(tiersService.isSuisse(ind, null));
		}

		// individu avec nationalité française
		{
			MockIndividu ind = new MockIndividu();
			ind.setNationalites(Arrays.<Nationalite>asList(new MockNationalite(null, null, MockPays.France)));
			assertFalse(tiersService.isSuisse(ind, null));
		}

		// [UNIREG-1588] individu sans nationalité
		{
			try {
				MockIndividu ind = new MockIndividu();
				tiersService.isSuisse(ind, null);
				fail();
			}
			catch (TiersException e) {
				assertEquals("Impossible de déterminer la nationalité de l'individu n°0", e.getMessage());
			}

			try {
				MockIndividu ind = new MockIndividu();
				ind.setNationalites(Collections.<Nationalite>emptyList());
				tiersService.isSuisse(ind, null);
				fail();
			}
			catch (TiersException e) {
				assertEquals("Impossible de déterminer la nationalité de l'individu n°0", e.getMessage());
			}
		}
	}

	private ForFiscalAutreImpot newForChien(RegDate dateDebut, @Nullable RegDate dateFin) {
		ForFiscalAutreImpot for0 = new ForFiscalAutreImpot();
		for0.setDateDebut(dateDebut);
		for0.setDateFin(dateFin);
		for0.setGenreImpot(GenreImpot.CHIENS);
		for0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for0.setNumeroOfsAutoriteFiscale(1234);
		return for0;
	}

	private static ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, @Nullable RegDate fermeture, Integer noOFS,
	                                                  TypeAutoriteFiscale type, MotifRattachement motif) {
		ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(ouverture);
		f.setDateFin(fermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(type);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f.setModeImposition(ModeImposition.ORDINAIRE);
		contribuable.addForFiscal(f);
		return f;
	}

	private static ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, @Nullable RegDate fermeture, Integer noOFS,
	                                                    TypeAutoriteFiscale type, MotifRattachement motif) {
		ForFiscalSecondaire f = new ForFiscalSecondaire();
		f.setDateDebut(ouverture);
		f.setDateFin(fermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(type);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		tiers.addForFiscal(f);
		return f;
	}

	private static void assertForGestion(RegDate debut, @Nullable RegDate fin, int noOfsCommune, ForGestion f) {
		assertNotNull(f);
		assertEquals(debut, f.getDateDebut());
		assertEquals(fin, f.getDateFin());
		assertEquals(noOfsCommune, f.getNoOfsCommune());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermeAdresseTemporaireSimple() throws Exception {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDemandeFermeture = date(2009, 10, 12);

		final PersonnePhysique achile = addNonHabitant("Achile", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
		addAdresseSuisse(achile, TypeAdresseTiers.COURRIER, dateDebut, null, MockRue.CossonayVille.AvenueDuFuniculaire);

		tiersService.fermeAdresseTiersTemporaire(achile, dateDemandeFermeture);

		// cas simple, l'adresse doit effectivement avoir été fermée
		final Set<AdresseTiers> adresses = achile.getAdressesTiers();
		assertNotNull(adresses);
		assertEquals(1, adresses.size());
		final AdresseTiers adresse = adresses.iterator().next();
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEquals(dateDemandeFermeture, adresse.getDateFin());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermeAdresseTemporairePermanente() {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDemandeFermeture = date(2009, 10, 12);

		final PersonnePhysique achile = addNonHabitant("Achile", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
		final AdresseSuisse adressePermanente = addAdresseSuisse(achile, TypeAdresseTiers.COURRIER, dateDebut, null, MockRue.CossonayVille.AvenueDuFuniculaire);
		adressePermanente.setPermanente(true);

		tiersService.fermeAdresseTiersTemporaire(achile, dateDemandeFermeture);

		// adresse permanente -> ne doit pas avoir été fermée
		final Set<AdresseTiers> adresses = achile.getAdressesTiers();
		assertNotNull(adresses);
		assertEquals(1, adresses.size());
		final AdresseTiers adresse = adresses.iterator().next();
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertNull(adresse.getDateFin());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermeAdresseTemporaireAnnulee() {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDemandeFermeture = date(2009, 10, 12);

		final PersonnePhysique achile = addNonHabitant("Achile", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
		final AdresseSuisse adresseAnnulee = addAdresseSuisse(achile, TypeAdresseTiers.COURRIER, dateDebut, null, MockRue.CossonayVille.AvenueDuFuniculaire);
		adresseAnnulee.setAnnule(true);

		tiersService.fermeAdresseTiersTemporaire(achile, dateDemandeFermeture);

		// adresse annulée -> ne doit pas avoir été fermée
		final Set<AdresseTiers> adresses = achile.getAdressesTiers();
		assertNotNull(adresses);
		assertEquals(1, adresses.size());
		final AdresseTiers adresse = adresses.iterator().next();
		assertNotNull(adresse);
		assertTrue(adresse.isAnnule());
		assertEquals(dateDebut, adresse.getDateDebut());
		assertNull(adresse.getDateFin());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermeAdresseTemporaireDebutDansLeFutur() {

		final RegDate dateDebut = date(2010, 1, 1);
		final RegDate dateDemandeFermeture = date(2009, 10, 12);

		final PersonnePhysique achile = addNonHabitant("Achile", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
		addAdresseSuisse(achile, TypeAdresseTiers.COURRIER, dateDebut, null, MockRue.CossonayVille.AvenueDuFuniculaire);

		tiersService.fermeAdresseTiersTemporaire(achile, dateDemandeFermeture);

		// adresse débute dans le futur de la date de demande de fermeture -> ne doit pas avoir été fermée
		final Set<AdresseTiers> adresses = achile.getAdressesTiers();
		assertNotNull(adresses);
		assertEquals(1, adresses.size());
		final AdresseTiers adresse = adresses.iterator().next();
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertNull(adresse.getDateFin());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermeAdresseTemporaireDejaFermee() {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateFin = date(2005, 12, 31);
		final RegDate dateDemandeFermeture = date(2009, 10, 12);

		final PersonnePhysique achile = addNonHabitant("Achile", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
		addAdresseSuisse(achile, TypeAdresseTiers.COURRIER, dateDebut, dateFin, MockRue.CossonayVille.AvenueDuFuniculaire);

		tiersService.fermeAdresseTiersTemporaire(achile, dateDemandeFermeture);

		// adresse déjà fermée -> on ne doit pas changer la date de fermeture
		final Set<AdresseTiers> adresses = achile.getAdressesTiers();
		assertNotNull(adresses);
		assertEquals(1, adresses.size());
		final AdresseTiers adresse = adresses.iterator().next();
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEquals(dateFin, adresse.getDateFin());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFlagHabitantApresOuvertureDeForSurCouple() throws Exception {

		// voilà le topo :
		// - un couple
		// - Monsieur n'est pas habitant (à l'étranger), Madame l'est
		// - déménagement (= ouverture d'un nouveau for du couple, i.e. madame, en fait) sur une autre commune
		// - si l'adresse de Monsieur reste hors du canton, il ne doit pas devenir habitant
		// - si l'adresse de Monsieur devient vaudoise, il doit alors devenir habitant

		final long noIndMadame = 123254L;
		final long noIndMonsieur = 424566L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndMonsieur, date(1950, 3, 24), "Achille", "Talon", true);
				addIndividu(noIndMadame, date(1950, 5, 12), "Huguette", "Marcot", false);
			}
		});

		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// ancien habitant...
				final PersonnePhysique monsieur = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				monsieur.setNumeroIndividu(noIndMonsieur);
				addAdresseSuisse(monsieur, TypeAdresseTiers.DOMICILE, date(1998, 1, 1), date(1999, 12, 31), MockRue.Lausanne.AvenueDeBeaulieu);
				addAdresseSuisse(monsieur, TypeAdresseTiers.DOMICILE, date(2000, 1, 1), null, MockRue.Neuchatel.RueDesBeauxArts);

				final PersonnePhysique madame = addHabitant(noIndMadame);
				addAdresseSuisse(madame, TypeAdresseTiers.DOMICILE, date(1998, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, date(1998, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				tiersService.closeForFiscalPrincipal(mc, date(2001, 12, 31), MotifFor.DEMENAGEMENT_VD);
				tiersService
						.openForFiscalPrincipal(mc, date(2002, 1, 1), MotifRattachement.DOMICILE, MockCommune.Aubonne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE,
								MotifFor.DEMENAGEMENT_VD);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, null);
				Assert.assertNotNull(ensemble);

				final PersonnePhysique monsieur = ensemble.getPrincipal();
				final PersonnePhysique madame = ensemble.getConjoint();
				Assert.assertNotNull(monsieur);
				Assert.assertNotNull(madame);
				Assert.assertEquals(noIndMonsieur, (long) monsieur.getNumeroIndividu());
				Assert.assertEquals(noIndMadame, (long) madame.getNumeroIndividu());
				Assert.assertTrue(madame.isHabitantVD());
				Assert.assertFalse(monsieur.isHabitantVD());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFlagHabitantSuiteAAnnulationDeForSurCouple() throws Exception {
		// cas tiré du cas jira UNIREG-2021 :
		// - couple, Monsieur est en Italie, Madame à Lausanne depuis le 24.02.2010, étranger avant (donc Madame est maintenant habitante)
		// - annulation du for Lausannois, ré-ouverture du for italien
		// - -> Monsieur doit rester non-habitant
		// - -> Madame doit rester habitante, car son adresse de domicile n'a pas changé...

		final long noIndMadame = 123254L;
		final long noIndMonsieur = 424566L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu mr = addIndividu(noIndMonsieur, date(1950, 3, 24), "Achille", "Talon", true);
				addAdresse(mr, TypeAdresseCivil.PRINCIPALE, "Parca Guell", "12", 1000, null, "Barcelona", MockPays.Espagne, date(1990, 5, 1), null);

				final MockIndividu mme = addIndividu(noIndMadame, date(1950, 5, 12), "Huguette", "Marcot", false);
				addAdresse(mme, TypeAdresseCivil.PRINCIPALE, "Parca Guell", "12", 1000, null, "Barcelona", MockPays.Espagne, date(1990, 5, 1), date(2010, 2, 23));
				addAdresse(mme, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2010, 2, 24), null);
			}
		});

		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				// ancien habitant...
				final PersonnePhysique monsieur = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				monsieur.setNumeroIndividu(noIndMonsieur);

				final PersonnePhysique madame = addHabitant(noIndMadame);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, date(1998, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(2000, 1, 1), MotifFor.DEPART_HS, date(2010, 2, 23), MotifFor.ARRIVEE_HS, MockPays.Espagne);
				addForPrincipal(mc, date(2010, 2, 24), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(date(2010, 2, 24));
				tiersService.annuleForFiscal(ffp);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, null);
				Assert.assertNotNull(ensemble);

				final PersonnePhysique monsieur = ensemble.getPrincipal();
				final PersonnePhysique madame = ensemble.getConjoint();
				Assert.assertNotNull(monsieur);
				Assert.assertNotNull(madame);
				Assert.assertEquals(noIndMonsieur, (long) monsieur.getNumeroIndividu());
				Assert.assertEquals(noIndMadame, (long) madame.getNumeroIndividu());
				Assert.assertTrue(madame.isHabitantVD());
				Assert.assertFalse(monsieur.isHabitantVD());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnuleDernierForFiscalPrincipalDeuxFois() throws Exception {

		final long ffpId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Simon", date(1930, 4, 3), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 5, 2), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return ffp.getId();
			}
		});

		// première annulation
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, ffpId);
				assertNotNull(ffp);
				assertFalse(ffp.isAnnule());
				tiersService.annuleForFiscal(ffp);
				return null;
			}
		});

		// deuxième annulation
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, ffpId);
				assertNotNull(ffp);
				assertTrue(ffp.isAnnule());
				try {
					tiersService.annuleForFiscal(ffp);
					fail("La deuxième annulation du for fiscal aurait dû être refusée !");
				}
				catch (ValidationException e) {
					assertEmpty(e.getWarnings());
					assertEquals(1, e.getErrors().size());

					final String erreur = e.getErrors().get(0).getMessage();
					assertEquals("Tous les fors principaux sont déjà annulés.", erreur);
				}
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisHabitant() throws Exception {

		// un habitant ne peut pas être sourcier gris!

		final long noIndAchille = 123456L;
		final long noIndHuguette = 32421L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndAchille, date(1950, 3, 24), "Achille", "Talon", true);
			}
		});

		final MutableLong idCtbHabitant = new MutableLong();
		final MutableLong idCtbNonHabitantAvecNumeroInd = new MutableLong();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique achille = addHabitant(noIndAchille);
				final ForFiscalPrincipal ffpAchille = addForPrincipal(achille, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffpAchille.setModeImposition(ModeImposition.SOURCE);
				idCtbHabitant.setValue(achille.getNumero());


				final PersonnePhysique huguette = addNonHabitant("Huguette", "Marcot", date(1950, 4, 12), Sexe.FEMININ);
				huguette.setNumeroIndividu(noIndHuguette);
				final ForFiscalPrincipal ffpHuguette = addForPrincipal(huguette, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffpHuguette.setModeImposition(ModeImposition.SOURCE);
				idCtbNonHabitantAvecNumeroInd.setValue(huguette.getNumero());
				return null;
			}
		});

		final PersonnePhysique achille = (PersonnePhysique) tiersDAO.get(idCtbHabitant.longValue());
		Assert.assertFalse("Achille est habitant, il ne devrait pas être 'sourcier gris'", tiersService.isSourcierGris(achille, null));

		final PersonnePhysique huguette = (PersonnePhysique) tiersDAO.get(idCtbNonHabitantAvecNumeroInd.longValue());
		Assert.assertFalse("Huguette a un numéro d'habitant, elle ne devrait pas être 'sourcier gris'", tiersService.isSourcierGris(huguette, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGris() throws Exception {

		final long noCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique nh = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(nh, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return nh.getNumero();
			}
		});

		final PersonnePhysique achille = (PersonnePhysique) tiersDAO.get(noCtb);
		Assert.assertTrue("Sourcier sur for vaudois sans numéro d'individu, pourquoi pas 'gris' ?", tiersService.isSourcierGris(achille, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisHorsCanton() throws Exception {

		final long noCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique nh = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(nh, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Neuchatel);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return nh.getNumero();
			}
		});

		final PersonnePhysique achille = (PersonnePhysique) tiersDAO.get(noCtb);
		Assert.assertFalse("Sourcier sur for HC, pourquoi 'gris' ?", tiersService.isSourcierGris(achille, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisHorsSuisse() throws Exception {

		final long noCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique nh = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(nh, date(2008, 1, 1), MotifFor.MAJORITE, MockPays.Espagne);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return nh.getNumero();
			}
		});

		final PersonnePhysique achille = (PersonnePhysique) tiersDAO.get(noCtb);
		Assert.assertFalse("Sourcier sur for HS, pourquoi 'gris' ?", tiersService.isSourcierGris(achille, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisMixte() throws Exception {

		final MutableLong idCtbMixte1 = new MutableLong();
		final MutableLong idCtbMixte2 = new MutableLong();

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PersonnePhysique nh1 = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp1 = addForPrincipal(nh1, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp1.setModeImposition(ModeImposition.MIXTE_137_1);
				idCtbMixte1.setValue(nh1.getNumero());

				final PersonnePhysique nh2 = addNonHabitant("Achille", "Talon-Deux", date(1950, 3, 24), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp2 = addForPrincipal(nh2, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp2.setModeImposition(ModeImposition.MIXTE_137_2);
				idCtbMixte2.setValue(nh2.getNumero());

				return null;
			}
		});

		final PersonnePhysique mixte1 = (PersonnePhysique) tiersDAO.get(idCtbMixte1.longValue());
		Assert.assertFalse("Sourcier mixte 1, pourquoi 'gris' ?", tiersService.isSourcierGris(mixte1, null));

		final PersonnePhysique mixte2 = (PersonnePhysique) tiersDAO.get(idCtbMixte2.longValue());
		Assert.assertFalse("Sourcier mixte 2, pourquoi 'gris' ?", tiersService.isSourcierGris(mixte2, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisMenage() throws Exception {

		final MutableLong idCtbPrincipal = new MutableLong();
		final MutableLong idCtbConjoint = new MutableLong();
		final MutableLong idCtbCouple = new MutableLong();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				idCtbPrincipal.setValue(prn.getNumero());

				final PersonnePhysique sec = addNonHabitant("Huguette", "Marcot", date(1950, 4, 12), Sexe.FEMININ);
				idCtbConjoint.setValue(sec.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, sec, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				final ForFiscalPrincipal ffp = addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);

				return null;
			}
		});

		final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(idCtbPrincipal.longValue());
		Assert.assertTrue("Couple avec for source vaudois sans habitant, pourquoi pas 'gris' ?", tiersService.isSourcierGris(principal, null));

		final PersonnePhysique conjoint = (PersonnePhysique) tiersDAO.get(idCtbConjoint.longValue());
		Assert.assertTrue("Couple avec for source vaudois sans habitant, pourquoi pas 'gris' ?", tiersService.isSourcierGris(conjoint, null));

		final MenageCommun menage = (MenageCommun) tiersDAO.get(idCtbCouple.longValue());
		Assert.assertTrue("Couple avec for source vaudois sans habitant, pourquoi pas 'gris' ?", tiersService.isSourcierGris(menage, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisMenageMarieSeul() throws Exception {

		final MutableLong idCtbPrincipal = new MutableLong();
		final MutableLong idCtbCouple = new MutableLong();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				idCtbPrincipal.setValue(prn.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, null, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				final ForFiscalPrincipal ffp = addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);

				return null;
			}
		});

		final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(idCtbPrincipal.longValue());
		Assert.assertTrue("Couple avec for source vaudois sans habitant, pourquoi pas 'gris' ?", tiersService.isSourcierGris(principal, null));

		final MenageCommun menage = (MenageCommun) tiersDAO.get(idCtbCouple.longValue());
		Assert.assertTrue("Couple avec for source vaudois sans habitant, pourquoi pas 'gris' ?", tiersService.isSourcierGris(menage, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisMenageAvecUnMembreConnuAuCivil() throws Exception {

		final long noIndAchille = 12345L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndAchille, date(1950, 3, 24), "Achille", "Talon", true);
			}
		});

		final MutableLong idCtbPrincipal = new MutableLong();
		final MutableLong idCtbConjoint = new MutableLong();
		final MutableLong idCtbCouple = new MutableLong();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				prn.setNumeroIndividu(noIndAchille);
				idCtbPrincipal.setValue(prn.getNumero());

				final PersonnePhysique sec = addNonHabitant("Huguette", "Marcot", date(1950, 4, 12), Sexe.FEMININ);
				idCtbConjoint.setValue(sec.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, sec, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				final ForFiscalPrincipal ffp = addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);

				return null;
			}
		});

		final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(idCtbPrincipal.longValue());
		Assert.assertFalse("Couple avec for source vaudois et un membre habitant, pourquoi 'gris' ?", tiersService.isSourcierGris(principal, null));

		final PersonnePhysique conjoint = (PersonnePhysique) tiersDAO.get(idCtbConjoint.longValue());
		Assert.assertFalse("Couple avec for source vaudois et un membre habitant, pourquoi 'gris' ?", tiersService.isSourcierGris(conjoint, null));

		final MenageCommun menage = (MenageCommun) tiersDAO.get(idCtbCouple.longValue());
		Assert.assertFalse("Couple avec for source vaudois et un membre habitant, pourquoi 'gris' ?", tiersService.isSourcierGris(menage, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisMenageMarieSeulConnuAuCivil() throws Exception {

		final long noIndAchille = 12345L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndAchille, date(1950, 3, 24), "Achille", "Talon", true);
			}
		});

		final MutableLong idCtbPrincipal = new MutableLong();
		final MutableLong idCtbCouple = new MutableLong();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				prn.setNumeroIndividu(noIndAchille);
				idCtbPrincipal.setValue(prn.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, null, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				final ForFiscalPrincipal ffp = addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);

				return null;
			}
		});

		final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(idCtbPrincipal.longValue());
		Assert.assertFalse("Couple avec for source vaudois et un membre habitant, pourquoi 'gris' ?", tiersService.isSourcierGris(principal, null));

		final MenageCommun menage = (MenageCommun) tiersDAO.get(idCtbCouple.longValue());
		Assert.assertFalse("Couple avec for source vaudois et un membre habitant, pourquoi 'gris' ?", tiersService.isSourcierGris(menage, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetDebiteurPrestationImposable() throws Exception {
		loadDatabase("TiersServiceTest.xml");

		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(1234L);
		Contribuable ctb = tiersService.getContribuable(dpi);
		assertNotNull(ctb);
		assertEquals(new Long(6789L), ctb.getNumero());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddPeriodiciteDebiteurPrestationImposable() throws Exception {


		//Ajout d'une première periodicite'
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2011, 1, 1), null);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Periodicite periodicite = dpi.getPeriodiciteAt(date(2011, 1, 1));
				assertNotNull(periodicite);
				assertEquals(PeriodiciteDecompte.TRIMESTRIEL, periodicite.getPeriodiciteDecompte());
				return null;
			}
		});

		//Ajout d'une première periodicite'
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Periodicite periodicite = dpi.getPeriodiciteAt(date(2011, 1, 1));
				assertNotNull(periodicite);
				assertEquals(PeriodiciteDecompte.MENSUEL, periodicite.getPeriodiciteDecompte());
				return null;
			}
		});

		//Ajout d'une nouvelle périodicité mensuel mais nous sommes en 2012 !!!!

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Periodicite periodicite = dpi.getPeriodiciteAt(date(2012, 1, 1));
				assertNotNull(periodicite);
				assertEquals(date(2011, 1, 1), periodicite.getDateDebut());
				assertEquals(PeriodiciteDecompte.MENSUEL, periodicite.getPeriodiciteDecompte());
				return null;
			}
		});

		//Ajout d'une nouvelle périodicité trimestriel mais nous sommes en 2012 !!!!

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Periodicite periodicite = dpi.getPeriodiciteAt(date(2011, 1, 1));
				assertNotNull(periodicite);
				assertEquals(date(2011, 1, 1), periodicite.getDateDebut());
				assertEquals(date(2011, 12, 31), periodicite.getDateFin());
				assertEquals(PeriodiciteDecompte.MENSUEL, periodicite.getPeriodiciteDecompte());

				Periodicite periodiciteTri = dpi.getPeriodiciteAt(date(2012, 1, 1));
				assertNotNull(periodiciteTri);
				assertEquals(date(2012, 1, 1), periodiciteTri.getDateDebut());
				assertNull(periodiciteTri.getDateFin());
				assertEquals(PeriodiciteDecompte.TRIMESTRIEL, periodiciteTri.getPeriodiciteDecompte());
				return null;
			}
		});

		//(Je ne sais definitivement pas ce que je veux !!!!)
		// Ajout d'une nouvelle périodicité Mensuel mais nous sommes en 2012 !!!!
		//Situation: On a une mensuel avec une date de fin suivi d'une trimestriel, on decide de remplacer la trimestriel
		//par une mensuel.
		//Resultat: On doit se retrouver avec une periode mensuel qui debute en 2011
		//

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Periodicite periodicite = dpi.getPeriodiciteAt(date(2012, 1, 1));
				assertNotNull(periodicite);
				assertEquals(date(2011, 1, 1), periodicite.getDateDebut());
				assertNull(periodicite.getDateFin());
				assertEquals(PeriodiciteDecompte.MENSUEL, periodicite.getPeriodiciteDecompte());
				return null;
			}
		});
	}


	//test SIFISC-6845
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddForDebiteurPeriodePassee() throws Exception {
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addForDebiteur(dpi, date(2009,6,22), MotifFor.INDETERMINE, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS());



				return dpi.getNumero();
			}
		});

		//Ajout d'un second for avant le dernier for ouvert
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addForDebiteur(dpi, date(2009, 4, 1), MotifFor.INDETERMINE, date(2009, 6, 21), MotifFor.INDETERMINE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);


				final List<ForFiscal> forsFiscauxNonAnnules = dpi.getForsFiscauxNonAnnules(true);
				// on doit se retrouver avec 2 fors
				assertEquals(2, forsFiscauxNonAnnules.size());


				//le  fors  ajouté a été placé correctement avant le for existant
				assertEquals(date(2009,4,1),forsFiscauxNonAnnules.get(0).getDateDebut());
				assertEquals(date(2009,6,21),forsFiscauxNonAnnules.get(0).getDateFin());

				assertEquals(date(2009, 6, 22), forsFiscauxNonAnnules.get(1).getDateDebut());
				assertNull(forsFiscauxNonAnnules.get(1).getDateFin());

				return null;
			}
		});



		//Ajout d'un troisième for après le dernier for ouvert
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addForDebiteur(dpi, date(2010,4,1), MotifFor.INDETERMINE, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);

				//on doit se retrouver avec 3 fors
				final List<ForFiscal> forsFiscauxNonAnnules = dpi.getForsFiscauxNonAnnules(true);
				assertEquals(3, forsFiscauxNonAnnules.size());

				//Le dernier for ouvert se retrouve fermé à la veille de la date de début du nouveau for
				assertEquals(date(2009, 6, 22), forsFiscauxNonAnnules.get(1).getDateDebut());
				assertEquals(date(2010, 3, 31), forsFiscauxNonAnnules.get(1).getDateFin());

				//le  nouveau for a été placé correctement après le dernier for ouvert (qui est maintenant fermé !)
				assertEquals(date(2010, 4, 1), forsFiscauxNonAnnules.get(2).getDateDebut());

				assertNull(forsFiscauxNonAnnules.get(2).getDateFin());

				return null;
			}
		});

	}

	//test UNIREG-3041 AJout d'une nouvelle périodicité avec une périodicité existante l'année suivante et une
	//absence de LR sur l'année en cours
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddPeriodicitesAbsencesLR() throws Exception {
		//Ajout d'une première periodicite'
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;
		final int anneePrecedente = anneeReference - 1;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 1, 1), date(anneeReference, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M11, date(anneeSuivante, 1, 1), null);

				addForDebiteur(dpi, date(anneeReference - 1, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale fiscale2009 = addPeriodeFiscale(anneeReference - 1);

				addLR(dpi, date(anneePrecedente, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneePrecedente, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneePrecedente, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneePrecedente, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				RegDate dateDebut = tiersService.getDateDebutNouvellePeriodicite(dpi, PeriodiciteDecompte.UNIQUE);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M12, dateDebut, null);
				Periodicite periodicite = dpi.getDernierePeriodicite();
				assertEquals(dateDebut, periodicite.getDateDebut());
				assertEquals(PeriodiciteDecompte.UNIQUE, periodicite.getPeriodiciteDecompte());
				assertEquals(PeriodeDecompte.M12, periodicite.getPeriodeDecompte());


				return null;
			}
		});

	}

	/**
	 * [UNIREG-3011] Vérifie que la méthode addRapport ne provoque pas des erreurs de validation lorsque: <ul> <li>on ajoute un rapport entre deux tiers qui ne valident pas, et</li> <li>le rapport ajouté
	 * fait que les deux tiers sont ensuite valides</li> </ul>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddRapportEntreTiersInvalidesQuiSontValidesEnsuite() throws Exception {

		final RegDate dateMariage = date(1995, 1, 1);

		class Ids {
			long paul;
			long mc;
		}
		final Ids ids = new Ids();

		// on désactive temporairement la validation pour permettre de sauver un ménage-commun qui ne valide pas (manque les rapports d'appartenance ménage)
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Crée un non-habitant tout nu
				PersonnePhysique paul = addNonHabitant("Paul", "Ruccola", date(1968, 1, 1), Sexe.MASCULIN);
				ids.paul = paul.getId();

				// Crée un ménage commun avec un for principal ouvert
				MenageCommun mc = new MenageCommun();
				mc = hibernateTemplate.merge(mc);
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				ids.mc = mc.getId();

				return null;
			}
		});

		// On vérifie l'état initial : paul doit valider mais pas le ménage commun
		final PersonnePhysique paul = hibernateTemplate.get(PersonnePhysique.class, ids.paul);
		assertNotNull(paul);
		assertFalse(validationService.validate(paul).hasErrors()); // ok
		final MenageCommun mc = hibernateTemplate.get(MenageCommun.class, ids.mc);
		assertNotNull(mc);
		assertTrue(validationService.validate(mc).hasErrors()); // manque l'appartenance ménage

		// Maintenant, on ajoute un rapport d'appartenance ménage entre le non-habitant et le ménage-commun
		AppartenanceMenage rapport = new AppartenanceMenage();
		rapport.setDateDebut(dateMariage);

		// On ajoute le rapport : cet ajout ne doit pas déclencher la validation du ménage-commun entre le moment où :
		//  - le rapport est sauvé
		//  - le rapport est ajouté aux collections rapportSujets/rapportsObjets de la personne physique et du ménage-commun
		rapport = (AppartenanceMenage) tiersService.addRapport(rapport, paul, mc);

		// Si on arrive ici, c'est que tout c'est bien passé. On assert 2-3 trucs pour la forme.
		assertNotNull(rapport);
		assertEquals(paul.getNumero(), rapport.getSujetId());
		assertEquals(mc.getNumero(), rapport.getObjetId());
		assertFalse(validationService.validate(mc).hasErrors()); // plus d'erreur sur le ménage
	}

	@Test
	public void testAddPeriodiciteBeforAddFor() throws Exception {

		//Ajout d'une première periodicite et d'un for à la même date
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null);
				addForDebiteur(dpi, date(2010, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Periodicite periodicite = dpi.getDernierePeriodicite();
				assertNotNull(periodicite);
				ForDebiteurPrestationImposable forDebiteur = dpi.getDernierForDebiteur();
				assertEquals(periodicite.getDateDebut(), forDebiteur.getDateDebut());
				return null;
			}
		});

		//Ajout d'une première periodicite' et d'un for, le for ayant une date de début anterieur à celle de la périodicité
		final long dpiId2 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null);
				addForDebiteur(dpi, date(2009, 6, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});


		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId2);
				Periodicite periodicite = dpi.getDernierePeriodicite();
				assertNotNull(periodicite);
				ForDebiteurPrestationImposable forDebiteur = dpi.getDernierForDebiteur();
				assertEquals(forDebiteur.getDateDebut(), periodicite.getDateDebut());
				return null;
			}
		});


	}

	@Test
	public void testGetDateDebutValiditeNouvellePeriodiciteSansLR() throws Exception {

		//Ajout d'une première periodicite'
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur();
				addForDebiteur(dpi, date(2009, 11, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RegDate dateDebutPeriodicite = RegDate.get(2009, 6, 1);
				final DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
				assertEquals(date(2009, 11, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.MENSUEL));
				assertEquals(date(2009, 10, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.TRIMESTRIEL));
				assertEquals(date(2009, 7, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.SEMESTRIEL));
				assertEquals(date(2009, 1, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.ANNUEL));
				assertEquals(date(2009, 1, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.UNIQUE));
				return null;
			}
		});

	}

	@Test
	public void testGetDateDebutValiditeNouvellePeriodiciteAvecLR() throws Exception {

		//Ajout d'une première periodicite'
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur();
				addForDebiteur(dpi, date(2009, 6, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				final PeriodeFiscale fiscale = addPeriodeFiscale(2009);

				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
				assertEquals(date(2009, 10, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.MENSUEL));
				assertEquals(date(2009, 10, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.TRIMESTRIEL));
				assertEquals(date(2010, 1, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.SEMESTRIEL));
				assertEquals(date(2010, 1, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.ANNUEL));
				assertEquals(date(2010, 1, 1), tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.UNIQUE));
				return null;
			}
		});

	}

	@Test
	public void testGetDateDebutValiditeNouvellePeriodiciteSansLRSansFor() throws Exception {

		//Ajout d'une première periodicite'
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur();
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final int anneeCourante = RegDate.get().year();
				final RegDate dateDebutPeriodicite = RegDate.get(anneeCourante, 1, 1);
				final DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
				assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.ANNUEL));
				assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.TRIMESTRIEL));
				assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.SEMESTRIEL));
				assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.ANNUEL));
				assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur, PeriodiciteDecompte.UNIQUE));
				return null;
			}
		});

	}

	@Test
	public void testGetRaisonSocialeDebiteurSansTiersReferent() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				dpi.setNom1("Tartempion");
				dpi.setNom2("Toto");
				dpi.setComplementNom("Titi");
				return dpi.getNumero();
			}
		});

		// vérification de la raison sociale d'un débiteur sans tiers référent
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
				Assert.assertNotNull(raisonSociale);
				Assert.assertEquals(2, raisonSociale.size());
				Assert.assertEquals("Tartempion", raisonSociale.get(0));
				Assert.assertEquals("Toto", raisonSociale.get(1));
				return null;
			}
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentPM() throws Exception {

		// mise en place du service PM
		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				addPM(MockPersonneMorale.NestleSuisse);
			}
		});

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				dpi.setNom1("Tartempion");
				dpi.setNom2("Toto");
				dpi.setComplementNom("Titi");

				// on indique le tiers référent
				final Entreprise pm = addEntreprise(MockPersonneMorale.NestleSuisse.getNumeroEntreprise());
				tiersService.addContactImpotSource(dpi, pm, date(2009, 1, 1));

				return dpi.getNumero();
			}
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
				Assert.assertNotNull(raisonSociale);
				Assert.assertEquals(1, raisonSociale.size());
				Assert.assertEquals(MockPersonneMorale.NestleSuisse.getRaisonSociale1(), raisonSociale.get(0));
				Assert.assertNull(MockPersonneMorale.NestleSuisse.getRaisonSociale2());
				Assert.assertNull(MockPersonneMorale.NestleSuisse.getRaisonSociale3());
				return null;
			}
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentPersonnePhysique() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				dpi.setNom1("Tartempion");
				dpi.setNom2("Toto");
				dpi.setComplementNom("Titi");

				// on indique le tiers référent
				final PersonnePhysique pp = addNonHabitant("Albus", "Dumbledore", date(1956, 7, 4), Sexe.MASCULIN);
				tiersService.addContactImpotSource(dpi, pp, date(2009, 1, 1));

				return dpi.getNumero();
			}
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
				Assert.assertNotNull(raisonSociale);
				Assert.assertEquals(1, raisonSociale.size());
				Assert.assertEquals("Albus Dumbledore", raisonSociale.get(0));
				return null;
			}
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentMenageCommun() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				dpi.setNom1("Tartempion");
				dpi.setNom2("Toto");
				dpi.setComplementNom("Titi");

				// on indique le tiers référent
				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1956, 7, 4), Sexe.MASCULIN);
				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1956, 2, 4), Sexe.FEMININ);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(m, mme, date(2001, 9, 11), null);
				tiersService.addContactImpotSource(dpi, ensemble.getMenage(), date(2009, 1, 1));

				return dpi.getNumero();
			}
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
				Assert.assertNotNull(raisonSociale);
				Assert.assertEquals(2, raisonSociale.size());
				Assert.assertEquals("Vernon Dursley", raisonSociale.get(0));
				Assert.assertEquals("Petunia Dursley", raisonSociale.get(1));
				return null;
			}
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentAutreCommunaute() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				dpi.setNom1("Tartempion");
				dpi.setNom2("Toto");
				dpi.setComplementNom("Titi");

				// on indique le tiers référent
				final AutreCommunaute ac = addAutreCommunaute("Hogwards college");
				tiersService.addContactImpotSource(dpi, ac, date(2009, 1, 1));

				return dpi.getNumero();
			}
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
				Assert.assertNotNull(raisonSociale);
				Assert.assertEquals(1, raisonSociale.size());
				Assert.assertEquals("Hogwards college", raisonSociale.get(0));
				return null;
			}
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentCollectiviteAdministrative() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				dpi.setNom1("Tartempion");
				dpi.setNom2("Toto");
				dpi.setComplementNom("Titi");

				// on indique le tiers référent
				final CollectiviteAdministrative ca = addCollAdm(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud);
				tiersService.addContactImpotSource(dpi, ca, date(2009, 1, 1));

				return dpi.getNumero();
			}
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
				Assert.assertNotNull(raisonSociale);
				Assert.assertEquals(2, raisonSociale.size());
				Assert.assertEquals(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNomComplet1(), raisonSociale.get(0));
				Assert.assertEquals(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNomComplet2(), raisonSociale.get(1));
				Assert.assertNull(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNomComplet3());
				return null;
			}
		});
	}

	@Test
	public void testOuvertureForHorsCantonEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return pp.getNumero();
			}
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				tiersService.openForFiscalPrincipal(pp, date(2000, 5, 12), MotifRattachement.DOMICILE, MockCommune.Bale.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.ORDINAIRE,
						MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testOuvertureForHorsSuisseEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return pp.getNumero();
			}
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				tiersService.openForFiscalPrincipal(pp, date(2000, 5, 12), MotifRattachement.DOMICILE, MockPays.RoyaumeUni.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, ModeImposition.ORDINAIRE,
						MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testOuvertureForVaudoisEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return pp.getNumero();
			}
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				tiersService.openForFiscalPrincipal(pp, date(2000, 5, 12), MotifRattachement.DOMICILE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
						ModeImposition.ORDINAIRE, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationForVaudoisSansForRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du for vaudois -> le contribuable se retrouve sans aucun for (le blocage devrait alors être réactivé)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp);
				Assert.assertNull(pp.getDernierForFiscalPrincipal());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testAnnulationForVaudoisAvecForVaudoisRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ARRIVEE_HS, date(2005, 6, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.DEMENAGEMENT_VD, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du dernier for vaudois -> le contribuable a encore un for vaudois ouvert -> pas de nouveau blocage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp);

				final ForFiscalPrincipal autreFfp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(autreFfp);
				Assert.assertNull("Le for précédent n'a pas été ré-ouvert ?", autreFfp.getDateFin());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), (int) autreFfp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testAnnulationForVaudoisAvecForHorsCantonRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2005, 6, 1), MotifFor.ARRIVEE_HC, MockCommune.Bern, MotifRattachement.DOMICILE);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HC, MockCommune.Renens);
				addForSecondaire(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.CheseauxSurLausanne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du dernier for vaudois -> le contribuable n'a plus de for vaudois ouvert -> blocage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp);

				final ForFiscalPrincipal autreFfp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(autreFfp);
				Assert.assertNull("Le for précédent n'a pas été ré-ouvert ?", autreFfp.getDateFin());
				Assert.assertEquals(MockCommune.Bern.getNoOFS(), (int) autreFfp.getNumeroOfsAutoriteFiscale());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testAnnulationForVaudoisAvecForHorsSuisseRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2005, 6, 1), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				addForSecondaire(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.CheseauxSurLausanne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du dernier for vaudois -> le contribuable n'a plus de for vaudois ouvert -> blocage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp);

				final ForFiscalPrincipal autreFfp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(autreFfp);
				Assert.assertNull("Le for précédent n'a pas été ré-ouvert ?", autreFfp.getDateFin());
				Assert.assertEquals(MockPays.Allemagne.getNoOFS(), (int) autreFfp.getNumeroOfsAutoriteFiscale());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDecesEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				tiersService.closeAllForsFiscaux(pp, date(2010, 5, 23), MotifFor.VEUVAGE_DECES);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MockCommune.Renens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDepartHorsSuisseEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				tiersService.closeAllForsFiscaux(pp, date(2010, 5, 23), MotifFor.DEPART_HS);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MockCommune.Renens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDepartHorsCantonEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				tiersService.closeAllForsFiscaux(pp, date(2010, 5, 23), MotifFor.DEPART_HC);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MockCommune.Renens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDemenagementVaudoisEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(true);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour déménagement vaudois (avec ré-ouverture d'un autre for vaudois) -> déblocage
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				tiersService.closeForFiscalPrincipal(pp, date(2010, 5, 23), MotifFor.DEMENAGEMENT_VD);
				tiersService.addForPrincipal(pp, date(2010, 5, 24), MotifFor.DEMENAGEMENT_VD, null, null, MotifRattachement.DOMICILE, MockCommune.Bex.getNoOFS(),
						TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	public void testExtractionNumeroIndividuPrincipalCoupleComplet() throws Exception {

		final long noIndM = 123564L;
		final long noIndMme = 1231422L;
		final RegDate dateMariage = date(1995, 5, 1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndM, date(1970, 4, 12), "Petipoint", "Justin", true);
				final MockIndividu mme = addIndividu(noIndMme, date(1972, 12, 26), "Petipoint", "Martine", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale
		final long idMenage = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndM);
				final PersonnePhysique mme = addHabitant(noIndMme);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(m, mme, dateMariage, null);
				return ensemble.getMenage().getNumero();
			}
		});

		// test
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun menage = (MenageCommun) tiersDAO.get(idMenage);
				final Long noIndividu = tiersService.extractNumeroIndividuPrincipal(menage);
				Assert.assertNotNull(noIndividu);
				Assert.assertEquals(noIndM, (long) noIndividu);
				return null;
			}
		});
	}

	@Test
	public void testExtractionNumeroIndividuPrincipalCoupleAnnule() throws Exception {

		final long noIndM = 123564L;
		final long noIndMme = 1231422L;
		final RegDate dateMariage = date(1995, 5, 1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndM, date(1970, 4, 12), "Petipoint", "Justin", true);
				final MockIndividu mme = addIndividu(noIndMme, date(1972, 12, 26), "Petipoint", "Martine", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale
		final long idMenage = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndM);
				final PersonnePhysique mme = addHabitant(noIndMme);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(m, mme, dateMariage, null);

				// annulation des rapports entre tiers
				final Set<RapportEntreTiers> rapports = ensemble.getMenage().getRapportsObjet();
				for (RapportEntreTiers rapport : rapports) {
					if (rapport instanceof AppartenanceMenage) {
						rapport.setAnnule(true);
					}
				}

				return ensemble.getMenage().getNumero();
			}
		});

		// test
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun menage = (MenageCommun) tiersDAO.get(idMenage);
				final Long noIndividu = tiersService.extractNumeroIndividuPrincipal(menage);
				Assert.assertNull(noIndividu);
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddAndSaveForSurCommuneFaitiereFractions() throws Exception {
		final PersonnePhysique pp = addNonHabitant("Emilie", "Jolie", date(1980, 10, 4), Sexe.FEMININ);

		final ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(date(1998, 10, 4));
		f.setMotifOuverture(MotifFor.ARRIVEE_HS);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(MockCommune.LeLieu.getNoOFS());
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		try {
			tiersDAO.addAndSave(pp, f);
			Assert.fail("L'appel aurait dû sauter car la commune est une commune faîtière de fractions de communes");
		}
		catch (ValidationException e) {
			final String message =
					String.format("[E] Le for fiscal %s ne peut pas être ouvert sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas\n",
							f, MockCommune.LeLieu.getNomOfficiel(), MockCommune.LeLieu.getNoOFS());
			Assert.assertTrue(e.getMessage(), e.getMessage().endsWith(message));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsMineur() throws Exception {

		final long noIndividu = 12345L;
		final long noIndividuInconnu = 6789L;
		final RegDate dateNaissance = date(1996, 1, 7);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Pittet", "Martine", false);
				addIndividu(noIndividuInconnu, null, "Incognito", "Benito", true);
			}
		});

		{
			final PersonnePhysique pp = addHabitant(noIndividu);
			Assert.assertTrue(tiersService.isMineur(pp, dateNaissance));
			Assert.assertTrue(tiersService.isMineur(pp, dateNaissance.addYears(18).addDays(-1)));
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance.addYears(18)));
		}
		{
			final PersonnePhysique pp = addHabitant(noIndividuInconnu);
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance));
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance.addYears(18).addDays(-1)));
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance.addYears(18)));
		}
		{
			final PersonnePhysique pp = addNonHabitant("Martine", "Pittet", dateNaissance, Sexe.FEMININ);
			Assert.assertTrue(tiersService.isMineur(pp, dateNaissance));
			Assert.assertTrue(tiersService.isMineur(pp, dateNaissance.addYears(18).addDays(-1)));
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance.addYears(18)));
		}
		{
			final PersonnePhysique pp = addNonHabitant("Benito", "Incognito", null, Sexe.MASCULIN);
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance));
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance.addYears(18).addDays(-1)));
			Assert.assertFalse(tiersService.isMineur(pp, dateNaissance.addYears(18)));
		}
	}

	@Test
	public void testFermetureForDebiteurSansFermetureRT() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final RegDate dateFermetureFor = date(2009, 10, 31);

		// mise en place
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
				final PersonnePhysique pp2 = addNonHabitant("Weasley", "Ronnald", date(1980, 5, 12), Sexe.MASCULIN);

				addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
				addRapportPrestationImposable(dpi, pp2, dateDebut, dateFermetureFor.addMonths(-1), false);

				return dpi.getNumero();
			}
		});

		// fermeture du for débiteur
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				assertNotNull(dpi);

				final ForDebiteurPrestationImposable forDebiteur = dpi.getForDebiteurPrestationImposableAt(null);
				assertNotNull(forDebiteur);

				tiersService.closeForDebiteurPrestationImposable(dpi, forDebiteur, dateFermetureFor, MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, false);
				return null;
			}
		});

		// vérification des rapports de travail : ils ne doivent pas avoir bougé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				assertNotNull(dpi);

				final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
				assertNotNull(rapports);
				assertEquals(2, rapports.size());

				boolean foundOpen = false;
				boolean foundClosed = false;
				for (RapportEntreTiers r : rapports) {
					assertNotNull(r);
					assertInstanceOf(RapportPrestationImposable.class, r);
					assertEquals(dateDebut, r.getDateDebut());
					assertFalse(r.isAnnule());
					if (r.getDateFin() == null) {
						assertFalse(foundOpen);
						foundOpen = true;
					}
					else {
						assertFalse(foundClosed);
						assertEquals(dateFermetureFor.addMonths(-1), r.getDateFin());
						foundClosed = true;
					}
				}
				assertTrue(foundOpen);
				assertTrue(foundClosed);

				return null;
			}
		});
	}

	@Test
	public void testFermetureForDebiteurAvecFermetureRT() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final RegDate dateFermetureFor = date(2009, 10, 31);

		// mise en place
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
				final PersonnePhysique pp2 = addNonHabitant("Weasley", "Ronnald", date(1980, 5, 12), Sexe.MASCULIN);

				addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
				addRapportPrestationImposable(dpi, pp2, dateDebut, dateFermetureFor.addMonths(-1), false);
				addRapportPrestationImposable(dpi, pp2, dateFermetureFor.addMonths(1), null, false);

				return dpi.getNumero();
			}
		});

		// fermeture du for débiteur
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				assertNotNull(dpi);

				final ForDebiteurPrestationImposable forDebiteur = dpi.getForDebiteurPrestationImposableAt(null);
				assertNotNull(forDebiteur);

				tiersService.closeForDebiteurPrestationImposable(dpi, forDebiteur, dateFermetureFor, MotifFor.INDETERMINE, true);
				return null;
			}
		});

		// vérification des rapports de travail : ils ne doivent pas avoir bougé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				assertNotNull(dpi);

				final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
				assertNotNull(rapports);
				assertEquals(3, rapports.size());

				final List<RapportEntreTiers> rapportsTries = new ArrayList<>(rapports);
				Collections.sort(rapportsTries, new DateRangeComparator<RapportEntreTiers>());

				// rapport déjà fermé -> pas modifié
				{
					final RapportEntreTiers r = rapportsTries.get(0);
					assertNotNull(r);
					assertInstanceOf(RapportPrestationImposable.class, r);
					assertEquals(dateDebut, r.getDateDebut());
					assertEquals(dateFermetureFor.addMonths(-1), r.getDateFin());
					assertFalse(r.isAnnule());
				}
				// rapport précédemment ouvert à la date de fermeture -> fermé
				{
					final RapportEntreTiers r = rapportsTries.get(1);
					assertNotNull(r);
					assertInstanceOf(RapportPrestationImposable.class, r);
					assertEquals(dateDebut, r.getDateDebut());
					assertEquals(dateFermetureFor, r.getDateFin());
					assertFalse(r.isAnnule());
				}
				// rapport précédemment ouvert après la date de fermeture du for -> annulé
				{
					final RapportEntreTiers r = rapportsTries.get(2);
					assertNotNull(r);
					assertInstanceOf(RapportPrestationImposable.class, r);
					assertEquals(dateFermetureFor.addMonths(1), r.getDateDebut());
					assertNull(r.getDateFin());
					assertTrue(r.isAnnule());
				}
				return null;
			}
		});
	}

	/**
	 * [UNIREG-3168] problème à la création d'un couple qui reprend exactement les mêmes éléments qu'un couple annulé
	 */
	@Test
	public void testAjoutNouveauRapportMenageIdentiqueARapportAnnule() throws Exception {
		final RegDate dateMariage = RegDate.get(2000, 5, 2);

		class Ids {
			long idpp;
			long idmc;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", RegDate.get(1974, 8, 3), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);

				final Ids ids = new Ids();
				ids.idpp = pp.getNumero();
				ids.idmc = couple.getMenage().getNumero();
				return ids;
			}
		});

		// pour l'instant, le rapport existant n'est pas annulé -> on ne doit pas être capable d'en ajouter un entre les même personnes aux mêmes dates
		try {
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idmc);

					final AppartenanceMenage candidat = new AppartenanceMenage(dateMariage, null, pp, mc);
					tiersService.addRapport(candidat, pp, mc);
					Assert.fail("Aurait dû être refusé au prétexte que le rapport existe déjà...");
					return null;
				}
			});
		}
		catch (IllegalArgumentException e) {
			final String expectedMessage = String.format(
					"Impossible d'ajouter le rapport-objet de type %s pour la période %s sur le tiers n°%d car il existe déjà.",
					TypeRapportEntreTiers.APPARTENANCE_MENAGE, DateRangeHelper.toString(new DateRangeHelper.Range(dateMariage, null)), ids.idmc);
			Assert.assertEquals(expectedMessage, e.getMessage());
		}

		// mais si on l'annule, alors tout doit bien se passer
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idmc);
				final AppartenanceMenage am = (AppartenanceMenage) mc.getRapportObjetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				Assert.assertNotNull(am);
				am.setAnnule(true);

				final AppartenanceMenage candidat = new AppartenanceMenage(dateMariage, null, pp, mc);
				tiersService.addRapport(candidat, pp, mc);
				return null;
			}
		});
	}

	/**
	 * [UNIREG-3244] Vérifie qu'un événement de fin d'autorité parentale est envoyé lorsqu'un enfant devient majeure et qu'on ne connaît que sa mère.
	 */
	@Test
	public void testOuvertureForPrincipalPourMajoriteMereSeule() throws Exception {

		final long indMere = 1;
		final long indFils = 2;
		final RegDate dateNaissance = date(1993, 2, 8);

		// On crée la situation de départ : une mère et un fils mineur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu fils = addIndividu(indFils, dateNaissance, "Cognac", "Yvan", true);
				addLiensFiliation(fils, null, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long mere;
			Long fils;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				addParente(fils, mere, dateNaissance, null);
				return null;
			}
		});

		// Précondition : pas d'événement fiscal envoyé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fils = hibernateTemplate.get(PersonnePhysique.class, ids.fils);
				assertNotNull(fils);

				tiersService.openForFiscalPrincipal(fils, date(2011, 2, 8), MotifRattachement.DOMICILE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
						ModeImposition.ORDINAIRE, MotifFor.MAJORITE);
				return null;
			}
		});

		// On vérifie que il y a eu :
		// - un événement pour l'ouverture du for fiscal
		// - un événement pour la fin d'autorité parentale
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(2, events.size());

				final EvenementFiscalFor event0 = (EvenementFiscalFor) events.get(0);
				assertNotNull(event0);

				final EvenementFiscalFinAutoriteParentale event1 = (EvenementFiscalFinAutoriteParentale) events.get(1);
				assertNotNull(event1);
				assertEquals(ids.mere, event1.getTiers().getNumero());
				assertEquals(ids.fils, event1.getEnfant().getNumero());
				assertEquals(date(2011, 2, 8), event1.getDateEvenement());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-3244] Vérifie qu'aucun événement de fin d'autorité parentale n'est envoyé lorsqu'un enfant devient majeure et qu'on ne connaît que son père.
	 */
	@Test
	public void testOuvertureForPrincipalPourMajoritePereSeul() throws Exception {

		final long indPere = 1;
		final long indFils = 2;
		final RegDate dateNaissance = date(1993, 2, 8);

		// On crée la situation de départ : un père et un fils mineur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Raoul", true);
				MockIndividu fils = addIndividu(indFils, dateNaissance, "Cognac", "Yvan", true);
				addLiensFiliation(fils, pere, null, dateNaissance, null);
			}
		});

		class Ids {
			Long pere;
			Long fils;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				addParente(fils, pere, dateNaissance, null);
				return null;
			}
		});

		// Précondition : pas d'événement fiscal envoyé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fils = hibernateTemplate.get(PersonnePhysique.class, ids.fils);
				assertNotNull(fils);

				tiersService.openForFiscalPrincipal(fils, date(2011, 2, 8), MotifRattachement.DOMICILE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
						ModeImposition.ORDINAIRE, MotifFor.MAJORITE);
				return null;
			}
		});

		// On vérifie que :
		// - il y a eu un événement pour l'ouverture du for fiscal
		// - il n'y a pas eu d'événement pour la fin d'autorité parentale
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(1, events.size());

				final EvenementFiscalFor event0 = (EvenementFiscalFor) events.get(0);
				assertNotNull(event0);

				return null;
			}
		});
	}

	/**
	 * [UNIREG-3244] Vérifie qu'un événement de fin d'autorité parentale est envoyé lorsqu'un enfant devient majeure et qu'on ne connaît que sa mère et que cette dernière appartient à un ménage-commun.
	 */
	@Test
	public void testOuvertureForPrincipalPourMajoriteMereEnCouple() throws Exception {

		final long indMere = 1;
		final long indFils = 2;
		final RegDate dateNaissance = date(1993, 2, 8);

		// On crée la situation de départ : une mère mariée seule et un fils mineur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu fils = addIndividu(indFils, dateNaissance, "Cognac", "Yvan", true);
				addLiensFiliation(fils, null, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long mere;
			Long menage;
			Long fils;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(mere, null, date(1990, 1, 1), null);
				ids.menage = ensemble.getMenage().getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				addParente(fils, mere, dateNaissance, null);
				return null;
			}
		});

		// Précondition : pas d'événement fiscal envoyé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fils = hibernateTemplate.get(PersonnePhysique.class, ids.fils);
				assertNotNull(fils);

				tiersService.openForFiscalPrincipal(fils, date(2011, 2, 8), MotifRattachement.DOMICILE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
						ModeImposition.ORDINAIRE, MotifFor.MAJORITE);
				return null;
			}
		});

		// On vérifie que il y a eu :
		// - un événement pour l'ouverture du for fiscal
		// - un événement pour la fin d'autorité parentale associé au ménage commun de la mère
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(2, events.size());

				final EvenementFiscalFor event0 = (EvenementFiscalFor) events.get(0);
				assertNotNull(event0);

				final EvenementFiscalFinAutoriteParentale event1 = (EvenementFiscalFinAutoriteParentale) events.get(1);
				assertNotNull(event1);
				assertEquals(ids.menage, event1.getTiers().getNumero());
				assertEquals(ids.fils, event1.getEnfant().getNumero());
				assertEquals(date(2011, 2, 8), event1.getDateEvenement());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-3244] Vérifie qu'aucun événement de fin d'autorité parentale n'est envoyé lorsqu'un enfant devient majeur alors qu'il est déjà été assujetti (pour cause de fortune personnelle, par
	 * exemple).
	 */
	@Test
	public void testOuvertureForPrincipalPourMajoriteEnfantDejaAssujettiDansLePasse() throws Exception {

		final long indMere = 1;
		final long indFils = 2;
		final RegDate dateNaissance = date(1993, 2, 8);

		// On crée la situation de départ : une mère et un fils mineur qui possède un immeuble
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu fils = addIndividu(indFils, dateNaissance, "Cognac", "Yvan", true);
				addLiensFiliation(fils, null, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long mere;
			Long fils;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				addParente(fils, mere, dateNaissance, null);
				addForPrincipal(fils, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2000, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny);
				addForSecondaire(fils, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2000, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return null;
			}
		});

		// Précondition : pas d'événement fiscal envoyé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fils = hibernateTemplate.get(PersonnePhysique.class, ids.fils);
				assertNotNull(fils);

				tiersService.openForFiscalPrincipal(fils, date(2011, 2, 8), MotifRattachement.DOMICILE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
						ModeImposition.ORDINAIRE, MotifFor.MAJORITE);
				return null;
			}
		});

		// On vérifie que  :
		// - il y a eu un événement pour l'ouverture du for fiscal
		// - il n'y a pas eu d'événement pour la fin d'autorité parentale
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(1, events.size());

				final EvenementFiscalFor event0 = (EvenementFiscalFor) events.get(0);
				assertNotNull(event0);
				return null;
			}
		});
	}

	// un couple avec une fille  majeur et un enfant mineur
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationMenage() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(1988, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeure
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();

		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fille, mere, dateNaissanceFille, null);
				addParente(fille, pere, dateNaissanceFille, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(1, enfantsForDeclaration.size());
		assertEquals(ids.fils, enfantsForDeclaration.get(0).getNumero());
	}


	//Couple avec Enfant décédé et 1 enfant mineur
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationEnfantDecede() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);
		final RegDate dateDecesFille = date(2011, 2, 2);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);
				fille.setDateDeces(dateDecesFille);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, dateDecesFille);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();

		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fille, mere, dateNaissanceFille, dateDecesFille);
				addParente(fille, pere, dateNaissanceFille, dateDecesFille);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(1, enfantsForDeclaration.size());
		assertEquals(ids.fils, enfantsForDeclaration.get(0).getNumero());
	}

	//Couple avec Enfant possédant 1 seul lien de filiation
	@Test
	@Transactional
	public void testGetEnfantsForDeclaration1Filiation() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateMariage = date(1985, 1, 1);
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);
				marieIndividus(pere, mere, dateMariage);
				addLiensFiliation(fils, null, mere, dateNaissanceFils, null);       // <-- pas de filiation vers le père
				addLiensFiliation(fille, null, mere, dateNaissanceFille, null);     // <-- pas de filiation vers le père

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();

		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, mere, dateNaissanceFille, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, dateMariage, null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(2, enfantsForDeclaration.size());
	}


	//Couple avec Enfant  ayant des EGID different
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationEGIDDifferent() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentRouteDeLausanne, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(0, enfantsForDeclaration.size());
	}

	//Couple avec Enfant  ayant des EWID differents (le garage ?)
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationEWIDDifferent() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 2, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(1, enfantsForDeclaration.size());
		assertEquals((Long) ids.fille, enfantsForDeclaration.get(0).getNumero());
	}

	//Couple avec Enfant  ayant des EWID absents (oubli de saisie ?)
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationEWIDAbsent() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(1, enfantsForDeclaration.size());
		assertEquals((Long) ids.fille, enfantsForDeclaration.get(0).getNumero());
	}

	//SIFISC-3053
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationPereSansDomicile() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(0, enfantsForDeclaration.size());
	}


	//SIFISC-3053
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationEnfantSansDomicile() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(1, enfantsForDeclaration.size());
	}


	//2 parents non en couple avec le même EGID.
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationMemeEGID() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				return ids.mere;
			}
		});


		final Contribuable ctbPere = (Contribuable) tiersDAO.get(ids.pere);
		List<PersonnePhysique> enfantsForDeclarationPere = tiersService.getEnfantsForDeclaration(ctbPere, date(2011, 12, 31));
		assertNotNull(enfantsForDeclarationPere);
		assertEquals(0, enfantsForDeclarationPere.size());

		final Contribuable ctbMere = (Contribuable) tiersDAO.get(ids.mere);
		List<PersonnePhysique> enfantsForDeclarationMere = tiersService.getEnfantsForDeclaration(ctbMere, date(2011, 12, 31));
		assertNotNull(enfantsForDeclarationMere);
		assertEquals(0, enfantsForDeclarationMere.size());
	}

	//2 parents non en couple avec le même EGID.
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationMereSeule() throws Exception {

		final long indMere = 1;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, null, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, null, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, mere, dateNaissanceFille, null);

				return ids.mere;
			}
		});


		final Contribuable ctbMere = (Contribuable) tiersDAO.get(ids.mere);
		final List<PersonnePhysique> enfantsForDeclarationMere = tiersService.getEnfantsForDeclaration(ctbMere, date(2011, 12, 31));
		assertNotNull(enfantsForDeclarationMere);
		assertEquals(2, enfantsForDeclarationMere.size());
	}

	//l'egid des 2 parents est identiques
	@Test
	@Transactional
	public void testIsEgidAutreParentDifferent() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();

		final long idCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				return ids.mere;
			}
		});


		final PersonnePhysique pere = (PersonnePhysique) tiersDAO.get(ids.pere);
		final PersonnePhysique mere = (PersonnePhysique) tiersDAO.get(ids.mere);
		final PersonnePhysique fille = (PersonnePhysique) tiersDAO.get(ids.fille);
		AdresseGenerique adressePere = adresseService.getAdresseFiscale(pere, TypeAdresseFiscale.DOMICILE, date(2011, 12, 31), false);
		assertFalse(TiersHelper.hasParentsAvecEgidEwidDifferents(fille, pere, adressePere, date(2011, 12, 31), adresseService, tiersService));
	}

	//l'egid des 2 parents est differents
	@Test
	@Transactional
	public void testIsEgidAutreParentDifferent2() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Villette.BatimentCheminDesGranges, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				return ids.mere;
			}
		});


		final PersonnePhysique pere = (PersonnePhysique) tiersDAO.get(ids.pere);
		final PersonnePhysique mere = (PersonnePhysique) tiersDAO.get(ids.mere);
		final PersonnePhysique fille = (PersonnePhysique) tiersDAO.get(ids.fille);
		AdresseGenerique adressePere = adresseService.getAdresseFiscale(pere, TypeAdresseFiscale.DOMICILE, date(2011, 12, 31), false);
		assertTrue(TiersHelper.hasParentsAvecEgidEwidDifferents(fille, pere, adressePere, date(2011, 12, 31), adresseService, tiersService));
	}

	//un parent à un egid, l'autre non
	@Test
	@Transactional
	public void testIsEgidAutreParentDifferent3() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.RueTrevelin, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFils, null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		final long idCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);

				return ids.mere;
			}
		});


		final PersonnePhysique pere = (PersonnePhysique) tiersDAO.get(ids.pere);
		final PersonnePhysique mere = (PersonnePhysique) tiersDAO.get(ids.mere);
		final PersonnePhysique fille = (PersonnePhysique) tiersDAO.get(ids.fille);
		AdresseGenerique adressePere = adresseService.getAdresseFiscale(pere, TypeAdresseFiscale.DOMICILE, date(2011, 12, 31), false);
		assertTrue(TiersHelper.hasParentsAvecEgidEwidDifferents(fille, pere, adressePere, date(2011, 12, 31), adresseService, tiersService));
	}

	// [SIFISC-2703]  Tri des enfants dans l'ordre décroissant
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationMenageOrdreCroissant() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final long indFille2 = 5;

		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2004, 2, 8);
		final RegDate dateNaissanceFille2 = date(2006, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Cognac", "Josette", false);
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);
				MockIndividu fille2 = addIndividu(indFille2, dateNaissanceFille2, "Cognac", "Lucie", false);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fille2, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);

				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(fille, pere, mere, dateNaissanceFille, null);
				addLiensFiliation(fille2, pere, mere, dateNaissanceFille2, null);
			}
		});

		class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
			Long fille2;
		}
		final Ids ids = new Ids();

		final long idMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();
				final PersonnePhysique fille2 = addHabitant(indFille2);
				ids.fille2 = fille2.getId();
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1985, 1, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(1998, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fils, mere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);
				addParente(fille, mere, dateNaissanceFille, null);
				addParente(fille2, pere, dateNaissanceFille2, null);
				addParente(fille2, mere, dateNaissanceFille2, null);

				return mc.getNumero();
			}
		});


		final Contribuable menageCommun = (Contribuable) tiersDAO.get(idMenage);
		List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(menageCommun, date(2011, 12, 31));
		assertNotNull(enfantsForDeclaration);
		assertEquals(3, enfantsForDeclaration.size());
		assertEquals(ids.fils, enfantsForDeclaration.get(0).getNumero());
		assertEquals(ids.fille, enfantsForDeclaration.get(1).getNumero());
		assertEquals(ids.fille2, enfantsForDeclaration.get(2).getNumero());
	}


	@Test
	public void testTraiterReOuvertureForDebiteur() throws Exception {
		class Ids {
			Long idFor1;
			Long idFor2;
			Long michelId;
			Long jeanId;
		}
		final Ids ids = new Ids();
		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				ids.idFor1 = addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, date(2010, 12, 31), MotifFor.INDETERMINE, MockCommune.Bussigny).getId();
				ids.idFor2 = addForDebiteur(dpi, date(2011, 1, 1), MotifFor.INDETERMINE, date(2011, 9, 30), MotifFor.INDETERMINE, MockCommune.Aubonne).getId();
				PersonnePhysique jean = addNonHabitant("Jean", "zep", null, Sexe.MASCULIN);
				PersonnePhysique michel = addNonHabitant("michel", "zep", null, Sexe.MASCULIN);
				ids.jeanId = jean.getNumero();
				ids.michelId = michel.getNumero();
				addRapportPrestationImposable(dpi, jean, date(2009, 1, 1), date(2009, 5, 12), false);
				addRapportPrestationImposable(dpi, michel, date(2009, 1, 1), date(2011, 9, 30), false);
				return dpi.getNumero();

			}
		});

		// Reouverture du second for
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				ForFiscal forDebiteur = forFiscalDAO.get(ids.idFor2);
				tiersService.reouvrirForDebiteur((ForDebiteurPrestationImposable) forDebiteur);
				return null;
			}
		});


		// vérification du for et du rapport ouvert
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Assert.assertNull(dpi.getDernierForDebiteur().getDateFin());
				final RapportEntreTiers rapportPrestation = dpi.getRapportObjetValidAt(RegDate.get(), TypeRapportEntreTiers.PRESTATION_IMPOSABLE);
				Assert.assertNotNull(rapportPrestation);
				Assert.assertEquals(ids.michelId, rapportPrestation.getSujetId());
				return null;
			}
		});
	}

	@Test
	public void testCloseAppartenanceMenageAvantOuverture() throws Exception {

		final long noIndividuLui = 48154846L;
		final long noIndividuElle = 451248463163L;
		final RegDate dateMariage = date(2010, 6, 12);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, date(1980, 10, 25), "Petitpoint", "Justin", true);
				final MockIndividu elle = addIndividu(noIndividuElle, date(1990, 12, 7), "Couchetoila", "Marie", false);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		// création du ménage commun
		final long idMenage = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				return mc.getNumero();
			}
		});

		// tentative de séparation à la veille du mariage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateMariage);
				try {
					tiersService.closeAppartenanceMenage(couple.getPrincipal(), couple.getMenage(), dateMariage.getOneDayBefore());
					Assert.fail("La fermeture du rapport d'appartenance ménage aurait dû être refusée");
				}
				catch (RapportEntreTiersException e) {
					Assert.assertEquals(String.format("On ne peut fermer le rapport d'appartenance ménage avant sa date de début (%s)", RegDateHelper.dateToDisplayString(dateMariage)),
					                    e.getMessage());
				}
				return null;
			}
		});
	}

	/**
	 * [SIFISC-5279] annulation d'un surcharge d'adresse courrier HC au moment de l'annulation d'un for principal HS sourcier ouvert pour motif départ HS
	 */
	@Test
	public void testAnnulationForAvecSurchargeCourrier() throws Exception {

		final long noIndividu = 43256734456243562L;
		final RegDate dateDebutForMixteVaudois = date(2009, 1, 1);
		final RegDate dateDebutResidenceHS = date(2011, 8, 1);
		final RegDate dateFinResidenceVD = dateDebutResidenceHS.getOneDayBefore();
		final RegDate dateDebutSurchargeCourrier = date(2012, 1, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1980, 8, 12);
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Tartempion", "Ernestine", false);
				addNationalite(individu, MockPays.France, dateNaissance, null);
				// msi 30.10.2012, dans le cas JIRA l'individu quitte la Suisse et possède une nouvelle adresse en France. Mais suite au découplage
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateDebutForMixteVaudois, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, dateDebutForMixteVaudois, MotifFor.ARRIVEE_HS, dateFinResidenceVD, MotifFor.DEPART_HS, MockCommune.Echallens, ModeImposition.MIXTE_137_2);
				addForPrincipal(pp, dateDebutResidenceHS, MotifFor.DEPART_HS, null, null, MockPays.France, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateDebutSurchargeCourrier, null, MockRue.Neuchatel.RueDesBeauxArts);
				return pp.getNumero();
			}
		});

		// annulation du for source...
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				assertNull(ffp.getDateFin());

				final Set<AdresseTiers> adresses = pp.getAdressesTiers();
				assertNotNull(adresses);
				assertEquals(1, adresses.size());
				final AdresseTiers adresse = adresses.iterator().next();
				assertNotNull(adresse);
				assertFalse(adresse.isAnnule());

				tiersService.annuleForFiscal(ffp);
				// l'update du flag habitant est maintenant découplée des fors fiscaux -> ajouté l'appel explicitement dans le test
				tiersService.updateHabitantStatus(pp, pp.getNumeroIndividu(), dateFinResidenceVD, null);
				return null;
			}
		});

		// et maintenant, le résultat... l'adresse doit bien avoir été annulée (elle est temporaire et commencer après la date de début
		// du for que l'on ré-ouvre + passage de non-habitant à habitant)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final Set<AdresseTiers> adresses = pp.getAdressesTiers();
				assertNotNull(adresses);
				assertEquals(1, adresses.size());
				final AdresseTiers adresse = adresses.iterator().next();
				assertNotNull(adresse);
				assertTrue(adresse.isAnnule());
				assertNotNull(adresse.getAnnulationUser());     // cet attribut n'était pas rempli avant !
				return null;
			}
		});
	}

	/**
	 * [SIFISC-5970] On s'assure que la méthode 'isDomicileDansLeCanton' fonctionne avec les adresses du style RegPP (= avec adresse principales hors-canton et hors-Suisse, mais sans les informations de
	 * localisation)
	 */
	@Test
	public void testIsDomicileDansLeCantonRegPPStyle() throws Exception {

		final long NON_MIGRE = 66666;
		final long JEAN = 12345;
		final long ARNOLD = 12121;
		final long GUDRUN = 23232;
		final long LOLA = 44444;
		final long HEIDI = 54321;
		final long URSULE = 404;
		final long HANS = 90909;
		final long URS = 35353;
		final long SHIRLEY = 52525;

		final long MOUSSA = 34343;
		final long BRAHIM = 78787;

		final MockServiceCivil civil = new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(JEAN, date(1965, 3, 24), "Jean", "Strudelopom", Sexe.MASCULIN);

				MockIndividu arnold = addIndividu(ARNOLD, date(1965, 3, 24), "Arnold", "Strudelopoir", Sexe.MASCULIN);
				addAdresse(arnold, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), null);

				MockIndividu gudrun = addIndividu(GUDRUN, date(1965, 3, 24), "Gudrun", "Strudeloprün", Sexe.FEMININ);
				addAdresse(gudrun, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), date(1990, 12, 31));

				MockIndividu lola = addIndividu(LOLA, date(1965, 3, 24), "Lola", "Strudelosuspentonvol", Sexe.FEMININ);
				addAdresse(lola, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1989, 12, 31));
				addAdresse(lola, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(1990, 1, 1), null);

				MockIndividu heidi = addIndividu(HEIDI, date(1965, 3, 24), "Heidi", "Strudelostrudel", Sexe.FEMININ);
				addAdresse(heidi, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1970, 12, 31));
				addAdresse(heidi, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), date(1990, 12, 31));

				MockIndividu ursule = addIndividu(URSULE, date(1965, 3, 24), "Ursule", "Strudelopustul", Sexe.FEMININ);
				addAdresse(ursule, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1970, 12, 31));

				MockIndividu hans = addIndividu(HANS, date(1965, 3, 24), "Hans", "Strudelodesespoir", Sexe.MASCULIN);
				addAdresse(hans, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.VoltaStrasse, null, date(1965, 3, 24), null);

				MockIndividu urs = addIndividu(URS, date(1965, 3, 24), "Urs", "Strudelopoil", Sexe.MASCULIN);
				addAdresse(urs, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.VoltaStrasse, null, date(1965, 3, 24), date(1989, 12, 31));
				addAdresse(urs, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), null);

				MockIndividu shirley = addIndividu(SHIRLEY, date(1965, 3, 24), "Shirley", "Strudelocottens", Sexe.MASCULIN);
				addAdresse(shirley, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1989, 12, 31));
				addAdresse(shirley, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.VoltaStrasse, null, date(1990, 1, 1), null);

				MockIndividu moussa = addIndividu(MOUSSA, date(1965, 3, 24), "Moussa", "Ka", Sexe.MASCULIN);
				addAdresse(moussa, TypeAdresseCivil.PRINCIPALE, null, "Deutsche Strasse", "22222 Brouf", MockPays.Gibraltar, date(1965, 3, 24), null);

				MockIndividu brahim = addIndividu(BRAHIM, date(1965, 3, 24), "Brahim", "Dupontet", Sexe.MASCULIN);
				addAdresse(brahim, TypeAdresseCivil.PRINCIPALE, null, "Deutsche Strasse", "22222 Brouf", MockPays.Gibraltar, date(1965, 3, 24), date(1989, 12, 31));
				addAdresse(brahim, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), null);
			}
		};

		final TiersServiceImpl service = new TiersServiceImpl();
		service.setServiceCivilService(new ServiceCivilImpl(serviceInfra, civil));
		service.setServiceInfra(serviceInfra);

		// Une personne inconnue au contrôle des habitants (non-habitant) quelconque
		assertFalse(service.isDomicileDansLeCanton(new PersonnePhysique(), null));

		// Une personne avec un numéro d'individu mais introuvable dans le registre civil (par exemple, personne non-migrée par RcPers)
		assertNull(service.isDomicileDansLeCanton(new PersonnePhysique(NON_MIGRE), null));

		// Jean qui ne possède aucune adresse de domicile
		final PersonnePhysique jean = new PersonnePhysique(JEAN);
		assertFalse(service.isDomicileDansLeCanton(jean, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(jean, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(jean, null));

		// Arnold qui a toujours habité dans le canton depuis sa naissance
		final PersonnePhysique arnold = new PersonnePhysique(ARNOLD);
		assertFalse(service.isDomicileDansLeCanton(arnold, date(1965, 2, 23))); // avant sa naissance...
		assertTrue(service.isDomicileDansLeCanton(arnold, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(arnold, null));

		// Gudrun qui a habité dans le canton durant l'année 1990
		final PersonnePhysique gudrun = new PersonnePhysique(GUDRUN);
		assertFalse(service.isDomicileDansLeCanton(gudrun, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(gudrun, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(gudrun, date(1990, 5, 28)));
		assertTrue(service.isDomicileDansLeCanton(gudrun, date(1990, 12, 31)));
		assertFalse(service.isDomicileDansLeCanton(gudrun, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(gudrun, null));

		// Lola qui a toujours habité dans le canton et à déménagé une fois
		final PersonnePhysique lola = new PersonnePhysique(LOLA);
		assertFalse(service.isDomicileDansLeCanton(lola, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(1968, 5, 1)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(1989, 12, 31)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(lola, null));

		// Heidi qui a habité dans le canton lors de deux périodes séparées
		final PersonnePhysique heidi = new PersonnePhysique(HEIDI);
		assertFalse(service.isDomicileDansLeCanton(heidi, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1968, 5, 1)));
		assertFalse(service.isDomicileDansLeCanton(heidi, date(1980, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1990, 5, 28)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1990, 12, 31)));
		assertFalse(service.isDomicileDansLeCanton(heidi, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(heidi, null));

		// Ursule qui a habité dans le canton et qui est parti pour une destination vaudoise mais n'a pas encore enregistré son arrivée
		final PersonnePhysique ursule = new PersonnePhysique(URSULE);
		assertFalse(service.isDomicileDansLeCanton(ursule, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(ursule, date(1968, 5, 1)));
		assertTrue(service.isDomicileDansLeCanton(ursule, date(1970, 12, 31)));
		assertFalse(service.isDomicileDansLeCanton(ursule, date(1971, 1, 1)));
		assertFalse(service.isDomicileDansLeCanton(ursule, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(ursule, null));

		// Hans qui habite hors-canton depuis toujours
		final PersonnePhysique hans = new PersonnePhysique(HANS);
		assertFalse(service.isDomicileDansLeCanton(hans, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(hans, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(hans, null));

		// Shirley qui est née à Cottens (VD) et qui est - charogne de gamine ! - partie à Zürich le 1er janvier 1990
		final PersonnePhysique shirley = new PersonnePhysique(SHIRLEY);
		assertTrue(service.isDomicileDansLeCanton(shirley, date(1970, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(shirley, date(1989, 12, 31)));
		assertFalse(service.isDomicileDansLeCanton(shirley, date(1990, 1, 1)));
		assertFalse(service.isDomicileDansLeCanton(shirley, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(shirley, null));

		// Urs qui est né hors-canton et qui est arrivé dans le canton le 1er janvier 1990
		final PersonnePhysique urs = new PersonnePhysique(URS);
		assertFalse(service.isDomicileDansLeCanton(urs, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(urs, date(1989, 12, 31)));
		assertTrue(service.isDomicileDansLeCanton(urs, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(urs, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(urs, null));

		// Moussa qui habite hors-Suisse depuis toujours
		final PersonnePhysique moussa = new PersonnePhysique(MOUSSA);
		assertFalse(service.isDomicileDansLeCanton(moussa, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(moussa, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(moussa, null));

		// Brahim qui habitait hors-Suisse et qui est arrivé le 1er janvier 1990 dans le canton
		final PersonnePhysique brahim = new PersonnePhysique(BRAHIM);
		assertFalse(service.isDomicileDansLeCanton(brahim, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(brahim, date(1989, 12, 31)));
		assertTrue(service.isDomicileDansLeCanton(brahim, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(brahim, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(brahim, null));
	}

	/**
	 * [SIFISC-5970] On s'assure que la méthode 'isDomicileDansLeCanton' fonctionne avec les adresses du style RcPers (= sans adresse principales hors-canton et hors-Suisse, mais avec les informations de
	 * localisation)
	 */
	@Test
	public void testIsDomicileDansLeCantonRcPersStyle() throws Exception {

		final long NON_MIGRE = 66666;
		final long JEAN = 12345;
		final long ARNOLD = 12121;
		final long GUDRUN = 23232;
		final long LOLA = 44444;
		final long HEIDI = 54321;
		final long URSULE = 404;
		final long TRUDI = 56739;
//		final long HANS = 90909;
		final long URS = 35353;
		final long SHIRLEY = 52525;

//		final long MOUSSA = 34343;
		final long BRAHIM = 78787;

		final MockServiceCivil civil = new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(JEAN, date(1965, 3, 24), "Jean", "Strudelopom", Sexe.MASCULIN);

				MockIndividu arnold = addIndividu(ARNOLD, date(1965, 3, 24), "Arnold", "Strudelopoir", Sexe.MASCULIN);
				addAdresse(arnold, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), null);

				MockIndividu gudrun = addIndividu(GUDRUN, date(1965, 3, 24), "Gudrun", "Strudeloprün", Sexe.FEMININ);
				addAdresse(gudrun, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), date(1990, 12, 31));

				MockIndividu lola = addIndividu(LOLA, date(1965, 3, 24), "Lola", "Strudelosuspentonvol", Sexe.FEMININ);
				MockAdresse a0 = addAdresse(lola, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1989, 12, 31));
				a0.setLocalisationSuivante(newLocalisation(MockCommune.Bussigny));
				MockAdresse a1 = addAdresse(lola, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(1990, 1, 1), null);
				a1.setLocalisationPrecedente(newLocalisation(MockCommune.Chamblon));

				MockIndividu heidi = addIndividu(HEIDI, date(1965, 3, 24), "Heidi", "Strudelostrudel", Sexe.FEMININ);
				addAdresse(heidi, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1970, 12, 31));
				addAdresse(heidi, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), date(1990, 12, 31));

				MockIndividu ursule = addIndividu(URSULE, date(1965, 3, 24), "Ursule", "Strudelopustul", Sexe.FEMININ);
				MockAdresse a2 = addAdresse(ursule, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1970, 12, 31));
				a2.setLocalisationSuivante(newLocalisation(MockCommune.Vallorbe));

				MockIndividu trudi = addIndividu(TRUDI, date(1965, 3, 24), "Trudi", "Strudelopignon", Sexe.FEMININ);
				MockAdresse a3 = addAdresse(trudi, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), null);
				a3.setLocalisationPrecedente(newLocalisation(MockCommune.Vallorbe));

				// Les non-habitants n'existent pas dans RcPers
//				MockIndividu hans = addIndividu(HANS, date(1965, 3, 24), "Hans", "Strudelodesespoir", Sexe.MASCULIN);
//				addAdresse(hans, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.VoltaStrasse, null, date(1965, 3, 24), null);

				MockIndividu urs = addIndividu(URS, date(1965, 3, 24), "Urs", "Strudelopoil", Sexe.MASCULIN);
				MockAdresse a4 = addAdresse(urs, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), null);
				a4.setLocalisationPrecedente(newLocalisation(MockCommune.Zurich));

				MockIndividu shirley = addIndividu(SHIRLEY, date(1965, 3, 24), "Shirley", "Strudelocottens", Sexe.MASCULIN);
				MockAdresse a5 = addAdresse(shirley, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1965, 3, 24), date(1989, 12, 31));
				a5.setLocalisationSuivante(newLocalisation(MockCommune.Zurich));

				// Les non-habitants n'existent pas dans RcPers
//				MockIndividu moussa = addIndividu(MOUSSA, date(1965, 3, 24), "Moussa", "Ka", Sexe.MASCULIN);
//				addAdresse(moussa, TypeAdresseCivil.PRINCIPALE, null, "Deutsche Strasse", "22222 Brouf", MockPays.Gibraltar, date(1965, 3, 24), null);

				MockIndividu brahim = addIndividu(BRAHIM, date(1965, 3, 24), "Brahim", "Dupontet", Sexe.MASCULIN);
				MockAdresse a6 = addAdresse(brahim, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, date(1990, 1, 1), null);
				a6.setLocalisationPrecedente(newLocalisation(MockPays.Gibraltar));
			}
		};

		final TiersServiceImpl service = new TiersServiceImpl();
		service.setServiceCivilService(new ServiceCivilImpl(serviceInfra, civil));
		service.setServiceInfra(serviceInfra);

		// Une personne inconnue au contrôle des habitants (non-habitant) quelconque
		assertFalse(service.isDomicileDansLeCanton(new PersonnePhysique(), null));

		// Une personne avec un numéro d'individu mais introuvable dans le registre civil (par exemple, personne non-migrée par RcPers)
		assertNull(service.isDomicileDansLeCanton(new PersonnePhysique(NON_MIGRE), null));

		// Jean qui ne possède aucune adresse de domicile
		final PersonnePhysique jean = new PersonnePhysique(JEAN);
		assertFalse(service.isDomicileDansLeCanton(jean, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(jean, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(jean, null));

		// Arnold qui a toujours habité dans le canton depuis sa naissance
		final PersonnePhysique arnold = new PersonnePhysique(ARNOLD);
		assertFalse(service.isDomicileDansLeCanton(arnold, date(1965, 2, 23))); // avant sa naissance...
		assertTrue(service.isDomicileDansLeCanton(arnold, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(arnold, null));

		// Gudrun qui a habité dans le canton durant l'année 1990
		final PersonnePhysique gudrun = new PersonnePhysique(GUDRUN);
		assertFalse(service.isDomicileDansLeCanton(gudrun, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(gudrun, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(gudrun, date(1990, 5, 28)));
		assertTrue(service.isDomicileDansLeCanton(gudrun, date(1990, 12, 31)));
		assertFalse(service.isDomicileDansLeCanton(gudrun, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(gudrun, null));

		// Lola qui a toujours habité dans le canton et à déménagé une fois
		final PersonnePhysique lola = new PersonnePhysique(LOLA);
		assertFalse(service.isDomicileDansLeCanton(lola, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(1968, 5, 1)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(1989, 12, 31)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(lola, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(lola, null));

		// Heidi qui a habité dans le canton lors de deux périodes séparées
		final PersonnePhysique heidi = new PersonnePhysique(HEIDI);
		assertFalse(service.isDomicileDansLeCanton(heidi, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1968, 5, 1)));
		assertFalse(service.isDomicileDansLeCanton(heidi, date(1980, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1990, 5, 28)));
		assertTrue(service.isDomicileDansLeCanton(heidi, date(1990, 12, 31)));
		assertFalse(service.isDomicileDansLeCanton(heidi, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(heidi, null));

		// Ursule qui a habité dans le canton et qui est parti pour une destination vaudoise mais n'a pas encore enregistré son arrivée
		final PersonnePhysique ursule = new PersonnePhysique(URSULE);
		assertFalse(service.isDomicileDansLeCanton(ursule, date(1965, 2, 23)));
		assertTrue(service.isDomicileDansLeCanton(ursule, date(1968, 5, 1)));
		assertTrue(service.isDomicileDansLeCanton(ursule, date(1970, 12, 31)));
		// [SIFISC-5970] différence de comportement entre RegPP et RcPers : la personne qui a annoncé son départ pour une
		// autre commune vaudoise reste habitante, même si elle n'a pas encore annoncé son arrivée
		assertTrue(service.isDomicileDansLeCanton(ursule, date(1971, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(ursule, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(ursule, null));

		// Trudi qui est arrivée dans une commune vaudoise le 1er janvier 1990 en provenance d'une autre commune vaudoise mais dont on a aucune trace (cas bizarre hypothétique)
		final PersonnePhysique trudi = new PersonnePhysique(TRUDI);
		assertFalse(service.isDomicileDansLeCanton(trudi, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(trudi,
				date(1989, 12, 31))); // malgré son annonce de provenance d'autre commune vaudoise, on considère cette personne HC/HS parce qu'on a pas trace de cette résidence.
		assertTrue(service.isDomicileDansLeCanton(trudi, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(trudi, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(trudi, null));

		// Shirley qui est née à Cottens (VD) et qui est - charogne de gamine ! - partie à Zürich le 1er janvier 1990
		final PersonnePhysique shirley = new PersonnePhysique(SHIRLEY);
		assertTrue(service.isDomicileDansLeCanton(shirley, date(1970, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(shirley, date(1989, 12, 31)));
		assertFalse(service.isDomicileDansLeCanton(shirley, date(1990, 1, 1)));
		assertFalse(service.isDomicileDansLeCanton(shirley, date(2010, 3, 12)));
		assertFalse(service.isDomicileDansLeCanton(shirley, null));

		// Urs qui est né hors-canton et qui est arrivé dans le canton le 1er janvier 1990
		final PersonnePhysique urs = new PersonnePhysique(URS);
		assertFalse(service.isDomicileDansLeCanton(urs, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(urs, date(1989, 12, 31)));
		assertTrue(service.isDomicileDansLeCanton(urs, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(urs, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(urs, null));

		// Brahim qui habitait hors-Suisse et qui est arrivé le 1er janvier 1990 dans le canton
		final PersonnePhysique brahim = new PersonnePhysique(BRAHIM);
		assertFalse(service.isDomicileDansLeCanton(brahim, date(1965, 2, 23)));
		assertFalse(service.isDomicileDansLeCanton(brahim, date(1989, 12, 31)));
		assertTrue(service.isDomicileDansLeCanton(brahim, date(1990, 1, 1)));
		assertTrue(service.isDomicileDansLeCanton(brahim, date(2010, 3, 12)));
		assertTrue(service.isDomicileDansLeCanton(brahim, null));
	}

	@Test
	public void testChangeHabitantEnNHAvecRepriseOrigine() throws Exception {

		final long noIndividu = 3467843L;
		final long noIndividu2 = 3467844L;
		final RegDate dateNaissance = date(1989, 10, 3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Bouliokova", "Tatiana", Sexe.FEMININ);
				addOrigine(ind, MockCommune.Orbe);
				final MockIndividu ind2 = addIndividu(noIndividu2, dateNaissance, "Gerard", "Mansudoux-Geveney", Sexe.MASCULIN);
				MockCommune [] communes = new MockCommune[] {
						MockCommune.Orbe, MockCommune.Lausanne, MockCommune.GrangesMarnand,	MockCommune.CheseauxSurLausanne, MockCommune.VufflensLaVille,
						MockCommune.YverdonLesBains, MockCommune.RomanelSurLausanne, MockCommune.Malapalud,	MockCommune.RomainmotierEnvy, MockCommune.ChateauDoex,
						MockCommune.BourgEnLavaux, MockCommune.LeChenit, MockCommune.Zurich, MockCommune.LeLieu,MockCommune.LesClees, MockCommune.Aubonne,
						MockCommune.Echallens, MockCommune.Mirage, MockCommune.Bale, MockCommune.Chamblon};
				for (MockCommune commune : communes) {
					addOrigine(ind2, commune);
				}
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				final PersonnePhysique pp2 = tiersService.createNonHabitantFromIndividu(noIndividu2);
				assertEquals("le libellé de la commune d'origine du non-habitant devrait être Orbe", "Orbe", pp.getLibelleCommuneOrigine());
				final int maxSize = LengthConstants.TIERS_LIB_ORIGINE;
				assertEquals("les origines trop longues devraient être abrégées à " + maxSize, maxSize, pp2.getLibelleCommuneOrigine().length());
				return pp.getNumero();
			}
		});
	}

	@Test
	public void testStatutMenageCommun() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final PersonnePhysique pp2 = addNonHabitant("Tata", "Tutu", date(1980, 1, 1), Sexe.FEMININ);
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, pp2, date(2000, 1, 1), null);
					assertEquals(StatutMenageCommun.EN_VIGUEUR, tiersService.getStatutMenageCommun(etc.getMenage()));
				}
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final PersonnePhysique pp2 = addNonHabitant("Tata", "Tutu", date(1980, 1, 1), Sexe.FEMININ);
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, pp2, date(2000, 1, 1), date(2010, 1, 1));
					assertEquals(StatutMenageCommun.TERMINE_SUITE_SEPARATION, tiersService.getStatutMenageCommun(etc.getMenage()));
				}
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final PersonnePhysique pp2 = addNonHabitant("Tata", "Tutu", date(1980, 1, 1), Sexe.FEMININ);
					pp2.setDateDeces(date(2010, 1, 1));
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, pp2, date(2000, 1, 1), date(2010, 1, 1));
					assertEquals(StatutMenageCommun.TERMINE_SUITE_DECES, tiersService.getStatutMenageCommun(etc.getMenage()));
				}
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final PersonnePhysique pp2 = addNonHabitant("Tata", "Tutu", date(1980, 1, 1), Sexe.FEMININ);
					pp1.setDateDeces(date(2010, 1, 1));
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, pp2, date(2000, 1, 1), date(2010, 1, 1));
					assertEquals(StatutMenageCommun.TERMINE_SUITE_DECES, tiersService.getStatutMenageCommun(etc.getMenage()));
				}
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final PersonnePhysique pp2 = addNonHabitant("Tata", "Tutu", date(1980, 1, 1), Sexe.FEMININ);
					pp1.setDateDeces(date(2010, 1, 1));
					pp2.setDateDeces(date(2010, 1, 1));
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, pp2, date(2000, 1, 1), date(2010, 1, 1));
					assertEquals(StatutMenageCommun.TERMINE_SUITE_DECES, tiersService.getStatutMenageCommun(etc.getMenage()));
				}

				// Marié seul
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, null, date(2000, 1, 1), null);
					assertEquals(StatutMenageCommun.EN_VIGUEUR, tiersService.getStatutMenageCommun(etc.getMenage()));
				}
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, null, date(2000, 1, 1), date(2010, 1, 1));
					assertEquals(StatutMenageCommun.TERMINE_SUITE_SEPARATION, tiersService.getStatutMenageCommun(etc.getMenage()));
				}
				{
					final PersonnePhysique pp1 = addNonHabitant("Toto", "Tutu", date(1980, 1, 1), Sexe.MASCULIN);
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(pp1, null, date(2000, 1, 1), date(2010, 1, 1));
					pp1.setDateDeces(date(2010, 1, 1));
					assertEquals(StatutMenageCommun.TERMINE_SUITE_DECES, tiersService.getStatutMenageCommun(etc.getMenage()));
				}
				return null;
			}
		});
	}

	/**
	 * Test {@link TiersService#getDateDecesDepuisDernierForPrincipal(PersonnePhysique)}
	 *
	 * Cas ou la date de décès est deductible depuis les fors
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDateDecesDepuisDernierForPrincipal_1() throws Exception {
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecPersonnePhysique(date(2010, 1, 1), MotifFor.VEUVAGE_DECES);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertEquals(date(2010, 1, 1), dateDeces);
					return null;
				}
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenage(date(2010, 1, 1), MotifFor.VEUVAGE_DECES);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertEquals(date(2010, 1, 1), dateDeces);
					return null;
				}
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(date(2010, 1, 1), MotifFor.VEUVAGE_DECES);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertEquals(date(2010, 1, 1), dateDeces);
					return null;
				}
			});
		}
	}

	/**
	 * Test {@link TiersService#getDateDecesDepuisDernierForPrincipal(PersonnePhysique)}
	 *
	 * Cas ou la date de décès n'est deductible depuis les fors (derniers for pas fermé)
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDateDecesDepuisDernierForPrincipal_2() throws Exception {
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecPersonnePhysique(null, null);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertNull(dateDeces);
					return null;
				}
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenage(null, null);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertNull(dateDeces);
					return null;
				}
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(null, null);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertNull(dateDeces);
					return null;
				}
			});
		}
	}

	/**
	 * Test {@link TiersService#getDateDecesDepuisDernierForPrincipal(PersonnePhysique)}
	 *
	 * Cas ou la date de décès n'est deductible depuis les fors (derniers for fermé mais avec un motif autre que veuvage/décès)
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDateDecesDepuisDernierForPrincipal_3() throws Exception {
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecPersonnePhysique(date(2010,1,1), MotifFor.DEPART_HS);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertNull(dateDeces);
					return null;
				}
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenage(date(2010,1,1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertNull(dateDeces);
					return null;
				}
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(date(2010,1,1), MotifFor.DEPART_HC);
			doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
					final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
					assertNull(dateDeces);
					return null;
				}
			});
		}
	}

	private Long testGetDateDecesDepuisDernierForPrincipalAvecMenage(@Nullable final RegDate dateFermetureFor, @Nullable final MotifFor motifFermeture) throws Exception {

		final Long noLui = 1234567L;
		final Long noElle = 1234568L;
		final RegDate dateMariage = date(1954,1,1);

		return doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addHabitant(noElle);
				final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateMariage, dateFermetureFor);
				addForPrincipal(
						etc.getMenage(),
						dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						dateFermetureFor, motifFermeture,
						MockCommune.Aigle
				);
				return lui.getNumero();
			}
		});
	}

	private Long testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(@Nullable final RegDate dateFermetureFor, @Nullable final MotifFor motifFermeture) throws Exception {

		final Long noLui = 1234567L;
		final Long noElle = 1234568L;
		final RegDate dateMariage = date(1954,1,1);
		final RegDate dateSeparation = date(2005,1,1);

		return doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addHabitant(noElle);
				final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation.getOneDayBefore());
				addForPrincipal(
						etc.getMenage(),
						dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
						MockCommune.Aigle
				);
				addForPrincipal(
						lui,
						dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
						dateFermetureFor, motifFermeture,
						MockCommune.Aigle
				);
				return lui.getNumero();
			}
		});

	}

	private Long testGetDateDecesDepuisDernierForPrincipalAvecPersonnePhysique(@Nullable final RegDate dateFermetureFor, @Nullable final MotifFor motifFermeture) throws Exception {

		final Long noLui = 1234567L;
		final RegDate dateMajorite = date(1954,1,1);

		// Mise en place du fiscal
		return doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				addForPrincipal(
						lui,
						dateMajorite, MotifFor.MAJORITE,
						dateFermetureFor, motifFermeture,
						MockCommune.Aigle
				);
				return lui.getNumero();
			}
		});
	}

	@Test
	public void testGetDateNaissance() throws Exception {

		final long noIndividuHabitant = 21543245L;
		final long noIndividuAncienHabitant = 3434362L;
		final RegDate dateNaissanceHabitant = date(2011, 2, 12);
		final RegDate dateNaissanceAncienHabitant = date(2011, 2, 21);
		final RegDate dateNaissanceNonHabitant = date(2010, 12, 4);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indigene = addIndividu(noIndividuHabitant, dateNaissanceHabitant, "Lepetit", "François", Sexe.MASCULIN);
				addAdresse(indigene, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissanceHabitant, null);

				final MockIndividu parti = addIndividu(noIndividuAncienHabitant, null, "Lapetite", "Françoise", Sexe.FEMININ);
				addAdresse(parti, TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, dateNaissanceAncienHabitant.addMonths(5), null);
			}
		});

		class Ids {
			long ppHabitant;
			long ppAncienHabitant;
			long ppNonHabitant;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique hab = addHabitant(noIndividuHabitant);
				final PersonnePhysique ancienHab = tiersService.createNonHabitantFromIndividu(noIndividuAncienHabitant);
				ancienHab.setDateNaissance(dateNaissanceAncienHabitant);
				final PersonnePhysique nonHab = addNonHabitant("Bienvenu", "Patant", null, Sexe.MASCULIN);
				final Ids ids = new Ids();
				ids.ppHabitant = hab.getNumero();
				ids.ppAncienHabitant = ancienHab.getNumero();
				ids.ppNonHabitant = nonHab.getNumero();
				return ids;
			}
		});

		Assert.assertNull(tiersService.getDateNaissance(null));

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(ids.ppHabitant);
				Assert.assertEquals(dateNaissanceHabitant, tiersService.getDateNaissance(hab));

				final PersonnePhysique ancienHab = (PersonnePhysique) tiersDAO.get(ids.ppAncienHabitant);
				Assert.assertEquals(dateNaissanceAncienHabitant, tiersService.getDateNaissance(ancienHab));

				final PersonnePhysique nonHabitant = (PersonnePhysique) tiersDAO.get(ids.ppNonHabitant);
				Assert.assertNull(tiersService.getDateNaissance(nonHabitant));
				nonHabitant.setDateNaissance(dateNaissanceNonHabitant);
				Assert.assertEquals(dateNaissanceNonHabitant, tiersService.getDateNaissance(nonHabitant));

				return null;
			}
		});
	}

	/**
	 * SIFISC-8597
	 */
	@Test
	public void testMiseAJourFlagHabitantSurCommuneNonIdentifiable() throws Exception {

		// service infrastructure piégé
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService() {
			@Override
			public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
				throw new ServiceInfrastructureException("Boom pour le test!!");
			}
		});

		final long noIndividu = 437843687L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1980, 8, 23), "Cardamonne", "Perlette", Sexe.FEMININ);
				final MockAdresse adresse = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, "Rue des champs", "19b", 1098, MockLocalite.Epesses, null, date(1980, 8, 23), null);
				adresse.setEgid(MockBatiment.Epesses.BatimentLaPlace.getEgid());
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// re-calcul du flag habitant
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				try {
					tiersService.updateHabitantStatus(pp, noIndividu, null, null);
					Assert.fail();
				}
				catch (TiersException e) {
					Assert.assertEquals("Impossible de déterminer si le domicile du contribuable " + pp.getNumero() + " est vaudois ou pas", e.getMessage());
				}
				return null;
			}
		});
	}

	@Test
	public void testRefreshParentesParNumeroIndividu() throws Exception {

		final long noIndPapy = 32352L;              // papa du papa
		final long noIndPapa = 537538L;             // papa de l'individu
		final long noIndMaman = 437634L;            // maman de l'individu
		final long noIndividu = 4378437L;           // l'individu
		final long noIndFifille = 23467256L;        // la fille de l'individu
		final long noIndFiston = 4378236L;          // le fiston de l'individu
		final long noIndFactrice = 42372436L;       // fausse maman du fiston...

		final RegDate dateDecesPapy = date(1999, 12, 27);
		final RegDate dateNaissancePapa = date(1945, 9, 4);
		final RegDate dateNaissance = date(1973, 12, 9);
		final RegDate dateNaissanceFille = date(2002, 1, 26);
		final RegDate dateNaissanceFiston = date(2005, 10, 25);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papy = addIndividu(noIndPapy, null, "Bernard", "Eugène", Sexe.MASCULIN);
				papy.setDateDeces(dateDecesPapy);

				final MockIndividu papa = addIndividu(noIndPapa, dateNaissancePapa, "Bernard", "Michel", Sexe.MASCULIN);
				addLiensFiliation(papa, papy, null, dateNaissancePapa, null);       // pour vérifier si on prend bien en compte la date de décès même si elle n'est pas fournie par le registre civil

				final MockIndividu maman = addIndividu(noIndMaman, null, "Bernard", "Francine", Sexe.FEMININ);

				final MockIndividu moi = addIndividu(noIndividu, dateNaissance, "Bernard", "Olivier", Sexe.MASCULIN);
				addLiensFiliation(moi, papa, maman, dateNaissance, null);

				final MockIndividu fille = addIndividu(noIndFifille, dateNaissanceFille, "Bernard", "Chloé", Sexe.FEMININ);
				addLiensFiliation(fille, moi, null, dateNaissanceFille, null);

				final MockIndividu fiston = addIndividu(noIndFiston, dateNaissanceFiston, "Bernard", "Léo", Sexe.MASCULIN);
				addLiensFiliation(fiston, moi, null, dateNaissanceFiston, null);

				addIndividu(noIndFactrice, null, "Bolomey", "Sandrine", Sexe.FEMININ);
			}
		});

		class Ids {
			long ppPapy;
			long ppPapa;
			long ppMaman;
			long ppMoi;
			long ppFiston;
			long ppFifille;
			long ppFactrice;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique papy = addHabitant(noIndPapy);
				final PersonnePhysique papa = addHabitant(noIndPapa);
				final PersonnePhysique maman = addHabitant(noIndMaman);
				final PersonnePhysique moi = addHabitant(noIndividu);
				final PersonnePhysique fiston = addHabitant(noIndFiston);
				final PersonnePhysique fifille = addHabitant(noIndFifille);
				final PersonnePhysique factrice = addHabitant(noIndFactrice);

				addParente(papa, papy, dateNaissancePapa, null);        // celle-ci devra être annulée et remplacée par une autre avec une date de fin au décès de papy si on démarre de papa
				addParente(moi, papa, dateNaissance, null);             // celle-ci ne doit pas bouger
//				addParente(moi, maman, dateNaissance, null);            // en commentaire -> elle devra être créée
				addParente(fiston, moi, dateNaissanceFiston, null);     // ne doit pas bouger
//				addParente(fifille, moi, dateNaissanceFille, null);     // en commentaire -> elle ne serait créée que si on démarre de la fille
				addParente(fiston, factrice, dateNaissanceFiston, null);    // n'apparaît pas dans le civil -> devra être annulée

				final Ids ids = new Ids();
				ids.ppPapy = papy.getNumero();
				ids.ppPapa = papa.getNumero();
				ids.ppMaman = maman.getNumero();
				ids.ppMoi = moi.getNumero();
				ids.ppFiston = fiston.getNumero();
				ids.ppFifille = fifille.getNumero();
				ids.ppFactrice = factrice.getNumero();
				return ids;
			}
		});

		// refresh de "moi"
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				tiersService.refreshParentesDepuisNumeroIndividu(noIndividu);
				return null;
			}
		});

		// vérification de l'impact
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// papy
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppPapy);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppPapy, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppPapa, rapportEnfant.getSujetId());
					assertEquals(dateNaissancePapa, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());         // n'a pas bougé, c'est normal car on ne vérifie qu'un niveau autour du personnage central
					assertFalse(rapportEnfant.isAnnule());
				}

				// papa
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppPapa);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(1, parents.size());

					final RapportEntreTiers rapportParent = parents.iterator().next();
					assertNotNull(rapportParent);
					assertEquals((Long) ids.ppPapy, rapportParent.getObjetId());
					assertEquals((Long) ids.ppPapa, rapportParent.getSujetId());
					assertEquals(dateNaissancePapa, rapportParent.getDateDebut());
					assertNull(rapportParent.getDateFin());         // n'a pas bougé, c'est normal car on ne vérifie qu'un niveau autour du personnage central
					assertFalse(rapportParent.isAnnule());

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppPapa, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppMoi, rapportEnfant.getSujetId());
					assertEquals(dateNaissance, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// maman
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppMaman);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());                    // nouvelle relation !

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppMaman, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppMoi, rapportEnfant.getSujetId());
					assertEquals(dateNaissance, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// moi
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppMoi);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(2, parents.size());                    // dont une nouvelle relation !

					final List<RapportEntreTiers> sortedParents = new ArrayList<>(parents);
					Collections.sort(sortedParents, new Comparator<RapportEntreTiers>() {
						@Override
						public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
							return Long.compare(o1.getObjetId(), o2.getObjetId());
						}
					});
					{
						final RapportEntreTiers rapport = sortedParents.get(0);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppPapa, rapport.getObjetId());
						assertEquals((Long) ids.ppMoi, rapport.getSujetId());
						assertEquals(dateNaissance, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}
					{
						final RapportEntreTiers rapport = sortedParents.get(1);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppMaman, rapport.getObjetId());
						assertEquals((Long) ids.ppMoi, rapport.getSujetId());
						assertEquals(dateNaissance, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());                    // pas de nouvelle relation !

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppMoi, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppFiston, rapportEnfant.getSujetId());
					assertEquals(dateNaissanceFiston, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// fiston
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFiston);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(2, parents.size());

					final List<RapportEntreTiers> sortedParents = new ArrayList<>(parents);
					Collections.sort(sortedParents, new Comparator<RapportEntreTiers>() {
						@Override
						public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
							return Long.compare(o1.getObjetId(), o2.getObjetId());
						}
					});
					{
						final RapportEntreTiers rapport = sortedParents.get(0);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppMoi, rapport.getObjetId());
						assertEquals((Long) ids.ppFiston, rapport.getSujetId());
						assertEquals(dateNaissanceFiston, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}
					{
						final RapportEntreTiers rapport = sortedParents.get(1);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppFactrice, rapport.getObjetId());
						assertEquals((Long) ids.ppFiston, rapport.getSujetId());
						assertEquals(dateNaissanceFiston, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertTrue(rapport.isAnnule());             // la filiation vers la factrice n'est pas dans le civil
					}

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEmpty(enfants);
				}

				// fifille
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFifille);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEmpty(enfants);
				}

				// factrice
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFactrice);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppFactrice, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppFiston, rapportEnfant.getSujetId());
					assertEquals(dateNaissanceFiston, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertTrue(rapportEnfant.isAnnule());           // la filiation entre le fiston et la factrice n'est pas dans le civil
				}

				return null;
			}
		});

		// refresh de "papa"
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				tiersService.refreshParentesDepuisNumeroIndividu(noIndPapa);
				return null;
			}
		});

		// vérification de l'impact
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// papy
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppPapy);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(2, enfants.size());            // 1 annulé, et l'autre actif, modification en raison de la date de fin

					final List<RapportEntreTiers> sortedEnfants = new ArrayList<>(enfants);
					Collections.sort(sortedEnfants, new Comparator<RapportEntreTiers>() {
						@Override
						public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
							return NullDateBehavior.EARLIEST.compare(o1.getDateFin(), o2.getDateFin());         // le rapport sans fin (annulé) d'abord
						}
					});
					{
						final RapportEntreTiers rapport = sortedEnfants.get(0);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppPapy, rapport.getObjetId());
						assertEquals((Long) ids.ppPapa, rapport.getSujetId());
						assertEquals(dateNaissancePapa, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertTrue(rapport.isAnnule());
					}
					{
						final RapportEntreTiers rapport = sortedEnfants.get(1);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppPapy, rapport.getObjetId());
						assertEquals((Long) ids.ppPapa, rapport.getSujetId());
						assertEquals(dateNaissancePapa, rapport.getDateDebut());
						assertEquals(dateDecesPapy, rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}
				}

				// papa
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppPapa);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(2, parents.size());        // 1 annulé, et l'autre actif, modification en raison de la date de fin

					final List<RapportEntreTiers> sortedParents = new ArrayList<>(parents);
					Collections.sort(sortedParents, new Comparator<RapportEntreTiers>() {
						@Override
						public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
							return NullDateBehavior.EARLIEST.compare(o1.getDateFin(), o2.getDateFin());         // le rapport sans fin (annulé) d'abord
						}
					});
					{
						final RapportEntreTiers rapport = sortedParents.get(0);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppPapy, rapport.getObjetId());
						assertEquals((Long) ids.ppPapa, rapport.getSujetId());
						assertEquals(dateNaissancePapa, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertTrue(rapport.isAnnule());
					}
					{
						final RapportEntreTiers rapport = sortedParents.get(1);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppPapy, rapport.getObjetId());
						assertEquals((Long) ids.ppPapa, rapport.getSujetId());
						assertEquals(dateNaissancePapa, rapport.getDateDebut());
						assertEquals(dateDecesPapy, rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppPapa, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppMoi, rapportEnfant.getSujetId());
					assertEquals(dateNaissance, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// maman
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppMaman);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());                    // nouvelle relation !

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppMaman, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppMoi, rapportEnfant.getSujetId());
					assertEquals(dateNaissance, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// moi
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppMoi);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(2, parents.size());                    // dont une nouvelle relation !

					final List<RapportEntreTiers> sortedParents = new ArrayList<>(parents);
					Collections.sort(sortedParents, new Comparator<RapportEntreTiers>() {
						@Override
						public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
							return Long.compare(o1.getObjetId(), o2.getObjetId());
						}
					});
					{
						final RapportEntreTiers rapport = sortedParents.get(0);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppPapa, rapport.getObjetId());
						assertEquals((Long) ids.ppMoi, rapport.getSujetId());
						assertEquals(dateNaissance, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}
					{
						final RapportEntreTiers rapport = sortedParents.get(1);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppMaman, rapport.getObjetId());
						assertEquals((Long) ids.ppMoi, rapport.getSujetId());
						assertEquals(dateNaissance, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());                    // pas de nouvelle relation !

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppMoi, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppFiston, rapportEnfant.getSujetId());
					assertEquals(dateNaissanceFiston, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// fiston
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFiston);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(2, parents.size());

					final List<RapportEntreTiers> sortedParents = new ArrayList<>(parents);
					Collections.sort(sortedParents, new Comparator<RapportEntreTiers>() {
						@Override
						public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
							return Long.compare(o1.getObjetId(), o2.getObjetId());
						}
					});
					{
						final RapportEntreTiers rapport = sortedParents.get(0);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppMoi, rapport.getObjetId());
						assertEquals((Long) ids.ppFiston, rapport.getSujetId());
						assertEquals(dateNaissanceFiston, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}
					{
						final RapportEntreTiers rapport = sortedParents.get(1);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppFactrice, rapport.getObjetId());
						assertEquals((Long) ids.ppFiston, rapport.getSujetId());
						assertEquals(dateNaissanceFiston, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertTrue(rapport.isAnnule());
					}

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEmpty(enfants);
				}

				// fifille
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFifille);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEmpty(enfants);
				}

				// factrice
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFactrice);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppFactrice, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppFiston, rapportEnfant.getSujetId());
					assertEquals(dateNaissanceFiston, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertTrue(rapportEnfant.isAnnule());
				}

				return null;
			}
		});
	}

	@Test
	public void testEliminationDoublonParenteParRefresh() throws Exception {

		final long noIndividuParent = 434375L;
		final long noIndividuEnfant = 4378436287L;
		final RegDate dateNaissanceEnfant = date(2000, 7, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent = addIndividu(noIndividuParent, null, "Swift", "John Senior", Sexe.MASCULIN);
				final MockIndividu enfant = addIndividu(noIndividuEnfant, dateNaissanceEnfant, "Swift", "John Junior", Sexe.MASCULIN);
				addLiensFiliation(enfant, parent, null, dateNaissanceEnfant, null);
			}
		});

		final class Ids {
			long idParent;
			long idEnfant;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique parent = addHabitant(noIndividuParent);
				final PersonnePhysique enfant = addHabitant(noIndividuEnfant);

				// le doublon de parenté est ajouté "à la main" car les méthodes officielles ne permettent pas de le faire
				hibernateTemplate.merge(new Parente(dateNaissanceEnfant, null, parent, enfant));
				hibernateTemplate.merge(new Parente(dateNaissanceEnfant, null, parent, enfant));        // doublon de parenté

				final Ids ids = new Ids();
				ids.idParent = parent.getNumero();
				ids.idEnfant = enfant.getNumero();
				return ids;
			}
		});

		// vérification de l'existance du doublon de parenté
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
				final Set<RapportEntreTiers> relParents = enfant.getRapportsSujet();
				assertNotNull(relParents);
				assertEquals(2, relParents.size());
				for (RapportEntreTiers rel : relParents) {
					assertEquals(TypeRapportEntreTiers.PARENTE, rel.getType());
					assertEquals((Long) ids.idEnfant, rel.getSujetId());
					assertEquals((Long) ids.idParent, rel.getObjetId());
					assertEquals(dateNaissanceEnfant, rel.getDateDebut());
					assertNull(rel.getDateFin());
					assertFalse(rel.isAnnule());
				}
				return null;
			}
		});

		// vérification de la bonne récupération des candidats doublons par le DAO
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Pair<Long, Long>> doublons = rapportEntreTiersDAO.getDoublonsCandidats(TypeRapportEntreTiers.PARENTE);
				assertNotNull(doublons);
				assertEquals(1, doublons.size());

				final Pair<Long, Long> doublon = doublons.get(0);
				assertNotNull(doublon);
				assertEquals((Long) ids.idEnfant, doublon.getLeft());
				assertEquals((Long) ids.idParent, doublon.getRight());
				return null;
			}
		});

		// demande de refresh
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				tiersService.refreshParentesDepuisNumeroIndividu(noIndividuEnfant);
				return null;
			}
		});

		// vérification que l'un des doublons a bien été éliminé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
				final Set<RapportEntreTiers> relParents = enfant.getRapportsSujet();
				assertNotNull(relParents);
				assertEquals(2, relParents.size());

				final List<RapportEntreTiers> sortedRelParents = new ArrayList<>(relParents);
				Collections.sort(sortedRelParents, new Comparator<RapportEntreTiers>() {
					@Override
					public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
						return Boolean.compare(o1.isAnnule(), o2.isAnnule());       // annulé à la fin
					}
				});

				{
					final RapportEntreTiers rel = sortedRelParents.get(0);
					assertEquals(TypeRapportEntreTiers.PARENTE, rel.getType());
					assertEquals((Long) ids.idEnfant, rel.getSujetId());
					assertEquals((Long) ids.idParent, rel.getObjetId());
					assertEquals(dateNaissanceEnfant, rel.getDateDebut());
					assertNull(rel.getDateFin());
					assertFalse(rel.isAnnule());
				}
				{
					final RapportEntreTiers rel = sortedRelParents.get(1);
					assertEquals(TypeRapportEntreTiers.PARENTE, rel.getType());
					assertEquals((Long) ids.idEnfant, rel.getSujetId());
					assertEquals((Long) ids.idParent, rel.getObjetId());
					assertEquals(dateNaissanceEnfant, rel.getDateDebut());
					assertNull(rel.getDateFin());
					assertTrue(rel.isAnnule());
				}
				return null;
			}
		});

		// vérification de la bonne récupération des candidats doublons par le DAO
		// (ici, il n'y en a plus car l'un des deux a été annulé)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Pair<Long, Long>> doublons = rapportEntreTiersDAO.getDoublonsCandidats(TypeRapportEntreTiers.PARENTE);
				assertNotNull(doublons);
				assertEquals(0, doublons.size());
				return null;
			}
		});
	}

	@Test
	public void testRefreshParentesInactifSurAncienHabitant() throws Exception {

		final long noIndPapy = 32352L;              // papa du papa
		final long noIndPapa = 537538L;             // papa de l'individu
		final long noIndMaman = 437634L;            // maman de l'individu
		final long noIndividu = 4378437L;           // l'individu
		final long noIndFifille = 23467256L;        // la fille de l'individu
		final long noIndFiston = 4378236L;          // le fiston de l'individu
		final long noIndFactrice = 42372436L;       // fausse maman du fiston...

		final RegDate dateDecesPapy = date(1999, 12, 27);
		final RegDate dateNaissancePapa = date(1945, 9, 4);
		final RegDate dateNaissance = date(1973, 12, 9);
		final RegDate dateNaissanceFille = date(2002, 1, 26);
		final RegDate dateNaissanceFiston = date(2005, 10, 25);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papy = addIndividu(noIndPapy, null, "Bernard", "Eugène", Sexe.MASCULIN);
				papy.setDateDeces(dateDecesPapy);

				final MockIndividu papa = addIndividu(noIndPapa, dateNaissancePapa, "Bernard", "Michel", Sexe.MASCULIN);
				addLiensFiliation(papa, papy, null, dateNaissancePapa, null);       // pour vérifier si on prend bien en compte la date de décès même si elle n'est pas fournie par le registre civil

				final MockIndividu maman = addIndividu(noIndMaman, null, "Bernard", "Francine", Sexe.FEMININ);

				final MockIndividu moi = addIndividu(noIndividu, dateNaissance, "Bernard", "Olivier", Sexe.MASCULIN);
				addLiensFiliation(moi, papa, maman, dateNaissance, null);

				final MockIndividu fille = addIndividu(noIndFifille, dateNaissanceFille, "Bernard", "Chloé", Sexe.FEMININ);
				addLiensFiliation(fille, moi, null, dateNaissanceFille, null);

				final MockIndividu fiston = addIndividu(noIndFiston, dateNaissanceFiston, "Bernard", "Léo", Sexe.MASCULIN);
				addLiensFiliation(fiston, moi, null, dateNaissanceFiston, null);

				addIndividu(noIndFactrice, null, "Bolomey", "Sandrine", Sexe.FEMININ);
			}
		});

		class Ids {
			long ppPapy;
			long ppPapa;
			long ppMaman;
			long ppMoi;
			long ppFiston;
			long ppFifille;
			long ppFactrice;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique papy = tiersService.createNonHabitantFromIndividu(noIndPapy);
				final PersonnePhysique papa = tiersService.createNonHabitantFromIndividu(noIndPapa);
				final PersonnePhysique maman = tiersService.createNonHabitantFromIndividu(noIndMaman);
				final PersonnePhysique moi = tiersService.createNonHabitantFromIndividu(noIndividu);
				final PersonnePhysique fiston = tiersService.createNonHabitantFromIndividu(noIndFiston);
				final PersonnePhysique fifille = tiersService.createNonHabitantFromIndividu(noIndFifille);
				final PersonnePhysique factrice = tiersService.createNonHabitantFromIndividu(noIndFactrice);

				addParente(papa, papy, dateNaissancePapa, null);        // celle-ci aurait pu être annulée et remplacée par une autre avec une date de fin au décès de papy si on démarre de papa
				addParente(moi, papa, dateNaissance, null);             // celle-ci ne doit pas bouger
//				addParente(moi, maman, dateNaissance, null);            // en commentaire -> elle aurait pu être créée
				addParente(fiston, moi, dateNaissanceFiston, null);     // ne doit pas bouger
//				addParente(fifille, moi, dateNaissanceFille, null);     // en commentaire -> elle ne serait créée que si on démarre de la fille
				addParente(fiston, factrice, dateNaissanceFiston, null);    // n'apparaît pas dans le civil -> aurait pu être annulée

				final Ids ids = new Ids();
				ids.ppPapy = papy.getNumero();
				ids.ppPapa = papa.getNumero();
				ids.ppMaman = maman.getNumero();
				ids.ppMoi = moi.getNumero();
				ids.ppFiston = fiston.getNumero();
				ids.ppFifille = fifille.getNumero();
				ids.ppFactrice = factrice.getNumero();
				return ids;
			}
		});

		// refresh de "moi"
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				tiersService.refreshParentesDepuisNumeroIndividu(noIndividu);
				return null;
			}
		});

		// vérification de l'impact
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// papa
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppPapa);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(1, parents.size());

					final RapportEntreTiers rapportParent = parents.iterator().next();
					assertNotNull(rapportParent);
					assertEquals((Long) ids.ppPapy, rapportParent.getObjetId());
					assertEquals((Long) ids.ppPapa, rapportParent.getSujetId());
					assertEquals(dateNaissancePapa, rapportParent.getDateDebut());
					assertNull(rapportParent.getDateFin());         // n'a pas bougé, c'est normal car on ne vérifie qu'un niveau autour du personnage central
					assertFalse(rapportParent.isAnnule());

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppPapa, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppMoi, rapportEnfant.getSujetId());
					assertEquals(dateNaissance, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// maman
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppMaman);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(0, enfants.size());                    // pas de nouvelle relation car non-habitant !
				}

				// moi
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppMoi);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(1, parents.size());                    // pas de une nouvelle relation sur les non-habitants !

					final RapportEntreTiers rapport = parents.iterator().next();
					assertNotNull(rapport);
					assertEquals((Long) ids.ppPapa, rapport.getObjetId());
					assertEquals((Long) ids.ppMoi, rapport.getSujetId());
					assertEquals(dateNaissance, rapport.getDateDebut());
					assertNull(rapport.getDateFin());
					assertFalse(rapport.isAnnule());

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());                    // pas de nouvelle relation !

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppMoi, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppFiston, rapportEnfant.getSujetId());
					assertEquals(dateNaissanceFiston, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());
				}

				// fiston
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFiston);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEquals(2, parents.size());

					final List<RapportEntreTiers> sortedParents = new ArrayList<>(parents);
					Collections.sort(sortedParents, new Comparator<RapportEntreTiers>() {
						@Override
						public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
							return Long.compare(o1.getObjetId(), o2.getObjetId());
						}
					});
					{
						final RapportEntreTiers rapport = sortedParents.get(0);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppMoi, rapport.getObjetId());
						assertEquals((Long) ids.ppFiston, rapport.getSujetId());
						assertEquals(dateNaissanceFiston, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());
					}
					{
						final RapportEntreTiers rapport = sortedParents.get(1);
						assertNotNull(rapport);
						assertEquals((Long) ids.ppFactrice, rapport.getObjetId());
						assertEquals((Long) ids.ppFiston, rapport.getSujetId());
						assertEquals(dateNaissanceFiston, rapport.getDateDebut());
						assertNull(rapport.getDateFin());
						assertFalse(rapport.isAnnule());             // la filiation vers la factrice n'est pas dans le civil, mais on ne touche à rien sur les non-habitants
					}

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEmpty(enfants);
				}

				// fifille
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFifille);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEmpty(enfants);
				}

				// factrice
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppFactrice);
					assertNotNull(pp);

					final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
					assertEmpty(parents);

					final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
					assertEquals(1, enfants.size());

					final RapportEntreTiers rapportEnfant = enfants.iterator().next();
					assertNotNull(rapportEnfant);
					assertEquals((Long) ids.ppFactrice, rapportEnfant.getObjetId());
					assertEquals((Long) ids.ppFiston, rapportEnfant.getSujetId());
					assertEquals(dateNaissanceFiston, rapportEnfant.getDateDebut());
					assertNull(rapportEnfant.getDateFin());
					assertFalse(rapportEnfant.isAnnule());           // la filiation entre le fiston et la factrice n'est pas dans le civil, mais on ne touche à rien sur les non-habitants
				}

				return null;
			}
		});
	}

	@Test
	public void testPriseEnCompteDatesFiliationsCivilesDansParentes() throws Exception {

		final long noIndParent = 473423L;
		final long noIndEnfant = 4378433L;
		final RegDate dateNaissanceEnfant = date(2005, 6, 30);
		final RegDate dateAdoptionEnfant = date(2005, 8, 1);
		final RegDate dateDesaveuEnfant = date(2006, 4, 30);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent = addIndividu(noIndParent, null, "Dursley", "Vernon", Sexe.MASCULIN);
				final MockIndividu enfant = addIndividu(noIndEnfant, dateNaissanceEnfant, "Potter", "Harry", Sexe.MASCULIN);
				addLiensFiliation(enfant, parent, null, dateAdoptionEnfant, dateDesaveuEnfant);
			}
		});

		final class Ids {
			long idEnfant;
			long idParent;
		}

		// mise en place fiscale avec création des parentés
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique parent = addHabitant(noIndParent);
				final PersonnePhysique enfant = addHabitant(noIndEnfant);

				final Ids ids = new Ids();
				ids.idEnfant = enfant.getNumero();
				ids.idParent = parent.getNumero();
				return ids;
			}
		});

		// contrôle des dates de la relation de parenté créée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
				assertNotNull(enfant);

				final Set<RapportEntreTiers> relSujet = enfant.getRapportsSujet();
				assertNotNull(relSujet);
				assertEquals(1, relSujet.size());

				final RapportEntreTiers parente = relSujet.iterator().next();
				assertNotNull(parente);
				assertEquals(TypeRapportEntreTiers.PARENTE, parente.getType());
				assertEquals((Long) ids.idEnfant, parente.getSujetId());
				assertEquals((Long) ids.idParent, parente.getObjetId());
				assertFalse(parente.isAnnule());
				assertEquals(dateAdoptionEnfant, parente.getDateDebut());
				assertEquals(dateDesaveuEnfant, parente.getDateFin());

				return null;
			}
		});
	}

	@Test
	public void testDateNaissancePartielleEtParente() throws Exception {

		final long noIndParent = 473423L;
		final long noIndEnfant = 4378433L;
		final RegDate dateNaissanceEnfant = date(2005, 6);
		final RegDate dateDesaveuEnfant = date(2006, 4, 30);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent = addIndividu(noIndParent, null, "Dursley", "Vernon", Sexe.MASCULIN);
				final MockIndividu enfant = addIndividu(noIndEnfant, dateNaissanceEnfant, "Potter", "Harry", Sexe.MASCULIN);
				addLiensFiliation(enfant, parent, null, dateNaissanceEnfant, dateDesaveuEnfant);
			}
		});

		final class Ids {
			long idEnfant;
			long idParent;
		}

		// mise en place fiscale avec création des parentés
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique parent = addHabitant(noIndParent);
				final PersonnePhysique enfant = addHabitant(noIndEnfant);

				final Ids ids = new Ids();
				ids.idEnfant = enfant.getNumero();
				ids.idParent = parent.getNumero();
				return ids;
			}
		});

		// contrôle des dates de la relation de parenté créée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
				assertNotNull(enfant);

				final Set<RapportEntreTiers> relSujet = enfant.getRapportsSujet();
				assertNotNull(relSujet);
				assertEquals(1, relSujet.size());

				final RapportEntreTiers parente = relSujet.iterator().next();
				assertNotNull(parente);
				assertEquals(TypeRapportEntreTiers.PARENTE, parente.getType());
				assertEquals((Long) ids.idEnfant, parente.getSujetId());
				assertEquals((Long) ids.idParent, parente.getObjetId());
				assertFalse(parente.isAnnule());
				assertEquals(date(dateNaissanceEnfant.year(), dateNaissanceEnfant.month(), 1), parente.getDateDebut());
				assertEquals(dateDesaveuEnfant, parente.getDateFin());

				return null;
			}
		});
	}

	/**
	 * Cas des environnements de test où la synchro entre le civil et le fiscal n'est pas parfaite
	 */
	@Test
	public void testInitParenteAvecIndividuInconnuAuCivil() throws Exception {

		final long noIndividu = 42L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Fujitsu", null, Sexe.MASCULIN);
				pp.setHabitant(true);
				pp.setNumeroIndividu(noIndividu);
				return pp.getNumero();
			}
		});

		final ParenteUpdateResult result = doInNewTransactionAndSession(new TransactionCallback<ParenteUpdateResult>() {
			@Override
			public ParenteUpdateResult doInTransaction(TransactionStatus status) {
				// vérification que la personne physique est toujours habitante (des fois qu'on mettrait en place un intercepteur qui recalcule le bousin)
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertTrue(pp.isHabitantVD());
				assertFalse(pp.isParenteDirty());

				// lancement du traitement
				return tiersService.initParentesDepuisFiliationsCiviles(pp);
			}
		});

		assertNotNull(result);
		assertEquals(0, result.getUpdates().size());
		assertEquals(1, result.getErrors().size());

		final ParenteUpdateResult.Error error = result.getErrors().get(0);
		assertEquals(ppId, error.getNoCtb());
		assertEquals(String.format("Individu %d lié à l'habitant %d non-récupérable depuis le registre civil", noIndividu, ppId), error.getErrorMsg());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertTrue(pp.isHabitantVD());
				assertTrue(pp.isParenteDirty());
			}
		});
	}

	/**
	 * Cas des environnements de test où la synchro entre le civil et le fiscal n'est pas parfaite
	 */
	@Test
	public void testRefreshParenteAvecIndividuInconnuAuCivil() throws Exception {

		final long noIndividu = 42L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Fujitsu", null, Sexe.MASCULIN);
				pp.setHabitant(true);
				pp.setNumeroIndividu(noIndividu);
				return pp.getNumero();
			}
		});

		final ParenteUpdateResult result = doInNewTransactionAndSession(new TransactionCallback<ParenteUpdateResult>() {
			@Override
			public ParenteUpdateResult doInTransaction(TransactionStatus status) {
				// vérification que la personne physique est toujours habitante (des fois qu'on mettrait en place un intercepteur qui recalcule le bousin)
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertTrue(pp.isHabitantVD());
				assertFalse(pp.isParenteDirty());

				// lancement du traitement
				return tiersService.refreshParentesSurPersonnePhysique(pp, false);
			}
		});

		assertNotNull(result);
		assertEquals(0, result.getUpdates().size());
		assertEquals(1, result.getErrors().size());

		final ParenteUpdateResult.Error error = result.getErrors().get(0);
		assertEquals(ppId, error.getNoCtb());
		assertEquals(String.format("Individu %d lié à l'habitant %d non-récupérable depuis le registre civil", noIndividu, ppId), error.getErrorMsg());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertTrue(pp.isHabitantVD());
				assertTrue(pp.isParenteDirty());
			}
		});
	}
}

