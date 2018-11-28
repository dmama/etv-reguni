package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.tiers.LienAssociesEtSNC;
import ch.vd.unireg.tiers.Tiers;

/**
 * Service autour de la gestion des rapports entre tiers et la SNC : import des liens entre tiers et la SNC en masse, édition des liens...
 */
public interface LienAssociesSNCService {

	LienAssociesSNCEnMasseImporterResults importLienAssociesSNCEnMasse(List<DonneesLienAssocieEtSNC> rapportEntreTiersSnc, final RegDate dateTraitement, StatusManager statusManager);

	/**
	 * Vérifie si le rapport entre tiers de type {@link LienAssociesEtSNC} est conforme aux conditions pour être ajouté.
	 *
	 * @param sujet     :Associe, commanditaire
	 * @param objet     : la SNC
	 * @param dateDebut : date de début du rapport
	 * @return Indique si le rapport vérifie les conditions d'ajout
	 */
	boolean isAllowed(Tiers sujet, Tiers objet, RegDate dateDebut) throws LienAssociesEtSNCException;
}
