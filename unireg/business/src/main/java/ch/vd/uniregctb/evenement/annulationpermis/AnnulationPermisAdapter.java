package ch.vd.uniregctb.evenement.annulationpermis;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Adapter pour l'annulation de l'obtention de permis.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationPermisAdapter extends GenericEvenementAdapter implements AnnulationPermis {

	private static final Log LOGGER = LogFactory.getLog(AnnulationPermisHandler.class);
	
	/** Le permis obtenu. */
	private Permis permis;

	protected AnnulationPermisAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);

		try {
			final Collection<Permis> listePermis = super.getIndividu().getPermis();
			if (listePermis == null) {
				throw new EvenementAdapterException("Aucun permis trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (getDate().equals(permis.getDateDebutValidite()) && permis.getDateAnnulation() != null) {
					this.permis = permis;
					break;
				}
			}

			// si le permis n'a pas été trouvé, on lance une exception
			if ( this.permis == null ) {
				throw new EvenementAdapterException("Aucun permis trouvé dans le registre civil");
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EvenementAdapterException(e.getMessage(), e);
		}
	}

	public TypePermis getTypePermis() {
		return permis.getTypePermis();
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}
}
