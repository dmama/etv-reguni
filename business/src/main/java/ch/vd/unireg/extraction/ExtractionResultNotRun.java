package ch.vd.unireg.extraction;

public class ExtractionResultNotRun extends ExtractionResult {

	@Override
	public final State getSummary() {
		return State.NOT_RUN;
	}

	public String toString() {
		return "Non exécuté";
	}
}
