package ch.vd.uniregctb.migration.pm;

import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;

public interface Graphe {

	/**
	 * @return les entreprises du graphe, indexées par leur identifiant RegPM
	 */
	Map<Long, RegpmEntreprise> getEntreprises();

	/**
	 * @return les établissements du graphe, indexés par leur identifiant RegPM
	 */
	Map<Long, RegpmEtablissement> getEtablissements();

	/**
	 * @return les individus du graphe, indexés par leur identifiant RegPM
	 */
	Map<Long, RegpmIndividu> getIndividus();

}
