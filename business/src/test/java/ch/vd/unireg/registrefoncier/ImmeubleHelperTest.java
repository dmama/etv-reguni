package ch.vd.unireg.registrefoncier;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

public class ImmeubleHelperTest {

	@Test
	public void testGetNatureImmeubleSansImplantationNiSurfaceAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setSurfacesAuSol(Collections.emptySet());
		immeuble.setImplantations(Collections.emptySet());
		Assert.assertNull(ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecUneSurfaceAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setImmeuble(immeuble);
		surface.setSurface(42);
		surface.setType("Jardin, route");
		immeuble.setSurfacesAuSol(Collections.singleton(surface));
		immeuble.setImplantations(Collections.emptySet());

		Assert.assertEquals("Jardin, route", ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecUneImplantation() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();

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

		Assert.assertEquals("Bâtiment industriel", ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecDeuxSurfacesAuSolDeMemeNature() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();
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

		Assert.assertEquals("Jardin, route", ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecDeuxSurfacesAuSolDeNaturesDifferentes() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();
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

		Assert.assertEquals("Jardin, route / Piscine", ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecImplantationsEtSurfacesAuSol() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();
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

		Assert.assertEquals("Bâtiment industriel / Bâtiment commercial / Jardin, route / Piscine", ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecPlusieursSurfacesEtGrandeNatureResultante() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();
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
		Assert.assertEquals("Bâtiment militaire à vocation industrielle / Terrain de foot / Jardin, route / Chemin de ronde", ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}

	@Test
	public void testGetNatureImmeubleAvecUneSurfaceAuSolDeGrandeNature() throws Exception {
		final ImmeubleRF immeuble = new BienFondsRF();
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setImmeuble(immeuble);
		surface.setSurface(42);
		surface.setType("Jardin, route, chemin, petit bois, bâtiment industriel à vocation militaire, terrain d'aviation, piscine, terrain de foot");
		immeuble.setSurfacesAuSol(Collections.singleton(surface));
		immeuble.setImplantations(Collections.emptySet());

		Assert.assertEquals("Jardin, route, chemin, petit bois, bâtiment industriel à vocation militaire, terrain d'aviation, ...", ImmeubleHelper.getNatureImmeuble(immeuble, RegDate.get(), 100));
	}
}
