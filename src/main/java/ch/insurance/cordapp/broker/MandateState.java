package ch.insurance.cordapp.broker;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MandateState implements LinearState {
    private final Party client;
    private final Party broker;
    private final Instant startAt;
    private final Instant expiredAt;
    private final boolean allowPnC;
    private final boolean allowIL;
    private final boolean allowGL;
    private final boolean allowHealth;
    private final boolean accepted;
    private final UniqueIdentifier id;


    @ConstructorForDeserialization
    public MandateState(@NotNull Party client, @NotNull Party broker, @NotNull Instant startAt, @NotNull Instant expiredAt, boolean allowPnC, boolean allowIL, boolean allowGL, boolean allowHealth, boolean accepted, @NotNull UniqueIdentifier id) {
        this.client = client;
        this.broker = broker;
        this.startAt = startAt;
        this.expiredAt = expiredAt;
        this.allowPnC = allowPnC;
        this.allowIL = allowIL;
        this.allowGL = allowGL;
        this.allowHealth = allowHealth;
        this.accepted = accepted;
        this.id = id;
    }

    public MandateState(@NotNull Party client, @NotNull Party broker, @NotNull Instant startAt, @NotNull Instant expiredAt, boolean allowPnC, boolean allowIL, boolean allowGL, boolean allowHealth) {
        this.client = client;
        this.broker = broker;
        this.startAt = startAt;
        this.expiredAt = expiredAt;
        this.allowPnC = allowPnC;
        this.allowIL = allowIL;
        this.allowGL = allowGL;
        this.allowHealth = allowHealth;
        this.accepted = false;
        this.id = new UniqueIdentifier();
    }

    public MandateState(@NotNull Party client, @NotNull Party broker, boolean allowPnC, boolean allowIL, boolean allowGL, boolean allowHealth) {
        this.client = client;
        this.broker = broker;
        this.startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.expiredAt = this.startAt.plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.allowPnC = allowPnC;
        this.allowIL = allowIL;
        this.allowGL = allowGL;
        this.allowHealth = allowHealth;
        this.accepted = false;
        this.id = new UniqueIdentifier();
    }
    public MandateState(@NotNull Party client, @NotNull Party broker) {
        this(client, broker, true, true, true, true);
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

    public boolean isAllowPnC() {
        return allowPnC;
    }

    public boolean isAllowIL() {
        return allowIL;
    }

    public boolean isAllowGL() {
        return allowGL;
    }

    public boolean isAllowHealth() {
        return allowHealth;
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
                this.client, this.broker,this.startAt, this.expiredAt, this.allowPnC, this.allowIL, this.allowGL, this.allowHealth, true, this.getId());
    }

    public MandateState accept(Instant startAt, long amountDuration, TemporalUnit unit) {
        Instant newExpired = startAt.plus(amountDuration, unit);
        return new MandateState(
                this.client, this.broker,startAt, newExpired, this.allowPnC, this.allowIL, this.allowGL, this.allowHealth, true, this.getId());
    }

    public MandateState updateTimestamps(Instant startAt, long amountDuration, TemporalUnit unit) {
        Instant newExpired = startAt.plus(amountDuration, unit);
        return new MandateState(
                this.client, this.broker,startAt, newExpired, this.allowPnC, this.allowIL, this.allowGL, this.allowHealth, false, this.getId());
    }
    public MandateState updateAllowance(boolean allowPnC, boolean allowIL, boolean allowGL, boolean allowHealth) {
        return new MandateState(
                this.client, this.broker,this.startAt, this.expiredAt, allowPnC, allowIL, allowGL, allowHealth, false, this.getId());
    }


    public boolean isAccepted() {
        return accepted;
    }
}
