package com.hiinoono.rest.auth;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.jersey.server.model.AnnotatedMethod;


/**
 * Check Methods annotated with @HiinoonoRolesAllowed and add a RequestFilter to
 * check Roles at runtime.
 *
 * @author Lyle T Harris
 */
public class HiinoonoRolesFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {

        final AnnotatedMethod am
                = new AnnotatedMethod(resourceInfo.getResourceMethod());

        HiinoonoRolesAllowed ra = am.getAnnotation(HiinoonoRolesAllowed.class);
        if (ra != null) {
            context.register(new HiinoonoRolesAllowedRequestFilter(ra));
        }

    }


}
