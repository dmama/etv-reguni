package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class MockRue implements Rue {

	public static class Lausanne {
		public static final MockRue RouteMaisonNeuve = new MockRue(MockLocalite.Lausanne, "Route de la Maison Neuve", 141554);
		public static final MockRue AvenueDeMarcelin = new MockRue(MockLocalite.Lausanne, "Av de Marcelin", 9832);
		public static final MockRue AvenueDeBeaulieu = new MockRue(MockLocalite.Lausanne, "Av de Beaulieu", 76437);
		public static final MockRue BoulevardGrancy = new MockRue(MockLocalite.Lausanne, "Boulevard de Grancy", 30581);
		public static final MockRue CheminDeMornex = new MockRue(MockLocalite.Lausanne, "Chemin de Mornex", 30350);
		public static final MockRue CheminPrazBerthoud = new MockRue(MockLocalite.Lausanne, "Chemin de Praz-Berthoud", 30933);
		public static final MockRue PlaceSaintFrancois = new MockRue(MockLocalite.Lausanne, "Place Saint-François", 30370);
		public static final MockRue AvenueDeLaGare = new MockRue(MockLocalite.Lausanne, "Avenue de la Gare", 30317);
		public static final MockRue CheminMessidor = new MockRue(MockLocalite.Lausanne, "Chemin Messidor", 30593);
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

	public static class Renens {
		public static final MockRue QuatorzeAvril = new MockRue(MockLocalite.Renens, "Avenue du 14 avril", 31093);
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

	public static class GrangesMarnand {
		public static final MockRue SousLeBois = new MockRue(MockLocalite.GrangesMarnand, "Chemin Sous le Bois", 127968);
		public static final MockRue RueDeVerdairuz = new MockRue(MockLocalite.GrangesMarnand, "Rue de Verdairuz", 127970);
	}

	public static class Bussigny {
		public static final MockRue RueDeLIndustrie = new MockRue(MockLocalite.Bussigny, "Rue de l'Industrie", 31444);
	}

	public static class Vevey {
		public static final MockRue RueDesMoulins = new MockRue(MockLocalite.Vevey, "Rue des Moulins", 34262);
	}

	public static class Echallens {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Echallens, "Grand Rue", 31560);
	}

	public static class Lonay {
		public static final MockRue CheminDuRechoz = new MockRue(MockLocalite.Lonay, "Chemin de Réchoz", 99548);
	}

	public static class Geneve {
		public static final MockRue AvenueGuiseppeMotta = new MockRue(MockLocalite.GeneveSecteurDist, "Avenue Guiseppe-Motta", 46491);
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
