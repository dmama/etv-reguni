package ch.vd.uniregctb.evenement.fiscal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.event.fiscal.v5.FiscalEvent;

public interface EvenementFiscalV5Factory {
	@Nullable
	FiscalEvent buildOutputData(@NotNull EvenementFiscal evt);
}
