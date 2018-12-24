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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MandateState implements LinearState {
    private final Party client;
    private final Party broker;
    private final Instant start;
    private final Instant expiry;
    private final boolean allowPnC;
    private final boolean allowIL;
    private final boolean allowGL;
    private final boolean allowHealth;
    private final UniqueIdentifier id;


    @ConstructorForDeserialization
    public MandateState(@NotNull Party client, @NotNull Party broker, @NotNull Instant start, @NotNull Instant expiry, boolean allowPnC, boolean allowIL, boolean allowGL, boolean allowHealth, @NotNull UniqueIdentifier id) {
        this.client = client;
        this.broker = broker;
        this.start = start;
        this.expiry = expiry;
        this.allowPnC = allowPnC;
        this.allowIL = allowIL;
        this.allowGL = allowGL;
        this.allowHealth = allowHealth;
        this.id = id;
    }

    public MandateState(@NotNull Party client, @NotNull Party broker, boolean allowPnC, boolean allowIL, boolean allowGL, boolean allowHealth) {
        this.client = client;
        this.broker = broker;
        this.start = Instant.now().truncatedTo(ChronoUnit.DAYS);
        this.expiry = this.start.plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.allowPnC = allowPnC;
        this.allowIL = allowIL;
        this.allowGL = allowGL;
        this.allowHealth = allowHealth;
        this.id = new UniqueIdentifier();
    }
    public MandateState(@NotNull Party client, @NotNull Party broker) {
        this.client = client;
        this.broker = broker;
        this.start = Instant.now().truncatedTo(ChronoUnit.DAYS);
        this.expiry = this.start.plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        this.allowPnC = true;
        this.allowIL = true;
        this.allowGL = true;
        this.allowHealth = true;
        this.id = new UniqueIdentifier();
    }

    public boolean isValidAt(Instant time) {
        // start <= time < expiry
        return this.start.equals(time) || (
                this.start.isBefore(time) && this.expiry.isAfter(time));
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
    public Instant getStart() {
        return start;
    }

    @NotNull
    public Instant getExpiry() {
        return expiry;
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

}
