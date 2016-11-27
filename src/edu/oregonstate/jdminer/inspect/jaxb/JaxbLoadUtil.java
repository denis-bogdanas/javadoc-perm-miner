package edu.oregonstate.jdminer.inspect.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 4/26/2016.
 */
public class JaxbLoadUtil {

    public static List<JaxbCallback> load(File file) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(JaxbCallbackList.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return ((JaxbCallbackList) jaxbUnmarshaller.unmarshal(file)).getCallbacks();
    }
}
