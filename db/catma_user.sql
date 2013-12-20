CREATE USER 'catma'@'localhost' IDENTIFIED BY 'roaring_like_thunder';

GRANT USAGE ON * . * TO 'catma'@'localhost' IDENTIFIED BY 'roaring_like_thunder' WITH MAX_QUERIES_PER_HOUR 0 MAX_CONNECTIONS_PER_HOUR 0 MAX_UPDATES_PER_HOUR 0 MAX_USER_CONNECTIONS 0 ;

GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `CatmaIndex` . * TO 'catma'@'localhost';
GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `CatmaRepository` . * TO 'catma'@'localhost';

-- GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `quartz` . * TO 'catma'@'localhost';
