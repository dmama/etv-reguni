package ch.vd.uniregctb.efacture;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

public abstract class EFactureHelper {

	private static final EnumSet<ModeImposition> MODE_IMPOSITIONS_AUTORISES = EnumSet.of(ModeImposition.ORDINAIRE,
	                                                                                     ModeImposition.MIXTE_137_1,
	                                                                                     ModeImposition.MIXTE_137_2,
	                                                                                     ModeImposition.DEPENSE);

	private static final EnumSet<MotifFor> MOTIF_FORS_INTERDITS = EnumSet.of(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
	                                                                         MotifFor.VEUVAGE_DECES);

	/**
	 * @param tiers tiers dont on veut valider la compatibilité avec une inscription à la e-facture
	 * @return <code>true</code> si on peut entrer en matière pour la e-facture après analyse des fors/assujettissements, <code>false</code> sinon (-> demande de contact)
	 */
	public static boolean valideEtatFiscalContribuablePourInscription(@NotNull Tiers tiers) {
		// Verification du for principal
		final ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
		if (ffp == null) {
			return false;
		}

		if (ffp.getDateFin() != null && MOTIF_FORS_INTERDITS.contains(ffp.getMotifFermeture())) {
			return false;
		}

		return MODE_IMPOSITIONS_AUTORISES.contains(ffp.getModeImposition());
	}

}
