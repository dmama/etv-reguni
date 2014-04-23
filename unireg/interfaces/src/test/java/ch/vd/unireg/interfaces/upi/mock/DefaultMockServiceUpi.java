package ch.vd.unireg.interfaces.upi.mock;

public class DefaultMockServiceUpi extends MockServiceUpi {

	@Override
	protected void init() {
		addReplacement("7567986294906", "7565115001333");
		addReplacement("7566101270542", "7566285711978");
		addReplacement("7561163512081", "7564457068837");
		addReplacement("7560142399040", "7569050304498");
		addReplacement("7568683576722", "7564775497586");
		addUnknown("7560000000002");
	}
}
