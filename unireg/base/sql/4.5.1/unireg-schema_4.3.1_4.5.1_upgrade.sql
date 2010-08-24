-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.5.1', '4.3.1_4.5.1_upgrade');

--
-- [UNIREG-2412] Ajout de possibilités au service d'identification UniReg asynchrone

--Ajout du mode d'identification
--Ajout du de l'indicateur d'attente de l'identification manuel
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB add (MODE_IDENTIFICATION nvarchar2(255) default 'MANUEL_SANS_ACK' not null,ATTENTE_IDENTIF_MANUEL number(10,0) default 0);

--
-- [UNIREG-1059] Déplacé les remarques dans une table satellite
--
create table REMARQUE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), TEXTE nvarchar2(2000), TIERS_ID number(19,0) not null, primary key (id));
alter table REMARQUE add constraint FK_REMARQUE_TRS_ID foreign key (TIERS_ID) references TIERS;

insert into REMARQUE(ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, TEXTE, TIERS_ID)
select hibernate_sequence.nextval, CURRENT_DATE, '[reprise initiale]', CURRENT_DATE, '[reprise initiale]', t.REMARQUE, t.NUMERO from TIERS t where t.REMARQUE is not null;

create index IDX_REMARQUE_TIERS_ID on REMARQUE (TIERS_ID);

--
--[UNIREG-2138] Historisation de la periodicite du débiteur
--
create table PERIODICITE (PERIODICITE_TYPE nvarchar2(31) not null,id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65),DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0) ,PERIODE_DECOMPTE nvarchar2(3), DEBITEUR_ID number(19,0) not null, primary key (id));

alter table PERIODICITE add constraint FK_PERIODICITE_DB_ID foreign key (DEBITEUR_ID) references TIERS;

create index IDX_P_DEBITEUR_ID on PERIODICITE (DEBITEUR_ID);

--
-- [UNIREG-2399] Fusion des événements civils unitaires et regroupés - cleanup
--
drop table EVENEMENT_CIVIL_UNITAIRE;

--
-- [UNIREG-1979] Ajout de la fonctionnalité de schedule de réindexation dans le futur
--
ALTER TABLE TIERS add (REINDEX_ON number(10,0));

--
-- Mouvements de dossiers en masse : drop oublié
ALTER TABLE MOUVEMENT_DOSSIER DROP COLUMN OID_EMETTEUR;

--
-- [UNIREG-2690] Annulation de tiers à une date donnée
--
UPDATE TIERS T SET ANNULATION_DATE=NULL, ANNULATION_USER=NULL
WHERE TO_CHAR(T.ANNULATION_DATE, 'HH24:MI:SS') = '00:00:00'
AND EXISTS (SELECT 1 FROM FOR_FISCAL FF WHERE FF.TIERS_ID = T.NUMERO AND FF.ANNULATION_DATE IS NULL AND FF.MOTIF_FERMETURE='ANNULATION' AND FF.DATE_FERMETURE = CAST(TO_CHAR(T.ANNULATION_DATE, 'YYYYMMDD') AS NUMBER));

--
-- [UNIREG-2735] Identification des DI dites "libres"
--
ALTER TABLE DECLARATION ADD (LIBRE number(1,0));
UPDATE DECLARATION SET LIBRE=0 WHERE DOCUMENT_TYPE='DI';
UPDATE DECLARATION SET LIBRE=1
WHERE ID IN (	SELECT DI.ID FROM DECLARATION DI JOIN PERIODE_FISCALE PF ON DI.PERIODE_ID=PF.ID AND PF.ANNEE=TO_CHAR(CURRENT_DATE,'YYYY')
				WHERE DI.ANNULATION_DATE IS NULL AND DI.DOCUMENT_TYPE='DI'
				AND NOT EXISTS (SELECT 1 FROM FOR_FISCAL FF WHERE FF.TIERS_ID=DI.TIERS_ID AND FF.ANNULATION_DATE IS NULL AND FF.DATE_FERMETURE=DI.DATE_FIN)
			);
