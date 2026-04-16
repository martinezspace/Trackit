package com.trackit.priceworker;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

import java.util.List;

//Lambda entry poin - AWS and SAM CLI both call handleRequest()
// On AWS: triggered by EventBridge at 18 weekdays
//Locally: sam local invoke PriceWorkerFunction --event events/scheduled.json --env-vars env.json
public class PriceWorkerHandler implements RequestHandler<ScheduledEvent, String> {

    private final AlphaVantageClient alphaVantageClient;
    private final InvestmentServiceClient investmentServiceClient;

    public PriceWorkerHandler() {
        this.alphaVantageClient = new AlphaVantageClient(
                System.getenv("ALPHA_VANTAGE_API_KEY"));
        this.investmentServiceClient = new InvestmentServiceClient(
                System.getenv("INVESTMENT_SERVICE_URL"));
    }

    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        context.getLogger().log("PriceWorker started");

        try {
            //Step 1 - get instruments with active holdings
            //Returns InstrumentResponseDTO list - has both id and ticker
            List<InvestmentServiceClient.InstrumentInfo> instruments =
                    investmentServiceClient.getActiveHoldingInstruments();

            context.getLogger().log(
                    "Found " + instruments.size() + " active instruments");

            if (instruments.isEmpty()) {
                return "No active holdings - nothing to update";
            }

            int successCount = 0;
            int errorCount = 0;

            for (int i = 0; i < instruments.size(); i++) {
                InvestmentServiceClient.InstrumentInfo instrument = instruments.get(i);
                try {
                    //Step 2 - fetch price from Alpha Vantage using ticker
                    Double price = alphaVantageClient.getLatestPrice(instrument.ticker());

                    if (price == null) {
                        context.getLogger().log("No price found for: " + instrument.ticker());
                        errorCount++;
                        continue;
                    }

                    //Step 3 - save price using instrumentId - no lookup needed server side
                    investmentServiceClient.savePriceHistory(instrument.instrumentId(), price);
                    successCount++;
                    context.getLogger().log("Updated " + instrument.ticker() + ": " + price);

                    //Alpha Vantage free tier: 5 calls/minute - 12s delay between calls
                    if (i < instruments.size() - 1) {
                        Thread.sleep(12000);
                    }
                } catch (Exception e) {
                    context.getLogger().log("Failed " + instrument.ticker() + ": " + e.getMessage());
                    errorCount++;
                }
            }

            String result = "Completed - success: " + successCount + ", error: " + errorCount;
            context.getLogger().log(result);
            return result;
        } catch (Exception e) {
            context.getLogger().log("PriceWorker failed: " + e.getMessage());
            throw new RuntimeException("PriceWorker failed", e);
        }
    }
}
