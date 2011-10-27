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
		public static final MockRue AvenueDesBergieres = new MockRue(MockLocalite.Lausanne, "Avenue Bergières", 30389);
	}


	public static class Aubonne {
		public static final MockRue CheminCurzilles = new MockRue(MockLocalite.Aubonne, "Chemin des Curzilles", 88450);
		public static final MockRue RueTrevelin = new MockRue(MockLocalite.Aubonne, "Rue de Trévelin", 884503);
	}
	public static class Prilly {
		public static final MockRue RueDesMetiers = new MockRue(MockLocalite.Prilly, "Rue des Métiers", 78450);
		public static final MockRue CheminDeLaPossession = new MockRue(MockLocalite.Prilly, "Chemin de la Possession", 30796);
		public static final MockRue SentierFleurDeLys = new MockRue(MockLocalite.Prilly, "Sentier de la Fleur-de-Lys", 81681);
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
		public static final MockRue chemin = new MockRue(MockLocalite.Enney, "Chemin d'Afflon", 334842);
	}

	public static class Chamblon {
		public static final MockRue RueDesUttins = new MockRue(MockLocalite.Chamblon, "Rue des Uttins", 198539);
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

	public static class Moudon {
		public static final MockRue LeBourg = new MockRue(MockLocalite.Moudon, "Rue du Bourg", 33050);
	}

	public static class Pully {
		public static final MockRue CheminDesRoches = new MockRue(MockLocalite.Pully, "Chemin des Roches", 30887);
	}

	// Quelques rues de communes fusionnées au 1er juillet 2011
	public static class Villette {
		public static final MockRue RouteDeLausanne = new MockRue(MockLocalite.Villette, "Route de Lausanne", 328265);
		public static final MockRue CheminDeCreuxBechet = new MockRue(MockLocalite.Villette, "Chemin de Creux-Béchet", 108559);
		public static final MockRue CheminDesGranges = new MockRue(MockLocalite.Aran, "Chemin des Granges", 199333);
	}

	public static class Grandvaux {
		public static final MockRue SentierDesVinches = new MockRue(MockLocalite.Grandvaux, "Sentier des Vinches", 33455);
		public static final MockRue RouteDeLausanne = new MockRue(MockLocalite.Cully, "Route de Lausanne", 31903);
		public static final MockRue RueSaintGeorges = new MockRue(MockLocalite.Grandvaux, "Rue Saint-Georges", 33446);
	}

	public static class Cully {
		public static final MockRue PlaceDuTemple = new MockRue(MockLocalite.Cully, "Place du Temple", 31906);
		public static final MockRue ChCFRamuz = new MockRue(MockLocalite.Cully, "Ch. C.-F. Ramuz", 31890);
		public static final MockRue ChDesColombaires = new MockRue(MockLocalite.Cully, "Ch. des Colombaires", 31894);
	}

	public static class Riex {
		public static final MockRue RueDuCollege = new MockRue(MockLocalite.Riex, "Rue du Collège", 193825);
		public static final MockRue RouteDeRossetDessus = new MockRue(MockLocalite.Riex, "Route de Rosset-Dessus", 193836);
		public static final MockRue RouteDeLaCorniche = new MockRue(MockLocalite.Riex, "Route de la Corniche", 193826);
	}

	public static class Epesses {
		public static final MockRue ChDuMont = new MockRue(MockLocalite.Epesses, "Ch. du Mont", 193897);
		public static final MockRue LaPlace = new MockRue(MockLocalite.Epesses, "La Place", 347669);
		public static final MockRue RueDeLaMottaz = new MockRue(MockLocalite.Epesses, "Rue de la Mottaz", 193898);
	}

	public static class Gressy {
		public static final MockRue RueCentrale = new MockRue(MockLocalite.Gressy, "Rue Centrale", 198580);
		public static final MockRue LesPechauds = new MockRue(MockLocalite.Gressy, "Les Péchauds", 198575);
		public static final MockRue CheminDuMichamp = new MockRue(MockLocalite.Gressy, "Chemin du Michamp", 351117);
	}

	public static class YverdonLesBains {
		public static final MockRue CheminDesMuguets = new MockRue(MockLocalite.YverdonLesBains, "Chemin des Muguets", 32723);
		public static final MockRue QuaiDeLaThiele = new MockRue(MockLocalite.YverdonLesBains, "Quai de la Thièle", 32774);
		public static final MockRue RueDeLaFaiencerie = new MockRue(MockLocalite.YverdonLesBains, "Rue de la Faïencerie", 32675);
	}

	private final MockLocalite localite;
	private final String designationCourrier;
	private final Integer noRue;

	public MockRue(MockLocalite localite, String designationCourrier, Integer noTechniqueRue) {
		this.designationCourrier = designationCourrier;
		this.noRue = noTechniqueRue;
		this.localite = localite;

		DefaultMockServiceInfrastructureService.addRue(this);
	}

	@Override
	public String getDesignationCourrier() {
		return designationCourrier;
	}

	@Override
	public Integer getNoLocalite() {
		return localite.getNoOrdre();
	}

	@Override
	public Integer getNoRue() {
		return noRue;
	}

	public MockLocalite getLocalite() {
		return localite;
	}
}
