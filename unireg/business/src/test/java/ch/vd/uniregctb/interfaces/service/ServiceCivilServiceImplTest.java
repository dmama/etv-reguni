package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilListImpl;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class ServiceCivilServiceImplTest extends BusinessTest {

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEtatCivilActifAucunEtatCivil() {

		// Aucun état civil
		final long noIndividu = 1;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				momo.setEtatsCivils(new EtatCivilListImpl());
			}
		});

		EtatCivil etatCivil1 = serviceCivil.getEtatCivilActif(noIndividu, null);
		assertNull(etatCivil1);
		EtatCivil etatCivil2 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(2000, 1, 13));
		assertNull(etatCivil2);

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEtatCivilActifUnSeulEtatCivil() {

		final long noIndividu = 1;

		// Un seul état civil = [1.3.1960..fin-des-temps]
		final MockEtatCivil ec1 = new MockEtatCivil();
		ec1.setDateDebut(RegDate.get(1960, 3, 1));

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final EtatCivilListImpl etatsCivils = new EtatCivilListImpl();
				etatsCivils.add(ec1);
				momo.setEtatsCivils(etatsCivils);
			}
		});

		EtatCivil etatCivil1 = serviceCivil.getEtatCivilActif(noIndividu, null);
		assertNotNull(etatCivil1);
		assertSame(ec1, etatCivil1);

		EtatCivil etatCivil2 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1903, 1, 13));
		assertNull(etatCivil2);

		EtatCivil etatCivil3 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1989, 11, 20));
		assertNotNull(etatCivil3);
		assertSame(ec1, etatCivil3);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEtatCivilActifListeEtatsCivils() {

		final long noIndividu = 1;

		// Une liste d'état civil (désordonnés) :
		// 1. -> [ 1.3.1930..fin-des-temps]
		// 2. -> [21.4.1985..fin-des-temps]
		// 3. -> [ 8.1.1973..fin-des-temps]
		final MockEtatCivil ec1 = new MockEtatCivil();
		final MockEtatCivil ec2 = new MockEtatCivil();
		final MockEtatCivil ec3 = new MockEtatCivil();
		ec1.setDateDebut(RegDate.get(1930, 3, 1));
		ec2.setDateDebut(RegDate.get(1985, 4, 21));
		ec3.setDateDebut(RegDate.get(1973, 1, 8));

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final EtatCivilListImpl etatsCivils = new EtatCivilListImpl();
				etatsCivils.add(ec1);
				etatsCivils.add(ec2);
				etatsCivils.add(ec3);
				momo.setEtatsCivils(etatsCivils);
			}
		});

		EtatCivil etatCivil1 = serviceCivil.getEtatCivilActif(noIndividu, null);
		assertNotNull(etatCivil1);
		assertSame(ec2, etatCivil1);

		EtatCivil etatCivil2 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1903, 1, 13));
		assertNull(etatCivil2);

		EtatCivil etatCivil3 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1940, 11, 20));
		assertNotNull(etatCivil3);
		assertSame(ec1, etatCivil3);

		EtatCivil etatCivil4 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1975, 12, 25));
		assertNotNull(etatCivil4);
		assertSame(ec3, etatCivil4);

		EtatCivil etatCivil5 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1988, 1, 2));
		assertNotNull(etatCivil5);
		assertSame(ec2, etatCivil5);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEtatCivilActifAdresseDebutNulle() {

		final long noIndividu = 1;

		/*
		 * Un seul état civil avec adresse de début nulle = [null..fin-des-temps] (ceci est un cas réel existant sur le host)
		 */
		final MockEtatCivil ec1 = new MockEtatCivil();
		ec1.setDateDebut(null);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final EtatCivilListImpl etatsCivils = new EtatCivilListImpl();
				etatsCivils.add(ec1);
				momo.setEtatsCivils(etatsCivils);
			}
		});

		EtatCivil etatCivil1 = serviceCivil.getEtatCivilActif(noIndividu, null);
		assertNotNull(etatCivil1);
		assertSame(ec1, etatCivil1);

		EtatCivil etatCivil2 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1903, 1, 13));
		assertNotNull(etatCivil2);
		assertSame(ec1, etatCivil2);

		EtatCivil etatCivil3 = serviceCivil.getEtatCivilActif(noIndividu, RegDate.get(1989, 11, 20));
		assertNotNull(etatCivil3);
		assertSame(ec1, etatCivil3);
	}

	/**
	 * Vérifie que getAdresses détecte bien des incohérences de données lorsque le mode stricte est activé.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAdressesStrict() {

		final long noIndividu = 1;

		// crée un individu avec deux adresses principales actives en même temps
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, RegDate.get(1980, 5, 12), RegDate.get(2003, 11, 28));
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, RegDate.get(2000, 1, 1), null);
			}
		});

		try {
			serviceCivil.getAdresses(noIndividu, RegDate.get(2002, 7, 1), true);
			fail();
		}
		catch (DonneesCivilesException e) {
			assertEquals(String.format("Plus d'une adresse 'principale' détectée sur l'individu n°%d et pour la date 2002.07.01.", noIndividu), e.getMessage());
		}
	}

	/**
	 * Vérifie que getAdresses ignore bien des incohérences de données lorsque le mode stricte est désactivé.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAdressesNonStrict() throws Exception {

		final long noIndividu = 1;

		// crée un individu avec deux adresses principales actives en même temps
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, RegDate.get(1980, 5, 12), RegDate.get(2003, 11, 28));
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(2000, 1, 1), null);
			}
		});

		final AdressesCiviles adresses = new AdressesCiviles(serviceCivil.getAdresses(noIndividu, RegDate.get(2002, 7, 1), false));
		assertNotNull(adresses);
		assertNull(adresses.courrier);
		assertNull(adresses.secondaire);
		assertNull(adresses.tutelle);

		// l'adresse la plus récente doit être retournée
		final Adresse principale = adresses.principale;
		assertNotNull(principale);
		assertEquals(RegDate.get(2000, 1, 1), principale.getDateDebut());
		assertNull(principale.getDateFin());
		assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier(), principale.getRue());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHistoriqueDomicilesToutesCommunes() throws Exception {

		final long noIndividu = 1L;

		final RegDate naissance = RegDate.get(1961, 3, 12);
		final RegDate arriveeZurich = RegDate.get(1980, 6, 1);
		final RegDate arriveeAllemagne = RegDate.get(1985, 11, 5);
		final RegDate arriveeCossonay = RegDate.get(1987, 3, 15);
		final RegDate arriveeLeSentier = RegDate.get(1990, 4, 22);

		// crée un individu avec tout un parcours (y compris HC/HS)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, naissance, "Durant", "Maurice", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, naissance, arriveeZurich.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, arriveeZurich, arriveeAllemagne.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, "Pariser Platz", "1", null, null, "10117 Berlin", MockPays.Allemagne, arriveeAllemagne, arriveeCossonay.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, arriveeCossonay, arriveeLeSentier.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, arriveeLeSentier, null);
			}
		});

		final List<HistoriqueCommune> domiciles = serviceCivil.getCommunesDomicileHisto(naissance, noIndividu, false, false);
		assertNotNull(domiciles);
		assertEquals(5, domiciles.size());

		// test du contenu
		{
			final HistoriqueCommune domicile = domiciles.get(0);
			assertEquals(naissance, domicile.getDateDebut());
			assertEquals(arriveeZurich.getOneDayBefore(), domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(1);
			assertEquals(arriveeZurich, domicile.getDateDebut());
			assertEquals(arriveeAllemagne.getOneDayBefore(), domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Zurich.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(2);
			assertEquals(arriveeAllemagne, domicile.getDateDebut());
			assertEquals(arriveeCossonay.getOneDayBefore(), domicile.getDateFin());
			assertNull(domicile.getCommune());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(3);
			assertEquals(arriveeCossonay, domicile.getDateDebut());
			assertEquals(arriveeLeSentier.getOneDayBefore(), domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Cossonay.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(4);
			assertEquals(arriveeLeSentier, domicile.getDateDebut());
			assertNull(domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Fraction.LeSentier.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHistoriqueDomicilesCommunesVaudoises() throws Exception {

		final long noIndividu = 1L;

		final RegDate naissance = RegDate.get(1961, 3, 12);
		final RegDate arriveeZurich = RegDate.get(1980, 6, 1);
		final RegDate arriveeAllemagne = RegDate.get(1985, 11, 5);
		final RegDate arriveeCossonay = RegDate.get(1987, 3, 15);
		final RegDate arriveeLeSentier = RegDate.get(1990, 4, 22);

		// crée un individu avec tout un parcours (y compris HC/HS)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, naissance, "Durant", "Maurice", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, naissance, arriveeZurich.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, arriveeZurich, arriveeAllemagne.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, "Pariser Platz", "1", null, null, "10117 Berlin", MockPays.Allemagne, arriveeAllemagne, arriveeCossonay.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, arriveeCossonay, arriveeLeSentier.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, arriveeLeSentier, null);
			}
		});

		final List<HistoriqueCommune> domiciles = serviceCivil.getCommunesDomicileHisto(naissance, noIndividu, false, true);
		assertNotNull(domiciles);
		assertEquals(4, domiciles.size());

		// test du contenu
		{
			final HistoriqueCommune domicile = domiciles.get(0);
			assertEquals(naissance, domicile.getDateDebut());
			assertEquals(arriveeZurich.getOneDayBefore(), domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(1);
			assertEquals(arriveeZurich, domicile.getDateDebut());
			assertEquals(arriveeCossonay.getOneDayBefore(), domicile.getDateFin());
			assertNull(domicile.getCommune());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(2);
			assertEquals(arriveeCossonay, domicile.getDateDebut());
			assertEquals(arriveeLeSentier.getOneDayBefore(), domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Cossonay.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(3);
			assertEquals(arriveeLeSentier, domicile.getDateDebut());
			assertNull(domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Fraction.LeSentier.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHistoriqueDomicilesAvecTrous() throws Exception {

		final long noIndividu = 1L;

		final RegDate naissance = RegDate.get(1961, 3, 12);
		final RegDate arriveeZurich = RegDate.get(1980, 6, 1);
		final RegDate arriveeAllemagne = RegDate.get(1985, 11, 5);
		final RegDate arriveeCossonay = RegDate.get(1987, 3, 15);
		final RegDate departCossonay = RegDate.get(1989, 10, 31);
		final RegDate arriveeLeSentier = RegDate.get(1990, 4, 22);
		final RegDate departLeSentier = RegDate.get(1999, 12, 31);

		// crée un individu avec tout un parcours (y compris HC/HS)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, naissance, "Durant", "Maurice", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, naissance, arriveeZurich.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, arriveeZurich, arriveeAllemagne.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, "Pariser Platz", "1", null, null, "10117 Berlin", MockPays.Allemagne, arriveeAllemagne, arriveeCossonay.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, arriveeCossonay, departCossonay);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, arriveeLeSentier, departLeSentier);
			}
		});

		final RegDate limite = naissance.addMonths(-12);
		final List<HistoriqueCommune> domiciles = serviceCivil.getCommunesDomicileHisto(limite, noIndividu, false, true);
		assertNotNull(domiciles);
		assertEquals(7, domiciles.size());

		// test du contenu
		{
			final HistoriqueCommune domicile = domiciles.get(0);
			assertEquals(limite.getOneDayBefore(), domicile.getDateDebut());
			assertEquals(naissance.getOneDayBefore(), domicile.getDateFin());
			assertNull(domicile.getCommune());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(1);
			assertEquals(naissance, domicile.getDateDebut());
			assertEquals(arriveeZurich.getOneDayBefore(), domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(2);
			assertEquals(arriveeZurich, domicile.getDateDebut());
			assertEquals(arriveeCossonay.getOneDayBefore(), domicile.getDateFin());
			assertNull(domicile.getCommune());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(3);
			assertEquals(arriveeCossonay, domicile.getDateDebut());
			assertEquals(departCossonay, domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Cossonay.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(4);
			assertEquals(departCossonay.getOneDayAfter(), domicile.getDateDebut());
			assertEquals(arriveeLeSentier.getOneDayBefore(), domicile.getDateFin());
			assertNull(domicile.getCommune());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(5);
			assertEquals(arriveeLeSentier, domicile.getDateDebut());
			assertEquals(departLeSentier, domicile.getDateFin());
			assertNotNull(domicile.getCommune());
			assertEquals(MockCommune.Fraction.LeSentier.getNoOFSEtendu(), domicile.getCommune().getNoOFSEtendu());
		}
		{
			final HistoriqueCommune domicile = domiciles.get(6);
			assertEquals(departLeSentier.getOneDayAfter(), domicile.getDateDebut());
			assertNull(domicile.getDateFin());
			assertNull(domicile.getCommune());
		}
	}

	/**
	 * C'est le cas du marié avec un non-habitant RCPers : la relation est bien renvoyée par le serviceCivil
	 * avec un numéro d'individu pour le conjoint, mais le GetPerson ne renvoie personne pour ce conjoint...
	 */
	@Test
	public void testIndividuAvecRelationVersConjointQueLeServiceNeRenvoiePas() throws Exception {

		final long noIndividuPresent = 1564236767L;
		final long noIndividuConjointAbsent = 3254625426L;
		final RegDate dateMariage = date(1989, 4, 9);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu present = addIndividu(noIndividuPresent, null, "Flintstone", "Henry", true);
				final MockIndividu absent = addIndividu(noIndividuConjointAbsent, null, "Flintstone", "Missing", false);
				absent.setNonHabitantNonRenvoye(true);
				marieIndividus(present, absent, dateMariage);
			}
		});

		// on connait le numéro d'individu du conjoint
		final Long noIndividuConjoint = serviceCivil.getNumeroIndividuConjoint(noIndividuPresent, null);
		assertNotNull(noIndividuConjoint);
		assertEquals((long) noIndividuConjoint, noIndividuConjointAbsent);

		// mais le serviceCivil civil ne le renvoie pas
		final Individu individuNonTrouve = serviceCivil.getIndividu(noIndividuConjointAbsent, null);
		assertNull(individuNonTrouve);

		// ni là non plus...
		final Individu conjoint = serviceCivil.getConjoint(noIndividuPresent, null);
		assertNull(conjoint);
	}
}
