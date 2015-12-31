package com.hiinoono.rest.auth;

import com.hiinoono.persistence.PersistenceManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;


/**
 *
 * @author Lyle T Harris
 */
public class Authenticate extends HystrixCommand<Boolean> {

    private final PersistenceManager pm;

    private final String tenant;

    private final String username;

    private final String password;

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("Authentication");

    /**
     * Monitor failures but don't currently open CircuitBreaker for DDoS attacks
     */
    private static final HystrixCommandProperties.Setter CB_DISABLED
            = HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false);


    public Authenticate(PersistenceManager pm,
            String tenant,
            String username,
            String password) {

        super(Setter
                .withGroupKey(GROUP_KEY)
                .andCommandPropertiesDefaults(CB_DISABLED)
        );

        this.pm = pm;
        this.tenant = tenant;
        this.username = username;
        this.password = password;

    }


    @Override
    protected Boolean run() throws Exception {
        String storedHash = pm.getHash(tenant, username);
        // So two Users with same password will showup as different hashes.
        String currentHash = pm.hash(tenant + username + password);

        if (currentHash.equals(storedHash)) {
            return true;
        } else {
            // Need to throw Exception here so Hystrix registers failure.
            throw new IllegalArgumentException("Bad Credentials");
        }
    }


    @Override
    protected Boolean getFallback() {
        return false;
    }


}
