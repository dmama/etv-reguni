package ch.vd.unireg.evenement.civil.interne.annulationpermis;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.type.TypePermis;

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
			final Permis permis = getIndividuOrThrowException().getPermis().getPermisAnnule(evenement.getDateEvenement());
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

	protected AnnulationPermis(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options, TypePermis typePermis) throws EvenementCivilException {
		super(evenement, context, options);
		this.typePermis = typePermis;
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
		super.validateSpecific(erreurs,warnings);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if (isAnnulationPermisC()) {
			return super.handle(warnings);
		}
		return HandleStatus.TRAITE;
	}

	private boolean isAnnulationPermisC() {
		return getTypePermis() == TypePermis.ETABLISSEMENT;
	}
}
