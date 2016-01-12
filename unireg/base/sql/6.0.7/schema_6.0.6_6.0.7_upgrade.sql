-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.7', '6.0.6_6.0.7_upgrade');

-- allongement de la taille de la colonne 20 -> 31
ALTER TABLE FLAG_ENTREPRISE MODIFY FLAG NVARCHAR2(31);

-- événement fiscal autour de la gestion des flags entreprise
ALTER TABLE EVENEMENT_FISCAL ADD (FLAG_ENTREPRISE_ID NUMBER(19,0), TYPE_EVT_FLAG NVARCHAR2(15));
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_FLAG_ID FOREIGN KEY (FLAG_ENTREPRISE_ID) REFERENCES FLAG_ENTREPRISE;

--
-- Les 'Données RC' et les capitaux vont être regroupés dans trois entités distinctes de la même table
--
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
CREATE INDEX IDX_DONCIV_ENTRP_ID ON DONNEE_CIVILE_ENTREPRISE (ENTREPRISE_ID ASC, DATE_DEBUT ASC, TYPE_DONNEE ASC);
INSERT INTO DONNEE_CIVILE_ENTREPRISE (ID, ANNULATION_DATE, ANNULATION_USER, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, TYPE_DONNEE, DATE_DEBUT, DATE_FIN, RS_RAISON_SOCIALE, ENTREPRISE_ID)
		SELECT HIBERNATE_SEQUENCE.NEXTVAL, ANNULATION_DATE, ANNULATION_USER, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, 'RaisonSociale', DATE_DEBUT, DATE_FIN, RAISON_SOCIALE, ENTREPRISE_ID
		FROM DONNEES_RC;
INSERT INTO DONNEE_CIVILE_ENTREPRISE (ID, ANNULATION_DATE, ANNULATION_USER, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, TYPE_DONNEE, DATE_DEBUT, DATE_FIN, FJ_FORME_JURIDIQUE, ENTREPRISE_ID)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, ANNULATION_DATE, ANNULATION_USER, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, 'FormeJuridique', DATE_DEBUT, DATE_FIN, FORME_JURIDIQUE, ENTREPRISE_ID
	FROM DONNEES_RC;
INSERT INTO DONNEE_CIVILE_ENTREPRISE (ID, ANNULATION_DATE, ANNULATION_USER, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, TYPE_DONNEE, DATE_DEBUT, DATE_FIN, CAP_MONTANT, CAP_MONNAIE, ENTREPRISE_ID)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, ANNULATION_DATE, ANNULATION_USER, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, 'Capital', DATE_DEBUT, DATE_FIN, MONTANT, MONNAIE, ENTREPRISE_ID
	FROM CAPITAL_ENTREPRISE;
DROP TABLE DONNEES_RC PURGE;
DROP TABLE CAPITAL_ENTREPRISE PURGE;
