package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.vd.capitastra.grundstueck.GrundstueckZuGebaeude;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(UniregJUnit4Runner.class)
public class ImplantationRFHelperTest {

	@Test
	public void testDataEqualsListNullity() throws Exception {

		assertTrue(ImplantationRFHelper.dataEquals((List<ImplantationRF>) null, null));
		assertTrue(ImplantationRFHelper.dataEquals(Collections.emptyList(), null));
		assertTrue(ImplantationRFHelper.dataEquals(null, Collections.emptyList()));
		assertTrue(ImplantationRFHelper.dataEquals(Collections.emptyList(), Collections.emptyList()));

		assertFalse(ImplantationRFHelper.dataEquals(null, Collections.singletonList(new GrundstueckZuGebaeude())));
		assertFalse(ImplantationRFHelper.dataEquals(Collections.singletonList(new ImplantationRF()), null));
	}

	@Test
	public void testDataEqualsListDifferentSizes() throws Exception {
		assertFalse(ImplantationRFHelper.dataEquals(Collections.singletonList(new ImplantationRF()),
		                                            Arrays.asList(new GrundstueckZuGebaeude(), new GrundstueckZuGebaeude())));
	}

	@Test
	public void testDataEqualsList() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("7388991");

		final ImmeubleRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("0e0912a");

		final ImmeubleRF immeuble3 = new BienFondRF();
		immeuble3.setIdRF("482e2ea");

		final ImplantationRF implantation1 = new ImplantationRF();
		implantation1.setImmeuble(immeuble1);
		implantation1.setSurface(234);

		final ImplantationRF implantation2 = new ImplantationRF();
		implantation2.setImmeuble(immeuble2);
		implantation2.setSurface(532);

		final ImplantationRF implantation3 = new ImplantationRF();
		implantation3.setImmeuble(immeuble3);
		implantation3.setSurface(24);

		final GrundstueckZuGebaeude gzg1 = new GrundstueckZuGebaeude();
		gzg1.setGrundstueckIDREF("7388991");
		gzg1.setAbschnittFlaeche(234);

		final GrundstueckZuGebaeude gzg2 = new GrundstueckZuGebaeude();
		gzg2.setGrundstueckIDREF("0e0912a");
		gzg2.setAbschnittFlaeche(532);

		final GrundstueckZuGebaeude gzg3 = new GrundstueckZuGebaeude();
		gzg3.setGrundstueckIDREF("482e2ea");
		gzg3.setAbschnittFlaeche(24);

		assertTrue(ImplantationRFHelper.dataEquals(newList(implantation1, implantation2, implantation3), newList(gzg1, gzg2, gzg3)));
		assertTrue(ImplantationRFHelper.dataEquals(newList(implantation1, implantation2, implantation3), newList(gzg3, gzg2, gzg1)));
		assertFalse(ImplantationRFHelper.dataEquals(newList(implantation1, implantation2), newList(gzg2, gzg3)));
		assertFalse(ImplantationRFHelper.dataEquals(newList(implantation1, implantation3), newList(gzg2, gzg1)));
	}

	@Test
	public void testDataEqualsNullity() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("7388991");

		final ImplantationRF implantation1 = new ImplantationRF();
		implantation1.setImmeuble(immeuble1);
		implantation1.setSurface(234);

		final GrundstueckZuGebaeude gzg1 = new GrundstueckZuGebaeude();
		gzg1.setGrundstueckIDREF("7388991");
		gzg1.setAbschnittFlaeche(234);

		assertTrue(ImplantationRFHelper.dataEquals(null, (ImplantationRF) null));
		assertTrue(ImplantationRFHelper.dataEquals(null, (GrundstueckZuGebaeude) null));
		assertFalse(ImplantationRFHelper.dataEquals(implantation1, (ImplantationRF) null));
		assertFalse(ImplantationRFHelper.dataEquals(implantation1, (GrundstueckZuGebaeude) null));
		assertFalse(ImplantationRFHelper.dataEquals(null, gzg1));
	}

	@Test
	public void testDataEquals() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("7388991");

		final ImmeubleRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("0e0912a");

		final ImplantationRF implantation1 = new ImplantationRF();
		implantation1.setImmeuble(immeuble1);
		implantation1.setSurface(234);

		final ImplantationRF implantation2 = new ImplantationRF();
		implantation2.setImmeuble(immeuble2);
		implantation2.setSurface(532);

		final GrundstueckZuGebaeude gzg1 = new GrundstueckZuGebaeude();
		gzg1.setGrundstueckIDREF("7388991");
		gzg1.setAbschnittFlaeche(234);

		final GrundstueckZuGebaeude gzg2 = new GrundstueckZuGebaeude();
		gzg2.setGrundstueckIDREF("0e0912a");
		gzg2.setAbschnittFlaeche(532);

		assertTrue(ImplantationRFHelper.dataEquals(implantation1, implantation1));
		assertTrue(ImplantationRFHelper.dataEquals(implantation1, gzg1));
		assertFalse(ImplantationRFHelper.dataEquals(implantation1, implantation2));
		assertFalse(ImplantationRFHelper.dataEquals(implantation1, gzg2));
	}

	@NotNull
	@SafeVarargs
	private static <T> List<T> newList(T... o) {
		return Arrays.asList(o);
	}
}