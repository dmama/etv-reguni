-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.2.1', '6.2.0_6.2.1_upgrade');

--
-- Autres documents fiscaux : nouveaux champs pour nouveaux documents
-- AR_DATE_DEMANDE : date de la demande de radiation du RC à placer dans le courrier d'autorisation de radiation
-- DBF_PERIODE_FISCALE : période fiscale indiquée dans la lettre de demande de bilan final
-- DBF_DATE_REQ_RADIATION : date de la réquisition de radiation émise par le RC dans la lettre de demande de bilan final
--

ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD AR_DATE_DEMANDE NUMBER(10,0);
ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD (DBF_PERIODE_FISCALE NUMBER(10,0), DBF_DATE_REQ_RADIATION NUMBER(10,0));