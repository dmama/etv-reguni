package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class MockCanton extends MockEntityOFS implements Canton {

	private static final long serialVersionUID = -4771321479134814053L;

	public static final MockCanton Vaud = new MockCanton(22, "VD", "Vaud");
	public static final MockCanton Geneve = new MockCanton(25, "GE", "Genève");
	public static final MockCanton Zurich = new MockCanton(1, "ZH", "Zurich");
	public static final MockCanton Neuchatel = new MockCanton(24, "NE", "Neuchâtel");
	public static final MockCanton Berne = new MockCanton(02, "BE", "Berne");

	private String nomMinusculeOFS;

	/**
	 * Permet de forcer le chargement des Mock dans le DefaultMockService
	 * Il faut ajouter les nouveaux Mock dans cette methode
	 */
	@SuppressWarnings("unused")
	public static void forceLoad() {
		MockCanton c;
		c = Vaud;
		c = Geneve;
		c = Zurich;
		c = Neuchatel;
		c = Berne;
	}

	public MockCanton(int noOFS, String sigleOFS, String nomMinuscule) {
		super(noOFS, sigleOFS, nomMinuscule);
		DefaultMockServiceInfrastructureService.addCanton(this);
	}

	public String getNomMinusculeOFS() {
		return nomMinusculeOFS;
	}

	public void setNomMinusculeOFS(String nomMinusculeOFS) {
		this.nomMinusculeOFS = nomMinusculeOFS;
	}
}
