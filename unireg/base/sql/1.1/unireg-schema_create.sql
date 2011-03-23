create table ADRESSE_TIERS (ADR_TYPE varchar2(31 char) not null, id number(19,0) not null, DATE_DEBUT date not null, DATE_FIN date, COMPLEMENT varchar2(255 char), NUMERO_APPARTEMENT varchar2(255 char), NUMERO_CASE_POSTALE number(10,0), NUMERO_MAISON varchar2(255 char), rue varchar2(255 char), TEXTE_CASE_POSTALE number(10,0), complementLocalite varchar2(255 char), numeroOfsPays number(10,0), numeroPostalLocalite varchar2(255 char), NUMERO_ORDRE_POSTE number(10,0), NUMERO_RUE number(10,0), TIERS_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_ERREUR (id number(19,0) not null, champ varchar2(255 char), message varchar2(255 char), EVT_CIVIL_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_REGROUPE (id number(19,0) not null, DATE_EVENEMENT timestamp, DATE_TRAITEMENT timestamp, etat number(10,0), NO_INDIVIDU_CONJOINT number(19,0), NO_INDIVIDU_PRINCIPAL number(19,0), NUMERO_OFS_ANNONCE number(19,0), TYPE number(10,0), CONJOINT number(19,0), INDIVIDU_PRINCIPAL number(19,0), primary key (id));

create table EVENEMENT_CIVIL_UNITAIRE (id number(19,0) not null, DATE_EVENEMENT timestamp, DATE_TRAITEMENT timestamp, ETAT number(10,0), NUMERO_INDIVIDU number(19,0), NUMERO_OFS_ANNONCE number(19,0), type number(10,0), primary key (id));

create table EVENEMENT_FISCAL (id number(19,0) not null, DATE_EVENEMENT date, DATE_TRAITEMENT date, type number(10,0), CTB_ID number(19,0), primary key (id));

create table FOR_FISCAL (id number(19,0) not null, EST_ANNULE number(1,0), DATE_FERMETURE timestamp, DATE_OUVERTURE timestamp, GENRE_IMPOT number(10,0), NUMERO_OFS_HORS_CANTON number(10,0), NUMERO_OFS_VAUD number(10,0), NUMERO_OFS_PAYS number(10,0), rattachement number(10,0), TYPE_FOR number(10,0), CTB_ID number(19,0), primary key (id));

create table IDENTIFICATION_PERSONNE (id number(19,0) not null, identifiant varchar2(255 char), source number(10,0), NON_HABITANT_ID number(19,0), primary key (id));

create table MODE_IMPOSITION (id number(19,0) not null, CATEGORIE_IMPOT_ORDINAIRE number(10,0), contribuable raw(255), DATE_DEBUT timestamp, DATE_FIN timestamp, CTB_ID number(19,0), primary key (id));

create table POINT_COMMUNICATION (id number(19,0) not null, tiers raw(255), type number(10,0), valeur varchar2(255 char), TIERS_ID number(19,0), primary key (id));

create table RAPPORT_ENTRE_TIERS (id number(19,0) not null, DATE_DEBUT timestamp, DATE_FIN timestamp, type number(10,0), TIERS_OBJET_ID number(19,0), TIERS_SUJET_ID number(19,0), primary key (id));

create table TIERS (TIERS_TYPE varchar2(31 char) not null, NUMERO number(19,0) not null, ADRESSE_SWIFT varchar2(255 char), ANCIEN_NUMERO_SOURCIER number(19,0), BLOC_REMB_AUTO number(1,0), FORMAT_IBAN number(1,0), NUMERO_COMPTE_BANCAIRE varchar2(255 char), NUMERO_INSTU_FINAN number(10,0), REMARQUE varchar2(255 char), TITULAIRE_COMPTE_BANCAIRE varchar2(255 char), CATEGORIE_IMPOT_SOURCE number(10,0), MODE_COMMUNICATION number(10,0), PERIODICITE_DECOMPTE number(10,0), NUMERO_ENTREPRISE number(19,0), NUMERO_ETABLISSEMENT number(19,0), NUMERO_INDIVIDU number(19,0), ANCIEN_NUMERO_AVS varchar2(255 char), DATE_VALID_AUTORISATION date, DATE_NAISSANCE varchar2(255 char), nationalite varchar2(255 char), nom varchar2(255 char), NUMERO_ASSURE_SOCIAL varchar2(255 char), prenom varchar2(255 char), sexe number(10,0), TYPE_AUTORISATION number(10,0), primary key (NUMERO));

alter table ADRESSE_TIERS add constraint FKCC9F2B0192A5F163 foreign key (TIERS_ID) references TIERS;

alter table EVENEMENT_CIVIL_ERREUR add constraint FK6487445F516A94E5 foreign key (EVT_CIVIL_ID) references EVENEMENT_CIVIL_REGROUPE;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FKE4752B5B18BCAE52 foreign key (CONJOINT) references TIERS;

alter table EVENEMENT_CIVIL_REGROUPE add constraint FKE4752B5BB5047927 foreign key (INDIVIDU_PRINCIPAL) references TIERS;

alter table EVENEMENT_FISCAL add constraint FK9BFBDC74D26CBEF6 foreign key (CTB_ID) references TIERS;

alter table FOR_FISCAL add constraint FKF0623A14D26CBEF6 foreign key (CTB_ID) references TIERS;

alter table IDENTIFICATION_PERSONNE add constraint FKC88C7E7D38CC1ADE foreign key (NON_HABITANT_ID) references TIERS;

alter table MODE_IMPOSITION add constraint FK5BCFE69D26CBEF6 foreign key (CTB_ID) references TIERS;

alter table POINT_COMMUNICATION add constraint FK727AD32792A5F163 foreign key (TIERS_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK13790EB36653D1DC foreign key (TIERS_OBJET_ID) references TIERS;

alter table RAPPORT_ENTRE_TIERS add constraint FK13790EB3F2EEEB2B foreign key (TIERS_SUJET_ID) references TIERS;

create sequence S_HABITANT start with 80000001 increment by 1;

create sequence S_NON_HABITANT start with 85000001 increment by 1;

create sequence S_TIERS start with 75000001 increment by 1;

create sequence hibernate_sequence;
