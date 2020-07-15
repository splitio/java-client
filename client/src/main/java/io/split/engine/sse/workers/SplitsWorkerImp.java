package io.split.engine.sse.workers;

import io.split.engine.experiments.SplitFetcher;

public class SplitsWorkerImp extends Worker<Long> implements SplitsWorker {
    private final SplitFetcher _splitFetcher;

    public SplitsWorkerImp(SplitFetcher splitFetcher) {
        super("Splits");
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
