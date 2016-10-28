create table ADRESSE_TIERS (ADR_TYPE varchar2(31 char) not null, id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ANNULEE number(1,0), DATE_DEBUT timestamp, DATE_FIN timestamp, USAGE_TYPE varchar2(255 char), TYPE varchar2(255 char), STYPE varchar2(255 char), COMPLEMENT varchar2(255 char), NUMERO_APPARTEMENT varchar2(255 char), NUMERO_CASE_POSTALE number(10,0), NUMERO_MAISON varchar2(255 char), rue varchar2(255 char), TEXTE_CASE_POSTALE varchar2(255 char), COMPLEMENT_LOCALITE varchar2(255 char), NUMERO_OFS_PAYS number(10,0), NUMERO_POSTAL_LOCALITE varchar2(255 char), NUMERO_ORDRE_POSTE number(10,0), NUMERO_RUE number(10,0), TIERS_ID number(19,0), AUTRE_TIERS_ID number(19,0), primary key (id));

create table AUDIT_LOG (id number(19,0) not null, LOG_DATE timestamp, EVT_ID number(10,0), LOG_LEVEL varchar2(255 char), MESSAGE varchar2(255 char), THREAD_ID number(19,0), LOG_USER varchar2(255 char), primary key (id));

create table DECLARATION (DOCUMENT_TYPE varchar2(31 char) not null, id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ANNULE number(1,0), DATE_DEBUT timestamp, DATE_FIN timestamp, NOM_DOCUMENT varchar2(255 char), NUMERO number(10,0), NO_OFS_FOR_GESTION number(10,0), TYPE_DECLARATION varchar2(255 char), MODE_COM varchar2(255 char), PERIODICITE varchar2(255 char), PERIODE_ID number(19,0), TIERS_ID number(19,0), primary key (id));

create table DELAI_DECLARATION (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), annule number(1,0), CONFIRMATION_ECRITE number(1,0), DATE_DEMANDE timestamp, DATE_TRAITEMENT timestamp, DELAI_ACCORDE_AU timestamp, DECLARATION_ID number(19,0), primary key (id));

create table ETAT_DECLARATION (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), annule number(1,0), DATE_OBTENTION timestamp, TYPE varchar2(255 char), DECLARATION_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_ERREUR (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), message varchar2(255 char), EVT_CIVIL_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_REGROUPE (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT timestamp, DATE_TRAITEMENT timestamp, etat varchar2(255 char), NO_INDIVIDU_CONJOINT number(19,0), NO_INDIVIDU_PRINCIPAL number(19,0), NUMERO_OFS_ANNONCE number(10,0), TYPE varchar2(255 char), HAB_CONJOINT number(19,0), HAB_PRINCIPAL number(19,0), primary key (id));

create table EVENEMENT_CIVIL_UNITAIRE (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT timestamp, ETAT varchar2(255 char), NUMERO_INDIVIDU number(19,0), NUMERO_OFS_ANNONCE number(10,0), TYPE varchar2(255 char), primary key (id));

create table EVENEMENT_EXTERNE (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT date, DATE_TRAITEMENT date, MESSAGE clob, primary key (id));

create table EVENEMENT_FISCAL (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), DATE_EVENEMENT date, DATE_TRAITEMENT date, type varchar2(255 char), TIERS_ID number(19,0), primary key (id));

create table FOR_FISCAL (FOR_TYPE varchar2(31 char) not null, id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), EST_ANNULE number(1,0), DATE_FERMETURE timestamp, DATE_OUVERTURE timestamp, GENRE_IMPOT varchar2(255 char), NUMERO_OFS number(10,0), TYPE_FOR varchar2(255 char), FOR_GESTION number(1,0), MOTIF_FERMETURE varchar2(255 char), MOTIF_OUVERTURE varchar2(255 char), MOTIF_RATTACHEMENT varchar2(255 char), MODE_IMPOSITION varchar2(255 char), CTB_ID number(19,0), primary key (id));

create table IDENTIFICATION_PERSONNE (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), identifiant varchar2(255 char), source varchar2(255 char), NON_HABITANT_ID number(19,0), primary key (id));

create table LUCENE_IDX (NAME_ varchar2(50 char) not null, DELETED_ number(1,0), LF_ timestamp, SIZE_ number(10,0), VALUE_ blob, primary key (NAME_));

create table LUCENE_IDX_HOST (NAME_ varchar2(50 char) not null, DELETED_ number(1,0), LF_ timestamp, SIZE_ number(10,0), VALUE_ blob, primary key (NAME_));

create table MIGREG_ERROR (id number(19,0) not null, FOR_PRINCIPAL_CTB varchar2(255 char), LIBELLE_MESSAGE varchar2(255 char), NO_CONTRIBUABLE number(19,0), NO_INDIVIDU number(19,0), NOM_CONTRIBUABLE varchar2(255 char), NOM_INDIVIDU varchar2(255 char), TYPE_ERREUR varchar2(255 char), primary key (id));

create table PERIODE_FISCALE (id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ANNEE number(10,0), DELAI_GEN_SOMMATION timestamp, primary key (id));

create table RAPPORT_ENTRE_TIERS (RAPPORT_ENTRE_TIERS_TYPE varchar2(31 char) not null, id number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), annule number(1,0), DATE_DEBUT timestamp, DATE_FIN timestamp, TYPE varchar2(255 char), DATE_FIN_DER_ELE_IMP timestamp, tauxActivite number(10,0), typeActivite varchar2(255 char), EXTENSION_EXECUTION_FORCEE number(1,0), TIERS_TUTEUR_ID number(19,0), TIERS_SUJET_ID number(19,0), TIERS_OBJET_ID number(19,0), primary key (id));

create table SITUATION_FAMILLE (SITUATION_FAMILLE_TYPE varchar2(31 char) not null, id number(19,0) not null, annule number(1,0), DATE_DEBUT timestamp, DATE_FIN timestamp, NOMBRE_ENFANTS number(10,0), TARIF_APPLICABLE varchar2(255 char), TIERS_PRINCIPAL_ID number(19,0), CTB_ID number(19,0), primary key (id));

create table TIERS (TIERS_TYPE varchar2(31 char) not null, NUMERO number(19,0) not null, INDEX_DIRTY number(1,0), LOG_CDATE timestamp, LOG_CUSER varchar2(255 char), LOG_MDATE timestamp, LOG_MUSER varchar2(255 char), ADRESSE_BIC_SWIFT varchar2(255 char), ADRESSE_EMAIL varchar2(255 char), BLOC_REMB_AUTO number(1,0), COMPLEMENT_NOM varchar2(255 char), DATE_DEBUT_ACTI timestamp, DATE_FIN_ACTI timestamp, NUMERO_COMPTE_BANCAIRE varchar2(255 char), NUMERO_TELECOPIE varchar2(255 char), NUMERO_TEL_PORTABLE varchar2(255 char), NUMERO_TEL_PRIVE varchar2(255 char), NUMERO_TEL_PROF varchar2(255 char), PERSONNE_CONTACT varchar2(255 char), REMARQUE varchar2(255 char), TITULAIRE_COMPTE_BANCAIRE varchar2(255 char), AC_FORME_JURIDIQUE varchar2(255 char), AC_NOM varchar2(255 char), NUMERO_CA number(19,0), CATEGORIE_IMPOT_SOURCE varchar2(255 char), MODE_COM varchar2(255 char), MOIS_ENVOI varchar2(255 char), PERIODICITE_DECOMPTE varchar2(255 char), SANS_LISTE_RECAP number(1,0), SANS_RAPPEL number(1,0), NUMERO_ETABLISSEMENT number(19,0), ANCIEN_NUMERO_SOURCIER number(19,0), NUMERO_INDIVIDU number(19,0) unique, NH_DATE_DEBUT_VALID_AUTORIS timestamp, NH_DATE_NAISSANCE number(10,0), NH_DECEDE number(1,0), NH_ETAT_CIVIL varchar2(255 char), NH_MOTIF_DISS_PART varchar2(255 char), NH_NOM varchar2(255 char), NH_NUMERO_ASSURE_SOCIAL varchar2(255 char), NH_NO_OFS_PAYS_ORIGINE number(10,0), NH_PRENOM varchar2(255 char), NH_SEPARATION varchar2(255 char), NH_SEXE varchar2(255 char), NH_TYPE_AUTORIS varchar2(255 char), CTB_DPI_ID number(19,0), primary key (NUMERO));

alter table ADRESSE_TIERS add constraint FKCC9F2B017EECEE45 foreign key (TIERS_ID) references TIERS;

alter table ADRESSE_TIERS add constraint FKCC9F2B01468DA811 foreign key (AUTRE_TIERS_ID) references TIERS;

alter table DECLARATION add constraint FK76B095A11FC9E5 foreign key (PERIODE_ID) references PERIODE_FISCALE;

alter table DECLARATION add constraint FK76B095A7EECEE45 foreign key (TIERS_ID) references TIERS;

alter table DELAI_DECLARATION add constraint FK2597C46EBFE4EC8E foreign key (DECLARATION_ID) references DECLARATION;

alter table ETAT_DECLARATION add constraint FKC6F565BDBFE4EC8E foreign key (DECLARATION_ID) references DECLARATION;

alter table EVENEMENT_CIVIL_ERREUR add constraint FK6487445F52A7D943 foreign key (EVT_CIVIL_ID) references EVENEMENT_CIVIL_REGROUPE;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FKE4752B5B1E5A2BE0 foreign key (HAB_PRINCIPAL) references TIERS;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FKE4752B5B6005726 foreign key (HAB_CONJOINT) references TIERS;

alter table EVENEMENT_FISCAL add constraint FK9BFBDC747EECEE45 foreign key (TIERS_ID) references TIERS;

alter table FOR_FISCAL add constraint FKF0623A1485BDFD4 foreign key (CTB_ID) references TIERS;

alter table IDENTIFICATION_PERSONNE add constraint FKC88C7E7DAE266640 foreign key (NON_HABITANT_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK13790EB3C80D1999 foreign key (TIERS_TUTEUR_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK13790EB3529ACEBE foreign key (TIERS_OBJET_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK13790EB3DF35E80D foreign key (TIERS_SUJET_ID) references TIERS;

alter table SITUATION_FAMILLE add constraint FK8565EE6D85BDFD4 foreign key (CTB_ID) references TIERS;

alter table SITUATION_FAMILLE add constraint FK8565EE6DB266BB45 foreign key (TIERS_PRINCIPAL_ID) references TIERS;

alter table TIERS add constraint FK4C1F2B116E7BEF6 foreign key (CTB_DPI_ID) references TIERS;

create sequence S_PM start with 2000000 increment by 1;

create sequence S_DPI start with 1500000 increment by 1;

create sequence S_CTB start with 10000000 increment by 1;

create sequence hibernate_sequence;

