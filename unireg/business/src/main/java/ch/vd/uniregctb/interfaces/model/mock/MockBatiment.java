package ch.vd.uniregctb.interfaces.model.mock;

/**
 * Pseudo-entité bâtiment qui permet de spécifier le lien entre un numéro de bâtiment (egid) et une rue.
 */
public class MockBatiment {

	private int egid;
	private MockRue rue;

	// Quelques bâtiments sur quelques localités

	public static class Villette {
		public static MockBatiment BatimentRouteDeLausanne = new MockBatiment(793324, MockRue.Villette.RouteDeLausanne);
		public static MockBatiment BatimentCheminDeCreuxBechet = new MockBatiment(793359, MockRue.Villette.CheminDeCreuxBechet);
		public static MockBatiment BatimentCheminDesGranges= new MockBatiment(793456, MockRue.Villette.CheminDesGranges);
	}

	public static class Grandvaux {
		public static MockBatiment BatimentSentierDesVinches = new MockBatiment(789773, MockRue.Grandvaux.SentierDesVinches);
		public static MockBatiment BatimentRouteDeLausanne = new MockBatiment(789756, MockRue.Grandvaux.RouteDeLausanne);
		public static MockBatiment BatimentRueSaintGeorges = new MockBatiment(789802, MockRue.Grandvaux.RueSaintGeorges);
	}

	public static class Cully {
		public static MockBatiment BatimentPlaceDuTemple = new MockBatiment(1770024, MockRue.Cully.PlaceDuTemple);
		public static MockBatiment BatimentChCFRamuz = new MockBatiment(1769966, MockRue.Cully.ChCFRamuz);
		public static MockBatiment BatimentChDesColombaires = new MockBatiment(1770118, MockRue.Cully.ChDesColombaires);
	}

	public static class Riex {
		public static MockBatiment BatimentRueDuCollege = new MockBatiment(792312, MockRue.Riex.RueDuCollege);
		public static MockBatiment BatimentRouteDeRossetDessus = new MockBatiment(792351, MockRue.Riex.RouteDeRossetDessus);
		public static MockBatiment BatimentRouteDeLaCorniche = new MockBatiment(792273, MockRue.Riex.RouteDeLaCorniche);
	}

	public static class Epesses {
		public static MockBatiment BatimentChDuMont = new MockBatiment(789306, MockRue.Epesses.ChDuMont);
		public static MockBatiment BatimentLaPlace = new MockBatiment(789271, MockRue.Epesses.LaPlace);
		public static MockBatiment BatimentRueDeLaMottaz = new MockBatiment(789307, MockRue.Epesses.RueDeLaMottaz);
	}

	public static class Gressy {
		public static MockBatiment BatimentRueCentrale = new MockBatiment(842713, MockRue.Gressy.RueCentrale);
		public static MockBatiment BatimentLesPechauds = new MockBatiment(842701, MockRue.Gressy.LesPechauds);
		public static MockBatiment BatimentCheminDuMichamp = new MockBatiment(842706, MockRue.Gressy.CheminDuMichamp);
	}

	public static class YverdonLesBains {
		public static MockBatiment BatimentCheminDesMuguets = new MockBatiment(845012, MockRue.YverdonLesBains.CheminDesMuguets);
		public static MockBatiment BatimentQuaiDeLaThiele = new MockBatiment(3164396, MockRue.YverdonLesBains.QuaiDeLaThiele);
		public static MockBatiment BatimentRueDeLaFaiencerie = new MockBatiment(844765, MockRue.YverdonLesBains.RueDeLaFaiencerie);
	}
	
	public MockBatiment(int egid, MockRue rue) {
		this.egid = egid;
		this.rue = rue;
	}

	public int getEgid() {
		return egid;
	}

	public MockRue getRue() {
		return rue;
	}
}
