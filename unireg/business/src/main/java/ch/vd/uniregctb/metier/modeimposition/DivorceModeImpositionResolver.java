package ch.vd.uniregctb.metier.modeimposition;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Resolver du mode d'imposition pour le cas séparation/divorce.
 *
 * @author Pavel BLANCO
 *
 */
public class DivorceModeImpositionResolver extends TiersModeImpositionResolver {

	private final Long numeroEvenement;
	
	public DivorceModeImpositionResolver(TiersService tiersService, Long numeroEvenement) {
		super(tiersService);
		this.numeroEvenement = numeroEvenement;
	}

	@Override
	public Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition imposition) throws ModeImpositionResolverException {

		Assert.isTrue(contribuable instanceof PersonnePhysique);
		
		PersonnePhysique contribuablePP = (PersonnePhysique) contribuable;
		
		Imposition result = new Imposition();
		result.setDateDebut(date);
		
		try {
			
			/**
			 * Le mode d'imposition du ou des contribuables imposés séparément est 
			 * déterminé selon le tableau suivant (SCU-EnregistrerSeparation.doc):
			 * 
			 * |==========================================================|
			 * | Mode original | Mode résultant                           |                         
			 * |===============+==========================================|
			 * | ORDINAIRE     | - Reste ORDINAIRE si le contribuable est |
			 * |               | suisse ou titulaire d'un permis C        |
			 * |               | - Passe a MIXTE 1 dans le cas contraire  |
			 * |---------------+------------------------------------------|
			 * | DEPENSE       | DEPENSE                                  |
			 * |---------------+------------------------------------------|
			 * | INDIGENT      | INDIGENT                                 |
			 * |---------------+------------------------------------------|
			 * | MIXTE 1       | MIXTE 1                                  |
			 * |---------------+------------------------------------------|
			 * | MIXTE 2       | MIXTE 2                                  |
			 * |---------------+------------------------------------------|
			 * | SOURCE        | SOURCE                                   |
			 * |---------------+------------------------------------------|
			 */
			switch (imposition) {
				case ORDINAIRE:
					if (getTiersService().isEtrangerSansPermisC(contribuablePP, date)) {
						result.setModeImposition(ModeImposition.MIXTE_137_1);
						Audit.info(numeroEvenement, String.format("Couple ordinaire : contribuable %1$s, étranger -> mixte 137 al. 1", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					}
					else {
						result.setModeImposition(ModeImposition.ORDINAIRE);
						Audit.info(numeroEvenement, String.format("Couple ordinaire : contribuable %1$s, suisse ou permis C -> ordinaire", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					}
					break;
				case DEPENSE:
					result.setModeImposition(ModeImposition.DEPENSE);
					Audit.info(numeroEvenement, String.format("Couple à la dépense : contribuable %1$s -> dépense", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
				case INDIGENT:
					result.setModeImposition(ModeImposition.INDIGENT);
					Audit.info(numeroEvenement, String.format("Couple indigent : contribuable %1$s -> indigent", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
				case MIXTE_137_1:
					result.setModeImposition(ModeImposition.MIXTE_137_1);
					Audit.info(numeroEvenement, String.format("Couple mixte 137 al. 1 : contribuable %1$s -> mixte 137 al. 1", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
				case MIXTE_137_2:
					result.setModeImposition(ModeImposition.MIXTE_137_2);
					Audit.info(numeroEvenement, String.format("Couple mixte 137 al. 2 : contribuable %1$s -> mixte 137 al. 2", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
				case SOURCE:
					result.setModeImposition(ModeImposition.SOURCE);
					Audit.info(numeroEvenement, String.format("Couple à la source : contribuable %1$s -> source", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
				
			}
			
		}
		catch (TiersException te) {
			throw new ModeImpositionResolverException("Impossible de déterminer le permis du contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()), te);
		}

		return result;
	}

}