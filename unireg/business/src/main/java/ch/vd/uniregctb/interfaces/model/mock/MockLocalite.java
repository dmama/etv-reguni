package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class MockLocalite implements Localite {

	private static final long serialVersionUID = -1808690130516540052L;

	public static final MockLocalite Lausanne = new MockLocalite(104, 1000, null, "Lausanne", MockCommune.Lausanne);
	public static final MockLocalite CossonayVille = new MockLocalite(528, 1304, null, "Cossonay-Ville", MockCommune.Cossonay);
	public static final MockLocalite Romainmotier = new MockLocalite(564, 1323, null, "Romainmôtier", MockCommune.RomainmotierEnvy);
	public static final MockLocalite LesClees = new MockLocalite(578, 1356, null, "Clées, Les", MockCommune.LesClees);
	public static final MockLocalite Bex = new MockLocalite(1124, 1880, null, "Bex", MockCommune.Bex);
	public static final MockLocalite VillarsSousYens = new MockLocalite(283, 1168, null, "Villars-sous-Yens", MockCommune.VillarsSousYens);
	public static final MockLocalite Orbe = new MockLocalite(571, 1350, null, "Orbe", MockCommune.Orbe);
	public static final MockLocalite Vevey = new MockLocalite(1043, 1800, null, "Vevey", MockCommune.Vevey);
	public static final MockLocalite Renens = new MockLocalite(165, 1020, null, "Renens VD", MockCommune.Renens);
	public static final MockLocalite CheseauzSurLausanne = new MockLocalite(180, 1033, null, "Cheseaux-sur-Lausanne", MockCommune.CheseauxSurLausanne);
	public static final MockLocalite VufflensLaVille = new MockLocalite(526, 1302, null, "Vufflens-la-Ville", MockCommune.VufflensLaVille);
	public static final MockLocalite Vallorbe = new MockLocalite(535, 1337, null, "Vallorbe", MockCommune.Vallorbe);
	public static final MockLocalite LIsle = new MockLocalite(293, 1148, null, "L'Isle", MockCommune.LIsle);
	public static final MockLocalite GrangesMarnand = new MockLocalite(715, 1523, null, "Granges-près-Marnand", MockCommune.GrangesMarnand);
	public static final MockLocalite Chamblon = new MockLocalite(5876, 1436, null, "Chamblon", MockCommune.Chamblon);
	public static final MockLocalite Bussigny = new MockLocalite(178, 1030, null, "Bussigny-près-Lausanne", MockCommune.Bussigny);
	public static final MockLocalite Echallens = new MockLocalite(185, 1040, null, "Echallens", MockCommune.Echallens);
	public static final MockLocalite Lonay = new MockLocalite(174, 1027, null, "Lonay", MockCommune.Lonay);
	public static final MockLocalite RomanelSurLausanne = new MockLocalite(179, 1032, null, "Romanel-s-Lausanne", "Romanel-sur-Lausanne", MockCommune.RomanelSurLausanne);
	public static final MockLocalite Moudon = new MockLocalite(700, 1510, null, "Moudon", MockCommune.Moudon);
	public static final MockLocalite Pully = new MockLocalite(157, 1009, null, "Pully", MockCommune.Pully);
	public static final MockLocalite Prilly = new MockLocalite(156, 1008, null, "Prilly", MockCommune.Prilly);

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
	// communes hors-canton
	//

	public static final MockLocalite Neuchatel = new MockLocalite(1254, 2000, null, "Neuchâtel", MockCommune.Neuchatel);
	public static final MockLocalite Neuchatel1Cases = new MockLocalite(1269, 2001, 1, "Neuchâtel 1 Cases", MockCommune.Neuchatel);
	public static final MockLocalite Neuchatel3Serrieres = new MockLocalite(1286, 2003, null, "Neuchâtel 3 Serrières", MockCommune.Neuchatel);
	public static final MockLocalite Bumpliz = new MockLocalite(3333, 3018, null, "Bumpliz", MockCommune.Bern);
	public static final MockLocalite Enney = new MockLocalite(839, 1667, null, "Enney", MockCommune.Enney);
	public static final MockLocalite Zurich = new MockLocalite(8120, 8001, null, "Zurich", MockCommune.Zurich);
	public static final MockLocalite GeneveSecteurDist = new MockLocalite(368, 1202, null, "Genève Secteur de dist.", MockCommune.Geneve);

	
	private Integer chiffreComplementaire;
	private Integer complementNPA;
	private RegDate dateFinValidite;
	private Integer noCommune;
	private String nomAbregeMajuscule;
	private String nomAbregeMinuscule;
	private String nomCompletMajuscule;
	private String nomCompletMinuscule;
	private Integer noOrdre;
	private Integer nPA;
	private boolean valide;
	private CommuneSimple communeLocalite;

	public MockLocalite() {
		DefaultMockServiceInfrastructureService.addLocalite(this);
	}

	public MockLocalite(Integer noOrdre, Integer nPA, Integer complementNPA, String nomCompletMinuscule, MockCommune commune) {
		this.noOrdre = noOrdre;
		this.nPA = nPA;
		this.complementNPA = complementNPA;
		this.noCommune = commune.getNoOFSEtendu();
		this.nomCompletMinuscule = nomCompletMinuscule;
		this.nomAbregeMinuscule = nomCompletMinuscule;
		this.communeLocalite = commune;

		DefaultMockServiceInfrastructureService.addLocalite(this);
	}

	public MockLocalite(Integer noOrdre, Integer nPA, Integer complementNPA, String nomAbregeMinuscule, String nomCompletMinuscule, MockCommune commune) {
		this.noOrdre = noOrdre;
		this.nPA = nPA;
		this.complementNPA = complementNPA;
		this.noCommune = commune.getNoOFSEtendu();
		this.nomCompletMinuscule = nomCompletMinuscule;
		this.nomAbregeMinuscule = nomAbregeMinuscule;
		this.communeLocalite = commune;

		DefaultMockServiceInfrastructureService.addLocalite(this);
	}

	public Integer getChiffreComplementaire() {
		return chiffreComplementaire;
	}

	public void setChiffreComplementaire(Integer chiffreComplementaire) {
		this.chiffreComplementaire = chiffreComplementaire;
	}

	public Integer getComplementNPA() {
		return complementNPA;
	}

	public void setComplementNPA(Integer complementNPA) {
		this.complementNPA = complementNPA;
	}

	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	public Integer getNoCommune() {
		return noCommune;
	}

	public void setNoCommune(Integer noCommune) {
		this.noCommune = noCommune;
	}

	public String getNomAbregeMajuscule() {
		return nomAbregeMajuscule;
	}

	public void setNomAbregeMajuscule(String nomAbregeMajuscule) {
		this.nomAbregeMajuscule = nomAbregeMajuscule;
	}

	public String getNomAbregeMinuscule() {
		return nomAbregeMinuscule;
	}

	public void setNomAbregeMinuscule(String nomAbregeMinuscule) {
		this.nomAbregeMinuscule = nomAbregeMinuscule;
	}

	public String getNomCompletMajuscule() {
		return nomCompletMajuscule;
	}

	public void setNomCompletMajuscule(String nomCompletMajuscule) {
		this.nomCompletMajuscule = nomCompletMajuscule;
	}

	public String getNomCompletMinuscule() {
		return nomCompletMinuscule;
	}

	public void setNomCompletMinuscule(String nomCompletMinuscule) {
		this.nomCompletMinuscule = nomCompletMinuscule;
	}

	public Integer getNoOrdre() {
		return noOrdre;
	}

	public void setNoOrdre(Integer noOrdre) {
		this.noOrdre = noOrdre;
	}

	public Integer getNPA() {
		return nPA;
	}

	public void setNPA(Integer npa) {
		nPA = npa;
	}

	public boolean isValide() {
		return valide;
	}

	public void setValide(boolean valide) {
		this.valide = valide;
	}

	public CommuneSimple getCommuneLocalite() {
		return communeLocalite;
	}

	public void setCommuneLocalite(CommuneSimple c) {
		communeLocalite = c;
	}

}
