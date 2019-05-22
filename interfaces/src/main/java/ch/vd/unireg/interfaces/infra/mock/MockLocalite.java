package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;

public class MockLocalite implements Localite {

	public static final MockLocalite Lausanne = new MockLocalite(104, 1000, null, "Lausanne", MockCommune.Lausanne);
	public static final MockLocalite Lausanne1003 = new MockLocalite(150, 1003, null, "Lausanne", MockCommune.Lausanne);
	public static final MockLocalite Lausanne1005 = new MockLocalite(152, 1005, null, "Lausanne", MockCommune.Lausanne);
	public static final MockLocalite Lausanne1006 = new MockLocalite(153, 1006, null, "Lausanne", MockCommune.Lausanne);
	public static final MockLocalite Lausanne1014 = new MockLocalite(162, 1014, null, "Lausanne Adm cant VD", MockCommune.Lausanne);
	public static final MockLocalite CossonayVille = new MockLocalite(528, 1304, null, "Cossonay-Ville", MockCommune.Cossonay);
	public static final MockLocalite Romainmotier = new MockLocalite(564, 1323, null, "Romainmôtier", MockCommune.RomainmotierEnvy);
	public static final MockLocalite Morges = new MockLocalite(254, 1110, null, "Morges", MockCommune.Morges);
	public static final MockLocalite LesClees = new MockLocalite(578, 1356, null, "Clées, Les", MockCommune.LesClees);
	public static final MockLocalite Bex = new MockLocalite(1124, 1880, null, "Bex", MockCommune.Bex);
	public static final MockLocalite VillarsSousYens = new MockLocalite(283, 1168, null, "Villars-sous-Yens", MockCommune.VillarsSousYens);
	public static final MockLocalite Orbe = new MockLocalite(571, 1350, null, "Orbe", MockCommune.Orbe);
	public static final MockLocalite Vevey = new MockLocalite(1043, 1800, null, "Vevey", MockCommune.Vevey);
	public static final MockLocalite Renens = new MockLocalite(165, 1020, null, "Renens VD", MockCommune.Renens);
	public static final MockLocalite CheseauxSurLausanne = new MockLocalite(180, 1033, null, "Cheseaux-sur-Lausanne", MockCommune.CheseauxSurLausanne);
	public static final MockLocalite VufflensLaVille = new MockLocalite(526, 1302, null, "Vufflens-la-Ville", MockCommune.VufflensLaVille);
	public static final MockLocalite Vallorbe = new MockLocalite(535, 1337, null, "Vallorbe", MockCommune.Vallorbe);
	public static final MockLocalite LIsle = new MockLocalite(293, 1148, null, "L'Isle", MockCommune.LIsle);
	public static final MockLocalite GrangesMarnand = new MockLocalite(715, 1523, null, "Granges-près-Marnand", MockCommune.GrangesMarnand);
	public static final MockLocalite Chamblon = new MockLocalite(5876, 1436, null, "Chamblon", MockCommune.Chamblon);
	public static final MockLocalite Bussigny = new MockLocalite(178, 1030, null, "Bussigny", MockCommune.Bussigny);
	public static final MockLocalite Echallens = new MockLocalite(185, 1040, null, "Echallens", MockCommune.Echallens);
	public static final MockLocalite Lonay = new MockLocalite(174, 1027, null, "Lonay", MockCommune.Lonay);
	public static final MockLocalite RomanelSurLausanne = new MockLocalite(179, 1032, null, "Romanel-s-Lausanne", "Romanel-sur-Lausanne", MockCommune.RomanelSurLausanne);
	public static final MockLocalite Moudon = new MockLocalite(700, 1510, null, "Moudon", MockCommune.Moudon);
	public static final MockLocalite Pully = new MockLocalite(157, 1009, null, "Pully", MockCommune.Pully);
	public static final MockLocalite Prilly = new MockLocalite(156, 1008, null, "Prilly", MockCommune.Prilly);
	public static final MockLocalite Villette = new MockLocalite(7949, 1096, null, "Villette (Lavaux)", MockCommune.Villette);
	public static final MockLocalite Aran = new MockLocalite(785, 1091, null, "Aran", MockCommune.Villette);
	public static final MockLocalite Grandvaux = new MockLocalite(784, 1091, null, "Grandvaux", MockCommune.Grandvaux);
	public static final MockLocalite Cully = new MockLocalite(228, 1096, null, "Cully", MockCommune.Cully);
	public static final MockLocalite Riex = new MockLocalite(229, 1097, null, "Riex", MockCommune.Riex);
	public static final MockLocalite Epesses = new MockLocalite(230, 1098, null, "Epesses", MockCommune.Epesses);
	public static final MockLocalite Gressy = new MockLocalite(5881, 1432, null, "Gressy", MockCommune.Gressy);
	public static final MockLocalite YverdonLesBains = new MockLocalite(592, 1400, null, "Yverdon-les-Bains", MockCommune.YverdonLesBains);
	public static final MockLocalite Aubonne = new MockLocalite(299, 1170, null, "Aubonne", MockCommune.Aubonne);
	public static final MockLocalite Leysin = new MockLocalite(1096, 1854, null, "Leysin", MockCommune.Leysin);
	public static final MockLocalite Savigny = new MockLocalite(222, 1073, null, "Savigny", MockCommune.Savigny);
	public static final MockLocalite ForelLavaux = new MockLocalite(789, 1072, null, "Forel (Lavaux)", MockCommune.ForelLavaux);

	// fractions de communes - L'Abbaye
	public static final MockLocalite LePont = new MockLocalite(543, 1342, null, "Pont, Le", MockCommune.LAbbaye);
	public static final MockLocalite LAbbaye = new MockLocalite(542, 1344, null, "L'Abbaye", MockCommune.LAbbaye);
	public static final MockLocalite LesBioux = new MockLocalite(541, 1346, null, "Les Bioux", MockCommune.LAbbaye);

	// fractions de communes - Le Chenit
	public static final MockLocalite Orient = new MockLocalite(540, 1341, null, "Orient", MockCommune.LeChenit);
	public static final MockLocalite LeSentier = new MockLocalite(546, 1347, null, "Le Sentier", MockCommune.LeChenit);
	public static final MockLocalite LeBrassus = new MockLocalite(550, 1348, 1, "Le Brassus", MockCommune.LeChenit);
	public static final MockLocalite LeSolliat = new MockLocalite(7447, 1347, 1, "Le Solliat", MockCommune.LeChenit);

	// fractions de communes - Le Lieu
	public static final MockLocalite LeLieu = new MockLocalite(544, 1345, null, "Le Lieu", MockCommune.LeLieu);
	// Le Séchey est une fraction de commune, mais pas une localité...
	// public static final MockLocalite LeSechey = new MockLocalite("Le Séchey");
	public static final MockLocalite LesCharbonnieres = new MockLocalite(545, 1343, null, "Les Charbonnières", MockCommune.LeLieu);

	//
	// localités hors-canton
	//

	public static final MockLocalite Bale = new MockLocalite(2431, 4000, null, "Basel", MockCommune.Bale);
	public static final MockLocalite Bern = new MockLocalite(1670, 3000, null, "Bern", MockCommune.Bern);
	public static final MockLocalite Chur = new MockLocalite(3970, 7000, null, "Chur", MockCommune.Chur);
	public static final MockLocalite Neuchatel = new MockLocalite(1254, 2000, null, "Neuchâtel", MockCommune.Neuchatel);
	public static final MockLocalite Neuchatel1Cases = new MockLocalite(1269, 2001, 1, "Neuchâtel 1 Cases", MockCommune.Neuchatel);
	public static final MockLocalite Neuchatel3Serrieres = new MockLocalite(1286, 2003, null, "Neuchâtel 3 Serrières", MockCommune.Neuchatel);
	public static final MockLocalite Bumpliz = new MockLocalite(3333, 3018, null, "Bumpliz", MockCommune.Bern);
	public static final MockLocalite Enney = new MockLocalite(839, 1667, null, "Enney", MockCommune.Enney);
	public static final MockLocalite Zurich = new MockLocalite(8120, 8001, null, "Zurich", MockCommune.Zurich);
	public static final MockLocalite Zurich8004 = new MockLocalite(4388, 8004, null, "Zurich", MockCommune.Zurich);
	public static final MockLocalite Zurich8044 = new MockLocalite(4457, 8044, null, "Zurich", MockCommune.Zurich);
	public static final MockLocalite Geneve = new MockLocalite(368, 1202, null, "Genève", MockCommune.Geneve);


	private final Integer chiffreComplementaire;
	private final Integer complementNPA;
	private final Integer noCommune;
	private final String nomAbrege;
	private final String nomComplet;
	private final Integer noOrdre;
	private final Integer nPA;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Commune communeLocalite;

	public MockLocalite(Integer noOrdre, Integer nPA, Integer complementNPA, String nom, MockCommune commune) {
		if (commune == null) {
			throw new IllegalArgumentException();
		}
		this.noOrdre = noOrdre;
		this.nPA = nPA;
		this.chiffreComplementaire = null;
		this.complementNPA = complementNPA;
		this.noCommune = commune.getNoOFS();
		this.nomComplet = nom;
		this.nomAbrege = nom;
		this.communeLocalite = commune;
		this.dateDebut = null;
		this.dateFin = null;

		DefaultMockInfrastructureConnector.addLocalite(this);
	}

	public MockLocalite(Integer noOrdre, Integer nPA, Integer complementNPA, String nomAbrege, String nomComplet, MockCommune commune) {
		this.noOrdre = noOrdre;
		this.nPA = nPA;
		this.chiffreComplementaire = null;
		this.complementNPA = complementNPA;
		this.noCommune = commune.getNoOFS();
		this.nomComplet = nomComplet;
		this.nomAbrege = nomAbrege;
		this.communeLocalite = commune;
		this.dateDebut = null;
		this.dateFin = null;

		DefaultMockInfrastructureConnector.addLocalite(this);
	}

	@Override
	public Integer getChiffreComplementaire() {
		return chiffreComplementaire;
	}

	@Override
	public Integer getComplementNPA() {
		return complementNPA;
	}

	@Override
	public Integer getNoCommune() {
		return noCommune;
	}

	@Override
	public String getNomAbrege() {
		return nomAbrege;
	}

	@Override
	public String getNom() {
		return nomComplet;
	}

	@Override
	public Integer getNoOrdre() {
		return noOrdre;
	}

	@Override
	public Integer getNPA() {
		return nPA;
	}

	@Override
	public Commune getCommuneLocalite() {
		return communeLocalite;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}
}
