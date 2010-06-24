package ch.vd.uniregctb.evenement.mariage;

import org.apache.log4j.Logger;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

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

	/**
	 * Récupère la commune à partir de l'adresse principale
	 * @throws EvenementAdapterException
	 */
	@Override
	public void init(EvenementCivilData evenementCivil, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenementCivil, serviceCivil, infrastructureService);

		/*
		 * Calcul de l'année où a eu lieu l'événement
		 */
		int anneeEvenement = getDate().year();

		/*
		 * Récupération des informations sur le conjoint de l'individu depuis le host.
		 * getIndividu().getConjoint() peut être null si mariage le 01.01
		 */
		EnumAttributeIndividu[] attributs = { EnumAttributeIndividu.CONJOINT };
		final long noIndividu = getNoIndividu();
		Individu individuPrincipal = serviceCivil.getIndividu(noIndividu, anneeEvenement, attributs);
		this.nouveauConjoint = getConjointValide(individuPrincipal,serviceCivil);
		//this.nouveauConjoint = individuPrincipal.getConjoint();
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
	 * un état civil cohérent avec l'évènement de mariage traité
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
				if (EnumTypeEtatCivil.MARIE.equals(etatCivilConjoint.getTypeEtatCivil()) || EnumTypeEtatCivil.PACS.equals(etatCivilConjoint.getTypeEtatCivil())) {
					return conjointTrouve ;
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
