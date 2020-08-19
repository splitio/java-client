package io.split.engine.sse.workers;

import io.split.engine.common.Synchronizer;
import io.split.engine.experiments.SplitFetcher;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsWorkerImp extends Worker<Long> implements SplitsWorker {
    private final Synchronizer _synchronizer;

    public SplitsWorkerImp(Synchronizer synchronizer) {
        super("Splits");
        _synchronizer = checkNotNull(synchronizer);
    }

    @Override
    public void killSplit(long changeNumber, String splitName, String defaultTreatment) {
        try {
            _synchronizer.localKillSplit(splitName, defaultTreatment, changeNumber);
            _log.debug(String.format("Kill split: %s, changeNumber: %s, defaultTreatment: %s", splitName, changeNumber, defaultTreatment));
        } catch (Exception ex) {
            _log.error(String.format("Exception on SplitWorker killSplit: %s", ex.getMessage()));
        }
    }

    @Override
    protected void executeRefresh(Long changeNumber) {
        _synchronizer.refreshSplits(changeNumber);
    }
}
