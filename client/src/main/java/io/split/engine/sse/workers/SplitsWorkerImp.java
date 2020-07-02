package io.split.engine.sse.workers;

import io.split.engine.experiments.SplitFetcher;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public class SplitsWorkerImp extends WorkerImp<Long> implements SplitsWorker {
    private final SplitFetcher _splitFetcher;

    public SplitsWorkerImp(SplitFetcher splitFetcher) {
        super(LoggerFactory.getLogger(SplitsWorker.class), "Splits");
        _splitFetcher = splitFetcher;
    }

    @Override
    public void killSplit(long changeNumber, String splitName, String defaultTreatment) {
        try {
            _splitFetcher.killSplit(splitName, defaultTreatment, changeNumber);
            _log.debug(String.format("Kill split: %s, changeNumber: %s, defaultTreatment: %s", splitName, changeNumber, defaultTreatment));
        } catch (Exception ex) {
            _log.error(String.format("Exception on SplitWorker killSplit: %s", ex.getMessage()));
        }
    }

    @Override
    protected void executeRefresh(Long changeNumber) {
        if (changeNumber > _splitFetcher.changeNumber()) {
            _splitFetcher.forceRefresh();
        }
    }
}
