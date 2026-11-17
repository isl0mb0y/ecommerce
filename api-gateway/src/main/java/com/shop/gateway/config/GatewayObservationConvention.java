package com.shop.gateway.config;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

@Component
public class GatewayObservationConvention extends DefaultServerRequestObservationConvention {

    @Override
    public String getContextualName(ServerRequestObservationContext context) {
        Object route = context.getAttributes().get(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route instanceof Route r) {
            return context.getCarrier().getMethod().name() + " " + r.getId();
        }
        return super.getContextualName(context);
    }
}