package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class MockRue implements Rue {

	public static class Lausanne {
		public static final MockRue RouteMaisonNeuve = new MockRue(MockLocalite.Lausanne, "Route de la Maison Neuve", 141554);
		public static final MockRue AvenueDeMarcelin = new MockRue(MockLocalite.Lausanne, "Av de Marcelin", 9832);
		public static final MockRue AvenueDeBeaulieu = new MockRue(MockLocalite.Lausanne, "Av de Beaulieu", 76437);
	}

	public static class CossonayVille {
		public static final MockRue AvenueDuFuniculaire = new MockRue(MockLocalite.CossonayVille, "Avenue du Funiculaire", 32296);
		public static final MockRue CheminDeRiondmorcel = new MockRue(MockLocalite.CossonayVille, "Chemin de Riondmorcel", 83404);
	}

	public static class LesClees {
		public static final MockRue ChampDuRaffour = new MockRue(MockLocalite.LesClees, "Champ du Raffour", 184328);
	}

	public static class Bex {
		public static final MockRue RouteDuBoet = new MockRue(MockLocalite.Bex, "Route du Boêt", 35365);
	}

	public static class Romainmotier {
		public static final MockRue CheminDuCochet = new MockRue(MockLocalite.Romainmotier, "Chemin du Cochet", 262521);
	}

	public static class VillarsSousYens {
		public static final MockRue CheminDuCollege = new MockRue(MockLocalite.VillarsSousYens, "Chemin du college", 195804);
		public static final MockRue RuelleDuCarroz = new MockRue(MockLocalite.VillarsSousYens, "Ruelle du Carroz", 195803);
		public static final MockRue RouteDeStPrex = new MockRue(MockLocalite.VillarsSousYens, "Route de St-Prex", 195812);
	}

	public static class Neuchatel {
		public static final MockRue RueDesBeauxArts = new MockRue(MockLocalite.Neuchatel, "Rue des Beaux-Arts", 42534);
	}

	public static class Orbe {
		public static final MockRue RueDavall = new MockRue(MockLocalite.Orbe, "Rue de la tombe", 32540);
		public static final MockRue RueDuMoulinet = new MockRue(MockLocalite.Orbe, "Rue du moulinet", 32562);
		public static final MockRue GrandRue = new MockRue(MockLocalite.Orbe, "Grand-Rue", 32549);
	}

	public static class Vallorbe {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Vallorbe, "Grand-Rue", 32397);
	}

	public static class Zurich {
		public static final MockRue VoltaStrasse = new MockRue(MockLocalite.Zurich, "Volta Strasse", 16);
		public static final MockRue GloriaStrasse = new MockRue(MockLocalite.Zurich, "Gloria Strasse", 12);
	}

	public static class LeSentier {
		public static final MockRue GrandRue = new MockRue(MockLocalite.LeSentier, "Grande-Rue", 32468);
	}

	public static class Enney {
		public static final MockRue chemin = new MockRue(MockLocalite.Enney, "Chemin d'Afflon", 11);
	}

	public static class Chamblon {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Chamblon, "Rue des Uttins", 23);
	}

	public static class LIsle {
		public static final MockRue rueMoulin = new MockRue(MockLocalite.LIsle, "Rue du moulin", 338528);
	}

	private final MockLocalite localite;
	private String designationCourrier;
	private Integer noRue;

	public MockRue(MockLocalite localite, String designationCourrier, Integer noRue) {
		this.designationCourrier = designationCourrier;
		this.noRue = noRue;
		this.localite = localite;

		DefaultMockServiceInfrastructureService.addRue(this);
	}

	public String getDesignationCourrier() {
		return designationCourrier;
	}

	public Integer getNoLocalite() {
		return localite.getNoOrdre();
	}

	public Integer getNoRue() {
		return noRue;
	}

	public MockLocalite getLocalite() {
		return localite;
	}
}
