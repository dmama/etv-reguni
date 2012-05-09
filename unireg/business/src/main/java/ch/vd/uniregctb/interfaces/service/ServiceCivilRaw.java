package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;

public interface ServiceCivilRaw {

	public static final String SERVICE_NAME = "ServiceCivil";

	/**
	 * Retourne l'individu, valide <b>jusqu'à</b> l'année en paramètre, identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants :
	 * <ul>
	 * <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <li>La liste des conjoints l'individu.</li>
	 * </ul>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param date       la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée.
	 */
	Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties);

	/**
	 * Retourne un lot d'individu avec les parties spécifiées.
	 * <p/>
	 * <b>Attention !</b> L'ordre des individus retourné ne correspond pas forcément à celui des numéros d'individu spécifiés.
	 *
	 * @param nosIndividus les numéros d'individus demandés
	 * @param date         la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return la liste des individus trouvés, ou <b>null</b> si le service n'est pas capable de charger les individus par lots.
	 */
	List<Individu> getIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties);

	/**
	 * @return <b>vrai</b> si l'implémentation courante du service civil possède un cache et que ce cache est susceptible d'être chauffé avec un appel à getIndividus().
	 */
	boolean isWarmable();

	/**
	 * Active ou non le dump des individus récupérés dans le registre civil pour le thread courant.
	 *
	 * @param value <b>vrai</b> si le dump est activé; <b>faux</b> s'il n'est pas activé.
	 */
	void setIndividuLogger(boolean value);
}
