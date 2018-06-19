package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;

/**
 * Service autour de la gestion des rapports entre tiers et la SNC : import des liens entre tiers et la SNC en masse, édition des liens...
 */
public interface LienAssociesSNCService {

	LienAssociesSNCEnMasseImporterResults importLienAssociesSNCEnMasse(List<DonneesLienAssocieEtSNC> rapportEntreTiersSnc, final RegDate dateTraitement, StatusManager statusManager);
}
