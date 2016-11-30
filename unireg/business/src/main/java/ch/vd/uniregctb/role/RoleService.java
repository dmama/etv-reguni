package ch.vd.uniregctb.role;

import org.jetbrains.annotations.Nullable;

import ch.vd.shared.batchtemplate.StatusManager;

public interface RoleService {

	/**
	 * Extraction du rôle communal des personnes physiques
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param ofsCommune [optionel] numéro OFS de la commune spécifique pour laquelle est tiré le rôle
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	RolePPCommunesResults produireRolePPCommunes(int annee, int nbThreads, @Nullable Integer ofsCommune, @Nullable StatusManager statusManager);

	/**
	 * Extraction du rôle communal (par office d'impôt) des personnes physiques
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param oid [optionel] numéro de collectivité administrative de l'OID concerné
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	RolePPOfficesResults produireRolePPOffices(int annee, int nbThreads, @Nullable Integer oid, @Nullable StatusManager statusManager);

	/**
	 * Extraction du rôle communal des personnes morales
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param ofsCommune [optionel] numéro OFS de la commune spécifique pour laquelle est tiré le rôle
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	RolePMCommunesResults produireRolePMCommunes(int annee, int nbThreads, @Nullable Integer ofsCommune, @Nullable StatusManager statusManager);

	/**
	 * Extraction du rôle complet des personnes morales (une seule liste pour l'OIPM)
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	RolePMOfficeResults produireRolePMOffice(int annee, int nbThreads, @Nullable StatusManager statusManager);
}
