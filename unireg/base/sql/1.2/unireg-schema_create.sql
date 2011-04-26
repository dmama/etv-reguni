create table ADRESSE_TIERS (ADR_TYPE varchar2(31 char) not null, id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_DEBUT timestamp, DATE_FIN timestamp, USAGE_TYPE varchar2(255 char), TYPE varchar2(255 char), STYPE varchar2(255 char), COMPLEMENT varchar2(255 char), NUMERO_APPARTEMENT varchar2(255 char), NUMERO_CASE_POSTALE number(10,0), NUMERO_MAISON varchar2(255 char), rue varchar2(255 char), TEXTE_CASE_POSTALE varchar2(255 char), COMPLEMENT_LOCALITE varchar2(255 char), NUMERO_OFS_PAYS number(10,0), NUMERO_POSTAL_LOCALITE varchar2(255 char), NUMERO_ORDRE_POSTE number(10,0), NUMERO_RUE number(10,0), TIERS_ADR_AUTRE_ID number(19,0), TIERS_ID number(19,0), primary key (id));

create table AUDIT_LOG (id number(19,0) not null, LOG_DATE timestamp, EVT_ID number(10,0), LOG_LEVEL varchar2(255 char), MESSAGE varchar2(255 char), THREAD_ID number(19,0), LOG_USER varchar2(255 char), primary key (id));

create table DOCUMENT (DOCUMENT_TYPE varchar2(31 char) not null, id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), NUMERO_PERIODE_IMPOSITION number(10,0), DATE_DEBUT_PERIODE timestamp, DATE_FIN_PERIODE timestamp, TIERS_ID number(19,0), PERIODE_ID number(19,0), primary key (id));

create table ECHEANCE_DOCUMENT (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), CONFIRMATION_ECRITE number(1,0), DATE_DEMANDE timestamp, DATE_TRAITEMENT timestamp, DELAI_ACCORDE_AU timestamp, DOCUMENT_ID number(19,0), primary key (id));

create table ETAT_DOCUMENT (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EXPEDITION timestamp, TYPE varchar2(255 char), DOCUMENT_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_ERREUR (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), message varchar2(255 char), EVT_CIVIL_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_REGROUPE (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT timestamp, DATE_TRAITEMENT timestamp, etat varchar2(255 char), NO_INDIVIDU_CONJOINT number(19,0), NO_INDIVIDU_PRINCIPAL number(19,0), NUMERO_OFS_ANNONCE number(10,0), TYPE varchar2(255 char), INDIVIDU_PRINCIPAL number(19,0), CONJOINT number(19,0), primary key (id));

create table EVENEMENT_CIVIL_UNITAIRE (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT timestamp, ETAT varchar2(255 char), NUMERO_INDIVIDU number(19,0), NUMERO_OFS_ANNONCE number(10,0), TYPE varchar2(255 char), primary key (id));

create table EVENEMENT_FISCAL (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT date, DATE_TRAITEMENT date, type varchar2(255 char), CTB_ID number(19,0), primary key (id));

create table FOR_FISCAL (FOR_TYPE varchar2(31 char) not null, id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), EST_ANNULE number(1,0), DATE_FERMETURE timestamp, DATE_OUVERTURE timestamp, forGestion number(1,0), GENRE_IMPOT varchar2(255 char), NUMERO_OFS_HORS_CANTON number(10,0), NUMERO_OFS_VAUD number(10,0), NUMERO_OFS_PAYS number(10,0), rattachement varchar2(255 char), TYPE_FOR varchar2(255 char), CAT_IMP_ORD varchar2(255 char), CTB_ID number(19,0), primary key (id));

create table IDENTIFICATION_PERSONNE (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), identifiant varchar2(255 char), nonHabitant raw(255), source varchar2(255 char), NON_HABITANT_ID number(19,0), primary key (id));

create table MIGREG_ERROR (id number(19,0) not null, FOR_PRINCIPAL_CTB varchar2(255 char), LIBELLE_MESSAGE varchar2(255 char), NO_CONTRIBUABLE number(19,0), NO_INDIVIDU number(19,0), NOM_CONTRIBUABLE varchar2(255 char), NOM_INDIVIDU varchar2(255 char), TYPE_ERREUR varchar2(255 char), primary key (id));

create table PERIODE_FISCALE (id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ANNEE number(10,0), delaiGeneralSommation timestamp, primary key (id));

create table RAPPORT_ENTRE_TIERS (RAPPORT_ENTRE_TIERS_TYPE varchar2(31 char) not null, id number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_DEBUT timestamp, DATE_FIN timestamp, TYPE varchar2(255 char), DATE_FIN_DER_ELE_IMP timestamp, tarifApplicable varchar2(255 char), tauxActivite number(10,0), typeActivite varchar2(255 char), TIERS_SUJET_ID number(19,0), TIERS_OBJET_ID number(19,0), primary key (id));

create table SITUATION_FAMILLE (id number(19,0) not null, NOMBRE_ENFANTS number(10,0), TARIF_APPLICABLE varchar2(255 char), typeMenage varchar2(255 char), CTB_ID number(19,0), primary key (id));

create table TIERS (TIERS_TYPE varchar2(31 char) not null, NUMERO number(19,0) not null, LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ADRESSE_BIC_SWIFT varchar2(255 char), ADRESSE_EMAIL varchar2(255 char), BLOC_REMB_AUTO number(1,0), COMPLEMENT_NOM varchar2(255 char), dateFinActivite timestamp, NUMERO_COMPTE_BANCAIRE varchar2(255 char), NUMERO_TELECOPIE varchar2(255 char), NUMERO_TEL_PORTABLE varchar2(255 char), NUMERO_TEL_PRIVE varchar2(255 char), NUMERO_TEL_PROF varchar2(255 char), PERSONNE_CONTACT varchar2(255 char), REMARQUE varchar2(255 char), TITULAIRE_COMPTE_BANCAIRE varchar2(255 char), typeDeclaration varchar2(255 char), AC_FORME_JURIDIQUE varchar2(255 char), AC_NOM varchar2(255 char), NUMERO_CA number(19,0), ANCIEN_NUM_DEB number(19,0), CATEGORIE_IMPOT_SOURCE varchar2(255 char), MODE_COM varchar2(255 char), PERIODICITE_DECOMPTE varchar2(255 char), sansListeRecaptulative number(1,0), sansRappel number(1,0), NUMERO_ENTREPRISE number(19,0), NUMERO_ETABLISSEMENT number(19,0), ANCIEN_NUMERO_SOURCIER number(19,0), NUMERO_INDIVIDU number(19,0), NH_DATE_DEBUT_VALID_AUTORIS timestamp, NH_DATE_NAISSANCE varchar2(255 char), NH_DECEDE number(1,0), NH_ETAT_CIVIL varchar2(255 char), NH_MOTIF_DISS_PART varchar2(255 char), NH_NOM varchar2(255 char), NH_NUMERO_ASSURE_SOCIAL varchar2(255 char), NH_NO_OFS_PAYS_ORIGINE number(10,0), NH_PRENOM varchar2(255 char), NH_SEPARATION varchar2(255 char), NH_SEXE varchar2(255 char), NH_TYPE_AUTORIS varchar2(255 char), CTB_DIS_ID number(19,0), primary key (NUMERO));

alter table ADRESSE_TIERS add constraint FKCC9F2B01629C1D21 foreign key (TIERS_ADR_AUTRE_ID) references TIERS;

alter table ADRESSE_TIERS add constraint FKCC9F2B017EECEE45 foreign key (TIERS_ID) references TIERS;

alter table DOCUMENT add constraint FK6202C11BFE840874 foreign key (PERIODE_ID) references PERIODE_FISCALE;

alter table DOCUMENT add constraint FK6202C11B7EECEE45 foreign key (TIERS_ID) references TIERS;

alter table ECHEANCE_DOCUMENT add constraint FKBE663D0B4664115 foreign key (DOCUMENT_ID) references DOCUMENT;

alter table ETAT_DOCUMENT add constraint FK2D2D7798B4664115 foreign key (DOCUMENT_ID) references DOCUMENT;

alter table EVENEMENT_CIVIL_ERREUR add constraint FK6487445F52A7D943 foreign key (EVT_CIVIL_ID) references EVENEMENT_CIVIL_REGROUPE;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FKE4752B5BF1AAEE30 foreign key (CONJOINT) references TIERS;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FKE4752B5B8DF2B905 foreign key (INDIVIDU_PRINCIPAL) references TIERS;

alter table EVENEMENT_FISCAL add constraint FK9BFBDC7485BDFD4 foreign key (CTB_ID) references TIERS;

alter table FOR_FISCAL add constraint FKF0623A1485BDFD4 foreign key (CTB_ID) references TIERS;

alter table IDENTIFICATION_PERSONNE add constraint FKC88C7E7DAE266640 foreign key (NON_HABITANT_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK13790EB3529ACEBE foreign key (TIERS_OBJET_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK13790EB3DF35E80D foreign key (TIERS_SUJET_ID) references TIERS;

alter table SITUATION_FAMILLE add constraint FK8565EE6D85BDFD4 foreign key (CTB_ID) references TIERS;

create index INDEX_NUMERO_INDIVIDU on TIERS (NUMERO_INDIVIDU);

alter table TIERS add constraint FK4C1F2B11689A625 foreign key (CTB_DIS_ID) references TIERS;

create sequence S_HABITANT start with 80000000 increment by 1;

create sequence S_TIERS start with 70000000 increment by 1;

create sequence S_NON_HABITANT start with 90000000 increment by 1;

create sequence hibernate_sequence;
