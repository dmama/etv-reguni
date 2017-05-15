-- Certains régimes fiscaux ont été mal entrés : 109-2 au lieu de 190-2
UPDATE REGIME_FISCAL SET CODE='190-2' WHERE CODE='109-2';