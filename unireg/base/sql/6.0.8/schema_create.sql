-- table VERSION
CREATE TABLE VERSION_DB (VERSION_NB NVARCHAR2(10) NOT NULL, SCRIPT_ID NVARCHAR2(50) NOT NULL, TS TIMESTAMP DEFAULT SYSDATE);
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.8', 'create');

create table ADRESSE_TIERS (ADR_TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0), USAGE_TYPE nvarchar2(14) not null, AUTRE_TIERS_ID number(19,0), AUTRE_TYPE nvarchar2(14), STYPE nvarchar2(10), COMPLEMENT nvarchar2(100), NUMERO_APPARTEMENT nvarchar2(35), NUMERO_CASE_POSTALE number(10,0), NUMERO_MAISON nvarchar2(35), PERMANENTE number(1,0), RUE nvarchar2(100), TEXTE_CASE_POSTALE nvarchar2(15), COMPLEMENT_LOCALITE nvarchar2(100), NUMERO_OFS_PAYS number(10,0), NUMERO_POSTAL_LOCALITE nvarchar2(35), TYPE_PM nvarchar2(11), NPA_CASE_POSTALE number(10,0), NUMERO_ORDRE_POSTE number(10,0), NUMERO_RUE number(10,0), TIERS_ID number(19,0) not null, primary key (id));

create table AUDIT_LOG (id number(19,0) not null, LOG_DATE timestamp, DOC_ID number(19,0), EVT_ID number(19,0), LOG_LEVEL nvarchar2(7), MESSAGE nvarchar2(255), THREAD_ID number(19,0), LOG_USER nvarchar2(255), primary key (id));

create table BORDEREAU_MVT_DOSSIER (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), primary key (id));

create table DECLARATION (DOCUMENT_TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0) not null, CODE_CONTROLE nvarchar2(6), CODE_SEGMENT number(10,0), DATE_IMPR_CHEMISE_TO timestamp, DELAI_RETOUR_IMPRIME number(10,0), LIBRE number(1,0), NUMERO number(10,0), NO_OFS_FOR_GESTION number(10,0), QUALIFICATION nvarchar2(16), RETOUR_COLL_ADMIN_ID number(19,0), TYPE_CTB nvarchar2(17), MODE_COM nvarchar2(12), PERIODICITE nvarchar2(11), SANS_RAPPEL number(1,0), MODELE_DOC_ID number(19,0), PERIODE_ID number(19,0) not null, DATE_DEBUT_EXERCICE NUMBER(10, 0), DATE_FIN_EXERCICE NUMBER(10, 0), TIERS_ID number(19,0) not null, primary key (id));

create table DELAI_DECLARATION (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEMANDE number(10,0), DATE_TRAITEMENT number(10,0), DELAI_ACCORDE_AU number(10,0), ETAT nvarchar2(10) not null, SURSIS number(1,0) not null, CLE_ARCHIVAGE_COURRIER nvarchar2(40), DECLARATION_ID number(19,0) not null, primary key (id));

create table DOC_INDEX (DOC_TYPE nvarchar2(50) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DESCRIPTION nvarchar2(255), FILE_EXT nvarchar2(255) not null, FILE_NAME nvarchar2(255) not null, FILE_SIZE number(19,0) not null, NOM nvarchar2(100) not null, SUB_PATH nvarchar2(255) not null, NB_TIERS number(10,0), primary key (id));

create table DROIT_ACCES (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0), NIVEAU nvarchar2(255) not null, NUMERO_IND_OPER number(19,0) not null, TYPE nvarchar2(255) not null, TIERS_ID number(19,0) not null, primary key (id));

create table ETAT_DECLARATION (TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_OBTENTION number(10,0), DATE_ENVOI_COURRIER number(10,0), SOURCE nvarchar2(255), DECLARATION_ID number(19,0) not null, primary key (id));

create table EVENEMENT_CIVIL (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), COMMENTAIRE_TRAITEMENT nvarchar2(255), DATE_EVENEMENT number(10,0), DATE_TRAITEMENT timestamp, ETAT nvarchar2(10), NO_INDIVIDU_CONJOINT number(19,0), NO_INDIVIDU_PRINCIPAL number(19,0), NUMERO_OFS_ANNONCE number(10,0), TYPE nvarchar2(45), primary key (id));

create table EVENEMENT_CIVIL_ECH (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), ACTION_EVT nvarchar2(20) not null, COMMENTAIRE_TRAITEMENT nvarchar2(255), DATE_EVENEMENT number(10,0) not null, DATE_TRAITEMENT timestamp, ETAT nvarchar2(10) not null, NO_INDIVIDU number(19,0), REF_MESSAGE_ID number(19,0), TYPE nvarchar2(40) not null, primary key (id));

create table EVENEMENT_CIVIL_ERREUR (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), CALLSTACK nvarchar2(2000), MESSAGE nvarchar2(1024), TYPE nvarchar2(7) not null, EVT_CIVIL_ID number(19,0) not null, primary key (id));

create table EVENEMENT_CIVIL_ECH_ERREUR (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), CALLSTACK nvarchar2(2000), MESSAGE nvarchar2(1024), TYPE nvarchar2(7) not null, EVT_CIVIL_ID number(19,0) not null, primary key (id));

create table EVENEMENT_ORGANISATION (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), COMMENTAIRE_TRAITEMENT nvarchar2(255), DATE_EVENEMENT number(10,0) not null, DATE_TRAITEMENT timestamp, ETAT nvarchar2(10) not null, NO_ORGANISATION number(19,0) not null, TYPE nvarchar2(120) not null, primary key (id));

create table EVENEMENT_ORGANISATION_ERREUR (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), CALLSTACK nvarchar2(2000), MESSAGE nvarchar2(1024), TYPE nvarchar2(7) not null, EVT_ORGANISATION_ID number(19,0) not null, LIST_INDEX number(19,0) not null, primary key (id));

create table EVENEMENT_EXTERNE (EVENT_TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), CORRELATION_ID nvarchar2(255) not null, DATE_EVENEMENT date, DATE_TRAITEMENT date, ERREUR_MESSAGE nvarchar2(255), ETAT nvarchar2(10), MESSAGE clob, QLR_DATE_DEBUT number(10,0), QLR_DATE_FIN number(10,0), QLR_TYPE nvarchar2(13), TIERS_ID number(19,0), primary key (id));

-- les attributs temporels spécifiques aux entreprises (utilité publique, société immobilière...)
CREATE TABLE FLAG_ENTREPRISE (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10,0) NOT NULL,
	DATE_FIN NUMBER(10,0),
	FLAG NVARCHAR2(31) NOT NULL,
	ENTREPRISE_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);

CREATE TABLE EVENEMENT_FISCAL (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	EVT_TYPE NVARCHAR2(29) NOT NULL,
	DATE_VALEUR NUMBER(10,0) NOT NULL,
	TIERS_ID NUMBER(19,0) NOT NULL,
	PARENTE_ENFANT_ID NUMBER(19,0),
	TYPE_EVT_PARENTE NVARCHAR2(25),
	FOR_FISCAL_ID NUMBER(19,0),
	TYPE_EVT_FOR NVARCHAR2(20),
	ALLEGEMENT_ID NUMBER(19,0),
	TYPE_EVT_ALLEGEMENT NVARCHAR2(15),
	TYPE_EVT_INFO_COMPL NVARCHAR2(60),
	DECLARATION_ID NUMBER(19,0),
	TYPE_EVT_DECLARATION NVARCHAR2(15),
	REGIME_FISCAL_ID NUMBER(19,0),
	TYPE_EVT_REGIME NVARCHAR2(15),
	FLAG_ENTREPRISE_ID NUMBER(19,0),
	TYPE_EVT_FLAG NVARCHAR2(15),
	PRIMARY KEY (ID)
);

-- Autres documents fiscaux
CREATE TABLE AUTRE_DOCUMENT_FISCAL(
	ID NUMBER(19,0) NOT NULL,
	DOC_TYPE NVARCHAR2(25) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_ENVOI NUMBER(10,0) NOT NULL,
	DELAI_RETOUR NUMBER(10,0),
	DATE_RETOUR NUMBER(10,0),
	DATE_RAPPEL NUMBER(10,0),
	LB_TYPE NVARCHAR2(20),
	ENTREPRISE_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);

create table EVENEMENT_IDENTIFICATION_CTB (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_TRAITEMENT timestamp, DATE_DEMANDE timestamp, EMETTEUR_ID nvarchar2(50) not null, MESSAGE_ID nvarchar2(255) not null, MODE_IDENTIFICATION nvarchar2(255) not null, MONTANT number(19,0), PERIODE_FISCALE number(10,0) not null, NAVS11 nvarchar2(11), NAVS13 nvarchar2(13), ADR_CH_COMPL nvarchar2(2), ADR_CODE_PAYS nvarchar2(12), ADR_LIEU nvarchar2(40), ADR_LIGNE_1 nvarchar2(60), ADR_LIGNE_2 nvarchar2(60), ADR_LOCALITE nvarchar2(40), ADR_NO_APPART nvarchar2(10), ADR_ORDRE_POSTE number(10,0), ADR_NO_POLICE nvarchar2(12), ADR_NPA_ETRANGER nvarchar2(15), ADR_NPA_SUISSE number(10,0), ADR_NO_CP number(10,0), ADR_RUE nvarchar2(60), ADR_TEXT_CP nvarchar2(15), ADR_TYPE nvarchar2(255), DATE_NAISSANCE number(10,0), NOM nvarchar2(100), PRENOMS nvarchar2(100), SEXE nvarchar2(8), PRIO_EMETTEUR nvarchar2(255) not null, PRIO_UTILISATEUR number(10,0) not null, TRANSMETTEUR nvarchar2(255), DEMANDE_TYPE nvarchar2(30), TYPE_MESSAGE nvarchar2(20) not null, ETAT nvarchar2(23) not null, BUSINESS_ID nvarchar2(255) not null, BUSINESS_USER nvarchar2(255) not null, DOCUMENT_URL nvarchar2(255), META_DATA NVARCHAR2(1024), REPLY_TO nvarchar2(255) not null, NB_CTB_TROUVES number(10,0), DATE_REPONSE timestamp, ATTENTE_IDENTIF_MANUEL number(1,0), ERREUR_CODE nvarchar2(20), ERREUR_MESSAGE nvarchar2(1000), ERREUR_TYPE nvarchar2(9), NO_CONTRIBUABLE number(19,0), NO_MENAGE_COMMUN number(19,0), TRAITEMENT_USER nvarchar2(65), WORK_USER nvarchar2(65), NAVS13_UPI nvarchar2(13), COMMENTAIRE_TRAITEMENT nvarchar2(255), primary key (id));

create table FOR_FISCAL (FOR_TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_OUVERTURE number(10,0) not null, DATE_FERMETURE number(10,0), GENRE_IMPOT nvarchar2(29) not null, NUMERO_OFS number(10,0) not null, TYPE_AUT_FISC nvarchar2(22) not null, MOTIF_FERMETURE nvarchar2(49), MOTIF_OUVERTURE nvarchar2(49), MOTIF_RATTACHEMENT nvarchar2(26), MODE_IMPOSITION nvarchar2(11), TIERS_ID number(19,0) not null, primary key (id));

create table IDENTIFICATION_PERSONNE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), CATEGORIE nvarchar2(10), IDENTIFIANT nvarchar2(13), NON_HABITANT_ID number(19,0) not null, primary key (id));

create table IMMEUBLE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0), DATE_DERNIERE_MUTATION number(10,0), DATE_FIN number(10,0), DATE_VALID_RF number(10,0), DERNIERE_MUTATION nvarchar2(28), ESTIMATION_FISCALE number(10,0), GENRE_PROPRIETE nvarchar2(12) not null, ID_RF nvarchar2(40) not null, LIEN_RF nvarchar2(500), NATURE_IMMEUBLE nvarchar2(255), NOM_COMMUNE nvarchar2(255) not null, NUMERO_IMMEUBLE nvarchar2(20) not null, PART_PROPRIETE_DENOMINATEUR number(10,0) not null, PART_PROPRIETE_NUMERATEUR number(10,0) not null, ID_PROPRIETAIRE_RF nvarchar2(40) not null, NUMERO_INDIVIDU_RF number(19,0) not null, REF_ESTIM_FISC nvarchar2(255), TYPE_IMMEUBLE nvarchar2(27) not null, CTB_ID number(19,0) not null, primary key (id));

create table MIGREG_ERROR (id number(19,0) not null, FOR_PRINCIPAL_CTB nvarchar2(255), LIBELLE_MESSAGE nvarchar2(255), NO_CONTRIBUABLE number(19,0) unique, NO_INDIVIDU number(10,0) unique, NOM_CONTRIBUABLE nvarchar2(255), NOM_INDIVIDU nvarchar2(255), TYPE_ERREUR number(10,0), primary key (id));

create table MODELE_DOCUMENT (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), TYPE_DOCUMENT nvarchar2(32), PERIODE_ID number(19,0), primary key (id));

create table MODELE_FEUILLE_DOC (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), SORT_INDEX number(10,0), INTITULE_FEUILLE nvarchar2(255), NO_FORMULAIRE nvarchar2(255), MODELE_ID number(19,0), primary key (id));

create table MOUVEMENT_DOSSIER (MVT_TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_MOUVEMENT number(10,0), ETAT nvarchar2(15) not null, NUMERO_INDIVIDU number(19,0), CTB_ID number(19,0), COLL_ADMIN_EMETTRICE_ID number(19,0), BORDEREAU_ID number(19,0), COLL_ADMIN_DEST_ID number(19,0), COLL_ADMIN_RECEPTRICE_ID number(19,0), primary key (id));

create table PARAMETRE (nom nvarchar2(255) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), valeur nvarchar2(255), primary key (nom));

create table PARAMETRE_PERIODE_FISCALE (PPF_TYPE nvarchar2(5) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_FIN_ENVOI_MASSE number(10,0), TERME_GEN_SOMM_EFFECT number(10,0), TERME_GEN_SOMM_REGL number(10,0), TYPE_CTB nvarchar2(17), PERIODE_ID number(19,0), PM_DELAI_IMPRIME_MOIS NUMBER(10,0), PM_TOLERANCE_JOURS NUMBER(10,0), PM_DELAI_IMPRIME_FIN_MOIS NUMBER(1,0), PM_TOLERANCE_FIN_MOIS NUMBER(1,0), primary key (id));

create table PERIODE_FISCALE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), ANNEE number(10,0) not null unique, CODE_CTRL_SOMM_DI_PP number(1,0) not null, primary key (id));

create table PERIODICITE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0), PERIODE_DECOMPTE nvarchar2(3), PERIODICITE_TYPE nvarchar2(255) not null, DEBITEUR_ID number(19,0) not null, primary key (id));

create table RAPPORT_ENTRE_TIERS (RAPPORT_ENTRE_TIERS_TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0), DATE_FIN number(10,0), TIERS_OBJET_ID number(19,0) not null, TIERS_SUJET_ID number(19,0) not null, DATE_FIN_DER_ELE_IMP number(10,0), EXTENSION_EXECUTION_FORCEE number(1,0), TIERS_TUTEUR_ID number(19,0), TYPE_MANDAT nvarchar2(15), IBAN_MANDAT nvarchar2(34), BIC_SWIFT_MANDAT nvarchar2(15), NOM_CONTACT_MANDAT NVARCHAR2(50), PRENOM_CONTACT_MANDAT NVARCHAR2(50), TEL_CONTACT_MANDAT NVARCHAR2(35), ETB_PRINCIPAL NUMBER(1,0), ADMIN_PRESIDENT NUMBER(1,0), primary key (id));

create table REMARQUE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), TEXTE nvarchar2(2000), TIERS_ID number(19,0) not null, primary key (id));

create table SITUATION_FAMILLE (SITUATION_FAMILLE_TYPE nvarchar2(31) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0), ETAT_CIVIL nvarchar2(34), NOMBRE_ENFANTS number(10,0) not null, TIERS_PRINCIPAL_ID number(19,0), TARIF_APPLICABLE nvarchar2(11), CTB_ID number(19,0) not null, primary key (id));

create table TACHE (TACHE_TYPE nvarchar2(15) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_ECHEANCE number(10,0), ETAT nvarchar2(11) not null, DECL_ADRESSE_RETOUR nvarchar2(4), CODE_SEGMENT number(10,0), DECL_DATE_DEBUT number(10,0), DECL_DATE_FIN number(10,0), DECL_DATE_DEBUT_EXERCICE NUMBER(10, 0), DECL_DATE_FIN_EXERCICE NUMBER(10, 0), QUALIFICATION nvarchar2(16), DECL_TYPE_CTB nvarchar2(17), DECL_TYPE_DOC nvarchar2(32), , CATEGORIE_ENTREPRISE nvarchar2(10), CA_ID number(19,0), CTB_ID number(19,0), DECLARATION_ID number(19,0), primary key (id));

create table TIERS (TIERS_TYPE nvarchar2(31) not null, NUMERO number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), ADRESSE_BIC_SWIFT nvarchar2(15), ADRESSE_EMAIL nvarchar2(255), BLOC_REMB_AUTO number(1,0), COMPLEMENT_NOM nvarchar2(250), DEBITEUR_INACTIF number(1,0) not null, INDEX_DIRTY number(1,0), NUMERO_COMPTE_BANCAIRE nvarchar2(34), NUMERO_TELECOPIE nvarchar2(35), NUMERO_TEL_PORTABLE nvarchar2(35), NUMERO_TEL_PRIVE nvarchar2(35), NUMERO_TEL_PROF nvarchar2(35), OID number(10,0), PERSONNE_CONTACT nvarchar2(200), REINDEX_ON number(10,0), TITULAIRE_COMPTE_BANCAIRE nvarchar2(200), DATE_LIMITE_EXCLUSION number(10,0), AC_FORME_JURIDIQUE nvarchar2(5), AC_NOM nvarchar2(250), DISTRICT_FISCAL_ID number(19,0), REGION_FISCALE_ID number(19,0), NUMERO_CA number(10,0) unique, CATEGORIE_IMPOT_SOURCE nvarchar2(32), LOGICIEL_ID number(19,0), MODE_COM nvarchar2(12), DPI_NOM1 nvarchar2(250), DPI_NOM2 nvarchar2(250), PERIODE_DECOMPTE nvarchar2(3), PERIODICITE_DECOMPTE nvarchar2(11), SANS_LISTE_RECAP number(1,0), SANS_RAPPEL number(1,0), DPI_ACI_HC NUMBER(1,0), NUMERO_ETABLISSEMENT number(19,0), NUMERO_ENTREPRISE NUMBER(19,0), ANCIEN_NUMERO_SOURCIER number(19,0), NH_CAT_ETRANGER nvarchar2(50), NH_DATE_DEBUT_VALID_AUTORIS number(10,0), DATE_DECES number(10,0), NH_DATE_NAISSANCE number(10,0), PP_HABITANT number(1,0), MAJORITE_TRAITEE number(1,0), NH_NOM nvarchar2(250), NH_NUMERO_ASSURE_SOCIAL nvarchar2(13), NUMERO_INDIVIDU number(19,0), NH_LIBELLE_COMMUNE_ORIGINE nvarchar2(250), NH_NO_OFS_NATIONALITE number(10,0), NH_PRENOM nvarchar2(250), NH_SEXE nvarchar2(8), RENTIER_SRC_ROLE number(1,0), PP_PARENTE_DIRTY NUMBER(1,0), NH_NOM_PERE NVARCHAR2(100), NH_PRENOMS_PERE NVARCHAR2(100), NH_NOM_MERE NVARCHAR2(100), NH_PRENOMS_MERE NVARCHAR2(100), NH_TOUS_PRENOMS NVARCHAR2(100), NH_LIBELLE_ORIGINE NVARCHAR2(100), NH_CANTON_ORIGINE NVARCHAR2(2), NH_NOM_NAISSANCE NVARCHAR2(250), ETB_ENSEIGNE NVARCHAR2(250), ETB_RAISON_SOCIALE NVARCHAR2(250), DATE_DEBUT_PREMIER_EXERCICE NUMBER(10,0), primary key (NUMERO));

create index IDX_ADR_AT_TRS_ID on ADRESSE_TIERS (AUTRE_TIERS_ID);

create index IDX_ADR_TRS_ID on ADRESSE_TIERS (TIERS_ID);

alter table ADRESSE_TIERS add constraint FK_ADR_TRS_ID foreign key (TIERS_ID) references TIERS;

create index IDX_AUDIT_LOG_DATE on AUDIT_LOG (LOG_DATE);

create index IDX_DECL_TRS_ID on DECLARATION (TIERS_ID);

alter table DECLARATION add constraint FK_DECL_TRS_ID foreign key (TIERS_ID) references TIERS;

alter table DECLARATION add constraint FK_DECL_DOC_ID foreign key (MODELE_DOC_ID) references MODELE_DOCUMENT;

alter table DECLARATION add constraint FK_DECL_PF_ID foreign key (PERIODE_ID) references PERIODE_FISCALE;

create index IDX_DE_DI_DI_ID on DELAI_DECLARATION (DECLARATION_ID);

alter table DELAI_DECLARATION add constraint FK_DECL_DEL_DI_ID foreign key (DECLARATION_ID) references DECLARATION;

create index IDX_DA_TIERS_ID on DROIT_ACCES (TIERS_ID);

create index IDX_NUMERO_IND_OPER on DROIT_ACCES (NUMERO_IND_OPER);

alter table DROIT_ACCES add constraint FK_DA_TRS_ID foreign key (TIERS_ID) references TIERS;

create index IDX_ET_DI_DI_ID on ETAT_DECLARATION (DECLARATION_ID);

alter table ETAT_DECLARATION add constraint FK_ET_DI_DI_ID foreign key (DECLARATION_ID) references DECLARATION;

create index IDX_EV_CIV_ETAT on EVENEMENT_CIVIL (ETAT);

create index IDX_EV_CIV_NO_IND_PR on EVENEMENT_CIVIL (NO_INDIVIDU_PRINCIPAL);

create index IDX_EV_CIV_ECH_ETAT on EVENEMENT_CIVIL_ECH (ETAT);

create index IDX_EV_CIV_ECH_NO_IND on EVENEMENT_CIVIL_ECH (NO_INDIVIDU);

create index IDX_EV_CIV_ECH_REF on EVENEMENT_CIVIL_ECH (REF_MESSAGE_ID);

create index IDX_EV_ORGA_ETAT on EVENEMENT_ORGANISATION (ETAT);

create index IDX_EV_ORGA_NO_ORGA on EVENEMENT_ORGANISATION (NO_ORGANISATION);

alter table EVENEMENT_CIVIL_ERREUR add constraint FK_EV_ERR_EV_RGR_ID foreign key (EVT_CIVIL_ID) references EVENEMENT_CIVIL;

create index IDX_EV_ECH_ERR_EV_ID on EVENEMENT_CIVIL_ECH_ERREUR (EVT_CIVIL_ID);

alter table EVENEMENT_CIVIL_ECH_ERREUR add constraint FK_EV_ERR_EV_ECH_ID foreign key (EVT_CIVIL_ID) references EVENEMENT_CIVIL_ECH;

alter table EVENEMENT_ORGANISATION_ERREUR add constraint FK_EV_ERR_EV_ORGA_ID foreign key (EVT_ORGANISATION_ID) references EVENEMENT_ORGANISATION;

ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_TIERS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_ENFANT_ID FOREIGN KEY (PARENTE_ENFANT_ID) REFERENCES TIERS;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_FOR_ID FOREIGN KEY (FOR_FISCAL_ID) REFERENCES FOR_FISCAL;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_ALLGMT_ID FOREIGN KEY (ALLEGEMENT_ID) REFERENCES ALLEGEMENT_FISCAL;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_DECL_ID FOREIGN KEY (DECLARATION_ID) REFERENCES DECLARATION;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_REGFISC_ID FOREIGN KEY (REGIME_FISCAL_ID) REFERENCES REGIME_FISCAL;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_FLAG_ID FOREIGN KEY (FLAG_ENTREPRISE_ID) REFERENCES FLAG_ENTREPRISE;

ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD CONSTRAINT FK_ADF_ENT_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_ADF_ENT_ID ON AUTRE_DOCUMENT_FISCAL (ENTREPRISE_ID ASC);

ALTER TABLE FLAG_ENTREPRISE ADD CONSTRAINT FK_FLAG_ENTRP_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_FLAG_ENTRP_ID ON FLAG_ENTREPRISE(ENTREPRISE_ID ASC, DATE_DEBUT ASC);

create index IDX_FF_TIERS_ID on FOR_FISCAL (TIERS_ID);

alter table FOR_FISCAL add constraint FK_FF_TIERS_ID foreign key (TIERS_ID) references TIERS;

create index IDX_ID_PERS_TIERS_ID on IDENTIFICATION_PERSONNE (NON_HABITANT_ID);

alter table IDENTIFICATION_PERSONNE add constraint FK_ID_PERS_TRS_ID foreign key (NON_HABITANT_ID) references TIERS;

create index IDX_IMM_CTB_ID on IMMEUBLE (CTB_ID);

alter table IMMEUBLE add constraint FK_IMM_CTB_ID foreign key (CTB_ID) references TIERS;

alter table MODELE_DOCUMENT add constraint FK_DOC_PF_ID foreign key (PERIODE_ID) references PERIODE_FISCALE;

alter table MODELE_FEUILLE_DOC add constraint FK_FLLE_MODOC_ID foreign key (MODELE_ID) references MODELE_DOCUMENT;

create index IDX_MVT_DOSSIER_CTB_ID on MOUVEMENT_DOSSIER (CTB_ID);

create index IDX_MVT_DOSSIER_BORD_ID on MOUVEMENT_DOSSIER (BORDEREAU_ID);

create index IDX_MOUVEMENT_DOSSIER_ETAT_CTB on MOUVEMENT_DOSSIER (ETAT, CTB_ID);

alter table MOUVEMENT_DOSSIER add constraint FK_ENV_DOS_CA_EMETT_ID foreign key (COLL_ADMIN_EMETTRICE_ID) references TIERS;

alter table MOUVEMENT_DOSSIER add constraint FK_REC_DOS_CA_ID foreign key (COLL_ADMIN_RECEPTRICE_ID) references TIERS;

alter table MOUVEMENT_DOSSIER add constraint FK_MVT_DOSSIER_BORD_ID foreign key (BORDEREAU_ID) references BORDEREAU_MVT_DOSSIER;

alter table MOUVEMENT_DOSSIER add constraint FK_ENV_DOS_CA_DEST_ID foreign key (COLL_ADMIN_DEST_ID) references TIERS;

alter table MOUVEMENT_DOSSIER add constraint FK_MOV_DOS_CTB_ID foreign key (CTB_ID) references TIERS;

alter table PARAMETRE_PERIODE_FISCALE add constraint FK_PARAM_PF_ID foreign key (PERIODE_ID) references PERIODE_FISCALE;

create index IDX_P_DEBITEUR_ID on PERIODICITE (DEBITEUR_ID);

alter table PERIODICITE add constraint FK_PERIODICITE_DB_ID foreign key (DEBITEUR_ID) references TIERS;

create index IDX_RET_TRS_TUT_ID on RAPPORT_ENTRE_TIERS (TIERS_TUTEUR_ID);

create index IDX_RET_TRS_SUJ_ID on RAPPORT_ENTRE_TIERS (TIERS_SUJET_ID);

create index IDX_RET_TRS_OBJ_ID on RAPPORT_ENTRE_TIERS (TIERS_OBJET_ID);

alter table RAPPORT_ENTRE_TIERS add constraint FK_RET_TRS_SUJ_ID foreign key (TIERS_SUJET_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK_RET_TRS_OBJ_ID foreign key (TIERS_OBJET_ID) references TIERS;

create index IDX_REMARQUE_TIERS_ID on REMARQUE (TIERS_ID);

alter table REMARQUE add constraint FK_REMARQUE_TRS_ID foreign key (TIERS_ID) references TIERS;

create index IDX_SIT_FAM_CTB_ID on SITUATION_FAMILLE (CTB_ID);

create index IDX_SIT_FAM_MC_CTB_ID on SITUATION_FAMILLE (TIERS_PRINCIPAL_ID);

alter table SITUATION_FAMILLE add constraint FK_SF_CTB_ID foreign key (CTB_ID) references TIERS;

create index IDX_TACHE_DATE_ECH on TACHE (DATE_ECHEANCE);

create index IDX_TACHE_CTB_ID on TACHE (CTB_ID);

create index IDX_TACHE_ETAT on TACHE (ETAT);

create index IDX_TACHE_TYPE_DOC on TACHE (DECL_TYPE_DOC);

create index IDX_TACHE_TYPE_CTB on TACHE (DECL_TYPE_CTB);

create index IDX_TACHE_ANNULATION_DATE on TACHE (ANNULATION_DATE);

create index IDX_TACHE_TYPE_CA on TACHE(TACHE_TYPE, ETAT, ANNULATION_DATE, DATE_ECHEANCE, CA_ID);

alter table TACHE add constraint FK_TACH_CTB_ID foreign key (CTB_ID) references TIERS;

alter table TACHE add constraint FK_TACH_DECL_ID foreign key (DECLARATION_ID) references DECLARATION;

alter table TACHE add constraint FK_TACH_CA_ID foreign key (CA_ID) references TIERS;

create index IDX_ANC_NO_SRC on TIERS (ANCIEN_NUMERO_SOURCIER);

create index IDX_NUMERO_INDIVIDU on TIERS (NUMERO_INDIVIDU);
CREATE INDEX IDX_TIERS_NO_ENTREPRISE ON TIERS(NUMERO_ENTREPRISE ASC);
CREATE INDEX IDX_TIERS_NO_ETABLISSEMENT ON TIERS(NUMERO_ETABLISSEMENT ASC);

create index IDX_TIERS_LOG_MDATE on TIERS (LOG_MDATE);

create index IDX_FF_LOG_MDATE on FOR_FISCAL (LOG_MDATE);

create index IDX_ED_LOG_MDATE on ETAT_DECLARATION (LOG_MDATE);

create index IDX_EVT_IDENT_CTB_TRAIT_USER on EVENEMENT_IDENTIFICATION_CTB (TRAITEMENT_USER);

create index IDX_EVT_IDENT_CTB_ETAT on EVENEMENT_IDENTIFICATION_CTB (ETAT);

CREATE INDEX IDX_EVT_IDENT_CTB_PF_ETAT ON EVENEMENT_IDENTIFICATION_CTB(PERIODE_FISCALE, ETAT);

CREATE INDEX IDX_EVT_IDENT_CTB_EMTID_ETAT ON EVENEMENT_IDENTIFICATION_CTB(EMETTEUR_ID, ETAT);

CREATE INDEX IDX_EVT_IDENT_CTB_TM_DT_ETAT ON EVENEMENT_IDENTIFICATION_CTB(TYPE_MESSAGE, DEMANDE_TYPE, ETAT);

-- [SIFISC-11689] Ajout des identifiants d'entreprise IDE
CREATE TABLE IDENTIFICATION_ENTREPRISE (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), NUMERO_IDE NVARCHAR2(12) NOT NULL, TIERS_ID NUMBER(19,9) NOT NULL, PRIMARY KEY (ID));
CREATE INDEX IDX_ID_ENTREPRISE_TIERS_ID ON IDENTIFICATION_ENTREPRISE(TIERS_ID);
ALTER TABLE IDENTIFICATION_ENTREPRISE ADD CONSTRAINT FK_IDE_TIERS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;

-- [SIFISC-12571] Réception des événements de l'application des notaires
CREATE TABLE EVENEMENT_REQDES (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), XML CLOB NOT NULL, DOUBLON NUMBER(1,0) NOT NULL, DATE_ACTE NUMBER(10,0) NOT NULL, NUMERO_MINUTE NVARCHAR2(30) NOT NULL, NO_AFFAIRE NUMBER(19, 0), VISA_NOTAIRE NVARCHAR2(65) NOT NULL, NOM_NOTAIRE NVARCHAR2(100) NOT NULL, PRENOM_NOTAIRE NVARCHAR2(100) NOT NULL, VISA_OPERATEUR NVARCHAR2(65), NOM_OPERATEUR NVARCHAR2(100), PRENOM_OPERATEUR NVARCHAR2(100), PRIMARY KEY (ID));
CREATE INDEX IDX_REQDES_EVT_NO_MINUTE ON EVENEMENT_REQDES(NUMERO_MINUTE ASC, VISA_NOTAIRE ASC);

CREATE TABLE REQDES_UNITE_TRAITEMENT (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), ETAT NVARCHAR2(10) NOT NULL, DATE_TRAITEMENT TIMESTAMP, EVENEMENT_ID NUMBER(19,0) NOT NULL, PRIMARY KEY (ID));
ALTER TABLE REQDES_UNITE_TRAITEMENT ADD CONSTRAINT FK_REQDES_UT_EVT_ID FOREIGN KEY (EVENEMENT_ID) REFERENCES EVENEMENT_REQDES;
CREATE INDEX IDX_REQDES_UT_EVT_ID ON REQDES_UNITE_TRAITEMENT(EVENEMENT_ID ASC);
CREATE INDEX IDX_REQDES_UT_ETAT ON REQDES_UNITE_TRAITEMENT(ETAT ASC);
CREATE INDEX IDX_REQDES_UT_DT_ETAT ON REQDES_UNITE_TRAITEMENT(DATE_TRAITEMENT ASC, ETAT ASC);

CREATE TABLE REQDES_ERREUR (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), MESSAGE NVARCHAR2(1024), CALLSTACK NVARCHAR2(2000), TYPE NVARCHAR2(7) NOT NULL, UNITE_TRAITEMENT_ID NUMBER(19,0) NOT NULL, PRIMARY KEY (ID));
ALTER TABLE REQDES_ERREUR ADD CONSTRAINT FK_REQDES_UT_ERR_UT_ID FOREIGN KEY (UNITE_TRAITEMENT_ID) REFERENCES REQDES_UNITE_TRAITEMENT;
CREATE INDEX IDX_REQDES_ERREUR_UT_ID ON REQDES_ERREUR(UNITE_TRAITEMENT_ID ASC);

CREATE TABLE REQDES_PARTIE_PRENANTE (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), NOM NVARCHAR2(100) NOT NULL, PRENOMS NVARCHAR2(100), DATE_NAISSANCE NUMBER(10,0), SEXE NVARCHAR2(8), DATE_DECES NUMBER(10,0), SOURCE_CIVILE NUMBER(1,0) NOT NULL, NO_CTB NUMBER(19,0), NAVS NVARCHAR2(13), NOM_MERE NVARCHAR2(100), PRENOMS_MERE NVARCHAR2(100), NOM_PERE NVARCHAR2(100), PRENOMS_PERE NVARCHAR2(100), ETAT_CIVIL NVARCHAR2(34), DATE_ETAT_CIVIL NUMBER(10,0), DATE_SEPARATION NUMBER(10,0), OFS_PAYS_NATIONALITE NUMBER(10,0), CATEGORIE_ETRANGER NVARCHAR2(50), TEXTE_CASE_POSTALE NVARCHAR2(15), NUMERO_CASE_POSTALE NUMBER(10,0), LOCALITE NVARCHAR2(100), NO_ORDRE_POSTAL NUMBER(10,0), NPA NVARCHAR2(35), NPA_CPLT NUMBER(10,0), OFS_PAYS_RESIDENCE NUMBER(10,0), RUE NVARCHAR2(100), NUMERO_MAISON NVARCHAR2(35), NUMERO_APPARTEMENT NVARCHAR2(35), COMPLEMENT_ADRESSE NVARCHAR2(100), OFS_COMMUNE_RESIDENCE NUMBER(10,0), NOM_CONJOINT NVARCHAR2(100), PRENOMS_CONJOINT NVARCHAR2(100), CONJOINT_ID NUMBER(19,0), UNITE_TRAITEMENT_ID NUMBER(19,0) NOT NULL, NOM_NAISSANCE NVARCHAR2(100), LIBELLE_ORIGINE NVARCHAR2(50), CANTON_ORIGINE NVARCHAR2(2), NO_CTB_CREE NUMBER(19,0), PRIMARY KEY (ID));
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_PP_UT_ID FOREIGN KEY (UNITE_TRAITEMENT_ID) REFERENCES REQDES_UNITE_TRAITEMENT;
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_PP_CONJOINT_ID FOREIGN KEY (CONJOINT_ID) REFERENCES REQDES_PARTIE_PRENANTE;
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_PP_CTB_CREE FOREIGN KEY (NO_CTB_CREE) REFERENCES TIERS;
CREATE INDEX IDX_REQDES_PP_UT_ID ON REQDES_PARTIE_PRENANTE(UNITE_TRAITEMENT_ID ASC);

CREATE TABLE REQDES_TRANSACTION_IMMOBILIERE (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), OFS_COMMUNE NUMBER(10,0) NOT NULL, DESCRIPTION NVARCHAR2(100), MODE_INSCRIPTION NVARCHAR2(12) NOT NULL, TYPE_INSCRIPTION NVARCHAR2(15) NOT NULL, EVENEMENT_ID NUMBER(19,0) NOT NULL, PRIMARY KEY (ID));
ALTER TABLE REQDES_TRANSACTION_IMMOBILIERE ADD CONSTRAINT FK_REQDES_TI_EVT_ID FOREIGN KEY (EVENEMENT_ID) REFERENCES EVENEMENT_REQDES;

CREATE TABLE REQDES_ROLE_PARTIE_PRENANTE (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), ROLE NVARCHAR2(10) NOT NULL, PARTIE_PRENANTE_ID NUMBER(19,0) NOT NULL, TRANSACTION_IMMOBILIERE_ID NUMBER(19,0) NOT NULL, PRIMARY KEY (ID));
ALTER TABLE REQDES_ROLE_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_RPP_PP_ID FOREIGN KEY (PARTIE_PRENANTE_ID) REFERENCES REQDES_PARTIE_PRENANTE;
ALTER TABLE REQDES_ROLE_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_RPP_TI_ID FOREIGN KEY (TRANSACTION_IMMOBILIERE_ID) REFERENCES REQDES_TRANSACTION_IMMOBILIERE;
CREATE INDEX IDX_REQDES_RPP_PP_ID ON REQDES_ROLE_PARTIE_PRENANTE(PARTIE_PRENANTE_ID ASC);

--SIFISC-12624
CREATE TABLE DECISION_ACI (id number(19,0) NOT NULL, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0),NUMERO_OFS number(10,0) not null, TYPE_AUT_FISC nvarchar2(22) not null, REMARQUE nvarchar2(2000), TIERS_ID number(19,0) not null, primary key (id));
CREATE index IDX_DECISION_ACI_TIERS_ID on DECISION_ACI(TIERS_ID);
ALTER TABLE DECISION_ACI ADD CONSTRAINT FK_DECISION_ACI_TRS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;

-- [SIPM] Les domiciles d'un établissement
CREATE TABLE DOMICILE_ETABLISSEMENT (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10,0) NOT NULL,
	DATE_FIN NUMBER(10,0),
	TYPE_AUT_FISC NVARCHAR2(22) NOT NULL,
	NUMERO_OFS_AUT_FISC NUMBER(10,0) NOT NULL,
	ETABLISSEMENT_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE DOMICILE_ETABLISSEMENT ADD CONSTRAINT FK_DOM_ETB_ETB_ID FOREIGN KEY (ETABLISSEMENT_ID) REFERENCES TIERS;
CREATE INDEX IDX_DOM_ETB_ETB_ID ON DOMICILE_ETABLISSEMENT (ETABLISSEMENT_ID ASC);

-- [SIPM] Les bouclements d'une entreprise
CREATE TABLE BOUCLEMENT (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10, 0) NOT NULL,	-- YYYMMDD
	ANCRAGE NUMBER(4, 0) NOT NULL,			-- MMDD
	PERIODE_MOIS NUMBER(2, 0) NOT NULL,
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	PRIMARY KEY (ID)
);
COMMENT ON COLUMN BOUCLEMENT.DATE_DEBUT IS 'Date de début de validité de la ligne.';
COMMENT ON COLUMN BOUCLEMENT.ANCRAGE IS 'Prochain ancrage de date de bouclement après la date de début de validité.';
COMMENT ON COLUMN BOUCLEMENT.PERIODE_MOIS IS 'Périodicité de bouclement (en mois) depuis le point d''ancrage.';
ALTER TABLE BOUCLEMENT ADD CONSTRAINT FK_BOUCLEMENT_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_BOUCLEMENT_ENTR_ID ON BOUCLEMENT(ENTREPRISE_ID ASC);

-- Nouvelle table pour les régimes fiscaux
CREATE TABLE REGIME_FISCAL (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10, 0) NOT NULL,
	DATE_FIN NUMBER(10, 0),
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	PORTEE NVARCHAR2(2) NOT NULL,
	CODE NVARCHAR2(10) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE REGIME_FISCAL ADD CONSTRAINT FK_REGFISC_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_REGFISC_ENTR_ID ON REGIME_FISCAL(ENTREPRISE_ID ASC);

-- Nouvelle table pour les données civiles des entreprises
CREATE TABLE DONNEE_CIVILE_ENTREPRISE (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DONNEE_TYPE NVARCHAR2(20) NOT NULL,
	DATE_DEBUT NUMBER(10, 0) NOT NULL,
	DATE_FIN NUMBER(10, 0),
	RS_RAISON_SOCIALE NVARCHAR2(250),
	FJ_FORME_JURIDIQUE NVARCHAR2(15),
	CAP_MONTANT NUMBER(19,0),
	CAP_MONNAIE NVARCHAR2(3),
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE DONNEE_CIVILE_ENTREPRISE ADD CONSTRAINT FK_DONCIV_ENTRP_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_DONCIV_ENTRP_ID ON DONNEE_CIVILE_ENTREPRISE (ENTREPRISE_ID ASC, DATE_DEBUT ASC, DONNEE_TYPE ASC);

-- Nouvelle table pour les allègements fiscaux
CREATE TABLE ALLEGEMENT_FISCAL (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10, 0) NOT NULL,
	DATE_FIN NUMBER(10, 0),
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	POURCENTAGE_ALLEGEMENT NUMBER(5, 2),
	TYPE_IMPOT NVARCHAR2(50),
	TYPE_COLLECTIVITE NVARCHAR2(15) NOT NULL,
	NO_OFS_COMMUNE NUMBER(10,0),
	TYPE_ALLEGEMENT_ICC NVARCHAR2(25),
	TYPE_ALLEGEMENT_IFD NVARCHAR2(25),
	PRIMARY KEY (ID)
);
ALTER TABLE ALLEGEMENT_FISCAL ADD CONSTRAINT FK_ALLFISC_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_ALLFISC_ENTR_ID ON ALLEGEMENT_FISCAL(ENTREPRISE_ID ASC);

-- Les états d'entreprise
CREATE TABLE ETAT_ENTREPRISE (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_OBTENTION NUMBER(10, 0) NOT NULL,
	TYPE_ETAT NVARCHAR2(20) NOT NULL,
	TYPE_GENERATION NVARCHAR2(11) NOT NULL,
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE ETAT_ENTREPRISE ADD CONSTRAINT FK_ETAENT_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_ETAENT_ENTR_ID ON ETAT_ENTREPRISE(ENTREPRISE_ID ASC);

-- Table de la migration PM (inutile à l'application Unireg elle-même, mais utilisée par
-- le programme de migration des PM pour sa reprise en cas de crash)
CREATE TABLE MIGRATION_PM_MAPPING (
	ID NUMBER(19, 0) NOT NULL,
	LOG_DATE TIMESTAMP DEFAULT SYSDATE,
	TYPE_ENTITE NVARCHAR2(20) NOT NULL,
	ID_REGPM NUMBER(19, 0) NOT NULL,
	ID_UNIREG NUMBER(19, 0) NOT NULL,
	PRIMARY KEY (ID)
);
COMMENT ON COLUMN MIGRATION_PM_MAPPING.TYPE_ENTITE IS 'Type de l''entité migrée (entreprise, établissement ou individu).';
COMMENT ON COLUMN MIGRATION_PM_MAPPING.ID_REGPM IS 'Identifiant de l''entité dans Reg-PM (= avant migration).';
COMMENT ON COLUMN MIGRATION_PM_MAPPING.ID_UNIREG IS 'Identifiant de l''entité dans Unireg (= après migration).';
CREATE UNIQUE INDEX IDX_MIGRATION_PM_ENTITE ON MIGRATION_PM_MAPPING(TYPE_ENTITE ASC, ID_REGPM ASC);
CREATE SEQUENCE S_MIGR_PM;

CREATE SEQUENCE S_PM START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE S_CAAC START WITH 2000000 INCREMENT BY 1;

CREATE SEQUENCE S_DPI START WITH 1500000 INCREMENT BY 1;

CREATE SEQUENCE S_ETB START WITH 3000000 INCREMENT BY 1;

CREATE SEQUENCE S_CTB START WITH 10000000 INCREMENT BY 1;

CREATE SEQUENCE hibernate_sequence;

