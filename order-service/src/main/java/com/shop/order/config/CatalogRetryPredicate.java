package com.shop.order.config;


import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.util.function.Predicate;

public class CatalogRetryPredicate implements Predicate<Throwable> {


    @Override
    public boolean test(Throwable t) {
        return t instanceof ResourceAccessException
                && t.getCause() instanceof ConnectException;
    }
}
