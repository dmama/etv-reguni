package ch.vd.uniregctb.tiers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockNationalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

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
	private TiersDAO tiersDAO;
	private RapportEntreTiersDAO rapportEntreTiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		rapportEntreTiersDAO = getBean(RapportEntreTiersDAO.class, "rapportEntreTiersDAO");
	}

	@Test
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
	public void testIsEtrangerSansPermisC_AvecNationaliteSuisse() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1953, 11, 2), null, 0);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));

	}

	@Test
	public void testIsEtrangerSansPermisC_AvecNationaliteEtrangere_PermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.France, RegDate.get(1953, 11, 2), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1953, 11, 2), null, 0, false);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testIsEtrangerSansPermisC_AvecNationaliteEtrangere_PermisCAnnule() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.France, RegDate.get(1953, 11, 2), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1953, 11, 2), null, 0, true);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testIsEtrangerSansPermisC_AvecNationaliteEtrangere_PermisNonC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addNationalite(pierre, MockPays.France, RegDate.get(1953, 11, 2), null, 0);
				addPermis(pierre, EnumTypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, 0, false);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué comme etranger sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineSuisse() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, EnumTypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, 0, false);
				addOrigine(pierre, MockPays.Suisse, MockCommune.Cossonay, RegDate.get(1953, 11, 2));
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineRenseigne_SansPermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, EnumTypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, 0, false);
				addOrigine(pierre, MockPays.France, null, RegDate.get(1953, 11, 2));
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué comme etranger sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineRenseigne_AvecPermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1953, 11, 2), null, 0, false);
				addOrigine(pierre, MockPays.France, null, RegDate.get(1953, 11, 2));
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineNonRenseigne_AvecPermisC() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1953, 11, 2), null, 0, false);
				addOrigine(pierre, null, null, RegDate.get(1953, 11, 2));
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertFalse("Pierre devrait être indiqué comme suisse", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testGetIndividu() {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1953, 11, 2), null, 0, false);
				addOrigine(pierre, null, null, RegDate.get(1953, 11, 2));
			}
		});

		{
			PersonnePhysique hab = new PersonnePhysique(true);
			hab.setNumeroIndividu(NUMERO_INDIVIDU);
			Individu ind = tiersService.getIndividu(hab);
			assertNotNull(ind);
		}

		try {
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
	public void testGetIndividuParAnnee() {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, EnumTypePermis.FRONTALIER, RegDate.get(1953, 11, 2), RegDate.get(1979, 12, 31), 0, false);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1980, 1, 1), null, 0, false);
				addOrigine(pierre, null, null, RegDate.get(1953, 11, 2));
			}
		});

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(NUMERO_INDIVIDU);

		{
			// Vue de l'habitant pour 1960
			final Individu ind = tiersService.getIndividu(hab, 1960, new AttributeIndividu[]{
					AttributeIndividu.PERMIS
			});
			assertNotNull(ind);

			assertEquals(EnumTypePermis.FRONTALIER, ind.getPermis().iterator().next().getTypePermis());
		}

		{
			// Vue de l'habitant pour 2000
			final Individu ind = tiersService.getIndividu(hab, 2000, new AttributeIndividu[]{
					AttributeIndividu.PERMIS
			});
			assertNotNull(ind);

			final Collection<Permis> permis = ind.getPermis();
			assertEquals(2, permis.size());

			final Iterator<Permis> iter = permis.iterator();
			assertEquals(EnumTypePermis.FRONTALIER, iter.next().getTypePermis());
			assertEquals(EnumTypePermis.ETABLLISSEMENT, iter.next().getTypePermis());
		}

		{
			// Vue de l'habitant pour -1
			final Individu ind = tiersService.getIndividu(hab, -1, new AttributeIndividu[]{
					AttributeIndividu.PERMIS
			});
			assertNotNull(ind);

			final Collection<Permis> permis = ind.getPermis();
			assertEquals(2, permis.size());

			final Iterator<Permis> iter = permis.iterator();
			assertEquals(EnumTypePermis.FRONTALIER, iter.next().getTypePermis());
			assertEquals(EnumTypePermis.ETABLLISSEMENT, iter.next().getTypePermis());
		}

		{
			PersonnePhysique h = new PersonnePhysique(true);
			h.setNumeroIndividu(null);
			Individu ind = tiersService.getIndividu(h);
			assertNull(ind);
		}
	}

	@Test
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineNonRenseigne_SansPermisC() throws Exception {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addPermis(pierre, EnumTypePermis.COURTE_DUREE, RegDate.get(1953, 11, 2), null, 0, false);
				addOrigine(pierre, null, null, RegDate.get(1953, 11, 2));
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		assertTrue("Pierre devrait être indiqué comme étranger sans permis C", tiersService.isEtrangerSansPermisC(habitant, null));
	}

	@Test
	public void testIsEtrangerSansPermisC_SansNationalite_OrigineNonSuisse_PaysOrigineNonRenseigne_SansAucunPermis() {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(NUMERO_INDIVIDU, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				addOrigine(pierre, null, null, RegDate.get(1953, 11, 2));
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
	public void testOpenForFiscalPrincipal() {

		final RegDate dateOuverture = RegDate.get(1990, 7, 1);

		serviceCivil.setUp(new DefaultMockServiceCivil());

		final Set<ForFiscal> forsFiscaux = new HashSet<ForFiscal>();
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(54321L);
		habitant.setForsFiscaux(forsFiscaux);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);
		assertEquals(0, habitant.getForsFiscaux().size());

		tiersService.openForFiscalPrincipal(habitant, dateOuverture, MotifRattachement.DOMICILE, MockCommune.Cossonay.getNoOFSEtendu(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HC, true);
		assertEquals(1, habitant.getForsFiscaux().size());

		ForFiscalPrincipal ff = (ForFiscalPrincipal) habitant.getForsFiscaux().toArray()[0];
		assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), ff.getNumeroOfsAutoriteFiscale());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
		assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());
		assertEquals(MotifRattachement.DOMICILE, ff.getMotifRattachement());
		assertEquals(dateOuverture, ff.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, ff.getModeImposition());
	}

	@Test
	public void testAnnuleForFiscalPrincipalUNIREG1370() {
		final RegDate dateOuverture = RegDate.get(1990, 7, 1);

		serviceCivil.setUp(new DefaultMockServiceCivil());

		final Set<ForFiscal> forsFiscaux = new HashSet<ForFiscal>();
		PersonnePhysique pp = new PersonnePhysique(true);
		pp.setNumeroIndividu(54321L);
		pp.setForsFiscaux(forsFiscaux);
		pp.setHabitant(false);
		pp.setNom("Bolomey");
		pp = (PersonnePhysique) tiersDAO.save(pp);
		ForFiscalPrincipal forVD = tiersService.openForFiscalPrincipal(pp, dateOuverture, MotifRattachement.DOMICILE, MockCommune.Cossonay.getNoOFSEtendu(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE, MotifFor.ARRIVEE_HC, true);
		assertTrue(pp.isHabitantVD());
		tiersService.closeForFiscalPrincipal(forVD, date(2007, 12, 31), MotifFor.DEPART_HS);
		ForFiscalPrincipal forHS = tiersService.openForFiscalPrincipal(pp, date(2008, 1, 1), MotifRattachement.DOMICILE, MockPays.France.getNoOFS(),
				TypeAutoriteFiscale.PAYS_HS, ModeImposition.ORDINAIRE, MotifFor.DEPART_HS, true);
		assertTrue(!pp.isHabitantVD());
		tiersService.annuleForFiscal(forHS, true);
		assertTrue(pp.isHabitantVD());
	}

	private AppartenanceMenage buildAppartenanceMenage(MenageCommun mc, PersonnePhysique pp, RegDate dateDebut, RegDate dateFin, boolean isAnnule) {
		final AppartenanceMenage am = new AppartenanceMenage(dateDebut, dateFin, pp, mc);
		am.setAnnule(isAnnule);
		return (AppartenanceMenage) rapportEntreTiersDAO.save(am);
	}

	@Test
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

		final Set<RapportEntreTiers> rapports = new HashSet<RapportEntreTiers>();
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
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.noCtbPourAnnulation = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbPourAnnulation);

		// premier essai, le "get par numéro d'individu" doit fonctionner
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNull(pp);
				return null;
			}
		});

		// création d'un deuxième tiers avec le même numéro d'individu -> il devait alors sortir du "get par numéro d'individu"
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.noCtbAutre = addHabitant(noIndividu).getNumero();
				return null;
			}
		});

		assertNotNull(ids.noCtbAutre);
		assertTrue(ids.noCtbAutre.longValue() != ids.noCtbPourAnnulation.longValue());

		// le "get par numéro d'individu" doit maintenant retourner le deuxième tiers
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNull(pp);
				return null;
			}
		});

		// création d'un deuxième tiers avec le même numéro d'individu -> il devait alors sortir du "get par numéro d'individu"
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ids.noCtbAutre);
				assertNotNull(pp);
				pp.setAnnule(true);
				return null;
			}
		});

		// le "get par numéro d'individu" doit maintenant retourner le deuxième tiers
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransaction(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
	public void testAnnuleForPrincipalAvecPrecedentAdjacent() throws Exception {

		class Ids {
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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

		final ForFiscalPrincipal secondForPrincipal = (ForFiscalPrincipal) tiersDAO.getHibernateTemplate().get(ForFiscalPrincipal.class,
				ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);

		// annulation du second for principal
		tiersService.annuleForFiscal(secondForPrincipal, true);

		// vérification que le second for est bien annulé
		assertTrue(secondForPrincipal.isAnnule());

		// vérification que le premier for est bien ré-ouvert
		final ForFiscalPrincipal premierForPrincipal = (ForFiscalPrincipal) tiersDAO.getHibernateTemplate().get(ForFiscalPrincipal.class,
				ids.premierForPrincipalId);
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
	public void testAnnuleForSecondaireContribuableDecede() throws Exception {

		class Id {
			Long forSecondaire;
		}

		final Id id = new Id();

		doInNewTransaction(new TxCallback() {
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

		final ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) tiersDAO.getHibernateTemplate().get(ForFiscalSecondaire.class, id.forSecondaire);
		assertNotNull(forFiscalSecondaire);

		// annulation du for fiscal secondaire
		tiersService.annuleForFiscal(forFiscalSecondaire, true);

	}

	/**
	 * [UNIREG-1443] Correction bug lors de l'ouverture d'un for fiscal secondaire si le contribuable est décédé.
	 *
	 * @throws Exception
	 */
	@Test
	public void testOpenForSecondaireContribuableDecede() throws Exception {

		class Id {
			Long idEric;
		}

		final Id id = new Id();

		doInNewTransaction(new TxCallback() {
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
	public void testAnnuleForPrincipalAvecPrecedentNonAdjacents() throws Exception {

		class Ids {
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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

		final ForFiscalPrincipal secondForPrincipal = (ForFiscalPrincipal) tiersDAO.getHibernateTemplate().get(ForFiscalPrincipal.class,
				ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);

		// annulation du second for principal
		tiersService.annuleForFiscal(secondForPrincipal, true);

		// vérification que le second for est bien annulé
		assertTrue(secondForPrincipal.isAnnule());

		// vérification que le premier for n'est pas ré-ouvert
		final ForFiscalPrincipal premierForPrincipal = (ForFiscalPrincipal) tiersDAO.getHibernateTemplate().get(ForFiscalPrincipal.class,
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
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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

		final ForFiscalPrincipal premierForPrincipal = (ForFiscalPrincipal) tiersDAO.getHibernateTemplate().get(ForFiscalPrincipal.class,
				ids.premierForPrincipalId);
		assertNotNull(premierForPrincipal);
		final ForFiscalPrincipal secondForPrincipal = (ForFiscalPrincipal) tiersDAO.getHibernateTemplate().get(ForFiscalPrincipal.class,
				ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);

		// annulation du premier for principal
		try {
			tiersService.annuleForFiscal(premierForPrincipal, true);
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
	public void testIsSuisse() throws Exception {

		// individu avec nationalité suisse
		{
			MockIndividu ind = new MockIndividu();
			ind.setNationalites(Arrays.<Nationalite>asList(new MockNationalite(null, null, MockPays.Suisse, 1)));
			assertTrue(tiersService.isSuisse(ind, RegDate.get(2000, 1, 1)));
		}

		// individu avec nationalité française
		{
			MockIndividu ind = new MockIndividu();
			ind.setNationalites(Arrays.<Nationalite>asList(new MockNationalite(null, null, MockPays.France, 1)));
			assertFalse(tiersService.isSuisse(ind, RegDate.get(2000, 1, 1)));
		}

		// [UNIREG-1588] individu sans nationalité
		{
			try {
				MockIndividu ind = new MockIndividu();
				tiersService.isSuisse(ind, RegDate.get(2000, 1, 1));
				fail();
			}
			catch (TiersException e) {
				assertEquals("Impossible de déterminer la nationalité de l'individu n°0", e.getMessage());
			}

			try {
				MockIndividu ind = new MockIndividu();
				ind.setNationalites(Collections.<Nationalite>emptyList());
				tiersService.isSuisse(ind, RegDate.get(2000, 1, 1));
				fail();
			}
			catch (TiersException e) {
				assertEquals("Impossible de déterminer la nationalité de l'individu n°0", e.getMessage());
			}
		}
	}

	private ForFiscalAutreImpot newForChien(RegDate dateDebut, RegDate dateFin) {
		ForFiscalAutreImpot for0 = new ForFiscalAutreImpot();
		for0.setDateDebut(dateDebut);
		for0.setDateFin(dateFin);
		for0.setGenreImpot(GenreImpot.CHIENS);
		for0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for0.setNumeroOfsAutoriteFiscale(1234);
		return for0;
	}

	private static ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, RegDate fermeture, Integer noOFS,
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

	private static ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, RegDate fermeture, Integer noOFS,
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

	private static void assertForGestion(RegDate debut, RegDate fin, int noOfsCommune, ForGestion f) {
		assertNotNull(f);
		assertEquals(debut, f.getDateDebut());
		assertEquals(fin, f.getDateFin());
		assertEquals(noOfsCommune, f.getNoOfsCommune());
	}

	@Test
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

		final long idMenage = (Long) doInNewTransaction(new TxCallback() {
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

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				tiersService.closeForFiscalPrincipal(mc, date(2001, 12, 31), MotifFor.DEMENAGEMENT_VD);
				tiersService
						.openForFiscalPrincipal(mc, date(2002, 1, 1), MotifRattachement.DOMICILE, MockCommune.Aubonne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE,
								MotifFor.DEMENAGEMENT_VD, true);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
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

		final long idMenage = (Long) doInNewTransaction(new TxCallback() {
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


		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage);
				final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(date(2010, 2, 24));
				tiersService.annuleForFiscal(ffp, true);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
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
	public void testAnnuleDernierForFiscalPrincipalDeuxFois() throws Exception {

		final long ffpId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Simon", date(1930, 4, 3), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 5, 2), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return ffp.getId();
			}
		});

		// première annulation
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final ForFiscalPrincipal ffp = (ForFiscalPrincipal) hibernateTemplate.get(ForFiscalPrincipal.class, ffpId);
				assertNotNull(ffp);
				assertFalse(ffp.isAnnule());
				tiersService.annuleForFiscal(ffp, true);
				return null;
			}
		});

		// deuxième annulation
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final ForFiscalPrincipal ffp = (ForFiscalPrincipal) hibernateTemplate.get(ForFiscalPrincipal.class, ffpId);
				assertNotNull(ffp);
				assertTrue(ffp.isAnnule());
				try {
					tiersService.annuleForFiscal(ffp, true);
					fail("La deuxième annulation du for fiscal aurait dû être refusée !");
				}
				catch (ValidationException e) {
					assertEmpty(e.getWarnings());
					assertEquals(1, e.getErrors().size());

					final String erreur = e.getErrors().get(0);
					assertEquals("Tous les fors principaux sont déjà annulés.", erreur);
				}
				return null;
			}
		});
	}

	@Test
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

		doInNewTransaction(new TxCallback() {
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
	public void testSourcierGris() throws Exception {

		final long noCtb = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
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
	public void testSourcierGrisHorsCanton() throws Exception {

		final long noCtb = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
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
	public void testSourcierGrisHorsSuisse() throws Exception {

		final long noCtb = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
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
	public void testSourcierGrisMixte() throws Exception {

		final MutableLong idCtbMixte1 = new MutableLong();
		final MutableLong idCtbMixte2 = new MutableLong();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

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
	public void testSourcierGrisMenage() throws Exception {

		final MutableLong idCtbPrincipal = new MutableLong();
		final MutableLong idCtbConjoint = new MutableLong();
		final MutableLong idCtbCouple = new MutableLong();

		doInNewTransaction(new TxCallback() {
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
	public void testSourcierGrisMenageMarieSeul() throws Exception {

		final MutableLong idCtbPrincipal = new MutableLong();
		final MutableLong idCtbCouple = new MutableLong();

		doInNewTransaction(new TxCallback() {
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

		doInNewTransaction(new TxCallback() {
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

		doInNewTransaction(new TxCallback() {
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
	public void testGetDebiteurPrestationImposable() throws Exception {
		loadDatabase("TiersServiceTest.xml");

		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(1234L);
		Contribuable ctb = tiersService.getContribuable(dpi);
		assertNotNull(ctb);
		assertEquals(new Long(6789L), ctb.getNumero());
	}

	@Test
	public void testAddPeriodiciteDebiteurPrestationImposable() throws Exception {


		//Ajout d'une première periodicite'
		final long dpiId = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2011, 1, 1), null);
				return  dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback() {
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
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
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

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
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

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
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

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2012, 1, 1), null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
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

	@Test
	public void testGetDateDebutValiditeNouvellePeriodicite() throws Exception {
		loadDatabase("TiersServiceTest.xml");
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(1234L);
				assertEquals(RegDate.get(2009,01,01),tiersService.getDateDebutNouvellePeriodicite(dpi));
				return null;
			}
		});

	}

	@Test
	public void testAddPeriodiciteBeforAddFor() throws Exception{

		//Ajout d'une première periodicite et d'un for à la même date
		final long dpiId = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null);
				addForDebiteur(dpi,date(2010, 1, 1),null, MockCommune.Bex);
				return  dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Periodicite periodicite = dpi.getDernierePeriodicite();
				assertNotNull(periodicite);
				ForDebiteurPrestationImposable forDebiteur = dpi.getDernierForDebiteur();
				assertEquals(periodicite.getDateDebut(),forDebiteur.getDateDebut());
				return null;
			}
		});

//Ajout d'une première periodicite' et d'un for, le for ayant une date de début anterieur à celle de la périodicité
		final long dpiId2 = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 1, 1), null);
				addForDebiteur(dpi,date(2009, 6, 1),null, MockCommune.Bex);
				return  dpi.getNumero();
			}
		});



		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId2);			
				Periodicite periodicite = dpi.getDernierePeriodicite();
				assertNotNull(periodicite);
				ForDebiteurPrestationImposable forDebiteur = dpi.getDernierForDebiteur();
				assertEquals(forDebiteur.getDateDebut(),periodicite.getDateDebut());
				return null;
			}
		});


	}

	@Test
	public void testGetDateDebutValiditeNouvellePeriodiciteSansLR() throws Exception {

			//Ajout d'une première periodicite'
		final long dpiId = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				addForDebiteur(dpi,date(2009, 6, 1),null, MockCommune.Bex);
				return  dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			RegDate dateDebutPeriodicite = RegDate.get(2009, 6, 1);
			DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
			assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur));
			return null;
			}
		});

	}

	@Test
	public void testGetDateDebutValiditeNouvellePeriodiciteAvecLR() throws Exception {

			//Ajout d'une première periodicite'
		final long dpiId = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				addForDebiteur(dpi,date(2009, 6, 1),null, MockCommune.Bex);
				final PeriodeFiscale fiscale = addPeriodeFiscale(2009);

				addLR(dpi, date(2009,7,1),date(2009,9,30), fiscale, TypeEtatDeclaration.EMISE);
				return  dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			RegDate dateDebutPeriodicite = RegDate.get(2010, 1, 1);
			DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
			assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur));
			return null;
			}
		});

	}
		@Test
	public void testGetDateDebutValiditeNouvellePeriodiciteSansLRSansFor() throws Exception {

			//Ajout d'une première periodicite'
		final long dpiId = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();


				return  dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			int anneeCourante = RegDate.get().year();
			RegDate dateDebutPeriodicite = RegDate.get(anneeCourante, 1, 1);
			DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
			assertEquals(dateDebutPeriodicite, tiersService.getDateDebutNouvellePeriodicite(debiteur));
			return null;
			}
		});

	}


	@NotTransactional
	@Test
	public void testGetRaisonSocialeDebiteurSansTiersReferent() throws Exception {

		// mise en place des données fiscales
		final long dpiId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				dpi.setNom1("Tartempion");
				dpi.setNom2("Toto");
				dpi.setComplementNom("Titi");
				return dpi.getNumero();
			}
		});

		// vérification de la raison sociale d'un débiteur sans tiers référent
		doInNewTransactionAndSession(new TransactionCallback() {
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

	@NotTransactional
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
		final long dpiId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
		doInNewTransactionAndSession(new TransactionCallback() {
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

	@NotTransactional
	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentPersonnePhysique() throws Exception {

		// mise en place des données fiscales
		final long dpiId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
		doInNewTransactionAndSession(new TransactionCallback() {
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

	@NotTransactional
	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentMenageCommun() throws Exception {

		// mise en place des données fiscales
		final long dpiId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
		doInNewTransactionAndSession(new TransactionCallback() {
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

	@NotTransactional
	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentAutreCommunaute() throws Exception {

		// mise en place des données fiscales
		final long dpiId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
		doInNewTransactionAndSession(new TransactionCallback() {
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

	@NotTransactional
	@Test
	public void testGetRaisonSocialeDebiteurAvecTiersReferentCollectiviteAdministrative() throws Exception {

		// mise en place des données fiscales
		final long dpiId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
		doInNewTransactionAndSession(new TransactionCallback() {
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
	@NotTransactional
	public void testOuvertureForHorsCantonEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return pp.getNumero();
			}
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				tiersService.openForFiscalPrincipal(pp, date(2000, 5, 12), MotifRattachement.DOMICILE, MockCommune.Bale.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.ORDINAIRE, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, false);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testOuvertureForHorsSuisseEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return pp.getNumero();
			}
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				tiersService.openForFiscalPrincipal(pp, date(2000, 5, 12), MotifRattachement.DOMICILE, MockPays.RoyaumeUni.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, ModeImposition.ORDINAIRE, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, false);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testOuvertureForVaudoisEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				return pp.getNumero();
			}
		});

		// ouverture d'un for hors-canton
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
				tiersService.openForFiscalPrincipal(pp, date(2000, 5, 12), MotifRattachement.DOMICILE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, false);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testAnnulationForVaudoisSansForRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du for vaudois -> le contribuable se retrouve sans aucun for (le blocage devrait alors être réactivé)
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp, false);
				Assert.assertNull(pp.getDernierForFiscalPrincipal());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testAnnulationForVaudoisAvecForVaudoisRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ARRIVEE_HS, date(2005, 6, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.DEMENAGEMENT_VD, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du dernier for vaudois -> le contribuable a encore un for vaudois ouvert -> pas de nouveau blocage
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp, false);

				final ForFiscalPrincipal autreFfp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(autreFfp);
				Assert.assertNull("Le for précédent n'a pas été ré-ouvert ?", autreFfp.getDateFin());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) autreFfp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testAnnulationForVaudoisAvecForHorsCantonRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2005, 6, 1), MotifFor.ARRIVEE_HC, MockCommune.Bern, MotifRattachement.DOMICILE);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HC, MockCommune.Renens);
				addForSecondaire(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du dernier for vaudois -> le contribuable n'a plus de for vaudois ouvert -> blocage
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp, false);

				final ForFiscalPrincipal autreFfp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(autreFfp);
				Assert.assertNull("Le for précédent n'a pas été ré-ouvert ?", autreFfp.getDateFin());
				Assert.assertEquals(MockCommune.Bern.getNoOFSEtendu(), (int) autreFfp.getNumeroOfsAutoriteFiscale());
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testAnnulationForVaudoisAvecForHorsSuisseRestantEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2005, 6, 1), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				addForSecondaire(pp, date(2000, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// annulation du dernier for vaudois -> le contribuable n'a plus de for vaudois ouvert -> blocage
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp, false);

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
	@NotTransactional
	public void testFermetureForVaudoisDecesEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(new TransactionCallback() {
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
	@NotTransactional
	public void testFermetureForVaudoisDepartHorsSuisseEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(new TransactionCallback() {
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
	@NotTransactional
	public void testFermetureForVaudoisDepartHorsCantonEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour décès -> blocage
		doInNewTransactionAndSession(new TransactionCallback() {
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
	@NotTransactional
	public void testFermetureForVaudoisDemenagementVaudoisEtBlocageRemboursementAutomatique() throws Exception {

		// mise en place d'un contribuable
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastor", "Maugrey", date(1956, 9, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 6, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				pp.setBlocageRemboursementAutomatique(true);
				return pp.getNumero();
			}
		});

		// fermeture du for vaudois pour déménagement vaudois (avec ré-ouverture d'un autre for vaudois) -> déblocage
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());

				tiersService.closeForFiscalPrincipal(pp, date(2010, 5, 23), MotifFor.DEMENAGEMENT_VD);
				tiersService.addForPrincipal(pp, date(2010, 5, 24), MotifFor.DEMENAGEMENT_VD, null, null, MotifRattachement.DOMICILE, MockCommune.Bex.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ModeImposition.ORDINAIRE);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());

				return null;
			}
		});
	}

	@Test
	@NotTransactional
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
		final long idMenage = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndM);
				final PersonnePhysique mme = addHabitant(noIndMme);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(m, mme, dateMariage, null);
				return ensemble.getMenage().getNumero();
			}
		});

		// test
		doInNewTransactionAndSession(new TransactionCallback() {
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
	@NotTransactional
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
		final long idMenage = (Long) doInNewTransactionAndSession(new TransactionCallback() {
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
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun menage = (MenageCommun) tiersDAO.get(idMenage);
				final Long noIndividu = tiersService.extractNumeroIndividuPrincipal(menage);
				Assert.assertNull(noIndividu);
				return null;
			}
		});
	}
}
