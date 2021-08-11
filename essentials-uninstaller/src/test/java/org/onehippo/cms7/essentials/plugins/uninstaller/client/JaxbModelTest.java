package org.onehippo.cms7.essentials.plugins.uninstaller.client;

import org.junit.Test;
import org.onehippo.cms7.essentials.plugins.uninstaller.model.SimpleNode;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.junit.Assert.assertEquals;

public class JaxbModelTest {

    @Test
    public void testJaxb() throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(SimpleNode.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        SimpleNode simpleNode = (SimpleNode) unmarshaller.unmarshal(getClass().getResourceAsStream("/xml/namespaces/bannerdocument.xml"));
        String name = simpleNode.getName();
        assertEquals(name, "bannerdocument");
    }
}
