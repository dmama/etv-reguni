package ch.vd.uniregctb.evenement.obtentionpermis;

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.evenement.EvenementAdapterAvecAdresses;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Adapter pour l'obtention de permis.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionPermisAdapter extends EvenementAdapterAvecAdresses implements ObtentionPermis {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(ObtentionPermisAdapter.class);

	/**
	 * Le permis obtenu.
	 */
	private Permis permis;

	/**
	 * le numero OFS étendu de la commune vaudoise de l'adresse principale
	 */
	private Integer numeroOfsEtenduCommunePrincipale;

	protected ObtentionPermisAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);

		try {
			// on récupère le permis (= à la date d'événement)
			final int anneeCourante = evenement.getDateEvenement().year();
			final Collection<Permis> listePermis = context.getServiceCivil().getPermis(super.getNoIndividu(), anneeCourante);
			if (listePermis == null) {
				throw new EvenementAdapterException("Aucun permis trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (evenement.getDateEvenement().equals(permis.getDateDebutValidite())) {
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

		try {
			// on récupère la commune de l'adresse principale en gérant les fractions
			// à utiliser pour déterminer le numeroOFS si besoin d'ouvrir un nouveau for vaudois
			final Adresse adressePrincipale = getAdressePrincipale();
			if (context.getServiceInfra().estDansLeCanton(adressePrincipale)) {
				final CommuneSimple communePrincipale = context.getServiceInfra().getCommuneByAdresse(adressePrincipale);
				if (communePrincipale == null) {
					throw new EvenementAdapterException("Incohérence dans l'adresse principale");
				}
				this.numeroOfsEtenduCommunePrincipale = communePrincipale.getNoOFSEtendu();
			}
			else {
				this.numeroOfsEtenduCommunePrincipale = 0;
			}
		}
		catch (InfrastructureException e) {
			throw new EvenementAdapterException("Echec de résolution de l'adresse principale.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.obtentionpermis.ObtentionPermis#getTypePermis()
	 */
	public TypePermis getTypePermis() {
		return permis.getTypePermis();
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}
}
