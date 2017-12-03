CREATE USER 'catma'@'localhost' IDENTIFIED BY 'test';

-- GRANT USAGE ON * . * TO 'catma'@'localhost' IDENTIFIED BY 'test';

GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `catmaindex` . * TO 'catma'@'localhost';
GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `catmarepository` . * TO 'catma'@'localhost';

GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `quartz` . * TO 'catma'@'localhost';
