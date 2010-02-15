-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('3.5.2', '3.5.1_3.5.2_upgrade');

-- Récupération d'une erreur passée (valeur oubliée dans le script 3.3.3_3.4_upgrade)
ALTER TABLE DOC_INDEX MODIFY (DOC_TYPE NVARCHAR2(50));

-- Eclatement des types de mouvements de dossier
UPDATE MOUVEMENT_DOSSIER SET MVT_TYPE='EnvoiVersCollAdm' WHERE MVT_TYPE='EnvoiDossier' AND COLL_ADMIN_ID IS NOT NULL;
UPDATE MOUVEMENT_DOSSIER SET MVT_TYPE='EnvoiVersCollaborateur' WHERE MVT_TYPE='EnvoiDossier';

UPDATE MOUVEMENT_DOSSIER SET MVT_TYPE='ReceptionClassementGeneral' WHERE MVT_TYPE='ReceptionDossier' AND LOCALISATION='CLASSEMENT_GENERAL';
UPDATE MOUVEMENT_DOSSIER SET MVT_TYPE='ReceptionClassementIndepdt' WHERE MVT_TYPE='ReceptionDossier' AND LOCALISATION='CLASSEMENT_INDEPENDANTS';
UPDATE MOUVEMENT_DOSSIER SET MVT_TYPE='ReceptionPersonnel' WHERE MVT_TYPE='ReceptionDossier' AND LOCALISATION='PERSONNE';
UPDATE MOUVEMENT_DOSSIER SET MVT_TYPE='ReceptionArchives' WHERE MVT_TYPE='ReceptionDossier' AND LOCALISATION='ARCHIVES';

ALTER TABLE MOUVEMENT_DOSSIER DROP COLUMN LOCALISATION;
ALTER TABLE MOUVEMENT_DOSSIER RENAME COLUMN COLL_ADMIN_ID TO COLL_ADMIN_DEST_ID;
ALTER TABLE MOUVEMENT_DOSSIER ADD (COLL_ADMIN_EMETTRICE_ID number(19,0), ETAT nvarchar2(15));
ALTER TABLE MOUVEMENT_DOSSIER ADD CONSTRAINT FK_ENV_DOS_CA_EMETT_ID FOREIGN KEY (COLL_ADMIN_EMETTRICE_ID) REFERENCES TIERS;
ALTER TABLE MOUVEMENT_DOSSIER DROP CONSTRAINT FK_ENV_DOS_CA_ID;
ALTER TABLE MOUVEMENT_DOSSIER ADD CONSTRAINT FK_ENV_DOS_CA_DEST_ID FOREIGN KEY (COLL_ADMIN_DEST_ID) REFERENCES TIERS;
UPDATE MOUVEMENT_DOSSIER SET ETAT='TRAITE';
ALTER TABLE MOUVEMENT_DOSSIER MODIFY ETAT nvarchar2(15) NOT NULL;

CREATE TABLE BORDEREAU_MVT_DOSSIER (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), primary key (id));
ALTER TABLE MOUVEMENT_DOSSIER ADD BORDEREAU_ID number(19,0);
ALTER TABLE MOUVEMENT_DOSSIER ADD CONSTRAINT FK_MVT_DOSSIER_BORD_ID FOREIGN KEY (BORDEREAU_ID) REFERENCES BORDEREAU_MVT_DOSSIER;

ALTER TABLE MOUVEMENT_DOSSIER ADD COLL_ADMIN_RECEPTRICE_ID number(19,0);
ALTER TABLE MOUVEMENT_DOSSIER ADD CONSTRAINT FK_REC_DOS_CA_ID FOREIGN KEY (COLL_ADMIN_RECEPTRICE_ID) REFERENCES TIERS;

CREATE INDEX IDX_MVT_DOSSIER_CTB_ID ON MOUVEMENT_DOSSIER(CTB_ID);
CREATE INDEX IDX_MOUVEMENT_DOSSIER_ETAT_CTB ON MOUVEMENT_DOSSIER (ETAT, CTB_ID);

ALTER TABLE MOUVEMENT_DOSSIER ADD DATE_MOUVEMENT number(10,0);
UPDATE MOUVEMENT_DOSSIER SET DATE_MOUVEMENT=TO_CHAR(LOG_CDATE, 'YYYYMMDD') WHERE ETAT='TRAITE';

ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD WORK_USER nvarchar2(65);
