package ch.vd.uniregctb.efacture;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
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
		// TODO [SIPM] Pour le moment, ne pas être dans le camps des personnes physiques est rédhibitoire
		if (!(tiers instanceof ContribuableImpositionPersonnesPhysiques)) {
			return false;
		}

		final ContribuableImpositionPersonnesPhysiques ctb = (ContribuableImpositionPersonnesPhysiques) tiers;

		// Verification du for principal
		final ForFiscalPrincipalPP ffp = ctb.getDernierForFiscalPrincipal();
		if (ffp == null) {
			return false;
		}

		if (ffp.getDateFin() != null && MOTIF_FORS_INTERDITS.contains(ffp.getMotifFermeture())) {
			return false;
		}

		return MODE_IMPOSITIONS_AUTORISES.contains(ffp.getModeImposition());
	}

	// [SIFISC-12805] la valeur dans le champ noAvs peut être autre chose qu'un NAVS13... (on détecte le numéro AVS par sa longueur et le début à 756)
	public static boolean isNavs13(String noSecuOuAvs) {
		return noSecuOuAvs != null && noSecuOuAvs.length() == 13 && noSecuOuAvs.startsWith("756");
	}
}
