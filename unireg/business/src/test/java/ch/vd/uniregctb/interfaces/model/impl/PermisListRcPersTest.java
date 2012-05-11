package ch.vd.uniregctb.interfaces.model.impl;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisListRcPers;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class PermisListRcPersTest extends WithoutSpringTest {

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPermisActifAucunPermis() {

		final long noIndividu = 1;

		// Aucun permis
		final PermisListRcPers list = new PermisListRcPers(noIndividu);

		final Permis permis1 = list.getPermisActif(null);
		assertNull(permis1);

		final Permis permis2 = list.getPermisActif(RegDate.get(2000, 1, 13));
		assertNull(permis2);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPermisActifUnSeulPermisOuvert() {

		final long noIndividu = 1;

		// Un seul permis = [1.3.1960..fin-des-temps]
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(RegDate.get(1960, 3, 1));
		final PermisListRcPers list = new PermisListRcPers(noIndividu, Arrays.<Permis>asList(permis1));

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
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPermisActifUnSeulPermisFerme() {

		final long noIndividu = 1;

		// Un seul permis = [1.3.1960..1.1.1990]
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(RegDate.get(1960, 3, 1));
		permis1.setDateFinValidite(RegDate.get(1990, 1, 1));
		final PermisListRcPers list = new PermisListRcPers(noIndividu, Arrays.<Permis>asList(permis1));

		final Permis p1 = list.getPermisActif(null);
		assertNull(p1);

		final Permis p2 = list.getPermisActif(RegDate.get(1903, 1, 13));
		assertNull(p2);

		final Permis p3 = list.getPermisActif(RegDate.get(1989, 11, 20));
		assertNotNull(p3);
		assertSame(permis1, p3);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final PermisListRcPers list = new PermisListRcPers(noIndividu, Arrays.<Permis>asList(permis1, permis2, permis3));

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
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPermisActifAdresseDebutNulle() {

		final long noIndividu = 1;

		// Un seul permis avec adresse de début nulle = [null..fin-des-temps]
		// (ceci est un cas réel existant sur le host)
		final MockPermis permis1 = new MockPermis();
		permis1.setDateDebutValidite(null);
		final PermisListRcPers list = new PermisListRcPers(noIndividu, Arrays.<Permis>asList(permis1));

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
}
