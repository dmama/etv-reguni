package ch.vd.uniregctb.documentfiscal;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;

public class ImpressionDemandeDegrevementICIHelperTest extends WithoutSpringTest {

	@Test
	public void testGetTypeImmeuble() throws Exception {
		Assert.assertEquals("PPE", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new ProprieteParEtageRF()));
		Assert.assertEquals("DDP", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new DroitDistinctEtPermanentRF()));
		Assert.assertEquals("Mine", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new MineRF()));
		Assert.assertEquals("Bien-fonds", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new BienFondRF()));
		Assert.assertEquals("Copropriété", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new PartCoproprieteRF()));
	}

	@Test
	public void testGetNatureImmeubleSansSurfaceAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setSurfacesAuSol(Collections.emptySet());
		Assert.assertNull(ImpressionDemandeDegrevementICIHelperImpl.getNatureImmeuble(immeuble, RegDate.get()));
	}

	@Test
	public void testGetNatureImmeubleAvecUneSurfaceAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setImmeuble(immeuble);
		surface.setSurface(42);
		surface.setType("Jardin, route");
		immeuble.setSurfacesAuSol(Collections.singleton(surface));

		Assert.assertEquals("Jardin, route", ImpressionDemandeDegrevementICIHelperImpl.getNatureImmeuble(immeuble, RegDate.get()));
	}

	@Test
	public void testGetNatureImmeubleAvecDeuxSurfacesAuSolDeMemeNature() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setSurfacesAuSol(new HashSet<>());
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(42);
			surface.setType("Jardin, route");
			immeuble.getSurfacesAuSol().add(surface);
		}
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(12);
			surface.setType("Jardin, route");           // la même
			immeuble.getSurfacesAuSol().add(surface);
		}

		Assert.assertEquals("Jardin, route", ImpressionDemandeDegrevementICIHelperImpl.getNatureImmeuble(immeuble, RegDate.get()));
	}

	@Test
	public void testGetNatureImmeubleAvecDeuxSurfacesAuSolDeNaturesDifferentes() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setSurfacesAuSol(new HashSet<>());
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(42);
			surface.setType("Jardin, route");
			immeuble.getSurfacesAuSol().add(surface);
		}
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(12);
			surface.setType("Piscine");
			immeuble.getSurfacesAuSol().add(surface);
		}

		Assert.assertEquals("Jardin, route / Piscine", ImpressionDemandeDegrevementICIHelperImpl.getNatureImmeuble(immeuble, RegDate.get()));
	}

	@Test
	public void testGetNatureImmeubleAvecPlusieursSurfacesEtGrandeNatureResultante() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setSurfacesAuSol(new HashSet<>());
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(42);
			surface.setType("Jardin, route");
			immeuble.getSurfacesAuSol().add(surface);
		}
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(12);
			surface.setType("Piscine");
			immeuble.getSurfacesAuSol().add(surface);
		}
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(25);
			surface.setType("Chemin de ronde");
			immeuble.getSurfacesAuSol().add(surface);
		}
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(1000);
			surface.setType("Terrain de foot");
			immeuble.getSurfacesAuSol().add(surface);
		}
		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(10000);
			surface.setType("Bâtiment militaire à vocation industrielle");
			immeuble.getSurfacesAuSol().add(surface);
		}

		// on ne prend que les natures qui ne font pas dépasser la longueur maximale
		// dans l'ordre de leur surface décroissante
		Assert.assertEquals("Bâtiment militaire à vocation industrielle / Terrain de foot / Jardin, route / Chemin de ronde", ImpressionDemandeDegrevementICIHelperImpl.getNatureImmeuble(immeuble, RegDate.get()));
	}

	@Test
	public void testGetNatureImmeubleAvecUneSurfaceAuSolDeGrandeNature() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setImmeuble(immeuble);
		surface.setSurface(42);
		surface.setType("Jardin, route, chemin, petit bois, bâtiment industriel à vocation militaire, terrain d'aviation, piscine, terrain de foot");
		immeuble.setSurfacesAuSol(Collections.singleton(surface));

		Assert.assertEquals("Jardin, route, chemin, petit bois, bâtiment industriel à vocation militaire, terrain d'aviation, ...", ImpressionDemandeDegrevementICIHelperImpl.getNatureImmeuble(immeuble, RegDate.get()));
	}
}
