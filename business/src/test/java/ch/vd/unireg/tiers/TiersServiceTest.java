package ch.vd.unireg.tiers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalParente;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockNationalite;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.tiers.dao.DecisionAciDAO;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.StatutMenageCommun;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.validation.ValidationService;

import static ch.vd.unireg.interfaces.InterfacesTestHelper.newLocalisation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc", "Duplicates"})
public class TiersServiceTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersServiceTest.class);

	private static final long NUMERO_INDIVIDU = 12345L;
	private TiersService tiersService;
	private AdresseService adresseService;
	private TiersDAO tiersDAO;
	private ForFiscalDAO forFiscalDAO;
	private RapportEntreTiersDAO rapportEntreTiersDAO;
	private ValidationService validationService;
	private EvenementFiscalDAO evenementFiscalDAO;
	private DecisionAciDAO decisionAciDAO;

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
		decisionAciDAO = getBean(DecisionAciDAO.class, "decisionAciDAO");
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

			ForFiscalPrincipalPP premierForFiscal = new ForFiscalPrincipalPP();
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

			ForFiscalPrincipalPP nouveauForFiscal = new ForFiscalPrincipalPP();
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
			assertEquals("Avenue de Beaulieu", ind.getAdresses().iterator().next().getRue());
		}

		{
			// Vue de l'habitant pour 2000
			final Individu ind = tiersService.getIndividu(hab, date(2000, 12, 31), AttributeIndividu.ADRESSES);
			assertNotNull(ind);

			final Collection<Adresse> adresses = ind.getAdresses();
			assertEquals(2, adresses.size());

			final Iterator<Adresse> iter = adresses.iterator();
			assertEquals("Avenue de Beaulieu", iter.next().getRue());
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
			assertEquals("Message d'erreur incorrect", e.getMessage(), "Impossible de déterminer la nationalité de l'individu " + NUMERO_INDIVIDU);
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
		alf.setPrenomUsuel("Alf");
		alf.setNom("Alf");
		alf.setSexe(null);

		PersonnePhysique esc = new PersonnePhysique(false);
		esc.setPrenomUsuel("Escar");
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

		ForFiscalPrincipalPP ff = (ForFiscalPrincipalPP) habitant.getForsFiscaux().toArray()[0];
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
			public Object execute(TransactionStatus status) {
				ids.noCtbPourAnnulation = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbPourAnnulation);

		// premier essai, le "get par numéro d'individu" doit fonctionner
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNull(pp);
				return null;
			}
		});

		// création d'un deuxième tiers avec le même numéro d'individu -> il devait alors sortir du "get par numéro d'individu"
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				ids.noCtbAutre = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbAutre);
		assertTrue(ids.noCtbAutre.longValue() != ids.noCtbPourAnnulation.longValue());

		// le "get par numéro d'individu" doit maintenant retourner le deuxième tiers
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {

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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNull(pp);
				return null;
			}
		});

		// création d'un deuxième tiers avec le même numéro d'individu -> il devait alors sortir du "get par numéro d'individu"
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {

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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {

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
			public Object execute(TransactionStatus status) {
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

		//
		// Une personne avec deux ménages :
		//
		// o [1990,1,1] à [2000,1,1]
		// o [2004,1,1] à ...
		//
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			arnold.setPrenomUsuel("Arnold");
			arnold.setNom("Schwarzie");
			arnold.setSexe(Sexe.MASCULIN);
			arnold = (PersonnePhysique) tiersDAO.save(arnold);
			NO_CTB_ARNOLD = arnold.getNumero();

			PersonnePhysique nolwen = new PersonnePhysique(false);
			nolwen.setPrenomUsuel("Nowlen");
			nolwen.setNom("Raflss");
			nolwen.setSexe(Sexe.FEMININ);
			nolwen = (PersonnePhysique) tiersDAO.save(nolwen);
			NO_CTB_NOLWEN = nolwen.getNumero();

			PersonnePhysique alf = new PersonnePhysique(false);
			alf.setPrenomUsuel("Alf");
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
	public void testAnnuleForPrincipalAvecPrecedentAdjacent() throws Exception {

		class Ids {
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}

		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) {

				// Un contribuable avec deux fors principaux adjacent
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

				final ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 3, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				premierForPrincipal.setTiers(eric);

				final ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 4, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);
				secondForPrincipal.setTiers(eric);

				final Ids ids = new Ids();
				ids.premierForPrincipalId = premierForPrincipal.getId();
				ids.secondForPrincipalId = secondForPrincipal.getId();
				return ids;
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final ForFiscalPrincipal secondForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.secondForPrincipalId);
				assertNotNull(secondForPrincipal);

				// annulation du second for principal
				final ForFiscal reouvert = tiersService.annuleForFiscal(secondForPrincipal);

				// vérification que le second for est bien annulé
				assertTrue(secondForPrincipal.isAnnule());

				// vérification que le premier for est bien annulé aussi ...
				final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.premierForPrincipalId);
				assertNotNull(premierForPrincipal);
				assertTrue(premierForPrincipal.isAnnule());

				// ... et remplacé par un for principal ré-ouvert
				assertNotNull(reouvert);
				assertEquals(ForFiscalPrincipalPP.class, reouvert.getClass());
				assertFalse(reouvert.isAnnule());
				assertEquals(date(1983, 4, 13), reouvert.getDateDebut());
				assertEquals(MotifFor.MAJORITE, ((ForFiscalPrincipalPP) reouvert).getMotifOuverture());
				assertNull(reouvert.getDateFin());
				assertNull(((ForFiscalPrincipalPP) reouvert).getMotifFermeture());
			}
		});
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
			public Object execute(TransactionStatus status) {

				// Un contribuable avec deux fors principaux non-adjacents
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

				ForFiscalPrincipal forPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 2, 28), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				forPrincipal.setTiers(eric);

				ForFiscalSecondaire forFiscalSecondaire =
						addForSecondaire(eric, date(1990, 4, 13), MotifFor.ACHAT_IMMOBILIER, date(2008, 2, 28), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne,
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
			public Object execute(TransactionStatus status) {

				// Un contribuable décédé
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
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
				MotifFor.ACHAT_IMMOBILIER,
				MotifFor.VENTE_IMMOBILIER,
				GenreImpot.REVENU_FORTUNE);
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
			public Object execute(TransactionStatus status) {

				// Un contribuable avec deux fors principaux non-adjacents
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

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
	public void testAnnuleForPrincipalAvecSuivant() throws Exception {

		class Ids {
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}

		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) {

				// Un contribuable avec deux fors principaux adjacent
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);

				final ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 3, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				premierForPrincipal.setTiers(eric);

				final ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 4, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);
				secondForPrincipal.setTiers(eric);

				final Ids ids = new Ids();
				ids.secondForPrincipalId = secondForPrincipal.getId();
				ids.premierForPrincipalId = premierForPrincipal.getId();
				return ids;
			}
		});

		try {
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.premierForPrincipalId);
					assertNotNull(premierForPrincipal);

					// l'annulation doit sauter...
					tiersService.annuleForFiscal(premierForPrincipal);
				}
			});
			fail();
		}
		catch (ValidationException e) {
			// effectivement, ça saute...
			assertTrue(e.getMessage().contains("Seul le dernier for fiscal principal peut être annulé"));
		}

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.premierForPrincipalId);
				assertNotNull(premierForPrincipal);
				final ForFiscalPrincipal secondForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.secondForPrincipalId);
				assertNotNull(secondForPrincipal);

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
		});
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
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
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
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HS,null,null,MockCommune.Lausanne,MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
		}

		// Contribuable avec un for principal fermé
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), date(2008, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), date(2008, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2004, 7, 3)));
			assertForGestion(date(2000, 1, 1), date(2008, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2008, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalFermeDansMemePeriode() {

		// Contribuable avec un for principal ferme dans la meme période
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2015, 4, 1), MotifFor.ARRIVEE_HC, date(2015, 6, 30), MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2015, 7, 1), MotifFor.DEPART_HC, null, null, MockCommune.Bern, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(2015, 6, 30)));
			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(0, histo.size());
		}


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable2ForFermeFermeDansMemePeriode() {

		// Contribuable avec un for principal ferme dans la meme période
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2015, 4, 1), MotifFor.ARRIVEE_HC, date(2015, 5, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2015,6, 1), MotifFor.DEMENAGEMENT_VD, date(2015, 6, 30), MotifFor.DEPART_HC, MockCommune.Aubonne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2015, 7, 1), MotifFor.DEPART_HC, null, null, MockCommune.Bern, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(2015, 5, 30)));
			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(0, histo.size());
		}


	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalFermeDansPeriodeDifferente() {

		// Contribuable avec un for principal ferme dans la meme période
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2014, 4, 1), MotifFor.ARRIVEE_HC, date(2015, 6, 30), MotifFor.ARRIVEE_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2015, 7, 1), MotifFor.DEPART_HC, null, null, MockCommune.Bern, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2014, 4, 1), date(2015,6,30), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2015,6,30)));
			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2014, 4, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
		}


	}


	//SIFISC-16670
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableApresDepartHsEtMariage() {

		// Contribuable avec un for principal ferme pour départ Hors suisse
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2012, 6, 15), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, date(2012, 7, 1), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2012,7, 2), MotifFor.DEPART_HS, date(2012, 11, 30), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.France, ModeImposition.ORDINAIRE);

			assertForGestion(date(2012, 6,15),date(2012, 7, 1), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2012, 6, 20)));
			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2012, 6, 15), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
		}


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalHorsCanton() {

		// Contribuable avec un for principal ouvert hors-canton
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HC, null, null, MockCommune.Neuchatel, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

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
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.SOURCE);

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
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2002, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);
		addForPrincipal(c, date(2003, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, date(2008, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), histo.get(0));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Moudon.getNoOFS(), histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForSecondaire() {

		// Contribuable avec un for secondaire activite independante
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
			addForSecondaire(c, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne, MotifRattachement.ACTIVITE_INDEPENDANTE);
			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
		}

		// Contribuable avec un for secondaire immeuble
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
			addForSecondaire(c,date(2000,1,1),MotifFor.ACHAT_IMMOBILIER,null,null,MockCommune.Lausanne,MotifRattachement.IMMEUBLE_PRIVE);
			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, null));

			final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
			assertEquals(1, histo.size());
			assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
		}

		// Contribuable avec un for administrateur
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
			addForAutreElementImposable(c,date(2000,1,1),null,MockCommune.Lausanne,TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,MotifRattachement.ADMINISTRATEUR);

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
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
		addForSecondaire(c,date(2000,1,1),MotifFor.ACHAT_IMMOBILIER,date(2002, 12, 31),MotifFor.VENTE_IMMOBILIER,MockCommune.Lausanne,MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c, date(2003, 1, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);


		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getDernierForGestionConnu(c, null));
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2008, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), histo.get(0));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDeuxForsSecondairesSeRecoupant() {

		// Contribuable avec deux fors secondaires se recoupant
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
		addForSecondaire(c,date(2000,1,1),MotifFor.ACHAT_IMMOBILIER,date(2007, 12, 31),MotifFor.VENTE_IMMOBILIER,MockCommune.Lausanne,MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c, date(2003, 1, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);


		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getDernierForGestionConnu(c, null));
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2005, 9, 12)));
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2008, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2007, 12, 31), MockCommune.Lausanne.getNoOFS(), histo.get(0));
		assertForGestion(date(2008, 1, 1), null, MockCommune.Echallens.getNoOFS(), histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalOuvertEtUnForSecondaireOuvert() {

		// Contribuable avec un for principal ouvert et un for secondaire ouvert
		PersonnePhysique c =addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HC, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForSecondaire(c,date(2000,1,1),MotifFor.ACHAT_IMMOBILIER,null,null,MockCommune.Moudon,MotifRattachement.IMMEUBLE_PRIVE);



		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalFermeEtUnForSecondaireOuvert() {

		// Contribuable avec un for principal fermé et un for secondaires ouvert
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);

		addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HC, date(2004, 12, 31),MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, date(2005, 1, 1), MotifFor.DEPART_HS, null,null, MockPays.Colombie);
		addForSecondaire(c,date(2000,1,1),MotifFor.ACHAT_IMMOBILIER,null,null,MockCommune.Moudon,MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31),MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31),MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2004, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, date(2005, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFS(), histo.get(0));
		assertForGestion(date(2005, 1, 1), null,MockCommune.Moudon.getNoOFS(), histo.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalOuvertEtUnForSecondaireFerme() {

		// Contribuable avec un for principal ouvert et un for secondaire fermé
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);

		addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HC, null,null, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForSecondaire(c,date(2000,1,1),MotifFor.ACHAT_IMMOBILIER,date(2004, 12, 31),MotifFor.VENTE_IMMOBILIER,MockCommune.Moudon,MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2004, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2005, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalHorsCantonEtUnForSecondaire() {

		// Contribuable avec un for principal hors-canton et un for secondaire ouvert dans le canton
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		c.setNom("Duvoisin");
		addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HC, null,null, MockCommune.Neuchatel, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForSecondaire(c,date(2000,1,1),MotifFor.ACHAT_IMMOBILIER, null,null,MockCommune.Moudon,MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Moudon.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, MockCommune.Moudon.getNoOFS(), histo.get(0));
	}


	/*
	   Calcul du for de gestion
	   Contribuable avec  1 for principal vaudois dans avec un départ HC

	                                           +--------------------------------+
for principal	                               | Mode imposition: ORDINAIRE     |
	                                           +--------------------------------+
	                                        arrivée HS                     Départ HC

	    |-------------------------------------------------------------------------------------------------------------------------|
	 debut PF                                                                                                                   fin PF
	*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableForsAvecDepartHorsCanton() {

		PersonnePhysique c = addHabitant(123654L);
		final RegDate dateDebutForAvecDepartHC = date(2014, 6, 15);
		final RegDate dateFinForAvecDepartHC = date(2014, 10, 1);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		//Le for avec Départ HC fait qu'il n'y a pas d'assujettissement sur le canton
		assertNull(tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));
	}


	/*
	   Calcul du for de gestion
	   Contribuable avec  2 fors principaux vaudois dans la même PF: le premier est un départ HS, le second un départ HC

	                    ----------------------------------------+                        +--------------------------------+
for principal	              Mode imposition: ORDINAIRE        |                        | Mode imposition: ORDINAIRE     |
	                    ----------------------------------------+                        +--------------------------------+
	                                                       Départ HS                 arrivée HS                     Départ HC

	    |-------------------------------------------------------------------------------------------------------------------------|
	 debut PF                                                                                                                   fin PF
	*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable2ForsEt1DepartHorsCanton() {

		PersonnePhysique c = addHabitant(123547L);
		final RegDate dateDebutForAvecDepartHS = date(2001, 1, 1);
		final RegDate dateFinForAvecDepartHS = date(2014, 2, 1);

		addForPrincipal(c, dateDebutForAvecDepartHS, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

		//For Hors suisse
		final RegDate dateDebutHS = date(2014, 2, 2);
		final RegDate dateFinHS = date(2014, 6, 14);

		addForPrincipal(c, dateDebutHS, MotifFor.DEPART_HS, dateFinHS, MotifFor.ARRIVEE_HS,MockPays.CoreeSud);

		final RegDate dateDebutForAvecDepartHC = date(2014, 6, 15);
		final RegDate dateFinForAvecDepartHC = date(2014, 10, 1);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HS, dateFinForAvecDepartHC, MotifFor.DEPART_HC, MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		//Le for avec Départ Hors Suisse est for de gestion
		assertForGestion(dateDebutForAvecDepartHS, dateFinForAvecDepartHS, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));
	}



	/*
	   Calcul du for de gestion
	   Contribuable avec  3 fors principaux vaudois dans la même PF: le premier et le second finissent en déménagement, le dernier est un départ HC

	                    +---------------------------------+-----------------------------+--------------------------------+
for principal	        |             Bussigny            |             Morges          |              Lausanne          |
	                    +---------------------------------+-----------------------------+--------------------------------+
	                   arivee HS                        demenagement VD             demenagement VD                  Départ HC

	    |-------------------------------------------------------------------------------------------------------------------------|
	 debut PF                                                                                                                   fin PF
	*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable3ForsEt1DepartHorsCanton() {

		PersonnePhysique c = addHabitant(7894562L);
		final RegDate dateDebutFor1 = date(2014, 1, 1);
		final RegDate dateFinFor1 = date(2014, 3, 31);
		final RegDate dateDebutFor2 = date(2014, 4, 1);
		final RegDate dateFinFor2 = date(2014, 7, 31);
		final RegDate dateDebutFor3 = date(2014, 8, 1);
		final RegDate dateFinFor3 = date(2014, 8, 31);
		addForPrincipal(c, dateDebutFor1, MotifFor.ARRIVEE_HS, dateFinFor1, MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor2, MotifFor.DEMENAGEMENT_VD, dateFinFor2, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor3, MotifFor.DEMENAGEMENT_VD, dateFinFor3, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		//Aucun for de gestion connu
		assertNull(tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));
		assertNull(tiersService.getDernierForGestionConnu(c, null));
	}

	/*
   Calcul du for de gestion
   Contribuable avec  3 fors principaux vaudois dans la même PF et 1 dasn la période précédente: le dernier est un départ HC

	               --------------------+---------------------------------+-----------------------------+--------------------------------+
     for principal	 Echallens         |             Bussigny            |             Morges          |              Lausanne          |
                   --------------------+---------------------------------+-----------------------------+--------------------------------+
				                   demenagement VD                     demenagement VD            demenagement VD                Départ HC

	             ----------------------|-------------------------------------------------------------------------------------------------------------|
                                     debut PF                                                                                                      fin PF
*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable4ForsEt1DepartHorsCanton() {

		PersonnePhysique c = addHabitant(3694521L);
		final RegDate dateDebutFor0 = date(2011, 1, 1);
		final RegDate dateFinFor0 = date(2013, 12, 31);
		final RegDate dateDebutFor1 = date(2014, 1, 1);
		final RegDate dateFinFor1 = date(2014, 3, 31);
		final RegDate dateDebutFor2 = date(2014, 4, 1);
		final RegDate dateFinFor2 = date(2014, 7, 31);
		final RegDate dateDebutFor3 = date(2014, 8, 1);
		final RegDate dateFinFor3 = date(2014, 8, 31);
		addForPrincipal(c, dateDebutFor0, MotifFor.ARRIVEE_HS, dateFinFor0, MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor1, MotifFor.DEMENAGEMENT_VD, dateFinFor1, MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor2, MotifFor.DEMENAGEMENT_VD, dateFinFor2, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor3, MotifFor.DEMENAGEMENT_VD, dateFinFor3, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		assertNull(tiersService.getForGestionActif(c, date(2014, 3, 1)));
		final ForGestion dernierForGestionConnu = tiersService.getDernierForGestionConnu(c, null);
		assertNotNull(dernierForGestionConnu);
		assertForGestion(dateDebutFor0, dateFinFor0, MockCommune.Echallens.getNoOFS(), dernierForGestionConnu);
		final List<ForGestion> forsGestionHisto = tiersService.getForsGestionHisto(c);
		assertEquals(1, forsGestionHisto.size());
		final ForGestion forGestionHistorique = forsGestionHisto.get(0);
		assertForGestion(dateDebutFor0, null, MockCommune.Echallens.getNoOFS(), forGestionHistorique);

	}



	/*
   Calcul du for de gestion
   Contribuable avec  3 fors principaux vaudois dans la même PF et 1 dans la période précédente et qui déborde sur la periode
    courante: le dernier est un départ HC

	               -------------------------+---------------------------------+-----------------------------+--------------------------------+
     for principal	 Echallens              |             Bussigny            |             Morges          |              Lausanne          |
                   -------------------------+---------------------------------+-----------------------------+--------------------------------+
				                   demenagement VD                     demenagement VD            demenagement VD                Départ HC

	             ----------------------|-------------------------------------------------------------------------------------------------------------|
                                     debut PF                                                                                                      fin PF
*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable4Fors1DansPeriodePrecedente1DepartHorsCanton() {

		PersonnePhysique c = addHabitant(9876321L);
		final RegDate dateDebutFor0 = date(2011, 1, 1);
		final RegDate dateFinFor0 = date(2013, 12, 31);
		final RegDate dateDebutFor1 = date(2014, 1, 1);
		final RegDate dateFinFor1 = date(2014, 3, 31);
		final RegDate dateDebutFor2 = date(2014, 4, 1);
		final RegDate dateFinFor2 = date(2014, 7, 31);
		final RegDate dateDebutFor3 = date(2014,8 , 1);
		final RegDate dateFinFor3 = date(2014, 8, 31);

		addForPrincipal(c, dateDebutFor0, MotifFor.ARRIVEE_HS, dateFinFor0, MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor1, MotifFor.DEMENAGEMENT_VD, dateFinFor1, MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor2, MotifFor.DEMENAGEMENT_VD, dateFinFor2, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor3, MotifFor.DEMENAGEMENT_VD, dateFinFor3, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		//Le for avec Départ Hors Suisse est for de gestion
		assertNull(tiersService.getForGestionActif(c, date(2015, 12, 31)));
		final ForGestion dernierForGestionConnu = tiersService.getDernierForGestionConnu(c, null);
		assertNotNull(dernierForGestionConnu);
		assertEquals(dateDebutFor0, dernierForGestionConnu.getDateDebut());
		assertEquals(MockCommune.Echallens.getNoOFS(),dernierForGestionConnu.getNoOfsCommune());
		assertForGestion(dateDebutFor0, null, MockCommune.Echallens.getNoOFS(), tiersService.getForsGestionHisto(c).get(0));
	}

	/*
Calcul du for de gestion
Contribuable avec  3 fors principaux vaudois dans la même PF et 1 dans la période précédente et qui déborde sur la periode
courante: le dernier est un départ HC dans la période suivante

			   -------------------------+---------------------------------+-----------------------------+--------------------------------+
 for principal	 Echallens              |             Bussigny            |             Morges          |              Lausanne          |
			   -------------------------+---------------------------------+-----------------------------+--------------------------------+
							   demenagement VD                     demenagement VD            demenagement VD                Départ HC

			 ----------------------|----------------------------------------------------------------------------------------|------------------
								 debut PF                                                                                 fin PF
*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable4Fors1DansPeriodePrecedente1DepartHorsCantonPeriodeSuivante() {

		PersonnePhysique c = addHabitant(753159L);
		final RegDate dateDebutFor0 = date(2011, 1, 1);
		final RegDate dateFinFor0 = date(2013, 12, 31);
		final RegDate dateDebutFor1 = date(2014, 1, 1);
		final RegDate dateFinFor1 = date(2014, 3, 31);
		final RegDate dateDebutFor2 = date(2014, 4, 1);
		final RegDate dateFinFor2 = date(2014, 7, 31);
		final RegDate dateDebutFor3 = date(2014,8 , 1);
		final RegDate dateFinFor3 = date(2015, 3, 31);

		addForPrincipal(c, dateDebutFor0, MotifFor.ARRIVEE_HS, dateFinFor0, MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor1, MotifFor.DEMENAGEMENT_VD, dateFinFor1, MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor2, MotifFor.DEMENAGEMENT_VD, dateFinFor2, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateDebutFor3, MotifFor.DEMENAGEMENT_VD, dateFinFor3, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

		//Le for avec Départ Hors Suisse est for de gestion
		assertNull(tiersService.getForGestionActif(c, date(2015, 12, 31)));
		final ForGestion dernierForGestionConnu = tiersService.getDernierForGestionConnu(c, null);
		assertNotNull(dernierForGestionConnu);
		assertEquals(dateDebutFor3, dernierForGestionConnu.getDateDebut());
		assertEquals(MockCommune.Lausanne.getNoOFS(), dernierForGestionConnu.getNoOfsCommune());
	}



	/*
	   Calcul du for de gestion
	   Contribuable avec  2 fors principaux vaudois dans la même PF: le premier est un départ HS, le second un départ HC mIXTE 2

	                    ----------------------------------------+                        +--------------------------------+
for principal	              Mode imposition: ORDINAIRE        |                        | Mode imposition: MIXTE_2       |
	                    ----------------------------------------+                        +--------------------------------+
	                                                       Départ HS                 arrivée HS                     Départ HC

	    |-------------------------------------------------------------------------------------------------------------------------|
	 debut PF                                                                                                                   fin PF
	*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable2ForsEt1DepartHorsCantonMixte2() {

		PersonnePhysique c = addHabitant(8632147L);
		final RegDate dateDebutForAvecDepartHS = date(2001, 1, 1);
		final RegDate dateFinForAvecDepartHS = date(2014, 2, 1);

		addForPrincipal(c, dateDebutForAvecDepartHS, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

		//For Hors Suisse
		final RegDate dateDebutHS = date(2014, 2, 2);
		final RegDate dateFinHS = date(2014, 6, 14);
		addForPrincipal(c, dateDebutHS, MotifFor.DEPART_HS, dateFinHS, MotifFor.ARRIVEE_HS,MockPays.Colombie);


		final RegDate dateDebutForAvecDepartHC = date(2014, 6, 15);
		final RegDate dateFinForAvecDepartHC = date(2014, 10, 1);


		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HS, dateFinForAvecDepartHC, MotifFor.DEPART_HC, MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);
		//Le for avec Départ Hors canton est for de gestion
		assertForGestion(dateDebutForAvecDepartHC, dateFinForAvecDepartHC, MockCommune.Moudon.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));
	}


/*
   Calcul du for de gestion
   Contribuable avec  2 fors principaux vaudois dans la même PF: le premier est un départ HS, le second un départ HC
   1 for secondaire vaudois fermé avant le départ hors suisses

                    ----------------------------+
For scondaire                                   |
                    ----------------------------+


					----------------------------------------+                        +--------------------------------+
For principal	       Mode imposition: ORDINAIRE           |                        |   Mode imposition: ORDINAIRE   |
					----------------------------------------+                        +--------------------------------+
													   Départ HS                 arrivée HS                     Départ HC

	|-------------------------------------------------------------------------------------------------------------------------|
 debut PF                                                                                                                   fin PF
*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable2ForsPrincipalForSecondaireAvantDepartHS() {

		PersonnePhysique c =addHabitant(94621453L);
		final RegDate dateFinForAvecDepartHS = date(2014, 2, 1);
		final RegDate dateDebutForAvecDepartHS = date(2001, 1, 1);
		final RegDate dateDebutForSecondaire = date(2001, 2, 1);
		final RegDate dateFinForSecondaire = dateFinForAvecDepartHS.addMonths(-1);
		addForPrincipal(c, dateDebutForAvecDepartHS, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		final RegDate dateFinForHC = date(2014, 10, 1);
		final RegDate dateDebutForAvecDepartHC = date(2014, 6, 15);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HS, dateFinForHC, MotifFor.DEPART_HC, MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);


		addForSecondaire(c,dateDebutForSecondaire,MotifFor.ACHAT_IMMOBILIER,dateFinForSecondaire,MotifFor.VENTE_IMMOBILIER,MockCommune.Echallens,MotifRattachement.IMMEUBLE_PRIVE);
		//Le for avec Départ Hors Suisse est for de gestion
		assertForGestion(dateDebutForAvecDepartHS, dateFinForAvecDepartHS, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));

	}


	/*
   Calcul du for de gestion
   Contribuable avec  2 fors principaux vaudois dans la même PF: le premier est un départ HS, le second un départ HC
   1 for secondaire vaudois fermé avant le départ hors suisses

                    ----------------------------------------+
For secondaire                                              |
                    ----------------------------------------+


					----------------------------------------+                        +--------------------------------+
For principal	         Mode imposition: ORDINAIRE         |                        |Mode imposition: ORDINAIRE      |
					----------------------------------------+                        +--------------------------------+
													   Départ HS                 arrivée HS                     Départ HC

	|-------------------------------------------------------------------------------------------------------------------------|
 debut PF                                                                                                                   fin PF
*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable2ForsPrincipalForSecondaireMemeFinDepartHS() {

		PersonnePhysique c = addHabitant(7493515L);
		final RegDate dateFinForAvecDepartHS = date(2014, 2, 1);
		final RegDate dateDebutForAvecDepartHS = date(2001, 1, 1);
		final RegDate dateDebutForSecondaire = date(2001, 1, 1);
		final RegDate dateDebutForAvecDepartHC = date(2014, 6, 15);
		//noinspection UnnecessaryLocalVariable
		final RegDate dateFinForSecondaire = dateFinForAvecDepartHS;
		addForPrincipal(c, dateDebutForAvecDepartHS, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		final RegDate dateFinForHC = date(2014, 10, 1);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HS, dateFinForHC, MotifFor.DEPART_HC, MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

		addForSecondaire(c,dateDebutForSecondaire,MotifFor.ACHAT_IMMOBILIER,dateFinForSecondaire,MotifFor.VENTE_IMMOBILIER,MockCommune.Echallens,MotifRattachement.IMMEUBLE_PRIVE);
		//Le for avec Départ Hors Suisse est for de gestion
		assertForGestion(dateDebutForAvecDepartHS, dateFinForAvecDepartHS, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));

	}


	/*
   Calcul du for de gestion
   Contribuable avec  2 fors principaux vaudois dans la même PF: le premier est un départ HS, le second un départ HC
   1 for secondaire vaudois fermé entre le départ hors suisse et l'arrivée HS

                    -----------------------------------------------+
For secondaire                                                     |
                    -----------------------------------------------+


					----------------------------------------+                        +--------------------------------+
For principal	        Mode imposition: ORDINAIRE          |                        |    Mode imposition: ORDINAIRE  |
					----------------------------------------+                        +--------------------------------+
													   Départ HS                 arrivée HS                     Départ HC

	|-------------------------------------------------------------------------------------------------------------------------|
 debut PF                                                                                                                   fin PF
*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable2ForsPrincipalForSecondaireApresFinDepartHS() {

		PersonnePhysique c = addHabitant(93147856L);
		final RegDate dateDebutForAvecDepartHS = date(2001, 1, 1);
		final RegDate dateFinForAvecDepartHS = date(2014, 2, 1);
		final RegDate dateDebutForSecondaire = date(2001, 2, 1);
		final RegDate dateDebutForAvecDepartHC = date(2014, 6, 15);
		final RegDate dateFinForHC = date(2014, 10, 1);
		final RegDate dateFinForSecondaire = date(2014, 5, 1);


		addForPrincipal(c, dateDebutForAvecDepartHS, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

		//For Hors suisse
		final RegDate dateDebutHS = date(2014, 2, 2);
		final RegDate dateFinHS = date(2014, 6, 14);
		addForPrincipal(c, dateDebutHS, MotifFor.DEPART_HS, dateFinHS, MotifFor.ARRIVEE_HS,MockPays.Allemagne);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HS, dateFinForHC, MotifFor.DEPART_HC, MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);

		addForSecondaire(c,dateDebutForSecondaire,MotifFor.ACHAT_IMMOBILIER,dateFinForSecondaire,MotifFor.VENTE_IMMOBILIER,MockCommune.Echallens,MotifRattachement.IMMEUBLE_PRIVE);
		//Le for secondaire  est for de gestion
		assertForGestion(dateDebutForSecondaire, dateFinForSecondaire, MockCommune.Echallens.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));
		assertForGestion(dateDebutForSecondaire, dateFinForSecondaire, MockCommune.Echallens.getNoOFS(), tiersService.getDernierForGestionConnu(c, null));

	}

	/*
Calcul du for de gestion
Contribuable avec  2 fors principaux vaudois dans la même PF: le premier est un départ HS, le second un sourcier mixte_2 avec départ HC
1 for secondaire vaudois fermé entre le départ hors suisse et l'arrivée HS

				-----------------------------------------------+
For secondaire                                                 |
				-----------------------------------------------+


				----------------------------------------+                        +--------------------------------+
For principal	        Mode imposition: ORDINAIRE      |                        |    Mode imposition: MIXTE_2    |
				----------------------------------------+                        +--------------------------------+
												   Départ HS                 arrivée HS                     Départ HC

|-------------------------------------------------------------------------------------------------------------------------|
debut PF                                                                                                                   fin PF
*/
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuable2ForsPrincipalForSecondaireApresFinDepartHSEtMixte2() {

		PersonnePhysique c = addHabitant(12048896L);
		final RegDate dateDebutForAvecDepartHS = date(2001, 1, 1);
		final RegDate dateFinForAvecDepartHS = date(2014, 2, 1);

		final RegDate dateFinForAvecDepartHC = date(2014, 10, 1);
		final RegDate dateDebutForAvecDepartHC = date(2014, 6, 15);

		//For Hors suisse
		final RegDate dateDebutHS = date(2014, 2, 2);
		final RegDate dateFinHS = date(2014, 6, 14);

		final RegDate dateFinForSecondaire = date(2014, 5, 1);

		addForPrincipal(c, dateDebutForAvecDepartHS, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);



		addForPrincipal(c, dateDebutHS, MotifFor.DEPART_HS, dateFinHS, MotifFor.ARRIVEE_HS,MockPays.Japon);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HS, dateFinForAvecDepartHC, MotifFor.DEPART_HC, MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);

		addForSecondaire(c, dateDebutForAvecDepartHS,MotifFor.ACHAT_IMMOBILIER,dateFinForSecondaire,MotifFor.VENTE_IMMOBILIER,MockCommune.Echallens,MotifRattachement.IMMEUBLE_PRIVE);
		//Le for avec départ HC  est for de gestion
		assertForGestion(dateDebutForAvecDepartHC, dateFinForAvecDepartHC, MockCommune.Moudon.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));

	}



	//Calcul du for de gestion
	//Contribuable avec un for principal hors-canton et un for secondaire ouvert dans le canton et fermé avant le départ  hors Canton
	/*
	                       --------------------------------+
	  For scondaire                                        |
	                       --------------------------------+

	                       --------------------------------------------------------------------------+
	 For Principal vaudois                  Mode imposition: ORDINAIRE                               |
	                       --------------------------------------------------------------------------+
	                                                                                              Départ HC

	    |----------------------------------------------------------------------------------------------------------------|
	 debut PF                                                                                                          fin PF
	*/

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDepartHorsCantonEtUnForSecondaire() {

		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		final RegDate dateDebutForAvecDepartHC = date(2001, 1, 1);
		final RegDate dateFinForAvecDepartHC = date(2014, 6, 14);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		final RegDate dateDebutForSecondaire = date(2001, 2, 1);
		final RegDate dateFinForSecondaire = date(2014, 3, 20);
		addForSecondaire(c,dateDebutForSecondaire,MotifFor.ACHAT_IMMOBILIER,dateFinForSecondaire,MotifFor.VENTE_IMMOBILIER,MockCommune.Echallens,MotifRattachement.IMMEUBLE_PRIVE);
		//Le for secondaire  est for de gestion
		assertForGestion(dateDebutForSecondaire, dateFinForSecondaire, MockCommune.Echallens.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));
	}

	//Calcul du for de gestion
	//Contribuable avec un for principal hors-canton sourcier MIXTE_2 et un for secondaire ouvert dans le canton et fermé avant le départ  hors Canton
	/*
	                       --------------------------------+
	  For scondaire                                        |
	                       --------------------------------+

	                       --------------------------------------------------------------------------+
	 For Principal vaudois             Mode imposition MIXTE_2                                       |
	                       --------------------------------------------------------------------------+
	                                                                                              Départ HC

	    |----------------------------------------------------------------------------------------------------------------|
	 debut PF                                                                                                          fin PF
	*/

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDepartHorsCantonMixte2EtUnForSecondaire() {

		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);

		final RegDate dateDebutForAvecDepartHC = date(2001, 1, 1);
		final RegDate dateFinForAvecDepartHC = date(2014, 6, 14);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.MIXTE_137_2);
		final RegDate dateDebutForSecondaire = date(2001, 1, 1);
		final RegDate dateFinForSecondaire = date(2014, 3, 20);
		addForSecondaire(c,dateDebutForSecondaire,MotifFor.ACHAT_IMMOBILIER,dateFinForSecondaire,MotifFor.VENTE_IMMOBILIER,MockCommune.Echallens,MotifRattachement.IMMEUBLE_PRIVE);
		//Le for depart HC  est for de gestion
		assertForGestion(dateDebutForAvecDepartHC, dateFinForAvecDepartHC, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 12, 31)));
	}

	//Calcul du for de gestion
	//Contribuable avec un for principal hors-canton et un for secondaire ouvert dans le canton
	/*
	                       ----------------------------------------------------------+
	  For scondaire                                                                   ---->>>
	                       ----------------------------------------------------------+

	                       ------------------------------------------------+
	 For Principal vaudois                                                 |
	                       ------------------------------------------------+
	                                                                    Départ HC

	    |----------------------------------------------------------------------------------------------------------------|
	 debut PF                                                                                                          fin PF
	*/

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableDepartHorsCantonEtUnForSecondaireOuvert() {

		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		final RegDate dateDebutForAvecDepartHC = date(2001, 1, 1);
		final RegDate dateFinForAvecDepartHC = date(2014, 6, 14);
		addForPrincipal(c, dateDebutForAvecDepartHC, MotifFor.ARRIVEE_HC, dateFinForAvecDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		addForPrincipal(c, dateFinForAvecDepartHC.addDays(1), MotifFor.DEPART_HC, null,null, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		final RegDate dateDebutForSecondaire = date(2005, 3, 20);
		addForSecondaire(c,dateDebutForSecondaire,MotifFor.ACHAT_IMMOBILIER,null,null,MockCommune.Echallens,MotifRattachement.IMMEUBLE_PRIVE);
		//Le for secondaire  est for de gestion
		assertForGestion(dateDebutForSecondaire, null, MockCommune.Echallens.getNoOFS(), tiersService.getDernierForGestionConnu(c, date(2014, 1, 1)));
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalHorsCantonEtDeuxForsSecondairesSeRecoupant() {

		// Contribuable avec un for principal hors-canton et deux fors secondaires dans le canton se recoupant
		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HC, null, null, MockCommune.Neuchatel, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);

		addForSecondaire(c, date(2002, 1, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER,date(2003, 12, 31), MotifFor.VENTE_IMMOBILIER,MockCommune.Bussigny,MotifRattachement.IMMEUBLE_PRIVE);


		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), MockCommune.Bussigny.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), MockCommune.Bussigny.getNoOFS(), tiersService.getForGestionActif(c, date(2003, 12, 31)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2004, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Echallens.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(2, histo.size());
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), MockCommune.Bussigny.getNoOFS(), histo.get(0));
		assertForGestion(date(2004, 1, 1), null, MockCommune.Echallens.getNoOFS(), histo.get(1));
	}

	/**
	 * Contribuable avec un for principal dans le canton fermé suivi d'un for principal hors-canton ouvert et deux fors secondaires dans le canton se recoupant
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForGestionContribuableUnForPrincipalCantonFermeUnForPrincipalHorsCantonOuvertEtDeuxForsSecondairesSeRecoupant() {

		PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c, date(1990, 1, 1), MotifFor.ARRIVEE_HC, date(2000, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		addForPrincipal(c, date(2001, 1, 1), MotifFor.DEPART_HC, date(2001, 1, 1), MotifFor.DEPART_HS, MockCommune.Neuchatel, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		addForPrincipal(c, date(2001, 1, 2), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
		addForSecondaire(c,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER,date(2003, 12, 31), MotifFor.VENTE_IMMOBILIER,MockCommune.Bussigny,MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c, date(2002, 1, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1989, 12, 31)));
		assertForGestion(date(1990, 1, 1), date(2000, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(1990, 1, 1)));
		assertForGestion(date(1990, 1, 1), date(2000, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), MockCommune.Bussigny.getNoOFS(), tiersService.getForGestionActif(c, date(2001, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2003, 12, 31), MockCommune.Bussigny.getNoOFS(), tiersService.getForGestionActif(c, date(2003, 12, 31)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Renens.getNoOFS(), tiersService.getForGestionActif(c, date(2004, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Renens.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 6, 13)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Renens.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
		assertForGestion(date(2002, 1, 1), null, MockCommune.Renens.getNoOFS(), tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(3, histo.size());
		assertForGestion(date(1990, 1, 1), date(2000, 12, 31), MockCommune.Lausanne.getNoOFS(), histo.get(0));
		assertForGestion(date(2001, 1, 1), date(2003, 12, 31), MockCommune.Bussigny.getNoOFS(), histo.get(1));
		assertForGestion(date(2004, 1, 1), null, MockCommune.Renens.getNoOFS(), histo.get(2));
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
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
			addForPrincipal(c, date(1990, 1, 1), MotifFor.ARRIVEE_HC, date(1998, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Bex, MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HC, date(2001, 1, 1), MotifFor.DEPART_HS, MockCommune.Neuchatel, MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2001, 1, 2), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
			addForSecondaire(c,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER, date(2003, 12, 31), MotifFor.VENTE_IMMOBILIER,MockCommune.Renens,MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(c,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER,null, null,MockCommune.Bex,MotifRattachement.IMMEUBLE_PRIVE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1989, 12, 31)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), MockCommune.Bex.getNoOFS(), tiersService.getForGestionActif(c, date(1990, 1, 1)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), MockCommune.Bex.getNoOFS(), tiersService.getForGestionActif(c, date(1998, 12, 31)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Bex.getNoOFS(), tiersService.getForGestionActif(c, date(2001, 1, 1))); // numéro Ofs de for3
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
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);

			addForPrincipal(c, date(1990, 1, 1), MotifFor.ARRIVEE_HC, date(1998, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HC, date(2001, 1, 1), MotifFor.DEPART_HS, MockCommune.Neuchatel, MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2001, 1, 2), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
			addForSecondaire(c,date(2000, 1, 1),MotifFor.DEBUT_EXPLOITATION, date(2003, 12, 31), MotifFor.FIN_EXPLOITATION,MockCommune.Bex,MotifRattachement.ACTIVITE_INDEPENDANTE);
			addForSecondaire(c,date(2000, 1, 1),MotifFor.DEBUT_EXPLOITATION,null, null,MockCommune.Aubonne,MotifRattachement.ACTIVITE_INDEPENDANTE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1989, 12, 31)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(1990, 1, 1)));
			assertForGestion(date(1990, 1, 1), date(1998, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(1998, 12, 31)));
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
			assertForGestion(date(1990, 1, 1), date(1999, 12, 31), MockCommune.Lausanne.getNoOFS(), histo.get(0));
			assertForGestion(date(2000, 1, 1), null, aubonne, histo.get(1));
		}

		// Cas où il n'y a pas de dernier for principal vaudois
		{
			PersonnePhysique c = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);

			addForPrincipal(c, date(2000, 1, 1), MotifFor.DEPART_HC, date(2001, 1, 1), MotifFor.DEPART_HS, MockCommune.Neuchatel, MotifRattachement.DOMICILE,
					ModeImposition.ORDINAIRE);
			addForPrincipal(c, date(2001, 1, 2), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
			addForSecondaire(c,date(2000, 1, 1),MotifFor.DEBUT_EXPLOITATION, date(2003, 12, 31), MotifFor.FIN_EXPLOITATION,MockCommune.Aubonne,MotifRattachement.ACTIVITE_INDEPENDANTE);
			addForSecondaire(c,date(2000, 1, 1),MotifFor.DEBUT_EXPLOITATION,null, null,MockCommune.Bex,MotifRattachement.ACTIVITE_INDEPENDANTE);

			assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
			assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
			// [UNIREG-1029] aubonne est placé devant bex dans l'ordre alphabétique
			assertForGestion(date(2000, 1, 1), date(2003, 12, 31), MockCommune.Aubonne.getNoOFS(), tiersService.getForGestionActif(c, date(2001, 1, 1)));
			assertForGestion(date(2000, 1, 1), date(2003, 12, 31), MockCommune.Aubonne.getNoOFS(), tiersService.getForGestionActif(c, date(2003, 12, 31)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Bex.getNoOFS(), tiersService.getForGestionActif(c, date(2004, 1, 1))); // for1 est maintenant fermé
			assertForGestion(date(2000, 1, 1), null, MockCommune.Bex.getNoOFS(), tiersService.getForGestionActif(c, date(2007, 6, 13)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Bex.getNoOFS(), tiersService.getForGestionActif(c, date(2097, 1, 1)));
			assertForGestion(date(2000, 1, 1), null, MockCommune.Bex.getNoOFS(), tiersService.getForGestionActif(c, null));

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
		PersonnePhysique c = addNonHabitant("Eymeric", "Duvoisin", date(1973, 2, 3), Sexe.MASCULIN);
		addForPrincipal(c, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2002, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		ForFiscalPrincipal for1 = addForPrincipal(c, date(2005, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bussigny, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		for1.setAnnulationDate(DateHelper.getDate(2005, 3, 1));
		assertNull(tiersService.getForGestionActif(c, date(1950, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getForGestionActif(c, date(2002, 12, 31)));
		assertNull(tiersService.getForGestionActif(c, date(2003, 1, 1)));
		assertNull(tiersService.getForGestionActif(c, null));

		final List<ForGestion> histo = tiersService.getForsGestionHisto(c);
		assertEquals(1, histo.size());
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), histo.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetDernierForGestionConnuContribuable() {

		// Contribuable sans for
		PersonnePhysique c1 = addNonHabitant("Eymeric","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		assertNull(tiersService.getDernierForGestionConnu(c1, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c1, date(2005, 1, 23)));
		assertNull(tiersService.getDernierForGestionConnu(c1, date(2030, 12, 31)));

		// Contribuable avec un for principal
		PersonnePhysique c2 = addNonHabitant("Axel","Du",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c2, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		assertNull(tiersService.getDernierForGestionConnu(c2, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c2, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c2, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c2, date(2043, 3, 11)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c2, null));

		// Contribuable avec deux fors principaux
		PersonnePhysique c3 = addNonHabitant("john","Doe",date(1973,2,3),Sexe.MASCULIN);

		addForPrincipal(c3, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2002, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		addForPrincipal(c3, date(2003, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bussigny, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		assertNull(tiersService.getDernierForGestionConnu(c3, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c3, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c3, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c3, date(2001, 9, 21)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c3, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c3, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c3, date(2033, 6, 6)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c3, null));

		// Contribuable avec un for secondaire
		PersonnePhysique c4 = addNonHabitant("Ronald","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c4, date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
		addForSecondaire(c4, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getDernierForGestionConnu(c4, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c4, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c4, null));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c4, date(2018, 12, 31)));

		// Contribuable avec deux fors secondaires
		PersonnePhysique c5 = addNonHabitant("Eymeric","trump",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c5, date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
		addForSecondaire(c5,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER,date(2002, 12, 31), MotifFor.VENTE_IMMOBILIER,MockCommune.Lausanne,MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(c5, date(2003, 1, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

		assertNull(tiersService.getDernierForGestionConnu(c5, date(1997, 3, 3)));
		assertNull(tiersService.getDernierForGestionConnu(c5, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c5, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c5, date(2002, 12, 31)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c5, date(2003, 1, 1)));
		assertForGestion(date(2003, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c5, null));

		// Contribuable avec un for principal et un for secondaire
		PersonnePhysique c6 = addNonHabitant("Saul","Goodman",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c6, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		addForSecondaire(c6,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER,null, null,MockCommune.Bussigny,MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c6, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c6, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c6, date(2022, 2, 7)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c6, null));

		// Contribuable avec un for principal fermé et un for secondaires ouvert
		PersonnePhysique c7 = addNonHabitant("Ava","Gartner",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c7, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2004, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		addForPrincipal(c7, date(2005, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.CoreeSud);
		addForSecondaire(c7,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER,null, null,MockCommune.Bussigny,MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c7, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c7, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c7, date(2004, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c7, date(2005, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c7, date(2021, 7, 17)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c7, null));

		// Contribuable avec un for principal ouvert et un for secondaire fermé
		PersonnePhysique c8 = addNonHabitant("Melchior","Duvoisin",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c8, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		addForSecondaire(c8,date(2000, 1, 1),MotifFor.ACHAT_IMMOBILIER,date(2004, 12, 31), MotifFor.VENTE_IMMOBILIER,MockCommune.Bussigny,MotifRattachement.IMMEUBLE_PRIVE);
		assertNull(tiersService.getDernierForGestionConnu(c8, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c8, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c8, date(2012, 7, 8)));
		assertForGestion(date(2000, 1, 1), null, MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c8, null));

		// Contribuable avec deux fors principaux dont le plus récent est annulé
		PersonnePhysique c9 = addNonHabitant("Eymeric","Onasys",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c9, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2002, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		ForFiscalPrincipal for9_2 = addForPrincipal(c9, date(2003, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bussigny, MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE);
		for9_2.setAnnulationDate(DateHelper.getDate(2005, 3, 1));
		assertNull(tiersService.getDernierForGestionConnu(c9, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c9, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c9, date(2002, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c9, date(2003, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c9, date(2052, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c9, null));

		// Contribuable avec deux fors principaux disjoints et fermés
		PersonnePhysique c10 = addNonHabitant("Raul","Castro",date(1973,2,3),Sexe.MASCULIN);
		addForPrincipal(c10, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2002, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE,ModeImposition.ORDINAIRE);
		addForPrincipal(c10, date(2005, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Bussigny, MotifRattachement.DOMICILE,ModeImposition.ORDINAIRE);
				assertNull(tiersService.getDernierForGestionConnu(c10, date(1999, 12, 31)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c10, date(2000, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c10, date(2001, 3, 19)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c10, date(2002, 12, 31)));
		// le for10_1 reste le for de gestion, même fermé
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c10, date(2003, 1, 1)));
		assertForGestion(date(2000, 1, 1), date(2002, 12, 31), MockCommune.Lausanne.getNoOFS(), tiersService.getDernierForGestionConnu(c10, date(2004, 12, 31)));
		// le for10_2 prend la relève à partir de là
		assertForGestion(date(2005, 1, 1), date(2007, 6, 30), MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c10, date(2005, 1, 1)));
		assertForGestion(date(2005, 1, 1), date(2007, 6, 30), MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c10, date(2052, 1, 1)));
		assertForGestion(date(2005, 1, 1), date(2007, 6, 30), MockCommune.Bussigny.getNoOFS(), tiersService.getDernierForGestionConnu(c10, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsSuisse() throws Exception {

		// individu avec nationalité suisse
		{
			MockIndividu ind = new MockIndividu();
			ind.setNationalites(Collections.singletonList(new MockNationalite(null, null, MockPays.Suisse)));
			assertTrue(tiersService.isSuisse(ind, null));
		}

		// individu avec nationalité française
		{
			MockIndividu ind = new MockIndividu();
			ind.setNationalites(Collections.singletonList(new MockNationalite(null, null, MockPays.France)));
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
				ind.setNationalites(Collections.emptyList());
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
			public Long execute(TransactionStatus status) {

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
			public Object execute(TransactionStatus status) {

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
			public Object execute(TransactionStatus status) {

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
			public Long execute(TransactionStatus status) {

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
			public Object execute(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(date(2010, 2, 24));
				tiersService.annuleForFiscal(ffp);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {

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

		final long ffpId = doInNewTransactionAndSession(transactionStatus -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Simon", date(1930, 4, 3), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 5, 2), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return ffp.getId();
		});

		// première annulation
		doInNewTransactionAndSession(transactionStatus -> {
			final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, ffpId);
			assertNotNull(ffp);
			assertFalse(ffp.isAnnule());
			tiersService.annuleForFiscal(ffp);
			return null;
		});

		// deuxième annulation
		doInNewTransactionAndSession(transactionStatus -> {
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
			public Object execute(TransactionStatus status) {
				final PersonnePhysique achille = addHabitant(noIndAchille);
				addForPrincipal(achille, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
				idCtbHabitant.setValue(achille.getNumero());


				final PersonnePhysique huguette = addNonHabitant("Huguette", "Marcot", date(1950, 4, 12), Sexe.FEMININ);
				huguette.setNumeroIndividu(noIndHuguette);
				addForPrincipal(huguette, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
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
			public Long execute(TransactionStatus status) {
				final PersonnePhysique nh = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				addForPrincipal(nh, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
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
			public Long execute(TransactionStatus status) {
				final PersonnePhysique nh = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				addForPrincipal(nh, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Neuchatel, ModeImposition.SOURCE);
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
			public Long execute(TransactionStatus status) {
				final PersonnePhysique nh = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				addForPrincipal(nh, date(2008, 1, 1), MotifFor.MAJORITE, MockPays.Espagne, ModeImposition.SOURCE);
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
			public Long execute(TransactionStatus status) {

				final PersonnePhysique nh1 = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				addForPrincipal(nh1, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				idCtbMixte1.setValue(nh1.getNumero());

				final PersonnePhysique nh2 = addNonHabitant("Achille", "Talon-Deux", date(1950, 3, 24), Sexe.MASCULIN);
				addForPrincipal(nh2, date(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
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
			public Object execute(TransactionStatus status) {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				idCtbPrincipal.setValue(prn.getNumero());

				final PersonnePhysique sec = addNonHabitant("Huguette", "Marcot", date(1950, 4, 12), Sexe.FEMININ);
				idCtbConjoint.setValue(sec.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, sec, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
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
			public Object execute(TransactionStatus status) {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				idCtbPrincipal.setValue(prn.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, null, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
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
			public Object execute(TransactionStatus status) {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				prn.setNumeroIndividu(noIndAchille);
				idCtbPrincipal.setValue(prn.getNumero());

				final PersonnePhysique sec = addNonHabitant("Huguette", "Marcot", date(1950, 4, 12), Sexe.FEMININ);
				idCtbConjoint.setValue(sec.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, sec, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
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
			public Object execute(TransactionStatus status) {

				final PersonnePhysique prn = addNonHabitant("Achille", "Talon", date(1950, 3, 24), Sexe.MASCULIN);
				prn.setNumeroIndividu(noIndAchille);
				idCtbPrincipal.setValue(prn.getNumero());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(prn, null, date(1975, 1, 5), null);
				final MenageCommun mc = ensemble.getMenage();
				idCtbCouple.setValue(mc.getNumero());

				addForPrincipal(mc, date(1975, 1, 5), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
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
			public Long execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2011, 1, 1), null);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				tiersService.addForDebiteur(dpi, date(2009,6,22), MotifFor.INDETERMINE, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS());
				return dpi.getNumero();
			}
		});

		//Ajout d'un second for avant le dernier for ouvert
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addForDebiteur(dpi, date(2009, 4, 1), MotifFor.INDETERMINE, date(2009, 6, 21), MotifFor.INDETERMINE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addForDebiteur(dpi, date(2010,4,1), MotifFor.INDETERMINE, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 1, 1), date(anneeReference, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M11, date(anneeSuivante, 1, 1), null);

				addForDebiteur(dpi, date(anneeReference - 1, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale fiscale2009 = addPeriodeFiscale(anneeReference - 1);

				addLR(dpi, date(anneePrecedente, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDocumentFiscal.EMIS);
				addLR(dpi, date(anneePrecedente, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDocumentFiscal.EMIS);
				addLR(dpi, date(anneePrecedente, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDocumentFiscal.EMIS);
				addLR(dpi, date(anneePrecedente, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDocumentFiscal.EMIS);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
		doInNewTransactionAndSessionWithoutValidation(status -> {

			// Crée un non-habitant tout nu
			PersonnePhysique paul = addNonHabitant("Paul", "Ruccola", date(1968, 1, 1), Sexe.MASCULIN);
			ids.paul = paul.getId();

			// Crée un ménage commun avec un for principal ouvert
			MenageCommun mc = new MenageCommun();
			mc = hibernateTemplate.merge(mc);
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			ids.mc = mc.getId();

			return null;
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
			public Long execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null);
				addForDebiteur(dpi, date(2010, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null);
				addForDebiteur(dpi, date(2009, 6, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});


		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				addForDebiteur(dpi, date(2009, 11, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				addForDebiteur(dpi, date(2009, 6, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				final PeriodeFiscale fiscale = addPeriodeFiscale(2009);

				addLR(dpi, date(2009, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDocumentFiscal.EMIS);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
	public void testGetRaisonSocialeActiviteEconomiqueSansTiersReferent() throws Exception {

		// mise en place des données fiscales
		final long etbId = doInNewTransactionAndSession(status -> {
			final Etablissement etb = addEtablissement();
			etb.setRaisonSociale("Une super raison sociale");
			etb.setComplementNom("Titi");
			return etb.getNumero();
		});

		// vérification de la raison sociale d'un débiteur sans tiers référent
		doInNewTransactionAndSession(status -> {
			final Etablissement etb = (Etablissement) tiersDAO.get(etbId);
			final String raisonSociale = tiersService.getDerniereRaisonSociale(etb);
			Assert.assertNotNull(raisonSociale);
			Assert.assertEquals("Une super raison sociale", raisonSociale);
			return null;
		});
	}

	@Test
	public void testGetRapportActiviteEconomiqueAvecTiersReferentPM() throws Exception {

		// mise en place du service PM
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.NESTLE);
			}
		});

		// mise en place des données fiscales
		final long etbId = doInNewTransactionAndSession(status -> {
			final Etablissement etb = addEtablissement();
			etb.setRaisonSociale("Une super raison sociale");
			etb.setComplementNom("Titi");

			// on indique le tiers référent
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.NESTLE.getNumeroEntreprise());
			tiersService.addActiviteEconomique(etb, pm, date(2009, 1, 1), false);

			return etb.getNumero();
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(status -> {
			final Etablissement etb = (Etablissement) tiersDAO.get(etbId);
			final String raisonSociale = tiersService.getDerniereRaisonSociale(etb);
			Assert.assertNotNull(raisonSociale);
			final Set<RapportEntreTiers> rapportsEntreTiers = etb.getRapportsObjet();
			Assert.assertNotNull(rapportsEntreTiers);
			Assert.assertEquals(1, rapportsEntreTiers.size());
			return null;
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurSansTiersReferent() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
			dpi.setNom1("Tartempion");
			dpi.setNom2("Toto");
			dpi.setComplementNom("Titi");
			return dpi.getNumero();
		});

		// vérification de la raison sociale d'un débiteur sans tiers référent
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
			Assert.assertNotNull(raisonSociale);
			Assert.assertEquals(2, raisonSociale.size());
			Assert.assertEquals("Tartempion", raisonSociale.get(0));
			Assert.assertEquals("Toto", raisonSociale.get(1));
			return null;
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentPM() throws Exception {

		// mise en place du service PM
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.NESTLE);
			}
		});

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
			dpi.setNom1("Tartempion");
			dpi.setNom2("Toto");
			dpi.setComplementNom("Titi");

			// on indique le tiers référent
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.NESTLE.getNumeroEntreprise());
			tiersService.addContactImpotSource(dpi, pm, date(2009, 1, 1));

			return dpi.getNumero();
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
			Assert.assertNotNull(raisonSociale);
			Assert.assertEquals(1, raisonSociale.size());
			Assert.assertEquals(MockEntrepriseFactory.NESTLE.getNom().get(0).getPayload(), raisonSociale.get(0));
			return null;
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentPersonnePhysique() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
			dpi.setNom1("Tartempion");
			dpi.setNom2("Toto");
			dpi.setComplementNom("Titi");

			// on indique le tiers référent
			final PersonnePhysique pp = addNonHabitant("Albus", "Dumbledore", date(1956, 7, 4), Sexe.MASCULIN);
			tiersService.addContactImpotSource(dpi, pp, date(2009, 1, 1));

			return dpi.getNumero();
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
			Assert.assertNotNull(raisonSociale);
			Assert.assertEquals(1, raisonSociale.size());
			Assert.assertEquals("Albus Dumbledore", raisonSociale.get(0));
			return null;
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentMenageCommun() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(status -> {
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
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
			Assert.assertNotNull(raisonSociale);
			Assert.assertEquals(2, raisonSociale.size());
			Assert.assertEquals("Vernon Dursley", raisonSociale.get(0));
			Assert.assertEquals("Petunia Dursley", raisonSociale.get(1));
			return null;
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentAutreCommunaute() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
			dpi.setNom1("Tartempion");
			dpi.setNom2("Toto");
			dpi.setComplementNom("Titi");

			// on indique le tiers référent
			final AutreCommunaute ac = addAutreCommunaute("Hogwards college");
			tiersService.addContactImpotSource(dpi, ac, date(2009, 1, 1));

			return dpi.getNumero();
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
			Assert.assertNotNull(raisonSociale);
			Assert.assertEquals(1, raisonSociale.size());
			Assert.assertEquals("Hogwards college", raisonSociale.get(0));
			return null;
		});
	}

	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentCollectiviteAdministrative() throws Exception {

		// mise en place des données fiscales
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
			dpi.setNom1("Tartempion");
			dpi.setNom2("Toto");
			dpi.setComplementNom("Titi");

			// on indique le tiers référent
			final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNoColAdm());
			tiersService.addContactImpotSource(dpi, ca, date(2009, 1, 1));

			return dpi.getNumero();
		});

		// vérification de la raison sociale d'un débiteur avec tiers référent
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
			Assert.assertNotNull(raisonSociale);
			Assert.assertEquals(2, raisonSociale.size());
			Assert.assertEquals(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNomComplet1(), raisonSociale.get(0));
			Assert.assertEquals(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNomComplet2(), raisonSociale.get(1));
			Assert.assertNull(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNomComplet3());
			return null;
		});
	}

	@Test
	public void testOuvertureForHorsCantonEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {

			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
			return pp.getNumero();
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {

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
		final long ppId = doInNewTransactionAndSession(status -> {

			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
			return pp.getNumero();
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {

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
		final long ppId = doInNewTransactionAndSession(status -> {

			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
			return pp.getNumero();
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				tiersService.openForFiscalPrincipal(pp, date(2000, 5, 12), MotifRattachement.DOMICILE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
						ModeImposition.ORDINAIRE, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				return null;
			}
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationForVaudoisSansForRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});

		// annulation du for vaudois -> le contribuable se retrouve sans aucun for (le blocage devrait alors être réactivé)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			tiersService.annuleForFiscal(ffp);
			Assert.assertNull(pp.getDernierForFiscalPrincipal());
			return null;
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationForVaudoisAvecForVaudoisRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 5, 12), MotifFor.ARRIVEE_HS, date(2005, 6, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			addForPrincipal(pp, date(2005, 6, 2), MotifFor.DEMENAGEMENT_VD, MockCommune.Renens);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});

		// annulation du dernier for vaudois -> le contribuable a encore un for vaudois ouvert -> pas de nouveau blocage
		doInNewTransactionAndSession(status -> {
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
		});
	}

	@Test
	public void testAnnulationForVaudoisAvecForHorsCantonRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2005, 6, 1), MotifFor.ARRIVEE_HC, MockCommune.Bern, MotifRattachement.DOMICILE);
			addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HC, MockCommune.Renens);
			addForSecondaire(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.CheseauxSurLausanne, MotifRattachement.IMMEUBLE_PRIVE);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});

		// annulation du dernier for vaudois -> le contribuable n'a plus de for vaudois ouvert -> blocage
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			tiersService.annuleForFiscal(ffp);

			final ForFiscalPrincipal autreFfp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(autreFfp);
			Assert.assertNull("Le for précédent n'a pas été ré-ouvert ?", autreFfp.getDateFin());
			Assert.assertEquals(MockCommune.Bern.getNoOFS(), (int) autreFfp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationForVaudoisAvecForHorsSuisseRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2005, 6, 1), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
			addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
			addForSecondaire(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.CheseauxSurLausanne,
			                 MotifRattachement.IMMEUBLE_PRIVE);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});

		// annulation du dernier for vaudois -> le contribuable n'a plus de for vaudois ouvert -> blocage
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			tiersService.annuleForFiscal(ffp);

			final ForFiscalPrincipal autreFfp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(autreFfp);
			Assert.assertNull("Le for précédent n'a pas été ré-ouvert ?", autreFfp.getDateFin());
			Assert.assertEquals(MockPays.Allemagne.getNoOFS(), (int) autreFfp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDecesEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

			tiersService.closeAllForsFiscaux(pp, date(2010, 5, 23), MotifFor.VEUVAGE_DECES);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(MockCommune.Renens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDepartHorsSuisseEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

			tiersService.closeAllForsFiscaux(pp, date(2010, 5, 23), MotifFor.DEPART_HS);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(MockCommune.Renens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDepartHorsCantonEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

			tiersService.closeAllForsFiscaux(pp, date(2010, 5, 23), MotifFor.DEPART_HC);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(MockCommune.Renens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	public void testFermetureForVaudoisDemenagementVaudoisEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
			addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
			pp.setBlocageRemboursementAutomatique(true);
			return pp.getNumero();
		});

		// fermeture du for vaudois pour déménagement vaudois (avec ré-ouverture d'un autre for vaudois) -> déblocage
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				tiersService.closeForFiscalPrincipal(pp, date(2010, 5, 23), MotifFor.DEMENAGEMENT_VD);
				tiersService.addForPrincipal(pp, date(2010, 5, 24), MotifFor.DEMENAGEMENT_VD, null, null, MotifRattachement.DOMICILE, MockCommune.Bex.getNoOFS(),
				                             TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				return null;
			}
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
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
		final long idMenage = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(noIndM);
			final PersonnePhysique mme = addHabitant(noIndMme);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(m, mme, dateMariage, null);
			return ensemble.getMenage().getNumero();
		});

		// test
		doInNewTransactionAndSession(status -> {
			final MenageCommun menage = (MenageCommun) tiersDAO.get(idMenage);
			final Long noIndividu = tiersService.extractNumeroIndividuPrincipal(menage);
			Assert.assertNotNull(noIndividu);
			Assert.assertEquals(noIndM, (long) noIndividu);
			return null;
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
		final long idMenage = doInNewTransactionAndSession(status -> {
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
		});

		// test
		doInNewTransactionAndSession(status -> {
			final MenageCommun menage = (MenageCommun) tiersDAO.get(idMenage);
			final Long noIndividu = tiersService.extractNumeroIndividuPrincipal(menage);
			Assert.assertNull(noIndividu);
			return null;
		});
	}

	@Test
	public void testAddAndSaveForSurCommuneFaitiereFractions() throws Exception {

		final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
		f.setDateDebut(date(1998, 10, 4));
		f.setMotifOuverture(MotifFor.ARRIVEE_HS);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(MockCommune.LeLieu.getNoOFS());
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		try {
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final PersonnePhysique pp = addNonHabitant("Emilie", "Jolie", date(1980, 10, 4), Sexe.FEMININ);
					tiersDAO.addAndSave(pp, f);
				}
			});
			Assert.fail("L'appel aurait dû sauter car la commune est une commune faîtière de fractions de communes");
		}
		catch (ValidationException e) {
			final String message =
					String.format("[E] Le for fiscal %s ne peut pas être sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas\n",
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
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebut);
			addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

			final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Weasley", "Ronnald", date(1980, 5, 12), Sexe.MASCULIN);

			addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
			addRapportPrestationImposable(dpi, pp2, dateDebut, dateFermetureFor.addMonths(-1), false);

			return dpi.getNumero();
		});

		// fermeture du for débiteur
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final ForDebiteurPrestationImposable forDebiteur = dpi.getForDebiteurPrestationImposableAt(null);
			assertNotNull(forDebiteur);

			tiersService.closeForDebiteurPrestationImposable(dpi, forDebiteur, dateFermetureFor, MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, false);
			return null;
		});

		// vérification des rapports de travail : ils ne doivent pas avoir bougé
		doInNewTransactionAndSession(status -> {
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
		});
	}

	@Test
	public void testFermetureForDebiteurAvecFermetureRT() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final RegDate dateFermetureFor = date(2009, 10, 31);

		// mise en place
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebut);
			addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

			final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Weasley", "Ronnald", date(1980, 5, 12), Sexe.MASCULIN);

			addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
			addRapportPrestationImposable(dpi, pp2, dateDebut, dateFermetureFor.addMonths(-1), false);
			addRapportPrestationImposable(dpi, pp2, dateFermetureFor.addMonths(1), null, false);

			return dpi.getNumero();
		});

		// fermeture du for débiteur
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final ForDebiteurPrestationImposable forDebiteur = dpi.getForDebiteurPrestationImposableAt(null);
			assertNotNull(forDebiteur);

			tiersService.closeForDebiteurPrestationImposable(dpi, forDebiteur, dateFermetureFor, MotifFor.INDETERMINE, true);
			return null;
		});

		// vérification des rapports de travail : ils ne doivent pas avoir bougé
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			assertNotNull(dpi);

			final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
			assertNotNull(rapports);
			assertEquals(3, rapports.size());

			final List<RapportEntreTiers> rapportsTries = new ArrayList<>(rapports);
			rapportsTries.sort(new DateRangeComparator<>());

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

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", RegDate.get(1974, 8, 3), Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);

			final Ids ids1 = new Ids();
			ids1.idpp = pp.getNumero();
			ids1.idmc = couple.getMenage().getNumero();
			return ids1;
		});

		// pour l'instant, le rapport existant n'est pas annulé -> on ne doit pas être capable d'en ajouter un entre les même personnes aux mêmes dates
		try {
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idmc);

					final AppartenanceMenage candidat = new AppartenanceMenage(dateMariage, null, pp, mc);
					tiersService.addRapport(candidat, pp, mc);
				}
			});
			Assert.fail("Aurait dû exploser au motif que qu'un rapport identique existe déjà");
		}
		catch (ValidationException e) {
			final List<ValidationMessage> errors = e.getErrors();
			Assert.assertNotNull(errors);
			Assert.assertEquals(2, errors.size());

			{
				final ValidationMessage error = errors.get(0);
				Assert.assertNotNull(error);
				final String expectedMessage = String.format("AppartenanceMenage (02.05.2000 - ?) entre le tiers personne physique %s et le tiers ménage commun %s est présent plusieurs fois à l'identique",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idpp),
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idmc));
				Assert.assertEquals(expectedMessage, error.getMessage());
			}
			{
				final ValidationMessage error = errors.get(1);
				Assert.assertNotNull(error);
				Assert.assertEquals("La personne physique appartient à plusieurs ménages communs sur la période [02.05.2000 ; ]", error.getMessage());
			}
		}

		// mais si on l'annule, alors tout doit bien se passer
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idpp);
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idmc);
				final AppartenanceMenage am = (AppartenanceMenage) mc.getRapportObjetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				Assert.assertNotNull(am);
				am.setAnnule(true);

				final AppartenanceMenage candidat = new AppartenanceMenage(dateMariage, null, pp, mc);
				tiersService.addRapport(candidat, pp, mc);
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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(2, events.size());

				final List<EvenementFiscal> tries = new ArrayList<>(events);
				tries.sort(Comparator.comparingLong(EvenementFiscal::getId));

				final EvenementFiscalFor event0 = (EvenementFiscalFor) tries.get(0);
				assertNotNull(event0);

				final EvenementFiscalParente event1 = (EvenementFiscalParente) tries.get(1);
				assertNotNull(event1);
				assertEquals(ids.mere, event1.getTiers().getNumero());
				assertEquals(ids.fils, event1.getEnfant().getNumero());
				assertEquals(EvenementFiscalParente.TypeEvenementFiscalParente.FIN_AUTORITE_PARENTALE, event1.getType());
				assertEquals(date(2011, 2, 8), event1.getDateValeur());
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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(2, events.size());

				final List<EvenementFiscal> tries = new ArrayList<>(events);
				tries.sort(Comparator.comparingLong(EvenementFiscal::getId));

				final EvenementFiscalFor event0 = (EvenementFiscalFor) tries.get(0);
				assertNotNull(event0);

				final EvenementFiscalParente event1 = (EvenementFiscalParente) tries.get(1);
				assertNotNull(event1);
				assertEquals(ids.menage, event1.getTiers().getNumero());
				assertEquals(ids.fils, event1.getEnfant().getNumero());
				assertEquals(EvenementFiscalParente.TypeEvenementFiscalParente.FIN_AUTORITE_PARENTALE, event1.getType());
				assertEquals(date(2011, 2, 8), event1.getDateValeur());
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
			public Object execute(TransactionStatus status) {
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				addParente(fils, mere, dateNaissance, null);
				addForPrincipal(fils, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2000, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny);
				addForSecondaire(fils, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2000, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);
				return null;
			}
		});

		// Précondition : pas d'événement fiscal envoyé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				assertEmpty(evenementFiscalDAO.getAll());
				return null;
			}
		});

		// Le fils devient majeur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
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
			public Object execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
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

	/**
	 * Couple marié avec Papa et Maman qui n'habitent pas au même endroit
	 * [SIFISC-22155] -> les enfants sont maintenant sur la DI du couple
	 */
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationParentsMariesResidentsEndroitsDifferents() throws Exception {

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
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentRouteDeLausanne, 1, null, dateNaissanceFille, null);
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
			public Long execute(TransactionStatus status) {
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
		assertEquals(2, enfantsForDeclaration.size());
	}

	/**
	 * Couple en concubinage avec enfants
	 * [SIFISC-22155] -> les enfants doivent être sur la DI de leur(s) parent(s)
	 */
	@Test
	@Transactional
	public void testGetEnfantsForDeclarationParentsEnConcubinage() throws Exception {

		final long indMere = 1;
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final long indEnfantCommun = 42;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2007, 2, 8);
		final RegDate dateNaissanceEnfantCommun = date(2012, 11, 22);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu mere = addIndividu(indMere, date(1960, 1, 1), "Champagne", "Josette", Sexe.FEMININ);
				final MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", Sexe.MASCULIN);
				final MockIndividu filsAPapa = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", Sexe.MASCULIN);
				final MockIndividu filleAMaman = addIndividu(indFille, dateNaissanceFille, "Champagne", "Eva", Sexe.FEMININ);
				final MockIndividu enfantCommun = addIndividu(indEnfantCommun, dateNaissanceEnfantCommun, "Champagne", "Trublion", Sexe.MASCULIN);

				// chacun ses enfants
				addLiensFiliation(filsAPapa, pere, null, dateNaissanceFils, null);
				addLiensFiliation(filleAMaman, null, mere, dateNaissanceFille, null);
				addLiensFiliation(enfantCommun, pere, mere, dateNaissanceEnfantCommun, null);

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(filsAPapa, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFils, null);
				addAdresse(filleAMaman, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceFille, null);
				addAdresse(enfantCommun, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, dateNaissanceEnfantCommun, null);
			}
		});

		final class Ids {
			Long mere;
			Long pere;
			Long fils;
			Long fille;
			Long enfantCommun;
		}

		final Ids ids = doInNewTransaction(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) {
				final PersonnePhysique mere = addHabitant(indMere);
				final PersonnePhysique pere = addHabitant(indPere);
				final PersonnePhysique fils = addHabitant(indFils);
				final PersonnePhysique fille = addHabitant(indFille);
				final PersonnePhysique enfantCommun = addHabitant(indEnfantCommun);

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fille, mere, dateNaissanceFille, null);
				addParente(enfantCommun, pere, dateNaissanceEnfantCommun, null);
				addParente(enfantCommun, mere, dateNaissanceEnfantCommun, null);

				final Ids ids = new Ids();
				ids.mere = mere.getId();
				ids.pere = pere.getId();
				ids.fils = fils.getId();
				ids.fille = fille.getId();
				ids.enfantCommun = enfantCommun.getId();
				return ids;
			}
		});

		// déclaration du monsieur
		{
			final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(ids.pere);
			final List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(ctb, date(2012, 12, 31));
			assertNotNull(enfantsForDeclaration);
			assertEquals(Arrays.asList(ids.fils, ids.enfantCommun), enfantsForDeclaration.stream().map(PersonnePhysique::getNumero).sorted().collect(Collectors.toList()));
		}

		// déclaration de madame
		{
			final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(ids.mere);
			final List<PersonnePhysique> enfantsForDeclaration = tiersService.getEnfantsForDeclaration(ctb, date(2012, 12, 31));
			assertNotNull(enfantsForDeclaration);
			assertEquals(Arrays.asList(ids.fille, ids.enfantCommun), enfantsForDeclaration.stream().map(PersonnePhysique::getNumero).sorted().collect(Collectors.toList()));
		}
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
			public Long execute(TransactionStatus status) {
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
		assertEquals(ids.fille, enfantsForDeclaration.get(0).getNumero());
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
			public Long execute(TransactionStatus status) {
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
		assertEquals(ids.fille, enfantsForDeclaration.get(0).getNumero());
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
			public Long execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
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


		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
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
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();


		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
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

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
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


		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
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

				addAdresse(mere, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminTraverse, null, date(1998, 1, 1), null);
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


		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
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
			public Long execute(TransactionStatus status) {
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
		final long dpiId = doInNewTransactionAndSession(status -> {
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

		});

		// Reouverture du second for
		doInNewTransactionAndSession(status -> {
			ForFiscal forDebiteur = forFiscalDAO.get(ids.idFor2);
			tiersService.reouvrirForDebiteur((ForDebiteurPrestationImposable) forDebiteur);
			return null;
		});


		// vérification du for et du rapport ouvert
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			Assert.assertNull(dpi.getDernierForDebiteur().getDateFin());
			final RapportEntreTiers rapportPrestation = dpi.getRapportObjetValidAt(RegDate.get(), TypeRapportEntreTiers.PRESTATION_IMPOSABLE);
			Assert.assertNotNull(rapportPrestation);
			Assert.assertEquals(ids.michelId, rapportPrestation.getSujetId());
			return null;
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
		final long idMenage = doInNewTransactionAndSession(transactionStatus -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			return mc.getNumero();
		});

		// tentative de séparation à la veille du mariage
		doInNewTransactionAndSession(transactionStatus -> {
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateDebutForMixteVaudois, MotifFor.ARRIVEE_HS, dateFinResidenceVD, MotifFor.DEPART_HS, MockCommune.Echallens, ModeImposition.MIXTE_137_2);
			addForPrincipal(pp, dateDebutResidenceHS, MotifFor.DEPART_HS, null, null, MockPays.France, ModeImposition.SOURCE);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateDebutSurchargeCourrier, null, MockRue.Neuchatel.RueDesBeauxArts);
			return pp.getNumero();
		});

		// annulation du for source...
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
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
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final Set<AdresseTiers> adresses = pp.getAdressesTiers();
			assertNotNull(adresses);
			assertEquals(1, adresses.size());
			final AdresseTiers adresse = adresses.iterator().next();
			assertNotNull(adresse);
			assertTrue(adresse.isAnnule());
			assertNotNull(adresse.getAnnulationUser());     // cet attribut n'était pas rempli avant !
			return null;
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

		final long FALBALA = 784515L;

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

				MockIndividu falbala = addIndividu(FALBALA, date(1956, 7, 21), "Toutatix", "Falbala", Sexe.FEMININ);
				addAdresse(falbala, TypeAdresseCivil.PRINCIPALE, MockRue.Cully.ChDesColombaires, null, date(1990, 1, 1), date(2012, 5, 12));
				addAdresse(falbala, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeLaGare, null, date(2010, 6, 10), null);
			}
		};

		final TiersServiceImpl service = new TiersServiceImpl();
		service.setServiceCivilService(new ServiceCivilImpl(serviceInfra, civil));
		service.setServiceInfra(serviceInfra);

		// Une personne inconnue au contrôle des habitants (non-habitant) quelconque
		assertFalse(service.isDomicileDansLeCanton(new PersonnePhysique(), RegDate.get(), false));

		// Une personne avec un numéro d'individu mais introuvable dans le registre civil (par exemple, personne non-migrée par RcPers)
		assertNull(service.isDomicileDansLeCanton(new PersonnePhysique(NON_MIGRE), RegDate.get(), false));

		// Jean qui ne possède aucune adresse de domicile
		final PersonnePhysique jean = new PersonnePhysique(JEAN);
		assertFalse(service.isDomicileDansLeCanton(jean, RegDate.get(), false));

		// Arnold qui a toujours habité dans le canton depuis sa naissance
		final PersonnePhysique arnold = new PersonnePhysique(ARNOLD);
		assertTrue(service.isDomicileDansLeCanton(arnold, RegDate.get(), false));

		// Gudrun qui a habité dans le canton durant l'année 1990
		final PersonnePhysique gudrun = new PersonnePhysique(GUDRUN);
		assertFalse(service.isDomicileDansLeCanton(gudrun, RegDate.get(), false));

		// Lola qui a toujours habité dans le canton et à déménagé une fois
		final PersonnePhysique lola = new PersonnePhysique(LOLA);
		assertTrue(service.isDomicileDansLeCanton(lola, RegDate.get(), false));

		// Heidi qui a habité dans le canton lors de deux périodes séparées
		final PersonnePhysique heidi = new PersonnePhysique(HEIDI);
		assertFalse(service.isDomicileDansLeCanton(heidi, RegDate.get(), false));

		// Ursule qui a habité dans le canton et qui est parti pour une destination vaudoise mais n'a pas encore enregistré son arrivée
		// [SIFISC-13741] ce cas ne doit maintenant plus se produire (et donc on peut se permettre de ne plus tenir compte des destinations suivantes annoncées)
		final PersonnePhysique ursule = new PersonnePhysique(URSULE);
		assertFalse(service.isDomicileDansLeCanton(ursule, RegDate.get(), false));

		// Hans qui habite hors-canton depuis toujours
		final PersonnePhysique hans = new PersonnePhysique(HANS);
		assertFalse(service.isDomicileDansLeCanton(hans, RegDate.get(), false));

		// Shirley qui est née à Cottens (VD) et qui est - charogne de gamine ! - partie à Zürich le 1er janvier 1990
		final PersonnePhysique shirley = new PersonnePhysique(SHIRLEY);
		assertTrue(service.isDomicileDansLeCanton(shirley, date(1989, 12, 31), false));
		assertFalse(service.isDomicileDansLeCanton(shirley, date(1990, 1, 1), false));
		assertFalse(service.isDomicileDansLeCanton(shirley, RegDate.get(), false));

		// Urs qui est né hors-canton et qui est arrivé dans le canton le 1er janvier 1990
		final PersonnePhysique urs = new PersonnePhysique(URS);
		assertFalse(service.isDomicileDansLeCanton(urs, date(1989, 12, 31), false));
		assertTrue(service.isDomicileDansLeCanton(urs, date(1990, 1, 1), false));
		assertTrue(service.isDomicileDansLeCanton(urs, RegDate.get(), false));

		// Moussa qui habite hors-Suisse depuis toujours
		final PersonnePhysique moussa = new PersonnePhysique(MOUSSA);
		assertFalse(service.isDomicileDansLeCanton(moussa, RegDate.get(), false));

		// Brahim qui habitait hors-Suisse et qui est arrivé le 1er janvier 1990 dans le canton
		final PersonnePhysique brahim = new PersonnePhysique(BRAHIM);
		assertTrue(service.isDomicileDansLeCanton(brahim, RegDate.get(), false));

		// Falbala, qui a eu une résidence secondaire...
		final PersonnePhysique falbala = new PersonnePhysique(FALBALA);
		assertFalse(service.isDomicileDansLeCanton(falbala, date(1989, 12, 31), false));
		assertFalse(service.isDomicileDansLeCanton(falbala, date(1989, 12, 31), true));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(1990, 1, 1), false));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(1990, 1, 1), true));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(2010, 7, 1), false));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(2010, 7, 1), true));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(2012, 7, 1), false));
		assertFalse(service.isDomicileDansLeCanton(falbala, date(2012, 7, 1), true));
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

		final long FALBALA = 784515L;

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

				MockIndividu falbala = addIndividu(FALBALA, date(1956, 7, 21), "Toutatix", "Falbala", Sexe.FEMININ);
				MockAdresse a7 = addAdresse(falbala, TypeAdresseCivil.PRINCIPALE, MockRue.Cully.ChDesColombaires, null, date(1990, 1, 1), date(2012, 5, 12));
				a7.setLocalisationSuivante(newLocalisation(MockCommune.Zurich));
				addAdresse(falbala, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeLaGare, null, date(2010, 6, 10), null);
			}
		};

		final TiersServiceImpl service = new TiersServiceImpl();
		service.setServiceCivilService(new ServiceCivilImpl(serviceInfra, civil));
		service.setServiceInfra(serviceInfra);

		// Une personne inconnue au contrôle des habitants (non-habitant) quelconque
		assertFalse(service.isDomicileDansLeCanton(new PersonnePhysique(), RegDate.get(), false));

		// Une personne avec un numéro d'individu mais introuvable dans le registre civil (par exemple, personne non-migrée par RcPers)
		assertNull(service.isDomicileDansLeCanton(new PersonnePhysique(NON_MIGRE), RegDate.get(), false));

		// Jean qui ne possède aucune adresse de domicile
		final PersonnePhysique jean = new PersonnePhysique(JEAN);
		assertFalse(service.isDomicileDansLeCanton(jean, RegDate.get(), false));

		// Arnold qui a toujours habité dans le canton depuis sa naissance
		final PersonnePhysique arnold = new PersonnePhysique(ARNOLD);
		assertTrue(service.isDomicileDansLeCanton(arnold, RegDate.get(), false));

		// Gudrun qui a habité dans le canton durant l'année 1990
		final PersonnePhysique gudrun = new PersonnePhysique(GUDRUN);
		assertFalse(service.isDomicileDansLeCanton(gudrun, RegDate.get(), false));

		// Lola qui a toujours habité dans le canton et à déménagé une fois
		final PersonnePhysique lola = new PersonnePhysique(LOLA);
		assertTrue(service.isDomicileDansLeCanton(lola, RegDate.get(), false));

		// Heidi qui a habité dans le canton lors de deux périodes séparées
		final PersonnePhysique heidi = new PersonnePhysique(HEIDI);
		assertFalse(service.isDomicileDansLeCanton(heidi, RegDate.get(), false));

		// Ursule qui a habité dans le canton et qui est parti pour une destination vaudoise mais n'a pas encore enregistré son arrivée
		// [SIFISC-13741] ce cas ne doit maintenant plus se produire (et donc on peut se permettre de ne plus tenir compte des destinations suivantes annoncées)
		final PersonnePhysique ursule = new PersonnePhysique(URSULE);
		assertFalse(service.isDomicileDansLeCanton(ursule, RegDate.get(), false));

		// Trudi qui est arrivée dans une commune vaudoise le 1er janvier 1990 en provenance d'une autre commune vaudoise mais dont on a aucune trace (cas bizarre hypothétique)
		final PersonnePhysique trudi = new PersonnePhysique(TRUDI);
		assertTrue(service.isDomicileDansLeCanton(trudi, RegDate.get(), false));

		// Shirley qui est née à Cottens (VD) et qui est - charogne de gamine ! - partie à Zürich le 1er janvier 1990
		final PersonnePhysique shirley = new PersonnePhysique(SHIRLEY);
		assertTrue(service.isDomicileDansLeCanton(shirley, date(1989, 12, 31), false));
		assertFalse(service.isDomicileDansLeCanton(shirley, date(1990, 1, 1), false));
		assertFalse(service.isDomicileDansLeCanton(shirley, RegDate.get(), false));

		// Urs qui est né hors-canton et qui est arrivé dans le canton le 1er janvier 1990
		final PersonnePhysique urs = new PersonnePhysique(URS);
		assertFalse(service.isDomicileDansLeCanton(urs, date(1989, 12, 31), false));
		assertTrue(service.isDomicileDansLeCanton(urs, date(1990, 1, 1), false));
		assertTrue(service.isDomicileDansLeCanton(urs, RegDate.get(), false));

		// Brahim qui habitait hors-Suisse et qui est arrivé le 1er janvier 1990 dans le canton
		final PersonnePhysique brahim = new PersonnePhysique(BRAHIM);
		assertTrue(service.isDomicileDansLeCanton(brahim, RegDate.get(), false));

		// Falbala, qui a eu une résidence secondaire...
		final PersonnePhysique falbala = new PersonnePhysique(FALBALA);
		assertFalse(service.isDomicileDansLeCanton(falbala, date(1989, 12, 31), false));
		assertFalse(service.isDomicileDansLeCanton(falbala, date(1989, 12, 31), true));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(1990, 1, 1), false));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(1990, 1, 1), true));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(2010, 7, 1), false));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(2010, 7, 1), true));
		assertTrue(service.isDomicileDansLeCanton(falbala, date(2012, 7, 1), false));
		assertFalse(service.isDomicileDansLeCanton(falbala, date(2012, 7, 1), true));
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

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			assertNotNull("le libellé de la commune d'origine du non-habitant devrait être Orbe (VD)", pp.getOrigine());
			assertEquals("le libellé de la commune d'origine du non-habitant devrait être Orbe (VD)", "Orbe", pp.getOrigine().getLibelle());
			assertEquals("le libellé de la commune d'origine du non-habitant devrait être Orbe (VD)", "VD", pp.getOrigine().getSigleCanton());
			return pp.getNumero();
		});
	}

	@Test
	public void testStatutMenageCommun() throws Exception {

		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
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
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertEquals(date(2010, 1, 1), dateDeces);
				return null;
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenage(date(2010, 1, 1), MotifFor.VEUVAGE_DECES);
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertEquals(date(2010, 1, 1), dateDeces);
				return null;
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(date(2010, 1, 1), MotifFor.VEUVAGE_DECES);
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertEquals(date(2010, 1, 1), dateDeces);
				return null;
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
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertNull(dateDeces);
				return null;
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenage(null, null);
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertNull(dateDeces);
				return null;
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(null, null);
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertNull(dateDeces);
				return null;
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
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertNull(dateDeces);
				return null;
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenage(date(2010,1,1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertNull(dateDeces);
				return null;
			});
		}
		{
			final Long noLui = testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(date(2010,1,1), MotifFor.DEPART_HC);
			doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
				final PersonnePhysique lui = (PersonnePhysique) tiersService.getTiers(noLui);
				final RegDate dateDeces = tiersService.getDateDecesDepuisDernierForPrincipal(lui);
				assertNull(dateDeces);
				return null;
			});
		}
	}

	private Long testGetDateDecesDepuisDernierForPrincipalAvecMenage(@Nullable final RegDate dateFermetureFor, @Nullable final MotifFor motifFermeture) throws Exception {

		final Long noLui = 1234567L;
		final Long noElle = 1234568L;
		final RegDate dateMariage = date(1954,1,1);

		return doInNewTransactionAndSession(status -> {
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
		});
	}

	private Long testGetDateDecesDepuisDernierForPrincipalAvecMenageSepare(@Nullable final RegDate dateFermetureFor, @Nullable final MotifFor motifFermeture) throws Exception {

		final Long noLui = 1234567L;
		final Long noElle = 1234568L;
		final RegDate dateMariage = date(1954,1,1);
		final RegDate dateSeparation = date(2005,1,1);

		return doInNewTransactionAndSession(status -> {
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
		});

	}

	private Long testGetDateDecesDepuisDernierForPrincipalAvecPersonnePhysique(@Nullable final RegDate dateFermetureFor, @Nullable final MotifFor motifFermeture) throws Exception {

		final Long noLui = 1234567L;
		final RegDate dateMajorite = date(1954,1,1);

		// Mise en place du fiscal
		return doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noLui);
			addForPrincipal(
					lui,
					dateMajorite, MotifFor.MAJORITE,
					dateFermetureFor, motifFermeture,
					MockCommune.Aigle
			);
			return lui.getNumero();
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

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique hab = addHabitant(noIndividuHabitant);
			final PersonnePhysique ancienHab = tiersService.createNonHabitantFromIndividu(noIndividuAncienHabitant);
			ancienHab.setDateNaissance(dateNaissanceAncienHabitant);
			final PersonnePhysique nonHab = addNonHabitant("Bienvenu", "Patant", null, Sexe.MASCULIN);
			final Ids ids1 = new Ids();
			ids1.ppHabitant = hab.getNumero();
			ids1.ppAncienHabitant = ancienHab.getNumero();
			ids1.ppNonHabitant = nonHab.getNumero();
			return ids1;
		});

		Assert.assertNull(tiersService.getDateNaissance(null));

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(ids.ppHabitant);
			Assert.assertEquals(dateNaissanceHabitant, tiersService.getDateNaissance(hab));

			final PersonnePhysique ancienHab = (PersonnePhysique) tiersDAO.get(ids.ppAncienHabitant);
			Assert.assertEquals(dateNaissanceAncienHabitant, tiersService.getDateNaissance(ancienHab));

			final PersonnePhysique nonHabitant = (PersonnePhysique) tiersDAO.get(ids.ppNonHabitant);
			Assert.assertNull(tiersService.getDateNaissance(nonHabitant));
			nonHabitant.setDateNaissance(dateNaissanceNonHabitant);
			Assert.assertEquals(dateNaissanceNonHabitant, tiersService.getDateNaissance(nonHabitant));

			return null;
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// re-calcul du flag habitant
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			try {
				tiersService.updateHabitantStatus(pp, noIndividu, null, null);
				Assert.fail();
			}
			catch (TiersException e) {
				Assert.assertEquals("Impossible de déterminer si le domicile du contribuable " + pp.getNumero() + " est vaudois ou pas", e.getMessage());
			}
			return null;
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
		final Ids ids = doInNewTransactionAndSession(status -> {
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

			final Ids ids1 = new Ids();
			ids1.ppPapy = papy.getNumero();
			ids1.ppPapa = papa.getNumero();
			ids1.ppMaman = maman.getNumero();
			ids1.ppMoi = moi.getNumero();
			ids1.ppFiston = fiston.getNumero();
			ids1.ppFifille = fifille.getNumero();
			ids1.ppFactrice = factrice.getNumero();
			return ids1;
		});

		// refresh de "moi"
		doInNewTransactionAndSession(status -> {
			tiersService.refreshParentesDepuisNumeroIndividu(noIndividu);
			return null;
		});

		// vérification de l'impact
		doInNewTransactionAndSession(status -> {

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
				sortedParents.sort(Comparator.comparingLong(RapportEntreTiers::getObjetId));
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
				sortedParents.sort(Comparator.comparingLong(RapportEntreTiers::getObjetId));
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
		});

		// refresh de "papa"
		doInNewTransactionAndSession(status -> {
			tiersService.refreshParentesDepuisNumeroIndividu(noIndPapa);
			return null;
		});

		// vérification de l'impact
		doInNewTransactionAndSession(status -> {

			// papy
			{
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.ppPapy);
				assertNotNull(pp);

				final Set<RapportEntreTiers> parents = pp.getRapportsSujet();
				assertEmpty(parents);

				final Set<RapportEntreTiers> enfants = pp.getRapportsObjet();
				assertEquals(2, enfants.size());            // 1 annulé, et l'autre actif, modification en raison de la date de fin

				final List<RapportEntreTiers> sortedEnfants = new ArrayList<>(enfants);
				sortedEnfants.sort((o1, o2) -> {
					return NullDateBehavior.EARLIEST.compare(o1.getDateFin(), o2.getDateFin());         // le rapport sans fin (annulé) d'abord
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
				sortedParents.sort((o1, o2) -> {
					return NullDateBehavior.EARLIEST.compare(o1.getDateFin(), o2.getDateFin());         // le rapport sans fin (annulé) d'abord
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
				sortedParents.sort(Comparator.comparingLong(RapportEntreTiers::getObjetId));
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
				sortedParents.sort(Comparator.comparingLong(RapportEntreTiers::getObjetId));
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique parent = addHabitant(noIndividuParent);
			final PersonnePhysique enfant = addHabitant(noIndividuEnfant);

			// le doublon de parenté est ajouté "à la main" car les méthodes officielles ne permettent pas de le faire
			hibernateTemplate.merge(new Parente(dateNaissanceEnfant, null, parent, enfant));
			hibernateTemplate.merge(new Parente(dateNaissanceEnfant, null, parent, enfant));        // doublon de parenté

			final Ids ids1 = new Ids();
			ids1.idParent = parent.getNumero();
			ids1.idEnfant = enfant.getNumero();
			return ids1;
		});

		// vérification de l'existance du doublon de parenté
		doInNewTransactionAndSession(status -> {
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
		});

		// vérification de la bonne récupération des candidats doublons par le DAO
		doInNewTransactionAndSession(status -> {
			final List<Pair<Long, Long>> doublons = rapportEntreTiersDAO.getDoublonsCandidats(TypeRapportEntreTiers.PARENTE);
			assertNotNull(doublons);
			assertEquals(1, doublons.size());

			final Pair<Long, Long> doublon = doublons.get(0);
			assertNotNull(doublon);
			assertEquals((Long) ids.idEnfant, doublon.getLeft());
			assertEquals((Long) ids.idParent, doublon.getRight());
			return null;
		});

		// demande de refresh
		doInNewTransactionAndSession(status -> {
			tiersService.refreshParentesDepuisNumeroIndividu(noIndividuEnfant);
			return null;
		});

		// vérification que l'un des doublons a bien été éliminé
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
			final Set<RapportEntreTiers> relParents = enfant.getRapportsSujet();
			assertNotNull(relParents);
			assertEquals(2, relParents.size());

			final List<RapportEntreTiers> sortedRelParents = new ArrayList<>(relParents);
			sortedRelParents.sort((o1, o2) -> {
				return Boolean.compare(o1.isAnnule(), o2.isAnnule());       // annulé à la fin
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
		});

		// vérification de la bonne récupération des candidats doublons par le DAO
		// (ici, il n'y en a plus car l'un des deux a été annulé)
		doInNewTransactionAndSession(status -> {
			final List<Pair<Long, Long>> doublons = rapportEntreTiersDAO.getDoublonsCandidats(TypeRapportEntreTiers.PARENTE);
			assertNotNull(doublons);
			assertEquals(0, doublons.size());
			return null;
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
		final Ids ids = doInNewTransactionAndSession(status -> {
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

			final Ids ids1 = new Ids();
			ids1.ppPapy = papy.getNumero();
			ids1.ppPapa = papa.getNumero();
			ids1.ppMaman = maman.getNumero();
			ids1.ppMoi = moi.getNumero();
			ids1.ppFiston = fiston.getNumero();
			ids1.ppFifille = fifille.getNumero();
			ids1.ppFactrice = factrice.getNumero();
			return ids1;
		});

		// refresh de "moi"
		doInNewTransactionAndSession(status -> {
			tiersService.refreshParentesDepuisNumeroIndividu(noIndividu);
			return null;
		});

		// vérification de l'impact
		doInNewTransactionAndSession(status -> {

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
				sortedParents.sort(Comparator.comparingLong(RapportEntreTiers::getObjetId));
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
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique parent = addHabitant(noIndParent);
			final PersonnePhysique enfant = addHabitant(noIndEnfant);

			final Ids ids1 = new Ids();
			ids1.idEnfant = enfant.getNumero();
			ids1.idParent = parent.getNumero();
			return ids1;
		});

		// contrôle des dates de la relation de parenté créée
		doInNewTransactionAndSession(status -> {
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
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique parent = addHabitant(noIndParent);
			final PersonnePhysique enfant = addHabitant(noIndEnfant);

			final Ids ids1 = new Ids();
			ids1.idEnfant = enfant.getNumero();
			ids1.idParent = parent.getNumero();
			return ids1;
		});

		// contrôle des dates de la relation de parenté créée
		doInNewTransactionAndSession(status -> {
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Fujitsu", null, Sexe.MASCULIN);
			pp.setHabitant(true);
			pp.setNumeroIndividu(noIndividu);
			return pp.getNumero();
		});

		final ParenteUpdateResult result = doInNewTransactionAndSession(status -> {
			// vérification que la personne physique est toujours habitante (des fois qu'on mettrait en place un intercepteur qui recalcule le bousin)
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);
			assertTrue(pp.isHabitantVD());
			assertFalse(pp.isParenteDirty());

			// lancement du traitement
			return tiersService.initParentesDepuisFiliationsCiviles(pp);
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Fujitsu", null, Sexe.MASCULIN);
			pp.setHabitant(true);
			pp.setNumeroIndividu(noIndividu);
			return pp.getNumero();
		});

		final ParenteUpdateResult result = doInNewTransactionAndSession(status -> {
			// vérification que la personne physique est toujours habitante (des fois qu'on mettrait en place un intercepteur qui recalcule le bousin)
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);
			assertTrue(pp.isHabitantVD());
			assertFalse(pp.isParenteDirty());

			// lancement du traitement
			return tiersService.refreshParentesSurPersonnePhysique(pp, false);
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
	 * [SIFISC-9993] Gestion du flag de remboursement automatique sur les non-vaudois
	 */
	@Test
	public void testBlocageRemboursementAutomatiqueNonVaudoisAvecPeriodeImpositionISSourceEtIBAN() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dayatsu", null, Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addRapportPrestationImposable(dpi, pp, date(2009, 1, 1), date(2011, 12, 31), false);
			addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 12, 31), MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.SOURCE);
			pp.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH8109000000177448451", null));
			pp.setBlocageRemboursementAutomatique(true);        // pour partir d'une situation bloquée
			return pp.getNumero();
		});

		// vérification que l'ajout d'un for HC après le for lausannois fermé débloque bien la situation
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertTrue(pp.getBlocageRemboursementAutomatique());

				tiersService.addForPrincipal(pp, date(2012, 1, 1), MotifFor.DEPART_HC, null, null, MotifRattachement.DOMICILE, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC,
				                             ModeImposition.SOURCE);
			}
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-9993] Gestion du flag de remboursement automatique sur les non-vaudois
	 */
	@Test
	public void testBlocageRemboursementAutomatiqueNonVaudoisAvecPeriodeImpositionISSourceSansIBAN() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dayatsu", null, Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addRapportPrestationImposable(dpi, pp, date(2009, 1, 1), date(2011, 12, 31), false);
			addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 12, 31), MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.SOURCE);
			pp.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH810900000017744845123", null)); // trop long!
			pp.setBlocageRemboursementAutomatique(false);        // pour partir d'une situation débloquée
			return pp.getNumero();
		});

		// vérification que l'ajout d'un for HC après le for lausannois fermé débloque bien la situation
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertFalse(pp.getBlocageRemboursementAutomatique());

				tiersService.addForPrincipal(pp, date(2012, 1, 1), MotifFor.DEPART_HC, null, null, MotifRattachement.DOMICILE, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.SOURCE);
			}
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-9993] Gestion du flag de remboursement automatique sur les non-vaudois
	 */
	@Test
	public void testBlocageRemboursementAutomatiqueNonVaudoisAvecPeriodeImpositionISSourceAvecIBANMaisDecede() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dayatsu", null, Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addRapportPrestationImposable(dpi, pp, date(2009, 1, 1), date(2011, 12, 31), false);
			addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 12, 31), MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.SOURCE);
			pp.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH8109000000177448451", null));
			pp.setBlocageRemboursementAutomatique(false);        // pour partir d'une situation débloquée
			return pp.getNumero();
		});

		// vérification que l'ajout d'un for HC après le for lausannois fermé débloque bien la situation
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertFalse(pp.getBlocageRemboursementAutomatique());

				final RegDate dateDeces = date(2013, 5, 2);
				pp.setDateDeces(dateDeces);
				tiersService.addForPrincipal(pp, date(2012, 1, 1), MotifFor.DEPART_HC, dateDeces, MotifFor.VEUVAGE_DECES, MotifRattachement.DOMICILE, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.SOURCE);
			}
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-9993] Gestion du flag de remboursement automatique sur les non-vaudois
	 */
	@Test
	public void testBlocageRemboursementAutomatiqueNonVaudoisSansPeriodeImpositionISEtIBAN() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dayatsu", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 12, 31), MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.ORDINAIRE);
			pp.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH8109000000177448451", null));
			pp.setBlocageRemboursementAutomatique(false);        // pour partir d'une situation débloquée
			return pp.getNumero();
		});

		// vérification que l'ajout d'un for HC après le for lausannois fermé débloque bien la situation
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertFalse(pp.getBlocageRemboursementAutomatique());

				tiersService.addForPrincipal(pp, date(2012, 1, 1), MotifFor.DEPART_HC, null, null, MotifRattachement.DOMICILE, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.SOURCE);
			}
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-9993] Gestion du flag de remboursement automatique sur les non-vaudois
	 */
	@Test
	public void testBlocageRemboursementAutomatiqueNonVaudoisAvecPeriodeImpositionISMixteEtIBAN() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dayatsu", null, Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addRapportPrestationImposable(dpi, pp, date(2009, 1, 1), null, false);
			addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 12, 31), MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.ORDINAIRE);
			pp.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH8109000000177448451", null));
			pp.setBlocageRemboursementAutomatique(false);        // pour partir d'une situation débloquée
			return pp.getNumero();
		});

		// vérification que l'ajout d'un for HC après le for lausannois fermé débloque bien la situation
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertFalse(pp.getBlocageRemboursementAutomatique());

				tiersService.addForPrincipal(pp, date(2012, 1, 1), MotifFor.DEPART_HC, null, null, MotifRattachement.DOMICILE, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.SOURCE);
			}
		});

		// valeur du flag de blocage de remboursement automatique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-12580] Recalcul du flag sur une personne physique membre d'un couple dont on modifie le for (mais pas sur la PP)
	 */
	@Test
	public void testBlocageRemboursementAutomatiqueSurPersonnePhysiqueMembreDeCouple() throws Exception {

		final RegDate dateMariage = date(2012, 7, 13);
		final RegDate dateArrivee = date(2013, 10, 3);
		final RegDate dateDepart = date(2014, 3, 12);

		final class Ids {
			long ppM;
			long ppMme;
			long mc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
			final PersonnePhysique mme = addNonHabitant("Philomène", "Dubourg", null, Sexe.FEMININ);

			// il faut un IBAN pour débloquer la situation...
			m.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH6100767000K51392545", null));
			mme.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH250025525510075340X", null));

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
			final MenageCommun menage = couple.getMenage();
			addForPrincipal(menage, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			menage.setBlocageRemboursementAutomatique(false);

			final Ids ids1 = new Ids();
			ids1.ppM = m.getNumero();
			ids1.ppMme = mme.getNumero();
			ids1.mc = menage.getNumero();
			return ids1;
		});

		// vérification de l'état de départ du flag et départ du couple à l'étranger
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.mc);
				Assert.assertNotNull(mc);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, null);
				Assert.assertNotNull(couple);

				Assert.assertFalse(mc.getBlocageRemboursementAutomatique());
				Assert.assertTrue(couple.getPrincipal().getBlocageRemboursementAutomatique());
				Assert.assertTrue(couple.getConjoint().getBlocageRemboursementAutomatique());

				// départ du couple vers l'étranger
				tiersService.closeForFiscalPrincipal(mc, dateDepart, MotifFor.DEPART_HS);
				tiersService.openForFiscalPrincipal(mc, dateDepart.getOneDayAfter(), MotifRattachement.DOMICILE, MockPays.Allemagne.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, ModeImposition.SOURCE,
				                                    MotifFor.DEPART_HS);
			}
		});

		// vérification de l'état après départ
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.mc);
				Assert.assertNotNull(mc);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, null);
				Assert.assertNotNull(couple);

				Assert.assertTrue(mc.getBlocageRemboursementAutomatique());                     // parti -> bloqué
				Assert.assertFalse(couple.getPrincipal().getBlocageRemboursementAutomatique()); // parti mais PIIS source avec IBAN -> débloqué
				Assert.assertFalse(couple.getConjoint().getBlocageRemboursementAutomatique());  // parti mais PIIS source avec IBAN -> débloqué
			}
		});
	}

	/**
	 * [SIFISC-9600] Cas où un contribuable apparemment parti HC est pourtant toujours indiqué comme habitant même après un recalcul.
	 * <br/>Le problème, après analyse, était le suivant:
	 * <ul>
	 *     <li>bien avant le départ HC, le contribuable avait une adresse secondaire fermée avec une localisation suivante vaudoise</li>
	 *     <li>du coup, la <i>dernière adresse secondaire</i> est considérée comme un départ VD sans annonce d'arrivée reçue, et donc la résidence est toujours estimée vaudoise</li>
	 * </ul>
	 * La solution passe forcément par l'introduction d'une dépendance entre les adresses secondaires et principales dans ce calcul...
	 * <br/>En l'occurrence, ici, le problème est insoluble, car l'adresse secondaire se ferme en indiquant la commune suivante comme étant celle de l'ancienne adresse
	 * principale (qui se ferme le même jour). En fait, il y a en pratique un échange des répartitions des communes principales et secondaires à la date du déménagement principal.
	 * <pre>
	 *     Principal
	 *           Orbe ------------------|---------- Romainmôtier-Envy --------------|------- Genève --------
	 *                              -> Romainmôtier                            -> Genève
	 *
	 *     Secondaire
	 *           Romainmôtier-Envy -----|
	 *                              -> Orbe     (destination ignorée)
	 * </pre>
	 */
	@Test
	public void testRecalculFlagHabitantAvecVieilleAdresseSecondaireFermeeVersVaudEchangeCommunes() throws Exception {

		final long noIndividuLui = 236723537L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Bollomey", "Francis", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Orbe.GrandRue, null, date(1994, 6, 1), date(2010, 5, 31));
				final MockAdresse derniereAdressePrincipaleVaudoiseLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, date(2010, 6, 1), date(2013, 7, 31));
				derniereAdressePrincipaleVaudoiseLui.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(),
				                                                                              new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2013, 8, 1),
				                                                                                              null)));
				final MockAdresse adresseSecondaire = addAdresse(lui, TypeAdresseCivil.SECONDAIRE, MockRue.Romainmotier.CheminDuCochet, null, date(2009, 10, 1), date(2010, 5, 31));
				adresseSecondaire.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Orbe.getNoOFS(), null));     // <-- ici est la cause de nos maux...

				// l'adresse principale change, et à la même date l'adresse secondaire se ferme et déclare partir vers la commune de l'ancienne
				// adresse principale... Unireg va donc considérer que
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2007, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, date(2010, 5, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);
			addForPrincipal(lui, date(2010, 6, 1), MotifFor.DEMENAGEMENT_VD, date(2013, 7, 31), MotifFor.DEPART_HC, MockCommune.RomainmotierEnvy);
			addForPrincipal(lui, date(2013, 8, 1), MotifFor.DEPART_HC, MockCommune.Geneve);
			return lui.getNumero();
		});

		// recalcul du flag habitant
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertTrue(pp.isHabitantVD());

				// force le recalcul
				tiersService.updateHabitantFlag(pp, noIndividuLui, null);
			}
		});

		// vérification du nouveau flag (devrait être non-habitant !!)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isHabitantVD());       // <-- la personne n'est plus considérée comme habitante la destination des adresses secondaires est maintenant ignorée
			}
		});
	}

	/**
	 * [SIFISC-9600] Cas hérité de celui du jira, dans le cas où il n'y accord entre les communes de destinations principales et secondaires
	 * <pre>
	 *     Principal
	 *           Orbe ------------------|---------- Lausanne --------------|------- Genève --------
	 *                              -> Lausanne    _                   -> Genève
	 *                                             /|
	 *     Secondaire                             /
	 *           Romainmôtier-Envy -----|        /
	 *                              -> Lausanne
	 * </pre>
	 * Ici, on peut peut-être s'en sortir puisqu'il n'y a plus qu'une seule commune vaudoise lors du dernier départ...
	 */
	@Test
	public void testRecalculFlagHabitantAvecVieilleAdresseSecondaireFermeeVersCommuneVaudoisePrincipale() throws Exception {

		final long noIndividuLui = 236723537L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Bollomey", "Francis", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Orbe.GrandRue, null, date(1994, 6, 1), date(2010, 5, 31));
				final MockAdresse derniereAdressePrincipaleVaudoiseLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeLaGare, null, date(2010, 6, 1), date(2013, 7, 31));
				derniereAdressePrincipaleVaudoiseLui.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(),
				                                                                              new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2013, 8, 1),
				                                                                                              null)));
				final MockAdresse adresseSecondaire = addAdresse(lui, TypeAdresseCivil.SECONDAIRE, MockRue.Romainmotier.CheminDuCochet, null, date(2009, 10, 1), date(2010, 5, 31));
				adresseSecondaire.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));     // <-- ici est la cause de nos maux...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2007, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, date(2010, 5, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);
			addForPrincipal(lui, date(2010, 6, 1), MotifFor.DEMENAGEMENT_VD, date(2013, 7, 31), MotifFor.DEPART_HC, MockCommune.Lausanne);
			addForPrincipal(lui, date(2013, 8, 1), MotifFor.DEPART_HC, MockCommune.Geneve);
			return lui.getNumero();
		});

		// recalcul du flag habitant
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertTrue(pp.isHabitantVD());

				// force le recalcul
				tiersService.updateHabitantFlag(pp, noIndividuLui, null);
			}
		});

		// vérification du nouveau flag (devrait être non-habitant !!)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse("Le flag habitant n'a pas été remis à false", pp.isHabitantVD());
			}
		});
	}

	@Test
	public void testMiseAJourDecisionsSimple() throws Exception {

		final class IdsDecision {
			public Long idOriginal;
		}
		final class Ids {
			long ppM;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
			final Ids ids1 = new Ids();
			ids1.ppM = m.getNumero();
			return ids1;
		});

		// mise en place decision
		final IdsDecision idsDecision = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = (PersonnePhysique) tiersDAO.get(ids.ppM);
			DecisionAci d = addDecisionAci(m,date(2006,11,1),null,MockCommune.Lausanne.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final IdsDecision idDec = new IdsDecision();
			idDec.idOriginal = d.getId();
			return idDec;
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				DecisionAci amodifier = decisionAciDAO.get(idsDecision.idOriginal);
				tiersService.updateDecisionAci(amodifier, null, "Nouvelle Remarque", null);
			}
		});

		// vérification de l'état après départ
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppM);
				Assert.assertNotNull(pp);
				final List<DecisionAci> decisionsAci = new ArrayList<>(pp.getDecisionsAci());
				assertNotNull(decisionsAci);
				assertFalse(decisionsAci.isEmpty());
				assertEquals(1, decisionsAci.size());
				DecisionAci d = decisionsAci.get(0);
				assertEquals(idsDecision.idOriginal, d.getId());
				assertEquals("Nouvelle Remarque", d.getRemarque());

			}
		});
	}

	@Test
	public void testMiseAJourAutoriteDecisions() throws Exception {

		final class IdsDecision {
			 Long idOriginal;
			 Long idNouvel;
		}
		final class Ids {
			long ppM;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
			final Ids ids1 = new Ids();
			ids1.ppM = m.getNumero();
			return ids1;
		});

		// mise en place decision
		final IdsDecision idsDecision = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = (PersonnePhysique) tiersDAO.get(ids.ppM);
			DecisionAci d = addDecisionAci(m,date(2006,11,1),null,MockCommune.Lausanne.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final IdsDecision idDec = new IdsDecision();
			idDec.idOriginal = d.getId();
			return idDec;
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				DecisionAci amodifier = decisionAciDAO.get(idsDecision.idOriginal);
				DecisionAci modifiee =tiersService.updateDecisionAci(amodifier, null, null, MockCommune.Aubonne.getNoOFS());
				idsDecision.idNouvel = modifiee.getId();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppM);
				Assert.assertNotNull(pp);
				final List<DecisionAci> decisionsAci = new ArrayList<>(pp.getDecisionsAci());
				assertNotNull(decisionsAci);
				assertFalse(decisionsAci.isEmpty());
				assertEquals(2, decisionsAci.size());
				DecisionAci dOriginal = decisionAciDAO.get(idsDecision.idOriginal);
				DecisionAci dNouvelle = decisionAciDAO.get(idsDecision.idNouvel);
				assertTrue(dOriginal.isAnnule());
				assertEquals(MockCommune.Aubonne.getNoOFS(), dNouvelle.getNumeroOfsAutoriteFiscale().intValue());

			}
		});
	}

	@Test
	public void testPremiereMiseAJourDateFinRemarqueDecisions() throws Exception {

		final class IdsDecision {
			Long idOriginal;
			Long idNouvel;
		}
		final class Ids {
			long ppM;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
			final Ids ids1 = new Ids();
			ids1.ppM = m.getNumero();
			return ids1;
		});

		// mise en place decision
		final IdsDecision idsDecision = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = (PersonnePhysique) tiersDAO.get(ids.ppM);
			DecisionAci d = addDecisionAci(m,date(2006,11,1),null,MockCommune.Lausanne.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final IdsDecision idDec = new IdsDecision();
			idDec.idOriginal = d.getId();
			return idDec;
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				DecisionAci amodifier = decisionAciDAO.get(idsDecision.idOriginal);
				DecisionAci modifiee =tiersService.updateDecisionAci(amodifier, date(2013, 5, 6), "Ma remarque", null);
				idsDecision.idNouvel = modifiee.getId();
			}
		});

		// vérification de l'état après départ
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppM);
				Assert.assertNotNull(pp);
				final List<DecisionAci> decisionsAci = new ArrayList<>(pp.getDecisionsAci());
				assertNotNull(decisionsAci);
				assertFalse(decisionsAci.isEmpty());
				assertEquals(1, decisionsAci.size());
				DecisionAci dOriginal = decisionAciDAO.get(idsDecision.idOriginal);
				assertFalse(dOriginal.isAnnule());
				assertEquals(date(2013, 5, 6), dOriginal.getDateFin());
				assertEquals("Ma remarque",dOriginal.getRemarque());

			}
		});
	}

	@Test
	public void testMiseAJourDateFinEtRemarqueDecisions() throws Exception {

		final class IdsDecision {
			Long idOriginal;
			Long idNouvel;
		}
		final class Ids {
			long ppM;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
			final Ids ids1 = new Ids();
			ids1.ppM = m.getNumero();
			return ids1;
		});

		// mise en place decision
		final IdsDecision idsDecision = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = (PersonnePhysique) tiersDAO.get(ids.ppM);
			DecisionAci d = addDecisionAci(m,date(2006,11,1),date(2013,12,10),MockCommune.Lausanne.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,"ma remarque");
			final IdsDecision idDec = new IdsDecision();
			idDec.idOriginal = d.getId();
			return idDec;
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				DecisionAci amodifier = decisionAciDAO.get(idsDecision.idOriginal);
				DecisionAci modifiee =tiersService.updateDecisionAci(amodifier,date(2012,12,10),"finalement rien",MockCommune.Lausanne.getNoOFS());
				idsDecision.idNouvel = modifiee.getId();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppM);
				Assert.assertNotNull(pp);
				final List<DecisionAci> decisionsAci = new ArrayList<>(pp.getDecisionsAci());
				assertNotNull(decisionsAci);
				assertFalse(decisionsAci.isEmpty());
				assertEquals(2, decisionsAci.size());
				DecisionAci dOriginal = decisionAciDAO.get(idsDecision.idOriginal);
				DecisionAci dNouvelle = decisionAciDAO.get(idsDecision.idNouvel);
				assertTrue(dOriginal.isAnnule());
				assertEquals(MockCommune.Lausanne.getNoOFS(), dNouvelle.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(date(2012, 12, 10), dNouvelle.getDateFin());
				assertEquals("finalement rien",dNouvelle.getRemarque());


			}
		});
	}

	//une personne se marie 2 fois, en moins de 5 ans, on pose une décision ACI sur son ex.
	//Il doit être sous influence,
	@Test
	public void testinfluenceDecisionsSurExConjoint() throws Exception {

		final class IdsDecision {
			public Long idOriginal;
		}
		final class Ids {
			long ppLui;
			long ppElle;
			long ppElleEx;
			long menageCommun;
			long menageCommunEx;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
			final PersonnePhysique mm = addNonHabitant("Juliane", "Dubourg", null, Sexe.FEMININ);
			final PersonnePhysique mmEx = addNonHabitant("Valentine", "Dubourg", null, Sexe.FEMININ);
			final int anneeFinAncienCouple = RegDate.get().year() -4;
			final int anneeDebutNouveauCouple = RegDate.get().year() -3;
			EnsembleTiersCouple etcEx = addEnsembleTiersCouple(m,mmEx,date(2001,3,7),date(anneeFinAncienCouple,5,1));
			EnsembleTiersCouple etc = addEnsembleTiersCouple(m,mm,date(anneeDebutNouveauCouple,5,7),null);
			final MenageCommun mc = etc.getMenage();
			final MenageCommun mcEx = etcEx.getMenage();
			final Ids ids1 = new Ids();
			ids1.ppLui = m.getNumero();
			ids1.ppElle = mm.getNumero();
			ids1.ppElleEx = mmEx.getNumero();
			ids1.menageCommun = mc.getNumero();
			ids1.menageCommunEx = mcEx.getNumero();

			return ids1;
		});

		// mise en place decision
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique personneEx = (PersonnePhysique) tiersDAO.get(ids.ppElleEx);
			DecisionAci d = addDecisionAci(personneEx,date(2013,11,1),null,MockCommune.Lausanne.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final IdsDecision idDec = new IdsDecision();
			idDec.idOriginal = d.getId();
			return idDec;
		});


		// vérification de la situation sur le nouveau couple,
		//tout le monde sous influence de la décision aci posé sur l'ex, c'est la nouvelle femme qui va être contente ....
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique ppLui = (PersonnePhysique) tiersDAO.get(ids.ppLui);
				Assert.assertNotNull(ppLui);
				assertTrue(tiersService.isSousInfluenceDecisions(ppLui));

				final MenageCommun nouveauMc = (MenageCommun) tiersDAO.get(ids.menageCommun);
				Assert.assertNotNull(nouveauMc);
				assertTrue(tiersService.isSousInfluenceDecisions(nouveauMc));

				final PersonnePhysique ppElle = (PersonnePhysique) tiersDAO.get(ids.ppElle);
				Assert.assertNotNull(ppElle);
				assertTrue(tiersService.isSousInfluenceDecisions(ppElle));
			}
		});
	}

	@Test
	public void testinfluenceDecisionsApresMenageVieuxDeCinqAns() throws Exception {

		final class IdsDecision {
			public Long idOriginal;
		}
		final class Ids {
			long ppLui;
			long ppElle;
			long ppElleEx;
			long menageCommun;
			long menageCommunEx;
		}
		final int anneeDebutDecisionAci = RegDate.get().year() -1;

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
			final PersonnePhysique mm = addNonHabitant("Juliane", "Dubourg", null, Sexe.FEMININ);
			final PersonnePhysique mmEx = addNonHabitant("Valentine", "Dubourg", null, Sexe.FEMININ);
			final int anneeFinAncienCouple = RegDate.get().year() -6;
			final int anneeDebutNouveauCouple = RegDate.get().year() -3;

			EnsembleTiersCouple etcEx = addEnsembleTiersCouple(m,mmEx,date(2001,3,7),date(anneeFinAncienCouple,5,1));
			EnsembleTiersCouple etc = addEnsembleTiersCouple(m,mm,date(anneeDebutNouveauCouple,5,7),null);
			final MenageCommun mc = etc.getMenage();
			final MenageCommun mcEx = etcEx.getMenage();
			final Ids ids1 = new Ids();
			ids1.ppLui = m.getNumero();
			ids1.ppElle = mm.getNumero();
			ids1.ppElleEx = mmEx.getNumero();
			ids1.menageCommun = mc.getNumero();
			ids1.menageCommunEx = mcEx.getNumero();

			return ids1;
		});

		// mise en place decision
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique personneEx = (PersonnePhysique) tiersDAO.get(ids.ppElleEx);
			DecisionAci d = addDecisionAci(personneEx,date(anneeDebutDecisionAci,11,1),null,MockCommune.Lausanne.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final IdsDecision idDec = new IdsDecision();
			idDec.idOriginal = d.getId();
			return idDec;
		});


		// vérification de la situation sur le nouveau couple,
		//tout le monde sous influence de la décision aci posé sur l'ex, c'est la nouvelle femme qui va être contente ....
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique ppLui = (PersonnePhysique) tiersDAO.get(ids.ppLui);
				Assert.assertNotNull(ppLui);
				assertFalse(tiersService.isSousInfluenceDecisions(ppLui));

				final PersonnePhysique ppEx = (PersonnePhysique) tiersDAO.get(ids.ppElleEx);
				Assert.assertNotNull(ppEx);
				assertTrue(tiersService.isSousInfluenceDecisions(ppEx));

				final MenageCommun nouveauMc = (MenageCommun) tiersDAO.get(ids.menageCommun);
				Assert.assertNotNull(nouveauMc);
				assertFalse(tiersService.isSousInfluenceDecisions(nouveauMc));

				final PersonnePhysique ppElle = (PersonnePhysique) tiersDAO.get(ids.ppElle);
				Assert.assertNotNull(ppElle);
				assertFalse(tiersService.isSousInfluenceDecisions(ppElle));
			}
		});
	}

	@Test
	public void testExercicesCommerciauxSansForNiBouclement() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			return entreprise.getNumero();
		});

		// vérification du calcul : rien
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);
				Assert.assertEquals(0, exercices.size());
			}
		});
	}

	@Test
	public void testExercicesCommerciauxSansForMaisAvecBouclements() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addBouclement(entreprise, date(2000, 12, 1), DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// vérification du calcul : le premier exercice commercial commence au lendemain de la première date de bouclement connue, et se termine cette année
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);

				final int nbExercicesAttendus = RegDate.get().year() - 2000;
				Assert.assertEquals(nbExercicesAttendus, exercices.size());
				for (int i = 0 ; i < nbExercicesAttendus ; ++ i) {
					final ExerciceCommercial ex = exercices.get(i);
					Assert.assertNotNull(ex);
					Assert.assertEquals(date(i + 2001, 1, 1), ex.getDateDebut());
					Assert.assertEquals(date(i + 2001, 12, 31), ex.getDateFin());
				}
			}
		});
	}

	@Test
	public void testExercicesCommerciauxAvecForOuvertMaisSansBouclements() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, date(2000, 1, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2000, 1, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2000, 1, 3), null, MockCommune.Bale);
			return entreprise.getNumero();
		});

		// vérification du calcul : un seul exercice commercial qui couvre l'intervale maximal des fors principaux et se ferme à la fin de cette année (décision arbitraire)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);
				Assert.assertEquals(1, exercices.size());

				final ExerciceCommercial ex = exercices.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2000, 1, 3), ex.getDateDebut());
				Assert.assertEquals(date(RegDate.get().year(), 12, 31), ex.getDateFin());
			}
		});
	}

	@Test
	public void testExercicesCommerciauxAvecForsFermesMaisSansBouclements() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addForPrincipal(entreprise, date(2000, 1, 3), null, date(2006, 12, 5), MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MockCommune.Bale);
			return entreprise.getNumero();
		});

		// vérification du calcul : un seul exercice commercial qui couvre l'intervale maximal des fors principaux et se ferme à la date de fermeture du for
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);
				Assert.assertEquals(1, exercices.size());

				final ExerciceCommercial ex = exercices.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2000, 1, 3), ex.getDateDebut());
				Assert.assertEquals(date(2006, 12, 5), ex.getDateFin());
			}
		});
	}

	@Test
	public void testExercicesCommerciauxAvecForOuvertEtBouclements() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2000, 5, 3), null, MockCommune.Bale);
			addBouclement(entreprise, date(2001, 6, 1), DayMonth.get(6, 30), 12);       // tous les ans depuis le 30.06.2001
			return entreprise.getNumero();
		});

		// vérification du calcul : exercices commerciaux annuels 01.07 -> 30.06 (sauf le premier qui commence au 03.05)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);

				final int nbExercicesAttendus = RegDate.get().year() - 2000 + (DayMonth.get().compareTo(DayMonth.get(6, 30)) > 0 ? 1 : 0);
				Assert.assertEquals(nbExercicesAttendus, exercices.size());
				{
					final ExerciceCommercial ex = exercices.get(0);
					Assert.assertNotNull(ex);
					Assert.assertEquals(date(2000, 5, 3), ex.getDateDebut());
					Assert.assertEquals(date(2001, 6, 30), ex.getDateFin());
				}
				for (int i = 1 ; i < nbExercicesAttendus ; ++ i) {
					final ExerciceCommercial ex = exercices.get(i);
					Assert.assertNotNull(ex);
					Assert.assertEquals(date(i + 2000, 7, 1), ex.getDateDebut());
					Assert.assertEquals(date(i + 2001, 6, 30), ex.getDateFin());
				}
			}
		});
	}

	@Test
	public void testExercicesCommerciauxAvecForsFermesEtBouclements() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addForPrincipal(entreprise, date(2000, 5, 3), null, date(2006, 12, 5), MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MockCommune.Bale);
			addBouclement(entreprise, date(2001, 6, 1), DayMonth.get(6, 30), 12);       // tous les ans depuis le 30.06.2001
			return entreprise.getNumero();
		});

		// vérification du calcul : exercices commerciaux annuels 01.07 -> 30.06 (sauf le premier qui commence au 03.05, et le dernier qui se termine avec le for)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);

				Assert.assertEquals(7, exercices.size());
				{
					final ExerciceCommercial ex = exercices.get(0);
					Assert.assertNotNull(ex);
					Assert.assertEquals(date(2000, 5, 3), ex.getDateDebut());
					Assert.assertEquals(date(2001, 6, 30), ex.getDateFin());
				}
				for (int i = 1 ; i < 6 ; ++ i) {
					final ExerciceCommercial ex = exercices.get(i);
					Assert.assertNotNull(ex);
					Assert.assertEquals(date(i + 2000, 7, 1), ex.getDateDebut());
					Assert.assertEquals(date(i + 2001, 6, 30), ex.getDateFin());
				}
				{
					final ExerciceCommercial ex = exercices.get(6);
					Assert.assertNotNull(ex);
					Assert.assertEquals(date(2006, 7, 1), ex.getDateDebut());
					Assert.assertEquals(date(2006, 12, 5), ex.getDateFin());
				}
			}
		});
	}

	@Test
	public void testGetCapitaux() throws Exception {

		final long noEntrepriseCivile = 48518745L;
		final long noEtablissement = 346742L;
		final RegDate dateDebut = date(2000, 1, 4);
		final RegDate dateDebutSurchargeCapital = date(2005, 3, 1);
		final RegDate dateFinSurchargeCapital = date(2012, 6, 30);

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noEntrepriseCivile);
				MockEtablissementCivilFactory.addEtablissement(noEtablissement, ent, dateDebut, null, "Turlututu SARL", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, true,
				                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
				                                               StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996",
				                                               BigDecimal.valueOf(10000000L), MontantMonetaire.CHF);
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addCapitalEntreprise(entreprise, dateDebutSurchargeCapital, dateFinSurchargeCapital, new MontantMonetaire(42L, MontantMonetaire.CHF));
			return entreprise.getNumero();
		});

		// récupération des capitaux d'après le tiers service
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				final List<CapitalHisto> capitaux = tiersService.getCapitaux(entreprise, false);
				Assert.assertNotNull(capitaux);
				Assert.assertEquals(3, capitaux.size());
				{
					final CapitalHisto capital = capitaux.get(0);
					Assert.assertNotNull(capital);
					Assert.assertEquals(dateDebut, capital.getDateDebut());
					Assert.assertEquals(dateDebutSurchargeCapital.getOneDayBefore(), capital.getDateFin());
					Assert.assertEquals((Long) 10000000L, capital.getMontant().getMontant());
					Assert.assertEquals("CHF", capital.getMontant().getMonnaie());
					Assert.assertEquals(Source.CIVILE, capital.getSource());
				}
				{
					final CapitalHisto capital = capitaux.get(1);
					Assert.assertNotNull(capital);
					Assert.assertEquals(dateDebutSurchargeCapital, capital.getDateDebut());
					Assert.assertEquals(dateFinSurchargeCapital, capital.getDateFin());
					Assert.assertEquals((Long) 42L, capital.getMontant().getMontant());
					Assert.assertEquals("CHF", capital.getMontant().getMonnaie());
					Assert.assertEquals(Source.FISCALE, capital.getSource());
				}
				{
					final CapitalHisto capital = capitaux.get(2);
					Assert.assertNotNull(capital);
					Assert.assertEquals(dateFinSurchargeCapital.getOneDayAfter(), capital.getDateDebut());
					Assert.assertNull(capital.getDateFin());
					Assert.assertEquals((Long) 10000000L, capital.getMontant().getMontant());
					Assert.assertEquals("CHF", capital.getMontant().getMonnaie());
					Assert.assertEquals(Source.CIVILE, capital.getSource());
				}
			}
		});
	}

	@Test
	public void testPeriodesDeResidenceSurInconnuAuCivil() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = addNonHabitant("Alfred", "Dirladada", date(1987, 5, 12), Sexe.MASCULIN);
			addAdresseSuisse(ctb, TypeAdresseTiers.COURRIER, date(1987, 5, 12), null, MockRue.CossonayVille.CheminDeRiondmorcel);
			return ctb.getNumero();
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				Assert.assertNotNull(ctb);
				Assert.assertNull(ctb.getNumeroIndividu());

				Assert.assertEquals(Collections.emptyList(), tiersService.getPeriodesDeResidence(ctb, true));
				Assert.assertEquals(Collections.emptyList(), tiersService.getPeriodesDeResidence(ctb, false));
			}
		});
	}

	@Test
	public void testPeriodesDeResidenceSurHabitantPrincipalParti() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);
		final RegDate dateDepart = date(2015, 2, 13);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, dateDepart);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = tiersService.createNonHabitantFromIndividu(noIndividu);
			return ctb.getNumero();
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				Assert.assertNotNull(ctb);
				Assert.assertEquals((Long) noIndividu, ctb.getNumeroIndividu());
				Assert.assertFalse(ctb.isHabitantVD());

				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, dateDepart)), tiersService.getPeriodesDeResidence(ctb, true));
				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, dateDepart)), tiersService.getPeriodesDeResidence(ctb, false));
			}
		});
	}

	@Test
	public void testPeriodesDeResidenceSurHabitantSecondaireParti() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);
		final RegDate dateDepart = date(2015, 2, 13);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, dateDepart);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = tiersService.createNonHabitantFromIndividu(noIndividu);
			return ctb.getNumero();
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				Assert.assertNotNull(ctb);
				Assert.assertEquals((Long) noIndividu, ctb.getNumeroIndividu());
				Assert.assertFalse(ctb.isHabitantVD());

				Assert.assertEquals(Collections.emptyList(), tiersService.getPeriodesDeResidence(ctb, true));
				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, dateDepart)), tiersService.getPeriodesDeResidence(ctb, false));
			}
		});
	}

	@Test
	public void testPeriodesDeResidenceSurHabitantPrincipalToujoursLa() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = addHabitant(noIndividu);
			return ctb.getNumero();
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				Assert.assertNotNull(ctb);
				Assert.assertEquals((Long) noIndividu, ctb.getNumeroIndividu());
				Assert.assertTrue(ctb.isHabitantVD());

				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, null)), tiersService.getPeriodesDeResidence(ctb, true));
				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, null)), tiersService.getPeriodesDeResidence(ctb, false));
			}
		});
	}

	@Test
	public void testPeriodesDeResidenceSurHabitantSecondaireToujoursLa() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = addHabitant(noIndividu);
			return ctb.getNumero();
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				Assert.assertNotNull(ctb);
				Assert.assertEquals((Long) noIndividu, ctb.getNumeroIndividu());
				Assert.assertTrue(ctb.isHabitantVD());

				Assert.assertEquals(Collections.emptyList(), tiersService.getPeriodesDeResidence(ctb, true));
				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, null)), tiersService.getPeriodesDeResidence(ctb, false));
			}
		});
	}

	@Test
	public void testPeriodesDeResidenceSurPrincipalPartiEtRevenuEnSecondaireToujoursLa() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);
		final RegDate dateDepart = date(2010, 4, 22);
		final RegDate dateRetour = date(2015, 3, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, dateDepart);
				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateRetour, null);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = addHabitant(noIndividu);
			return ctb.getNumero();
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				Assert.assertNotNull(ctb);
				Assert.assertEquals((Long) noIndividu, ctb.getNumeroIndividu());
				Assert.assertTrue(ctb.isHabitantVD());

				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, dateDepart)), tiersService.getPeriodesDeResidence(ctb, true));
				Assert.assertEquals(Arrays.asList(new DateRangeHelper.Range(dateNaissance, dateDepart),
				                                  new DateRangeHelper.Range(dateRetour, null)),
				                    tiersService.getPeriodesDeResidence(ctb, false));
			}
		});
	}

	@Test
	public void testPeriodesDeResidenceSurAncienHabitantDecede() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);
		final RegDate dateDeces = date(2010, 4, 22);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				individu.setDateDeces(dateDeces);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = tiersService.createNonHabitantFromIndividu(noIndividu);
			return ctb.getNumero();
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				Assert.assertNotNull(ctb);
				Assert.assertEquals((Long) noIndividu, ctb.getNumeroIndividu());
				Assert.assertFalse(ctb.isHabitantVD());

				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, dateDeces)), tiersService.getPeriodesDeResidence(ctb, true));
				Assert.assertEquals(Collections.singletonList(new DateRangeHelper.Range(dateNaissance, dateDeces)), tiersService.getPeriodesDeResidence(ctb, false));
			}
		});
	}

	@Test
	public void testAddRaisonSociale() throws Exception {

		final String donne = " Ma  petite \n\rentreprise de \trédaction de texte ";
		final String attendu = "Ma petite entreprise de rédaction de texte";

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			try {
				tiersService.addRaisonSocialeFiscale(entreprise, donne, date(2017, 1, 24), null);
			}
			catch (TiersException e) {
				return null;
			}
			return entreprise.getNumero();
		});

		// vérification de la raison sociale telle qu'enregistrée en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals(attendu, tiersService.getDerniereRaisonSociale(entreprise));
			}
		});
	}

	/**
	 * [SIFISC-19923] Fermeture des rapports entre tiers pour les établissements du For fiscal secondaire (ETABLISSEMENT_STABLE) clos
	 */
	@Test
	public void testCloseForFiscalSecondaireEtablissementStable() throws Exception {

		class ValueStore {
			Long activiteEconomiqueId;
			Long forFiscalSecondaireEtabStableId;
			final int numeroOFS = MockCommune.Bex.getNoOFS();
			final RegDate dateFermeture = date(2017, 10, 23);
		}
		final ValueStore values = new ValueStore();

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2000, 5, 3), null, MockCommune.Bale);
			ForFiscalSecondaire forFiscalSecondaireEtabStable = addForSecondaire(entreprise, date(2000, 5, 3), MotifFor.DEBUT_EXPLOITATION, values.numeroOFS, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.REVENU_FORTUNE);

			Etablissement etablissement = addEtablissement();
			addDomicileEtablissement(etablissement, date(2000, 5, 5), null, MockCommune.Bex);
			ActiviteEconomique activiteEconomique = addActiviteEconomique(entreprise, etablissement, date(2000, 5, 5), null, false);

			values.activiteEconomiqueId = activiteEconomique.getId();
			values.forFiscalSecondaireEtabStableId = forFiscalSecondaireEtabStable.getId();
			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				final ForFiscalSecondaire forFiscalSecondaireEtabStable = (ForFiscalSecondaire) forFiscalDAO.get(values.forFiscalSecondaireEtabStableId);
				final ActiviteEconomique activiteEconomique = (ActiviteEconomique) rapportEntreTiersDAO.get(values.activiteEconomiqueId);

				// Fermeture du For fiscal secondaire Etablissement Stable
				// Contrôles sur l'activité économique (Rapport entre tiers) avant sa clôture
				assertNull(activiteEconomique.getDateFin());

				// Fermeture du For fiscal secondaire (ETABLISSEMENT_STABLE)
				tiersService.closeForFiscalSecondaire(entreprise, forFiscalSecondaireEtabStable, values.dateFermeture, MotifFor.FIN_EXPLOITATION);

				// Contrôles sur l'activité économique (Rapport entre tiers) après sa clôture
				assertSame(activiteEconomique.getDateFin(), values.dateFermeture);


			}
		});
	}

	/**
	 * [SIFISC-19923] Pas de Fermeture des rapports entre tiers pour les établissements du For fiscal secondaire (IMMEUBLE_PRIVE) clos
	 */
	@Test
	public void testCloseForFiscalSecondaireImmeublePrive() throws Exception {

		class ValueStore {
			Long activiteEconomiqueId;
			Long forFiscalSecondaireImmPrvId;
			final int numeroOFS = MockCommune.Echallens.getNoOFS();
			final RegDate dateFermeture = date(2017, 10, 23);
		}
		final ValueStore values = new ValueStore();

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2000, 5, 3), null, MockCommune.Bale);
			ForFiscalSecondaire forFiscalSecondaireImmPrv = addForSecondaire(entreprise, date(2000, 5, 3), MotifFor.DEBUT_EXPLOITATION, values.numeroOFS, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.REVENU_FORTUNE);

			Etablissement etablissement = addEtablissement();
			addDomicileEtablissement(etablissement, date(2000, 5, 5), null, MockCommune.Echallens);
			ActiviteEconomique activiteEconomique = addActiviteEconomique(entreprise, etablissement, date(2000, 5, 5), null, false);

			values.activiteEconomiqueId = activiteEconomique.getId();
			values.forFiscalSecondaireImmPrvId = forFiscalSecondaireImmPrv.getId();
			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				final ForFiscalSecondaire forFiscalSecondaireImmPrv = (ForFiscalSecondaire) forFiscalDAO.get(values.forFiscalSecondaireImmPrvId);
				final ActiviteEconomique activiteEconomique = (ActiviteEconomique) rapportEntreTiersDAO.get(values.activiteEconomiqueId);

				// Contrôles sur l'activité économique (Rapport entre tiers) avant sa clôture
				assertNull(activiteEconomique.getDateFin());

				// Fermeture du For fiscal secondaire (IMMEUBLE_PRIVE)
				tiersService.closeForFiscalSecondaire(entreprise, forFiscalSecondaireImmPrv, values.dateFermeture, MotifFor.FIN_EXPLOITATION);

				// Contrôles sur l'activité économique (Rapport entre tiers) après sa clôture
				assertNull(activiteEconomique.getDateFin());


			}
		});
	}

	/**
	 * [SIFISC-19923] pour for fiscal secondaire "ETABLISSEMENT_STABLE" réouvert, réouverture des établissements (rapports entre tiers) correspondants
	 */
	@Test
	public void testCorrigerForFiscalSecondaireEtablissementStable() throws Exception {
		class ValuesStore {
			Long activiteEconomiqueId;
			Long forFiscalSecondaireId;
			final int numeroOFS = MockCommune.Lausanne.getNoOFS();
			final RegDate dateFermeture = date(2017, 10, 23);
		}
		final ValuesStore values = new ValuesStore();

		// mise en place
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2000, 5, 3), null, MockCommune.Bale);
			// For fiscal secondaire clos
			ForFiscalSecondaire forFiscalSecondaire =
					addForSecondaire(entreprise, date(2000, 5, 3), MotifFor.DEBUT_EXPLOITATION, values.dateFermeture, MotifFor.FIN_EXPLOITATION, values.numeroOFS, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.REVENU_FORTUNE);

			Etablissement etablissement = addEtablissement();
			addDomicileEtablissement(etablissement, date(2000, 5, 5), values.dateFermeture, MockCommune.Lausanne);
			ActiviteEconomique activiteEconomique = addActiviteEconomique(entreprise, etablissement, date(2000, 5, 5), values.dateFermeture, false);

			values.activiteEconomiqueId = activiteEconomique.getId();
			values.forFiscalSecondaireId = forFiscalSecondaire.getId();
			return entreprise.getNumero();
		});


		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscalDAO.get(values.forFiscalSecondaireId);
				final ActiviteEconomique activiteEconomique = (ActiviteEconomique) rapportEntreTiersDAO.get(values.activiteEconomiqueId);

				// contrôle sur l'activité économique (Rapport entre tiers) avant la réouverture du for fiscal secondaire
				assertSame(activiteEconomique.getDateFin(), values.dateFermeture);

				// Réouverture du For fiscal secondaire (ETABLISSEMENT_STABLE)
				tiersService.updateForSecondaire(forFiscalSecondaire, forFiscalSecondaire.getDateDebut(), forFiscalSecondaire.getMotifOuverture(), null, null, values.numeroOFS);

				// contrôle sur l'activité économique (Rapport entre tiers) après la réouverture du for fiscal secondaire
				assertNull(activiteEconomique.getDateFin());
			}
		});
	}

	/**
	 * [SIFISC-19923] Lors de la réouverture du for fiscal secondaire,<BR />
	 * pas de réouverture des rapports entre tiers autre que "ETABLISSEMENT_STABLE"
	 */
	@Test
	public void testCorrigerForFiscalSecondaireImmeuble() throws Exception {
		class ValuesStore {
			Long activiteEconomiqueId;
			Long forFiscalSecondaireId;
			final int numeroOFS = MockCommune.Renens.getNoOFS();
			final RegDate dateFermeture = date(2017, 10, 23);
		}
		final ValuesStore values = new ValuesStore();

		// mise en place
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2000, 5, 3), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2000, 5, 3), null, MockCommune.Bale);
			// For fiscal secondaire clos
			ForFiscalSecondaire forFiscalSecondaire =
					addForSecondaire(entreprise, date(2001, 5, 3), MotifFor.DEBUT_EXPLOITATION, values.dateFermeture, MotifFor.FIN_EXPLOITATION, values.numeroOFS, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.REVENU_FORTUNE);

			Etablissement etablissement = addEtablissement();
			addDomicileEtablissement(etablissement, date(2000, 5, 5), values.dateFermeture, MockCommune.Renens);
			ActiviteEconomique activiteEconomique = addActiviteEconomique(entreprise, etablissement, date(2000, 5, 5), values.dateFermeture, false);

			values.activiteEconomiqueId = activiteEconomique.getId();
			values.forFiscalSecondaireId = forFiscalSecondaire.getId();
			return entreprise.getNumero();
		});


		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ActiviteEconomique activiteEconomique = (ActiviteEconomique) rapportEntreTiersDAO.get(values.activiteEconomiqueId);
				final ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscalDAO.get(values.forFiscalSecondaireId);

				// Contrôle de l'activité économique (Rapport entre tiers) avant réouverture du for fiscal secondaire (IMMEUBLE_PRIVE)
				assertSame(activiteEconomique.getDateFin(), values.dateFermeture);

				// Réouverture du For fiscal secondaire
				tiersService.updateForSecondaire(forFiscalSecondaire, forFiscalSecondaire.getDateDebut(), forFiscalSecondaire.getMotifOuverture(), null, null, values.numeroOFS);

				// Contrôle de l'activité économique après réouverture du for fiscal secondaire => pas de réouverture
				assertSame(activiteEconomique.getDateFin(), values.dateFermeture);
			}
		});
	}
}

