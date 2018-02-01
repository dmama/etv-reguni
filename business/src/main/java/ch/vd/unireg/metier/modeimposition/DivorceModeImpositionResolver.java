package ch.vd.unireg.metier.modeimposition;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Resolver du mode d'imposition pour le cas séparation/divorce.
 *
 * @author Pavel BLANCO
 *
 */
public class DivorceModeImpositionResolver extends TerminaisonCoupleModeImpositionResolver {

	private final Long numeroEvenement;
	
	public DivorceModeImpositionResolver(TiersService tiersService, Long numeroEvenement) {
		super(tiersService);
		this.numeroEvenement = numeroEvenement;
	}

	@Override
	public Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition ancienModeImposition, TypeAutoriteFiscale futurTypeAutoriteFiscale, boolean hadForSecondaire) throws ModeImpositionResolverException {

		Assert.isTrue(contribuable instanceof PersonnePhysique);
		
		final PersonnePhysique contribuablePP = (PersonnePhysique) contribuable;
		final Imposition result = new Imposition();
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
			 * |               | (ou SOURCE si HC/HS)                     |
			 * |---------------+------------------------------------------|
			 * | DEPENSE       | DEPENSE                                  |
			 * |---------------+------------------------------------------|
			 * | INDIGENT      | INDIGENT                                 |
			 * |---------------+------------------------------------------|
			 * | MIXTE 1       | MIXTE 1 (ou SOURCE si HC/HS)             |
			 * |---------------+------------------------------------------|
			 * | MIXTE 2       | MIXTE 2 (ou SOURCE si HC/HS)             |
			 * |---------------+------------------------------------------|
			 * | SOURCE        | SOURCE                                   |
			 * |---------------+------------------------------------------|
			 */
			switch (ancienModeImposition) {
				case ORDINAIRE:
					if (getTiersService().isEtrangerSansPermisC(contribuablePP, date)) {
						final ModeImposition nveauMode = normaliseModeImpositionMixte(ModeImposition.MIXTE_137_1, futurTypeAutoriteFiscale, hadForSecondaire, numeroEvenement,
						                                                              String.format("Couple ordinaire : contribuable %s, étranger", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
						result.setModeImposition(nveauMode);
					}
					else {
						result.setModeImposition(ModeImposition.ORDINAIRE);
						Audit.info(numeroEvenement, String.format("Couple ordinaire : contribuable %s, suisse ou permis C -> ordinaire", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					}
					break;
				case DEPENSE:
					result.setModeImposition(ModeImposition.DEPENSE);
					Audit.info(numeroEvenement, String.format("Couple à la dépense : contribuable %s -> dépense", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
				case INDIGENT:
					result.setModeImposition(ModeImposition.INDIGENT);
					Audit.info(numeroEvenement, String.format("Couple indigent : contribuable %s -> indigent", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
				case MIXTE_137_1:
				case MIXTE_137_2:
					{
						final ModeImposition nveauMode = normaliseModeImpositionMixte(ancienModeImposition, futurTypeAutoriteFiscale, hadForSecondaire, numeroEvenement,
						                                                              String.format("Couple %s : contribuable %s", ancienModeImposition.texte(), FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
						result.setModeImposition(nveauMode);
					}
					break;
				case SOURCE:
					result.setModeImposition(ModeImposition.SOURCE);
					Audit.info(numeroEvenement, String.format("Couple à la source : contribuable %s -> source", FormatNumeroHelper.numeroCTBToDisplay(contribuablePP.getNumero())));
					break;
			}
			
		}
		catch (TiersException te) {
			throw new ModeImpositionResolverException("Impossible de déterminer le permis du contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()), te);
		}

		return result;
	}

	private static ModeImposition normaliseModeImpositionMixte(ModeImposition modeImposition, TypeAutoriteFiscale typeAutoriteFiscale, boolean hasForSecondaire, Long numeroEvenement, String auditPrefixe) {
		if (modeImposition != ModeImposition.MIXTE_137_1 && modeImposition != ModeImposition.MIXTE_137_2) {
			throw new IllegalArgumentException("Mode d'imposition non mixte : " + modeImposition);
		}
		final ModeImposition normalise = typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
				? modeImposition
				: (hasForSecondaire ? ModeImposition.ORDINAIRE : ModeImposition.SOURCE);
		Audit.info(numeroEvenement, String.format("%s (%s) -> %s", auditPrefixe, typeAutoriteFiscale, normalise.texte()));
		return normalise;
	}
}