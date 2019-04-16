package ch.vd.unireg.interfaces.infra.mock;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.Canton;

public class MockCanton extends MockEntityOFS implements Canton {

	public static final MockCanton Vaud = new MockCanton(22, "VD", "Vaud");
	public static final MockCanton Geneve = new MockCanton(25, "GE", "Genève");
	public static final MockCanton Zurich = new MockCanton(1, "ZH", "Zurich");
	public static final MockCanton Neuchatel = new MockCanton(24, "NE", "Neuchâtel");
	public static final MockCanton Berne = new MockCanton(2, "BE", "Berne");
	public static final MockCanton BaleVille = new MockCanton(12, "BS", "Bâle-Ville");

	public MockCanton(int noOFS, @NotNull String sigleOFS, @NotNull String nom) {
		super(noOFS, sigleOFS, nom, nom);
		DefaultMockServiceInfrastructureService.addCanton(this);
	}
}
