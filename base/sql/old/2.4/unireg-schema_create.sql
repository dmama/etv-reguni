create table ADRESSE_TIERS (ADR_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_DEBUT number(10,0), DATE_FIN number(10,0), USAGE_TYPE varchar2(14 char), TYPE varchar2(14 char), STYPE varchar2(10 char), COMPLEMENT varchar2(255 char), NUMERO_APPARTEMENT varchar2(255 char), NUMERO_CASE_POSTALE number(10,0), NUMERO_MAISON varchar2(255 char), PERMANENTE number(1,0), rue varchar2(255 char), TEXTE_CASE_POSTALE varchar2(15 char), COMPLEMENT_LOCALITE varchar2(255 char), NUMERO_OFS_PAYS number(10,0), NUMERO_POSTAL_LOCALITE varchar2(255 char), NUMERO_ORDRE_POSTE number(10,0), NUMERO_RUE number(10,0), TIERS_ID number(19,0), AUTRE_TIERS_ID number(19,0), primary key (id));

create table AUDIT_LOG (id number(19,0) not null, LOG_DATE timestamp, DOC_ID number(19,0), EVT_ID number(10,0), LOG_LEVEL varchar2(7 char), MESSAGE varchar2(255 char), THREAD_ID number(19,0), LOG_USER varchar2(255 char), primary key (id));

create table DECLARATION (DOCUMENT_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0) not null, NOM_DOCUMENT varchar2(255 char), DATE_IMPR_CHEMISE_TO timestamp, NUMERO number(10,0), NO_OFS_FOR_GESTION number(10,0), TYPE_CTB varchar2(17 char), MODE_COM varchar2(12 char), PERIODICITE varchar2(11 char), MODELE_DOC_ID number(19,0), TIERS_ID number(19,0), PERIODE_ID number(19,0) not null, primary key (id));

create table DELAI_DECLARATION (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), CONFIRMATION_ECRITE number(1,0), DATE_DEMANDE number(10,0), DATE_TRAITEMENT number(10,0), DELAI_ACCORDE_AU number(10,0), DECLARATION_ID number(19,0), primary key (id));

create table DOC_INDEX (DOC_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DESCRIPTION varchar2(255 char), FILE_EXT varchar2(255 char) not null, FILE_NAME varchar2(255 char) not null, FILE_SIZE number(19,0) not null, NOM varchar2(100 char) not null, SUB_PATH varchar2(255 char) not null, NB_TIERS number(10,0), primary key (id));

create table ETAT_DECLARATION (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_OBTENTION number(10,0), TYPE varchar2(12 char), DECLARATION_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_ERREUR (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), message varchar2(255 char), TYPE varchar2(7 char) not null, EVT_CIVIL_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_REGROUPE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT number(10,0), DATE_TRAITEMENT timestamp, ETAT varchar2(10 char), NO_INDIVIDU_CONJOINT number(19,0), NO_INDIVIDU_PRINCIPAL number(19,0), NUMERO_OFS_ANNONCE number(10,0), TYPE varchar2(45 char), HAB_CONJOINT number(19,0), HAB_PRINCIPAL number(19,0), primary key (id));

create table EVENEMENT_CIVIL_UNITAIRE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT number(10,0), ETAT varchar2(10 char), NUMERO_INDIVIDU number(19,0), NUMERO_OFS_ANNONCE number(10,0), TYPE varchar2(60 char), primary key (id));

create table EVENEMENT_EXTERNE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), CORRELATION_ID varchar2(255 char) not null, DATE_EVENEMENT date, DATE_TRAITEMENT date, ERREUR_MESSAGE varchar2(255 char), ETAT varchar2(10 char), MESSAGE clob, TIERS_ID number(19,0), primary key (id));

create table EVENEMENT_FISCAL (EVT_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT number(10,0), NUMERO_TECHNIQUE number(19,0), TYPE varchar2(29 char), MOTIF_FOR varchar2(49 char), DATE_DEBUT_PERIODE number(10,0), DATE_FIN_PERIODE number(10,0), TIERS_ID number(19,0), primary key (id));

create table FOR_FISCAL (FOR_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_OUVERTURE number(10,0) not null, DATE_FERMETURE number(10,0), GENRE_IMPOT varchar2(29 char) not null, NUMERO_OFS number(10,0) not null, TYPE_AUT_FISC varchar2(22 char) not null, MOTIF_FERMETURE varchar2(49 char), MOTIF_OUVERTURE varchar2(49 char), MOTIF_RATTACHEMENT varchar2(22 char), MODE_IMPOSITION varchar2(11 char), TIERS_ID number(19,0), primary key (id));

create table IDENTIFICATION_PERSONNE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), CATEGORIE varchar2(10 char), IDENTIFIANT varchar2(255 char), NON_HABITANT_ID number(19,0), primary key (id));

create table LUCENE_IDX (NAME_ varchar2(50 char) not null, DELETED_ number(1,0), LF_ timestamp, SIZE_ number(10,0), VALUE_ blob, primary key (NAME_));

create table LUCENE_IDX_HOST (NAME_ varchar2(50 char) not null, DELETED_ number(1,0), LF_ timestamp, SIZE_ number(10,0), VALUE_ blob, primary key (NAME_));

create table MIGREG_ERROR (id number(19,0) not null, FOR_PRINCIPAL_CTB varchar2(255 char), LIBELLE_MESSAGE varchar2(255 char), NO_CONTRIBUABLE number(19,0) unique, NO_INDIVIDU number(10,0) unique, NOM_CONTRIBUABLE varchar2(255 char), NOM_INDIVIDU varchar2(255 char), TYPE_ERREUR number(10,0), primary key (id));

create table MODELE_DOCUMENT (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), TYPE_DOCUMENT varchar2(29 char), PERIODE_ID number(19,0), primary key (id));

create table MODELE_FEUILLE_DOC (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), INTITULE_FEUILLE varchar2(255 char), MODELE_ID number(19,0), primary key (id));

create table MOUVEMENT_DOSSIER (MVT_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), NUMERO_INDIVIDU number(19,0), LOCALISATION varchar2(23 char), COLL_ADMIN_ID number(19,0), CTB_ID number(19,0), primary key (id));

create table PARAMETRE (nom varchar2(255 char) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), valeur varchar2(255 char), primary key (nom));

create table PERIODE_FISCALE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ANNEE number(10,0) not null unique, DATE_FIN_ENVOI_MASSE number(10,0) not null, TERME_GEN_SOMM_EFFECT number(10,0), TERME_GEN_SOMM_REGL number(10,0), primary key (id));

create table RAPPORT_ENTRE_TIERS (RAPPORT_ENTRE_TIERS_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_DEBUT number(10,0), DATE_FIN number(10,0), TYPE varchar2(20 char), DATE_FIN_DER_ELE_IMP number(10,0), TAUX_ACTIVITE number(10,0), TYPE_ACTIVITE varchar2(14 char), EXTENSION_EXECUTION_FORCEE number(1,0), TIERS_OBJET_ID number(19,0), TIERS_TUTEUR_ID number(19,0), TIERS_SUJET_ID number(19,0), primary key (id));

create table SITUATION_FAMILLE (SITUATION_FAMILLE_TYPE varchar2(31 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0), ETAT_CIVIL varchar2(34 char), NOMBRE_ENFANTS number(10,0) not null, TARIF_APPLICABLE varchar2(11 char), CTB_ID number(19,0), TIERS_PRINCIPAL_ID number(19,0), primary key (id));

create table TACHE (TACHE_TYPE varchar2(15 char) not null, id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_ECHEANCE number(10,0), ETAT varchar2(11 char) not null, DECL_DATE_DEBUT number(10,0), DECL_DATE_FIN number(10,0), DECL_TYPE_CTB varchar2(17 char), DECL_TYPE_DOC varchar2(29 char), CTB_ID number(19,0), DECLARATION_ID number(19,0), primary key (id));

create table TIERS (TIERS_TYPE varchar2(31 char) not null, NUMERO number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(255 char), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ADRESSE_BIC_SWIFT varchar2(15 char), ADRESSE_EMAIL varchar2(255 char), BLOC_REMB_AUTO number(1,0), COMPLEMENT_NOM varchar2(255 char), DEBITEUR_INACTIF number(1,0) not null, INDEX_DIRTY number(1,0), NUMERO_COMPTE_BANCAIRE varchar2(34 char), NUMERO_TELECOPIE varchar2(255 char), NUMERO_TEL_PORTABLE varchar2(255 char), NUMERO_TEL_PRIVE varchar2(255 char), NUMERO_TEL_PROF varchar2(255 char), OID number(10,0), PERSONNE_CONTACT varchar2(255 char), REMARQUE varchar2(2000 char), TITULAIRE_COMPTE_BANCAIRE varchar2(255 char), AC_FORME_JURIDIQUE varchar2(5 char), AC_NOM varchar2(255 char), NUMERO_CA number(19,0), CATEGORIE_IMPOT_SOURCE varchar2(32 char), MODE_COM varchar2(12 char), MOIS_ENVOI varchar2(9 char), PERIODICITE_DECOMPTE varchar2(11 char), SANS_LISTE_RECAP number(1,0), SANS_RAPPEL number(1,0), NUMERO_PM number(19,0), NUMERO_ETABLISSEMENT number(19,0), ANCIEN_NUMERO_SOURCIER number(19,0), NUMERO_INDIVIDU number(19,0), NH_CAT_ETRANGER varchar2(35 char), NH_DATE_DEBUT_VALID_AUTORIS number(10,0), NH_DATE_NAISSANCE number(10,0), NH_DECEDE number(1,0), NH_NOM varchar2(255 char), NH_NUMERO_ASSURE_SOCIAL varchar2(255 char), NH_NO_OFS_PAYS_ORIGINE number(10,0), NH_PRENOM varchar2(255 char), NH_SEXE varchar2(8 char), CTB_DPI_ID number(19,0), primary key (NUMERO));

create index IDX_ADR_AT_TRS_ID on ADRESSE_TIERS (AUTRE_TIERS_ID);

create index IDX_ADR_TRS_ID on ADRESSE_TIERS (TIERS_ID);

alter table ADRESSE_TIERS add constraint FK_ADR_TRS_ID foreign key (TIERS_ID) references TIERS;

alter table ADRESSE_TIERS add constraint FK_ADR_AT_TRS_ID foreign key (AUTRE_TIERS_ID) references TIERS;

create index IDX_AUDIT_LOG_DATE on AUDIT_LOG (LOG_DATE);

create index IDX_DECL_TRS_ID on DECLARATION (TIERS_ID);

alter table DECLARATION add constraint FK_DECL_PF_ID foreign key (PERIODE_ID) references PERIODE_FISCALE;

alter table DECLARATION add constraint FK_DECL_TRS_ID foreign key (TIERS_ID) references TIERS;

alter table DECLARATION add constraint FK_DECL_DOC_ID foreign key (MODELE_DOC_ID) references MODELE_DOCUMENT;

alter table DELAI_DECLARATION add constraint FK_DECL_DEL_DI_ID foreign key (DECLARATION_ID) references DECLARATION;

create index IDX_ET_DI_DI_ID on ETAT_DECLARATION (DECLARATION_ID);

alter table ETAT_DECLARATION add constraint FK_ET_DI_DI_ID foreign key (DECLARATION_ID) references DECLARATION;

alter table EVENEMENT_CIVIL_ERREUR add constraint FK_EV_ERR_EV_RGR_ID foreign key (EVT_CIVIL_ID) references EVENEMENT_CIVIL_REGROUPE;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FK_EV_RGR_TRS_PRC_ID foreign key (HAB_PRINCIPAL) references TIERS;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FK_EV_RGR_TRS_CJT_ID foreign key (HAB_CONJOINT) references TIERS;

alter table EVENEMENT_EXTERNE add constraint FK_EV_EXT_TRS_ID foreign key (TIERS_ID) references TIERS;

alter table EVENEMENT_FISCAL add constraint FK_EV_FSC_TRS_ID foreign key (TIERS_ID) references TIERS;

create index IDX_FF_TIERS_ID on FOR_FISCAL (TIERS_ID);

alter table FOR_FISCAL add constraint FK_FF_TIERS_ID foreign key (TIERS_ID) references TIERS;

alter table IDENTIFICATION_PERSONNE add constraint FK_ID_PERS_TRS_ID foreign key (NON_HABITANT_ID) references TIERS;

alter table MODELE_DOCUMENT add constraint FK_DOC_PF_ID foreign key (PERIODE_ID) references PERIODE_FISCALE;

alter table MODELE_FEUILLE_DOC add constraint FK_FLLE_MODOC_ID foreign key (MODELE_ID) references MODELE_DOCUMENT;

alter table MOUVEMENT_DOSSIER add constraint FK_MOV_DOS_CTB_ID foreign key (CTB_ID) references TIERS;

alter table MOUVEMENT_DOSSIER add constraint FK_ENV_DOS_CA_ID foreign key (COLL_ADMIN_ID) references TIERS;

create index IDX_RET_TRS_TUT_ID on RAPPORT_ENTRE_TIERS (TIERS_TUTEUR_ID);

create index IDX_RET_TRS_OBJ_ID on RAPPORT_ENTRE_TIERS (TIERS_OBJET_ID);

create index IDX_RET_TRS_SUJ_ID on RAPPORT_ENTRE_TIERS (TIERS_SUJET_ID);

alter table RAPPORT_ENTRE_TIERS add constraint FK_RET_TRS_TUT_ID foreign key (TIERS_TUTEUR_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK_RET_TRS_OBJ_ID foreign key (TIERS_OBJET_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK_RET_TRS_SUJ_ID foreign key (TIERS_SUJET_ID) references TIERS;

create index IDX_SIT_FAM_CTB_ID on SITUATION_FAMILLE (CTB_ID);

create index IDX_SIT_FAM_MC_CTB_ID on SITUATION_FAMILLE (TIERS_PRINCIPAL_ID);

alter table SITUATION_FAMILLE add constraint FK_SF_CTB_ID foreign key (CTB_ID) references TIERS;

alter table SITUATION_FAMILLE add constraint FK_SIT_FAM_MC_CTB_ID foreign key (TIERS_PRINCIPAL_ID) references TIERS;

create index IDX_TACHE_ETAT on TACHE (ETAT);

create index IDX_TACHE_TYPE_DOC on TACHE (DECL_TYPE_DOC);

create index IDX_TACHE_TYPE_CTB on TACHE (DECL_TYPE_CTB);

create index IDX_TACHE_ANNULATION_DATE on TACHE (ANNULATION_DATE);

create index IDX_TACHE_DATE_ECH on TACHE (DATE_ECHEANCE);

alter table TACHE add constraint FK_TACH_CTB_ID foreign key (CTB_ID) references TIERS;

alter table TACHE add constraint FK_TACH_DECL_ID foreign key (DECLARATION_ID) references DECLARATION;

create index IDX_NUMERO_INDIVIDU on TIERS (NUMERO_INDIVIDU);

create index IDX_CTB_DPI_ID on TIERS (CTB_DPI_ID);

create index IDX_NUMERO_CA on TIERS (NUMERO_CA);

alter table TIERS add constraint FK_DPI_CTB_ID foreign key (CTB_DPI_ID) references TIERS;

create sequence S_PM start with 2000000 increment by 1;

create sequence S_DPI start with 1500000 increment by 1;

create sequence S_CTB start with 10000000 increment by 1;

create sequence hibernate_sequence;

