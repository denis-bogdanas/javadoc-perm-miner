package edu.oregonstate.jdminer.inspect.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 4/26/2016.
 */
@XmlRootElement(name = "callbacks")
public class JaxbCallbackList {

    private List<JaxbCallback> callbacks = new ArrayList<>();

    @XmlElement(name = "callback")
    public List<JaxbCallback> getCallbacks() {
        return callbacks;
    }
}
