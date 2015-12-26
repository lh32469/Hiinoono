package com.hiinoono.rest.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * For user with HiinoonoRolesFeature for security access.
 *
 * @author Lyle T Harris
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD})
public @interface HiinoonoRolesAllowed {

    /**
     * User Roles that are allowed to access the annotated method.
     *
     * @return
     */
    public String[] roles();


    /**
     * Message to be returned to Client/User if User is not in one of the Roles
     * present in roles().
     *
     * @return
     */
    public String message();


}
