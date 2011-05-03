package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
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

	protected AnnulationPermis(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		try {
			final Collection<Permis> listePermis = super.getIndividu().getPermis();
			if (listePermis == null) {
				throw new EvenementCivilException("Aucun permis trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (getDate().equals(permis.getDateDebutValidite()) && permis.getDateAnnulation() != null) {
					this.typePermis = permis.getTypePermis();
					break;
				}
			}

			// si le permis n'a pas été trouvé, on lance une exception
			if ( this.typePermis == null ) {
				throw new EvenementCivilException("Aucun permis trouvé dans le registre civil");
			}
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
		super(individu, conjoint, TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, date, numeroOfsCommuneAnnonce, context);
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
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// rien à faire
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		// rien à faire
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		if (isAnnulationPermisC(this)) {
			return super.handle(warnings);
		}
		return null;
	}

	private boolean isAnnulationPermisC(AnnulationPermis annulationPermis) {
		return annulationPermis.getTypePermis() == TypePermis.ETABLISSEMENT;
	}
}
