-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.1.2bis', '7.1.2_7.1.2bis_upgrade');

-- [SIFISC-24715] historisation des quotes-parts
CREATE TABLE RF_QUOTE_PART
(
    ID               NUMBER(19) NOT NULL PRIMARY KEY,
    ANNULATION_DATE  TIMESTAMP(6),
    ANNULATION_USER  NVARCHAR2(65),
    LOG_CDATE        TIMESTAMP(6),
    LOG_CUSER        NVARCHAR2(65),
    LOG_MDATE        TIMESTAMP(6),
    LOG_MUSER        NVARCHAR2(65),
    DATE_DEBUT       NUMBER(10),
    DATE_FIN         NUMBER(10),
    QUOTE_PART_DENOM NUMBER(10) NOT NULL,
    QUOTE_PART_NUM   NUMBER(10) NOT NULL,
    IMMEUBLE_ID      NUMBER(19) NOT NULL CONSTRAINT FK_QUOTE_PART_RF_IMMEUBLE_ID REFERENCES RF_IMMEUBLE
);
CREATE INDEX IDX_QUOTE_PART_RF_IMMEUBLE_ID ON RF_QUOTE_PART (IMMEUBLE_ID);

INSERT INTO RF_QUOTE_PART SELECT
                              HIBERNATE_SEQUENCE.nextval,
                              NULL,
                              NULL,
                              CURRENT_DATE,
                              'SQL-SIFISC-24715',
                              CURRENT_DATE,
                              'SQL-SIFISC-24715',
                              NULL,
                              NULL,
                              i.QUOTE_PART_DENOM,
                              i.QUOTE_PART_NUM,
                              i.ID
                          FROM RF_IMMEUBLE i
                          WHERE i.TYPE IN ('ProprieteParEtage', 'PartCopropriete') AND i.QUOTE_PART_DENOM IS NOT NULL AND i.QUOTE_PART_NUM IS NOT NULL;

ALTER TABLE RF_IMMEUBLE DROP COLUMN QUOTE_PART_DENOM;
ALTER TABLE RF_IMMEUBLE DROP COLUMN QUOTE_PART_NUM;