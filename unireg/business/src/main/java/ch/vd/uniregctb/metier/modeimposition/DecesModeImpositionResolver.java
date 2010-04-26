package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Résolution du nouveau mode d'imposition pour le cas décès.
 *
 * @author Pavel BLANCO
 *
 */
public class DecesModeImpositionResolver extends TiersModeImpositionResolver {

	private final Long numeroEvenement;

	public DecesModeImpositionResolver(TiersService tiersService, Long numeroEvenement) {
		super(tiersService);
		this.numeroEvenement = numeroEvenement;
	}

	public Imposition resolve(Contribuable survivant, RegDate date, ModeImposition impositionCouple) throws ModeImpositionResolverException {

		final Imposition result = new Imposition();
		result.setDateDebut(date);

		// la spec dit:
		// - si couple à la dépense, alors:
		//		- si survivant suisse -> ordinaire
		//		- si survivant étranger -> dépense
		// - si couple à l'ordinaire, alors:
		//		- si survivant suisse ou permis C -> ordinaire
		//		- si survivant étranger non permis C -> mixte 137 al. 2
		// - si couple indigent, alors survivant passe à ordinaire
		if (impositionCouple.equals(ModeImposition.DEPENSE)) {
			try {
				if (getTiersService().isSuisse((PersonnePhysique) survivant, date)) {
					result.setModeImposition(ModeImposition.ORDINAIRE);
					Audit.info(numeroEvenement, "Conjoint suisse : le nouveau for fiscal aura le rôle ordinaire");
				}
				else {
					result.setModeImposition(ModeImposition.DEPENSE);
					Audit.info(numeroEvenement, "Conjoint étranger : le nouveau for fiscal aura le rôle dépense");
				}
			}
			catch (TiersException e) {
				throw new ModeImpositionResolverException("Impossible de déterminer la nationalité du contribuable n°" + FormatNumeroHelper.numeroCTBToDisplay(survivant.getNumero()), e);
			}
		}
		else if (impositionCouple.equals(ModeImposition.ORDINAIRE)) {
			try {
				if (getTiersService().isEtrangerSansPermisC((PersonnePhysique) survivant, date)) {
					result.setModeImposition(ModeImposition.MIXTE_137_2);
					Audit.info(numeroEvenement, "Conjoint étranger sans permis C : le nouveau for fiscal aura le rôle mixte 137 al. 2");
				}
				else {
					result.setModeImposition(ModeImposition.ORDINAIRE);
					Audit.info(numeroEvenement, "Conjoint suisse ou permis C : le nouveau for fiscal aura le rôle ordinaire");
				}
			}
			catch (TiersException e) {
				throw new ModeImpositionResolverException("Impossible de déterminer le permis du contribuable n°" + FormatNumeroHelper.numeroCTBToDisplay(survivant.getNumero()), e);
			}
		}
		else if(impositionCouple.equals(ModeImposition.INDIGENT)) {
			Audit.info(numeroEvenement, "Le nouveau for fiscal principal du conjoint aura le rôle ordinaire");
			result.setModeImposition(ModeImposition.ORDINAIRE);
		}
		else {//on conserve le mode d'imposition
			Audit.info(numeroEvenement, "Le nouveau for fiscal principal du conjoint aura le même rôle");
			result.setModeImposition(impositionCouple);
		}

		return result;
	}

}