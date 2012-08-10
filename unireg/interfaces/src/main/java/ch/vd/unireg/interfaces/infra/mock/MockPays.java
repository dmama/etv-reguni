package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;


public class MockPays extends MockEntityOFS implements Pays {

	//
	// Les états souverains
	//

	public static final MockPays Suisse = new MockPays(ServiceInfrastructureRaw.noOfsSuisse, "Suisse", "CH", "CH", "CHE");
	public static final MockPays Albanie = new MockPays(8201, "Albanie", "AL", "AL", "ALB");
	public static final MockPays Danemark = new MockPays(8206, "Danemark", "DK", "DK", "DNK");
	public static final MockPays Allemagne = new MockPays(8207, "Allemagne", "DE", "DE", "DEU");
	public static final MockPays France = new MockPays(8212, "France", "FR", "FR", "FRA");
	public static final MockPays RoyaumeUni = new MockPays(8215, "Royaume-Uni", "GB", "GB", "GBR");
	public static final MockPays Liechtenstein = new MockPays(8222, "Liechtenstein", "LI", "LI", "LIE");
	public static final MockPays Espagne = new MockPays(8236, "Espagne", "ES", "ES", "ESP");
	public static final MockPays Turquie = new MockPays(8239, "Turquie", "TR", "TR", "TUR");
	public static final MockPays Russie = new MockPays(8264, "Russie", "RU", "RU", "RUS");
	public static final MockPays Colombie = new MockPays(8424, "Colombie", "CO", "CO", "COL");
	public static final MockPays EtatsUnis = new MockPays(8439, "Etats-Unis", "US", "US", "USA");
	public static final MockPays Japon = new MockPays(8515, "Japon", "JP", "JP", "JPN");
	public static final MockPays CoreeSud = new MockPays(8539, "Corée (Sud)", "KR", "KR", "KOR");
	public static final MockPays Kosovo = new MockPays(8256, "Kosovo", null, null, null);

	//
	// les territoires
	//

	public static final MockPays Gibraltar = new MockPays(8213, "Gibraltar", "GI", true, "GI", "GIB", MockPays.RoyaumeUni.getNoOFS());

	//
	// les cas bizarres (inactifs, pays inconnu...)
	//

	public static final MockPays PaysInconnu = new MockPays(ServiceInfrastructureRaw.noPaysInconnu, "PaysInconnu", "INC", null, null);
	public static final MockPays Apatridie = new MockPays(ServiceInfrastructureRaw.noPaysApatride, "Apatridie", "---", true, null, null, ServiceInfrastructureRaw.noPaysApatride);     // <-- à n'utiliser que pour les nationalités!
	public static final MockPays RDA = new MockPays(8208, "République démocratique allemande", "", false, null, null);

	private final boolean valide;
	private final int ofsEtatSouverain;
	private final String codeIso2;
	private final String codeIso3;

	public MockPays(int numeroOFS, String nomMiniscule, String codeIso2, String codeIso3) {
		super(numeroOFS, null, nomMiniscule);
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.valide = true;
		this.ofsEtatSouverain = numeroOFS;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, String codeIso2, String codeIso3) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.valide = true;
		this.ofsEtatSouverain = numeroOFS;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, boolean valide, String codeIso2, String codeIso3) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.valide = valide;
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.ofsEtatSouverain = numeroOFS;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	/**
	 * Constructeur pour les territoires
	 */
	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, boolean valide, String codeIso2, String codeIso3, int ofsEtatSouverainParent) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.valide = valide;
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.ofsEtatSouverain = ofsEtatSouverainParent;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	@Override
	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureRaw.noOfsSuisse;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

	@Override
	public String getCodeIso2() {
		return codeIso2;
	}

	@Override
	public String getCodeIso3() {
		return codeIso3;
	}

	@Override
	public boolean isEtatSouverain() {
		return getNoOFS() == getNoOfsEtatSouverain();
	}

	@Override
	public int getNoOfsEtatSouverain() {
		return ofsEtatSouverain;
	}
}
