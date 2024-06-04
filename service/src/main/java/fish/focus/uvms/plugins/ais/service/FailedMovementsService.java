package fish.focus.uvms.plugins.ais.service;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FailedMovementsService {

    private static final Logger LOG = LoggerFactory.getLogger(FailedMovementsService.class);

    private final List<MovementBaseType> failedSendList = new ArrayList<>();

    @Inject
    private StartupBean startUp;

    @Inject
    private ExchangeService exchangeService;

    @Schedule(minute = "*/15", hour = "*", persistent = false)
    public void resend() {
        if (!startUp.isRegistered()) {
            return;
        }
        try {
            List<MovementBaseType> failedMovements = getAndClearFailedMovementList();
            List<MovementBaseType> failedToSendMovements = exchangeService.sendMovements(failedMovements);
            add(failedToSendMovements);
        } catch (Exception e) {
            LOG.error(e.toString(), e);
        }
    }

    public void add(List<MovementBaseType> failedToSendMovements) {
        synchronized (failedSendList) {
            failedSendList.addAll(failedToSendMovements);
        }
    }

    private List<MovementBaseType> getAndClearFailedMovementList() {
        List<MovementBaseType> tmp;
        synchronized (failedSendList) {
            tmp = new ArrayList<>(failedSendList);
            failedSendList.clear();
        }
        return tmp;
    }
}
