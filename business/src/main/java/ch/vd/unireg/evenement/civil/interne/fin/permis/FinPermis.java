package ch.vd.unireg.evenement.civil.interne.fin.permis;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.TypePermis;

/**
 * Adapter pour la fin obtention d'un permis.
 *
 * @author Pavel BLANCO
 *
 */
public class FinPermis extends EvenementCivilInterne {

	private static final Logger LOGGER = LoggerFactory.getLogger(FinPermis.class);

	private TypePermis typePermis;

	protected FinPermis(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		try {
			// on récupère le permis à partir de sa date de fin (= à la date d'événement)
			final Permis permis = getIndividuOrThrowException().getPermis().getPermisActif(evenement.getDateEvenement());
			if (permis == null || permis.getDateFin() != evenement.getDateEvenement()) {
				throw new EvenementCivilException("Le permis n'a pas été trouvé dans le registre civil");
			}
			this.typePermis = permis.getTypePermis();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EvenementCivilException(e.getMessage(), e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected FinPermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, TypePermis typePermis, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.typePermis = typePermis;
	}

	public TypePermis getTypePermis() {
		return typePermis;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/* Seulement le permis C est traité */
		if (getTypePermis() == TypePermis.ETABLISSEMENT) {

			PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(getNoIndividu(), erreurs);
			if (habitant == null) {
				return;
			}

			boolean isSuisse = false;
			// vérification si la nationalité suisse a été obtenue
			try {
				isSuisse = context.getTiersService().isSuisse(habitant, getDate().getOneDayAfter());
			}
			catch (Exception e) {
				erreurs.addErreur(e);
				return;
			}

			if (isSuisse) {
				context.audit.info(getNumeroEvenement(), "Permis C : l'habitant a obtenu la nationalité suisse, rien à faire");
			}
			else {
				context.audit.info(getNumeroEvenement(), "Permis C : l'habitant n'a pas obtenu la nationalité suisse, passage en traitement manuel");
				erreurs.addErreur("La fin du permis C doit être traitée manuellement");
			}
		}
		else {
			context.audit.info(getNumeroEvenement(), String.format("Permis non C (%s) : ignoré", getTypePermis()));
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// rien à faire tout ce passe dans le validateSpecific
		return HandleStatus.TRAITE;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		parts.add(AttributeIndividu.PERMIS);
	}
}
