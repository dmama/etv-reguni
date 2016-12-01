package ch.vd.uniregctb.registrefoncier;

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.registrefoncier.detector.AyantDroitRFDetector;
import ch.vd.uniregctb.registrefoncier.detector.BatimentRFDetector;
import ch.vd.uniregctb.registrefoncier.detector.DroitRFDetector;
import ch.vd.uniregctb.registrefoncier.detector.ImmeubleRFDetector;
import ch.vd.uniregctb.registrefoncier.detector.SurfaceAuSolRFDetector;

/**
 * Cette classe reçoit les données extraites de l'import du registre foncier, les compare avec les données en base et génère des événements de mutation correspondants.
 */
public class DataRFMutationsDetector {

	private final AyantDroitRFDetector ayantDroitRFDetector;
	private final BatimentRFDetector batimentRFDetector;
	private final DroitRFDetector droitRFDetector;
	private final ImmeubleRFDetector immeubleRFDetector;
	private final SurfaceAuSolRFDetector surfaceAuSolRFDetector;

	public DataRFMutationsDetector(AyantDroitRFDetector ayantDroitRFDetector,
	                               BatimentRFDetector batimentRFDetector,
	                               DroitRFDetector droitRFDetector,
	                               ImmeubleRFDetector immeubleRFDetector,
	                               SurfaceAuSolRFDetector surfaceAuSolRFDetector) {
		this.ayantDroitRFDetector = ayantDroitRFDetector;
		this.batimentRFDetector = batimentRFDetector;
		this.droitRFDetector = droitRFDetector;
		this.immeubleRFDetector = immeubleRFDetector;
		this.surfaceAuSolRFDetector = surfaceAuSolRFDetector;
	}

	public void processImmeubles(long importId, final int nbThreads, @NotNull Iterator<Grundstueck> iterator, @Nullable StatusManager statusManager) {
		immeubleRFDetector.processImmeubles(importId, nbThreads, iterator, statusManager);
	}

	public void processDroits(long importId, int nbThreads, Iterator<PersonEigentumAnteil> iterator, @Nullable StatusManager statusManager) {
		droitRFDetector.processDroits(importId, nbThreads, iterator, statusManager);
	}

	public void processProprietaires(long importId, int nbThreads, Iterator<Personstamm> iterator, @Nullable StatusManager statusManager) {
		ayantDroitRFDetector.processProprietaires(importId, nbThreads, iterator, statusManager);
	}

	public void processBatiments(long importId, int nbThreads, Iterator<Gebaeude> iterator, @Nullable StatusManager statusManager) {
		batimentRFDetector.processBatiments(importId, nbThreads, iterator, statusManager);
	}

	public void processSurfaces(long importId, int nbThreads, Iterator<Bodenbedeckung> iterator, @Nullable StatusManager statusManager) {
		surfaceAuSolRFDetector.processSurfaces(importId, nbThreads, iterator, statusManager);
	}
}
