package ch.vd.uniregctb.documentfiscal;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;

public class DemandeDegrevementICIHelperTest {

	@Test
	public void testGetNatureImmeubleSansImplantationNiSurfaceAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setSurfacesAuSol(Collections.emptySet());
		immeuble.setImplantations(Collections.emptySet());
		Assert.assertNull(DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecUneSurfaceAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setImmeuble(immeuble);
		surface.setSurface(42);
		surface.setType("Jardin, route");
		immeuble.setSurfacesAuSol(Collections.singleton(surface));
		immeuble.setImplantations(Collections.emptySet());

		Assert.assertEquals("Jardin, route", DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecUneImplantation() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();

		final DescriptionBatimentRF description = new DescriptionBatimentRF();
		description.setSurface(10000);
		description.setType("Bâtiment industriel");

		final BatimentRF batiment = new BatimentRF();
		batiment.setMasterIdRF("4r73854tifgbsjg");
		batiment.setDescriptions(Collections.singleton(description));
		description.setBatiment(batiment);

		final ImplantationRF implantationRF = new ImplantationRF();
		implantationRF.setImmeuble(immeuble);
		implantationRF.setSurface(10000);
		implantationRF.setBatiment(batiment);
		immeuble.setImplantations(Collections.singleton(implantationRF));

		immeuble.setSurfacesAuSol(Collections.emptySet());
		immeuble.setImplantations(Collections.singleton(implantationRF));

		Assert.assertEquals("Bâtiment industriel", DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecDeuxSurfacesAuSolDeMemeNature() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setImplantations(Collections.emptySet());
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

		Assert.assertEquals("Jardin, route", DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecDeuxSurfacesAuSolDeNaturesDifferentes() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setImplantations(Collections.emptySet());
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

		Assert.assertEquals("Jardin, route / Piscine", DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecImplantationsEtSurfacesAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setSurfacesAuSol(new HashSet<>());
		immeuble.setImplantations(new HashSet<>());

		{
			final SurfaceAuSolRF surface = new SurfaceAuSolRF();
			surface.setImmeuble(immeuble);
			surface.setSurface(750);                        // priorité au bâtiment, même si la surface est plus grande
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
			final DescriptionBatimentRF description = new DescriptionBatimentRF();
			description.setSurface(null);
			description.setType("Bâtiment industriel");

			final BatimentRF batiment = new BatimentRF();
			batiment.setMasterIdRF("4r73854tifgbsjg");
			batiment.setDescriptions(Collections.singleton(description));
			description.setBatiment(batiment);

			final ImplantationRF implantationRF = new ImplantationRF();
			implantationRF.setImmeuble(immeuble);
			implantationRF.setSurface(10000);
			implantationRF.setBatiment(batiment);
			immeuble.getImplantations().add(implantationRF);
		}
		{
			final DescriptionBatimentRF description = new DescriptionBatimentRF();
			description.setSurface(500);
			description.setType("Bâtiment commercial");

			final BatimentRF batiment = new BatimentRF();
			batiment.setMasterIdRF("4r73854tifgbsjg");
			batiment.setDescriptions(Collections.singleton(description));
			description.setBatiment(batiment);

			final ImplantationRF implantationRF = new ImplantationRF();
			implantationRF.setImmeuble(immeuble);
			implantationRF.setSurface(null);
			implantationRF.setBatiment(batiment);
			immeuble.getImplantations().add(implantationRF);
		}

		Assert.assertEquals("Bâtiment industriel / Bâtiment commercial / Jardin, route / Piscine", DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecPlusieursSurfacesEtGrandeNatureResultante() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setImplantations(Collections.emptySet());
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
		Assert.assertEquals("Bâtiment militaire à vocation industrielle / Terrain de foot / Jardin, route / Chemin de ronde", DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecUneSurfaceAuSolDeGrandeNature() throws Exception {
		final ImmeubleRF immeuble = new BienFondRF();
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setImmeuble(immeuble);
		surface.setSurface(42);
		surface.setType("Jardin, route, chemin, petit bois, bâtiment industriel à vocation militaire, terrain d'aviation, piscine, terrain de foot");
		immeuble.setSurfacesAuSol(Collections.singleton(surface));
		immeuble.setImplantations(Collections.emptySet());

		Assert.assertEquals("Jardin, route, chemin, petit bois, bâtiment industriel à vocation militaire, terrain d'aviation, ...", DemandeDegrevementICIHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}
}
