package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Adapter pour l'annulation de l'obtention de permis.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationPermis extends AnnulationPermisCOuNationaliteSuisse {

	private static final Log LOGGER = LogFactory.getLog(AnnulationPermisTranslationStrategy.class);

	/** Le type de permis obtenu. */
	private TypePermis typePermis;

	protected AnnulationPermis(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		try {
			final Permis permis = context.getServiceCivil().getPermis(super.getNoIndividu(), evenement.getDateEvenement());
			if (permis == null || permis.getDateAnnulation() == null) {
				throw new EvenementCivilException("Aucun permis annulé n'a été trouvé dans le registre civil");
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
	protected AnnulationPermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, TypePermis typePermis, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.typePermis = typePermis;
	}

	public TypePermis getTypePermis() {
		return typePermis;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// rien à faire
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if (isAnnulationPermisC(this)) {
			return super.handle(warnings);
		}
		return null;
	}

	private boolean isAnnulationPermisC(AnnulationPermis annulationPermis) {
		return annulationPermis.getTypePermis() == TypePermis.ETABLISSEMENT;
	}
}
