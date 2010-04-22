create table ADRESSE_TIERS (ADR_TYPE varchar2(31 char) not null, id number(19,0) not null, DATE_DEBUT date not null, DATE_FIN date, 
LIGNE_SUPPL varchar2(255 char), RUE varchar2(255 char), NUMERO_MAISON varchar2(255 char), NUMERO_APPART varchar2(255 char), 
texteCasePostale number(10,0), NUMERO_CP number(10,0), numeroPostal varchar2(255 char), paysOFS number(10,0), 
localite varchar2(255 char), lieu varchar2(255 char), NUMERO_RUE number(10,0), NUMERO_ONRP number(10,0), REPRESENTANT_ID number(19,0), 
EC_ID number(19,0), TIERS_ID number(19,0), primary key (id));


create table COMPOSITION_FAMILLE (ID number(19,0) not null, dateDebut date, dateFin date, situationFiscale number(10,0), 
nombreEnfants number(10,0), TIERS_ID number(19,0), CONT_PERS_PHYS_ID number(19,0), primary key (ID));

create table ENTITE_CIVILE (ENTITE_TYPE varchar2(31 char) not null, id number(19,0) not null, NUMERO_ENTREPRISE number(19,0), 
NUMERO_INDIVIDU number(19,0) unique, NUMERO_ASSURE_SOCIAL varchar2(255 char), ANCIEN_NUMERO_AVS varchar2(255 char), nom varchar2(255 char), 
prenom varchar2(255 char), DATE_NAISSANCE varchar2(255 char), sexe number(10,0), nationalite varchar2(255 char), 
TYPE_AUTORISATION number(10,0), DATE_VALID_AUTORISATION date, nom1 varchar2(255 char), nom2 varchar2(255 char), 
nom3 varchar2(255 char), formeJuridique number(10,0), primary key (id));

create table ERREUR (id number(19,0) not null, champ varchar2(255 char), message varchar2(255 char), ERREUR_ID number(19,0), 
primary key (id));

create table EVENEMENT_CIVIL (id number(19,0) not null, DATE_EVENEMENT date, DATE_TRAITEMENT date, numeroIndividu number(19,0), 
numeroTechnique number(19,0), NO_OFS_COMMUNE number(19,0), CODE number(10,0), numeroConjoint number(19,0), statut number(10,0), 
EC_ID number(19,0), primary key (id));

create table EVENEMENT_CIVIL_ENFANT (id number(19,0) not null, evenementId number(19,0), numeroIndividu number(19,0), primary key (id));

create table EVENEMENT_FISCAL (id number(19,0) not null, type number(10,0), DATE_EVENEMENT date, DATE_TRAITEMENT date, 
CTB_ID number(19,0), primary key (id));

create table FOR_FISCAL (FOR_DISCR varchar2(31 char) not null, id number(19,0) not null, TYPE_FOR number(10,0), GENRE_IMPOT number(10,0), 
EST_LE_FOR_DE_GESTION number(1,0), EST_ANNULE number(1,0), NUMERO_FOR number(10,0), DATE_EVENEMENT date, DATE_FERMETURE date, 
rattachement number(10,0), CTB_ID number(19,0), primary key (id));

create table IDENTIFICATION_PERSONNE (id number(19,0) not null, source number(10,0), identifiant varchar2(255 char), 
NON_HABITANT_ID number(19,0), primary key (id));

create table MODE_ASSUJETTISSEMENT (id number(19,0) not null, DATE_DEBUT date, DATE_FIN date, CATEGORIE_IMPOT_ORDINAIRE number(10,0), 
CTB_ID number(19,0), primary key (id));

create table REPRESENTATION (REPRESENTATION_TYPE varchar2(31 char) not null, id number(19,0) not null, dateDebut date, dateFin date, 
TIERS_REPRESENTANT_ID number(19,0), primary key (id));

create table TIERS (TIERS_TYPE varchar2(31 char) not null, NUMERO number(19,0) not null, FORMULE_POLITESSE number(10,0), 
NOM_COURRIER1 varchar2(255 char), NOM_COURRIER2 varchar2(255 char), COMPLEMENT_RAISON_SOCIALE varchar2(255 char), 
NUMERO_COMPTE_BANCAIRE varchar2(255 char), NUMERO_TELECOPIE varchar2(255 char), NUMERO_TELEPHONE_FIXE varchar2(255 char), 
NUMERO_TELEPHONE_PORTABLE varchar2(255 char), REMARQUE varchar2(255 char), TITULAIRE_COMPTE_BANCAIRE varchar2(255 char), 
ADRESSE_COURRIER_ELECTRONIQUE varchar2(255 char), ANCIEN_NO_SOURCIER number(19,0), CATEGORIE_IMPOT_SOURCE number(10,0), 
PERIODICITE_DECOMPTE number(10,0), MODE_COMMUNICATION number(10,0), EC_ID number(19,0), SECOND_MEMBRE_ID number(19,0), 
PREMIER_MEMBRE_ID number(19,0), primary key (NUMERO));

alter table ADRESSE_TIERS add constraint FKCC9F2B0143149FB5 foreign key (REPRESENTANT_ID) references TIERS;
alter table ADRESSE_TIERS add constraint FKCC9F2B01C4944474 foreign key (EC_ID) references ENTITE_CIVILE;
alter table ADRESSE_TIERS add constraint FKCC9F2B0192A5F163 foreign key (TIERS_ID) references TIERS;
alter table COMPOSITION_FAMILLE add constraint FK3817B619CFE937CF foreign key (CONT_PERS_PHYS_ID) references TIERS;
alter table COMPOSITION_FAMILLE add constraint FK3817B619E6D9800C foreign key (TIERS_ID) references TIERS;
alter table ERREUR add constraint FK7A683E3D77E730A6 foreign key (ERREUR_ID) references EVENEMENT_CIVIL;
alter table EVENEMENT_CIVIL add constraint FKF459C45D40D37E66 foreign key (EC_ID) references ENTITE_CIVILE;
alter table EVENEMENT_CIVIL_ENFANT add constraint FK6449620C8D89EF8D foreign key (EVENEMENTID) references EVENEMENT_CIVIL;
alter table EVENEMENT_FISCAL add constraint FK9BFBDC741F0DA0CC foreign key (CTB_ID) references TIERS;
alter table FOR_FISCAL add constraint FKF0623A14D26CBEF6 foreign key (CTB_ID) references TIERS;
alter table IDENTIFICATION_PERSONNE add constraint FKC88C7E7D38CC1ADE foreign key (NON_HABITANT_ID) references ENTITE_CIVILE;
alter table MODE_ASSUJETTISSEMENT add constraint FK261FC185D26CBEF6 foreign key (CTB_ID) references TIERS;
alter table REPRESENTATION add constraint FK56053CCD98641E47 foreign key (TIERS_REPRESENTANT_ID) references TIERS;
alter table TIERS add constraint FK4C1F2B1BDA67F71 foreign key (SECOND_MEMBRE_ID) references TIERS;
alter table TIERS add constraint FK4C1F2B1C4944474 foreign key (EC_ID) references ENTITE_CIVILE;
alter table TIERS add constraint FK4C1F2B141407169 foreign key (PREMIER_MEMBRE_ID) references TIERS;
