package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Rue;

public class MockRue implements Rue {

	public static class Lausanne {
		public static final MockRue RouteGrangeNeuve = new MockRue(MockLocalite.Lausanne1003, "Route de la Grange-Neuve", 1134061);
		public static final MockRue AvenueJolimont = new MockRue(MockLocalite.Lausanne1005, "Avenue Jolimont", 1133660);
		public static final MockRue AvenueDeBeaulieu = new MockRue(MockLocalite.Lausanne1003, "Avenue de Beaulieu", 1133688);
		public static final MockRue BoulevardGrancy = new MockRue(MockLocalite.Lausanne1003, "Boulevard de Grancy", 1133757);
		public static final MockRue CheminDeMornex = new MockRue(MockLocalite.Lausanne1003, "Chemin de Mornex", 1133829);
		public static final MockRue CheminPrazBerthoud = new MockRue(MockLocalite.Lausanne1003, "Chemin de Praz-Berthoud", 1133833);
		public static final MockRue PlaceSaintFrancois = new MockRue(MockLocalite.Lausanne1003, "Place Saint-François", 1134023);
		public static final MockRue AvenueDeLaGare = new MockRue(MockLocalite.Lausanne1003, "Avenue de la Gare", 1133725);
		public static final MockRue CheminMessidor = new MockRue(MockLocalite.Lausanne1006, "Chemin Messidor", 1133775);
		public static final MockRue AvenueDesBergieres = new MockRue(MockLocalite.Lausanne1003, "Avenue des Bergières", 1133734);
		public static final MockRue AvenueGabrielDeRumine = new MockRue(MockLocalite.Lausanne1003, "Avenue Gabrielle-de-Rumine", 1133643);
	}

	public static class Morges {
		public static final MockRue RueDesAlpes = new MockRue(MockLocalite.Morges, "Rue des Alpes", 1136139);
		public static final MockRue RueDeLAvenir = new MockRue(MockLocalite.Morges, "Rue de l'Avenir", 1136137);
	}

	public static class Aubonne {
		public static final MockRue CheminDesClos = new MockRue(MockLocalite.Aubonne, "Chemin des Clos", 1130533);
		public static final MockRue CheminTraverse = new MockRue(MockLocalite.Aubonne, "Chemin de la Traverse", 1130530);
	}
	public static class Prilly {
		public static final MockRue RueDesMetiers = new MockRue(MockLocalite.Prilly, "Rue des Métiers", 1134376);
		public static final MockRue CheminDeLaPossession = new MockRue(MockLocalite.Prilly, "Chemin de la Possession", 1134342);
		public static final MockRue SentierFleurDeLys = new MockRue(MockLocalite.Prilly, "Sentier de la Fleur-de-Lys", 1134377);
	}

	public static class CossonayVille {
		public static final MockRue AvenueDuFuniculaire = new MockRue(MockLocalite.CossonayVille, "Avenue du Funiculaire", 1131419);
		public static final MockRue CheminDeRiondmorcel = new MockRue(MockLocalite.CossonayVille, "Chemin de Riondmorcel", 1131423);
	}

	public static class LesClees {
		public static final MockRue PlaceDeLaVille = new MockRue(MockLocalite.LesClees, "Place de la Ville", 2089585);
	}

	public static class Bex {
		public static final MockRue CheminDeLaForet = new MockRue(MockLocalite.Bex, "Chemin de la Forêt", 1129501);
	}

	public static class Romainmotier {
		public static final MockRue CheminDuCochet = new MockRue(MockLocalite.Romainmotier, "Chemin du Cochet", 2099524);
	}

	public static class VillarsSousYens {
		public static final MockRue CheminDuCollege = new MockRue(MockLocalite.VillarsSousYens, "Chemin du Collège", 1136416);
		public static final MockRue RuelleDuCarroz = new MockRue(MockLocalite.VillarsSousYens, "Ruelle du Carroz", 1136417);
		public static final MockRue RouteDeStPrex = new MockRue(MockLocalite.VillarsSousYens, "Route de St-Prex", 1136414);
	}

	public static class Neuchatel {
		public static final MockRue RueDesBeauxArts = new MockRue(MockLocalite.Neuchatel, "Rue des Beaux-Arts", 1152941);
	}

	public static class Orbe {
		public static final MockRue CheminDeLaTranchee = new MockRue(MockLocalite.Orbe, "Chemin de la Tranchée", 1138823);
		public static final MockRue RueDuMoulinet = new MockRue(MockLocalite.Orbe, "Rue du Moulinet", 1138885);
		public static final MockRue GrandRue = new MockRue(MockLocalite.Orbe, "Grand-Rue", 1138853);
	}

	public static class Vallorbe {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Vallorbe, "Grand-Rue", 1139006);
	}

	public static class Renens {
		public static final MockRue QuatorzeAvril = new MockRue(MockLocalite.Renens, "Avenue du 14-Avril", 1134518);
	}

	public static class Zurich {
		public static final MockRue VoltaStrasse = new MockRue(MockLocalite.Zurich8044, "Voltastrasse", 1016163);
		public static final MockRue GloriaStrasse = new MockRue(MockLocalite.Zurich8044, "Gloriastrasse", 1015072);
		public static final MockRue BadenerStrasse = new MockRue(MockLocalite.Zurich8004, "Badenerstrasse", 1014635);
	}

	public static class Chur {
		public static final MockRue Grabenstrasse = new MockRue(MockLocalite.Chur, "Grabenstrasse", 1107707);
	}

	public static class LeSentier {
		public static final MockRue GrandRue = new MockRue(MockLocalite.LeSentier, "Grand-Rue", 1141047);
	}

	public static class LesBioux {
		public static final MockRue LeClos = new MockRue(MockLocalite.LesBioux, "Le Clos", 1140993);
		public static final MockRue LaGrandePartie = new MockRue(MockLocalite.LesBioux, "La Grande Partie", 1140991);
	}

	public static class LePont {
		public static final MockRue SurLesQuais = new MockRue(MockLocalite.LePont, "Sur les Quais", 1141019);
	}

	public static class Enney {
		public static final MockRue CheminDAfflon = new MockRue(MockLocalite.Enney, "Chemin d'Afflon", 1066944);
	}

	public static class Chamblon {
		public static final MockRue RueDesUttins = new MockRue(MockLocalite.Chamblon, "Rue des Uttins", 1142198);
	}

	public static class LIsle {
		public static final MockRue rueMoulin = new MockRue(MockLocalite.LIsle, "Rue du Moulin", 2066389);
	}

	public static class GrangesMarnand {
		public static final MockRue SousLeBois = new MockRue(MockLocalite.GrangesMarnand, "Chemin Sous le Bois", 1139865);
		public static final MockRue ImpasseDeVerdairu = new MockRue(MockLocalite.GrangesMarnand, "Impasse de Verdairu", 2181191);
	}

	public static class Bussigny {
		public static final MockRue RueDeLIndustrie = new MockRue(MockLocalite.Bussigny, "Rue de l'Industrie", 1135490);
	}

	public static class Vevey {
		public static final MockRue RueDesMoulins = new MockRue(MockLocalite.Vevey, "Rue des Moulins", 1142094);
	}

	public static class Echallens {
		public static final MockRue GrandRue = new MockRue(MockLocalite.Echallens, "Grand'Rue", 1132343);
		public static final MockRue RouteDeMoudon = new MockRue(MockLocalite.Echallens, "Route de Moudon", 1132358);
	}

	public static class Lonay {
		public static final MockRue CheminDuRechoz = new MockRue(MockLocalite.Lonay, "Chemin de Réchoz", 1135906);
	}

	public static class Geneve {
		public static final MockRue AvenueGuiseppeMotta = new MockRue(MockLocalite.Geneve, "Avenue Guiseppe-Motta", 1154922);
	}

	public static class Moudon {
		public static final MockRue LeBourg = new MockRue(MockLocalite.Moudon, "Rue du Bourg", 1136845);
	}

	public static class Pully {
		public static final MockRue CheminDesRoches = new MockRue(MockLocalite.Pully, "Chemin des Roches", 1134458);
	}

	// Quelques rues de communes fusionnées au 1er juillet 2011
	public static class Villette {
		public static final MockRue RouteDeLausanne = new MockRue(MockLocalite.Villette, "Route de Lausanne", 1134717);
		public static final MockRue CheminDeCreuxBechet = new MockRue(MockLocalite.Villette, "Chemin de Creux-Béchet", 1135362);
		public static final MockRue CheminDesGranges = new MockRue(MockLocalite.Aran, "Chemin des Granges", 1135356);
	}

	public static class Grandvaux {
		public static final MockRue SentierDesVinches = new MockRue(MockLocalite.Grandvaux, "Sentier des Vinches", 1134878);
		public static final MockRue RouteDeLausanne = new MockRue(MockLocalite.Cully, "Route de Lausanne", 1134717);
		public static final MockRue RueSaintGeorges = new MockRue(MockLocalite.Grandvaux, "Rue Saint-Georges", 1134870);
	}

	public static class Cully {
		public static final MockRue PlaceDuTemple = new MockRue(MockLocalite.Cully, "Place du Temple", 1134713);
		public static final MockRue ChCFRamuz = new MockRue(MockLocalite.Cully, "Chemin Charles-Ferdinand-Ramuz", 1134691);
		public static final MockRue ChDesColombaires = new MockRue(MockLocalite.Cully, "Chemin des Colombaires", 1134697);
	}

	public static class Riex {
		public static final MockRue RueDuCollege = new MockRue(MockLocalite.Riex, "Rue du Collège", 1135189);
		public static final MockRue RouteDeRossetDessus = new MockRue(MockLocalite.Riex, "Route de Rosset-Dessus", 1135172);
		public static final MockRue RouteDeLaCorniche = new MockRue(MockLocalite.Riex, "Route de la Corniche", 1134720);
	}

	public static class Epesses {
		public static final MockRue ChDuMont = new MockRue(MockLocalite.Epesses, "Chemin du Mont", 1134735);
		public static final MockRue LaPlace = new MockRue(MockLocalite.Epesses, "La Place", 1134744);
		public static final MockRue RueDeLaMottaz = new MockRue(MockLocalite.Epesses, "Rue de la Mottaz", 1134751);
	}

	public static class Gressy {
		public static final MockRue RueCentrale = new MockRue(MockLocalite.Gressy, "Rue Centrale", 2187239);
		public static final MockRue LesPechaux = new MockRue(MockLocalite.Gressy, "Les Péchaux", 2187218);
		public static final MockRue CheminDeMichamp = new MockRue(MockLocalite.Gressy, "Chemin de Michamp", 2187198);
	}

	public static class YverdonLesBains {
		public static final MockRue CheminDesMuguets = new MockRue(MockLocalite.YverdonLesBains, "Chemin des Muguets", 1142761);
		public static final MockRue QuaiDeLaThiele = new MockRue(MockLocalite.YverdonLesBains, "Quai de la Thièle", 1142679);
		public static final MockRue RueDeLaFaiencerie = new MockRue(MockLocalite.YverdonLesBains, "Rue de la Faïencerie", 1142730);
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

	@Override
	public boolean isValidAt(RegDate date) {
		return true;
	}

	@Override
	public RegDate getDateDebut() {
		return null;
	}

	@Override
	public RegDate getDateFin() {
		return null;
	}

	public MockLocalite getLocalite() {
		return localite;
	}
}
