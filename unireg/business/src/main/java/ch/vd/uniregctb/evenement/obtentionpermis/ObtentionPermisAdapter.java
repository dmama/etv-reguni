package ch.vd.uniregctb.evenement.obtentionpermis;

import java.util.Collection;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Adapter pour l'obtention de permis.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionPermisAdapter extends GenericEvenementAdapter implements ObtentionPermis {

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

	@Override
	public void init(EvenementCivilRegroupe evenementCivilRegroupe, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenementCivilRegroupe, serviceCivil, infrastructureService);

		try {
			// on récupère le permis (= à la date d'événement)
			final int anneeCourante = evenementCivilRegroupe.getDateEvenement().year();
			final Collection<Permis> listePermis = serviceCivil.getPermis(super.getIndividu().getNoTechnique(), anneeCourante);
			if (listePermis == null) {
				throw new EvenementAdapterException("Aucun permis trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (evenementCivilRegroupe.getDateEvenement().equals(permis.getDateDebutValidite())) {
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
			if (infrastructureService.estDansLeCanton(adressePrincipale)) {
				final CommuneSimple communePrincipale = infrastructureService.getCommuneByAdresse(adressePrincipale);
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
	public EnumTypePermis getTypePermis() {
		return permis.getTypePermis();
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}
}
