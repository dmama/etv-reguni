package ch.vd.uniregctb.interfaces.service;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.mock.*;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;

public class ServiceCivilServiceImplTest extends WithoutSpringTest {

	private final ProxyServiceCivil service = new ProxyServiceCivil();

	@Override
	public void onTearDown() throws Exception {
		service.tearDown();
		super.onTearDown();
	}

	@Test
	public void testGetEtatCivilActifAucunEtatCivil() {

		// Aucun état civil
		final long noIndividu = 1;

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
				momo.setEtatsCivils(etatsCivils);
			}
		});

		EtatCivil etatCivil1 = service.getEtatCivilActif(noIndividu, null);
		assertNull(etatCivil1);
		EtatCivil etatCivil2 = service.getEtatCivilActif(noIndividu, RegDate.get(2000, 01, 13));
		assertNull(etatCivil2);

	}

	@Test
	public void testGetEtatCivilActifUnSeulEtatCivil() {

		final long noIndividu = 1;

		// Un seul état civil = [1.3.1960..fin-des-temps]
		final MockEtatCivil ec1 = new MockEtatCivil();
		ec1.setDateDebutValidite(RegDate.get(1960, 3, 1));

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
				etatsCivils.add(ec1);
				momo.setEtatsCivils(etatsCivils);
			}
		});

		EtatCivil etatCivil1 = service.getEtatCivilActif(noIndividu, null);
		assertNotNull(etatCivil1);
		assertSame(ec1, etatCivil1);

		EtatCivil etatCivil2 = service.getEtatCivilActif(noIndividu, RegDate.get(1903, 01, 13));
		assertNull(etatCivil2);

		EtatCivil etatCivil3 = service.getEtatCivilActif(noIndividu, RegDate.get(1989, 11, 20));
		assertNotNull(etatCivil3);
		assertSame(ec1, etatCivil3);
	}

	@Test
	public void testGetEtatCivilActifListeEtatsCivils() {

		final long noIndividu = 1;

		// Une liste d'état civil (désordonnés) :
		// 1. -> [ 1.3.1930..fin-des-temps]
		// 2. -> [21.4.1985..fin-des-temps]
		// 3. -> [ 8.1.1973..fin-des-temps]
		final MockEtatCivil ec1 = new MockEtatCivil();
		final MockEtatCivil ec2 = new MockEtatCivil();
		final MockEtatCivil ec3 = new MockEtatCivil();
		ec1.setDateDebutValidite(RegDate.get(1930, 3, 1));
		ec1.setNoSequence(1);
		ec2.setDateDebutValidite(RegDate.get(1985, 4, 21));
		ec2.setNoSequence(3);
		ec3.setDateDebutValidite(RegDate.get(1973, 1, 8));
		ec3.setNoSequence(2);

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
				etatsCivils.add(ec1);
				etatsCivils.add(ec2);
				etatsCivils.add(ec3);
				momo.setEtatsCivils(etatsCivils);
			}
		});

		EtatCivil etatCivil1 = service.getEtatCivilActif(noIndividu, null);
		assertNotNull(etatCivil1);
		assertSame(ec2, etatCivil1);

		EtatCivil etatCivil2 = service.getEtatCivilActif(noIndividu, RegDate.get(1903, 01, 13));
		assertNull(etatCivil2);

		EtatCivil etatCivil3 = service.getEtatCivilActif(noIndividu, RegDate.get(1940, 11, 20));
		assertNotNull(etatCivil3);
		assertSame(ec1, etatCivil3);

		EtatCivil etatCivil4 = service.getEtatCivilActif(noIndividu, RegDate.get(1975, 12, 25));
		assertNotNull(etatCivil4);
		assertSame(ec3, etatCivil4);

		EtatCivil etatCivil5 = service.getEtatCivilActif(noIndividu, RegDate.get(1988, 1, 2));
		assertNotNull(etatCivil5);
		assertSame(ec2, etatCivil5);
	}

	@Test
	public void testGetEtatCivilActifAdresseDebutNulle() {

		final long noIndividu = 1;

		/*
		 * Un seul état civil avec adresse de début nulle = [null..fin-des-temps] (ceci est un cas réel existant sur le host)
		 */
		final MockEtatCivil ec1 = new MockEtatCivil();
		ec1.setDateDebutValidite(null);

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
				etatsCivils.add(ec1);
				momo.setEtatsCivils(etatsCivils);
			}
		});

		EtatCivil etatCivil1 = service.getEtatCivilActif(noIndividu, null);
		assertNotNull(etatCivil1);
		assertSame(ec1, etatCivil1);

		EtatCivil etatCivil2 = service.getEtatCivilActif(noIndividu, RegDate.get(1903, 01, 13));
		assertNotNull(etatCivil2);
		assertSame(ec1, etatCivil2);

		EtatCivil etatCivil3 = service.getEtatCivilActif(noIndividu, RegDate.get(1989, 11, 20));
		assertNotNull(etatCivil3);
		assertSame(ec1, etatCivil3);
	}

	@Test
	public void testGetPermisActifAucunPermis() {

		final long noIndividu = 1;

		// Aucun permis
		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
			}
		});

		final Permis permis1 = service.getPermisActif(noIndividu, null);
		assertNull(permis1);

		final Permis permis2 = service.getPermisActif(noIndividu, RegDate.get(2000, 01, 13));
		assertNull(permis2);
	}

	@Test
	public void testGetPermisActifUnSeulPermisOuvert() {

		final long noIndividu = 1;

		// Un seul permis = [1.3.1960..fin-des-temps]
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(RegDate.get(1960, 3, 1));

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<Permis> permis = new ArrayList<Permis>();
				permis.add(permis1);
				momo.setPermis(permis);
			}
		});

		final Permis p1 = service.getPermisActif(noIndividu, null);
		assertNotNull(p1);
		assertSame(permis1, p1);

		final Permis p2 = service.getPermisActif(noIndividu, RegDate.get(1903, 01, 13));
		assertNull(p2);

		final Permis p3 = service.getPermisActif(noIndividu, RegDate.get(1989, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);
	}

	@Test
	public void testGetPermisActifUnSeulPermisFerme() {

		final long noIndividu = 1;

		// Un seul permis = [1.3.1960..1.1.1990]
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(RegDate.get(1960, 3, 1));
		permis1.setDateFinValidite(RegDate.get(1990, 1, 1));

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<Permis> permis = new ArrayList<Permis>();
				permis.add(permis1);
				momo.setPermis(permis);
			}
		});

		final Permis p1 = service.getPermisActif(noIndividu, null);
		assertNull(p1);

		final Permis p2 = service.getPermisActif(noIndividu, RegDate.get(1903, 01, 13));
		assertNull(p2);

		final Permis p3 = service.getPermisActif(noIndividu, RegDate.get(1989, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);
	}

	@Test
	public void testGetPermisActifListDesordonnees() {

		final long noIndividu = 1;

		// Une liste de permis (désordonnés) :
		// 1. -> [ 1.3.1930..fin-des-temps]
		// 2. -> [21.4.1985..fin-des-temps]
		// 3. -> [ 8.1.1973..fin-des-temps]
		final MockPermis permis1 = new MockPermis();
		final MockPermis permis2 = new MockPermis();
		final MockPermis permis3 = new MockPermis();
		permis1.setDateDebutValidite(RegDate.get(1930, 3, 1));
		permis2.setDateDebutValidite(RegDate.get(1985, 4, 21));
		permis3.setDateDebutValidite(RegDate.get(1973, 1, 8));

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<Permis> permis = new ArrayList<Permis>();
				permis.add(permis1);
				permis.add(permis2);
				permis.add(permis3);
				momo.setPermis(permis);
			}
		});

		final Permis p1 = service.getPermisActif(noIndividu, null);
		assertNotNull(p1);
		assertSame(permis2, p1);

		final Permis p2 = service.getPermisActif(noIndividu, RegDate.get(1903, 01, 13));
		assertNull(p2);

		final Permis p3 = service.getPermisActif(noIndividu, RegDate.get(1940, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);

		final Permis p4 = service.getPermisActif(noIndividu, RegDate.get(1975, 12, 25));
		assertNotNull(p4);
		assertSame(permis3, p4);

		final Permis p5 = service.getPermisActif(noIndividu, RegDate.get(1988, 1, 2));
		assertNotNull(p5);
		assertSame(permis2, p5);
	}

	@Test
	public void testGetPermisActifAdresseDebutNulle() {

		final long noIndividu = 1;

		// Un seul permis avec adresse de début nulle = [null..fin-des-temps]
		// (ceci est un cas réel existant sur le host)
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(null);

		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				final ArrayList<Permis> permis = new ArrayList<Permis>();
				permis.add(permis1);
				momo.setPermis(permis);
			}
		});

		Permis p1 = service.getPermisActif(noIndividu, null);
		assertNotNull(p1);
		assertSame(permis1, p1);

		Permis p2 = service.getPermisActif(noIndividu, RegDate.get(1903, 01, 13));
		assertNotNull(p2);
		assertSame(permis1, p2);

		Permis p3 = service.getPermisActif(noIndividu, RegDate.get(1989, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);
	}

	/**
	 * Vérifie que getAdresses détecte bien des incohérences de données lorsque le mode stricte est activé.
	 */
	@Test
	public void testGetAdressesStrict() {

		final long noIndividu = 1;

		// crée un individu avec deux adresses principales actives en même temps
		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, RegDate.get(1980, 5, 12), RegDate.get(2003, 11, 28));
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, RegDate.get(2000, 1, 1), null);
			}
		});

		try {
			service.getAdresses(noIndividu, RegDate.get(2002, 7, 1), true);
			fail();
		}
		catch (DonneesCivilesException e) {
			assertEquals(String.format("Plus d'une adresse 'principale' détectée sur l'individu n°%d et pour l'année %d.", noIndividu, 2002), e.getMessage());
		}
	}

	/**
	 * Vérifie que getAdresses ignore bien des incohérences de données lorsque le mode stricte est désactivé.
	 */
	@Test
	public void testGetAdressesNonStrict() throws Exception {

		final long noIndividu = 1;

		// crée un individu avec deux adresses principales actives en même temps
		service.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, RegDate.get(1980, 5, 12), RegDate.get(2003, 11, 28));
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(2000, 1, 1), null);
			}
		});

		final AdressesCiviles adresses = service.getAdresses(noIndividu, RegDate.get(2002, 7, 1), false);
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
}
