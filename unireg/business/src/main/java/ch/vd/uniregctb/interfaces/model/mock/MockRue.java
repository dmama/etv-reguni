package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class MockRue implements Rue {

	public static class Lausanne {
		public static final MockRue RouteMaisonNeuve = new MockRue(MockLocalite.Lausanne, "Route de la Maison Neuve", MockLocalite.Lausanne.getNoOrdre(), 141554);
		public static final MockRue AvenueDeMarcelin = new MockRue(MockLocalite.Lausanne, "Av de Marcelin", MockLocalite.Lausanne.getNoOrdre(), 9832);
		public static final MockRue AvenueDeBeaulieu = new MockRue(MockLocalite.Lausanne, "Av de Beaulieu", MockLocalite.Lausanne.getNoOrdre(), 76437);
	}

	public static class CossonayVille {
		public static final MockRue AvenueDuFuniculaire = new MockRue(MockLocalite.CossonayVille, "Avenue du Funiculaire", MockLocalite.CossonayVille.getNoOrdre(), 32296);
		public static final MockRue CheminDeRiondmorcel = new MockRue(MockLocalite.CossonayVille, "Chemin de Riondmorcel", MockLocalite.CossonayVille.getNoOrdre(), 83404);
	}

	public static class LesClees {
		public static final MockRue ChampDuRaffour = new MockRue(MockLocalite.LesClees, "Champ du Raffour", MockLocalite.LesClees.getNoOrdre(), 184328);
	}

	public static class Bex {
		public static final MockRue RouteDuBoet = new MockRue(MockLocalite.Bex, "Route du Boêt", MockLocalite.Bex.getNoOrdre(), 35365);
	}

	public static class Romainmotier {
		public static final MockRue CheminDuCochet = new MockRue(MockLocalite.Romainmotier, "Chemin du Cochet", MockLocalite.Romainmotier.getNoOrdre(), 262521);
	}

	public static class VillarsSousYens {
		public static final MockRue CheminDuCollege = new MockRue(MockLocalite.VillarsSousYens, "Chemin du college", MockLocalite.VillarsSousYens.getNoOrdre(), 195804);
		public static final MockRue RuelleDuCarroz = new MockRue(MockLocalite.VillarsSousYens, "Ruelle du Carroz", MockLocalite.VillarsSousYens.getNoOrdre(), 195803);
		public static final MockRue RouteDeStPrex = new MockRue(MockLocalite.VillarsSousYens, "Route de St-Prex", MockLocalite.VillarsSousYens.getNoOrdre(), 195812);
	}

	public static class Neuchatel {
		public static final MockRue RueDesBeauxArts = new MockRue(MockLocalite.Neuchatel, "Rue des Beaux-Arts", MockLocalite.Neuchatel.getNoOrdre(), 42534);
	}

	public static class Orbe {
		public static final MockRue RueDavall = new MockRue(MockLocalite.Orbe, "Rue de la tombe", MockLocalite.Orbe.getNoOrdre(), 32540);
		public static final MockRue RueDuMoulinet = new MockRue(MockLocalite.Orbe, "Rue du moulinet", MockLocalite.Orbe.getNoOrdre(), 32562);
		public static final MockRue GrandRue = new MockRue(MockLocalite.Orbe, "Grand-Rue", MockLocalite.Orbe.getNoOrdre(), 32549);
	}

	public static class Vallorbe {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Vallorbe, "Grand-Rue", MockLocalite.Vallorbe.getNoOrdre(), 32397);
	}

	public static class Zurich {
		public static final MockRue VoltaStrasse = new MockRue(MockLocalite.Zurich, "Volta Strasse", MockLocalite.Zurich.getNoOrdre(), 16);
		public static final MockRue GloriaStrasse = new MockRue(MockLocalite.Zurich, "Gloria Strasse", MockLocalite.Zurich.getNoOrdre(),12);
	}

	public static class LeSentier {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Zurich, "Volta Strasse", MockLocalite.LeSentier.getNoOrdre(), 16);

	}

	public static class Enney {
		public static final MockRue chemin = new MockRue(MockLocalite.Enney, "Chemin d'Afflon", MockLocalite.Enney.getNoOrdre(),11);

	}

	public static class Chamblon {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Chamblon, "Rue des Uttins", MockLocalite.Chamblon.getNoOrdre(), 23);

	}


	public static class Lisles {
		public static final MockRue rueMoulin = new MockRue(MockLocalite.Chamblon, "Rue du moulin 10", MockLocalite.Chamblon.getNoOrdre(), 23);

	}
	/**
	 * Permet de forcer le chargement des Mock dans le DefaultMockService
	 * Il faut ajouter les nouveaux Mock dans cette methode
	 * Il n'est nécessaire d'appeler qu'une seule rue par Localite
	 */
	@SuppressWarnings("unused")
	public static void forceLoad() {
		MockRue r;
		r = Lausanne.AvenueDeBeaulieu;
		r = CossonayVille.AvenueDuFuniculaire;
		r = LesClees.ChampDuRaffour;
		r = Bex.RouteDuBoet;
		r = Romainmotier.CheminDuCochet;
		r = VillarsSousYens.CheminDuCollege;
		r = Neuchatel.RueDesBeauxArts;
		r = Orbe.RueDavall;
	}


	private final MockLocalite localite;
	private String designationCourrier;
	private Integer noLocalite;
	private Integer noRue;

	public MockRue(MockLocalite localite, String designationCourrier, Integer noLocalite, Integer noRue) {
		this.designationCourrier = designationCourrier;
		this.noLocalite = noLocalite;
		this.noRue = noRue;
		this.localite = localite;

		DefaultMockServiceInfrastructureService.addRue(this);
	}

	public String getDesignationCourrier() {
		return designationCourrier;
	}

	public void setDesignationCourrier(String designationCourrier) {
		this.designationCourrier = designationCourrier;
	}

	public Integer getNoLocalite() {
		return noLocalite;
	}

	public void setNoLocalite(Integer noLocalite) {
		this.noLocalite = noLocalite;
	}

	public Integer getNoRue() {
		return noRue;
	}

	public void setNoRue(Integer noRue) {
		this.noRue = noRue;
	}

	public MockLocalite getLocalite() {
		return localite;
	}

}
