package com.sctrcd.drools;

import com.sctrcd.beans.BeanMatcher;
import com.sctrcd.beans.BeanPropertyFilter;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.rule.FactHandle;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FactFinder<T> {

    private BeanMatcher beanMatcher = new BeanMatcher();

    private Class<?> classToFind;

    public FactFinder(Class<?> classToFind) {
        this.classToFind = classToFind;
    }

    /**
     * An assertion that a fact of the expected class with specified properties
     * is in working memory.
     *
     * @param session            A {@link KieSession} in which we are looking for the
     *                           fact.
     * @param expectedProperties A sequence of expected property name/value pairs.
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("unchecked")
    public List<T> findFacts(final KieSession session, final BeanPropertyFilter... expectedProperties) {

        ObjectFilter filter = object -> object.getClass().equals(classToFind)
                                        && beanMatcher.matches(object, expectedProperties);

        Collection<FactHandle> factHandles = session.getFactHandles(filter);
        return factHandles.stream().map(factHandle -> (T)session.getObject(factHandle)).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<T> findFacts(final KieSession session) {

        ObjectFilter filter = object -> object.getClass().equals(classToFind);

        Collection<FactHandle> factHandles = session.getFactHandles(filter);
        return factHandles.stream().map(factHandle -> (T)session.getObject(factHandle)).collect(Collectors.toList());
    }

    public void deleteFacts(final KieSession session, final BeanPropertyFilter... expectedProperties) {

        ObjectFilter filter = object -> object.getClass().equals(classToFind)
                                        && beanMatcher.matches(object, expectedProperties);

        Collection<FactHandle> factHandles = session.getFactHandles(filter);
        factHandles.forEach(session::delete);
    }

}
