package com.deutscheboerse.risk.dave.restapi.ers;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;

import java.util.ArrayList;
import java.util.List;

public class PositionReportApi extends AbstractErsApi {

    public PositionReportApi(Vertx vertx) {
        super(vertx, "query.latestPositionReport", "query.historyPositionReport", "pr");
    }

    @Override
    protected List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add("clearer");
        parameters.add("member");
        parameters.add("account");
        parameters.add("symbol");
        parameters.add("putCall");
        parameters.add("strikePrice");
        parameters.add("optAttribute");
        parameters.add("maturityMonthYear");
        return parameters;
    }

    public Router getRoutes()
    {
        Router router = Router.router(vertx);

        router.get("/latest").handler(this::latestCall);
        router.get("/latest/:clearer").handler(this::latestCall);
        router.get("/latest/:clearer/:member").handler(this::latestCall);
        router.get("/latest/:clearer/:member/:account").handler(this::latestCall);
        router.get("/latest/:clearer/:member/:account/:symbol").handler(this::latestCall);
        router.get("/latest/:clearer/:member/:account/:symbol/:putCall").handler(this::latestCall);
        router.get("/latest/:clearer/:member/:account/:symbol/:putCall/:strikePrice").handler(this::latestCall);
        router.get("/latest/:clearer/:member/:account/:symbol/:putCall/:strikePrice/:optAttribute").handler(this::latestCall);
        router.get("/latest/:clearer/:member/:account/:symbol/:putCall/:strikePrice/:optAttribute/:maturityMonthYear").handler(this::latestCall);
        router.get("/history").handler(this::historyCall);
        router.get("/history/:clearer").handler(this::historyCall);
        router.get("/history/:clearer/:member").handler(this::historyCall);
        router.get("/history/:clearer/:member/:account").handler(this::historyCall);
        router.get("/history/:clearer/:member/:account/:symbol").handler(this::historyCall);
        router.get("/history/:clearer/:member/:account/:symbol/:putCall").handler(this::historyCall);
        router.get("/history/:clearer/:member/:account/:symbol/:putCall/:strikePrice").handler(this::historyCall);
        router.get("/history/:clearer/:member/:account/:symbol/:putCall/:strikePrice/:optAttribute").handler(this::historyCall);
        router.get("/history/:clearer/:member/:account/:symbol/:putCall/:strikePrice/:optAttribute/:maturityMonthYear").handler(this::historyCall);

        return router;
    }
}
