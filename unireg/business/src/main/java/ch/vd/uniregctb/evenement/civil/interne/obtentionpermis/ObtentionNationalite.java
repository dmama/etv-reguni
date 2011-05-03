package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour l'obtention de nationalité.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionNationalite extends ObtentionPermisCOuNationaliteSuisse {

	/**
	 * LOGGER log4J
	 */
	protected static Logger LOGGER = Logger.getLogger(ObtentionNationalite.class);

	/**
	 * le numero OFS étendu de la commune de l'adresse principale
	 */
	private Integer numeroOfsEtenduCommunePrincipale;

	protected ObtentionNationalite(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		try {
			// on récupère la commune de l'adresse principale en gérant les fractions
			//à utiliser pour déterminer le numeroOFS si besoin d'ouvrir un nouveau for
			final Commune communePrincipale = context.getServiceInfra().getCommuneByAdresse(getAdressePrincipale(), evenement.getDateEvenement());
			this.numeroOfsEtenduCommunePrincipale = communePrincipale == null ? 0 : communePrincipale.getNoOFSEtendu();
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ObtentionNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsEtenduCommunePrincipale, boolean nationaliteSuisse,
	                               EvenementCivilContext context) {
		super(individu, conjoint, nationaliteSuisse ? TypeEvenementCivil.NATIONALITE_SUISSE : TypeEvenementCivil.NATIONALITE_NON_SUISSE, date, numeroOfsCommuneAnnonce, null, null, null, context);
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// Obsolète dans cet handler, l'obtention de nationalité est un événement ne concernant qu'un seul individu.
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		if (TypeEvenementCivil.NATIONALITE_SUISSE == getType()) {
			super.validateSpecific(erreurs, warnings);
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		// quelque soit la nationalité, si l'individu correspond à un non-habitant (= ancien habitant)
		// il faut mettre à jour la nationalité chez nous
		final PersonnePhysique pp = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());
		if (pp != null && !pp.isHabitantVD()) {
			if (getType() == TypeEvenementCivil.NATIONALITE_SUISSE) {
				pp.setNumeroOfsNationalite(ServiceInfrastructureService.noOfsSuisse);
			}
			else {
				for (Nationalite nationalite : getIndividu().getNationalites()) {
					if (getDate().equals(nationalite.getDateDebutValidite())) {
						pp.setNumeroOfsNationalite(nationalite.getPays().getNoOFS());
						Audit.info(getNumeroEvenement(), String.format("L'individu %d (tiers non-habitant %s) a maintenant la nationalité du pays '%s'",
								getNoIndividu(), FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), nationalite.getPays().getNomMinuscule()));
						break;
					}
				}
			}
		}

		switch (getType()) {
			case NATIONALITE_SUISSE:
				return super.handle(warnings);

			case NATIONALITE_NON_SUISSE:
				/* Seul l'obtention de nationalité suisse est traitée */
				Audit.info(getNumeroEvenement(), "Nationalité non suisse : ignorée fiscalement");
				break;

			default:
				Assert.fail();
		}

		return null;
	}
}
