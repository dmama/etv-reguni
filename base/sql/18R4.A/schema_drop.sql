drop table VERSION_DB;

drop table EVENEMENT_FISCAL cascade constraints;

drop table ADRESSE_TIERS cascade constraints;

drop table ADRESSE_MANDATAIRE cascade constraints;

drop table AUDIT_LOG cascade constraints;

drop table BORDEREAU_MVT_DOSSIER cascade constraints;

drop table DOCUMENT_FISCAL cascade constraints;

drop table DELAI_DOCUMENT_FISCAL cascade constraints;

drop table DOC_INDEX cascade constraints;

drop table DROIT_ACCES cascade constraints;

drop table ETAT_DOCUMENT_FISCAL cascade constraints;

drop table EVENEMENT_CIVIL cascade constraints;

drop table EVENEMENT_CIVIL_ERREUR cascade constraints;

drop table EVENEMENT_CIVIL_ECH cascade constraints;

drop table EVENEMENT_CIVIL_ECH_ERREUR cascade constraints;

drop table EVENEMENT_ORGANISATION cascade constraints;

drop table EVENEMENT_ORGANISATION_ERREUR cascade constraints;

drop table EVENEMENT_EXTERNE cascade constraints;

drop table EVENEMENT_IDENTIFICATION_CTB cascade constraints;

drop table EVENEMENT_RF_IMPORT cascade constraints;

drop table EVENEMENT_RF_MUTATION cascade constraints;

drop table FOR_FISCAL cascade constraints;

drop table IDENTIFICATION_PERSONNE cascade constraints;

drop table IDENTIFICATION_ENTREPRISE cascade constraints;

drop table IMMEUBLE cascade constraints;

drop table MIGREG_ERROR cascade constraints;

drop table MODELE_DOCUMENT cascade constraints;

drop table MODELE_FEUILLE_DOC cascade constraints;

drop table MOUVEMENT_DOSSIER cascade constraints;

drop table PARAMETRE cascade constraints;

drop table PARAMETRE_PERIODE_FISCALE cascade constraints;

drop table PERIODE_FISCALE cascade constraints;

drop table PERIODICITE cascade constraints;

drop table RAPPORT_ENTRE_TIERS cascade constraints;

drop table REMARQUE cascade constraints;

drop table SITUATION_FAMILLE cascade constraints;

drop table TACHE cascade constraints;

drop table REQDES_ROLE_PARTIE_PRENANTE cascade constraints;

drop table REQDES_TRANSACTION_IMMOBILIERE cascade constraints;

drop table REQDES_PARTIE_PRENANTE cascade constraints;

drop table REQDES_ERREUR cascade constraints;

drop table REQDES_UNITE_TRAITEMENT cascade constraints;

drop table EVENEMENT_REQDES cascade constraints;

drop table DECISION_ACI cascade constraints;

drop table BOUCLEMENT cascade constraints;

drop table DOMICILE_ETABLISSEMENT cascade constraints;

drop table REGIME_FISCAL cascade constraints;

drop table DONNEE_CIVILE_ENTREPRISE cascade constraints;

drop table FLAG_ENTREPRISE cascade constraints;

drop table ETAT_ENTREPRISE cascade constraints;

drop table ALLEGEMENT_FISCAL cascade constraints;

drop table REFERENCE_ANNONCE_IDE cascade constraints;

drop table RAPPROCHEMENT_RF cascade constraints;

drop table ALLEGEMENT_FONCIER cascade constraints;

drop table RF_QUOTE_PART cascade constraints;
drop table RF_SURFACE_TOTALE cascade constraints;
drop table RF_SURFACE_AU_SOL cascade constraints;
drop table RF_SITUATION cascade constraints;
drop table RF_IMPLANTATION cascade constraints;
drop table RF_ESTIMATION cascade constraints;
drop table RF_RAISON_ACQUISITION cascade constraints;
drop table RF_SERVITUDE_AYANT_DROIT cascade constraints;
drop table RF_SERVITUDE_IMMEUBLE cascade constraints;
drop table RF_DROIT cascade constraints;
drop table RF_IMMEUBLE cascade constraints;
drop table RF_DESCRIPTION_BATIMENT cascade constraints;
drop table RF_BATIMENT cascade constraints;
drop table RF_AYANT_DROIT cascade constraints;
drop table RF_COMMUNE cascade constraints;
drop table RF_MEMBRE_COMMUNAUTE cascade constraints;
drop table RF_PRINCIPAL_COMMUNAUTE cascade constraints;
drop table RF_REGROUPEMENT_COMMUNAUTE cascade constraints;
drop table RF_MODELE_COMMUNAUTE cascade constraints;

drop table ETIQUETTE_TIERS cascade constraints;
drop table ETIQUETTE cascade constraints;
drop table DOCUMENT_EFACTURE cascade constraints;
drop table COORDONNEE_FINANCIERE cascade constraints;
drop table TIERS cascade constraints;

drop table MIGRATION_PM_MAPPING cascade constraints;

drop sequence S_MIGR_PM;


drop sequence S_PM;

drop sequence S_CAAC;

drop sequence S_DPI;

drop sequence S_CTB;

drop sequence S_ETB;

drop sequence hibernate_sequence;

