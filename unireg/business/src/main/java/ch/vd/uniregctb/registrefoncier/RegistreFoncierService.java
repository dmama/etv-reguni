package ch.vd.uniregctb.registrefoncier;

import java.io.InputStream;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;

public interface RegistreFoncierService {

	/**
	 * Fait le rapprochement entre les contribuables et les données transmises par le registre foncier
	 *
	 * @param listeProprietaireFoncier La liste des propriétaires fonciers à rapprocher
	 * @param dateTraitement           TODO
	 * @param nbThreads
	 * @return
	 */
	RapprocherCtbResults rapprocherCtbRegistreFoncier(List<ProprietaireFoncier> listeProprietaireFoncier, StatusManager s, RegDate dateTraitement, int nbThreads);

	/**
	 * [SIFISC-2337] Supprime les immeubles existant et importe les immeubles contenus dans le stream CSV spécifié. Le format attendu du stream CSV est le suivant :
	 * <pre>
	 * "NO_CTB";"NOM";"PRENOM";"DATE_NAISSANCE";"GENRE_PERSONNE";"NO_IMMEUBLE";"NATURE";"GENRE";"ESTIMATION_FISCALE";"DATE_ESTIMATION_FISCALE";"ANCIENNE_ESTIMATION_FISCALE";"GENRE_PROPRIETE";"PART_PROPRIETE";"DATE_DEPOT_PJ";"DATE_VALIDATION_RF";"DATE_FIN";"URL"
	 * 18616706;"Blatter";"Madeleine";"11.10.1938";"P";"132/3129";"Place-jardin";"";1200000;"";"";1;"1/1";"09.01.2001";"06.02.2001";"";"url"
	 * 10403440;"Lyon";"Jean-Denis";"03.09.1937";"P";"132/154";"Place-jardin";"";336000;"05.09.2006";"";1;"1/1";"31.01.2006";"17.05.2006";"";"url"
	 * ...
	 * </pre>
	 *
	 * @param csvStream le flux CSV du fichier d'import
	 * @param encoding  l'encoding du fichier CSV
	 * @param status    un status manager  @return le résultat de l'import
	 * @return les résultats de l'importation.
	 */
	ImportImmeublesResults importImmeubles(InputStream csvStream, String encoding, StatusManager status);
}
