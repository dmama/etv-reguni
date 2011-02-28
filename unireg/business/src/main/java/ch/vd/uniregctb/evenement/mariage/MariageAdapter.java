package ch.vd.uniregctb.evenement.mariage;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

/**
 * Modélise un événement conjugal (mariage, pacs)
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class MariageAdapter extends GenericEvenementAdapter implements Mariage {

	protected static Logger LOGGER = Logger.getLogger(MariageAdapter.class);

	/**
	 * Le nouveau conjoint de l'individu concerné par le mariage.
	 */
	private Individu nouveauConjoint;

	public MariageAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);

		/*
		 * Calcul de l'année où a eu lieu l'événement
		 */
		int anneeEvenement = getDate().year();

		/*
		 * Récupération des informations sur le conjoint de l'individu depuis le host.
		 * getIndividu().getConjoint() peut être null si mariage le 01.01
		 */
		final long noIndividu = getNoIndividu();
		Individu individuPrincipal = context.getServiceCivil().getIndividu(noIndividu, anneeEvenement, AttributeIndividu.CONJOINT);
		this.nouveauConjoint = getConjointValide(individuPrincipal, context.getServiceCivil());
		//this.nouveauConjoint = individuPrincipal.getConjoint();
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	/*l
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.mariage.Mariage#getNouveauConjoint()
	 */
	public Individu getNouveauConjoint() {
		return nouveauConjoint;
	}

	/**UNIREG-2055
	 * Cette méthode permet de vérifier que le conjoint trouvé pour l'individu principal a bien
	 * un état civil cohérent avec l'événement de mariage traité
	 *
	 * @param individuPrincipal
	 * @return le conjoint correct ou null si le conjoint trouvé n'a pas le bon état civil
	 */
	private Individu getConjointValide(Individu individuPrincipal,ServiceCivilService serviceCivil) {
		Individu conjointTrouve = serviceCivil.getConjoint(individuPrincipal.getNoTechnique(),getDate().getOneDayAfter());
		if (conjointTrouve!=null && isBonConjoint(individuPrincipal, conjointTrouve, serviceCivil)) {
			final EtatCivil etatCivilConjoint = serviceCivil.getEtatCivilActif(conjointTrouve.getNoTechnique(), getDate());
			//Si le conjoint n'a pas d'état civil ou son état civil est différent de marié, on renvoie null
			if (etatCivilConjoint!=null ) {
				if (TypeEtatCivil.MARIE == etatCivilConjoint.getTypeEtatCivil() || TypeEtatCivil.PACS == etatCivilConjoint.getTypeEtatCivil()) {
					return conjointTrouve;
				}

			}

		}
		return null;
	}


	private boolean isBonConjoint(Individu principal, Individu conjoint, ServiceCivilService serviceCivil){
		Individu principalAttendu = serviceCivil.getConjoint(conjoint.getNoTechnique(),getDate().getOneDayAfter());
		if (principalAttendu!=null && principal.getNoTechnique()== principalAttendu.getNoTechnique()) {
			return true;
		}
		return false;
	}

}
