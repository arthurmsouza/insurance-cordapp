package ch.insurance.cordapp.broker;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class MandateState implements LinearState {
    private final Party client;
    private final Party broker;
    private final Instant startAt;
    private final Instant expiredAt;
    private final List<String> allowedBusiness;

    private final Phase phase;
    private final UniqueIdentifier id;

    public static class LineOfBusiness {
        private Set<String> list = new HashSet<>();

        public static LineOfBusiness all() {
            return new LineOfBusiness().PnC().LnS();
        }
        public LineOfBusiness() {
            super();
        }
        public LineOfBusiness(@NotNull List<String> list) {
            super();
            this.list.addAll(list);
        }

        private LineOfBusiness add(Line elem) {
            this.list.add(elem.name());
            return this;
        }
        public boolean contains(Line elem) {
            return this.list.contains(elem.name());
        }
        public LineOfBusiness PnC() {
            return this.add(Line.PnC);
        }
        public LineOfBusiness IL() {
            return this.add(Line.IL);
        }
        public LineOfBusiness GL() {
            return this.add(Line.GL);
        }
        public LineOfBusiness Health() {
            return this.add(Line.Health);
        }
        public LineOfBusiness LnS() {
            return this.IL().GL().Health();
        }
        public List<String> toList() {
            return new ArrayList<>(this.list);
        }
    }

    @CordaSerializable
    public enum Line {
        PnC,
        IL,
        GL,
        Health;
    }

    @CordaSerializable
    public enum Phase {
        REQUESTED,
        WITHDRAWN,
        ACCEPTED,
        DENIED;
    }

    @ConstructorForDeserialization
    public MandateState(@NotNull Party client, @NotNull Party broker, @NotNull Instant startAt,
                        @NotNull Instant expiredAt,
                        @NotNull List<String> allowedBusiness,
                        Phase phase, @NotNull UniqueIdentifier id) {
        this.client = client;
        this.broker = broker;
        this.startAt = startAt;
        this.expiredAt = expiredAt;
        this.allowedBusiness = new LineOfBusiness(allowedBusiness).toList();
        this.phase = phase;
        this.id = id;
        this.checkValidAllowedBusiness();
    }

    public MandateState(@NotNull Party client, @NotNull Party broker,
                        @NotNull Instant startAt, @NotNull Instant expiredAt,
                        @NotNull List<String> allowedBusiness) {
        this.client = client;
        this.broker = broker;
        this.startAt = startAt;
        this.expiredAt = expiredAt;
        this.allowedBusiness = new LineOfBusiness(allowedBusiness).toList();
        this.phase = Phase.REQUESTED;
        this.id = new UniqueIdentifier();
        this.checkValidAllowedBusiness();
    }

    public MandateState(@NotNull Party client, @NotNull Party broker,
                        @NotNull List<String> allowedBusiness) {
        this.client = client;
        this.broker = broker;
        this.startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.expiredAt = this.startAt.plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.allowedBusiness = new LineOfBusiness(allowedBusiness).toList();
        this.phase = Phase.REQUESTED;
        this.id = new UniqueIdentifier();
        this.checkValidAllowedBusiness();
    }
    public MandateState(@NotNull Party client, @NotNull Party broker) {
        this(client, broker, new LineOfBusiness().all().toList());
    }

    public boolean isValidAt(Instant time) {
        // startAt <= time < expiredAt
        return this.startAt.equals(time) || (
                this.startAt.isBefore(time) && this.expiredAt.isAfter(time));
    }

    private void checkValidAllowedBusiness() {
        if (allowedBusiness.isEmpty()) throw new IllegalArgumentException("one line of business must be choosen");
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.client, this.broker);
    }

    @NotNull
    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    public UniqueIdentifier getId() {
        return id;
    }

    @NotNull
    public Party getClient() {
        return client;
    }

    @NotNull
    public Party getBroker() {
        return broker;
    }

    @NotNull
    public Instant getStartAt() {
        return startAt;
    }

    @NotNull
    public Instant getExpiredAt() {
        return expiredAt;
    }

    @NotNull
    public List<String> getAllowedBusiness() {
        return this.allowedBusiness;
    }

    public boolean isBusinessAllowed(Line line) {
        return this.allowedBusiness.contains(line);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.getId();
    }


    /*
        private final Instant startAt;
    private final Instant expiredAt;
    private final boolean allowPnC;
    private final boolean allowIL;
    private final boolean allowGL;
    private final boolean allowHealth;
    private final boolean accepted;
     */

    public MandateState accept() {
        return new MandateState(
                this.client, this.broker,this.startAt, this.expiredAt, this.allowedBusiness, Phase.ACCEPTED, this.getId());
    }
    public MandateState deny() {
        return new MandateState(
                this.client, this.broker,this.startAt, this.expiredAt, this.allowedBusiness, Phase.DENIED, this.getId());
    }
    public MandateState withdraw() {
        return new MandateState(
                this.client, this.broker,this.startAt, this.expiredAt, this.allowedBusiness, Phase.WITHDRAWN, this.getId());
    }

    public MandateState accept(Instant startAt, long days) {
        Instant newExpired = startAt.plus(days, ChronoUnit.DAYS);
        return new MandateState(
                this.client, this.broker,startAt, newExpired, this.allowedBusiness, Phase.ACCEPTED, this.getId());
    }

    public MandateState updateTimestamps(Instant startAt, long days) {
        Instant newExpired = startAt.plus(days, ChronoUnit.DAYS);
        return new MandateState(
                this.client, this.broker,startAt, newExpired, this.allowedBusiness, this.phase, this.getId());
    }
    public MandateState updateAllowedBusiness(@NotNull List<String> allowedBusiness) {
        return new MandateState(
                this.client, this.broker,this.startAt, this.expiredAt, allowedBusiness, this.phase, this.getId());
    }

    public boolean isAccepted() { return this.phase == Phase.ACCEPTED; }
    public boolean isDenied() { return this.phase == Phase.DENIED; }
    public boolean isRequested() {
        return this.phase == Phase.REQUESTED;
    }
    public boolean isWithdrawn() {
        return this.phase == Phase.WITHDRAWN;
    }

    public boolean canBeUpdated() {
        return this.phase == Phase.REQUESTED;
    }

    public Phase getPhase() { return phase; }


}
