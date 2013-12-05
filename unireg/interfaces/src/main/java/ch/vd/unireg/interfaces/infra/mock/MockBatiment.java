package ch.vd.unireg.interfaces.infra.mock;

/**
 * Pseudo-entité bâtiment qui permet de spécifier le lien entre un numéro de bâtiment (egid) et une rue.
 */
public class MockBatiment {

	private final int egid;
	private final MockRue rue;

	// Quelques bâtiments sur quelques localités

	public static class Villette {
		public static final MockBatiment BatimentRouteDeLausanne = new MockBatiment(793324, MockRue.Villette.RouteDeLausanne);
		public static final MockBatiment BatimentCheminDeCreuxBechet = new MockBatiment(793359, MockRue.Villette.CheminDeCreuxBechet);
		public static final MockBatiment BatimentCheminDesGranges= new MockBatiment(793456, MockRue.Villette.CheminDesGranges);
	}

	public static class Grandvaux {
		public static final MockBatiment BatimentSentierDesVinches = new MockBatiment(789773, MockRue.Grandvaux.SentierDesVinches);
		public static final MockBatiment BatimentRouteDeLausanne = new MockBatiment(789756, MockRue.Grandvaux.RouteDeLausanne);
		public static final MockBatiment BatimentRueSaintGeorges = new MockBatiment(789802, MockRue.Grandvaux.RueSaintGeorges);
	}

	public static class Cully {
		public static final MockBatiment BatimentPlaceDuTemple = new MockBatiment(1770024, MockRue.Cully.PlaceDuTemple);
		public static final MockBatiment BatimentChCFRamuz = new MockBatiment(1769966, MockRue.Cully.ChCFRamuz);
		public static final MockBatiment BatimentChDesColombaires = new MockBatiment(1770118, MockRue.Cully.ChDesColombaires);
	}

	public static class Riex {
		public static final MockBatiment BatimentRueDuCollege = new MockBatiment(792312, MockRue.Riex.RueDuCollege);
		public static final MockBatiment BatimentRouteDeRossetDessus = new MockBatiment(792351, MockRue.Riex.RouteDeRossetDessus);
		public static final MockBatiment BatimentRouteDeLaCorniche = new MockBatiment(792273, MockRue.Riex.RouteDeLaCorniche);
	}

	public static class Epesses {
		public static final MockBatiment BatimentChDuMont = new MockBatiment(789306, MockRue.Epesses.ChDuMont);
		public static final MockBatiment BatimentLaPlace = new MockBatiment(789271, MockRue.Epesses.LaPlace);
		public static final MockBatiment BatimentRueDeLaMottaz = new MockBatiment(789307, MockRue.Epesses.RueDeLaMottaz);
	}

	public static class Gressy {
		public static final MockBatiment BatimentRueCentrale = new MockBatiment(842713, MockRue.Gressy.RueCentrale);
		public static final MockBatiment BatimentLesPechauds = new MockBatiment(842701, MockRue.Gressy.LesPechauds);
		public static final MockBatiment BatimentCheminDuMichamp = new MockBatiment(842706, MockRue.Gressy.CheminDuMichamp);
	}

	public static class YverdonLesBains {
		public static final MockBatiment BatimentCheminDesMuguets = new MockBatiment(845012, MockRue.YverdonLesBains.CheminDesMuguets);
		public static final MockBatiment BatimentQuaiDeLaThiele = new MockBatiment(3164396, MockRue.YverdonLesBains.QuaiDeLaThiele);
		public static final MockBatiment BatimentRueDeLaFaiencerie = new MockBatiment(844765, MockRue.YverdonLesBains.RueDeLaFaiencerie);
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
