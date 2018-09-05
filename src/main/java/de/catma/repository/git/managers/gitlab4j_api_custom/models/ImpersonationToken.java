package de.catma.repository.git.managers.gitlab4j_api_custom.models;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ImpersonationToken {
	public Integer id;
	public Boolean revoked;
	public String[] scopes;
	public String token;
	public Boolean active;
	public Boolean impersonation;
	public String name;
	public String createdAt;
	public String expiresAt;
}
