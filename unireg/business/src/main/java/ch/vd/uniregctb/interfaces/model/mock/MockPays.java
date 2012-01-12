package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;


public class MockPays extends MockEntityOFS implements Pays {

	public static final MockPays Suisse = new MockPays(ServiceInfrastructureService.noOfsSuisse, "Suisse", "CH", "CH", "CHE");
	public static final MockPays Albanie = new MockPays(8201, "Albanie", "AL", "AL", "ALB");
	public static final MockPays Danemark = new MockPays(8206, "Danemark", "DK", "DK", "DNK");
	public static final MockPays Allemagne = new MockPays(8207, "Allemagne", "DE", "DE");
	public static final MockPays France = new MockPays(8212, "France", "FR", "DEU");
	public static final MockPays Gibraltar = new MockPays(8213, "Gibraltar", "GI", true, false, "GI", "GIB");
	public static final MockPays RoyaumeUni = new MockPays(8215, "Royaume-Uni", "GB", "GB", "GBR");
	public static final MockPays Liechtenstein = new MockPays(8222, "Liechtenstein", "LI", "LI", "LIE");
	public static final MockPays Espagne = new MockPays(8236, "Espagne", "ES", "ES", "ESP");
	public static final MockPays Turquie = new MockPays(8239, "Turquie", "TR", "TR", "TUR");
	public static final MockPays Russie = new MockPays(8264, "Russie", "RU", "RU", "RUS");
	public static final MockPays Colombie = new MockPays(8424, "Colombie", "CO", "CO", "COL");
	public static final MockPays EtatsUnis = new MockPays(8439, "Etats-Unis", "US", "US", "USA");
	public static final MockPays CoreeSud = new MockPays(8539, "Corée (Sud)", "KR", "KR", "KOR");

	public static final MockPays PaysInconnu = new MockPays(ServiceInfrastructureService.noPaysInconnu, "PaysInconnu", "INC", null, null);
	public static final MockPays RDA = new MockPays(8208, "République démocratique allemande", "", false, null, null);

	private final boolean valide;
	private final boolean etatSouverain;
	private final String codeIso2;
	private final String codeIso3;

	public MockPays(int numeroOFS, String nomMiniscule, String codeIso2, String codeIso3) {
		super(numeroOFS, null, nomMiniscule);
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.valide = true;
		this.etatSouverain = true;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, String codeIso2, String codeIso3) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.valide = true;
		this.etatSouverain = true;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, boolean valide, String codeIso2, String codeIso3) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.valide = valide;
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.etatSouverain = true;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, boolean valide, boolean etatSouverain, String codeIso2, String codeIso3) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.valide = valide;
		this.etatSouverain = etatSouverain;
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	@Override
	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureService.noOfsSuisse;
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
		return etatSouverain;
	}
}
