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
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;

public class MandateState implements LinearState {
    private final Party client;
    private final Party broker;
    private final Instant startAt;
    private final Instant expiredAt;
    private final List<Line> allowedBusiness;
    private final boolean accepted;
    private final UniqueIdentifier id;

    public static class LineOfBusiness extends HashSet<Line> {
        public static LineOfBusiness all() {
            return new LineOfBusiness().PnC().LnS();
        }
        public LineOfBusiness() {
            super();
        }
        public LineOfBusiness(@NotNull Collection<? extends Line> c) {
            super(c);
        }

        public LineOfBusiness PnC() {
            this.add(Line.PnC);
            return this;
        }
        public LineOfBusiness IL() {
            this.add(Line.IL);
            return this;
        }
        public LineOfBusiness GL() {
            this.add(Line.GL);
            return this;
        }
        public LineOfBusiness Health() {
            this.add(Line.Health);
            return this;
        }
        public LineOfBusiness LnS() {
            return this.IL().GL().Health();
        }
        public List<Line> makeImmutable() {
            return new ArrayList<>(this);
        }
    }

    @CordaSerializable
    public enum Line {
        PnC,
        IL,
        GL,
        Health;

        LineOfBusiness asSet() {
            LineOfBusiness set = new LineOfBusiness();
            set.add(this);
            return set;
        }
    }


    @ConstructorForDeserialization
    public MandateState(@NotNull Party client, @NotNull Party broker, @NotNull Instant startAt,
                        @NotNull Instant expiredAt,
                        @NotNull List<Line> allowedBusiness,
                        boolean accepted, @NotNull UniqueIdentifier id) {
        this.client = client;
        this.broker = broker;
        this.startAt = startAt;
        this.expiredAt = expiredAt;
        this.allowedBusiness = allowedBusiness;
        this.accepted = accepted;
        this.id = id;
    }

    public MandateState(@NotNull Party client, @NotNull Party broker,
                        @NotNull Instant startAt, @NotNull Instant expiredAt,
                        @NotNull List<Line> allowedBusiness) {
        this.client = client;
        this.broker = broker;
        this.startAt = startAt;
        this.expiredAt = expiredAt;
        this.allowedBusiness = allowedBusiness;
        this.accepted = false;
        this.id = new UniqueIdentifier();
    }

    public MandateState(@NotNull Party client, @NotNull Party broker,
                        @NotNull List<Line> allowedBusiness) {
        this.client = client;
        this.broker = broker;
        this.startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.expiredAt = this.startAt.plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.allowedBusiness = allowedBusiness;
        this.accepted = false;
        this.id = new UniqueIdentifier();
    }
    public MandateState(@NotNull Party client, @NotNull Party broker) {
        this(client, broker, new LineOfBusiness().all().makeImmutable());
    }

    public boolean isValidAt(Instant time) {
        // startAt <= time < expiredAt
        return this.startAt.equals(time) || (
                this.startAt.isBefore(time) && this.expiredAt.isAfter(time));
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
    public List<Line> getAllowedBusiness() {
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
                this.client, this.broker,this.startAt, this.expiredAt, this.allowedBusiness, true, this.getId());
    }

    public MandateState accept(Instant startAt, long amountDuration, TemporalUnit unit) {
        Instant newExpired = startAt.plus(amountDuration, unit);
        return new MandateState(
                this.client, this.broker,startAt, newExpired, this.allowedBusiness, true, this.getId());
    }

    public MandateState updateTimestamps(Instant startAt, long amountDuration, TemporalUnit unit) {
        Instant newExpired = startAt.plus(amountDuration, unit);
        return new MandateState(
                this.client, this.broker,startAt, newExpired, this.allowedBusiness, false, this.getId());
    }
    public MandateState updateAllowedBusiness(LineOfBusiness allowedBusiness) {
        return new MandateState(
                this.client, this.broker,this.startAt, this.expiredAt, allowedBusiness.makeImmutable(), false, this.getId());
    }
    public MandateState updateAllowedBusiness(List<Line> allowedBusiness) {
        return updateAllowedBusiness(new LineOfBusiness(allowedBusiness));
    }

    public boolean isAccepted() {
        return accepted;
    }
}
