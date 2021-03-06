package ch.vd.unireg.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Classe abstraite de base des resolvers de mode d'imposition lors de la terminaison d'un couple
 * (divorce, séparation, décès...)
 */
public abstract class TerminaisonCoupleModeImpositionResolver extends ModeImpositionResolver {

	protected TerminaisonCoupleModeImpositionResolver(TiersService tiersService, AuditManager audit) {
		super(tiersService, audit);
	}

	/**
	 * Calcule du nouveau mode d'imposition pour le contribuable à la date et en prenant en compte son ancien mode d'imposition.
	 *
	 * @param contribuable le contribuable
	 * @param date date à partir de laquelle le mode d'imposition sera appliqué
	 * @param ancienModeImposition l'ancien mode d'imposition
	 * @param futurTypeAutoriteFiscale le type d'autorité fiscale du for qui sera ouvert
	 * @param hadForSecondaire <code>true</code> si le contribuable couple avait au moins un for secondaire encore ouvert au moment de la clôture
	 * @return le nouveau mode d'imposition
	 *
	 * @throws ModeImpositionResolverException en cas de souci
	 */
	public abstract Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition ancienModeImposition, TypeAutoriteFiscale futurTypeAutoriteFiscale, boolean hadForSecondaire) throws ModeImpositionResolverException;

}
