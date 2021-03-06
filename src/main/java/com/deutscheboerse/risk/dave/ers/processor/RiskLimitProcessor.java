package com.deutscheboerse.risk.dave.ers.processor;

import com.deutscheboerse.risk.dave.ers.jaxb.RiskWarningLevelGrpBlockT;
import com.deutscheboerse.risk.dave.ers.jaxb.AbstractMessageT;
import com.deutscheboerse.risk.dave.ers.jaxb.FIXML;
import com.deutscheboerse.risk.dave.ers.jaxb.PartyDetailGrpBlockT;
import com.deutscheboerse.risk.dave.ers.jaxb.PartyRiskLimitsReportMessageT;
import com.deutscheboerse.risk.dave.ers.jaxb.RiskLimitTypesGrpBlockT;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import javax.xml.bind.JAXBElement;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class RiskLimitProcessor extends AbstractProcessor implements Processor {

    private JsonArray parseFromFIXML(FIXML fixml) {
        JsonArray riskLimits = new JsonArray();

        JAXBElement<? extends AbstractMessageT> msg = fixml.getMessage();
        PartyRiskLimitsReportMessageT rlMessage = (PartyRiskLimitsReportMessageT) msg.getValue();

        // Parse generic data which are common for all limit types
        Date received = new Date();
        String clearer = null;
        String member = null;
        String maintainer = null;
        GregorianCalendar txnTmInFrankfurtZone = rlMessage.getTxnTm().toGregorianCalendar();
        txnTmInFrankfurtZone.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        String reqId = rlMessage.getReqID();
        String rptId = rlMessage.getRptID();
        String reqRslt = rlMessage.getReqRslt();
        String txt = rlMessage.getTxt();

        List<PartyDetailGrpBlockT> parties = rlMessage.getPtyRiskLmt().get(0).getPtyDetl();

        for (PartyDetailGrpBlockT party : parties)
        {
            switch (party.getR().intValue())
            {
                case 4:
                    clearer = party.getID();
                    break;
                case 1:
                    member = party.getID();
                    break;
                case 7:
                    maintainer = party.getID();
                    break;
                default:
                    break;
            }
        }

        // Parse the specific risk limits
        List<RiskLimitTypesGrpBlockT> limits = rlMessage.getPtyRiskLmt().get(0).getRiskLmt().get(0).getRiskLmtTyp();

        for (RiskLimitTypesGrpBlockT limit : limits)
        {
            JsonObject rl = new JsonObject();
            rl.put("received", new JsonObject().put("$date", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            rl.put("clearer", clearer);
            rl.put("member", member);
            rl.put("maintainer", maintainer);
            rl.put("txnTm", new JsonObject().put("$date", txnTmInFrankfurtZone.toZonedDateTime().withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            rl.put("reqId", reqId);
            rl.put("rptId", rptId);
            rl.put("reqRslt", reqRslt);
            rl.put("txt", txt);

            switch (limit.getTyp())
            {
                case "4001":
                    rl.put("limitType", "TMR");
                    break;
                case "4002":
                    rl.put("limitType", "CULI");
                    break;
                case "4003":
                    rl.put("limitType", "CASH");
                    break;
                case "4004":
                    rl.put("limitType", "NDM");
                    break;
            }

            rl.put("utilization", limit.getUtilztnAmt().doubleValue());
            for (RiskWarningLevelGrpBlockT level : limit.getWarnLvl())
            {
                switch (level.getActn().toString())
                {
                    case "4":
                        rl.put("warningLevel", new BigDecimal(level.getAmt()).doubleValue());
                        break;
                    case "0":
                        rl.put("throttleLevel", new BigDecimal(level.getAmt()).doubleValue());
                        break;
                    case "2":
                        rl.put("rejectLevel", new BigDecimal(level.getAmt()).doubleValue());
                        break;
                }
            }

            riskLimits.add(rl);
        }

        return riskLimits;
    }

   @Override
   public void process(Exchange exchange) {
        Message in = exchange.getIn();
        in.setBody(this.parseFromFIXML((FIXML)in.getBody()));
    }
}
