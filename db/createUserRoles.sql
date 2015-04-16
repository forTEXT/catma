
-- -----------------------------------------------------
-- Table `CatmaRepository`.`permission`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `CatmaRepository`.`permission` (
  `permissionID` INT NOT NULL AUTO_INCREMENT,
  `identifier` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`permissionID`),
  UNIQUE INDEX `UK_perm_identifier` (`identifier` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `CatmaRepository`.`role`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `CatmaRepository`.`role` (
  `roleID` INT NOT NULL AUTO_INCREMENT,
  `identifier` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`roleID`),
  UNIQUE INDEX `UK_role_identifier` (`identifier` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `CatmaRepository`.`user_role`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `CatmaRepository`.`user_role` (
  `user_roleID` INT NOT NULL AUTO_INCREMENT,
  `userID` INT NOT NULL,
  `roleID` INT NOT NULL,
  PRIMARY KEY (`user_roleID`),
  INDEX `FK_user_role_userID_idx` (`userID` ASC),
  INDEX `FK_user_role_roleID_idx` (`roleID` ASC),
  CONSTRAINT `FK_user_role_userID`
    FOREIGN KEY (`userID`)
    REFERENCES `CatmaRepository`.`user` (`userID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_user_role_roleID`
    FOREIGN KEY (`roleID`)
    REFERENCES `CatmaRepository`.`role` (`roleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `CatmaRepository`.`role_permission`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `CatmaRepository`.`role_permission` (
  `role_permissionID` INT NOT NULL AUTO_INCREMENT,
  `roleID` INT NOT NULL,
  `permissionID` INT NOT NULL,
  PRIMARY KEY (`role_permissionID`),
  INDEX `FK_role_perm_roleID_idx` (`roleID` ASC),
  INDEX `FK_role_perm_permissionID_idx` (`permissionID` ASC),
  CONSTRAINT `FK_role_perm_roleID`
    FOREIGN KEY (`roleID`)
    REFERENCES `CatmaRepository`.`role` (`roleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_role_perm_permissionID`
    FOREIGN KEY (`permissionID`)
    REFERENCES `CatmaRepository`.`permission` (`permissionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

ALTER TABLE user DROP COLUMN role;

INSERT INTO role(identifier) VALUES('admin');
INSERT INTO role(identifier) VALUES('user');
INSERT INTO role(identifier) VALUES('heureclea');

INSERT INTO permission(identifier) VALUES('adminwindow');
INSERT INTO permission(identifier) VALUES('autotagging');
INSERT INTO permission(identifier) VALUES('exportcorpus');

SELECT @adminRole:=roleID FROM role WHERE identifier = 'admin';
SELECT @heurecleaRole:=roleID FROM role WHERE identifier = 'heureclea';
SELECT @adminWindow:=permissionID FROM permission WHERE identifier = 'adminwindow';
SELECT @autotagging:=permissionID FROM permission WHERE identifier = 'autotagging';
SELECT @exportcorpus:=permissionID FROM permission WHERE identifier = 'exportcorpus';

INSERT INTO role_permission(roleID, permissionID) VALUES (@adminRole, @adminWindow);
INSERT INTO role_permission(roleID, permissionID) VALUES (@adminRole, @autotagging);
INSERT INTO role_permission(roleID, permissionID) VALUES (@adminRole, @exportcorpus);
INSERT INTO role_permission(roleID, permissionID) VALUES (@heurecleaRole, @autotagging);

SELECT @userRole := roleID FROM role WHERE identifier = 'user';

INSERT INTO user_role(userID, roleID) SELECT userID, @userRole FROM user;

-- SELECT @mp := userID FROM user WHERE identifier = 'petris.it@googlemail.com';

-- INSERT INTO user_role(userID, roleID) values(@mp,@adminRole);
