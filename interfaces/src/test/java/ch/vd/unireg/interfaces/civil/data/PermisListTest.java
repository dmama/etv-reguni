package ch.vd.unireg.interfaces.civil.data;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class PermisListTest extends WithoutSpringTest {

	@Test
	public void testGetPermisActifAucunPermis() {

		// Aucun permis
		final PermisList list = new PermisListImpl();

		final Permis permis1 = list.getPermisActif(null);
		assertNull(permis1);

		final Permis permis2 = list.getPermisActif(RegDate.get(2000, 1, 13));
		assertNull(permis2);
	}

	@Test
	public void testGetPermisActifUnSeulPermisOuvert() {

		// Un seul permis = [1.3.1960..fin-des-temps]
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(RegDate.get(1960, 3, 1));
		final PermisList list = new PermisListImpl(Collections.<Permis>singletonList(permis1));

		final Permis p1 = list.getPermisActif(null);
		assertNotNull(p1);
		assertSame(permis1, p1);

		final Permis p2 = list.getPermisActif(RegDate.get(1903, 1, 13));
		assertNull(p2);

		final Permis p3 = list.getPermisActif(RegDate.get(1989, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);
	}

	@Test
	public void testGetPermisActifUnSeulPermisFerme() {

		// Un seul permis = [1.3.1960..1.1.1990]
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(RegDate.get(1960, 3, 1));
		permis1.setDateFinValidite(RegDate.get(1990, 1, 1));
		final PermisList list = new PermisListImpl(Collections.<Permis>singletonList(permis1));

		final Permis p1 = list.getPermisActif(null);
		assertNull(p1);

		final Permis p2 = list.getPermisActif(RegDate.get(1903, 1, 13));
		assertNull(p2);

		final Permis p3 = list.getPermisActif(RegDate.get(1989, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);
	}

	//Selon SIFISC-16109 l'orde des permis est garantie du plus ancien au plus récent.
	@Test
	public void testGetPermisActifListDesordonnees() {

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
		//Selon l'ordre RcPers
		final PermisList list = new PermisListImpl(Arrays.<Permis>asList(permis1, permis3, permis2));

		final Permis p1 = list.getPermisActif(null);
		assertNotNull(p1);
		assertSame(permis2, p1);

		final Permis p2 = list.getPermisActif(RegDate.get(1903, 1, 13));
		assertNull(p2);

		final Permis p3 = list.getPermisActif(RegDate.get(1940, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);

		final Permis p4 = list.getPermisActif(RegDate.get(1975, 12, 25));
		assertNotNull(p4);
		assertSame(permis3, p4);

		final Permis p5 = list.getPermisActif(RegDate.get(1988, 1, 2));
		assertNotNull(p5);
		assertSame(permis2, p5);
	}

	@Test
	public void testGetPermisActifAdresseDebutNulle() {

		// Un seul permis avec adresse de début nulle = [null..fin-des-temps]
		// (ceci est un cas réel existant sur le host)
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(null);
		final PermisList list = new PermisListImpl(Collections.<Permis>singletonList(permis1));

		Permis p1 = list.getPermisActif(null);
		assertNotNull(p1);
		assertSame(permis1, p1);

		Permis p2 = list.getPermisActif(RegDate.get(1903, 1, 13));
		assertNotNull(p2);
		assertSame(permis1, p2);

		Permis p3 = list.getPermisActif(RegDate.get(1989, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);
	}

	@Test
	public void testGetPermisActifSiVieuxPermisSansDateFin() throws Exception {
		final MockPermis vieux = new MockPermis(date(2011, 1, 1), null, null, TypePermis.SEJOUR);
		final MockPermis nouveau = new MockPermis(date(2013, 4, 1), date(2018, 3, 31), null, TypePermis.ETABLISSEMENT);

		final PermisList list = new PermisListImpl(Arrays.<Permis>asList(vieux, nouveau));
		final Permis actif = list.getPermisActif(null);
		assertNotNull(actif);
		assertSame(nouveau, actif);

		final Permis actif2013 = list.getPermisActif(date(2013, 4, 4));
		assertNotNull(actif2013);
		assertSame(nouveau, actif2013);

		final Permis actif2012 = list.getPermisActif(date(2012, 5, 12));
		assertNotNull(actif2012);
		assertSame(vieux, actif2012);

		final Permis actif2010 = list.getPermisActif(date(2010, 1, 1));
		assertNull(actif2010);
	}
}
