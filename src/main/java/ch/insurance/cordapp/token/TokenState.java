package ch.insurance.cordapp.token;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

/* Our state, defining a shared fact on the ledger.
 * See src/main/java/examples/ArtState.java for an example. */
public class TokenState implements LinearState {
    private final Party issuer;
    private final Party owner;
    private final Amount<Currency> amount;
    private final Amount<Currency> paid;
    private final UniqueIdentifier id;


    public TokenState(Party issuer, Party owner, Amount<Currency> amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.paid = new Amount<>(0, amount.getToken());
        this.id = new UniqueIdentifier();
    }

    public TokenState(Party issuer, Party owner, Amount<Currency> amount, UniqueIdentifier id) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.paid = new Amount<>(0, amount.getToken());
        this.id = id;
    }

    @ConstructorForDeserialization
    public TokenState(Party issuer, Party owner, Amount<Currency> amount, Amount<Currency> paid, UniqueIdentifier id) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.paid = paid;
        this.id = id;
    }

    @NotNull
	@Override
	public List<AbstractParty> getParticipants() {
		return Arrays.asList(this.issuer, this.owner);
    }

    @NotNull
    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    public Party getOwner() {
        return owner;
    }

    public Party getIssuer() {
        return issuer;
    }

    public Amount<Currency>  getAmount() {
        return amount;
    }

    public Amount<Currency>  getPaid() {
        return paid;
    }

    public UniqueIdentifier getId() {
        return id;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.getId();
    }

    // create new state with new owner
    public TokenState withNewOwner(Party newOwner) {
        return new TokenState(this.owner, newOwner, this.amount, this.id);
    }

    // create new state with new paid amount
    public TokenState pay(Amount<Currency> amountToPay) {
        return new TokenState(
                this.issuer,
                this.owner,
                this.amount,
                this.paid.plus(amountToPay),
                this.id
        );
    }
}