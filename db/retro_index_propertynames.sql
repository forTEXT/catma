alter table CatmaIndex.property add column name varchar(45);

update CatmaIndex.property pi
set pi.name = 
(select pd.name
 from CatmaRepository.propertydefinition pd
 join CatmaRepository.property p
	on p.propertyDefinitionID = pd.propertyDefinitionID
 join CatmaRepository.taginstance ti
	on ti.tagInstanceID = p.tagInstanceID
 where pd.uuid = pi.propertyDefinitionID and ti.uuid = pi.tagInstanceID);