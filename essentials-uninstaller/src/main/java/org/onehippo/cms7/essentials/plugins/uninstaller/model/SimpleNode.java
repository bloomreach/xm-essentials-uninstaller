package org.onehippo.cms7.essentials.plugins.uninstaller.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "node", namespace = "http://www.jcp.org/jcr/sv/1.0")
public class SimpleNode {
	private String name;

	@XmlAttribute(name = "name", namespace = "http://www.jcp.org/jcr/sv/1.0")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}